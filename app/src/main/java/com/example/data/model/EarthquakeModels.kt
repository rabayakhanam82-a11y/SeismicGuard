package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@JsonClass(generateAdapter = true)
data class UsgsFeatureCollection(
    @Json(name = "type") val type: String = "FeatureCollection",
    @Json(name = "features") val features: List<UsgsFeature> = emptyList()
)

@JsonClass(generateAdapter = true)
data class UsgsFeature(
    @Json(name = "id") val id: String,
    @Json(name = "properties") val properties: UsgsProperties,
    @Json(name = "geometry") val geometry: UsgsGeometry
)

@JsonClass(generateAdapter = true)
data class UsgsProperties(
    @Json(name = "mag") val mag: Double?,
    @Json(name = "place") val place: String?,
    @Json(name = "time") val time: Long?,
    @Json(name = "url") val url: String?,
    @Json(name = "alert") val alert: String?,
    @Json(name = "status") val status: String?
)

@JsonClass(generateAdapter = true)
data class UsgsGeometry(
    @Json(name = "type") val type: String,
    @Json(name = "coordinates") val coordinates: List<Double> // [longitude, latitude, depth]
)

data class EarthquakeItem(
    val id: String,
    val magnitude: Double,
    val place: String,
    val timeMillis: Long,
    val latitude: Double,
    val longitude: Double,
    val depthKm: Double,
    val distanceKmFromUser: Double = 0.0
)

fun calculateHaversineDistanceKm(
    lat1: Double, lon1: Double,
    lat2: Double, lon2: Double
): Double {
    val r = 6371.0 // Earth radius in km
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c
}
