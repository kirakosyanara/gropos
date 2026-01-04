package com.unisight.gropos.features.auth.presentation

/**
 * UI State for the Lock Screen.
 *
 * Per LOCK_SCREEN_AND_CASHIER_LOGIN.md: Lock Screen displays station info, time,
 * locked employee details, and PIN entry for unlock.
 *
 * Lock triggers include:
 * - Inactivity (5 minutes, type: AutoLocked)
 * - Manual F4 key (type: Locked)
 * - Manager lock (type: ManagerLocked)
 */
data class LockUiState(
    /** Station identifier (e.g., "Register 1", "Station 4") */
    val stationName: String = "Register 1",
    
    /** Current time formatted as "HH:mm" */
    val currentTime: String = "",
    
    /** Current date formatted as "EEEE, MMMM d, yyyy" */
    val currentDate: String = "",
    
    /** Name of the locked employee */
    val employeeName: String = "",
    
    /** Role of the locked employee */
    val employeeRole: String = "",
    
    /** Avatar URL of the locked employee (optional) */
    val employeeAvatarUrl: String? = null,
    
    /** Current PIN input (masked) */
    val pinInput: String = "",
    
    /** True when verifying PIN with backend */
    val isVerifying: Boolean = false,
    
    /** Error message to display */
    val errorMessage: String? = null,
    
    /** Lock type for display/logging */
    val lockType: LockType = LockType.AutoLocked,
    
    /** True when unlock was successful - UI should navigate back */
    val unlockSuccess: Boolean = false,
    
    // Sign-out flow states (L4, U1, U2)
    /** Show logout options dialog (Release Till / End of Shift) */
    val showLogoutOptions: Boolean = false,
    
    /** Show manager approval dialog */
    val showManagerApproval: Boolean = false,
    
    /** Description of action requiring manager approval */
    val managerApprovalAction: String = "",
    
    /** True when verifying manager PIN */
    val isVerifyingManager: Boolean = false,
    
    /** Manager approval error message */
    val managerApprovalError: String? = null,
    
    /** True when sign-out is complete - UI should navigate to login */
    val signOutSuccess: Boolean = false,
    
    /** Pending logout type (for after manager approval) */
    val pendingLogoutType: LogoutType? = null
) {
    /** Number of masked PIN dots to show */
    val pinDotCount: Int get() = pinInput.length
    
    companion object {
        fun initial(): LockUiState = LockUiState()
    }
}

/**
 * Types of screen lock per CASHIER_OPERATIONS.md
 */
enum class LockType {
    /** Automatic lock after 5 minutes of inactivity */
    AutoLocked,
    
    /** Manual lock via F4 key */
    Locked,
    
    /** Forced lock by manager */
    ManagerLocked
}

/**
 * Types of logout per LOCK_SCREEN_AND_CASHIER_LOGIN.md
 */
enum class LogoutType {
    /** Quick logout - releases till for next employee */
    ReleaseTill,
    
    /** Full close - till count and Z-report */
    EndOfShift
}

