package com.unisight.gropos.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ============================================================================
// Color Palette - Professional POS System
// ============================================================================

// Primary: Deep Teal - Professional, trustworthy, easy on eyes during long shifts
private val PrimaryLight = Color(0xFF006B5A)
private val OnPrimaryLight = Color(0xFFFFFFFF)
private val PrimaryContainerLight = Color(0xFF7CF8DB)
private val OnPrimaryContainerLight = Color(0xFF002019)

// Secondary: Warm Amber - Highlights, call-to-action
private val SecondaryLight = Color(0xFF8B5000)
private val OnSecondaryLight = Color(0xFFFFFFFF)
private val SecondaryContainerLight = Color(0xFFFFDDB8)
private val OnSecondaryContainerLight = Color(0xFF2C1600)

// Tertiary: Slate Blue - Information, badges
private val TertiaryLight = Color(0xFF4A5F7A)
private val OnTertiaryLight = Color(0xFFFFFFFF)
private val TertiaryContainerLight = Color(0xFFD2E4FF)
private val OnTertiaryContainerLight = Color(0xFF031C35)

// Error: Vibrant Red - Clear visibility for errors
private val ErrorLight = Color(0xFFBA1A1A)
private val OnErrorLight = Color(0xFFFFFFFF)
private val ErrorContainerLight = Color(0xFFFFDAD6)
private val OnErrorContainerLight = Color(0xFF410002)

// Background/Surface
private val BackgroundLight = Color(0xFFFBFDF9)
private val OnBackgroundLight = Color(0xFF191C1B)
private val SurfaceLight = Color(0xFFFBFDF9)
private val OnSurfaceLight = Color(0xFF191C1B)
private val SurfaceVariantLight = Color(0xFFDBE5E0)
private val OnSurfaceVariantLight = Color(0xFF3F4945)

// Outline
private val OutlineLight = Color(0xFF6F7975)
private val OutlineVariantLight = Color(0xFFBFC9C4)

// Dark Theme Colors
private val PrimaryDark = Color(0xFF5FDBBE)
private val OnPrimaryDark = Color(0xFF00382D)
private val PrimaryContainerDark = Color(0xFF005143)
private val OnPrimaryContainerDark = Color(0xFF7CF8DB)

private val SecondaryDark = Color(0xFFFFB95F)
private val OnSecondaryDark = Color(0xFF4A2800)
private val SecondaryContainerDark = Color(0xFF693C00)
private val OnSecondaryContainerDark = Color(0xFFFFDDB8)

private val TertiaryDark = Color(0xFFB2C8E8)
private val OnTertiaryDark = Color(0xFF1B314B)
private val TertiaryContainerDark = Color(0xFF324862)
private val OnTertiaryContainerDark = Color(0xFFD2E4FF)

private val ErrorDark = Color(0xFFFFB4AB)
private val OnErrorDark = Color(0xFF690005)
private val ErrorContainerDark = Color(0xFF93000A)
private val OnErrorContainerDark = Color(0xFFFFDAD6)

private val BackgroundDark = Color(0xFF191C1B)
private val OnBackgroundDark = Color(0xFFE1E3E0)
private val SurfaceDark = Color(0xFF191C1B)
private val OnSurfaceDark = Color(0xFFE1E3E0)
private val SurfaceVariantDark = Color(0xFF3F4945)
private val OnSurfaceVariantDark = Color(0xFFBFC9C4)

private val OutlineDark = Color(0xFF89938E)
private val OutlineVariantDark = Color(0xFF3F4945)

// ============================================================================
// Color Schemes
// ============================================================================

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark
)

// ============================================================================
// Theme Composable
// ============================================================================

/**
 * GroPOS application theme.
 * 
 * A professional, accessible theme designed for POS environments:
 * - High contrast for readability during long shifts
 * - Clear error states for transaction issues
 * - Warm accents for call-to-action elements
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
        content = content
    )
}

