package com.unisight.gropos.features.auth.domain.repository

import com.unisight.gropos.features.auth.domain.model.AuthUser

/**
 * Repository interface for authentication operations.
 * 
 * Defined in Domain layer, implemented in Data layer.
 * Per Clean Architecture: Domain defines the contract,
 * Data provides the implementation (local/remote).
 */
interface AuthRepository {
    
    /**
     * Authenticates a user with username and PIN.
     * 
     * @param username The employee username
     * @param pin The 4-digit PIN (already validated by UseCase)
     * @return Result containing AuthUser on success, or AuthError on failure
     */
    suspend fun login(username: String, pin: String): Result<AuthUser>
    
    /**
     * Logs out the current user and clears session.
     */
    suspend fun logout()
    
    /**
     * Returns the currently authenticated user, if any.
     */
    suspend fun getCurrentUser(): AuthUser?
}

