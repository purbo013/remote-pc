# Setup — Internet (relay)

Internet mode lets the phone control the PC **without the same Wi‑Fi**. The PC opens an **outbound** connection to a relay server; the phone connects to that relay. You do **not** need to port-forward TCP 8765 on your home router.

## Architecture

```text
Android  ──HTTPS/WSS──►  Relay (VPS)  ◄──WSS──  Windows PC agent
                              │
                         bridges HTTP + WebSocket
                              │
                         localhost:8765 on PC
```

## 1. Run the relay (VPS or cloud)

On a server with a public IP or domain:

```bash
cd relay
npm install
PUBLIC_URL=https://relay.yourdomain.com npm start
```

Default port: **8780**. Put **nginx/Caddy** in front for TLS (`wss://` / `https://`).

Health check:

```bash
curl https://relay.yourdomain.com/health
```

## 2. Enable relay on the Windows PC

Edit `server/config.json`:

```json
"relay": {
  "enabled": true,
  "url": "https://relay.yourdomain.com",
  "nodeId": "my-pc",
  "secret": ""
}
```

- **`nodeId`**: unique name (letters, numbers, `_`, `-`). Shown on the PC console for the phone.
- **`secret`**: leave empty once; the server generates a random secret and saves it. **Do not share** the secret.
- **`url`**: public base URL of the relay (no `/agent` suffix).

Restart the Windows server:

```bat
cd server
npm run dev
```

On `http://127.0.0.1:8765/console`, confirm **Internet (relay)** shows **ONLINE** and note **Relay URL** + **Node ID**.

## 3. Configure the Android app

1. Home → **Internet**
2. **Relay URL**: e.g. `https://relay.yourdomain.com`
3. **Node ID**: same as `relay.nodeId` on the PC (e.g. `my-pc`)
4. **PAIR** with the 6-digit code from the PC console (pairing still loopback-only on the PC)
5. **CONNECT**

## Security notes

- Use **HTTPS/WSS** on the relay in production.
- Pairing codes are only on the PC console (`127.0.0.1`).
- Device tokens are still required for WebSocket access.
- The relay sees encrypted TLS traffic if you use TLS; it still **relays** screen and input — treat the relay as semi-trusted or self-host it.
- Do not expose the PC console or `relay.secret` publicly.

## Without your own VPS

You do not need `relay/` if another path gives the phone a route to the PC:

- **Phone hotspot** — connect the PC to the hotspot, use Android **LAN** mode with the PC’s IP on that network.
- **[Tailscale](https://tailscale.com/)** or **ZeroTier** — install on PC and phone, use **LAN** mode with the PC’s VPN IP (often the simplest “from mobile data” setup).

Use the built-in **relay** when you want the app’s **Internet** mode and a small outbound-only bridge on a VPS.

## LOCALHOST / Projects over Internet

Project URLs that use `{pcIp}` point at the PC’s **LAN** address and usually **do not work** from mobile data. Use **REMOTE** for desktop control, or add project URLs that are reachable from the internet (deployed staging site, tunnelled dev URL, etc.).

## Local testing (same machine)

```bash
# Terminal 1
cd relay && npm start

# Terminal 2 — set relay.enabled true, url http://127.0.0.1:8780
cd server && npm run dev
```

On the phone (emulator or device), Relay URL `http://<PC-LAN-IP>:8780` only works if the relay port is reachable; for real internet use a VPS.
