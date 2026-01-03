package com.unisight.gropos.core.auth

import com.unisight.gropos.features.auth.domain.model.AuthUser
import com.unisight.gropos.features.auth.domain.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * API Authentication Service for GroPOS.
 * 
 * Per REMEDIATION_CHECKLIST: Implement `employeeGroPOSLogin()` with bearer token storage.
 * Per AUTH_SESSION.md: Handle employee authentication against backend API.
 * 
 * Responsibilities:
 * - Authenticate employees via API
 * - Store and manage bearer tokens
 * - Handle token refresh
 * - Provide current auth state
 */
interface ApiAuthService {
    
    /**
     * Current authentication state.
     */
    val authState: StateFlow<AuthState>
    
    /**
     * Whether user is currently authenticated.
     */
    val isAuthenticated: Boolean
    
    /**
     * The current bearer token, if authenticated.
     */
    val bearerToken: String?
    
    /**
     * Authenticates an employee using their credentials.
     * 
     * Per REMEDIATION_CHECKLIST: `employeeGroPOSLogin()` with bearer token storage.
     * 
     * @param username The employee's username or employee ID
     * @param password The employee's password or PIN
     * @return Result containing AuthUser on success, error on failure
     */
    suspend fun employeeGroPOSLogin(username: String, password: String): Result<AuthUser>
    
    /**
     * Authenticates an employee using PIN only (for registered devices).
     * 
     * @param pin The employee's PIN
     * @return Result containing AuthUser on success, error on failure
     */
    suspend fun employeePinLogin(pin: String): Result<AuthUser>
    
    /**
     * Refreshes the current authentication token.
     * 
     * Per REMEDIATION_CHECKLIST: Token Refresh - refresh token logic.
     * 
     * @return Result.success if refresh succeeded, Result.failure otherwise
     */
    suspend fun refreshToken(): Result<Unit>
    
    /**
     * Logs out the current user.
     * 
     * @return Result.success if logout succeeded
     */
    suspend fun logout(): Result<Unit>
    
    /**
     * Sets the bearer token directly (for Manager override scenarios).
     * 
     * Per REMEDIATION_CHECKLIST: `Manager.setBearerToken()`.
     * 
     * @param token The bearer token to set
     * @param user The user associated with this token
     */
    fun setBearerToken(token: String, user: AuthUser)
    
    /**
     * Checks if the current token is expired or about to expire.
     * 
     * @return true if token needs refresh
     */
    fun needsTokenRefresh(): Boolean
}

/**
 * Current authentication state.
 */
sealed class AuthState {
    /** Not authenticated */
    data object Unauthenticated : AuthState()
    
    /** Authentication in progress */
    data object Authenticating : AuthState()
    
    /** Successfully authenticated */
    data class Authenticated(
        val user: AuthUser,
        val token: String,
        val expiresAt: Instant,
        val refreshToken: String?
    ) : AuthState()
    
    /** Token expired, needs refresh */
    data class TokenExpired(
        val user: AuthUser,
        val refreshToken: String?
    ) : AuthState()
    
    /** Authentication failed */
    data class Error(val message: String) : AuthState()
}

/**
 * Response from the authentication API.
 */
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String?,
    val expiresIn: Long, // seconds
    val tokenType: String = "Bearer",
    val user: AuthUser
)

/**
 * Default implementation of ApiAuthService.
 */
class DefaultApiAuthService(
    private val tokenStorage: TokenStorage
) : ApiAuthService {
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    override val isAuthenticated: Boolean
        get() = _authState.value is AuthState.Authenticated
    
    override val bearerToken: String?
        get() = (_authState.value as? AuthState.Authenticated)?.token
    
    override suspend fun employeeGroPOSLogin(username: String, password: String): Result<AuthUser> {
        _authState.value = AuthState.Authenticating
        
        return try {
            // TODO: Replace with actual API call
            // val response = apiClient.post("/api/auth/employee/login") { body = LoginRequest(username, password) }
            
            // Simulated API response for now
            val response = simulateLogin(username, password)
            
            if (response != null) {
                // Store tokens securely
                tokenStorage.saveAccessToken(response.accessToken)
                response.refreshToken?.let { tokenStorage.saveRefreshToken(it) }
                
                val expiresAt = Clock.System.now() + response.expiresIn.seconds
                
                _authState.value = AuthState.Authenticated(
                    user = response.user,
                    token = response.accessToken,
                    expiresAt = expiresAt,
                    refreshToken = response.refreshToken
                )
                
                println("ApiAuthService: Login successful for ${response.user.username}")
                Result.success(response.user)
            } else {
                _authState.value = AuthState.Error("Invalid credentials")
                Result.failure(AuthException("Invalid username or password"))
            }
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.message ?: "Authentication failed")
            Result.failure(e)
        }
    }
    
    override suspend fun employeePinLogin(pin: String): Result<AuthUser> {
        _authState.value = AuthState.Authenticating
        
        return try {
            // TODO: Replace with actual API call for PIN-based login
            // Requires device to be pre-registered
            
            val response = simulatePinLogin(pin)
            
            if (response != null) {
                tokenStorage.saveAccessToken(response.accessToken)
                response.refreshToken?.let { tokenStorage.saveRefreshToken(it) }
                
                val expiresAt = Clock.System.now() + response.expiresIn.seconds
                
                _authState.value = AuthState.Authenticated(
                    user = response.user,
                    token = response.accessToken,
                    expiresAt = expiresAt,
                    refreshToken = response.refreshToken
                )
                
                println("ApiAuthService: PIN login successful for ${response.user.username}")
                Result.success(response.user)
            } else {
                _authState.value = AuthState.Error("Invalid PIN")
                Result.failure(AuthException("Invalid PIN"))
            }
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.message ?: "PIN authentication failed")
            Result.failure(e)
        }
    }
    
    override suspend fun refreshToken(): Result<Unit> {
        val currentState = _authState.value
        
        val refreshToken = when (currentState) {
            is AuthState.Authenticated -> currentState.refreshToken
            is AuthState.TokenExpired -> currentState.refreshToken
            else -> null
        } ?: return Result.failure(AuthException("No refresh token available"))
        
        return try {
            // TODO: Replace with actual API call
            // val response = apiClient.post("/api/auth/refresh") { body = RefreshRequest(refreshToken) }
            
            val response = simulateTokenRefresh(refreshToken)
            
            if (response != null) {
                tokenStorage.saveAccessToken(response.accessToken)
                response.refreshToken?.let { tokenStorage.saveRefreshToken(it) }
                
                val expiresAt = Clock.System.now() + response.expiresIn.seconds
                
                _authState.value = AuthState.Authenticated(
                    user = response.user,
                    token = response.accessToken,
                    expiresAt = expiresAt,
                    refreshToken = response.refreshToken
                )
                
                println("ApiAuthService: Token refreshed successfully")
                Result.success(Unit)
            } else {
                _authState.value = AuthState.Unauthenticated
                tokenStorage.clearTokens()
                Result.failure(AuthException("Token refresh failed"))
            }
        } catch (e: Exception) {
            _authState.value = AuthState.Unauthenticated
            tokenStorage.clearTokens()
            Result.failure(e)
        }
    }
    
    override suspend fun logout(): Result<Unit> {
        return try {
            // TODO: Call logout API endpoint
            // apiClient.post("/api/auth/logout")
            
            tokenStorage.clearTokens()
            _authState.value = AuthState.Unauthenticated
            
            println("ApiAuthService: Logout successful")
            Result.success(Unit)
        } catch (e: Exception) {
            // Still clear local state even if API call fails
            tokenStorage.clearTokens()
            _authState.value = AuthState.Unauthenticated
            Result.failure(e)
        }
    }
    
    override fun setBearerToken(token: String, user: AuthUser) {
        // Set token directly (for manager override scenarios)
        val expiresAt = Clock.System.now() + 1.hours // Assume 1 hour validity
        
        _authState.value = AuthState.Authenticated(
            user = user,
            token = token,
            expiresAt = expiresAt,
            refreshToken = null
        )
        
        tokenStorage.saveAccessToken(token)
        
        println("ApiAuthService: Bearer token set directly for ${user.username}")
    }
    
    override fun needsTokenRefresh(): Boolean {
        val state = _authState.value
        
        if (state !is AuthState.Authenticated) return false
        
        // Refresh if token expires within 5 minutes
        val refreshThreshold = Clock.System.now() + 5.minutes
        return state.expiresAt < refreshThreshold
    }
    
    // ========================================================================
    // Simulated API Responses (Replace with real API calls)
    // ========================================================================
    
    private suspend fun simulateLogin(username: String, password: String): AuthResponse? {
        kotlinx.coroutines.delay(500) // Simulate network delay
        
        // Simulated users for testing
        return when {
            username == "cashier" && password == "1234" -> AuthResponse(
                accessToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.cashier.${System.currentTimeMillis()}",
                refreshToken = "refresh_cashier_${System.currentTimeMillis()}",
                expiresIn = 3600,
                user = AuthUser(
                    id = "1",
                    username = "John Smith",
                    role = UserRole.CASHIER,
                    permissions = listOf(
                        "GroPOS.Transactions.Sale",
                        "GroPOS.Transactions.Discounts.Items.Request"
                    ),
                    isManager = false,
                    jobTitle = "Cashier"
                )
            )
            username == "manager" && password == "5678" -> AuthResponse(
                accessToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.manager.${System.currentTimeMillis()}",
                refreshToken = "refresh_manager_${System.currentTimeMillis()}",
                expiresIn = 3600,
                user = AuthUser(
                    id = "2",
                    username = "Jane Manager",
                    role = UserRole.MANAGER,
                    permissions = listOf(
                        "GroPOS.Transactions.Sale",
                        "GroPOS.Transactions.Void",
                        "GroPOS.Transactions.Discounts.Items",
                        "GroPOS.Transactions.Discounts.Total",
                        "GroPOS.Transactions.Price Override",
                        "GroPOS.Returns",
                        "GroPOS.Cash Pickup."
                    ),
                    isManager = true,
                    jobTitle = "Store Manager"
                )
            )
            else -> null
        }
    }
    
    private suspend fun simulatePinLogin(pin: String): AuthResponse? {
        kotlinx.coroutines.delay(300)
        
        return when (pin) {
            "1234" -> simulateLogin("cashier", "1234")
            "5678" -> simulateLogin("manager", "5678")
            else -> null
        }
    }
    
    private suspend fun simulateTokenRefresh(refreshToken: String): AuthResponse? {
        kotlinx.coroutines.delay(200)
        
        // Extract user type from refresh token
        return when {
            refreshToken.contains("cashier") -> AuthResponse(
                accessToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.cashier.refreshed.${System.currentTimeMillis()}",
                refreshToken = "refresh_cashier_${System.currentTimeMillis()}",
                expiresIn = 3600,
                user = AuthUser(
                    id = "1",
                    username = "John Smith",
                    role = UserRole.CASHIER,
                    permissions = emptyList(),
                    isManager = false
                )
            )
            refreshToken.contains("manager") -> AuthResponse(
                accessToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.manager.refreshed.${System.currentTimeMillis()}",
                refreshToken = "refresh_manager_${System.currentTimeMillis()}",
                expiresIn = 3600,
                user = AuthUser(
                    id = "2",
                    username = "Jane Manager",
                    role = UserRole.MANAGER,
                    permissions = emptyList(),
                    isManager = true
                )
            )
            else -> null
        }
    }
}

/**
 * Extension property to convert Long seconds to Duration.
 */
private val Long.seconds: kotlin.time.Duration
    get() = kotlin.time.Duration.Companion.parse("${this}s")

/**
 * Authentication exception.
 */
class AuthException(message: String) : Exception(message)

