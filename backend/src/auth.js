const crypto = require('crypto');

const TOKEN_TTL_MS = 30 * 24 * 60 * 60 * 1000; // 30 days

function getTokenSecret() {
  if (!process.env.TOKEN_SECRET) {
    if (!global.__solarHubRuntimeSecret) {
      global.__solarHubRuntimeSecret = crypto.randomBytes(32).toString('hex');
      console.warn('[AUTH] TOKEN_SECRET env not set; using ephemeral runtime secret (set TOKEN_SECRET in production so tokens survive restarts).');
    }
    return global.__solarHubRuntimeSecret;
  }
  return process.env.TOKEN_SECRET;
}

function getAdminPassword() {
  return process.env.ADMIN_PASSWORD || 'SolarAdmin@2026';
}

function base64UrlEncode(input) {
  return Buffer.from(input)
    .toString('base64')
    .replace(/=/g, '')
    .replace(/\+/g, '-')
    .replace(/\//g, '_');
}

function base64UrlDecode(str) {
  let padded = str.replace(/-/g, '+').replace(/_/g, '/');
  while (padded.length % 4 !== 0) padded += '=';
  return Buffer.from(padded, 'base64').toString('utf8');
}

function signToken(payload) {
  const header = base64UrlEncode(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const body = base64UrlEncode(
    JSON.stringify({
      ...payload,
      iat: Date.now(),
      exp: Date.now() + TOKEN_TTL_MS,
    })
  );
  const sig = base64UrlEncode(
    crypto.createHmac('sha256', getTokenSecret()).update(`${header}.${body}`).digest()
  );
  return `${header}.${body}.${sig}`;
}

function verifyToken(token) {
  if (!token || typeof token !== 'string') return null;
  const parts = token.split('.');
  if (parts.length !== 3) return null;
  const [header, body, sig] = parts;
  const expectedSig = base64UrlEncode(
    crypto.createHmac('sha256', getTokenSecret()).update(`${header}.${body}`).digest()
  );
  const sigBuf = Buffer.from(sig);
  const expBuf = Buffer.from(expectedSig);
  if (sigBuf.length !== expBuf.length || !crypto.timingSafeEqual(sigBuf, expBuf)) {
    return null;
  }
  try {
    const decoded = JSON.parse(base64UrlDecode(body));
    if (!decoded || typeof decoded !== 'object') return null;
    if (decoded.exp && Date.now() > decoded.exp) return null;
    return decoded;
  } catch {
    return null;
  }
}

function verifyAdminPassword(inputPassword) {
  if (!inputPassword || typeof inputPassword !== 'string') return false;
  const expected = getAdminPassword();
  const h1 = crypto.createHash('sha256').update(inputPassword.trim()).digest();
  const h2 = crypto.createHash('sha256').update(expected).digest();
  return crypto.timingSafeEqual(h1, h2);
}

function normalizeMobile(mobile) {
  const digits = String(mobile || '').replace(/\D/g, '');
  if (digits.length > 10 && digits.startsWith('91')) {
    return digits.slice(-10);
  }
  return digits;
}

function authMiddleware(req, res, next) {
  const header = req.headers.authorization || '';
  const queryToken = req.query && req.query.token ? String(req.query.token) : null;
  const rawToken = header.startsWith('Bearer ') ? header.slice(7).trim() : queryToken;
  const decoded = verifyToken(rawToken);
  if (!decoded) {
    return res.status(401).json({ error: 'Authentication required. Please log in.' });
  }
  req.user = decoded;
  next();
}

function requireAdmin(req, res, next) {
  if (!req.user || req.user.role !== 'ADMIN') {
    return res.status(403).json({ error: 'Forbidden: Admin privileges required.' });
  }
  next();
}

module.exports = {
  signToken,
  verifyToken,
  verifyAdminPassword,
  normalizeMobile,
  authMiddleware,
  requireAdmin,
};
