package com.unisight.gropos.core

/**
 * Application-wide constants.
 * 
 * Centralized location for version, copyright, and other
 * hardcoded values that should be consistent across the app.
 */
object AppConstants {
    /**
     * Application version string.
     * 
     * TODO: Replace with BuildKonfig.VERSION when build configuration is set up.
     */
    const val APP_VERSION = "v1.0.0-alpha"
    
    /**
     * Copyright year for footer display.
     */
    const val COPYRIGHT_YEAR = "2024"
    
    /**
     * Company name for copyright notice.
     */
    const val COMPANY_NAME = "Unisight BIT"
    
    /**
     * Full copyright notice for display in footers.
     */
    val COPYRIGHT_NOTICE: String
        get() = "© $COMPANY_NAME $COPYRIGHT_YEAR"
    
    /**
     * Full version and copyright string for Login footer.
     */
    val VERSION_COPYRIGHT: String
        get() = "$APP_VERSION • $COPYRIGHT_NOTICE"
}

