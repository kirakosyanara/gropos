package com.unisight.gropos.features.transaction.data.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * API Response DTOs for Transaction Submission.
 * 
 * **Source of Truth:** docs/development-plan/TRANSACTION_API_SUBMISSION_IMPLEMENTATION_PLAN.md
 * **Backend Endpoint:** POST /api/v1/transactions/create-transaction
 * 
 * Response codes:
 * - 201: Created successfully
 * - 202: Accepted (processing may have failed, logged for reconciliation)
 * - 400: Validation error
 * - 401: Unauthorized (token expired)
 * - 500: Server error
 */

// ============================================================================
// Success Response
// ============================================================================

/**
 * Response from transaction creation.
 * 
 * Per END_OF_TRANSACTION_API_SUBMISSION.md:
 * - HTTP 201: Success - id and transactionGuid will be populated
 * - HTTP 202: Accepted - processing may have failed, id may be null
 * 
 * **IMPORTANT:** Always check the `status` field first:
 * - "Success" = transaction was created successfully
 * - "Failure" = transaction processing failed (even with HTTP 202)
 */
@Serializable
data class CreateTransactionResponse(
    /** Backend-assigned transaction ID (null if processing failed) */
    val id: Int? = null,
    
    /** Echo of submitted GUID (null if processing failed) */
    val transactionGuid: String? = null,
    
    /** Success/info/error message */
    val message: String? = null,
    
    /** Result status: "Success" or "Failure" */
    val status: String? = null,
    
    /** Flag for cash drawer alert */
    val cashPickupNeeded: Boolean = false
) {
    /** Check if the transaction was successfully created */
    val isSuccess: Boolean get() = status == "Success" && id != null
    
    /** Check if the transaction failed (even with HTTP 202 Accepted) */
    val isFailure: Boolean get() = status == "Failure" || id == null
}

// ============================================================================
// Sync Response (for marking transactions as synced)
// ============================================================================

/**
 * Response after syncing a transaction.
 * 
 * Contains backend IDs to update local records.
 */
@Serializable
data class TransactionSyncResponse(
    /** Backend transaction ID */
    val remoteId: Int,
    
    /** Transaction GUID */
    val transactionGuid: String,
    
    /** Whether sync was successful */
    val success: Boolean,
    
    /** Error message if failed */
    val errorMessage: String? = null,
    
    /** Whether cash pickup is needed */
    val cashPickupNeeded: Boolean = false
)

// ============================================================================
// Error Responses
// ============================================================================

/**
 * Error response from transaction API.
 * 
 * Per END_OF_TRANSACTION_API_SUBMISSION.md:
 * HTTP 400/500 response structure.
 */
@Serializable
data class TransactionApiError(
    /** High-level error message */
    val message: String,
    
    /** Inner exception details (nullable) */
    val innerException: JsonObject? = null,
    
    /** List of validation errors */
    val errors: List<TransactionValidationError> = emptyList(),
    
    /** Stack trace (for debugging, not displayed to user) */
    val stackTrace: String? = null
)

/**
 * Individual validation error.
 * 
 * Per END_OF_TRANSACTION_API_SUBMISSION.md:
 * Detailed error for specific field validation failures.
 */
@Serializable
data class TransactionValidationError(
    /** Property that failed validation (e.g., "items[0].quantitySold") */
    val propertyName: String,
    
    /** Human-readable error message */
    val errorMessage: String,
    
    /** The value that was attempted */
    val attemptedValue: JsonPrimitive? = null,
    
    /** Severity level (0 = Error, 1 = Warning) */
    val severity: Int = 0,
    
    /** Machine-readable error code (e.g., "INVALID_QUANTITY") */
    val errorCode: String
)

// ============================================================================
// Lookup/Query Responses
// ============================================================================

/**
 * Response when looking up a transaction by ID or GUID.
 */
@Serializable
data class TransactionLookupResponse(
    /** Whether transaction was found */
    val found: Boolean,
    
    /** Transaction ID if found */
    val transactionId: Int? = null,
    
    /** Transaction GUID if found */
    val transactionGuid: String? = null,
    
    /** Transaction status */
    val status: String? = null,
    
    /** Error message if not found */
    val errorMessage: String? = null
)

