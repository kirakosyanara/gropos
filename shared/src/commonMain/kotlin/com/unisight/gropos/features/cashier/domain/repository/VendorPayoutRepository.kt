package com.unisight.gropos.features.cashier.domain.repository

import com.unisight.gropos.features.cashier.domain.model.VendorPayout
import java.math.BigDecimal

/**
 * Repository interface for vendor payout records.
 * 
 * Per COUCHBASE_LOCAL_STORAGE.md: Reads/writes to VendorPayout collection in the `pos` scope.
 * Used for tracking vendor payments made from the till.
 */
interface VendorPayoutRepository {
    
    /**
     * Saves a vendor payout record.
     * 
     * @param payout The payout to save
     * @return Result indicating success or failure
     */
    suspend fun savePayout(payout: VendorPayout): Result<Unit>
    
    /**
     * Gets a payout by ID.
     * 
     * @param payoutId The payout ID
     * @return The payout, or null if not found
     */
    suspend fun getPayoutById(payoutId: Long): VendorPayout?
    
    /**
     * Gets all payouts for a specific date range.
     * 
     * @param startDate Start of range (ISO-8601)
     * @param endDate End of range (ISO-8601)
     * @return List of payouts in the range
     */
    suspend fun getPayoutsForDateRange(startDate: String, endDate: String): List<VendorPayout>
    
    /**
     * Gets payouts for a specific vendor.
     * 
     * @param vendorId The vendor ID
     * @param limit Maximum number of results
     * @return List of payouts for the vendor
     */
    suspend fun getPayoutsForVendor(vendorId: Int, limit: Int = 50): List<VendorPayout>
    
    /**
     * Gets payouts for a specific station/register.
     * 
     * @param stationId The station ID
     * @param limit Maximum number of results
     * @return List of payouts for the station
     */
    suspend fun getPayoutsForStation(stationId: Int, limit: Int = 50): List<VendorPayout>
    
    /**
     * Gets the total payout amount for today.
     * 
     * @param stationId Optional station filter
     * @return Total payout amount
     */
    suspend fun getTodayPayoutTotal(stationId: Int? = null): BigDecimal
    
    /**
     * Gets unsynchronized payouts.
     * 
     * @return List of payouts pending sync
     */
    suspend fun getUnsyncedPayouts(): List<VendorPayout>
    
    /**
     * Marks a payout as synchronized.
     * 
     * @param payoutId The payout ID
     * @return Result indicating success or failure
     */
    suspend fun markAsSynced(payoutId: Long): Result<Unit>
}

