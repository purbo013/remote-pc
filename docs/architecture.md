# Architecture

```text
                    Wi-Fi LAN
                       │
             ┌─────────┴─────────┐
             │                   │
             ▼                   ▼
      ANDROID CLIENT          WINDOWS PC
      Remote UI               Node.js server
      Touch / keyboard        Screen JPEG
      WebView                 Win32 mouse/keyboard
                              Cursor / PHP / MySQL / Apache
```

## Roles

- **Android** is a client: remote display, input, and LAN browser.
- **Windows** is the server and the only place engines run.

## Processes on the PC

- HTTP API on `0.0.0.0:8765` for health, pairing, projects, and a loopback console
- WebSocket on `/ws` for auth, screen frames, and input
- Native Win32 via `koffi` for cursor and key events
- JPEG capture (~1280px wide, configurable quality and FPS)

## Data stored on the PC

- `server/config.json` — port, host, remoteControl, quality, FPS, project list
- `server/data/tokens.json` — SHA-256 hashes of device tokens (not the tokens themselves)

## Out of scope for MVP

File explorer, terminal, clipboard sync, audio, multi-PC, VPN/internet, H.264/WebRTC.
