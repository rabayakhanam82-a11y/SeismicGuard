package com.example.ui.components

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.data.model.WeatherSkyTheme
import com.example.emergency.SoundSynthesizer
import com.example.ui.theme.EmergencyCrimson
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TelemetryEmerald

@Composable
fun SettingsPanel(
    telemetry: HardwareTelemetry,
    selectedTone: String,
    selectedSkyTheme: WeatherSkyTheme? = null,
    onSelectSkyTheme: (WeatherSkyTheme?) -> Unit = {},
    isDeveloperMode: Boolean,
    onToggleDeveloperMode: () -> Unit,
    onGatewaySelected: (BackendGateway) -> Unit,
    onTestEmergencyAlert: () -> Unit,
    onSensitivityChanged: (Int) -> Unit,
    onDebounceChanged: (Int) -> Unit,
    onToneSelected: (String) -> Unit
) {
    val context = LocalContext.current

    val canDrawOverlays = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Settings.canDrawOverlays(context)
    } else true

    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    val isDndAllowed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        notificationManager?.isNotificationPolicyAccessGranted == true
    } else true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .testTag("settings_panel"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "SETTINGS & PREFERENCES",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Developer Mode Master Switch Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isDeveloperMode) Color(0xFF1E293B) else ObsidianSurface
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = null,
                        tint = if (isDeveloperMode) Color(0xFFFFB703) else Color(0xFF94A3B8),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Developer Mode",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (isDeveloperMode)
                                "Unlocked: Pin inspector, alert simulator, ESP32 calibration & cloud guides."
                            else
                                "Turn on to reveal hardware diagnostics, test overlay alerts & ESP32 tools.",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            lineHeight = 15.sp
                        )
                    }
                }

                Switch(
                    checked = isDeveloperMode,
                    onCheckedChange = { onToggleDeveloperMode() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFFFFB703),
                        checkedTrackColor = Color(0xFF334155)
                    )
                )
            }
        }

        // When Developer Mode is ON -> Show Advanced Developer Tools
        if (isDeveloperMode) {
            // Live Presentation Test Trigger
            Card(
                colors = CardDefaults.cardColors(containerColor = EmergencyCrimson.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "TEST EMERGENCY OVERRIDE & OVERLAY",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF4D6D),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Launches the high-priority lockscreen-bypassing strobe activity and 2.4kHz acoustic alarm for demonstrations.",
                        fontSize = 11.sp,
                        color = Color(0xFFCBD5E1)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onTestEmergencyAlert,
                        colors = ButtonDefaults.buttonColors(containerColor = EmergencyCrimson),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "TRIGGER TEST EMERGENCY ALARM", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // Backend Gateway Selector
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "TELEMETRY GATEWAY",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    BackendGateway.values().forEach { gw ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = telemetry.gateway == gw,
                                onClick = { onGatewaySelected(gw) },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF38BDF8))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(text = gw.displayName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(text = gw.description, fontSize = 11.sp, color = Color(0xFF94A3B8))
                            }
                        }
                    }
                }
            }

            // SW-420 Potentiometer & Debounce Calibration
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Tune, contentDescription = null, tint = Color(0xFFFFB703), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "SW-420 HARDWARE CALIBRATION", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFB703))
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Sensitivity Threshold:", fontSize = 12.sp, color = Color(0xFF94A3B8))
                        Text(text = "${telemetry.potentiometerSensitivityPct}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
                    }
                    Slider(
                        value = telemetry.potentiometerSensitivityPct.toFloat(),
                        onValueChange = { onSensitivityChanged(it.toInt()) },
                        valueRange = 10f..100f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFFFB703), activeTrackColor = Color(0xFFFFB703))
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Debounce Window:", fontSize = 12.sp, color = Color(0xFF94A3B8))
                        Text(text = "${telemetry.sensorDebounceMs} ms", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
                    }
                    Slider(
                        value = telemetry.sensorDebounceMs.toFloat(),
                        onValueChange = { onDebounceChanged(it.toInt()) },
                        valueRange = 100f..1500f,
                        steps = 13,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF38BDF8), activeTrackColor = Color(0xFF38BDF8))
                    )
                }
            }
        }

        // Consumer Friendly Settings (Available to Everyone)
        // Atmosphere & Sky Theme (Auto by default)
        Card(
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ATMOSPHERE & SKY THEME",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8),
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Controls the dynamic weather visual backdrop. Auto synchronizes with live weather conditions.",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Option: Auto (Live Weather) - Default
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectSkyTheme(null) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedSkyTheme == null,
                        onClick = { onSelectSkyTheme(null) },
                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF38BDF8))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = if (selectedSkyTheme == null) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Auto (Live Weather) [Default]",
                            fontSize = 13.sp,
                            fontWeight = if (selectedSkyTheme == null) FontWeight.Bold else FontWeight.Normal,
                            color = Color.White
                        )
                        Text(
                            text = "Detects sunny, cloudy, rain, and night automatically from Open-Meteo",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                listOf(
                    Triple(WeatherSkyTheme.CLEAR_DAY, "Sunny & Clear", Icons.Default.WbSunny),
                    Triple(WeatherSkyTheme.CLOUDY_DAY, "Partly Cloudy", Icons.Default.Cloud),
                    Triple(WeatherSkyTheme.CLEAR_NIGHT, "Starry Night", Icons.Default.DarkMode),
                    Triple(WeatherSkyTheme.RAINY, "Rain & Showers", Icons.Default.WaterDrop),
                    Triple(WeatherSkyTheme.THUNDERSTORM, "Storm & High Alert", Icons.Default.Bolt)
                ).forEach { (theme, label, icon) ->
                    val isSelected = selectedSkyTheme == theme
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectSkyTheme(theme) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelectSkyTheme(theme) },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF38BDF8))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Alarm Tone Selector
        Card(
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "EMERGENCY ALARM SOUND PROFILE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                listOf(
                    SoundSynthesizer.TONE_2400HZ_PULSE to "2.4kHz Pulsing Buzzer (Standard)",
                    SoundSynthesizer.TONE_DUAL_SWEEP to "Dual-Tone Rising Siren",
                    SoundSynthesizer.TONE_MORSE_SOS to "SOS Morse Code Acoustic Beacon"
                ).forEach { (key, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedTone == key,
                            onClick = { onToneSelected(key) },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF38BDF8))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = label, fontSize = 13.sp, color = Color.White)
                    }
                }
            }
        }



        // Permission Auditor
        Card(
            colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceVariant),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = TelemetryEmerald, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "ALERT PERMISSION AUDITOR", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TelemetryEmerald)
                }

                Spacer(modifier = Modifier.height(10.dp))

                PermissionAuditRow(
                    label = "System Overlay (Draw Over Apps)",
                    isGranted = canDrawOverlays,
                    onGrant = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        }
                    }
                )

                PermissionAuditRow(
                    label = "Do Not Disturb (DND) Bypass",
                    isGranted = isDndAllowed,
                    onGrant = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                            context.startActivity(intent)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun PermissionAuditRow(label: String, isGranted: Boolean, onGrant: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Text(
                text = if (isGranted) "Authorized" else "Needs Permission",
                fontSize = 10.sp,
                color = if (isGranted) TelemetryEmerald else EmergencyCrimson
            )
        }

        if (isGranted) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Granted",
                tint = TelemetryEmerald,
                modifier = Modifier.size(20.dp)
            )
        } else {
            OutlinedButton(
                onClick = onGrant,
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFB703))
            ) {
                Text(text = "Grant", fontSize = 10.sp)
            }
        }
    }
}
