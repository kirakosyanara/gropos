package com.unisight.gropos.features.transaction.domain.repository

import com.unisight.gropos.features.transaction.domain.model.Transaction

/**
 * Repository interface for transaction persistence.
 * 
 * Per DATABASE_SCHEMA.md:
 * - Collection: LocalTransaction
 * - Scope: local
 * - Documents stored with ID pattern: {id} or {id}-P (pending sync)
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
}

