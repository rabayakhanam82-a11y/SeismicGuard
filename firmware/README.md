# SeismicGuard Omega - ESP32 Production Firmware & Hardware Guide

**Class 7 English Version (EV) | BIS Science Projects 2026**  
**Project Lead & System Architect:** Abdullah Karim  
**Contributors:** Mahir, Ahmad, Behan, Zarif  

---

## 1. Quick Start - Flashing the ESP32

1. Open **Arduino IDE (v2.0 or higher)**.
2. Open `/firmware/SeismicGuard_ESP32/SeismicGuard_ESP32.ino`.
3. In `Tools -> Board`, select **ESP32 Dev Module** (or *DOIT ESP32 DEVKIT V1*).
4. In `Tools -> Port`, select your ESP32's COM port (e.g., `COM3`, `COM4`).
5. Update lines **39** and **42** in the code with your Wi-Fi credentials:
   ```cpp
   const char* WIFI_SSID     = "Your_WiFi_Name";
   const char* WIFI_PASSWORD = "Your_WiFi_Password";
   ```
6. Click **Upload** (the `->` button).
7. If the IDE shows `Connecting........_____.....`, hold down the **BOOT** button on your ESP32 board for 2 seconds until the percentage counter appears.

---

## 2. Hardware Wiring (Buzzer Only - Zero Red LED)

> **Important Note:** There is **NO Red LED** in this circuit. Only the SW-420 motion sensor and BC547 Acoustic Buzzer are connected.

| Component | Pin on Component | ESP32 Connection | Notes |
| :--- | :--- | :--- | :--- |
| **SW-420 Sensor** | `VCC` | `3V3` Pin | 3.3V power |
| **SW-420 Sensor** | `GND` | `GND` Pin | Common Ground |
| **SW-420 Sensor** | `DO` | **`GPIO 18`** | Digital Output (Interrupt) |
| **BC547 Transistor** | Base (Pin 2) | **`GPIO 23`** | Via **1kΩ** resistor |
| **BC547 Transistor** | Collector (Pin 1) | Buzzer `(-)` Terminal | Switched ground |
| **BC547 Transistor** | Emitter (Pin 3) | `GND` Pin | Ground plane |
| **9042 Buzzer** | Positive `(+)` Terminal | `3V3` Pin | 3.3V or 5V rail |

---

## 3. Hand-Shake Sensitivity Calibration

The firmware contains a sliding accumulation algorithm specifically tuned for a **deliberate, firm hand shake**:

```cpp
const int SHAKE_PULSE_THRESHOLD       = 4;     // Requires 4 rapid bounces
const unsigned long SHAKE_WINDOW_MS   = 250;   // In 250 milliseconds
const unsigned long ALARM_COOLDOWN_MS = 2500;  // 2.5 second cooldown between alarms
```

- **Gentle Touch / Table Taps:** Generate only 1 or 2 pulses, which are filtered out and ignored.
- **Firm Hand Shake:** Generates 4 to 8 rapid bounces within 250ms, triggering the buzzer siren and pushing an instant alert to Firebase Realtime Database.
- **Tuning the Blue Potentiometer Screw:**
  - Turn the brass screw **counter-clockwise** if the onboard DO LED stays on continuously while still.
  - Turn it slightly **clockwise** if it requires too violent of a shake to turn on.

---

## 4. Cloud & App Integration

- **Firebase Realtime Database Host:**  
  `https://seismic-guard-default-rtdb.asia-southeast1.firebasedatabase.app`
- **Telemetry Path:** `/telemetry.json` (Real-time hardware status & heartbeat)
- **Alerts Path:** `/alerts.json` (Logged emergency events)
- **Android App:**  
  The sideloadable APK is built and ready in `/apks/SeismicGuard-v1.8-phone-ready.apk`.
