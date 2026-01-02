package com.unisight.gropos.features.auth.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.unisight.gropos.core.session.InactivityManager
import com.unisight.gropos.core.session.LockEventType
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
 * Per SCREEN_LAYOUTS.md:
 * - Displays station name, real-time clock, locked employee info
 * - PIN entry for unlock verification
 * - Sign Out option (may require manager approval)
 *
 * Per CASHIER_OPERATIONS.md:
 * - Verify PIN against stored session credentials
 * - Report unlock event to backend (when API integration exists)
 *
 * Per kotlin-standards.mdc:
 * - Uses StateFlow for reactive UI
 * - Structured concurrency with screenModelScope
 */
class LockViewModel(
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
     */
    private fun initialize() {
        // TODO: Get actual employee data from session store
        // For now, using placeholder data
        val lockType = when (lockEventType) {
            LockEventType.Inactivity -> LockType.AutoLocked
            LockEventType.Manual -> LockType.Locked
            LockEventType.Manager -> LockType.ManagerLocked
        }
        
        _state.value = _state.value.copy(
            stationName = "Register 1", // TODO: Get from AppStore.branch
            employeeName = "Cashier", // TODO: Get from AppStore.employee
            employeeRole = "Cashier",
            lockType = lockType
        )
        
        updateClock()
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
     * Per CASHIER_OPERATIONS.md:
     * - Call employeeVerifyPassword API
     * - Report unlock event on success
     * - Navigate back to previous screen
     *
     * For Walking Skeleton: Uses local PIN validation (1234).
     */
    fun onVerify(): UnlockResult {
        val pin = _state.value.pinInput
        
        if (pin.isEmpty()) {
            _state.value = _state.value.copy(errorMessage = "Please enter your PIN")
            return UnlockResult.Error
        }
        
        if (pin.length < 4) {
            _state.value = _state.value.copy(
                errorMessage = "PIN must be at least 4 digits",
                pinInput = ""
            )
            return UnlockResult.Error
        }
        
        _state.value = _state.value.copy(isVerifying = true)
        
        // TODO: Replace with actual API verification
        // For Walking Skeleton: Accept "1234" as valid PIN
        return if (pin == "1234") {
            _state.value = _state.value.copy(isVerifying = false)
            
            // Restart inactivity timer on successful unlock
            InactivityManager.start()
            
            UnlockResult.Success
        } else {
            _state.value = _state.value.copy(
                isVerifying = false,
                pinInput = "",
                errorMessage = "Invalid PIN. Please try again."
            )
            UnlockResult.Error
        }
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

