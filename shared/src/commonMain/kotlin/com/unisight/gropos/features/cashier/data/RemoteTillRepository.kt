package com.unisight.gropos.features.cashier.data

import com.unisight.gropos.core.network.ApiClient
import com.unisight.gropos.features.cashier.data.dto.TillAssignRequest
import com.unisight.gropos.features.cashier.data.dto.TillDomainMapper.toDomainList
import com.unisight.gropos.features.cashier.data.dto.TillListResponse
import com.unisight.gropos.features.cashier.data.dto.TillOperationResponse
import com.unisight.gropos.features.cashier.domain.model.Till
import com.unisight.gropos.features.cashier.domain.repository.TillRepository
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody

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
        private const val ENDPOINT_TILLS = "/till"
        private const val ENDPOINT_ASSIGN = "/till/{tillId}/assign"
        private const val ENDPOINT_RELEASE = "/till/{tillId}/release"
    }
    
    /**
     * Fetches all tills for the current branch/station.
     * 
     * **API:** GET /till
     * **Response:** TillListResponse with list of TillDto
     */
    override suspend fun getTills(): Result<List<Till>> {
        return apiClient.authenticatedRequest<TillListResponse> {
            get(ENDPOINT_TILLS)
        }.map { response ->
            response.tills.toDomainList()
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
        
        return apiClient.authenticatedRequest<TillOperationResponse> {
            post(endpoint) {
                setBody(TillAssignRequest(employeeId, employeeName))
            }
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
        
        return apiClient.authenticatedRequest<TillOperationResponse> {
            post(endpoint)
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

