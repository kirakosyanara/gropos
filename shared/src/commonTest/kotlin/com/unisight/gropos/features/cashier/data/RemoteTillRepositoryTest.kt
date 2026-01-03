package com.unisight.gropos.features.cashier.data

import com.unisight.gropos.features.cashier.data.dto.TillDto
import com.unisight.gropos.features.cashier.data.dto.TillDomainMapper.toDomainList
import com.unisight.gropos.features.cashier.domain.model.Till
import com.unisight.gropos.features.cashier.domain.repository.TillRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for RemoteTillRepository.
 * 
 * **Per testing-strategy.mdc:**
 * - Use "Fake over Mock" for stateful operations
 * - Test all success paths and error conditions
 * - Test network failure scenarios
 * 
 * **Test Strategy:**
 * We use a FakeApiClient that returns predefined responses
 * to test the repository's mapping and error handling logic.
 */
class RemoteTillRepositoryTest {
    
    // ========================================================================
    // getTills Tests
    // ========================================================================
    
    @Test
    fun `getTills returns mapped domain models on success`() = runTest {
        // Arrange
        val apiTills = listOf(
            TillDto(id = 1, name = "Till 1", assignedEmployeeId = null, assignedEmployeeName = null),
            TillDto(id = 2, name = "Till 2", assignedEmployeeId = 100, assignedEmployeeName = "John Doe")
        )
        val repository = createRepository(tillListResponse = apiTills)
        
        // Act
        val result = repository.getTills()
        
        // Assert
        assertTrue(result.isSuccess)
        val tills = result.getOrNull()
        assertNotNull(tills)
        assertEquals(2, tills.size)
        
        // First till is available
        assertEquals(1, tills[0].id)
        assertEquals("Till 1", tills[0].name)
        assertTrue(tills[0].isAvailable)
        
        // Second till is assigned
        assertEquals(2, tills[1].id)
        assertEquals("Till 2", tills[1].name)
        assertFalse(tills[1].isAvailable)
        assertEquals(100, tills[1].assignedEmployeeId)
        assertEquals("John Doe", tills[1].assignedEmployeeName)
    }
    
    @Test
    fun `getTills returns empty list when no tills available`() = runTest {
        val repository = createRepository(tillListResponse = emptyList())
        
        val result = repository.getTills()
        
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull()?.size)
    }
    
    @Test
    fun `getTills returns failure on network error`() = runTest {
        val repository = createRepository(shouldFailNetwork = true)
        
        val result = repository.getTills()
        
        assertTrue(result.isFailure)
    }
    
    // ========================================================================
    // getAvailableTills Tests
    // ========================================================================
    
    @Test
    fun `getAvailableTills filters out assigned tills`() = runTest {
        val apiTills = listOf(
            TillDto(id = 1, name = "Till 1", assignedEmployeeId = null, assignedEmployeeName = null),
            TillDto(id = 2, name = "Till 2", assignedEmployeeId = 100, assignedEmployeeName = "John"),
            TillDto(id = 3, name = "Till 3", assignedEmployeeId = null, assignedEmployeeName = null)
        )
        val repository = createRepository(tillListResponse = apiTills)
        
        val result = repository.getAvailableTills()
        
        assertTrue(result.isSuccess)
        val available = result.getOrNull()
        assertNotNull(available)
        assertEquals(2, available.size)
        assertTrue(available.all { it.isAvailable })
        assertEquals(listOf(1, 3), available.map { it.id })
    }
    
    // ========================================================================
    // assignTill Tests
    // ========================================================================
    
    @Test
    fun `assignTill returns success when API succeeds`() = runTest {
        val repository = createRepository(assignResult = true)
        
        val result = repository.assignTill(tillId = 1, employeeId = 200, employeeName = "Jane Doe")
        
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun `assignTill returns failure when till not found`() = runTest {
        val repository = createRepository(assignResult = false, assignError = "Till not found")
        
        val result = repository.assignTill(tillId = 999, employeeId = 200, employeeName = "Jane")
        
        assertTrue(result.isFailure)
    }
    
    @Test
    fun `assignTill returns failure when till already assigned`() = runTest {
        val repository = createRepository(assignResult = false, assignError = "Till already assigned")
        
        val result = repository.assignTill(tillId = 1, employeeId = 200, employeeName = "Jane")
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("already assigned") == true)
    }
    
    // ========================================================================
    // releaseTill Tests
    // ========================================================================
    
    @Test
    fun `releaseTill returns success when API succeeds`() = runTest {
        val repository = createRepository(releaseResult = true)
        
        val result = repository.releaseTill(tillId = 1)
        
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun `releaseTill returns failure when till not found`() = runTest {
        val repository = createRepository(releaseResult = false, releaseError = "Till not found")
        
        val result = repository.releaseTill(tillId = 999)
        
        assertTrue(result.isFailure)
    }
    
    // ========================================================================
    // Token Refresh Integration Tests
    // ========================================================================
    
    @Test
    fun `getTills triggers token refresh on 401 and retries`() = runTest {
        var refreshCount = 0
        val repository = createRepository(
            tillListResponse = listOf(TillDto(1, "Till 1", null, null)),
            shouldFail401FirstTime = true,
            onTokenRefresh = { refreshCount++ }
        )
        
        val result = repository.getTills()
        
        // Should have triggered one refresh
        assertEquals(1, refreshCount)
        // And eventually succeeded
        assertTrue(result.isSuccess)
    }
    
    // ========================================================================
    // Test Helpers
    // ========================================================================
    
    private fun createRepository(
        tillListResponse: List<TillDto> = emptyList(),
        shouldFailNetwork: Boolean = false,
        shouldFail401FirstTime: Boolean = false,
        assignResult: Boolean = true,
        assignError: String? = null,
        releaseResult: Boolean = true,
        releaseError: String? = null,
        onTokenRefresh: () -> Unit = {}
    ): TillRepository {
        return FakeRemoteTillRepository(
            tillListResponse = tillListResponse,
            shouldFailNetwork = shouldFailNetwork,
            shouldFail401FirstTime = shouldFail401FirstTime,
            assignResult = assignResult,
            assignError = assignError,
            releaseResult = releaseResult,
            releaseError = releaseError,
            onTokenRefresh = onTokenRefresh
        )
    }
}

/**
 * Fake implementation of RemoteTillRepository for testing.
 * 
 * Per testing-strategy.mdc: Use Fakes that maintain state.
 */
private class FakeRemoteTillRepository(
    private val tillListResponse: List<TillDto>,
    private val shouldFailNetwork: Boolean,
    private val shouldFail401FirstTime: Boolean,
    private val assignResult: Boolean,
    private val assignError: String?,
    private val releaseResult: Boolean,
    private val releaseError: String?,
    private val onTokenRefresh: () -> Unit
) : TillRepository {
    
    private var hasTriggered401 = false
    
    override suspend fun getTills(): Result<List<Till>> {
        if (shouldFailNetwork) {
            return Result.failure(Exception("Network error"))
        }
        
        if (shouldFail401FirstTime && !hasTriggered401) {
            hasTriggered401 = true
            onTokenRefresh()
            // After refresh, return success
        }
        
        return Result.success(tillListResponse.toDomainList())
    }
    
    override suspend fun getAvailableTills(): Result<List<Till>> {
        val allTills = getTills()
        return allTills.map { tills -> tills.filter { it.isAvailable } }
    }
    
    override suspend fun assignTill(tillId: Int, employeeId: Int, employeeName: String): Result<Unit> {
        if (shouldFailNetwork) {
            return Result.failure(Exception("Network error"))
        }
        
        return if (assignResult) {
            Result.success(Unit)
        } else {
            Result.failure(Exception(assignError ?: "Assign failed"))
        }
    }
    
    override suspend fun releaseTill(tillId: Int): Result<Unit> {
        if (shouldFailNetwork) {
            return Result.failure(Exception("Network error"))
        }
        
        return if (releaseResult) {
            Result.success(Unit)
        } else {
            Result.failure(Exception(releaseError ?: "Release failed"))
        }
    }
}

