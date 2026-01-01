# Core Concepts and Data Models

[← Back to Index](./INDEX.md) | [Next: Price Determination →](./PRICE_DETERMINATION.md)

---

## Transaction Data Model

### Transaction (Header)

The transaction header contains summary information and metadata:

```kotlin
data class Transaction(
    // Identification
    val guid: String,                           // Unique transaction identifier
    val id: Int? = null,                        // Database ID
    val transactionNumber: String? = null,      // Human-readable number
    
    // Timing
    val startDate: Instant,                     // When first item scanned
    val completedDate: Instant? = null,         // When transaction finalized
    
    // Status
    val status: TransactionStatus = TransactionStatus.InProgress,
    
    // Counts
    val rowCount: Int = 0,                      // Total line items
    val itemCount: Int = 0,                     // Non-removed items
    val uniqueProductCount: Int = 0,            // Distinct products
    val uniqueSaleProductCount: Int = 0,        // Distinct products on sale
    val totalPurchaseCount: BigDecimal = BigDecimal.ZERO,  // Sum of quantities
    
    // Financial Totals
    val subTotal: BigDecimal = BigDecimal.ZERO,       // Sum of line subtotals
    val taxTotal: BigDecimal = BigDecimal.ZERO,       // Sum of line taxes
    val crvTotal: BigDecimal = BigDecimal.ZERO,       // Sum of CRV deposits
    val depositTotal: BigDecimal = BigDecimal.ZERO,   // Sum of all deposits
    val savingsTotal: BigDecimal = BigDecimal.ZERO,   // Sum of all savings
    val grandTotal: BigDecimal = BigDecimal.ZERO,     // Final amount due
    val costTotal: BigDecimal = BigDecimal.ZERO,      // Sum of costs (for margin)
    
    // Benefit Tracking
    val snapEligibleTotal: BigDecimal = BigDecimal.ZERO,  // Amount payable by SNAP
    val wicEligibleTotal: BigDecimal = BigDecimal.ZERO,   // Amount payable by WIC
    val snapAppliedTotal: BigDecimal = BigDecimal.ZERO,   // SNAP actually applied
    val wicAppliedTotal: BigDecimal = BigDecimal.ZERO,    // WIC actually applied
    
    // Customer
    val customerId: Int? = null,                // Linked customer (if identified)
    val customerGroupId: Int? = null,           // Customer's group
    val employeeCustomerId: Int? = null,        // If employee transaction
    
    // Discounts Applied
    val invoiceDiscountPercent: BigDecimal = BigDecimal.ZERO,
    val invoiceDiscountAmount: BigDecimal = BigDecimal.ZERO,
    val invoiceDiscountEmployeeId: Int? = null,
    
    // Store/Register
    val storeId: Int,
    val registerId: Int,
    val cashierId: Int
)
```

### TransactionItem (Line Item)

Each line item contains all pricing and calculation data:

```kotlin
data class TransactionItem(
    // ═══════════════════════════════════════════════════════════════════════
    // IDENTIFICATION
    // ═══════════════════════════════════════════════════════════════════════
    val id: Int? = null,                        // Database ID
    val transactionGuid: String,                // Parent transaction
    val transactionItemGuid: String = UUID.randomUUID().toString(),
    val branchProductId: Int,                   // Product ID
    val branchProductName: String,              // Product name
    val itemNumber: String,                     // UPC/PLU scanned
    val rowNumber: Int = 0,                     // Display order
    val scanDate: Instant = Clock.System.now(),
    
    // ═══════════════════════════════════════════════════════════════════════
    // BASE PRICING
    // ═══════════════════════════════════════════════════════════════════════
    val retailPrice: BigDecimal,                // Standard retail price
    val salePrice: BigDecimal? = null,          // Active sale price
    val promptedPrice: BigDecimal? = null,      // Manual entry price
    val isPromptedPrice: Boolean = false,       // Was price manually entered
    val priceUsed: BigDecimal,                  // Effective price before discounts
    val floorPrice: BigDecimal = BigDecimal.ZERO,
    val cost: BigDecimal = BigDecimal.ZERO,     // Product cost (for margin)
    
    // Bulk Pricing
    val bulkPrice: BigDecimal? = null,          // Bulk unit price (if applicable)
    val bulkQuantityThreshold: Int? = null,     // Qty to trigger bulk pricing
    val isBulkPriced: Boolean = false,          // Is bulk pricing applied
    
    // ═══════════════════════════════════════════════════════════════════════
    // DEPOSITS AND FEES
    // ═══════════════════════════════════════════════════════════════════════
    val crvRatePerUnit: BigDecimal = BigDecimal.ZERO,
    val bottleDepositPerUnit: BigDecimal = BigDecimal.ZERO,
    val bagFeePerUnit: BigDecimal = BigDecimal.ZERO,
    val otherFeesPerUnit: BigDecimal = BigDecimal.ZERO,
    val totalDepositsPerUnit: BigDecimal = BigDecimal.ZERO,
    
    // ═══════════════════════════════════════════════════════════════════════
    // QUANTITY
    // ═══════════════════════════════════════════════════════════════════════
    val quantitySold: BigDecimal = BigDecimal.ONE,
    val quantityReturned: BigDecimal = BigDecimal.ZERO,
    val quantityUsed: BigDecimal = BigDecimal.ONE,  // Net: sold - returned
    val isManualQuantity: Boolean = false,
    val isRemoved: Boolean = false,
    val isWeighted: Boolean = false,
    
    // ═══════════════════════════════════════════════════════════════════════
    // PROMOTIONAL DISCOUNTS
    // ═══════════════════════════════════════════════════════════════════════
    val promotionId: Int? = null,
    val promotionName: String? = null,
    val promotionType: PromotionType? = null,
    val promotionDiscountPerUnit: BigDecimal = BigDecimal.ZERO,
    val promotionDiscountTotal: BigDecimal = BigDecimal.ZERO,
    val isPromotionTrigger: Boolean = false,
    val isPromotionReward: Boolean = false,
    val promotionGroupId: String? = null,
    
    // ═══════════════════════════════════════════════════════════════════════
    // COUPON DISCOUNTS
    // ═══════════════════════════════════════════════════════════════════════
    val couponDiscountPerUnit: BigDecimal = BigDecimal.ZERO,
    val couponDiscountTotal: BigDecimal = BigDecimal.ZERO,
    
    // ═══════════════════════════════════════════════════════════════════════
    // CUSTOMER DISCOUNTS
    // ═══════════════════════════════════════════════════════════════════════
    val customerDiscountId: Int? = null,
    val customerDiscountType: CustomerDiscountType? = null,
    val customerDiscountPercent: BigDecimal = BigDecimal.ZERO,
    val customerDiscountPerUnit: BigDecimal = BigDecimal.ZERO,
    val customerDiscountTotal: BigDecimal = BigDecimal.ZERO,
    
    // ═══════════════════════════════════════════════════════════════════════
    // MANUAL DISCOUNTS (LINE LEVEL)
    // ═══════════════════════════════════════════════════════════════════════
    val discountTypeId: DiscountType? = null,
    val discountTypeAmount: BigDecimal = BigDecimal.ZERO,
    val discountAmountPerUnit: BigDecimal = BigDecimal.ZERO,
    
    // ═══════════════════════════════════════════════════════════════════════
    // INVOICE-LEVEL DISCOUNT (ALLOCATED TO THIS LINE)
    // ═══════════════════════════════════════════════════════════════════════
    val transactionDiscountTypeId: DiscountType? = null,
    val transactionDiscountTypeAmount: BigDecimal = BigDecimal.ZERO,
    val transactionDiscountAmountPerUnit: BigDecimal = BigDecimal.ZERO,
    
    // ═══════════════════════════════════════════════════════════════════════
    // FLOOR PRICE OVERRIDE
    // ═══════════════════════════════════════════════════════════════════════
    val isFloorPriceOverridden: Boolean = false,
    val floorPriceOverrideEmployeeId: Int? = null,
    val floorPriceOverrideEmployee: String? = null,
    
    // ═══════════════════════════════════════════════════════════════════════
    // FINAL PRICE (AFTER ALL DISCOUNTS)
    // ═══════════════════════════════════════════════════════════════════════
    val finalPrice: BigDecimal,                 // Net price per unit (includes deposits)
    val finalPriceExcludingDeposits: BigDecimal = BigDecimal.ZERO,
    
    // ═══════════════════════════════════════════════════════════════════════
    // TAX
    // ═══════════════════════════════════════════════════════════════════════
    val taxPercentSum: BigDecimal = BigDecimal.ZERO,
    val taxPerUnit: BigDecimal = BigDecimal.ZERO,
    val taxTotal: BigDecimal = BigDecimal.ZERO,
    val taxes: List<TaxBreakdown> = emptyList(),
    val isTaxExempt: Boolean = false,
    val taxExemptReason: String? = null,
    
    // ═══════════════════════════════════════════════════════════════════════
    // SUBTOTALS
    // ═══════════════════════════════════════════════════════════════════════
    val subTotal: BigDecimal = BigDecimal.ZERO,           // finalPrice × quantityUsed
    val subjectToTaxTotal: BigDecimal = BigDecimal.ZERO,  // Taxable portion (after SNAP)
    val finalPriceTaxSum: BigDecimal = BigDecimal.ZERO,   // finalPrice + taxPerUnit
    val costTotal: BigDecimal = BigDecimal.ZERO,          // cost × quantityUsed
    
    // ═══════════════════════════════════════════════════════════════════════
    // SAVINGS TRACKING
    // ═══════════════════════════════════════════════════════════════════════
    val savingsPerUnit: BigDecimal = BigDecimal.ZERO,
    val savingsTotal: BigDecimal = BigDecimal.ZERO,
    val promotionSavings: BigDecimal = BigDecimal.ZERO,
    val couponSavings: BigDecimal = BigDecimal.ZERO,
    val customerSavings: BigDecimal = BigDecimal.ZERO,
    val manualSavings: BigDecimal = BigDecimal.ZERO,
    
    // ═══════════════════════════════════════════════════════════════════════
    // BENEFIT ELIGIBILITY
    // ═══════════════════════════════════════════════════════════════════════
    val isSNAPEligible: Boolean = false,        // EBT SNAP eligible
    val isWICEligible: Boolean = false,         // WIC eligible
    val wicCategoryId: Int? = null,             // WIC category for limits
    val wicMaxQuantity: BigDecimal? = null,     // WIC quantity limit
    val wicQuantityUsed: BigDecimal = BigDecimal.ZERO,
    
    // ═══════════════════════════════════════════════════════════════════════
    // PAYMENT TRACKING
    // ═══════════════════════════════════════════════════════════════════════
    val snapPaidAmount: BigDecimal = BigDecimal.ZERO,
    val snapPaidPercent: BigDecimal = BigDecimal.ZERO,
    val wicPaidAmount: BigDecimal = BigDecimal.ZERO,
    val wicPaidPercent: BigDecimal = BigDecimal.ZERO,
    val nonSNAPTotal: BigDecimal = BigDecimal.ZERO,
    val paidTotal: BigDecimal = BigDecimal.ZERO,
    
    // ═══════════════════════════════════════════════════════════════════════
    // RESTRICTIONS
    // ═══════════════════════════════════════════════════════════════════════
    val ageRestriction: AgeType = AgeType.NONE,
    val ageVerified: Boolean = false,
    val quantityLimit: Int? = null
) {
    /** Whether this item has a line-level discount applied */
    val hasLineDiscount: Boolean
        get() = discountTypeId != null && discountAmountPerUnit > BigDecimal.ZERO
}
```

---

## Supporting Data Classes

### Tax Breakdown

```kotlin
data class TaxBreakdown(
    val taxId: Int,
    val taxName: String,
    val taxRate: BigDecimal,      // e.g., 7.25 for 7.25%
    val amount: BigDecimal        // Calculated tax amount
)
```

### Payment

```kotlin
data class Payment(
    val paymentGuid: String = UUID.randomUUID().toString(),
    val paymentType: PaymentType,
    val amount: BigDecimal,
    val authCode: String? = null,
    val referenceNumber: String? = null,
    val cardType: String? = null,
    val lastFour: String? = null,
    val entryMode: String? = null,
    val status: PaymentStatus = PaymentStatus.Success
)

enum class PaymentType {
    CASH,
    CREDIT,
    DEBIT,
    EBT_SNAP,
    EBT_CASH,
    WIC,
    CHECK,
    GIFT_CARD,
    STORE_CREDIT
}

enum class PaymentStatus {
    Pending,
    Success,
    Declined,
    Error,
    Voided,
    Refund
}
```

---

## Enumerations

### Transaction Status

```kotlin
enum class TransactionStatus {
    InProgress,     // Currently being built
    OnHold,         // Suspended for later
    Completed,      // Successfully finalized
    Voided,         // Cancelled
    Returned,       // Full return
    PartialReturn,  // Partial return
    Errored         // Failed to sync
}
```

### Discount Types

```kotlin
enum class DiscountType {
    // Line-Level Discounts
    ItemPercentage,          // X% off the item
    ItemAmountPerUnit,       // $X off per unit
    ItemAmountTotal,         // Set total price for line
    
    // Transaction-Level Discounts
    TransactionPercentTotal, // X% off entire transaction
    TransactionAmountTotal   // $X off entire transaction
}

enum class CustomerDiscountType {
    GROUP,          // Customer group discount
    LOYALTY,        // Loyalty reward
    EMPLOYEE,       // Employee discount
    ITEM_SPECIFIC   // Customer's item-specific deal
}
```

### Promotion Types

```kotlin
enum class PromotionType {
    // Single Item Promotions
    SALE_PRICE,              // Simple reduced price
    PERCENT_OFF,             // X% off item
    AMOUNT_OFF,              // $X off item
    
    // Quantity-Based Promotions
    MULTI_BUY,               // Buy X for $Y (e.g., 3 for $5)
    BULK_PRICING,            // Volume discount tiers
    BUY_X_GET_Y_FREE,        // BOGO-style
    BUY_X_GET_Y_PERCENT_OFF, // Buy X get Y at Z% off
    BUY_X_GET_Y_AMOUNT_OFF,  // Buy X get Y at $Z off
    
    // Mix and Match
    MIX_MATCH_QUANTITY,      // Buy X items from group for $Y
    MIX_MATCH_SPEND,         // Spend $X on group, get Y% off
    MIX_MATCH_TIERED,        // Different rewards at different levels
    
    // Cross-Category
    CROSS_CATEGORY_BOGO,     // Buy from A, get B discounted
    BUNDLE,                  // Fixed price for item combination
    
    // Threshold
    SPEND_X_SAVE_Y,          // Spend $50, save $5
    SPEND_X_GET_ITEM         // Spend $50, get item free/discounted
}
```

### Age Restrictions

```kotlin
enum class AgeType {
    NONE,       // No restriction
    AGE_18,     // 18+ (tobacco, vape)
    AGE_21      // 21+ (alcohol)
}
```

---

## Calculation Precision Rules

### Rounding Rules

| Value Type | Precision | Rounding Mode | Example |
|------------|-----------|---------------|---------|
| Currency (display) | 2 decimals | HALF_UP | $1.995 → $2.00 |
| Currency (internal) | 4 decimals | HALF_UP | For intermediate calculations |
| Tax Rate | 3 decimals | HALF_UP | 9.500% |
| Quantity (count) | 0 decimals | HALF_UP | 3 items |
| Quantity (weight) | 3 decimals | HALF_UP | 1.234 lbs |
| Percentage | 2 decimals | HALF_UP | 10.00% |

### Kotlin Extension Functions for Precision

```kotlin
fun BigDecimal.toMoney(): BigDecimal = 
    this.setScale(2, RoundingMode.HALF_UP)

fun BigDecimal.toTaxPercent(): BigDecimal = 
    this.setScale(3, RoundingMode.HALF_UP)

fun BigDecimal.toQuantity(): BigDecimal = 
    this.setScale(3, RoundingMode.HALF_UP)

fun BigDecimal.toInternalPrecision(): BigDecimal = 
    this.setScale(4, RoundingMode.HALF_UP)
```

### Calculation Order for Rounding

```
FOR SUBTOTALS AND DISCOUNTS:
1. Calculate all per-unit values to 4 decimal places
2. Multiply by quantity
3. Round final line total to 2 decimal places
4. Sum all line totals for transaction total

FOR TAX (EXCEPTION):
1. Calculate tax for ONE UNIT
2. Round taxPerUnit to 2 decimal places FIRST
3. Multiply rounded taxPerUnit by quantity
4. Round final line tax total

Why the difference? Tax must be rounded per unit to ensure fairness:
- 1 customer buying 3 items pays the SAME tax as
- 3 customers each buying 1 item
```

---

## Item Calculation Flow

```kotlin
fun buildTransactionItem(
    product: Product,
    quantity: BigDecimal,
    priceCalculator: PriceCalculator,
    taxCalculator: TaxCalculator
): TransactionItem {
    // 1. Determine price used
    val priceUsed = priceCalculator.getPriceUsed(product)
    
    // 2. Get CRV and deposits
    val crvRate = product.crvRate ?: BigDecimal.ZERO
    
    // 3. Calculate final price (no discounts yet)
    val finalPrice = priceUsed + crvRate
    
    // 4. Calculate tax
    val taxPercentSum = product.taxes.sumOf { it.percent }
    val taxPerUnit = taxCalculator.calculateTaxPerUnit(finalPrice, taxPercentSum)
    
    // 5. Calculate subtotal
    val subTotal = (finalPrice * quantity).toMoney()
    val taxTotal = (taxPerUnit * quantity).toMoney()
    
    return TransactionItem(
        transactionGuid = UUID.randomUUID().toString(),
        branchProductId = product.branchProductId,
        branchProductName = product.name,
        itemNumber = product.barcode,
        retailPrice = product.retailPrice,
        salePrice = product.salePrice,
        priceUsed = priceUsed,
        floorPrice = product.floorPrice,
        cost = product.cost,
        crvRatePerUnit = crvRate,
        quantityUsed = quantity,
        finalPrice = finalPrice,
        taxPercentSum = taxPercentSum,
        taxPerUnit = taxPerUnit,
        taxTotal = taxTotal,
        subTotal = subTotal,
        subjectToTaxTotal = subTotal,
        isSNAPEligible = product.isSNAPEligible,
        isWICEligible = product.isWICApproved,
        ageRestriction = product.ageRestriction,
        isWeighted = product.isWeighted
    )
}
```

---

[← Back to Index](./INDEX.md) | [Next: Price Determination →](./PRICE_DETERMINATION.md)

