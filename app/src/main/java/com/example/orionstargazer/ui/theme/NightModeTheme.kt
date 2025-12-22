package com.example.orionstargazer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Red100 = Color(0xFFFFCDD2)
val Red200 = Color(0xFFEF9A9A)
val Red300 = Color(0xFFE57373)
val Red400 = Color(0xFFEF5350)
val Red700 = Color(0xFFD32F2F)
val Red800 = Color(0xFFC62828)
val Red900 = Color(0xFFB71C1C)
val Blue50 = Color(0xFFE3F2FD)
val Blue100 = Color(0xFFBBDEFB)
val Blue200 = Color(0xFF90CAF9)
val Blue300 = Color(0xFF64B5F6)
val Blue700 = Color(0xFF1976D2)
val Blue900 = Color(0xFF0D47A1)

private val NightModeColorScheme = darkColorScheme(
    primary = Red200,
    onPrimary = Red700,
    secondary = Red300,
    background = Red900,
    surface = Red800,
    error = Red400,
    onSecondary = Red900,
    onBackground = Red400,
    onSurface = Red400,
)

private val LightModeColorScheme = lightColorScheme(
    primary = Blue200,
    onPrimary = Blue700,
    secondary = Blue300,
    background = Blue100,
    surface = Blue50,
    error = Red400,
    onSecondary = Blue900,
    onBackground = Blue900,
    onSurface = Blue900,
)

@Composable
fun OrionStargazerThemeNight(nightMode: Boolean, content: @Composable () -> Unit) {
    val colors = if (nightMode) NightModeColorScheme else LightModeColorScheme
    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}
