package com.unisight.gropos.features.transaction.data

import com.unisight.gropos.features.transaction.domain.model.HeldTransaction
import com.unisight.gropos.features.transaction.domain.model.Transaction
import com.unisight.gropos.features.transaction.domain.repository.TransactionRepository

/**
 * Fake implementation of TransactionRepository for testing.
 * 
 * Per testing-strategy.mdc: Use Fakes for repositories in tests.
 * Stores transactions in memory for test verification.
 */
class FakeTransactionRepository : TransactionRepository {
    
    private val transactions = mutableListOf<Transaction>()
    private val heldTransactions = mutableListOf<HeldTransaction>()
    
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
        return transactions.filter { it.transactionStatusId == Transaction.PENDING }
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
    
    // Test helpers
    fun getHeldCount(): Int = heldTransactions.size
    fun getTransactionCount(): Int = transactions.size
    fun clear() {
        transactions.clear()
        heldTransactions.clear()
    }
}

