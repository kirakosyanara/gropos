package com.unisight.gropos.features.device.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.unisight.gropos.features.device.data.FakeDeviceRepository
import com.unisight.gropos.features.device.domain.model.DeviceInfo
import com.unisight.gropos.features.device.domain.model.RegistrationState
import com.unisight.gropos.features.device.domain.repository.DeviceRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * ViewModel for the Registration Screen.
 * 
 * **Per DEVICE_REGISTRATION.md:**
 * - Generates pairing code on init
 * - Manages QR code display
 * - Polls for registration status every 10 seconds
 * - Transitions to login on successful activation
 * 
 * **C3 FIX:** Implemented full status polling mechanism with:
 * - 10 second polling interval
 * - 10 minute default timeout
 * - 60 minute extended timeout when IN_PROGRESS
 * - Proper cancellation on dispose
 * 
 * @param deviceRepository Repository for device registration operations
 * @param coroutineScope Scope for launching coroutines (injectable for tests)
 */
class RegistrationViewModel(
    private val deviceRepository: DeviceRepository,
    private val coroutineScope: CoroutineScope? = null
) : ScreenModel {
    
    companion object {
        /**
         * Polling interval per DEVICE_REGISTRATION.md Section 6.
         */
        private const val POLL_INTERVAL_MS = 10_000L        // 10 seconds
        
        /**
         * Default timeout per DEVICE_REGISTRATION.md Section 6.
         * QR code expires if not scanned within this time.
         */
        private const val DEFAULT_TIMEOUT_MS = 10 * 60 * 1000L   // 10 minutes
        
        /**
         * Extended timeout per DEVICE_REGISTRATION.md Section 6.
         * Applied when admin is actively assigning branch (IN_PROGRESS state).
         */
        private const val EXTENDED_TIMEOUT_MS = 60 * 60 * 1000L  // 60 minutes
    }
    
    private val scope: CoroutineScope
        get() = coroutineScope ?: screenModelScope
    
    private val _state = MutableStateFlow(RegistrationUiState())
    val state: StateFlow<RegistrationUiState> = _state.asStateFlow()
    
    // Track if we're the active registration screen (to stop polling when navigating away)
    private var isActive = true
    
    // C3 FIX: Job reference for status polling (allows cancellation)
    private var pollingJob: Job? = null
    
    init {
        // Start clock updates
        startClockUpdates()
        
        // Check initial registration state
        checkInitialState()
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
            is RegistrationEvent.RetryAfterTimeout -> generatePairingCode()
        }
    }
    
    /**
     * Check initial registration state on screen load.
     * 
     * Per DEVICE_REGISTRATION.md: Start with LOADING state while checking database.
     */
    private fun checkInitialState() {
        scope.launch {
            _state.update { it.copy(registrationState = RegistrationState.LOADING) }
            
            try {
                if (deviceRepository.isRegistered()) {
                    val deviceInfo = deviceRepository.getDeviceInfo()
                    _state.update {
                        it.copy(
                            registrationState = RegistrationState.REGISTERED,
                            branchName = deviceInfo?.branchName ?: "Unknown Branch",
                            stationName = "Station ${deviceInfo?.stationId?.take(8) ?: "?"}"
                        )
                    }
                } else {
                    // Not registered, generate pairing code
                    generatePairingCode()
                }
            } catch (e: Exception) {
                // Error checking state, proceed to registration
                generatePairingCode()
            }
        }
    }
    
    /**
     * Generate a new pairing code and request QR code from server.
     * 
     * Per DEVICE_REGISTRATION.md Section 4.1:
     * - POST /device-registration/qr-registration
     * - Receive QR code image and temporary access token
     * - Start status polling with the assigned device GUID
     */
    private fun generatePairingCode() {
        // Cancel any existing polling
        pollingJob?.cancel()
        
        scope.launch {
            _state.update { 
                it.copy(
                    isLoading = true, 
                    errorMessage = null,
                    registrationState = RegistrationState.LOADING
                ) 
            }
            
            try {
                // Request QR code from server
                val qrResult = deviceRepository.requestQrCode()
                
                qrResult.onSuccess { response ->
                    val deviceGuid = response.assignedGuid
                    
                    // Extract activation code from URL (last path segment)
                    // URL format: https://dev.unisight.io/admin/hardware/devices/activation/1029c783
                    val activationCode = response.url?.substringAfterLast("/")?.uppercase() ?: ""
                    val displayUrl = response.url?.substringAfter("://") ?: "admin.gropos.com"
                    
                    _state.update { 
                        it.copy(
                            pairingCode = activationCode,  // Use backend activation code, not local random
                            qrCodeImage = response.qrCodeImage,
                            deviceGuid = deviceGuid,
                            activationUrl = displayUrl,
                            registrationState = RegistrationState.PENDING,
                            isLoading = false
                        )
                    }
                    
                    // C3 FIX: Start status polling with the device GUID
                    if (!deviceGuid.isNullOrEmpty()) {
                        startStatusPolling(deviceGuid)
                    }
                    
                }.onFailure { error ->
                    // On error, generate a local fallback code
                    val fallbackCode = deviceRepository.generatePairingCode()
                    _state.update { 
                        it.copy(
                            pairingCode = fallbackCode,
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
     * Start polling for device registration status.
     * 
     * **C3 FIX:** Full implementation per DEVICE_REGISTRATION.md Section 6:
     * - Poll every 10 seconds
     * - Default timeout: 10 minutes
     * - Extended timeout: 60 minutes when IN_PROGRESS (admin assigning branch)
     * - Cancel on dispose or new pairing code generation
     * 
     * @param deviceGuid The device GUID from the QR registration response
     */
    private fun startStatusPolling(deviceGuid: String) {
        pollingJob?.cancel()
        
        pollingJob = scope.launch {
            val startTime = Clock.System.now().toEpochMilliseconds()
            var currentTimeout = DEFAULT_TIMEOUT_MS
            
            while (isActive && this@RegistrationViewModel.isActive) {
                val elapsedTime = Clock.System.now().toEpochMilliseconds() - startTime
                
                // Check for timeout
                if (elapsedTime >= currentTimeout) {
                    _state.update {
                        it.copy(
                            registrationState = RegistrationState.TIMEOUT,
                            errorMessage = "Registration timed out. Please try again."
                        )
                    }
                    break
                }
                
                try {
                    val statusResult = deviceRepository.checkRegistrationStatus(deviceGuid)
                    
                    statusResult.onSuccess { response ->
                        println("[REGISTRATION] Poll response: status=${response.deviceStatus}, branch=${response.branch}, hasApiKey=${response.apiKey != null}")
                        
                        when (response.deviceStatus) {
                            "Pending" -> {
                                // Still waiting for admin to scan QR - continue polling
                                // State already PENDING, no update needed
                            }
                            "In-Progress" -> {
                                // Admin has scanned, assigning branch
                                // Extend timeout to 60 minutes
                                currentTimeout = EXTENDED_TIMEOUT_MS
                                _state.update {
                                    it.copy(
                                        registrationState = RegistrationState.IN_PROGRESS,
                                        branchName = response.branch ?: "Configuring..."
                                    )
                                }
                            }
                            "Registered", "Complete" -> {
                                // Registration complete! Save credentials and transition
                                // Note: Backend may return "Complete" or "Registered"
                                handleRegistrationComplete(deviceGuid, response)
                                return@launch  // Exit polling loop
                            }
                        }
                    }.onFailure { error ->
                        // Log error but continue polling (transient network issues)
                        println("[REGISTRATION] Status poll failed: ${error.message}")
                    }
                    
                } catch (e: CancellationException) {
                    throw e  // Re-throw cancellation
                } catch (e: Exception) {
                    // Log but continue polling
                    println("[REGISTRATION] Polling error: ${e.message}")
                }
                
                // Wait before next poll
                delay(POLL_INTERVAL_MS)
            }
        }
    }
    
    /**
     * Handle successful registration completion.
     * 
     * Per DEVICE_REGISTRATION.md Section 5:
     * - Save API key and branch info to SecureStorage
     * - Transition to REGISTERED state
     * - UI will navigate to LoginScreen
     * 
     * @param deviceGuid The device GUID (becomes stationId)
     * @param response The status response with apiKey and branch info
     */
    private suspend fun handleRegistrationComplete(
        deviceGuid: String,
        response: com.unisight.gropos.features.device.domain.model.DeviceStatusResponse
    ) {
        val apiKey = response.apiKey
        if (apiKey.isNullOrEmpty()) {
            _state.update {
                it.copy(
                    registrationState = RegistrationState.ERROR,
                    errorMessage = "Registration failed: No API key received"
                )
            }
            return
        }
        
        val deviceInfo = DeviceInfo(
            stationId = deviceGuid,  // C4 FIX: Use assignedGuid as stationId
            apiKey = apiKey,
            branchName = response.branch ?: "Unknown Branch",
            branchId = response.branchId ?: -1,
            environment = "PRODUCTION",  // TODO: Get from config
            registeredAt = Clock.System.now().toString()
        )
        
        val saveResult = deviceRepository.registerDevice(deviceInfo)
        
        saveResult.onSuccess {
            _state.update {
                it.copy(
                    registrationState = RegistrationState.REGISTERED,
                    branchName = deviceInfo.branchName,
                    stationName = "Station ${deviceGuid.take(8)}",
                    errorMessage = null
                )
            }
        }.onFailure { error ->
            _state.update {
                it.copy(
                    registrationState = RegistrationState.ERROR,
                    errorMessage = "Failed to save credentials: ${error.message}"
                )
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
        // C3 FIX: Cancel polling when screen is disposed
        pollingJob?.cancel()
        pollingJob = null
    }
}

