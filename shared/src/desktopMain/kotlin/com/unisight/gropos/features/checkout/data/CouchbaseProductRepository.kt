package com.unisight.gropos.features.checkout.data

import com.couchbase.lite.ArrayExpression
import com.couchbase.lite.Collection
import com.couchbase.lite.ConcurrencyControl
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
import com.unisight.gropos.features.checkout.data.dto.LegacyProductDto
import com.unisight.gropos.features.checkout.domain.model.Product
import com.unisight.gropos.features.checkout.domain.repository.LookupCategory
import com.unisight.gropos.features.checkout.domain.repository.ProductRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * CouchbaseLite implementation of ProductRepository for Desktop (JVM).
 * 
 * Per COUCHBASE_LOCAL_STORAGE.md:
 * - Legacy Collection: Product in scope "pos"
 * - Query by barcode: ArrayExpression on itemNumbers array
 * - Query by category: Expression.property("categoryId")
 * 
 * Per BACKEND_INTEGRATION_STATUS.md:
 * - Uses LegacyProductDto for parsing legacy JSON structure
 * - Handles field renames (name -> productName, categoryId -> category, etc.)
 * - Reads from legacy "pos" scope, writes to both for migration
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
    
    /**
     * Legacy collection in "pos" scope (per COUCHBASE_LOCAL_STORAGE.md).
     * This is where the backend syncs product data.
     */
    private val legacyCollection: Collection by lazy {
        val db = databaseProvider.getTypedDatabase()
        db.createCollection(
            DatabaseConfig.COLLECTION_PRODUCT,
            DatabaseConfig.SCOPE_POS
        )
    }
    
    /**
     * New collection in "base_data" scope for migrated data.
     * Used for write operations during migration period.
     */
    private val newCollection: Collection by lazy {
        val db = databaseProvider.getTypedDatabase()
        db.createCollection(
            DatabaseConfig.COLLECTION_PRODUCT,
            DatabaseConfig.SCOPE_BASE_DATA
        )
    }
    
    /**
     * Primary collection for reads - prefer legacy, fallback to new.
     */
    private val collection: Collection
        get() = if (legacyCollection.count > 0) legacyCollection else newCollection
    
    /**
     * Finds a product by barcode.
     * 
     * Per COUCHBASE_LOCAL_STORAGE.md - Query by barcode (array contains):
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
                    dict?.let { parseProductDocument(it.toMap()) }
                }
            }
        } catch (e: Exception) {
            println("CouchbaseProductRepository: Error getting product by barcode '$barcode' - ${e.message}")
            null
        }
    }
    
    /**
     * Finds products by category.
     * 
     * Per COUCHBASE_LOCAL_STORAGE.md: 
     * - Legacy uses "categoryId" field
     * - Query by category, ordered by order field.
     */
    override suspend fun getByCategory(categoryId: Int): List<Product> = withContext(Dispatchers.IO) {
        try {
            // Try legacy field name first (categoryId), then new (category)
            val categoryExpression = Expression.property("categoryId")
                .equalTo(Expression.intValue(categoryId))
                .or(Expression.property("category").equalTo(Expression.intValue(categoryId)))
            
            // Status check - legacy uses statusId="Active", new uses isActive=true
            val statusExpression = Expression.property("statusId")
                .equalTo(Expression.string("Active"))
                .or(Expression.property("isActive").equalTo(Expression.booleanValue(true)))
            
            val query = QueryBuilder
                .select(SelectResult.all())
                .from(DataSource.collection(collection))
                .where(categoryExpression.and(statusExpression))
                .orderBy(Ordering.property("order").ascending())
            
            query.execute().use { resultSet ->
                resultSet.allResults().mapNotNull { result ->
                    val dict = result.getDictionary(collection.name)
                    dict?.let { parseProductDocument(it.toMap()) }
                }
            }
        } catch (e: Exception) {
            println("CouchbaseProductRepository: Error getting products by category $categoryId - ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Finds a product by branchProductId.
     * 
     * Per COUCHBASE_LOCAL_STORAGE.md: Document ID is branchProductId.
     */
    override suspend fun getById(branchProductId: Int): Product? = withContext(Dispatchers.IO) {
        try {
            val doc = collection.getDocument(branchProductId.toString())
            doc?.let { parseProductDocument(it.toMap()) }
        } catch (e: Exception) {
            println("CouchbaseProductRepository: Error getting product by ID $branchProductId - ${e.message}")
            null
        }
    }
    
    /**
     * Finds a product by productId (master product ID).
     * 
     * Per LOOKUP_TABLE.md: Lookup items reference products by productId field,
     * which may be different from branchProductId (document ID).
     */
    override suspend fun getByProductId(productId: Int): Product? = withContext(Dispatchers.IO) {
        try {
            val query = QueryBuilder
                .select(SelectResult.all())
                .from(DataSource.collection(collection))
                .where(Expression.property("productId").equalTo(Expression.intValue(productId)))
            
            query.execute().use { resultSet ->
                resultSet.allResults().firstOrNull()?.let { result ->
                    val dict = result.getDictionary(collection.name)
                    dict?.let { parseProductDocument(it.toMap()) }
                }
            }
        } catch (e: Exception) {
            println("CouchbaseProductRepository: Error getting product by productId $productId - ${e.message}")
            null
        }
    }
    
    /**
     * Searches products by name.
     * 
     * Per COUCHBASE_LOCAL_STORAGE.md: 
     * - Legacy uses "name" field
     * - New uses "productName" field
     */
    override suspend fun searchByName(query: String): List<Product> = withContext(Dispatchers.IO) {
        try {
            val likePattern = "%${query}%"
            
            // Search both legacy "name" and new "productName" fields
            val nameExpression = Expression.property("name")
                .like(Expression.string(likePattern))
                .or(Expression.property("productName").like(Expression.string(likePattern)))
            
            // Status check - legacy uses statusId="Active", new uses isActive=true
            val statusExpression = Expression.property("statusId")
                .equalTo(Expression.string("Active"))
                .or(Expression.property("isActive").equalTo(Expression.booleanValue(true)))
            
            val dbQuery = QueryBuilder
                .select(SelectResult.all())
                .from(DataSource.collection(collection))
                .where(nameExpression.and(statusExpression))
                .orderBy(
                    Ordering.property("name").ascending(),
                    Ordering.property("productName").ascending()
                )
            
            dbQuery.execute().use { resultSet ->
                resultSet.allResults().mapNotNull { result ->
                    val dict = result.getDictionary(collection.name)
                    dict?.let { parseProductDocument(it.toMap()) }
                }
            }
        } catch (e: Exception) {
            println("CouchbaseProductRepository: Error searching products by name '$query' - ${e.message}")
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
                val statusExpression = Expression.property("statusId")
                    .equalTo(Expression.string("Active"))
                    .or(Expression.property("isActive").equalTo(Expression.booleanValue(true)))
                
                val dbQuery = QueryBuilder
                    .select(SelectResult.all())
                    .from(DataSource.collection(collection))
                    .where(statusExpression)
                    .orderBy(
                        Ordering.property("name").ascending(),
                        Ordering.property("productName").ascending()
                    )
                
                return@withContext dbQuery.execute().use { resultSet ->
                    resultSet.allResults().mapNotNull { result ->
                        val dict = result.getDictionary(collection.name)
                        dict?.let { parseProductDocument(it.toMap()) }
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
            println("CouchbaseProductRepository: Error searching products '$query' - ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Gets all available lookup categories.
     * 
     * Per COUCHBASE_LOCAL_STORAGE.md: 
     * - Legacy uses "categoryId" and "category" (denormalized name)
     * - New uses "category" (id) and "categoryName"
     */
    override suspend fun getCategories(): List<LookupCategory> = withContext(Dispatchers.IO) {
        try {
            val query = QueryBuilder
                .select(
                    SelectResult.property("categoryId"),
                    SelectResult.property("category")
                )
                .from(DataSource.collection(collection))
                .where(
                    Expression.property("statusId").equalTo(Expression.string("Active"))
                        .or(Expression.property("isActive").equalTo(Expression.booleanValue(true)))
                )
                .groupBy(Expression.property("categoryId"), Expression.property("category"))
                .orderBy(Ordering.property("categoryId").ascending())
            
            query.execute().use { resultSet ->
                resultSet.allResults().mapNotNull { result ->
                    // Legacy: categoryId is Int, category is String (name)
                    val categoryId = result.getInt("categoryId")
                    val categoryName = result.getString("category") ?: "Unknown"
                    
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
            println("CouchbaseProductRepository: Error getting categories - ${e.message}")
            emptyList()
        }
    }
    
    // ========================================================================
    // Document Parsing - Uses LegacyProductDto for proper field mapping
    // ========================================================================
    
    /**
     * Parses a Couchbase document to a Product entity.
     * 
     * Uses LegacyProductDto to handle field renames and transformations
     * per BACKEND_INTEGRATION_STATUS.md field mapping reference.
     */
    @Suppress("UNCHECKED_CAST")
    private fun parseProductDocument(map: Map<String, Any?>): Product? {
        // Use the DTO's fromMap for legacy documents
        val dto = LegacyProductDto.fromMap(map)
        if (dto != null) {
            return dto.toDomain()
        }
        
        // Fallback: Try to parse as new format (already uses domain field names)
        return parseNewFormatProduct(map)
    }
    
    /**
     * Fallback parser for documents already in new format.
     */
    @Suppress("UNCHECKED_CAST")
    private fun parseNewFormatProduct(map: Map<String, Any?>): Product? {
        return try {
            val branchProductId = (map["branchProductId"] as? Number)?.toInt() ?: return null
            val productId = (map["productId"] as? Number)?.toInt() ?: branchProductId
            val productName = map["productName"] as? String ?: return null
            
            // Parse itemNumbers array
            val itemNumbers = (map["itemNumbers"] as? List<Map<String, Any?>>)?.map { itemMap ->
                com.unisight.gropos.features.checkout.domain.model.ItemNumber(
                    itemNumber = itemMap["itemNumber"] as? String ?: "",
                    isPrimary = itemMap["isPrimary"] as? Boolean ?: false
                )
            } ?: emptyList()
            
            // Parse taxes array
            val taxes = (map["taxes"] as? List<Map<String, Any?>>)?.map { taxMap ->
                com.unisight.gropos.features.checkout.domain.model.ProductTax(
                    taxId = (taxMap["taxId"] as? Number)?.toInt() ?: 0,
                    tax = taxMap["tax"] as? String ?: "",
                    percent = java.math.BigDecimal((taxMap["percent"] as? Number)?.toString() ?: "0")
                )
            } ?: emptyList()
            
            // Parse currentSale if present
            val currentSaleMap = map["currentSale"] as? Map<String, Any?>
            val currentSale = currentSaleMap?.let { saleMap ->
                com.unisight.gropos.features.checkout.domain.model.ProductSale(
                    id = (saleMap["id"] as? Number)?.toInt() ?: 0,
                    retailPrice = java.math.BigDecimal((saleMap["retailPrice"] as? Number)?.toString() ?: "0"),
                    discountedPrice = java.math.BigDecimal((saleMap["discountedPrice"] as? Number)?.toString() ?: "0"),
                    discountAmount = java.math.BigDecimal((saleMap["discountAmount"] as? Number)?.toString() ?: "0"),
                    startDate = saleMap["startDate"] as? String ?: "",
                    endDate = saleMap["endDate"] as? String ?: ""
                )
            }
            
            Product(
                branchProductId = branchProductId,
                productId = productId,
                productName = productName,
                description = map["description"] as? String,
                brand = map["brand"] as? String,
                category = (map["category"] as? Number)?.toInt(),
                categoryName = map["categoryName"] as? String,
                departmentId = (map["departmentId"] as? Number)?.toInt(),
                departmentName = map["departmentName"] as? String,
                retailPrice = java.math.BigDecimal((map["retailPrice"] as? Number)?.toString() ?: "0"),
                floorPrice = (map["floorPrice"] as? Number)?.let { java.math.BigDecimal(it.toString()) },
                cost = (map["cost"] as? Number)?.let { java.math.BigDecimal(it.toString()) },
                soldById = map["soldById"] as? String ?: "Quantity",
                soldByName = map["soldByName"] as? String ?: "Each",
                unitSize = (map["unitSize"] as? Number)?.let { java.math.BigDecimal(it.toString()) },
                isSnapEligible = (map["isSnapEligible"] as? Boolean) 
                    ?: (map["isFoodStampEligible"] as? Boolean) 
                    ?: false,
                isActive = map["isActive"] as? Boolean ?: true,
                isForSale = map["isForSale"] as? Boolean ?: true,
                ageRestriction = (map["ageRestriction"] as? Number)?.toInt(),
                order = (map["order"] as? Number)?.toInt() ?: 0,
                itemNumbers = itemNumbers,
                taxes = taxes,
                currentSale = currentSale,
                crvRatePerUnit = java.math.BigDecimal((map["crvRatePerUnit"] as? Number)?.toString() ?: "0"),
                crvId = (map["crvId"] as? Number)?.toInt(),
                qtyLimitPerCustomer = (map["qtyLimitPerCustomer"] as? Number)?.let { java.math.BigDecimal(it.toString()) },
                receiptName = map["receiptName"] as? String,
                returnPolicyId = map["returnPolicyId"] as? String,
                primaryImageUrl = map["primaryImageUrl"] as? String,
                createdDate = map["createdDate"] as? String,
                updatedDate = map["updatedDate"] as? String
            )
        } catch (e: Exception) {
            println("CouchbaseProductRepository: Error parsing new format document - ${e.message}")
            null
        }
    }
    
    // ========================================================================
    // Document Insertion - Writes in LEGACY format for backend compatibility
    // ========================================================================
    
    /**
     * Upserts a product document into the database.
     * 
     * Uses ConcurrencyControl.LAST_WRITE_WINS to implement true upsert behavior:
     * - Creates new document if it doesn't exist
     * - Updates existing document if it does exist (no conflict errors)
     * 
     * **Per BACKEND_INTEGRATION_STATUS.md:**
     * Writes using LEGACY field names for backend compatibility:
     * - productName -> name
     * - category -> categoryId (for the ID)
     * - categoryName -> category (for the name)
     * - isSnapEligible -> foodStampable
     * - ageRestriction -> ageRestrictionId (converted to enum string)
     * - isActive -> statusId (converted to "Active"/"Inactive")
     * 
     * Used by DebugDataSeeder and ProductSyncService.
     */
    override suspend fun insertProduct(product: Product): Boolean = withContext(Dispatchers.IO) {
        try {
            val doc = MutableDocument(product.branchProductId.toString())
            
            // Primary identifiers - Legacy: id, branchProductId
            doc.setInt("id", product.productId)
            doc.setInt("branchProductId", product.branchProductId)
            
            // Display info - Legacy: name, brand, receiptName
            doc.setString("name", product.productName)
            product.brand?.let { doc.setString("brand", it) }
            product.description?.let { doc.setString("description", it) }
            product.receiptName?.let { doc.setString("receiptName", it) }
            
            // Category - Legacy: categoryId (Int), category (String name)
            product.category?.let { doc.setInt("categoryId", it) }
            product.categoryName?.let { doc.setString("category", it) }
            product.departmentId?.let { doc.setInt("departmentId", it) }
            product.departmentName?.let { doc.setString("departmentName", it) }
            
            // Pricing
            doc.setDouble("retailPrice", product.retailPrice.toDouble())
            product.floorPrice?.let { doc.setDouble("floorPrice", it.toDouble()) }
            product.cost?.let { doc.setDouble("cost", it.toDouble()) }
            
            // Unit/Quantity - Legacy: unitSize, unitTypeId (= soldByName), soldById
            doc.setString("soldById", product.soldById)
            doc.setString("unitTypeId", product.soldByName)
            product.unitSize?.let { doc.setDouble("unitSize", it.toDouble()) }
            product.qtyLimitPerCustomer?.let { doc.setDouble("qtyLimitPerCustomer", it.toDouble()) }
            
            // Compliance - Legacy: foodStampable, ageRestrictionId (enum), statusId (enum)
            doc.setBoolean("foodStampable", product.isSnapEligible)
            doc.setString("statusId", if (product.isActive) "Active" else "Inactive")
            product.ageRestriction?.let { age ->
                val ageRestrictionId = when (age) {
                    21 -> "Age21"
                    18 -> "Age18"
                    else -> "None"
                }
                doc.setString("ageRestrictionId", ageRestrictionId)
            } ?: doc.setString("ageRestrictionId", "None")
            
            product.returnPolicyId?.let { doc.setString("returnPolicyId", it) }
            
            // CRV
            product.crvId?.let { doc.setInt("crvId", it) }
            
            // Display order
            doc.setInt("order", product.order)
            
            // Image
            product.primaryImageUrl?.let { doc.setString("primaryImageUrl", it) }
            
            // Set primary item number for legacy compatibility
            product.primaryItemNumber?.let { doc.setString("primaryItemNumber", it) }
            
            // Set itemNumbers as array of maps - Legacy format includes id field
            val itemNumbersList = product.itemNumbers.mapIndexed { index, itemNumber ->
                mapOf(
                    "id" to (index + 1),
                    "itemNumber" to itemNumber.itemNumber,
                    "isPrimary" to itemNumber.isPrimary
                )
            }
            doc.setArray("itemNumbers", MutableArray(itemNumbersList))
            
            // Set taxes as array of maps - Legacy format includes id, productId
            val taxesList = product.taxes.mapIndexed { index, tax ->
                mapOf(
                    "id" to (index + 1),
                    "taxId" to tax.taxId,
                    "productId" to product.productId,
                    "tax" to tax.tax,
                    "percent" to tax.percent.toDouble()
                )
            }
            doc.setArray("taxes", MutableArray(taxesList))
            
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
                doc.setDictionary("currentSale", MutableDictionary(saleMap))
            }
            
            // Audit timestamps
            product.createdDate?.let { doc.setString("createdDate", it) }
            product.updatedDate?.let { doc.setString("updatedDate", it) }
            
            // Use LAST_WRITE_WINS to implement upsert - avoids LiteCoreException: conflict
            // This ensures the new data always wins, whether inserting or updating
            legacyCollection.save(doc, ConcurrencyControl.LAST_WRITE_WINS)
            println("CouchbaseProductRepository: Upserted product ${product.branchProductId} - ${product.productName}")
            true
        } catch (e: Exception) {
            println("CouchbaseProductRepository: Error upserting product ${product.branchProductId} - ${e.message}")
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Gets the count of products in the collection.
     * Used by DebugDataSeeder and ProductSyncService to check if seeding is needed.
     */
    override suspend fun getProductCount(): Long = withContext(Dispatchers.IO) {
        try {
            collection.count
        } catch (e: Exception) {
            println("CouchbaseProductRepository: Error getting product count - ${e.message}")
            0L
        }
    }
}
