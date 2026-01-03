package com.unisight.gropos.features.transaction.data

import com.couchbase.lite.Collection
import com.couchbase.lite.DataSource
import com.couchbase.lite.Expression
import com.couchbase.lite.MutableArray
import com.couchbase.lite.MutableDictionary
import com.couchbase.lite.MutableDocument
import com.couchbase.lite.Ordering
import com.couchbase.lite.QueryBuilder
import com.couchbase.lite.SelectResult
import com.unisight.gropos.core.database.DatabaseConfig
import com.unisight.gropos.core.database.DatabaseProvider
import com.unisight.gropos.features.returns.domain.service.PullbackItemForCreate
import com.unisight.gropos.features.transaction.data.dto.LegacyTransactionDto
import com.unisight.gropos.features.transaction.domain.model.HeldTransaction
import com.unisight.gropos.features.transaction.domain.model.Transaction
import com.unisight.gropos.features.transaction.domain.repository.TransactionRepository
import com.unisight.gropos.features.transaction.domain.repository.TransactionSearchCriteria
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.util.UUID

/**
 * CouchbaseLite implementation of TransactionRepository for Desktop (JVM).
 * 
 * Per COUCHBASE_LOCAL_STORAGE.md:
 * - Collection: LocalTransaction in scope "pos"
 * - Document ID: {guid} or {guid}-P (pending during active transaction)
 * 
 * Per BACKEND_INTEGRATION_STATUS.md:
 * - Implements the Pending Document Pattern:
 *   1. Active transactions saved with "-P" suffix
 *   2. On completion, document is finalized (suffix removed)
 *   3. On startup, check for "-P" documents to resume crashed transactions
 * 
 * Per reliability-rules.mdc:
 * - All database operations wrapped in try/catch
 * - Uses Result<T> for operations that can fail
 */
class CouchbaseTransactionRepository(
    private val databaseProvider: DatabaseProvider
) : TransactionRepository {
    
    companion object {
        /** Suffix for pending (in-progress) transaction documents */
        private const val PENDING_SUFFIX = "-P"
    }
    
    /**
     * LocalTransaction collection in "pos" scope (per COUCHBASE_LOCAL_STORAGE.md).
     */
    private val transactionCollection: Collection by lazy {
        val db = databaseProvider.getTypedDatabase()
        db.createCollection(
            DatabaseConfig.COLLECTION_LOCAL_TRANSACTION,
            DatabaseConfig.SCOPE_POS
        )
    }
    
    /**
     * HeldTransaction collection for suspended transactions.
     */
    private val heldCollection: Collection by lazy {
        val db = databaseProvider.getTypedDatabase()
        db.createCollection(
            DatabaseConfig.COLLECTION_HELD_TRANSACTION,
            DatabaseConfig.SCOPE_POS
        )
    }
    
    // ========================================================================
    // Transaction CRUD Operations
    // ========================================================================
    
    /**
     * Saves a completed transaction to the local database.
     * 
     * Per COUCHBASE_LOCAL_STORAGE.md - Pending Document Pattern:
     * 1. Delete any pending document with "-P" suffix
     * 2. Save the final document with just the guid as ID
     */
    override suspend fun saveTransaction(transaction: Transaction): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val docId = transaction.guid
            val pendingDocId = "${transaction.guid}$PENDING_SUFFIX"
            
            // 1. Delete pending document if it exists
            transactionCollection.getDocument(pendingDocId)?.let {
                transactionCollection.delete(it)
                println("CouchbaseTransactionRepository: Deleted pending document $pendingDocId")
            }
            
            // 2. Save the final document
            val doc = createTransactionDocument(docId, transaction)
            transactionCollection.save(doc)
            
            println("CouchbaseTransactionRepository: Saved transaction ${transaction.guid}")
            Result.success(Unit)
        } catch (e: Exception) {
            println("CouchbaseTransactionRepository: Error saving transaction - ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Saves a transaction as pending (during active transaction).
     * 
     * Per COUCHBASE_LOCAL_STORAGE.md - Pending Document Pattern:
     * Document ID: {guid}-P
     * 
     * This is called periodically during transaction to ensure data isn't lost on crash.
     */
    suspend fun savePendingTransaction(transaction: Transaction): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val pendingDocId = "${transaction.guid}$PENDING_SUFFIX"
            val doc = createTransactionDocument(pendingDocId, transaction)
            transactionCollection.save(doc)
            
            println("CouchbaseTransactionRepository: Saved pending transaction ${transaction.guid}")
            Result.success(Unit)
        } catch (e: Exception) {
            println("CouchbaseTransactionRepository: Error saving pending transaction - ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Retrieves any pending transactions (crashed sessions).
     * 
     * Per COUCHBASE_LOCAL_STORAGE.md:
     * On startup, check for documents with "-P" suffix to resume.
     */
    suspend fun getPendingTransactionsForResume(): List<Transaction> = withContext(Dispatchers.IO) {
        try {
            // Query all documents with "-P" suffix
            val query = QueryBuilder
                .select(SelectResult.all())
                .from(DataSource.collection(transactionCollection))
            
            query.execute().use { resultSet ->
                resultSet.allResults()
                    .mapNotNull { result ->
                        val dict = result.getDictionary(transactionCollection.name)
                        dict?.let { 
                            // Check if document ID ends with -P
                            val id = dict.getString("guid")
                            if (id != null) {
                                val docId = "${id}$PENDING_SUFFIX"
                                val pendingDoc = transactionCollection.getDocument(docId)
                                pendingDoc?.let { parseTransactionDocument(it.toMap()) }
                            } else null
                        }
                    }
            }
        } catch (e: Exception) {
            println("CouchbaseTransactionRepository: Error getting pending transactions for resume - ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Deletes a pending transaction document.
     * Called when user cancels/voids an active transaction.
     */
    suspend fun deletePendingTransaction(guid: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val pendingDocId = "$guid$PENDING_SUFFIX"
            transactionCollection.getDocument(pendingDocId)?.let {
                transactionCollection.delete(it)
                println("CouchbaseTransactionRepository: Deleted pending transaction $pendingDocId")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            println("CouchbaseTransactionRepository: Error deleting pending transaction - ${e.message}")
            Result.failure(e)
        }
    }
    
    override suspend fun getById(id: Long): Transaction? = withContext(Dispatchers.IO) {
        try {
            // Query by id field
            val query = QueryBuilder
                .select(SelectResult.all())
                .from(DataSource.collection(transactionCollection))
                .where(Expression.property("id").equalTo(Expression.longValue(id)))
            
            query.execute().use { resultSet ->
                resultSet.allResults().firstOrNull()?.let { result ->
                    val dict = result.getDictionary(transactionCollection.name)
                    dict?.let { parseTransactionDocument(it.toMap()) }
                }
            }
        } catch (e: Exception) {
            println("CouchbaseTransactionRepository: Error getting transaction by ID $id - ${e.message}")
            null
        }
    }
    
    override suspend fun getRecent(limit: Int): List<Transaction> = withContext(Dispatchers.IO) {
        try {
            val query = QueryBuilder
                .select(SelectResult.all())
                .from(DataSource.collection(transactionCollection))
                .where(
                    Expression.property("transactionStatusId")
                        .equalTo(Expression.string("Completed"))
                )
                .orderBy(Ordering.property("completedDate").descending())
                .limit(Expression.intValue(limit))
            
            query.execute().use { resultSet ->
                resultSet.allResults().mapNotNull { result ->
                    val dict = result.getDictionary(transactionCollection.name)
                    dict?.let { parseTransactionDocument(it.toMap()) }
                }
            }
        } catch (e: Exception) {
            println("CouchbaseTransactionRepository: Error getting recent transactions - ${e.message}")
            emptyList()
        }
    }
    
    override suspend fun getPending(): List<Transaction> = withContext(Dispatchers.IO) {
        try {
            val query = QueryBuilder
                .select(SelectResult.all())
                .from(DataSource.collection(transactionCollection))
                .where(
                    Expression.property("transactionStatusId")
                        .equalTo(Expression.string("Open"))
                        .or(Expression.property("transactionStatusId").equalTo(Expression.string("Pending")))
                )
            
            query.execute().use { resultSet ->
                resultSet.allResults().mapNotNull { result ->
                    val dict = result.getDictionary(transactionCollection.name)
                    dict?.let { parseTransactionDocument(it.toMap()) }
                }
            }
        } catch (e: Exception) {
            println("CouchbaseTransactionRepository: Error getting pending transactions - ${e.message}")
            emptyList()
        }
    }
    
    // ========================================================================
    // Hold/Recall Operations
    // ========================================================================
    
    override suspend fun holdTransaction(heldTransaction: HeldTransaction): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val doc = MutableDocument(heldTransaction.id)
            doc.setString("id", heldTransaction.id)
            doc.setString("holdName", heldTransaction.holdName)
            doc.setString("heldDateTime", heldTransaction.heldDateTime)
            heldTransaction.employeeId?.let { doc.setInt("employeeId", it) }
            heldTransaction.employeeName?.let { doc.setString("employeeName", it) }
            doc.setInt("stationId", heldTransaction.stationId)
            doc.setInt("branchId", heldTransaction.branchId)
            doc.setInt("itemCount", heldTransaction.itemCount)
            doc.setDouble("grandTotal", heldTransaction.grandTotal.toDouble())
            doc.setDouble("subTotal", heldTransaction.subTotal.toDouble())
            doc.setDouble("taxTotal", heldTransaction.taxTotal.toDouble())
            doc.setDouble("crvTotal", heldTransaction.crvTotal.toDouble())
            
            // Store serialized cart items (per HeldTransactionItem schema)
            val itemsList = heldTransaction.items.map { item ->
                mapOf(
                    "branchProductId" to item.branchProductId,
                    "productName" to item.productName,
                    "quantityUsed" to item.quantityUsed.toDouble(),
                    "priceUsed" to item.priceUsed.toDouble(),
                    "discountAmountPerUnit" to item.discountAmountPerUnit.toDouble(),
                    "transactionDiscountAmountPerUnit" to item.transactionDiscountAmountPerUnit.toDouble(),
                    "isRemoved" to item.isRemoved,
                    "isPromptedPrice" to item.isPromptedPrice,
                    "isFloorPriceOverridden" to item.isFloorPriceOverridden,
                    "scanDateTime" to item.scanDateTime
                )
            }
            doc.setArray("items", MutableArray(itemsList))
            
            heldCollection.save(doc)
            println("CouchbaseTransactionRepository: Held transaction ${heldTransaction.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            println("CouchbaseTransactionRepository: Error holding transaction - ${e.message}")
            Result.failure(e)
        }
    }
    
    override suspend fun getHeldTransactions(): List<HeldTransaction> = withContext(Dispatchers.IO) {
        try {
            val query = QueryBuilder
                .select(SelectResult.all())
                .from(DataSource.collection(heldCollection))
                .orderBy(Ordering.property("heldDateTime").descending())
            
            query.execute().use { resultSet ->
                resultSet.allResults().mapNotNull { result ->
                    val dict = result.getDictionary(heldCollection.name)
                    dict?.let { parseHeldTransaction(it.toMap()) }
                }
            }
        } catch (e: Exception) {
            println("CouchbaseTransactionRepository: Error getting held transactions - ${e.message}")
            emptyList()
        }
    }
    
    override suspend fun getHeldTransactionById(id: String): HeldTransaction? = withContext(Dispatchers.IO) {
        try {
            val doc = heldCollection.getDocument(id)
            doc?.let { parseHeldTransaction(it.toMap()) }
        } catch (e: Exception) {
            println("CouchbaseTransactionRepository: Error getting held transaction $id - ${e.message}")
            null
        }
    }
    
    override suspend fun deleteHeldTransaction(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            heldCollection.getDocument(id)?.let {
                heldCollection.delete(it)
                println("CouchbaseTransactionRepository: Deleted held transaction $id")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            println("CouchbaseTransactionRepository: Error deleting held transaction - ${e.message}")
            Result.failure(e)
        }
    }
    
    // ========================================================================
    // Transaction Search
    // ========================================================================
    
    override suspend fun searchTransactions(criteria: TransactionSearchCriteria): List<Transaction> = withContext(Dispatchers.IO) {
        try {
            var whereExpression: Expression = Expression.property("transactionStatusId")
                .equalTo(Expression.string("Completed"))
            
            // Filter by receipt number (partial match on guid or id)
            criteria.receiptNumber?.let { receipt ->
                whereExpression = whereExpression.and(
                    Expression.property("guid").like(Expression.string("%$receipt%"))
                        .or(Expression.property("id").like(Expression.string("%$receipt%")))
                )
            }
            
            // Filter by employee ID
            criteria.employeeId?.let { empId ->
                whereExpression = whereExpression.and(
                    Expression.property("employeeId").equalTo(Expression.intValue(empId))
                )
            }
            
            // Filter by date range
            criteria.startDate?.let { startDate ->
                whereExpression = whereExpression.and(
                    Expression.property("completedDate").greaterThanOrEqualTo(Expression.string(startDate))
                )
            }
            criteria.endDate?.let { endDate ->
                whereExpression = whereExpression.and(
                    Expression.property("completedDate").lessThanOrEqualTo(Expression.string(endDate))
                )
            }
            
            val query = QueryBuilder
                .select(SelectResult.all())
                .from(DataSource.collection(transactionCollection))
                .where(whereExpression)
                .orderBy(Ordering.property("completedDate").descending())
                .limit(Expression.intValue(criteria.limit))
            
            query.execute().use { resultSet ->
                resultSet.allResults().mapNotNull { result ->
                    val dict = result.getDictionary(transactionCollection.name)
                    dict?.let { parseTransactionDocument(it.toMap()) }
                }.let { transactions ->
                    // Additional filter by amount (not easily done in CouchbaseLite query)
                    criteria.amount?.let { amount ->
                        transactions.filter { it.grandTotal.compareTo(amount) == 0 }
                    } ?: transactions
                }
            }
        } catch (e: Exception) {
            println("CouchbaseTransactionRepository: Error searching transactions - ${e.message}")
            emptyList()
        }
    }
    
    // ========================================================================
    // Pullback Operations
    // ========================================================================
    
    override suspend fun findByGuid(guid: String): Transaction? = withContext(Dispatchers.IO) {
        try {
            // Try direct document lookup first
            val doc = transactionCollection.getDocument(guid)
            if (doc != null) {
                return@withContext parseTransactionDocument(doc.toMap())
            }
            
            // Fallback: query by guid field
            val query = QueryBuilder
                .select(SelectResult.all())
                .from(DataSource.collection(transactionCollection))
                .where(Expression.property("guid").equalTo(Expression.string(guid)))
            
            query.execute().use { resultSet ->
                resultSet.allResults().firstOrNull()?.let { result ->
                    val dict = result.getDictionary(transactionCollection.name)
                    dict?.let { parseTransactionDocument(it.toMap()) }
                }
            }
        } catch (e: Exception) {
            println("CouchbaseTransactionRepository: Error finding transaction by GUID $guid - ${e.message}")
            null
        }
    }
    
    override suspend fun getReturnedQuantities(transactionId: Long): Map<Long, BigDecimal> = withContext(Dispatchers.IO) {
        try {
            // Query all return transactions that reference this original transaction
            val query = QueryBuilder
                .select(SelectResult.all())
                .from(DataSource.collection(transactionCollection))
                .where(
                    Expression.property("originalTransactionId")
                        .equalTo(Expression.longValue(transactionId))
                        .and(Expression.property("transactionTypeName").equalTo(Expression.string("Return")))
                )
            
            val returnedQuantities = mutableMapOf<Long, BigDecimal>()
            
            query.execute().use { resultSet ->
                resultSet.allResults().forEach { result ->
                    val dict = result.getDictionary(transactionCollection.name)
                    dict?.let { map ->
                        @Suppress("UNCHECKED_CAST")
                        val items = (map.toMap()["items"] as? List<Map<String, Any?>>)
                        items?.forEach { item ->
                            val itemId = (item["originalItemId"] as? Number)?.toLong() ?: return@forEach
                            val qty = BigDecimal.valueOf((item["quantityUsed"] as? Number)?.toDouble() ?: 0.0)
                            returnedQuantities[itemId] = (returnedQuantities[itemId] ?: BigDecimal.ZERO) + qty
                        }
                    }
                }
            }
            
            returnedQuantities
        } catch (e: Exception) {
            println("CouchbaseTransactionRepository: Error getting returned quantities - ${e.message}")
            emptyMap()
        }
    }
    
    override suspend fun createPullbackTransaction(
        originalTransactionId: Long,
        items: List<PullbackItemForCreate>,
        totalValue: BigDecimal
    ): Long = withContext(Dispatchers.IO) {
        try {
            val newId = System.currentTimeMillis()
            val guid = UUID.randomUUID().toString()
            val now = java.time.Instant.now().toString()
            
            val doc = MutableDocument(guid)
            doc.setLong("id", newId)
            doc.setString("guid", guid)
            doc.setLong("originalTransactionId", originalTransactionId)
            doc.setString("transactionTypeName", "Return")
            doc.setString("transactionStatusId", "Completed")
            doc.setString("startDate", now)
            doc.setString("completedDate", now)
            doc.setDouble("grandTotal", -totalValue.toDouble()) // Negative for returns
            doc.setDouble("subTotal", -totalValue.toDouble())
            doc.setDouble("taxTotal", 0.0)
            doc.setDouble("crvTotal", 0.0)
            doc.setDouble("savingsTotal", 0.0)
            doc.setInt("itemCount", items.sumOf { it.quantity.toInt() })
            
            val itemsList = items.map { item ->
                mapOf(
                    "originalItemId" to item.originalItemId,
                    "branchProductId" to item.branchProductId,
                    "branchProductName" to "Returned Item",
                    "quantityUsed" to item.quantity.toDouble(),
                    "priceUsed" to -item.priceUsed.toDouble()
                )
            }
            doc.setArray("items", MutableArray(itemsList))
            
            transactionCollection.save(doc)
            println("CouchbaseTransactionRepository: Created pullback transaction $newId for original $originalTransactionId")
            
            newId
        } catch (e: Exception) {
            println("CouchbaseTransactionRepository: Error creating pullback transaction - ${e.message}")
            throw e
        }
    }
    
    // ========================================================================
    // Document Creation/Parsing
    // ========================================================================
    
    /**
     * Creates a MutableDocument from a Transaction.
     * Uses LEGACY field names per BACKEND_INTEGRATION_STATUS.md.
     */
    private fun createTransactionDocument(docId: String, transaction: Transaction): MutableDocument {
        val dto = LegacyTransactionDto.fromDomain(transaction)
        val doc = MutableDocument(docId)
        
        // Primary identifiers
        doc.setLong("id", dto.id)
        doc.setString("guid", dto.guid)
        
        // Branch/Employee
        doc.setInt("branchId", dto.branchId)
        dto.branch?.let { doc.setString("branch", it) }
        dto.employeeId?.let { doc.setInt("employeeId", it) }
        dto.employee?.let { doc.setString("employee", it) }
        
        // Status
        dto.transactionStatusId?.let { doc.setString("transactionStatusId", it) }
        
        // Timestamps - using legacy field names
        dto.startDate?.let { doc.setString("startDate", it) }
        dto.completedDate?.let { doc.setString("completedDate", it) }
        
        // Counts
        dto.rowCount?.let { doc.setInt("rowCount", it) }
        dto.itemCount?.let { doc.setInt("itemCount", it) }
        dto.uniqueProductCount?.let { doc.setInt("uniqueProductCount", it) }
        
        // Totals - using legacy field names (savingsTotal instead of discountTotal)
        dto.savingsTotal?.let { doc.setDouble("savingsTotal", it) }
        dto.taxTotal?.let { doc.setDouble("taxTotal", it) }
        dto.subTotal?.let { doc.setDouble("subTotal", it) }
        dto.crvTotal?.let { doc.setDouble("crvTotal", it) }
        dto.fee?.let { doc.setDouble("fee", it) }
        dto.grandTotal?.let { doc.setDouble("grandTotal", it) }
        
        // Items array
        dto.items?.let { items ->
            val itemsList = items.map { item ->
                mapOf(
                    "id" to item.id,
                    "transactionId" to item.transactionId,
                    "branchProductId" to item.branchProductId,
                    "branchProductName" to item.branchProductName,
                    "quantityUsed" to item.quantityUsed,
                    "unitType" to (item.unitType ?: "Each"),
                    "retailPrice" to item.retailPrice,
                    "salePrice" to item.salePrice,
                    "priceUsed" to item.priceUsed,
                    "discountAmountPerUnit" to item.discountAmountPerUnit,
                    "transactionDiscountAmountPerUnit" to item.transactionDiscountAmountPerUnit,
                    "floorPrice" to item.floorPrice,
                    "taxPerUnit" to item.taxPerUnit,
                    "taxTotal" to item.taxTotal,
                    "crvRatePerUnit" to item.crvRatePerUnit,
                    "subTotal" to item.subTotal,
                    "savingsTotal" to item.savingsTotal,
                    "isRemoved" to item.isRemoved,
                    "isPromptedPrice" to item.isPromptedPrice,
                    "isFloorPriceOverridden" to item.isFloorPriceOverridden,
                    "soldById" to item.soldById,
                    "taxIndicator" to item.taxIndicator,
                    "isFoodStampEligible" to item.isFoodStampEligible,
                    "scanDateTime" to item.scanDateTime
                )
            }
            doc.setArray("items", MutableArray(itemsList))
        }
        
        // Payments array
        dto.payments?.let { payments ->
            val paymentsList = payments.map { payment ->
                mapOf(
                    "id" to payment.id,
                    "transactionId" to payment.transactionId,
                    "paymentMethodId" to payment.paymentMethodId,
                    "paymentMethodName" to payment.paymentMethodName,
                    "value" to payment.value,
                    "referenceNumber" to payment.referenceNumber,
                    "approvalCode" to payment.approvalCode,
                    "cardType" to payment.cardType,
                    "cardLastFour" to payment.cardLastFour,
                    "isSuccessful" to payment.isSuccessful,
                    "paymentDateTime" to payment.paymentDateTime
                )
            }
            doc.setArray("payments", MutableArray(paymentsList))
        }
        
        return doc
    }
    
    /**
     * Parses a Couchbase document to a Transaction entity.
     * Uses LegacyTransactionDto for field mapping.
     */
    @Suppress("UNCHECKED_CAST")
    private fun parseTransactionDocument(map: Map<String, Any?>): Transaction? {
        val dto = LegacyTransactionDto.fromMap(map)
        return dto?.toDomain()
    }
    
    /**
     * Parses a held transaction document.
     * 
     * Per HeldTransaction model - includes holdName, stationId, branchId, and totals.
     */
    @Suppress("UNCHECKED_CAST")
    private fun parseHeldTransaction(map: Map<String, Any?>): HeldTransaction? {
        return try {
            val id = map["id"] as? String ?: return null
            
            // Parse held items
            val itemsList = (map["items"] as? List<Map<String, Any?>>)?.map { itemMap ->
                com.unisight.gropos.features.transaction.domain.model.HeldTransactionItem(
                    branchProductId = (itemMap["branchProductId"] as? Number)?.toInt() ?: 0,
                    productName = itemMap["productName"] as? String ?: "",
                    quantityUsed = BigDecimal.valueOf((itemMap["quantityUsed"] as? Number)?.toDouble() ?: 1.0),
                    priceUsed = BigDecimal.valueOf((itemMap["priceUsed"] as? Number)?.toDouble() ?: 0.0),
                    discountAmountPerUnit = BigDecimal.valueOf((itemMap["discountAmountPerUnit"] as? Number)?.toDouble() ?: 0.0),
                    transactionDiscountAmountPerUnit = BigDecimal.valueOf((itemMap["transactionDiscountAmountPerUnit"] as? Number)?.toDouble() ?: 0.0),
                    isRemoved = itemMap["isRemoved"] as? Boolean ?: false,
                    isPromptedPrice = itemMap["isPromptedPrice"] as? Boolean ?: false,
                    isFloorPriceOverridden = itemMap["isFloorPriceOverridden"] as? Boolean ?: false,
                    scanDateTime = itemMap["scanDateTime"] as? String
                )
            } ?: emptyList()
            
            HeldTransaction(
                id = id,
                holdName = map["holdName"] as? String ?: "Held #$id",
                heldDateTime = map["heldDateTime"] as? String ?: "",
                employeeId = (map["employeeId"] as? Number)?.toInt(),
                employeeName = map["employeeName"] as? String,
                stationId = (map["stationId"] as? Number)?.toInt() ?: 1,
                branchId = (map["branchId"] as? Number)?.toInt() ?: 1,
                itemCount = (map["itemCount"] as? Number)?.toInt() ?: 0,
                grandTotal = BigDecimal.valueOf((map["grandTotal"] as? Number)?.toDouble() ?: 0.0),
                subTotal = BigDecimal.valueOf((map["subTotal"] as? Number)?.toDouble() ?: 0.0),
                taxTotal = BigDecimal.valueOf((map["taxTotal"] as? Number)?.toDouble() ?: 0.0),
                crvTotal = BigDecimal.valueOf((map["crvTotal"] as? Number)?.toDouble() ?: 0.0),
                items = itemsList
            )
        } catch (e: Exception) {
            println("CouchbaseTransactionRepository: Error parsing held transaction - ${e.message}")
            null
        }
    }
}
