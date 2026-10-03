# Setup — Windows

## 1. Install Node.js 20+

Then:

```bat
cd c:\www\me\remote-code\server
npm install
npm run dev
```

Default bind: `0.0.0.0:8765` so phones on the LAN can connect. This is **not** an invitation to port-forward the port on the router.

## 2. Confirm the PC LAN IP

The console shows private IPv4 addresses (example `192.168.1.100`). The phone must use that IP.

## 3. Windows Firewall (Private only)

Allow inbound TCP 8765 on the **Private** profile. Do not open the **Public** profile by default.

Example (run PowerShell **as Administrator** only if you choose to add a rule):

```powershell
New-NetFirewallRule -DisplayName "Remote Coding Workstation" -Direction Inbound -Protocol TCP -LocalPort 8765 -Action Allow -Profile Private
```

Do not disable the firewall entirely.

## 4. Let Apache / Laragon / Vite accept LAN clients

The phone cannot use `localhost` (that points at the phone).

- Apache/XAMPP: listen on `0.0.0.0:80` (or your site port)
- Laragon: enable network / bind all interfaces
- Vite: `npm run dev -- --host` so it listens on the LAN
- Then open `http://192.168.1.100/` or `http://192.168.1.100:5173/` from the phone

## 5. Config

Edit `server/config.json`:

```json
{
  "port": 8765,
  "host": "0.0.0.0",
  "remoteControl": true,
  "screenQuality": 70,
  "maxFps": 15,
  "projects": [
    { "name": "Nangkis", "url": "http://{pcIp}/nangkis" }
  ]
}
```

`{pcIp}` is replaced with the detected LAN address.

## 6. Pairing

The pairing code is only shown on `http://127.0.0.1:8765/console`. After a successful pair, the hashed token is stored in `server/data/tokens.json`.

Keep `ALLOW REMOTE CONTROL` off when you only want status/screen and no input.
