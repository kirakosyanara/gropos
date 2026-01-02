package com.unisight.gropos.core.theme

import androidx.compose.ui.graphics.Color

/**
 * GroPOS Color Palette
 * 
 * Per UI_DESIGN_SYSTEM.md: Strict adherence to the documented color values.
 * This is the Source of Truth for all colors in the application.
 */
object GroPOSColors {
    // ========================================================================
    // Primary Colors (Green - Main brand, success states)
    // ========================================================================
    
    /** Primary Green: #04571B - Primary actions, success states, branding */
    val PrimaryGreen = Color(0xFF04571B)
    
    /** Primary Green Hover: #1C5733 - Selected states, button hover */
    val PrimaryGreenHover = Color(0xFF1C5733)
    
    /** Accent Green: #327D4F - Secondary accents, text highlights */
    val AccentGreen = Color(0xFF327D4F)
    
    // ========================================================================
    // Secondary Colors (Functional colors)
    // ========================================================================
    
    /** Primary Blue: #2073BE - Primary alternative actions */
    val PrimaryBlue = Color(0xFF2073BE)
    
    /** Primary Blue Hover: #135B9C - Button hover state */
    val PrimaryBlueHover = Color(0xFF135B9C)
    
    /** Warning Orange: #FE793A - Manager requests, lock button, alerts */
    val WarningOrange = Color(0xFFFE793A)
    
    /** Danger Red: #FA1B1B - Cancel, delete, danger actions */
    val DangerRed = Color(0xFFFA1B1B)
    
    /** Danger Red Hover: #D20707 - Danger button hover */
    val DangerRedHover = Color(0xFFD20707)
    
    // ========================================================================
    // Neutral Colors (Backgrounds, surfaces, text)
    // ========================================================================
    
    /** White: #FFFFFF - Backgrounds, cards */
    val White = Color(0xFFFFFFFF)
    
    /** Light Gray 1: #EFF1F1 - Right panel background */
    val LightGray1 = Color(0xFFEFF1F1)
    
    /** Light Gray 2: #E1E3E3 - Left panel background */
    val LightGray2 = Color(0xFFE1E3E3)
    
    /** Light Gray 3: #ECEFF6 - Scroll bar background */
    val LightGray3 = Color(0xFFECEFF6)
    
    /** Border Gray: #857370 - Borders, secondary buttons */
    val BorderGray = Color(0xFF857370)
    
    /** Disabled Gray: #D9D9D9 - Disabled states */
    val DisabledGray = Color(0xFFD9D9D9)
    
    /** Text Primary: #000000 - Primary text */
    val TextPrimary = Color(0xFF000000)
    
    /** Text Secondary: #A0A0A0 - Secondary text */
    val TextSecondary = Color(0xFFA0A0A0)
    
    /** Overlay Black: #000000B2 - Modal overlays (70% opacity) */
    val OverlayBlack = Color(0xB2000000)
    
    // ========================================================================
    // Accent Colors (Special purpose)
    // ========================================================================
    
    /** Purple Border: #7A55B3 - Multi-row button borders (lookup items) */
    val PurpleBorder = Color(0xFF7A55B3)
    
    // ========================================================================
    // SNAP/EBT Colors (For eligibility indicators)
    // ========================================================================
    
    /** SNAP Green: #2E7D32 - SNAP eligible badge */
    val SnapGreen = Color(0xFF2E7D32)
    
    /** SNAP Badge Background: #E8F5E9 - SNAP badge background */
    val SnapBadgeBackground = Color(0xFFE8F5E9)
    
    // ========================================================================
    // Container Colors (Surface variants for elevation/highlighting)
    // ========================================================================
    
    /** ContainerHigh: #E8F5E9 - Subtle highlight for newest/most recent items */
    val ContainerHigh = Color(0xFFE8F5E9)
    
    // ========================================================================
    // Savings Colors
    // ========================================================================
    
    /** Savings Red: #D32F2F - Savings/discount text */
    val SavingsRed = Color(0xFFD32F2F)
}

