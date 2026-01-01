# Price Determination

[← Back to Index](./INDEX.md) | [Previous: Core Concepts](./CORE_CONCEPTS.md) | [Next: Promotions →](./PROMOTIONS.md)

---

## Overview

Price determination is the first phase of transaction calculation. It establishes the base price for each item before any discounts are applied.

---

## Price Hierarchy

Prices are selected in strict priority order:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                       PRICE SELECTION HIERARCHY                         │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  PRIORITY 1: PROMPTED PRICE                                             │
│  ═══════════════════════════                                            │
│  • User manually entered a price                                        │
│  • Used for: PLU items, deli, bakery, custom items                      │
│  • Overrides ALL other prices                                           │
│                                                                         │
│          ↓ (if not prompted)                                            │
│                                                                         │
│  PRIORITY 2: CUSTOMER-SPECIFIC PRICE                                    │
│  ════════════════════════════════════                                   │
│  • Contract pricing for specific customers                              │
│  • VIP/wholesale pricing tiers                                          │
│  • Replaces base price entirely                                         │
│                                                                         │
│          ↓ (if no customer price)                                       │
│                                                                         │
│  PRIORITY 3: SALE PRICE                                                 │
│  ══════════════════════                                                 │
│  • Time-limited promotional pricing                                     │
│  • Validated by date, time, day-of-week                                 │
│  • Multiple sales: use best (lowest) valid price                        │
│                                                                         │
│          ↓ (if no valid sale)                                           │
│                                                                         │
│  PRIORITY 4: BULK PRICE                                                 │
│  ══════════════════════                                                 │
│  • Volume-based unit price                                              │
│  • Triggers when quantity meets threshold                               │
│  • May have multiple tiers                                              │
│                                                                         │
│          ↓ (if qty below bulk threshold)                                │
│                                                                         │
│  PRIORITY 5: RETAIL PRICE                                               │
│  ════════════════════════                                               │
│  • Standard everyday price                                              │
│  • Default if no other price applies                                    │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Price Used Calculation

```kotlin
fun determinePriceUsed(item: TransactionItemViewModel, customer: CustomerProfile?): BigDecimal {
    
    // Priority 1: Prompted Price
    if (item.isPromptedPrice && item.promptedPrice > BigDecimal.ZERO) {
        return item.promptedPrice
    }
    
    // Priority 2: Customer-Specific Price
    if (customer != null) {
        val customerPrice = getCustomerSpecificPrice(item.branchProductId, customer)
        if (customerPrice != null) {
            return customerPrice.price
        }
    }
    
    // Priority 3: Sale Price (validated)
    val validSales = getValidSalePrices(item.branchProductId)
    if (validSales.isNotEmpty()) {
        val bestSale = validSales.minByOrNull { it.discountedPrice }
        return bestSale!!.discountedPrice
    }
    
    // Priority 4: Bulk Price
    val bulkThreshold = getBulkThreshold(item.branchProductId)
    if (item.quantityUsed >= bulkThreshold) {
        val bulkPrice = getBulkPrice(item.branchProductId, item.quantityUsed)
        if (bulkPrice != null) return bulkPrice
    }
    
    // Priority 5: Retail Price
    return item.retailPrice
}
```

---

## Sale Price System

### Sale Price Definition

```kotlin
data class ProductSalePriceViewModel(
    val id: Int,
    val productId: Int,
    val saleName: String,                  // "Weekly Special"
    
    // Pricing
    val retailPrice: BigDecimal,           // Original retail for comparison
    val discountedPrice: BigDecimal,       // The sale price
    val discountAmount: BigDecimal,        // Savings per unit
    val discountPercent: BigDecimal,       // Equivalent percentage
    
    // Validity Period
    val startDate: OffsetDateTime?,
    val endDate: OffsetDateTime?,
    val startTime: String?,                // "06:00:00"
    val endTime: String?,                  // "22:00:00"
    val salePriceDays: SalePriceDays,      // Bitmask for valid days
    
    // Status
    val active: Boolean,
    
    // Limits
    val maxQuantityPerCustomer: Int?,
    val maxQuantityPerTransaction: Int?
)
```

### Sale Price Validation

```kotlin
fun validateSalePrice(sale: ProductSalePriceViewModel): Boolean {
    val now = OffsetDateTime.now()
    val today = now.toLocalDate()
    val currentTime = now.toLocalTime()
    
    // Check 1: Active flag
    if (!sale.active) return false
    
    // Check 2: Date range
    if (sale.startDate != null && now < sale.startDate) return false
    if (sale.endDate != null && now > sale.endDate) return false
    
    // Check 3: Day of week
    if (!isDayValid(sale.salePriceDays, today.dayOfWeek)) return false
    
    // Check 4: Time of day
    val startTime = parseTime(sale.startTime)
    val endTime = parseTime(sale.endTime)
    
    if (startTime != null && endTime != null) {
        if (startTime <= endTime) {
            // Normal time range (e.g., 9AM to 5PM)
            if (currentTime < startTime || currentTime > endTime) return false
        } else {
            // Overnight range (e.g., 10PM to 6AM)
            if (currentTime < startTime && currentTime > endTime) return false
        }
    }
    
    return true
}
```

### Day-of-Week Bitmask

```kotlin
object DayOfWeek {
    const val SUNDAY    = 1    // 0000001
    const val MONDAY    = 2    // 0000010
    const val TUESDAY   = 4    // 0000100
    const val WEDNESDAY = 8    // 0001000
    const val THURSDAY  = 16   // 0010000
    const val FRIDAY    = 32   // 0100000
    const val SATURDAY  = 64   // 1000000
    
    const val WEEKDAYS  = 62   // Mon-Fri
    const val WEEKENDS  = 65   // Sat-Sun
    const val ALL_DAYS  = 127  // Every day
}

fun isDayValid(bitmask: Int, day: java.time.DayOfWeek): Boolean {
    val dayValue = when (day) {
        java.time.DayOfWeek.SUNDAY -> DayOfWeek.SUNDAY
        java.time.DayOfWeek.MONDAY -> DayOfWeek.MONDAY
        java.time.DayOfWeek.TUESDAY -> DayOfWeek.TUESDAY
        java.time.DayOfWeek.WEDNESDAY -> DayOfWeek.WEDNESDAY
        java.time.DayOfWeek.THURSDAY -> DayOfWeek.THURSDAY
        java.time.DayOfWeek.FRIDAY -> DayOfWeek.FRIDAY
        java.time.DayOfWeek.SATURDAY -> DayOfWeek.SATURDAY
    }
    return (bitmask and dayValue) != 0
}
```

---

## Bulk/Volume Pricing

### Bulk Pricing Definition

```kotlin
data class ProductBulkPricing(
    val id: Int,
    val productId: Int,
    
    // Tier Definition
    val tiers: List<BulkPricingTier>,
    
    // Applicability
    val active: Boolean,
    val startDate: OffsetDateTime?,
    val endDate: OffsetDateTime?
)

data class BulkPricingTier(
    val minQuantity: Int,                  // Min qty for this tier
    val maxQuantity: Int?,                 // Max qty (null = unlimited)
    val unitPrice: BigDecimal?,            // Price per unit at this tier
    val percentOff: BigDecimal?,           // OR: percentage off retail
    val amountOff: BigDecimal?             // OR: amount off per unit
)
```

### Bulk Pricing Examples

**Example 1: Simple Case Discount**
```
Product: 24-pack Water
Retail: $5.99 each
Bulk: Buy 4+, $4.99 each

Tiers:
  [1-3]:  $5.99/unit
  [4+]:   $4.99/unit
```

**Example 2: Tiered Volume Discount**
```
Product: Office Supplies
Retail: $10.00 each

Tiers:
  [1-9]:    $10.00/unit (0% off)
  [10-24]:  $9.00/unit (10% off)
  [25-49]:  $8.00/unit (20% off)
  [50+]:    $7.00/unit (30% off)
```

**Example 3: Weight-Based Bulk**
```
Product: Deli Meat
Retail: $8.99/lb

Tiers:
  [0-0.99 lb]:   $8.99/lb
  [1-2.99 lb]:   $7.99/lb
  [3+ lb]:       $6.99/lb
```

### Bulk Price Calculation

```kotlin
fun calculateBulkPrice(productId: Int, quantity: BigDecimal): BigDecimal? {
    val bulkPricing = getBulkPricing(productId)
    
    if (bulkPricing == null || !bulkPricing.active) {
        return null  // No bulk pricing, use regular price
    }
    
    // Find applicable tier
    val applicableTier = bulkPricing.tiers
        .filter { quantity >= BigDecimal(it.minQuantity) }
        .filter { it.maxQuantity == null || quantity <= BigDecimal(it.maxQuantity) }
        .maxByOrNull { it.minQuantity }  // Highest qualifying tier
    
    if (applicableTier == null) return null
    
    // Calculate unit price
    return when {
        applicableTier.unitPrice != null -> applicableTier.unitPrice
        applicableTier.percentOff != null -> {
            val retail = getRetailPrice(productId)
            retail * (BigDecimal.ONE - applicableTier.percentOff / BigDecimal(100))
        }
        applicableTier.amountOff != null -> {
            val retail = getRetailPrice(productId)
            retail - applicableTier.amountOff
        }
        else -> null
    }
}
```

---

## Customer-Specific Pricing

### Customer Price Override

Some customers have negotiated specific pricing:

```kotlin
data class CustomerPriceOverride(
    val id: Int,
    val customerId: Int?,                  // Or customerGroupId
    val productId: Int?,                   // Specific product
    val categoryId: Int?,                  // Or entire category
    
    // Price Type
    val type: PriceOverrideType,           // FixedPrice, PercentOff, AmountOff
    val value: BigDecimal,                 // The price or discount
    
    // Validity
    val startDate: OffsetDateTime?,
    val endDate: OffsetDateTime?,
    val active: Boolean,
    
    // Limits
    val maxQuantityPerTransaction: BigDecimal?,
    val maxSpendPerDay: BigDecimal?
)

enum class PriceOverrideType {
    FIXED_PRICE,     // Set specific price
    PERCENT_OFF,     // % off retail
    AMOUNT_OFF,      // $ off retail
    COST_PLUS        // Cost + X% markup
}
```

### Customer Price Lookup

```kotlin
fun getCustomerSpecificPrice(productId: Int, customer: CustomerProfile): CustomerPriceResult? {
    if (customer == null) return null
    
    // Check for product-specific override
    val productOverride = findCustomerPriceOverride(
        customerId = customer.id,
        productId = productId,
        active = true
    )
    
    if (productOverride != null && isValid(productOverride)) {
        return calculateOverridePrice(productOverride, productId)
    }
    
    // Check for category override
    val product = getProduct(productId)
    val categoryOverride = findCustomerPriceOverride(
        customerId = customer.id,
        categoryId = product.categoryId,
        active = true
    )
    
    if (categoryOverride != null && isValid(categoryOverride)) {
        return calculateOverridePrice(categoryOverride, productId)
    }
    
    // Check customer group overrides
    if (customer.customerGroupId != null) {
        return getGroupSpecificPrice(productId, customer.customerGroupId)
    }
    
    return null
}

fun calculateOverridePrice(override: CustomerPriceOverride, productId: Int): CustomerPriceResult {
    val retail = getRetailPrice(productId)
    
    val price = when (override.type) {
        PriceOverrideType.FIXED_PRICE -> override.value
        PriceOverrideType.PERCENT_OFF -> retail * (BigDecimal.ONE - override.value / BigDecimal(100))
        PriceOverrideType.AMOUNT_OFF -> retail - override.value
        PriceOverrideType.COST_PLUS -> {
            val cost = getProductCost(productId)
            cost * (BigDecimal.ONE + override.value / BigDecimal(100))
        }
    }
    
    return CustomerPriceResult(price = price, overrideType = override.type)
}
```

---

## Clearance and Markdown Pricing

### Markdown Definition

```kotlin
data class ProductMarkdown(
    val id: Int,
    val productId: Int,
    
    // Markdown Type
    val type: MarkdownType,                // Clearance, Seasonal, Damaged
    val reason: String?,                   // "Seasonal closeout", "Package damage"
    
    // Pricing
    val markdownPrice: BigDecimal?,        // The reduced price
    val markdownPercent: BigDecimal?,      // OR: % off retail
    
    // Validity
    val startDate: OffsetDateTime?,
    val endDate: OffsetDateTime?,
    val active: Boolean,
    
    // Stock Control
    val maxQuantity: Int?,                 // Only X units at this price
    val soldQuantity: Int,                 // Already sold at markdown
    
    // Floor Override
    val allowBelowFloor: Boolean           // Clearance can go below floor
)

enum class MarkdownType {
    CLEARANCE,           // End of line
    SEASONAL,            // Seasonal closeout
    DAMAGED,             // Damaged packaging
    SHORT_DATE,          // Near expiration
    OVERSTOCK,           // Excess inventory
    MANAGER_SPECIAL      // Manager discretion
}
```

### Markdown vs Sale Price

| Attribute | Sale Price | Markdown |
|-----------|------------|----------|
| Duration | Time-limited | Until sold out |
| Floor Price | Must respect floor | Can go below floor |
| Coupons | Usually stackable | Usually not stackable |
| Rain Check | Eligible | Not eligible |
| Inventory | Normal stock | Limited quantity |

---

## Price Matching

### Competitor Price Match

```kotlin
data class PriceMatchRequest(
    val productId: Int,
    val competitorPrice: BigDecimal,       // Price to match
    val competitorName: String,            // "Walmart", "Target"
    val proofType: String,                 // "Ad", "Receipt", "Website"
    val approvedByEmployeeId: Int?,        // Manager approval
    
    // Adjustment
    val matchType: PriceMatchType,         // Match, BeatBy
    val beatByPercent: BigDecimal?,        // If "we beat by 10%"
    var finalPrice: BigDecimal?            // Calculated final price
)

fun calculatePriceMatch(request: PriceMatchRequest, product: ProductViewModel): BigDecimal {
    // Validate competitor price is lower than our price
    val ourPrice = getCurrentPrice(product)
    if (request.competitorPrice >= ourPrice) {
        return ourPrice  // No match needed
    }
    
    // Check floor price
    if (request.competitorPrice < product.floorPrice) {
        if (request.approvedByEmployeeId == null) {
            throw RequiresManagerApprovalException()
        }
    }
    
    // Calculate match price
    return when (request.matchType) {
        PriceMatchType.Match -> request.competitorPrice
        PriceMatchType.BeatBy -> {
            val beaten = request.competitorPrice * (BigDecimal.ONE - request.beatByPercent!! / BigDecimal(100))
            maxOf(beaten, product.cost)  // Never below cost
        }
    }
}
```

---

## Deposits and Fees

Deposits and fees are added to the base price to form the "price with deposits":

### Fee Types

```kotlin
enum class FeeType {
    CRV,                 // California Redemption Value
    BOTTLE_DEPOSIT,      // Returnable bottle deposit
    BAG_FEE,             // Plastic/paper bag fee
    ENVIRONMENTAL_FEE,   // E-waste, battery recycling
    TIRE_FEE,            // Tire disposal fee
    DELIVERY_FEE         // Per-item delivery charge
}

data class ProductFee(
    val id: Int,
    val productId: Int,
    val feeType: FeeType,
    val amount: BigDecimal,
    val isTaxable: Boolean,                // Is this fee subject to tax
    val isRefundable: Boolean,             // On return, refund this fee
    val includeInEbt: Boolean              // Can EBT cover this fee
)
```

### Fee Application

```kotlin
fun calculateTotalDeposits(item: TransactionItemViewModel, product: ProductViewModel): BigDecimal {
    var totalDeposits = BigDecimal.ZERO
    
    // CRV
    if (product.crvId != null) {
        val crv = getCRVById(product.crvId)
        item.crvRatePerUnit = crv.price
        totalDeposits += crv.price
    }
    
    // Bottle Deposit
    if (product.hasBottleDeposit) {
        val deposit = getBottleDeposit(product.bottleDepositId)
        item.bottleDepositPerUnit = deposit.amount
        totalDeposits += deposit.amount
    }
    
    // Bag Fee (special - usually separate item)
    // Typically handled as separate line item, not per-product
    
    // Other Fees
    for (fee in getProductFees(product.id)) {
        totalDeposits += fee.amount
    }
    
    item.totalDepositsPerUnit = totalDeposits
    item.otherFeesPerUnit = totalDeposits - item.crvRatePerUnit - item.bottleDepositPerUnit
    
    return totalDeposits
}
```

---

## Floor Price Enforcement

### Floor Price Logic

The floor price is the minimum selling price, protecting margins:

```kotlin
fun enforceFloorPrice(item: TransactionItemViewModel): BigDecimal {
    // Calculate price after all discounts
    val calculatedPrice = item.priceUsed -
        item.promotionDiscountPerUnit -
        item.couponDiscountPerUnit -
        item.customerDiscountPerUnit -
        item.discountAmountPerUnit -
        item.transactionDiscountAmountPerUnit
    
    // Check floor
    if (calculatedPrice < item.floorPrice) {
        // Exception 1: Corporate sale price below floor
        if (item.salePrice > BigDecimal.ZERO && item.priceUsed == item.salePrice) {
            if (item.priceUsed < item.floorPrice) {
                // Corporate approved this sale below floor
                return calculatedPrice
            }
        }
        
        // Exception 2: Clearance/Markdown
        if (item.markdownId != null) {
            val markdown = getMarkdown(item.markdownId)
            if (markdown.allowBelowFloor) {
                return calculatedPrice
            }
        }
        
        // Exception 3: Manager override approved
        if (item.isFloorPriceOverridden) {
            return calculatedPrice
        }
        
        // Default: Enforce floor price
        item.finalPriceExcludingDeposits = item.floorPrice
        return item.floorPrice
    }
    
    item.finalPriceExcludingDeposits = calculatedPrice
    return calculatedPrice
}
```

### Floor Price Override Request

```kotlin
fun requestFloorPriceOverride(item: TransactionItemViewModel, targetPrice: BigDecimal): Boolean {
    if (targetPrice >= item.floorPrice) {
        // No override needed
        return true
    }
    
    // Check if target is at least above cost
    if (targetPrice < item.cost) {
        return false  // Cannot sell below cost
    }
    
    // Request manager approval
    val overrideRequest = FloorPriceOverrideRequest(
        itemId = item.id,
        currentFloor = item.floorPrice,
        requestedPrice = targetPrice,
        reason = getOverrideReason(),
        requestedBy = getCurrentCashier()
    )
    
    val approval = awaitManagerApproval(overrideRequest)
    
    if (approval.approved) {
        item.isFloorPriceOverridden = true
        item.floorPriceOverrideEmployeeId = approval.managerId
        return true
    }
    
    return false
}
```

---

## Final Price Calculation

After price selection and before tax:

```kotlin
fun calculateFinalPrice(item: TransactionItemViewModel): BigDecimal {
    // Start with effective price (after all discounts)
    var effectivePrice = item.priceUsed -
        item.promotionDiscountPerUnit -
        item.couponDiscountPerUnit -
        item.customerDiscountPerUnit -
        item.discountAmountPerUnit -
        item.transactionDiscountAmountPerUnit
    
    // Enforce floor price
    effectivePrice = enforceFloorPrice(item, effectivePrice)
    
    // Store price excluding deposits
    item.finalPriceExcludingDeposits = effectivePrice
    
    // Add deposits to get final price
    item.finalPrice = effectivePrice + item.totalDepositsPerUnit
    
    return item.finalPrice
}
```

---

## Examples

### Example 1: Regular Item

```
Product: Cereal
Retail Price: $4.99
No sale, no bulk pricing, no customer price

Price Used: $4.99 (retail)
Deposits: $0.00
Final Price: $4.99
```

### Example 2: Sale Item

```
Product: Soda 12-pack
Retail Price: $5.99
Sale Price: $3.99 (valid today)
CRV: $0.60

Price Used: $3.99 (sale)
Deposits: $0.60 (CRV)
Final Price: $4.59
```

### Example 3: Bulk Pricing

```
Product: Cases of Water
Retail Price: $4.99/case
Bulk: Buy 5+, $3.99/case
Quantity: 6 cases

Price Used: $3.99 (bulk tier triggered)
Deposits: $0.30/case (CRV)
Final Price: $4.29/case
Line Total: $25.74 (6 × $4.29)
```

### Example 4: Customer-Specific Pricing

```
Product: Office Paper
Retail Price: $9.99
Customer: Business Account #12345
Customer Price: Cost + 15% ($5.75 cost)

Customer Price: $5.75 × 1.15 = $6.61
Price Used: $6.61 (customer price)
Final Price: $6.61
```

---

[← Back to Index](./INDEX.md) | [Previous: Core Concepts](./CORE_CONCEPTS.md) | [Next: Promotions →](./PROMOTIONS.md)

