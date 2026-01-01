# Calculation Engine

[← Back to Index](./INDEX.md) | [Previous: Returns & Adjustments](./RETURNS_ADJUSTMENTS.md) | [Next: Examples →](./EXAMPLES.md)

---

## Overview

The Calculation Engine orchestrates all pricing, discount, tax, and payment calculations in a precise sequence to ensure accurate and consistent results.

---

## Master Calculation Sequence

### Complete Item Calculation Flow

```kotlin
fun calculateItem(item: TransactionItemViewModel, transaction: Transaction) {
    val product = getProduct(item.branchProductId)
    val customer = transaction.customer
    
    // ════════════════════════════════════════════════════════════════
    // PHASE 1: BASE PRICE DETERMINATION
    // ════════════════════════════════════════════════════════════════
    
    item.retailPrice = product.retailPrice
    item.cost = product.cost
    item.floorPrice = product.floorPrice
    item.salePrice = getValidSalePrice(product)
    val customerPrice = getCustomerPrice(product, customer)
    item.priceUsed = determinePriceUsed(item, customerPrice)
    
    if (shouldApplyBulkPricing(item, product)) {
        item.priceUsed = getBulkPrice(product, item.quantityUsed)
        item.isBulkPriced = true
    }
    
    // ════════════════════════════════════════════════════════════════
    // PHASE 2: DEPOSITS AND FEES
    // ════════════════════════════════════════════════════════════════
    
    item.crvRatePerUnit = calculateCRV(product)
    item.bottleDepositPerUnit = calculateBottleDeposit(product)
    item.otherFeesPerUnit = calculateOtherFees(product)
    item.totalDepositsPerUnit = item.crvRatePerUnit + 
        item.bottleDepositPerUnit + item.otherFeesPerUnit
    
    // ════════════════════════════════════════════════════════════════
    // PHASE 3: QUANTITY CALCULATIONS
    // ════════════════════════════════════════════════════════════════
    
    item.quantityUsed = item.quantitySold - item.quantityReturned
    item.costTotal = item.cost * item.quantityUsed
    
    // ════════════════════════════════════════════════════════════════
    // PHASE 4-6: DISCOUNTS (Promotions, Coupons, Customer)
    // ════════════════════════════════════════════════════════════════
    
    // Applied by separate methods before this
    item.couponDiscountPerUnit = item.appliedCoupons
        .sumOf { it.discountAmount } / item.quantityUsed
    item.couponDiscountTotal = item.appliedCoupons.sumOf { it.discountAmount }
    
    // ════════════════════════════════════════════════════════════════
    // PHASE 7: MANUAL/INVOICE DISCOUNTS
    // ════════════════════════════════════════════════════════════════
    
    if (item.discountTypeId == null && OrderStore.invoiceDiscount > BigDecimal.ZERO) {
        applyInvoiceDiscountToItem(item, OrderStore.invoiceDiscount)
    }
    
    // ════════════════════════════════════════════════════════════════
    // PHASE 8: FINAL PRICE CALCULATION
    // ════════════════════════════════════════════════════════════════
    
    val totalDiscountPerUnit = item.promotionDiscountPerUnit +
        item.couponDiscountPerUnit +
        item.customerDiscountPerUnit +
        item.discountAmountPerUnit +
        item.transactionDiscountAmountPerUnit
    
    var effectivePrice = item.priceUsed - totalDiscountPerUnit
    
    // Enforce floor price
    if (!item.isFloorPriceOverridden && effectivePrice < item.floorPrice) {
        if (!isSalePriceBelowFloor(item)) {
            effectivePrice = item.floorPrice
        }
    }
    
    item.finalPriceExcludingDeposits = effectivePrice
    item.finalPrice = effectivePrice + item.totalDepositsPerUnit
    
    // ════════════════════════════════════════════════════════════════
    // PHASE 9: TAX CALCULATION
    // ════════════════════════════════════════════════════════════════
    
    item.taxPercentSum = getTaxPercent(product)
    
    if (item.isTaxExempt || customer?.isTaxExempt == true) {
        item.taxPercentSum = BigDecimal.ZERO
    }
    
    if (isTaxHolidayActive(item)) {
        applyTaxHoliday(item)
    }
    
    val taxableAmount = calculateTaxableAmount(item)
    item.taxPerUnit = (taxableAmount * item.taxPercentSum / BigDecimal(100))
        .setScale(2, RoundingMode.HALF_UP)
    
    // ════════════════════════════════════════════════════════════════
    // PHASE 10: LINE TOTALS
    // ════════════════════════════════════════════════════════════════
    
    item.subTotal = (item.finalPrice * item.quantityUsed)
        .setScale(2, RoundingMode.HALF_UP)
    
    val snapFactor = BigDecimal.ONE - (item.snapPaidPercent / BigDecimal(100))
    item.subjectToTaxTotal = (item.subTotal * snapFactor)
        .setScale(2, RoundingMode.HALF_UP)
    item.taxTotal = (item.taxPerUnit * item.quantityUsed * snapFactor)
        .setScale(2, RoundingMode.HALF_UP)
    item.finalPriceTaxSum = item.finalPrice + item.taxPerUnit
    
    // ════════════════════════════════════════════════════════════════
    // PHASE 11: SAVINGS CALCULATION
    // ════════════════════════════════════════════════════════════════
    
    val originalPrice = if (item.isPromptedPrice) item.promptedPrice else item.retailPrice
    item.savingsPerUnit = (originalPrice + item.totalDepositsPerUnit - item.finalPrice)
        .setScale(2, RoundingMode.HALF_UP)
    item.savingsTotal = (item.savingsPerUnit * item.quantityUsed)
        .setScale(2, RoundingMode.HALF_UP)
    
    // ════════════════════════════════════════════════════════════════
    // PHASE 12: BUILD TAX BREAKDOWN
    // ════════════════════════════════════════════════════════════════
    
    item.taxes = buildTaxBreakdown(product, item.taxTotal, item.taxPercentSum)
    
    // ════════════════════════════════════════════════════════════════
    // PHASE 13: BENEFIT ELIGIBILITY
    // ════════════════════════════════════════════════════════════════
    
    item.isSNAPEligible = product.isSNAPEligible
    item.isWicEligible = checkWicEligibility(product)
}
```

---

## Transaction-Level Calculations

### Full Transaction Recalculation

```kotlin
fun recalculateTransactionTotals(transaction: Transaction) {
    val activeItems = transaction.items.filter { !it.isRemoved }
    
    // STEP 1: RECALCULATE ALL ITEMS
    for (item in activeItems) {
        calculateItem(item, transaction)
    }
    
    // STEP 2: EVALUATE PROMOTIONS (CROSS-ITEM)
    for (item in activeItems) {
        if (item.promotionId != null && !item.isPromotionLocked) {
            clearPromotionFromItem(item)
        }
    }
    
    val promotionResults = evaluateAllPromotions(activeItems)
    for (result in promotionResults) {
        applyPromotionResult(result, activeItems)
    }
    
    for (item in activeItems) {
        if (item.promotionWasUpdated) {
            calculateItem(item, transaction)
        }
    }
    
    // STEP 3: SUM LINE TOTALS
    transaction.subTotal = activeItems.sumOf { it.subTotal }
    transaction.taxTotal = activeItems.sumOf { it.taxTotal }
    transaction.crvTotal = activeItems.sumOf { it.crvRatePerUnit * it.quantityUsed }
    transaction.depositTotal = activeItems.sumOf { it.totalDepositsPerUnit * it.quantityUsed }
    transaction.savingsTotal = activeItems.sumOf { it.savingsTotal }
    transaction.costTotal = activeItems.sumOf { it.costTotal }
    
    // STEP 4: CALCULATE GRAND TOTAL
    transaction.grandTotal = (transaction.subTotal + transaction.taxTotal)
        .setScale(2, RoundingMode.HALF_UP)
    
    // STEP 5: CALCULATE BENEFIT ELIGIBILITY
    transaction.ebtEligibleTotal = activeItems
        .filter { it.isSNAPEligible && it.snapPaidPercent < BigDecimal(100) }
        .sumOf { it.subjectToTaxTotal - it.snapPaidAmount }
    
    if (transaction.customer?.hasWicBenefits == true) {
        transaction.wicEligibleTotal = calculateWicEligible(
            activeItems,
            transaction.customer.wicBenefit
        )
    }
    
    // STEP 6: UPDATE COUNTS
    transaction.rowCount = transaction.items.size
    transaction.itemCount = activeItems.size
    transaction.uniqueProductCount = activeItems.map { it.branchProductId }.distinct().count()
    transaction.totalPurchaseCount = activeItems.sumOf { it.quantityUsed }
    
    // STEP 7: CALCULATE AMOUNT DUE
    val totalPaid = transaction.payments
        .filter { it.statusId == PaymentStatus.Success && it.paymentTypeId != TransactionPaymentType.CashChange }
        .sumOf { it.value }
    
    transaction.amountDue = maxOf(BigDecimal.ZERO, transaction.grandTotal - totalPaid)
}
```

---

## Rounding Strategy

### Rounding Rules

```kotlin
data class RoundingConfig(
    // Currency rounding
    val currencyDecimals: Int = 2,
    val currencyRoundingMode: RoundingMode = RoundingMode.HALF_UP,
    
    // Intermediate calculation precision
    val intermediatePrecision: Int = 4,
    
    // Tax rounding
    val taxRoundingMode: RoundingMode = RoundingMode.HALF_UP,
    val roundTaxPerItem: Boolean = true,      // vs round only total
    
    // Quantity rounding
    val quantityDecimals: Int = 3,            // For weighted items
    val countDecimals: Int = 0,               // For counted items
    
    // Points rounding
    val pointsRoundingMode: RoundingMode = RoundingMode.DOWN  // Never round up points earned
)
```

### Tax Rounding Application

> **Tax Consistency Rule:** Tax MUST be calculated and rounded per unit FIRST, 
> then multiplied by quantity. This ensures that 1 customer buying 3 items 
> pays the same total tax as 3 customers each buying 1 item.

```kotlin
// CORRECT: Round per unit, then multiply by quantity
fun calculateLineTaxCorrect(item: TransactionItemViewModel): BigDecimal {
    // Step 1: Calculate tax for ONE unit
    val taxPerUnit = (item.finalPrice * item.taxPercentSum) / BigDecimal(100)
    
    // Step 2: Round the per-unit tax
    val roundedTaxPerUnit = taxPerUnit.setScale(2, RoundingMode.HALF_UP)
    
    // Step 3: Multiply rounded per-unit tax by quantity
    val lineTotal = roundedTaxPerUnit * item.quantityUsed
    
    // Step 4: Round the line total (handles weighted items)
    return lineTotal.setScale(2, RoundingMode.HALF_UP)
}

// WRONG: Tax on line total creates inconsistency - DO NOT USE
// This creates a "bulk tax discount" - UNFAIR!
// Example: $2.69 item, 9.5% tax
// WRONG: 3 items = $8.07 × 9.5% = $0.77 tax
// CORRECT: $0.26/unit × 3 = $0.78 tax
```

---

## Calculation Triggers

### When to Recalculate

```kotlin
// Full recalculation triggers
fun onItemAdded(newItem: TransactionItemViewModel) {
    calculateItem(newItem)
    evaluatePromotions(transaction)  // May affect other items
    recalculateTransactionTotals(transaction)
}

fun onItemRemoved(item: TransactionItemViewModel) {
    item.isRemoved = true
    evaluatePromotions(transaction)  // Promotions may no longer qualify
    recalculateTransactionTotals(transaction)
}

fun onItemQuantityChanged(item: TransactionItemViewModel) {
    calculateItem(item)
    evaluatePromotions(transaction)
    recalculateTransactionTotals(transaction)
}

fun onDiscountApplied(affectedItem: TransactionItemViewModel) {
    calculateItem(affectedItem)
    recalculateTransactionTotals(transaction)
}

fun onPaymentApplied(payment: Payment) {
    if (payment.type in listOf(TransactionPaymentType.EBTFoodstamp, TransactionPaymentType.WIC)) {
        applyBenefitPaymentToItems(payment)
        // Tax changes, must recalculate
        for (item in affectedItems) {
            calculateItem(item)
        }
    }
    recalculateTransactionTotals(transaction)
}
```

---

## Performance Optimization

### Lazy Calculation

```kotlin
class TransactionItemViewModel {
    var needsRecalculation: Boolean = false
    
    fun setQuantityUsed(value: BigDecimal) {
        if (this.quantityUsed != value) {
            this.quantityUsed = value
            this.needsRecalculation = true
        }
    }
}

fun recalculateIfNeeded(transaction: Transaction) {
    val itemsToRecalc = transaction.items.filter { it.needsRecalculation }
    
    if (itemsToRecalc.isEmpty()) return  // Nothing to do
    
    for (item in itemsToRecalc) {
        calculateItem(item)
        item.needsRecalculation = false
    }
    
    // Always recalc totals if any item changed
    recalculateTransactionTotals(transaction)
}
```

### Caching

```kotlin
class CalculationCache {
    private val productCache = mutableMapOf<Int, ProductViewModel>()
    private val taxRateCache = mutableMapOf<Int, List<TaxRate>>()
    private val activePromotionsCache = mutableMapOf<Int, List<PromotionDefinition>>()
    private var cacheExpiry: OffsetDateTime? = null
    
    fun getProduct(id: Int): ProductViewModel {
        return productCache.getOrPut(id) {
            Manager.product.getById(id)
        }
    }
    
    fun getTaxRates(productId: Int): List<TaxRate> {
        return taxRateCache.getOrPut(productId) {
            loadTaxRates(productId)
        }
    }
    
    fun invalidate() {
        productCache.clear()
        taxRateCache.clear()
        activePromotionsCache.clear()
    }
}
```

---

[← Back to Index](./INDEX.md) | [Previous: Returns & Adjustments](./RETURNS_ADJUSTMENTS.md) | [Next: Examples →](./EXAMPLES.md)

