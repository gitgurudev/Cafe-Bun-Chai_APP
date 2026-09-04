package com.cafebunchai.pos.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CafeColors: ColorScheme = lightColorScheme(
    primary = TeaBrown,
    onPrimary = Color.White,
    primaryContainer = ChipBg,
    onPrimaryContainer = TeaBrownDark,
    secondary = AmberBun,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8C48A),
    onSecondaryContainer = TeaBrownDark,
    background = Cream,
    onBackground = TeaBrownDark,
    surface = Foam,
    onSurface = TeaBrownDark,
    surfaceVariant = ChipBg,
    onSurfaceVariant = TeaBrown,
    error = LowStock,
    onError = Color.White,
    outline = Color(0xFFBFA58F),
)

@Composable
fun CafeBunChaiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CafeColors,
        typography = CafeTypography,
        content = content,
    )
}
