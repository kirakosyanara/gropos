package com.unisight.gropos.features.cashier.data

import com.unisight.gropos.core.network.ApiClient
import com.unisight.gropos.features.cashier.data.dto.EmployeeListResponse
import com.unisight.gropos.features.cashier.data.dto.EmployeeDtoMapper.toDomainList
import com.unisight.gropos.features.cashier.domain.model.Employee
import com.unisight.gropos.features.cashier.domain.repository.EmployeeRepository
import io.ktor.client.request.get
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.path

/**
 * Remote implementation of EmployeeRepository using backend API.
 * 
 * **Per CASHIER_OPERATIONS.md Section "Fetching Cashier List":**
 * - API: GET /employee/cashiers
 * - Auth: x-api-key header (device API key)
 * - Returns scheduled cashiers for this device/branch
 * 
 * **Per API.md Authentication Section:**
 * - x-api-key: Device API key from registration
 * - version: v1 header required
 * - Headers are added automatically by ApiClient.request()
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
     * **Per CASHIER_OPERATIONS.md:**
     * - Returns cashiers scheduled for this location
     * - Filtered by role (cashier/register permissions)
     * - Only active (not terminated) employees
     * 
     * **Per API.md:**
     * - Endpoint: GET /employee/cashiers
     * - Headers: x-api-key, version: v1 (added by ApiClient)
     */
    override suspend fun getEmployees(): Result<List<Employee>> {
        // Return cached if available
        cachedEmployees?.let { 
            println("[RemoteEmployeeRepository] Returning ${it.size} cached employees")
            return Result.success(it) 
        }
        
        println("[RemoteEmployeeRepository] Fetching employees from backend...")
        println("[RemoteEmployeeRepository] Endpoint: $ENDPOINT_CASHIERS")
        
        return try {
            // Use authenticatedRequest for POS API endpoints
            // CRITICAL: Set the full POS API URL directly (App Service, not APIM)
            val fullUrl = apiClient.config.posApiBaseUrl + ENDPOINT_CASHIERS
            println("[RemoteEmployeeRepository] Full URL: $fullUrl")
            val response = apiClient.authenticatedRequest<EmployeeListResponse> {
                method = io.ktor.http.HttpMethod.Get
                url(fullUrl)
            }
            
            response.fold(
                onSuccess = { wrapper ->
                    val employees = wrapper.success.toDomainList()
                    cachedEmployees = employees
                    println("[RemoteEmployeeRepository] SUCCESS: Fetched ${employees.size} employees from backend")
                    Result.success(employees)
                },
                onFailure = { error ->
                    println("[RemoteEmployeeRepository] API ERROR: ${error.message}")
                    println("[RemoteEmployeeRepository] Error type: ${error::class.simpleName}")
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            println("[RemoteEmployeeRepository] EXCEPTION: ${e.message}")
            println("[RemoteEmployeeRepository] Exception type: ${e::class.simpleName}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    /**
     * Verify employee PIN by calling the Login API.
     * 
     * Per pos.json API spec:
     * - API: POST /api/Employee/Login
     * - Request: { userName, password, branchId, ... }
     * - Validates PIN and returns employee profile + tokens
     */
    override suspend fun verifyPin(employeeId: Int, pin: String): Result<Employee> {
        println("[RemoteEmployeeRepository] Verifying PIN for employeeId: $employeeId")
        
        // Find employee from cache to get their username/email
        val employee = cachedEmployees?.find { it.id == employeeId }
            ?: return Result.failure(IllegalArgumentException("Employee not found"))
        
        val userName = employee.email
        if (userName.isNullOrBlank()) {
            println("[RemoteEmployeeRepository] ERROR: Employee has no email/username")
            return Result.failure(IllegalArgumentException("Employee has no username configured"))
        }
        
        println("[RemoteEmployeeRepository] Calling Login API for: $userName")
        
        // Call the actual login API
        val fullUrl = apiClient.config.posApiBaseUrl + ENDPOINT_LOGIN
        println("[RemoteEmployeeRepository] Login URL: $fullUrl")
        
        return try {
            val response = apiClient.authenticatedRequest<LoginResponseDto> {
                method = io.ktor.http.HttpMethod.Post
                url(fullUrl)
                setBody(PinLoginRequest(
                    userName = userName,
                    password = pin,
                    branchId = DEFAULT_BRANCH_ID
                ))
            }
            
            response.fold(
                onSuccess = { loginResponse ->
                    println("[RemoteEmployeeRepository] Login SUCCESS for $userName")
                    // Return the employee from cache (API already validated)
                    Result.success(employee)
                },
                onFailure = { error ->
                    println("[RemoteEmployeeRepository] Login FAILED: ${error.message}")
                    Result.failure(IllegalArgumentException("Invalid PIN"))
                }
            )
        } catch (e: Exception) {
            println("[RemoteEmployeeRepository] Login EXCEPTION: ${e.message}")
            e.printStackTrace()
            Result.failure(IllegalArgumentException("Login failed: ${e.message}"))
        }
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
        /**
         * Per LOCK_SCREEN_AND_CASHIER_LOGIN.md:
         * GET /api/Employee/GetCashierEmployees
         * Returns list of scheduled cashiers for this location
         */
        private const val ENDPOINT_CASHIERS = "/api/Employee/GetCashierEmployees"
        
        /**
         * Per pos.json API spec:
         * POST /api/Employee/Login
         * Authenticates employee with PIN
         */
        private const val ENDPOINT_LOGIN = "/api/Employee/Login"
        
        /**
         * Default branch ID - should come from device registration
         */
        private const val DEFAULT_BRANCH_ID = 2
    }
}

/**
 * Login request DTO for PIN verification.
 * Per pos.json API spec: POST /api/Employee/Login
 */
@kotlinx.serialization.Serializable
data class PinLoginRequest(
    val userName: String,
    val password: String,
    val branchId: Int,
    val clientName: String = "device"
)

/**
 * Login response DTO.
 * The API returns tokens on successful login.
 */
@kotlinx.serialization.Serializable
data class LoginResponseDto(
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val expiresIn: Int? = null,
    val success: Boolean? = null,
    val message: String? = null
)

