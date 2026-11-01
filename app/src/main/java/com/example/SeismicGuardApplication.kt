package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.data.repository.SeismicHardwareRepository
import com.example.emergency.EmergencyAlarmService

class SeismicGuardApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createEmergencyNotificationChannel()
        // Pre-initialize repository
        SeismicHardwareRepository.getInstance(this)
    }

    private fun createEmergencyNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                EmergencyAlarmService.CHANNEL_ID,
                "Seismic Critical Override Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority full-screen alerts for critical seismic tremors"
                enableVibration(true)
                setBypassDnd(true)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
