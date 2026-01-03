package com.unisight.gropos.features.pricing.domain.repository

import com.unisight.gropos.features.pricing.domain.model.Crv

/**
 * Repository interface for CRV (California Redemption Value) definitions.
 * 
 * Per COUCHBASE_LOCAL_STORAGE.md: Reads from CRV collection in the `pos` scope.
 * Used for container deposit rate lookups.
 */
interface CrvRepository {
    
    /**
     * Gets all CRV tiers.
     * 
     * @return List of all CRV definitions
     */
    suspend fun getAllCrvRates(): List<Crv>
    
    /**
     * Gets a CRV tier by ID.
     * Used for resolving product.crvId field.
     * 
     * @param crvId The CRV ID to look up
     * @return The CRV definition, or null if not found
     */
    suspend fun getCrvById(crvId: Int): Crv?
    
    /**
     * Gets the default CRV rate for containers under 24oz.
     * 
     * @return The default small container CRV, or null if not found
     */
    suspend fun getDefaultSmallContainerCrv(): Crv?
    
    /**
     * Gets the default CRV rate for containers 24oz and over.
     * 
     * @return The default large container CRV, or null if not found
     */
    suspend fun getDefaultLargeContainerCrv(): Crv?
}

