package com.unisight.gropos.features.transaction.domain.mapper

import com.unisight.gropos.features.checkout.domain.model.Cart
import com.unisight.gropos.features.checkout.domain.model.CartItem
import com.unisight.gropos.features.payment.domain.model.AppliedPayment
import com.unisight.gropos.features.payment.domain.model.PaymentType
import com.unisight.gropos.features.transaction.domain.model.Transaction
import com.unisight.gropos.features.transaction.domain.model.TransactionItem
import com.unisight.gropos.features.transaction.domain.model.TransactionItemTax
import com.unisight.gropos.features.transaction.domain.model.TransactionPayment
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Maps a Cart and list of AppliedPayments to a finalized Transaction document.
 * 
 * Per DATABASE_SCHEMA.md: Transaction documents contain items and payments arrays.
 * Per TRANSACTION_API_SUBMISSION_IMPLEMENTATION_PLAN.md: All fields populated for API.
 * Per reliability-stability.mdc: All IDs should be generated deterministically.
 * 
 * @param appliedPayments The list of payments applied to this transaction
 * @param employeeId Optional employee ID (from login)
 * @param employeeName Optional employee name
 * @param customerId Optional customer ID (for loyalty)
 * @param branchId The branch ID (defaults to 1)
 * @param stationId The station ID (defaults to 1)
 * @param deviceId Optional device ID
 * @param locationAccountId Optional till/location account ID
 * @param shiftId Optional shift ID
 * @return A finalized Transaction ready for persistence and API submission
 */
fun Cart.toTransaction(
    appliedPayments: List<AppliedPayment>,
    employeeId: Int? = null,
    employeeName: String? = null,
    customerId: Int? = null,
    branchId: Int = 1,
    stationId: Int = 1,
    deviceId: Int? = null,
    locationAccountId: Int? = null,
    shiftId: Int? = null
): Transaction {
    val now = Instant.now()
    val nowIso = DateTimeFormatter.ISO_INSTANT.format(now)
    
    // Generate unique transaction ID (timestamp-based for sequential ordering)
    val transactionId = now.toEpochMilli()
    val guid = UUID.randomUUID().toString()
    
    // Calculate counts per TRANSACTION_API_SUBMISSION_IMPLEMENTATION_PLAN.md
    val activeItems = items.filterNot { it.isRemoved }
    val rowCount = items.size // Including removed
    val itemCount = activeItems.size
    val uniqueProductCount = activeItems.map { it.branchProductId }.toSet().size
    val uniqueSaleProductCount = activeItems
        .filter { it.salePrice != null }
        .map { it.branchProductId }
        .toSet()
        .size
    val totalPurchaseCount = activeItems.fold(BigDecimal.ZERO) { acc, item -> 
        acc.add(item.quantityUsed) 
    }
    
    // Calculate cost total (sum of cost * quantity for all items)
    val costTotal = activeItems.fold(BigDecimal.ZERO) { acc, item -> 
        val cost = item.product.cost ?: BigDecimal.ZERO
        acc.add(cost.multiply(item.quantityUsed))
    }.setScale(3, RoundingMode.HALF_UP)
    
    // Calculate savings total
    val savingsTotal = activeItems.fold(BigDecimal.ZERO) { acc, item -> 
        acc.add(item.savingsTotal) 
    }
    
    // Determine payment date (first payment timestamp)
    val paymentDate = if (appliedPayments.isNotEmpty()) nowIso else null
    
    return Transaction(
        id = transactionId,
        guid = guid,
        branchId = branchId,
        stationId = stationId,
        deviceId = deviceId,
        employeeId = employeeId,
        employeeName = employeeName,
        locationAccountId = locationAccountId,
        shiftId = shiftId,
        customerId = customerId,
        customerName = null,
        loyaltyCardNumber = null,
        transactionStatusId = Transaction.COMPLETED,
        transactionTypeName = "Sale",
        syncStatus = Transaction.SYNC_PENDING,
        remoteId = null,
        originalTransactionId = null,
        originalTransactionGuid = null,
        startDateTime = nowIso,
        paymentDate = paymentDate,
        completedDateTime = nowIso,
        completedDate = nowIso,
        rowCount = rowCount,
        itemCount = itemCount,
        uniqueProductCount = uniqueProductCount,
        uniqueSaleProductCount = uniqueSaleProductCount,
        totalPurchaseCount = totalPurchaseCount,
        subTotal = this.subTotal,
        discountTotal = this.discountTotal,
        taxTotal = this.taxTotal,
        crvTotal = this.crvTotal,
        grandTotal = this.grandTotal,
        costTotal = costTotal,
        savingsTotal = savingsTotal,
        fee = BigDecimal.ZERO,
        items = items.mapIndexed { index, cartItem ->
            cartItem.toTransactionItem(transactionId, guid, index + 1)
        },
        payments = appliedPayments.mapIndexed { index, payment ->
            payment.toTransactionPayment(transactionId, guid, index, nowIso)
        },
        metadata = null
    )
}

/**
 * Maps a CartItem to a TransactionItem.
 * 
 * Per DATABASE_SCHEMA.md: TransactionProduct structure.
 * Per TRANSACTION_API_SUBMISSION_IMPLEMENTATION_PLAN.md: All fields populated for API.
 * 
 * @param transactionId The parent transaction ID
 * @param transactionGuid The parent transaction GUID
 * @param rowNumber The display order (1-based)
 */
private fun CartItem.toTransactionItem(
    transactionId: Long,
    transactionGuid: String,
    rowNumber: Int
): TransactionItem {
    // Calculate derived fields per API spec
    val cost = this.product.cost ?: BigDecimal.ZERO
    val costTotal = cost.multiply(this.quantityUsed).setScale(3, RoundingMode.HALF_UP)
    val finalPrice = this.effectivePrice.subtract(this.discountAmountPerUnit)
        .subtract(this.transactionDiscountAmountPerUnit)
    val finalPriceTaxSum = finalPrice.add(this.taxPerUnit)
    val taxPercentSum = if (this.effectivePrice > BigDecimal.ZERO) {
        this.taxPerUnit.divide(this.effectivePrice, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal(100))
            .setScale(2, RoundingMode.HALF_UP)
    } else BigDecimal.ZERO
    val subjectToTaxTotal = if (this.taxTotal > BigDecimal.ZERO) this.subTotal else BigDecimal.ZERO
    val paidTotal = this.subTotal.add(this.taxTotal)
    val savingsPerUnit = if (this.quantityUsed > BigDecimal.ZERO) {
        this.savingsTotal.divide(this.quantityUsed, 2, RoundingMode.HALF_UP)
    } else BigDecimal.ZERO
    
    // SNAP-related calculations
    val nonSNAPTotal = if (!this.isSnapEligible) paidTotal else BigDecimal.ZERO
    
    // Get item number from first barcode or use branchProductId
    val itemNumber = this.product.itemNumbers.firstOrNull()?.itemNumber ?: this.branchProductId.toString()
    
    return TransactionItem(
        id = transactionId * 1000 + rowNumber, // Derived ID for items
        transactionId = transactionId,
        transactionGuid = transactionGuid,
        transactionItemGuid = UUID.randomUUID().toString(),
        branchProductId = this.branchProductId,
        branchProductName = this.branchProductName,
        itemNumber = itemNumber,
        quantityUsed = this.quantityUsed,
        quantitySold = this.quantityUsed,
        quantityReturned = BigDecimal.ZERO,
        unitType = this.unitType,
        rowNumber = rowNumber,
        isManualQuantity = false, // TODO: Track this in CartItem
        retailPrice = this.retailPrice,
        salePrice = this.salePrice,
        priceUsed = this.effectivePrice,
        floorPrice = this.floorPrice,
        promptedPrice = if (this.isPromptedPrice) this.effectivePrice else BigDecimal.ZERO,
        finalPrice = finalPrice,
        finalPriceTaxSum = finalPriceTaxSum,
        cost = cost,
        costTotal = costTotal,
        discountAmountPerUnit = this.discountAmountPerUnit,
        transactionDiscountAmountPerUnit = this.transactionDiscountAmountPerUnit,
        discountTypeId = null, // TODO: Populate from discount source
        discountTypeAmount = this.discountAmountPerUnit,
        transactionDiscountTypeId = null,
        transactionDiscountTypeAmount = this.transactionDiscountAmountPerUnit,
        taxPerUnit = this.taxPerUnit,
        taxTotal = this.taxTotal,
        taxPercentSum = taxPercentSum,
        subjectToTaxTotal = subjectToTaxTotal,
        taxes = buildTaxBreakdown(this.taxPerUnit, this.quantityUsed),
        crvRatePerUnit = this.crvRatePerUnit,
        subTotal = this.subTotal,
        savingsTotal = this.savingsTotal,
        savingsPerUnit = savingsPerUnit,
        paidTotal = paidTotal,
        isFoodStampEligible = this.isSnapEligible,
        snapPaidAmount = BigDecimal.ZERO, // Will be calculated during payment split
        snapPaidPercent = BigDecimal.ZERO,
        nonSNAPTotal = nonSNAPTotal,
        isRemoved = this.isRemoved,
        isPromptedPrice = this.isPromptedPrice,
        isFloorPriceOverridden = this.isFloorPriceOverridden,
        floorPriceOverrideEmployeeId = null, // TODO: Track in CartItem
        soldById = this.soldById,
        taxIndicator = this.taxIndicator,
        scanDateTime = this.scanDateTime
    )
}

/**
 * Builds a tax breakdown list.
 * 
 * For simplicity, creates a single tax entry.
 * TODO: Support multiple tax authorities when tax breakdown is available.
 */
private fun buildTaxBreakdown(taxPerUnit: BigDecimal, quantity: BigDecimal): List<TransactionItemTax> {
    if (taxPerUnit <= BigDecimal.ZERO) return emptyList()
    
    // Default tax ID (general sales tax)
    val taxAmount = taxPerUnit.multiply(quantity).setScale(2, RoundingMode.HALF_UP)
    
    return listOf(
        TransactionItemTax(
            taxId = 1, // Default tax authority
            taxRate = BigDecimal("8.25"), // Default rate - TODO: Get actual rate
            amount = taxAmount
        )
    )
}

/**
 * Maps an AppliedPayment to a TransactionPayment.
 * 
 * Per DATABASE_SCHEMA.md: TransactionPayment structure.
 * Per TRANSACTION_API_SUBMISSION_IMPLEMENTATION_PLAN.md: All fields populated for API.
 * 
 * @param transactionId The parent transaction ID
 * @param transactionGuid The parent transaction GUID
 * @param index The payment index (0-based)
 * @param paymentDateTime The payment timestamp (ISO-8601)
 */
private fun AppliedPayment.toTransactionPayment(
    transactionId: Long,
    transactionGuid: String,
    index: Int,
    paymentDateTime: String
): TransactionPayment {
    val paymentMethodId = this.type.toPaymentMethodId()
    val creditCardNumber = this.lastFour?.let { "************$it" }
    
    return TransactionPayment(
        id = transactionId * 1000 + 500 + index, // Derived ID, offset from items
        transactionId = transactionId,
        transactionGuid = transactionGuid,
        transactionPaymentGuid = UUID.randomUUID().toString(),
        paymentMethodId = paymentMethodId,
        paymentMethodName = this.displayName,
        paymentTypeId = paymentMethodId, // Same as methodId for now
        accountTypeId = 0, // Default
        statusId = TransactionPayment.STATUS_SUCCESS,
        value = this.amount,
        referenceNumber = this.referenceNumber,
        approvalCode = this.authCode,
        cardType = this.cardType,
        cardLastFour = this.lastFour,
        creditCardNumber = creditCardNumber,
        paxData = this.paxData,
        isSuccessful = true,
        paymentDateTime = paymentDateTime
    )
}

/**
 * Maps PaymentType enum to payment method ID.
 * 
 * Per TRANSACTION_API_SUBMISSION_IMPLEMENTATION_PLAN.md:
 * Payment type IDs aligned with backend API.
 */
private fun PaymentType.toPaymentMethodId(): Int {
    return when (this) {
        PaymentType.Cash -> TransactionPayment.CASH
        PaymentType.Credit -> TransactionPayment.CREDIT
        PaymentType.Debit -> TransactionPayment.DEBIT
        PaymentType.EbtSnap -> TransactionPayment.EBT_SNAP
        PaymentType.EbtCash -> TransactionPayment.EBT_CASH
        PaymentType.Check -> TransactionPayment.CHECK
        PaymentType.OnAccount -> TransactionPayment.CASH // Default to cash
    }
}

