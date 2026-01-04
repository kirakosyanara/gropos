package com.unisight.gropos.features.transaction.data

import com.unisight.gropos.features.returns.domain.service.PullbackItemForCreate
import com.unisight.gropos.features.transaction.domain.model.HeldTransaction
import com.unisight.gropos.features.transaction.domain.model.Transaction
import com.unisight.gropos.features.transaction.domain.repository.TransactionRepository
import com.unisight.gropos.features.transaction.domain.repository.TransactionSearchCriteria
import java.math.BigDecimal

/**
 * Fake implementation of TransactionRepository for testing.
 * 
 * Per testing-strategy.mdc: Use Fakes for repositories in tests.
 * Stores transactions in memory for test verification.
 */
class FakeTransactionRepository : TransactionRepository {
    
    private val transactions = mutableListOf<Transaction>()
    private val heldTransactions = mutableListOf<HeldTransaction>()
    private val returnedQuantities = mutableMapOf<Long, MutableMap<Long, BigDecimal>>()
    
    override suspend fun saveTransaction(transaction: Transaction): Result<Unit> {
        transactions.add(transaction)
        return Result.success(Unit)
    }
    
    override suspend fun getById(id: Long): Transaction? {
        return transactions.find { it.id == id }
    }
    
    override suspend fun getRecent(limit: Int): List<Transaction> {
        return transactions.takeLast(limit)
    }
    
    override suspend fun getPending(): List<Transaction> {
        return transactions.filter { it.syncStatus == Transaction.SYNC_PENDING }
    }
    
    // ========================================================================
    // Sync Operations
    // ========================================================================
    
    override suspend fun markAsSynced(transactionGuid: String, remoteId: Int): Result<Unit> {
        val index = transactions.indexOfFirst { it.guid == transactionGuid }
        if (index >= 0) {
            val updated = transactions[index].copy(
                syncStatus = Transaction.SYNC_COMPLETED,
                remoteId = remoteId
            )
            transactions[index] = updated
            println("FakeTransactionRepository: Marked $transactionGuid as synced (remoteId: $remoteId)")
        }
        return Result.success(Unit)
    }
    
    override suspend fun markSyncFailed(transactionGuid: String, errorMessage: String?): Result<Unit> {
        val index = transactions.indexOfFirst { it.guid == transactionGuid }
        if (index >= 0) {
            val updated = transactions[index].copy(
                syncStatus = Transaction.SYNC_FAILED
            )
            transactions[index] = updated
            println("FakeTransactionRepository: Marked $transactionGuid as sync failed: $errorMessage")
        }
        return Result.success(Unit)
    }
    
    override suspend fun getUnsynced(limit: Int): List<Transaction> {
        return transactions.filter { 
            it.syncStatus == Transaction.SYNC_PENDING || it.syncStatus == Transaction.SYNC_FAILED 
        }.take(limit)
    }
    
    override suspend fun deleteById(id: Long): Result<Unit> {
        transactions.removeAll { it.id == id }
        println("FakeTransactionRepository: Deleted transaction $id")
        return Result.success(Unit)
    }
    
    override suspend fun holdTransaction(heldTransaction: HeldTransaction): Result<Unit> {
        heldTransactions.add(heldTransaction)
        return Result.success(Unit)
    }
    
    override suspend fun getHeldTransactions(): List<HeldTransaction> {
        return heldTransactions.toList()
    }
    
    override suspend fun getHeldTransactionById(id: String): HeldTransaction? {
        return heldTransactions.find { it.id == id }
    }
    
    override suspend fun deleteHeldTransaction(id: String): Result<Unit> {
        heldTransactions.removeAll { it.id == id }
        return Result.success(Unit)
    }
    
    override suspend fun searchTransactions(criteria: TransactionSearchCriteria): List<Transaction> {
        return transactions.filter { transaction ->
            var matches = true
            
            // Filter by receipt number (partial match)
            criteria.receiptNumber?.let { receipt ->
                matches = matches && transaction.id.toString().contains(receipt)
            }
            
            // Filter by amount (exact match)
            criteria.amount?.let { amount ->
                matches = matches && transaction.grandTotal.compareTo(amount) == 0
            }
            
            // Filter by employee ID
            criteria.employeeId?.let { empId ->
                matches = matches && transaction.employeeId == empId
            }
            
            // Date filters would require parsing - simplified for fake implementation
            matches
        }.take(criteria.limit)
    }
    
    // ========================================================================
    // Pullback Operations
    // ========================================================================
    
    override suspend fun findByGuid(guid: String): Transaction? {
        return transactions.find { it.guid == guid }
    }
    
    override suspend fun getReturnedQuantities(transactionId: Long): Map<Long, BigDecimal> {
        return returnedQuantities[transactionId] ?: emptyMap()
    }
    
    override suspend fun createPullbackTransaction(
        originalTransactionId: Long,
        items: List<PullbackItemForCreate>,
        totalValue: BigDecimal
    ): Long {
        // Track returned quantities
        val quantities = returnedQuantities.getOrPut(originalTransactionId) { mutableMapOf() }
        items.forEach { item ->
            val current = quantities[item.originalItemId] ?: BigDecimal.ZERO
            quantities[item.originalItemId] = current + item.quantity
        }
        
        // Generate new transaction ID
        val newId = System.currentTimeMillis()
        println("FakeTransactionRepository: Created pullback transaction $newId for original $originalTransactionId")
        return newId
    }
    
    // Test helpers
    fun getHeldCount(): Int = heldTransactions.size
    fun getTransactionCount(): Int = transactions.size
    fun clear() {
        transactions.clear()
        heldTransactions.clear()
        returnedQuantities.clear()
    }
}

