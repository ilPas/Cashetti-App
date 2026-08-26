package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Fintech Elegance Theme Colors
val PureWhite = Color(0xFFFFFFFF)
val TextDark = Color(0xFF1E1E1E)

// Dark Theme (Default)
val BackgroundDarkValue = Color(0xFF000000) // Pure Black
val SurfaceDarkValue = Color(0xFF121212) // Very Dark Gray for main surface
val SurfaceCardDarkValue = Color(0xFF1C1C1E) // Elevated cards
val SurfaceCardVariantDarkValue = Color(0xFF2C2C2E) // Higher elevation/borders

// Primary Brand Colors (Violet/Purple)
val PrimaryPurple = Color(0xFF8B5CF6)
val SecondaryPurple = Color(0xFFC4B5FD)

// Status Colors
val AlertRed = Color(0xFFFF4C4C)
val SuccessGreen = Color(0xFF4CAF50)
val WarningOrange = Color(0xFFF5A623)
val FixedOrange = Color(0xFFF5A623)

object AppColorPalette {
    val Background = BackgroundDarkValue
    val Surface = SurfaceDarkValue
    val SurfaceCard = SurfaceCardDarkValue
    val SurfaceCardDark = SurfaceCardVariantDarkValue
    
    val TextPrimary = PureWhite
    val TextSecondary = Color(0xFFA1A1AA) // Zinc 400
    val TextMuted = Color(0xFF71717A) // Zinc 500
    
    val TextOnPrimary = Color(0xFF000000) // Pure Black for Neo-Brutalist high contrast
    
    val Primary = PrimaryPurple
    val Secondary = SecondaryPurple
    
    val StatusExpense = AlertRed
    val StatusFixedCost = FixedOrange
    val StatusSaving = SuccessGreen
    val StatusPositive = SuccessGreen
    val StatusError = AlertRed
    val StatusWarning = WarningOrange
}
