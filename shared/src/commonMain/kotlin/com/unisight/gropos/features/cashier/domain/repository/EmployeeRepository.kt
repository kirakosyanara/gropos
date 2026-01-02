package com.unisight.gropos.features.cashier.domain.repository

import com.unisight.gropos.features.cashier.domain.model.Employee

/**
 * Repository interface for employee operations.
 * 
 * Per CASHIER_OPERATIONS.md:
 * - Fetches scheduled cashiers for this station
 * - Filters by active status
 */
interface EmployeeRepository {
    
    /**
     * Get all active employees scheduled for this station.
     * These are the employees shown on the login screen.
     * 
     * @return List of active, scheduled employees
     */
    suspend fun getEmployees(): Result<List<Employee>>
    
    /**
     * Verify an employee's PIN.
     * 
     * @param employeeId The employee attempting to authenticate
     * @param pin The PIN entered
     * @return Success with verified employee or failure
     */
    suspend fun verifyPin(employeeId: Int, pin: String): Result<Employee>
    
    /**
     * Get employees who can approve a specific action.
     * Filters to managers/supervisors with approval permissions.
     * 
     * @return List of employees with manager/supervisor roles
     */
    suspend fun getApprovers(): Result<List<Employee>>
}

