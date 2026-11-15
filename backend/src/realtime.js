const { WebSocketServer, WebSocket } = require('ws');
const { verifyToken } = require('./auth');

class RealtimeHub {
  constructor() {
    this.wss = null;
    this.clients = new Set();
    this.heartbeatTimer = null;
  }

  attach(server) {
    this.wss = new WebSocketServer({ server, path: '/ws' });

    this.wss.on('connection', (ws, req) => {
      try {
        const url = new URL(req.url, 'http://localhost');
        const token =
          url.searchParams.get('token') ||
          (req.headers.authorization || '').replace(/^Bearer\s+/i, '');
        const user = verifyToken(token);
        if (!user) {
          ws.close(4001, 'Unauthorized');
          return;
        }

        const client = {
          ws,
          userId: user.userId,
          role: user.role,
          isAlive: true,
        };
        this.clients.add(client);

        ws.on('pong', () => {
          client.isAlive = true;
        });

        ws.on('message', (raw) => {
          try {
            const msg = JSON.parse(raw.toString());
            if (msg && msg.type === 'PING') {
              if (ws.readyState === WebSocket.OPEN) {
                ws.send(JSON.stringify({ type: 'PONG', serverTime: Date.now() }));
              }
            }
          } catch {
            // ignore malformed ping frames
          }
        });

        ws.on('close', () => {
          this.clients.delete(client);
        });

        ws.on('error', () => {
          this.clients.delete(client);
        });

        ws.send(
          JSON.stringify({
            type: 'CONNECTED',
            userId: user.userId,
            role: user.role,
            serverTime: Date.now(),
          })
        );
      } catch {
        ws.close(4000, 'Invalid connection');
      }
    });

    this.heartbeatTimer = setInterval(() => {
      for (const client of this.clients) {
        if (!client.isAlive) {
          client.ws.terminate();
          this.clients.delete(client);
          continue;
        }
        client.isAlive = false;
        if (client.ws.readyState === WebSocket.OPEN) {
          client.ws.ping();
        }
      }
    }, 25000);
  }

  close() {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer);
      this.heartbeatTimer = null;
    }
    if (this.wss) {
      for (const client of this.clients) {
        try {
          client.ws.close();
        } catch {}
      }
      this.clients.clear();
      this.wss.close();
      this.wss = null;
    }
  }

  _send(client, payloadObj) {
    if (client.ws.readyState === WebSocket.OPEN) {
      client.ws.send(JSON.stringify(payloadObj));
    }
  }

  broadcastPublic(type, data) {
    const msg = { type, data, serverTime: Date.now() };
    for (const client of this.clients) {
      this._send(client, msg);
    }
  }

  broadcastToAdmins(type, data) {
    const msg = { type, data, serverTime: Date.now() };
    for (const client of this.clients) {
      if (client.role === 'ADMIN') {
        this._send(client, msg);
      }
    }
  }

  broadcastToUserAndAdmins(ownerUserId, type, data) {
    const msg = { type, data, serverTime: Date.now() };
    for (const client of this.clients) {
      if (client.role === 'ADMIN' || client.userId === ownerUserId) {
        this._send(client, msg);
      }
    }
  }
}

module.exports = { RealtimeHub };
