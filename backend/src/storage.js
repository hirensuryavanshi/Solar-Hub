const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { Pool } = require('pg');

const INITIAL_DEPARTMENTS = [
  { id: 'dept_production', name: 'Production', iconKey: 'factory', orderIndex: 1, createdAt: 1000, updatedAt: 1000 },
  { id: 'dept_quality', name: 'Quality', iconKey: 'verified', orderIndex: 2, createdAt: 1001, updatedAt: 1001 },
  { id: 'dept_packing', name: 'Packing', iconKey: 'inventory', orderIndex: 3, createdAt: 1002, updatedAt: 1002 },
  { id: 'dept_sap', name: 'SAP', iconKey: 'hub', orderIndex: 4, createdAt: 1003, updatedAt: 1003 },
  { id: 'dept_ppc', name: 'PPC', iconKey: 'timeline', orderIndex: 5, createdAt: 1004, updatedAt: 1004 },
  { id: 'dept_solar_cell', name: 'Solar Cell', iconKey: 'solar_cell', orderIndex: 6, createdAt: 1005, updatedAt: 1005 },
  { id: 'dept_solar_module', name: 'Solar Module', iconKey: 'solar_module', orderIndex: 7, createdAt: 1006, updatedAt: 1006 },
  { id: 'dept_maintenance', name: 'Maintenance', iconKey: 'build', orderIndex: 8, createdAt: 1007, updatedAt: 1007 },
  { id: 'dept_safety', name: 'Safety', iconKey: 'security', orderIndex: 9, createdAt: 1008, updatedAt: 1008 },
  { id: 'dept_renewable_energy', name: 'Renewable Energy', iconKey: 'eco', orderIndex: 10, createdAt: 1009, updatedAt: 1009 },
  { id: 'dept_store', name: 'Store', iconKey: 'warehouse', orderIndex: 11, createdAt: 1010, updatedAt: 1010 },
  { id: 'dept_dispatch', name: 'Dispatch', iconKey: 'local_shipping', orderIndex: 12, createdAt: 1011, updatedAt: 1011 },
  { id: 'dept_rnd', name: 'R&D', iconKey: 'science', orderIndex: 13, createdAt: 1012, updatedAt: 1012 },
  { id: 'dept_engineering', name: 'Engineering', iconKey: 'engineering', orderIndex: 14, createdAt: 1013, updatedAt: 1013 },
  { id: 'dept_ehs', name: 'EHS', iconKey: 'eco', orderIndex: 15, createdAt: 1014, updatedAt: 1014 },
  { id: 'dept_hr_training', name: 'HR & Training', iconKey: 'groups', orderIndex: 16, createdAt: 1015, updatedAt: 1015 },
  { id: 'dept_purchase', name: 'Purchase', iconKey: 'shopping_cart', orderIndex: 17, createdAt: 1016, updatedAt: 1016 },
  { id: 'dept_planning_inventory', name: 'Planning & Inventory', iconKey: 'calendar_month', orderIndex: 18, createdAt: 1017, updatedAt: 1017 }
];

class PersistentStore {
  constructor(options = {}) {
    this.dataDir = options.dataDir || path.join(__dirname, '..', 'data');
    this.uploadsDir = options.uploadsDir || path.join(__dirname, '..', 'uploads');
    this.backupsDir = path.join(this.dataDir, 'backups');
    this.dbFile = path.join(this.dataDir, 'solarhub.json');
    this.pool = null;
    this.usePg = Boolean(process.env.DATABASE_URL);
    this.state = {
      initialized: false,
      users: [],
      departments: [],
      contentItems: [],
      requests: [],
      files: [],
      tombstones: [],
    };
    this._writeChain = Promise.resolve();
  }

  async init() {
    fs.mkdirSync(this.dataDir, { recursive: true });
    fs.mkdirSync(this.uploadsDir, { recursive: true });
    fs.mkdirSync(this.backupsDir, { recursive: true });

    if (this.usePg) {
      this.pool = new Pool({
        connectionString: process.env.DATABASE_URL,
        ssl: process.env.DATABASE_URL.includes('localhost') ? false : { rejectUnauthorized: false },
      });
      await this._initPgTables();
      await this._seedDepartmentsIfEmpty();
    } else {
      if (fs.existsSync(this.dbFile)) {
        const raw = fs.readFileSync(this.dbFile, 'utf8');
        const parsed = JSON.parse(raw);
        this.state = {
          initialized: Boolean(parsed.initialized),
          users: Array.isArray(parsed.users) ? parsed.users : [],
          departments: Array.isArray(parsed.departments) ? parsed.departments : [],
          contentItems: Array.isArray(parsed.contentItems) ? parsed.contentItems : [],
          requests: Array.isArray(parsed.requests) ? parsed.requests : [],
          files: Array.isArray(parsed.files) ? parsed.files : [],
          tombstones: Array.isArray(parsed.tombstones) ? parsed.tombstones : [],
        };
      }
      await this._seedDepartmentsIfEmpty();
    }
  }

  async close() {
    if (this.pool) {
      await this.pool.end();
      this.pool = null;
    }
  }

  async _initPgTables() {
    await this.pool.query(`
      CREATE TABLE IF NOT EXISTS users (
        id TEXT PRIMARY KEY,
        name TEXT NOT NULL,
        mobile TEXT UNIQUE NOT NULL,
        role TEXT NOT NULL DEFAULT 'EMPLOYEE',
        created_at BIGINT NOT NULL,
        last_login_at BIGINT NOT NULL
      );
      CREATE TABLE IF NOT EXISTS departments (
        id TEXT PRIMARY KEY,
        name TEXT NOT NULL,
        icon_key TEXT NOT NULL,
        order_index INT NOT NULL DEFAULT 0,
        created_at BIGINT NOT NULL,
        updated_at BIGINT NOT NULL
      );
      CREATE TABLE IF NOT EXISTS content_items (
        id TEXT PRIMARY KEY,
        department_id TEXT NOT NULL,
        category TEXT NOT NULL DEFAULT '',
        subcategory TEXT NOT NULL DEFAULT '',
        title TEXT NOT NULL,
        description TEXT NOT NULL DEFAULT '',
        content_body TEXT NOT NULL DEFAULT '',
        content_type TEXT NOT NULL DEFAULT 'TEXT',
        attachment_file_id TEXT,
        attachment_name TEXT,
        attachment_url TEXT,
        attachment_size_bytes BIGINT NOT NULL DEFAULT 0,
        attachment_mime_type TEXT,
        is_published BOOLEAN NOT NULL DEFAULT TRUE,
        is_announcement BOOLEAN NOT NULL DEFAULT FALSE,
        published_at BIGINT NOT NULL,
        updated_at BIGINT NOT NULL,
        admin_author TEXT NOT NULL DEFAULT 'Admin'
      );
      CREATE TABLE IF NOT EXISTS requests (
        id TEXT PRIMARY KEY,
        client_request_id TEXT,
        user_id TEXT NOT NULL,
        user_name TEXT NOT NULL,
        user_mobile TEXT NOT NULL,
        department_id TEXT NOT NULL DEFAULT '',
        department_name TEXT NOT NULL DEFAULT '',
        title TEXT NOT NULL DEFAULT '',
        details TEXT NOT NULL,
        status TEXT NOT NULL DEFAULT 'PENDING',
        admin_reply TEXT NOT NULL DEFAULT '',
        created_at BIGINT NOT NULL,
        updated_at BIGINT NOT NULL,
        replied_at BIGINT
      );
      CREATE UNIQUE INDEX IF NOT EXISTS idx_requests_user_client_id
        ON requests (user_id, client_request_id) WHERE client_request_id IS NOT NULL;
      CREATE TABLE IF NOT EXISTS files (
        id TEXT PRIMARY KEY,
        idempotency_key TEXT UNIQUE,
        original_name TEXT NOT NULL,
        mime_type TEXT NOT NULL,
        size_bytes BIGINT NOT NULL,
        sha256 TEXT NOT NULL,
        storage_provider TEXT NOT NULL DEFAULT 'local',
        storage_path TEXT NOT NULL,
        created_at BIGINT NOT NULL
      );
      CREATE TABLE IF NOT EXISTS tombstones (
        id SERIAL PRIMARY KEY,
        entity_type TEXT NOT NULL,
        entity_id TEXT NOT NULL,
        owner_user_id TEXT,
        deleted_at BIGINT NOT NULL
      );
    `);
  }

  async _seedDepartmentsIfEmpty() {
    if (this.usePg) {
      const res = await this.pool.query('SELECT COUNT(*)::int AS cnt FROM departments');
      if (res.rows[0].cnt === 0) {
        for (const d of INITIAL_DEPARTMENTS) {
          await this.pool.query(
            `INSERT INTO departments (id, name, icon_key, order_index, created_at, updated_at)
             VALUES ($1, $2, $3, $4, $5, $6) ON CONFLICT (id) DO NOTHING`,
            [d.id, d.name, d.iconKey, d.orderIndex, d.createdAt, d.updatedAt]
          );
        }
      } else {
        // Non-destructively ensure Renewable Energy exists if not already present and user has never deleted it
        await this.pool.query(
          `INSERT INTO departments (id, name, icon_key, order_index, created_at, updated_at)
           SELECT $1, $2, $3, $4, $5, $6
           WHERE NOT EXISTS (SELECT 1 FROM departments WHERE LOWER(name) = 'renewable energy')
             AND NOT EXISTS (SELECT 1 FROM tombstones WHERE entity_type = 'department' AND entity_id = $1)`,
          ['dept_renewable_energy', 'Renewable Energy', 'eco', 10, Date.now(), Date.now()]
        );
      }
      return;
    }

    if (!this.state.initialized && this.state.departments.length === 0) {
      this.state.departments = [...INITIAL_DEPARTMENTS];
      this.state.initialized = true;
      await this._flushFile();
    } else if (!this.state.initialized) {
      this.state.initialized = true;
      await this._flushFile();
    }
  }

  async _flushFile() {
    this._writeChain = this._writeChain.then(() => {
      const tmpFile = `${this.dbFile}.tmp.${process.pid}`;
      const json = JSON.stringify(this.state, null, 2);
      fs.writeFileSync(tmpFile, json, 'utf8');
      fs.renameSync(tmpFile, this.dbFile);
    });
    return this._writeChain;
  }

  async createBackupSnapshot() {
    const stamp = new Date().toISOString().replace(/[:.]/g, '-');
    const snapshotPath = path.join(this.backupsDir, `solarhub-backup-${stamp}.json`);
    let payload;
    if (this.usePg) {
      const [users, departments, contentItems, requests, files] = await Promise.all([
        this.pool.query('SELECT * FROM users'),
        this.pool.query('SELECT * FROM departments'),
        this.pool.query('SELECT * FROM content_items'),
        this.pool.query('SELECT * FROM requests'),
        this.pool.query('SELECT * FROM files'),
      ]);
      payload = {
        exportedAt: Date.now(),
        users: users.rows,
        departments: departments.rows,
        contentItems: contentItems.rows,
        requests: requests.rows,
        files: files.rows,
      };
    } else {
      payload = { exportedAt: Date.now(), ...this.state };
    }
    fs.writeFileSync(snapshotPath, JSON.stringify(payload, null, 2), 'utf8');

    // Keep last 10 backup files
    const existing = fs
      .readdirSync(this.backupsDir)
      .filter((f) => f.startsWith('solarhub-backup-'))
      .sort();
    while (existing.length > 10) {
      const oldest = existing.shift();
      try {
        fs.unlinkSync(path.join(this.backupsDir, oldest));
      } catch {}
    }
    return snapshotPath;
  }

  // --- USERS ---
  async upsertEmployeeUser(name, normalizedMobile) {
    const now = Date.now();
    if (this.usePg) {
      const existing = await this.pool.query('SELECT * FROM users WHERE mobile = $1', [normalizedMobile]);
      if (existing.rows.length > 0) {
        const u = existing.rows[0];
        await this.pool.query('UPDATE users SET name = $1, last_login_at = $2 WHERE id = $3', [
          name.trim(),
          now,
          u.id,
        ]);
        return {
          id: u.id,
          name: name.trim(),
          mobile: u.mobile,
          role: 'EMPLOYEE',
          createdAt: Number(u.created_at),
          lastLoginAt: now,
        };
      }
      const id = `usr_${crypto.randomUUID().replace(/-/g, '').slice(0, 12)}`;
      await this.pool.query(
        'INSERT INTO users (id, name, mobile, role, created_at, last_login_at) VALUES ($1, $2, $3, $4, $5, $6)',
        [id, name.trim(), normalizedMobile, 'EMPLOYEE', now, now]
      );
      return { id, name: name.trim(), mobile: normalizedMobile, role: 'EMPLOYEE', createdAt: now, lastLoginAt: now };
    }

    let user = this.state.users.find((u) => u.mobile === normalizedMobile);
    if (user) {
      user.name = name.trim();
      user.lastLoginAt = now;
    } else {
      user = {
        id: `usr_${crypto.randomUUID().replace(/-/g, '').slice(0, 12)}`,
        name: name.trim(),
        mobile: normalizedMobile,
        role: 'EMPLOYEE',
        createdAt: now,
        lastLoginAt: now,
      };
      this.state.users.push(user);
    }
    await this._flushFile();
    return { ...user };
  }

  // --- DEPARTMENTS ---
  async listDepartments() {
    if (this.usePg) {
      const res = await this.pool.query('SELECT * FROM departments ORDER BY order_index ASC, name ASC');
      return res.rows.map((r) => ({
        id: r.id,
        name: r.name,
        iconKey: r.icon_key,
        orderIndex: r.order_index,
        createdAt: Number(r.created_at),
        updatedAt: Number(r.updated_at),
      }));
    }
    return [...this.state.departments].sort((a, b) => a.orderIndex - b.orderIndex || a.name.localeCompare(b.name));
  }

  async saveDepartment({ id, name, iconKey, orderIndex }) {
    const now = Date.now();
    const deptId = id || `dept_${crypto.randomUUID().replace(/-/g, '').slice(0, 8)}`;
    const depts = await this.listDepartments();
    const existing = depts.find((d) => d.id === deptId);
    const finalOrder =
      orderIndex !== undefined && orderIndex !== null
        ? orderIndex
        : existing
        ? existing.orderIndex
        : (depts.reduce((m, d) => Math.max(m, d.orderIndex), 0) || 0) + 1;
    const dept = {
      id: deptId,
      name: String(name).trim(),
      iconKey: iconKey || existing?.iconKey || 'factory',
      orderIndex: finalOrder,
      createdAt: existing ? existing.createdAt : now,
      updatedAt: now,
    };

    if (this.usePg) {
      await this.pool.query(
        `INSERT INTO departments (id, name, icon_key, order_index, created_at, updated_at)
         VALUES ($1, $2, $3, $4, $5, $6)
         ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, icon_key = EXCLUDED.icon_key, order_index = EXCLUDED.order_index, updated_at = EXCLUDED.updated_at`,
        [dept.id, dept.name, dept.iconKey, dept.orderIndex, dept.createdAt, dept.updatedAt]
      );
    } else {
      const idx = this.state.departments.findIndex((d) => d.id === dept.id);
      if (idx >= 0) this.state.departments[idx] = dept;
      else this.state.departments.push(dept);
      await this._flushFile();
    }
    return dept;
  }

  async deleteDepartment(id) {
    const now = Date.now();
    if (this.usePg) {
      const contents = await this.pool.query('SELECT id FROM content_items WHERE department_id = $1', [id]);
      for (const row of contents.rows) {
        await this.pool.query('INSERT INTO tombstones (entity_type, entity_id, deleted_at) VALUES ($1, $2, $3)', [
          'content',
          row.id,
          now,
        ]);
      }
      await this.pool.query('DELETE FROM content_items WHERE department_id = $1', [id]);
      await this.pool.query('DELETE FROM departments WHERE id = $1', [id]);
      await this.pool.query('INSERT INTO tombstones (entity_type, entity_id, deleted_at) VALUES ($1, $2, $3)', [
        'department',
        id,
        now,
      ]);
      return;
    }

    const removedContent = this.state.contentItems.filter((c) => c.departmentId === id);
    for (const c of removedContent) {
      this.state.tombstones.push({ entityType: 'content', entityId: c.id, ownerUserId: null, deletedAt: now });
    }
    this.state.contentItems = this.state.contentItems.filter((c) => c.departmentId !== id);
    this.state.departments = this.state.departments.filter((d) => d.id !== id);
    this.state.tombstones.push({ entityType: 'department', entityId: id, ownerUserId: null, deletedAt: now });
    await this._flushFile();
  }

  // --- CONTENT & ANNOUNCEMENTS ---
  _mapPgContent(r) {
    return {
      id: r.id,
      departmentId: r.department_id,
      category: r.category,
      subcategory: r.subcategory,
      title: r.title,
      description: r.description,
      contentBody: r.content_body,
      contentType: r.content_type,
      attachmentFileId: r.attachment_file_id,
      attachmentName: r.attachment_name,
      attachmentUrlOrPath: r.attachment_url,
      attachmentSizeBytes: Number(r.attachment_size_bytes || 0),
      attachmentMimeType: r.attachment_mime_type,
      isPublished: Boolean(r.is_published),
      isAnnouncement: Boolean(r.is_announcement),
      publishedAt: Number(r.published_at),
      updatedAt: Number(r.updated_at),
      adminAuthor: r.admin_author,
    };
  }

  async listContent({ includeDrafts = false, departmentId = null } = {}) {
    if (this.usePg) {
      const clauses = [];
      const params = [];
      if (!includeDrafts) clauses.push('is_published = TRUE');
      if (departmentId) {
        params.push(departmentId);
        clauses.push(`department_id = $${params.length}`);
      }
      const where = clauses.length ? `WHERE ${clauses.join(' AND ')}` : '';
      const res = await this.pool.query(`SELECT * FROM content_items ${where} ORDER BY published_at DESC`, params);
      return res.rows.map((r) => this._mapPgContent(r));
    }

    return this.state.contentItems
      .filter((c) => (includeDrafts || c.isPublished) && (!departmentId || c.departmentId === departmentId))
      .sort((a, b) => b.publishedAt - a.publishedAt);
  }

  async getContentById(id) {
    if (this.usePg) {
      const res = await this.pool.query('SELECT * FROM content_items WHERE id = $1', [id]);
      return res.rows[0] ? this._mapPgContent(res.rows[0]) : null;
    }
    return this.state.contentItems.find((c) => c.id === id) || null;
  }

  async saveContent(input) {
    const now = Date.now();
    const id = input.id || `cnt_${crypto.randomUUID().replace(/-/g, '').slice(0, 12)}`;
    const existing = await this.getContentById(id);
    const isPublished = input.isPublished !== undefined ? Boolean(input.isPublished) : true;
    const item = {
      id,
      departmentId: String(input.departmentId || existing?.departmentId || ''),
      category: String(input.category || '').trim(),
      subcategory: String(input.subcategory || '').trim(),
      title: String(input.title || '').trim(),
      description: String(input.description || '').trim(),
      contentBody: String(input.contentBody || '').trim(),
      contentType: String(input.contentType || 'TEXT'),
      attachmentFileId: input.attachmentFileId !== undefined ? input.attachmentFileId : existing?.attachmentFileId || null,
      attachmentName: input.attachmentName !== undefined ? input.attachmentName : existing?.attachmentName || null,
      attachmentUrlOrPath:
        input.attachmentUrlOrPath !== undefined ? input.attachmentUrlOrPath : existing?.attachmentUrlOrPath || null,
      attachmentSizeBytes:
        input.attachmentSizeBytes !== undefined
          ? Number(input.attachmentSizeBytes || 0)
          : existing?.attachmentSizeBytes || 0,
      attachmentMimeType:
        input.attachmentMimeType !== undefined ? input.attachmentMimeType : existing?.attachmentMimeType || null,
      isPublished,
      isAnnouncement: input.isAnnouncement !== undefined ? Boolean(input.isAnnouncement) : existing?.isAnnouncement || false,
      publishedAt: existing ? (isPublished && !existing.isPublished ? now : existing.publishedAt) : now,
      updatedAt: now,
      adminAuthor: String(input.adminAuthor || 'Admin'),
    };

    if (this.usePg) {
      await this.pool.query(
        `INSERT INTO content_items (
           id, department_id, category, subcategory, title, description, content_body, content_type,
           attachment_file_id, attachment_name, attachment_url, attachment_size_bytes, attachment_mime_type,
           is_published, is_announcement, published_at, updated_at, admin_author
         ) VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14,$15,$16,$17,$18)
         ON CONFLICT (id) DO UPDATE SET
           department_id = EXCLUDED.department_id,
           category = EXCLUDED.category,
           subcategory = EXCLUDED.subcategory,
           title = EXCLUDED.title,
           description = EXCLUDED.description,
           content_body = EXCLUDED.content_body,
           content_type = EXCLUDED.content_type,
           attachment_file_id = EXCLUDED.attachment_file_id,
           attachment_name = EXCLUDED.attachment_name,
           attachment_url = EXCLUDED.attachment_url,
           attachment_size_bytes = EXCLUDED.attachment_size_bytes,
           attachment_mime_type = EXCLUDED.attachment_mime_type,
           is_published = EXCLUDED.is_published,
           is_announcement = EXCLUDED.is_announcement,
           published_at = EXCLUDED.published_at,
           updated_at = EXCLUDED.updated_at,
           admin_author = EXCLUDED.admin_author`,
        [
          item.id,
          item.departmentId,
          item.category,
          item.subcategory,
          item.title,
          item.description,
          item.contentBody,
          item.contentType,
          item.attachmentFileId,
          item.attachmentName,
          item.attachmentUrlOrPath,
          item.attachmentSizeBytes,
          item.attachmentMimeType,
          item.isPublished,
          item.isAnnouncement,
          item.publishedAt,
          item.updatedAt,
          item.adminAuthor,
        ]
      );
    } else {
      const idx = this.state.contentItems.findIndex((c) => c.id === item.id);
      if (idx >= 0) this.state.contentItems[idx] = item;
      else this.state.contentItems.push(item);
      await this._flushFile();
    }
    return item;
  }

  async deleteContent(id) {
    const now = Date.now();
    if (this.usePg) {
      await this.pool.query('DELETE FROM content_items WHERE id = $1', [id]);
      await this.pool.query('INSERT INTO tombstones (entity_type, entity_id, deleted_at) VALUES ($1, $2, $3)', [
        'content',
        id,
        now,
      ]);
      return;
    }
    this.state.contentItems = this.state.contentItems.filter((c) => c.id !== id);
    this.state.tombstones.push({ entityType: 'content', entityId: id, ownerUserId: null, deletedAt: now });
    await this._flushFile();
  }

  // --- REQUESTS ---
  _mapPgRequest(r) {
    return {
      id: r.id,
      clientRequestId: r.client_request_id,
      userId: r.user_id,
      userName: r.user_name,
      userMobile: r.user_mobile,
      departmentId: r.department_id,
      departmentName: r.department_name,
      title: r.title,
      details: r.details,
      status: r.status,
      adminReply: r.admin_reply,
      createdAt: Number(r.created_at),
      updatedAt: Number(r.updated_at),
      repliedAt: r.replied_at ? Number(r.replied_at) : null,
    };
  }

  async createRequest({ clientRequestId, userId, userName, userMobile, departmentId, departmentName, title, details }) {
    const now = Date.now();
    if (clientRequestId) {
      if (this.usePg) {
        const dup = await this.pool.query(
          'SELECT * FROM requests WHERE user_id = $1 AND client_request_id = $2',
          [userId, clientRequestId]
        );
        if (dup.rows.length > 0) return { request: this._mapPgRequest(dup.rows[0]), duplicate: true };
      } else {
        const dup = this.state.requests.find((r) => r.userId === userId && r.clientRequestId === clientRequestId);
        if (dup) return { request: { ...dup }, duplicate: true };
      }
    }

    const reqObj = {
      id: `req_${crypto.randomUUID().replace(/-/g, '').slice(0, 12)}`,
      clientRequestId: clientRequestId || null,
      userId,
      userName: String(userName).trim(),
      userMobile: String(userMobile).trim(),
      departmentId: String(departmentId || ''),
      departmentName: String(departmentName || ''),
      title: String(title || '').trim(),
      details: String(details).trim(),
      status: 'PENDING',
      adminReply: '',
      createdAt: now,
      updatedAt: now,
      repliedAt: null,
    };

    if (this.usePg) {
      await this.pool.query(
        `INSERT INTO requests (
           id, client_request_id, user_id, user_name, user_mobile, department_id, department_name,
           title, details, status, admin_reply, created_at, updated_at, replied_at
         ) VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14)`,
        [
          reqObj.id,
          reqObj.clientRequestId,
          reqObj.userId,
          reqObj.userName,
          reqObj.userMobile,
          reqObj.departmentId,
          reqObj.departmentName,
          reqObj.title,
          reqObj.details,
          reqObj.status,
          reqObj.adminReply,
          reqObj.createdAt,
          reqObj.updatedAt,
          reqObj.repliedAt,
        ]
      );
    } else {
      this.state.requests.push(reqObj);
      await this._flushFile();
    }
    return { request: reqObj, duplicate: false };
  }

  async listRequestsForUser(user) {
    if (this.usePg) {
      const res =
        user.role === 'ADMIN'
          ? await this.pool.query('SELECT * FROM requests ORDER BY created_at DESC')
          : await this.pool.query('SELECT * FROM requests WHERE user_id = $1 ORDER BY created_at DESC', [user.userId]);
      return res.rows.map((r) => this._mapPgRequest(r));
    }

    const list =
      user.role === 'ADMIN'
        ? this.state.requests
        : this.state.requests.filter((r) => r.userId === user.userId);
    return [...list].sort((a, b) => b.createdAt - a.createdAt);
  }

  async getRequestById(id) {
    if (this.usePg) {
      const res = await this.pool.query('SELECT * FROM requests WHERE id = $1', [id]);
      return res.rows[0] ? this._mapPgRequest(res.rows[0]) : null;
    }
    return this.state.requests.find((r) => r.id === id) || null;
  }

  async updateRequestByAdmin(id, { status, adminReply }) {
    const existing = await this.getRequestById(id);
    if (!existing) return null;
    const now = Date.now();
    const updated = {
      ...existing,
      status: status || existing.status,
      adminReply: adminReply !== undefined ? String(adminReply).trim() : existing.adminReply,
      updatedAt: now,
      repliedAt: adminReply !== undefined && String(adminReply).trim() ? now : existing.repliedAt,
    };

    if (this.usePg) {
      await this.pool.query(
        'UPDATE requests SET status = $1, admin_reply = $2, updated_at = $3, replied_at = $4 WHERE id = $5',
        [updated.status, updated.adminReply, updated.updatedAt, updated.repliedAt, id]
      );
    } else {
      const idx = this.state.requests.findIndex((r) => r.id === id);
      if (idx >= 0) this.state.requests[idx] = updated;
      await this._flushFile();
    }
    return updated;
  }

  async deleteRequestByAdmin(id) {
    const existing = await this.getRequestById(id);
    if (!existing) return null;
    const now = Date.now();
    if (this.usePg) {
      await this.pool.query('DELETE FROM requests WHERE id = $1', [id]);
      await this.pool.query(
        'INSERT INTO tombstones (entity_type, entity_id, owner_user_id, deleted_at) VALUES ($1, $2, $3, $4)',
        ['request', id, existing.userId, now]
      );
    } else {
      this.state.requests = this.state.requests.filter((r) => r.id !== id);
      this.state.tombstones.push({
        entityType: 'request',
        entityId: id,
        ownerUserId: existing.userId,
        deletedAt: now,
      });
      await this._flushFile();
    }
    return existing;
  }

  // --- FILES ---
  async saveUploadedFile({ buffer, originalName, mimeType, idempotencyKey }) {
    if (idempotencyKey) {
      if (this.usePg) {
        const dup = await this.pool.query('SELECT * FROM files WHERE idempotency_key = $1', [idempotencyKey]);
        if (dup.rows.length > 0) {
          const f = dup.rows[0];
          return {
            id: f.id,
            originalName: f.original_name,
            mimeType: f.mime_type,
            sizeBytes: Number(f.size_bytes),
            sha256: f.sha256,
            storageProvider: f.storage_provider,
            storagePath: f.storage_path,
            createdAt: Number(f.created_at),
            duplicate: true,
          };
        }
      } else {
        const dup = this.state.files.find((f) => f.idempotencyKey === idempotencyKey);
        if (dup) return { ...dup, duplicate: true };
      }
    }

    const id = `file_${crypto.randomUUID().replace(/-/g, '').slice(0, 14)}`;
    const sha256 = crypto.createHash('sha256').update(buffer).digest('hex');
    const safeName = String(originalName || 'attachment.bin').replace(/[^a-zA-Z0-9._-]/g, '_');
    const now = Date.now();

    let storageProvider = 'local';
    let storagePath = `${id}_${safeName}`;

    // Optional Supabase Storage integration when configured on server
    if (process.env.SUPABASE_URL && process.env.SUPABASE_SERVICE_ROLE_KEY) {
      const bucket = process.env.SUPABASE_BUCKET || 'solar-hub-files';
      const uploadUrl = `${process.env.SUPABASE_URL.replace(/\/$/, '')}/storage/v1/object/${bucket}/${storagePath}`;
      const resp = await fetch(uploadUrl, {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${process.env.SUPABASE_SERVICE_ROLE_KEY}`,
          'Content-Type': mimeType || 'application/octet-stream',
          'x-upsert': 'true',
        },
        body: buffer,
      });
      if (!resp.ok) {
        const txt = await resp.text();
        throw new Error(`Supabase Storage upload failed (${resp.status}): ${txt}`);
      }
      storageProvider = 'supabase';
    } else {
      fs.writeFileSync(path.join(this.uploadsDir, storagePath), buffer);
    }

    const record = {
      id,
      idempotencyKey: idempotencyKey || null,
      originalName: originalName || safeName,
      mimeType: mimeType || 'application/octet-stream',
      sizeBytes: buffer.length,
      sha256,
      storageProvider,
      storagePath,
      createdAt: now,
    };

    if (this.usePg) {
      await this.pool.query(
        `INSERT INTO files (id, idempotency_key, original_name, mime_type, size_bytes, sha256, storage_provider, storage_path, created_at)
         VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9)`,
        [
          record.id,
          record.idempotencyKey,
          record.originalName,
          record.mimeType,
          record.sizeBytes,
          record.sha256,
          record.storageProvider,
          record.storagePath,
          record.createdAt,
        ]
      );
    } else {
      this.state.files.push(record);
      await this._flushFile();
    }

    return { ...record, duplicate: false };
  }

  async getFileRecord(id) {
    if (this.usePg) {
      const res = await this.pool.query('SELECT * FROM files WHERE id = $1', [id]);
      if (!res.rows[0]) return null;
      const f = res.rows[0];
      return {
        id: f.id,
        originalName: f.original_name,
        mimeType: f.mime_type,
        sizeBytes: Number(f.size_bytes),
        sha256: f.sha256,
        storageProvider: f.storage_provider,
        storagePath: f.storage_path,
        createdAt: Number(f.created_at),
      };
    }
    return this.state.files.find((f) => f.id === id) || null;
  }

  async readFileBuffer(record) {
    if (record.storageProvider === 'supabase' && process.env.SUPABASE_URL && process.env.SUPABASE_SERVICE_ROLE_KEY) {
      const bucket = process.env.SUPABASE_BUCKET || 'solar-hub-files';
      const url = `${process.env.SUPABASE_URL.replace(/\/$/, '')}/storage/v1/object/${bucket}/${record.storagePath}`;
      const resp = await fetch(url, {
        headers: { Authorization: `Bearer ${process.env.SUPABASE_SERVICE_ROLE_KEY}` },
      });
      if (!resp.ok) throw new Error('File not found in Supabase Storage');
      return Buffer.from(await resp.arrayBuffer());
    }
    const fullPath = path.join(this.uploadsDir, record.storagePath);
    if (!fs.existsSync(fullPath)) return null;
    return fs.readFileSync(fullPath);
  }

  // --- SYNC ---
  async syncSince(sinceTimestamp, user) {
    const since = Number(sinceTimestamp || 0);
    const isAdmin = user.role === 'ADMIN';
    const [allDepts, allContent, userRequests] = await Promise.all([
      this.listDepartments(),
      this.listContent({ includeDrafts: isAdmin }),
      this.listRequestsForUser(user),
    ]);

    const departments = since > 0 ? allDepts.filter((d) => (d.updatedAt || d.createdAt) > since) : allDepts;
    const contentItems = since > 0 ? allContent.filter((c) => c.updatedAt > since) : allContent;
    const requests = since > 0 ? userRequests.filter((r) => r.updatedAt > since) : userRequests;

    let tombstones = [];
    if (since > 0) {
      if (this.usePg) {
        const res = await this.pool.query('SELECT * FROM tombstones WHERE deleted_at > $1', [since]);
        tombstones = res.rows.map((t) => ({
          entityType: t.entity_type,
          entityId: t.entity_id,
          ownerUserId: t.owner_user_id,
          deletedAt: Number(t.deleted_at),
        }));
      } else {
        tombstones = this.state.tombstones.filter((t) => t.deletedAt > since);
      }
    }

    const deletedDepartments = tombstones.filter((t) => t.entityType === 'department').map((t) => t.entityId);
    const deletedContent = tombstones.filter((t) => t.entityType === 'content').map((t) => t.entityId);
    const deletedRequests = tombstones
      .filter((t) => t.entityType === 'request' && (isAdmin || t.ownerUserId === user.userId))
      .map((t) => t.entityId);

    // Also if non-admin and an existing published item was unpublished, treat as deletedContent for employees
    if (!isAdmin && since > 0) {
      const allIncludingDrafts = await this.listContent({ includeDrafts: true });
      for (const c of allIncludingDrafts) {
        if (!c.isPublished && c.updatedAt > since) {
          deletedContent.push(c.id);
        }
      }
    }

    return {
      serverTime: Date.now(),
      fullSync: since === 0,
      departments,
      contentItems,
      requests,
      deletedDepartments,
      deletedContent,
      deletedRequests,
    };
  }
}

module.exports = { PersistentStore, INITIAL_DEPARTMENTS };
