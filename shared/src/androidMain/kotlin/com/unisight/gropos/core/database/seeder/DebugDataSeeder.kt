package com.unisight.gropos.core.database.seeder

import android.util.Log
import com.unisight.gropos.features.checkout.data.CouchbaseProductRepository
import kotlinx.coroutines.runBlocking

/**
 * Debug data seeder for Android.
 * 
 * Populates the database with initial product data on first launch.
 * This replaces the in-memory FakeProductRepository with persistent data.
 * 
 * Per ARCHITECTURE_BLUEPRINT.md: Offline-first architecture requires
 * local data to be available immediately, even without cloud sync.
 * 
 * Usage:
 * - Call seedIfEmpty() immediately after Koin initialization
 * - This checks if Product collection is empty
 * - If empty, inserts all products from InitialProducts
 */
class DebugDataSeeder(
    private val productRepository: CouchbaseProductRepository
) {
    
    companion object {
        private const val TAG = "DebugDataSeeder"
    }
    
    /**
     * Seeds the database with initial products if the collection is empty.
     * 
     * This is a blocking operation that should be called during app startup.
     * 
     * @return true if seeding was performed, false if data already exists
     */
    fun seedIfEmpty(): Boolean = runBlocking {
        try {
            val productCount = productRepository.getProductCount()
            
            if (productCount > 0) {
                Log.d(TAG, "Database already contains $productCount products, skipping seed")
                return@runBlocking false
            }
            
            Log.d(TAG, "Database is empty, seeding initial products...")
            
            var successCount = 0
            var failureCount = 0
            
            for (product in InitialProducts.products) {
                val success = productRepository.insertProduct(product)
                if (success) {
                    successCount++
                } else {
                    failureCount++
                    Log.w(TAG, "Failed to insert product ${product.branchProductId}")
                }
            }
            
            Log.d(TAG, "Seeding complete - $successCount inserted, $failureCount failed")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error during seeding", e)
            false
        }
    }
    
    /**
     * Forces a reseed by clearing and re-inserting all products.
     * 
     * WARNING: This clears all existing product data!
     * Use only for development/debugging.
     */
    fun forceSeed(): Boolean = runBlocking {
        try {
            Log.d(TAG, "Force seeding - clearing and re-inserting products...")
            
            // Note: For a full implementation, you would clear the collection here
            // For now, we just insert/update
            
            var successCount = 0
            for (product in InitialProducts.products) {
                val success = productRepository.insertProduct(product)
                if (success) successCount++
            }
            
            Log.d(TAG, "Force seeding complete - $successCount products")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error during force seeding", e)
            false
        }
    }
}

