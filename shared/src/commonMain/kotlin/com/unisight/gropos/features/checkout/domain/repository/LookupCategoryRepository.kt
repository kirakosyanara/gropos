package com.unisight.gropos.features.checkout.domain.repository

import com.unisight.gropos.features.checkout.domain.model.LookupCategoryWithItems
import com.unisight.gropos.features.checkout.domain.model.LookupProduct

/**
 * Repository for Lookup Categories (PosLookupCategory collection).
 * 
 * Per LOOKUP_TABLE.md - Data Storage in Couchbase:
 * - Database: unisight
 * - Scope: pos
 * - Collection: PosLookupCategory
 * 
 * This is separate from ProductRepository categories.
 * Lookup categories are explicitly configured "quick buttons" 
 * for fast product access, not auto-derived from products.
 */
interface LookupCategoryRepository {
    
    /**
     * Gets all lookup categories with their items.
     * 
     * Per LOOKUP_TABLE.md: Returns categories ordered by 'order' field.
     * 
     * @return List of lookup categories with embedded items
     */
    suspend fun getAllCategories(): List<LookupCategoryWithItems>
    
    /**
     * Gets a single lookup category by ID.
     * 
     * @param id Category ID
     * @return The category with items, or null if not found
     */
    suspend fun getCategoryById(id: Int): LookupCategoryWithItems?
    
    /**
     * Gets products for a specific category.
     * 
     * @param categoryId The category ID
     * @return List of products in the category, ordered by 'order' field
     */
    suspend fun getProductsByCategory(categoryId: Int): List<LookupProduct>
    
    /**
     * Saves a lookup category to the database (upsert).
     * 
     * Per LOOKUP_TABLE.md: Uses ConcurrencyControl.LAST_WRITE_WINS for upsert.
     * 
     * @param category The category to save
     * @return true if successful
     */
    suspend fun saveCategory(category: LookupCategoryWithItems): Boolean
    
    /**
     * Saves multiple categories in a batch.
     * 
     * @param categories List of categories to save
     * @return Number of successfully saved categories
     */
    suspend fun saveCategories(categories: List<LookupCategoryWithItems>): Int
    
    /**
     * Deletes a category by ID.
     * 
     * @param id Category ID to delete
     * @return true if deleted
     */
    suspend fun deleteCategory(id: Int): Boolean
    
    /**
     * Gets total count of categories.
     * 
     * @return Number of categories in the database
     */
    suspend fun getCategoryCount(): Long
    
    /**
     * Clears all lookup categories.
     * 
     * Used when wiping database.
     */
    suspend fun clearAll()
}

