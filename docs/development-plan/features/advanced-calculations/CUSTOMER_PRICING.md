# Customer-Specific Pricing

[← Back to Index](./INDEX.md) | [Previous: Discounts](./DISCOUNTS.md) | [Next: Tax Calculations →](./TAX_CALCULATIONS.md)

---

## Overview

Customer-specific pricing in GroPOS covers all discounts and pricing based on customer identity, including loyalty programs, customer groups, employee discounts, and personalized offers.

---

## Customer Groups

### Group Types

```
┌─────────────────────────────────────────────────────────────────────────┐
│                       CUSTOMER GROUP TYPES                              │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  DEMOGRAPHIC GROUPS                                                     │
│  ═══════════════════                                                    │
│  • Senior (65+)           - 5-10% discount                              │
│  • Military/Veteran       - 10% discount                                │
│  • Student                - 5-10% discount                              │
│  • First Responder        - 10% discount                                │
│                                                                         │
│  BUSINESS GROUPS                                                        │
│  ═══════════════                                                        │
│  • Wholesale              - Cost + markup pricing                       │
│  • Restaurant/Food Service - Volume pricing                             │
│  • Nonprofit              - Special pricing                             │
│  • Corporate Account      - Negotiated pricing                          │
│                                                                         │
│  LOYALTY TIERS                                                          │
│  ═════════════                                                          │
│  • Bronze                 - Basic member benefits                       │
│  • Silver                 - 5% + member benefits                        │
│  • Gold                   - 10% + member benefits                       │
│  • Platinum               - 15% + member benefits                       │
│                                                                         │
│  SPECIAL GROUPS                                                         │
│  ══════════════                                                         │
│  • Employee               - Employee discount (exclusive)               │
│  • Family of Employee     - Reduced employee rate                       │
│  • Vendor/Supplier        - Cost pricing                                │
│  • VIP                    - Custom negotiated                           │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### Customer Group Definition

```kotlin
data class CustomerGroup(
    val id: Int,
    val name: String,                         // "Senior Citizens"
    val code: String,                         // "SENIOR"
    val type: CustomerGroupType,              // Demographic, Business, Loyalty, Special
    
    // General Discount
    val generalDiscountPercent: BigDecimal,   // Applied to all eligible items
    val generalDiscountMaxPerItem: BigDecimal?, // Cap per item
    val generalDiscountMaxPerTransaction: BigDecimal?, // Cap per transaction
    
    // Category-Specific Discounts
    val hasCategoryDiscounts: Boolean,
    val categoryDiscounts: List<CategoryDiscount>,
    
    // Item-Specific Discounts
    val hasItemDiscounts: Boolean,
    val itemDiscounts: List<ItemDiscount>,
    
    // Exclusions
    val excludedCategoryIds: List<Int>,       // No discount on these
    val excludedProductIds: List<Int>,        // No discount on these
    val excludedBrandIds: List<Int>,          // No discount on these brands
    val excludeSaleItems: Boolean,            // No discount on items already on sale
    val excludePromotions: Boolean,           // No discount on promo items
    val excludeCouponedItems: Boolean,        // No discount if coupon applied
    
    // Stacking Rules
    val stackWithSales: Boolean,              // Can combine with sale prices
    val stackWithCoupons: Boolean,            // Can combine with coupons
    val stackWithPromotions: Boolean,         // Can combine with promotions
    val stackWithOtherGroups: Boolean,        // Multiple group discounts
    
    // Verification
    val verification: VerificationType,       // None, ID, Card, Certificate
    val requiresManagerApproval: Boolean,
    
    // Limits
    val dailySpendLimit: BigDecimal?,         // Max spend per day at discount
    val monthlySpendLimit: BigDecimal?,       // Max spend per month
    val transactionsPerDay: Int?              // Max discounted transactions/day
)

data class CategoryDiscount(
    val categoryId: Int,
    val categoryName: String,
    val discountPercent: BigDecimal,
    val discountMaxAmount: BigDecimal?
)

data class ItemDiscount(
    val productId: Int,
    val productName: String,
    val type: DiscountType,                   // Percent, Amount, FixedPrice
    val discountValue: BigDecimal
)

enum class VerificationType {
    NONE,                    // No verification needed
    LOYALTY_CARD,            // Scan loyalty card
    GOVERNMENT_ID,           // Show ID
    MILITARY_ID,             // Show military ID
    STUDENT_ID,              // Show student ID
    TAX_CERTIFICATE,         // For tax-exempt
    EMPLOYEE_BADGE           // Employee badge
}
```

### Customer Group Discount Application

```kotlin
fun applyCustomerGroupDiscounts(
    transaction: Transaction,
    customer: CustomerProfile?
) {
    if (customer == null || customer.customerGroupId == null) {
        return
    }
    
    val group = getCustomerGroup(customer.customerGroupId)
    
    // Check daily/monthly limits
    if (!checkSpendLimits(customer, group)) {
        logWarning("Customer spend limit reached")
        return
    }
    
    var totalCustomerDiscount = BigDecimal.ZERO
    
    for (item in transaction.items) {
        if (item.isRemoved) continue
        
        // Check exclusions
        if (isExcludedFromGroupDiscount(item, group)) continue
        
        // Check stacking
        if (!canApplyGroupDiscount(item, group)) continue
        
        // Calculate discount
        var discount = calculateGroupDiscount(item, group, customer)
        
        if (discount > BigDecimal.ZERO) {
            // Check per-item cap
            group.generalDiscountMaxPerItem?.let { maxPerItem ->
                discount = minOf(discount, maxPerItem)
            }
            
            // Check transaction cap
            group.generalDiscountMaxPerTransaction?.let { maxPerTrans ->
                val remainingCap = maxPerTrans - totalCustomerDiscount
                discount = minOf(discount, remainingCap)
            }
            
            if (discount > BigDecimal.ZERO) {
                item.customerDiscountId = group.id
                item.customerDiscountType = CustomerDiscountType.GROUP
                item.customerDiscountPercent = group.generalDiscountPercent
                item.customerDiscountPerUnit = discount
                item.customerDiscountTotal = discount * item.quantityUsed
                
                totalCustomerDiscount += item.customerDiscountTotal
                
                recalculateItem(item)
            }
        }
    }
}

fun calculateGroupDiscount(
    item: TransactionItemViewModel,
    group: CustomerGroup,
    customer: CustomerProfile
): BigDecimal {
    // Check for item-specific discount first
    if (group.hasItemDiscounts) {
        val itemDiscount = group.itemDiscounts.find { it.productId == item.branchProductId }
        if (itemDiscount != null) {
            return calculateSpecificDiscount(item, itemDiscount)
        }
    }
    
    // Check for category discount
    if (group.hasCategoryDiscounts) {
        val product = getProduct(item.branchProductId)
        val categoryDiscount = group.categoryDiscounts.find { it.categoryId == product.categoryId }
        if (categoryDiscount != null) {
            return calculateCategoryDiscount(item, categoryDiscount)
        }
    }
    
    // Apply general discount
    if (group.generalDiscountPercent > BigDecimal.ZERO) {
        val effectivePrice = item.priceUsed -
            item.promotionDiscountPerUnit -
            item.couponDiscountPerUnit
        
        return effectivePrice * (group.generalDiscountPercent / BigDecimal(100))
    }
    
    return BigDecimal.ZERO
}

fun isExcludedFromGroupDiscount(item: TransactionItemViewModel, group: CustomerGroup): Boolean {
    val product = getProduct(item.branchProductId)
    
    // Check product exclusion
    if (item.branchProductId in group.excludedProductIds) return true
    
    // Check category exclusion
    if (product.categoryId in group.excludedCategoryIds) return true
    
    // Check brand exclusion
    if (product.brandId in group.excludedBrandIds) return true
    
    // Check sale item exclusion
    if (group.excludeSaleItems && item.salePrice > BigDecimal.ZERO) return true
    
    // Check promotion exclusion
    if (group.excludePromotions && item.promotionId != null) return true
    
    // Check coupon exclusion
    if (group.excludeCouponedItems && item.appliedCoupons.isNotEmpty()) return true
    
    return false
}
```

---

## Loyalty Programs

### Loyalty Points System

```kotlin
data class LoyaltyProgram(
    val id: Int,
    val name: String,                         // "Rewards Club"
    
    // Earning Rules
    val pointsPerDollar: BigDecimal,          // 1 point per $1 spent
    val bonusPointsPerDollar: BigDecimal,     // For premium members
    val bonusRules: List<BonusPointRule>,     // Category/product bonus points
    
    // Redemption Rules
    val pointValue: BigDecimal,               // $0.01 per point
    val minRedemptionPoints: Int,             // Minimum to redeem
    val redemptionIncrement: Int,             // Redeem in increments
    val maxRedemptionPercent: BigDecimal,     // Max % of transaction
    
    // Exclusions
    val excludedFromEarning: List<Int>,       // Don't earn on these
    val excludedFromRedemption: List<Int>,    // Can't use points on these
    
    // Expiration
    val pointsExpirationMonths: Int?,         // Points expire after X months
    val expireOnInactivity: Boolean           // Expire if inactive
)

data class BonusPointRule(
    val type: BonusType,                      // Category, Product, Promotion
    val targetId: Int,                        // Category/Product/Promo ID
    val bonusMultiplier: BigDecimal,          // 2x, 3x points, etc.
    val startDate: OffsetDateTime,
    val endDate: OffsetDateTime
)
```

### Points Calculation

```kotlin
fun calculatePointsEarned(transaction: Transaction, customer: CustomerProfile?): Int {
    if (customer == null || !customer.hasLoyaltyCard) {
        return 0
    }
    
    val loyaltyProgram = getLoyaltyProgram()
    var totalPoints = BigDecimal.ZERO
    
    for (item in transaction.items) {
        if (item.isRemoved) continue
        
        // Check if earning is excluded
        if (isExcludedFromEarning(item, loyaltyProgram)) continue
        
        // Calculate base points
        val eligibleAmount = item.subTotal  // Final price × quantity
        val basePoints = eligibleAmount * loyaltyProgram.pointsPerDollar
        
        // Check for bonus points
        val bonusMultiplier = getBonusMultiplier(item, loyaltyProgram)
        val itemPoints = basePoints * bonusMultiplier
        
        totalPoints += itemPoints
    }
    
    // Apply tier bonus
    val tierBonus = customer.tier.bonusPointsPercent / BigDecimal(100)
    totalPoints *= (BigDecimal.ONE + tierBonus)
    
    return totalPoints.setScale(0, RoundingMode.DOWN).toInt()  // Always round down for points
}

fun getBonusMultiplier(item: TransactionItemViewModel, program: LoyaltyProgram): BigDecimal {
    val product = getProduct(item.branchProductId)
    var multiplier = BigDecimal.ONE
    
    for (rule in program.bonusRules) {
        if (!isRuleActive(rule)) continue
        
        when (rule.type) {
            BonusType.CATEGORY -> if (rule.targetId == product.categoryId) {
                multiplier = maxOf(multiplier, rule.bonusMultiplier)
            }
            BonusType.PRODUCT -> if (rule.targetId == product.id) {
                multiplier = maxOf(multiplier, rule.bonusMultiplier)
            }
            BonusType.PROMOTION -> if (rule.targetId == item.promotionId) {
                multiplier = maxOf(multiplier, rule.bonusMultiplier)
            }
        }
    }
    
    return multiplier
}
```

### Points Redemption

```kotlin
fun redeemLoyaltyPoints(
    transaction: Transaction,
    customer: CustomerProfile,
    pointsToRedeem: Int
): LoyaltyRedemptionResult {
    if (customer.loyaltyPoints < pointsToRedeem) {
        throw InsufficientPointsException()
    }
    
    val program = getLoyaltyProgram()
    
    // Validate minimum
    if (pointsToRedeem < program.minRedemptionPoints) {
        throw MinimumPointsNotMetException()
    }
    
    // Validate increment
    var actualPoints = pointsToRedeem
    if (pointsToRedeem % program.redemptionIncrement != 0) {
        actualPoints = (pointsToRedeem / program.redemptionIncrement) * program.redemptionIncrement
    }
    
    // Calculate dollar value
    var redemptionValue = BigDecimal(actualPoints) * program.pointValue
    
    // Check max redemption percent
    val maxRedemption = transaction.subTotal * (program.maxRedemptionPercent / BigDecimal(100))
    redemptionValue = minOf(redemptionValue, maxRedemption)
    
    // Recalculate points needed for capped value
    val actualPointsUsed = (redemptionValue / program.pointValue).toInt()
    
    // Apply as transaction discount
    applyLoyaltyRedemption(transaction, redemptionValue, actualPointsUsed)
    
    // Deduct points from customer
    customer.loyaltyPoints -= actualPointsUsed
    saveCustomer(customer)
    
    return LoyaltyRedemptionResult(
        pointsUsed = actualPointsUsed,
        dollarValue = redemptionValue
    )
}

fun applyLoyaltyRedemption(transaction: Transaction, value: BigDecimal, points: Int) {
    // Apply like an invoice discount
    val eligibleItems = transaction.items.filter { item ->
        !item.isRemoved && !isExcludedFromRedemption(item)
    }
    
    val eligibleTotal = eligibleItems.sumOf { it.subTotal }
    
    for (item in eligibleItems) {
        val proportion = item.subTotal / eligibleTotal
        val itemDiscount = value * proportion
        
        item.customerDiscountId = LOYALTY_REDEMPTION
        item.customerDiscountType = CustomerDiscountType.LOYALTY_POINTS
        item.customerDiscountPerUnit = itemDiscount / item.quantityUsed
        item.customerDiscountTotal = itemDiscount
        
        recalculateItem(item)
    }
    
    transaction.loyaltyPointsRedeemed = points
    transaction.loyaltyRedemptionValue = value
}
```

---

## Employee Discounts

### Employee Discount Rules

```kotlin
data class EmployeeDiscountConfig(
    // Base Discount
    val discountPercent: BigDecimal,          // Standard employee discount (e.g., 15%)
    val familyDiscountPercent: BigDecimal,    // Family member discount (e.g., 10%)
    
    // Category Variations
    val categoryRates: List<CategoryRate>,    // Different rates by category
    
    // Limits
    val maxDiscountPerTransaction: BigDecimal?,
    val maxDiscountPerDay: BigDecimal?,
    val maxDiscountPerWeek: BigDecimal?,
    val maxDiscountPerMonth: BigDecimal?,
    val maxTransactionsPerDay: Int?,
    
    // Exclusions
    val excludedCategories: List<Int>,
    val excludedProducts: List<Int>,
    val excludeSaleItems: Boolean,
    val excludePromotions: Boolean,
    val excludeAlcohol: Boolean,
    val excludeTobacco: Boolean,
    val excludeGiftCards: Boolean,
    val excludeLottery: Boolean,
    
    // Requirements
    val requireManagerApproval: Boolean,
    val requireOnShift: Boolean,              // Must be during shift
    val requireBadgeScan: Boolean             // Must scan badge
)

data class CategoryRate(
    val categoryId: Int,
    val discountPercent: BigDecimal           // Override rate for this category
)
```

### Employee Discount Application

```kotlin
fun applyEmployeeDiscount(transaction: Transaction, employee: Employee) {
    val config = getEmployeeDiscountConfig()
    
    // Validate employee
    if (!employee.isActive) {
        throw EmployeeNotActiveException()
    }
    
    // Check if on shift (if required)
    if (config.requireOnShift && !isEmployeeOnShift(employee)) {
        requireManagerOverride = true
    }
    
    // Check limits
    if (!checkEmployeeSpendLimits(employee, config)) {
        throw EmployeeSpendLimitExceededException()
    }
    
    // Determine discount rate
    var discountRate = config.discountPercent
    if (transaction.isForFamily) {
        discountRate = config.familyDiscountPercent
    }
    
    var totalDiscount = BigDecimal.ZERO
    
    for (item in transaction.items) {
        if (item.isRemoved) continue
        
        // Check exclusions
        if (isExcludedFromEmployeeDiscount(item, config)) continue
        
        // Get category-specific rate if applicable
        val categoryRate = getCategoryRate(item, config)
        if (categoryRate != null) {
            discountRate = categoryRate.discountPercent
        }
        
        // Calculate discount
        val effectivePrice = item.priceUsed  // Employee discount typically from retail
        var discount = effectivePrice * (discountRate / BigDecimal(100))
        
        // Check per-transaction cap
        config.maxDiscountPerTransaction?.let { maxPerTrans ->
            val remainingCap = maxPerTrans - totalDiscount
            discount = minOf(discount, remainingCap)
        }
        
        if (discount > BigDecimal.ZERO) {
            item.customerDiscountType = CustomerDiscountType.EMPLOYEE
            item.customerDiscountPercent = discountRate
            item.customerDiscountPerUnit = discount
            item.customerDiscountTotal = discount * item.quantityUsed
            
            totalDiscount += item.customerDiscountTotal
            
            // Employee discount REPLACES other discounts
            item.promotionDiscountPerUnit = BigDecimal.ZERO
            item.couponDiscountPerUnit = BigDecimal.ZERO
            item.discountAmountPerUnit = BigDecimal.ZERO
            item.transactionDiscountAmountPerUnit = BigDecimal.ZERO
            
            recalculateItem(item)
        }
    }
    
    // Track employee discount usage
    recordEmployeeDiscountUsage(employee, totalDiscount)
    
    transaction.employeeId = employee.id
    transaction.employeeDiscountTotal = totalDiscount
}

fun isExcludedFromEmployeeDiscount(
    item: TransactionItemViewModel,
    config: EmployeeDiscountConfig
): Boolean {
    val product = getProduct(item.branchProductId)
    
    if (product.categoryId in config.excludedCategories) return true
    if (product.id in config.excludedProducts) return true
    if (config.excludeSaleItems && item.salePrice > BigDecimal.ZERO) return true
    if (config.excludePromotions && item.promotionId != null) return true
    if (config.excludeAlcohol && product.isAlcohol) return true
    if (config.excludeTobacco && product.isTobacco) return true
    if (config.excludeGiftCards && product.isGiftCard) return true
    if (config.excludeLottery && product.isLottery) return true
    
    return false
}
```

---

## Personalized Offers

### Customer-Specific Promotions

```kotlin
data class PersonalizedOffer(
    val id: Int,
    val customerId: Int,                      // Specific customer
    val offerCode: String,
    val description: String,
    
    // Offer Type
    val type: OfferType,                      // Discount, FreeItem, BonusPoints
    val discountValue: BigDecimal?,
    val freeProductId: Int?,
    val bonusPoints: Int?,
    
    // Qualifying Items
    val qualifyingProductIds: List<Int>,
    val qualifyingCategoryIds: List<Int>,
    val minPurchase: BigDecimal?,
    val minQuantity: Int?,
    
    // Validity
    val startDate: OffsetDateTime,
    val expirationDate: OffsetDateTime,
    val singleUse: Boolean,
    val used: Boolean = false,
    
    // Tracking
    val offerSource: String,                  // "Email", "App", "Receipt"
    val campaign: String?                     // Marketing campaign ID
)

fun applyPersonalizedOffer(transaction: Transaction, offerCode: String) {
    val offer = findOffer(offerCode) ?: throw OfferNotFoundException()
    
    // Validate
    if (offer.customerId != transaction.customerId) {
        throw OfferNotForThisCustomerException()
    }
    
    if (offer.used && offer.singleUse) {
        throw OfferAlreadyUsedException()
    }
    
    if (OffsetDateTime.now() > offer.expirationDate) {
        throw OfferExpiredException()
    }
    
    // Check qualifying items
    val qualifyingItems = transaction.items.filter { item ->
        isItemInOffer(item, offer)
    }
    
    if (qualifyingItems.isEmpty()) {
        throw NoQualifyingItemsException()
    }
    
    // Apply offer
    when (offer.type) {
        OfferType.DISCOUNT -> applyOfferDiscount(qualifyingItems, offer)
        OfferType.FREE_ITEM -> applyOfferFreeItem(transaction, offer)
        OfferType.BONUS_POINTS -> applyOfferBonusPoints(transaction, offer)
    }
    
    // Mark as used if single-use
    if (offer.singleUse) {
        offer.used = true
        saveOffer(offer)
    }
}
```

---

## Multiple Group Handling

### Group Priority

When customer belongs to multiple groups:

```kotlin
fun resolveMultipleGroups(customer: CustomerProfile): List<CustomerGroup> {
    val groups = customer.getAllGroups()  // Primary + secondary
    
    // Sort by priority/exclusivity
    val sortedGroups = groups.sortedBy { it.priority }
    
    // Check for exclusive groups
    for (group in sortedGroups) {
        if (group.isExclusive) {
            // Use only this group
            return listOf(group)
        }
    }
    
    // Check if stacking allowed
    val stackableGroups = groups.filter { it.stackWithOtherGroups }
    val nonStackableGroups = groups.filter { !it.stackWithOtherGroups }
    
    return if (nonStackableGroups.isNotEmpty()) {
        // Use highest priority non-stackable
        listOf(nonStackableGroups.first())
    } else {
        stackableGroups  // All stackable groups
    }
}

fun applyMultipleGroupDiscounts(transaction: Transaction, groups: List<CustomerGroup>) {
    for (item in transaction.items) {
        if (item.isRemoved) continue
        
        var bestDiscount = BigDecimal.ZERO
        var bestGroup: CustomerGroup? = null
        
        for (group in groups) {
            if (isExcludedFromGroupDiscount(item, group)) continue
            
            val discount = calculateGroupDiscount(item, group, transaction.customer!!)
            if (discount > bestDiscount) {
                bestDiscount = discount
                bestGroup = group
            }
        }
        
        if (bestDiscount > BigDecimal.ZERO && bestGroup != null) {
            item.customerDiscountId = bestGroup.id
            item.customerDiscountPerUnit = bestDiscount
            item.customerDiscountTotal = bestDiscount * item.quantityUsed
            
            recalculateItem(item)
        }
    }
}
```

---

[← Back to Index](./INDEX.md) | [Previous: Discounts](./DISCOUNTS.md) | [Next: Tax Calculations →](./TAX_CALCULATIONS.md)

