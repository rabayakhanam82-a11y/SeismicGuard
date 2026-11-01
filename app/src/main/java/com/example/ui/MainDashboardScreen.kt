package com.example.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import com.example.R
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyForecast
import com.example.data.model.DisasterNewsItem
import com.example.data.model.EarthquakeItem
import com.example.data.model.HardwareTelemetry
import com.example.data.model.HourlyForecast
import com.example.data.model.MeteorologicalTelemetry
import com.example.data.model.WeatherSkyTheme
import com.example.ui.components.BmdRegionalHub
import com.example.ui.components.CloudBackendGuide
import com.example.ui.components.DeveloperSuiteView
import com.example.ui.components.Esp32ConnectGuide
import com.example.ui.components.FirmwareReferenceView
import com.example.ui.components.LivePinInspector
import com.example.ui.components.NativeTectonicMapView
import com.example.ui.components.WorldTectonicMapView
import com.example.ui.components.SafetyActionGuide
import com.example.ui.components.SeismicActivityLogView
import com.example.ui.components.SettingsPanel
import com.example.ui.components.TeamOmegaCredits
import com.example.ui.theme.EmergencyCrimson
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.TelemetryEmerald
import com.example.ui.viewmodel.DashboardDrawerDestination
import com.example.ui.viewmodel.SeismicGuardViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen(viewModel: SeismicGuardViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var showSettingsSheet by remember { mutableStateOf(false) }

    val currentDestination by viewModel.currentDestination.collectAsState()
    val isDevMode by viewModel.isDeveloperMode.collectAsState()
    val telemetry by viewModel.telemetryState.collectAsState()
    val weather by viewModel.weatherTelemetry.collectAsState()
    val earthquakes by viewModel.earthquakes.collectAsState()
    val newsItems by viewModel.newsItems.collectAsState()
    val historicalTremors by viewModel.historicalTremors.collectAsState()
    val selectedFilter by viewModel.selectedSeverityFilter.collectAsState()
    val isOfflineMode by viewModel.isOfflineMode.collectAsState()
    val userLat by viewModel.userLatitude.collectAsState()
    val userLon by viewModel.userLongitude.collectAsState()
    val manualSkyTheme by viewModel.manualSkyTheme.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    BackHandler(enabled = currentDestination != DashboardDrawerDestination.HOME) {
        viewModel.navigateTo(DashboardDrawerDestination.HOME)
    }

    LaunchedEffect(Unit) {
        viewModel.userMessage.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF0F172A),
                drawerContentColor = Color.White
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_seismicguard_logo),
                            contentDescription = "SeismicGuard App Icon",
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Image(
                            painter = painterResource(R.drawable.ic_seismicguard_wordmark_tight),
                            contentDescription = "SEISMICGUARD",
                            modifier = Modifier
                                .height(26.dp)
                                .widthIn(max = 180.dp),
                            contentScale = ContentScale.Fit,
                            alignment = Alignment.CenterStart
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Early Warning & Weather Hub",
                        fontSize = 12.sp,
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Filter destinations: Consumer destinations first
                val destinationsToShow = if (isDevMode) {
                    DashboardDrawerDestination.values().toList()
                } else {
                    DashboardDrawerDestination.values().filter { !it.isDevOnly }
                }

                destinationsToShow.forEach { destination ->
                    val isSelected = currentDestination == destination
                    val icon = when (destination) {
                        DashboardDrawerDestination.HOME -> Icons.Default.Home
                        DashboardDrawerDestination.INTERACTIVE_MAP -> Icons.Default.Map
                        DashboardDrawerDestination.SAFETY_GUIDE -> Icons.Default.Security
                        DashboardDrawerDestination.BMD_REGIONAL_HUB -> Icons.Default.WbSunny
                        DashboardDrawerDestination.DEV_SUITE -> Icons.Default.Build
                        DashboardDrawerDestination.ESP32_WIZARD -> Icons.Default.DeviceHub
                        DashboardDrawerDestination.CLOUD_GUIDE -> Icons.Default.Cloud
                        DashboardDrawerDestination.PIN_INSPECTOR -> Icons.Default.Sensors
                        DashboardDrawerDestination.SEISMIC_LOG -> Icons.Default.History
                        DashboardDrawerDestination.FIRMWARE_CODE -> Icons.Default.Code
                        DashboardDrawerDestination.TEAM_CREDITS -> Icons.Default.Bolt
                    }

                    NavigationDrawerItem(
                        icon = { Icon(imageVector = icon, contentDescription = null) },
                        label = { Text(destination.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                        selected = isSelected,
                        onClick = {
                            viewModel.navigateTo(destination)
                            coroutineScope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = Color(0xFF38BDF8).copy(alpha = 0.2f),
                            selectedIconColor = Color(0xFF38BDF8),
                            selectedTextColor = Color.White,
                            unselectedContainerColor = Color.Transparent,
                            unselectedIconColor = Color(0xFF94A3B8),
                            unselectedTextColor = Color(0xFF94A3B8)
                        ),
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }

                if (!isDevMode) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        color = Color(0xFF1E293B).copy(alpha = 0.6f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clickable {
                                viewModel.setDeveloperMode(true)
                                Toast.makeText(context, "Developer Mode Activated!", Toast.LENGTH_SHORT).show()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Build, contentDescription = null, tint = Color(0xFFFFB703), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Turn On Developer Mode", fontSize = 11.sp, color = Color(0xFFFFB703), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    ) {
        // Dynamic Samsung Weather Style Atmosphere Backdrop with Animated Growing Stars
        com.example.ui.components.ImmersiveWeatherBackground(skyTheme = weather.skyTheme) {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.testTag("app_bar_title_brand")
                            ) {
                                val dotColor = if (telemetry.isConnected) {
                                    TelemetryEmerald
                                } else if (telemetry.firebaseConnected) {
                                    Color(0xFF38BDF8)
                                } else {
                                    Color(0xFF64748B)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(dotColor)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Image(
                                    painter = painterResource(id = R.drawable.ic_seismicguard_wordmark_tight),
                                    contentDescription = "SEISMICGUARD",
                                    modifier = Modifier
                                        .height(24.dp)
                                        .widthIn(max = 165.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        },
                        navigationIcon = {
                            if (currentDestination == DashboardDrawerDestination.HOME) {
                                IconButton(
                                    onClick = { coroutineScope.launch { drawerState.open() } },
                                    modifier = Modifier.testTag("left_drawer_button")
                                ) {
                                    Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                                }
                            } else {
                                IconButton(
                                    onClick = { viewModel.navigateTo(DashboardDrawerDestination.HOME) },
                                    modifier = Modifier.testTag("back_to_home_button")
                                ) {
                                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Dashboard", tint = Color.White)
                                }
                            }
                        },
                        actions = {
                            if (isDevMode) {
                                Surface(
                                    color = Color(0xFFFFB703).copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.clickable { viewModel.navigateTo(DashboardDrawerDestination.DEV_SUITE) }
                                ) {
                                    Text(
                                        text = "DEV",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFFFFB703),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                            }

                            IconButton(
                                onClick = { showSettingsSheet = true },
                                modifier = Modifier.testTag("settings_button")
                            ) {
                                Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )
                },
                containerColor = Color.Transparent
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    when (currentDestination) {
                        DashboardDrawerDestination.HOME -> PullToRefreshBox(
                            isRefreshing = isRefreshing,
                            onRefresh = { viewModel.refreshAllData(showToast = true) },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            SamsungWeatherDashboard(
                                telemetry = telemetry,
                                weather = weather,
                                earthquakes = earthquakes,
                                newsItems = newsItems,
                                isDevMode = isDevMode,
                                onOpenMap = { viewModel.navigateTo(DashboardDrawerDestination.INTERACTIVE_MAP) },
                                onOpenDevSuite = { viewModel.navigateTo(DashboardDrawerDestination.DEV_SUITE) },
                                onQuickDemoTrigger = { viewModel.triggerPresentationDemoAlarm() }
                            )
                        }
                        DashboardDrawerDestination.INTERACTIVE_MAP -> WorldTectonicMapView(
                            earthquakes = earthquakes,
                            userLat = userLat,
                            userLon = userLon,
                            locationLabel = weather.locationName,
                            isOffline = isOfflineMode,
                            onRefresh = { viewModel.fetchLiveEarthquakes(forceOnline = true) }
                        )
                        DashboardDrawerDestination.SAFETY_GUIDE -> SafetyActionGuide()
                        DashboardDrawerDestination.BMD_REGIONAL_HUB -> BmdRegionalHub(
                            stations = viewModel.bmdStations,
                            onSelectLocation = { lat, lon, label -> viewModel.updateUserLocation(lat, lon, label) }
                        )
                        DashboardDrawerDestination.DEV_SUITE -> DeveloperSuiteView(
                            telemetry = telemetry,
                            onTestEmergencyAlert = { viewModel.triggerPresentationDemoAlarm() },
                            onTestBuzzer = { viewModel.testBuzzerPulse() },
                            onOpenConnectWizard = { viewModel.navigateTo(DashboardDrawerDestination.ESP32_WIZARD) },
                            onOpenCloudGuide = { viewModel.navigateTo(DashboardDrawerDestination.CLOUD_GUIDE) },
                            onOpenFirmware = { viewModel.navigateTo(DashboardDrawerDestination.FIRMWARE_CODE) },
                            onOpenCredits = { viewModel.navigateTo(DashboardDrawerDestination.TEAM_CREDITS) },
                            onSensitivityChanged = { viewModel.setPotentiometerSensitivity(it) },
                            onDebounceChanged = { viewModel.setSensorDebounce(it) }
                        )
                        DashboardDrawerDestination.ESP32_WIZARD -> Esp32ConnectGuide(
                            telemetry = telemetry,
                            onIpUpdated = { viewModel.setEsp32Ip(it) },
                            onTestBuzzer = { viewModel.testBuzzerPulse() },
                            onSimulateTremor = { viewModel.triggerPresentationDemoAlarm() },
                            onTestPing = { onResult -> viewModel.testEsp32Connection(telemetry.esp32Ip, onResult) }
                        )
                        DashboardDrawerDestination.CLOUD_GUIDE -> CloudBackendGuide(
                            currentGateway = telemetry.gateway,
                            onSelectGateway = { viewModel.setGateway(it) },
                            onTestFirebase = { onResult -> viewModel.testFirebaseConnection(onResult) }
                        )
                        DashboardDrawerDestination.PIN_INSPECTOR -> LivePinInspector(telemetry = telemetry)
                        DashboardDrawerDestination.SEISMIC_LOG -> SeismicActivityLogView(
                            events = historicalTremors,
                            selectedFilter = selectedFilter,
                            onFilterChanged = { viewModel.setSeverityFilter(it) },
                            onClearAll = { viewModel.clearLogHistory() },
                            onExportCsv = { viewModel.exportLogAsCsv() }
                        )
                        DashboardDrawerDestination.FIRMWARE_CODE -> FirmwareReferenceView()
                        DashboardDrawerDestination.TEAM_CREDITS -> TeamOmegaCredits()
                    }
                }
            }
        }
    }

    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = ObsidianBackground
        ) {
            SettingsPanel(
                telemetry = telemetry,
                selectedTone = "2400HZ_PULSE",
                selectedSkyTheme = manualSkyTheme,
                onSelectSkyTheme = { viewModel.setManualSkyTheme(it) },
                isDeveloperMode = isDevMode,
                onToggleDeveloperMode = { viewModel.toggleDeveloperMode() },
                onGatewaySelected = { viewModel.setGateway(it) },
                onTestEmergencyAlert = {
                    showSettingsSheet = false
                    viewModel.triggerPresentationDemoAlarm()
                },
                onSensitivityChanged = { viewModel.setPotentiometerSensitivity(it) },
                onDebounceChanged = { viewModel.setSensorDebounce(it) },
                onToneSelected = { viewModel.setAlarmTone(it) }
            )
        }
    }
}

@Composable
fun SamsungWeatherDashboard(
    telemetry: HardwareTelemetry,
    weather: MeteorologicalTelemetry,
    earthquakes: List<EarthquakeItem>,
    newsItems: List<DisasterNewsItem>,
    isDevMode: Boolean,
    onOpenMap: () -> Unit,
    onOpenDevSuite: () -> Unit,
    onQuickDemoTrigger: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Dev Mode Active Notice Pill if turned on
        if (isDevMode) {
            Surface(
                color = Color(0xFFFFB703).copy(alpha = 0.18f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFFFB703).copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenDevSuite() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Build, contentDescription = null, tint = Color(0xFFFFB703), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Developer Console Active",
                            fontSize = 12.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Surface(
                        color = Color(0xFFFFB703),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "OPEN",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // 1. Samsung Weather Hero Display
        SamsungHeroHeader(weather = weather)

        // 2. Consumer Ground Safety Status Card
        GroundSafetyCard(
            telemetry = telemetry,
            onQuickDemoTrigger = onQuickDemoTrigger
        )

        // 3. Hourly Weather Forecast Ribbon (Samsung Style)
        SamsungHourlyForecastRibbon(forecasts = weather.hourlyForecasts)

        // 4. 7-Day Localized Weather & Rain Outlook (Samsung Style)
        SamsungDailyForecastCard(forecasts = weather.dailyForecasts)

        // 5. Environmental Metrics Glass Tiles (AQI, UV, Wind, Pressure)
        SamsungWeatherMetricsGrid(weather = weather)

        // 6. Interactive Native Tectonic Map Launcher Card
        FreeMapLauncherCard(
            earthquakesCount = earthquakes.size,
            onOpenMap = onOpenMap
        )

        // 7. Disaster News Feed
        DisasterNewsSection(newsItems = newsItems)

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun SamsungHeroHeader(weather: MeteorologicalTelemetry) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = weather.locationName,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Text(
            text = weather.regionalDivision,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.8f)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "%.0f°".format(weather.temperatureCelsius),
            fontSize = 72.sp,
            fontWeight = FontWeight.Light,
            color = Color.White
        )

        Text(
            text = weather.conditionDescription,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Feels like %.0f°".format(weather.feelsLikeCelsius),
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.85f)
            )
            Text(
                text = "•",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
            Text(
                text = weather.airQualityIndex,
                fontSize = 12.sp,
                color = Color(0xFF6EE7B7),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}



@Composable
fun GroundSafetyCard(
    telemetry: HardwareTelemetry,
    onQuickDemoTrigger: () -> Unit
) {
    val isTriggered = telemetry.gpio18SensorState

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isTriggered) EmergencyCrimson.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.12f)
        ),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isTriggered) Color.White else TelemetryEmerald)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isTriggered) "EARTHQUAKE ALERT" else "Ground Status: Calm & Safe",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                val statusBadgeText = if (telemetry.isConnected) {
                    "ESP32 Linked"
                } else if (telemetry.firebaseConnected) {
                    "Firebase Online"
                } else {
                    "Hardware Offline"
                }
                val statusBadgeColor = if (telemetry.isConnected) {
                    TelemetryEmerald
                } else if (telemetry.firebaseConnected) {
                    Color(0xFF38BDF8)
                } else {
                    Color(0xFFE2E8F0)
                }

                Surface(
                    color = statusBadgeColor.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(statusBadgeColor)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = statusBadgeText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusBadgeColor,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (isTriggered)
                    "CRITICAL GROUND MOTION DETECTED! Drop, Cover, and Hold on immediately!"
                else if (telemetry.isConnected)
                    "Your ESP32 SW-420 sensor is actively monitoring for ground tremors via live telemetry stream."
                else if (telemetry.firebaseConnected)
                    "Firebase Realtime Database (seismic-guard) is active. Waiting for tremor telemetry packet from your physical ESP32."
                else
                    "ESP32 hardware is not currently linked. Real-time seismic detection relies on cloud USGS feeds until your sensor is connected.",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.9f),
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
fun SamsungHourlyForecastRibbon(forecasts: List<HourlyForecast>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "HOURLY FORECAST",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.75f),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(forecasts) { item ->
                    HourlyItemView(item = item)
                }
            }
        }
    }
}

@Composable
fun HourlyItemView(item: HourlyForecast) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(52.dp)
    ) {
        Text(
            text = item.time,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.85f),
            fontWeight = if (item.isNow) FontWeight.Bold else FontWeight.Normal
        )

        Spacer(modifier = Modifier.height(8.dp))

        Icon(
            imageVector = if (item.rainProb > 40) Icons.Default.WaterDrop else Icons.Default.WbSunny,
            contentDescription = null,
            tint = if (item.rainProb > 40) Color(0xFF38BDF8) else Color(0xFFFDE047),
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = item.temp,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        if (item.rainProb > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${item.rainProb}%",
                fontSize = 10.sp,
                color = Color(0xFF38BDF8),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun SamsungDailyForecastCard(forecasts: List<DailyForecast>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "7-DAY LOCALIZED OUTLOOK",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.75f),
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            forecasts.forEach { day ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = day.dayName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.width(60.dp)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (day.condition.contains("Rain")) Icons.Default.WaterDrop else Icons.Default.WbSunny,
                            contentDescription = null,
                            tint = if (day.condition.contains("Rain")) Color(0xFF38BDF8) else Color(0xFFFDE047),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = day.condition,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.width(110.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = "${day.minTemp}  —  ${day.maxTemp}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun SamsungWeatherMetricsGrid(weather: MeteorologicalTelemetry) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SamsungMetricTile(
                icon = Icons.Default.WbSunny,
                title = "UV INDEX",
                value = "%.1f".format(weather.uvIndex),
                subtext = "Moderate protection",
                modifier = Modifier.weight(1f)
            )
            SamsungMetricTile(
                icon = Icons.Default.WaterDrop,
                title = "HUMIDITY",
                value = "${weather.humidityPercent}%",
                subtext = "Dew point: 23°",
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SamsungMetricTile(
                icon = Icons.Default.Air,
                title = "WIND",
                value = "%.0f km/h".format(weather.windSpeedKmh),
                subtext = "Steady breeze",
                modifier = Modifier.weight(1f)
            )
            SamsungMetricTile(
                icon = Icons.Default.Thermostat,
                title = "PRESSURE",
                value = "%.0f hPa".format(weather.barometricPressureHpa),
                subtext = "Stable atmospheric level",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun SamsungMetricTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    subtext: String,
    modifier: Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = Color.White.copy(alpha = 0.75f), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.75f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = subtext, fontSize = 11.sp, color = Color.White.copy(alpha = 0.75f), lineHeight = 14.sp)
        }
    }
}

@Composable
fun FreeMapLauncherCard(earthquakesCount: Int, onOpenMap: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Map, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LIVE TECTONIC MAP",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Surface(
                    color = Color(0xFF38BDF8).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "$earthquakesCount Earthquakes",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Explore the smooth vector map showing live USGS earthquake epicenters and major Bangladesh fault lines (Madhupur, Dauki, Chittagong).",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.85f),
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onOpenMap,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.Map, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Open Interactive Map", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DisasterNewsSection(newsItems: List<DisasterNewsItem>) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Newspaper, contentDescription = null, tint = Color(0xFFFFB703), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "SEISMIC & WEATHER BULLETINS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.85f),
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        newsItems.take(2).forEach { news ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.10f)),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            color = Color(0xFF38BDF8).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = news.category,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(text = news.timestampFormatted, fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = news.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = news.summary, fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f), lineHeight = 15.sp)
                }
            }
        }
    }
}
