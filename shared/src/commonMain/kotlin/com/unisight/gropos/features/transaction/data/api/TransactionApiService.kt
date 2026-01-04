package com.unisight.gropos.features.transaction.data.api

import com.unisight.gropos.features.transaction.data.dto.CreateTransactionRequest
import com.unisight.gropos.features.transaction.data.dto.CreateTransactionResponse
import com.unisight.gropos.features.transaction.data.dto.TransactionSyncResponse

/**
 * Service interface for transaction-related API operations.
 * 
 * **Source of Truth:** docs/development-plan/TRANSACTION_API_SUBMISSION_IMPLEMENTATION_PLAN.md
 * **Backend Endpoint:** POST /api/v1/transactions/create-transaction
 * 
 * **Per END_OF_TRANSACTION_API_SUBMISSION.md:**
 * - Submits completed transactions to backend
 * - Handles success (201), accepted (202), and error responses
 * - Supports token refresh on 401
 * 
 * **Zero-Trust:** Implementations must NOT log PII or transaction details.
 */
interface TransactionApiService {
    
    /**
     * Submits a completed transaction to the backend.
     * 
     * **Per reliability-stability.mdc:**
     * - Returns Result to handle success/failure
     * - Caller should retry on transient errors (5xx, timeout)
     * - Caller should NOT retry on permanent errors (400, validation)
     * 
     * @param request The transaction data to submit
     * @return Result containing response on success, or error on failure
     */
    suspend fun createTransaction(request: CreateTransactionRequest): Result<CreateTransactionResponse>
    
    /**
     * Syncs a pending transaction to the backend.
     * 
     * Convenience method that creates request from transaction data
     * and returns a sync-specific response.
     * 
     * @param request The transaction data to submit
     * @return Result containing sync response
     */
    suspend fun syncTransaction(request: CreateTransactionRequest): Result<TransactionSyncResponse>
}

