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
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BmdStationData
import com.example.ui.theme.EmergencyCrimson
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TelemetryEmerald

@Composable
fun BmdRegionalHub(
    stations: List<BmdStationData>,
    onSelectLocation: (Double, Double, String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("bmd_hub_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Agency Header
        Card(
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = "BMD",
                        tint = Color(0xFF00B4D8),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "BANGLADESH METEOROLOGICAL DEPT.",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "Agargaon Meteorological Complex, Dhaka-1207",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Specialized national monitoring console tracking monsoon depressions, Bay of Bengal tropical storm vectors, and active structural tectonic fault lines across Bangladesh.",
                    fontSize = 12.sp,
                    color = Color(0xFFCBD5E1),
                    lineHeight = 18.sp
                )
            }
        }

        // Active Fault Lines Summary
        Card(
            colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceVariant),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Fault lines",
                        tint = Color(0xFFFFB703),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MAJOR BANGLADESH FAULT SYSTEMS",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFB703)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                FaultLineItem(
                    name = "Madhupur Tract Fault",
                    location = "Dhaka / Gazipur / Tangail Corridor",
                    risk = "Potential M7.0+ shallow event threat to capital region"
                )
                FaultLineItem(
                    name = "Dauki Fault Line",
                    location = "Meghalaya Border / Sylhet Basin",
                    risk = "Major tectonic subduction boundary, historically active"
                )
                FaultLineItem(
                    name = "Chittagong-Tripura Fold Belt",
                    location = "Southeastern Hill Tracts & Coastal Arc",
                    risk = "Complex compression zone capable of undersea tsunamigenic tremors"
                )
            }
        }

        // Regional Stations
        Text(
            text = "REGIONAL OBSERVATORY DIVISIONS",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF94A3B8),
            letterSpacing = 1.sp
        )

        stations.forEach { station ->
            StationCard(
                station = station,
                onCenterMap = {
                    when (station.division) {
                        "Dhaka Division" -> onSelectLocation(23.8103, 90.4125, "Gazipur / Dhaka (BMD)")
                        "Sylhet Division" -> onSelectLocation(24.8949, 91.8687, "Sylhet Sadar (BMD)")
                        "Chittagong Division" -> onSelectLocation(22.3569, 91.7832, "Chittagong Port (BMD)")
                        else -> onSelectLocation(24.3745, 88.6042, "Rajshahi (BMD)")
                    }
                }
            )
        }
    }
}

@Composable
fun StationCard(station: BmdStationData, onCenterMap: () -> Unit) {
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
                        text = station.stationCity,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = station.division,
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                Surface(
                    color = if (station.alertLevel == "NOMINAL") TelemetryEmerald.copy(alpha = 0.2f) else EmergencyCrimson.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = station.alertLevel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (station.alertLevel == "NOMINAL") TelemetryEmerald else EmergencyCrimson,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Thermostat, contentDescription = null, tint = Color(0xFF00B4D8), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = station.currentTemp, fontSize = 12.sp, color = Color.White)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.WaterDrop, contentDescription = null, tint = Color(0xFF06D6A0), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "24h Rain: ${station.rainfallMm24h}", fontSize = 12.sp, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tectonic Zone: ${station.tectonicZone}",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFFFB703)
            )
            Text(
                text = "Building Code Seismic Rating: ${station.seismicZoneRating}",
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
            )

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onCenterMap,
                colors = ButtonDefaults.buttonColors(containerColor = ObsidianSurfaceVariant),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Focus Proximity Radar On This Station", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun FaultLineItem(name: String, location: String, risk: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
        Text(text = location, fontSize = 11.sp, color = Color(0xFF00B4D8))
        Text(text = risk, fontSize = 11.sp, color = Color(0xFF94A3B8))
        Spacer(modifier = Modifier.height(6.dp))
    }
}
