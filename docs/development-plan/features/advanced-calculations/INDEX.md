# Advanced POS Transaction Calculations - Master Specification

**Version:** 2.0 (Kotlin/Compose)  
**Status:** Specification Document  
**Purpose:** Enterprise-grade POS transaction calculation logic for food retailers

---

## Executive Summary

This specification defines a comprehensive transaction calculation engine capable of handling all pricing, discount, promotion, tax, and payment scenarios encountered in modern food retail environments. The system supports operations from single-station convenience stores to 18+ lane supermarkets.

**Implementation:** Kotlin Multiplatform with Compose UI

---

## Document Structure

This specification is organized into the following modules:

| Module | Document | Description |
|--------|----------|-------------|
| 1 | [CORE_CONCEPTS.md](./CORE_CONCEPTS.md) | Foundational data models and calculation principles |
| 2 | [PRICE_DETERMINATION.md](./PRICE_DETERMINATION.md) | Price hierarchy, sale prices, bulk pricing |
| 3 | [PROMOTIONS.md](./PROMOTIONS.md) | Mix & Match, BOGO, multi-buy, threshold promotions |
| 4 | [DISCOUNTS.md](./DISCOUNTS.md) | Line, invoice, customer, employee, coupons |
| 5 | [CUSTOMER_PRICING.md](./CUSTOMER_PRICING.md) | Loyalty, customer groups, VIP, profile-based pricing |
| 6 | [TAX_CALCULATIONS.md](./TAX_CALCULATIONS.md) | Multi-jurisdiction tax, exemptions, holidays |
| 7 | [GOVERNMENT_BENEFITS.md](./GOVERNMENT_BENEFITS.md) | SNAP/EBT, WIC, tax exemption rules |
| 8 | [DEPOSITS_FEES.md](./DEPOSITS_FEES.md) | CRV, bottle deposits, bag fees, environmental fees |
| 9 | [PAYMENT_PROCESSING.md](../PAYMENT_PROCESSING.md) | Split tender, payment application order |
| 10 | [RETURNS_ADJUSTMENTS.md](./RETURNS_ADJUSTMENTS.md) | Full/partial returns, exchanges, SNAP refunds |
| 11 | [CALCULATION_ENGINE.md](./CALCULATION_ENGINE.md) | Calculation sequence, rounding, precision |
| 12 | [EXAMPLES.md](./EXAMPLES.md) | Comprehensive worked examples |
| 13 | [ADDITIONAL_FEATURES.md](./ADDITIONAL_FEATURES.md) | Age verification, lottery, gift cards |
| 14 | [GAPS_AND_FUTURE.md](./GAPS_AND_FUTURE.md) | Modern payments, tips, multi-currency, edge cases |
| 15 | [CLOUD_DATA_TRANSMISSION.md](./CLOUD_DATA_TRANSMISSION.md) | API payloads, backend sync requirements |

---

## Key Capabilities

### Pricing Features
- ✅ Multi-tier price hierarchy (Prompted → Customer → Sale → Bulk → Retail)
- ✅ Bulk/volume pricing with quantity breaks
- ✅ Time-based and day-based sale prices
- ✅ Customer group-specific pricing
- ✅ Competitor price matching
- ✅ Clearance and markdown management

### Promotion Features
- ✅ Mix and Match across categories
- ✅ Buy X Get Y (BOGO, BXGY)
- ✅ Multi-buy discounts (3 for $5)
- ✅ Threshold promotions (spend $X, save $Y)
- ✅ Bundle/combo deals
- ✅ Cross-category promotions
- ✅ Stackable vs exclusive promotions

### Discount Features
- ✅ Manufacturer coupons
- ✅ Store coupons (digital and paper)
- ✅ Customer loyalty discounts
- ✅ Employee discounts
- ✅ Senior/military/student discounts
- ✅ Invoice-level discounts
- ✅ Item-specific customer discounts
- ✅ Coupon stacking rules

### Tax Features
- ✅ Multi-jurisdiction taxation
- ✅ Tax-exempt customers and items
- ✅ Tax holidays
- ✅ SNAP tax exemption
- ✅ CRV always taxable (California law); other deposits configurable
- ✅ Tax-inclusive pricing support

### Government Benefits
- ✅ EBT SNAP (Food Stamps)
- ✅ EBT Cash Benefits
- ✅ WIC with CVB (Cash Value Benefit)
- ✅ Dual benefit transactions
- ✅ Proper benefit allocation

### Payment Features
- ✅ Split tender (multiple payment types)
- ✅ Optimal payment application
- ✅ Change calculation
- ✅ Overpayment handling
- ✅ Partial payments

### Returns Features
- ✅ Full and partial transaction returns
- ✅ Exchange processing
- ✅ No-receipt returns with limits and fraud prevention
- ✅ SNAP benefit refunds back to EBT card
- ✅ Cross-store returns
- ✅ Promotion return recalculation (BOGO, Mix & Match)

---

## Calculation Priority Order

The system processes calculations in this strict order:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    CALCULATION PRIORITY ORDER                           │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  PHASE 1: ITEM SETUP                                                    │
│  ════════════════════                                                   │
│  1.1 Determine base price (retail/sale/prompted)                        │
│  1.2 Apply bulk/volume pricing                                          │
│  1.3 Apply customer-specific base pricing                               │
│  1.4 Add deposits and fees (CRV, bottle, bag)                           │
│                                                                         │
│  PHASE 2: PROMOTIONAL DISCOUNTS                                         │
│  ══════════════════════════════                                         │
│  2.1 Identify qualifying promotions                                     │
│  2.2 Apply Mix & Match promotions                                       │
│  2.3 Apply BOGO/BXGY promotions                                         │
│  2.4 Apply multi-buy promotions (3 for $5)                              │
│  2.5 Apply threshold promotions                                         │
│  2.6 Select best promotion per item (if exclusive)                      │
│                                                                         │
│  PHASE 3: COUPON DISCOUNTS                                              │
│  ═════════════════════════                                              │
│  3.1 Validate coupon eligibility                                        │
│  3.2 Apply manufacturer coupons                                         │
│  3.3 Apply store coupons                                                │
│  3.4 Enforce stacking rules                                             │
│  3.5 Apply coupon limits                                                │
│                                                                         │
│  PHASE 4: CUSTOMER DISCOUNTS                                            │
│  ═══════════════════════════                                            │
│  4.1 Apply loyalty rewards/points redemption                            │
│  4.2 Apply customer group discounts                                     │
│  4.3 Apply employee discounts                                           │
│  4.4 Apply item-specific customer discounts                             │
│                                                                         │
│  PHASE 5: MANUAL DISCOUNTS                                              │
│  ═════════════════════════                                              │
│  5.1 Apply line-item manual discounts                                   │
│  5.2 Enforce floor price (request override if needed)                   │
│  5.3 Apply invoice-level discounts                                      │
│                                                                         │
│  PHASE 6: TAX CALCULATION                                               │
│  ════════════════════════                                               │
│  6.1 Determine taxable amount (final price after all discounts)         │
│  6.2 Identify tax exemptions (item-level, customer-level)               │
│  6.3 Apply tax rates by jurisdiction                                    │
│  6.4 Calculate per-item and total tax                                   │
│                                                                         │
│  PHASE 7: TRANSACTION TOTALS                                            │
│  ═══════════════════════════                                            │
│  7.1 Sum all line subtotals                                             │
│  7.2 Sum all line taxes                                                 │
│  7.3 Sum all deposits/fees                                              │
│  7.4 Calculate grand total                                              │
│  7.5 Calculate SNAP-eligible amount                                     │
│  7.6 Calculate WIC-eligible amount                                      │
│                                                                         │
│  PHASE 8: PAYMENT APPLICATION                                           │
│  ═════════════════════════════                                          │
│  8.1 Apply WIC payments first (most restrictive)                        │
│  8.2 Apply SNAP/EBT payments (recalculate tax exemption)                │
│  8.3 Apply other payments (cards, cash)                                 │
│  8.4 Calculate change due                                               │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Quick Reference: Kotlin Formulas

### Line Item Calculations

```kotlin
// Determine price to use
fun getPriceUsed(item: TransactionItem): BigDecimal = when {
    item.isPromptedPrice && item.promptedPrice != null -> item.promptedPrice
    item.customerPrice != null && item.customerPrice > BigDecimal.ZERO -> item.customerPrice
    item.salePrice != null && item.salePrice > BigDecimal.ZERO -> item.salePrice
    item.bulkPrice != null && item.isBulkPriced -> item.bulkPrice
    else -> item.retailPrice
}

// Calculate final price after discounts
fun getFinalPrice(item: TransactionItem): BigDecimal {
    val discountedPrice = item.priceUsed -
        item.promotionDiscountPerUnit -
        item.couponDiscountPerUnit -
        item.customerDiscountPerUnit -
        item.discountAmountPerUnit -
        item.transactionDiscountAmountPerUnit
    
    val enforced = if (item.isFloorPriceOverridden) {
        discountedPrice
    } else {
        maxOf(discountedPrice, item.floorPrice)
    }
    
    return enforced + item.crvRatePerUnit + item.bottleDepositPerUnit
}

// Calculate tax per unit (round FIRST, then multiply)
fun calculateTaxPerUnit(finalPrice: BigDecimal, taxPercentSum: BigDecimal): BigDecimal =
    (finalPrice * taxPercentSum / BigDecimal(100)).setScale(2, RoundingMode.HALF_UP)

// Calculate line tax (accounting for SNAP exemption)
fun calculateLineTax(item: TransactionItem): BigDecimal {
    val snapFraction = item.snapPaidPercent / BigDecimal(100)
    return (item.taxPerUnit * item.quantityUsed * (BigDecimal.ONE - snapFraction))
        .setScale(2, RoundingMode.HALF_UP)
}

// Calculate line subtotal
fun calculateLineSubtotal(item: TransactionItem): BigDecimal =
    (item.finalPrice * item.quantityUsed).setScale(2, RoundingMode.HALF_UP)
```

### Transaction Calculations

```kotlin
fun calculateTransactionTotals(items: List<TransactionItem>): TransactionTotals {
    val activeItems = items.filter { !it.isRemoved }
    
    return TransactionTotals(
        subtotal = activeItems.sumOf { it.subTotal }.setScale(2, RoundingMode.HALF_UP),
        taxTotal = activeItems.sumOf { it.taxTotal }.setScale(2, RoundingMode.HALF_UP),
        depositTotal = activeItems.sumOf { it.totalDepositsPerUnit * it.quantityUsed }
            .setScale(2, RoundingMode.HALF_UP),
        savingsTotal = activeItems.sumOf { it.savingsTotal }.setScale(2, RoundingMode.HALF_UP),
        grandTotal = activeItems.sumOf { it.subTotal + it.taxTotal }
            .setScale(2, RoundingMode.HALF_UP),
        snapEligible = activeItems.filter { it.isSNAPEligible }
            .sumOf { it.subjectToTaxTotal }.setScale(2, RoundingMode.HALF_UP),
        wicEligible = activeItems.filter { it.isWICEligible }
            .sumOf { it.subTotal }.setScale(2, RoundingMode.HALF_UP)
    )
}

// Change calculation
fun calculateChangeDue(
    grandTotal: BigDecimal,
    payments: List<Payment>,
    cashTendered: BigDecimal
): BigDecimal {
    val totalPaid = payments.sumOf { it.amount }
    val amountDue = grandTotal - totalPaid
    return if (amountDue <= BigDecimal.ZERO) {
        cashTendered - amountDue.abs()
    } else {
        BigDecimal.ZERO
    }
}
```

---

## Key Business Rules Summary

### Discount Stacking Rules

| Discount Type | Can Stack With | Cannot Stack With |
|---------------|----------------|-------------------|
| Sale Price | Coupons (usually) | Other sale prices |
| Mix & Match | Nothing (exclusive) | All other promotions |
| BOGO | Store coupons (sometimes) | Mix & Match |
| Manufacturer Coupon | Store coupon, loyalty | Another mfr coupon on same item |
| Store Coupon | Mfr coupon, sale price | Another store coupon on same item |
| Customer Group % | Employee discount | Other customer discounts |
| Employee Discount | Nothing | All other discounts |
| Invoice Discount | Excludes items with line discounts | Line discounts |

### Floor Price Hierarchy

```
1. ALWAYS HONORED: Cost floor (never sell below cost)
2. OVERRIDE ALLOWED: Store floor price (manager approval)
3. EXEMPT: Corporate-set sale prices (can go below floor)
4. EXEMPT: Clearance/markdown pricing
```

### Tax Application Rules

```
Tax is calculated on: Final Price AFTER all discounts
Tax includes: CRV and deposits (jurisdiction-dependent)
Tax exempt: SNAP-paid portion, WIC items, tax-exempt customers
```

---

## Naming Conventions (Kotlin)

| Legacy (Java) | New (Kotlin) |
|---------------|--------------|
| `isFoodStampable` | `isSNAPEligible` |
| `isFoodStampEligible` | `isSNAPEligible` |
| `foodStampable` | `snapEligible` |
| `ebtEligibleTotal` | `snapEligibleTotal` |
| `GrowPOS` | `GroPOS` |

---

## Document Conventions

Throughout this specification:

- **MUST** - Absolute requirement
- **SHOULD** - Recommended but not mandatory
- **MAY** - Optional feature
- **Currency** - All amounts in USD with 2 decimal precision
- **Percentage** - Stored as decimal (e.g., 9.5% = 9.500)
- **Quantity** - 3 decimal precision for weighted items

---

## Next Steps

Proceed to [CORE_CONCEPTS.md](./CORE_CONCEPTS.md) for foundational data models.

---

*Last Updated: January 2026*

