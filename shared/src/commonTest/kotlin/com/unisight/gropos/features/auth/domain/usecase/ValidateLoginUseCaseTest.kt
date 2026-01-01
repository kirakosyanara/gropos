package com.unisight.gropos.features.auth.domain.usecase

import com.unisight.gropos.features.auth.domain.model.AuthError
import com.unisight.gropos.features.auth.domain.model.AuthUser
import com.unisight.gropos.features.auth.domain.model.UserRole
import com.unisight.gropos.features.auth.domain.repository.AuthRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Unit tests for ValidateLoginUseCase.
 * 
 * Per testing-strategy.mdc: Write tests before implementation (TDD).
 * These tests validate PIN format and login flow.
 */
class ValidateLoginUseCaseTest {
    
    // ========================================================================
    // Test Doubles
    // ========================================================================
    
    /**
     * Fake repository that returns success for admin/1234, failure otherwise.
     */
    private class FakeAuthRepository : AuthRepository {
        
        var loginCallCount = 0
            private set
        
        override suspend fun login(username: String, pin: String): Result<AuthUser> {
            loginCallCount++
            return if (username == "admin" && pin == "1234") {
                Result.success(
                    AuthUser(
                        id = "1",
                        username = "admin",
                        role = UserRole.ADMIN
                    )
                )
            } else {
                Result.failure(AuthError.InvalidCredentials)
            }
        }
        
        override suspend fun logout() {
            // No-op for tests
        }
        
        override suspend fun getCurrentUser(): AuthUser? = null
    }
    
    // ========================================================================
    // PIN Validation Tests
    // ========================================================================
    
    @Test
    fun `should return failure when PIN is empty`() = runTest {
        // Given
        val repository = FakeAuthRepository()
        val useCase = ValidateLoginUseCase(repository)
        
        // When
        val result = useCase("admin", "")
        
        // Then
        assertTrue(result.isFailure)
        assertIs<AuthError.InvalidPinFormat>(result.exceptionOrNull())
        assertEquals(0, repository.loginCallCount, "Repository should NOT be called for invalid PIN")
    }
    
    @Test
    fun `should return failure when PIN has less than 4 digits`() = runTest {
        // Given
        val repository = FakeAuthRepository()
        val useCase = ValidateLoginUseCase(repository)
        
        // When
        val result = useCase("admin", "123")
        
        // Then
        assertTrue(result.isFailure)
        assertIs<AuthError.InvalidPinFormat>(result.exceptionOrNull())
        assertEquals(0, repository.loginCallCount)
    }
    
    @Test
    fun `should return failure when PIN has more than 4 digits`() = runTest {
        // Given
        val repository = FakeAuthRepository()
        val useCase = ValidateLoginUseCase(repository)
        
        // When
        val result = useCase("admin", "12345")
        
        // Then
        assertTrue(result.isFailure)
        assertIs<AuthError.InvalidPinFormat>(result.exceptionOrNull())
        assertEquals(0, repository.loginCallCount)
    }
    
    @Test
    fun `should return failure when PIN contains non-digit characters`() = runTest {
        // Given
        val repository = FakeAuthRepository()
        val useCase = ValidateLoginUseCase(repository)
        
        // When
        val result = useCase("admin", "12ab")
        
        // Then
        assertTrue(result.isFailure)
        assertIs<AuthError.InvalidPinFormat>(result.exceptionOrNull())
        assertEquals(0, repository.loginCallCount)
    }
    
    // ========================================================================
    // Successful Login Tests
    // ========================================================================
    
    @Test
    fun `should return success when PIN is valid and credentials are correct`() = runTest {
        // Given
        val repository = FakeAuthRepository()
        val useCase = ValidateLoginUseCase(repository)
        
        // When
        val result = useCase("admin", "1234")
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals("admin", result.getOrNull()?.username)
        assertEquals(UserRole.ADMIN, result.getOrNull()?.role)
        assertEquals(1, repository.loginCallCount, "Repository should be called once")
    }
    
    // ========================================================================
    // Failed Login Tests (Valid PIN, Wrong Credentials)
    // ========================================================================
    
    @Test
    fun `should return failure when PIN is valid but credentials are wrong`() = runTest {
        // Given
        val repository = FakeAuthRepository()
        val useCase = ValidateLoginUseCase(repository)
        
        // When
        val result = useCase("admin", "9999")
        
        // Then
        assertTrue(result.isFailure)
        assertIs<AuthError.InvalidCredentials>(result.exceptionOrNull())
        assertEquals(1, repository.loginCallCount, "Repository should be called for valid PIN format")
    }
    
    @Test
    fun `should return failure when username is wrong`() = runTest {
        // Given
        val repository = FakeAuthRepository()
        val useCase = ValidateLoginUseCase(repository)
        
        // When
        val result = useCase("unknown", "1234")
        
        // Then
        assertTrue(result.isFailure)
        assertIs<AuthError.InvalidCredentials>(result.exceptionOrNull())
        assertEquals(1, repository.loginCallCount)
    }
}

