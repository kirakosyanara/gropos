package com.unisight.gropos.features.checkout.data

import com.couchbase.lite.Collection
import com.couchbase.lite.ConcurrencyControl
import com.couchbase.lite.DataSource
import com.couchbase.lite.MutableArray
import com.couchbase.lite.MutableDocument
import com.couchbase.lite.Ordering
import com.couchbase.lite.QueryBuilder
import com.couchbase.lite.SelectResult
import com.unisight.gropos.core.database.DatabaseConfig
import com.unisight.gropos.core.database.DatabaseProvider
import com.unisight.gropos.features.checkout.domain.model.LookupCategoryWithItems
import com.unisight.gropos.features.checkout.domain.model.LookupProduct
import com.unisight.gropos.features.checkout.domain.repository.LookupCategoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * CouchbaseLite implementation of LookupCategoryRepository for Desktop (JVM).
 * 
 * Per LOOKUP_TABLE.md - Data Storage in Couchbase:
 * - Database: unisight
 * - Scope: pos
 * - Collection: PosLookupCategory
 * 
 * Documents contain embedded items array with lookup products.
 */
class CouchbaseLookupCategoryRepository(
    private val databaseProvider: DatabaseProvider
) : LookupCategoryRepository {
    
    /**
     * PosLookupCategory collection in "pos" scope.
     */
    private val collection: Collection by lazy {
        val db = databaseProvider.getTypedDatabase()
        db.createCollection(
            DatabaseConfig.COLLECTION_POS_LOOKUP_CATEGORY,
            DatabaseConfig.SCOPE_POS
        )
    }
    
    override suspend fun getAllCategories(): List<LookupCategoryWithItems> = withContext(Dispatchers.IO) {
        try {
            val query = QueryBuilder
                .select(SelectResult.all())
                .from(DataSource.collection(collection))
                .orderBy(Ordering.property("order").ascending())
            
            query.execute().use { resultSet ->
                resultSet.allResults().mapNotNull { result ->
                    val dict = result.getDictionary(collection.name)
                    dict?.let { parseCategoryDocument(it.toMap()) }
                }
            }
        } catch (e: Exception) {
            println("CouchbaseLookupCategoryRepository: Error getting all categories - ${e.message}")
            emptyList()
        }
    }
    
    override suspend fun getCategoryById(id: Int): LookupCategoryWithItems? = withContext(Dispatchers.IO) {
        try {
            val doc = collection.getDocument(id.toString())
            doc?.let { parseCategoryDocument(it.toMap()) }
        } catch (e: Exception) {
            println("CouchbaseLookupCategoryRepository: Error getting category $id - ${e.message}")
            null
        }
    }
    
    override suspend fun getProductsByCategory(categoryId: Int): List<LookupProduct> = withContext(Dispatchers.IO) {
        getCategoryById(categoryId)?.items ?: emptyList()
    }
    
    override suspend fun saveCategory(category: LookupCategoryWithItems): Boolean = withContext(Dispatchers.IO) {
        try {
            val doc = MutableDocument(category.id.toString())
            
            doc.setInt("id", category.id)
            doc.setString("name", category.name)
            doc.setInt("order", category.order)
            
            // Items as array of maps
            val itemsList = category.items.map { item ->
                mapOf(
                    "id" to item.id,
                    "lookupGroupId" to item.categoryId,
                    "productId" to item.productId,
                    "product" to item.name,
                    "itemNumber" to item.itemNumber,
                    "order" to item.order,
                    "fileUrl" to item.imageUrl
                )
            }
            doc.setArray("items", MutableArray(itemsList))
            
            // Use LAST_WRITE_WINS to implement upsert
            collection.save(doc, ConcurrencyControl.LAST_WRITE_WINS)
            println("CouchbaseLookupCategoryRepository: Upserted category ${category.id} - ${category.name} (${category.items.size} items)")
            true
        } catch (e: Exception) {
            println("CouchbaseLookupCategoryRepository: Error saving category ${category.id} - ${e.message}")
            e.printStackTrace()
            false
        }
    }
    
    override suspend fun saveCategories(categories: List<LookupCategoryWithItems>): Int = withContext(Dispatchers.IO) {
        var savedCount = 0
        categories.forEach { category ->
            if (saveCategory(category)) {
                savedCount++
            }
        }
        savedCount
    }
    
    override suspend fun deleteCategory(id: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val doc = collection.getDocument(id.toString())
            if (doc != null) {
                collection.delete(doc)
                println("CouchbaseLookupCategoryRepository: Deleted category $id")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            println("CouchbaseLookupCategoryRepository: Error deleting category $id - ${e.message}")
            false
        }
    }
    
    override suspend fun getCategoryCount(): Long = withContext(Dispatchers.IO) {
        try {
            collection.count
        } catch (e: Exception) {
            println("CouchbaseLookupCategoryRepository: Error getting count - ${e.message}")
            0L
        }
    }
    
    override suspend fun clearAll() = withContext(Dispatchers.IO) {
        try {
            val query = QueryBuilder
                .select(SelectResult.expression(com.couchbase.lite.Meta.id))
                .from(DataSource.collection(collection))
            
            query.execute().use { resultSet ->
                resultSet.allResults().forEach { result ->
                    result.getString("id")?.let { docId ->
                        collection.getDocument(docId)?.let { doc ->
                            collection.delete(doc)
                        }
                    }
                }
            }
            println("CouchbaseLookupCategoryRepository: Cleared all categories")
        } catch (e: Exception) {
            println("CouchbaseLookupCategoryRepository: Error clearing categories - ${e.message}")
        }
    }
    
    /**
     * Parse a Couchbase document to a LookupCategoryWithItems domain model.
     */
    @Suppress("UNCHECKED_CAST")
    private fun parseCategoryDocument(map: Map<String, Any?>): LookupCategoryWithItems? {
        return try {
            val id = (map["id"] as? Number)?.toInt() ?: return null
            val name = map["name"] as? String ?: "Category $id"
            val order = (map["order"] as? Number)?.toInt() ?: 0
            
            val itemsRaw = map["items"] as? List<Map<String, Any?>> ?: emptyList()
            val items = itemsRaw.mapNotNull { itemMap ->
                parseItemDocument(itemMap)
            }.sortedBy { it.order }
            
            LookupCategoryWithItems(
                id = id,
                name = name,
                order = order,
                items = items
            )
        } catch (e: Exception) {
            println("CouchbaseLookupCategoryRepository: Error parsing document - ${e.message}")
            null
        }
    }
    
    /**
     * Parse item map to LookupProduct domain model.
     */
    private fun parseItemDocument(map: Map<String, Any?>): LookupProduct? {
        return try {
            LookupProduct(
                id = (map["id"] as? Number)?.toInt() ?: return null,
                categoryId = (map["lookupGroupId"] as? Number)?.toInt() ?: 0,
                productId = (map["productId"] as? Number)?.toInt() ?: 0,
                name = map["product"] as? String ?: "Unknown",
                itemNumber = map["itemNumber"] as? String ?: "",
                order = (map["order"] as? Number)?.toInt() ?: 0,
                imageUrl = map["fileUrl"] as? String
            )
        } catch (e: Exception) {
            println("CouchbaseLookupCategoryRepository: Error parsing item - ${e.message}")
            null
        }
    }
}

/**
 * Extension to get typed Database from DatabaseProvider.
 */
private fun DatabaseProvider.getTypedDatabase(): com.couchbase.lite.Database {
    return getDatabase() as com.couchbase.lite.Database
}

