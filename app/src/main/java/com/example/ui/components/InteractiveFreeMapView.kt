package com.example.ui.components

import androidx.compose.runtime.Composable
import com.example.data.model.EarthquakeItem

@Composable
fun InteractiveFreeMapView(
    earthquakes: List<EarthquakeItem>,
    userLat: Double = 23.8103,
    userLon: Double = 90.4125,
    locationLabel: String = "Station Location",
    onRefresh: () -> Unit = {}
) {
    WorldTectonicMapView(
        earthquakes = earthquakes,
        userLat = userLat,
        userLon = userLon,
        locationLabel = locationLabel,
        isOffline = false,
        onRefresh = onRefresh
    )
}
