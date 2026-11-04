# SeismicGuard Omega

**IoT Early Earthquake & Tremor Alarm System**  
**Bangladesh International School (BIS) Science Exhibition 2026**  
**Academic Section:** Class 7 English Version (EV)  
**Project Lead:** Abdullah Karim 

**App and Code:** Abdullah Karim

**Hardware Team:** Mahir,Behan

**Presentation:** Ahmed, zarif

---

## Direct App Download

The compiled Android application package is ready for direct installation on phones and tablets.

- **Direct Download Link:** [Download SeismicGuard v1.8 APK](https://release-assets.githubusercontent.com/github-production-release-asset/1441410787/88c9b609-02ab-4f03-a7e5-8130ce793553?sp=r&sv=2018-11-09&sr=b&spr=https&se=2026-11-04T16%3A08%3A50Z&rscd=attachment%3B+filename%3DSeismicGuard-v1.8-phone-ready.apk&rsct=application%2Fvnd.android.package-archive&skoid=96c2d410-5711-43a1-aedd-ab1947aa7ab0&sktid=398a6654-997b-47e9-b12b-9515b896b4de&skt=2026-11-04T15%3A07%3A56Z&ske=2026-11-04T16%3A08%3A50Z&sks=b&skv=2018-11-09&sig=iSnO9VanEioqMax8YktQN2xkwVJFGuszfZrL56cwEIA%3D&jwt=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmVsZWFzZS1hc3NldHMuZ2l0aHVidXNlcmNvbnRlbnQuY29tIiwia2V5Ijoia2V5MSIsImV4cCI6MTc5MzgwNjY3NywibmJmIjoxNzkzODA0ODc3LCJwYXRoIjoicmVsZWFzZWFzc2V0cHJvZHVjdGlvbi5ibG9iLmNvcmUud2luZG93cy5uZXQifQ.erwXoUDOruvHA-l23M0e_mrtyy2aZqjTsX4aJurGaPE&response-content-disposition=attachment%3B%20filename%3DSeismicGuard-v1.8-phone-ready.apk&response-content-type=application%2Fvnd.android.package-archive )
- **Repository Location:** `apks/SeismicGuard-v1.8-phone-ready.apk`
- **Application ID:** `com.aistudio.seismicguard.omega`
- **Version:** `1.8` (Build `9`)
- **Compatibility:** Android 7.0 (Nougat, API 24) through Android 15 (API 36)
- **Architecture:** Universal (ARM64-v8a, ARMeabi-v7a, x86_64)

### How to Install the APK

1. Click the download link above or navigate to the releases section and download `SeismicGuard-v1.8-phone-ready.apk`.
2. Open the downloaded file on your Android device.
3. If prompted by Android Security, toggle **Allow from this source** (or **Install unknown apps**).
4. Tap **Install** and launch **SeismicGuard**.
5. Grant Notification permissions when requested so that background seismic alerts can sound in real time.

---

## System Overview

SeismicGuard Omega is an integrated hardware and software seismic monitoring system. It combines an on-site ESP32 microcontroller with a calibrated SW-420 high-sensitivity vibration sensor, a BC547-driven acoustic emergency siren, and real-time cloud synchronization via Firebase Realtime Database. 

When ground tremors or strong mechanical vibrations are detected, the system immediately sounds a local acoustic alarm and broadcasts low-latency telemetry to all connected Android mobile stations within 15 milliseconds.

### Core Capabilities

- **Real-Time Tremor Detection:** Uses hardware interrupts (`FALLING` edge) on GPIO 18 to capture rapid mechanical oscillations.
- **Accidental Bump Filtering:** A sliding window algorithm requires 4 rapid bounces within 250 milliseconds, preventing false alarms from light surface touches or desk bumps while responding to a firm, deliberate shake.
- **Audible Siren System:** BC547 NPN transistor powers a high-output acoustic buzzer on GPIO 23 with swept-frequency audio alerts (2000 Hz to 3200 Hz).
- **Sub-15ms Cloud Dispatch:** Transmits emergency packets and telemetry over HTTPS directly to Firebase Realtime Database.
- **Full-Screen Emergency Override:** The companion Android application triggers a persistent, high-visibility emergency alert screen with tactile vibration patterns and override audio.
- **Dynamic Weather & Meteorological Engine:** Live weather monitoring with swipe-down pull-to-refresh and automatic update on every app launch.

---

## Hardware Configuration

There is no red LED in this circuit. The system uses a dedicated acoustic buzzer driven by an NPN transistor for maximum audible warning volume.

### Wiring Reference Table

| Component | Pin / Terminal | ESP32 Pin | Wiring Description |
| :--- | :--- | :--- | :--- |
| **SW-420 Vibration Sensor** | `VCC` | `3V3` | 3.3V DC power rail |
| **SW-420 Vibration Sensor** | `GND` | `GND` | Common ground rail |
| **SW-420 Vibration Sensor** | `DO` | `GPIO 18` | Digital interrupt input (`INPUT_PULLUP`) |
| **BC547 NPN Transistor** | Base (Pin 2) | `GPIO 23` | Connected through a 1k Ohm base resistor |
| **BC547 NPN Transistor** | Collector (Pin 1) | Buzzer `(-)` | Switched ground terminal for buzzer |
| **BC547 NPN Transistor** | Emitter (Pin 3) | `GND` | Common ground rail |
| **9042 Acoustic Buzzer** | Positive `(+)` | `3V3` | 3.3V power bus |

### Sensor Calibration

The SW-420 sensor module features an onboard blue potentiometer with a brass adjustment screw:

1. Connect the ESP32 to power and observe the sensor status LED.
2. While resting still on a flat surface, the status LED should remain OFF.
3. If the LED stays ON continuously while still, turn the brass screw counter-clockwise until the LED turns OFF.
4. Give the sensor a firm, deliberate shake. The LED will flash rapidly and the buzzer will trigger the emergency sequence.

---

## ESP32 Firmware

The complete firmware is organized in the `firmware/` directory and requires no external third-party libraries.

### Firmware Files

- **`firmware/SeismicGuard_ESP32.ino`** — Primary Arduino sketch file.
- **`firmware/ESP32_CODE_COPY_PASTE.txt`** — Plain-text version for quick copying without opening subfolders.
- **`firmware/INSTRUCTIONS_ARDUINO_IDE.txt`** — Detailed flashing manual.

### Required Setup Steps

1. Open Arduino IDE (version 2.0 or higher).
2. Open `firmware/SeismicGuard_ESP32.ino` or paste the contents into a new sketch.
3. In `Tools -> Board`, select **ESP32 Dev Module** (or **DOIT ESP32 DEVKIT V1**).
4. Update the Wi-Fi credentials in lines 38 and 41:
   ```cpp
   const char* WIFI_SSID     = "YOUR_WIFI_NAME_HERE";
   const char* WIFI_PASSWORD = "YOUR_WIFI_PASSWORD_HERE";
   ```
5. Ensure the Firebase Host is configured:
   ```cpp
   const char* FIREBASE_HOST = "https://seismic-guard-default-rtdb.asia-southeast1.firebasedatabase.app";
   ```
6. Connect the ESP32 via USB and select the corresponding COM port.
7. Click **Upload**. If the output displays `Connecting........_____`, hold the **BOOT** button on the ESP32 for 2 seconds.

---

## Android Architecture

The mobile application is built using modern Android Jetpack Compose and Clean Architecture principles:

- **Jetpack Compose & Material 3:** Adaptive layout supporting phones, foldables, and tablets with dynamic theming.
- **State Management:** Kotlin Coroutines, `StateFlow`, and `collectAsStateWithLifecycle` for non-blocking reactive UI updates.
- **Network & Cloud Client:** Firebase Realtime Database SDK paired with OkHttp and Retrofit for weather forecasting data.
- **Full-Screen Intent:** `EmergencyAlertActivity` with system-level lock screen display privileges (`setShowWhenLocked`, `setTurnScreenOn`).
- **Telemetry Listener:** Active snapshot listener attached to `/telemetry` and `/alerts` endpoints.

---

## Building from Source

To build the Android project locally:

```bash
git clone https://github.com/your-username/seismicguard.git
cd seismicguard
gradle assembleDebug
```

The resulting debug APK will be located in `app/build/outputs/apk/debug/app-debug.apk`.

---

## Team Credits

- **Project Lead & Architecture:** Abdullah Karim (Class 7 English Version)
- **Circuit Assembly & Diagnostics:** Mahir, Ahmad
- **Testing & Presentation:** Behan, Zarif
- **Institution:** Bangladesh International School (BIS) Science Exhibition 2026
