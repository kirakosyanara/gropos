package com.unisight.gropos.features.transaction.data.api

import com.unisight.gropos.core.network.ApiClient
import com.unisight.gropos.features.transaction.data.dto.CreateTransactionRequest
import com.unisight.gropos.features.transaction.data.dto.CreateTransactionResponse
import com.unisight.gropos.features.transaction.data.dto.TransactionSyncResponse
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType

/**
 * Production implementation of TransactionApiService.
 * 
 * **Source of Truth:** docs/development-plan/TRANSACTION_API_SUBMISSION_IMPLEMENTATION_PLAN.md
 * 
 * **Per END_OF_TRANSACTION_API_SUBMISSION.md:**
 * - Base URL: APIM Gateway (apim-service-unisight-dev.azure-api.net)
 * - Endpoint: POST /transactions/create-transaction
 * - Header: version: {API_VERSION}
 * - Content-Type: application/json
 * 
 * **Zero-Trust:** 
 * - Does NOT log transaction payload (contains financial data)
 * - Only logs transaction GUID for tracing
 */
class DefaultTransactionApiService(
    private val apiClient: ApiClient
) : TransactionApiService {
    
    companion object {
        /**
         * API endpoint path.
         * Per END_OF_TRANSACTION_API_SUBMISSION.md: POST /transactions/create-transaction
         * NO version prefix in URL path - version is sent as header only.
         */
        private const val ENDPOINT = "/transactions/create-transaction"
        
        /** 
         * API version header value.
         * Per END_OF_TRANSACTION_API_SUBMISSION.md: "version: v1" (NOT "1.0")
         */
        private const val API_VERSION = "v1"
        
        /** Header name for API version */
        private const val VERSION_HEADER = "version"
    }
    
    /**
     * Submits a completed transaction to the backend.
     * 
     * **Flow:**
     * 1. Build POST request with JSON body
     * 2. Add required headers (version, Content-Type)
     * 3. Send via ApiClient (handles auth, retry)
     * 4. Parse response or error
     * 
     * **Error Handling:**
     * - 201: Success - returns CreateTransactionResponse
     * - 202: Accepted - may still have "Failure" status in body!
     * - 400: Validation error - permanent failure
     * - 401: Unauthorized - ApiClient handles token refresh
     * - 5xx: Server error - transient, caller should retry
     * 
     * **IMPORTANT:** Always check response.status field:
     * - "Success" = transaction created
     * - "Failure" = processing failed (even with HTTP 202)
     */
    override suspend fun createTransaction(
        request: CreateTransactionRequest
    ): Result<CreateTransactionResponse> {
        // Log GUID only (no sensitive data) for tracing
        println("[TransactionApiService] Submitting transaction: ${request.transaction.guid}")
        
        // Log payload structure for debugging (no amounts, just counts)
        println("[TransactionApiService] Payload: ${request.items.size} items, ${request.payments.size} payments")
        println("[TransactionApiService] Transaction fields: rowCount=${request.transaction.rowCount}, itemCount=${request.transaction.itemCount}")
        
        // Per END_OF_TRANSACTION_API_SUBMISSION.md: Use APIM gateway (baseUrl), not App Service
        val fullUrl = apiClient.config.baseUrl + ENDPOINT
        println("[TransactionApiService] POST to: $fullUrl")
        
        return try {
            val result = apiClient.request<CreateTransactionResponse> {
                method = HttpMethod.Post
                url(fullUrl)
                header(VERSION_HEADER, API_VERSION)
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            
            result.fold(
                onSuccess = { response ->
                    // Check the status field - HTTP 202 can still mean "Failure"
                    if (response.isSuccess) {
                        println("[TransactionApiService] Transaction synced successfully: ${response.transactionGuid}")
                        Result.success(response)
                    } else {
                        // Backend returned HTTP 2xx but status = "Failure"
                        val errorMsg = response.message ?: "Transaction processing failed on backend"
                        println("[TransactionApiService] Backend returned Failure: $errorMsg")
                        println("[TransactionApiService] Response status: ${response.status}, id: ${response.id}")
                        Result.failure(Exception("Backend processing failed: $errorMsg"))
                    }
                },
                onFailure = { error ->
                    // Log error type (no sensitive details)
                    println("[TransactionApiService] Failed to sync transaction: ${error::class.simpleName}")
                    println("[TransactionApiService] Error message: ${error.message}")
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            // Catch any unexpected exceptions
            println("[TransactionApiService] Exception during sync: ${e::class.simpleName}")
            println("[TransactionApiService] Exception message: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Syncs a pending transaction and returns a sync-specific response.
     * 
     * Wraps createTransaction with sync-focused response structure.
     */
    override suspend fun syncTransaction(
        request: CreateTransactionRequest
    ): Result<TransactionSyncResponse> {
        return createTransaction(request).fold(
            onSuccess = { response ->
                Result.success(
                    TransactionSyncResponse(
                        remoteId = response.id ?: 0,
                        transactionGuid = response.transactionGuid ?: request.transaction.guid,
                        success = response.isSuccess,
                        errorMessage = if (response.isFailure) response.message else null,
                        cashPickupNeeded = response.cashPickupNeeded
                    )
                )
            },
            onFailure = { error ->
                Result.success(
                    TransactionSyncResponse(
                        remoteId = 0,
                        transactionGuid = request.transaction.guid,
                        success = false,
                        errorMessage = error.message ?: "Unknown error",
                        cashPickupNeeded = false
                    )
                )
            }
        )
    }
}

