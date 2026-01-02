package com.unisight.gropos.features.transaction.domain.mapper

import com.unisight.gropos.features.checkout.domain.model.Cart
import com.unisight.gropos.features.checkout.domain.model.CartItem
import com.unisight.gropos.features.payment.domain.model.AppliedPayment
import com.unisight.gropos.features.payment.domain.model.PaymentType
import com.unisight.gropos.features.transaction.domain.model.Transaction
import com.unisight.gropos.features.transaction.domain.model.TransactionItem
import com.unisight.gropos.features.transaction.domain.model.TransactionPayment
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Maps a Cart and list of AppliedPayments to a finalized Transaction document.
 * 
 * Per DATABASE_SCHEMA.md: Transaction documents contain items and payments arrays.
 * Per reliability-stability.mdc: All IDs should be generated deterministically.
 * 
 * @param appliedPayments The list of payments applied to this transaction
 * @param employeeId Optional employee ID (from login)
 * @param employeeName Optional employee name
 * @param branchId The branch ID (defaults to 1)
 * @param stationId The station ID (defaults to 1)
 * @return A finalized Transaction ready for persistence
 */
fun Cart.toTransaction(
    appliedPayments: List<AppliedPayment>,
    employeeId: Int? = null,
    employeeName: String? = null,
    branchId: Int = 1,
    stationId: Int = 1
): Transaction {
    val now = Instant.now()
    val nowIso = DateTimeFormatter.ISO_INSTANT.format(now)
    
    // Generate unique transaction ID (timestamp-based for sequential ordering)
    val transactionId = now.toEpochMilli()
    val guid = UUID.randomUUID().toString()
    
    return Transaction(
        id = transactionId,
        guid = guid,
        branchId = branchId,
        stationId = stationId,
        employeeId = employeeId,
        employeeName = employeeName,
        transactionStatusId = Transaction.COMPLETED,
        transactionTypeName = "Sale",
        startDateTime = nowIso, // For simplicity, using same timestamp
        completedDateTime = nowIso,
        completedDate = nowIso,
        subTotal = this.subTotal,
        discountTotal = this.discountTotal,
        taxTotal = this.taxTotal,
        crvTotal = this.crvTotal,
        grandTotal = this.grandTotal,
        itemCount = this.uniqueItemCount,
        customerName = null,
        loyaltyCardNumber = null,
        items = this.items
            .filterNot { it.isRemoved }
            .mapIndexed { index, cartItem ->
                cartItem.toTransactionItem(transactionId, index)
            },
        payments = appliedPayments.mapIndexed { index, payment ->
            payment.toTransactionPayment(transactionId, index, nowIso)
        }
    )
}

/**
 * Maps a CartItem to a TransactionItem.
 * 
 * Per DATABASE_SCHEMA.md: TransactionProduct structure.
 */
private fun CartItem.toTransactionItem(
    transactionId: Long,
    index: Int
): TransactionItem {
    return TransactionItem(
        id = transactionId * 1000 + index, // Derived ID for items
        transactionId = transactionId,
        branchProductId = this.branchProductId,
        branchProductName = this.branchProductName,
        quantityUsed = this.quantityUsed,
        unitType = this.unitType,
        retailPrice = this.retailPrice,
        salePrice = this.salePrice,
        priceUsed = this.effectivePrice,
        discountAmountPerUnit = this.discountAmountPerUnit,
        transactionDiscountAmountPerUnit = this.transactionDiscountAmountPerUnit,
        floorPrice = this.floorPrice,
        taxPerUnit = this.taxPerUnit,
        taxTotal = this.taxTotal,
        crvRatePerUnit = this.crvRatePerUnit,
        subTotal = this.subTotal,
        savingsTotal = this.savingsTotal,
        isRemoved = false, // Filtered out removed items above
        isPromptedPrice = this.isPromptedPrice,
        isFloorPriceOverridden = this.isFloorPriceOverridden,
        soldById = this.soldById,
        taxIndicator = this.taxIndicator,
        isFoodStampEligible = this.isSnapEligible,
        scanDateTime = this.scanDateTime
    )
}

/**
 * Maps an AppliedPayment to a TransactionPayment.
 * 
 * Per DATABASE_SCHEMA.md: TransactionPayment structure.
 */
private fun AppliedPayment.toTransactionPayment(
    transactionId: Long,
    index: Int,
    paymentDateTime: String
): TransactionPayment {
    return TransactionPayment(
        id = transactionId * 1000 + 500 + index, // Derived ID, offset from items
        transactionId = transactionId,
        paymentMethodId = this.type.toPaymentMethodId(),
        paymentMethodName = this.displayName,
        value = this.amount,
        referenceNumber = null,
        approvalCode = this.authCode,
        cardType = null,
        cardLastFour = this.lastFour,
        isSuccessful = true,
        paymentDateTime = paymentDateTime
    )
}

/**
 * Maps PaymentType enum to payment method ID.
 * 
 * Per PAYMENT_PROCESSING.md: Payment method IDs.
 */
private fun PaymentType.toPaymentMethodId(): Int {
    return when (this) {
        PaymentType.Cash -> TransactionPayment.CASH
        PaymentType.Credit -> TransactionPayment.CREDIT
        PaymentType.Debit -> TransactionPayment.DEBIT
        PaymentType.EbtSnap -> TransactionPayment.EBT_SNAP
        PaymentType.EbtCash -> TransactionPayment.EBT_CASH
        PaymentType.Check -> TransactionPayment.CHECK
        PaymentType.OnAccount -> 7 // Not defined in schema, using 7
    }
}

