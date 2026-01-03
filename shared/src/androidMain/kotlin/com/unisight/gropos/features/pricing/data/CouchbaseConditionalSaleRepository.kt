package com.unisight.gropos.features.pricing.data

import com.couchbase.lite.Collection
import com.couchbase.lite.DataSource
import com.couchbase.lite.Expression
import com.couchbase.lite.QueryBuilder
import com.couchbase.lite.SelectResult
import com.unisight.gropos.core.database.DatabaseConfig
import com.unisight.gropos.core.database.DatabaseProvider
import com.unisight.gropos.features.pricing.data.dto.LegacyConditionalSaleDto
import com.unisight.gropos.features.pricing.domain.model.ConditionalSale
import com.unisight.gropos.features.pricing.domain.repository.ConditionalSaleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * CouchbaseLite implementation of ConditionalSaleRepository for Android.
 * 
 * Per COUCHBASE_LOCAL_STORAGE.md:
 * - Collection: ConditionalSale in scope "pos"
 * - Used for age restrictions and conditional sale rules
 * 
 * Per BACKEND_INTEGRATION_STATUS.md:
 * - Uses LegacyConditionalSaleDto for parsing legacy JSON structure
 * - Reads from legacy "pos" scope
 * 
 * Per reliability-rules.mdc:
 * - All database operations wrapped in try/catch
 * - Returns empty list or null on error instead of crashing
 * 
 * Per kotlin-standards.mdc:
 * - Uses withContext(Dispatchers.IO) for all DB operations
 */
class CouchbaseConditionalSaleRepository(
    private val databaseProvider: DatabaseProvider
) : ConditionalSaleRepository {
    
    /**
     * ConditionalSale collection in "pos" scope.
     */
    private val collection: Collection by lazy {
        val db = databaseProvider.getTypedDatabase()
        db.createCollection(
            DatabaseConfig.COLLECTION_CONDITIONAL_SALE,
            DatabaseConfig.SCOPE_POS
        )
    }
    
    override suspend fun getActiveRules(): List<ConditionalSale> = withContext(Dispatchers.IO) {
        try {
            val query = QueryBuilder.select(SelectResult.all())
                .from(DataSource.collection(collection))
                .where(Expression.property("isActive").equalTo(Expression.booleanValue(true)))
            
            val results = query.execute()
            results.allResults().mapNotNull { result ->
                val map = result.getDictionary(DatabaseConfig.COLLECTION_CONDITIONAL_SALE)?.toMap()
                map?.let { LegacyConditionalSaleDto.fromMap(it)?.toDomain() }
            }
        } catch (e: Exception) {
            android.util.Log.e("ConditionalSaleRepo", "Error getting active rules: ${e.message}")
            emptyList()
        }
    }
    
    override suspend fun getRuleById(ruleId: Int): ConditionalSale? = withContext(Dispatchers.IO) {
        try {
            val query = QueryBuilder.select(SelectResult.all())
                .from(DataSource.collection(collection))
                .where(Expression.property("id").equalTo(Expression.intValue(ruleId)))
            
            val result = query.execute().allResults().firstOrNull()
            val map = result?.getDictionary(DatabaseConfig.COLLECTION_CONDITIONAL_SALE)?.toMap()
            map?.let { LegacyConditionalSaleDto.fromMap(it)?.toDomain() }
        } catch (e: Exception) {
            android.util.Log.e("ConditionalSaleRepo", "Error getting rule by ID $ruleId: ${e.message}")
            null
        }
    }
    
    override suspend fun getRulesForProduct(branchProductId: Int, categoryId: Int?): List<ConditionalSale> = withContext(Dispatchers.IO) {
        try {
            val allRules = getActiveRules()
            
            allRules.filter { rule ->
                rule.appliesToProduct(branchProductId, categoryId)
            }
        } catch (e: Exception) {
            android.util.Log.e("ConditionalSaleRepo", "Error getting rules for product: ${e.message}")
            emptyList()
        }
    }
    
    override suspend fun getAgeRestrictionRules(): List<ConditionalSale> = withContext(Dispatchers.IO) {
        try {
            val allRules = getActiveRules()
            
            allRules.filter { rule ->
                rule.isAgeRestricted
            }
        } catch (e: Exception) {
            android.util.Log.e("ConditionalSaleRepo", "Error getting age restriction rules: ${e.message}")
            emptyList()
        }
    }
    
    override suspend fun getRequiredAgeForProduct(branchProductId: Int, categoryId: Int?): Int? = withContext(Dispatchers.IO) {
        try {
            val rulesForProduct = getRulesForProduct(branchProductId, categoryId)
            
            rulesForProduct
                .filter { it.isAgeRestricted }
                .maxOfOrNull { it.minimumAge ?: 0 }
                ?.takeIf { it > 0 }
        } catch (e: Exception) {
            android.util.Log.e("ConditionalSaleRepo", "Error getting required age for product: ${e.message}")
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

