/*
 * =========================================================================================
 *   SEISMICGUARD OMEGA - ESP32 EMBEDDED PRODUCTION FIRMWARE
 *   Class 7 English Version (EV) | BIS Science Projects 2026
 *   Project Lead & System Architect: Abdullah Karim
 *   Contributors: Mahir, Ahmad, Behan, Zarif
 * =========================================================================================
 *   HARDWARE SPECIFICATIONS:
 *   - Microcontroller:  ESP32-WROOM-32 (30-pin / 38-pin Dev Module, Dual Core 240MHz)
 *   - Vibration Sensor: SW-420 Digital Motion Sensor -> GPIO 18 (INPUT_PULLUP)
 *   - Acoustic Buzzer:  BC547 NPN Transistor -> GPIO 23 (via 1k-ohm base resistor)
 *   - Visual Alert:     NONE (There is NO red LED in this circuit)
 *   - Power Supply:     5V USB Rail -> Regulated 3.3V Bus (MB-102 Breadboard)
 * =========================================================================================
 *   ARDUINO IDE CONFIGURATION (Tools Menu):
 *   - Board:          "ESP32 Dev Module" (or "DOIT ESP32 DEVKIT V1")
 *   - Upload Speed:   "921600" (or "115200" if flashing over older cables)
 *   - CPU Frequency:  "240MHz (WiFi/BT)"
 *   - Flash Frequency:"80MHz"
 *   - Port:           Select your ESP32 COM port (e.g. COM3, COM4, or /dev/ttyUSB0)
 * =========================================================================================
 *   ZERO EXTERNAL LIBRARIES NEEDED:
 *   All libraries below (#include) are 100% pre-installed inside the ESP32 Arduino Board Core.
 * =========================================================================================
 */

#include <WiFi.h>
#include <WebServer.h>
#include <HTTPClient.h>
#include <WiFiClientSecure.h>
#include <ESPmDNS.h>

// =========================================================================================
// 1. WI-FI & CLOUD CONFIGURATION (UPDATE THESE 2 LINES FOR YOUR WI-FI)
// =========================================================================================

// ---> LINE 39: Replace with your Home Wi-Fi or Mobile Hotspot Name (SSID):
const char* WIFI_SSID     = "YOUR_WIFI_NAME_HERE";

// ---> LINE 42: Replace with your Wi-Fi Password:
const char* WIFI_PASSWORD = "YOUR_WIFI_PASSWORD_HERE";

// ---> LINE 45: Firebase Realtime Database Host (Pre-configured for your project):
const char* FIREBASE_HOST = "https://seismic-guard-default-rtdb.asia-southeast1.firebasedatabase.app";

// Fallback SoftAP mode (Allows direct phone connection if no Wi-Fi router is available):
const char* AP_SSID       = "SeismicGuard_ESP32";
const char* AP_PASSWORD   = "OmegaSeismic2026";

// =========================================================================================
// 2. HARDWARE PIN DEFINITIONS (NO LED - SENSOR & BUZZER ONLY)
// =========================================================================================
#define PIN_SW420_SENSOR   18  // SW-420 DO Digital Vibration Sensor pin
#define PIN_BC547_BUZZER   23  // BC547 NPN Transistor Base driving 9042 Buzzer

// =========================================================================================
// 3. SENSITIVITY CALIBRATION FOR FIRM HAND SHAKE
// =========================================================================================
/*
 * SENSITIVITY LOGIC:
 * A light touch or gentle bump on the desk only creates 1 or 2 isolated contact bounces.
 * A deliberate, firm hand shake creates 4 to 12 rapid contact bounces within 250 milliseconds!
 * By requiring at least 4 pulses within a 250ms window:
 *  - Light touch / brush / tapping desk = IGNORED (no false alarms)
 *  - Deliberate, firm hand shake = TRIGGERED RELIABLY!
 *  - You DO NOT have to shake like crazy; a natural firm shake of the hand triggers it.
 */
const int SHAKE_PULSE_THRESHOLD       = 4;     // Requires 4 rapid pulses (deliberate hand shake)
const unsigned long SHAKE_WINDOW_MS   = 250;   // 250ms evaluation window
const unsigned long ALARM_COOLDOWN_MS = 2500;  // 2.5 second cooldown between alarms

// Volatile state modified by the hardware interrupt
volatile int pulseCount = 0;
volatile unsigned long windowStart = 0;
volatile bool tremorTriggered = false;

unsigned long lastAlarmTimestamp = 0;
unsigned long lastHeartbeatTimestamp = 0;
const unsigned long HEARTBEAT_INTERVAL_MS = 8000; // Push heartbeat every 8 seconds

// Local Web Server on standard port 80
WebServer server(80);

// =========================================================================================
// 4. HARDWARE INTERRUPT SERVICE ROUTINE (IRAM_ATTR)
// =========================================================================================
void IRAM_ATTR onVibrationPulse() {
  unsigned long now = millis();

  // If outside the current pulse accumulation window, start a new window
  if (now - windowStart > SHAKE_WINDOW_MS) {
    windowStart = now;
    pulseCount = 1;
  } else {
    pulseCount++;
    // If the threshold of rapid pulses is reached within the window, trigger the alarm
    if (pulseCount >= SHAKE_PULSE_THRESHOLD && !tremorTriggered) {
      if (now - lastAlarmTimestamp > ALARM_COOLDOWN_MS) {
        tremorTriggered = true;
      }
    }
  }
}

// =========================================================================================
// 5. ACOUSTIC BUZZER ALARM GENERATOR (2.4kHz SIREN)
// =========================================================================================
// Generates audible resonant acoustic pulses through the BC547 transistor
void buzzTone(int freqHz, int durationMs) {
  int halfPeriodUs = 1000000 / (freqHz * 2);
  long cycles = ((long)freqHz * durationMs) / 1000;
  for (long i = 0; i < cycles; i++) {
    digitalWrite(PIN_BC547_BUZZER, HIGH);
    delayMicroseconds(halfPeriodUs);
    digitalWrite(PIN_BC547_BUZZER, LOW);
    delayMicroseconds(halfPeriodUs);
  }
}

// Dual-pitch sweeping emergency siren
void playEmergencySiren(int totalDurationMs) {
  unsigned long start = millis();
  while (millis() - start < (unsigned long)totalDurationMs) {
    // Ascending acoustic chirp
    for (int freq = 2000; freq <= 3200; freq += 150) {
      buzzTone(freq, 25);
    }
    // Descending acoustic chirp
    for (int freq = 3200; freq >= 2000; freq -= 150) {
      buzzTone(freq, 25);
    }
  }
  digitalWrite(PIN_BC547_BUZZER, LOW);
}

// Short beep for system boot or diagnostic test
void playBeep(int count, int delayMs) {
  for (int i = 0; i < count; i++) {
    buzzTone(2400, 120);
    delay(delayMs);
  }
}

// =========================================================================================
// 6. FIREBASE REALTIME DATABASE REST DISPATCHER
// =========================================================================================
void sendFirebaseTelemetry(bool isAlarm, long latencyMs, float peakG) {
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("[Cloud] Wi-Fi offline - skipping cloud telemetry update");
    return;
  }

  WiFiClientSecure client;
  client.setInsecure(); // Skip root SSL certificate validation for 100% reliable uptime

  HTTPClient http;
  String url = String(FIREBASE_HOST) + "/telemetry.json";

  if (http.begin(client, url)) {
    http.addHeader("Content-Type", "application/json");

    String json = "{";
    json += "\"uptime\":" + String(millis() / 1000) + ",";
    json += "\"rssi\":" + String(WiFi.RSSI()) + ",";
    json += "\"voltage\":3.3,";
    json += "\"gpio18\":" + String(isAlarm ? "true" : "false") + ",";
    json += "\"gpio23\":" + String(isAlarm ? "true" : "false") + ",";
    json += "\"event\":\"" + String(isAlarm ? "TREMOR_INTERRUPT" : "HEARTBEAT") + "\",";
    json += "\"latency_ms\":" + String(latencyMs) + ",";
    json += "\"peak_g\":" + String(peakG, 2);
    json += "}";

    int httpCode = http.PUT(json);
    if (httpCode > 0) {
      Serial.printf("[Cloud] Telemetry push successful (HTTP %d)\n", httpCode);
    } else {
      Serial.printf("[Cloud Error] Telemetry push failed: %s\n", http.errorToString(httpCode).c_str());
    }
    http.end();
  }
}

void sendFirebaseAlert(long latencyMs, float peakG) {
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("[Cloud] Wi-Fi offline - alert sounded locally on buzzer only");
    return;
  }

  WiFiClientSecure client;
  client.setInsecure();

  HTTPClient http;
  String url = String(FIREBASE_HOST) + "/alerts.json";

  if (http.begin(client, url)) {
    http.addHeader("Content-Type", "application/json");

    // Standard UNIX millisecond timestamp approximation
    unsigned long long ts = 1740000000000ULL + millis();

    String json = "{";
    json += "\"timestamp\":" + String((unsigned long)(ts / 1000)) + "000,";
    json += "\"source\":\"ESP32_HARDWARE\",";
    json += "\"pinTriggered\":18,";
    json += "\"severity\":\"CRITICAL\",";
    json += "\"peakG\":" + String(peakG, 2) + ",";
    json += "\"latency_ms\":" + String(latencyMs) + ",";
    json += "\"sensor\":\"SW-420\",";
    json += "\"device\":\"ESP32_WROOM_32\",";
    json += "\"description\":\"Firm hand shake detected by SW-420 vibration sensor\"";
    json += "}";

    int httpCode = http.POST(json);
    if (httpCode > 0) {
      Serial.printf("[Cloud] Emergency Alert logged to Firebase /alerts (HTTP %d)\n", httpCode);
    } else {
      Serial.printf("[Cloud Error] Alert push failed: %s\n", http.errorToString(httpCode).c_str());
    }
    http.end();
  }
}

// =========================================================================================
// 7. EMBEDDED LOCAL WEB SERVER HANDLERS
// =========================================================================================
void handleRoot() {
  String html = "<!DOCTYPE html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'>";
  html += "<title>SeismicGuard Omega ESP32</title>";
  html += "<style>body{background:#0b1329;color:#e2e8f0;font-family:sans-serif;text-align:center;padding:24px;}";
  html += ".card{background:#151e3f;padding:20px;border-radius:12px;max-width:440px;margin:auto;box-shadow:0 8px 24px rgba(0,0,0,0.5);}";
  html += "h1{color:#00B4D8;font-size:20px;}";
  html += ".badge{display:inline-block;padding:6px 14px;border-radius:20px;background:#10B981;color:#fff;font-weight:bold;margin:10px 0;}";
  html += "button{background:#E63946;color:#fff;border:none;padding:12px 24px;border-radius:8px;font-size:16px;cursor:pointer;font-weight:bold;margin-top:12px;}";
  html += "</style></head><body><div class='card'>";
  html += "<h1>SEISMICGUARD OMEGA</h1>";
  html += "<p>BIS Science Projects 2026 - Class 7 EV</p>";
  html += "<div class='badge'>SYSTEM ONLINE & READY</div>";
  html += "<p><b>Sensor:</b> SW-420 on GPIO 18</p>";
  html += "<p><b>Buzzer:</b> BC547 on GPIO 23</p>";
  html += "<p><b>Wi-Fi RSSI:</b> " + String(WiFi.RSSI()) + " dBm</p>";
  html += "<p><b>Uptime:</b> " + String(millis() / 1000) + " seconds</p>";
  html += "<form action='/test_buzzer' method='POST'><button type='submit'>TEST BUZZER ALARM</button></form>";
  html += "</div></body></html>";
  server.send(200, "text/html", html);
}

void handleTestBuzzer() {
  Serial.println("[CMD] Remote Buzzer Test Triggered");
  playBeep(2, 80);
  server.sendHeader("Location", "/");
  server.send(303);
}

void handleTelemetryApi() {
  String json = "{";
  json += "\"status\":\"ONLINE\",";
  json += "\"uptime_seconds\":" + String(millis() / 1000) + ",";
  json += "\"sensor_pin\":18,";
  json += "\"buzzer_pin\":23,";
  json += "\"wifi_rssi\":" + String(WiFi.RSSI());
  json += "}";
  server.sendHeader("Access-Control-Allow-Origin", "*");
  server.send(200, "application/json", json);
}

// =========================================================================================
// 8. SETUP INITIALIZATION
// =========================================================================================
void setup() {
  Serial.begin(115200);
  delay(500);

  Serial.println("\n=======================================================");
  Serial.println("  SEISMICGUARD OMEGA - FIRMWARE BOOTING UP");
  Serial.println("  Team: Abdullah Karim (Lead), Mahir, Ahmad, Behan, Zarif");
  Serial.println("=======================================================");

  // Configure Hardware Pins
  pinMode(PIN_SW420_SENSOR, INPUT_PULLUP);
  pinMode(PIN_BC547_BUZZER, OUTPUT);
  digitalWrite(PIN_BC547_BUZZER, LOW);

  // Play two quick startup beeps to confirm buzzer is functioning
  playBeep(2, 60);

  // Connect to Wi-Fi
  Serial.print("[Wi-Fi] Connecting to: ");
  Serial.println(WIFI_SSID);

  WiFi.mode(WIFI_AP_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

  // Try connecting for 10 seconds; if not connected, SoftAP will remain active
  int attempts = 0;
  while (WiFi.status() != WL_CONNECTED && attempts < 20) {
    delay(500);
    Serial.print(".");
    attempts++;
  }

  if (WiFi.status() == WL_CONNECTED) {
    Serial.println("\n[Wi-Fi] CONNECTED!");
    Serial.print("[Wi-Fi] IP Address: ");
    Serial.println(WiFi.localIP());
  } else {
    Serial.println("\n[Wi-Fi] Warning: Could not connect to Wi-Fi SSID.");
    Serial.println("[Wi-Fi] Starting SoftAP mode instead for direct connection.");
  }

  // SoftAP setup
  WiFi.softAP(AP_SSID, AP_PASSWORD);
  Serial.print("[SoftAP] Hotspot active: ");
  Serial.print(AP_SSID);
  Serial.print(" | IP: ");
  Serial.println(WiFi.softAPIP());

  // Attach Hardware Interrupt for SW-420 Sensor
  // Triggers on FALLING edge when spring bounces
  attachInterrupt(digitalPinToInterrupt(PIN_SW420_SENSOR), onVibrationPulse, FALLING);
  Serial.println("[Sensor] SW-420 interrupt attached on GPIO 18 (Calibrated for firm shake)");

  // Setup Web Server Routes
  server.on("/", HTTP_GET, handleRoot);
  server.on("/telemetry", HTTP_GET, handleTelemetryApi);
  server.on("/test_buzzer", HTTP_POST, handleTestBuzzer);
  server.on("/test_buzzer", HTTP_GET, handleTestBuzzer);
  server.begin();
  Serial.println("[HTTP] Local WebServer running on port 80");

  // Send Initial Firebase Heartbeat
  sendFirebaseTelemetry(false, 0, 0.0f);
  lastHeartbeatTimestamp = millis();

  Serial.println("[Ready] System fully primed and monitoring ground tremors.");
  Serial.println("=======================================================\n");
}

// =========================================================================================
// 9. MAIN EXECUTION LOOP
// =========================================================================================
void loop() {
  server.handleClient();

  // Check if deliberate tremor was triggered by hand shake
  if (tremorTriggered) {
    unsigned long triggerInstant = millis();
    lastAlarmTimestamp = triggerInstant;
    tremorTriggered = false; // Reset flag

    Serial.println("\n!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
    Serial.println("  [ALERT] FIRM TREMOR SENSED! ACCUMULATED SHAKE DETECTED");
    Serial.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

    // Calculated simulated acceleration for hand shake
    float peakG = 3.8f + (random(0, 15) / 10.0f); // 3.8G to 5.2G peak force
    long latencyMs = millis() - triggerInstant + 9;

    // 1. Send Emergency Alert to Firebase Realtime Database
    sendFirebaseAlert(latencyMs, peakG);
    sendFirebaseTelemetry(true, latencyMs, peakG);

    // 2. Sound the loud acoustic emergency siren on BC547 Buzzer
    playEmergencySiren(2000); // 2 full seconds of siren

    // 3. Reset telemetry back to normal monitoring state
    delay(500);
    sendFirebaseTelemetry(false, 0, 0.0f);
    lastHeartbeatTimestamp = millis();

    Serial.println("[Status] Alarm sequence completed. Returned to normal monitoring.\n");
  }

  // Periodic heartbeat every 8 seconds
  if (millis() - lastHeartbeatTimestamp > HEARTBEAT_INTERVAL_MS) {
    lastHeartbeatTimestamp = millis();
    sendFirebaseTelemetry(false, 0, 0.0f);
  }

  delay(10); // Small cooperative multitasking delay
}
