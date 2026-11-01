package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tremor_events")
data class TremorEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val source: String, // "ESP32_GPIO18", "SUPABASE_REALTIME", "LOCAL_WEBSOCKET", "SIMULATED_TEST"
    val pinTriggered: Int = 18,
    val peakAmplitudeG: Float,
    val interruptLatencyMs: Long,
    val durationMs: Long,
    val severity: String, // "LOW", "MODERATE", "CRITICAL"
    val acknowledged: Boolean = false,
    val notes: String = ""
)
