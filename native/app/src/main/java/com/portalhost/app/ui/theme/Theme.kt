package com.portalhost.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Portal accent colors (match the PortalHost logo)
val portalViolet = Color(0xFF7C4DFF)
val portalCyan = Color(0xFF40C4FF)
val portalObsidian = Color(0xFF17131E)
val portalMagenta = Color(0xFFD500F9)
val hostGreen = Color(0xFF4ADE80)

private val DarkColorScheme = darkColorScheme(
    primary = portalViolet,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF4B3B8C),
    onPrimaryContainer = Color(0xFFE9E1FF),
    secondary = portalCyan,
    onSecondary = Color(0xFF003A4A),
    secondaryContainer = Color(0xFF00506B),
    onSecondaryContainer = Color(0xFFBDE9FF),
    tertiary = portalMagenta,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF7A0086),
    onTertiaryContainer = Color(0xFFFFD8FF),
    background = Color(0xFF0F1117),
    surface = portalObsidian,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFC5CAD5),
    surfaceVariant = Color(0xFF1D2230),
    surfaceContainerLow = Color(0xFF131722),
    surfaceContainer = portalObsidian,
    surfaceContainerHigh = Color(0xFF1D2230),
    surfaceContainerHighest = Color(0xFF232A3D),
    outline = Color(0xFF2A3144),
    outlineVariant = Color(0xFF2E3445),
    error = Color(0xFFEF4444),
    onError = Color(0xFFFFFFFF),
)

private val LightColorScheme = lightColorScheme(
    primary = portalViolet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE7FF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF0080B8),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFBDE9FF),
    onSecondaryContainer = Color(0xFF003A4A),
    tertiary = Color(0xFFC400E0),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD9FF),
    onTertiaryContainer = Color(0xFF4A0054),
    background = Color(0xFFF7F8FB),
    surface = Color.White,
    onBackground = Color(0xFF1C1C1C),
    onSurface = Color(0xFF1C1C1C),
    onSurfaceVariant = Color(0xFF6B7280),
    surfaceVariant = Color(0xFFF0F2F7),
    error = Color(0xFFD32F2F),
    onError = Color.White,
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
