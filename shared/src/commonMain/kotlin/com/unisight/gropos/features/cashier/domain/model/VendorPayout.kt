package com.unisight.gropos.features.cashier.domain.model

import java.math.BigDecimal

/**
 * Represents a vendor payout record.
 * 
 * Per COUCHBASE_LOCAL_STORAGE.md: VendorPayout collection schema.
 * Tracks payments made to vendors from the till (lottery, inventory, etc.).
 * 
 * @property id Unique payout identifier
 * @property vendorId FK to Vendor
 * @property vendorName Vendor display name (denormalized)
 * @property amount Payout amount
 * @property payoutType Type of payout (LOTTERY, INVENTORY, OTHER)
 * @property branchId Branch where payout occurred
 * @property stationId Station/register that made the payout
 * @property employeeId Employee who processed the payout
 * @property employeeName Employee name (denormalized)
 * @property managerId Manager who approved (if required)
 * @property managerName Manager name (denormalized)
 * @property referenceNumber Optional reference/check number
 * @property notes Optional notes about the payout
 * @property payoutDateTime When the payout was made
 * @property isSynced Whether payout has been synced to server
 * @property createdDate Record creation timestamp
 */
data class VendorPayout(
    val id: Long,
    val vendorId: Int,
    val vendorName: String,
    val amount: BigDecimal,
    val payoutType: VendorPayoutType = VendorPayoutType.OTHER,
    val branchId: Int,
    val stationId: Int,
    val employeeId: Int,
    val employeeName: String? = null,
    val managerId: Int? = null,
    val managerName: String? = null,
    val referenceNumber: String? = null,
    val notes: String? = null,
    val payoutDateTime: String,
    val isSynced: Boolean = false,
    val createdDate: String? = null
)

/**
 * Types of vendor payouts.
 */
enum class VendorPayoutType(val displayName: String) {
    LOTTERY("Lottery Payout"),
    INVENTORY("Inventory/Stock Payment"),
    DELIVERY("Delivery Payment"),
    REFUND("Vendor Refund"),
    OTHER("Other");
    
    companion object {
        /**
         * Parses a string to VendorPayoutType.
         */
        fun fromString(value: String?): VendorPayoutType {
            return when (value?.uppercase()) {
                "LOTTERY" -> LOTTERY
                "INVENTORY" -> INVENTORY
                "DELIVERY" -> DELIVERY
                "REFUND" -> REFUND
                else -> OTHER
            }
        }
    }
}

