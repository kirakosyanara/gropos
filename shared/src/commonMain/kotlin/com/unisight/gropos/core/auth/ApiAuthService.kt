package com.unisight.gropos.core.auth

import com.unisight.gropos.core.network.ApiClient
import com.unisight.gropos.features.auth.domain.model.AuthUser
import com.unisight.gropos.features.auth.domain.model.UserRole
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
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
 * Default implementation of ApiAuthService using real API calls.
 * 
 * **P0 FIX (QA Audit):** Now uses ApiClient for real API calls instead of simulation.
 * 
 * **Per LOCK_SCREEN_AND_CASHIER_LOGIN.md:**
 * - POST /api/Employee/Login - Cashier authentication with till
 * - POST /api/Employee/VerifyPassword - PIN verification on lock screen
 * - POST /api/Employee/LockDevice - Report lock/unlock events
 * - POST /api/Employee/Logout - End session
 * - POST /api/Employee/LogoutWithEndOfShift - End-of-shift logout
 * - GET /api/Employee/GetProfile - Get user profile
 */
class DefaultApiAuthService(
    private val tokenStorage: TokenStorage,
    private val apiClient: ApiClient
) : ApiAuthService {
    
    companion object {
        /**
         * Per LOCK_SCREEN_AND_CASHIER_LOGIN.md: Correct API endpoints
         */
        private const val ENDPOINT_LOGIN = "/api/Employee/Login"
        private const val ENDPOINT_PROFILE = "/api/Employee/GetProfile"
        private const val ENDPOINT_REFRESH = "/api/Employee/RefreshToken"
        private const val ENDPOINT_LOGOUT = "/api/Employee/Logout"
        private const val ENDPOINT_LOGOUT_END_OF_SHIFT = "/api/Employee/LogoutWithEndOfShift"
        private const val ENDPOINT_VERIFY_PASSWORD = "/api/Employee/VerifyPassword"
        private const val ENDPOINT_LOCK_DEVICE = "/api/Employee/LockDevice"
        
        // Default branch ID (should come from SecureStorage in production)
        private const val DEFAULT_BRANCH_ID = 1
    }
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    override val isAuthenticated: Boolean
        get() = _authState.value is AuthState.Authenticated
    
    override val bearerToken: String?
        get() = (_authState.value as? AuthState.Authenticated)?.token
    
    override suspend fun employeeGroPOSLogin(username: String, password: String): Result<AuthUser> {
        _authState.value = AuthState.Authenticating
        
        return try {
            // Per API_INTEGRATION.md: POST /employee/login with credentials
            val tokenResult = apiClient.deviceRequest<TokenResponseDto> {
                post(ENDPOINT_LOGIN) {
                    setBody(CredentialLoginRequest(
                        username = username,
                        password = password,
                        branchId = DEFAULT_BRANCH_ID
                    ))
                }
            }
            
            tokenResult.fold(
                onSuccess = { tokenResponse ->
                    // Store tokens securely
                    tokenStorage.saveAccessToken(tokenResponse.accessToken)
                    tokenResponse.refreshToken?.let { tokenStorage.saveRefreshToken(it) }
                    
                    // Fetch user profile
                    val userResult = fetchUserProfile()
                    
                    userResult.fold(
                        onSuccess = { user ->
                            val expiresAt = Clock.System.now() + tokenResponse.expiresIn.seconds
                            
                            _authState.value = AuthState.Authenticated(
                                user = user,
                                token = tokenResponse.accessToken,
                                expiresAt = expiresAt,
                                refreshToken = tokenResponse.refreshToken
                            )
                            
                            println("ApiAuthService: Login successful for ${user.username}")
                            Result.success(user)
                        },
                        onFailure = { error ->
                            // Token received but profile fetch failed - use minimal user
                            val minimalUser = AuthUser(
                                id = "unknown",
                                username = username,
                                role = UserRole.CASHIER,
                                permissions = emptyList(),
                                isManager = false
                            )
                            
                            val expiresAt = Clock.System.now() + tokenResponse.expiresIn.seconds
                            
                            _authState.value = AuthState.Authenticated(
                                user = minimalUser,
                                token = tokenResponse.accessToken,
                                expiresAt = expiresAt,
                                refreshToken = tokenResponse.refreshToken
                            )
                            
                            println("ApiAuthService: Login successful (profile fetch failed)")
                            Result.success(minimalUser)
                        }
                    )
                },
                onFailure = { error ->
                    _authState.value = AuthState.Error(error.message ?: "Login failed")
                    Result.failure(AuthException(error.message ?: "Invalid username or password"))
                }
            )
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.message ?: "Authentication failed")
            Result.failure(e)
        }
    }
    
    override suspend fun employeePinLogin(pin: String): Result<AuthUser> {
        _authState.value = AuthState.Authenticating
        
        return try {
            // Per API_INTEGRATION.md: POST /employee/login with PIN
            val tokenResult = apiClient.deviceRequest<TokenResponseDto> {
                post(ENDPOINT_LOGIN) {
                    setBody(LoginRequest(
                        pin = pin,
                        branchId = DEFAULT_BRANCH_ID
                    ))
                }
            }
            
            tokenResult.fold(
                onSuccess = { tokenResponse ->
                    // Store tokens securely
                    tokenStorage.saveAccessToken(tokenResponse.accessToken)
                    tokenResponse.refreshToken?.let { tokenStorage.saveRefreshToken(it) }
                    
                    // Fetch user profile
                    val userResult = fetchUserProfile()
                    
                    userResult.fold(
                        onSuccess = { user ->
                            val expiresAt = Clock.System.now() + tokenResponse.expiresIn.seconds
                            
                            _authState.value = AuthState.Authenticated(
                                user = user,
                                token = tokenResponse.accessToken,
                                expiresAt = expiresAt,
                                refreshToken = tokenResponse.refreshToken
                            )
                            
                            println("ApiAuthService: PIN login successful for ${user.username}")
                            Result.success(user)
                        },
                        onFailure = { error ->
                            // Token received but profile fetch failed - use minimal user
                            val minimalUser = AuthUser(
                                id = "unknown",
                                username = "Employee",
                                role = UserRole.CASHIER,
                                permissions = emptyList(),
                                isManager = false
                            )
                            
                            val expiresAt = Clock.System.now() + tokenResponse.expiresIn.seconds
                            
                            _authState.value = AuthState.Authenticated(
                                user = minimalUser,
                                token = tokenResponse.accessToken,
                                expiresAt = expiresAt,
                                refreshToken = tokenResponse.refreshToken
                            )
                            
                            println("ApiAuthService: PIN login successful (profile fetch failed)")
                            Result.success(minimalUser)
                        }
                    )
                },
                onFailure = { error ->
                    _authState.value = AuthState.Error(error.message ?: "PIN login failed")
                    Result.failure(AuthException(error.message ?: "Invalid PIN"))
                }
            )
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.message ?: "PIN authentication failed")
            Result.failure(e)
        }
    }
    
    override suspend fun refreshToken(): Result<Unit> {
        val currentState = _authState.value
        
        val refreshTokenValue = when (currentState) {
            is AuthState.Authenticated -> currentState.refreshToken
            is AuthState.TokenExpired -> currentState.refreshToken
            else -> null
        } ?: return Result.failure(AuthException("No refresh token available"))
        
        return try {
            // Per API_INTEGRATION.md: POST /employee/refresh
            val tokenResult = apiClient.deviceRequest<TokenResponseDto> {
                post(ENDPOINT_REFRESH) {
                    setBody(RefreshTokenRequest(
                        token = refreshTokenValue,
                        clientName = "device"
                    ))
                }
            }
            
            tokenResult.fold(
                onSuccess = { tokenResponse ->
                    tokenStorage.saveAccessToken(tokenResponse.accessToken)
                    tokenResponse.refreshToken?.let { tokenStorage.saveRefreshToken(it) }
                    
                    // Preserve existing user from state
                    val existingUser = when (currentState) {
                        is AuthState.Authenticated -> currentState.user
                        is AuthState.TokenExpired -> currentState.user
                        else -> AuthUser(
                            id = "unknown",
                            username = "Employee",
                            role = UserRole.CASHIER,
                            permissions = emptyList(),
                            isManager = false
                        )
                    }
                    
                    val expiresAt = Clock.System.now() + tokenResponse.expiresIn.seconds
                    
                    _authState.value = AuthState.Authenticated(
                        user = existingUser,
                        token = tokenResponse.accessToken,
                        expiresAt = expiresAt,
                        refreshToken = tokenResponse.refreshToken ?: refreshTokenValue
                    )
                    
                    println("ApiAuthService: Token refreshed successfully")
                    Result.success(Unit)
                },
                onFailure = { error ->
                    _authState.value = AuthState.Unauthenticated
                    tokenStorage.clearTokens()
                    Result.failure(AuthException("Token refresh failed: ${error.message}"))
                }
            )
        } catch (e: Exception) {
            _authState.value = AuthState.Unauthenticated
            tokenStorage.clearTokens()
            Result.failure(e)
        }
    }
    
    override suspend fun logout(): Result<Unit> {
        return try {
            // Per API_INTEGRATION.md: POST /employee/logout
            // Fire and forget - we clear local state regardless of API response
            apiClient.authenticatedRequest<Unit> {
                post(ENDPOINT_LOGOUT)
            }
            
            tokenStorage.clearTokens()
            _authState.value = AuthState.Unauthenticated
            
            println("ApiAuthService: Logout successful")
            Result.success(Unit)
        } catch (e: Exception) {
            // Still clear local state even if API call fails
            tokenStorage.clearTokens()
            _authState.value = AuthState.Unauthenticated
            println("ApiAuthService: Logout completed (API call failed: ${e.message})")
            Result.success(Unit) // Return success anyway - user is logged out locally
        }
    }
    
    /**
     * Fetches the user profile from the API.
     * 
     * Per API_INTEGRATION.md: GET /employee/profile
     */
    private suspend fun fetchUserProfile(): Result<AuthUser> {
        return apiClient.authenticatedRequest<UserProfileDto> {
            get(ENDPOINT_PROFILE)
        }.map { it.toDomain() }
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

