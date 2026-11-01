package com.example.data.model

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

enum class WeatherSkyTheme(
    val displayName: String,
    val topColor: Color,
    val middleColor: Color,
    val bottomColor: Color,
    val isDark: Boolean
) {
    CLEAR_DAY(
        "Sunny & Clear",
        Color(0xFF0E2A54),
        Color(0xFF0A1B38),
        Color(0xFF060B18),
        true
    ),
    CLOUDY_DAY(
        "Partly Cloudy",
        Color(0xFF0E1A30),
        Color(0xFF13233F),
        Color(0xFF080E1C),
        true
    ),
    RAINY(
        "Rain & Showers",
        Color(0xFF08152E),
        Color(0xFF0D2248),
        Color(0xFF050B16),
        true
    ),
    THUNDERSTORM(
        "Storm & High Alert",
        Color(0xFF0A0D24),
        Color(0xFF14193F),
        Color(0xFF060818),
        true
    ),
    SUNSET(
        "Golden Dusk",
        Color(0xFF2B1425),
        Color(0xFF1B1638),
        Color(0xFF070B1A),
        true
    ),
    CLEAR_NIGHT(
        "Starry Night",
        Color(0xFF040714),
        Color(0xFF081028),
        Color(0xFF0B1636),
        true
    );

    fun toBrush(): Brush {
        return Brush.verticalGradient(
            colors = listOf(topColor, middleColor, bottomColor)
        )
    }
}

data class HourlyForecast(
    val time: String,
    val temp: String,
    val condition: String,
    val rainProb: Int,
    val isNow: Boolean = false
)

data class DailyForecast(
    val dayName: String,
    val condition: String,
    val maxTemp: String,
    val minTemp: String,
    val rainMm: String,
    val warningTag: String? = null
)

@JsonClass(generateAdapter = true)
data class OpenMeteoWeatherResponse(
    @Json(name = "latitude") val latitude: Double,
    @Json(name = "longitude") val longitude: Double,
    @Json(name = "current") val current: OpenMeteoCurrent?,
    @Json(name = "hourly") val hourly: OpenMeteoHourly?,
    @Json(name = "daily") val daily: OpenMeteoDaily?
)

@JsonClass(generateAdapter = true)
data class OpenMeteoCurrent(
    @Json(name = "temperature_2m") val temperature: Double?,
    @Json(name = "relative_humidity_2m") val humidity: Double?,
    @Json(name = "surface_pressure") val pressure: Double?,
    @Json(name = "wind_speed_10m") val windSpeed: Double?,
    @Json(name = "weather_code") val weatherCode: Int?,
    @Json(name = "is_day") val isDay: Int?,
    @Json(name = "apparent_temperature") val apparentTemperature: Double?,
    @Json(name = "uv_index") val uvIndex: Double?,
    @Json(name = "rain") val rain: Double? = null,
    @Json(name = "showers") val showers: Double? = null,
    @Json(name = "cloud_cover") val cloudCover: Int? = null
)

@JsonClass(generateAdapter = true)
data class OpenMeteoHourly(
    @Json(name = "time") val time: List<String>?,
    @Json(name = "temperature_2m") val temperature: List<Double>?,
    @Json(name = "precipitation_probability") val precipitationProbability: List<Int>?,
    @Json(name = "weather_code") val weatherCode: List<Int>?
)

@JsonClass(generateAdapter = true)
data class OpenMeteoDaily(
    @Json(name = "time") val time: List<String>?,
    @Json(name = "temperature_2m_max") val tempMax: List<Double>?,
    @Json(name = "temperature_2m_min") val tempMin: List<Double>?,
    @Json(name = "precipitation_sum") val precipitationSum: List<Double>?,
    @Json(name = "weather_code") val weatherCode: List<Int>?
)

data class MeteorologicalTelemetry(
    val locationName: String = "Gazipur / Dhaka",
    val regionalDivision: String = "Dhaka Division, Bangladesh",
    val latitude: Double = 23.8103,
    val longitude: Double = 90.4125,
    val temperatureCelsius: Double = 29.4,
    val feelsLikeCelsius: Double = 32.1,
    val humidityPercent: Int = 74,
    val barometricPressureHpa: Double = 1011.2,
    val windSpeedKmh: Double = 12.8,
    val uvIndex: Double = 6.4,
    val airQualityIndex: String = "Good (AQI 42)",
    val conditionDescription: String = "Partly Cloudy • Gentle Breeze",
    val skyTheme: WeatherSkyTheme = if (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) in 6..17) WeatherSkyTheme.CLEAR_DAY else WeatherSkyTheme.CLEAR_NIGHT,
    val seismicThreatIndex: String = "NORMAL & SAFE", // NORMAL & SAFE, ELEVATED WATCH, CRITICAL
    val activeFaultLineDistance: String = "Madhupur Fault ~28 km North",
    val monsoonStatus: String = "Active Southwest Monsoon Flow - Normal Barometric Gradients",
    val hourlyForecasts: List<HourlyForecast> = emptyList(),
    val dailyForecasts: List<DailyForecast> = emptyList()
)

data class BmdStationData(
    val division: String,
    val stationCity: String,
    val tectonicZone: String,
    val seismicZoneRating: String, // Zone I, Zone II, Zone III, Zone IV
    val currentTemp: String,
    val rainfallMm24h: String,
    val alertLevel: String,
    val aqiStatus: String = "Moderate"
)
