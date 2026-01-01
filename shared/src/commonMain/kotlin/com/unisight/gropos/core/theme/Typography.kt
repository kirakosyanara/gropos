package com.unisight.gropos.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * GroPOS Typography System
 * 
 * Per UI_DESIGN_SYSTEM.md:
 * - Font Family: Archivo (fallback to system sans-serif for KMP)
 * - Font Sizes: Base 16.sp, Small 12.sp, Medium 20.sp, Large 24.sp, 
 *               XL 26.sp, XXL 32.sp, XXXL 35.sp
 * 
 * Note: Custom font loading requires expect/actual for KMP resources.
 * For the Walking Skeleton, we use the default font family.
 * TODO: Add Archivo font files and implement expect/actual for font loading.
 */

/**
 * Font size constants per UI_DESIGN_SYSTEM.md
 */
object GroPOSFontSize {
    /** Base: 16.sp - Default text */
    val Base = 16.sp
    
    /** Small: 12.sp - Multi-row button text */
    val Small = 12.sp
    
    /** Medium: 20.sp - Secondary buttons, lock button */
    val Medium = 20.sp
    
    /** Large: 24.sp - Button large, labels */
    val Large = 24.sp
    
    /** XL: 26.sp - Price info text, discount info */
    val XL = 26.sp
    
    /** XXL: 32.sp - Ten-key buttons */
    val XXL = 32.sp
    
    /** XXXL: 35.sp - Error dialog text, change dialog labels */
    val XXXL = 35.sp
}

/**
 * GroPOS Typography Configuration
 * 
 * Mapped to Material3 Typography with GroPOS-specific sizes.
 */
val GroPOSTypography = Typography(
    // displayLarge: XXXL (35.sp) - Error dialog text, change dialog labels
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif, // TODO: Replace with ArchivoFontFamily
        fontWeight = FontWeight.Bold,
        fontSize = GroPOSFontSize.XXXL,
        lineHeight = 42.sp
    ),
    
    // headlineLarge: XXL (32.sp) - Ten-key buttons
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = GroPOSFontSize.XXL,
        lineHeight = 40.sp
    ),
    
    // headlineMedium: XL (26.sp) - Price info text, discount info
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = GroPOSFontSize.XL,
        lineHeight = 32.sp
    ),
    
    // titleLarge: Large (24.sp) - Button large, labels
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = GroPOSFontSize.Large,
        lineHeight = 28.sp
    ),
    
    // bodyLarge: Medium (20.sp) - Secondary buttons, lock button
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = GroPOSFontSize.Medium,
        lineHeight = 24.sp
    ),
    
    // bodyMedium: Base (16.sp) - Default text
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = GroPOSFontSize.Base,
        lineHeight = 20.sp
    ),
    
    // bodySmall: Small (12.sp) - Multi-row button text
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = GroPOSFontSize.Small,
        lineHeight = 16.sp
    ),
    
    // labelLarge: Used for button text
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = GroPOSFontSize.Base,
        lineHeight = 20.sp
    ),
    
    // labelMedium: Used for smaller labels
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp
    ),
    
    // labelSmall: Used for badges and tiny labels
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )
)

