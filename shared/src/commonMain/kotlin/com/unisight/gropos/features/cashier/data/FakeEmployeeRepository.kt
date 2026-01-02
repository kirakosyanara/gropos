package com.unisight.gropos.features.cashier.data

import com.unisight.gropos.features.auth.domain.model.UserRole
import com.unisight.gropos.features.cashier.domain.model.Employee
import com.unisight.gropos.features.cashier.domain.repository.EmployeeRepository
import kotlinx.coroutines.delay

/**
 * Fake implementation of EmployeeRepository for development and testing.
 * 
 * Provides a list of scheduled employees for the login screen.
 * Per testing-strategy.mdc: "Use Fakes for State"
 */
class FakeEmployeeRepository : EmployeeRepository {
    
    /**
     * Fake employee database matching our FakeAuthRepository credentials
     */
    private val employees = listOf(
        Employee(
            id = 1,
            firstName = "Admin",
            lastName = "User",
            email = "admin",
            role = UserRole.ADMIN,
            imageUrl = null,
            assignedTillId = null
        ),
        Employee(
            id = 9999,
            firstName = "Store",
            lastName = "Manager",
            email = "manager",
            role = UserRole.MANAGER,
            imageUrl = null,
            assignedTillId = null
        ),
        Employee(
            id = 9998,
            firstName = "Sarah",
            lastName = "Supervisor",
            email = "supervisor",
            role = UserRole.SUPERVISOR,
            imageUrl = null,
            assignedTillId = null
        ),
        Employee(
            id = 3,
            firstName = "Jane",
            lastName = "Cashier",
            email = "cashier",
            role = UserRole.CASHIER,
            imageUrl = null,
            assignedTillId = null
        )
    )
    
    override suspend fun getEmployees(): Result<List<Employee>> {
        delay(SIMULATED_DELAY_MS)
        return Result.success(employees)
    }
    
    override suspend fun verifyPin(employeeId: Int, pin: String): Result<Employee> {
        delay(SIMULATED_DELAY_MS)
        
        val employee = employees.find { it.id == employeeId }
            ?: return Result.failure(IllegalArgumentException("Employee not found"))
        
        // All employees use PIN "1234" for walking skeleton
        if (pin != VALID_PIN) {
            return Result.failure(IllegalArgumentException("Invalid PIN"))
        }
        
        return Result.success(employee)
    }
    
    override suspend fun getApprovers(): Result<List<Employee>> {
        delay(SIMULATED_DELAY_MS)
        // Return managers and supervisors
        return Result.success(employees.filter { 
            it.role == UserRole.MANAGER || it.role == UserRole.SUPERVISOR 
        })
    }
    
    companion object {
        private const val SIMULATED_DELAY_MS = 200L
        private const val VALID_PIN = "1234"
    }
}

