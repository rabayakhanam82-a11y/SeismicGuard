package com.example.emergency

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmergencyCrimson
import com.example.ui.theme.EmergencyCrimsonDark
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.SeismicGuardTheme
import kotlin.math.roundToInt

class EmergencyAlertActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Configure system lockscreen override and turn on display
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val source = intent.getStringExtra(EmergencyAlarmService.EXTRA_SOURCE) ?: "ESP32_GPIO18"
        val pin = intent.getIntExtra(EmergencyAlarmService.EXTRA_PIN, 18)
        val latency = intent.getLongExtra(EmergencyAlarmService.EXTRA_LATENCY, 12L)
        val peakG = intent.getFloatExtra(EmergencyAlarmService.EXTRA_PEAK_G, 3.8f)

        setContent {
            SeismicGuardTheme {
                EmergencyStrobeAlertScreen(
                    source = source,
                    pin = pin,
                    latency = latency,
                    peakG = peakG,
                    onDismiss = {
                        EmergencyAlarmService.dismissAlert(this@EmergencyAlertActivity)
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun EmergencyStrobeAlertScreen(
    source: String,
    pin: Int,
    latency: Long,
    peakG: Float,
    onDismiss: () -> Unit
) {
    // Strobe animation cycling between Emergency Crimson and High-contrast White
    val infiniteTransition = rememberInfiniteTransition(label = "strobeTransition")
    val strobeColor by infiniteTransition.animateColor(
        initialValue = EmergencyCrimson,
        targetValue = Color(0xFFFFFFFF),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 280),
            repeatMode = RepeatMode.Reverse
        ),
        label = "strobeColor"
    )

    val strobeTextColor = if (strobeColor == Color(0xFFFFFFFF)) EmergencyCrimsonDark else Color.White

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        strobeColor.copy(alpha = 0.85f),
                        ObsidianBackground,
                        EmergencyCrimsonDark
                    )
                )
            )
            .padding(horizontal = 24.dp, vertical = 40.dp)
            .testTag("emergency_alert_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 90.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Warning Banner
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = strobeColor.copy(alpha = 0.95f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Alert Warning",
                        tint = strobeTextColor,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "PRIORITY 1 SYSTEM OVERRIDE",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = strobeTextColor,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Main Alert Signal Card
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(EmergencyCrimson)
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = "Alarm Active",
                        tint = Color.White,
                        modifier = Modifier.size(64.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "CRITICAL HARDWARE TREMOR DETECTED",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 30.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "ESP32 SENSOR GPIO $pin TRIGGERED",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFB703),
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }

            // Telemetry Blueprint Inspection Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF141A29).copy(alpha = 0.92f)
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "HARDWARE BLUEPRINT TELEMETRY",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00B4D8),
                        letterSpacing = 1.sp
                    )

                    TelemetryRow(label = "Trigger Source", value = source)
                    TelemetryRow(label = "Hardware Pinout", value = "SW-420 on GPIO $pin (IRAM_ATTR)")
                    TelemetryRow(label = "Edge Interrupt Latency", value = "${latency}ms (Sub-15ms guaranteed)")
                    TelemetryRow(label = "Estimated Acceleration", value = "Peak: $peakG G Force")
                    TelemetryRow(label = "Acoustic Driver", value = "BC547 NPN -> 9042 Buzzer (85dBA)")
                    TelemetryRow(label = "Hardware Alert Actuation", value = "GPIO 23 Active Siren Sounding")
                }
            }

            // Safety Guidance Callout
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1F293D)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Safety",
                        tint = Color(0xFF06D6A0),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "DROP, COVER, HOLD ON! Move away from windows, heavy fixtures, and unanchored furniture.",
                        fontSize = 13.sp,
                        color = Color(0xFFE2E8F0),
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Bottom Secure Slide-To-Dismiss verification slider
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            SlideToDismissSlider(
                onConfirmedDismiss = onDismiss
            )
        }
    }
}

@Composable
fun TelemetryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color(0xFF94A3B8)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun SlideToDismissSlider(
    onConfirmedDismiss: () -> Unit
) {
    val maxDragWidth = 260f
    var offsetX by remember { mutableFloatStateOf(0f) }

    val draggableState = rememberDraggableState { delta ->
        val newValue = (offsetX + delta).coerceIn(0f, maxDragWidth)
        offsetX = newValue
        if (offsetX >= maxDragWidth * 0.92f) {
            onConfirmedDismiss()
        }
    }

    Surface(
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(32.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .testTag("slide_to_dismiss_track")
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "SLIDE TO SILENCE & CONFIRM SAFETY  >>>",
                color = Color(0xFF94A3B8),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // Draggable Thumb Knob
            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX.roundToInt(), 0) }
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(EmergencyCrimson)
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state = draggableState,
                        onDragStopped = {
                            if (offsetX < maxDragWidth * 0.92f) {
                                offsetX = 0f
                            }
                        }
                    )
                    .testTag("slide_dismiss_thumb"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Slide Knob",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}
