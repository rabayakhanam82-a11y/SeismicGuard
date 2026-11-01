package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlin.math.sqrt

@Composable
fun NativeTectonicMapView(
    earthquakes: List<EarthquakeItem>,
    userLat: Double,
    userLon: Double,
    locationLabel: String,
    onRefresh: () -> Unit
) {
    // Zoom and pan state
    var scale by remember { mutableFloatStateOf(1.8f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    var selectedQuake by remember { mutableStateOf<EarthquakeItem?>(null) }
    var showNearMeDetails by remember { mutableStateOf(false) }

    // World & Regional Coordinate Projection Helper:
    // Longitude: -180 to 180, Latitude: -85 to 85
    fun geoToScreen(lat: Double, lon: Double, width: Float, height: Float): Offset {
        val x = ((lon + 180.0) / 360.0 * width).toFloat()
        val y = ((90.0 - lat) / 180.0 * height).toFloat()
        return Offset(
            (x - width / 2f) * scale + width / 2f + offsetX,
            (y - height / 2f) * scale + height / 2f + offsetY
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .testTag("native_tectonic_map_view")
    ) {
        // Interactive Canvas Layer
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.8f, 12f)
                        offsetX += pan.x
                        offsetY += pan.y
                    }
                }
                .pointerInput(earthquakes, scale, offsetX, offsetY) {
                    detectTapGestures { tapOffset ->
                        // Check if an earthquake marker was tapped
                        var clicked: EarthquakeItem? = null
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()

                        for (quake in earthquakes) {
                            val pos = geoToScreen(quake.latitude, quake.longitude, w, h)
                            val dist = sqrt((pos.x - tapOffset.x) * (pos.x - tapOffset.x) + (pos.y - tapOffset.y) * (pos.y - tapOffset.y))
                            val hitRadius = (quake.magnitude * 3.5f * (scale / 2f).coerceIn(1f, 3f)).toFloat().coerceAtLeast(24f)
                            if (dist <= hitRadius) {
                                clicked = quake
                                break
                            }
                        }
                        selectedQuake = clicked
                    }
                }
        ) {
            val w = size.width
            val h = size.height

            // 1. Ocean Background
            drawRect(color = Color(0xFF0F172A))

            // 2. Latitude & Longitude Grid Lines
            val gridColor = Color(0xFF1E293B)
            for (lon in -180..180 step 30) {
                val p1 = geoToScreen(85.0, lon.toDouble(), w, h)
                val p2 = geoToScreen(-85.0, lon.toDouble(), w, h)
                drawLine(color = gridColor, start = p1, end = p2, strokeWidth = 1f)
            }
            for (lat in -60..80 step 20) {
                val p1 = geoToScreen(lat.toDouble(), -180.0, w, h)
                val p2 = geoToScreen(lat.toDouble(), 180.0, w, h)
                drawLine(color = gridColor, start = p1, end = p2, strokeWidth = 1f)
            }

            // 3. Vector Continental Landmass Outlines (Smooth polygonal paths)
            val landColor = Color(0xFF1E293B)
            val landBorderColor = Color(0xFF334155)

            // Eurasian & Indian Subcontinent Landmass Polygon
            val asiaPath = Path().apply {
                val pts = listOf(
                    Pair(70.0, 30.0), Pair(72.0, 60.0), Pair(68.0, 90.0), Pair(60.0, 120.0),
                    Pair(50.0, 140.0), Pair(40.0, 130.0), Pair(35.0, 120.0), Pair(22.0, 115.0),
                    Pair(15.0, 105.0), Pair(10.0, 100.0), Pair(1.0, 104.0), Pair(6.0, 96.0),
                    Pair(16.0, 96.0), Pair(21.0, 92.0), Pair(21.5, 89.0), Pair(22.0, 89.5),
                    Pair(21.5, 87.0), Pair(17.0, 83.0), Pair(13.0, 80.0), Pair(8.0, 77.5),
                    Pair(12.0, 75.0), Pair(19.0, 73.0), Pair(24.0, 69.0), Pair(25.0, 62.0),
                    Pair(30.0, 50.0), Pair(38.0, 48.0), Pair(42.0, 30.0), Pair(55.0, 20.0),
                    Pair(65.0, 25.0)
                )
                pts.forEachIndexed { i, (lat, lon) ->
                    val screenPt = geoToScreen(lat, lon, w, h)
                    if (i == 0) moveTo(screenPt.x, screenPt.y) else lineTo(screenPt.x, screenPt.y)
                }
                close()
            }
            drawPath(path = asiaPath, color = landColor)
            drawPath(path = asiaPath, color = landBorderColor, style = Stroke(width = 1.5f))

            // Bangladesh Regional Detailed Boundary Polygon
            val bdPath = Path().apply {
                val bdPts = listOf(
                    Pair(26.6, 88.5), Pair(26.2, 89.8), Pair(25.2, 90.5), Pair(25.1, 92.0),
                    Pair(24.9, 92.5), Pair(23.8, 92.3), Pair(22.0, 92.6), Pair(21.2, 92.2),
                    Pair(20.7, 92.3), Pair(21.5, 91.9), Pair(22.0, 91.8), Pair(22.5, 91.3),
                    Pair(22.0, 90.5), Pair(21.8, 89.8), Pair(22.2, 89.1), Pair(24.0, 88.5),
                    Pair(25.0, 88.2), Pair(26.0, 88.1)
                )
                bdPts.forEachIndexed { i, (lat, lon) ->
                    val p = geoToScreen(lat, lon, w, h)
                    if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                }
                close()
            }
            drawPath(path = bdPath, color = Color(0xFF0F3A2E).copy(alpha = 0.5f))
            drawPath(path = bdPath, color = TelemetryEmerald.copy(alpha = 0.8f), style = Stroke(width = 2f))

            // 4. Bangladesh Major Tectonic Fault Lines (Vector Overlays)
            // Madhupur Fault Line
            val madhupurPts = listOf(Pair(24.8, 90.0), Pair(24.2, 90.3), Pair(23.8, 90.5))
            val madhupurPath = Path()
            madhupurPts.forEachIndexed { i, (lat, lon) ->
                val p = geoToScreen(lat, lon, w, h)
                if (i == 0) madhupurPath.moveTo(p.x, p.y) else madhupurPath.lineTo(p.x, p.y)
            }
            drawPath(path = madhupurPath, color = EmergencyCrimson, style = Stroke(width = 3.5f))

            // Dauki Fault Line
            val daukiPts = listOf(Pair(25.2, 90.0), Pair(25.18, 91.5), Pair(25.12, 92.5))
            val daukiPath = Path()
            daukiPts.forEachIndexed { i, (lat, lon) ->
                val p = geoToScreen(lat, lon, w, h)
                if (i == 0) daukiPath.moveTo(p.x, p.y) else daukiPath.lineTo(p.x, p.y)
            }
            drawPath(path = daukiPath, color = Color(0xFFFFB703), style = Stroke(width = 3f))

            // Chittagong-Tripura Fold Belt
            val chittagongPts = listOf(Pair(23.8, 92.2), Pair(22.8, 92.0), Pair(21.5, 92.2))
            val chittagongPath = Path()
            chittagongPts.forEachIndexed { i, (lat, lon) ->
                val p = geoToScreen(lat, lon, w, h)
                if (i == 0) chittagongPath.moveTo(p.x, p.y) else chittagongPath.lineTo(p.x, p.y)
            }
            drawPath(path = chittagongPath, color = Color(0xFF00B4D8), style = Stroke(width = 3f))

            // 5. Plot Live Earthquakes from USGS
            earthquakes.forEach { quake ->
                val pos = geoToScreen(quake.latitude, quake.longitude, w, h)
                if (pos.x in -100f..(w + 100f) && pos.y in -100f..(h + 100f)) {
                    val color = when {
                        quake.magnitude >= 5.5 -> EmergencyCrimson
                        quake.magnitude >= 4.0 -> Color(0xFFFFB703)
                        else -> TelemetryEmerald
                    }
                    val radius = (quake.magnitude * 2.2f * (scale / 2.5f).coerceIn(0.8f, 2.5f)).toFloat().coerceIn(5f, 24f)

                    // Outer pulse glow
                    drawCircle(color = color.copy(alpha = 0.35f), radius = radius * 1.6f, center = pos)
                    // Core epicenter circle
                    drawCircle(color = color, radius = radius, center = pos)
                    // Center white dot
                    drawCircle(color = Color.White, radius = 2.5f, center = pos)
                }
            }

            // 6. User Station Marker (Blue Glowing Dot)
            val userPos = geoToScreen(userLat, userLon, w, h)
            drawCircle(color = Color(0xFF00B4D8).copy(alpha = 0.35f), radius = 22f, center = userPos)
            drawCircle(color = Color(0xFF00B4D8), radius = 8f, center = userPos)
            drawCircle(color = Color.White, radius = 3f, center = userPos)
        }

        // Top Glass Navigation Bar
        Card(
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface.copy(alpha = 0.88f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Public, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = "LIVE TECTONIC MAP", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(text = "${earthquakes.size} Active Earthquakes (USGS Feed)", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        }
                    }

                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Location Jump Presets: strictly Bangladesh and Global View
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PresetButton(label = "Bangladesh", modifier = Modifier.weight(1f)) {
                        scale = 5.5f
                        offsetX = -40f
                        offsetY = -120f
                    }
                    PresetButton(label = "Global View", modifier = Modifier.weight(1f)) {
                        scale = 1.2f
                        offsetX = 0f
                        offsetY = 0f
                    }
                }
            }
        }

        // Floating Zoom Controls (+ / - / Reset)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = { scale = (scale * 1.35f).coerceAtMost(12f) },
                colors = IconButtonDefaults.iconButtonColors(containerColor = ObsidianSurface.copy(alpha = 0.9f)),
                modifier = Modifier.size(42.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Zoom In", tint = Color.White)
            }

            IconButton(
                onClick = { scale = (scale / 1.35f).coerceAtLeast(0.8f) },
                colors = IconButtonDefaults.iconButtonColors(containerColor = ObsidianSurface.copy(alpha = 0.9f)),
                modifier = Modifier.size(42.dp)
            ) {
                Icon(imageVector = Icons.Default.Remove, contentDescription = "Zoom Out", tint = Color.White)
            }

            IconButton(
                onClick = {
                    scale = 4.0f
                    offsetX = 0f
                    offsetY = 0f
                },
                colors = IconButtonDefaults.iconButtonColors(containerColor = ObsidianSurface.copy(alpha = 0.9f)),
                modifier = Modifier.size(42.dp)
            ) {
                Icon(imageVector = Icons.Default.MyLocation, contentDescription = "Recenter", tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
            }
        }

        // Bottom Map Legend & "Check Earthquakes Near Me" Button
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // "Are There Earthquakes Near Me?" Button
            Button(
                onClick = { showNearMeDetails = !showNearMeDetails },
                colors = ButtonDefaults.buttonColors(containerColor = EmergencyCrimson),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(imageVector = Icons.Default.LocationSearching, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (showNearMeDetails) "Hide Nearest Quake Calculation" else "Are There Earthquakes Near Me Right Now?",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Proximity Calculation Banner
            AnimatedVisibility(visible = showNearMeDetails) {
                val closest = earthquakes.minByOrNull { it.distanceKmFromUser }
                Card(
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "PROXIMITY RADAR: NEAREST EPICENTER",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        if (closest != null) {
                            Text(
                                text = "Epicenter: ${closest.place}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Distance: %.0f km away • Magnitude: M%.1f • Depth: %.1f km".format(
                                    closest.distanceKmFromUser,
                                    closest.magnitude,
                                    closest.depthKm
                                ),
                                fontSize = 12.sp,
                                color = Color(0xFFFFB703),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Map Legend Bar
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface.copy(alpha = 0.9f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MapLegendItem(color = Color(0xFF00B4D8), label = "Your Station")
                    MapLegendItem(color = EmergencyCrimson, label = "Madhupur Fault")
                    MapLegendItem(color = Color(0xFFFFB703), label = "Dauki Fault")
                    MapLegendItem(color = TelemetryEmerald, label = "Live Quakes")
                }
            }
        }

        // Tapped Earthquake Detail Popup
        if (selectedQuake != null) {
            val q = selectedQuake!!
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceVariant),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.Center)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = (if (q.magnitude >= 5.0) EmergencyCrimson else TelemetryEmerald).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "MAGNITUDE M%.1f".format(q.magnitude),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = if (q.magnitude >= 5.0) EmergencyCrimson else TelemetryEmerald,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        IconButton(
                            onClick = { selectedQuake = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = q.place, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Distance: %.0f km away from your station\nFocal Depth: %.1f km\nCoordinates: %.2f°N, %.2f°E".format(
                            q.distanceKmFromUser,
                            q.depthKm,
                            q.latitude,
                            q.longitude
                        ),
                        fontSize = 12.sp,
                        color = Color(0xFFCBD5E1),
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun PresetButton(label: String, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
        shape = RoundedCornerShape(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        modifier = modifier.height(32.dp)
    ) {
        Text(text = label, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun MapLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 10.sp, color = Color(0xFFCBD5E1))
    }
}
