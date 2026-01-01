package com.unisight.gropos.features.auth.data

import com.unisight.gropos.features.auth.domain.model.AuthError
import com.unisight.gropos.features.auth.domain.model.AuthUser
import com.unisight.gropos.features.auth.domain.model.UserRole
import com.unisight.gropos.features.auth.domain.repository.AuthRepository
import kotlinx.coroutines.delay

/**
 * Fake implementation of AuthRepository for development and testing.
 * 
 * This implementation simulates network latency and provides
 * hardcoded credentials for the walking skeleton phase.
 * 
 * Per testing-strategy.mdc: "Use Fakes for State" - this fake
 * maintains currentUser state and can be inspected in tests.
 * 
 * TODO: Replace with CouchbaseLiteAuthRepository when database layer is ready
 */
class FakeAuthRepository : AuthRepository {
    
    /**
     * Currently authenticated user, null if logged out.
     */
    private var currentUser: AuthUser? = null
    
    /**
     * Simulates network authentication with 500ms delay.
     * 
     * Valid credentials:
     * - admin / 1234 -> ADMIN role
     * - manager / 1234 -> MANAGER role
     * - cashier / 1234 -> CASHIER role
     */
    override suspend fun login(username: String, pin: String): Result<AuthUser> {
        // Simulate network latency (per requirements: 500ms)
        delay(SIMULATED_NETWORK_DELAY_MS)
        
        // Check credentials against fake user database
        val user = FAKE_USERS[username]
        
        return when {
            user == null -> Result.failure(AuthError.InvalidCredentials)
            pin != VALID_PIN -> Result.failure(AuthError.InvalidCredentials)
            else -> {
                currentUser = user
                Result.success(user)
            }
        }
    }
    
    override suspend fun logout() {
        currentUser = null
    }
    
    override suspend fun getCurrentUser(): AuthUser? = currentUser
    
    companion object {
        private const val SIMULATED_NETWORK_DELAY_MS = 500L
        private const val VALID_PIN = "1234"
        
        /**
         * Fake user database for walking skeleton.
         * Maps username to AuthUser.
         */
        private val FAKE_USERS = mapOf(
            "admin" to AuthUser(
                id = "1",
                username = "admin",
                role = UserRole.ADMIN
            ),
            "manager" to AuthUser(
                id = "2",
                username = "manager",
                role = UserRole.MANAGER
            ),
            "cashier" to AuthUser(
                id = "3",
                username = "cashier",
                role = UserRole.CASHIER
            )
        )
    }
}

