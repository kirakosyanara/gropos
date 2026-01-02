package com.unisight.gropos.features.cashier.domain.repository

import com.unisight.gropos.features.cashier.domain.model.Vendor

/**
 * Repository interface for vendor operations.
 * 
 * Per FUNCTIONS_MENU.md (Vendor Payout section):
 * - Provides list of vendors for payout selection
 * - In production, would fetch from backend API
 */
interface VendorRepository {
    
    /**
     * Retrieves all available vendors for payout.
     * 
     * @return List of vendors that can receive payouts
     */
    suspend fun getVendors(): List<Vendor>
    
    /**
     * Retrieves a vendor by ID.
     * 
     * @param vendorId The vendor ID to look up
     * @return The vendor if found, null otherwise
     */
    suspend fun getVendorById(vendorId: String): Vendor?
}

