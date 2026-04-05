package com.mouli.habittracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = AccentBlue,
    secondary = CrystalBlue,
    tertiary = IceBlue,
    background = SkyMist,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = InkBlue,
    onBackground = InkBlue,
    onSurface = InkBlue
)

private val DarkColors = darkColorScheme(
    primary = CrystalBlue,
    secondary = MistyBlue,
    tertiary = AccentBlue,
    background = DeepNavy,
    surface = Color(0xFF1B2E42),
    onPrimary = InkBlue,
    onSecondary = InkBlue,
    onBackground = PandaSnow,
    onSurface = PandaSnow
)

@Composable
fun HabTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = HabTypography,
        content = content
    )
}

@Composable
fun habBackgroundBrush(): Brush {
    val colors = if (isSystemInDarkTheme()) {
        listOf(Color(0xFF102133), Color(0xFF1A2B3E), Color(0xFF24384D))
    } else {
        listOf(Color(0xFFF8FCFF), Color(0xFFEAF4FF), Color(0xFFF3F8FF))
    }
    return Brush.verticalGradient(colors)
}

@Composable
fun glassBrush(): Brush {
    val colors = if (isSystemInDarkTheme()) {
        listOf(Color.White.copy(alpha = 0.12f), Color(0xFF89BFF2).copy(alpha = 0.08f))
    } else {
        listOf(Color.White.copy(alpha = 0.96f), Color(0xFFF0F7FF))
    }
    return Brush.linearGradient(colors)
}
