package com.example.emergency

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

class SoundSynthesizer {
    private var audioTrack: AudioTrack? = null
    private var synthesisJob: Job? = null
    @Volatile
    private var isPlaying = false

    companion object {
        private const val TAG = "SoundSynthesizer"
        private const val SAMPLE_RATE = 44100
        const val TONE_2400HZ_PULSE = "2400HZ_PULSE"
        const val TONE_DUAL_SWEEP = "DUAL_SWEEP"
        const val TONE_MORSE_SOS = "MORSE_SOS"
    }

    fun startSiren(toneProfile: String = TONE_2400HZ_PULSE) {
        if (isPlaying) return
        isPlaying = true

        val bufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ) * 2

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val audioFormat = AudioFormat.Builder()
            .setSampleRate(SAMPLE_RATE)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()

        try {
            audioTrack = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AudioTrack.Builder()
                    .setAudioAttributes(audioAttributes)
                    .setAudioFormat(audioFormat)
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                AudioTrack(
                    AudioManager.STREAM_ALARM,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize,
                    AudioTrack.MODE_STREAM
                )
            }

            audioTrack?.play()

            synthesisJob = CoroutineScope(Dispatchers.Default).launch {
                val numSamples = SAMPLE_RATE / 10 // 100ms chunk
                val generatedPcm = ShortArray(numSamples)
                var phase = 0.0

                var currentFreq = 2400.0
                var step = 0

                while (isActive && isPlaying) {
                    when (toneProfile) {
                        TONE_2400HZ_PULSE -> {
                            // 9042 Passive Buzzer: 2.4kHz high-frequency pulsed burst (150ms on, 80ms off)
                            val isPulseOn = (step % 4) < 3
                            val freq = if (isPulseOn) 2400.0 else 0.0
                            for (i in 0 until numSamples) {
                                if (freq > 0) {
                                    phase += 2.0 * PI * freq / SAMPLE_RATE
                                    if (phase > 2.0 * PI) phase -= 2.0 * PI
                                    // High harmonic square wave emulation for 85dBA buzzer
                                    val sample = if (sin(phase) >= 0.0) Short.MAX_VALUE else Short.MIN_VALUE
                                    generatedPcm[i] = (sample * 0.85).toInt().toShort()
                                } else {
                                    generatedPcm[i] = 0
                                }
                            }
                        }
                        TONE_DUAL_SWEEP -> {
                            // Dual siren sweep 1200Hz to 2800Hz
                            currentFreq = 1200.0 + ((step % 20) * 80.0)
                            for (i in 0 until numSamples) {
                                phase += 2.0 * PI * currentFreq / SAMPLE_RATE
                                if (phase > 2.0 * PI) phase -= 2.0 * PI
                                generatedPcm[i] = (sin(phase) * Short.MAX_VALUE * 0.9).toInt().toShort()
                            }
                        }
                        else -> { // MORSE_SOS
                            val sosPattern = booleanArrayOf(
                                true, false, true, false, true, false, false, // S: ...
                                true, true, false, true, true, false, true, true, false, false, // O: ---
                                true, false, true, false, true, false, false, false // S: ...
                            )
                            val activeSlot = sosPattern[step % sosPattern.size]
                            for (i in 0 until numSamples) {
                                if (activeSlot) {
                                    phase += 2.0 * PI * 2400.0 / SAMPLE_RATE
                                    if (phase > 2.0 * PI) phase -= 2.0 * PI
                                    generatedPcm[i] = (sin(phase) * Short.MAX_VALUE * 0.9).toInt().toShort()
                                } else {
                                    generatedPcm[i] = 0
                                }
                            }
                        }
                    }

                    audioTrack?.write(generatedPcm, 0, numSamples)
                    step++
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "AudioTrack synthesis error", e)
        }
    }

    fun stopSiren() {
        isPlaying = false
        synthesisJob?.cancel()
        synthesisJob = null
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            Log.e(TAG, "AudioTrack release error", e)
        }
        audioTrack = null
    }
}
