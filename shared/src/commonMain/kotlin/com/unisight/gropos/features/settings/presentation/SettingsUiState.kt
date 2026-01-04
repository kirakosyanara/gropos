package com.unisight.gropos.features.settings.presentation

import com.unisight.gropos.core.AppConstants

/**
 * UI State for the Admin Settings Dialog.
 * 
 * Per SCREEN_LAYOUTS.md - Hidden Settings Menu (Administration Settings):
 * - Accessible via secret trigger on Login Screen footer
 * - Contains: Device Info, Database Stats, Environment Selection
 * - Safety: Wipe Database requires confirmation dialog
 */
data class SettingsUiState(
    val selectedTab: AdminTab = AdminTab.DEVICE_INFO,
    
    // Device Info
    val appVersion: String = AppConstants.APP_VERSION,
    val deviceId: String = "",  // GUID from registration
    val ipAddress: String = "Unknown",
    val branchName: String = "",
    
    // Database Stats
    val databaseStats: DatabaseStats = DatabaseStats(),
    val isLoadingStats: Boolean = false,
    
    // Environment - default to DEVELOPMENT to match EnvironmentType.fromString(null)
    val currentEnvironment: EnvironmentType = EnvironmentType.DEVELOPMENT,
    val selectedEnvironment: EnvironmentType = EnvironmentType.DEVELOPMENT,
    
    // Wipe Confirmation
    val showWipeConfirmation: Boolean = false,
    val isWiping: Boolean = false,
    
    // Feedback
    val feedbackMessage: String? = null,
    val isError: Boolean = false
)

/**
 * Admin Settings tabs per SCREEN_LAYOUTS.md
 */
enum class AdminTab(val displayName: String) {
    DEVICE_INFO("Device Info"),
    DATABASE("Database"),
    ENVIRONMENT("Environment")
}

/**
 * Environment types for API targeting.
 * 
 * **API Architecture (per Java codebase analysis 2026-01-03):**
 * HYBRID architecture with TWO endpoints:
 * 
 * 1. `baseUrl` (APIM Gateway) - Device Registration, Heartbeat
 * 2. `posApiBaseUrl` (App Service Direct) - Cashiers, Login, Products, etc.
 * 
 * - Environment selection persists to SecureStorage
 * - Changing environment requires app restart to reconfigure ApiClient
 */
enum class EnvironmentType(
    val displayName: String,
    val baseUrl: String,       // APIM gateway (Device Registration, Heartbeat)
    val posApiBaseUrl: String  // App Service direct (Cashiers, Login, Products)
) {
    PRODUCTION(
        displayName = "Production",
        baseUrl = "https://apim-service-unisight-prod.azure-api.net",
        posApiBaseUrl = "https://app-pos-api-prod-001.azurewebsites.net"
    ),
    STAGING(
        displayName = "Staging",
        baseUrl = "https://apim-service-unisight-staging.azure-api.net",
        posApiBaseUrl = "https://app-pos-api-staging-001.azurewebsites.net"
    ),
    DEVELOPMENT(
        displayName = "Development",
        baseUrl = "https://apim-service-unisight-dev.azure-api.net",
        posApiBaseUrl = "https://app-pos-api-dev-001.azurewebsites.net"
    );
    
    companion object {
        /**
         * Gets the environment type from a stored string value.
         * Defaults to DEVELOPMENT if not found or invalid.
         */
        fun fromString(value: String?): EnvironmentType {
            return entries.find { it.name == value } ?: DEVELOPMENT
        }
    }
}

/**
 * Database statistics model
 */
data class DatabaseStats(
    val productCount: Long = 0,
    val transactionCount: Long = 0,
    val categoryCount: Long = 0,
    val employeeCount: Long = 0,
    val customerCount: Long = 0,
    val heldTransactionCount: Long = 0,
    val lastSyncTime: String = "Never"
)

/**
 * Events for Admin Settings actions
 */
sealed interface SettingsEvent {
    data class SelectTab(val tab: AdminTab) : SettingsEvent
    data class SelectEnvironment(val environment: EnvironmentType) : SettingsEvent
    data object RefreshStats : SettingsEvent
    data object RequestWipeDatabase : SettingsEvent
    data object ConfirmWipeDatabase : SettingsEvent
    data object CancelWipeDatabase : SettingsEvent
    data object DismissFeedback : SettingsEvent
}

