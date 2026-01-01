package com.unisight.gropos.features.checkout.domain.repository

import com.unisight.gropos.features.checkout.domain.model.Product

/**
 * Repository interface for product data access.
 * 
 * Defined in Domain layer, implemented in Data layer.
 * Per Clean Architecture: Domain defines the contract,
 * Data provides the implementation (CouchbaseLite, API, etc.).
 */
interface ProductRepository {
    
    /**
     * Finds a product by its SKU (barcode).
     * 
     * @param sku The Stock Keeping Unit / barcode to search for
     * @return Result containing the Product if found, or an error
     */
    suspend fun findBySku(sku: String): Result<Product>
    
    /**
     * Finds a product by its unique ID.
     * 
     * @param id The product's unique identifier
     * @return Result containing the Product if found, or an error
     */
    suspend fun findById(id: String): Result<Product>
    
    /**
     * Searches products by name (partial match).
     * 
     * @param query The search query
     * @return List of matching products
     */
    suspend fun searchByName(query: String): List<Product>
}

/**
 * Error thrown when a product cannot be found.
 */
class ProductNotFoundException(val sku: String) : Exception("Product not found: $sku")

