package com.example.data.model

enum class BackendGateway(val displayName: String, val description: String) {
    MULTI_GATEWAY("Multi-Gateway Auto", "Dynamic fallback: Firebase Cloud -> Local WS -> Simulation"),
    FIREBASE_RTDB("Firebase Realtime Cloud", "Direct live sync with your Asia-Southeast1 Firebase RTDB"),
    LOCAL_WEBSOCKET("ESP32 Local WebSocket", "Direct low-latency ws://<ESP32_IP>/ws connection (<15ms)"),
    SUPABASE_REALTIME("Supabase Cloud Realtime", "Cloud PostgreSQL telemetry table subscriber"),
    PRESENTATION_DEMO("Presentation Simulation Mode", "Safe interactive simulation for live exhibition demos")
}

data class HardwareTelemetry(
    val isConnected: Boolean = false, // false by default until physical ESP32 or Firebase cloud transmits
    val isSimulated: Boolean = false,
    val gateway: BackendGateway = BackendGateway.MULTI_GATEWAY,
    val firebaseConnected: Boolean = false,
    val firebaseDbUrl: String = "https://seismic-guard-default-rtdb.asia-southeast1.firebasedatabase.app",
    val esp32Ip: String = "192.168.4.1",
    val cpuUptimeSeconds: Long = 3420L,
    val wifiRssiDbm: Int = -58,
    val operatingVoltageBus: Float = 3.32f,
    val gpio18SensorState: Boolean = false, // false = stable, true = tremor detected
    val gpio22LedState: Boolean = false,   // 5mm red status LED
    val gpio23BuzzerState: Boolean = false,// BC547 transistor driving 9042 passive buzzer
    val triggerCount: Int = 14,
    val lastTriggerLatencyMs: Long = 12L,
    val sensorDebounceMs: Int = 500,
    val potentiometerSensitivityPct: Int = 75,
    val lastHeartbeatTimestamp: Long = System.currentTimeMillis()
)

data class CriticalTremorAlert(
    val triggerTimestamp: Long = System.currentTimeMillis(),
    val source: String = "ESP32_GPIO18",
    val pinTriggered: Int = 18,
    val peakG: Float = 3.8f,
    val latencyMs: Long = 11L,
    val formattedTime: String = "Just now",
    val message: String = "CRITICAL HARDWARE TREMOR DETECTED - ESP32 SENSOR GPIO 18 TRIGGERED"
)
