package com.unisight.gropos.features.cashier.data

import com.unisight.gropos.features.cashier.domain.model.Vendor
import com.unisight.gropos.features.cashier.domain.repository.VendorRepository

/**
 * Fake implementation of VendorRepository for development/testing.
 * 
 * Per FUNCTIONS_MENU.md:
 * - Provides seeded vendor data for the Vendor Payout flow
 * - In production, would be replaced with network repository
 */
class FakeVendorRepository : VendorRepository {
    
    /**
     * Seeded vendor list per requirement:
     * "Coca-Cola", "Pepsi", "Frito-Lay", "Local Bakery"
     */
    private val vendors = listOf(
        Vendor(id = "vendor_001", name = "Coca-Cola"),
        Vendor(id = "vendor_002", name = "Pepsi"),
        Vendor(id = "vendor_003", name = "Frito-Lay"),
        Vendor(id = "vendor_004", name = "Local Bakery")
    )
    
    override suspend fun getVendors(): List<Vendor> {
        return vendors
    }
    
    override suspend fun getVendorById(vendorId: String): Vendor? {
        return vendors.find { it.id == vendorId }
    }
}

