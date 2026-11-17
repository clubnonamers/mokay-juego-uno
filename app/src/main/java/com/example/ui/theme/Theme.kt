package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val MokayDarkColorScheme = darkColorScheme(
    primary = MokayFlame,
    onPrimary = MokayCharcoal,
    primaryContainer = Color(0xFF4A211A),
    onPrimaryContainer = MokayCream,
    secondary = MokayLavender,
    onSecondary = MokayCharcoal,
    secondaryContainer = Color(0xFF332D59),
    onSecondaryContainer = MokayCream,
    tertiary = MokaySpark,
    onTertiary = MokayCharcoal,
    background = RetroBgDark,
    onBackground = MokayCream,
    surface = RetroSurface,
    onSurface = MokayCream,
    surfaceVariant = RetroSurfaceElevated,
    onSurfaceVariant = MokayLavender,
    outline = RetroBorderLight,
    error = RetroDanger,
    onError = MokayCharcoal
)

// Crisp pixel-chamfered minimal corner shapes
private val MokayShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(10.dp)
)

@Composable
fun MokayWorldTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MokayDarkColorScheme,
        typography = MokayTypography,
        shapes = MokayShapes,
        content = content
    )
}
