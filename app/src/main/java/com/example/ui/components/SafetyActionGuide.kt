package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmergencyCrimson
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant

@Composable
fun SafetyActionGuide() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("safety_guide_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card: Drop, Cover, Hold
        Card(
            colors = CardDefaults.cardColors(containerColor = EmergencyCrimson.copy(alpha = 0.15f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.HealthAndSafety,
                        contentDescription = "Safety",
                        tint = EmergencyCrimson,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "CORE ACTION: DROP, COVER, HOLD ON",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                ProtocolStep("1. DROP down onto your hands and knees. This position protects you from being knocked over.")
                ProtocolStep("2. COVER your head and neck under a sturdy table or desk. If no shelter is nearby, crawl next to an interior wall.")
                ProtocolStep("3. HOLD ON to your shelter until the shaking stops. Be prepared to move with your shelter if it shifts.")
            }
        }

        // Indoor vs Outdoor Protocols
        Card(
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "CRITICAL SITUATIONAL INSTRUCTIONS",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00B4D8)
                )

                Spacer(modifier = Modifier.height(10.dp))
                InstructionRow("If Indoors:", "Stay inside! Do NOT run outside or use elevators. Avoid windows, hanging lights, and heavy cabinets.")
                InstructionRow("If in High-Rise:", "Expect fire sprinklers and alarms to activate. Move away from glass facades and take cover under desks.")
                InstructionRow("If Outdoors:", "Move to an open clearing away from electrical power lines, streetlamps, glass facades, and brick chimneys.")
                InstructionRow("If Driving:", "Pull over smoothly to a safe shoulder away from overpasses, bridges, and power lines. Keep emergency brake engaged.")
            }
        }

        // 72-Hour Emergency Supply Checklist
        Card(
            colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceVariant),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "72-HOUR EMERGENCY GO-BAG CHECKLIST",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF06D6A0)
                )
                Spacer(modifier = Modifier.height(10.dp))
                ChecklistItem("Water: 4 liters per person per day for hydration and sanitation")
                ChecklistItem("Food: 3-day supply of non-perishable high-calorie items")
                ChecklistItem("Lighting: High-lumen LED flashlight and extra AA/AAA batteries")
                ChecklistItem("Acoustic Whistle: To signal rescue teams trapped under debris")
                ChecklistItem("First Aid Kit: Sterile gauze, antiseptic, bandages, and burn dressings")
                ChecklistItem("Power Bank: 20,000mAh portable charger with USB cable for ESP32 & phone")
                ChecklistItem("Vital Documents: National ID, passport, property deeds in waterproof pouch")
            }
        }
    }
}

@Composable
private fun ProtocolStep(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = null,
            tint = Color(0xFFFFB703),
            modifier = Modifier
                .size(16.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, fontSize = 12.sp, color = Color(0xFFF1F5F9), lineHeight = 18.sp)
    }
}

@Composable
private fun InstructionRow(title: String, desc: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(text = desc, fontSize = 12.sp, color = Color(0xFF94A3B8), lineHeight = 16.sp)
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun ChecklistItem(item: String) {
    Row(
        modifier = Modifier.padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF06D6A0),
            modifier = Modifier
                .size(16.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = item, fontSize = 12.sp, color = Color(0xFFE2E8F0))
    }
}
