# SeismicGuard Phone-Ready APKs

### Version 1.8 (Build 9) - Science Exhibition Final
- **Application ID:** `com.aistudio.seismicguard.omega`
- **Version Name:** `1.8`
- **Version Code:** `9`
- **Target Android Version:** Android 7.0 (API 24) to Android 15 (API 36)
- **Architecture:** Universal (Supports all ARM64, ARMv7, and x86_64 devices)

### Available Files
1. **`SeismicGuard-v1.8-phone-ready.apk`** (28 MB):
   - Fully compiled and ready to sideload directly onto any Android phone or tablet.
   - **Auto Weather Sync on Open**: Meteorological data updates automatically every time the app is opened or resumed from background.
   - **Pull-To-Refresh**: Weather & Ground Safety home dashboard supports native swipe-down ("scroll upward to refresh") with live refresh animation.
   - **No Red LED**: Red LED references removed across the app and diagnostics. Only the BC547 Acoustic Buzzer is used for audible tremor warnings.
   - **ESP32 & Firebase Realtime Cloud**: Full two-way sync with Firebase Realtime Database at `seismic-guard-default-rtdb.asia-southeast1.firebasedatabase.app`.
   - **Firmware Folder**: Complete, copy-pasteable Arduino IDE code provided in `/firmware/SeismicGuard_ESP32/SeismicGuard_ESP32.ino`.

### How to Install on Your Android Phone
1. Download **`SeismicGuard-v1.8-phone-ready.apk`** from this `/apks` folder or from the project ZIP export.
2. Transfer or open the `.apk` file on your Android device.
3. If prompted, allow **"Install unknown apps"** from your file manager or browser.
4. Tap **Install** to complete setup.
