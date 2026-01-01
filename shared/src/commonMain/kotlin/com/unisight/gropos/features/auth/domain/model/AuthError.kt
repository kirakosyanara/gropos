package com.unisight.gropos.features.auth.domain.model

/**
 * Domain-specific errors for authentication operations.
 * 
 * These are mapped from API/database errors in the Data layer
 * to provide meaningful error messages to the UI.
 */
sealed class AuthError : Exception() {
    
    /**
     * PIN format is invalid (must be exactly 4 digits).
     */
    data object InvalidPinFormat : AuthError() {
        private fun readResolve(): Any = InvalidPinFormat
        override val message: String = "PIN must be exactly 4 digits"
    }
    
    /**
     * Username or PIN is incorrect.
     */
    data object InvalidCredentials : AuthError() {
        private fun readResolve(): Any = InvalidCredentials
        override val message: String = "Invalid username or PIN"
    }
    
    /**
     * User account is locked (too many failed attempts).
     */
    data object AccountLocked : AuthError() {
        private fun readResolve(): Any = AccountLocked
        override val message: String = "Account is locked. Contact a manager."
    }
    
    /**
     * Network or server error during authentication.
     */
    data class NetworkError(override val message: String) : AuthError()
}

