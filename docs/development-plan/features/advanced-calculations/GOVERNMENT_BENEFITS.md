# Government Benefits Processing

[← Back to Index](./INDEX.md) | [Previous: Tax Calculations](./TAX_CALCULATIONS.md) | [Next: Deposits & Fees →](./DEPOSITS_FEES.md)

---

## Overview

This module covers government-funded payment programs including SNAP (Supplemental Nutrition Assistance Program), WIC (Women, Infants, and Children), and EBT Cash Benefits.

---

## Program Overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    GOVERNMENT BENEFIT PROGRAMS                          │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  SNAP / EBT FOOD STAMPS                                                 │
│  ══════════════════════                                                 │
│  • Covers eligible food items                                           │
│  • TAX EXEMPT on paid portion                                           │
│  • Cannot cover: alcohol, tobacco, hot food, non-food                   │
│  • No quantity restrictions                                             │
│                                                                         │
│  EBT CASH BENEFITS                                                      │
│  ═════════════════                                                      │
│  • TANF (Temporary Assistance for Needy Families)                       │
│  • Treated like cash/debit                                              │
│  • NOT tax exempt                                                       │
│  • Can cover any item (with retailer restrictions)                      │
│                                                                         │
│  WIC (Women, Infants, and Children)                                     │
│  ══════════════════════════════════                                     │
│  • Covers specific approved items only                                  │
│  • Strict brand/size/quantity limits                                    │
│  • TAX EXEMPT                                                           │
│  • Items must match WIC prescription                                    │
│  • Includes Cash Value Benefit (CVB) for produce                        │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## SNAP/EBT Food Stamps

### SNAP Eligibility Configuration

```kotlin
data class Product(
    // ... other fields ...
    val isSNAPEligible: Boolean  // Can be purchased with SNAP
)

// SNAP Eligible Categories (typical):
// - Fresh/Frozen Produce
// - Fresh/Frozen Meat & Seafood
// - Dairy & Eggs
// - Bread & Bakery (not hot/prepared)
// - Canned & Packaged Foods
// - Beverages (non-alcoholic)
// - Snacks
// - Seeds & Plants (food producing)

// SNAP Ineligible:
// - Alcohol
// - Tobacco
// - Hot/Prepared Foods (ready to eat)
// - Non-food items (cleaning, paper, pet food)
// - Vitamins & Supplements
// - Medicine
```

### SNAP Payment Processing

```kotlin
class SNAPPaymentProcessor(
    private val orderStore: OrderStore,
    private val paymentTerminal: PaymentTerminal
) {
    
    suspend fun processSNAPPayment(paymentAmount: BigDecimal): PaymentResult {
        val items = orderStore.orderItems.value
        
        // Step 1: Calculate total SNAP-eligible amount
        val eligibleTotal = calculateSNAPEligible(items)
        
        if (eligibleTotal <= BigDecimal.ZERO) {
            return PaymentResult.Error("No SNAP-eligible items in transaction")
        }
        
        // Step 2: Validate payment doesn't exceed eligible
        val actualPayment = minOf(paymentAmount, eligibleTotal)
        
        // Step 3: Process EBT card
        val ebtResult = paymentTerminal.processEBT(
            amount = actualPayment,
            transactionType = EBTType.SNAP_FOOD
        )
        
        if (ebtResult.status != PaymentStatus.Success) {
            return PaymentResult.Declined(ebtResult.errorMessage)
        }
        
        // Step 4: Apply payment to items
        applySNAPPaymentToItems(ebtResult.approvedAmount)
        
        // Step 5: Recalculate taxes (SNAP portion is exempt)
        recalculateTransactionTotals()
        
        return PaymentResult.Success(
            approvedAmount = ebtResult.approvedAmount,
            remainingBalance = ebtResult.remainingBalance
        )
    }
    
    fun calculateSNAPEligible(items: List<TransactionItem>): BigDecimal {
        return items
            .filter { !it.isRemoved && it.isSNAPEligible && it.snapPaidPercent < BigDecimal(100) }
            .sumOf { item ->
                // Calculate unpaid portion eligible for SNAP
                item.subjectToTaxTotal - item.snapPaidAmount
            }
            .setScale(2, RoundingMode.HALF_UP)
    }
}
```

### SNAP Payment Allocation

SNAP payments are allocated strategically to **maximize tax savings for the customer**:

```kotlin
fun applySNAPPaymentToItems(snapAmount: BigDecimal) {
    val items = orderStore.orderItems.value
    
    // Priority: Apply to taxable items first (saves customer tax)
    val eligibleItems = items.filter { item ->
        !item.isRemoved && item.isSNAPEligible && item.snapPaidPercent < BigDecimal(100)
    }
    
    // Sort: Taxable items first, then by lowest SNAP coverage
    val sortedItems = eligibleItems.sortedWith(
        compareByDescending<TransactionItem> { it.taxPercentSum > BigDecimal.ZERO }
            .thenBy { it.snapPaidPercent }
    )
    
    var remainingSnap = snapAmount
    
    for (item in sortedItems) {
        if (remainingSnap <= BigDecimal.ZERO) break
        
        // Calculate maximum that can be applied to this item
        val maxApplicable = item.subjectToTaxTotal - item.snapPaidAmount
        
        if (maxApplicable <= BigDecimal.ZERO) continue
        
        val applyToItem = minOf(remainingSnap, maxApplicable)
        
        // Update item SNAP tracking
        updateItemSNAPPayment(item, applyToItem)
        
        remainingSnap -= applyToItem
    }
    
    // Handle bag fee (typically made free when using SNAP)
    if (orderStore.bagProduct.value != null && remainingSnap > BigDecimal.ZERO) {
        applyBagDiscount()
    }
}
```

### SNAP Field Updates

```kotlin
fun updateItemSNAPPayment(item: TransactionItem, amount: BigDecimal) {
    val newSnapPaidAmount = item.snapPaidAmount + amount
    val total = item.finalPrice * item.quantityUsed
    val newSnapPaidPercent = (newSnapPaidAmount / total) * BigDecimal(100)
    
    // Recalculate tax (SNAP portion is exempt)
    val taxablePortion = BigDecimal.ONE - (newSnapPaidPercent / BigDecimal(100))
    val newSubjectToTaxTotal = (item.finalPrice * item.quantityUsed) * taxablePortion
    val newTaxTotal = (item.taxPerUnit * item.quantityUsed * taxablePortion)
        .setScale(2, RoundingMode.HALF_UP)
    
    // Update item
    orderStore.updateItem(item.transactionItemGuid) { current ->
        current.copy(
            snapPaidAmount = newSnapPaidAmount,
            snapPaidPercent = newSnapPaidPercent,
            subjectToTaxTotal = newSubjectToTaxTotal,
            taxTotal = newTaxTotal,
            taxes = recalculateTaxBreakdown(current, taxablePortion)
        )
    }
}
```

### SNAP Payment Priority

```
┌─────────────────────────────────────────────────────────────────┐
│              SNAP PAYMENT APPLICATION ORDER                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  SNAP payments are applied to items in this order:              │
│                                                                 │
│  1. SNAP-ELIGIBLE ITEMS WITH TAX (highest priority)             │
│     └── Paying with SNAP eliminates tax on these items          │
│     └── Greatest benefit to customer                            │
│                                                                 │
│  2. SNAP-ELIGIBLE ITEMS WITHOUT TAX (second priority)           │
│     └── Tax-exempt food items                                   │
│                                                                 │
│  3. PARTIALLY SNAP-PAID ITEMS (within groups above)             │
│     └── Items with lower snapPaidPercent first                  │
│     └── Ensures even distribution                               │
│                                                                 │
│  NOTE: Bag products are given 100% discount when SNAP is used   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## EBT Cash Benefits

EBT Cash Benefits (TANF) are treated like regular cash/debit:

```kotlin
suspend fun processEBTCashPayment(amount: BigDecimal): PaymentResult {
    val ebtResult = paymentTerminal.processEBT(
        amount = amount,
        transactionType = EBTType.CASH_BENEFIT
    )
    
    if (ebtResult.status == PaymentStatus.Success) {
        // EBT Cash does NOT affect tax calculations
        // Process like regular payment
        applyRegularPaymentToItems(ebtResult.approvedAmount)
    }
    
    return PaymentResult(
        status = ebtResult.status,
        approvedAmount = ebtResult.approvedAmount
    )
}
```

### Key Differences from SNAP

| Feature | SNAP | EBT Cash |
|---------|------|----------|
| Tax Exemption | ✅ Yes (paid portion) | ❌ No |
| Item Restrictions | ✅ Food only | ❌ Minimal |
| Payment Priority | First (to save tax) | After SNAP |
| Affects Tax Calculation | ✅ Yes | ❌ No |

---

## WIC (Women, Infants, and Children)

### WIC Item Configuration

```kotlin
data class Product(
    // ... other fields ...
    val isWICApproved: Boolean,
    val wicCategoryId: Int?,
    val wicUPCList: List<String>?  // Specific UPCs allowed
)

data class WICPrescription(
    val categoryId: Int,
    val categoryName: String,      // "Milk, Gallon"
    val allowedQuantity: BigDecimal,
    val remainingQuantity: BigDecimal,
    val allowedBrands: List<String>,
    val allowedSizes: List<String>
)
```

### WIC Validation

```kotlin
class WICValidator {
    
    fun validateWICItem(
        item: TransactionItem,
        prescription: WICPrescription
    ): WICValidationResult {
        
        // Check if item is WIC-approved
        if (!item.isWICApproved) {
            return WICValidationResult.NotApproved
        }
        
        // Check category match
        if (item.wicCategoryId != prescription.categoryId) {
            return WICValidationResult.WrongCategory
        }
        
        // Check quantity limit
        if (item.quantityUsed > prescription.remainingQuantity) {
            return WICValidationResult.ExceedsLimit(
                allowed = prescription.remainingQuantity,
                requested = item.quantityUsed
            )
        }
        
        // Check brand restrictions
        if (prescription.allowedBrands.isNotEmpty() && 
            item.brandName !in prescription.allowedBrands) {
            return WICValidationResult.BrandNotAllowed
        }
        
        return WICValidationResult.Valid
    }
}
```

### WIC Payment Processing

```kotlin
suspend fun processWICPayment(wicCard: WICCard): PaymentResult {
    val items = orderStore.orderItems.value
    val wicEligibleItems = items.filter { it.isWICApproved && !it.isRemoved }
    
    // Validate each item against WIC prescription
    val prescriptions = wicCard.getPrescriptions()
    
    for (item in wicEligibleItems) {
        val prescription = prescriptions.find { it.categoryId == item.wicCategoryId }
            ?: continue
            
        val validation = wicValidator.validateWICItem(item, prescription)
        if (validation != WICValidationResult.Valid) {
            return PaymentResult.Error("WIC validation failed: $validation")
        }
    }
    
    // Process WIC payment
    val wicTotal = wicEligibleItems.sumOf { it.subTotal }
    val result = paymentTerminal.processWIC(wicCard, wicTotal)
    
    if (result.status == PaymentStatus.Success) {
        // WIC items are fully tax-exempt
        applyWICPaymentToItems(wicEligibleItems, result.approvedAmount)
    }
    
    return result
}
```

---

## Mixed Payment Scenarios

### SNAP + Regular Payment

```kotlin
// Example: $100 transaction, $60 SNAP-eligible, EBT SNAP $50, Cash $50.08

// 1. Calculate totals
val grandTotal = 100.00
val snapEligible = 60.00

// 2. Apply SNAP first (prioritize taxable items)
// SNAP $50 applied to:
//   - Soda $25 (taxable) → $25 from SNAP, tax now $0
//   - Bread $25 (taxable) → $25 from SNAP, tax now $0

// 3. Recalculate tax
// Before SNAP: Tax on $60 @ 9.5% = $5.70
// After SNAP:  Tax on ($60 - $50) = $10 @ 9.5% = $0.95
// Tax savings: $4.75!

// 4. New grand total
val newGrandTotal = subtotal + newTax  // $100 + $0.95 = $100.95
// Wait... but SNAP already paid $50

// 5. Remaining due
val remaining = newGrandTotal - snapApplied  // $100.95 - $50 = $50.95
// Actually: The recalculation means...
// Original items: $94.30 (subtotal) + $5.70 (tax) = $100
// After SNAP: $94.30 - $50 = $44.30 (remaining subtotal)
//             Tax on remaining taxable = $0.95
//             Remaining due = $44.30 + $0.95 = $45.25
```

### Correct SNAP Calculation Flow

```kotlin
data class SNAPCalculationResult(
    val originalSubtotal: BigDecimal,
    val originalTax: BigDecimal,
    val originalGrandTotal: BigDecimal,
    val snapEligibleAmount: BigDecimal,
    val snapAppliedAmount: BigDecimal,
    val adjustedTax: BigDecimal,
    val adjustedGrandTotal: BigDecimal,
    val remainingDue: BigDecimal
)

fun calculateSNAPImpact(
    items: List<TransactionItem>,
    snapPaymentAmount: BigDecimal
): SNAPCalculationResult {
    val activeItems = items.filter { !it.isRemoved }
    
    val originalSubtotal = activeItems.sumOf { it.subTotal }
    val originalTax = activeItems.sumOf { it.taxTotal }
    val originalGrandTotal = originalSubtotal + originalTax
    
    val snapEligible = activeItems
        .filter { it.isSNAPEligible }
        .sumOf { it.subTotal }
    
    val snapApplied = minOf(snapPaymentAmount, snapEligible)
    
    // Calculate new tax (SNAP-paid portion is exempt)
    val adjustedTax = activeItems.sumOf { item ->
        if (item.isSNAPEligible) {
            val itemSnapFraction = snapApplied / snapEligible
            val taxableFraction = BigDecimal.ONE - itemSnapFraction
            (item.taxPerUnit * item.quantityUsed * taxableFraction)
                .setScale(2, RoundingMode.HALF_UP)
        } else {
            item.taxTotal
        }
    }
    
    val adjustedGrandTotal = originalSubtotal + adjustedTax
    val remainingDue = adjustedGrandTotal - snapApplied
    
    return SNAPCalculationResult(
        originalSubtotal = originalSubtotal,
        originalTax = originalTax,
        originalGrandTotal = originalGrandTotal,
        snapEligibleAmount = snapEligible,
        snapAppliedAmount = snapApplied,
        adjustedTax = adjustedTax,
        adjustedGrandTotal = adjustedGrandTotal,
        remainingDue = remainingDue
    )
}
```

---

## Payment Order for Benefits

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    PAYMENT ORDER (Optimal for Customer)                  │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  1. WIC FIRST (most restrictive)                                        │
│     └── Must match prescription exactly                                 │
│     └── Cannot use on non-WIC items                                     │
│     └── Tax exempt                                                      │
│                                                                         │
│  2. SNAP SECOND (saves tax)                                             │
│     └── Apply to SNAP-eligible items                                    │
│     └── Prioritize taxable items first                                  │
│     └── Reduces tax liability                                           │
│                                                                         │
│  3. EBT CASH THIRD                                                      │
│     └── Like regular payment                                            │
│     └── No tax impact                                                   │
│                                                                         │
│  4. REGULAR PAYMENTS LAST                                               │
│     └── Credit/Debit/Cash                                               │
│     └── Applied to remaining balance                                    │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Worked Example

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    EXAMPLE: MIXED SNAP TRANSACTION                       │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ITEMS:                                                                 │
│    1. Bread $3.50 (SNAP-eligible, tax-exempt)                          │
│    2. Soda $2.59 + $0.25 tax (SNAP-eligible, taxable)                  │
│    3. Paper Towels $5.99 + $0.57 tax (NOT SNAP-eligible)               │
│                                                                         │
│  BEFORE PAYMENT:                                                        │
│    Subtotal: $3.50 + $2.59 + $5.99 = $12.08                            │
│    Tax: $0 + $0.25 + $0.57 = $0.82                                     │
│    Grand Total: $12.90                                                  │
│    SNAP Eligible: $3.50 + $2.59 = $6.09                                │
│                                                                         │
│  PAYMENT: EBT SNAP $6.00, Cash $6.90                                   │
│                                                                         │
│  SNAP ALLOCATION (prioritize taxable first):                           │
│    1. Soda $2.59 fully from SNAP → tax becomes $0 (saved $0.25!)       │
│    2. Bread $3.41 from SNAP ($6.00 - $2.59)                            │
│                                                                         │
│  AFTER SNAP:                                                            │
│    Bread remaining: $0.09 (snapPaidPercent = 97.4%)                    │
│    Bread tax: $0.00 (was already $0)                                   │
│    Soda tax: $0.00 (was $0.25, now SNAP-paid)                          │
│    Paper towels: $5.99 + $0.57 = $6.56                                 │
│                                                                         │
│  ADJUSTED TOTALS:                                                       │
│    Subtotal: $12.08 (unchanged)                                        │
│    Adjusted Tax: $0.57 (saved $0.25)                                   │
│    Adjusted Grand Total: $12.65                                        │
│    SNAP Applied: $6.00                                                  │
│    Cash Needed: $6.65                                                   │
│    Change Due: $6.90 - $6.65 = $0.25                                   │
│                                                                         │
│  CUSTOMER BENEFIT:                                                      │
│    - Tax savings: $0.25                                                │
│    - Total saved by using SNAP optimally                               │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Related Documentation

- [TAX_CALCULATIONS.md](./TAX_CALCULATIONS.md) - Tax calculation details
- [PAYMENT_PROCESSING.md](./PAYMENT_PROCESSING.md) - Full payment flow
- [../BUSINESS_RULES.md](../BUSINESS_RULES.md) - Business rules
- [../../modules/SERVICES.md](../../modules/SERVICES.md) - Calculator services

---

[← Back to Index](./INDEX.md) | [Previous: Tax Calculations](./TAX_CALCULATIONS.md) | [Next: Deposits & Fees →](./DEPOSITS_FEES.md)

