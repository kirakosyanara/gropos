# Discounts and Coupons

[← Back to Index](./INDEX.md) | [Previous: Promotions](./PROMOTIONS.md) | [Next: Customer Pricing →](./CUSTOMER_PRICING.md)

---

## Overview

This module covers all discount types that are not promotional in nature, including coupons, manual discounts, and invoice-level discounts.

---

## Discount Type Hierarchy

```
┌─────────────────────────────────────────────────────────────────────────┐
│                       DISCOUNT CATEGORIES                               │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  COUPONS                                                                │
│  ═══════                                                                │
│  • Manufacturer Coupons    (Store gets reimbursed)                      │
│  • Store Coupons           (Store-funded)                               │
│  • Digital Coupons         (App/loyalty loaded)                         │
│  • Competitor Coupons      (Price match via coupon)                     │
│                                                                         │
│  MANUAL LINE DISCOUNTS                                                  │
│  ═════════════════════                                                  │
│  • Percentage Off          (10% off item)                               │
│  • Amount Off              ($2 off item)                                │
│  • Price Override          (Set price to $X)                            │
│  • Damaged/Open Box        (Markdown for condition)                     │
│                                                                         │
│  INVOICE DISCOUNTS                                                      │
│  ═════════════════                                                      │
│  • Percentage Off Total    (5% off entire order)                        │
│  • Amount Off Total        ($10 off order)                              │
│                                                                         │
│  AUTOMATIC DISCOUNTS (see Customer Pricing)                             │
│  ═══════════════════                                                    │
│  • Customer Group          (Senior 10%)                                 │
│  • Loyalty Rewards         (Points redemption)                          │
│  • Employee Discount       (15% employee)                               │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Coupon System

### Coupon Validation Flow

```kotlin
fun validateCoupon(coupon: CouponDefinition, transaction: Transaction): ValidationResult {
    val errors = mutableListOf<String>()
    
    // 1. Check coupon is active
    if (!coupon.isActive) {
        errors.add("Coupon is not active")
        return ValidationResult(valid = false, errors = errors)
    }
    
    // 2. Check expiration
    if (coupon.expirationDate != null && now() > coupon.expirationDate) {
        errors.add("Coupon has expired")
        return ValidationResult(valid = false, errors = errors)
    }
    
    // 3. Check start date
    if (coupon.startDate != null && now() < coupon.startDate) {
        errors.add("Coupon is not yet valid")
        return ValidationResult(valid = false, errors = errors)
    }
    
    // 4. Check redemption limits
    if (coupon.maxTotalRedemptions != null) {
        if (coupon.currentRedemptions >= coupon.maxTotalRedemptions) {
            errors.add("Coupon redemption limit reached")
            return ValidationResult(valid = false, errors = errors)
        }
    }
    
    // 5. Check per-customer limit
    if (coupon.maxPerCustomer != null && transaction.customerId != null) {
        val customerRedemptions = getCustomerRedemptions(
            transaction.customerId,
            coupon.id,
            LocalDate.now()
        )
        if (customerRedemptions >= coupon.maxPerCustomer) {
            errors.add("Customer limit reached for this coupon")
            return ValidationResult(valid = false, errors = errors)
        }
    }
    
    // 6. Check per-transaction limit
    val currentInTransaction = countCouponInTransaction(transaction, coupon.id)
    if (currentInTransaction >= coupon.maxPerTransaction) {
        errors.add("Maximum uses per transaction reached")
        return ValidationResult(valid = false, errors = errors)
    }
    
    // 7. Check minimum purchase
    if (coupon.minPurchase != null) {
        val eligibleTotal = calculateEligibleTotal(transaction, coupon)
        if (eligibleTotal < coupon.minPurchase) {
            errors.add("Minimum purchase of $${coupon.minPurchase} required")
            return ValidationResult(valid = false, errors = errors)
        }
    }
    
    // 8. Check qualifying items exist
    val qualifyingItems = findQualifyingItems(transaction, coupon)
    if (qualifyingItems.isEmpty()) {
        errors.add("No qualifying items in transaction")
        return ValidationResult(valid = false, errors = errors)
    }
    
    // 9. Check required items
    if (coupon.requiredProducts.isNotEmpty()) {
        val hasRequired = transaction.items.any { item ->
            coupon.requiredProducts.contains(item.branchProductId)
        }
        if (!hasRequired) {
            errors.add("Required product not in transaction")
            return ValidationResult(valid = false, errors = errors)
        }
    }
    
    // 10. Check stacking rules
    if (!checkStackingRules(transaction, coupon)) {
        errors.add("Cannot combine with existing discounts")
        return ValidationResult(valid = false, errors = errors)
    }
    
    return ValidationResult(
        valid = true,
        qualifyingItems = qualifyingItems,
        discountAmount = calculateCouponDiscount(coupon, qualifyingItems)
    )
}
```

### Coupon Application

```kotlin
fun applyCoupon(coupon: CouponDefinition, transaction: Transaction) {
    val validation = validateCoupon(coupon, transaction)
    if (!validation.valid) {
        throw CouponValidationException(validation.errors)
    }
    
    // Find items to apply coupon to
    val qualifyingItems = validation.qualifyingItems
    
    // Sort items (typically apply to cheapest first for customer benefit)
    val sortedItems = qualifyingItems.sortedBy { item ->
        item.priceUsed - item.promotionDiscountPerUnit
    }
    
    // Apply coupon based on type
    var remainingValue = coupon.value
    var appliedCount = 0
    
    for (item in sortedItems) {
        if (appliedCount >= coupon.minQuantity) {
            if (appliedCount >= getMaxApplications(coupon, item)) {
                break
            }
        }
        
        val discount = calculateItemCouponDiscount(coupon, item)
        
        // Apply discount
        val application = ItemCouponApplication(
            couponId = coupon.id,
            couponCode = coupon.couponCode,
            discountAmount = discount,
            couponType = coupon.type
        )
        
        item.appliedCoupons.add(application)
        item.couponDiscountPerUnit += discount / item.quantityUsed
        item.couponDiscountTotal += discount
        
        appliedCount++
        
        // Track coupon usage
        if (coupon.valueType == CouponValueType.AMOUNT_OFF) {
            remainingValue -= discount
            if (remainingValue <= BigDecimal.ZERO) break
        }
    }
    
    // Add coupon to transaction
    val transactionCoupon = TransactionCouponViewModel(
        couponId = coupon.id,
        couponCode = coupon.couponCode,
        discountAmount = coupon.value - remainingValue,
        couponType = coupon.type
    )
    transaction.coupons.add(transactionCoupon)
    
    // Increment redemption counter
    incrementRedemptionCount(coupon)
}

fun calculateItemCouponDiscount(coupon: CouponDefinition, item: TransactionItemViewModel): BigDecimal {
    val effectivePrice = item.priceUsed -
        item.promotionDiscountPerUnit -
        item.customerDiscountPerUnit
    
    return when (coupon.valueType) {
        CouponValueType.PERCENT_OFF -> {
            var discount = effectivePrice * (coupon.value / BigDecimal(100))
            if (coupon.maxValue != null) {
                discount = minOf(discount, coupon.maxValue)
            }
            discount
        }
        CouponValueType.AMOUNT_OFF -> minOf(coupon.value, effectivePrice)
        CouponValueType.FREE_ITEM -> effectivePrice
        CouponValueType.FIXED_PRICE -> maxOf(BigDecimal.ZERO, effectivePrice - coupon.value)
    }
}
```

### Coupon Stacking Rules

```kotlin
fun checkStackingRules(transaction: Transaction, newCoupon: CouponDefinition): Boolean {
    val existingCoupons = transaction.coupons
    
    for (existing in existingCoupons) {
        val existingDef = getCouponDefinition(existing.couponId)
        
        // Rule 1: No duplicate coupons
        if (existing.couponId == newCoupon.id) {
            return false
        }
        
        // Rule 2: Check manufacturer stacking
        if (newCoupon.type == CouponType.MANUFACTURER && existingDef.type == CouponType.MANUFACTURER) {
            if (!newCoupon.stackWithManufacturer) {
                // Check if same product
                if (haveSameQualifyingProduct(newCoupon, existingDef)) {
                    return false
                }
            }
        }
        
        // Rule 3: Check store coupon stacking
        if (newCoupon.type == CouponType.STORE && existingDef.type == CouponType.STORE) {
            if (!newCoupon.stackWithStore) {
                if (haveSameQualifyingProduct(newCoupon, existingDef)) {
                    return false
                }
            }
        }
    }
    
    // Rule 4: Check if item already has conflicting discount
    for (item in findQualifyingItems(transaction, newCoupon)) {
        if (item.hasExclusivePromotion && !newCoupon.stackWithPromotion) {
            return false
        }
    }
    
    return true
}
```

---

## Manual Line Discounts

### Line Discount Application

```kotlin
fun applyLineDiscount(
    item: TransactionItemViewModel,
    discountType: TransactionDiscountType,
    discountValue: BigDecimal,
    employeeId: Int
) {
    // Calculate discount amount per unit
    val discountAmount = when (discountType) {
        TransactionDiscountType.ItemPercentage ->
            item.priceUsed * (discountValue / BigDecimal(100))
        TransactionDiscountType.ItemAmountPerUnit ->
            discountValue
        TransactionDiscountType.ItemAmountTotal ->
            item.priceUsed - discountValue  // discountValue is target price
        else -> BigDecimal.ZERO
    }
    
    // Check floor price
    val priceAfterDiscount = item.priceUsed - discountAmount
    
    if (priceAfterDiscount < item.floorPrice) {
        if (!hasFloorPriceOverride(item, employeeId)) {
            // Try to get manager approval
            val approved = requestFloorPriceOverride(
                item,
                priceAfterDiscount,
                employeeId
            )
            if (!approved) {
                // Cap discount at floor price
                discountAmount = item.priceUsed - item.floorPrice
            }
        }
    }
    
    // Apply line discount
    item.discountTypeId = discountType
    item.discountTypeAmount = discountValue
    item.discountAmountPerUnit = discountAmount
    
    // IMPORTANT: Line discount removes invoice discount from this item
    item.transactionDiscountTypeId = null
    item.transactionDiscountTypeAmount = BigDecimal.ZERO
    item.transactionDiscountAmountPerUnit = BigDecimal.ZERO
    
    // Recalculate item
    recalculateItem(item)
}
```

### Discount Type Calculations

```kotlin
fun calculateDiscountAmount(
    priceUsed: BigDecimal,
    discountType: TransactionDiscountType,
    discountValue: BigDecimal
): BigDecimal {
    return when (discountType) {
        TransactionDiscountType.ItemPercentage ->
            // discountValue is percentage (e.g., 10 for 10%)
            priceUsed * (discountValue / BigDecimal(100))
        
        TransactionDiscountType.ItemAmountPerUnit ->
            // discountValue is dollar amount off
            minOf(discountValue, priceUsed)
        
        TransactionDiscountType.ItemAmountTotal -> {
            // discountValue is target price
            if (discountValue >= priceUsed) BigDecimal.ZERO
            else priceUsed - discountValue
        }
        
        TransactionDiscountType.TransactionPercentTotal ->
            // Applied at invoice level, not per item
            priceUsed * (discountValue / BigDecimal(100))
        
        TransactionDiscountType.TransactionAmountTotal ->
            // Fixed amount distributed across items
            // Handled differently - see invoice discount
            BigDecimal.ZERO
    }
}
```

---

## Invoice-Level Discounts

### Invoice Discount Application

Invoice discounts apply to all items that don't already have a line discount:

```kotlin
fun applyInvoiceDiscount(transaction: Transaction, discountPercent: BigDecimal) {
    // Store at transaction level
    transaction.invoiceDiscountPercent = discountPercent
    OrderStore.invoiceDiscount = discountPercent
    
    // Apply to each eligible item
    for (item in transaction.items) {
        if (item.isRemoved) continue
        
        // Skip items with line discounts
        if (item.discountTypeId != null) continue
        
        // Calculate invoice discount for this item
        if (discountPercent > BigDecimal.ZERO) {
            item.transactionDiscountTypeId = TransactionDiscountType.TransactionPercentTotal
            item.transactionDiscountTypeAmount = discountPercent
            
            val discountAmount = item.priceUsed * (discountPercent / BigDecimal(100))
            item.transactionDiscountAmountPerUnit = discountAmount
        } else {
            // Remove invoice discount
            item.transactionDiscountTypeId = null
            item.transactionDiscountTypeAmount = BigDecimal.ZERO
            item.transactionDiscountAmountPerUnit = BigDecimal.ZERO
        }
        
        // Recalculate item
        recalculateItem(item)
    }
}
```

### Invoice Amount Discount

For fixed dollar amount off transaction:

```kotlin
fun applyInvoiceAmountDiscount(transaction: Transaction, discountAmount: BigDecimal): Boolean {
    // Get eligible items (those without line discounts)
    val eligibleItems = transaction.items.filter { item ->
        !item.isRemoved && item.discountTypeId == null
    }
    
    if (eligibleItems.isEmpty()) return false
    
    // Calculate total eligible amount
    val eligibleTotal = eligibleItems.sumOf { item ->
        item.priceUsed * item.quantityUsed
    }
    
    // Distribute discount proportionally
    for (item in eligibleItems) {
        val itemPortion = (item.priceUsed * item.quantityUsed) / eligibleTotal
        val itemDiscount = discountAmount * itemPortion
        
        item.transactionDiscountTypeId = TransactionDiscountType.TransactionAmountTotal
        item.transactionDiscountTypeAmount = discountAmount  // Total amount
        item.transactionDiscountAmountPerUnit = itemDiscount / item.quantityUsed
        
        recalculateItem(item)
    }
    
    transaction.invoiceDiscountAmount = discountAmount
    return true
}
```

---

## Discount Interaction Rules

> **Cross-Reference:** Discount stacking rules are also summarized in:
> - [INDEX.md - Key Business Rules](./INDEX.md#key-business-rules-summary)
> - [PROMOTIONS.md - Promotion Stacking](./PROMOTIONS.md#promotion-priority-and-stacking)

### Discount Priority Matrix

When multiple discounts could apply, use this priority:

| Priority | Discount Type | Stacks With |
|----------|---------------|-------------|
| 1 | Sale Price | Coupons (usually) |
| 2 | Promotion (Mix & Match, BOGO) | Nothing (exclusive) |
| 3 | Manufacturer Coupon | Store coupon, loyalty |
| 4 | Store Coupon | Mfr coupon, sale |
| 5 | Customer Group Discount | Employee discount (No) |
| 6 | Employee Discount | Nothing |
| 7 | Manual Line Discount | Nothing |
| 8 | Invoice Discount | Excludes items with line discount |

### Mutual Exclusivity Rules

```kotlin
fun checkDiscountExclusivity(
    item: TransactionItemViewModel,
    newDiscount: DiscountSource
): ConflictResult {
    val exclusiveWith = getExclusiveDiscounts(newDiscount)
    
    // Check promotions
    if (item.promotionId != null && DiscountSource.PROMOTION in exclusiveWith) {
        return ConflictResult(
            canApply = false,
            reason = "Item already has promotion"
        )
    }
    
    // Check coupons
    if (item.appliedCoupons.isNotEmpty() && DiscountSource.COUPON in exclusiveWith) {
        return ConflictResult(
            canApply = false,
            reason = "Item already has coupon"
        )
    }
    
    // Check customer discount
    if (item.customerDiscountId != null && DiscountSource.CUSTOMER in exclusiveWith) {
        return ConflictResult(
            canApply = false,
            reason = "Item already has customer discount"
        )
    }
    
    // Check manual discount
    if (item.discountTypeId != null && DiscountSource.MANUAL in exclusiveWith) {
        return ConflictResult(
            canApply = false,
            reason = "Item already has manual discount"
        )
    }
    
    return ConflictResult(canApply = true)
}

fun getExclusiveDiscounts(source: DiscountSource): Set<DiscountSource> {
    return when (source) {
        DiscountSource.PROMOTION -> setOf(DiscountSource.PROMOTION, DiscountSource.COUPON_STORE)
        DiscountSource.COUPON_MANUFACTURER -> setOf(DiscountSource.COUPON_MANUFACTURER)
        DiscountSource.COUPON_STORE -> setOf(DiscountSource.COUPON_STORE, DiscountSource.PROMOTION)
        DiscountSource.CUSTOMER_EMPLOYEE -> setOf(
            DiscountSource.CUSTOMER_GROUP,
            DiscountSource.CUSTOMER_EMPLOYEE,
            DiscountSource.CUSTOMER_LOYALTY
        )
        DiscountSource.MANUAL_LINE -> setOf(DiscountSource.MANUAL_LINE, DiscountSource.MANUAL_INVOICE)
        else -> emptySet()
    }
}
```

---

## Total Discount Calculation

### Per-Item Total Discount

```kotlin
fun calculateTotalItemDiscount(item: TransactionItemViewModel): BigDecimal {
    var totalDiscountPerUnit = BigDecimal.ZERO
    
    // Add promotion discount
    totalDiscountPerUnit += item.promotionDiscountPerUnit ?: BigDecimal.ZERO
    
    // Add coupon discount
    totalDiscountPerUnit += item.couponDiscountPerUnit ?: BigDecimal.ZERO
    
    // Add customer discount
    totalDiscountPerUnit += item.customerDiscountPerUnit ?: BigDecimal.ZERO
    
    // Add manual line discount (OR invoice discount, not both)
    if (item.discountAmountPerUnit > BigDecimal.ZERO) {
        totalDiscountPerUnit += item.discountAmountPerUnit
    } else {
        totalDiscountPerUnit += item.transactionDiscountAmountPerUnit ?: BigDecimal.ZERO
    }
    
    return totalDiscountPerUnit
}
```

### Transaction Savings Total

```kotlin
fun calculateTransactionSavings(items: List<TransactionItemViewModel>): SavingsBreakdown {
    val savingsBreakdown = SavingsBreakdown()
    
    for (item in items) {
        if (item.isRemoved) continue
        
        // Sale price savings (not in savingsPerUnit, tracked separately)
        if (item.salePrice > BigDecimal.ZERO && item.salePrice < item.retailPrice) {
            val saleSavings = (item.retailPrice - item.salePrice) * item.quantityUsed
            savingsBreakdown.salePriceSavings += saleSavings
        }
        
        // Promotion savings
        savingsBreakdown.promotionSavings += item.promotionDiscountTotal ?: BigDecimal.ZERO
        
        // Coupon savings
        savingsBreakdown.couponSavings += item.couponDiscountTotal ?: BigDecimal.ZERO
        
        // Customer discount savings
        savingsBreakdown.customerSavings += item.customerDiscountTotal ?: BigDecimal.ZERO
        
        // Manual/invoice discount savings
        if (item.discountAmountPerUnit > BigDecimal.ZERO) {
            savingsBreakdown.manualSavings +=
                item.discountAmountPerUnit * item.quantityUsed
        } else {
            savingsBreakdown.invoiceSavings +=
                item.transactionDiscountAmountPerUnit * item.quantityUsed
        }
    }
    
    savingsBreakdown.totalSavings =
        savingsBreakdown.salePriceSavings +
        savingsBreakdown.promotionSavings +
        savingsBreakdown.couponSavings +
        savingsBreakdown.customerSavings +
        savingsBreakdown.manualSavings +
        savingsBreakdown.invoiceSavings
    
    return savingsBreakdown
}
```

---

## Coupon Reporting

### Coupon Tracking for Reimbursement

Manufacturer coupons require tracking for reimbursement:

```kotlin
data class CouponRedemptionRecord(
    val id: Int,
    val couponId: Int,
    val couponCode: String,
    val transactionId: Int,
    val redemptionDate: OffsetDateTime,
    
    // Item Applied To
    val productId: Int,
    val upc: String,
    val quantity: BigDecimal,
    
    // Values
    val faceValue: BigDecimal,             // Coupon face value
    val appliedValue: BigDecimal,          // Actual discount given
    val reimbursementAmount: BigDecimal,   // Amount to claim from mfr
    
    // Status
    val status: CouponReimbursementStatus, // Pending, Submitted, Paid
    val submittedDate: OffsetDateTime?,
    val paidDate: OffsetDateTime?
)
```

---

## Rain Checks

### Rain Check Issuance

```kotlin
data class RainCheck(
    val id: Int,
    val rainCheckNumber: String,           // Unique identifier
    
    // Original Sale Info
    val productId: Int,
    val productName: String,
    val salePrice: BigDecimal,
    val retailPrice: BigDecimal,
    val originalPromotionName: String?,
    
    // Validity
    val issueDate: OffsetDateTime,
    val expirationDate: OffsetDateTime,    // Usually 30-60 days
    val maxQuantity: Int,                  // Quantity limit
    
    // Redemption
    var redeemed: Boolean = false,
    var redemptionTransactionId: Int? = null,
    var redemptionDate: OffsetDateTime? = null,
    
    // Customer
    val customerId: Int?,
    val customerName: String?
)

fun issueRainCheck(product: ProductViewModel, sale: SalePromotion, quantity: Int): RainCheck {
    val rainCheck = RainCheck(
        id = generateId(),
        rainCheckNumber = generateRainCheckNumber(),
        productId = product.id,
        productName = product.name,
        salePrice = sale.discountedPrice,
        retailPrice = product.retailPrice,
        originalPromotionName = sale.name,
        issueDate = now(),
        expirationDate = now().plusDays(RAIN_CHECK_VALID_DAYS.toLong()),
        maxQuantity = quantity,
        customerId = currentCustomerId()
    )
    
    save(rainCheck)
    printRainCheck(rainCheck)
    
    return rainCheck
}
```

### Rain Check Redemption

```kotlin
fun redeemRainCheck(rainCheckNumber: String, item: TransactionItemViewModel) {
    val rainCheck = findRainCheck(rainCheckNumber)
        ?: throw RainCheckNotFoundException()
    
    // Validate
    if (rainCheck.redeemed) {
        throw RainCheckAlreadyRedeemedException()
    }
    
    if (now() > rainCheck.expirationDate) {
        throw RainCheckExpiredException()
    }
    
    if (item.branchProductId != rainCheck.productId) {
        throw ProductMismatchException()
    }
    
    val applyToQuantity = if (item.quantityUsed > BigDecimal(rainCheck.maxQuantity)) {
        // Partial redemption - apply to max quantity only
        BigDecimal(rainCheck.maxQuantity)
    } else {
        item.quantityUsed
    }
    
    // Apply rain check price
    val discount = item.priceUsed - rainCheck.salePrice
    
    if (applyToQuantity < item.quantityUsed) {
        // Prorate discount
        item.discountAmountPerUnit =
            (discount * applyToQuantity) / item.quantityUsed
    } else {
        item.discountAmountPerUnit = discount
    }
    
    item.discountTypeId = TransactionDiscountType.RainCheck
    item.discountTypeAmount = rainCheck.salePrice
    
    // Mark as redeemed
    rainCheck.redeemed = true
    rainCheck.redemptionTransactionId = currentTransactionId()
    rainCheck.redemptionDate = now()
    
    save(rainCheck)
    recalculateItem(item)
}
```

---

[← Back to Index](./INDEX.md) | [Previous: Promotions](./PROMOTIONS.md) | [Next: Customer Pricing →](./CUSTOMER_PRICING.md)

