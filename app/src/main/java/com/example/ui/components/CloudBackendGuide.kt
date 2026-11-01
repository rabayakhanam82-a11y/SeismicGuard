package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.ui.theme.EmergencyCrimson
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TelemetryEmerald

@Composable
fun CloudBackendGuide(
    currentGateway: BackendGateway,
    onSelectGateway: (BackendGateway) -> Unit,
    onTestFirebase: (((Boolean, String) -> Unit) -> Unit)? = null
) {
    val context = LocalContext.current
    var isTestingFirebase by remember { mutableStateOf(false) }
    var testResultText by remember { mutableStateOf<String?>(null) }
    var testResultSuccess by remember { mutableStateOf(false) }

    val firebaseRulesJson = """
{
  "rules": {
    ".read": true,
    ".write": true
  }
}
    """.trimIndent()

    val esp32HttpSnippet = """
// ESP32 Arduino C++ (Compatible with Core v2.x and v3.x)
#include <WiFi.h>
#include <HTTPClient.h>
#include <WiFiClientSecure.h>

void sendTremorToFirebase(unsigned long latencyMs, float peakG) {
  if (WiFi.status() == WL_CONNECTED) {
    WiFiClientSecure client;
    client.setInsecure(); // Required for HTTPS without SSL cert management
    client.setTimeout(3000);

    HTTPClient http;
    const char* url = "https://seismic-guard-default-rtdb.asia-southeast1.firebasedatabase.app/telemetry.json";
    
    if (http.begin(client, url)) {
      http.addHeader("Content-Type", "application/json");
      String payload = "{\"event\":\"TREMOR_INTERRUPT\",\"gpio18\":true,\"latency_ms\":" + String(latencyMs) + ",\"peak_g\":" + String(peakG, 2) + ",\"timestamp\":" + String(millis()) + "}";
      int code = http.PUT(payload);
      http.end();
    }
  }
}
    """.trimIndent()

    val supabaseSql = """
-- Run this in your Supabase SQL Editor (100% Free Tier Alternative)
CREATE TABLE IF NOT EXISTS seismic_telemetry (
    id BIGSERIAL PRIMARY KEY,
    source TEXT DEFAULT 'ESP32_GPIO18',
    pin INT DEFAULT 18,
    latency_ms INT DEFAULT 12,
    peak_g FLOAT DEFAULT 3.8,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER PUBLICATION supabase_realtime ADD TABLE seismic_telemetry;
    """.trimIndent()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("cloud_guide_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Active Firebase Project Banner
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
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = TelemetryEmerald,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "FIREBASE PROJECT CONFIGURED",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Project: seismic-guard • Asia-Southeast1",
                                fontSize = 12.sp,
                                color = TelemetryEmerald
                            )
                        }
                    }

                    Surface(
                        color = TelemetryEmerald.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "READY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TelemetryEmerald,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Your real google-services.json and Firebase Realtime Database are configured. The app can stream live hardware tremors directly from your Asia-Southeast1 cloud database.",
                    fontSize = 12.sp,
                    color = Color(0xFFCBD5E1),
                    lineHeight = 17.sp
                )
            }
        }

        // 1. Google Firebase Realtime Database Card (Featured)
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
                    Text(
                        text = "1. Firebase Realtime Database (Active)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFB703)
                    )

                    Surface(
                        color = Color(0xFFFFB703).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "PRIMARY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFB703),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Database URL: https://seismic-guard-default-rtdb.asia-southeast1.firebasedatabase.app\n• Package Name: com.aistudio.seismicguard.omega\n• Project Number: 19503922262",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8),
                    lineHeight = 16.sp,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Test Live Firebase Ping
                Button(
                    onClick = {
                        isTestingFirebase = true
                        testResultText = null
                        onTestFirebase?.invoke { success, message ->
                            isTestingFirebase = false
                            testResultSuccess = success
                            testResultText = message
                        } ?: run {
                            isTestingFirebase = false
                            testResultSuccess = false
                            testResultText = "Test handler not attached"
                        }
                    },
                    enabled = !isTestingFirebase,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B4D8)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("test_firebase_ping_button")
                ) {
                    if (isTestingFirebase) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Writing to Firebase RTDB...", fontSize = 12.sp, color = Color.White)
                    } else {
                        Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Send Live Test Heartbeat to Firebase", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                testResultText?.let { msg ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = if (testResultSuccess) TelemetryEmerald.copy(alpha = 0.2f) else EmergencyCrimson.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = (if (testResultSuccess) "[OK] " else "[ERROR] ") + msg,
                            color = if (testResultSuccess) TelemetryEmerald else EmergencyCrimson,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Firebase Database Rules (Copy to Firebase Console -> Realtime Database -> Rules):",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = ObsidianBackground,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .padding(10.dp)
                            .horizontalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = firebaseRulesJson,
                            fontSize = 11.sp,
                            color = Color(0xFFFFB703),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Firebase_Rules", firebaseRulesJson))
                        Toast.makeText(context, "Rules JSON copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ObsidianSurfaceVariant),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Database Rules JSON", fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { onSelectGateway(BackendGateway.FIREBASE_RTDB) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentGateway == BackendGateway.FIREBASE_RTDB) TelemetryEmerald else Color(0xFF1E293B)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (currentGateway == BackendGateway.FIREBASE_RTDB) "CURRENTLY ACTIVE (Firebase RTDB)" else "Switch To Firebase Realtime Gateway",
                        color = if (currentGateway == BackendGateway.FIREBASE_RTDB) Color.Black else Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 2. ESP32 Direct Firebase Integration
        Card(
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "ESP32 Hardware -> Firebase HTTP Push",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Your ESP32 can send tremor events directly to your Firebase URL via a simple HTTP PUT. Full source code is available in 'ESP32 Firmware Source' in the developer drawer.",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = ObsidianBackground,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .padding(10.dp)
                            .horizontalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = esp32HttpSnippet,
                            fontSize = 11.sp,
                            color = Color(0xFF38BDF8),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // 3. Alternative Cloud Gateways (Supabase & Public MQTT)
        Card(
            colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceVariant),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Alternative: Supabase & Public MQTT",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00B4D8)
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "If you ever need an alternative zero-setup gateway without authentication:\n• HiveMQ Public MQTT: broker.hivemq.com (Port 1883)\n• Topic: team_omega/seismicguard/telemetry\n• Supabase Free PostgreSQL: SQL snippet available",
                    fontSize = 12.sp,
                    color = Color(0xFFCBD5E1),
                    lineHeight = 18.sp
                )
            }
        }
    }
}
