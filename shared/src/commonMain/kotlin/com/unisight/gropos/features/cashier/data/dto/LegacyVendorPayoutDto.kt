package com.unisight.gropos.features.cashier.data.dto

import com.unisight.gropos.features.cashier.domain.model.VendorPayout
import com.unisight.gropos.features.cashier.domain.model.VendorPayoutType
import java.math.BigDecimal

/**
 * DTO for mapping legacy VendorPayout JSON from Couchbase to domain model.
 * 
 * Per COUCHBASE_LOCAL_STORAGE.md: VendorPayout collection schema.
 * Per BACKEND_INTEGRATION_STATUS.md: Maps from legacy `pos` scope.
 */
data class LegacyVendorPayoutDto(
    val id: Long,
    val vendorId: Int,
    val vendorName: String,
    val amount: BigDecimal,
    val payoutType: String? = null,
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
) {
    /**
     * Converts this DTO to a domain model.
     */
    fun toDomain(): VendorPayout {
        return VendorPayout(
            id = this.id,
            vendorId = this.vendorId,
            vendorName = this.vendorName,
            amount = this.amount,
            payoutType = VendorPayoutType.fromString(this.payoutType),
            branchId = this.branchId,
            stationId = this.stationId,
            employeeId = this.employeeId,
            employeeName = this.employeeName,
            managerId = this.managerId,
            managerName = this.managerName,
            referenceNumber = this.referenceNumber,
            notes = this.notes,
            payoutDateTime = this.payoutDateTime,
            isSynced = this.isSynced,
            createdDate = this.createdDate
        )
    }
    
    companion object {
        /**
         * Creates a DTO from a domain model.
         */
        fun fromDomain(payout: VendorPayout): LegacyVendorPayoutDto {
            return LegacyVendorPayoutDto(
                id = payout.id,
                vendorId = payout.vendorId,
                vendorName = payout.vendorName,
                amount = payout.amount,
                payoutType = payout.payoutType.name,
                branchId = payout.branchId,
                stationId = payout.stationId,
                employeeId = payout.employeeId,
                employeeName = payout.employeeName,
                managerId = payout.managerId,
                managerName = payout.managerName,
                referenceNumber = payout.referenceNumber,
                notes = payout.notes,
                payoutDateTime = payout.payoutDateTime,
                isSynced = payout.isSynced,
                createdDate = payout.createdDate
            )
        }
        
        /**
         * Creates a DTO from a Couchbase document map.
         * 
         * @param map The document as a Map
         * @return The DTO, or null if required fields are missing
         */
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>): LegacyVendorPayoutDto? {
            return try {
                LegacyVendorPayoutDto(
                    id = (map["id"] as? Number)?.toLong() ?: return null,
                    vendorId = (map["vendorId"] as? Number)?.toInt() ?: return null,
                    vendorName = map["vendorName"] as? String ?: return null,
                    amount = (map["amount"] as? Number)?.let { BigDecimal(it.toString()) } ?: return null,
                    payoutType = map["payoutType"] as? String,
                    branchId = (map["branchId"] as? Number)?.toInt() ?: 0,
                    stationId = (map["stationId"] as? Number)?.toInt() ?: 0,
                    employeeId = (map["employeeId"] as? Number)?.toInt() ?: 0,
                    employeeName = map["employeeName"] as? String,
                    managerId = (map["managerId"] as? Number)?.toInt(),
                    managerName = map["managerName"] as? String,
                    referenceNumber = map["referenceNumber"] as? String,
                    notes = map["notes"] as? String,
                    payoutDateTime = map["payoutDateTime"] as? String ?: "",
                    isSynced = map["isSynced"] as? Boolean ?: false,
                    createdDate = map["createdDate"] as? String
                )
            } catch (e: Exception) {
                println("Error mapping LegacyVendorPayoutDto from map: ${e.message}")
                null
            }
        }
    }
}

