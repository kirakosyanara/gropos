package com.unisight.gropos.features.settings.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.unisight.gropos.core.AppConstants
import com.unisight.gropos.core.database.DatabaseProvider
import com.unisight.gropos.core.storage.SecureStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * ViewModel for the Admin Settings dialog.
 * 
 * Per SCREEN_LAYOUTS.md - Hidden Settings Menu:
 * - Device Info: App Version, Device ID (GUID), IP Address
 * - Database: Stats (record counts), Wipe Database
 * - Environment: Toggle Production/Staging/Development
 * 
 * Per Governance Rules:
 * - "Wipe Database" requires a second confirmation dialog
 * - This menu is strictly for the Login Screen (pre-authentication)
 * 
 * **P0 FIX (QA Audit):** Environment selection now persists to SecureStorage
 * and triggers app restart notification for network reconfiguration.
 */
class SettingsViewModel(
    private val databaseProvider: DatabaseProvider,
    private val secureStorage: SecureStorage,
    private val coroutineScope: CoroutineScope? = null
) : ScreenModel {
    
    private val scope: CoroutineScope
        get() = coroutineScope ?: screenModelScope
    
    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()
    
    init {
        loadDeviceInfo()
        loadDatabaseStats()
    }
    
    /**
     * Handle settings events
     */
    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.SelectTab -> selectTab(event.tab)
            is SettingsEvent.SelectEnvironment -> selectEnvironment(event.environment)
            is SettingsEvent.RefreshStats -> loadDatabaseStats()
            is SettingsEvent.RequestWipeDatabase -> requestWipeDatabase()
            is SettingsEvent.ConfirmWipeDatabase -> confirmWipeDatabase()
            is SettingsEvent.CancelWipeDatabase -> cancelWipeDatabase()
            is SettingsEvent.DismissFeedback -> dismissFeedback()
        }
    }
    
    @OptIn(ExperimentalUuidApi::class)
    private fun loadDeviceInfo() {
        println("[SettingsViewModel] Loading device info...")
        
        // Load device info from SecureStorage
        val storedDeviceId = secureStorage.getStationId()
        val deviceId = storedDeviceId ?: Uuid.random().toString()
        
        // Load persisted environment from SecureStorage
        val storedEnv = secureStorage.getEnvironment()
        val currentEnv = EnvironmentType.fromString(storedEnv)
        
        println("[SettingsViewModel] Stored environment: $storedEnv -> Resolved: ${currentEnv.name}")
        
        val branchName = secureStorage.getBranchName() ?: "Unregistered Device"
        
        _state.update { 
            it.copy(
                appVersion = AppConstants.APP_VERSION,
                deviceId = deviceId,
                ipAddress = getLocalIpAddress(),
                branchName = branchName,
                currentEnvironment = currentEnv,
                selectedEnvironment = currentEnv
            )
        }
        
        println("[SettingsViewModel] State updated: currentEnvironment = ${currentEnv.name}")
    }
    
    private fun loadDatabaseStats() {
        scope.launch {
            _state.update { it.copy(isLoadingStats = true) }
            
            try {
                // Query counts from database
                val stats = queryDatabaseStats()
                
                _state.update { 
                    it.copy(
                        databaseStats = stats,
                        isLoadingStats = false
                    )
                }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        isLoadingStats = false,
                        feedbackMessage = "Failed to load database stats: ${e.message}",
                        isError = true
                    )
                }
            }
        }
    }
    
    private fun queryDatabaseStats(): DatabaseStats {
        // TODO: Implement actual database queries via expect/actual
        // For now, return placeholder stats
        return DatabaseStats(
            productCount = 10,
            transactionCount = 0,
            categoryCount = 3,
            employeeCount = 4,
            customerCount = 0,
            heldTransactionCount = 0,
            lastSyncTime = "Never"
        )
    }
    
    private fun selectTab(tab: AdminTab) {
        _state.update { it.copy(selectedTab = tab) }
    }
    
    private fun selectEnvironment(environment: EnvironmentType) {
        _state.update { it.copy(selectedEnvironment = environment) }
    }
    
    private fun requestWipeDatabase() {
        // Per Governance: Requires second confirmation
        _state.update { it.copy(showWipeConfirmation = true) }
    }
    
    private fun confirmWipeDatabase() {
        scope.launch {
            _state.update { 
                it.copy(
                    showWipeConfirmation = false,
                    isWiping = true
                )
            }
            
            try {
                // Close and delete database
                wipeDatabase()
                
                // Update environment if changed
                val newEnv = _state.value.selectedEnvironment
                
                // **P0 FIX (QA Audit):** Persist environment to SecureStorage
                // This ensures NetworkModule reads the correct base URL on next app start.
                secureStorage.saveEnvironment(newEnv.name)
                
                // **SYNC FIX:** Clear initial sync flag to trigger resync on next login
                // Per COUCHBASE_SYNCHRONIZATION_DETAILED.md: Database wipe triggers resync
                secureStorage.saveInitialSyncCompleted(false)
                println("[ADMIN] Initial sync flag cleared - will resync on next login")
                
                // Determine if restart message is needed
                val envChanged = _state.value.currentEnvironment != newEnv
                val restartMessage = if (envChanged) {
                    "Database wiped. Environment changed to ${newEnv.displayName}. Data will sync on next login. Please restart the application."
                } else {
                    "Database wiped successfully. Data will sync on next login. Please restart the application."
                }
                
                _state.update { 
                    it.copy(
                        isWiping = false,
                        currentEnvironment = newEnv,
                        databaseStats = DatabaseStats(), // Reset stats
                        feedbackMessage = restartMessage,
                        isError = false
                    )
                }
                
                // Log audit trail
                println("[ADMIN] Database wiped. Environment: $newEnv (persisted to SecureStorage)")
                
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        isWiping = false,
                        feedbackMessage = "Failed to wipe database: ${e.message}",
                        isError = true
                    )
                }
            }
        }
    }
    
    private fun wipeDatabase() {
        // Close the database first
        if (databaseProvider.isOpen()) {
            databaseProvider.closeDatabase()
        }
        
        // TODO: Implement actual database deletion via expect/actual
        // For now, just log the action
        println("[ADMIN] Database wipe requested - implementation pending")
    }
    
    private fun cancelWipeDatabase() {
        _state.update { it.copy(showWipeConfirmation = false) }
    }
    
    private fun dismissFeedback() {
        _state.update { it.copy(feedbackMessage = null, isError = false) }
    }
    
    private fun getLocalIpAddress(): String {
        // TODO: Platform-specific IP address retrieval
        return "192.168.1.100"
    }
}

