# Setup — Android

Minimum: Android 8.0 (API 26). Same Wi-Fi as the PC.

## Build

Open `android/` in Android Studio and run on a device, or:

```bat
cd android
set JAVA_HOME=C:\Java\jdk-17
gradlew.bat assembleDebug
```

APK: `android/app/build/outputs/apk/debug/app-debug.apk`

`android/local.properties` must point at your Android SDK.

## First connection

1. Start the Windows server and open the PC console.
2. On the phone, enter the **PC LAN IP** and port `8765`.
3. Tap **PAIR** and type the 6-digit code.
4. Tap **CONNECT**. Status should become ONLINE.

Do not type `localhost` as the host.

## Modes

- **REMOTE** — PC screen, mouse gestures, shortcut toolbar, special keys, text field for long typing
- **LOCALHOST** — WebView at `http://<pc-ip>/`
- **PROJECTS** — URLs from the server config plus manual entries
- **SETTINGS** — IP, port, device name, auto-reconnect, landscape, quality hint

## Gestures

- Single tap — left click
- Double tap — double click
- Long press — right click
- One-finger drag after a press — mouse drag
- Two-finger vertical move — scroll

Modifier chips (CTRL / SHIFT / ALT) stay latched until you tap them again, then the next key is sent as a combo (example: CTRL then S → Ctrl+S).

## Errors

If connect fails, the app lists likely causes: PC offline, wrong IP, firewall, server not running, or different Wi-Fi.

If Wi-Fi drops and auto-reconnect is on, the app retries without asking for a new pairing code.
