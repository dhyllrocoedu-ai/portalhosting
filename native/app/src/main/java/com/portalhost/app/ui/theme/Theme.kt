package com.portalhost.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkGreen = Color(0xFF1B5E1A)
private val LightGreen = Color(0xFF4CAF50)
private val DarkBg = Color(0xFF121212)
private val SurfaceDark = Color(0xFF1E1E1E)
private val TerminalGreen = Color(0xFF00FF41)
private val TerminalBg = Color(0xFF0D0D0D)

private val DarkColorScheme = darkColorScheme(
    primary = LightGreen,
    onPrimary = Color.White,
    secondary = Color(0xFF03DAC6),
    background = DarkBg,
    surface = SurfaceDark,
    onBackground = Color.White,
    onSurface = Color.White,
    error = Color(0xFFCF6679),
)

private val LightColorScheme = lightColorScheme(
    primary = DarkGreen,
    onPrimary = Color.White,
    secondary = Color(0xFF03DAC6),
    background = Color.White,
    surface = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black,
)

@Composable
fun PortalHostTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        content = content
    )
}
