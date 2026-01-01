# Promotions Engine

[← Back to Index](./INDEX.md) | [Previous: Price Determination](./PRICE_DETERMINATION.md) | [Next: Discounts →](./DISCOUNTS.md)

---

## Overview

The Promotions Engine handles complex multi-item discounts including Mix & Match, Buy-One-Get-One (BOGO), Multi-Buy pricing, and threshold-based promotions.

---

## Promotion Types

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         PROMOTION TYPES                                 │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  SINGLE-ITEM PROMOTIONS                                                 │
│  ══════════════════════                                                 │
│  • Percent Off Item       (10% off this item)                           │
│  • Amount Off Item        ($2 off this item)                            │
│  • Sale Price             (Special price $X)                            │
│                                                                         │
│  QUANTITY-BASED PROMOTIONS                                              │
│  ═════════════════════════                                              │
│  • Multi-Buy Fixed        (3 for $10)                                   │
│  • Multi-Buy Percent      (Buy 2, get 25% off each)                     │
│  • BOGO Same Item         (Buy 1 Get 1 Free/50% off)                    │
│  • BXGY Same Item         (Buy 3 Get 1 Free)                            │
│                                                                         │
│  MIX & MATCH PROMOTIONS                                                 │
│  ══════════════════════                                                 │
│  • Mix & Match Quantity   (Any 5 from group for $10)                    │
│  • Mix & Match Tiered     (5 for $10, 10 for $18)                       │
│  • Mix & Match Categories (Mix produce items, save 20%)                 │
│                                                                         │
│  CROSS-CATEGORY PROMOTIONS                                              │
│  ═════════════════════════                                              │
│  • Buy A Get B Free       (Buy chips, get salsa free)                   │
│  • Buy A Get B Discounted (Buy entree, 50% off dessert)                 │
│  • Bundle Pricing         (Burger + Fries + Drink = $8)                 │
│                                                                         │
│  THRESHOLD PROMOTIONS                                                   │
│  ════════════════════                                                   │
│  • Spend X Save Y         (Spend $50, save $5)                          │
│  • Spend X Get Item       (Spend $100, get free gift)                   │
│  • Category Spend         (Spend $30 on beauty, get 20% off)            │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Mix & Match Promotions

### Mix & Match Definition

```kotlin
data class MixMatchPromotion(
    val id: Int,
    val name: String,                      // "Pick Any 5 Yogurts for $5"
    val description: String?,
    
    // Groups of qualifying items
    val qualifyingGroups: List<MixMatchGroup>,
    
    // Qualification
    val totalRequiredQuantity: Int,        // Total items needed
    val totalRequiredSpend: BigDecimal?,   // OR: total $ from groups
    
    // Reward
    val rewardType: MixMatchRewardType,
    val rewardValue: BigDecimal,
    
    // Allocation
    val allocation: MixMatchAllocation,
    
    // Limits
    val maxApplicationsPerTransaction: Int?,
    val isExclusive: Boolean,
    
    // Validity
    val startDate: OffsetDateTime,
    val endDate: OffsetDateTime?,
    val active: Boolean
)

data class MixMatchGroup(
    val groupId: Int,
    val groupName: String,
    val productIds: List<Int>,
    val categoryIds: List<Int>,
    val brandIds: List<Int>,
    val departmentIds: List<Int>,
    val minQuantityFromGroup: Int?,
    val maxQuantityFromGroup: Int?
)

enum class MixMatchRewardType {
    FIXED_TOTAL,             // X items for $Y total
    PERCENT_OFF_ALL,         // X items get Y% off each
    AMOUNT_OFF_EACH,         // X items get $Y off each
    CHEAPEST_FREE,           // X items, cheapest is free
    CHEAPEST_PERCENT_OFF     // X items, cheapest is Y% off
}

enum class MixMatchAllocation {
    PROPORTIONAL,            // Discount split by price ratio
    EQUAL,                   // Equal discount to each item
    CHEAPEST_FIRST,          // Apply to cheapest items
    MOST_EXPENSIVE_FIRST     // Apply to most expensive
}
```

### Mix & Match Evaluation

```kotlin
fun evaluateMixMatchPromotion(
    promo: MixMatchPromotion,
    orderItems: List<TransactionItemViewModel>
): PromotionResult {
    
    // Step 1: Find qualifying items
    val qualifyingItems = mutableListOf<TransactionItemViewModel>()
    
    for (group in promo.qualifyingGroups) {
        val groupItems = orderItems.filter { item ->
            isItemInGroup(item, group) &&
            !item.hasExclusivePromotion &&
            !item.isRemoved
        }.let { items ->
            if (group.maxQuantityFromGroup != null) {
                items.take(group.maxQuantityFromGroup)
            } else items
        }
        qualifyingItems.addAll(groupItems)
    }
    
    // Step 2: Check minimum requirements
    val totalQty = qualifyingItems.sumOf { it.quantityUsed }
    
    if (totalQty < BigDecimal(promo.totalRequiredQuantity)) {
        return PromotionResult.NoMatch
    }
    
    // Check per-group minimums
    for (group in promo.qualifyingGroups) {
        val groupQty = countFromGroup(qualifyingItems, group)
        if (group.minQuantityFromGroup != null && groupQty < group.minQuantityFromGroup) {
            return PromotionResult.NoMatch
        }
    }
    
    // Step 3: Calculate complete sets
    var completeSets = (totalQty / BigDecimal(promo.totalRequiredQuantity)).toInt()
    if (promo.maxApplicationsPerTransaction != null) {
        completeSets = minOf(completeSets, promo.maxApplicationsPerTransaction)
    }
    
    // Step 4: Select items and calculate discount
    val selectedItems = selectItemsForPromotion(
        qualifyingItems,
        completeSets * promo.totalRequiredQuantity,
        promo.allocation
    )
    
    val discount = calculateMixMatchDiscount(promo, selectedItems)
    allocateDiscount(selectedItems, discount, promo.allocation)
    
    return PromotionResult.Match(
        promotionId = promo.id,
        itemsAffected = selectedItems,
        totalDiscount = discount,
        completeSets = completeSets
    )
}
```

---

## BOGO Promotions

### BOGO Definition

```kotlin
data class BogoPromotion(
    val id: Int,
    val name: String,                      // "Buy 1 Get 1 Free"
    
    // Buy Requirement
    val buyQuantity: Int,
    val buyProductIds: List<Int>,
    val buyCategoryIds: List<Int>,
    val buyMinPrice: BigDecimal?,
    
    // Get Reward
    val getQuantity: Int,
    val getProductIds: List<Int>,
    val getCategoryIds: List<Int>,
    
    // Discount on "Get" items
    val discountType: BogoDiscountType,
    val discountValue: BigDecimal,
    
    // Limits
    val maxApplicationsPerTransaction: Int?,
    val requireDifferentItems: Boolean,
    
    // Item Selection
    val getItemSelection: BogoItemSelection,
    
    // Validity
    val startDate: OffsetDateTime,
    val endDate: OffsetDateTime?,
    val active: Boolean
)

enum class BogoDiscountType {
    FREE,                    // 100% off
    PERCENT_OFF,             // X% off
    AMOUNT_OFF,              // $X off
    FIXED_PRICE              // Get item for $X
}

enum class BogoItemSelection {
    CHEAPEST,
    MOST_EXPENSIVE,
    CUSTOMER_CHOICE,
    SPECIFIC_PRODUCT
}
```

### BOGO Evaluation

```kotlin
fun evaluateBogoPromotion(
    promo: BogoPromotion,
    orderItems: List<TransactionItemViewModel>
): PromotionResult {
    
    // Step 1: Find "Buy" qualifying items
    val buyItems = orderItems
        .filter { item ->
            isItemInBogoGroup(item, promo.buyProductIds, promo.buyCategoryIds) &&
            (promo.buyMinPrice == null || item.priceUsed >= promo.buyMinPrice) &&
            !item.hasExclusivePromotion &&
            !item.isRemoved
        }
        .sortedByDescending { it.priceUsed }
    
    // Step 2: Find "Get" qualifying items
    val getItems = if (promo.requireDifferentItems) {
        orderItems.filter { item ->
            isItemInBogoGroup(item, promo.getProductIds, promo.getCategoryIds) &&
            !buyItems.contains(item) &&
            !item.hasExclusivePromotion
        }
    } else {
        buyItems
    }.let { items ->
        sortBySelection(items, promo.getItemSelection)
    }
    
    // Step 3: Calculate complete sets
    val buyUnits = buyItems.sumOf { it.quantityUsed }
    val getUnits = getItems.sumOf { it.quantityUsed }
    
    val possibleSets = (buyUnits / BigDecimal(promo.buyQuantity)).toInt()
    val possibleRewards = (getUnits / BigDecimal(promo.getQuantity)).toInt()
    
    var completeSets = minOf(possibleSets, possibleRewards)
    if (promo.maxApplicationsPerTransaction != null) {
        completeSets = minOf(completeSets, promo.maxApplicationsPerTransaction)
    }
    
    if (completeSets == 0) return PromotionResult.NoMatch
    
    // Step 4: Mark buy items and apply discount to get items
    markBuyItems(buyItems, completeSets * promo.buyQuantity, promo.id)
    val rewardItemCount = completeSets * promo.getQuantity
    val discountedItems = applyBogoDiscount(getItems, rewardItemCount, promo)
    
    return PromotionResult.Match(
        promotionId = promo.id,
        triggerItems = buyItems.take(completeSets * promo.buyQuantity),
        rewardItems = discountedItems,
        completeSets = completeSets
    )
}
```

---

## Multi-Buy Promotions

### Multi-Buy Definition

```kotlin
data class MultiBuyPromotion(
    val id: Int,
    val name: String,                      // "3 for $10"
    
    // Qualifying Items
    val productIds: List<Int>,
    val categoryIds: List<Int>,
    val brandIds: List<Int>,
    
    // Buy Requirement
    val quantity: Int,
    
    // Price
    val priceType: MultiBuyPriceType,
    val priceValue: BigDecimal,
    
    // Limits
    val maxApplicationsPerTransaction: Int?,
    val mustBuyExact: Boolean,
    
    // Validity
    val startDate: OffsetDateTime,
    val endDate: OffsetDateTime?,
    val active: Boolean
)

enum class MultiBuyPriceType {
    FIXED_TOTAL,             // X for $Y total
    EACH_AT_PRICE,           // X at $Y each
    PERCENT_OFF_EACH,        // X at Y% off each
    AMOUNT_OFF_EACH          // X at $Y off each
}
```

---

## Promotion Priority and Stacking

### Stacking Rules

```kotlin
data class PromotionStackingRules(
    // Global flags
    val allowMultiplePromotionsPerItem: Boolean = false,
    val allowPromotionPlusCoupon: Boolean = true,
    
    // Priority order (lower = higher priority)
    val priorityOrder: Map<PromotionType, Int> = mapOf(
        PromotionType.MIX_MATCH to 1,
        PromotionType.BOGO to 2,
        PromotionType.MULTI_BUY to 3,
        PromotionType.THRESHOLD to 4,
        PromotionType.PERCENT_OFF to 5,
        PromotionType.SALE_PRICE to 6
    )
)
```

### Best Deal Logic

```kotlin
fun selectBestPromotion(
    item: TransactionItemViewModel,
    applicablePromotions: List<PromotionResult>
): PromotionResult? {
    
    if (applicablePromotions.isEmpty()) return null
    if (applicablePromotions.size == 1) return applicablePromotions[0]
    
    // Calculate savings for each and return the best
    return applicablePromotions.maxByOrNull { promo ->
        calculatePromotionSavings(item, promo)
    }
}
```

---

## Promotion Display on Receipt

```
For Mix & Match:
  CHEERIOS 10OZ               $4.29
    Mix & Match 5 for $5     -$0.86

For BOGO:
  SODA 2-LITER                $2.99
  SODA 2-LITER                $2.99
    Buy 1 Get 1 Free         -$2.99

For Multi-Buy:
  CANDY BAR                   $1.79
  CANDY BAR                   $1.79
  CANDY BAR                   $1.79
    3 for $4.00              -$1.37
```

---

[← Back to Index](./INDEX.md) | [Previous: Price Determination](./PRICE_DETERMINATION.md) | [Next: Discounts →](./DISCOUNTS.md)

