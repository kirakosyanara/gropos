package com.unisight.gropos.features.checkout.data

import android.util.Log
import com.couchbase.lite.ArrayExpression
import com.couchbase.lite.Collection
import com.couchbase.lite.DataSource
import com.couchbase.lite.Database
import com.couchbase.lite.Expression
import com.couchbase.lite.MutableDocument
import com.couchbase.lite.Ordering
import com.couchbase.lite.QueryBuilder
import com.couchbase.lite.SelectResult
import com.unisight.gropos.core.database.DatabaseConfig
import com.unisight.gropos.core.database.DatabaseProvider
import com.unisight.gropos.features.checkout.domain.model.ItemNumber
import com.unisight.gropos.features.checkout.domain.model.Product
import com.unisight.gropos.features.checkout.domain.model.ProductSale
import com.unisight.gropos.features.checkout.domain.model.ProductTax
import com.unisight.gropos.features.checkout.domain.repository.LookupCategory
import com.unisight.gropos.features.checkout.domain.repository.ProductRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal

/**
 * CouchbaseLite implementation of ProductRepository for Android.
 * 
 * Per DATABASE_SCHEMA.md:
 * - Collection: Product in base_data scope
 * - Query by barcode: ArrayExpression on itemNumbers array
 * - Query by category: Expression.property("category")
 * 
 * Per reliability-rules.mdc:
 * - All database operations wrapped in try/catch
 * - Returns null on error instead of crashing
 * 
 * Per kotlin-standards.mdc:
 * - Uses withContext(Dispatchers.IO) for all DB operations
 */
class CouchbaseProductRepository(
    private val databaseProvider: DatabaseProvider
) : ProductRepository {
    
    companion object {
        private const val TAG = "CouchbaseProductRepo"
    }
    
    /**
     * Lazily gets or creates the Product collection.
     * 
     * Per DATABASE_SCHEMA.md: Collection "Product" in scope "base_data"
     */
    private val collection: Collection by lazy {
        val db = databaseProvider.getTypedDatabase()
        // Create collection if it doesn't exist
        db.createCollection(
            DatabaseConfig.COLLECTION_PRODUCT,
            DatabaseConfig.SCOPE_BASE_DATA
        )
    }
    
    /**
     * Finds a product by barcode.
     * 
     * Per DATABASE_SCHEMA.md - Query by barcode (array contains):
     * ArrayExpression.any("x")
     *     .in(Expression.property("itemNumbers"))
     *     .satisfies(Expression.variable("x.itemNumber").equalTo(barcode))
     */
    override suspend fun getByBarcode(barcode: String): Product? = withContext(Dispatchers.IO) {
        try {
            val itemVar = ArrayExpression.variable("x")
            val valueVar = ArrayExpression.variable("x.itemNumber")
            
            val query = QueryBuilder
                .select(SelectResult.all())
                .from(DataSource.collection(collection))
                .where(
                    ArrayExpression.any(itemVar)
                        .`in`(Expression.property("itemNumbers"))
                        .satisfies(valueVar.equalTo(Expression.string(barcode)))
                )
            
            query.execute().use { resultSet ->
                resultSet.allResults().firstOrNull()?.let { result ->
                    val dict = result.getDictionary(collection.name)
                    dict?.let { mapToProduct(it.toMap()) }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting product by barcode '$barcode'", e)
            null
        }
    }
    
    /**
     * Finds products by category.
     * 
     * Per DATABASE_SCHEMA.md: Query by category, ordered by order field.
     */
    override suspend fun getByCategory(categoryId: Int): List<Product> = withContext(Dispatchers.IO) {
        try {
            val query = QueryBuilder
                .select(SelectResult.all())
                .from(DataSource.collection(collection))
                .where(
                    Expression.property("category")
                        .equalTo(Expression.intValue(categoryId))
                        .and(Expression.property("isActive").equalTo(Expression.booleanValue(true)))
                        .and(Expression.property("isForSale").equalTo(Expression.booleanValue(true)))
                )
                .orderBy(Ordering.property("order").ascending())
            
            query.execute().use { resultSet ->
                resultSet.allResults().mapNotNull { result ->
                    val dict = result.getDictionary(collection.name)
                    dict?.let { mapToProduct(it.toMap()) }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting products by category $categoryId", e)
            emptyList()
        }
    }
    
    /**
     * Finds a product by branchProductId.
     * 
     * Per DATABASE_SCHEMA.md: Document ID is branchProductId.
     */
    override suspend fun getById(branchProductId: Int): Product? = withContext(Dispatchers.IO) {
        try {
            val doc = collection.getDocument(branchProductId.toString())
            doc?.let { mapToProduct(it.toMap()) }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting product by ID $branchProductId", e)
            null
        }
    }
    
    /**
     * Searches products by name.
     * 
     * Per DATABASE_SCHEMA.md: Uses LIKE operator on productName.
     */
    override suspend fun searchByName(query: String): List<Product> = withContext(Dispatchers.IO) {
        try {
            val likePattern = "%${query}%"
            
            val dbQuery = QueryBuilder
                .select(SelectResult.all())
                .from(DataSource.collection(collection))
                .where(
                    Expression.property("productName")
                        .like(Expression.string(likePattern))
                        .and(Expression.property("isActive").equalTo(Expression.booleanValue(true)))
                        .and(Expression.property("isForSale").equalTo(Expression.booleanValue(true)))
                )
                .orderBy(Ordering.property("productName").ascending())
            
            dbQuery.execute().use { resultSet ->
                resultSet.allResults().mapNotNull { result ->
                    val dict = result.getDictionary(collection.name)
                    dict?.let { mapToProduct(it.toMap()) }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching products by name '$query'", e)
            emptyList()
        }
    }
    
    /**
     * Searches products by name OR barcode.
     * 
     * Per SCREEN_LAYOUTS.md: Product Lookup Dialog supports search by name and barcode.
     */
    override suspend fun searchProducts(query: String): List<Product> = withContext(Dispatchers.IO) {
        try {
            if (query.isBlank()) {
                // Return all active products
                val dbQuery = QueryBuilder
                    .select(SelectResult.all())
                    .from(DataSource.collection(collection))
                    .where(
                        Expression.property("isActive").equalTo(Expression.booleanValue(true))
                            .and(Expression.property("isForSale").equalTo(Expression.booleanValue(true)))
                    )
                    .orderBy(Ordering.property("productName").ascending())
                
                return@withContext dbQuery.execute().use { resultSet ->
                    resultSet.allResults().mapNotNull { result ->
                        val dict = result.getDictionary(collection.name)
                        dict?.let { mapToProduct(it.toMap()) }
                    }
                }
            }
            
            // Search by name (LIKE)
            val nameResults = searchByName(query)
            
            // Search by barcode (exact match)
            val barcodeResult = getByBarcode(query)
            
            // Combine results, avoiding duplicates
            val combined = nameResults.toMutableList()
            barcodeResult?.let { product ->
                if (combined.none { it.branchProductId == product.branchProductId }) {
                    combined.add(0, product) // Add barcode match at the top
                }
            }
            
            combined
        } catch (e: Exception) {
            Log.e(TAG, "Error searching products '$query'", e)
            emptyList()
        }
    }
    
    /**
     * Gets all available lookup categories.
     * 
     * Per DATA_MODELS.md: Builds categories from product catalog.
     */
    override suspend fun getCategories(): List<LookupCategory> = withContext(Dispatchers.IO) {
        try {
            val query = QueryBuilder
                .select(
                    SelectResult.property("category"),
                    SelectResult.property("categoryName")
                )
                .from(DataSource.collection(collection))
                .where(
                    Expression.property("isActive").equalTo(Expression.booleanValue(true))
                        .and(Expression.property("isForSale").equalTo(Expression.booleanValue(true)))
                        .and(Expression.property("category").notNullOrMissing())
                )
                .groupBy(Expression.property("category"), Expression.property("categoryName"))
                .orderBy(Ordering.property("category").ascending())
            
            query.execute().use { resultSet ->
                resultSet.allResults().mapNotNull { result ->
                    val categoryId = result.getInt("category")
                    val categoryName = result.getString("categoryName") ?: "Unknown"
                    if (categoryId > 0) {
                        LookupCategory(
                            id = categoryId,
                            name = categoryName,
                            displayOrder = categoryId
                        )
                    } else null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting categories", e)
            emptyList()
        }
    }
    
    // ========================================================================
    // Document Mapping
    // ========================================================================
    
    /**
     * Maps a Couchbase document to a Product entity.
     * 
     * Per DATABASE_SCHEMA.md: Strict field mapping to ensure calculation engine works.
     */
    @Suppress("UNCHECKED_CAST")
    private fun mapToProduct(map: Map<String, Any?>): Product? {
        return try {
            val branchProductId = (map["branchProductId"] as? Number)?.toInt() ?: return null
            val productId = (map["productId"] as? Number)?.toInt() ?: branchProductId
            val productName = map["productName"] as? String ?: return null
            
            // Parse itemNumbers array
            val itemNumbers = (map["itemNumbers"] as? List<Map<String, Any?>>)?.map { itemMap ->
                ItemNumber(
                    itemNumber = itemMap["itemNumber"] as? String ?: "",
                    isPrimary = itemMap["isPrimary"] as? Boolean ?: false
                )
            } ?: emptyList()
            
            // Parse taxes array
            val taxes = (map["taxes"] as? List<Map<String, Any?>>)?.map { taxMap ->
                ProductTax(
                    taxId = (taxMap["taxId"] as? Number)?.toInt() ?: 0,
                    tax = taxMap["tax"] as? String ?: "",
                    percent = BigDecimal((taxMap["percent"] as? Number)?.toString() ?: "0")
                )
            } ?: emptyList()
            
            // Parse currentSale if present
            val currentSaleMap = map["currentSale"] as? Map<String, Any?>
            val currentSale = currentSaleMap?.let { saleMap ->
                ProductSale(
                    id = (saleMap["id"] as? Number)?.toInt() ?: 0,
                    retailPrice = BigDecimal((saleMap["retailPrice"] as? Number)?.toString() ?: "0"),
                    discountedPrice = BigDecimal((saleMap["discountedPrice"] as? Number)?.toString() ?: "0"),
                    discountAmount = BigDecimal((saleMap["discountAmount"] as? Number)?.toString() ?: "0"),
                    startDate = saleMap["startDate"] as? String ?: "",
                    endDate = saleMap["endDate"] as? String ?: ""
                )
            }
            
            Product(
                branchProductId = branchProductId,
                productId = productId,
                productName = productName,
                description = map["description"] as? String,
                category = (map["category"] as? Number)?.toInt(),
                categoryName = map["categoryName"] as? String,
                departmentId = (map["departmentId"] as? Number)?.toInt(),
                departmentName = map["departmentName"] as? String,
                retailPrice = BigDecimal((map["retailPrice"] as? Number)?.toString() ?: "0"),
                floorPrice = (map["floorPrice"] as? Number)?.let { BigDecimal(it.toString()) },
                cost = (map["cost"] as? Number)?.let { BigDecimal(it.toString()) },
                soldById = map["soldById"] as? String ?: "Quantity",
                soldByName = map["soldByName"] as? String ?: "Each",
                // Note: Schema uses isFoodStampEligible but we use isSnapEligible
                isSnapEligible = (map["isSnapEligible"] as? Boolean) 
                    ?: (map["isFoodStampEligible"] as? Boolean) 
                    ?: false,
                isActive = map["isActive"] as? Boolean ?: true,
                isForSale = map["isForSale"] as? Boolean ?: true,
                ageRestriction = map["ageRestriction"] as? String ?: "NO",
                order = (map["order"] as? Number)?.toInt() ?: 0,
                itemNumbers = itemNumbers,
                taxes = taxes,
                currentSale = currentSale,
                crvRatePerUnit = BigDecimal((map["crvRatePerUnit"] as? Number)?.toString() ?: "0"),
                crvId = (map["crvId"] as? Number)?.toInt()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error mapping document to Product", e)
            null
        }
    }
    
    // ========================================================================
    // Document Insertion (for seeding)
    // ========================================================================
    
    /**
     * Inserts a product document into the database.
     * Used by DebugDataSeeder.
     */
    suspend fun insertProduct(product: Product): Boolean = withContext(Dispatchers.IO) {
        try {
            val doc = MutableDocument(product.branchProductId.toString())
            
            doc.setInt("branchProductId", product.branchProductId)
            doc.setInt("productId", product.productId)
            doc.setString("productName", product.productName)
            product.description?.let { doc.setString("description", it) }
            product.category?.let { doc.setInt("category", it) }
            product.categoryName?.let { doc.setString("categoryName", it) }
            product.departmentId?.let { doc.setInt("departmentId", it) }
            product.departmentName?.let { doc.setString("departmentName", it) }
            doc.setDouble("retailPrice", product.retailPrice.toDouble())
            product.floorPrice?.let { doc.setDouble("floorPrice", it.toDouble()) }
            product.cost?.let { doc.setDouble("cost", it.toDouble()) }
            doc.setString("soldById", product.soldById)
            doc.setString("soldByName", product.soldByName)
            doc.setBoolean("isSnapEligible", product.isSnapEligible)
            doc.setBoolean("isFoodStampEligible", product.isSnapEligible) // For schema compatibility
            doc.setBoolean("isActive", product.isActive)
            doc.setBoolean("isForSale", product.isForSale)
            doc.setString("ageRestriction", product.ageRestriction)
            doc.setInt("order", product.order)
            doc.setDouble("crvRatePerUnit", product.crvRatePerUnit.toDouble())
            product.crvId?.let { doc.setInt("crvId", it) }
            
            // Set itemNumbers as array of maps
            val itemNumbersList = product.itemNumbers.map { itemNumber ->
                mapOf(
                    "itemNumber" to itemNumber.itemNumber,
                    "isPrimary" to itemNumber.isPrimary
                )
            }
            doc.setArray("itemNumbers", com.couchbase.lite.MutableArray(itemNumbersList))
            
            // Set taxes as array of maps
            val taxesList = product.taxes.map { tax ->
                mapOf(
                    "taxId" to tax.taxId,
                    "tax" to tax.tax,
                    "percent" to tax.percent.toDouble()
                )
            }
            doc.setArray("taxes", com.couchbase.lite.MutableArray(taxesList))
            
            // Set currentSale if present
            product.currentSale?.let { sale ->
                val saleMap = mapOf(
                    "id" to sale.id,
                    "retailPrice" to sale.retailPrice.toDouble(),
                    "discountedPrice" to sale.discountedPrice.toDouble(),
                    "discountAmount" to sale.discountAmount.toDouble(),
                    "startDate" to sale.startDate,
                    "endDate" to sale.endDate
                )
                doc.setDictionary("currentSale", com.couchbase.lite.MutableDictionary(saleMap))
            }
            
            collection.save(doc)
            Log.d(TAG, "Inserted product ${product.branchProductId} - ${product.productName}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting product ${product.branchProductId}", e)
            false
        }
    }
    
    /**
     * Gets the count of products in the collection.
     * Used by DebugDataSeeder to check if seeding is needed.
     */
    suspend fun getProductCount(): Long = withContext(Dispatchers.IO) {
        try {
            collection.count
        } catch (e: Exception) {
            Log.e(TAG, "Error getting product count", e)
            0L
        }
    }
}

