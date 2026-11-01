package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.SeismicDatabase
import com.example.data.local.TremorEventDao
import com.example.data.local.TremorEventEntity
import com.example.data.model.BackendGateway
import com.example.data.model.CriticalTremorAlert
import com.example.data.model.HardwareTelemetry
import com.example.emergency.SoundSynthesizer
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class SeismicHardwareRepository(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val tremorDao: TremorEventDao = SeismicDatabase.getDatabase(context).tremorEventDao()

    private val okHttpClient = OkHttpClient.Builder()
        .pingInterval(5, TimeUnit.SECONDS)
        .build()

    private var activeWebSocket: WebSocket? = null
    private var simulationJob: Job? = null

    // Firebase Realtime Database references
    private var firebaseDb: FirebaseDatabase? = null
    private var firebaseConnListener: ValueEventListener? = null
    private var firebaseTelemetryListener: ValueEventListener? = null
    private var firebaseAlertsListener: ValueEventListener? = null

    private val _telemetryState = MutableStateFlow(HardwareTelemetry())
    val telemetryState: StateFlow<HardwareTelemetry> = _telemetryState.asStateFlow()

    private val _criticalAlertEvents = MutableSharedFlow<CriticalTremorAlert>(extraBufferCapacity = 5)
    val criticalAlertEvents: SharedFlow<CriticalTremorAlert> = _criticalAlertEvents.asSharedFlow()

    var selectedAlarmTone: String = SoundSynthesizer.TONE_2400HZ_PULSE

    companion object {
        private const val TAG = "SeismicRepository"
        const val FIREBASE_RTDB_URL = "https://seismic-guard-default-rtdb.asia-southeast1.firebasedatabase.app"

        @Volatile
        private var INSTANCE: SeismicHardwareRepository? = null

        fun getInstance(context: Context): SeismicHardwareRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SeismicHardwareRepository(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    init {
        startGateway(BackendGateway.MULTI_GATEWAY)
    }

    private fun getFirebase(): FirebaseDatabase? {
        if (firebaseDb == null) {
            try {
                firebaseDb = FirebaseDatabase.getInstance(FIREBASE_RTDB_URL)
            } catch (e: Exception) {
                Log.e(TAG, "Failed initializing Firebase Database", e)
            }
        }
        return firebaseDb
    }

    fun setGateway(newGateway: BackendGateway) {
        _telemetryState.update { it.copy(gateway = newGateway) }
        startGateway(newGateway)
    }

    fun updateEsp32Ip(ip: String) {
        _telemetryState.update { it.copy(esp32Ip = ip) }
        if (_telemetryState.value.gateway == BackendGateway.LOCAL_WEBSOCKET) {
            connectLocalWebSocket(ip)
        }
    }

    fun updateSensitivity(pct: Int) {
        _telemetryState.update { it.copy(potentiometerSensitivityPct = pct) }
        sendCommandToHardware("SET_SENSITIVITY:$pct")
    }

    fun updateDebounce(ms: Int) {
        _telemetryState.update { it.copy(sensorDebounceMs = ms) }
        sendCommandToHardware("SET_DEBOUNCE:$ms")
    }

    private fun startGateway(gateway: BackendGateway) {
        simulationJob?.cancel()
        activeWebSocket?.close(1000, "Switching gateway")
        activeWebSocket = null
        cleanupFirebaseListeners()

        when (gateway) {
            BackendGateway.FIREBASE_RTDB -> {
                connectFirebaseRealtimeDb()
            }
            BackendGateway.LOCAL_WEBSOCKET -> {
                connectLocalWebSocket(_telemetryState.value.esp32Ip)
            }
            BackendGateway.SUPABASE_REALTIME -> {
                connectSupabaseRealtime()
            }
            BackendGateway.MULTI_GATEWAY -> {
                connectFirebaseRealtimeDb()
                connectLocalWebSocket(_telemetryState.value.esp32Ip)
            }
            BackendGateway.PRESENTATION_DEMO -> {
                startPresentationSimulationLoop()
            }
        }
    }

    private fun cleanupFirebaseListeners() {
        val db = firebaseDb ?: return
        try {
            firebaseConnListener?.let { db.getReference(".info/connected").removeEventListener(it) }
            firebaseTelemetryListener?.let { db.getReference("telemetry").removeEventListener(it) }
            firebaseAlertsListener?.let { db.getReference("alerts").removeEventListener(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Error cleaning up Firebase listeners", e)
        }
    }

    private fun connectFirebaseRealtimeDb() {
        val db = getFirebase() ?: run {
            Log.w(TAG, "Firebase RTDB not available")
            return
        }

        try {
            // Monitor connectivity to Firebase Realtime Database
            val connRef = db.getReference(".info/connected")
            firebaseConnListener?.let { connRef.removeEventListener(it) }
            val connL = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val connected = snapshot.getValue(Boolean::class.java) == true
                    _telemetryState.update { it.copy(firebaseConnected = connected) }
                    Log.d(TAG, "Firebase connection state: $connected")
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "Firebase connection listener cancelled: ${error.message}")
                }
            }
            firebaseConnListener = connL
            connRef.addValueEventListener(connL)

            // Listen to live hardware telemetry from ESP32: /telemetry
            val telemetryRef = db.getReference("telemetry")
            firebaseTelemetryListener?.let { telemetryRef.removeEventListener(it) }
            val telemL = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) return
                    try {
                        val map = snapshot.value as? Map<*, *> ?: return
                        val uptime = (map["uptime"] as? Number)?.toLong() ?: _telemetryState.value.cpuUptimeSeconds
                        val rssi = (map["rssi"] as? Number)?.toInt() ?: _telemetryState.value.wifiRssiDbm
                        val voltage = (map["voltage"] as? Number)?.toFloat()
                            ?: ((map["voltage"] as? Double)?.toFloat() ?: 3.3f)
                        val gpio18 = (map["gpio18"] as? Boolean) == true
                        val gpio22 = (map["gpio22"] as? Boolean) == true
                        val gpio23 = (map["gpio23"] as? Boolean) == true
                        val event = map["event"] as? String

                        _telemetryState.update {
                            it.copy(
                                isConnected = true,
                                isSimulated = false,
                                cpuUptimeSeconds = uptime,
                                wifiRssiDbm = rssi,
                                operatingVoltageBus = voltage,
                                gpio18SensorState = gpio18,
                                gpio22LedState = gpio22,
                                gpio23BuzzerState = gpio23,
                                lastHeartbeatTimestamp = System.currentTimeMillis()
                            )
                        }

                        if (event == "TREMOR_INTERRUPT" || gpio18) {
                            val latency = (map["latency_ms"] as? Number)?.toLong() ?: 11L
                            val peakG = (map["peak_g"] as? Number)?.toFloat() ?: 3.8f
                            triggerEmergencyTremorAlert("ESP32_FIREBASE_RTDB", 18, latency, peakG)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing Firebase telemetry snapshot", e)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "Firebase telemetry cancelled: ${error.message}")
                }
            }
            firebaseTelemetryListener = telemL
            telemetryRef.addValueEventListener(telemL)

            // Listen to emergency alerts: /alerts
            val alertsRef = db.getReference("alerts")
            firebaseAlertsListener?.let { alertsRef.removeEventListener(it) }
            val alertL = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) return
                    try {
                        val map = snapshot.value as? Map<*, *> ?: return
                        val timestamp = (map["timestamp"] as? Number)?.toLong() ?: 0L
                        // Prevent re-triggering old alerts
                        if (System.currentTimeMillis() - timestamp < 15000L && timestamp > 0) {
                            val latency = (map["latency_ms"] as? Number)?.toLong() ?: 12L
                            val peakG = (map["peak_g"] as? Number)?.toFloat() ?: 4.2f
                            triggerEmergencyTremorAlert("ESP32_FIREBASE_RTDB", 18, latency, peakG)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in Firebase alerts listener", e)
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            }
            firebaseAlertsListener = alertL
            alertsRef.addValueEventListener(alertL)

            Log.i(TAG, "Successfully subscribed to Firebase RTDB ($FIREBASE_RTDB_URL)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed connecting to Firebase RTDB", e)
        }
    }

    fun sendFirebaseTestPing(onResult: (Boolean, String) -> Unit) {
        val db = getFirebase()
        if (db == null) {
            onResult(false, "Firebase Database is not initialized")
            return
        }
        try {
            val pingRef = db.getReference("test_ping")
            val pingData = mapOf(
                "device" to "SeismicGuard_Android_App",
                "timestamp" to System.currentTimeMillis(),
                "status" to "ONLINE",
                "rtdb_endpoint" to FIREBASE_RTDB_URL,
                "notes" to "Heartbeat test packet from Team Omega Mobile Gateway"
            )
            pingRef.setValue(pingData).addOnSuccessListener {
                _telemetryState.update { it.copy(firebaseConnected = true) }
                onResult(true, "Successfully written to Firebase RTDB node '/test_ping'!")
            }.addOnFailureListener { err ->
                onResult(false, "Write failed: ${err.message}. Check Database Rules in Firebase Console.")
            }
        } catch (e: Exception) {
            onResult(false, e.localizedMessage ?: "Unknown error")
        }
    }

    private fun connectLocalWebSocket(ip: String) {
        // 1. Attempt WebSocket connection (sub-15ms streaming)
        try {
            val request = Request.Builder()
                .url("ws://$ip/ws")
                .build()

            activeWebSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    _telemetryState.update {
                        it.copy(isConnected = true, isSimulated = false)
                    }
                    Log.d(TAG, "Connected to ESP32 Local WebSocket: $ip")
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    parseHardwarePayload(text, "ESP32_LOCAL_WS")
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.w(TAG, "Local WebSocket failed to connect to $ip, falling back to HTTP REST", t)
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Failed initiating WebSocket to $ip", e)
        }

        // 2. Immediate HTTP REST Ping Fallback (Guaranteed to work with standard WebServer.h)
        scope.launch {
            try {
                val req = Request.Builder().url("http://$ip/status").build()
                val resp = okHttpClient.newCall(req).execute()
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: ""
                    if (body.isNotEmpty()) {
                        parseHardwarePayload(body, "ESP32_HTTP_REST")
                        _telemetryState.update { it.copy(isConnected = true, isSimulated = false) }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "HTTP REST query to $ip: ${e.message}")
            }
        }
    }

    fun testHardwarePing(ip: String, onResult: (Boolean, String) -> Unit) {
        scope.launch {
            try {
                val req = Request.Builder()
                    .url("http://$ip/ping")
                    .build()
                val start = System.currentTimeMillis()
                val resp = okHttpClient.newCall(req).execute()
                val latency = System.currentTimeMillis() - start
                if (resp.isSuccessful) {
                    _telemetryState.update { it.copy(isConnected = true, isSimulated = false) }
                    onResult(true, "ESP32 Responsive! Latency: ${latency}ms • Board: ESP32-WROOM-32")
                } else {
                    onResult(false, "ESP32 returned HTTP error code ${resp.code}")
                }
            } catch (e: Exception) {
                onResult(false, "No response from http://$ip/ping (${e.localizedMessage}). Check phone Wi-Fi.")
            }
        }
    }

    private fun connectSupabaseRealtime() {
        _telemetryState.update { it.copy(isConnected = false, isSimulated = true) }
        startPresentationSimulationLoop(isCloud = true)
    }

    private fun startPresentationSimulationLoop(isCloud: Boolean = false) {
        simulationJob?.cancel()
        simulationJob = scope.launch {
            _telemetryState.update { it.copy(isConnected = false, isSimulated = true) }
            var uptime = _telemetryState.value.cpuUptimeSeconds

            while (isActive) {
                delay(1000)
                uptime += 1
                val jitterRssi = -55 - (uptime % 8).toInt()
                val busVoltage = 3.30f + ((uptime % 5) * 0.005f)

                _telemetryState.update { current ->
                    current.copy(
                        cpuUptimeSeconds = uptime,
                        wifiRssiDbm = jitterRssi,
                        operatingVoltageBus = busVoltage,
                        lastHeartbeatTimestamp = System.currentTimeMillis()
                    )
                }
            }
        }
    }

    fun triggerEmergencyTremorAlert(
        source: String = "ESP32_GPIO18",
        pin: Int = 18,
        latencyMs: Long = 11L,
        peakG: Float = 4.2f
    ) {
        scope.launch {
            val timeString = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

            _telemetryState.update {
                it.copy(
                    gpio18SensorState = true,
                    gpio22LedState = true,
                    gpio23BuzzerState = true,
                    triggerCount = it.triggerCount + 1,
                    lastTriggerLatencyMs = latencyMs
                )
            }

            // Save to local Room DB
            val entity = TremorEventEntity(
                source = source,
                pinTriggered = pin,
                peakAmplitudeG = peakG,
                interruptLatencyMs = latencyMs,
                durationMs = 3500L,
                severity = "CRITICAL",
                notes = "SW-420 edge interrupt trigger. BC547 buzzer and Red LED active."
            )
            tremorDao.insertEvent(entity)

            // Emit to shared flow for UI / Notification / Service
            _criticalAlertEvents.emit(
                CriticalTremorAlert(
                    triggerTimestamp = System.currentTimeMillis(),
                    source = source,
                    pinTriggered = pin,
                    peakG = peakG,
                    latencyMs = latencyMs,
                    formattedTime = timeString
                )
            )

            // Reset pin triggers after cooldown
            delay(4000)
            _telemetryState.update {
                it.copy(
                    gpio18SensorState = false,
                    gpio22LedState = false,
                    gpio23BuzzerState = false
                )
            }
        }
    }

    private fun parseHardwarePayload(jsonString: String, sourceTag: String) {
        try {
            val json = JSONObject(jsonString)
            val eventType = json.optString("event", "telemetry")
            if (eventType == "TREMOR_INTERRUPT" || json.optBoolean("gpio18", false)) {
                val latency = json.optLong("latency_ms", 12L)
                val peakG = json.optDouble("peak_g", 3.8).toFloat()
                triggerEmergencyTremorAlert(sourceTag, 18, latency, peakG)
            } else {
                _telemetryState.update {
                    it.copy(
                        isConnected = true,
                        isSimulated = false,
                        cpuUptimeSeconds = json.optLong("uptime", it.cpuUptimeSeconds),
                        wifiRssiDbm = json.optInt("rssi", it.wifiRssiDbm),
                        operatingVoltageBus = json.optDouble("voltage", it.operatingVoltageBus.toDouble()).toFloat(),
                        gpio18SensorState = json.optBoolean("gpio18", false),
                        gpio22LedState = json.optBoolean("gpio22", false),
                        gpio23BuzzerState = json.optBoolean("gpio23", false)
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed parsing hardware payload: $jsonString", e)
        }
    }

    fun sendCommandToHardware(command: String) {
        // 1. Send via WebSocket if connected
        activeWebSocket?.send(command)

        // 2. Send via HTTP REST to ESP32 Local Server (WebServer on port 80)
        scope.launch {
            try {
                val ip = _telemetryState.value.esp32Ip
                val endpoint = when (command) {
                    "TEST_BUZZER" -> "test_buzzer"
                    "TEST_LED" -> "test_led"
                    "SIMULATE_TREMOR" -> "trigger"
                    else -> "status"
                }
                val req = Request.Builder()
                    .url("http://$ip/$endpoint")
                    .build()
                okHttpClient.newCall(req).execute().close()
            } catch (e: Exception) {
                Log.d(TAG, "HTTP command dispatch: ${e.message}")
            }
        }

        // 3. Mirror command to Firebase RTDB node
        try {
            firebaseDb?.getReference("commands")?.setValue(
                mapOf(
                    "command" to command,
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed writing command to Firebase RTDB", e)
        }
        Log.d(TAG, "Command sent to hardware: $command")
    }
}
