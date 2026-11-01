package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.WeatherSkyTheme
import java.util.Calendar
import kotlin.random.Random

private data class Raindrop(
    val xRatio: Float,
    val yRatio: Float,
    val length: Float,
    val speed: Float,
    val alpha: Float
)

@Composable
fun ImmersiveWeatherBackground(
    skyTheme: WeatherSkyTheme,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val isNightTime = skyTheme == WeatherSkyTheme.CLEAR_NIGHT || currentHour < 6 || currentHour >= 18

    val isRainy = skyTheme == WeatherSkyTheme.RAINY || skyTheme == WeatherSkyTheme.THUNDERSTORM

    // Select the actual background photo smartly
    val bgDrawableRes = when (skyTheme) {
        WeatherSkyTheme.CLEAR_NIGHT -> R.drawable.bg_night_sky
        WeatherSkyTheme.CLEAR_DAY, WeatherSkyTheme.SUNSET -> R.drawable.bg_sunny_sky
        WeatherSkyTheme.CLOUDY_DAY -> R.drawable.bg_cloudy_sky
        WeatherSkyTheme.RAINY, WeatherSkyTheme.THUNDERSTORM -> {
            if (isNightTime) R.drawable.bg_night_sky else R.drawable.bg_cloudy_sky
        }
    }

    // Exact solid continuation color matching the bottom row of each uploaded background
    val bottomSolidColor = when (bgDrawableRes) {
        R.drawable.bg_sunny_sky -> Color(0xFF56A9F7)   // Radiant azure blue
        R.drawable.bg_cloudy_sky -> Color(0xFF4E5460)  // Slate-grey
        else -> Color(0xFF0D0326)                      // Midnight indigo/black
    }

    val infiniteTransition = rememberInfiniteTransition(label = "rain_animation")

    // Continuous downward rain offset
    val rainFallOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rain_fall"
    )

    // Lightning pulse for thunderstorms
    val lightningFlash by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "lightning_flash"
    )

    // Deterministic raindrops
    val raindrops = remember {
        val rand = Random(99)
        val list = mutableListOf<Raindrop>()
        for (i in 0 until 90) {
            list.add(
                Raindrop(
                    xRatio = rand.nextFloat(),
                    yRatio = rand.nextFloat(),
                    length = 22f + rand.nextFloat() * 26f,
                    speed = 0.8f + rand.nextFloat() * 0.5f,
                    alpha = 0.35f + rand.nextFloat() * 0.45f
                )
            )
        }
        list
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bottomSolidColor)
    ) {
        // 1. High-Resolution Photographic Sky Background (uploaded by user)
        Image(
            painter = painterResource(id = bgDrawableRes),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(720.dp),
            contentScale = ContentScale.FillWidth,
            alignment = Alignment.TopCenter
        )

        // 2. Seamless gradient transition in the lower portion of the image into the bottom solid color
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .align(Alignment.TopCenter)
                .padding(top = 540.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            bottomSolidColor
                        )
                    )
                )
        )

        // 3. Falling Rain Layer (active during rain/thunderstorm conditions)
        if (isRainy) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                raindrops.forEach { drop ->
                    val progress = (drop.yRatio + rainFallOffset * drop.speed) % 1.0f
                    val rx = (drop.xRatio * width - progress * 40f + width) % width
                    val ry = progress * height

                    drawLine(
                        color = Color(0xFFE2E8F0).copy(alpha = drop.alpha * 0.85f),
                        start = Offset(rx, ry),
                        end = Offset(rx - 8f, ry + drop.length),
                        strokeWidth = 1.6f
                    )
                }

                // Thunderstorm lightning strobe effect
                if (skyTheme == WeatherSkyTheme.THUNDERSTORM && lightningFlash in 0.86f..0.91f) {
                    val flashAlpha = if (lightningFlash in 0.87f..0.89f) 0.35f else 0.18f
                    drawRect(color = Color(0xFFE0F2FE).copy(alpha = flashAlpha))
                }
            }
        }

        // 4. Main App Screens and Scaffold
        content()
    }
}
