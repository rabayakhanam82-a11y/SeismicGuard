package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BackendGateway
import com.example.data.model.HardwareTelemetry
import com.example.ui.theme.EmergencyCrimson
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TelemetryEmerald

@Composable
fun DeveloperSuiteView(
    telemetry: HardwareTelemetry,
    onTestEmergencyAlert: () -> Unit,
    onTestBuzzer: () -> Unit,
    onOpenConnectWizard: () -> Unit,
    onOpenCloudGuide: () -> Unit,
    onOpenFirmware: () -> Unit,
    onOpenCredits: () -> Unit,
    onSensitivityChanged: (Int) -> Unit,
    onDebounceChanged: (Int) -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("developer_suite_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Dev Suite Header
        Card(
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Build, contentDescription = null, tint = Color(0xFFFFB703), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = "DEVELOPER & TEST CONSOLE", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(text = "Team Omega Hardware Diagnostics", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        }
                    }

                    Surface(
                        color = Color(0xFFFFB703).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "DEV ACTIVE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFFB703),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Welcome to the developer lab. All engineering configurations, hardware testing overlays, ESP32 calibration sliders, and C++ firmware tools are centralized here to keep the consumer experience clean.",
                    fontSize = 12.sp,
                    color = Color(0xFFCBD5E1),
                    lineHeight = 16.sp
                )
            }
        }

        // 1. Interactive Alert & Overlay Simulator
        Card(
            colors = CardDefaults.cardColors(containerColor = EmergencyCrimson.copy(alpha = 0.15f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "EMERGENCY OVERRIDE & OVERLAY SIMULATOR",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF4D6D),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Trigger real device overrides to test screen wakeups, lockscreen bypassing, high-priority notifications, and 2.4kHz acoustic buzzer synthesis.",
                    fontSize = 11.sp,
                    color = Color(0xFFCBD5E1)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        Toast.makeText(context, "Executing full-screen critical override...", Toast.LENGTH_SHORT).show()
                        onTestEmergencyAlert()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmergencyCrimson),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "SIMULATE FULL EMERGENCY OVERRIDE", fontWeight = FontWeight.Black, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onTestBuzzer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, tint = Color(0xFFFFB703), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Test Acoustic Alarm Buzzer (GPIO 23)", fontSize = 12.sp, color = Color.White)
                }
            }
        }

        // 2. Hardware Live Pin Status & Calibration
        Card(
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Sensors, contentDescription = null, tint = Color(0xFF00B4D8), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "ESP32 PIN TELEMETRY & CALIBRATION", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        DevPinPill("GPIO 18 (SW-420 Sensor)", telemetry.gpio18SensorState)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        DevPinPill("GPIO 23 (BC547 Buzzer)", telemetry.gpio23BuzzerState)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "SW-420 Potentiometer Limit:", fontSize = 12.sp, color = Color(0xFF94A3B8))
                    Text(text = "${telemetry.potentiometerSensitivityPct}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
                }
                Slider(
                    value = telemetry.potentiometerSensitivityPct.toFloat(),
                    onValueChange = { onSensitivityChanged(it.toInt()) },
                    valueRange = 10f..100f,
                    colors = SliderDefaults.colors(thumbColor = EmergencyCrimson, activeTrackColor = EmergencyCrimson)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Interrupt Debounce Window:", fontSize = 12.sp, color = Color(0xFF94A3B8))
                    Text(text = "${telemetry.sensorDebounceMs} ms", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
                }
                Slider(
                    value = telemetry.sensorDebounceMs.toFloat(),
                    onValueChange = { onDebounceChanged(it.toInt()) },
                    valueRange = 100f..1500f,
                    steps = 13,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF00B4D8), activeTrackColor = Color(0xFF00B4D8))
                )
            }
        }

        // 3. Quick Navigation to Specialized Engineering Tools
        Text(
            text = "HARDWARE TOOLS & CLOUD GUIDES",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF94A3B8),
            letterSpacing = 1.sp
        )

        DevNavTile(
            icon = Icons.Default.DeviceHub,
            title = "ESP32 Wi-Fi Connection Wizard",
            subtitle = "IP configuration, hotspot setup, and breadboard pinout",
            onClick = onOpenConnectWizard
        )

        DevNavTile(
            icon = Icons.Default.Cloud,
            title = "Cloud & Firebase Architecture",
            subtitle = "Supabase PostgreSQL, Firebase Firestore & public MQTT guides",
            onClick = onOpenCloudGuide
        )

        DevNavTile(
            icon = Icons.Default.Code,
            title = "Arduino C++ Firmware Reference",
            subtitle = "View and copy production sketch with IRAM_ATTR interrupt code",
            onClick = onOpenFirmware
        )

        DevNavTile(
            icon = Icons.Default.Bolt,
            title = "Team Omega Credits & Blueprint",
            subtitle = "Abdullah Karim, Mahir, Behan, Ahmad, Zarif roster",
            onClick = onOpenCredits
        )
    }
}

@Composable
fun DevPinPill(label: String, isActive: Boolean) {
    Surface(
        color = if (isActive) EmergencyCrimson else ObsidianSurfaceVariant,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(if (isActive) Color.White else Color(0xFF475569), CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isActive) Color.White else Color(0xFF94A3B8),
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun DevNavTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(text = subtitle, fontSize = 11.sp, color = Color(0xFF94A3B8))
                }
            }

            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(text = "Open", fontSize = 11.sp, color = Color.White)
            }
        }
    }
}
