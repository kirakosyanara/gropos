package com.unisight.gropos.features.auth.presentation

import com.unisight.gropos.features.auth.domain.model.AuthError
import com.unisight.gropos.features.auth.domain.model.AuthUser
import com.unisight.gropos.features.auth.domain.model.UserRole
import com.unisight.gropos.features.auth.domain.repository.AuthRepository
import com.unisight.gropos.features.auth.domain.usecase.ValidateLoginUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Integration tests for LoginViewModel.
 * 
 * Per testing-strategy.mdc:
 * - Use runTest from kotlinx-coroutines-test
 * - Inject TestScope for coroutine control
 * - Test state transitions: Idle -> Loading -> Success/Error
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    
    // ========================================================================
    // Test Doubles
    // ========================================================================
    
    private class FakeAuthRepository(
        private val shouldSucceed: Boolean = true
    ) : AuthRepository {
        
        override suspend fun login(username: String, pin: String): Result<AuthUser> {
            return if (shouldSucceed) {
                Result.success(
                    AuthUser(
                        id = "1",
                        username = username,
                        role = UserRole.ADMIN
                    )
                )
            } else {
                Result.failure(AuthError.InvalidCredentials)
            }
        }
        
        override suspend fun logout() {}
        
        override suspend fun getCurrentUser(): AuthUser? = null
    }
    
    // ========================================================================
    // Helper to create ViewModel with test scope
    // ========================================================================
    
    private fun createViewModel(
        repository: AuthRepository,
        testScope: TestScope
    ): LoginViewModel {
        val useCase = ValidateLoginUseCase(repository)
        return LoginViewModel(
            validateLogin = useCase,
            coroutineScope = testScope
        )
    }
    
    // ========================================================================
    // State Transition Tests
    // ========================================================================
    
    @Test
    fun `initial state should be Idle`() = runTest {
        // Given
        val viewModel = createViewModel(FakeAuthRepository(), this)
        
        // Then
        assertIs<LoginUiState.Idle>(viewModel.state.value)
    }
    
    @Test
    fun `should transition from Idle to Loading to Success on valid login`() = runTest(
        UnconfinedTestDispatcher()
    ) {
        // Given
        val viewModel = createViewModel(FakeAuthRepository(shouldSucceed = true), this)
        
        // Verify initial state
        assertIs<LoginUiState.Idle>(viewModel.state.value)
        
        // When
        viewModel.onLoginClick("admin", "1234")
        
        // Then - with UnconfinedTestDispatcher, coroutines complete immediately
        advanceUntilIdle()
        
        // Verify final state is Success
        val finalState = viewModel.state.value
        assertIs<LoginUiState.Success>(finalState)
        assertEquals("admin", finalState.user.username)
        assertEquals(UserRole.ADMIN, finalState.user.role)
    }
    
    @Test
    fun `should transition from Idle to Loading to Error on invalid credentials`() = runTest(
        UnconfinedTestDispatcher()
    ) {
        // Given
        val viewModel = createViewModel(FakeAuthRepository(shouldSucceed = false), this)
        
        // When
        viewModel.onLoginClick("admin", "1234")
        advanceUntilIdle()
        
        // Then
        val finalState = viewModel.state.value
        assertIs<LoginUiState.Error>(finalState)
        assertEquals("Invalid username or PIN", finalState.message)
    }
    
    @Test
    fun `should show Error immediately for invalid PIN format`() = runTest(
        UnconfinedTestDispatcher()
    ) {
        // Given
        val viewModel = createViewModel(FakeAuthRepository(), this)
        
        // When - PIN is too short
        viewModel.onLoginClick("admin", "123")
        advanceUntilIdle()
        
        // Then - should fail PIN validation before reaching repository
        val finalState = viewModel.state.value
        assertIs<LoginUiState.Error>(finalState)
        assertEquals("PIN must be exactly 4 digits", finalState.message)
    }
    
    @Test
    fun `onErrorDismissed should reset state to Idle`() = runTest(
        UnconfinedTestDispatcher()
    ) {
        // Given
        val viewModel = createViewModel(FakeAuthRepository(shouldSucceed = false), this)
        
        // Put ViewModel in Error state
        viewModel.onLoginClick("admin", "1234")
        advanceUntilIdle()
        assertIs<LoginUiState.Error>(viewModel.state.value)
        
        // When
        viewModel.onErrorDismissed()
        
        // Then
        assertIs<LoginUiState.Idle>(viewModel.state.value)
    }
    
    @Test
    fun `resetState should return to Idle from any state`() = runTest(
        UnconfinedTestDispatcher()
    ) {
        // Given - viewModel in Success state
        val viewModel = createViewModel(FakeAuthRepository(shouldSucceed = true), this)
        viewModel.onLoginClick("admin", "1234")
        advanceUntilIdle()
        assertIs<LoginUiState.Success>(viewModel.state.value)
        
        // When
        viewModel.resetState()
        
        // Then
        assertIs<LoginUiState.Idle>(viewModel.state.value)
    }
}
