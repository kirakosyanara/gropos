package com.unisight.gropos.features.auth.presentation

import com.unisight.gropos.features.auth.domain.model.AuthUser

/**
 * UI State for the Login screen.
 * 
 * Per code-quality.mdc: Use sealed classes for status, not Strings.
 * This ensures compile-time safety and exhaustive when expressions.
 */
sealed interface LoginUiState {
    
    /**
     * Initial state - waiting for user input.
     */
    data object Idle : LoginUiState
    
    /**
     * Authentication in progress.
     * UI should show loading indicator and disable inputs.
     */
    data object Loading : LoginUiState
    
    /**
     * Authentication succeeded.
     * Contains the authenticated user for navigation decisions.
     */
    data class Success(val user: AuthUser) : LoginUiState
    
    /**
     * Authentication failed.
     * Contains user-friendly error message.
     */
    data class Error(val message: String) : LoginUiState
}

