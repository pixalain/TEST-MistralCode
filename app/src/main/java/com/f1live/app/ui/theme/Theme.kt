package com.f1live.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val F1Red = Color(0xFFE10600)
private val DarkBackground = Color(0xFF0B0E13)
private val DarkSurface = Color(0xFF151A23)

private val DarkColors = darkColorScheme(
    primary = F1Red,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3D0A10),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF9AA4B2),
    onSecondary = Color.Black,
    background = DarkBackground,
    onBackground = Color(0xFFE8EAEE),
    surface = DarkSurface,
    onSurface = Color(0xFFE8EAEE),
    surfaceVariant = Color(0xFF232A36),
    onSurfaceVariant = Color(0xFFB8BFC9)
)

private val LightColors = lightColorScheme(
    primary = F1Red,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDADD),
    onPrimaryContainer = Color(0xFF410002),
    background = Color(0xFFFAFBFC),
    onBackground = Color(0xFF14181F),
    surface = Color.White,
    onSurface = Color(0xFF14181F),
    surfaceVariant = Color(0xFFE9EDF2),
    onSurfaceVariant = Color(0xFF4A5462)
)

@Composable
fun F1LiveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme ->
            dynamicDarkColorScheme(LocalContext.current)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            dynamicLightColorScheme(LocalContext.current)
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
