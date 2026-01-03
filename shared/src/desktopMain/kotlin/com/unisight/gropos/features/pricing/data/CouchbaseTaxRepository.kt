package com.unisight.gropos.features.pricing.data

import com.couchbase.lite.Collection
import com.couchbase.lite.DataSource
import com.couchbase.lite.Expression
import com.couchbase.lite.QueryBuilder
import com.couchbase.lite.SelectResult
import com.unisight.gropos.core.database.DatabaseConfig
import com.unisight.gropos.core.database.DatabaseProvider
import com.unisight.gropos.features.pricing.data.dto.LegacyTaxDto
import com.unisight.gropos.features.pricing.domain.model.Tax
import com.unisight.gropos.features.pricing.domain.repository.TaxRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * CouchbaseLite implementation of TaxRepository for Desktop (JVM).
 * 
 * Per COUCHBASE_LOCAL_STORAGE.md:
 * - Collection: Tax in scope "pos"
 * - Used for standalone tax rate lookups
 * 
 * Per BACKEND_INTEGRATION_STATUS.md:
 * - Uses LegacyTaxDto for parsing legacy JSON structure
 * - Reads from legacy "pos" scope
 * 
 * Per reliability-rules.mdc:
 * - All database operations wrapped in try/catch
 * - Returns empty list or null on error instead of crashing
 * 
 * Per kotlin-standards.mdc:
 * - Uses withContext(Dispatchers.IO) for all DB operations
 */
class CouchbaseTaxRepository(
    private val databaseProvider: DatabaseProvider
) : TaxRepository {
    
    /**
     * Tax collection in "pos" scope.
     */
    private val collection: Collection by lazy {
        val db = databaseProvider.getTypedDatabase()
        db.createCollection(
            DatabaseConfig.COLLECTION_TAX,
            DatabaseConfig.SCOPE_POS
        )
    }
    
    override suspend fun getAllTaxes(): List<Tax> = withContext(Dispatchers.IO) {
        try {
            val query = QueryBuilder.select(SelectResult.all())
                .from(DataSource.collection(collection))
            
            val results = query.execute()
            results.allResults().mapNotNull { result ->
                val map = result.getDictionary(DatabaseConfig.COLLECTION_TAX)?.toMap()
                map?.let { LegacyTaxDto.fromMap(it)?.toDomain() }
            }
        } catch (e: Exception) {
            println("CouchbaseTaxRepository: Error getting all taxes - ${e.message}")
            emptyList()
        }
    }
    
    override suspend fun getTaxById(taxId: Int): Tax? = withContext(Dispatchers.IO) {
        try {
            val query = QueryBuilder.select(SelectResult.all())
                .from(DataSource.collection(collection))
                .where(Expression.property("id").equalTo(Expression.intValue(taxId)))
            
            val result = query.execute().allResults().firstOrNull()
            val map = result?.getDictionary(DatabaseConfig.COLLECTION_TAX)?.toMap()
            map?.let { LegacyTaxDto.fromMap(it)?.toDomain() }
        } catch (e: Exception) {
            println("CouchbaseTaxRepository: Error getting tax by ID $taxId - ${e.message}")
            null
        }
    }
    
    override suspend fun getTaxByName(name: String): Tax? = withContext(Dispatchers.IO) {
        try {
            val query = QueryBuilder.select(SelectResult.all())
                .from(DataSource.collection(collection))
                .where(Expression.property("name").like(Expression.string("%$name%")))
            
            val result = query.execute().allResults().firstOrNull()
            val map = result?.getDictionary(DatabaseConfig.COLLECTION_TAX)?.toMap()
            map?.let { LegacyTaxDto.fromMap(it)?.toDomain() }
        } catch (e: Exception) {
            println("CouchbaseTaxRepository: Error getting tax by name '$name' - ${e.message}")
            null
        }
    }
    
    override suspend fun getTaxesByIds(taxIds: List<Int>): List<Tax> = withContext(Dispatchers.IO) {
        if (taxIds.isEmpty()) return@withContext emptyList()
        
        try {
            // For small lists, query each individually
            // For large lists, consider using IN clause or batch query
            taxIds.mapNotNull { taxId ->
                getTaxById(taxId)
            }
        } catch (e: Exception) {
            println("CouchbaseTaxRepository: Error getting taxes by IDs - ${e.message}")
            emptyList()
        }
    }
}

/**
 * Extension function to get typed Database from DatabaseProvider.
 */
private fun DatabaseProvider.getTypedDatabase(): com.couchbase.lite.Database {
    return this.getDatabase() as com.couchbase.lite.Database
}

