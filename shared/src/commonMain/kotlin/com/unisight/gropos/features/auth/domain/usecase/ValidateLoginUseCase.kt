package com.unisight.gropos.features.auth.domain.usecase

import com.unisight.gropos.features.auth.domain.model.AuthError
import com.unisight.gropos.features.auth.domain.model.AuthUser
import com.unisight.gropos.features.auth.domain.repository.AuthRepository

/**
 * Use case for validating and executing user login.
 * 
 * Business Rules:
 * - PIN must be exactly 4 digits (numeric only)
 * - PIN validation happens BEFORE calling the repository
 * - This prevents unnecessary network calls for invalid input
 * 
 * Per project-structure.mdc: UseCase naming is Verb-based.
 * Per code-quality.mdc: Pure function, no side effects beyond repository call.
 */
class ValidateLoginUseCase(
    private val authRepository: AuthRepository
) {
    
    /**
     * Validates the PIN format and attempts login if valid.
     * 
     * @param username The employee username
     * @param pin The PIN to validate (must be exactly 4 digits)
     * @return Result containing AuthUser on success, or AuthError on failure
     */
    suspend operator fun invoke(username: String, pin: String): Result<AuthUser> {
        // Business Rule: PIN must be exactly 4 digits
        if (!isValidPinFormat(pin)) {
            return Result.failure(AuthError.InvalidPinFormat)
        }
        
        // PIN format is valid, proceed to authenticate
        return authRepository.login(username, pin)
    }
    
    /**
     * Validates that PIN is exactly 4 numeric digits.
     * 
     * Why validate here instead of in Repository?
     * - Fail fast: Don't waste network calls for invalid input
     * - Business rule belongs in Domain, not Data layer
     * - Makes the app work consistently offline/online
     */
    private fun isValidPinFormat(pin: String): Boolean {
        if (pin.length != PIN_LENGTH) return false
        if (!pin.all { it.isDigit() }) return false
        return true
    }
    
    companion object {
        private const val PIN_LENGTH = 4
    }
}

