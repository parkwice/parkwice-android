package com.mintech.parkwiseapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Parkwise is a strictly dark-themed app based on your UI designs!
private val DarkColorPalette = darkColors(
    primary = PrimaryApp,
    primaryVariant = PrimaryApp,
    secondary = PrimaryApp,
    background = Background,
    surface = SurfaceLow,
    error = ErrorApp,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun ParkwiseAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(), // Keeping the parameter for standard Compose compatibility
    content: @Composable () -> Unit
) {
    // We force the DarkColorPalette here to ensure the app always looks like the original iOS/Flutter design
    val colors = DarkColorPalette

    MaterialTheme(
        colors = colors,
        // If you have a Type.kt file, you can uncomment the next line:
        // typography = Typography,
        // If you have a Shape.kt file, you can uncomment the next line:
        // shapes = Shapes,
        content = content
    )
}