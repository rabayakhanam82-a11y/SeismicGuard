package com.example.ui.components

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.EarthquakeItem
import com.example.ui.theme.EmergencyCrimson
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TelemetryEmerald

private class AndroidMapBridge(
    val onQuakeSelected: (String, Double, String, Double) -> Unit
) {
    @JavascriptInterface
    fun onQuakeSelected(id: String, mag: Double, place: String, dist: Double) {
        onQuakeSelected.invoke(id, mag, place, dist)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WorldTectonicMapView(
    earthquakes: List<EarthquakeItem>,
    userLat: Double,
    userLon: Double,
    locationLabel: String,
    isOffline: Boolean,
    onRefresh: () -> Unit
) {
    var activeMode by remember { mutableStateOf("bangladesh") } // "bangladesh" or "global"
    var activeLayer by remember { mutableStateOf("google_hybrid") } // "google_hybrid", "google_streets", "osm"
    var selectedQuake by remember { mutableStateOf<EarthquakeItem?>(null) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isMapLoaded by remember { mutableStateOf(false) }

    fun formatQuakesJson(list: List<EarthquakeItem>): String {
        val sb = StringBuilder("[")
        list.forEachIndexed { i, q ->
            if (i > 0) sb.append(",")
            val esc = q.place.replace("\"", "\\\"").replace("'", "\\'")
            sb.append("{\"id\":\"${q.id}\",\"lat\":${q.latitude},\"lon\":${q.longitude},\"mag\":${q.magnitude},\"place\":\"$esc\",\"dist\":${q.distanceKmFromUser},\"depth\":${q.depthKm}}")
        }
        sb.append("]")
        return sb.toString()
    }

    fun syncMapData(wv: WebView?) {
        if (wv != null && isMapLoaded) {
            val json = formatQuakesJson(earthquakes)
            wv.evaluateJavascript("renderEarthquakes($json); updateUserStation($userLat, $userLon, '$locationLabel');", null)
        }
    }

    LaunchedEffect(earthquakes, userLat, userLon, isMapLoaded) {
        syncMapData(webViewRef)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("world_tectonic_map_view")
    ) {
        // Real Google Maps & OpenStreetMap GIS Tiles via Leaflet Engine
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.databaseEnabled = true
                    settings.allowFileAccess = true
                    settings.allowContentAccess = true
                    settings.setSupportZoom(true)
                    settings.builtInZoomControls = false
                    settings.displayZoomControls = false
                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isMapLoaded = true
                            webViewRef = view
                            // Fly to Bangladesh by default
                            view?.evaluateJavascript("flyToPreset($userLat, $userLon, 7.5);", null)
                            syncMapData(view)
                        }
                    }

                    addJavascriptInterface(
                        AndroidMapBridge { id, _, _, _ ->
                            val found = earthquakes.find { it.id == id }
                            if (found != null) {
                                selectedQuake = found
                            }
                        },
                        "AndroidBridge"
                    )

                    loadUrl("file:///android_asset/map/map.html")
                    webViewRef = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top Floating Control Card: Strictly Bangladesh & Global View
        Card(
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface.copy(alpha = 0.94f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .align(Alignment.TopCenter)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Header with Quake Count & Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = EmergencyCrimson.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "TECTONIC GIS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = EmergencyCrimson,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${earthquakes.size} Live Quakes",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = if (isOffline) Color(0xFFFFB703).copy(alpha = 0.2f) else TelemetryEmerald.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (isOffline) "OFFLINE ATLAS" else "GOOGLE MAPS LIVE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isOffline) Color(0xFFFFB703) else TelemetryEmerald,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Strictly 2 Options: Bangladesh and Global View
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            activeMode = "bangladesh"
                            webViewRef?.evaluateJavascript("flyToPreset($userLat, $userLon, 7.5);", null)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeMode == "bangladesh") Color(0xFF00B4D8) else Color(0xFF1E293B)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = if (activeMode == "bangladesh") Color.Black else Color(0xFF38BDF8)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Bangladesh",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeMode == "bangladesh") Color.Black else Color.White
                            )
                        }
                    }

                    Button(
                        onClick = {
                            activeMode = "global"
                            webViewRef?.evaluateJavascript("flyToPreset(20.0, 15.0, 2);", null)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeMode == "global") Color(0xFF00B4D8) else Color(0xFF1E293B)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = if (activeMode == "global") Color.Black else Color(0xFF38BDF8)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Global View",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeMode == "global") Color.Black else Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Map Style Switcher (Satellite / Streets / OpenStreetMap)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Layers, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                    Text(text = "Layer:", fontSize = 10.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)

                    listOf(
                        Triple("google_hybrid", "Satellite", "Google Hybrid"),
                        Triple("google_streets", "Streets", "Google Roads"),
                        Triple("osm", "OpenStreetMap", "OpenStreetMap")
                    ).forEach { (layerKey, label, _) ->
                        val isSelected = activeLayer == layerKey
                        Surface(
                            color = if (isSelected) Color(0xFF38BDF8).copy(alpha = 0.35f) else Color(0xFF1E293B),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    activeLayer = layerKey
                                    webViewRef?.evaluateJavascript("setTileLayer('$layerKey');", null)
                                }
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Floating Zoom and Location Controls (+ / - / Recenter)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = { webViewRef?.evaluateJavascript("map.zoomIn();", null) },
                colors = IconButtonDefaults.iconButtonColors(containerColor = ObsidianSurface.copy(alpha = 0.92f)),
                modifier = Modifier.size(42.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Zoom In", tint = Color.White)
            }

            IconButton(
                onClick = { webViewRef?.evaluateJavascript("map.zoomOut();", null) },
                colors = IconButtonDefaults.iconButtonColors(containerColor = ObsidianSurface.copy(alpha = 0.92f)),
                modifier = Modifier.size(42.dp)
            ) {
                Icon(imageVector = Icons.Default.Remove, contentDescription = "Zoom Out", tint = Color.White)
            }

            IconButton(
                onClick = {
                    if (activeMode == "bangladesh") {
                        webViewRef?.evaluateJavascript("flyToPreset($userLat, $userLon, 7.5);", null)
                    } else {
                        webViewRef?.evaluateJavascript("flyToPreset(20.0, 15.0, 2);", null)
                    }
                },
                colors = IconButtonDefaults.iconButtonColors(containerColor = ObsidianSurface.copy(alpha = 0.92f)),
                modifier = Modifier.size(42.dp)
            ) {
                Icon(imageVector = Icons.Default.MyLocation, contentDescription = "Recenter", tint = Color(0xFF00B4D8))
            }
        }

        // Selected Earthquake Detail Sheet
        AnimatedVisibility(
            visible = selectedQuake != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(14.dp)
        ) {
            selectedQuake?.let { q ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface.copy(alpha = 0.96f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = if (q.magnitude >= 5.5) EmergencyCrimson else if (q.magnitude >= 4.0) Color(0xFFFFB703) else TelemetryEmerald,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "MAGNITUDE ${q.magnitude}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.Black,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            IconButton(
                                onClick = { selectedQuake = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = q.place,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Distance: %.0f km away".format(q.distanceKmFromUser),
                                fontSize = 11.sp,
                                color = Color(0xFF38BDF8)
                            )
                            Text(
                                text = "Depth: ${q.depthKm} km",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }
        }

        // Bottom GIS Legend Strip (when no quake selected)
        if (selectedQuake == null) {
            Surface(
                color = ObsidianSurface.copy(alpha = 0.92f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GisMapLegendItem(color = Color(0xFF00B4D8), label = "Station")
                    GisMapLegendItem(color = EmergencyCrimson, label = "Madhupur")
                    GisMapLegendItem(color = Color(0xFFFFB703), label = "Dauki")
                    GisMapLegendItem(color = Color(0xFFFF5722), label = "Plates")
                    GisMapLegendItem(color = TelemetryEmerald, label = "Quakes")
                }
            }
        }
    }
}

@Composable
private fun GisMapLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color(0xFFCBD5E1),
            fontFamily = FontFamily.Monospace
        )
    }
}
