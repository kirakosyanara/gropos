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

