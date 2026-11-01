package com.example.ui.components

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
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HardwareTelemetry
import com.example.ui.theme.EmergencyCrimson
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TelemetryEmerald

@Composable
fun LivePinInspector(telemetry: HardwareTelemetry) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("pin_inspector_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // MCU Hardware Overview Card
        Card(
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = "MCU",
                            tint = Color(0xFF00B4D8),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "ESP32-WROOM-32 (30-Pin NodeMCU)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    StatusPill(
                        label = if (telemetry.isConnected) "ONLINE" else "OFFLINE",
                        color = if (telemetry.isConnected) TelemetryEmerald else EmergencyCrimson
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Dual-Core Tensilica Xtensa 32-bit LX6 @ 240MHz • CP2102 USB Bridge • MB-102 Breadboard Bus",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricBadge(icon = Icons.Default.Wifi, label = "Wi-Fi RSSI", value = "${telemetry.wifiRssiDbm} dBm")
                    MetricBadge(icon = Icons.Default.Bolt, label = "Rail Bus", value = "${telemetry.operatingVoltageBus}V DC")
                    MetricBadge(icon = Icons.Default.Sensors, label = "Triggers", value = "${telemetry.triggerCount} hits")
                }
            }
        }

        // Section Title: Hardware Pin Mappings
        Text(
            text = "DIGITAL GPIO PIN ASSIGNMENTS & ACTIVE TELEMETRY",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF94A3B8),
            letterSpacing = 1.sp
        )

        // GPIO 18: SW-420 Sensor
        PinTelemetryCard(
            pinNumber = "GPIO 18",
            pinRole = "Digital Input (Internal Pull-Up)",
            componentName = "SW-420 Piezoelectric Vibration Sensor",
            circuitDetails = "Interfaced with LM393 voltage comparator • IRAM_ATTR edge interrupt trigger • 500ms software debounce",
            isHigh = telemetry.gpio18SensorState,
            activeColor = EmergencyCrimson,
            activeText = "TREMOR TRIGGERED",
            idleText = "STABLE / MONITORING",
            extraBadge = "Latency: ${telemetry.lastTriggerLatencyMs}ms"
        )

        // GPIO 23: BC547 NPN Transistor & 9042 Passive Buzzer
        PinTelemetryCard(
            pinNumber = "GPIO 23",
            pinRole = "Digital Output (Low-Side Transistor Switch)",
            componentName = "BC547 NPN & 9042 Passive Buzzer (85dBA)",
            circuitDetails = "1kΩ base current-limiting resistor • Isolates microcontroller logic from inductive acoustic transducer load",
            isHigh = telemetry.gpio23BuzzerState,
            activeColor = Color(0xFFFFB703),
            activeText = "2.4kHz SIREN PULSING",
            idleText = "QUIET / STANDBY",
            extraBadge = "Output: 85 dBA"
        )

        // Breadboard Power Distribution
        Card(
            colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceVariant),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "POWER DISTRIBUTION MATRIX (MB-102 BREADBOARD)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00B4D8)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "• 5V USB Input: Supplies VBUS power bank or regulated adapter input\n• 3.3V System Bus: Powering ESP32 3V3, SW-420 VCC, and pull-up bias\n• Ground Plane (GND): Common negative return for sensor and BC547 transistor emitter",
                    fontSize = 12.sp,
                    color = Color(0xFFE2E8F0),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun PinTelemetryCard(
    pinNumber: String,
    pinRole: String,
    componentName: String,
    circuitDetails: String,
    isHigh: Boolean,
    activeColor: Color,
    activeText: String,
    idleText: String,
    extraBadge: String
) {
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
                Column {
                    Text(
                        text = pinNumber,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = pinRole,
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isHigh) activeColor else Color(0xFF475569))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isHigh) activeText else idleText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isHigh) activeColor else Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = componentName,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFE2E8F0)
            )

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = circuitDetails,
                fontSize = 12.sp,
                color = Color(0xFF94A3B8),
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                color = ObsidianSurfaceVariant,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = extraBadge,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00B4D8),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun StatusPill(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun MetricBadge(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color(0xFF94A3B8),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontSize = 10.sp, color = Color(0xFF64748B))
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}
