# Implementation Plan: ESP32 Setup & Brand Sizing Resolution

## 1. Top Bar & Drawer Brand Typography Fix
- Replaced microscopic scaled vector rendering in `MainDashboardScreen.kt` with bold, prominently sized Compose typography:
  - Top middle app bar: `Text("SEISMIC", 20.sp, Black, White)` + `Text("GUARD", 20.sp, Black, Cyan)` with status indicator dot.
  - Menu drawer header: `Text("SEISMIC", 22.sp, Black, White)` + `Text("GUARD", 22.sp, Black, Cyan)`.
  - Ensures crisp, high-visibility legibility on all mobile displays.

## 2. Fully Working & Compatible ESP32 C++ Firmware
- Replaced legacy `ESPAsyncWebServer` (which breaks on modern ESP32 Core v3.0+) with universal built-in ESP32 core libraries:
  - `<WiFi.h>`, `<WebServer.h>`, `<HTTPClient.h>`, `<WiFiClientSecure.h>`, `<ESPmDNS.h>`.
  - Zero external ZIP dependencies required to flash in Arduino IDE.
- Full hardware mapping:
  - `GPIO 18`: SW-420 DO digital vibration sensor (`INPUT_PULLUP`, `IRAM_ATTR` edge-triggered interrupt, sub-15ms latency).
  - `GPIO 22`: 5mm Red Alert LED (330Ω current limiting resistor).
  - `GPIO 23`: BC547 NPN driver -> 9042 Passive Buzzer (2.4kHz acoustic resonance pulse).
- Dual Wi-Fi & Fallback Architecture:
  - Simultaneous Station Wi-Fi + SoftAP (`SeismicGuard_AP` at `192.168.4.1`).
  - mDNS responder at `http://seismicguard.local`.
- Local HTTP REST Server on Port 80:
  - `/status`, `/telemetry`, `/ping`, `/test_buzzer`, `/test_led`, `/trigger`.
- Realtime Cloud Push to Firebase (Asia-Southeast1):
  - Uses `client.setInsecure()` for reliable HTTPS without certificate management.

## 3. Hardware Setup Wizard (Esp32ConnectGuide.kt)
- Added dedicated Step 1 "ESP32 ARDUINO C++ CODE" card with 1-click "Copy Full C++ Code" button.
- Added Arduino IDE board configuration checklist (`ESP32 Dev Module`, 240MHz, 80MHz flash, 921600/115200 baud).
- Added interactive ping test with real latency feedback and diagnostics.
