package com.unisight.gropos.features.transaction.data.dto

import com.unisight.gropos.features.transaction.domain.model.Transaction
import com.unisight.gropos.features.transaction.domain.model.TransactionItem
import com.unisight.gropos.features.transaction.domain.model.TransactionPayment
import java.math.BigDecimal

/**
 * Data Transfer Object for legacy Couchbase LocalTransaction documents.
 * 
 * Per COUCHBASE_LOCAL_STORAGE.md - LocalTransaction collection structure.
 * Per BACKEND_INTEGRATION_STATUS.md - Field mapping reference.
 * 
 * This DTO maps the legacy JSON structure to our domain model,
 * handling field renames and type transformations.
 * 
 * **Legacy Collection:** LocalTransaction (scope: pos)
 * **Document ID:** {id} or {guid}
 * **Pending ID:** {guid}-P (during active transaction)
 */
data class LegacyTransactionDto(
    // Primary identifiers
    val id: Long,
    val guid: String,
    
    // Branch/Station info
    val branchId: Int,
    val branch: String? = null,             // Maps to: (not stored - denormalized)
    
    // Employee info
    val employeeId: Int? = null,
    val employee: String? = null,           // Maps to: employeeName
    
    // Customer info
    val customerId: Int? = null,
    
    // Status - Legacy uses string enum, we use Int
    val transactionStatusId: String? = null,
    
    // Timestamps - Legacy uses different field names
    val startDate: String? = null,          // Maps to: startDateTime
    val paymentDate: String? = null,        // Maps to: (new field needed)
    val completedDate: String? = null,      // Maps to: completedDateTime
    
    // Item counts
    val rowCount: Int? = null,              // Number of line items
    val itemCount: Int? = null,             // Total item quantity
    val uniqueProductCount: Int? = null,    // Unique products
    
    // Totals - Legacy uses savingsTotal instead of discountTotal
    val savingsTotal: Double? = null,       // Maps to: discountTotal
    val taxTotal: Double? = null,
    val subTotal: Double? = null,
    val crvTotal: Double? = null,
    val fee: Double? = null,                // Maps to: (new field needed)
    val grandTotal: Double? = null,
    
    // Line items and payments (not stored in legacy, but we need them)
    val items: List<LegacyTransactionItemDto>? = null,
    val payments: List<LegacyTransactionPaymentDto>? = null
) {
    
    /**
     * Converts legacy DTO to domain Transaction model.
     * 
     * Per BACKEND_INTEGRATION_STATUS.md - Field Mapping Reference:
     * - Renames: employee -> employeeName, startDate -> startDateTime, etc.
     * - Transforms: transactionStatusId (string) -> Int
     */
    fun toDomain(): Transaction {
        return Transaction(
            id = id,
            guid = guid,
            
            // Branch/Station
            branchId = branchId,
            stationId = 1, // Default - legacy doesn't track station separately
            
            // Employee - RENAME: employee -> employeeName
            employeeId = employeeId,
            employeeName = employee,
            
            // Status - TRANSFORM: String enum -> Int
            transactionStatusId = parseTransactionStatus(transactionStatusId),
            transactionTypeName = "Sale",
            
            // Timestamps - RENAME: startDate -> startDateTime, completedDate -> completedDateTime
            startDateTime = startDate ?: "",
            completedDateTime = completedDate ?: "",
            completedDate = completedDate?.substringBefore("T") ?: "",
            
            // Totals - RENAME: savingsTotal -> discountTotal
            subTotal = BigDecimal.valueOf(subTotal ?: 0.0),
            discountTotal = BigDecimal.valueOf(savingsTotal ?: 0.0),
            taxTotal = BigDecimal.valueOf(taxTotal ?: 0.0),
            crvTotal = BigDecimal.valueOf(crvTotal ?: 0.0),
            grandTotal = BigDecimal.valueOf(grandTotal ?: 0.0),
            
            // Item count
            itemCount = itemCount ?: 0,
            
            // Customer info
            customerName = null, // Not stored in legacy
            loyaltyCardNumber = null,
            
            // Items and payments
            items = items?.map { it.toDomain() } ?: emptyList(),
            payments = payments?.map { it.toDomain() } ?: emptyList()
        )
    }
    
    /**
     * Converts domain Transaction to legacy DTO for persistence.
     */
    companion object {
        
        /**
         * Creates a LegacyTransactionDto from a domain Transaction.
         * Used for saving transactions in legacy format.
         */
        fun fromDomain(transaction: Transaction): LegacyTransactionDto {
            return LegacyTransactionDto(
                id = transaction.id,
                guid = transaction.guid,
                branchId = transaction.branchId,
                branch = null, // We don't store branch name in transactions
                employeeId = transaction.employeeId,
                employee = transaction.employeeName,
                customerId = null,
                transactionStatusId = transactionStatusToLegacy(transaction.transactionStatusId),
                startDate = transaction.startDateTime,
                paymentDate = null,
                completedDate = transaction.completedDateTime,
                rowCount = transaction.items.size,
                itemCount = transaction.itemCount,
                uniqueProductCount = transaction.items.distinctBy { it.branchProductId }.size,
                savingsTotal = transaction.discountTotal.toDouble(),
                taxTotal = transaction.taxTotal.toDouble(),
                subTotal = transaction.subTotal.toDouble(),
                crvTotal = transaction.crvTotal.toDouble(),
                fee = 0.0,
                grandTotal = transaction.grandTotal.toDouble(),
                items = transaction.items.map { LegacyTransactionItemDto.fromDomain(it) },
                payments = transaction.payments.map { LegacyTransactionPaymentDto.fromDomain(it) }
            )
        }
        
        /**
         * Creates a LegacyTransactionDto from a raw Map (Couchbase document).
         * 
         * Per reliability-rules.mdc: Defensive parsing with null safety.
         */
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>): LegacyTransactionDto? {
            return try {
                val id = (map["id"] as? Number)?.toLong() ?: return null
                val guid = map["guid"] as? String ?: return null
                val branchId = (map["branchId"] as? Number)?.toInt() ?: return null
                
                LegacyTransactionDto(
                    id = id,
                    guid = guid,
                    branchId = branchId,
                    branch = map["branch"] as? String,
                    employeeId = (map["employeeId"] as? Number)?.toInt(),
                    employee = map["employee"] as? String,
                    customerId = (map["customerId"] as? Number)?.toInt(),
                    transactionStatusId = map["transactionStatusId"] as? String,
                    startDate = map["startDate"] as? String,
                    paymentDate = map["paymentDate"] as? String,
                    completedDate = map["completedDate"] as? String,
                    rowCount = (map["rowCount"] as? Number)?.toInt(),
                    itemCount = (map["itemCount"] as? Number)?.toInt(),
                    uniqueProductCount = (map["uniqueProductCount"] as? Number)?.toInt(),
                    savingsTotal = (map["savingsTotal"] as? Number)?.toDouble(),
                    taxTotal = (map["taxTotal"] as? Number)?.toDouble(),
                    subTotal = (map["subTotal"] as? Number)?.toDouble(),
                    crvTotal = (map["crvTotal"] as? Number)?.toDouble(),
                    fee = (map["fee"] as? Number)?.toDouble(),
                    grandTotal = (map["grandTotal"] as? Number)?.toDouble(),
                    items = (map["items"] as? List<Map<String, Any?>>)?.mapNotNull {
                        LegacyTransactionItemDto.fromMap(it)
                    },
                    payments = (map["payments"] as? List<Map<String, Any?>>)?.mapNotNull {
                        LegacyTransactionPaymentDto.fromMap(it)
                    }
                )
            } catch (e: Exception) {
                println("LegacyTransactionDto: Error parsing document - ${e.message}")
                null
            }
        }
        
        /**
         * Parses legacy transactionStatusId string to Int.
         * 
         * Per COUCHBASE_LOCAL_STORAGE.md:
         * transactionStatusId: Enum - Open, Completed, Voided, Held
         */
        private fun parseTransactionStatus(statusId: String?): Int {
            return when (statusId?.lowercase()) {
                "open", "pending" -> Transaction.PENDING
                "completed" -> Transaction.COMPLETED
                "voided" -> Transaction.VOIDED
                "held", "on_hold" -> Transaction.ON_HOLD
                else -> Transaction.PENDING
            }
        }
        
        /**
         * Converts transaction status Int to legacy string enum.
         */
        private fun transactionStatusToLegacy(statusId: Int): String {
            return when (statusId) {
                Transaction.PENDING -> "Open"
                Transaction.COMPLETED -> "Completed"
                Transaction.VOIDED -> "Voided"
                Transaction.ON_HOLD -> "Held"
                else -> "Open"
            }
        }
    }
}

/**
 * DTO for legacy transaction line items.
 */
data class LegacyTransactionItemDto(
    val id: Long,
    val transactionId: Long,
    val branchProductId: Int,
    val branchProductName: String,
    val quantityUsed: Double,
    val unitType: String? = null,
    val retailPrice: Double,
    val salePrice: Double? = null,
    val priceUsed: Double,
    val discountAmountPerUnit: Double = 0.0,
    val transactionDiscountAmountPerUnit: Double = 0.0,
    val floorPrice: Double? = null,
    val taxPerUnit: Double = 0.0,
    val taxTotal: Double = 0.0,
    val crvRatePerUnit: Double = 0.0,
    val subTotal: Double = 0.0,
    val savingsTotal: Double = 0.0,
    val isRemoved: Boolean = false,
    val isPromptedPrice: Boolean = false,
    val isFloorPriceOverridden: Boolean = false,
    val soldById: String = "Each",
    val taxIndicator: String = "T",
    val isFoodStampEligible: Boolean = false,
    val scanDateTime: String? = null
) {
    fun toDomain(): TransactionItem = TransactionItem(
        id = id,
        transactionId = transactionId,
        branchProductId = branchProductId,
        branchProductName = branchProductName,
        quantityUsed = BigDecimal.valueOf(quantityUsed),
        unitType = unitType ?: "Each",
        retailPrice = BigDecimal.valueOf(retailPrice),
        salePrice = salePrice?.let { BigDecimal.valueOf(it) },
        priceUsed = BigDecimal.valueOf(priceUsed),
        discountAmountPerUnit = BigDecimal.valueOf(discountAmountPerUnit),
        transactionDiscountAmountPerUnit = BigDecimal.valueOf(transactionDiscountAmountPerUnit),
        floorPrice = floorPrice?.let { BigDecimal.valueOf(it) },
        taxPerUnit = BigDecimal.valueOf(taxPerUnit),
        taxTotal = BigDecimal.valueOf(taxTotal),
        crvRatePerUnit = BigDecimal.valueOf(crvRatePerUnit),
        subTotal = BigDecimal.valueOf(subTotal),
        savingsTotal = BigDecimal.valueOf(savingsTotal),
        isRemoved = isRemoved,
        isPromptedPrice = isPromptedPrice,
        isFloorPriceOverridden = isFloorPriceOverridden,
        soldById = soldById,
        taxIndicator = taxIndicator,
        isFoodStampEligible = isFoodStampEligible,
        scanDateTime = scanDateTime
    )
    
    companion object {
        fun fromDomain(item: TransactionItem): LegacyTransactionItemDto {
            return LegacyTransactionItemDto(
                id = item.id,
                transactionId = item.transactionId,
                branchProductId = item.branchProductId,
                branchProductName = item.branchProductName,
                quantityUsed = item.quantityUsed.toDouble(),
                unitType = item.unitType,
                retailPrice = item.retailPrice.toDouble(),
                salePrice = item.salePrice?.toDouble(),
                priceUsed = item.priceUsed.toDouble(),
                discountAmountPerUnit = item.discountAmountPerUnit.toDouble(),
                transactionDiscountAmountPerUnit = item.transactionDiscountAmountPerUnit.toDouble(),
                floorPrice = item.floorPrice?.toDouble(),
                taxPerUnit = item.taxPerUnit.toDouble(),
                taxTotal = item.taxTotal.toDouble(),
                crvRatePerUnit = item.crvRatePerUnit.toDouble(),
                subTotal = item.subTotal.toDouble(),
                savingsTotal = item.savingsTotal.toDouble(),
                isRemoved = item.isRemoved,
                isPromptedPrice = item.isPromptedPrice,
                isFloorPriceOverridden = item.isFloorPriceOverridden,
                soldById = item.soldById,
                taxIndicator = item.taxIndicator,
                isFoodStampEligible = item.isFoodStampEligible,
                scanDateTime = item.scanDateTime
            )
        }
        
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>): LegacyTransactionItemDto? {
            return try {
                LegacyTransactionItemDto(
                    id = (map["id"] as? Number)?.toLong() ?: 0,
                    transactionId = (map["transactionId"] as? Number)?.toLong() ?: 0,
                    branchProductId = (map["branchProductId"] as? Number)?.toInt() ?: 0,
                    branchProductName = map["branchProductName"] as? String ?: "",
                    quantityUsed = (map["quantityUsed"] as? Number)?.toDouble() ?: 1.0,
                    unitType = map["unitType"] as? String,
                    retailPrice = (map["retailPrice"] as? Number)?.toDouble() ?: 0.0,
                    salePrice = (map["salePrice"] as? Number)?.toDouble(),
                    priceUsed = (map["priceUsed"] as? Number)?.toDouble() ?: 0.0,
                    discountAmountPerUnit = (map["discountAmountPerUnit"] as? Number)?.toDouble() ?: 0.0,
                    transactionDiscountAmountPerUnit = (map["transactionDiscountAmountPerUnit"] as? Number)?.toDouble() ?: 0.0,
                    floorPrice = (map["floorPrice"] as? Number)?.toDouble(),
                    taxPerUnit = (map["taxPerUnit"] as? Number)?.toDouble() ?: 0.0,
                    taxTotal = (map["taxTotal"] as? Number)?.toDouble() ?: 0.0,
                    crvRatePerUnit = (map["crvRatePerUnit"] as? Number)?.toDouble() ?: 0.0,
                    subTotal = (map["subTotal"] as? Number)?.toDouble() ?: 0.0,
                    savingsTotal = (map["savingsTotal"] as? Number)?.toDouble() ?: 0.0,
                    isRemoved = map["isRemoved"] as? Boolean ?: false,
                    isPromptedPrice = map["isPromptedPrice"] as? Boolean ?: false,
                    isFloorPriceOverridden = map["isFloorPriceOverridden"] as? Boolean ?: false,
                    soldById = map["soldById"] as? String ?: "Each",
                    taxIndicator = map["taxIndicator"] as? String ?: "T",
                    isFoodStampEligible = map["isFoodStampEligible"] as? Boolean ?: false,
                    scanDateTime = map["scanDateTime"] as? String
                )
            } catch (e: Exception) {
                println("LegacyTransactionItemDto: Error parsing item - ${e.message}")
                null
            }
        }
    }
}

/**
 * DTO for legacy transaction payments.
 */
data class LegacyTransactionPaymentDto(
    val id: Long,
    val transactionId: Long,
    val paymentMethodId: Int,
    val paymentMethodName: String,
    val value: Double,
    val referenceNumber: String? = null,
    val approvalCode: String? = null,
    val cardType: String? = null,
    val cardLastFour: String? = null,
    val isSuccessful: Boolean = true,
    val paymentDateTime: String
) {
    fun toDomain(): TransactionPayment = TransactionPayment(
        id = id,
        transactionId = transactionId,
        paymentMethodId = paymentMethodId,
        paymentMethodName = paymentMethodName,
        value = BigDecimal.valueOf(value),
        referenceNumber = referenceNumber,
        approvalCode = approvalCode,
        cardType = cardType,
        cardLastFour = cardLastFour,
        isSuccessful = isSuccessful,
        paymentDateTime = paymentDateTime
    )
    
    companion object {
        fun fromDomain(payment: TransactionPayment): LegacyTransactionPaymentDto {
            return LegacyTransactionPaymentDto(
                id = payment.id,
                transactionId = payment.transactionId,
                paymentMethodId = payment.paymentMethodId,
                paymentMethodName = payment.paymentMethodName,
                value = payment.value.toDouble(),
                referenceNumber = payment.referenceNumber,
                approvalCode = payment.approvalCode,
                cardType = payment.cardType,
                cardLastFour = payment.cardLastFour,
                isSuccessful = payment.isSuccessful,
                paymentDateTime = payment.paymentDateTime
            )
        }
        
        fun fromMap(map: Map<String, Any?>): LegacyTransactionPaymentDto? {
            return try {
                LegacyTransactionPaymentDto(
                    id = (map["id"] as? Number)?.toLong() ?: 0,
                    transactionId = (map["transactionId"] as? Number)?.toLong() ?: 0,
                    paymentMethodId = (map["paymentMethodId"] as? Number)?.toInt() ?: 1,
                    paymentMethodName = map["paymentMethodName"] as? String ?: "Cash",
                    value = (map["value"] as? Number)?.toDouble() ?: 0.0,
                    referenceNumber = map["referenceNumber"] as? String,
                    approvalCode = map["approvalCode"] as? String,
                    cardType = map["cardType"] as? String,
                    cardLastFour = map["cardLastFour"] as? String,
                    isSuccessful = map["isSuccessful"] as? Boolean ?: true,
                    paymentDateTime = map["paymentDateTime"] as? String ?: ""
                )
            } catch (e: Exception) {
                println("LegacyTransactionPaymentDto: Error parsing payment - ${e.message}")
                null
            }
        }
    }
}

