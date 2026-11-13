package com.latraverse.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val TraverseYellow = Color(0xFFECB31E)
val TraverseBlack = Color(0xFF000000)
val TraverseWhite = Color(0xFFFFFFFF)
val TraverseGrey = Color(0xFF515151)
val TraverseDarkSurface = Color(0xFF1C1C1E)

private val DarkColors = darkColorScheme(
    primary = TraverseYellow,
    onPrimary = TraverseBlack,
    primaryContainer = Color(0xFF3A2E00),
    onPrimaryContainer = TraverseYellow,
    secondary = TraverseYellow,
    onSecondary = TraverseBlack,
    background = TraverseBlack,
    onBackground = TraverseWhite,
    surface = TraverseDarkSurface,
    onSurface = TraverseWhite,
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFFC6C6C8),
    outline = Color(0xFF515151)
)

private val LightColors = lightColorScheme(
    primary = TraverseBlack,
    onPrimary = TraverseWhite,
    primaryContainer = TraverseYellow,
    onPrimaryContainer = TraverseBlack,
    secondary = Color(0xFF8D3322),
    onSecondary = TraverseWhite,
    background = TraverseWhite,
    onBackground = TraverseBlack,
    surface = TraverseWhite,
    onSurface = TraverseBlack,
    surfaceVariant = Color(0xFFF2F2F2),
    onSurfaceVariant = TraverseGrey,
    outline = Color(0xFFC6C6C6)

// Slight tweak for light cards to stay white on the grey list background
)

@Composable
fun LaTraverseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = LaTraverseTypography,
        content = content
    )
}
