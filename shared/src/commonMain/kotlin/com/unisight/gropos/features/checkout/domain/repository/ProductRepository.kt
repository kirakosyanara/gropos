package com.unisight.gropos.features.checkout.domain.repository

import com.unisight.gropos.features.checkout.domain.model.Product

/**
 * Repository interface for product data access.
 * 
 * Per DATABASE_SCHEMA.md - Kotlin Multiplatform Equivalent section.
 * Method signatures match the schema exactly.
 * 
 * Defined in Domain layer, implemented in Data layer.
 * Per Clean Architecture: Domain defines the contract,
 * Data provides the implementation (CouchbaseLite, API, etc.).
 */
interface ProductRepository {
    
    /**
     * Finds a product by barcode (item number).
     * 
     * Per DATABASE_SCHEMA.md: Query checks if barcode exists in itemNumbers array.
     * Uses ArrayExpression.any() to match against itemNumbers[].itemNumber.
     * 
     * @param barcode The barcode/item number to search for
     * @return Product if found, null otherwise
     */
    suspend fun getByBarcode(barcode: String): Product?
    
    /**
     * Finds products by category.
     * 
     * Per DATABASE_SCHEMA.md: Query by category ID, ordered by order field.
     * 
     * @param categoryId The category ID to filter by
     * @return List of products in the category
     */
    suspend fun getByCategory(categoryId: Int): List<Product>
    
    /**
     * Finds a product by its branchProductId.
     * 
     * Per DATABASE_SCHEMA.md: Document ID is branchProductId.
     * 
     * @param branchProductId The product's unique identifier
     * @return Product if found, null otherwise
     */
    suspend fun getById(branchProductId: Int): Product?
    
    /**
     * Searches products by name (partial match).
     * 
     * Per DATABASE_SCHEMA.md: Uses full-text index on productName.
     * 
     * @param query The search query
     * @return List of matching products
     */
    suspend fun searchByName(query: String): List<Product>
    
    /**
     * Searches products by name OR barcode.
     * 
     * Per SCREEN_LAYOUTS.md: Product Lookup Dialog supports search by name and barcode.
     * This is the main search method for the lookup dialog.
     * 
     * @param query The search query (can be product name or barcode)
     * @return List of matching products
     */
    suspend fun searchProducts(query: String): List<Product>
    
    /**
     * Gets all available lookup categories.
     * 
     * Per DATA_MODELS.md: LookupGroupViewModel used for category navigation.
     * 
     * @return List of categories with their products
     */
    suspend fun getCategories(): List<LookupCategory>
    
    /**
     * Inserts or updates a product in the local database.
     * 
     * Per SYNC_MECHANISM.md: Used for initial data load and ongoing sync.
     * 
     * @param product The product to insert/update
     * @return true if successful, false otherwise
     */
    suspend fun insertProduct(product: Product): Boolean
    
    /**
     * Gets the total count of products in the database.
     * 
     * Used to check if initial sync is needed.
     * 
     * @return Number of products in the database
     */
    suspend fun getProductCount(): Long
}

/**
 * Lookup category for product navigation grid.
 * 
 * Per DATA_MODELS.md: LookupGroupViewModel structure.
 */
data class LookupCategory(
    val id: Int,
    val name: String,
    val imageUrl: String? = null,
    val displayOrder: Int = 0
)

/**
 * Error thrown when a product cannot be found.
 */
class ProductNotFoundException(val barcode: String) : Exception("Product not found: $barcode")
