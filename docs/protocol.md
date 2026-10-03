# Protocol

Base URL: `http://<pc-lan-ip>:8765`  
WebSocket: `ws://<pc-lan-ip>:8765/ws`

Coordinates are normalized `0.0`–`1.0` of the Windows virtual screen. Do not send Android pixel coordinates.

## HTTP

| Method | Path | Auth | Notes |
| --- | --- | --- | --- |
| GET | `/health` | no | Server liveness |
| GET | `/api/status` | no | ONLINE, LAN IPs, remote-control flag |
| POST | `/api/pair` | pairing code | Body `{ "code":"739421", "deviceName":"Android Phone" }` → `{ "token":"..." }` |
| GET | `/api/projects` | Bearer token | Resolves `{pcIp}` in configured URLs |
| GET | `/console` | loopback only | Operator UI |
| GET | `/api/console/state` | loopback only | Includes pairing code |
| POST | `/api/console/remote-control` | loopback only | `{ "enabled": true }` |
| POST | `/api/console/regenerate-code` | loopback only | New 6-digit code |

HTTP auth: `Authorization: Bearer <token>`.

## WebSocket

First client message must be:

```json
{ "type": "auth", "token": "<token>" }
```

Unauthorized sockets are closed with code `4003`.

### Client → server

```json
{ "type": "mouse_move", "x": 0.523, "y": 0.417 }
{ "type": "mouse_click", "button": "left", "double": false }
{ "type": "mouse_down", "button": "left" }
{ "type": "mouse_up", "button": "left" }
{ "type": "scroll", "delta": -5 }
{ "type": "scroll", "delta": -3, "deltaX": 2 }
{ "type": "key", "key": "CTRL" }
{ "type": "key_combo", "keys": ["CTRL", "S"] }
{ "type": "text_input", "text": "const app = createApp(App);" }
{ "type": "ping" }
```

If `remoteControl` is false, input messages are rejected. Screen frames may still be sent.

### Server → client

```json
{ "type": "hello", "remoteControl": true, "maxFps": 15 }
{ "type": "screen_frame", "mime": "image/jpeg", "width": 1280, "height": 720, "data": "<base64>" }
{ "type": "error", "message": "Unauthorized" }
{ "type": "pong" }
```

Unchanged JPEG hashes are not resent.

## Reconnect

The token stays valid. The phone reconnects without pairing again.
