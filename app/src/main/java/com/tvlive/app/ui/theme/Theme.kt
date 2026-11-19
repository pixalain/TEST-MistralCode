package com.tvlive.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TvBlue = Color(0xFF5FB4FF)
private val DarkBackground = Color(0xFF0E1622)
private val DarkSurface = Color(0xFF16202E)

private val DarkColors = darkColorScheme(
    primary = TvBlue,
    onPrimary = Color(0xFF00263F),
    primaryContainer = Color(0xFF0B3A5C),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF9AA4B2),
    background = DarkBackground,
    onBackground = Color(0xFFE8EAEE),
    surface = DarkSurface,
    onSurface = Color(0xFFE8EAEE),
    surfaceVariant = Color(0xFF232F40),
    onSurfaceVariant = Color(0xFFB8BFC9),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0B5FA5),
    onPrimary = Color.White,
    background = Color(0xFFFAFBFC),
    onBackground = Color(0xFF14181F),
    surface = Color.White,
    onSurface = Color(0xFF14181F),
)

@Composable
fun TvLiveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
