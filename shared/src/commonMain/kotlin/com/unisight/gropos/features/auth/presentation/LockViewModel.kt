package com.unisight.gropos.features.auth.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.unisight.gropos.core.auth.ApiAuthService
import com.unisight.gropos.core.auth.DeviceEventType
import com.unisight.gropos.core.auth.DeviceLockRequest
import com.unisight.gropos.core.auth.VerifyPasswordRequest
import com.unisight.gropos.core.session.InactivityManager
import com.unisight.gropos.core.session.LockEventType
import com.unisight.gropos.features.cashier.domain.service.CashierSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * ViewModel for the Lock Screen.
 *
 * Per LOCK_SCREEN_AND_CASHIER_LOGIN.md:
 * - Displays station name, real-time clock, locked employee info
 * - PIN entry for unlock verification via API
 * - Reports lock/unlock events to backend
 * - Sign Out option (may require manager approval)
 *
 * **L2 FIX:** Uses verifyPassword() API instead of hardcoded "1234"
 * **L3 FIX:** Gets employee data from CashierSessionManager instead of placeholder
 * **L5 FIX:** Reports lock/unlock events via lockDevice() API
 * **DI1 FIX:** Proper dependency injection
 *
 * Per kotlin-standards.mdc:
 * - Uses StateFlow for reactive UI
 * - Structured concurrency with screenModelScope
 */
class LockViewModel(
    private val authService: ApiAuthService,
    private val sessionManager: CashierSessionManager,
    private val lockEventType: LockEventType = LockEventType.Inactivity,
    private val scope: CoroutineScope? = null
) : ScreenModel {
    
    private val _state = MutableStateFlow(LockUiState.initial())
    val state: StateFlow<LockUiState> = _state.asStateFlow()
    
    private var clockJob: Job? = null
    
    private val effectiveScope: CoroutineScope
        get() = scope ?: screenModelScope
    
    init {
        initialize()
        startClock()
    }
    
    /**
     * Initialize the lock screen with current session data.
     * 
     * **L3 FIX:** Gets actual employee data from CashierSessionManager.
     * **L5 FIX:** Reports lock event to backend.
     */
    private fun initialize() {
        // L3 FIX: Get actual employee data from session manager
        val session = sessionManager.getCurrentSession()
        
        val lockType = when (lockEventType) {
            LockEventType.Inactivity -> LockType.AutoLocked
            LockEventType.Manual -> LockType.Locked
            LockEventType.Manager -> LockType.ManagerLocked
        }
        
        _state.value = _state.value.copy(
            stationName = session?.let { "Register ${it.registerId}" } ?: "Register 1",
            employeeName = session?.employeeName ?: "Cashier",
            employeeRole = "Cashier", // Could be enhanced to get from session
            lockType = lockType
        )
        
        // L5 FIX: Report lock event to backend
        reportLockEvent(lockType)
        
        updateClock()
    }
    
    /**
     * Report lock/unlock event to backend.
     * 
     * Per LOCK_SCREEN_AND_CASHIER_LOGIN.md:
     * - POST /api/Employee/LockDevice with DeviceEventType
     */
    private fun reportLockEvent(lockType: LockType) {
        val eventType = when (lockType) {
            LockType.AutoLocked -> DeviceEventType.AutoLocked
            LockType.Locked -> DeviceEventType.Locked
            LockType.ManagerLocked -> DeviceEventType.Locked
        }
        
        effectiveScope.launch {
            authService.lockDevice(DeviceLockRequest(lockType = eventType))
                .onSuccess {
                    println("[LockViewModel] Lock event reported: $eventType")
                }
                .onFailure { error ->
                    println("[LockViewModel] Failed to report lock event: ${error.message}")
                }
        }
    }
    
    /**
     * Start the real-time clock updater.
     * Updates every second.
     *
     * Per UI spec: Large clock display with HH:mm format.
     */
    private fun startClock() {
        clockJob?.cancel()
        clockJob = effectiveScope.launch {
            while (isActive) {
                updateClock()
                delay(1000) // Update every second
            }
        }
    }
    
    /**
     * Update the clock display.
     */
    private fun updateClock() {
        val now = Clock.System.now()
        val localTime = now.toLocalDateTime(TimeZone.currentSystemDefault())
        
        val hour = localTime.hour
        val minute = localTime.minute
        val amPm = if (hour < 12) "AM" else "PM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        
        val timeString = "$displayHour:${minute.toString().padStart(2, '0')} $amPm"
        
        val dayOfWeek = localTime.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
        val month = localTime.month.name.lowercase().replaceFirstChar { it.uppercase() }
        val day = localTime.dayOfMonth
        val year = localTime.year
        val dateString = "$dayOfWeek, $month $day, $year"
        
        _state.value = _state.value.copy(
            currentTime = timeString,
            currentDate = dateString
        )
    }
    
    // ========================================================================
    // PIN Entry
    // ========================================================================
    
    /**
     * Handle digit press on PIN pad.
     */
    fun onPinDigit(digit: String) {
        val currentPin = _state.value.pinInput
        if (currentPin.length < 8) { // Max 8 digits per spec
            _state.value = _state.value.copy(
                pinInput = currentPin + digit,
                errorMessage = null
            )
        }
    }
    
    /**
     * Clear all PIN input.
     */
    fun onPinClear() {
        _state.value = _state.value.copy(
            pinInput = "",
            errorMessage = null
        )
    }
    
    /**
     * Backspace - remove last PIN digit.
     */
    fun onPinBackspace() {
        val currentPin = _state.value.pinInput
        if (currentPin.isNotEmpty()) {
            _state.value = _state.value.copy(
                pinInput = currentPin.dropLast(1),
                errorMessage = null
            )
        }
    }
    
    /**
     * Verify PIN and unlock.
     *
     * Per LOCK_SCREEN_AND_CASHIER_LOGIN.md:
     * - L2 FIX: Calls POST /api/Employee/VerifyPassword API
     * - L5 FIX: Reports unlock event on success
     * - Navigate back to previous screen
     * 
     * Note: This method now starts an async verification. 
     * UI should observe state.unlockSuccess for navigation.
     */
    fun onVerify() {
        val pin = _state.value.pinInput
        val session = sessionManager.getCurrentSession()
        
        if (pin.isEmpty()) {
            _state.value = _state.value.copy(errorMessage = "Please enter your PIN")
            return
        }
        
        if (pin.length < 4) {
            _state.value = _state.value.copy(
                errorMessage = "PIN must be at least 4 digits",
                pinInput = ""
            )
            return
        }
        
        if (session == null) {
            _state.value = _state.value.copy(errorMessage = "No active session")
            return
        }
        
        _state.value = _state.value.copy(isVerifying = true, errorMessage = null)
        
        // L2 FIX: Use API verification instead of hardcoded PIN
        effectiveScope.launch {
            val request = VerifyPasswordRequest(
                userName = session.employeeId.toString(),
                password = pin,
                branchId = null, // Will be filled from session if available
                deviceId = session.registerId
            )
            
            authService.verifyPassword(request)
                .onSuccess { isValid ->
                    if (isValid) {
                        // L5 FIX: Report unlock event to backend
                        authService.lockDevice(DeviceLockRequest(lockType = DeviceEventType.Unlocked))
                        
                        // Update session status
                        sessionManager.unlockSession()
                        
                        // Restart inactivity timer on successful unlock
                        InactivityManager.start()
                        
                        _state.value = _state.value.copy(
                            isVerifying = false,
                            unlockSuccess = true
                        )
                        
                        println("[LockViewModel] Unlock successful, reported to backend")
                    } else {
                        _state.value = _state.value.copy(
                            isVerifying = false,
                            pinInput = "",
                            errorMessage = "Invalid PIN. Please try again."
                        )
                    }
                }
                .onFailure { error ->
                    println("[LockViewModel] PIN verification failed: ${error.message}")
                    _state.value = _state.value.copy(
                        isVerifying = false,
                        pinInput = "",
                        errorMessage = error.message ?: "Verification failed"
                    )
                }
        }
    }
    
    /**
     * Synchronous unlock for backward compatibility.
     * 
     * Returns immediately - use onVerify() for async verification.
     * UI should observe state.unlockSuccess for navigation.
     */
    @Deprecated("Use onVerify() and observe state.unlockSuccess instead")
    fun onVerifySync(): UnlockResult {
        onVerify()
        // Return pending - actual result comes via state update
        return if (_state.value.unlockSuccess) UnlockResult.Success else UnlockResult.Error
    }
    
    /**
     * Request sign out from lock screen.
     *
     * Per CASHIER_OPERATIONS.md:
     * - Signing out from lock screen requires manager approval
     * - For now, we just navigate to login
     */
    fun onSignOut(): SignOutResult {
        // TODO: Implement manager approval flow
        // For Walking Skeleton: Allow direct sign out
        InactivityManager.stop()
        return SignOutResult.Proceed
    }
    
    /**
     * Called when screen becomes visible.
     * Ensures clock is running.
     */
    fun onScreenVisible() {
        startClock()
    }
    
    /**
     * Cleanup when ViewModel is disposed.
     */
    override fun onDispose() {
        clockJob?.cancel()
        super.onDispose()
    }
}

/**
 * Result of unlock attempt.
 */
sealed interface UnlockResult {
    data object Success : UnlockResult
    data object Error : UnlockResult
}

/**
 * Result of sign out request.
 */
sealed interface SignOutResult {
    data object Proceed : SignOutResult
    data object RequiresApproval : SignOutResult
}

