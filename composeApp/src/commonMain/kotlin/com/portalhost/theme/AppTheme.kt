package com.portalhost.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography

object AppTheme {
    // Light color scheme
    private val LightColorScheme = lightColorScheme(
        primary = Color(0xFF2E7D32),
        primaryContainer = Color(0xFFA5D6A7),
        secondary = Color(0xFF424242),
        secondaryContainer = Color(0xFFE0E0E0),
        tertiary = Color(0xFF00695C),
        tertiaryContainer = Color(0xFF80CBC4),
        background = Color(0xFFF5F5F5),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFF5F5F5),
        error = Color(0xFFD32F2F),
        onPrimary = Color(0xFFFFFFFF),
        onPrimaryContainer = Color(0xFF1B5E20),
        onSecondary = Color(0xFFFFFFFF),
        onSecondaryContainer = Color(0xFF1C1C1C),
        onTertiary = Color(0xFFFFFFFF),
        onBackground = Color(0xFF1C1C1C),
        onSurface = Color(0xFF1C1C1C),
        onSurfaceVariant = Color(0xFF424242),
        onError = Color(0xFFFFFFFF),
        outline = Color(0xFF757575),
        outlineVariant = Color(0xFFBDBDBD),
        inverseSurface = Color(0xFF303030),
        inverseOnSurface = Color(0xFFF5F5F5),
        inversePrimary = Color(0xFF81C784),
        scrim = Color(0xFF000000),
        surfaceTint = Color(0xFF2E7D32),
    )

    // Dark color scheme
    private val DarkColorScheme = darkColorScheme(
        primary = Color(0xFF81C784),
        primaryContainer = Color(0xFF1B5E20),
        secondary = Color(0xFFBDBDBD),
        secondaryContainer = Color(0xFF424242),
        tertiary = Color(0xFF4DB6AC),
        tertiaryContainer = Color(0xFF004D40),
        background = Color(0xFF121212),
        surface = Color(0xFF1E1E1E),
        surfaceVariant = Color(0xFF424242),
        error = Color(0xFFEF5350),
        onPrimary = Color(0xFF1B5E20),
        onPrimaryContainer = Color(0xFFA5D6A7),
        onSecondary = Color(0xFF1C1C1C),
        onSecondaryContainer = Color(0xFFE0E0E0),
        onTertiary = Color(0xFF004D40),
        onBackground = Color(0xFFFFFFFF),
        onSurface = Color(0xFFFFFFFF),
        onSurfaceVariant = Color(0xFFBDBDBD),
        onError = Color(0xFF1C1C1C),
        outline = Color(0xFF9E9E9E),
        outlineVariant = Color(0xFF757575),
        inverseSurface = Color(0xFFE0E0E0),
        inverseOnSurface = Color(0xFF303030),
        inversePrimary = Color(0xFF2E7D32),
        scrim = Color(0xFF000000),
        surfaceTint = Color(0xFF81C784),
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