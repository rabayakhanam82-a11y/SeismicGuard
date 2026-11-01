package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SeismicGuardDarkNavyColorScheme = darkColorScheme(
    primary = BrandCyanTeal,
    onPrimary = Color(0xFF040714),
    primaryContainer = Color(0xFF0A1B38),
    onPrimaryContainer = BrandCyanWave,
    secondary = TelemetryEmerald,
    onSecondary = Color(0xFF040714),
    secondaryContainer = Color(0xFF042B20),
    onSecondaryContainer = TelemetryEmerald,
    tertiary = AtmosphericCyan,
    onTertiary = Color(0xFF040714),
    tertiaryContainer = Color(0xFF0A2540),
    onTertiaryContainer = AtmosphericCyan,
    background = ObsidianBackground,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = ObsidianCardBorder,
    error = EmergencyCrimson,
    onError = Color.White
)

@Composable
fun SeismicGuardTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SeismicGuardDarkNavyColorScheme,
        typography = Typography,
        content = content
    )
}

// Retain alias for backwards compatibility
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    SeismicGuardTheme(content = content)
}
