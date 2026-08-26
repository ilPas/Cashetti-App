package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    background = AppColorPalette.Background,
    surface = AppColorPalette.Surface,
    surfaceVariant = AppColorPalette.SurfaceCard,
    onBackground = AppColorPalette.TextPrimary,
    onSurface = AppColorPalette.TextPrimary,
    onSurfaceVariant = AppColorPalette.TextSecondary,
    primary = AppColorPalette.Primary,
    onPrimary = PureWhite,
    primaryContainer = Color(0xFF2E1065), // Dark purple container
    onPrimaryContainer = PureWhite,
    secondary = AppColorPalette.Secondary,
    onSecondary = PureWhite,
    secondaryContainer = AppColorPalette.SurfaceCardDark,
    onSecondaryContainer = PureWhite,
    error = AppColorPalette.StatusError,
    onError = PureWhite
)

private val LightColorScheme = lightColorScheme(
    background = AppColorPalette.Background,
    surface = AppColorPalette.Surface,
    surfaceVariant = AppColorPalette.SurfaceCard,
    onBackground = AppColorPalette.TextPrimary,
    onSurface = AppColorPalette.TextPrimary,
    onSurfaceVariant = AppColorPalette.TextSecondary,
    primary = AppColorPalette.Primary,
    onPrimary = PureWhite,
    primaryContainer = Color(0xFF2E1065),
    onPrimaryContainer = PureWhite,
    secondary = AppColorPalette.Secondary,
    onSecondary = PureWhite,
    secondaryContainer = AppColorPalette.SurfaceCardDark,
    onSecondaryContainer = PureWhite,
    error = AppColorPalette.StatusError,
    onError = PureWhite
)

@Composable
fun BudgetControlTheme(
    darkTheme: Boolean = true, // Force Dark theme based on Behance reference
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
