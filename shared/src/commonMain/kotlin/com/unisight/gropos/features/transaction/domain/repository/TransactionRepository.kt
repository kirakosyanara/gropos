package com.unisight.gropos.features.transaction.domain.repository

import com.unisight.gropos.features.transaction.domain.model.HeldTransaction
import com.unisight.gropos.features.transaction.domain.model.Transaction

/**
 * Repository interface for transaction persistence.
 * 
 * Per DATABASE_SCHEMA.md:
 * - Collection: LocalTransaction
 * - Scope: local
 * - Documents stored with ID pattern: {id} or {id}-P (pending sync)
 * 
 * Per TRANSACTION_FLOW.md: Supports Hold/Recall transaction operations.
 * 
 * Per project-structure.mdc: Interface in domain layer, implementation in data layer.
 */
interface TransactionRepository {
    
    /**
     * Saves a completed transaction to the local database.
     * 
     * Per reliability-stability.mdc: All write operations must be wrapped in Result.
     * Per DATABASE_SCHEMA.md: Transaction saved to LocalTransaction collection.
     * 
     * @param transaction The transaction to save
     * @return Result.success(Unit) on success, Result.failure on error
     */
    suspend fun saveTransaction(transaction: Transaction): Result<Unit>
    
    /**
     * Retrieves a transaction by its ID.
     * 
     * @param id The transaction ID
     * @return The transaction if found, null otherwise
     */
    suspend fun getById(id: Long): Transaction?
    
    /**
     * Retrieves the most recent transactions.
     * 
     * Per DATABASE_SCHEMA.md: Query by completedDateTime descending.
     * 
     * @param limit Maximum number of transactions to return
     * @return List of transactions ordered by completedDateTime descending
     */
    suspend fun getRecent(limit: Int = 10): List<Transaction>
    
    /**
     * Retrieves all pending (unsynced) transactions.
     * 
     * Per DATABASE_SCHEMA.md: Documents with "-P" suffix are pending sync.
     * 
     * @return List of pending transactions
     */
    suspend fun getPending(): List<Transaction>
    
    // ========================================================================
    // Sync Operations
    // Per TRANSACTION_API_SUBMISSION_IMPLEMENTATION_PLAN.md: Sync tracking
    // ========================================================================
    
    /**
     * Marks a transaction as synced with the backend.
     * 
     * Updates local record with:
     * - syncStatus = SYNC_COMPLETED
     * - remoteId = backend-assigned ID
     * 
     * Per TRANSACTION_API_SUBMISSION_IMPLEMENTATION_PLAN.md:
     * Called by TransactionSyncHandler after successful API submission.
     * 
     * @param transactionGuid The transaction GUID
     * @param remoteId The backend-assigned transaction ID
     * @return Result.success(Unit) on success, Result.failure on error
     */
    suspend fun markAsSynced(transactionGuid: String, remoteId: Int): Result<Unit>
    
    /**
     * Marks a transaction sync as failed.
     * 
     * Updates local record with:
     * - syncStatus = SYNC_FAILED
     * - Optionally stores error message
     * 
     * @param transactionGuid The transaction GUID
     * @param errorMessage Optional error message for debugging
     * @return Result.success(Unit) on success, Result.failure on error
     */
    suspend fun markSyncFailed(transactionGuid: String, errorMessage: String? = null): Result<Unit>
    
    /**
     * Gets transactions that need to be synced.
     * 
     * Returns transactions with syncStatus = SYNC_PENDING or SYNC_FAILED.
     * 
     * @param limit Maximum number to return
     * @return List of unsynced transactions
     */
    suspend fun getUnsynced(limit: Int = 50): List<Transaction>
    
    /**
     * Deletes a transaction by ID.
     * 
     * Per TRANSACTION_API_SUBMISSION_IMPLEMENTATION_PLAN.md:
     * Optionally called after successful sync to remove local copy.
     * 
     * @param id The transaction ID
     * @return Result.success(Unit) on success, Result.failure on error
     */
    suspend fun deleteById(id: Long): Result<Unit>
    
    // ========================================================================
    // Hold/Recall Operations
    // Per TRANSACTION_FLOW.md: Support for suspended transactions
    // ========================================================================
    
    /**
     * Holds (suspends) a transaction for later recall.
     * 
     * Per TRANSACTION_FLOW.md: "Holding" creates a Transaction record with status HELD.
     * Per FUNCTIONS_MENU.md: Hold button in Functions Panel.
     * 
     * @param heldTransaction The transaction to hold with all cart items
     * @return Result.success(Unit) on success, Result.failure on error
     */
    suspend fun holdTransaction(heldTransaction: HeldTransaction): Result<Unit>
    
    /**
     * Retrieves all held transactions.
     * 
     * Per TRANSACTION_FLOW.md: Recall shows list of HELD transactions.
     * 
     * @return List of held transactions ordered by heldDateTime descending
     */
    suspend fun getHeldTransactions(): List<HeldTransaction>
    
    /**
     * Retrieves a specific held transaction by ID.
     * 
     * @param id The held transaction ID
     * @return The held transaction if found, null otherwise
     */
    suspend fun getHeldTransactionById(id: String): HeldTransaction?
    
    /**
     * Deletes a held transaction (after recall or manual deletion).
     * 
     * Per TRANSACTION_FLOW.md: After recall, delete the HELD record.
     * 
     * @param id The held transaction ID to delete
     * @return Result.success(Unit) on success, Result.failure on error
     */
    suspend fun deleteHeldTransaction(id: String): Result<Unit>
    
    // ========================================================================
    // Transaction Search
    // Per REMEDIATION_CHECKLIST: Find Transaction Screen for returns lookup
    // ========================================================================
    
    /**
     * Searches transactions by various criteria.
     * 
     * Per RETURNS.md: Returns processing requires finding original transaction.
     * Per Find Transaction Screen: Search by receipt number, date range, or amount.
     * 
     * @param criteria The search criteria
     * @return List of matching transactions
     */
    suspend fun searchTransactions(criteria: TransactionSearchCriteria): List<Transaction>
    
    // ========================================================================
    // Pullback Operations
    // Per REMEDIATION_CHECKLIST: Pullback Flow - Implement pullback with receipt scan
    // ========================================================================
    
    /**
     * Finds a transaction by its GUID (receipt number).
     * 
     * Per RETURNS.md: Pullback requires finding transaction by scanned receipt.
     * 
     * @param guid The transaction GUID
     * @return The transaction if found, null otherwise
     */
    suspend fun findByGuid(guid: String): Transaction?
    
    /**
     * Gets previously returned quantities for a transaction.
     * 
     * @param transactionId The original transaction ID
     * @return Map of item ID to returned quantity
     */
    suspend fun getReturnedQuantities(transactionId: Long): Map<Long, java.math.BigDecimal>
    
    /**
     * Creates a pullback (return) transaction.
     * 
     * @param originalTransactionId The original transaction ID
     * @param items The items being pulled back
     * @param totalValue The total value of the pullback
     * @return The new transaction ID
     */
    suspend fun createPullbackTransaction(
        originalTransactionId: Long,
        items: List<com.unisight.gropos.features.returns.domain.service.PullbackItemForCreate>,
        totalValue: java.math.BigDecimal
    ): Long
}

/**
 * Criteria for transaction search.
 * 
 * Per RETURNS.md: Search by receipt number (transaction ID), date, or amount.
 */
data class TransactionSearchCriteria(
    /** Search by transaction/receipt ID (partial match) */
    val receiptNumber: String? = null,
    
    /** Search transactions on or after this date */
    val startDate: String? = null,
    
    /** Search transactions on or before this date */
    val endDate: String? = null,
    
    /** Search by exact amount */
    val amount: java.math.BigDecimal? = null,
    
    /** Search by employee ID */
    val employeeId: Int? = null,
    
    /** Maximum results to return */
    val limit: Int = 50
)

