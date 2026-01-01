# Calculation Gaps and Future Enhancements

[← Back to Index](./INDEX.md)

---

## Overview

This document identifies **calculation-specific gaps** in the current GroPOS specification. Features outside the scope of point-of-sale transaction calculations (e.g., omnichannel fulfillment, fuel rewards programs, enterprise integrations) are intentionally excluded from this document—those belong in operational or integration documentation.

---

## Scope Clarification

### ✅ In Scope for This Documentation

| Topic | Why It Belongs |
|-------|----------------|
| Payment method processing | Affects split tender, tax exemption, change calculation |
| Tip calculations | Part of transaction total calculation |
| Currency handling | Affects rounding and precision |
| Multi-currency | Affects exchange rate calculations at POS |
| Layaway calculations | Installment math at time of sale |
| Price-inclusive tax | Reverse tax calculation |
| Compound tax | Tax-on-tax calculation |
| Rounding edge cases | Precision improvements |

### ❌ Out of Scope for This Documentation

| Topic | Where It Belongs |
|-------|------------------|
| BOPIS/Omnichannel | Order Fulfillment Documentation |
| Fuel Rewards Programs | Loyalty Program Documentation |
| Subscription Programs | E-Commerce/Recurring Orders Documentation |
| Pharmacy Integration | External Systems Integration Documentation |
| Loss Prevention Alerts | Security/Monitoring Documentation |
| Multi-store Sync | Backend/Admin Documentation |
| Analytics/Reporting | Business Intelligence Documentation |

---

## Priority 1: Modern Payment Method Calculations

### Mobile Wallet Processing

Mobile wallets (Apple Pay, Google Pay, Samsung Pay) function identically to card payments from a **calculation perspective**:

```kotlin
// Mobile wallet payment processing
fun processMobileWalletPayment(
    transaction: Transaction,
    walletType: MobileWalletType,
    amount: BigDecimal
): PaymentResult {
    // From a CALCULATION standpoint, treat as card payment
    // The difference is in the hardware/tokenization layer, not calculation
    
    val payment = TransactionPaymentViewModel().apply {
        paymentTypeId = mapWalletToPaymentType(walletType)
        value = amount
    }
    
    // Same tax rules as credit/debit
    // Same split tender rules
    // Same change calculation (no change for mobile wallet)
    
    return processPayment(transaction, payment)
}

// Wallet type mapping for calculation purposes
fun mapWalletToPaymentType(walletType: MobileWalletType): PaymentType {
    // All mobile wallets behave like credit for calculation purposes
    return PaymentType.Credit
}
```

**Calculation Impact:** None—mobile wallets use existing credit card calculation logic.

### Buy Now Pay Later (BNPL) Calculations

BNPL services (Affirm, Klarna, Afterpay) introduce calculation considerations:

```kotlin
data class BNPLConfig(
    val minimumOrderAmount: BigDecimal = BigDecimal("35.00"),
    val maximumOrderAmount: BigDecimal = BigDecimal("1000.00"),
    
    // Categories excluded from BNPL-eligible total
    val excludedCategories: List<ProductCategory> = listOf(
        ProductCategory.GIFT_CARDS,
        ProductCategory.TOBACCO,
        ProductCategory.ALCOHOL,
        ProductCategory.LOTTERY,
        ProductCategory.MONEY_ORDERS,
        ProductCategory.BILL_PAY
    )
)

// Calculate BNPL-eligible amount
fun calculateBNPLEligibleAmount(transaction: Transaction): BigDecimal {
    val config = getBNPLConfig()
    
    return transaction.items
        .filter { it.categoryId !in config.excludedCategories }
        .sumOf { it.subTotal + it.taxTotal }
}

// Validate BNPL payment
fun validateBNPLPayment(transaction: Transaction, bnplAmount: BigDecimal): Boolean {
    val config = getBNPLConfig()
    val eligibleAmount = calculateBNPLEligibleAmount(transaction)
    
    if (eligibleAmount < config.minimumOrderAmount) {
        throw BNPLMinimumNotMetException(
            "BNPL requires minimum $${config.minimumOrderAmount} " +
            "in eligible items. Current eligible: $$eligibleAmount"
        )
    }
    
    if (bnplAmount > eligibleAmount) {
        throw BNPLAmountExceedsEligibleException(
            "Cannot apply $$bnplAmount BNPL to $$eligibleAmount eligible"
        )
    }
    
    // BNPL can be part of split tender
    // Remaining amount paid by other methods
    return true
}
```

**Calculation Impact:** New eligibility rules for split tender allocation.

---

## Priority 2: Tip Calculation

For future deli, bakery, or prepared food counter expansion:

```kotlin
data class TipConfig(
    val tipsEnabled: Boolean = false,
    val tipSuggestedPercents: List<Int> = listOf(15, 18, 20, 25),
    val allowCustomTip: Boolean = true,
    val maxTipPercent: BigDecimal = BigDecimal("100"),
    val calculateTipOnTax: Boolean = false
)

// Calculate suggested tip amounts
fun calculateSuggestedTips(transaction: Transaction): List<TipSuggestion> {
    val config = getTipConfig()
    
    // Tips calculated on subtotal (before tax)
    var tipBase = transaction.subTotal
    
    // Some jurisdictions: tips on post-tax amount
    if (config.calculateTipOnTax) {
        tipBase = transaction.subTotal + transaction.taxTotal
    }
    
    return config.tipSuggestedPercents.map { percent ->
        val amount = (tipBase * BigDecimal(percent) / BigDecimal(100))
            .setScale(2, RoundingMode.HALF_UP)
        TipSuggestion(percent = percent, amount = amount)
    }
}

// Apply tip to transaction
fun applyTip(transaction: Transaction, tipAmount: BigDecimal): Transaction {
    val config = getTipConfig()
    
    // Validate tip
    val maxAllowed = transaction.subTotal * (config.maxTipPercent / BigDecimal(100))
    if (tipAmount > maxAllowed) {
        throw TipExceedsMaximumException()
    }
    
    transaction.tipAmount = tipAmount
    transaction.grandTotal += tipAmount
    
    // Track for payment processing
    // Tip typically goes on same payment method as main payment
    return transaction
}
```

**Calculation Impact:** New line item in transaction totals.

---

## Priority 3: Multi-Currency Calculations

For stores near borders or in tourist areas:

```kotlin
data class MultiCurrencyConfig(
    val baseCurrency: String = "USD",
    val acceptedCurrencies: List<AcceptedCurrency>,
    val useRealTimeRates: Boolean = false,
    val rateMargin: BigDecimal = BigDecimal("3.0")  // 3% margin on exchange
)

data class AcceptedCurrency(
    val currencyCode: String,              // "CAD", "EUR", "MXN"
    val exchangeRate: BigDecimal,          // To USD
    val rateDate: OffsetDateTime,          // When rate was set
    val minimumAccepted: BigDecimal        // Min payment in this currency
)

// Calculate payment in foreign currency
fun calculateForeignCurrencyPayment(
    usdAmount: BigDecimal,
    foreignCurrency: String
): ForeignCurrencyResult {
    val config = getMultiCurrencyConfig()
    val currency = getAcceptedCurrency(foreignCurrency)
        ?: throw CurrencyNotAcceptedException()
    
    // Apply exchange rate with margin
    val effectiveRate = currency.exchangeRate * 
        (BigDecimal.ONE + config.rateMargin / BigDecimal(100))
    
    val foreignAmount = (usdAmount * effectiveRate)
        .setScale(2, RoundingMode.HALF_UP)
    
    return ForeignCurrencyResult(
        usdAmount = usdAmount,
        foreignAmount = foreignAmount,
        currency = foreignCurrency,
        rateUsed = effectiveRate
    )
}

// Calculate change in customer's preferred currency
fun calculateMultiCurrencyChange(
    paidForeign: BigDecimal,
    foreignCurrency: String,
    owedUsd: BigDecimal
): ChangeResult {
    val currency = getAcceptedCurrency(foreignCurrency)
    
    // Convert paid amount to USD
    val paidUsd = paidForeign / currency.exchangeRate
    val changeUsd = paidUsd - owedUsd
    
    // Change always given in USD
    // (Most stores don't stock foreign currency for change)
    return ChangeResult(
        changeAmount = changeUsd.setScale(2, RoundingMode.HALF_UP),
        changeCurrency = "USD"
    )
}
```

**Calculation Impact:** Exchange rate math in payment processing.

---

## Priority 4: Tax-Inclusive Pricing

For stores that display prices with tax included:

```kotlin
data class TaxInclusiveConfig(
    val useTaxInclusivePricing: Boolean = false,
    val taxInclusiveCategories: List<ProductCategory>?  // Or all items
)

// Reverse-calculate tax from inclusive price
fun extractTaxFromInclusivePrice(
    inclusivePrice: BigDecimal,
    taxRate: BigDecimal
): TaxExtractionResult {
    // Formula: Tax = InclusivePrice - (InclusivePrice / (1 + TaxRate))
    // Or: BasePrice = InclusivePrice / (1 + TaxRate)
    
    val taxMultiplier = BigDecimal.ONE + (taxRate / BigDecimal(100))
    var basePrice = inclusivePrice / taxMultiplier
    basePrice = basePrice.setScale(4, RoundingMode.HALF_UP)  // Keep precision
    
    var taxAmount = inclusivePrice - basePrice
    taxAmount = taxAmount.setScale(2, RoundingMode.HALF_UP)
    
    return TaxExtractionResult(
        basePrice = basePrice.setScale(2, RoundingMode.HALF_UP),
        taxAmount = taxAmount,
        totalPrice = inclusivePrice
    )
}

// Apply discount to tax-inclusive item
fun applyDiscountToTaxInclusiveItem(
    item: TransactionItemViewModel,
    discountPercent: BigDecimal
): TransactionItemViewModel {
    // Discount applies to full inclusive price
    val discountAmount = item.inclusivePrice * (discountPercent / BigDecimal(100))
    val newInclusivePrice = item.inclusivePrice - discountAmount
    
    // Re-extract tax from new inclusive price
    val extracted = extractTaxFromInclusivePrice(newInclusivePrice, item.taxPercentSum)
    
    item.priceUsed = extracted.basePrice
    item.taxPerUnit = extracted.taxAmount / item.quantityUsed
    item.finalPrice = extracted.basePrice
    
    return item
}
```

**Calculation Impact:** Reverse tax calculation, discount application on inclusive prices.

---

## Priority 5: Compound Tax Calculations

For jurisdictions with tax-on-tax:

```kotlin
data class CompoundTaxConfig(
    // Some jurisdictions charge tax on other taxes
    // Example: Quebec GST is taxable by QST
    val hasCompoundTax: Boolean = false,
    val compoundingRules: Map<TaxType, List<TaxType>>  // Which taxes compound
)

// Calculate compound tax
fun calculateCompoundTax(
    baseAmount: BigDecimal,
    taxes: List<TaxRate>
): TaxResult {
    // Separate into base taxes and compounding taxes
    val baseTaxes = taxes.filter { !it.isCompound }
    val compoundTaxes = taxes.filter { it.isCompound }
    
    // Calculate base taxes
    var baseTaxTotal = BigDecimal.ZERO
    for (tax in baseTaxes) {
        baseTaxTotal += baseAmount * (tax.rate / BigDecimal(100))
    }
    
    // Calculate compound taxes (tax on tax)
    var compoundTaxTotal = BigDecimal.ZERO
    for (tax in compoundTaxes) {
        // Compound tax applies to base + base taxes
        val compoundBase = baseAmount + baseTaxTotal
        compoundTaxTotal += compoundBase * (tax.rate / BigDecimal(100))
    }
    
    return TaxResult(
        baseTaxes = baseTaxTotal,
        compoundTaxes = compoundTaxTotal,
        totalTax = (baseTaxTotal + compoundTaxTotal).setScale(2, RoundingMode.HALF_UP)
    )
}

// Example: Quebec
// GST = 5% on $100 = $5.00
// QST = 9.975% on ($100 + $5) = $10.47
// Total tax = $15.47
```

**Calculation Impact:** Multi-layer tax calculation.

---

## Priority 6: Layaway Calculations

For stores offering layaway programs:

```kotlin
data class LayawayConfig(
    val minimumDownPaymentPercent: BigDecimal = BigDecimal("20"),
    val maxLayawayDays: Int = 90,
    val serviceFee: BigDecimal = BigDecimal("5.00"),
    val serviceFeeTaxable: Boolean = false,
    val cancellationFeePercent: BigDecimal = BigDecimal("10")
)

// Create layaway
fun createLayaway(transaction: Transaction, downPayment: BigDecimal): Layaway {
    val config = getLayawayConfig()
    var totalAmount = transaction.grandTotal
    
    // Validate down payment
    val minDownPayment = totalAmount * (config.minimumDownPaymentPercent / BigDecimal(100))
    if (downPayment < minDownPayment) {
        throw InsufficientDownPaymentException("Minimum down payment: $$minDownPayment")
    }
    
    // Add service fee if applicable
    var serviceFee = BigDecimal.ZERO
    if (config.serviceFee > BigDecimal.ZERO) {
        serviceFee = config.serviceFee
        if (config.serviceFeeTaxable) {
            serviceFee += calculateTax(config.serviceFee, transaction.taxRate)
        }
        totalAmount += serviceFee
    }
    
    val remainingBalance = totalAmount - downPayment
    
    return Layaway(
        transactionId = transaction.id,
        totalAmount = totalAmount,
        downPayment = downPayment,
        remainingBalance = remainingBalance,
        serviceFee = serviceFee,
        dueDate = LocalDate.now().plusDays(config.maxLayawayDays.toLong()),
        payments = mutableListOf(LayawayPayment(amount = downPayment, date = OffsetDateTime.now()))
    )
}

// Cancel layaway
fun cancelLayaway(layaway: Layaway): LayawayCancellation {
    val config = getLayawayConfig()
    val totalPaid = layaway.payments.sumOf { it.amount }
    
    val cancellationFee = layaway.totalAmount * (config.cancellationFeePercent / BigDecimal(100))
    val refundAmount = maxOf(BigDecimal.ZERO, totalPaid - cancellationFee)
    
    return LayawayCancellation(
        totalPaid = totalPaid,
        cancellationFee = cancellationFee,
        refundAmount = refundAmount
    )
}
```

**Calculation Impact:** Installment tracking and fee calculations.

---

## Priority 7: Rounding Edge Cases

### Sub-Penny Rounding Accumulation

```kotlin
// Issue: When splitting promotions across items, 
// sub-penny amounts can accumulate errors

// Example: 3 for $5 on items normally $2.00 each
// Per-item discount = $1.00 / 3 = $0.333333...
// If rounded: 3 × $0.33 = $0.99 (lost $0.01)

// Solution: Apply remainder to last item
fun allocateDiscountWithRemainder(
    totalDiscount: BigDecimal,
    items: List<TransactionItemViewModel>
) {
    val perItemDiscount = totalDiscount / BigDecimal(items.size)
    val roundedPerItem = perItemDiscount.setScale(2, RoundingMode.HALF_UP)
    
    // Calculate remainder
    val allocatedTotal = roundedPerItem * BigDecimal(items.size - 1)
    val lastItemDiscount = totalDiscount - allocatedTotal
    
    // Apply to items
    for (i in 0 until items.size - 1) {
        items[i].promotionDiscountPerUnit = roundedPerItem
    }
    
    // Last item gets remainder
    items.last().promotionDiscountPerUnit = lastItemDiscount
}
```

### Negative Penny Prevention

```kotlin
// Issue: After all discounts, line total could be negative pennies

fun preventNegativeLine(item: TransactionItemViewModel) {
    val lineTotal = item.finalPrice * item.quantityUsed + item.taxTotal
    
    if (lineTotal < BigDecimal.ZERO) {
        // Adjust to zero minimum
        item.finalPrice = BigDecimal.ZERO
        item.taxTotal = BigDecimal.ZERO
        item.discountCapped = true
        item.discountCapNote = "Discount capped to prevent negative total"
    }
}
```

---

## Summary

The current GroPOS calculation specification is **comprehensive for standard food retail POS operations**. The gaps identified above are:

| Gap | Priority | Complexity | Impact |
|-----|----------|------------|--------|
| BNPL Eligibility | Medium | Low | New split tender rules |
| Tip Calculation | Low | Low | Future counter service |
| Multi-Currency | Low | Medium | Border/tourist stores |
| Tax-Inclusive | Medium | Medium | International operations |
| Compound Tax | Low | Medium | Specific jurisdictions |
| Layaway | Low | Medium | Specific retail models |
| Rounding Edge Cases | High | Low | Accuracy improvement |

Mobile wallet payments require **no calculation changes**—they use existing credit card logic.

---

[← Back to Index](./INDEX.md)

