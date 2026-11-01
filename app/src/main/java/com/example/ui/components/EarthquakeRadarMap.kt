package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EarthquakeItem
import com.example.ui.theme.EmergencyCrimson
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TelemetryEmerald

@Composable
fun EarthquakeRadarMap(
    earthquakes: List<EarthquakeItem>,
    userLat: Double,
    userLon: Double,
    onRefresh: () -> Unit
) {
    var selectedQuake by remember { mutableStateOf<EarthquakeItem?>(null) }
    var showProximityDialog by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "radarPulse")
    val radarSweepRadius by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepRadius"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("earthquake_radar_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = "Radar",
                        tint = Color(0xFF00B4D8),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "TECTONIC PROXIMITY RADAR",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "USGS 24H FEED",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00B4D8),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Canvas Radar Map rendering epicenters relative to user coordinates
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ObsidianBackground)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val maxRadius = size.width.coerceAtMost(size.height) / 2f * 0.95f

                    // Concentric Radar Distance Rings
                    drawCircle(
                        color = Color(0xFF1E293B),
                        radius = maxRadius,
                        center = center,
                        style = Stroke(width = 1.5f)
                    )
                    drawCircle(
                        color = Color(0xFF1E293B),
                        radius = maxRadius * 0.66f,
                        center = center,
                        style = Stroke(width = 1.2f)
                    )
                    drawCircle(
                        color = Color(0xFF1E293B),
                        radius = maxRadius * 0.33f,
                        center = center,
                        style = Stroke(width = 1.0f)
                    )

                    // Crosshairs
                    drawLine(
                        color = Color(0xFF2E3D52),
                        start = Offset(center.x - maxRadius, center.y),
                        end = Offset(center.x + maxRadius, center.y),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = Color(0xFF2E3D52),
                        start = Offset(center.x, center.y - maxRadius),
                        end = Offset(center.x, center.y + maxRadius),
                        strokeWidth = 1f
                    )

                    // Dynamic radar pulse wave
                    drawCircle(
                        color = Color(0xFF00B4D8).copy(alpha = 0.35f * (1f - radarSweepRadius)),
                        radius = maxRadius * radarSweepRadius,
                        center = center,
                        style = Stroke(width = 2.5f)
                    )

                    // User Location Center Dot
                    drawCircle(
                        color = Color(0xFF06D6A0),
                        radius = 6f,
                        center = center
                    )
                    drawCircle(
                        color = Color(0xFF06D6A0).copy(alpha = 0.4f),
                        radius = 12f,
                        center = center,
                        style = Stroke(width = 1.5f)
                    )

                    // Plot Earthquakes on Equirectangular projection relative to user position
                    // 1 degree lat/lon mapping scale
                    val scale = maxRadius / 15.0f // ~15 degrees max radar view

                    earthquakes.take(25).forEach { quake ->
                        val dx = (quake.longitude - userLon).toFloat() * scale
                        val dy = -(quake.latitude - userLat).toFloat() * scale
                        val point = Offset(center.x + dx, center.y + dy)

                        if ((point - center).getDistance() <= maxRadius) {
                            val color = when {
                                quake.magnitude >= 5.5 -> EmergencyCrimson
                                quake.magnitude >= 4.0 -> Color(0xFFFFB703)
                                else -> TelemetryEmerald
                            }
                            val pointRadius = (quake.magnitude * 2.0).toFloat().coerceIn(4f, 14f)

                            drawCircle(
                                color = color.copy(alpha = 0.7f),
                                radius = pointRadius,
                                center = point
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 2f,
                                center = point
                            )
                        }
                    }
                }

                // Radar Legend
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                ) {
                    Text("• Center: Your Station", fontSize = 10.sp, color = TelemetryEmerald, fontFamily = FontFamily.Monospace)
                    Text("• Outer Ring: ~1500 km", fontSize = 10.sp, color = Color(0xFF94A3B8), fontFamily = FontFamily.Monospace)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Prominent Interactive Action Button: "Are There Earthquakes Near Me Right Now?"
            Button(
                onClick = { showProximityDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = EmergencyCrimson),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("earthquakes_near_me_button")
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Are There Earthquakes Near Me Right Now?",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Proximity computation banner if clicked
            if (showProximityDialog) {
                Spacer(modifier = Modifier.height(10.dp))
                val closest = earthquakes.minByOrNull { it.distanceKmFromUser }

                Surface(
                    color = ObsidianSurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "GEOSPATIAL HAVERSINE CALCULATION",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00B4D8),
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "CLOSE [X]",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.clickable { showProximityDialog = false }
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        if (closest != null) {
                            Text(
                                text = "Closest Active Tremor: ${closest.place}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Distance: %.1f km from your coordinates • Mag: M%.1f • Depth: %.1f km".format(
                                    closest.distanceKmFromUser,
                                    closest.magnitude,
                                    closest.depthKm
                                ),
                                fontSize = 12.sp,
                                color = Color(0xFFFFB703),
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (closest.distanceKmFromUser < 200) "WARNING: Significant tectonic proximity! Local tremor vulnerability high." else "NOMINAL: Closest epicenter is outside immediate destructive radius.",
                                fontSize = 11.sp,
                                color = if (closest.distanceKmFromUser < 200) EmergencyCrimson else TelemetryEmerald,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
