package com.unisight.gropos.features.cashier.data

import com.couchbase.lite.Collection
import com.couchbase.lite.DataSource
import com.couchbase.lite.Expression
import com.couchbase.lite.IndexBuilder
import com.couchbase.lite.MutableDocument
import com.couchbase.lite.Ordering
import com.couchbase.lite.QueryBuilder
import com.couchbase.lite.SelectResult
import com.couchbase.lite.ValueIndexItem
import com.unisight.gropos.core.database.DatabaseConfig
import com.unisight.gropos.core.database.DatabaseProvider
import com.unisight.gropos.features.cashier.data.dto.LegacyVendorPayoutDto
import com.unisight.gropos.features.cashier.domain.model.VendorPayout
import com.unisight.gropos.features.cashier.domain.repository.VendorPayoutRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * CouchbaseLite implementation of VendorPayoutRepository for Android.
 * 
 * Per COUCHBASE_LOCAL_STORAGE.md:
 * - Collection: VendorPayout in scope "pos"
 * - Used for tracking vendor payments made from the till
 * 
 * Per BACKEND_INTEGRATION_STATUS.md:
 * - Uses LegacyVendorPayoutDto for parsing legacy JSON structure
 * - Reads from legacy "pos" scope
 * 
 * Per reliability-rules.mdc:
 * - All database operations wrapped in try/catch
 * - Returns empty list or Result.failure on error instead of crashing
 * 
 * Per kotlin-standards.mdc:
 * - Uses withContext(Dispatchers.IO) for all DB operations
 */
class CouchbaseVendorPayoutRepository(
    private val databaseProvider: DatabaseProvider
) : VendorPayoutRepository {
    
    /**
     * VendorPayout collection in "pos" scope.
     */
    private val collection: Collection by lazy {
        val db = databaseProvider.getTypedDatabase()
        val coll = db.createCollection(
            DatabaseConfig.COLLECTION_VENDOR_PAYOUT,
            DatabaseConfig.SCOPE_POS
        )
        
        try {
            coll.createIndex(
                "payout_date_idx",
                IndexBuilder.valueIndex(ValueIndexItem.property("payoutDateTime"))
            )
        } catch (e: Exception) {
            android.util.Log.d("VendorPayoutRepo", "Index creation: ${e.message}")
        }
        
        coll
    }
    
    override suspend fun savePayout(payout: VendorPayout): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val doc = MutableDocument(payout.id.toString())
            
            doc.setLong("id", payout.id)
            doc.setInt("vendorId", payout.vendorId)
            doc.setString("vendorName", payout.vendorName)
            doc.setDouble("amount", payout.amount.toDouble())
            doc.setString("payoutType", payout.payoutType.name)
            doc.setInt("branchId", payout.branchId)
            doc.setInt("stationId", payout.stationId)
            doc.setInt("employeeId", payout.employeeId)
            payout.employeeName?.let { doc.setString("employeeName", it) }
            payout.managerId?.let { doc.setInt("managerId", it) }
            payout.managerName?.let { doc.setString("managerName", it) }
            payout.referenceNumber?.let { doc.setString("referenceNumber", it) }
            payout.notes?.let { doc.setString("notes", it) }
            doc.setString("payoutDateTime", payout.payoutDateTime)
            doc.setBoolean("isSynced", payout.isSynced)
            payout.createdDate?.let { doc.setString("createdDate", it) }
            
            collection.save(doc)
            android.util.Log.d("VendorPayoutRepo", "Saved payout ${payout.id} for ${payout.vendorName}")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("VendorPayoutRepo", "Error saving payout: ${e.message}")
            Result.failure(e)
        }
    }
    
    override suspend fun getPayoutById(payoutId: Long): VendorPayout? = withContext(Dispatchers.IO) {
        try {
            val doc = collection.getDocument(payoutId.toString())
            doc?.let { LegacyVendorPayoutDto.fromMap(it.toMap())?.toDomain() }
        } catch (e: Exception) {
            android.util.Log.e("VendorPayoutRepo", "Error getting payout by ID: ${e.message}")
            null
        }
    }
    
    override suspend fun getPayoutsForDateRange(startDate: String, endDate: String): List<VendorPayout> = withContext(Dispatchers.IO) {
        try {
            val query = QueryBuilder.select(SelectResult.all())
                .from(DataSource.collection(collection))
                .where(
                    Expression.property("payoutDateTime").greaterThanOrEqualTo(Expression.string(startDate))
                        .and(Expression.property("payoutDateTime").lessThanOrEqualTo(Expression.string(endDate)))
                )
                .orderBy(Ordering.property("payoutDateTime").descending())
            
            val results = query.execute()
            results.allResults().mapNotNull { result ->
                val map = result.getDictionary(DatabaseConfig.COLLECTION_VENDOR_PAYOUT)?.toMap()
                map?.let { LegacyVendorPayoutDto.fromMap(it)?.toDomain() }
            }
        } catch (e: Exception) {
            android.util.Log.e("VendorPayoutRepo", "Error getting payouts for date range: ${e.message}")
            emptyList()
        }
    }
    
    override suspend fun getPayoutsForVendor(vendorId: Int, limit: Int): List<VendorPayout> = withContext(Dispatchers.IO) {
        try {
            val query = QueryBuilder.select(SelectResult.all())
                .from(DataSource.collection(collection))
                .where(Expression.property("vendorId").equalTo(Expression.intValue(vendorId)))
                .orderBy(Ordering.property("payoutDateTime").descending())
                .limit(Expression.intValue(limit))
            
            val results = query.execute()
            results.allResults().mapNotNull { result ->
                val map = result.getDictionary(DatabaseConfig.COLLECTION_VENDOR_PAYOUT)?.toMap()
                map?.let { LegacyVendorPayoutDto.fromMap(it)?.toDomain() }
            }
        } catch (e: Exception) {
            android.util.Log.e("VendorPayoutRepo", "Error getting payouts for vendor: ${e.message}")
            emptyList()
        }
    }
    
    override suspend fun getPayoutsForStation(stationId: Int, limit: Int): List<VendorPayout> = withContext(Dispatchers.IO) {
        try {
            val query = QueryBuilder.select(SelectResult.all())
                .from(DataSource.collection(collection))
                .where(Expression.property("stationId").equalTo(Expression.intValue(stationId)))
                .orderBy(Ordering.property("payoutDateTime").descending())
                .limit(Expression.intValue(limit))
            
            val results = query.execute()
            results.allResults().mapNotNull { result ->
                val map = result.getDictionary(DatabaseConfig.COLLECTION_VENDOR_PAYOUT)?.toMap()
                map?.let { LegacyVendorPayoutDto.fromMap(it)?.toDomain() }
            }
        } catch (e: Exception) {
            android.util.Log.e("VendorPayoutRepo", "Error getting payouts for station: ${e.message}")
            emptyList()
        }
    }
    
    override suspend fun getTodayPayoutTotal(stationId: Int?): BigDecimal = withContext(Dispatchers.IO) {
        try {
            val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
            val startOfDay = "${today}T00:00:00"
            val endOfDay = "${today}T23:59:59"
            
            var whereExpr = Expression.property("payoutDateTime").greaterThanOrEqualTo(Expression.string(startOfDay))
                .and(Expression.property("payoutDateTime").lessThanOrEqualTo(Expression.string(endOfDay)))
            
            stationId?.let {
                whereExpr = whereExpr.and(Expression.property("stationId").equalTo(Expression.intValue(it)))
            }
            
            val query = QueryBuilder.select(SelectResult.all())
                .from(DataSource.collection(collection))
                .where(whereExpr)
            
            val results = query.execute()
            results.allResults().sumOf { result ->
                val map = result.getDictionary(DatabaseConfig.COLLECTION_VENDOR_PAYOUT)?.toMap()
                val payout = map?.let { LegacyVendorPayoutDto.fromMap(it)?.toDomain() }
                payout?.amount ?: BigDecimal.ZERO
            }
        } catch (e: Exception) {
            android.util.Log.e("VendorPayoutRepo", "Error getting today's total: ${e.message}")
            BigDecimal.ZERO
        }
    }
    
    override suspend fun getUnsyncedPayouts(): List<VendorPayout> = withContext(Dispatchers.IO) {
        try {
            val query = QueryBuilder.select(SelectResult.all())
                .from(DataSource.collection(collection))
                .where(Expression.property("isSynced").equalTo(Expression.booleanValue(false)))
                .orderBy(Ordering.property("payoutDateTime").ascending())
            
            val results = query.execute()
            results.allResults().mapNotNull { result ->
                val map = result.getDictionary(DatabaseConfig.COLLECTION_VENDOR_PAYOUT)?.toMap()
                map?.let { LegacyVendorPayoutDto.fromMap(it)?.toDomain() }
            }
        } catch (e: Exception) {
            android.util.Log.e("VendorPayoutRepo", "Error getting unsynced payouts: ${e.message}")
            emptyList()
        }
    }
    
    override suspend fun markAsSynced(payoutId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val doc = collection.getDocument(payoutId.toString())
            if (doc != null) {
                val mutableDoc = doc.toMutable()
                mutableDoc.setBoolean("isSynced", true)
                collection.save(mutableDoc)
                android.util.Log.d("VendorPayoutRepo", "Marked payout $payoutId as synced")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("VendorPayoutRepo", "Error marking payout as synced: ${e.message}")
            Result.failure(e)
        }
    }
}

/**
 * Extension function to get typed Database from DatabaseProvider.
 */
private fun DatabaseProvider.getTypedDatabase(): com.couchbase.lite.Database {
    return this.getDatabase() as com.couchbase.lite.Database
}

