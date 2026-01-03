package com.unisight.gropos.features.cashier.data

import com.unisight.gropos.core.network.ApiClient
import com.unisight.gropos.features.cashier.data.dto.EmployeeDto
import com.unisight.gropos.features.cashier.data.dto.EmployeeDtoMapper.toDomainList
import com.unisight.gropos.features.cashier.domain.model.Employee
import com.unisight.gropos.features.cashier.domain.repository.EmployeeRepository
import io.ktor.client.request.get

/**
 * Remote implementation of EmployeeRepository using backend API.
 * 
 * Per CASHIER_OPERATIONS.md Section "Fetching Cashier List":
 * - API: GET /employee/cashiers
 * - Auth: x-api-key header (device API key)
 * - Returns scheduled cashiers for this device/branch
 * 
 * This replaces FakeEmployeeRepository for production use.
 */
class RemoteEmployeeRepository(
    private val apiClient: ApiClient
) : EmployeeRepository {
    
    // Cache employees in memory for the session
    // They're reloaded on each app launch or when refresh is called
    private var cachedEmployees: List<Employee>? = null
    
    /**
     * Fetch list of scheduled cashiers from the backend.
     * 
     * Per CASHIER_OPERATIONS.md:
     * - Returns cashiers scheduled for this location
     * - Filtered by role (cashier/register permissions)
     * - Only active (not terminated) employees
     */
    override suspend fun getEmployees(): Result<List<Employee>> {
        // Return cached if available
        cachedEmployees?.let { 
            return Result.success(it) 
        }
        
        return try {
            val response = apiClient.authenticatedRequest<List<EmployeeDto>> {
                get(ENDPOINT_CASHIERS)
            }
            
            response.map { dtos ->
                val employees = dtos.toDomainList()
                cachedEmployees = employees
                println("[RemoteEmployeeRepository] Fetched ${employees.size} employees from backend")
                employees
            }
        } catch (e: Exception) {
            println("[RemoteEmployeeRepository] Failed to fetch employees: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Verify employee PIN.
     * 
     * Per CASHIER_OPERATIONS.md:
     * - API: POST /employee/gropos-login
     * - Validates PIN and returns employee profile + tokens
     * 
     * Note: This is a simplified implementation. In production,
     * this should call the gropos-login endpoint and return the
     * authenticated employee.
     */
    override suspend fun verifyPin(employeeId: Int, pin: String): Result<Employee> {
        // For now, find employee from cache and verify locally
        // In production, this would call POST /employee/gropos-login
        val employee = cachedEmployees?.find { it.id == employeeId }
            ?: return Result.failure(IllegalArgumentException("Employee not found"))
        
        // TODO: Replace with actual API call to /employee/gropos-login
        // For now, accept PIN "1234" or badge token matching employee ID
        val isValidPin = pin == TEMP_VALID_PIN
        val isBadgeToken = pin == employeeId.toString()
        
        if (!isValidPin && !isBadgeToken) {
            return Result.failure(IllegalArgumentException("Invalid PIN"))
        }
        
        return Result.success(employee)
    }
    
    /**
     * Get list of approvers (managers/supervisors).
     * 
     * Per ROLES_AND_PERMISSIONS.md:
     * - API: GET /employee/with-permission?permission=APPROVE_DISCOUNT
     * 
     * For now, filter cached employees by role.
     */
    override suspend fun getApprovers(): Result<List<Employee>> {
        // Ensure employees are loaded
        if (cachedEmployees == null) {
            val loadResult = getEmployees()
            if (loadResult.isFailure) {
                return loadResult
            }
        }
        
        val approvers = cachedEmployees?.filter { 
            it.role == com.unisight.gropos.features.auth.domain.model.UserRole.MANAGER ||
            it.role == com.unisight.gropos.features.auth.domain.model.UserRole.SUPERVISOR ||
            it.role == com.unisight.gropos.features.auth.domain.model.UserRole.ADMIN
        } ?: emptyList()
        
        return Result.success(approvers)
    }
    
    /**
     * Force refresh of employee list from backend.
     * Called when data might be stale.
     */
    fun clearCache() {
        cachedEmployees = null
    }
    
    companion object {
        private const val ENDPOINT_CASHIERS = "/employee/cashiers"
        // Temporary PIN for development - TODO: Remove when API is fully integrated
        private const val TEMP_VALID_PIN = "1234"
    }
}

