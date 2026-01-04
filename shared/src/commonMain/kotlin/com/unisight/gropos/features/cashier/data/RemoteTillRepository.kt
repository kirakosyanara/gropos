package com.unisight.gropos.features.cashier.data

import com.unisight.gropos.core.network.ApiClient
import com.unisight.gropos.features.cashier.data.dto.GridDataOfLocationAccountListViewModel
import com.unisight.gropos.features.cashier.data.dto.TillAssignRequest
import com.unisight.gropos.features.cashier.data.dto.TillDomainMapper.toDomainList
import com.unisight.gropos.features.cashier.data.dto.TillDomainMapper.getTotalRows
import com.unisight.gropos.features.cashier.data.dto.TillOperationResponse
import com.unisight.gropos.features.cashier.domain.model.Till
import com.unisight.gropos.features.cashier.domain.repository.TillRepository
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url

/**
 * Remote implementation of TillRepository using REST API.
 * 
 * **Per API_INTEGRATION.md:**
 * - Uses authenticatedClient for Bearer token auth
 * - Integrates with TokenRefreshManager for automatic 401 handling
 * 
 * **API Endpoints:**
 * - GET /till - Get all tills for current branch
 * - POST /till/{tillId}/assign - Assign till to employee
 * - POST /till/{tillId}/release - Release till assignment
 * 
 * **Per project-structure.mdc:**
 * - Repository implementation in Data layer
 * - Returns domain models (Till), not DTOs
 * 
 * **Per reliability-stability.mdc:**
 * - All operations return Result<T>, never throw
 * - Network errors are captured and wrapped
 */
class RemoteTillRepository(
    private val apiClient: ApiClient
) : TillRepository {
    
    companion object {
        /**
         * Per LOCK_SCREEN_AND_CASHIER_LOGIN.md:
         * GET /api/account/GetTillAccountList - Get available tills
         */
        private const val ENDPOINT_TILLS = "/api/account/GetTillAccountList"
        private const val ENDPOINT_ASSIGN = "/till/{tillId}/assign"
        private const val ENDPOINT_RELEASE = "/till/{tillId}/release"
    }
    
    /**
     * Fetches all tills for the current branch/station.
     * 
     * **Per LOCK_SCREEN_AND_CASHIER_LOGIN.md:**
     * **API:** GET /api/account/GetTillAccountList
     * **Response:** GridDataOfLocationAccountListViewModel with list of LocationAccountDto
     */
    override suspend fun getTills(): Result<List<Till>> {
        // Use authenticatedRequest for POS API endpoints
        val fullUrl = apiClient.config.posApiBaseUrl + ENDPOINT_TILLS
        println("[RemoteTillRepository] Fetching tills from: $fullUrl")
        
        return try {
            val result = apiClient.authenticatedRequest<GridDataOfLocationAccountListViewModel> {
                method = io.ktor.http.HttpMethod.Get
                url(fullUrl)
            }
            
            result.fold(
                onSuccess = { response ->
                    val tills = response.toDomainList()
                    println("[RemoteTillRepository] SUCCESS: Fetched ${tills.size} tills (totalRows: ${response.getTotalRows()})")
                    tills.forEach { till ->
                        println("[RemoteTillRepository]   Till: id=${till.id}, name=${till.name}, assigned=${till.assignedEmployeeName}")
                    }
                    Result.success(tills)
                },
                onFailure = { error ->
                    println("[RemoteTillRepository] API ERROR: ${error.message}")
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            println("[RemoteTillRepository] EXCEPTION: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    /**
     * Fetches only available (unassigned) tills.
     * 
     * Filters the full list client-side for simplicity.
     * Could be optimized with a dedicated API endpoint if needed.
     */
    override suspend fun getAvailableTills(): Result<List<Till>> {
        return getTills().map { tills ->
            tills.filter { it.isAvailable }
        }
    }
    
    /**
     * Assigns a till to an employee.
     * 
     * **API:** POST /till/{tillId}/assign
     * **Request Body:** TillAssignRequest
     * **Response:** TillOperationResponse
     * 
     * @param tillId The ID of the till to assign
     * @param employeeId The employee ID to assign the till to
     * @param employeeName Display name of the employee
     * @return Success or failure with error message
     */
    override suspend fun assignTill(tillId: Int, employeeId: Int, employeeName: String): Result<Unit> {
        val endpoint = ENDPOINT_ASSIGN.replace("{tillId}", tillId.toString())
        val fullUrl = apiClient.config.posApiBaseUrl + endpoint
        
        return apiClient.authenticatedRequest<TillOperationResponse> {
            method = io.ktor.http.HttpMethod.Post
            url(fullUrl)
            setBody(TillAssignRequest(employeeId, employeeName))
        }.mapCatching { response ->
            if (!response.success) {
                throw TillOperationException(
                    response.message ?: "Failed to assign till"
                )
            }
        }
    }
    
    /**
     * Releases a till from its current assignment.
     * 
     * **API:** POST /till/{tillId}/release
     * **Response:** TillOperationResponse
     * 
     * @param tillId The ID of the till to release
     * @return Success or failure with error message
     */
    override suspend fun releaseTill(tillId: Int): Result<Unit> {
        val endpoint = ENDPOINT_RELEASE.replace("{tillId}", tillId.toString())
        val fullUrl = apiClient.config.posApiBaseUrl + endpoint
        
        return apiClient.authenticatedRequest<TillOperationResponse> {
            method = io.ktor.http.HttpMethod.Post
            url(fullUrl)
        }.mapCatching { response ->
            if (!response.success) {
                throw TillOperationException(
                    response.message ?: "Failed to release till"
                )
            }
        }
    }
}

/**
 * Exception for till operation failures.
 */
class TillOperationException(message: String) : Exception(message)

