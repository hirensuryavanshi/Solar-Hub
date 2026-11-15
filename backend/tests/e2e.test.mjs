import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { createRequire } from 'node:module';

const require = createRequire(import.meta.url);
const WebSocket = require('ws');
const { createSolarHubServer } = require('../src/server');

async function jsonReq(baseUrl, urlPath, { method = 'GET', token, body } = {}) {
  const headers = {};
  if (token) headers['Authorization'] = `Bearer ${token}`;
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  const res = await fetch(`${baseUrl}${urlPath}`, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });
  const data = await res.json().catch(() => ({}));
  return { status: res.status, data };
}

function connectWs(baseUrl, token) {
  return new Promise((resolve, reject) => {
    const wsUrl = baseUrl.replace(/^http/, 'ws') + `/ws?token=${encodeURIComponent(token)}`;
    const ws = new WebSocket(wsUrl);
    const messages = [];
    ws.on('message', (buf) => {
      const msg = JSON.parse(buf.toString());
      messages.push(msg);
      if (msg.type === 'CONNECTED') {
        resolve({ ws, messages });
      }
    });
    ws.on('error', reject);
  });
}

function waitForEvent(messages, predicate, timeoutMs = 4000) {
  const existing = messages.find(predicate);
  if (existing) return Promise.resolve(existing);
  return new Promise((resolve, reject) => {
    const started = Date.now();
    const timer = setInterval(() => {
      const found = messages.find(predicate);
      if (found) {
        clearInterval(timer);
        resolve(found);
      } else if (Date.now() - started > timeoutMs) {
        clearInterval(timer);
        reject(new Error('Timed out waiting for WebSocket event'));
      }
    }, 25);
  });
}

test('SOLAR HUB End-to-End Real-Time Sync, Persistence, Security & Privacy (Tests 1-8)', async () => {
  const tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'solarhub-e2e-'));
  const dataDir = path.join(tmpDir, 'data');
  const uploadsDir = path.join(tmpDir, 'uploads');

  process.env.ADMIN_PASSWORD = 'TestStrongAdminPassword!99';
  process.env.TOKEN_SECRET = 'test-token-secret-1234567890abcdef';

  let srv = await createSolarHubServer({ dataDir, uploadsDir });
  let port = await srv.start(0);
  let baseUrl = `http://127.0.0.1:${port}`;

  // Login User A, User B, User C (Name + Mobile, no OTP) and Admin (Backend verified)
  const loginA = await jsonReq(baseUrl, '/api/auth/login', {
    method: 'POST',
    body: { name: 'User A', mobile: '9876543210' },
  });
  assert.equal(loginA.status, 200);
  const tokenA = loginA.data.token;
  const userAId = loginA.data.user.id;

  const loginB = await jsonReq(baseUrl, '/api/auth/login', {
    method: 'POST',
    body: { name: 'User B', mobile: '9876500002' },
  });
  assert.equal(loginB.status, 200);
  const tokenB = loginB.data.token;

  const loginC = await jsonReq(baseUrl, '/api/auth/login', {
    method: 'POST',
    body: { name: 'User C', mobile: '9876500003' },
  });
  assert.equal(loginC.status, 200);
  const tokenC = loginC.data.token;

  // Wrong admin password rejected
  const badAdmin = await jsonReq(baseUrl, '/api/auth/admin/login', {
    method: 'POST',
    body: { password: 'wrong' },
  });
  assert.equal(badAdmin.status, 401);

  // Correct admin password verified on backend
  const loginAdmin = await jsonReq(baseUrl, '/api/auth/admin/login', {
    method: 'POST',
    body: { password: 'TestStrongAdminPassword!99' },
  });
  assert.equal(loginAdmin.status, 200);
  const tokenAdmin = loginAdmin.data.token;

  // Connect real-time WebSockets for Admin, User A, User B, User C
  const wsAdmin = await connectWs(baseUrl, tokenAdmin);
  const wsA = await connectWs(baseUrl, tokenA);
  const wsB = await connectWs(baseUrl, tokenB);
  const wsC = await connectWs(baseUrl, tokenC);

  // TEST 1 & TEST 4: User A sends a request -> Admin sees it immediately, User B NEVER sees it
  const clientReqId = 'req-client-uuid-001';
  const reqRes = await jsonReq(baseUrl, '/api/requests', {
    method: 'POST',
    token: tokenA,
    body: {
      clientRequestId: clientReqId,
      departmentId: 'dept_solar_module',
      departmentName: 'Solar Module',
      title: 'Need Lamination SOP',
      details: 'Please upload the latest lamination temperature chart.',
    },
  });
  assert.equal(reqRes.status, 201);
  const createdReqId = reqRes.data.request.id;

  // Duplicate submission with same clientRequestId must be deduplicated
  const dupRes = await jsonReq(baseUrl, '/api/requests', {
    method: 'POST',
    token: tokenA,
    body: {
      clientRequestId: clientReqId,
      departmentId: 'dept_solar_module',
      title: 'Need Lamination SOP',
      details: 'Please upload the latest lamination temperature chart.',
    },
  });
  assert.equal(dupRes.status, 200);
  assert.equal(dupRes.data.duplicate, true);
  assert.equal(dupRes.data.request.id, createdReqId);

  // Admin WebSocket receives REQUEST_CREATED in real time
  const adminReqEvent = await waitForEvent(
    wsAdmin.messages,
    (m) => m.type === 'REQUEST_CREATED' && m.data.id === createdReqId
  );
  assert.equal(adminReqEvent.data.userName, 'User A');
  assert.equal(adminReqEvent.data.userMobile, '9876543210');

  // TEST 4: User B must NOT receive User A's private request via WS or REST, and cannot modify/delete it
  await new Promise((r) => setTimeout(r, 100));
  assert.equal(
    wsB.messages.some((m) => m.data && m.data.id === createdReqId),
    false
  );
  const listB = await jsonReq(baseUrl, '/api/requests', { token: tokenB });
  assert.equal(listB.data.requests.length, 0);

  const tamperPatch = await jsonReq(baseUrl, `/api/requests/${createdReqId}`, {
    method: 'PATCH',
    token: tokenB,
    body: { status: 'ACCEPTED' },
  });
  assert.equal(tamperPatch.status, 403);

  const tamperDel = await jsonReq(baseUrl, `/api/requests/${createdReqId}`, {
    method: 'DELETE',
    token: tokenB,
  });
  assert.equal(tamperDel.status, 403);

  // Admin accepts & replies -> User A immediately sees update, User B does not
  const replyRes = await jsonReq(baseUrl, `/api/requests/${createdReqId}`, {
    method: 'PATCH',
    token: tokenAdmin,
    body: { status: 'ACCEPTED', adminReply: 'Uploading updated chart today.' },
  });
  assert.equal(replyRes.status, 200);
  const userAReplyEv = await waitForEvent(
    wsA.messages,
    (m) => m.type === 'REQUEST_UPDATED' && m.data.id === createdReqId
  );
  assert.equal(userAReplyEv.data.status, 'ACCEPTED');
  assert.equal(userAReplyEv.data.adminReply, 'Uploading updated chart today.');

  // TEST 2 & TEST 7: Admin publishes an announcement -> User A, User B, User C all receive it immediately
  const annRes = await jsonReq(baseUrl, '/api/content', {
    method: 'POST',
    token: tokenAdmin,
    body: {
      departmentId: 'dept_safety',
      contentType: 'NOTICE',
      title: 'Plant Safety Drill Tomorrow',
      description: 'Mandatory assembly at 10:00 AM',
      contentBody: 'All shifts participate in safety drill.',
      isAnnouncement: true,
      isPublished: true,
    },
  });
  assert.equal(annRes.status, 200);
  const annId = annRes.data.contentItem.id;

  await Promise.all([
    waitForEvent(wsA.messages, (m) => m.type === 'CONTENT_UPDATED' && m.data.id === annId),
    waitForEvent(wsB.messages, (m) => m.type === 'CONTENT_UPDATED' && m.data.id === annId),
    waitForEvent(wsC.messages, (m) => m.type === 'CONTENT_UPDATED' && m.data.id === annId),
  ]);

  // TEST 3: Admin uploads a real PDF file -> All users can download exact PDF bytes
  const pdfBytes = Buffer.from('%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\n%%EOF\n');
  const form = new FormData();
  form.append('file', new Blob([pdfBytes], { type: 'application/pdf' }), 'Lamination_SOP.pdf');
  const uploadRes = await fetch(`${baseUrl}/api/files/upload`, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${tokenAdmin}`,
      'X-Idempotency-Key': 'pdf-upload-key-1',
    },
    body: form,
  });
  assert.equal(uploadRes.status, 200);
  const uploadJson = await uploadRes.json();
  assert.equal(uploadJson.file.originalName, 'Lamination_SOP.pdf');

  // Publish content item referencing uploaded PDF
  const pdfPostRes = await jsonReq(baseUrl, '/api/content', {
    method: 'POST',
    token: tokenAdmin,
    body: {
      departmentId: 'dept_solar_module',
      contentType: 'PDF',
      title: 'Lamination SOP Rev 4',
      description: 'Official lamination parameters',
      attachmentFileId: uploadJson.file.id,
      attachmentName: uploadJson.file.originalName,
      attachmentUrlOrPath: uploadJson.file.url,
      attachmentSizeBytes: uploadJson.file.sizeBytes,
      attachmentMimeType: uploadJson.file.mimeType,
      isPublished: true,
    },
  });
  assert.equal(pdfPostRes.status, 200);

  // User B downloads the PDF and receives identical bytes
  const dlB = await fetch(`${baseUrl}${uploadJson.file.url}`, {
    headers: { Authorization: `Bearer ${tokenB}` },
  });
  assert.equal(dlB.status, 200);
  const dlBuf = Buffer.from(await dlB.arrayBuffer());
  assert.deepEqual(dlBuf, pdfBytes);

  // TEST 8: User C disconnects -> Admin publishes new item while User C is offline -> User C reconnects and catches up via /api/sync?since=
  const syncBeforeDisconnect = await jsonReq(baseUrl, '/api/sync?since=0', { token: tokenC });
  const lastSyncTs = syncBeforeDisconnect.data.serverTime;
  wsC.ws.close();
  await new Promise((r) => setTimeout(r, 50));

  const offlinePost = await jsonReq(baseUrl, '/api/content', {
    method: 'POST',
    token: tokenAdmin,
    body: {
      departmentId: 'dept_renewable_energy',
      contentType: 'TEXT',
      title: 'Grid Sync Update While Offline',
      description: 'Missed update test',
      isPublished: true,
    },
  });
  assert.equal(offlinePost.status, 200);

  const catchUpC = await jsonReq(baseUrl, `/api/sync?since=${lastSyncTs}`, { token: tokenC });
  assert.equal(catchUpC.status, 200);
  assert.ok(
    catchUpC.data.contentItems.some((c) => c.id === offlinePost.data.contentItem.id),
    'Offline client must receive missed updates on reconnect sync'
  );

  // Close WebSockets and restart server to verify TEST 5 & TEST 6 (persistence across restart / APK reinstall)
  wsAdmin.ws.close();
  wsA.ws.close();
  wsB.ws.close();
  await srv.stop();

  // Restart backend server pointing to same persistent dataDir & uploadsDir
  srv = await createSolarHubServer({ dataDir, uploadsDir });
  port = await srv.start(0);
  baseUrl = `http://127.0.0.1:${port}`;

  // TEST 5: Admin logs in again after restart -> previous content, files, requests remain
  const adminRelogin = await jsonReq(baseUrl, '/api/auth/admin/login', {
    method: 'POST',
    body: { password: 'TestStrongAdminPassword!99' },
  });
  assert.equal(adminRelogin.status, 200);
  const adminSync = await jsonReq(baseUrl, '/api/sync?since=0', { token: adminRelogin.data.token });
  assert.ok(adminSync.data.contentItems.some((c) => c.id === annId));
  assert.ok(adminSync.data.requests.some((r) => r.id === createdReqId));

  // TEST 6: User A "reinstalls APK" and logs in again with Name + Mobile -> recognized as same user & sees all previous content + request history!
  const userAReinstallLogin = await jsonReq(baseUrl, '/api/auth/login', {
    method: 'POST',
    body: { name: 'User A', mobile: '+91 9876543210' },
  });
  assert.equal(userAReinstallLogin.status, 200);
  assert.equal(userAReinstallLogin.data.user.id, userAId);

  const userASync = await jsonReq(baseUrl, '/api/sync?since=0', {
    token: userAReinstallLogin.data.token,
  });
  assert.ok(userASync.data.contentItems.some((c) => c.id === annId));
  assert.ok(userASync.data.requests.some((r) => r.id === createdReqId));

  // Uploaded PDF file still downloads cleanly after server restart
  const dlAfterRestart = await fetch(`${baseUrl}${uploadJson.file.url}`, {
    headers: { Authorization: `Bearer ${userAReinstallLogin.data.token}` },
  });
  assert.equal(dlAfterRestart.status, 200);
  assert.deepEqual(Buffer.from(await dlAfterRestart.arrayBuffer()), pdfBytes);

  await srv.stop();
  fs.rmSync(tmpDir, { recursive: true, force: true });
});
