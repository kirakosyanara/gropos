package com.unisight.gropos.features.pricing.domain.repository

import com.unisight.gropos.features.pricing.domain.model.Tax

/**
 * Repository interface for tax definitions.
 * 
 * Per COUCHBASE_LOCAL_STORAGE.md: Reads from Tax collection in the `pos` scope.
 * Used for standalone tax rate lookups and real-time tax updates.
 */
interface TaxRepository {
    
    /**
     * Gets all active taxes.
     * 
     * @return List of all tax definitions
     */
    suspend fun getAllTaxes(): List<Tax>
    
    /**
     * Gets a tax by ID.
     * 
     * @param taxId The tax ID to look up
     * @return The tax definition, or null if not found
     */
    suspend fun getTaxById(taxId: Int): Tax?
    
    /**
     * Gets a tax by name.
     * 
     * @param name The tax name to search for (case-insensitive)
     * @return The tax definition, or null if not found
     */
    suspend fun getTaxByName(name: String): Tax?
    
    /**
     * Gets taxes by their IDs (batch lookup).
     * Used for resolving product.taxes array efficiently.
     * 
     * @param taxIds List of tax IDs to look up
     * @return List of matching tax definitions
     */
    suspend fun getTaxesByIds(taxIds: List<Int>): List<Tax>
}

