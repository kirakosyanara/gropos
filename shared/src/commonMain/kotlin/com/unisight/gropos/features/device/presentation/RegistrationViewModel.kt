package com.unisight.gropos.features.device.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.unisight.gropos.features.device.data.FakeDeviceRepository
import com.unisight.gropos.features.device.domain.model.DeviceInfo
import com.unisight.gropos.features.device.domain.model.RegistrationState
import com.unisight.gropos.features.device.domain.repository.DeviceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * ViewModel for the Registration Screen.
 * 
 * Per DEVICE_REGISTRATION.md:
 * - Generates pairing code on init
 * - Manages QR code display
 * - Polls for registration status
 * - Transitions to login on successful activation
 * 
 * @param deviceRepository Repository for device registration operations
 * @param coroutineScope Scope for launching coroutines (injectable for tests)
 */
class RegistrationViewModel(
    private val deviceRepository: DeviceRepository,
    private val coroutineScope: CoroutineScope? = null
) : ScreenModel {
    
    private val scope: CoroutineScope
        get() = coroutineScope ?: screenModelScope
    
    private val _state = MutableStateFlow(RegistrationUiState())
    val state: StateFlow<RegistrationUiState> = _state.asStateFlow()
    
    // Track if we're the active registration screen (to stop polling when navigating away)
    private var isActive = true
    
    init {
        // Start clock updates
        startClockUpdates()
        
        // Generate initial pairing code
        generatePairingCode()
    }
    
    /**
     * Handle registration events.
     */
    fun onEvent(event: RegistrationEvent) {
        when (event) {
            is RegistrationEvent.RefreshPairingCode -> generatePairingCode()
            is RegistrationEvent.SimulateActivation -> simulateActivation()
            is RegistrationEvent.ShowAdminSettings -> showAdminSettings()
            is RegistrationEvent.HideAdminSettings -> hideAdminSettings()
            is RegistrationEvent.DismissError -> dismissError()
        }
    }
    
    /**
     * Generate a new pairing code and optionally request QR code from server.
     */
    private fun generatePairingCode() {
        scope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            
            try {
                // Generate local pairing code
                val pairingCode = deviceRepository.generatePairingCode()
                
                // Request QR code from server (mock for P2)
                val qrResult = deviceRepository.requestQrCode()
                
                qrResult.onSuccess { response ->
                    _state.update { 
                        it.copy(
                            pairingCode = pairingCode,
                            qrCodeImage = response.qrCodeImage,
                            deviceGuid = response.assignedGuid,
                            activationUrl = response.url?.substringAfter("://") ?: "admin.gropos.com",
                            registrationState = RegistrationState.PENDING,
                            isLoading = false
                        )
                    }
                    
                    // Start polling for status (would be done in production)
                    // For P2, we rely on "Simulate Activation" button
                    // startStatusPolling(response.assignedGuid ?: "")
                    
                }.onFailure { error ->
                    _state.update { 
                        it.copy(
                            pairingCode = pairingCode, // Still show local code
                            errorMessage = error.message ?: "Failed to get QR code",
                            registrationState = RegistrationState.ERROR,
                            isLoading = false
                        )
                    }
                }
                
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        errorMessage = "Error: ${e.message}",
                        registrationState = RegistrationState.ERROR,
                        isLoading = false
                    )
                }
            }
        }
    }
    
    /**
     * Dev tool: Simulate successful activation.
     * 
     * Per governance: Only visible in Debug/Dev mode.
     * Bypasses the cloud check for development purposes.
     */
    private fun simulateActivation() {
        scope.launch {
            _state.update { 
                it.copy(
                    isLoading = true,
                    registrationState = RegistrationState.IN_PROGRESS,
                    branchName = "Configuring..."
                )
            }
            
            // Simulate brief delay for UX
            delay(500)
            
            // Use FakeDeviceRepository's simulate activation
            val fakeRepo = deviceRepository as? FakeDeviceRepository
            if (fakeRepo != null) {
                fakeRepo.simulateActivation()
                
                // Get the simulated device info
                val deviceInfo = fakeRepo.getDeviceInfo()
                
                _state.update { 
                    it.copy(
                        isLoading = false,
                        registrationState = RegistrationState.REGISTERED,
                        branchName = deviceInfo?.branchName ?: "Development Branch",
                        stationName = "Station ${deviceInfo?.stationId?.take(8) ?: "DEV"}"
                    )
                }
            } else {
                // Fallback for real repository
                val deviceInfo = DeviceInfo(
                    stationId = "dev-${System.currentTimeMillis()}",
                    apiKey = "dev_api_key_${System.currentTimeMillis()}",
                    branchName = "Development Branch",
                    branchId = 1,
                    environment = "DEVELOPMENT",
                    registeredAt = Clock.System.now().toString()
                )
                
                deviceRepository.registerDevice(deviceInfo)
                
                _state.update { 
                    it.copy(
                        isLoading = false,
                        registrationState = RegistrationState.REGISTERED,
                        branchName = deviceInfo.branchName,
                        stationName = "Station ${deviceInfo.stationId.take(8)}"
                    )
                }
            }
        }
    }
    
    private fun showAdminSettings() {
        _state.update { it.copy(showAdminSettings = true) }
    }
    
    fun hideAdminSettings() {
        _state.update { it.copy(showAdminSettings = false) }
    }
    
    private fun dismissError() {
        _state.update { it.copy(errorMessage = null) }
    }
    
    /**
     * Update clock display.
     */
    private fun startClockUpdates() {
        scope.launch {
            while (isActive) {
                _state.update { it.copy(currentTime = getCurrentTime()) }
                delay(1000)
            }
        }
    }
    
    private fun getCurrentTime(): String {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val hour = if (now.hour == 0) 12 else if (now.hour > 12) now.hour - 12 else now.hour
        val amPm = if (now.hour >= 12) "PM" else "AM"
        return "${hour}:${now.minute.toString().padStart(2, '0')} $amPm"
    }
    
    /**
     * Check if device is registered.
     * Used by App.kt to determine initial screen.
     */
    suspend fun isDeviceRegistered(): Boolean {
        return deviceRepository.isRegistered()
    }
    
    override fun onDispose() {
        super.onDispose()
        isActive = false
    }
}

