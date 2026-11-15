const http = require('http');
const path = require('path');
const express = require('express');
const cors = require('cors');
const multer = require('multer');

const {
  signToken,
  verifyAdminPassword,
  normalizeMobile,
  authMiddleware,
  requireAdmin,
} = require('./auth');
const { PersistentStore } = require('./storage');
const { RealtimeHub } = require('./realtime');

async function createSolarHubServer(options = {}) {
  const store = new PersistentStore({
    dataDir: options.dataDir,
    uploadsDir: options.uploadsDir,
  });
  await store.init();

  const hub = new RealtimeHub();
  const app = express();
  app.use(cors());
  app.use(express.json({ limit: '10mb' }));

  const upload = multer({
    storage: multer.memoryStorage(),
    limits: { fileSize: 50 * 1024 * 1024 }, // 50 MB max per file
  });

  // Static web companion app
  const publicDir = path.join(__dirname, '..', 'public');
  app.use(express.static(publicDir));

  // Health check
  app.get('/api/health', (req, res) => {
    res.json({ ok: true, serverTime: Date.now(), storageMode: store.usePg ? 'postgresql' : 'persistent-disk' });
  });

  // --- AUTHENTICATION ---
  // Employee simple login (Name + Mobile, no OTP)
  app.post('/api/auth/login', async (req, res) => {
    try {
      const name = String(req.body?.name || '').trim();
      const rawMobile = String(req.body?.mobile || '').trim();
      const mobile = normalizeMobile(rawMobile);

      if (name.length < 2) {
        return res.status(400).json({ error: 'Please enter your full name.' });
      }
      if (mobile.length < 10) {
        return res.status(400).json({ error: 'Please enter a valid 10-digit mobile number.' });
      }

      const user = await store.upsertEmployeeUser(name, mobile);
      const token = signToken({
        userId: user.id,
        name: user.name,
        mobile: user.mobile,
        role: 'EMPLOYEE',
      });

      res.json({ user, token });
    } catch (err) {
      res.status(500).json({ error: err.message || 'Login failed' });
    }
  });

  // Backend-verified Admin Login (NEVER verified in client code)
  app.post('/api/auth/admin/login', async (req, res) => {
    try {
      const password = String(req.body?.password || '');
      if (!verifyAdminPassword(password)) {
        return res.status(401).json({ error: 'Invalid admin credentials.' });
      }
      const user = {
        id: 'admin_master',
        name: String(req.body?.name || 'Admin').trim() || 'Admin',
        mobile: String(req.body?.mobile || '').trim(),
        role: 'ADMIN',
      };
      const token = signToken({
        userId: user.id,
        name: user.name,
        mobile: user.mobile,
        role: 'ADMIN',
      });
      res.json({ user, token });
    } catch (err) {
      res.status(500).json({ error: err.message || 'Admin login failed' });
    }
  });

  // --- SYNCHRONIZATION ---
  app.get('/api/sync', authMiddleware, async (req, res) => {
    try {
      const since = Number(req.query.since || 0);
      const data = await store.syncSince(since, req.user);
      res.json(data);
    } catch (err) {
      res.status(500).json({ error: err.message || 'Sync failed' });
    }
  });

  // --- DEPARTMENTS ---
  app.get('/api/departments', authMiddleware, async (req, res) => {
    try {
      const departments = await store.listDepartments();
      res.json({ departments });
    } catch (err) {
      res.status(500).json({ error: err.message || 'Failed to fetch departments' });
    }
  });

  app.post('/api/departments', authMiddleware, requireAdmin, async (req, res) => {
    try {
      const name = String(req.body?.name || '').trim();
      if (!name) return res.status(400).json({ error: 'Department name is required.' });
      const dept = await store.saveDepartment({
        id: req.body?.id || null,
        name,
        iconKey: req.body?.iconKey || 'factory',
        orderIndex: req.body?.orderIndex,
      });
      hub.broadcastPublic('DEPARTMENT_UPDATED', dept);
      res.json({ department: dept });
    } catch (err) {
      res.status(500).json({ error: err.message || 'Failed to save department' });
    }
  });

  app.put('/api/departments/:id', authMiddleware, requireAdmin, async (req, res) => {
    try {
      const name = String(req.body?.name || '').trim();
      if (!name) return res.status(400).json({ error: 'Department name is required.' });
      const dept = await store.saveDepartment({
        id: req.params.id,
        name,
        iconKey: req.body?.iconKey,
        orderIndex: req.body?.orderIndex,
      });
      hub.broadcastPublic('DEPARTMENT_UPDATED', dept);
      res.json({ department: dept });
    } catch (err) {
      res.status(500).json({ error: err.message || 'Failed to update department' });
    }
  });

  app.delete('/api/departments/:id', authMiddleware, requireAdmin, async (req, res) => {
    try {
      await store.deleteDepartment(req.params.id);
      hub.broadcastPublic('DEPARTMENT_DELETED', { id: req.params.id });
      res.json({ ok: true, id: req.params.id });
    } catch (err) {
      res.status(500).json({ error: err.message || 'Failed to delete department' });
    }
  });

  // --- CONTENT & ANNOUNCEMENTS ---
  app.get('/api/content', authMiddleware, async (req, res) => {
    try {
      const includeDrafts = req.user.role === 'ADMIN' && req.query.includeDrafts === '1';
      const departmentId = req.query.departmentId ? String(req.query.departmentId) : null;
      const items = await store.listContent({ includeDrafts, departmentId });
      res.json({ contentItems: items });
    } catch (err) {
      res.status(500).json({ error: err.message || 'Failed to list content' });
    }
  });

  app.post('/api/content', authMiddleware, requireAdmin, async (req, res) => {
    try {
      const title = String(req.body?.title || '').trim();
      const departmentId = String(req.body?.departmentId || '').trim();
      if (!title) return res.status(400).json({ error: 'Title is required.' });
      if (!departmentId) return res.status(400).json({ error: 'Department is required.' });

      const item = await store.saveContent({
        ...req.body,
        adminAuthor: req.user.name || 'Admin',
      });

      if (item.isPublished) {
        hub.broadcastPublic('CONTENT_UPDATED', item);
      } else {
        hub.broadcastToAdmins('CONTENT_UPDATED', item);
        // If previously published and now unpublished, tell employees to remove from public cache
        hub.broadcastPublic('CONTENT_UNPUBLISHED', { id: item.id, isPublished: false });
      }
      res.json({ contentItem: item });
    } catch (err) {
      res.status(500).json({ error: err.message || 'Failed to save content' });
    }
  });

  app.patch('/api/content/:id/publish', authMiddleware, requireAdmin, async (req, res) => {
    try {
      const existing = await store.getContentById(req.params.id);
      if (!existing) return res.status(404).json({ error: 'Content item not found.' });
      const isPublished = Boolean(req.body?.isPublished);
      const updated = await store.saveContent({ ...existing, isPublished });
      if (updated.isPublished) {
        hub.broadcastPublic('CONTENT_UPDATED', updated);
      } else {
        hub.broadcastToAdmins('CONTENT_UPDATED', updated);
        hub.broadcastPublic('CONTENT_UNPUBLISHED', { id: updated.id, isPublished: false });
      }
      res.json({ contentItem: updated });
    } catch (err) {
      res.status(500).json({ error: err.message || 'Failed to update publish status' });
    }
  });

  app.delete('/api/content/:id', authMiddleware, requireAdmin, async (req, res) => {
    try {
      await store.deleteContent(req.params.id);
      hub.broadcastPublic('CONTENT_DELETED', { id: req.params.id });
      res.json({ ok: true, id: req.params.id });
    } catch (err) {
      res.status(500).json({ error: err.message || 'Failed to delete content' });
    }
  });

  // --- PERSISTENT FILE UPLOAD & DOWNLOAD ---
  app.post('/api/files/upload', authMiddleware, requireAdmin, upload.single('file'), async (req, res) => {
    try {
      if (!req.file) {
        return res.status(400).json({ error: 'No file provided.' });
      }
      const idempotencyKey = req.headers['x-idempotency-key']
        ? String(req.headers['x-idempotency-key'])
        : req.body?.idempotencyKey || null;
      const saved = await store.saveUploadedFile({
        buffer: req.file.buffer,
        originalName: req.file.originalname,
        mimeType: req.file.mimetype,
        idempotencyKey,
      });

      res.json({
        file: {
          id: saved.id,
          originalName: saved.originalName,
          mimeType: saved.mimeType,
          sizeBytes: saved.sizeBytes,
          url: `/api/files/${saved.id}`,
          duplicate: Boolean(saved.duplicate),
        },
      });
    } catch (err) {
      res.status(500).json({ error: err.message || 'File upload failed' });
    }
  });

  app.get('/api/files/:id', authMiddleware, async (req, res) => {
    try {
      const record = await store.getFileRecord(req.params.id);
      if (!record) return res.status(404).json({ error: 'File not found.' });
      const buffer = await store.readFileBuffer(record);
      if (!buffer) return res.status(404).json({ error: 'File binary missing from storage.' });

      res.setHeader('Content-Type', record.mimeType || 'application/octet-stream');
      res.setHeader('Content-Length', buffer.length);
      res.setHeader('Accept-Ranges', 'bytes');
      res.setHeader(
        'Content-Disposition',
        `inline; filename="${encodeURIComponent(record.originalName)}"`
      );
      res.send(buffer);
    } catch (err) {
      res.status(500).json({ error: err.message || 'Failed to read file' });
    }
  });

  // --- REAL-TIME REQUESTS ---
  app.get('/api/requests', authMiddleware, async (req, res) => {
    try {
      const requests = await store.listRequestsForUser(req.user);
      res.json({ requests });
    } catch (err) {
      res.status(500).json({ error: err.message || 'Failed to load requests' });
    }
  });

  app.post('/api/requests', authMiddleware, async (req, res) => {
    try {
      const details = String(req.body?.details || '').trim();
      const title = String(req.body?.title || '').trim();
      if (!details) return res.status(400).json({ error: 'Request details are required.' });

      const { request, duplicate } = await store.createRequest({
        clientRequestId: req.body?.clientRequestId || null,
        userId: req.user.userId,
        userName: req.user.name,
        userMobile: req.user.mobile,
        departmentId: req.body?.departmentId || '',
        departmentName: req.body?.departmentName || '',
        title: title || 'General Request',
        details,
      });

      if (!duplicate) {
        // Privacy isolation: ONLY send to Admin and the submitting user!
        hub.broadcastToUserAndAdmins(req.user.userId, 'REQUEST_CREATED', request);
      }
      res.status(duplicate ? 200 : 201).json({ request, duplicate });
    } catch (err) {
      res.status(500).json({ error: err.message || 'Failed to submit request' });
    }
  });

  // Only Admin can accept/reject/reply/complete requests
  app.patch('/api/requests/:id', authMiddleware, requireAdmin, async (req, res) => {
    try {
      const allowedStatuses = ['PENDING', 'ACCEPTED', 'REJECTED', 'COMPLETED'];
      const status = req.body?.status ? String(req.body.status).toUpperCase() : undefined;
      if (status && !allowedStatuses.includes(status)) {
        return res.status(400).json({ error: 'Invalid request status.' });
      }
      const updated = await store.updateRequestByAdmin(req.params.id, {
        status,
        adminReply: req.body?.adminReply,
      });
      if (!updated) return res.status(404).json({ error: 'Request not found.' });

      // Privacy isolation: ONLY notify Admins and the owning user!
      hub.broadcastToUserAndAdmins(updated.userId, 'REQUEST_UPDATED', updated);
      res.json({ request: updated });
    } catch (err) {
      res.status(500).json({ error: err.message || 'Failed to update request' });
    }
  });

  // Only Admin can permanently delete requests
  app.delete('/api/requests/:id', authMiddleware, requireAdmin, async (req, res) => {
    try {
      const deleted = await store.deleteRequestByAdmin(req.params.id);
      if (!deleted) return res.status(404).json({ error: 'Request not found.' });
      hub.broadcastToUserAndAdmins(deleted.userId, 'REQUEST_DELETED', { id: deleted.id });
      res.json({ ok: true, id: deleted.id });
    } catch (err) {
      res.status(500).json({ error: err.message || 'Failed to delete request' });
    }
  });

  // Admin backup trigger
  app.post('/api/admin/backup', authMiddleware, requireAdmin, async (req, res) => {
    try {
      const backupFile = await store.createBackupSnapshot();
      res.json({ ok: true, backupFile: path.basename(backupFile) });
    } catch (err) {
      res.status(500).json({ error: err.message || 'Backup failed' });
    }
  });

  const server = http.createServer(app);
  hub.attach(server);

  return {
    app,
    server,
    store,
    hub,
    async start(port = process.env.PORT || 3000) {
      return new Promise((resolve) => {
        server.listen(port, () => {
          const addr = server.address();
          resolve(addr.port);
        });
      });
    },
    async stop() {
      hub.close();
      server.closeAllConnections?.();
      await new Promise((resolve) => server.close(resolve));
      await store.close();
    },
  };
}

if (require.main === module) {
  createSolarHubServer()
    .then(({ start }) => start(process.env.PORT || 3000))
    .then((port) => {
      console.log(`SOLAR HUB centralized backend listening on port ${port}`);
    })
    .catch((err) => {
      console.error('Fatal startup error:', err);
      process.exit(1);
    });
}

module.exports = { createSolarHubServer };
