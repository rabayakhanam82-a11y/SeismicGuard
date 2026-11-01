package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SeismicDatabase
import com.example.data.local.TremorEventEntity
import com.example.data.model.BackendGateway
import com.example.data.model.BmdStationData
import com.example.data.model.CriticalTremorAlert
import com.example.data.model.DailyForecast
import com.example.data.model.DefaultDisasterNews
import com.example.data.model.DisasterNewsItem
import com.example.data.model.EarthquakeItem
import com.example.data.model.HardwareTelemetry
import com.example.data.model.HourlyForecast
import com.example.data.model.MeteorologicalTelemetry
import com.example.data.model.WeatherSkyTheme
import com.example.data.model.calculateHaversineDistanceKm
import com.example.data.remote.RetrofitClient
import com.example.data.repository.SeismicHardwareRepository
import com.example.emergency.EmergencyAlarmService
import com.example.emergency.SoundSynthesizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class DashboardDrawerDestination(val title: String, val isDevOnly: Boolean = false) {
    HOME("Weather & Ground Safety"),
    INTERACTIVE_MAP("Live Tectonic Map"),
    SAFETY_GUIDE("Emergency Safety Guide"),
    BMD_REGIONAL_HUB("Regional Advisories"),
    TEAM_CREDITS("Team Omega Credits", false),

    // Dev-Only Destinations
    DEV_SUITE("Developer Console", true),
    ESP32_WIZARD("ESP32 Connection Wizard", true),
    CLOUD_GUIDE("Cloud & Firebase Guide", true),
    PIN_INSPECTOR("Hardware Pin Inspector", true),
    SEISMIC_LOG("Tremor History Log", true),
    FIRMWARE_CODE("ESP32 Firmware Source", true)
}

class SeismicGuardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SeismicHardwareRepository.getInstance(application)
    private val tremorDao = SeismicDatabase.getDatabase(application).tremorEventDao()
    private val offlineCacheManager = com.example.data.local.SeismicOfflineCacheManager.getInstance(application)

    val telemetryState: StateFlow<HardwareTelemetry> = repository.telemetryState

    private val _currentDestination = MutableStateFlow(DashboardDrawerDestination.HOME)
    val currentDestination: StateFlow<DashboardDrawerDestination> = _currentDestination.asStateFlow()

    private val _isDeveloperMode = MutableStateFlow(false)
    val isDeveloperMode: StateFlow<Boolean> = _isDeveloperMode.asStateFlow()

    private val _isOfflineMode = MutableStateFlow(!offlineCacheManager.isOnline())
    val isOfflineMode: StateFlow<Boolean> = _isOfflineMode.asStateFlow()

    private val _lastCacheSyncTime = MutableStateFlow(offlineCacheManager.getLastSyncTimestamp())
    val lastCacheSyncTime: StateFlow<Long> = _lastCacheSyncTime.asStateFlow()

    private val _earthquakes = MutableStateFlow<List<EarthquakeItem>>(emptyList())
    val earthquakes: StateFlow<List<EarthquakeItem>> = _earthquakes.asStateFlow()

    private val _weatherTelemetry = MutableStateFlow(MeteorologicalTelemetry())
    val weatherTelemetry: StateFlow<MeteorologicalTelemetry> = _weatherTelemetry.asStateFlow()

    private val _newsItems = MutableStateFlow(DefaultDisasterNews.sampleNews)
    val newsItems: StateFlow<List<DisasterNewsItem>> = _newsItems.asStateFlow()

    private val _userLatitude = MutableStateFlow(23.8103) // Default Gazipur / Dhaka, Bangladesh
    private val _userLongitude = MutableStateFlow(90.4125)
    val userLatitude: StateFlow<Double> = _userLatitude.asStateFlow()
    val userLongitude: StateFlow<Double> = _userLongitude.asStateFlow()

    private val _selectedSeverityFilter = MutableStateFlow("ALL")
    val selectedSeverityFilter: StateFlow<String> = _selectedSeverityFilter.asStateFlow()

    val historicalTremors: StateFlow<List<TremorEventEntity>> = tremorDao.getAllEvents()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _manualSkyTheme = MutableStateFlow<WeatherSkyTheme?>(null)
    val manualSkyTheme: StateFlow<WeatherSkyTheme?> = _manualSkyTheme.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _lastWeatherSyncTime = MutableStateFlow(System.currentTimeMillis())
    val lastWeatherSyncTime: StateFlow<Long> = _lastWeatherSyncTime.asStateFlow()

    fun setManualSkyTheme(theme: WeatherSkyTheme?) {
        _manualSkyTheme.value = theme
        if (theme != null) {
            _weatherTelemetry.update { it.copy(skyTheme = theme) }
            _userMessage.tryEmit("Atmosphere set to: ${theme.displayName}")
        } else {
            fetchMeteorologicalData(_userLatitude.value, _userLongitude.value)
            _userMessage.tryEmit("Atmosphere set to: Live Auto")
        }
    }

    fun refreshAllData(showToast: Boolean = false) {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                coroutineScope {
                    val weatherDeferred = async { fetchMeteorologicalDataInternal(_userLatitude.value, _userLongitude.value) }
                    val earthquakeDeferred = async { fetchLiveEarthquakesInternal(isBackgroundSync = false, forceOnline = true) }
                    weatherDeferred.await()
                    earthquakeDeferred.await()
                }
                _lastWeatherSyncTime.value = System.currentTimeMillis()
                if (showToast) {
                    _userMessage.tryEmit("Weather updated just now")
                }
            } catch (e: Exception) {
                if (showToast) {
                    _userMessage.tryEmit("Refreshed with cached data")
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private val _userMessage = MutableSharedFlow<String>(extraBufferCapacity = 3)
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    val bmdStations = listOf(
        BmdStationData(
            division = "Dhaka Division",
            stationCity = "Gazipur / Agargaon",
            tectonicZone = "Madhupur Tract Fault",
            seismicZoneRating = "Zone II (Moderate: 0.20g PGA)",
            currentTemp = "29.4°C",
            rainfallMm24h = "14.2 mm",
            alertLevel = "SAFE & NORMAL",
            aqiStatus = "Moderate (AQI 68)"
        ),
        BmdStationData(
            division = "Sylhet Division",
            stationCity = "Sylhet Sadar",
            tectonicZone = "Dauki Fault Line",
            seismicZoneRating = "Zone IV (Severe: 0.36g PGA)",
            currentTemp = "27.1°C",
            rainfallMm24h = "68.0 mm",
            alertLevel = "RAIN & FAULT WATCH",
            aqiStatus = "Good (AQI 32)"
        ),
        BmdStationData(
            division = "Chattogram Division",
            stationCity = "Chattogram Port / Cox's Bazar",
            tectonicZone = "Chittagong-Tripura Fold Belt",
            seismicZoneRating = "Zone III (High: 0.28g PGA)",
            currentTemp = "30.2°C",
            rainfallMm24h = "32.4 mm",
            alertLevel = "COASTAL ADVISORY",
            aqiStatus = "Moderate (AQI 55)"
        ),
        BmdStationData(
            division = "Rajshahi Division",
            stationCity = "Bogra / Rajshahi",
            tectonicZone = "Bogra Fault System",
            seismicZoneRating = "Zone I (Low: 0.12g PGA)",
            currentTemp = "31.5°C",
            rainfallMm24h = "2.1 mm",
            alertLevel = "SAFE & NORMAL",
            aqiStatus = "Good (AQI 45)"
        )
    )

    init {
        viewModelScope.launch {
            repository.criticalAlertEvents.collect { alert ->
                triggerEmergencyOverride(alert)
            }
        }

        // Automatic network restoration listener
        offlineCacheManager.registerNetworkCallback {
            viewModelScope.launch {
                _userMessage.tryEmit("Internet restored — Live USGS stream active & cache synced")
                offlineCacheManager.clearOfflineCacheOnNetworkRestore()
                fetchLiveEarthquakes(forceOnline = true)
            }
        }

        // Background 30-minute periodic earthquake sync
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(30 * 60 * 1000L) // 30 minutes
                fetchLiveEarthquakes(isBackgroundSync = true)
            }
        }

        fetchLiveEarthquakes()
        fetchMeteorologicalData(_userLatitude.value, _userLongitude.value)
    }

    fun navigateTo(dest: DashboardDrawerDestination) {
        _currentDestination.value = dest
    }

    fun toggleDeveloperMode() {
        val newVal = !_isDeveloperMode.value
        _isDeveloperMode.value = newVal
        _userMessage.tryEmit(if (newVal) "Developer Mode Activated" else "Switched to Consumer Mode")
    }

    fun setDeveloperMode(enabled: Boolean) {
        _isDeveloperMode.value = enabled
    }

    fun setGateway(gateway: BackendGateway) {
        repository.setGateway(gateway)
        _userMessage.tryEmit("Active backend: ${gateway.displayName}")
    }

    fun setEsp32Ip(ip: String) {
        repository.updateEsp32Ip(ip)
        _userMessage.tryEmit("Connecting to ESP32 at: $ip")
    }

    fun setPotentiometerSensitivity(pct: Int) {
        repository.updateSensitivity(pct)
    }

    fun setSensorDebounce(ms: Int) {
        repository.updateDebounce(ms)
    }

    fun setAlarmTone(tone: String) {
        repository.selectedAlarmTone = tone
        _userMessage.tryEmit("Alarm sound profile: $tone")
    }

    fun setSeverityFilter(filter: String) {
        _selectedSeverityFilter.value = filter
    }

    fun updateUserLocation(lat: Double, lon: Double, label: String = "Live Device Location") {
        _userLatitude.value = lat
        _userLongitude.value = lon
        _weatherTelemetry.update { it.copy(locationName = label, latitude = lat, longitude = lon) }
        recalculateEarthquakeDistances(lat, lon)
        fetchMeteorologicalData(lat, lon)
        _userMessage.tryEmit("Location set to: $label")
    }

    fun triggerPresentationDemoAlarm() {
        repository.triggerEmergencyTremorAlert(
            source = "DEV_SIMULATION_OVERRIDE",
            pin = 18,
            latencyMs = 9L,
            peakG = 4.8f
        )
    }

    fun testBuzzerPulse() {
        repository.sendCommandToHardware("TEST_BUZZER")
        _userMessage.tryEmit("Testing acoustic alert on BC547 buzzer...")
    }

    fun testFirebaseConnection(onResult: (Boolean, String) -> Unit) {
        repository.sendFirebaseTestPing(onResult)
    }

    fun testEsp32Connection(ip: String, onResult: (Boolean, String) -> Unit) {
        repository.testHardwarePing(ip, onResult)
    }

    private fun triggerEmergencyOverride(alert: CriticalTremorAlert) {
        val context = getApplication<Application>().applicationContext
        EmergencyAlarmService.triggerAlert(
            context = context,
            source = alert.source,
            pin = alert.pinTriggered,
            latencyMs = alert.latencyMs,
            peakG = alert.peakG,
            toneProfile = repository.selectedAlarmTone
        )
    }

    fun fetchLiveEarthquakes(isBackgroundSync: Boolean = false, forceOnline: Boolean = false) {
        viewModelScope.launch {
            fetchLiveEarthquakesInternal(isBackgroundSync, forceOnline)
        }
    }

    private suspend fun fetchLiveEarthquakesInternal(isBackgroundSync: Boolean = false, forceOnline: Boolean = false) {
        val isOnline = offlineCacheManager.isOnline()
        if (!isOnline && !forceOnline) {
            loadOfflineCachedEarthquakes()
            return
        }

        try {
            val response = withContext(Dispatchers.IO) {
                RetrofitClient.usgsApi.getRecentEarthquakes()
            }
            val items = response.features.mapNotNull { f ->
                val coords = f.geometry.coordinates
                if (coords.size >= 2) {
                    val lon = coords[0]
                    val lat = coords[1]
                    val depth = if (coords.size > 2) coords[2] else 10.0
                    val mag = f.properties.mag ?: 2.5
                    val place = f.properties.place ?: "Seismic Zone"
                    val time = f.properties.time ?: System.currentTimeMillis()
                    val dist = calculateHaversineDistanceKm(_userLatitude.value, _userLongitude.value, lat, lon)
                    EarthquakeItem(
                        id = f.id,
                        magnitude = mag,
                        place = place,
                        timeMillis = time,
                        latitude = lat,
                        longitude = lon,
                        depthKm = depth,
                        distanceKmFromUser = dist
                    )
                } else null
            }.sortedBy { it.distanceKmFromUser }

            if (items.isNotEmpty()) {
                _earthquakes.value = items
                _isOfflineMode.value = false
                offlineCacheManager.saveEarthquakesToCache(items)
                _lastCacheSyncTime.value = System.currentTimeMillis()
                if (!isBackgroundSync) {
                    _userMessage.tryEmit("Synced ${items.size} live earthquakes from USGS Global Feed")
                }
            } else {
                loadOfflineCachedEarthquakes()
            }
        } catch (e: Exception) {
            loadOfflineCachedEarthquakes()
        }
    }

    private fun loadOfflineCachedEarthquakes() {
        val cached = offlineCacheManager.getCachedEarthquakes()
        if (cached.isNotEmpty()) {
            val userLat = _userLatitude.value
            val userLon = _userLongitude.value
            val updated = cached.map {
                it.copy(distanceKmFromUser = calculateHaversineDistanceKm(userLat, userLon, it.latitude, it.longitude))
            }.sortedBy { it.distanceKmFromUser }
            _earthquakes.value = updated
            _isOfflineMode.value = true
            _userMessage.tryEmit("Offline Mode: Showing ${updated.size} cached earthquakes from storage")
        } else {
            loadFallbackEarthquakes()
        }
    }

    private fun loadFallbackEarthquakes() {
        val userLat = _userLatitude.value
        val userLon = _userLongitude.value
        val list = listOf(
            EarthquakeItem("usgs_1", 3.2, "24 km N of Gazipur, Bangladesh", System.currentTimeMillis() - 7200000L, 24.01, 90.42, 12.0),
            EarthquakeItem("usgs_2", 4.1, "Sylhet Sadar Basin, Bangladesh", System.currentTimeMillis() - 18000000L, 24.89, 91.86, 18.4),
            EarthquakeItem("usgs_3", 5.4, "Myanmar-Chittagong Border Region", System.currentTimeMillis() - 36000000L, 23.12, 93.45, 45.0),
            EarthquakeItem("usgs_4", 4.8, "Bay of Bengal Submarine Trench", System.currentTimeMillis() - 48000000L, 19.80, 89.20, 22.0),
            EarthquakeItem("usgs_5", 6.2, "Banda Sea, Indonesia", System.currentTimeMillis() - 54000000L, -6.12, 129.50, 110.0)
        ).map { item ->
            item.copy(distanceKmFromUser = calculateHaversineDistanceKm(userLat, userLon, item.latitude, item.longitude))
        }.sortedBy { it.distanceKmFromUser }

        _earthquakes.value = list
    }

    private fun recalculateEarthquakeDistances(lat: Double, lon: Double) {
        _earthquakes.update { list ->
            list.map { item ->
                item.copy(distanceKmFromUser = calculateHaversineDistanceKm(lat, lon, item.latitude, item.longitude))
            }.sortedBy { it.distanceKmFromUser }
        }
    }

    private fun fetchMeteorologicalData(lat: Double, lon: Double) {
        viewModelScope.launch {
            fetchMeteorologicalDataInternal(lat, lon)
        }
    }

    private suspend fun fetchMeteorologicalDataInternal(lat: Double, lon: Double) {
        try {
            val res = withContext(Dispatchers.IO) {
                RetrofitClient.weatherApi.getCurrentWeather(lat, lon)
            }

            val current = res.current
            val isDay = if (current?.isDay != null) {
                current.isDay == 1
            } else {
                val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                hour in 6..17
            }
            val code = current?.weatherCode ?: 0
            val rainMm = (current?.rain ?: 0.0) + (current?.showers ?: 0.0)
            val cloudCover = current?.cloudCover ?: 0

            val isRaining = rainMm > 0.05 || code in listOf(51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82)
            val isThunder = code in 95..99

            val sky = when {
                isThunder -> WeatherSkyTheme.THUNDERSTORM
                isRaining -> WeatherSkyTheme.RAINY
                !isDay -> WeatherSkyTheme.CLEAR_NIGHT
                code in 2..3 || code in 45..48 || cloudCover >= 45 -> WeatherSkyTheme.CLOUDY_DAY
                else -> WeatherSkyTheme.CLEAR_DAY
            }

            val conditionDesc = when (code) {
                0 -> "Clear Skies • Calm & Sunny"
                1, 2 -> "Mainly Clear • Light Clouds"
                3 -> "Overcast • Shaded Day"
                45, 48 -> "Morning Fog • Reduced Visibility"
                51, 53, 55 -> "Light Drizzle • Wet Pavements"
                61, 63, 65 -> "Moderate Rain • Fresh Showers"
                80, 81, 82 -> "Rain Showers • Water Accumulation"
                95, 96, 99 -> "Severe Thunderstorm • High Winds"
                else -> "Fair Weather • Pleasant"
            }

            val hourlyList = mutableListOf<HourlyForecast>()
            val hTimes: List<String> = res.hourly?.time ?: emptyList()
            val hTemps: List<Double> = res.hourly?.temperature ?: emptyList()
            val hProbs: List<Int> = res.hourly?.precipitationProbability ?: emptyList()
            val maxLimit = minOf(8, hTimes.size, hTemps.size)
            for (i in 0 until maxLimit) {
                val rawTime = hTimes[i]
                val hourStr = rawTime.substringAfter("T").take(5)
                hourlyList.add(
                    HourlyForecast(
                        time = if (i == 0) "Now" else hourStr,
                        temp = "%.0f°".format(hTemps[i]),
                        condition = "Fair",
                        rainProb = if (i < hProbs.size) hProbs[i] else 10,
                        isNow = (i == 0)
                    )
                )
            }

            val dailyList = mutableListOf<DailyForecast>()
            val dTimes = res.daily?.time ?: emptyList()
            val dMax = res.daily?.tempMax ?: emptyList()
            val dMin = res.daily?.tempMin ?: emptyList()
            val dRain = res.daily?.precipitationSum ?: emptyList()

            val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
            val parseFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            for (i in 0 until minOf(7, dTimes.size, dMax.size, dMin.size)) {
                val dStr = try {
                    val parsed = parseFormat.parse(dTimes[i])
                    if (i == 0) "Today" else dayFormat.format(parsed ?: Date())
                } catch (e: Exception) {
                    "Day $i"
                }
                val rainVal = if (i < dRain.size) "%.1f mm".format(dRain[i]) else "0 mm"
                dailyList.add(
                    DailyForecast(
                        dayName = dStr,
                        condition = if ((dRain.getOrNull(i) ?: 0.0) > 2.0) "Rain" else "Partly Cloudy",
                        maxTemp = "%.0f°".format(dMax[i]),
                        minTemp = "%.0f°".format(dMin[i]),
                        rainMm = rainVal,
                        warningTag = if (i == 0) "Monsoon Normal" else null
                    )
                )
            }

            _weatherTelemetry.update { curr ->
                curr.copy(
                    temperatureCelsius = current?.temperature ?: 29.4,
                    feelsLikeCelsius = current?.apparentTemperature ?: 31.8,
                    humidityPercent = (current?.humidity ?: 74.0).toInt(),
                    barometricPressureHpa = current?.pressure ?: 1011.2,
                    windSpeedKmh = current?.windSpeed ?: 12.8,
                    uvIndex = current?.uvIndex ?: 5.2,
                    conditionDescription = conditionDesc,
                    skyTheme = sky,
                    hourlyForecasts = if (hourlyList.isNotEmpty()) hourlyList else defaultHourly(),
                    dailyForecasts = if (dailyList.isNotEmpty()) dailyList else defaultDaily()
                )
            }
        } catch (e: Exception) {
            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            val isDay = hour in 6..17
            _weatherTelemetry.update {
                it.copy(
                    skyTheme = if (isDay) WeatherSkyTheme.CLEAR_DAY else WeatherSkyTheme.CLEAR_NIGHT,
                    hourlyForecasts = defaultHourly(),
                    dailyForecasts = defaultDaily()
                )
            }
        }
    }

    private fun defaultHourly(): List<HourlyForecast> {
        return listOf(
            HourlyForecast("Now", "29°", "Partly Cloudy", 15, true),
            HourlyForecast("14:00", "31°", "Sunny", 10),
            HourlyForecast("16:00", "30°", "Partly Cloudy", 20),
            HourlyForecast("18:00", "28°", "Sunset", 5),
            HourlyForecast("20:00", "26°", "Clear", 0),
            HourlyForecast("22:00", "25°", "Clear", 0),
            HourlyForecast("00:00", "24°", "Starry", 0)
        )
    }

    private fun defaultDaily(): List<DailyForecast> {
        return listOf(
            DailyForecast("Today", "Partly Cloudy", "32°", "24°", "4.2 mm"),
            DailyForecast("Tue", "Scattered Showers", "30°", "23°", "18.0 mm"),
            DailyForecast("Wed", "Thunderstorm Watch", "29°", "22°", "34.5 mm", "MONSOON ALERT"),
            DailyForecast("Thu", "Rain", "28°", "22°", "22.1 mm"),
            DailyForecast("Fri", "Partly Sunny", "31°", "24°", "2.0 mm"),
            DailyForecast("Sat", "Sunny & Warm", "33°", "25°", "0.0 mm"),
            DailyForecast("Sun", "Sunny", "32°", "24°", "0.0 mm")
        )
    }

    fun clearLogHistory() {
        viewModelScope.launch {
            tremorDao.clearAllEvents()
            _userMessage.tryEmit("Cleared all tremor history")
        }
    }

    fun exportLogAsCsv(): String {
        val list = historicalTremors.value
        val sb = StringBuilder()
        sb.append("ID,Timestamp,Source,Pin,PeakG,LatencyMs,DurationMs,Severity,Notes\n")
        list.forEach {
            sb.append("${it.id},${it.timestamp},${it.source},${it.pinTriggered},${it.peakAmplitudeG},${it.interruptLatencyMs},${it.durationMs},${it.severity},\"${it.notes}\"\n")
        }
        return sb.toString()
    }
}
