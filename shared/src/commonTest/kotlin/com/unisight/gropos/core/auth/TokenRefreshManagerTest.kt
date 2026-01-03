package com.unisight.gropos.core.auth

import com.unisight.gropos.features.auth.domain.model.AuthUser
import com.unisight.gropos.features.auth.domain.model.UserRole
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Unit tests for TokenRefreshManager.
 * 
 * **Per QA Audit Finding (HIGH):**
 * - Race condition when concurrent 401s trigger multiple refresh attempts
 * - Must use Mutex to ensure only ONE refresh happens at a time
 * - Waiting callers must receive the result of the in-flight refresh
 * 
 * Per testing-strategy.mdc:
 * - Use StandardTestDispatcher for controllable time
 * - Test concurrent scenarios with multiple async blocks
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TokenRefreshManagerTest {
    
    // ========================================================================
    // Basic Refresh Tests
    // ========================================================================
    
    @Test
    fun `forceRefresh succeeds when auth service succeeds`() = runTest {
        val authService = FakeApiAuthService()
        authService.simulateAuthenticatedState()
        
        val manager = DefaultTokenRefreshManager(
            authService = authService,
            dispatcher = StandardTestDispatcher(testScheduler)
        )
        
        val result = manager.forceRefresh()
        
        assertTrue(result.isSuccess)
        assertIs<TokenStatus.Valid>(manager.tokenStatus.value)
    }
    
    @Test
    fun `forceRefresh fails when auth service fails`() = runTest {
        val authService = FakeApiAuthService()
        authService.simulateRefreshFailure = true
        
        val manager = DefaultTokenRefreshManager(
            authService = authService,
            config = TokenRefreshConfig(maxRetries = 1, retryDelay = 0.seconds),
            dispatcher = StandardTestDispatcher(testScheduler)
        )
        
        val result = manager.forceRefresh()
        
        assertTrue(result.isFailure)
        assertIs<TokenStatus.RefreshFailed>(manager.tokenStatus.value)
    }
    
    @Test
    fun `handleUnauthorized returns true when refresh succeeds`() = runTest {
        val authService = FakeApiAuthService()
        authService.simulateAuthenticatedState()
        
        val manager = DefaultTokenRefreshManager(
            authService = authService,
            dispatcher = StandardTestDispatcher(testScheduler)
        )
        
        val result = manager.handleUnauthorized()
        
        assertTrue(result)
    }
    
    @Test
    fun `handleUnauthorized returns false when refresh fails`() = runTest {
        val authService = FakeApiAuthService()
        authService.simulateRefreshFailure = true
        
        val manager = DefaultTokenRefreshManager(
            authService = authService,
            config = TokenRefreshConfig(maxRetries = 1, retryDelay = 0.seconds),
            dispatcher = StandardTestDispatcher(testScheduler)
        )
        
        val result = manager.handleUnauthorized()
        
        assertFalse(result)
    }
    
    // ========================================================================
    // Retry Logic Tests
    // ========================================================================
    
    @Test
    fun `forceRefresh retries up to maxRetries times`() = runTest {
        val authService = FakeApiAuthService()
        authService.simulateRefreshFailure = true
        
        val manager = DefaultTokenRefreshManager(
            authService = authService,
            config = TokenRefreshConfig(maxRetries = 3, retryDelay = 0.seconds),
            dispatcher = StandardTestDispatcher(testScheduler)
        )
        
        manager.forceRefresh()
        
        assertEquals(3, authService.refreshCallCount)
    }
    
    @Test
    fun `forceRefresh succeeds on third attempt`() = runTest {
        val authService = FakeApiAuthService()
        authService.failureCountBeforeSuccess = 2 // Fail first 2, succeed on 3rd
        
        val manager = DefaultTokenRefreshManager(
            authService = authService,
            config = TokenRefreshConfig(maxRetries = 3, retryDelay = 0.seconds),
            dispatcher = StandardTestDispatcher(testScheduler)
        )
        
        val result = manager.forceRefresh()
        
        assertTrue(result.isSuccess)
        assertEquals(3, authService.refreshCallCount)
    }
    
    // ========================================================================
    // CRITICAL: Concurrent 401 Handling (Race Condition Fix)
    // ========================================================================
    
    @Test
    fun `concurrent handleUnauthorized calls result in single refresh`() = runTest {
        val authService = FakeApiAuthService()
        authService.refreshDelay = 100 // Slow refresh to ensure overlap
        authService.simulateAuthenticatedState()
        
        val manager = DefaultTokenRefreshManager(
            authService = authService,
            dispatcher = StandardTestDispatcher(testScheduler)
        )
        
        // Launch 5 concurrent 401 handlers
        val results = listOf(
            async { manager.handleUnauthorized() },
            async { manager.handleUnauthorized() },
            async { manager.handleUnauthorized() },
            async { manager.handleUnauthorized() },
            async { manager.handleUnauthorized() }
        )
        
        advanceUntilIdle()
        
        val allResults = results.awaitAll()
        
        // All should succeed
        assertTrue(allResults.all { it }, "All concurrent calls should succeed")
        
        // But only ONE refresh should have been made (the fix!)
        assertEquals(
            1,
            authService.refreshCallCount,
            "Only one refresh should occur for concurrent 401s"
        )
    }
    
    @Test
    fun `concurrent forceRefresh calls share single refresh result`() = runTest {
        val authService = FakeApiAuthService()
        authService.refreshDelay = 100
        authService.simulateAuthenticatedState()
        
        val manager = DefaultTokenRefreshManager(
            authService = authService,
            dispatcher = StandardTestDispatcher(testScheduler)
        )
        
        // Launch 3 concurrent force refresh
        val results = listOf(
            async { manager.forceRefresh() },
            async { manager.forceRefresh() },
            async { manager.forceRefresh() }
        )
        
        advanceUntilIdle()
        
        val allResults = results.awaitAll()
        
        // All should succeed
        assertTrue(allResults.all { it.isSuccess })
        
        // Only one actual refresh
        assertEquals(1, authService.refreshCallCount)
    }
    
    @Test
    fun `waiting callers receive failure when refresh fails`() = runTest {
        val authService = FakeApiAuthService()
        authService.refreshDelay = 100
        authService.simulateRefreshFailure = true
        
        val manager = DefaultTokenRefreshManager(
            authService = authService,
            config = TokenRefreshConfig(maxRetries = 1, retryDelay = 0.seconds),
            dispatcher = StandardTestDispatcher(testScheduler)
        )
        
        // Launch concurrent calls
        val results = listOf(
            async { manager.handleUnauthorized() },
            async { manager.handleUnauthorized() },
            async { manager.handleUnauthorized() }
        )
        
        advanceUntilIdle()
        
        val allResults = results.awaitAll()
        
        // All should fail (shared failure result)
        assertTrue(allResults.none { it }, "All should fail when refresh fails")
        
        // Only one refresh attempt (not 3)
        assertEquals(1, authService.refreshCallCount)
    }
    
    @Test
    fun `second batch of 401s after first completes triggers new refresh`() = runTest {
        val authService = FakeApiAuthService()
        authService.refreshDelay = 50
        authService.simulateAuthenticatedState()
        
        val manager = DefaultTokenRefreshManager(
            authService = authService,
            dispatcher = StandardTestDispatcher(testScheduler)
        )
        
        // First batch
        val firstResult = manager.handleUnauthorized()
        advanceUntilIdle()
        assertTrue(firstResult)
        assertEquals(1, authService.refreshCallCount)
        
        // Second batch (should trigger new refresh)
        val secondResult = manager.handleUnauthorized()
        advanceUntilIdle()
        assertTrue(secondResult)
        assertEquals(2, authService.refreshCallCount)
    }
    
    // ========================================================================
    // Token Expiration Monitoring Tests
    // ========================================================================
    
    @Test
    fun `startMonitoring detects expiring token`() = runTest {
        val authService = FakeApiAuthService()
        // Token expires in 3 minutes (within 5 minute threshold)
        authService.simulateAuthenticatedState(expiresIn = 3.minutes)
        
        val manager = DefaultTokenRefreshManager(
            authService = authService,
            config = TokenRefreshConfig(
                refreshThreshold = 5.minutes,
                checkInterval = 1.seconds
            ),
            dispatcher = StandardTestDispatcher(testScheduler)
        )
        
        manager.startMonitoring()
        advanceTimeBy(2.seconds.inWholeMilliseconds)
        
        // Should detect expiring soon and trigger refresh
        assertIs<TokenStatus.Valid>(manager.tokenStatus.value)
        
        manager.stopMonitoring()
    }
    
    // ========================================================================
    // Status Flow Tests
    // ========================================================================
    
    @Test
    fun `tokenStatus updates to Refreshing during refresh`() = runTest {
        val authService = FakeApiAuthService()
        authService.refreshDelay = 500
        authService.simulateAuthenticatedState()
        
        val manager = DefaultTokenRefreshManager(
            authService = authService,
            dispatcher = StandardTestDispatcher(testScheduler)
        )
        
        val statusesSeen = mutableListOf<TokenStatus>()
        
        // Start refresh
        val refreshJob = async { manager.forceRefresh() }
        
        // Capture status after starting
        advanceTimeBy(100)
        statusesSeen.add(manager.tokenStatus.value)
        
        advanceUntilIdle()
        refreshJob.await()
        statusesSeen.add(manager.tokenStatus.value)
        
        // Should have seen Refreshing at some point
        assertTrue(
            statusesSeen.any { it is TokenStatus.Refreshing },
            "Should show Refreshing status during refresh"
        )
        
        // Should end in Valid
        assertIs<TokenStatus.Valid>(statusesSeen.last())
    }
}

// ============================================================================
// Fake ApiAuthService for Testing
// ============================================================================

/**
 * Fake implementation of ApiAuthService for testing TokenRefreshManager.
 */
class FakeApiAuthService : ApiAuthService {
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    override val authState: StateFlow<AuthState> = _authState
    
    override val isAuthenticated: Boolean
        get() = _authState.value is AuthState.Authenticated
    
    override val bearerToken: String?
        get() = (_authState.value as? AuthState.Authenticated)?.token
    
    // Test control flags
    var simulateRefreshFailure = false
    var refreshDelay: Long = 0
    var refreshCallCount = 0
    var failureCountBeforeSuccess = 0
    private var currentFailures = 0
    
    fun simulateAuthenticatedState(expiresIn: kotlin.time.Duration = 1.hours) {
        _authState.value = AuthState.Authenticated(
            user = AuthUser(
                id = "test-user",
                username = "Test User",
                role = UserRole.CASHIER,
                permissions = emptyList(),
                isManager = false
            ),
            token = "test-token-${Clock.System.now().toEpochMilliseconds()}",
            expiresAt = Clock.System.now() + expiresIn,
            refreshToken = "test-refresh-token"
        )
    }
    
    override suspend fun employeeGroPOSLogin(username: String, password: String): Result<AuthUser> {
        return Result.failure(Exception("Not implemented in fake"))
    }
    
    override suspend fun employeePinLogin(pin: String): Result<AuthUser> {
        return Result.failure(Exception("Not implemented in fake"))
    }
    
    override suspend fun refreshToken(): Result<Unit> {
        refreshCallCount++
        
        if (refreshDelay > 0) {
            delay(refreshDelay)
        }
        
        // Check if should fail
        if (simulateRefreshFailure) {
            return Result.failure(Exception("Simulated refresh failure"))
        }
        
        // Check if should fail first N times
        if (currentFailures < failureCountBeforeSuccess) {
            currentFailures++
            return Result.failure(Exception("Simulated failure $currentFailures"))
        }
        
        // Success - update state
        simulateAuthenticatedState()
        return Result.success(Unit)
    }
    
    override suspend fun logout(): Result<Unit> {
        _authState.value = AuthState.Unauthenticated
        return Result.success(Unit)
    }
    
    override fun setBearerToken(token: String, user: AuthUser) {
        _authState.value = AuthState.Authenticated(
            user = user,
            token = token,
            expiresAt = Clock.System.now() + 1.hours,
            refreshToken = null
        )
    }
    
    override fun needsTokenRefresh(): Boolean {
        return false
    }
}

