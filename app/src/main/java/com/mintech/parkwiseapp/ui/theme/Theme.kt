package com.mintech.parkwiseapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppColorScheme = darkColorScheme(
    primary = PrimaryApp,
    onPrimary = Color.White,
    secondary = PrimaryApp,
    onSecondary = Color.White,
    background = Background,
    onBackground = Color.White,
    surface = SurfaceLow,
    onSurface = Color.White,
    surfaceVariant = SurfaceHigh,
    onSurfaceVariant = OnSurfaceVariant,
    error = ErrorApp,
    onError = Color.White
)

@Composable
fun ParkwiseAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        content = content
    )
}
