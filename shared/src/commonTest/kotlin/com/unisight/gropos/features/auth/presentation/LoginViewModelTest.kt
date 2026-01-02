package com.unisight.gropos.features.auth.presentation

import com.unisight.gropos.features.auth.domain.hardware.NfcResult
import com.unisight.gropos.features.auth.domain.hardware.NfcScanner
import com.unisight.gropos.features.auth.domain.model.UserRole
import com.unisight.gropos.features.cashier.domain.model.Employee
import com.unisight.gropos.features.cashier.domain.model.Till
import com.unisight.gropos.features.cashier.domain.repository.EmployeeRepository
import com.unisight.gropos.features.cashier.domain.repository.TillRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration tests for LoginViewModel.
 * 
 * Per CASHIER_OPERATIONS.md: Tests the login state machine:
 * LOADING -> EMPLOYEE_SELECT -> PIN_ENTRY -> TILL_ASSIGNMENT -> SUCCESS
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    
    // ========================================================================
    // Test Doubles
    // ========================================================================
    
    private val testEmployees = listOf(
        Employee(
            id = 1,
            firstName = "Jane",
            lastName = "Cashier",
            email = "cashier",
            role = UserRole.CASHIER,
            assignedTillId = null
        ),
        Employee(
            id = 2,
            firstName = "Store",
            lastName = "Manager",
            email = "manager",
            role = UserRole.MANAGER,
            assignedTillId = 1  // Already has till
        )
    )
    
    private val testTills = listOf(
        Till(id = 1, name = "Till 1", assignedEmployeeId = 2, assignedEmployeeName = "Store Manager"),
        Till(id = 2, name = "Till 2", assignedEmployeeId = null, assignedEmployeeName = null)
    )
    
    private class FakeEmployeeRepository(
        private val employees: List<Employee>,
        private val pinValid: Boolean = true
    ) : EmployeeRepository {
        
        override suspend fun getEmployees(): Result<List<Employee>> {
            return Result.success(employees)
        }
        
        override suspend fun verifyPin(employeeId: Int, pin: String): Result<Employee> {
            val employee = employees.find { it.id == employeeId }
                ?: return Result.failure(IllegalArgumentException("Employee not found"))
            
            return if (pinValid && pin == "1234") {
                Result.success(employee)
            } else {
                Result.failure(IllegalArgumentException("Invalid PIN"))
            }
        }
        
        override suspend fun getApprovers(): Result<List<Employee>> {
            return Result.success(employees.filter { it.role == UserRole.MANAGER })
        }
    }
    
    private class FakeTillRepository(
        private val tills: List<Till>
    ) : TillRepository {
        
        private val mutableTills = tills.toMutableList()
        
        override suspend fun getTills(): Result<List<Till>> {
            return Result.success(mutableTills.toList())
        }
        
        override suspend fun getAvailableTills(): Result<List<Till>> {
            return Result.success(mutableTills.filter { it.isAvailable })
        }
        
        override suspend fun assignTill(tillId: Int, employeeId: Int, employeeName: String): Result<Unit> {
            val index = mutableTills.indexOfFirst { it.id == tillId }
            if (index == -1) return Result.failure(IllegalArgumentException("Till not found"))
            
            val till = mutableTills[index]
            if (!till.isAvailable) return Result.failure(IllegalStateException("Till already assigned"))
            
            mutableTills[index] = till.copy(assignedEmployeeId = employeeId, assignedEmployeeName = employeeName)
            return Result.success(Unit)
        }
        
        override suspend fun releaseTill(tillId: Int): Result<Unit> {
            val index = mutableTills.indexOfFirst { it.id == tillId }
            if (index == -1) return Result.failure(IllegalArgumentException("Till not found"))
            
            mutableTills[index] = mutableTills[index].copy(assignedEmployeeId = null, assignedEmployeeName = null)
            return Result.success(Unit)
        }
    }
    
    /**
     * Fake NFC scanner for testing.
     * Returns cancelled by default (no badge tap in tests).
     */
    private class FakeNfcScanner : NfcScanner {
        override suspend fun startScan(): NfcResult = NfcResult.Cancelled
        override fun cancelScan() { /* no-op */ }
    }
    
    // ========================================================================
    // Helper to create ViewModel with test scope
    // ========================================================================
    
    private fun createViewModel(
        employeeRepo: EmployeeRepository = FakeEmployeeRepository(testEmployees),
        tillRepo: TillRepository = FakeTillRepository(testTills),
        nfcScanner: NfcScanner = FakeNfcScanner(),
        testScope: TestScope
    ): LoginViewModel {
        return LoginViewModel(
            employeeRepository = employeeRepo,
            tillRepository = tillRepo,
            nfcScanner = nfcScanner,
            coroutineScope = testScope
        )
    }
    
    // ========================================================================
    // State Transition Tests
    // ========================================================================
    
    @Test
    fun `initial state should load employees and show EMPLOYEE_SELECT`() = runTest(
        UnconfinedTestDispatcher()
    ) {
        // Given
        val viewModel = createViewModel(testScope = this)
        advanceUntilIdle()
        
        // Then
        assertEquals(LoginStage.EMPLOYEE_SELECT, viewModel.state.value.stage)
        assertEquals(2, viewModel.state.value.employees.size)
    }
    
    @Test
    fun `onEmployeeSelected should transition to PIN_ENTRY`() = runTest(
        UnconfinedTestDispatcher()
    ) {
        // Given
        val viewModel = createViewModel(testScope = this)
        advanceUntilIdle()
        
        val employee = viewModel.state.value.employees.first()
        
        // When
        viewModel.onEmployeeSelected(employee)
        
        // Then
        assertEquals(LoginStage.PIN_ENTRY, viewModel.state.value.stage)
        assertNotNull(viewModel.state.value.selectedEmployee)
        assertEquals(employee.id, viewModel.state.value.selectedEmployee?.id)
    }
    
    @Test
    fun `onPinDigit should accumulate PIN digits`() = runTest(
        UnconfinedTestDispatcher()
    ) {
        // Given
        val viewModel = createViewModel(testScope = this)
        advanceUntilIdle()
        
        val employee = viewModel.state.value.employees.first()
        viewModel.onEmployeeSelected(employee)
        
        // When
        viewModel.onPinDigit("1")
        viewModel.onPinDigit("2")
        viewModel.onPinDigit("3")
        viewModel.onPinDigit("4")
        
        // Then
        assertEquals("1234", viewModel.state.value.pinInput)
        assertEquals("●●●●", viewModel.state.value.pinDots)
        assertTrue(viewModel.state.value.canSubmitPin)
    }
    
    @Test
    fun `onPinSubmit with employee who has till should go to SUCCESS`() = runTest(
        UnconfinedTestDispatcher()
    ) {
        // Given - Manager has assigned till
        val viewModel = createViewModel(testScope = this)
        advanceUntilIdle()
        
        // Select manager (who has assignedTillId = 1)
        val manager = viewModel.state.value.employees.find { it.role == "Manager" }!!
        viewModel.onEmployeeSelected(manager)
        
        // Enter PIN
        viewModel.onPinDigit("1")
        viewModel.onPinDigit("2")
        viewModel.onPinDigit("3")
        viewModel.onPinDigit("4")
        
        // When
        viewModel.onPinSubmit()
        advanceUntilIdle()
        
        // Then - Should go directly to SUCCESS (no till assignment needed)
        assertEquals(LoginStage.SUCCESS, viewModel.state.value.stage)
        assertNotNull(viewModel.state.value.authenticatedUser)
    }
    
    @Test
    fun `onPinSubmit with employee without till should go to TILL_ASSIGNMENT`() = runTest(
        UnconfinedTestDispatcher()
    ) {
        // Given - Cashier has no till
        val viewModel = createViewModel(testScope = this)
        advanceUntilIdle()
        
        // Select cashier (who has no assignedTillId)
        val cashier = viewModel.state.value.employees.find { it.role == "Cashier" }!!
        viewModel.onEmployeeSelected(cashier)
        
        // Enter PIN
        viewModel.onPinDigit("1")
        viewModel.onPinDigit("2")
        viewModel.onPinDigit("3")
        viewModel.onPinDigit("4")
        
        // When
        viewModel.onPinSubmit()
        advanceUntilIdle()
        
        // Then - Should go to TILL_ASSIGNMENT
        assertEquals(LoginStage.TILL_ASSIGNMENT, viewModel.state.value.stage)
        assertTrue(viewModel.state.value.tills.isNotEmpty())
    }
    
    @Test
    fun `onTillSelected should complete login`() = runTest(
        UnconfinedTestDispatcher()
    ) {
        // Given - Cashier needs till
        val viewModel = createViewModel(testScope = this)
        advanceUntilIdle()
        
        val cashier = viewModel.state.value.employees.find { it.role == "Cashier" }!!
        viewModel.onEmployeeSelected(cashier)
        viewModel.onPinDigit("1")
        viewModel.onPinDigit("2")
        viewModel.onPinDigit("3")
        viewModel.onPinDigit("4")
        viewModel.onPinSubmit()
        advanceUntilIdle()
        
        assertEquals(LoginStage.TILL_ASSIGNMENT, viewModel.state.value.stage)
        
        // Find available till
        val availableTill = viewModel.state.value.tills.find { it.isAvailable }!!
        
        // When
        viewModel.onTillSelected(availableTill.id)
        advanceUntilIdle()
        
        // Then
        assertEquals(LoginStage.SUCCESS, viewModel.state.value.stage)
        assertEquals(availableTill.id, viewModel.state.value.selectedTillId)
    }
    
    @Test
    fun `onBackPressed from PIN_ENTRY should return to EMPLOYEE_SELECT`() = runTest(
        UnconfinedTestDispatcher()
    ) {
        // Given
        val viewModel = createViewModel(testScope = this)
        advanceUntilIdle()
        
        val employee = viewModel.state.value.employees.first()
        viewModel.onEmployeeSelected(employee)
        assertEquals(LoginStage.PIN_ENTRY, viewModel.state.value.stage)
        
        // When
        viewModel.onBackPressed()
        
        // Then
        assertEquals(LoginStage.EMPLOYEE_SELECT, viewModel.state.value.stage)
        assertNull(viewModel.state.value.selectedEmployee)
    }
    
    @Test
    fun `invalid PIN should show error`() = runTest(
        UnconfinedTestDispatcher()
    ) {
        // Given
        val viewModel = createViewModel(
            employeeRepo = FakeEmployeeRepository(testEmployees, pinValid = false),
            testScope = this
        )
        advanceUntilIdle()
        
        val cashier = viewModel.state.value.employees.first()
        viewModel.onEmployeeSelected(cashier)
        viewModel.onPinDigit("9")
        viewModel.onPinDigit("9")
        viewModel.onPinDigit("9")
        viewModel.onPinDigit("9")
        
        // When
        viewModel.onPinSubmit()
        advanceUntilIdle()
        
        // Then - should stay in PIN_ENTRY with error
        assertEquals(LoginStage.PIN_ENTRY, viewModel.state.value.stage)
        assertNotNull(viewModel.state.value.errorMessage)
        assertEquals("", viewModel.state.value.pinInput) // PIN cleared after failure
    }
}
