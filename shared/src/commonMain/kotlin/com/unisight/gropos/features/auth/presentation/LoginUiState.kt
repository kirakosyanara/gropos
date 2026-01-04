package com.unisight.gropos.features.auth.presentation

import com.unisight.gropos.core.AppConstants
import com.unisight.gropos.features.auth.domain.model.AuthUser
import com.unisight.gropos.features.cashier.domain.model.Employee
import com.unisight.gropos.features.cashier.domain.model.Till

/**
 * UI State for the Login screen.
 * 
 * Per LOCK_SCREEN_AND_CASHIER_LOGIN.md State Machine:
 * SPLASH -> EMPLOYEE_SELECT -> PIN_ENTRY -> TILL_ASSIGNMENT -> ACTIVE
 * 
 * **Station Claiming (L1):**
 * When a station is claimed, employeeId is set from device API response
 * and the employee is pre-selected, skipping EMPLOYEE_SELECT stage.
 * 
 * This is now a data class with a stage enum instead of a sealed interface
 * to simplify state management for the multi-stage login flow.
 */
data class LoginUiState(
    val stage: LoginStage = LoginStage.LOADING,
    
    // Station/branding info
    val stationName: String = "Register 1",
    val currentTime: String = "",
    val version: String = AppConstants.APP_VERSION,
    
    // Employee selection
    val employees: List<EmployeeUiModel> = emptyList(),
    
    // Selected employee for PIN entry
    val selectedEmployee: EmployeeUiModel? = null,
    
    // PIN entry state
    val pinInput: String = "",
    val pinMasked: String = "",  // Dots for display
    
    // Till selection
    val tills: List<TillUiModel> = emptyList(),
    val selectedTillId: Int? = null,
    
    // Loading and error states
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    
    // Authentication result
    val authenticatedUser: AuthUser? = null,
    
    // Admin Settings Dialog (Hidden Menu)
    val showAdminSettings: Boolean = false,
    
    // NFC Badge Scanning State
    val isNfcScanActive: Boolean = false,
    
    // Station Claiming State (L1)
    // Per LOCK_SCREEN_AND_CASHIER_LOGIN.md: Station is claimed when deviceInfo.employeeId is set
    val isStationClaimed: Boolean = false,
    val claimedTillId: Int? = null,
    
    // Sync Progress State (Per COUCHBASE_SYNCHRONIZATION_DETAILED.md)
    // Shows progress during initial data sync or after database wipe
    val syncProgress: SyncProgressUiModel = SyncProgressUiModel()
) {
    /**
     * Computed: PIN as dots for display
     */
    val pinDots: String get() = "●".repeat(pinInput.length)
    
    /**
     * Computed: Can submit PIN (minimum 4 digits)
     */
    val canSubmitPin: Boolean get() = pinInput.length >= 4
}

/**
 * Login flow stages per CASHIER_OPERATIONS.md
 */
enum class LoginStage {
    /**
     * Initial loading state - fetching employees
     */
    LOADING,
    
    /**
     * Syncing data from cloud - shown on initial load or after database wipe
     * Per COUCHBASE_SYNCHRONIZATION_DETAILED.md: Shows detailed progress
     */
    SYNCING,
    
    /**
     * Show grid of employees to select from
     */
    EMPLOYEE_SELECT,
    
    /**
     * Employee selected, enter PIN
     */
    PIN_ENTRY,
    
    /**
     * PIN verified, need to select a till
     */
    TILL_ASSIGNMENT,
    
    /**
     * Login complete, navigate to home
     */
    SUCCESS
}

/**
 * UI model for sync progress display.
 * 
 * Per COUCHBASE_SYNCHRONIZATION_DETAILED.md Section 5:
 * - Shows current entity being synced
 * - Displays progress percentage
 * - Provides estimated time remaining
 */
data class SyncProgressUiModel(
    val isActive: Boolean = false,
    val currentStep: Int = 0,
    val totalSteps: Int = 13,  // 1 (employees) + 12 (DataLoader entities)
    val currentEntity: String = "",
    val statusMessage: String = "Initializing...",
    val estimatedTimeRemaining: String? = null,
    val hasError: Boolean = false,
    val errorMessage: String? = null
) {
    /**
     * Progress as a fraction (0.0 to 1.0) for progress bar
     */
    val progressFraction: Float
        get() = if (totalSteps > 0) currentStep.toFloat() / totalSteps else 0f
    
    /**
     * Progress as percentage (0 to 100)
     */
    val progressPercent: Int
        get() = (progressFraction * 100).toInt()
    
    /**
     * Human-readable progress text
     */
    val progressText: String
        get() = when {
            hasError -> "Sync Failed"
            currentStep >= totalSteps -> "Complete!"
            currentStep == 0 -> "Preparing..."
            else -> "Step $currentStep of $totalSteps"
        }
}

/**
 * UI model for displaying an employee in the selection grid
 */
data class EmployeeUiModel(
    val id: Int,
    val firstName: String,
    val lastName: String,
    val email: String,
    val role: String,
    val imageUrl: String?,
    val assignedTillId: Int?
) {
    val fullName: String get() = "$firstName $lastName"
    val initials: String get() = "${firstName.firstOrNull() ?: ""}${lastName.firstOrNull() ?: ""}"
}

/**
 * UI model for displaying a till in the selection list
 */
data class TillUiModel(
    val id: Int,
    val name: String,
    val assignedTo: String?,
    val assignedEmployeeId: Int?,
    val isAvailable: Boolean
) {
    /**
     * Check if this till is selectable by the given employee.
     * A till is selectable if:
     * 1. It's available (no one assigned), OR
     * 2. It's assigned to this employee (their own till)
     */
    fun isSelectableBy(employeeId: Int): Boolean {
        return isAvailable || assignedEmployeeId == employeeId
    }
}

/**
 * Extension to convert domain Employee to UI model
 */
fun Employee.toUiModel() = EmployeeUiModel(
    id = id,
    firstName = firstName,
    lastName = lastName,
    email = email,
    role = roleDisplayName,
    imageUrl = imageUrl,
    assignedTillId = assignedTillId
)

/**
 * Extension to convert domain Till to UI model
 */
fun Till.toUiModel() = TillUiModel(
    id = id,
    name = name,
    assignedTo = assignedEmployeeName,
    assignedEmployeeId = assignedEmployeeId,
    isAvailable = isAvailable
)
