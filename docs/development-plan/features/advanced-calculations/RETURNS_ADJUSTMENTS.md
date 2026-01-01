# Returns and Adjustments

[← Back to Index](./INDEX.md) | [Previous: Payment Processing](../PAYMENT_PROCESSING.md) | [Next: Calculation Engine →](./CALCULATION_ENGINE.md)

---

## Overview

Returns and adjustments in GroPOS handle the reversal of all or part of a transaction, including proper refund of discounts, taxes, and government benefits. This document covers calculation logic for return scenarios.

---

## Return Types

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          RETURN TYPES                                   │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  FULL TRANSACTION RETURN                                                │
│  ═══════════════════════                                                │
│  • All items returned                                                   │
│  • Complete reversal of payments                                        │
│  • Voids entire transaction                                             │
│                                                                         │
│  PARTIAL ITEM RETURN                                                    │
│  ══════════════════════                                                 │
│  • Some items returned (keep others)                                    │
│  • Proportional refund calculation                                      │
│  • Original transaction modified                                        │
│                                                                         │
│  QUANTITY RETURN                                                        │
│  ═══════════════                                                        │
│  • Partial quantity of an item returned                                 │
│  • E.g., bought 5, returning 2                                          │
│  • Complex discount recalculation                                       │
│                                                                         │
│  EXCHANGE                                                               │
│  ════════                                                               │
│  • Return item(s) and purchase new item(s)                              │
│  • Net payment/refund calculated                                        │
│  • May use store credit for difference                                  │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Return Data Model

### Return Item

```kotlin
data class ReturnItem(
    val transactionItemId: Int,           // Original item being returned
    val transactionGuid: String,          // Original transaction
    
    // Return Details
    var quantityReturned: BigDecimal,     // How many being returned
    var quantityRemaining: BigDecimal,    // Quantity kept
    
    // Original Values
    val originalFinalPrice: BigDecimal,
    val originalTaxPerUnit: BigDecimal,
    val originalSubtotal: BigDecimal,
    val originalTaxTotal: BigDecimal,
    
    // Payment Breakdown
    var totalSnapAmount: BigDecimal = BigDecimal.ZERO,    // SNAP portion to refund
    var totalNonSnapAmount: BigDecimal = BigDecimal.ZERO, // Non-SNAP portion to refund
    var wicAmount: BigDecimal = BigDecimal.ZERO,          // WIC portion to refund
    var ebtCashAmount: BigDecimal = BigDecimal.ZERO,      // EBT Cash portion to refund
    var taxAmount: BigDecimal = BigDecimal.ZERO,          // Tax to refund
    
    // Refund Calculation
    var maxReturnAmount: BigDecimal = BigDecimal.ZERO,
    var actualRefundAmount: BigDecimal = BigDecimal.ZERO,
    var refundAfterDeductions: BigDecimal = BigDecimal.ZERO,
    
    // Fees
    var restockingFee: BigDecimal = BigDecimal.ZERO,
    var crvRefund: BigDecimal = BigDecimal.ZERO,
    var depositRefund: BigDecimal = BigDecimal.ZERO,
    
    // Reason
    var returnReasonCode: String? = null,
    var returnReasonDescription: String? = null,
    
    // Return Context
    var hasReceipt: Boolean = true,
    var isSameStore: Boolean = true,
    var isWithinPolicy: Boolean = true,
    var requiresManagerApproval: Boolean = false,
    
    // Condition
    var itemCondition: String? = null,    // "New", "Opened", "Damaged", "Defective"
    var isResellable: Boolean = true,
    
    // Authorization
    var authorizedBy: Int? = null,
    var authorizationNote: String? = null
)
```

---

## Return Calculation

### Calculating Refund Amount

```kotlin
fun calculateReturnRefund(
    originalItem: TransactionItemViewModel,
    returnQuantity: BigDecimal
): ReturnItem {
    // Validate quantity
    if (returnQuantity > originalItem.quantityUsed) {
        throw ReturnQuantityExceedsOriginalException()
    }
    
    if (returnQuantity <= BigDecimal.ZERO) {
        throw InvalidReturnQuantityException()
    }
    
    val returnItem = ReturnItem(
        transactionItemId = originalItem.id,
        transactionGuid = originalItem.transactionGuid,
        quantityReturned = returnQuantity,
        quantityRemaining = originalItem.quantityUsed - returnQuantity,
        originalFinalPrice = originalItem.finalPrice,
        originalTaxPerUnit = originalItem.taxPerUnit,
        originalSubtotal = originalItem.subTotal,
        originalTaxTotal = originalItem.taxTotal
    )
    
    // Calculate proportional return values
    val returnRatio = returnQuantity / originalItem.quantityUsed
    
    // ═══════════════════════════════════════════════════════════
    // CALCULATE REFUND COMPONENTS
    // ═══════════════════════════════════════════════════════════
    
    // Subtotal refund (proportional)
    returnItem.originalSubtotal = (originalItem.subTotal * returnRatio)
        .setScale(2, RoundingMode.HALF_UP)
    
    // Tax refund (proportional, adjusted for SNAP)
    // Only refund tax on non-SNAP portion
    val nonSnapRatio = BigDecimal.ONE - (originalItem.snapPaidPercent / BigDecimal(100))
    returnItem.taxAmount = (originalItem.taxPerUnit * returnQuantity * nonSnapRatio)
        .setScale(2, RoundingMode.HALF_UP)
    
    // ═══════════════════════════════════════════════════════════
    // CALCULATE PAYMENT SOURCE REFUNDS
    // ═══════════════════════════════════════════════════════════
    
    // SNAP refund (proportional)
    if (originalItem.snapPaidAmount > BigDecimal.ZERO) {
        returnItem.totalSnapAmount = (originalItem.snapPaidAmount * returnRatio)
            .setScale(2, RoundingMode.HALF_UP)
    }
    
    // WIC refund (proportional)
    if (originalItem.wicPaidAmount > BigDecimal.ZERO) {
        returnItem.wicAmount = (originalItem.wicPaidAmount * returnRatio)
            .setScale(2, RoundingMode.HALF_UP)
    }
    
    // Non-SNAP refund
    val nonSnapPortion = originalItem.nonSNAPTotal * returnRatio
    returnItem.totalNonSnapAmount = (nonSnapPortion + returnItem.taxAmount)
        .setScale(2, RoundingMode.HALF_UP)
    
    // ═══════════════════════════════════════════════════════════
    // CALCULATE DEPOSIT REFUNDS
    // ═══════════════════════════════════════════════════════════
    
    returnItem.crvRefund = originalItem.crvRatePerUnit * returnQuantity
    returnItem.depositRefund = originalItem.bottleDepositPerUnit * returnQuantity
    
    // ═══════════════════════════════════════════════════════════
    // APPLY RESTOCKING FEE (IF APPLICABLE)
    // ═══════════════════════════════════════════════════════════
    
    if (shouldApplyRestockingFee(originalItem)) {
        val restockingFeePercent = getRestockingFeePercent(originalItem)
        returnItem.restockingFee = (returnItem.originalSubtotal * 
            (restockingFeePercent / BigDecimal(100)))
            .setScale(2, RoundingMode.HALF_UP)
    }
    
    // ═══════════════════════════════════════════════════════════
    // CALCULATE FINAL REFUND
    // ═══════════════════════════════════════════════════════════
    
    returnItem.maxReturnAmount = returnItem.originalSubtotal +
        returnItem.taxAmount +
        returnItem.crvRefund +
        returnItem.depositRefund -
        returnItem.restockingFee
    
    returnItem.actualRefundAmount = returnItem.maxReturnAmount
    
    return returnItem
}
```

---

## SNAP Refund Processing

```kotlin
fun processSnapRefund(
    item: TransactionItemViewModel,
    returnItem: ReturnItem
): EbtResult? {
    if (returnItem.totalSnapAmount <= BigDecimal.ZERO) {
        return null  // No SNAP to refund
    }
    
    // Calculate new SNAP balance on item
    val newSnapAmount = item.snapPaidAmount - returnItem.totalSnapAmount
    
    // Update item
    updateTransactionItemAfterSnapRefund(item, newSnapAmount, returnItem.quantityReturned)
    
    // Process EBT credit
    val ebtResult = paymentService.creditEbt(
        type = "SNAP_FOOD",
        amount = returnItem.totalSnapAmount,
        originalAuthCode = getOriginalSnapAuth(item.transactionGuid)
    )
    
    if (!ebtResult.success) {
        throw SnapRefundFailedException()
    }
    
    return ebtResult
}

fun updateTransactionItemAfterSnapRefund(
    item: TransactionItemViewModel,
    newSnapAmount: BigDecimal,
    returnQty: BigDecimal
) {
    // Update quantities
    item.quantityReturned += returnQty
    item.quantityUsed = item.quantitySold - item.quantityReturned
    
    // Update SNAP tracking
    item.snapPaidAmount = newSnapAmount
    
    // Recalculate SNAP percentage
    val newTotal = item.finalPrice * item.quantityUsed
    item.snapPaidPercent = if (newTotal > BigDecimal.ZERO) {
        (item.snapPaidAmount / newTotal) * BigDecimal(100)
    } else {
        BigDecimal.ZERO
    }
    
    // Recalculate tax (based on new non-SNAP portion)
    recalculateItemAfterReturn(item)
}
```

---

## Promotion Return Handling

### Returning Promotional Items

When returning items that were part of a promotion:

```kotlin
fun handlePromotionalItemReturn(
    originalTransaction: Transaction,
    returnItem: ReturnItem
): ReturnItem {
    val item = getItem(returnItem.transactionItemId)
    
    if (item.promotionId == null) {
        // No promotion, standard return
        return processStandardReturn(returnItem)
    }
    
    val promotion = getPromotion(item.promotionId)
    
    return when (promotion.type) {
        PromotionType.MIX_MATCH -> handleMixMatchReturn(originalTransaction, returnItem, promotion)
        PromotionType.BOGO -> handleBogoReturn(originalTransaction, returnItem, promotion)
        PromotionType.MULTI_BUY -> handleMultiBuyReturn(originalTransaction, returnItem, promotion)
        else -> processStandardReturn(returnItem)
    }
}

fun handleBogoReturn(
    transaction: Transaction,
    returnItem: ReturnItem,
    promotion: BogoPromotion
): ReturnItem {
    val item = getItem(returnItem.transactionItemId)
    
    // Find related BOGO items
    val relatedItems = transaction.items.filter { i ->
        i.promotionGroupId == item.promotionGroupId
    }
    
    if (item.isPromotionReward) {
        // Returning the "free" item
        // Refund is just the discounted amount they paid (possibly $0)
        val paidAmount = item.finalPrice - item.promotionDiscountPerUnit
        returnItem.actualRefundAmount = paidAmount * returnItem.quantityReturned
    } else if (item.isPromotionTrigger) {
        // Returning the qualifying item
        // This may invalidate the BOGO, requiring recalculation
        val remainingTriggers = countRemainingTriggers(relatedItems, returnItem)
        
        if (remainingTriggers < promotion.buyQuantity) {
            // BOGO no longer qualifies, must charge for "free" item
            val rewardItem = relatedItems.find { it.isPromotionReward }
            
            if (rewardItem != null && !rewardItem.isReturned) {
                // Reduce refund by the discount that was given on reward
                val additionalCharge = rewardItem.promotionDiscountTotal
                returnItem.actualRefundAmount -= additionalCharge
            }
        }
    }
    
    return returnItem
}

fun handleMixMatchReturn(
    transaction: Transaction,
    returnItem: ReturnItem,
    promotion: MixMatchPromotion
): ReturnItem {
    val item = getItem(returnItem.transactionItemId)
    
    // Find all items in this mix & match
    val mixMatchItems = transaction.items.filter { i ->
        i.promotionId == promotion.id && !i.isRemoved
    }
    
    // Calculate new total quantity
    val currentQty = mixMatchItems.sumOf { it.quantityUsed }
    val newQty = currentQty - returnItem.quantityReturned
    
    if (newQty < promotion.totalRequiredQuantity) {
        // Mix & Match no longer qualifies
        // Must recalculate all items at regular price
        
        val totalPromotionSavings = mixMatchItems.sumOf { it.promotionDiscountTotal }
        returnItem.actualRefundAmount -= totalPromotionSavings
        
        // Log adjustment
        returnItem.adjustmentReason = "Mix & Match no longer qualifies"
    } else {
        // Mix & Match still qualifies, proportional return
        returnItem.actualRefundAmount = calculateProportionalReturn(item, returnItem)
    }
    
    return returnItem
}
```

---

## WIC Return Processing

WIC returns require special handling due to state regulations:

```kotlin
fun processWicReturn(
    originalItem: TransactionItemViewModel,
    returnItem: ReturnItem
): WicResult? {
    if (returnItem.wicAmount <= BigDecimal.ZERO) {
        return null  // No WIC portion to refund
    }
    
    // ═══════════════════════════════════════════════════════════
    // WIC REFUNDS GO BACK TO THE BENEFIT
    // Unlike regular refunds, WIC returns restore the customer's benefit
    // ═══════════════════════════════════════════════════════════
    
    val wicResult = wicService.refundBenefit(
        customerWicId = originalItem.wicCustomerId,
        categoryCode = originalItem.wicCategoryCode,
        quantity = returnItem.quantityReturned,
        amount = returnItem.wicAmount,
        originalTransactionId = returnItem.transactionGuid
    )
    
    if (!wicResult.success) {
        // WIC system may reject refund for various reasons
        when (wicResult.reasonCode) {
            "BENEFIT_EXPIRED" -> {
                // Cannot restore expired benefits - convert to store credit
                returnItem.wicAmount = BigDecimal.ZERO
                returnItem.convertToStoreCredit = true
                returnItem.storeCreditAmount += returnItem.wicAmount
            }
            "ALREADY_REFUNDED" -> throw WicAlreadyRefundedException()
            else -> throw WicRefundFailedException(wicResult.reason)
        }
    }
    
    // Track WIC refund
    returnItem.wicRefundConfirmation = wicResult.confirmationNumber
    returnItem.wicRefundDate = OffsetDateTime.now()
    
    return wicResult
}
```

---

## Exchange Processing

### Exchange Calculation

```kotlin
fun processExchange(
    returningItems: List<ReturnItem>,
    newItems: List<TransactionItemViewModel>
): ExchangeResult {
    // Calculate total refund
    val totalRefund = returningItems.sumOf { it.actualRefundAmount }
    
    // Calculate new purchase total
    val newPurchaseSubtotal = newItems.sumOf { it.subTotal }
    val newPurchaseTax = newItems.sumOf { it.taxTotal }
    val newPurchaseTotal = newPurchaseSubtotal + newPurchaseTax
    
    // Calculate net
    val netAmount = newPurchaseTotal - totalRefund
    
    return when {
        netAmount > BigDecimal.ZERO -> {
            // Customer owes money
            ExchangeResult(
                type = ExchangeType.CUSTOMER_OWES,
                amount = netAmount,
                refundItems = returningItems,
                purchaseItems = newItems
            )
        }
        netAmount < BigDecimal.ZERO -> {
            // Customer gets refund
            val refundAmount = -netAmount
            
            val refundMethod = if (refundAmount <= MAX_CASH_REFUND) {
                RefundMethod.CASH
            } else {
                RefundMethod.STORE_CREDIT
            }
            
            ExchangeResult(
                type = ExchangeType.CUSTOMER_REFUND,
                amount = refundAmount,
                refundMethod = refundMethod,
                refundItems = returningItems,
                purchaseItems = newItems
            )
        }
        else -> {
            // Even exchange
            ExchangeResult(
                type = ExchangeType.EVEN_EXCHANGE,
                amount = BigDecimal.ZERO,
                refundItems = returningItems,
                purchaseItems = newItems
            )
        }
    }
}
```

---

## Refund Payment Processing

### Determining Refund Method

```kotlin
fun determineRefundMethod(
    originalTransaction: Transaction,
    returnItem: ReturnItem
): List<RefundPayment> {
    // Get original payments
    val originalPayments = originalTransaction.payments
        .filter { it.statusId == PaymentStatus.Success }
        .sortedByDescending { it.paymentDate }
    
    val refundMethod = mutableListOf<RefundPayment>()
    var remainingRefund = returnItem.actualRefundAmount
    
    // SNAP portion must go back to EBT
    if (returnItem.totalSnapAmount > BigDecimal.ZERO) {
        refundMethod.add(RefundPayment(
            type = PaymentType.SNAP,
            amount = returnItem.totalSnapAmount
        ))
        remainingRefund -= returnItem.totalSnapAmount
    }
    
    // WIC portion must go back to WIC
    if (returnItem.wicAmount > BigDecimal.ZERO) {
        refundMethod.add(RefundPayment(
            type = PaymentType.WIC,
            amount = returnItem.wicAmount
        ))
        remainingRefund -= returnItem.wicAmount
    }
    
    // Non-benefit portion - refund to original tender
    for (payment in originalPayments) {
        if (remainingRefund <= BigDecimal.ZERO) break
        
        when (payment.paymentTypeId) {
            PaymentType.Credit, PaymentType.Debit -> {
                val refundToCard = minOf(remainingRefund, payment.value)
                refundMethod.add(RefundPayment(
                    type = payment.paymentTypeId,
                    amount = refundToCard,
                    originalPaymentId = payment.id
                ))
                remainingRefund -= refundToCard
            }
            PaymentType.Cash -> {
                refundMethod.add(RefundPayment(
                    type = PaymentType.Cash,
                    amount = remainingRefund
                ))
                remainingRefund = BigDecimal.ZERO
            }
            PaymentType.GiftCard -> {
                refundMethod.add(RefundPayment(
                    type = PaymentType.GiftCard,
                    amount = minOf(remainingRefund, payment.value),
                    giftCardNumber = payment.giftCardNumber
                ))
                remainingRefund -= minOf(remainingRefund, payment.value)
            }
        }
    }
    
    // Any remaining as store credit
    if (remainingRefund > BigDecimal.ZERO) {
        refundMethod.add(RefundPayment(
            type = PaymentType.StoreCredit,
            amount = remainingRefund
        ))
    }
    
    return refundMethod
}
```

---

## No-Receipt Returns

### No-Receipt Return Process

```kotlin
data class NoReceiptReturnConfig(
    val enabled: Boolean = true,
    val maxDollarAmount: BigDecimal = BigDecimal("50"),
    val dailyLimit: BigDecimal = BigDecimal("100"),
    val monthlyLimit: BigDecimal = BigDecimal("200"),
    val useLowestRecentPrice: Boolean = true,
    val priceHistoryDays: Int = 30,
    val requireId: Boolean = true,
    val acceptedIdTypes: List<String> = listOf("DriversLicense", "StateID", "Passport")
)

fun processNoReceiptReturn(request: NoReceiptReturnRequest): NoReceiptReturnResult {
    val config = getNoReceiptReturnConfig()
    
    // Step 1: Require and verify ID
    if (config.requireId) {
        val idInfo = captureIdentification(request.idType)
        if (idInfo == null || !idInfo.valid) {
            throw IdRequiredForNoReceiptReturnException()
        }
        
        // Check blacklist
        if (isBlacklisted(idInfo.idNumber)) {
            throw CustomerBlacklistedException("Return privileges suspended")
        }
    }
    
    // Step 2: Check customer limits
    val customerHistory = getNoReceiptHistory(idInfo.idNumber)
    
    val dailyTotal = customerHistory
        .filter { it.date == LocalDate.now() }
        .sumOf { it.amount }
    if (dailyTotal + request.totalAmount > config.dailyLimit) {
        throw DailyNoReceiptLimitExceededException()
    }
    
    val monthlyTotal = customerHistory
        .filter { it.date >= LocalDate.now().minusDays(30) }
        .sumOf { it.amount }
    if (monthlyTotal + request.totalAmount > config.monthlyLimit) {
        throw MonthlyNoReceiptLimitExceededException()
    }
    
    // Step 3: Determine refund price
    for (item in request.items) {
        // Use lowest price in last 30 days (prevents sale price abuse)
        val lowestPrice = getLowestPriceInPeriod(
            item.branchProductId,
            config.priceHistoryDays
        )
        
        // Never exceed current retail price
        val currentPrice = getCurrentRetailPrice(item.branchProductId)
        
        item.refundPrice = minOf(lowestPrice, currentPrice)
        
        // No tax refund without receipt (can't prove tax was paid)
        item.taxRefund = BigDecimal.ZERO
    }
    
    // Step 4: Require manager approval
    val approval = requestManagerOverride(ManagerOverrideRequest(
        operationType = "NO_RECEIPT_RETURN",
        reason = "No-receipt return",
        requestedValue = request.totalAmount
    ))
    
    if (!approval.approved) {
        throw ManagerApprovalDeniedException()
    }
    
    // Step 5: Determine refund method (typically store credit)
    val refundMethod = if (request.totalAmount <= config.cashThreshold) {
        RefundMethod.CASH
    } else {
        RefundMethod.STORE_CREDIT
    }
    
    // Step 6: Log for fraud prevention
    logNoReceiptReturn(idInfo, request, approval)
    
    return NoReceiptReturnResult(
        approved = true,
        refundAmount = request.items.sumOf { it.refundPrice * it.quantity },
        refundMethod = refundMethod,
        storeCreditNumber = if (refundMethod == RefundMethod.STORE_CREDIT) {
            generateStoreCreditNumber()
        } else null
    )
}
```

---

## Return Reason Codes

```kotlin
enum class ReturnReasonCode {
    // Customer-Initiated
    CHANGED_MIND,
    WRONG_ITEM,
    WRONG_SIZE,
    WRONG_FLAVOR,
    TOO_EXPENSIVE,
    DUPLICATE_PURCHASE,
    GIFT_UNWANTED,
    
    // Product Issues
    DEFECTIVE,
    DAMAGED,
    EXPIRED,
    SPOILED,
    QUALITY_ISSUE,
    MISSING_PARTS,
    SAFETY_RECALL,
    
    // Store Errors
    OVERCHARGED,
    WRONG_PRODUCT_SCANNED,
    DOUBLE_SCANNED,
    PRICE_ADJUSTMENT,
    
    // Other
    OTHER_SEE_NOTES
}

data class ReturnReasonAction(
    val code: ReturnReasonCode,
    val fullRefund: Boolean,              // No restocking fee
    val requiresDefectiveVerification: Boolean,
    val createVendorClaim: Boolean,       // For damaged/defective
    val noRestocking: Boolean,            // Item goes to waste/return
    val exchangePreferred: Boolean        // Suggest exchange first
)

val returnReasonActions = mapOf(
    ReturnReasonCode.DEFECTIVE to ReturnReasonAction(
        code = ReturnReasonCode.DEFECTIVE,
        fullRefund = true,
        requiresDefectiveVerification = true,
        createVendorClaim = true,
        noRestocking = true,
        exchangePreferred = false
    ),
    ReturnReasonCode.DAMAGED to ReturnReasonAction(
        code = ReturnReasonCode.DAMAGED,
        fullRefund = true,
        requiresDefectiveVerification = false,
        createVendorClaim = true,
        noRestocking = true,
        exchangePreferred = false
    ),
    ReturnReasonCode.EXPIRED to ReturnReasonAction(
        code = ReturnReasonCode.EXPIRED,
        fullRefund = true,
        requiresDefectiveVerification = false,
        createVendorClaim = false,
        noRestocking = true,
        exchangePreferred = false
    )
    // ... additional reason actions
)
```

---

[← Back to Index](./INDEX.md) | [Previous: Payment Processing](../PAYMENT_PROCESSING.md) | [Next: Calculation Engine →](./CALCULATION_ENGINE.md)

