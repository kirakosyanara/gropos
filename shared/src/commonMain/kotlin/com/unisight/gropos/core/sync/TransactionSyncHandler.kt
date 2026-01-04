package com.unisight.gropos.core.sync

import com.unisight.gropos.features.transaction.data.api.TransactionApiService
import com.unisight.gropos.features.transaction.data.dto.CreateTransactionRequest
import com.unisight.gropos.features.transaction.domain.repository.TransactionRepository
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Handles syncing transaction items from the offline queue to the backend.
 * 
 * **Source of Truth:** docs/development-plan/TRANSACTION_API_SUBMISSION_IMPLEMENTATION_PLAN.md
 * 
 * **Per END_OF_TRANSACTION_API_SUBMISSION.md:**
 * - Deserializes queued transaction payload
 * - Calls TransactionApiService
 * - Returns appropriate ProcessResult
 * - Handles error classification for retry/abandon decisions
 * 
 * **Error Handling Strategy:**
 * - 400 Bad Request: Abandon (permanent failure, validation error)
 * - 401 Unauthorized: Retry (token expired, will be refreshed)
 * - 5xx Server Error: Retry (transient failure)
 * - Network Error: Retry (connectivity issue)
 * - Serialization Error: Abandon (malformed payload)
 * 
 * **Zero-Trust:** 
 * - Does NOT log transaction payload or amounts
 * - Only logs transaction GUID and error types
 */
class TransactionSyncHandler(
    private val transactionApiService: TransactionApiService,
    private val transactionRepository: TransactionRepository,
    private val json: Json
) : QueueItemSyncHandler {
    
    /**
     * Syncs a queued item to the backend.
     * 
     * Routes to appropriate handler based on item type.
     */
    override suspend fun sync(item: QueuedItem): ProcessResult {
        return when (item.type) {
            QueueItemType.TRANSACTION -> syncTransaction(item)
            QueueItemType.RETURN -> syncReturn(item)
            QueueItemType.ADJUSTMENT -> syncAdjustment(item)
            QueueItemType.CLOCK_EVENT -> syncClockEvent(item)
            QueueItemType.APPROVAL_AUDIT -> syncApprovalAudit(item)
        }
    }
    
    // ========================================================================
    // Transaction Sync
    // ========================================================================
    
    /**
     * Syncs a transaction to the backend.
     * 
     * **Flow:**
     * 1. Deserialize payload to CreateTransactionRequest
     * 2. Call TransactionApiService.createTransaction()
     * 3. On success: Mark local transaction as synced, return Success
     * 4. On failure: Classify error, return Retry or Abandon
     */
    private suspend fun syncTransaction(item: QueuedItem): ProcessResult {
        val transactionGuid: String
        
        return try {
            // Step 1: Deserialize payload
            val request = json.decodeFromString<CreateTransactionRequest>(item.payload)
            transactionGuid = request.transaction.guid
            
            // Log attempt (GUID only, no sensitive data)
            println("[TransactionSyncHandler] Syncing transaction: $transactionGuid (attempt ${item.attempts + 1})")
            
            // Step 2: Make API call
            val result = transactionApiService.createTransaction(request)
            
            result.fold(
                onSuccess = { response ->
                    // Step 3a: Success - mark as synced
                    markTransactionAsSynced(transactionGuid, response.id)
                    
                    // Check if cash pickup is needed
                    if (response.cashPickupNeeded) {
                        println("[TransactionSyncHandler] ALERT: Cash pickup needed for transaction $transactionGuid")
                        // TODO: Trigger cash pickup notification
                    }
                    
                    ProcessResult.Success
                },
                onFailure = { error ->
                    // Step 3b: Failure - classify and decide
                    handleSyncError(error, transactionGuid)
                }
            )
        } catch (e: SerializationException) {
            // Malformed payload - permanent failure
            println("[TransactionSyncHandler] Serialization error: ${e.message?.take(100)}")
            ProcessResult.Abandon("Invalid payload: serialization failed")
        } catch (e: Exception) {
            // Unexpected error - retry
            println("[TransactionSyncHandler] Unexpected error: ${e::class.simpleName}")
            ProcessResult.Retry(e.message ?: "Unknown error")
        }
    }
    
    /**
     * Marks a local transaction as synced with the backend.
     * 
     * Updates local database with:
     * - syncStatus = SYNCED
     * - remoteId = backend-assigned ID
     * 
     * Per TRANSACTION_API_SUBMISSION_IMPLEMENTATION_PLAN.md:
     * After successful sync, local copy can be deleted or marked synced.
     */
    private suspend fun markTransactionAsSynced(transactionGuid: String, remoteId: Int?) {
        try {
            transactionRepository.markAsSynced(transactionGuid, remoteId ?: 0)
            println("[TransactionSyncHandler] Transaction $transactionGuid marked as synced (remoteId: ${remoteId ?: "pending"})")
        } catch (e: Exception) {
            // Log but don't fail - sync was successful
            println("[TransactionSyncHandler] Warning: Failed to update local sync status: ${e.message}")
        }
    }
    
    /**
     * Classifies sync errors and decides retry/abandon strategy.
     * 
     * **Classification Rules:**
     * - 400: Validation error → Abandon (won't succeed on retry)
     * - 401: Auth error → Retry (token refresh will happen)
     * - 403: Forbidden → Abandon (permission issue)
     * - 404: Not found → Abandon (endpoint issue)
     * - 409: Conflict → Success (already processed - idempotent)
     * - 5xx: Server error → Retry
     * - Network: Retry
     */
    private fun handleSyncError(error: Throwable, transactionGuid: String): ProcessResult {
        val errorMessage = error.message ?: "Unknown error"
        
        return when {
            // 400 Bad Request - validation error, permanent failure
            errorMessage.contains("400") -> {
                println("[TransactionSyncHandler] Validation error for $transactionGuid - abandoning")
                ProcessResult.Abandon("Validation error: $errorMessage")
            }
            
            // 401 Unauthorized - token expired, will be refreshed
            errorMessage.contains("401") -> {
                println("[TransactionSyncHandler] Auth error for $transactionGuid - will retry after token refresh")
                ProcessResult.Retry("Token expired")
            }
            
            // 403 Forbidden - permission issue
            errorMessage.contains("403") -> {
                println("[TransactionSyncHandler] Permission denied for $transactionGuid - abandoning")
                ProcessResult.Abandon("Permission denied")
            }
            
            // 404 Not Found - endpoint issue
            errorMessage.contains("404") -> {
                println("[TransactionSyncHandler] Endpoint not found for $transactionGuid - abandoning")
                ProcessResult.Abandon("Endpoint not found")
            }
            
            // 409 Conflict - already processed (idempotent success)
            errorMessage.contains("409") -> {
                println("[TransactionSyncHandler] Transaction $transactionGuid already processed (409 Conflict)")
                ProcessResult.Success
            }
            
            // 5xx Server Error - transient, retry
            errorMessage.contains("500") || 
            errorMessage.contains("502") ||
            errorMessage.contains("503") ||
            errorMessage.contains("504") -> {
                println("[TransactionSyncHandler] Server error for $transactionGuid - will retry")
                ProcessResult.Retry("Server error")
            }
            
            // Network errors - retry
            errorMessage.contains("UnknownHost") ||
            errorMessage.contains("SocketTimeout") ||
            errorMessage.contains("ConnectException") ||
            errorMessage.contains("NoRouteToHost") -> {
                println("[TransactionSyncHandler] Network error for $transactionGuid - will retry")
                ProcessResult.Retry("Network error")
            }
            
            // Unknown error - default to retry (safer than abandon)
            else -> {
                println("[TransactionSyncHandler] Unknown error for $transactionGuid: ${errorMessage.take(100)}")
                ProcessResult.Retry(errorMessage.take(100))
            }
        }
    }
    
    // ========================================================================
    // Other Queue Item Types (Placeholders)
    // ========================================================================
    
    private suspend fun syncReturn(item: QueuedItem): ProcessResult {
        // TODO: Implement return/refund sync
        println("[TransactionSyncHandler] Return sync not yet implemented")
        return ProcessResult.Retry("Return sync not implemented")
    }
    
    private suspend fun syncAdjustment(item: QueuedItem): ProcessResult {
        // TODO: Implement inventory adjustment sync
        println("[TransactionSyncHandler] Adjustment sync not yet implemented")
        return ProcessResult.Retry("Adjustment sync not implemented")
    }
    
    private suspend fun syncClockEvent(item: QueuedItem): ProcessResult {
        // TODO: Implement clock in/out sync
        println("[TransactionSyncHandler] Clock event sync not yet implemented")
        return ProcessResult.Retry("Clock event sync not implemented")
    }
    
    private suspend fun syncApprovalAudit(item: QueuedItem): ProcessResult {
        // TODO: Implement approval audit sync
        println("[TransactionSyncHandler] Approval audit sync not yet implemented")
        return ProcessResult.Retry("Approval audit sync not implemented")
    }
}

/**
 * Factory function to create TransactionSyncHandler with default dependencies.
 */
fun createTransactionSyncHandler(
    transactionApiService: TransactionApiService,
    transactionRepository: TransactionRepository,
    json: Json = Json { 
        ignoreUnknownKeys = true 
        isLenient = true
    }
): TransactionSyncHandler {
    return TransactionSyncHandler(
        transactionApiService = transactionApiService,
        transactionRepository = transactionRepository,
        json = json
    )
}

