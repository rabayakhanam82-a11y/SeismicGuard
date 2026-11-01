package com.example.emergency

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.R

class EmergencyAlarmService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var vibrator: Vibrator? = null
    private val soundSynthesizer = SoundSynthesizer()
    private var originalVolume: Int = 0

    companion object {
        private const val TAG = "EmergencyAlarmService"
        const val CHANNEL_ID = "seismic_critical_channel"
        const val NOTIFICATION_ID = 9042

        const val ACTION_TRIGGER_ALARM = "com.example.seismicguard.ACTION_TRIGGER_ALARM"
        const val ACTION_DISMISS_ALARM = "com.example.seismicguard.ACTION_DISMISS_ALARM"

        const val EXTRA_SOURCE = "extra_source"
        const val EXTRA_PIN = "extra_pin"
        const val EXTRA_LATENCY = "extra_latency"
        const val EXTRA_PEAK_G = "extra_peak_g"
        const val EXTRA_TONE = "extra_tone"

        fun triggerAlert(
            context: Context,
            source: String = "ESP32_GPIO18",
            pin: Int = 18,
            latencyMs: Long = 12L,
            peakG: Float = 3.8f,
            toneProfile: String = SoundSynthesizer.TONE_2400HZ_PULSE
        ) {
            val intent = Intent(context, EmergencyAlarmService::class.java).apply {
                action = ACTION_TRIGGER_ALARM
                putExtra(EXTRA_SOURCE, source)
                putExtra(EXTRA_PIN, pin)
                putExtra(EXTRA_LATENCY, latencyMs)
                putExtra(EXTRA_PEAK_G, peakG)
                putExtra(EXTRA_TONE, toneProfile)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun dismissAlert(context: Context) {
            val intent = Intent(context, EmergencyAlarmService::class.java).apply {
                action = ACTION_DISMISS_ALARM
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "SeismicGuard:EmergencyWakeLock"
        )?.apply {
            setReferenceCounted(false)
        }

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_TRIGGER_ALARM

        if (action == ACTION_DISMISS_ALARM) {
            stopEmergencyOverride()
            stopSelf()
            return START_NOT_STICKY
        }

        val source = intent?.getStringExtra(EXTRA_SOURCE) ?: "ESP32_GPIO18"
        val pin = intent?.getIntExtra(EXTRA_PIN, 18) ?: 18
        val latency = intent?.getLongExtra(EXTRA_LATENCY, 12L) ?: 12L
        val peakG = intent?.getFloatExtra(EXTRA_PEAK_G, 3.8f) ?: 3.8f
        val tone = intent?.getStringExtra(EXTRA_TONE) ?: SoundSynthesizer.TONE_2400HZ_PULSE

        startEmergencyOverride(source, pin, latency, peakG, tone)
        return START_STICKY
    }

    private fun startEmergencyOverride(
        source: String,
        pin: Int,
        latency: Long,
        peakG: Float,
        tone: String
    ) {
        // 1. Acquire WakeLock
        try {
            wakeLock?.acquire(10 * 60 * 1000L /* 10 minutes */)
        } catch (e: Exception) {
            Log.e(TAG, "Error acquiring wakelock", e)
        }

        // 2. Override Audio Manager to 100% Max ALARM stream volume
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        audioManager?.let { am ->
            try {
                originalVolume = am.getStreamVolume(AudioManager.STREAM_ALARM)
                val maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                am.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to force max volume", e)
            }
        }

        // 3. Start 2.4kHz acoustic siren
        soundSynthesizer.startSiren(tone)

        // 4. Start Morse Code SOS Vibration: ... --- ...
        startMorseSosVibration()

        // 5. Build High Priority Notification with Full Screen Intent
        val fullScreenIntent = Intent(this, EmergencyAlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_SOURCE, source)
            putExtra(EXTRA_PIN, pin)
            putExtra(EXTRA_LATENCY, latency)
            putExtra(EXTRA_PEAK_G, peakG)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            NOTIFICATION_ID,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val dismissIntent = Intent(this, EmergencyAlarmService::class.java).apply {
            action = ACTION_DISMISS_ALARM
        }
        val dismissPendingIntent = PendingIntent.getService(
            this,
            NOTIFICATION_ID + 1,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("CRITICAL HARDWARE TREMOR DETECTED!")
            .setContentText("ESP32 Pin GPIO $pin Triggered ($peakG G, Latency: ${latency}ms)")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(pendingIntent, true)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Silence Alarm", dismissPendingIntent)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        // Force launch full screen activity directly as well
        try {
            startActivity(fullScreenIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error directly launching EmergencyAlertActivity", e)
        }
    }

    private fun startMorseSosVibration() {
        val vibratorRef = vibrator ?: return
        // Morse SOS: S = 150 150 150; O = 400 150 400 150 400; S = 150 150 150
        val timings = longArrayOf(
            0,
            150, 150, 150, 150, 150, 350,       // S (...)
            400, 150, 400, 150, 400, 350,       // O (---)
            150, 150, 150, 150, 150, 1000      // S (...) and pause
        )
        val amplitudes = intArrayOf(
            0,
            255, 0, 255, 0, 255, 0,
            255, 0, 255, 0, 255, 0,
            255, 0, 255, 0, 255, 0
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(timings, amplitudes, 0)
                vibratorRef.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibratorRef.vibrate(timings, 0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing Morse SOS vibration", e)
        }
    }

    private fun stopEmergencyOverride() {
        soundSynthesizer.stopSiren()
        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling vibration", e)
        }

        // Restore volume if appropriate
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (originalVolume > 0) {
            try {
                audioManager?.setStreamVolume(AudioManager.STREAM_ALARM, originalVolume, 0)
            } catch (e: Exception) {
                Log.e(TAG, "Error restoring volume", e)
            }
        }

        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing wakelock", e)
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onDestroy() {
        stopEmergencyOverride()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Seismic Critical Emergency Override",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority unignorable alert for critical seismic tremors"
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
