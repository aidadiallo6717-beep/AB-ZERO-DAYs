const WebSocket = require('ws');
const mysql = require('mysql2');
const url = require('url');

const PORT = 8080;
const DB = {
    host: 'localhost',
    user: 'ghost_user',
    password: 'VotreMotDePasseIci123!',
    database: 'ghost_os'
};

const pool = mysql.createPool(DB).promise();
const clients = new Map();

const wss = new WebSocket.Server({ port: PORT });
console.log(`WebSocket started on port ${PORT}`);

wss.on('connection', async (ws, req) => {
    const params = new URLSearchParams(url.parse(req.url).query);
    const apiKey = params.get('api_key');
    const deviceId = params.get('device_id');
    const isPanel = params.get('panel') === 'true';
    
    const [users] = await pool.query('SELECT id FROM users WHERE api_key = ?', [apiKey]);
    if (users.length === 0) {
        ws.close(1008, 'Invalid API key');
        return;
    }
    
    const userId = users[0].id;
    
    if (!clients.has(userId)) {
        clients.set(userId, { devices: new Map(), panels: new Set() });
    }
    
    const userClients = clients.get(userId);
    
    if (isPanel) {
        userClients.panels.add(ws);
        const online = Array.from(userClients.devices.keys());
        ws.send(JSON.stringify({ type: 'devices_online', devices: online }));
    } else {
        userClients.devices.set(deviceId, ws);
        await pool.query(
            'UPDATE devices SET is_online = 1, last_seen = NOW() WHERE device_id = ? AND user_id = ?',
            [deviceId, userId]
        );
        
        userClients.panels.forEach(p => {
            if (p.readyState === WebSocket.OPEN) {
                p.send(JSON.stringify({ type: 'device_online', deviceId }));
            }
        });
    }
    
    ws.on('message', async (data) => {
        try {
            const msg = JSON.parse(data);
            switch (msg.type) {
                case 'command':
                    const device = userClients.devices.get(msg.deviceId);
                    if (device) device.send(JSON.stringify(msg));
                    break;
                case 'screenshot':
                    userClients.panels.forEach(p => {
                        if (p.readyState === WebSocket.OPEN) p.send(JSON.stringify(msg));
                    });
                    break;
                case 'ping':
                    ws.send(JSON.stringify({ type: 'pong' }));
                    break;
            }
        } catch (err) {
            console.error('Error:', err);
        }
    });
    
    ws.on('close', async () => {
        if (isPanel) {
            userClients.panels.delete(ws);
        } else {
            userClients.devices.delete(deviceId);
            await pool.query(
                'UPDATE devices SET is_online = 0 WHERE device_id = ? AND user_id = ?',
                [deviceId, userId]
            );
            userClients.panels.forEach(p => {
                if (p.readyState === WebSocket.OPEN) {
                    p.send(JSON.stringify({ type: 'device_offline', deviceId }));
                }
            });
        }
    });
});
