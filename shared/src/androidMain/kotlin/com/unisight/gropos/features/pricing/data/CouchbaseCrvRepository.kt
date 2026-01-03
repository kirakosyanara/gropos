package com.unisight.gropos.features.pricing.data

import com.couchbase.lite.Collection
import com.couchbase.lite.DataSource
import com.couchbase.lite.Expression
import com.couchbase.lite.QueryBuilder
import com.couchbase.lite.SelectResult
import com.unisight.gropos.core.database.DatabaseConfig
import com.unisight.gropos.core.database.DatabaseProvider
import com.unisight.gropos.features.pricing.data.dto.LegacyCrvDto
import com.unisight.gropos.features.pricing.domain.model.Crv
import com.unisight.gropos.features.pricing.domain.repository.CrvRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * CouchbaseLite implementation of CrvRepository for Android.
 * 
 * Per COUCHBASE_LOCAL_STORAGE.md:
 * - Collection: CRV in scope "pos"
 * - Used for container deposit rate lookups
 * 
 * Per BACKEND_INTEGRATION_STATUS.md:
 * - Uses LegacyCrvDto for parsing legacy JSON structure
 * - Reads from legacy "pos" scope
 * 
 * Per reliability-rules.mdc:
 * - All database operations wrapped in try/catch
 * - Returns empty list or null on error instead of crashing
 * 
 * Per kotlin-standards.mdc:
 * - Uses withContext(Dispatchers.IO) for all DB operations
 */
class CouchbaseCrvRepository(
    private val databaseProvider: DatabaseProvider
) : CrvRepository {
    
    /**
     * CRV collection in "pos" scope.
     */
    private val collection: Collection by lazy {
        val db = databaseProvider.getTypedDatabase()
        db.createCollection(
            DatabaseConfig.COLLECTION_CRV,
            DatabaseConfig.SCOPE_POS
        )
    }
    
    override suspend fun getAllCrvRates(): List<Crv> = withContext(Dispatchers.IO) {
        try {
            val query = QueryBuilder.select(SelectResult.all())
                .from(DataSource.collection(collection))
            
            val results = query.execute()
            results.allResults().mapNotNull { result ->
                val map = result.getDictionary(DatabaseConfig.COLLECTION_CRV)?.toMap()
                map?.let { LegacyCrvDto.fromMap(it)?.toDomain() }
            }
        } catch (e: Exception) {
            android.util.Log.e("CrvRepository", "Error getting all CRV rates: ${e.message}")
            emptyList()
        }
    }
    
    override suspend fun getCrvById(crvId: Int): Crv? = withContext(Dispatchers.IO) {
        try {
            val query = QueryBuilder.select(SelectResult.all())
                .from(DataSource.collection(collection))
                .where(Expression.property("id").equalTo(Expression.intValue(crvId)))
            
            val result = query.execute().allResults().firstOrNull()
            val map = result?.getDictionary(DatabaseConfig.COLLECTION_CRV)?.toMap()
            map?.let { LegacyCrvDto.fromMap(it)?.toDomain() }
        } catch (e: Exception) {
            android.util.Log.e("CrvRepository", "Error getting CRV by ID $crvId: ${e.message}")
            null
        }
    }
    
    override suspend fun getDefaultSmallContainerCrv(): Crv? = withContext(Dispatchers.IO) {
        try {
            val query = QueryBuilder.select(SelectResult.all())
                .from(DataSource.collection(collection))
                .where(Expression.property("name").like(Expression.string("%Under 24oz%")))
            
            val result = query.execute().allResults().firstOrNull()
            val map = result?.getDictionary(DatabaseConfig.COLLECTION_CRV)?.toMap()
            map?.let { LegacyCrvDto.fromMap(it)?.toDomain() }
        } catch (e: Exception) {
            android.util.Log.e("CrvRepository", "Error getting default small CRV: ${e.message}")
            null
        }
    }
    
    override suspend fun getDefaultLargeContainerCrv(): Crv? = withContext(Dispatchers.IO) {
        try {
            val query = QueryBuilder.select(SelectResult.all())
                .from(DataSource.collection(collection))
                .where(Expression.property("name").like(Expression.string("%24oz and Over%")))
            
            val result = query.execute().allResults().firstOrNull()
            val map = result?.getDictionary(DatabaseConfig.COLLECTION_CRV)?.toMap()
            map?.let { LegacyCrvDto.fromMap(it)?.toDomain() }
        } catch (e: Exception) {
            android.util.Log.e("CrvRepository", "Error getting default large CRV: ${e.message}")
            null
        }
    }
}

/**
 * Extension function to get typed Database from DatabaseProvider.
 */
private fun DatabaseProvider.getTypedDatabase(): com.couchbase.lite.Database {
    return this.getDatabase() as com.couchbase.lite.Database
}

