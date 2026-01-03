package com.unisight.gropos.core.auth

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Token Refresh Manager for automatic token refresh.
 * 
 * Per REMEDIATION_CHECKLIST: Token Refresh - refresh token logic and Manager.setBearerToken().
 * Per AUTH_SESSION.md: Tokens should be refreshed before expiration.
 * 
 * Responsibilities:
 * - Monitor token expiration
 * - Proactively refresh tokens before expiry
 * - Handle refresh failures gracefully
 * - Notify observers of token state changes
 */
interface TokenRefreshManager {
    
    /**
     * Current token status.
     */
    val tokenStatus: StateFlow<TokenStatus>
    
    /**
     * Starts automatic token refresh monitoring.
     */
    suspend fun startMonitoring()
    
    /**
     * Stops automatic token refresh monitoring.
     */
    suspend fun stopMonitoring()
    
    /**
     * Forces an immediate token refresh.
     * 
     * @return Result.success if refresh succeeded
     */
    suspend fun forceRefresh(): Result<Unit>
    
    /**
     * Called when a request fails with 401 Unauthorized.
     * Attempts to refresh token and retry.
     * 
     * @return true if token was refreshed successfully
     */
    suspend fun handleUnauthorized(): Boolean
}

/**
 * Current status of the authentication token.
 */
sealed class TokenStatus {
    /** No token available */
    data object NoToken : TokenStatus()
    
    /** Token is valid */
    data class Valid(val expiresAt: Instant) : TokenStatus()
    
    /** Token is expiring soon (refresh recommended) */
    data class ExpiringSoon(val expiresAt: Instant) : TokenStatus()
    
    /** Token is expired */
    data object Expired : TokenStatus()
    
    /** Token is being refreshed */
    data object Refreshing : TokenStatus()
    
    /** Refresh failed */
    data class RefreshFailed(val reason: String) : TokenStatus()
}

/**
 * Configuration for token refresh.
 */
data class TokenRefreshConfig(
    /** How long before expiry to start refresh */
    val refreshThreshold: Duration = 5.minutes,
    
    /** How often to check token status */
    val checkInterval: Duration = 30.seconds,
    
    /** Maximum retry attempts for refresh */
    val maxRetries: Int = 3,
    
    /** Delay between retry attempts */
    val retryDelay: Duration = 2.seconds
)

/**
 * Default implementation of TokenRefreshManager.
 */
class DefaultTokenRefreshManager(
    private val authService: ApiAuthService,
    private val config: TokenRefreshConfig = TokenRefreshConfig(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : TokenRefreshManager {
    
    private val scope = CoroutineScope(dispatcher + SupervisorJob())
    
    private val _tokenStatus = MutableStateFlow<TokenStatus>(TokenStatus.NoToken)
    override val tokenStatus: StateFlow<TokenStatus> = _tokenStatus.asStateFlow()
    
    private var monitoringJob: Job? = null
    private var isRefreshing = false
    
    override suspend fun startMonitoring() {
        if (monitoringJob?.isActive == true) {
            println("TokenRefreshManager: Already monitoring")
            return
        }
        
        println("TokenRefreshManager: Starting token monitoring")
        
        monitoringJob = scope.launch {
            while (isActive) {
                checkTokenStatus()
                delay(config.checkInterval)
            }
        }
    }
    
    override suspend fun stopMonitoring() {
        println("TokenRefreshManager: Stopping token monitoring")
        monitoringJob?.cancel()
        monitoringJob = null
    }
    
    override suspend fun forceRefresh(): Result<Unit> {
        println("TokenRefreshManager: Forcing token refresh")
        return refreshWithRetry()
    }
    
    override suspend fun handleUnauthorized(): Boolean {
        println("TokenRefreshManager: Handling 401 Unauthorized")
        
        val result = refreshWithRetry()
        return result.isSuccess
    }
    
    private suspend fun checkTokenStatus() {
        val state = authService.authState.value
        
        when (state) {
            is AuthState.Unauthenticated,
            is AuthState.Authenticating,
            is AuthState.Error -> {
                _tokenStatus.value = TokenStatus.NoToken
            }
            is AuthState.TokenExpired -> {
                _tokenStatus.value = TokenStatus.Expired
                // Attempt refresh
                refreshWithRetry()
            }
            is AuthState.Authenticated -> {
                val now = Clock.System.now()
                val expiresAt = state.expiresAt
                val timeUntilExpiry = expiresAt - now
                
                when {
                    timeUntilExpiry <= Duration.ZERO -> {
                        _tokenStatus.value = TokenStatus.Expired
                        refreshWithRetry()
                    }
                    timeUntilExpiry <= config.refreshThreshold -> {
                        _tokenStatus.value = TokenStatus.ExpiringSoon(expiresAt)
                        // Proactively refresh
                        refreshWithRetry()
                    }
                    else -> {
                        _tokenStatus.value = TokenStatus.Valid(expiresAt)
                    }
                }
            }
        }
    }
    
    private suspend fun refreshWithRetry(): Result<Unit> {
        if (isRefreshing) {
            println("TokenRefreshManager: Refresh already in progress")
            return Result.failure(Exception("Refresh already in progress"))
        }
        
        isRefreshing = true
        _tokenStatus.value = TokenStatus.Refreshing
        
        var lastError: Exception? = null
        
        repeat(config.maxRetries) { attempt ->
            println("TokenRefreshManager: Refresh attempt ${attempt + 1}/${config.maxRetries}")
            
            val result = authService.refreshToken()
            
            if (result.isSuccess) {
                isRefreshing = false
                val state = authService.authState.value
                if (state is AuthState.Authenticated) {
                    _tokenStatus.value = TokenStatus.Valid(state.expiresAt)
                }
                println("TokenRefreshManager: Token refreshed successfully")
                return Result.success(Unit)
            }
            
            lastError = result.exceptionOrNull() as? Exception
            
            if (attempt < config.maxRetries - 1) {
                delay(config.retryDelay)
            }
        }
        
        isRefreshing = false
        _tokenStatus.value = TokenStatus.RefreshFailed(lastError?.message ?: "Unknown error")
        println("TokenRefreshManager: Token refresh failed after ${config.maxRetries} attempts")
        
        return Result.failure(lastError ?: Exception("Token refresh failed"))
    }
}

/**
 * Manager object for setting bearer tokens (static access point).
 * 
 * Per REMEDIATION_CHECKLIST: Manager.setBearerToken().
 */
object Manager {
    
    private var authServiceInstance: ApiAuthService? = null
    
    /**
     * Initializes the Manager with an ApiAuthService instance.
     */
    fun initialize(authService: ApiAuthService) {
        authServiceInstance = authService
        println("Manager: Initialized with ApiAuthService")
    }
    
    /**
     * Sets the bearer token directly.
     * 
     * Per REMEDIATION_CHECKLIST: Manager.setBearerToken().
     * 
     * @param token The bearer token
     * @param user The user associated with the token
     */
    fun setBearerToken(token: String, user: com.unisight.gropos.features.auth.domain.model.AuthUser) {
        val service = authServiceInstance 
            ?: throw IllegalStateException("Manager not initialized. Call Manager.initialize() first.")
        
        service.setBearerToken(token, user)
        println("Manager: Bearer token set for ${user.username}")
    }
    
    /**
     * Gets the current bearer token.
     * 
     * @return The bearer token, or null if not authenticated
     */
    fun getBearerToken(): String? {
        return authServiceInstance?.bearerToken
    }
    
    /**
     * Checks if a user is authenticated.
     */
    fun isAuthenticated(): Boolean {
        return authServiceInstance?.isAuthenticated == true
    }
}

/**
 * Simulated implementation for testing.
 */
class SimulatedTokenRefreshManager : TokenRefreshManager {
    
    private val _tokenStatus = MutableStateFlow<TokenStatus>(TokenStatus.NoToken)
    override val tokenStatus: StateFlow<TokenStatus> = _tokenStatus.asStateFlow()
    
    override suspend fun startMonitoring() {
        println("SimulatedTokenRefreshManager: Started monitoring")
    }
    
    override suspend fun stopMonitoring() {
        println("SimulatedTokenRefreshManager: Stopped monitoring")
    }
    
    override suspend fun forceRefresh(): Result<Unit> {
        delay(200)
        _tokenStatus.value = TokenStatus.Valid(Clock.System.now() + 1.hours)
        return Result.success(Unit)
    }
    
    override suspend fun handleUnauthorized(): Boolean {
        return forceRefresh().isSuccess
    }
    
    // Test helpers
    fun setStatus(status: TokenStatus) {
        _tokenStatus.value = status
    }
}

private val Int.hours: Duration
    get() = Duration.parse("${this}h")

