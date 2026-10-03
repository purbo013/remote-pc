# Android Remote Coding Workstation

Use an Android phone as a remote coding terminal. The Windows PC remains the machine that runs Cursor, PHP, MySQL, Apache/Laragon, Node.js, and localhost.

The phone only:

- shows the PC screen
- sends mouse and keyboard input
- opens LAN URLs in a browser (never `localhost` on the phone)

## Requirements

- Windows PC on the same Wi-Fi as the phone
- Node.js 20+
- Android 8.0+ (API 26)
- Android Studio or JDK 17 to build the app

## Run the Windows server

```bat
cd server
npm install
npm run dev
```

The server listens on TCP **8765**. A console opens at `http://127.0.0.1:8765/console` (this PC only) and shows:

- LAN IP
- port
- pairing code
- connected devices
- remote-control toggle

Health check:

```bat
curl.exe http://127.0.0.1:8765/health
```

## Run the Android app

1. Open `android/` in Android Studio, or build:

```bat
cd android
set JAVA_HOME=C:\Java\jdk-17
gradlew.bat assembleDebug
```

2. Install `android/app/build/outputs/apk/debug/app-debug.apk` on the phone.
3. Enter the PC LAN IP (example: `192.168.1.100`) and port `8765`.
4. Pair with the 6-digit code from the PC console.
5. Connect, then use **REMOTE** or **LOCALHOST**.

## Typical workflow

1. Keep Cursor and your PHP/Vue stack running on the PC.
2. Connect the phone on the same Wi-Fi.
3. Control Cursor from **REMOTE** (mouse, keyboard, Ctrl+S / Ctrl+C / Ctrl+V).
4. Open the site from **LOCALHOST** / **PROJECTS** as `http://PC_IP/...` — not `http://localhost/...`.

## Security (MVP)

- Pairing code + hashed token (token is never stored in plaintext on the server)
- Unauthorized clients are rejected
- Console and pairing-code UI are loopback-only
- LAN only: do not port-forward 8765 to the internet
- Windows Firewall: allow TCP 8765 on the **Private** profile only

See [docs/](docs/) for architecture, protocol, and setup notes.
