package com.portalhost.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography

object AppTheme {
    // Portal brand colors (obsidian Nether portal identity)
    val portalViolet = Color(0xFF7C4DFF)
    val portalMagenta = Color(0xFFD500F9)
    val portalCyan = Color(0xFF40C4FF)
    val portalObsidian = Color(0xFF17131E)
    val hostGreen = Color(0xFF4ADE80)

    // Dark theme (recommended default for server dashboards)
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
        onBackground = Color(0xFFFFFFFF),
        surface = portalObsidian,
        onSurface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFF1D2230),
        onSurfaceVariant = Color(0xFFC5CAD5),
        surfaceContainerLowest = Color(0xFF0F1117),
        surfaceContainerLow = Color(0xFF131722),
        surfaceContainer = portalObsidian,
        surfaceContainerHigh = Color(0xFF1D2230),
        surfaceContainerHighest = Color(0xFF232A3D),
        error = Color(0xFFEF4444),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFF4A1414),
        onErrorContainer = Color(0xFFFFD6D6),
        outline = Color(0xFF2A3144),
        outlineVariant = Color(0xFF2E3445),
        inverseSurface = Color(0xFFFFFFFF),
        inverseOnSurface = Color(0xFF303030),
        inversePrimary = portalViolet,
        scrim = Color(0xFF000000),
        surfaceTint = portalViolet,
    )

    // Light color scheme
    private val LightColorScheme = lightColorScheme(
        primary = portalViolet,
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFEDE7FF),
        onPrimaryContainer = Color(0xFF21005D),
        secondary = Color(0xFF0080B8),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFBDE9FF),
        onSecondaryContainer = Color(0xFF003A4A),
        tertiary = Color(0xFFC400E0),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFD9FF),
        onTertiaryContainer = Color(0xFF4A0054),
        background = Color(0xFFF7F8FB),
        onBackground = Color(0xFF1C1C1C),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF1C1C1C),
        surfaceVariant = Color(0xFFF0F2F7),
        onSurfaceVariant = Color(0xFF6B7280),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF1F3F8),
        surfaceContainer = Color(0xFFECEEF4),
        surfaceContainerHigh = Color(0xFFE7E9F0),
        surfaceContainerHighest = Color(0xFFE1E4EC),
        error = Color(0xFFD32F2F),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        outline = Color(0xFFE2E8F0),
        outlineVariant = Color(0xFFE2E8F0),
        inverseSurface = Color(0xFF1C1C1C),
        inverseOnSurface = Color(0xFFF5F5F5),
        inversePrimary = Color(0xFFB39DDB),
        scrim = Color(0xFF000000),
        surfaceTint = portalViolet,
    )

    @Composable
    fun AppTheme(
        darkTheme: Boolean = false,
        content: @Composable () -> Unit
    ) {
        val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography.typography,
            content = content
        )
    }
}