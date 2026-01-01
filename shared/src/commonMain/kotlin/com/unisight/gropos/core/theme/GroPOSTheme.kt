package com.unisight.gropos.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * GroPOS Application Theme
 * 
 * Per UI_DESIGN_SYSTEM.md: Strict adherence to the documented color palette
 * and typography system. This is the Source of Truth for all theming.
 * 
 * Color Mapping from UI_DESIGN_SYSTEM.md to Material3:
 * - Primary: PrimaryGreen (#04571B) -> primary, onPrimary
 * - Secondary: PrimaryBlue (#2073BE) -> secondary
 * - Error: DangerRed (#FA1B1B) -> error
 * - Surface: White (#FFFFFF) -> surface
 * - Background: LightGray1 (#EFF1F1) -> background
 */

// ============================================================================
// Light Color Scheme
// Per UI_DESIGN_SYSTEM.md: Primary palette for POS environment (well-lit)
// ============================================================================

private val LightColorScheme = lightColorScheme(
    // Primary: Green for success, main actions
    primary = GroPOSColors.PrimaryGreen,
    onPrimary = GroPOSColors.White,
    primaryContainer = GroPOSColors.AccentGreen,
    onPrimaryContainer = GroPOSColors.White,
    
    // Secondary: Blue for alternative actions
    secondary = GroPOSColors.PrimaryBlue,
    onSecondary = GroPOSColors.White,
    secondaryContainer = GroPOSColors.PrimaryBlueHover,
    onSecondaryContainer = GroPOSColors.White,
    
    // Tertiary: Orange for warnings, manager requests
    tertiary = GroPOSColors.WarningOrange,
    onTertiary = GroPOSColors.White,
    tertiaryContainer = GroPOSColors.WarningOrange,
    onTertiaryContainer = GroPOSColors.White,
    
    // Error: Red for danger, cancel actions
    error = GroPOSColors.DangerRed,
    onError = GroPOSColors.White,
    errorContainer = Color(0xFFFFDAD6), // Light red container
    onErrorContainer = GroPOSColors.DangerRedHover,
    
    // Background: Light gray for panels
    background = GroPOSColors.LightGray1,
    onBackground = GroPOSColors.TextPrimary,
    
    // Surface: White for cards and content areas
    surface = GroPOSColors.White,
    onSurface = GroPOSColors.TextPrimary,
    surfaceVariant = GroPOSColors.LightGray2,
    onSurfaceVariant = GroPOSColors.TextSecondary,
    
    // Outline: Gray for borders
    outline = GroPOSColors.BorderGray,
    outlineVariant = GroPOSColors.LightGray3,
    
    // Inverse colors
    inverseSurface = GroPOSColors.TextPrimary,
    inverseOnSurface = GroPOSColors.White,
    inversePrimary = GroPOSColors.AccentGreen,
    
    // Scrim for overlays
    scrim = GroPOSColors.OverlayBlack
)

// ============================================================================
// Dark Color Scheme
// For low-light environments (night mode, reduced eye strain)
// ============================================================================

private val DarkColorScheme = darkColorScheme(
    // Primary: Slightly lighter green for dark mode contrast
    primary = GroPOSColors.AccentGreen,
    onPrimary = GroPOSColors.White,
    primaryContainer = GroPOSColors.PrimaryGreen,
    onPrimaryContainer = GroPOSColors.White,
    
    // Secondary: Blue remains consistent
    secondary = GroPOSColors.PrimaryBlue,
    onSecondary = GroPOSColors.White,
    secondaryContainer = GroPOSColors.PrimaryBlueHover,
    onSecondaryContainer = GroPOSColors.White,
    
    // Tertiary: Orange for warnings
    tertiary = GroPOSColors.WarningOrange,
    onTertiary = GroPOSColors.White,
    tertiaryContainer = GroPOSColors.WarningOrange,
    onTertiaryContainer = GroPOSColors.White,
    
    // Error: Slightly brighter red for dark mode
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    
    // Background: Dark gray
    background = Color(0xFF1A1A2E),
    onBackground = GroPOSColors.White,
    
    // Surface: Slightly lighter dark gray
    surface = Color(0xFF1F1F3D),
    onSurface = GroPOSColors.White,
    surfaceVariant = Color(0xFF2D2D4A),
    onSurfaceVariant = Color(0xFFB0B0C0),
    
    // Outline: Lighter gray for dark mode
    outline = Color(0xFF6F6F8A),
    outlineVariant = Color(0xFF3D3D5C),
    
    // Inverse colors
    inverseSurface = GroPOSColors.White,
    inverseOnSurface = Color(0xFF1A1A2E),
    inversePrimary = GroPOSColors.PrimaryGreen,
    
    // Scrim for overlays
    scrim = GroPOSColors.OverlayBlack
)

// ============================================================================
// Theme Composable
// ============================================================================

/**
 * GroPOS Application Theme.
 * 
 * A professional POS theme designed for:
 * - High contrast and readability during long shifts
 * - Clear visual hierarchy with green primary actions
 * - Distinct error states for transaction issues
 * - Consistent spacing and typography
 * 
 * Per UI_DESIGN_SYSTEM.md: All colors, typography, and spacing are
 * strictly defined in the design system documents.
 * 
 * @param darkTheme Whether to use dark theme (defaults to system preference)
 * @param content The composable content to theme
 */
@Composable
fun GroPOSTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = GroPOSTypography,
        content = content
    )
}
