package com.unisight.gropos.features.auth.presentation

import com.unisight.gropos.core.AppConstants
import com.unisight.gropos.features.auth.domain.model.AuthUser
import com.unisight.gropos.features.cashier.domain.model.Employee
import com.unisight.gropos.features.cashier.domain.model.Till

/**
 * UI State for the Login screen.
 * 
 * Per CASHIER_OPERATIONS.md State Machine:
 * SPLASH -> EMPLOYEE_SELECT -> PIN_ENTRY -> TILL_ASSIGNMENT -> ACTIVE
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
    val showAdminSettings: Boolean = false
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
    val isAvailable: Boolean
)

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
    isAvailable = isAvailable
)
