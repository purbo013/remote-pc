# Android Remote Coding Workstation

## 1. Tujuan Project

Buat sebuah sistem yang memungkinkan saya menggunakan HP Android sebagai perangkat untuk melakukan coding dengan bantuan PC sebagai mesin utama.

Saya memiliki PC Windows yang digunakan untuk:

* Cursor IDE
* PHP
* Vue.js
* MySQL
* Apache/XAMPP/Laragon
* Git
* Node.js
* project web lokal
* localhost

Saya ingin tetap menggunakan semua engine dan resource PC, tetapi bisa mengontrolnya dari HP.

### Tujuan utama

Dari HP saya harus bisa:

1. Melihat layar PC.
2. Menggerakkan mouse PC menggunakan touchscreen HP.
3. Klik kiri/kanan.
4. Scroll.
5. Mengetik menggunakan keyboard HP.
6. Mengirim shortcut keyboard seperti:

   * Ctrl
   * Shift
   * Alt
   * Tab
   * Esc
   * Ctrl+C
   * Ctrl+V
   * Ctrl+S
   * Ctrl+Z
   * Ctrl+Shift+P
   * F1-F12
7. Menggunakan Cursor di PC dari HP.
8. Membuka browser localhost dari HP.
9. Melihat hasil aplikasi PHP/Vue yang sedang dijalankan di PC.
10. Menjalankan project tetap di PC.
11. Tidak menjalankan PHP/MySQL/Node.js secara langsung di Android.

---

# 2. Konsep Arsitektur

Gunakan arsitektur:

```text
                    Wi-Fi LAN
                       │
             ┌─────────┴─────────┐
             │                   │
             ▼                   ▼
      ANDROID CLIENT          WINDOWS PC
      ┌──────────────┐        ┌────────────────────┐
      │              │        │                    │
      │ Remote UI    │◄──────►│ Remote Server      │
      │              │        │                    │
      │ Touch input  │        │ Screen capture     │
      │ Keyboard     │        │ Mouse control      │
      │ Browser      │        │ Keyboard control   │
      │              │        │                    │
      └──────────────┘        │ Cursor             │
                              │ PHP                │
                              │ MySQL              │
                              │ Apache             │
                              │ Node.js            │
                              │ localhost          │
                              └────────────────────┘
```

Android adalah CLIENT.

Windows adalah SERVER.

Semua pekerjaan berat dilakukan oleh Windows PC.

---

# 3. Prinsip Penting

Jangan membuat Android menjalankan:

* PHP
* MySQL
* Apache
* Node.js project
* Cursor

Semua itu tetap berjalan di PC.

Android hanya menjadi:

* remote control
* remote display
* browser
* keyboard/mouse interface

---

# 4. Teknologi

Gunakan teknologi yang sederhana, stabil, dan mudah dikembangkan.

## Android

Gunakan:

* Kotlin
* Jetpack Compose
* Android SDK modern
* WebSocket untuk komunikasi realtime
* HTTP untuk API biasa

Minimum Android:

Android 8.0+ jika memungkinkan.

---

# 5. Windows Server

Buat aplikasi server Windows.

Untuk server gunakan:

* Node.js
* TypeScript
* WebSocket
* HTTP API

Namun untuk fitur mouse/keyboard/screen capture, gunakan native Windows API jika diperlukan.

Jangan mengandalkan browser saja untuk mengontrol Windows.

Server harus dapat:

1. Capture screen.
2. Mengirim screen ke Android.
3. Menerima koordinat touch.
4. Mengubah touch menjadi mouse movement.
5. Mengirim mouse click.
6. Mengirim scroll.
7. Menerima keyboard input.
8. Mengirim keyboard event ke Windows.

---

# 6. Struktur Project

Gunakan struktur:

```text
remote-coding-workstation/
│
├── android/
│   └── Android application
│
├── server/
│   ├── src/
│   │   ├── index.ts
│   │   ├── websocket/
│   │   ├── api/
│   │   ├── screen/
│   │   ├── mouse/
│   │   ├── keyboard/
│   │   ├── security/
│   │   └── config/
│   │
│   ├── package.json
│   └── tsconfig.json
│
├── docs/
│   ├── architecture.md
│   ├── protocol.md
│   ├── setup-windows.md
│   └── setup-android.md
│
├── README.md
└── .gitignore
```

---

# 7. Fitur MVP

Jangan langsung membuat semuanya.

Buat MVP terlebih dahulu.

## MVP Phase 1

Android dapat:

* menemukan IP PC secara manual
* connect ke PC
* menampilkan status connection
* melihat screenshot PC
* refresh screenshot

Contoh:

```text
PC IP:
[ 192.168.1.100 ]

[ CONNECT ]

Status:
● Connected

[ SCREEN ]
```

---

# 8. Phase 2 - Remote Mouse

Tambahkan:

## Touchscreen

Touch HP diterjemahkan menjadi:

```text
Touch DOWN
Touch MOVE
Touch UP
```

Kemudian server mengubahnya menjadi:

```text
Mouse move
Left click
Right click
```

Gesture:

### Single tap

Left click.

### Double tap

Double click.

### Long press

Right click.

### One finger drag

Mouse drag.

### Two finger vertical movement

Scroll.

Pastikan koordinat touchscreen Android dikonversi secara proporsional ke resolusi layar Windows.

Contoh:

```text
Android screen:
1080 x 2400

PC:
1920 x 1080
```

Mapping harus tetap akurat.

---

# 9. Phase 3 - Keyboard

Buat keyboard overlay.

Minimal tersedia:

```text
ESC
TAB
CTRL
ALT
SHIFT
WIN
BACKSPACE
ENTER
SPACE
ARROW UP
ARROW DOWN
ARROW LEFT
ARROW RIGHT
HOME
END
PAGE UP
PAGE DOWN
DELETE
F1
F2
F3
F4
F5
F6
F7
F8
F9
F10
F11
F12
```

Keyboard Android normal tetap digunakan untuk mengetik teks.

Untuk shortcut, buat tombol modifier.

Contoh:

```text
[CTRL] [SHIFT] [ALT] [TAB]

[ESC] [~] [1] [2] [3] [4] [5] [6]

[Q] [W] [E] [R] [T] [Y]

[A] [S] [D] [F] [G] [H]

[Z] [X] [C] [V] [B] [N]

[CTRL] [ALT] [SPACE] [ENTER]
```

Modifier harus dapat dikunci.

Contoh:

Tap:

```text
CTRL
```

kemudian:

```text
S
```

menghasilkan:

```text
Ctrl + S
```

---

# 10. Phase 4 - Remote Screen

Jangan mengirim screenshot penuh dengan ukuran besar terus menerus jika performanya buruk.

Buat sistem streaming layar yang efisien.

Prioritas:

1. latency rendah
2. penggunaan bandwidth rendah
3. kualitas cukup untuk membaca kode

Untuk MVP boleh menggunakan JPEG/WebP screenshot.

Kemudian arsitektur dapat ditingkatkan ke:

* H.264
* WebRTC
* hardware encoding

Jika diperlukan.

Jangan melakukan optimasi kompleks sebelum MVP berjalan.

---

# 11. Mode Coding

Buat Android memiliki mode:

```text
REMOTE
BROWSER
```

## REMOTE

Menampilkan remote desktop.

Contoh:

```text
┌─────────────────────────────┐
│ PC Remote                   │
├─────────────────────────────┤
│                             │
│                             │
│       SCREEN PC             │
│                             │
│                             │
│                             │
├─────────────────────────────┤
│ CTRL SHIFT ALT TAB          │
│                             │
│ ESC   WIN   SPACE   ENTER   │
└─────────────────────────────┘
```

---

# 12. Browser Mode

Browser mode digunakan untuk melihat localhost.

Misalnya PC mempunyai project:

```text
http://localhost/nangkis
```

Android harus dapat membuka:

```text
http://192.168.1.100/nangkis
```

Jangan menggunakan:

```text
localhost
```

di Android.

Karena `localhost` pada Android menunjuk ke Android itu sendiri.

---

# 13. Localhost Access

Server web di PC harus dikonfigurasi agar dapat menerima koneksi LAN.

Contoh:

```text
PC:
192.168.1.100
```

Apache:

```text
0.0.0.0:80
```

Kemudian HP:

```text
http://192.168.1.100/
```

atau:

```text
http://192.168.1.100/nangkis
```

Jika menggunakan port tertentu:

```text
http://192.168.1.100:8080/
```

Server harus mendokumentasikan konfigurasi firewall Windows.

---

# 14. Project Discovery

Tambahkan fitur untuk mendeteksi project localhost.

Contoh:

```text
Projects

┌─────────────────────────────┐
│ Nangkis                     │
│ http://192.168.1.100/...    │
├─────────────────────────────┤
│ DTSEN                       │
│ http://192.168.1.100/...    │
├─────────────────────────────┤
│ PSKS                        │
│ http://192.168.1.100/...    │
└─────────────────────────────┘
```

Untuk MVP project URL dapat dikonfigurasi manual.

Jangan membuat autodetection yang terlalu kompleks terlebih dahulu.

---

# 15. PC Information

Android dapat menampilkan:

```text
PC ONLINE

IP:
192.168.1.100

CPU:
Ryzen 5 5600

RAM:
32 GB

GPU:
...

CPU Usage:
...

RAM Usage:
...

Uptime:
...
```

Fitur monitoring boleh dibuat setelah remote desktop stabil.

---

# 16. Security

Ini SANGAT penting.

Jangan membuat server remote control tanpa authentication.

Minimal gunakan:

```text
PAIRING CODE
```

Contoh:

PC menampilkan:

```text
REMOTE SERVER

Pairing code:

739421
```

Android:

```text
Enter pairing code

[ 739421 ]

[ PAIR ]
```

Setelah pairing berhasil, Android mendapat authentication token.

Token digunakan untuk koneksi berikutnya.

---

# 17. LAN Only Untuk MVP

Versi pertama hanya boleh berjalan di:

```text
Local Area Network
```

Contoh:

```text
HP
192.168.1.20

PC
192.168.1.100
```

Jangan membuka port remote server langsung ke internet.

Jangan membuat:

```text
0.0.0.0 + port forwarding router
```

sebagai konfigurasi default.

---

# 18. Security Rules

Server harus:

* authentication
* pairing
* token
* timeout
* connection logging
* reject unauthorized clients

Tambahkan:

```text
ALLOW REMOTE CONTROL: ON/OFF
```

Jika OFF:

Android masih dapat melihat status PC tetapi tidak dapat mengontrol mouse/keyboard.

---

# 19. Server UI

Buat aplikasi server sederhana untuk Windows.

Contoh:

```text
┌────────────────────────────────────┐
│ Remote Coding Workstation          │
├────────────────────────────────────┤
│ Status: ● Running                  │
│                                    │
│ IP Address                         │
│ 192.168.1.100                      │
│                                    │
│ Port                               │
│ 8765                               │
│                                    │
│ Pairing Code                       │
│ 739421                             │
│                                    │
│ Connected Devices                  │
│                                    │
│ Android Phone                     │
│ ● Connected                        │
│                                    │
│ [ STOP SERVER ]                    │
└────────────────────────────────────┘
```

---

# 20. WebSocket Protocol

Gunakan WebSocket untuk komunikasi realtime.

Contoh message:

```json
{
  "type": "mouse_move",
  "x": 0.523,
  "y": 0.417
}
```

Gunakan koordinat normalized:

```text
0.0 - 1.0
```

Jangan mengirim koordinat pixel Android secara langsung.

---

## Mouse click

```json
{
  "type": "mouse_click",
  "button": "left"
}
```

## Right click

```json
{
  "type": "mouse_click",
  "button": "right"
}
```

## Scroll

```json
{
  "type": "scroll",
  "delta": -5
}
```

## Keyboard

```json
{
  "type": "key",
  "key": "CTRL"
}
```

atau:

```json
{
  "type": "key_combo",
  "keys": ["CTRL", "S"]
}
```

---

# 21. Text Input

Untuk mengetik kode panjang, jangan mengirim setiap karakter sebagai mouse/keyboard event jika tidak diperlukan.

Buat command:

```json
{
  "type": "text_input",
  "text": "const app = createApp(App);"
}
```

Windows server kemudian memasukkan text tersebut ke active application.

Ini akan membuat typing dari HP jauh lebih nyaman.

---

# 22. Coding Shortcuts

Buat preset shortcut:

```text
Ctrl + S
Ctrl + C
Ctrl + V
Ctrl + X
Ctrl + Z
Ctrl + Y
Ctrl + A
Ctrl + F
Ctrl + H
Ctrl + P
Ctrl + Shift + P
Ctrl + B
Ctrl + `
Alt + Tab
F5
F11
```

Tambahkan custom shortcut di masa depan.

---

# 23. Cursor Support

Sistem harus bekerja dengan Cursor IDE tanpa integrasi khusus.

Artinya:

Android
↓
Remote control
↓
Windows
↓
Cursor

Cursor tidak perlu dimodifikasi.

Jangan membuat Cursor extension pada MVP.

---

# 24. Browser Workflow

Contoh workflow yang saya inginkan:

### Step 1

Buka Cursor di PC.

### Step 2

Buka project:

```text
D:\project\nangkis
```

### Step 3

Jalankan:

```text
php
```

atau Apache/XAMPP/Laragon.

### Step 4

Dari HP buka:

```text
Browser Mode
```

### Step 5

Pilih:

```text
Nangkis
```

### Step 6

HP membuka:

```text
http://192.168.1.100/nangkis
```

### Step 7

Jika ada error:

kembali ke Remote Mode.

### Step 8

Kontrol Cursor.

### Step 9

Edit code.

### Step 10

Refresh Browser Mode.

Dengan demikian:

```text
CODING
  ↓
PC ENGINE
  ↓
LOCALHOST
  ↓
HP BROWSER
```

---

# 25. UX Android

Buat interface modern dan sederhana.

Tema:

* dark mode
* minimal
* modern
* tidak terlalu banyak tombol

Home:

```text
┌─────────────────────────────┐
│ Remote Coding               │
├─────────────────────────────┤
│                             │
│ PC                         │
│ ● ONLINE                    │
│                             │
│ 192.168.1.100               │
│                             │
│ [ CONNECT ]                 │
│                             │
├─────────────────────────────┤
│                             │
│ [ 🖥 REMOTE ]               │
│                             │
│ [ 🌐 LOCALHOST ]            │
│                             │
│ [ 📁 PROJECTS ]             │
│                             │
│ [ ⚙ SETTINGS ]             │
│                             │
└─────────────────────────────┘
```

---

# 26. Remote Screen UX

Remote screen harus dapat:

* fullscreen
* zoom
* fit screen
* landscape mode
* portrait mode
* hide keyboard
* show keyboard
* show shortcut toolbar

Toolbar:

```text
[CTRL] [SHIFT] [ALT] [TAB] [ESC] [F5] [CTRL+S]
```

---

# 27. Landscape Mode

Remote coding sebaiknya otomatis menggunakan landscape.

Ketika user memilih:

```text
REMOTE
```

Android dapat menawarkan:

```text
Switch to landscape?
```

atau otomatis landscape jika memungkinkan.

---

# 28. Performance

Target MVP:

* LAN latency serendah mungkin
* mouse terasa realtime
* screen tidak terlalu patah-patah
* keyboard tidak terasa delay

Jangan mengejar kualitas video tinggi.

Untuk coding, lebih penting:

```text
LATENCY
```

daripada:

```text
4K QUALITY
```

Target awal:

```text
720p
10-30 FPS
```

Jika screen static, jangan mengirim frame baru terus-menerus.

---

# 29. Reconnection

Jika Wi-Fi terputus:

```text
Connection lost
```

Android mencoba reconnect otomatis.

Contoh:

```text
Retry:
1
2
3
...
```

Jangan membuat user harus mengatur ulang pairing setiap kali koneksi terputus.

---

# 30. PC Sleep

Tambahkan status:

```text
PC ONLINE
PC OFFLINE
PC SLEEP
```

Untuk MVP, cukup:

```text
ONLINE
OFFLINE
```

Wake-on-LAN dapat ditambahkan kemudian.

---

# 31. File Explorer

Buat sebagai fase berikutnya.

Tujuan:

Android dapat melihat:

```text
D:\project
```

Contoh:

```text
project/
├── app/
├── assets/
├── public/
├── index.php
├── package.json
└── README.md
```

User dapat:

* open file
* download file
* upload file
* create folder
* rename
* delete

Tetapi fitur file management harus memiliki permission dan confirmation.

---

# 32. Terminal

Fase berikutnya:

Android:

```text
┌─────────────────────────────┐
│ TERMINAL                    │
├─────────────────────────────┤
│ C:\project> npm run dev     │
│                             │
│ VITE ready...               │
│                             │
│ localhost:5173              │
│                             │
│ C:\project>                 │
└─────────────────────────────┘
```

Command tetap dieksekusi di PC.

Contoh:

```text
git status
npm run dev
npm install
php artisan ...
composer install
```

Jangan menjalankan command arbitrary tanpa authentication.

---

# 33. Clipboard

Tambahkan sinkronisasi clipboard.

Contoh:

```text
HP Copy
   ↓
PC Clipboard

PC Copy
   ↓
HP Clipboard
```

Tetapi clipboard synchronization harus dapat dinonaktifkan di Settings.

---

# 34. Audio

Tidak perlu untuk MVP.

Jangan streaming audio terlebih dahulu.

---

# 35. Multiple PC

Arsitektur harus memungkinkan beberapa PC.

Contoh:

```text
My Devices

● PC Rumah
● PC Kantor
● Laptop
```

MVP cukup satu PC.

---

# 36. Internet Access

Jangan implementasikan akses internet pada versi pertama.

Setelah LAN version stabil, baru desain opsi:

```text
VPN
Tailscale
WireGuard
```

Jika membutuhkan remote dari luar rumah.

Jangan membuat port forwarding publik sebagai solusi default.

---

# 37. Logging

Server harus mencatat:

```text
[12:31:02] Server started
[12:31:10] Client connected
[12:31:11] Authentication successful
[12:32:20] Remote control enabled
[13:00:01] Client disconnected
```

Jangan mencatat isi password atau pairing secret.

---

# 38. Error Handling

Android harus memberikan error yang jelas.

Contoh:

```text
Unable to connect.

Possible causes:

- PC offline
- Wrong IP address
- Firewall blocking connection
- Server not running
- Phone and PC are not on the same network
```

Jangan hanya menampilkan:

```text
Connection Error
```

---

# 39. Configuration

PC server config:

```json
{
  "port": 8765,
  "host": "0.0.0.0",
  "remoteControl": true,
  "screenQuality": 70,
  "maxFps": 20
}
```

Android config:

```text
PC IP
PC Port
Device Name
Auto Reconnect
Landscape Mode
Screen Quality
FPS
```

---

# 40. Development Order

WAJIB mengikuti urutan berikut.

## Phase 1

Buat Windows server.

Fitur:

* start server
* HTTP health endpoint
* WebSocket
* pairing
* authentication

## Phase 2

Buat Android client.

Fitur:

* input IP
* connect
* authentication
* connection status

## Phase 3

Remote screenshot.

## Phase 4

Mouse.

## Phase 5

Keyboard.

## Phase 6

Text input.

## Phase 7

Browser / localhost.

## Phase 8

Clipboard.

## Phase 9

Monitoring.

## Phase 10

File explorer.

## Phase 11

Terminal.

## Phase 12

Performance optimization.

## Phase 13

Optional internet/VPN support.

---

# 41. Important Development Rule

Jangan membuat semua fitur sekaligus.

Setiap phase harus menghasilkan aplikasi yang dapat dijalankan.

Contoh:

```text
Phase 1
Server jalan
↓
Test

Phase 2
Android connect
↓
Test

Phase 3
Screen tampil
↓
Test

Phase 4
Mouse bekerja
↓
Test
```

Jika suatu phase gagal, perbaiki terlebih dahulu sebelum lanjut.

---

# 42. Testing

Buat test untuk:

## Connection

* server online
* server offline
* wrong IP
* wrong port
* invalid token

## Mouse

* move
* left click
* right click
* double click
* scroll
* drag

## Keyboard

* normal typing
* Ctrl+C
* Ctrl+V
* Ctrl+S
* Ctrl+Z
* Alt+Tab
* function keys

## Screen

* portrait
* landscape
* different PC resolutions
* scaling

## Network

* Wi-Fi stable
* Wi-Fi disconnected
* reconnect

---

# 43. Windows Firewall

Buat dokumentasi otomatis untuk membuka port server.

Contoh:

```text
TCP 8765
```

Tetapi rule firewall harus dibatasi sebisa mungkin pada:

```text
Private Network
```

Jangan membuka:

```text
Public Network
```

secara default.

---

# 44. Do Not Do

Jangan:

* membuat remote desktop dengan akses anonymous
* membuka server ke internet
* menyimpan password plaintext
* mengirim authentication token tanpa encryption jika sudah melewati LAN
* menjalankan command administrator secara default
* mematikan Windows security
* meminta user mematikan firewall seluruhnya
* menggunakan port forwarding sebagai default
* membuat aplikasi terlalu kompleks pada MVP

---

# 45. Future Architecture

Jika MVP berhasil, arsitektur dapat dikembangkan menjadi:

```text
                    INTERNET
                       │
                    VPN/Tunnel
                       │
                       ▼
                 Android Client
                       │
                       ▼
                  PC Server
                  ┌────┴────┐
                  │         │
                Cursor   Localhost
                  │         │
                PHP       Vue
                  │         │
                MySQL     Apache
```

---

# 46. Definition of Done - MVP

MVP dianggap berhasil jika saya bisa melakukan hal berikut:

1. PC Windows menjalankan Remote Coding Server.
2. HP Android berada pada Wi-Fi yang sama.
3. Android connect ke PC.
4. Android melakukan pairing.
5. Android melihat layar PC.
6. Saya dapat menggerakkan mouse PC dari HP.
7. Saya dapat klik Cursor.
8. Saya dapat mengetik menggunakan keyboard HP.
9. Saya dapat menggunakan Ctrl+S.
10. Saya dapat menggunakan Ctrl+C/Ctrl+V.
11. Saya dapat menjalankan project PHP di PC.
12. Saya dapat membuka hasil localhost dari HP.
13. Saya dapat berpindah antara Cursor dan browser dengan nyaman.
14. Jika Wi-Fi terputus, aplikasi reconnect.
15. Tidak ada port remote yang dibuka ke internet.

---

# 47. Instruksi Untuk Cursor

Anda adalah lead developer untuk project ini.

Jangan hanya memberikan contoh kode.

Implementasikan project secara nyata dan bertahap.

Sebelum coding:

1. Periksa environment.
2. Periksa apakah Node.js tersedia.
3. Periksa apakah Android Studio/SDK tersedia.
4. Periksa apakah Java/JDK tersedia.
5. Tentukan versi yang kompatibel.
6. Buat struktur project.

Kemudian implementasikan:

```text
Phase 1
→ Phase 2
→ Phase 3
...
```

Setiap phase harus:

1. Membuat file yang diperlukan.
2. Menjelaskan cara menjalankan.
3. Menjelaskan cara testing.
4. Memperbaiki error jika ditemukan.
5. Tidak merusak phase sebelumnya.

Jangan mengimplementasikan fitur yang belum diperlukan.

Prioritaskan:

```text
SIMPLE
STABLE
LOW LATENCY
SECURE
EASY TO DEBUG
```

---

# 48. First Task

Mulai dari Phase 1.

Jangan langsung membuat remote desktop.

Pertama buat:

```text
Windows Remote Server
+
Android Client
+
Pairing
+
WebSocket
+
Connection Status
```

Setelah itu pastikan:

```text
Android
   ↓
Wi-Fi
   ↓
Windows Server
```

benar-benar dapat berkomunikasi.

Setelah berhasil, lanjutkan ke screen capture.

---

# 49. Final Goal

Pada akhirnya saya ingin workflow seperti ini:

Saya duduk di luar rumah atau tempat lain dalam jaringan Wi-Fi.

Saya mengambil HP.

Saya membuka:

```text
Remote Coding
```

Kemudian:

```text
CONNECT PC
```

Saya melihat desktop PC.

Saya membuka Cursor.

Saya coding menggunakan HP.

Project PHP/Vue tetap berjalan di PC.

Kemudian saya memilih:

```text
LOCALHOST
```

dan melihat hasil aplikasi.

Workflow yang diinginkan:

```text
                 ANDROID
                    │
       ┌────────────┴────────────┐
       │                         │
       ▼                         ▼
 Remote Desktop              Browser
       │                         │
       │                         │
       └──────────┬──────────────┘
                  │
                 Wi-Fi
                  │
                  ▼
             WINDOWS PC
                  │
       ┌──────────┼──────────┐
       │          │          │
     Cursor      PHP       MySQL
       │          │          │
       │        Apache       │
       │          │          │
       └──────────┼──────────┘
                  │
               localhost
```

Fokus utama project adalah:

**"Use Android as a remote coding terminal while Windows PC remains the actual development machine."**
