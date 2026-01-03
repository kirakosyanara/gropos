package com.unisight.gropos.features.transaction.data

import com.couchbase.lite.Collection
import com.couchbase.lite.DataSource
import com.couchbase.lite.Expression
import com.couchbase.lite.IndexBuilder
import com.couchbase.lite.MutableArray
import com.couchbase.lite.MutableDocument
import com.couchbase.lite.Ordering
import com.couchbase.lite.QueryBuilder
import com.couchbase.lite.SelectResult
import com.couchbase.lite.ValueIndexItem
import com.unisight.gropos.core.database.DatabaseConfig
import com.unisight.gropos.core.database.DatabaseProvider
import com.unisight.gropos.features.transaction.domain.model.HeldTransaction
import com.unisight.gropos.features.transaction.domain.model.HeldTransactionItem
import com.unisight.gropos.features.transaction.domain.model.Transaction
import com.unisight.gropos.features.transaction.domain.model.TransactionItem
import com.unisight.gropos.features.transaction.domain.model.TransactionPayment
import com.unisight.gropos.features.transaction.domain.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal

/**
 * CouchbaseLite implementation of TransactionRepository for Android.
 * 
 * Per DATABASE_SCHEMA.md:
 * - Collection: LocalTransaction in local scope
 * - Document ID: {id} or {id}-P (pending sync)
 * - Index: completedDateTime descending for history queries
 * 
 * Per reliability-rules.mdc:
 * - All database operations wrapped in try/catch
 * - Returns Result.failure on error instead of crashing
 * 
 * Per kotlin-standards.mdc:
 * - Uses withContext(Dispatchers.IO) for all DB operations
 */
class CouchbaseTransactionRepository(
    private val databaseProvider: DatabaseProvider
) : TransactionRepository {
    
    /**
     * Lazily gets or creates the LocalTransaction collection.
     * 
     * Per DATABASE_SCHEMA.md: Collection "LocalTransaction" in scope "local"
     */
    private val collection: Collection by lazy {
        val db = databaseProvider.getTypedDatabase()
        val coll = db.createCollection(
            DatabaseConfig.COLLECTION_LOCAL_TRANSACTION,
            DatabaseConfig.SCOPE_LOCAL
        )
        
        // Create index on completedDateTime for history queries
        try {
            coll.createIndex(
                "transaction_date_idx",
                IndexBuilder.valueIndex(ValueIndexItem.property("completedDateTime"))
            )
            android.util.Log.d("TransactionRepo", "Created completedDateTime index")
        } catch (e: Exception) {
            // Index may already exist, that's fine
            android.util.Log.d("TransactionRepo", "Index creation: ${e.message}")
        }
        
        coll
    }
    
    /**
     * Saves a completed transaction to the local database.
     * 
     * Per DATABASE_SCHEMA.md: Serialize transaction to JSON document.
     * Per reliability-stability.mdc: Wrap in Result for error handling.
     */
    override suspend fun saveTransaction(transaction: Transaction): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val documentId = transaction.id.toString()
            val doc = MutableDocument(documentId)
            
            // Core fields
            doc.setLong("id", transaction.id)
            doc.setString("guid", transaction.guid)
            doc.setInt("branchId", transaction.branchId)
            doc.setInt("stationId", transaction.stationId)
            transaction.employeeId?.let { doc.setInt("employeeId", it) }
            transaction.employeeName?.let { doc.setString("employeeName", it) }
            
            // Status
            doc.setInt("transactionStatusId", transaction.transactionStatusId)
            doc.setString("transactionTypeName", transaction.transactionTypeName)
            
            // Timestamps
            doc.setString("startDateTime", transaction.startDateTime)
            doc.setString("completedDateTime", transaction.completedDateTime)
            doc.setString("completedDate", transaction.completedDate)
            
            // Totals - store as Double for Couchbase compatibility
            doc.setDouble("subTotal", transaction.subTotal.toDouble())
            doc.setDouble("discountTotal", transaction.discountTotal.toDouble())
            doc.setDouble("taxTotal", transaction.taxTotal.toDouble())
            doc.setDouble("crvTotal", transaction.crvTotal.toDouble())
            doc.setDouble("grandTotal", transaction.grandTotal.toDouble())
            
            // Item count
            doc.setInt("itemCount", transaction.itemCount)
            
            // Customer info
            transaction.customerName?.let { doc.setString("customerName", it) }
            transaction.loyaltyCardNumber?.let { doc.setString("loyaltyCardNumber", it) }
            
            // Items array
            val itemsArray = MutableArray()
            transaction.items.forEach { item ->
                itemsArray.addDictionary(com.couchbase.lite.MutableDictionary().apply {
                    setLong("id", item.id)
                    setLong("transactionId", item.transactionId)
                    setInt("branchProductId", item.branchProductId)
                    setString("branchProductName", item.branchProductName)
                    setDouble("quantityUsed", item.quantityUsed.toDouble())
                    setString("unitType", item.unitType)
                    setDouble("retailPrice", item.retailPrice.toDouble())
                    item.salePrice?.let { setDouble("salePrice", it.toDouble()) }
                    setDouble("priceUsed", item.priceUsed.toDouble())
                    setDouble("discountAmountPerUnit", item.discountAmountPerUnit.toDouble())
                    setDouble("transactionDiscountAmountPerUnit", item.transactionDiscountAmountPerUnit.toDouble())
                    item.floorPrice?.let { setDouble("floorPrice", it.toDouble()) }
                    setDouble("taxPerUnit", item.taxPerUnit.toDouble())
                    setDouble("taxTotal", item.taxTotal.toDouble())
                    setDouble("crvRatePerUnit", item.crvRatePerUnit.toDouble())
                    setDouble("subTotal", item.subTotal.toDouble())
                    setDouble("savingsTotal", item.savingsTotal.toDouble())
                    setBoolean("isRemoved", item.isRemoved)
                    setBoolean("isPromptedPrice", item.isPromptedPrice)
                    setBoolean("isFloorPriceOverridden", item.isFloorPriceOverridden)
                    setString("soldById", item.soldById)
                    setString("taxIndicator", item.taxIndicator)
                    setBoolean("isFoodStampEligible", item.isFoodStampEligible)
                    item.scanDateTime?.let { setString("scanDateTime", it) }
                })
            }
            doc.setArray("items", itemsArray)
            
            // Payments array
            val paymentsArray = MutableArray()
            transaction.payments.forEach { payment ->
                paymentsArray.addDictionary(com.couchbase.lite.MutableDictionary().apply {
                    setLong("id", payment.id)
                    setLong("transactionId", payment.transactionId)
                    setInt("paymentMethodId", payment.paymentMethodId)
                    setString("paymentMethodName", payment.paymentMethodName)
                    setDouble("value", payment.value.toDouble())
                    payment.referenceNumber?.let { setString("referenceNumber", it) }
                    payment.approvalCode?.let { setString("approvalCode", it) }
                    payment.cardType?.let { setString("cardType", it) }
                    payment.cardLastFour?.let { setString("cardLastFour", it) }
                    setBoolean("isSuccessful", payment.isSuccessful)
                    setString("paymentDateTime", payment.paymentDateTime)
                })
            }
            doc.setArray("payments", paymentsArray)
            
            // Save to database
            collection.save(doc)
            
            android.util.Log.d("TransactionRepo", "Saved transaction ${transaction.id} with ${transaction.items.size} items and ${transaction.payments.size} payments")
            android.util.Log.d("TransactionRepo", "Grand Total = ${transaction.grandTotal}")
            
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("TransactionRepo", "Error saving transaction ${transaction.id}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Retrieves a transaction by its ID.
     */
    override suspend fun getById(id: Long): Transaction? = withContext(Dispatchers.IO) {
        try {
            val doc = collection.getDocument(id.toString())
            doc?.let { mapToTransaction(it.toMap()) }
        } catch (e: Exception) {
            android.util.Log.e("TransactionRepo", "Error getting transaction by ID $id", e)
            null
        }
    }
    
    /**
     * Retrieves the most recent transactions.
     * 
     * Per DATABASE_SCHEMA.md: Query by completedDateTime descending.
     */
    override suspend fun getRecent(limit: Int): List<Transaction> = withContext(Dispatchers.IO) {
        try {
            val query = QueryBuilder
                .select(SelectResult.all())
                .from(DataSource.collection(collection))
                .orderBy(Ordering.property("completedDateTime").descending())
                .limit(Expression.intValue(limit))
            
            query.execute().use { resultSet ->
                resultSet.allResults().mapNotNull { result ->
                    val dict = result.getDictionary(collection.name)
                    dict?.let { mapToTransaction(it.toMap()) }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("TransactionRepo", "Error getting recent transactions", e)
            emptyList()
        }
    }
    
    /**
     * Retrieves all pending (unsynced) transactions.
     * 
     * Per DATABASE_SCHEMA.md: Documents with "-P" suffix are pending sync.
     * Note: This implementation uses document ID pattern matching.
     */
    override suspend fun getPending(): List<Transaction> = withContext(Dispatchers.IO) {
        try {
            // For now, return empty list as we're not implementing sync yet
            // In production, we'd query by transactionStatusId or document ID pattern
            emptyList()
        } catch (e: Exception) {
            android.util.Log.e("TransactionRepo", "Error getting pending transactions", e)
            emptyList()
        }
    }
    
    /**
     * Searches transactions by various criteria.
     * 
     * Per RETURNS.md: Returns processing requires finding original transaction.
     */
    override suspend fun searchTransactions(criteria: com.unisight.gropos.features.transaction.domain.repository.TransactionSearchCriteria): List<Transaction> = withContext(Dispatchers.IO) {
        try {
            var whereExpression: Expression? = null
            
            // Filter by receipt number (partial match on ID)
            criteria.receiptNumber?.let { receipt ->
                val idExpression = Expression.property("id").like(Expression.string("%$receipt%"))
                whereExpression = idExpression
            }
            
            // Filter by date range
            criteria.startDate?.let { startDate ->
                val dateExpr = Expression.property("completedDateTime").greaterThanOrEqualTo(Expression.string(startDate))
                whereExpression = whereExpression?.and(dateExpr) ?: dateExpr
            }
            
            criteria.endDate?.let { endDate ->
                val dateExpr = Expression.property("completedDateTime").lessThanOrEqualTo(Expression.string(endDate))
                whereExpression = whereExpression?.and(dateExpr) ?: dateExpr
            }
            
            // Filter by employee
            criteria.employeeId?.let { empId ->
                val empExpr = Expression.property("employeeId").equalTo(Expression.intValue(empId))
                whereExpression = whereExpression?.and(empExpr) ?: empExpr
            }
            
            // Build query
            val queryBuilder = QueryBuilder
                .select(SelectResult.all())
                .from(DataSource.collection(collection))
            
            val query = if (whereExpression != null) {
                queryBuilder
                    .where(whereExpression!!)
                    .orderBy(Ordering.property("completedDateTime").descending())
                    .limit(Expression.intValue(criteria.limit))
            } else {
                queryBuilder
                    .orderBy(Ordering.property("completedDateTime").descending())
                    .limit(Expression.intValue(criteria.limit))
            }
            
            query.execute().use { resultSet ->
                val results = resultSet.allResults().mapNotNull { result ->
                    val dict = result.getDictionary(collection.name)
                    dict?.let { mapToTransaction(it.toMap()) }
                }
                
                // Filter by amount if specified (post-query filter for BigDecimal comparison)
                criteria.amount?.let { amount ->
                    results.filter { it.grandTotal.compareTo(amount) == 0 }
                } ?: results
            }
        } catch (e: Exception) {
            android.util.Log.e("TransactionRepo", "Error searching transactions", e)
            emptyList()
        }
    }
    
    /**
     * Finds a transaction by its GUID (receipt number).
     */
    override suspend fun findByGuid(guid: String): Transaction? = withContext(Dispatchers.IO) {
        try {
            val query = QueryBuilder
                .select(SelectResult.all())
                .from(DataSource.collection(collection))
                .where(Expression.property("guid").equalTo(Expression.string(guid)))
                .limit(Expression.intValue(1))
            
            query.execute().use { resultSet ->
                val result = resultSet.allResults().firstOrNull()
                val dict = result?.getDictionary(collection.name)
                dict?.let { mapToTransaction(it.toMap()) }
            }
        } catch (e: Exception) {
            android.util.Log.e("TransactionRepo", "Error finding transaction by GUID", e)
            null
        }
    }
    
    /**
     * Gets returned quantities for a transaction (for returns validation).
     */
    override suspend fun getReturnedQuantities(transactionId: Long): Map<Long, BigDecimal> = withContext(Dispatchers.IO) {
        returnedQuantities[transactionId]?.toMap() ?: emptyMap()
    }
    
    /**
     * Creates a pullback (return) transaction.
     */
    override suspend fun createPullbackTransaction(
        originalTransactionId: Long,
        items: List<com.unisight.gropos.features.returns.domain.service.PullbackItemForCreate>,
        totalValue: BigDecimal
    ): Long = withContext(Dispatchers.IO) {
        // TODO: Implement full return transaction creation
        // For now, generate a new transaction ID
        val newTransactionId = System.currentTimeMillis()
        
        // Track returned quantities
        items.forEach { item ->
            val quantities = returnedQuantities.getOrPut(originalTransactionId) { mutableMapOf() }
            val currentQty = quantities[item.originalItemId] ?: BigDecimal.ZERO
            quantities[item.originalItemId] = currentQty + item.quantity
        }
        
        android.util.Log.d("TransactionRepo", "Created pullback $newTransactionId for original $originalTransactionId")
        
        newTransactionId
    }
    
    // Track returned quantities per original transaction
    private val returnedQuantities = mutableMapOf<Long, MutableMap<Long, BigDecimal>>()
    
    // ========================================================================
    // Document Mapping
    // ========================================================================
    
    /**
     * Maps a Couchbase document to a Transaction entity.
     */
    @Suppress("UNCHECKED_CAST")
    private fun mapToTransaction(map: Map<String, Any?>): Transaction? {
        return try {
            val id = (map["id"] as? Number)?.toLong() ?: return null
            
            // Parse items array
            val items = (map["items"] as? List<Map<String, Any?>>)?.mapNotNull { itemMap ->
                mapToTransactionItem(itemMap)
            } ?: emptyList()
            
            // Parse payments array
            val payments = (map["payments"] as? List<Map<String, Any?>>)?.mapNotNull { paymentMap ->
                mapToTransactionPayment(paymentMap)
            } ?: emptyList()
            
            Transaction(
                id = id,
                guid = map["guid"] as? String ?: "",
                branchId = (map["branchId"] as? Number)?.toInt() ?: 1,
                stationId = (map["stationId"] as? Number)?.toInt() ?: 1,
                employeeId = (map["employeeId"] as? Number)?.toInt(),
                employeeName = map["employeeName"] as? String,
                transactionStatusId = (map["transactionStatusId"] as? Number)?.toInt() ?: Transaction.COMPLETED,
                transactionTypeName = map["transactionTypeName"] as? String ?: "Sale",
                startDateTime = map["startDateTime"] as? String ?: "",
                completedDateTime = map["completedDateTime"] as? String ?: "",
                completedDate = map["completedDate"] as? String ?: "",
                subTotal = BigDecimal((map["subTotal"] as? Number)?.toString() ?: "0"),
                discountTotal = BigDecimal((map["discountTotal"] as? Number)?.toString() ?: "0"),
                taxTotal = BigDecimal((map["taxTotal"] as? Number)?.toString() ?: "0"),
                crvTotal = BigDecimal((map["crvTotal"] as? Number)?.toString() ?: "0"),
                grandTotal = BigDecimal((map["grandTotal"] as? Number)?.toString() ?: "0"),
                itemCount = (map["itemCount"] as? Number)?.toInt() ?: 0,
                customerName = map["customerName"] as? String,
                loyaltyCardNumber = map["loyaltyCardNumber"] as? String,
                items = items,
                payments = payments
            )
        } catch (e: Exception) {
            android.util.Log.e("TransactionRepo", "Error mapping document to Transaction", e)
            null
        }
    }
    
    private fun mapToTransactionItem(map: Map<String, Any?>): TransactionItem? {
        return try {
            TransactionItem(
                id = (map["id"] as? Number)?.toLong() ?: return null,
                transactionId = (map["transactionId"] as? Number)?.toLong() ?: 0,
                branchProductId = (map["branchProductId"] as? Number)?.toInt() ?: 0,
                branchProductName = map["branchProductName"] as? String ?: "",
                quantityUsed = BigDecimal((map["quantityUsed"] as? Number)?.toString() ?: "1"),
                unitType = map["unitType"] as? String ?: "ea",
                retailPrice = BigDecimal((map["retailPrice"] as? Number)?.toString() ?: "0"),
                salePrice = (map["salePrice"] as? Number)?.let { BigDecimal(it.toString()) },
                priceUsed = BigDecimal((map["priceUsed"] as? Number)?.toString() ?: "0"),
                discountAmountPerUnit = BigDecimal((map["discountAmountPerUnit"] as? Number)?.toString() ?: "0"),
                transactionDiscountAmountPerUnit = BigDecimal((map["transactionDiscountAmountPerUnit"] as? Number)?.toString() ?: "0"),
                floorPrice = (map["floorPrice"] as? Number)?.let { BigDecimal(it.toString()) },
                taxPerUnit = BigDecimal((map["taxPerUnit"] as? Number)?.toString() ?: "0"),
                taxTotal = BigDecimal((map["taxTotal"] as? Number)?.toString() ?: "0"),
                crvRatePerUnit = BigDecimal((map["crvRatePerUnit"] as? Number)?.toString() ?: "0"),
                subTotal = BigDecimal((map["subTotal"] as? Number)?.toString() ?: "0"),
                savingsTotal = BigDecimal((map["savingsTotal"] as? Number)?.toString() ?: "0"),
                isRemoved = map["isRemoved"] as? Boolean ?: false,
                isPromptedPrice = map["isPromptedPrice"] as? Boolean ?: false,
                isFloorPriceOverridden = map["isFloorPriceOverridden"] as? Boolean ?: false,
                soldById = map["soldById"] as? String ?: "Quantity",
                taxIndicator = map["taxIndicator"] as? String ?: "T",
                isFoodStampEligible = map["isFoodStampEligible"] as? Boolean ?: false,
                scanDateTime = map["scanDateTime"] as? String
            )
        } catch (e: Exception) {
            android.util.Log.e("TransactionRepo", "Error mapping item", e)
            null
        }
    }
    
    private fun mapToTransactionPayment(map: Map<String, Any?>): TransactionPayment? {
        return try {
            TransactionPayment(
                id = (map["id"] as? Number)?.toLong() ?: return null,
                transactionId = (map["transactionId"] as? Number)?.toLong() ?: 0,
                paymentMethodId = (map["paymentMethodId"] as? Number)?.toInt() ?: 1,
                paymentMethodName = map["paymentMethodName"] as? String ?: "Cash",
                value = BigDecimal((map["value"] as? Number)?.toString() ?: "0"),
                referenceNumber = map["referenceNumber"] as? String,
                approvalCode = map["approvalCode"] as? String,
                cardType = map["cardType"] as? String,
                cardLastFour = map["cardLastFour"] as? String,
                isSuccessful = map["isSuccessful"] as? Boolean ?: true,
                paymentDateTime = map["paymentDateTime"] as? String ?: ""
            )
        } catch (e: Exception) {
            android.util.Log.e("TransactionRepo", "Error mapping payment", e)
            null
        }
    }
    
    // ========================================================================
    // Held Transactions (Hold/Recall)
    // Per TRANSACTION_FLOW.md: Support for suspended transactions
    // ========================================================================
    
    /**
     * Lazily gets or creates the HeldTransaction collection.
     */
    private val heldCollection: Collection by lazy {
        val db = databaseProvider.getTypedDatabase()
        val coll = db.createCollection(
            DatabaseConfig.COLLECTION_HELD_TRANSACTION,
            DatabaseConfig.SCOPE_LOCAL
        )
        
        // Create index on heldDateTime for ordering queries
        try {
            coll.createIndex(
                "held_date_idx",
                IndexBuilder.valueIndex(ValueIndexItem.property("heldDateTime"))
            )
            android.util.Log.d("TransactionRepo", "Created heldDateTime index")
        } catch (e: Exception) {
            // Index may already exist
            android.util.Log.d("TransactionRepo", "Held index creation: ${e.message}")
        }
        
        coll
    }
    
    /**
     * Holds (suspends) a transaction for later recall.
     */
    override suspend fun holdTransaction(heldTransaction: HeldTransaction): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val doc = MutableDocument(heldTransaction.id)
            
            // Core fields
            doc.setString("id", heldTransaction.id)
            doc.setString("holdName", heldTransaction.holdName)
            doc.setString("heldDateTime", heldTransaction.heldDateTime)
            heldTransaction.employeeId?.let { doc.setInt("employeeId", it) }
            heldTransaction.employeeName?.let { doc.setString("employeeName", it) }
            doc.setInt("stationId", heldTransaction.stationId)
            doc.setInt("branchId", heldTransaction.branchId)
            doc.setInt("itemCount", heldTransaction.itemCount)
            
            // Totals
            doc.setDouble("grandTotal", heldTransaction.grandTotal.toDouble())
            doc.setDouble("subTotal", heldTransaction.subTotal.toDouble())
            doc.setDouble("taxTotal", heldTransaction.taxTotal.toDouble())
            doc.setDouble("crvTotal", heldTransaction.crvTotal.toDouble())
            
            // Items array
            val itemsArray = MutableArray()
            heldTransaction.items.forEach { item ->
                itemsArray.addDictionary(com.couchbase.lite.MutableDictionary().apply {
                    setInt("branchProductId", item.branchProductId)
                    setString("productName", item.productName)
                    setDouble("quantityUsed", item.quantityUsed.toDouble())
                    setDouble("priceUsed", item.priceUsed.toDouble())
                    setDouble("discountAmountPerUnit", item.discountAmountPerUnit.toDouble())
                    setDouble("transactionDiscountAmountPerUnit", item.transactionDiscountAmountPerUnit.toDouble())
                    setBoolean("isRemoved", item.isRemoved)
                    setBoolean("isPromptedPrice", item.isPromptedPrice)
                    setBoolean("isFloorPriceOverridden", item.isFloorPriceOverridden)
                    item.scanDateTime?.let { setString("scanDateTime", it) }
                })
            }
            doc.setArray("items", itemsArray)
            
            // Save to database
            heldCollection.save(doc)
            
            android.util.Log.d("TransactionRepo", "Held transaction ${heldTransaction.id} - ${heldTransaction.holdName}")
            android.util.Log.d("TransactionRepo", "${heldTransaction.itemCount} items, Total = ${heldTransaction.grandTotal}")
            
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("TransactionRepo", "Error holding transaction", e)
            Result.failure(e)
        }
    }
    
    /**
     * Retrieves all held transactions.
     */
    override suspend fun getHeldTransactions(): List<HeldTransaction> = withContext(Dispatchers.IO) {
        try {
            val query = QueryBuilder
                .select(SelectResult.all())
                .from(DataSource.collection(heldCollection))
                .orderBy(Ordering.property("heldDateTime").descending())
            
            query.execute().use { resultSet ->
                resultSet.allResults().mapNotNull { result ->
                    val dict = result.getDictionary(heldCollection.name)
                    dict?.let { mapToHeldTransaction(it.toMap()) }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("TransactionRepo", "Error getting held transactions", e)
            emptyList()
        }
    }
    
    /**
     * Retrieves a specific held transaction by ID.
     */
    override suspend fun getHeldTransactionById(id: String): HeldTransaction? = withContext(Dispatchers.IO) {
        try {
            val doc = heldCollection.getDocument(id)
            doc?.let { mapToHeldTransaction(it.toMap()) }
        } catch (e: Exception) {
            android.util.Log.e("TransactionRepo", "Error getting held transaction by ID $id", e)
            null
        }
    }
    
    /**
     * Deletes a held transaction.
     */
    override suspend fun deleteHeldTransaction(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val doc = heldCollection.getDocument(id)
            if (doc != null) {
                heldCollection.delete(doc)
                android.util.Log.d("TransactionRepo", "Deleted held transaction $id")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("TransactionRepo", "Error deleting held transaction $id", e)
            Result.failure(e)
        }
    }
    
    // ========================================================================
    // Held Transaction Mapping
    // ========================================================================
    
    @Suppress("UNCHECKED_CAST")
    private fun mapToHeldTransaction(map: Map<String, Any?>): HeldTransaction? {
        return try {
            val items = (map["items"] as? List<Map<String, Any?>>)?.mapNotNull { itemMap ->
                mapToHeldTransactionItem(itemMap)
            } ?: emptyList()
            
            HeldTransaction(
                id = map["id"] as? String ?: return null,
                holdName = map["holdName"] as? String ?: "",
                heldDateTime = map["heldDateTime"] as? String ?: "",
                employeeId = (map["employeeId"] as? Number)?.toInt(),
                employeeName = map["employeeName"] as? String,
                stationId = (map["stationId"] as? Number)?.toInt() ?: 1,
                branchId = (map["branchId"] as? Number)?.toInt() ?: 1,
                itemCount = (map["itemCount"] as? Number)?.toInt() ?: 0,
                grandTotal = BigDecimal((map["grandTotal"] as? Number)?.toString() ?: "0"),
                subTotal = BigDecimal((map["subTotal"] as? Number)?.toString() ?: "0"),
                taxTotal = BigDecimal((map["taxTotal"] as? Number)?.toString() ?: "0"),
                crvTotal = BigDecimal((map["crvTotal"] as? Number)?.toString() ?: "0"),
                items = items
            )
        } catch (e: Exception) {
            android.util.Log.e("TransactionRepo", "Error mapping held transaction", e)
            null
        }
    }
    
    private fun mapToHeldTransactionItem(map: Map<String, Any?>): HeldTransactionItem? {
        return try {
            HeldTransactionItem(
                branchProductId = (map["branchProductId"] as? Number)?.toInt() ?: return null,
                productName = map["productName"] as? String ?: "",
                quantityUsed = BigDecimal((map["quantityUsed"] as? Number)?.toString() ?: "1"),
                priceUsed = BigDecimal((map["priceUsed"] as? Number)?.toString() ?: "0"),
                discountAmountPerUnit = BigDecimal((map["discountAmountPerUnit"] as? Number)?.toString() ?: "0"),
                transactionDiscountAmountPerUnit = BigDecimal((map["transactionDiscountAmountPerUnit"] as? Number)?.toString() ?: "0"),
                isRemoved = map["isRemoved"] as? Boolean ?: false,
                isPromptedPrice = map["isPromptedPrice"] as? Boolean ?: false,
                isFloorPriceOverridden = map["isFloorPriceOverridden"] as? Boolean ?: false,
                scanDateTime = map["scanDateTime"] as? String
            )
        } catch (e: Exception) {
            android.util.Log.e("TransactionRepo", "Error mapping held item", e)
            null
        }
    }
}

