package com.unisight.gropos.features.transaction.data.mapper

import com.unisight.gropos.features.transaction.data.dto.*
import com.unisight.gropos.features.transaction.domain.model.PaxData
import com.unisight.gropos.features.transaction.domain.model.Transaction
import com.unisight.gropos.features.transaction.domain.model.TransactionItem
import com.unisight.gropos.features.transaction.domain.model.TransactionItemTax
import com.unisight.gropos.features.transaction.domain.model.TransactionPayment
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Domain → API DTO Mapper Extensions.
 * 
 * **Source of Truth:** docs/development-plan/TRANSACTION_API_SUBMISSION_IMPLEMENTATION_PLAN.md
 * 
 * **CRITICAL - Precision Constraint:**
 * All monetary values are converted to String with proper scale:
 * - Quantities: 3 decimal places (e.g., "2.000", "1.250")
 * - Prices/Amounts: 2 decimal places (e.g., "4.99", "46.00")
 * - Cost/Tax Rates: 3 decimal places (e.g., "2.500", "8.750")
 * 
 * **Zero-Trust:** Do not log PII or sensitive data.
 */

// ============================================================================
// Transaction → CreateTransactionRequest
// ============================================================================

/**
 * Converts a domain Transaction to the API CreateTransactionRequest.
 * 
 * Per TRANSACTION_API_SUBMISSION_IMPLEMENTATION_PLAN.md:
 * - All monetary values as String for precision
 * - Status IDs mapped to API values
 * - Timestamps in ISO-8601 format
 */
fun Transaction.toCreateTransactionRequest(): CreateTransactionRequest {
    return CreateTransactionRequest(
        transaction = toAddEditTransactionRequest(),
        items = items.map { it.toAddEditTransactionItemRequest(guid) },
        payments = payments.map { it.toAddEditTransactionPaymentRequest(guid) }
    )
}

/**
 * Converts Transaction to AddEditTransactionRequest (header data).
 */
fun Transaction.toAddEditTransactionRequest(): AddEditTransactionRequest {
    return AddEditTransactionRequest(
        id = null, // Always null for new transactions
        guid = guid,
        customerId = customerId,
        transactionStatusId = TransactionStatusApi.fromLocalStatus(transactionStatusId),
        startDate = startDateTime,
        paymentDate = paymentDate,
        completedDate = completedDateTime,
        rowCount = rowCount,
        itemCount = itemCount,
        uniqueProductCount = uniqueProductCount,
        uniqueSaleProductCount = uniqueSaleProductCount,
        totalPurchaseCount = totalPurchaseCount.toMoneyString(3),
        costTotal = costTotal.toMoneyString(3),
        savingsTotal = savingsTotal.toMoneyString(2),
        taxTotal = taxTotal.toMoneyString(2),
        subTotal = subTotal.toMoneyString(2),
        crvTotal = crvTotal.toMoneyString(2),
        fee = fee.toMoneyString(2),
        grandTotal = grandTotal.toMoneyString(2)
    )
}

// ============================================================================
// TransactionItem → AddEditTransactionItemRequest
// ============================================================================

/**
 * Converts TransactionItem to AddEditTransactionItemRequest.
 * 
 * Per TRANSACTION_API_SUBMISSION_IMPLEMENTATION_PLAN.md - Transaction Item Mapping.
 */
fun TransactionItem.toAddEditTransactionItemRequest(
    parentTransactionGuid: String
): AddEditTransactionItemRequest {
    return AddEditTransactionItemRequest(
        id = null, // Always null for new items
        transactionGuid = parentTransactionGuid,
        transactionItemGuid = transactionItemGuid,
        branchProductId = branchProductId,
        isRemoved = isRemoved,
        scanDate = scanDateTime ?: "",
        rowNumber = rowNumber,
        isManualQuantity = isManualQuantity,
        quantitySold = quantitySold.toMoneyString(3),
        quantityReturned = quantityReturned.toMoneyString(3),
        itemNumber = itemNumber,
        isPromptedPrice = isPromptedPrice,
        isFoodStampable = isFoodStampEligible,
        isFloorPriceOverridden = isFloorPriceOverridden,
        floorPriceOverrideEmployeeId = floorPriceOverrideEmployeeId,
        discountTypeId = discountTypeId,
        discountTypeAmount = discountTypeAmount.toMoneyString(2),
        transactionDiscountTypeId = transactionDiscountTypeId,
        transactionDiscountTypeAmount = transactionDiscountTypeAmount.toMoneyString(2),
        cost = cost.toMoneyString(3),
        floorPrice = (floorPrice ?: BigDecimal.ZERO).toMoneyString(2),
        retailPrice = retailPrice.toMoneyString(2),
        salePrice = (salePrice ?: BigDecimal.ZERO).toMoneyString(2),
        promptedPrice = promptedPrice.toMoneyString(2),
        crvRatePerUnit = crvRatePerUnit.toMoneyString(2),
        priceUsed = priceUsed.toMoneyString(2),
        quantityUsed = quantityUsed.toMoneyString(3),
        costTotal = costTotal.toMoneyString(3),
        taxPercentSum = taxPercentSum.toMoneyString(2),
        discountAmountPerUnit = discountAmountPerUnit.toMoneyString(2),
        transactionDiscountAmountPerUnit = transactionDiscountAmountPerUnit.toMoneyString(2),
        finalPrice = finalPrice.toMoneyString(2),
        taxPerUnit = taxPerUnit.toMoneyString(2),
        finalPriceTaxSum = finalPriceTaxSum.toMoneyString(2),
        subTotal = subTotal.toMoneyString(2),
        snapPaidAmount = snapPaidAmount.toMoneyString(2),
        snapPaidPercent = snapPaidPercent.toMoneyString(2),
        subjectToTaxTotal = subjectToTaxTotal.toMoneyString(2),
        taxTotal = taxTotal.toMoneyString(2),
        nonSNAPTotal = nonSNAPTotal.toMoneyString(2),
        paidTotal = paidTotal.toMoneyString(2),
        savingsPerUnit = savingsPerUnit.toMoneyString(2),
        savingsTotal = savingsTotal.toMoneyString(2),
        taxes = taxes.map { it.toAddEditTransactionItemTaxRequest() }
    )
}

/**
 * Converts TransactionItemTax to AddEditTransactionItemTaxRequest.
 */
fun TransactionItemTax.toAddEditTransactionItemTaxRequest(): AddEditTransactionItemTaxRequest {
    return AddEditTransactionItemTaxRequest(
        taxId = taxId,
        taxRate = taxRate.toMoneyString(2),
        amount = amount.toMoneyString(2)
    )
}

// ============================================================================
// TransactionPayment → AddEditTransactionPaymentRequest
// ============================================================================

/**
 * Converts TransactionPayment to AddEditTransactionPaymentRequest.
 * 
 * Per TRANSACTION_API_SUBMISSION_IMPLEMENTATION_PLAN.md - Payment Mapping.
 */
fun TransactionPayment.toAddEditTransactionPaymentRequest(
    parentTransactionGuid: String
): AddEditTransactionPaymentRequest {
    return AddEditTransactionPaymentRequest(
        id = null, // Always null for new payments
        transactionGuid = parentTransactionGuid,
        transactionPaymentGuid = transactionPaymentGuid,
        paymentDate = paymentDateTime,
        paymentTypeId = PaymentTypeApi.fromLocalPaymentType(paymentMethodId),
        accountTypeId = accountTypeId,
        statusId = if (isSuccessful) PaymentStatusApi.SUCCESS else PaymentStatusApi.ERROR,
        value = value.toMoneyString(2),
        creditCardNumber = creditCardNumber ?: cardLastFour?.let { "************$it" },
        paxData = paxData?.toPaxDataRequest()
    )
}

/**
 * Converts PaxData to PaxDataRequest.
 */
fun PaxData.toPaxDataRequest(): PaxDataRequest {
    return PaxDataRequest(
        id = id,
        transactionPaymentId = null,
        resultCode = resultCode,
        resultTxt = resultTxt,
        authCode = authCode,
        approvedAmount = approvedAmount,
        avsResponse = avsResponse,
        bogusAccountNum = bogusAccountNum,
        cardType = cardType,
        cvResponse = cvResponse,
        hostCode = hostCode,
        hostResponse = hostResponse,
        message = message,
        refNum = refNum,
        rawResponse = rawResponse,
        remainingBalance = remainingBalance ?: "",
        extraBalance = extraBalance,
        requestedAmount = requestedAmount,
        timestamp = timestamp,
        sigFileName = sigFileName,
        signData = signData,
        extData = extData
    )
}

// ============================================================================
// Helper Extensions
// ============================================================================

/**
 * Converts BigDecimal to String with specified scale for API transmission.
 * 
 * **Precision Contract:**
 * - Uses HALF_UP rounding (banker's rounding)
 * - Returns exact decimal representation (e.g., "2.50" not "2.5")
 * 
 * @param scale Number of decimal places (typically 2 for money, 3 for quantities)
 */
private fun BigDecimal.toMoneyString(scale: Int): String {
    return this.setScale(scale, RoundingMode.HALF_UP).toPlainString()
}

/**
 * Safely converts a nullable BigDecimal to String.
 */
private fun BigDecimal?.toMoneyStringOrZero(scale: Int): String {
    return (this ?: BigDecimal.ZERO).toMoneyString(scale)
}

