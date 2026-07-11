package com.portalhost.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portalhost.app.R

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryGreen,
    onPrimary = Color.White,
    secondary = SecondaryTeal,
    tertiary = PrimaryGreen,
    background = BgDark,
    surface = SurfaceDark,
    onBackground = Color.White,
    onSurface = Color.White,
    error = ErrorRed,
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryDarkGreen,
    onPrimary = Color.White,
    secondary = SecondaryTeal,
    background = Color.White,
    surface = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black,
)

val PixelFont = FontFamily(
    Font(R.font.minecraft)
)

private val PixelTypography = Typography(
    headlineLarge = TextStyle(fontFamily = PixelFont, fontSize = 24.sp),
    headlineMedium = TextStyle(fontFamily = PixelFont, fontSize = 20.sp),
    headlineSmall = TextStyle(fontFamily = PixelFont, fontSize = 18.sp),
    titleLarge = TextStyle(fontFamily = PixelFont, fontSize = 18.sp),
    titleMedium = TextStyle(fontFamily = PixelFont, fontSize = 16.sp),
    titleSmall = TextStyle(fontFamily = PixelFont, fontSize = 14.sp),
)

val PortalHostShapes = Shapes(
    extraLarge = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(4.dp)
)

@Composable
fun PortalHostTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val useDynamic = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colorScheme = when {
        useDynamic && darkTheme -> dynamicDarkColorScheme(LocalContext.current)
        useDynamic && !darkTheme -> dynamicLightColorScheme(LocalContext.current)
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = PortalHostShapes,
        typography = PixelTypography,
        content = content
    )
}
