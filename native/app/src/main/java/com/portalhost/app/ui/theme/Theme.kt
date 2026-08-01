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

private val PortalGreen = Color(0xFF2E7D32)
private val PortalGreenLight = Color(0xFF81C784)
private val PortalGreenContainer = Color(0xFFA5D6A7)
private val PortalGreenDark = Color(0xFF1B5E20)
private val PortalDarkBg = Color(0xFF121212)
private val PortalSurfaceDark = Color(0xFF1E1E1E)

private val DarkColorScheme = darkColorScheme(
    primary = PortalGreenLight,
    onPrimary = PortalGreenDark,
    primaryContainer = PortalGreenDark,
    onPrimaryContainer = PortalGreenContainer,
    secondary = portalCyan,
    onSecondary = Color(0xFF00363A),
    secondaryContainer = Color(0xFF006064),
    onSecondaryContainer = Color(0xFFB2EBF2),
    tertiary = Color(0xFFB39DDB),
    onTertiary = Color(0xFF21005D),
    tertiaryContainer = Color(0xFF4A148C),
    onTertiaryContainer = Color(0xFFEDE7FF),
    background = PortalDarkBg,
    surface = PortalSurfaceDark,
    onBackground = Color.White,
    onSurface = Color.White,
    error = Color(0xFFEF5350),
    onError = Color(0xFF1C1C1C),
)

private val LightColorScheme = lightColorScheme(
    primary = PortalGreen,
    onPrimary = Color.White,
    primaryContainer = PortalGreenContainer,
    onPrimaryContainer = PortalGreenDark,
    secondary = Color(0xFF0097A7),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB2EBF2),
    onSecondaryContainer = Color(0xFF00363A),
    tertiary = portalViolet,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEDE7FF),
    onTertiaryContainer = Color(0xFF21005D),
    background = Color(0xFFF5F5F5),
    surface = Color.White,
    onBackground = Color(0xFF1C1C1C),
    onSurface = Color(0xFF1C1C1C),
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
