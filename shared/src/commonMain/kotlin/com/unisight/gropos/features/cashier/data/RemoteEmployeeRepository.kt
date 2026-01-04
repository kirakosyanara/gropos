package com.unisight.gropos.features.cashier.data

import com.unisight.gropos.core.auth.TokenStorage
import com.unisight.gropos.core.network.ApiClient
import com.unisight.gropos.core.network.ApiException
import com.unisight.gropos.core.storage.SecureStorage
import com.unisight.gropos.features.cashier.data.dto.EmployeeListResponse
import com.unisight.gropos.features.cashier.data.dto.EmployeeDtoMapper.toDomainList
import com.unisight.gropos.features.cashier.domain.model.Employee
import com.unisight.gropos.features.cashier.domain.repository.EmployeeRepository
import com.unisight.gropos.features.cashier.domain.repository.LoginResult
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
 * **Per END_OF_TRANSACTION_API_SUBMISSION.md:**
 * - On login success, must save access token for transaction submission
 * - Bearer token required for employee-level operations
 * 
 * This replaces FakeEmployeeRepository for production use.
 */
class RemoteEmployeeRepository(
    private val apiClient: ApiClient,
    private val tokenStorage: TokenStorage,
    private val secureStorage: SecureStorage
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
        
        println("[RemoteEmployeeRepository] Calling Login API for PIN verification: $userName")
        
        // Get branchId from device registration (not hardcoded)
        val branchId = secureStorage.getBranchId() ?: DEFAULT_BRANCH_ID
        println("[RemoteEmployeeRepository] Using branchId: $branchId (from SecureStorage)")
        
        // Call the login API with a simple request for PIN verification only
        // NOTE: This does NOT issue tokens - use login() for full authentication
        // Per BEARER_TOKEN_MANAGEMENT.md: Full login with tokens happens after till selection
        val fullUrl = apiClient.config.posApiBaseUrl + ENDPOINT_LOGIN
        println("[RemoteEmployeeRepository] Login URL: $fullUrl")
        
        return try {
            // Use a simple request for PIN verification only
            val verifyRequest = SimplePinVerifyRequest(
                userName = userName,
                password = pin,
                branchId = branchId,
                clientName = "device"
            )
            
            println("[RemoteEmployeeRepository] Sending PIN verify request: userName=$userName, branchId=$branchId")
            
            val response = apiClient.authenticatedRequest<LoginResponseDto> {
                method = io.ktor.http.HttpMethod.Post
                url(fullUrl)
                setBody(verifyRequest)
            }
            
            response.fold(
                onSuccess = { loginResponse ->
                    println("[RemoteEmployeeRepository] PIN verification SUCCESS for $userName")
                    println("[RemoteEmployeeRepository] Response: success=${loginResponse.success}, message=${loginResponse.message}")
                    // NOTE: Tokens from verifyPin are NOT stored
                    // Full token generation happens in login() after till selection
                    // Return the employee from cache (API validated the PIN)
                    Result.success(employee)
                },
                onFailure = { error ->
                    println("[RemoteEmployeeRepository] PIN verification FAILED: ${error.message}")
                    println("[RemoteEmployeeRepository] Error type: ${error::class.simpleName}")
                    
                    // Extract actual error message from API response if available
                    val errorMessage = when (error) {
                        is ApiException.HttpError -> {
                            println("[RemoteEmployeeRepository] HTTP Error ${error.statusCode}: ${error.body}")
                            // Parse the actual message from the backend error response
                            val parsedMessage = extractMessageFromErrorBody(error.body)
                            when (error.statusCode) {
                                401 -> parsedMessage ?: "Invalid PIN"
                                409 -> parsedMessage ?: "Session conflict. Please try again."
                                else -> parsedMessage ?: "Login failed (${error.statusCode})"
                            }
                        }
                        else -> error.message ?: "Invalid PIN"
                    }
                    
                    Result.failure(IllegalArgumentException(errorMessage))
                }
            )
        } catch (e: Exception) {
            println("[RemoteEmployeeRepository] PIN verification EXCEPTION: ${e.message}")
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
     * Full login with all required parameters per BEARER_TOKEN_MANAGEMENT.md.
     * 
     * Per BEARER_TOKEN_MANAGEMENT.md (lines 273-323):
     * 1. Build CashierLoginRequest with ALL fields including tillId
     * 2. Call login API
     * 3. Store refresh token persistently
     * 4. Set bearer token for authenticated requests
     * 
     * @return LoginResult with tokens on success
     */
    override suspend fun login(
        employeeId: Int,
        pin: String,
        tillId: Int,
        branchId: Int,
        deviceId: Int
    ): Result<LoginResult> {
        println("[RemoteEmployeeRepository] Full login for employeeId: $employeeId with tillId: $tillId")
        
        // Find employee from cache to get their username/email
        val employee = cachedEmployees?.find { it.id == employeeId }
            ?: return Result.failure(IllegalArgumentException("Employee not found"))
        
        val userName = employee.email
        if (userName.isNullOrBlank()) {
            println("[RemoteEmployeeRepository] ERROR: Employee has no email/username")
            return Result.failure(IllegalArgumentException("Employee has no username configured"))
        }
        
        println("[RemoteEmployeeRepository] Calling Login API with full CashierLoginRequest for: $userName")
        
        // Build full CashierLoginRequest per BEARER_TOKEN_MANAGEMENT.md (lines 282-290)
        val loginRequest = CashierLoginRequest(
            userName = userName,
            password = pin,
            clientName = "device",
            authenticationKey = null,
            locationAccountId = tillId,  // Till ID - critical for token generation
            branchId = branchId,
            deviceId = deviceId  // Station ID - critical for token generation
        )
        
        val fullUrl = apiClient.config.posApiBaseUrl + ENDPOINT_LOGIN
        println("[RemoteEmployeeRepository] Login URL: $fullUrl")
        
        return try {
            val response = apiClient.authenticatedRequest<LoginResponseDto> {
                method = io.ktor.http.HttpMethod.Post
                url(fullUrl)
                setBody(loginRequest)
            }
            
            response.fold(
                onSuccess = { loginResponse ->
                    println("[RemoteEmployeeRepository] Full login SUCCESS for $userName")
                    
                    val accessToken = loginResponse.accessToken
                    val refreshToken = loginResponse.refreshToken
                    
                    if (accessToken.isNullOrBlank()) {
                        println("[RemoteEmployeeRepository] WARNING: No access token in login response")
                        return Result.failure(IllegalStateException("No access token returned from login API"))
                    }
                    
                    // Per BEARER_TOKEN_MANAGEMENT.md: Store tokens
                    tokenStorage.saveAccessToken(accessToken)
                    println("[RemoteEmployeeRepository] Access token saved for authenticated requests")
                    
                    refreshToken?.let {
                        tokenStorage.saveRefreshToken(it)
                        println("[RemoteEmployeeRepository] Refresh token saved")
                    }
                    
                    Result.success(
                        LoginResult(
                            employee = employee,
                            accessToken = accessToken,
                            refreshToken = refreshToken ?: "",
                            expiresIn = loginResponse.expiresIn?.toLong()
                        )
                    )
                },
                onFailure = { error ->
                    println("[RemoteEmployeeRepository] Full login FAILED: ${error.message}")
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            println("[RemoteEmployeeRepository] Full login EXCEPTION: ${e.message}")
            e.printStackTrace()
            Result.failure(IllegalArgumentException("Login failed: ${e.message}"))
        }
    }
    
    /**
     * Force refresh of employee list from backend.
     * Called when data might be stale.
     */
    fun clearCache() {
        cachedEmployees = null
    }
    
    /**
     * Extracts the human-readable error message from the backend error response.
     * 
     * Backend error format:
     * {
     *   "Success": null,
     *   "Message": {
     *     "Message": "Employee: Patsy Heaney, already has location account...",
     *     "StatusCode": 409,
     *     ...
     *   }
     * }
     */
    private fun extractMessageFromErrorBody(body: String): String? {
        return try {
            // Simple regex extraction for "Message":"..."
            val messageRegex = """"Message"\s*:\s*"([^"]+)"""".toRegex()
            val match = messageRegex.find(body)
            match?.groupValues?.getOrNull(1)
        } catch (e: Exception) {
            println("[RemoteEmployeeRepository] Failed to parse error body: ${e.message}")
            null
        }
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
 * Per BEARER_TOKEN_MANAGEMENT.md (lines 282-290): Full CashierLoginRequest format
 * Per pos.json API spec: POST /api/Employee/Login
 * 
 * **All fields are required for proper token generation!**
 */
@kotlinx.serialization.Serializable
data class CashierLoginRequest(
    val userName: String,
    val password: String,
    val clientName: String = "device",
    val authenticationKey: String? = null,
    val locationAccountId: Int,  // Till/Register ID - REQUIRED for token generation
    val branchId: Int,
    val deviceId: Int  // Station ID - REQUIRED for token generation
)

/**
 * Simplified PIN verification request.
 * Used for verifyPin() before till selection.
 * NOTE: This request does NOT include tillId/deviceId, so tokens may not be issued.
 * For full authentication with tokens, use CashierLoginRequest via login().
 */
@kotlinx.serialization.Serializable
data class SimplePinVerifyRequest(
    val userName: String,
    val password: String,
    val branchId: Int,
    val clientName: String = "device"
)

/**
 * Login response wrapper DTO.
 * 
 * The API returns a nested structure:
 * {
 *   "success": { "access_token": "...", "refresh_token": "...", "expires_in": 3600, ... }
 * }
 * 
 * On error, it returns:
 * {
 *   "message": { "Message": "Error description", ... }
 * }
 */
@kotlinx.serialization.Serializable
data class LoginResponseDto(
    @kotlinx.serialization.SerialName("success")
    val success: LoginSuccessDto? = null,
    @kotlinx.serialization.SerialName("message")
    val message: LoginMessageDto? = null
) {
    // Convenience accessors for backward compatibility
    val accessToken: String? get() = success?.accessToken
    val refreshToken: String? get() = success?.refreshToken
    val expiresIn: Int? get() = success?.expiresIn
}

/**
 * Success payload containing tokens.
 */
@kotlinx.serialization.Serializable
data class LoginSuccessDto(
    @kotlinx.serialization.SerialName("access_token")
    val accessToken: String? = null,
    @kotlinx.serialization.SerialName("refresh_token")
    val refreshToken: String? = null,
    @kotlinx.serialization.SerialName("expires_in")
    val expiresIn: Int? = null,
    @kotlinx.serialization.SerialName("token_type")
    val tokenType: String? = null
)

/**
 * Error/Message payload.
 */
@kotlinx.serialization.Serializable
data class LoginMessageDto(
    @kotlinx.serialization.SerialName("Message")
    val message: String? = null,
    @kotlinx.serialization.SerialName("StatusCode")
    val statusCode: Int? = null,
    @kotlinx.serialization.SerialName("StatusMessage")
    val statusMessage: String? = null
)

