package com.unisight.gropos.features.settings.presentation

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
    val appVersion: String = "1.0.0",
    val deviceId: String = "",  // GUID from registration
    val ipAddress: String = "Unknown",
    val branchName: String = "",
    
    // Database Stats
    val databaseStats: DatabaseStats = DatabaseStats(),
    val isLoadingStats: Boolean = false,
    
    // Environment
    val currentEnvironment: EnvironmentType = EnvironmentType.PRODUCTION,
    val selectedEnvironment: EnvironmentType = EnvironmentType.PRODUCTION,
    
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
 * Environment types for API targeting
 */
enum class EnvironmentType(val displayName: String, val baseUrl: String) {
    PRODUCTION("Production", "https://api.gropos.com"),
    STAGING("Staging", "https://staging-api.gropos.com"),
    DEVELOPMENT("Development", "https://dev-api.gropos.com")
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

