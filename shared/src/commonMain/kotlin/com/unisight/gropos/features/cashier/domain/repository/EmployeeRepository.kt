package com.unisight.gropos.features.cashier.domain.repository

import com.unisight.gropos.features.cashier.domain.model.Employee

/**
 * Repository interface for employee operations.
 * 
 * Per CASHIER_OPERATIONS.md:
 * - Fetches scheduled cashiers for this station
 * - Filters by active status
 * 
 * Per BEARER_TOKEN_MANAGEMENT.md:
 * - Login with full CashierLoginRequest to receive tokens
 * - verifyPin is for simple PIN validation (no tokens)
 * - login() is for full authentication with token generation
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
     * Verify an employee's PIN (simple validation).
     * NOTE: This does NOT generate tokens. Use login() for full authentication.
     * 
     * @param employeeId The employee attempting to authenticate
     * @param pin The PIN entered
     * @return Success with verified employee or failure
     */
    suspend fun verifyPin(employeeId: Int, pin: String): Result<Employee>
    
    /**
     * Full login with all required parameters.
     * Per BEARER_TOKEN_MANAGEMENT.md: This generates access and refresh tokens.
     * 
     * @param employeeId The employee ID
     * @param pin The PIN entered
     * @param tillId The assigned till/register account ID (locationAccountId)
     * @param branchId The current branch/store ID
     * @param deviceId The POS station/device ID
     * @return Success with LoginResult containing tokens, or failure
     */
    suspend fun login(
        employeeId: Int,
        pin: String,
        tillId: Int,
        branchId: Int,
        deviceId: Int
    ): Result<LoginResult>
    
    /**
     * Get employees who can approve a specific action.
     * Filters to managers/supervisors with approval permissions.
     * 
     * @return List of employees with manager/supervisor roles
     */
    suspend fun getApprovers(): Result<List<Employee>>
}

/**
 * Result of a successful login.
 * Per BEARER_TOKEN_MANAGEMENT.md (TokenViewModel - lines 407-415):
 * Contains accessToken for authenticated requests and refreshToken for persistence.
 */
data class LoginResult(
    val employee: Employee,
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long? = null
)

