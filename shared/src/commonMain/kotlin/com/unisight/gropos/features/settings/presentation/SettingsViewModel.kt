package com.unisight.gropos.features.settings.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.unisight.gropos.core.database.DatabaseProvider
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
 */
class SettingsViewModel(
    private val databaseProvider: DatabaseProvider,
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
        // TODO: Load actual device info from database/preferences
        // For now, generate/use placeholder values
        val deviceId = Uuid.random().toString()
        
        _state.update { 
            it.copy(
                appVersion = "1.0.0-alpha",
                deviceId = deviceId,
                ipAddress = getLocalIpAddress(),
                branchName = "Development Branch",
                currentEnvironment = EnvironmentType.DEVELOPMENT,
                selectedEnvironment = EnvironmentType.DEVELOPMENT
            )
        }
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
                
                _state.update { 
                    it.copy(
                        isWiping = false,
                        currentEnvironment = newEnv,
                        databaseStats = DatabaseStats(), // Reset stats
                        feedbackMessage = "Database wiped successfully. Restart the application.",
                        isError = false
                    )
                }
                
                // Log audit trail
                println("[ADMIN] Database wiped. Environment: $newEnv")
                
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

