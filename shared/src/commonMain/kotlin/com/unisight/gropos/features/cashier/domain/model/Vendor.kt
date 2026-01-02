package com.unisight.gropos.features.cashier.domain.model

/**
 * Represents a vendor for payout operations.
 * 
 * Per FUNCTIONS_MENU.md (Vendor Payout section):
 * - Vendors are external suppliers who can receive cash payouts
 * - Used in the Vendor Payout flow for selecting payout recipients
 * 
 * @property id Unique identifier for the vendor
 * @property name Display name of the vendor
 */
data class Vendor(
    val id: String,
    val name: String
)

