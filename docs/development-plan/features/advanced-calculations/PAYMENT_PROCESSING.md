# Payment Processing

[← Back to Index](./INDEX.md) | [Previous: Deposits & Fees](./DEPOSITS_FEES.md) | [Next: Returns & Adjustments →](./RETURNS_ADJUSTMENTS.md)

---

## Overview

Payment processing handles applying one or more payment methods to a transaction. The system ensures:

1. **Order Independence** - Same total regardless of payment order
2. **Tax Accuracy** - SNAP/WIC portions are correctly tax-exempt
3. **Maximum Customer Benefit** - Optimal allocation of benefits
4. **WIC Certification Compliance** - Strict adherence to WIC rules

---

## Critical Requirement: Order-Independent Payments

> **RULE: Payment order MUST NOT affect the final totals.**
>
> A customer paying Cash → SNAP → Credit must pay the SAME total as
> a customer paying SNAP → Cash → Credit for identical items.

This is achieved by:
1. Pre-calculating eligibility for each payment type
2. Using deterministic allocation algorithms
3. Applying tax exemptions based on final allocations, not payment order

---

## Payment Types

```kotlin
enum class TransactionPaymentType {
    // Government Benefits (Tax-Exempt)
    WIC,                     // Women, Infants, Children - MOST RESTRICTIVE
    EBTFoodstamp,            // SNAP benefits - FOOD ITEMS ONLY
    
    // Government Benefits (Taxable)
    EBTCashBenefit,          // TANF cash benefits - treated like cash
    
    // Store Tender
    GiftCard,                // Store gift card
    StoreCredit,             // Store credit
    LoyaltyPoints,           // Points redemption
    
    // External Tender
    Credit,                  // Credit card
    Debit,                   // Debit card
    Check,                   // Personal check
    
    // Cash
    Cash,                    // Cash tendered
    CashChange               // Change returned (negative)
}
```

---

## Payment Allocation Architecture

### The Core Principle

Instead of applying payments in the order received, we use a **deterministic allocation model**:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    PAYMENT ALLOCATION MODEL                             │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  STEP 1: CALCULATE ELIGIBILITY (Before any payment)                     │
│  ════════════════════════════════════════════════════                   │
│  For each line item, determine:                                         │
│    • wicEligibleAmount      (based on WIC rules)                        │
│    • snapEligibleAmount     (based on SNAP eligibility flag)            │
│    • regularAmount          (everything else)                           │
│                                                                         │
│  STEP 2: CREATE ALLOCATION POOLS                                        │
│  ═════════════════════════════════                                      │
│  │ Pool A: WIC-Only Items        │ Only WIC can pay                     │
│  │ Pool B: SNAP+WIC Items        │ Both can pay (WIC priority)          │
│  │ Pool C: SNAP-Only Items       │ Only SNAP can pay                    │
│  │ Pool D: Taxable SNAP Items    │ SNAP can pay, has tax benefit        │
│  │ Pool E: Regular Items         │ Any payment method                   │
│  │ Pool F: Tax Amounts           │ Regular payments only                │
│                                                                         │
│  STEP 3: APPLY PAYMENTS TO POOLS (Deterministic Order)                  │
│  ═══════════════════════════════════════════════════════                │
│  Regardless of WHEN payment is received:                                │
│    1. WIC → Pool A, Pool B                                              │
│    2. SNAP → Pool D (taxable first!), Pool C, Pool B (remainder)        │
│    3. Regular → Pool E, Pool F, any remainder                           │
│                                                                         │
│  STEP 4: RECALCULATE TAX                                                │
│  ════════════════════════                                               │
│  Based on FINAL allocations, not payment order                          │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Pre-Payment Eligibility Calculation

Before accepting ANY payment, calculate eligibility for each item:

```kotlin
fun calculatePaymentEligibility(transaction: Transaction): TransactionEligibility {
    val eligibility = TransactionEligibility()
    
    for (item in transaction.items) {
        if (item.isRemoved) continue
        
        val itemElig = ItemEligibility(
            itemId = item.id,
            subtotal = item.subTotal,
            taxAmount = item.taxTotal
        )
        
        // ════════════════════════════════════════════════════════════════
        // WIC ELIGIBILITY (Most Restrictive)
        // ════════════════════════════════════════════════════════════════
        if (item.isWicApproved && transaction.hasWicBenefit) {
            val wicResult = checkWicEligibility(item, transaction.wicBenefit)
            if (wicResult.eligible) {
                itemElig.wicEligibleAmount = calculateWicEligibleAmount(item, wicResult)
                itemElig.wicEligibleQty = wicResult.eligibleQuantity
                itemElig.wicCategory = wicResult.category
            }
        }
        
        // ════════════════════════════════════════════════════════════════
        // SNAP ELIGIBILITY
        // ════════════════════════════════════════════════════════════════
        if (item.isSNAPEligible) {
            // SNAP can pay for subtotal (not tax)
            // But SNAP payment makes that portion tax-exempt!
            itemElig.snapEligibleAmount = item.subTotal
            itemElig.isSnapTaxable = item.taxPercentSum > BigDecimal.ZERO
        }
        
        // ════════════════════════════════════════════════════════════════
        // REGULAR PAYMENT ELIGIBILITY
        // ════════════════════════════════════════════════════════════════
        itemElig.regularEligibleAmount = item.subTotal + item.taxTotal
        
        eligibility.items.add(itemElig)
    }
    
    // Calculate totals
    eligibility.totalWicEligible = eligibility.items.sumOf { it.wicEligibleAmount }
    eligibility.totalSnapEligible = eligibility.items.sumOf { it.snapEligibleAmount }
    eligibility.totalTaxableSnap = eligibility.items
        .filter { it.snapEligibleAmount > BigDecimal.ZERO && it.isSnapTaxable }
        .sumOf { it.snapEligibleAmount }
    eligibility.totalTax = eligibility.items.sumOf { it.taxAmount }
    eligibility.grandTotal = transaction.grandTotal
    
    return eligibility
}
```

---

## SNAP Payment Allocation (Tax Optimization)

### SNAP Allocation Priority

SNAP payments MUST be allocated to **taxable food items FIRST** to maximize tax savings:

```kotlin
fun allocateSnapPayment(transaction: Transaction, snapAmount: BigDecimal): BigDecimal {
    var remainingSnap = snapAmount
    
    // ════════════════════════════════════════════════════════════════
    // PRIORITY 1: TAXABLE SNAP-ELIGIBLE ITEMS
    // ════════════════════════════════════════════════════════════════
    // These items have tax that will be EXEMPTED when paid by SNAP
    
    val taxableSnapItems = transaction.items
        .filter { item ->
            !item.isRemoved &&
            item.isSNAPEligible &&
            item.taxPercentSum > BigDecimal.ZERO &&
            item.snapPaidPercent < BigDecimal(100)
        }
        .sortedByDescending { it.taxPercentSum }  // Highest tax rate first
    
    for (item in taxableSnapItems) {
        if (remainingSnap <= BigDecimal.ZERO) break
        
        val unpaidAmount = item.subTotal - item.snapPaidAmount
        if (unpaidAmount <= BigDecimal.ZERO) continue
        
        val applyAmount = minOf(remainingSnap, unpaidAmount)
        applySnapToItem(item, applyAmount)
        remainingSnap -= applyAmount
    }
    
    // ════════════════════════════════════════════════════════════════
    // PRIORITY 2: NON-TAXABLE SNAP-ELIGIBLE ITEMS
    // ════════════════════════════════════════════════════════════════
    if (remainingSnap > BigDecimal.ZERO) {
        val nonTaxableSnapItems = transaction.items
            .filter { item ->
                !item.isRemoved &&
                item.isSNAPEligible &&
                item.taxPercentSum == BigDecimal.ZERO &&
                item.snapPaidPercent < BigDecimal(100)
            }
        
        for (item in nonTaxableSnapItems) {
            if (remainingSnap <= BigDecimal.ZERO) break
            
            val unpaidAmount = item.subTotal - item.snapPaidAmount
            val applyAmount = minOf(remainingSnap, unpaidAmount)
            applySnapToItem(item, applyAmount)
            remainingSnap -= applyAmount
        }
    }
    
    return snapAmount - remainingSnap  // Actual amount applied
}

fun applySnapToItem(item: TransactionItemViewModel, amount: BigDecimal) {
    // Update SNAP tracking on item
    item.snapPaidAmount += amount
    
    // Calculate new SNAP percentage
    item.snapPaidPercent = (item.snapPaidAmount / item.subTotal) * BigDecimal(100)
    
    // Cap at 100%
    if (item.snapPaidPercent > BigDecimal(100)) {
        item.snapPaidPercent = BigDecimal(100)
        item.snapPaidAmount = item.subTotal
    }
    
    // Recalculate tax (SNAP portion is exempt)
    recalculateItemTax(item)
}
```

---

## Tax Recalculation After Benefit Payments

### The Critical Tax Update

After any WIC or SNAP payment, tax MUST be recalculated:

```kotlin
fun recalculateItemTax(item: TransactionItemViewModel) {
    // Original tax (if no benefits applied)
    val originalTax = item.taxPerUnit * item.quantityUsed
    
    // Calculate exempt portions
    val wicExemptPercent = item.wicPaidPercent / BigDecimal(100)
    val snapExemptPercent = item.snapPaidPercent / BigDecimal(100)
    
    // Total exempt percentage (cannot exceed 100%)
    val totalExemptPercent = minOf(BigDecimal.ONE, wicExemptPercent + snapExemptPercent)
    
    // Taxable percentage
    val taxablePercent = BigDecimal.ONE - totalExemptPercent
    
    // New tax amount
    item.taxTotal = (item.taxPerUnit * item.quantityUsed * taxablePercent)
        .setScale(2, RoundingMode.HALF_UP)
    
    // Update subject-to-tax total
    item.subjectToTaxTotal = item.subTotal * taxablePercent
    
    // Update tax breakdown proportionally
    for (tax in item.taxes) {
        tax.amount = (tax.amount * taxablePercent).setScale(2, RoundingMode.HALF_UP)
    }
}

fun recalculateTaxAfterPayment(transaction: Transaction) {
    // Recalculate tax for all items with benefit payments
    for (item in transaction.items) {
        if (item.isRemoved) continue
        
        if (item.snapPaidAmount > BigDecimal.ZERO || item.wicPaidAmount > BigDecimal.ZERO) {
            recalculateItemTax(item)
        }
    }
    
    // Sum new tax total
    transaction.taxTotal = transaction.items
        .filter { !it.isRemoved }
        .sumOf { it.taxTotal }
    
    // Recalculate grand total
    transaction.grandTotal = transaction.subTotal + transaction.taxTotal
}
```

---

## Regular Payment Allocation

### Non-Benefit Payment Flow

```kotlin
fun allocateRegularPayment(transaction: Transaction, amount: BigDecimal): BigDecimal {
    var remainingAmount = amount
    
    // ════════════════════════════════════════════════════════════════
    // PRIORITY 1: NON-SNAP-ELIGIBLE ITEMS (subtotal + tax)
    // ════════════════════════════════════════════════════════════════
    val nonSnapItems = transaction.items
        .filter { item ->
            !item.isRemoved &&
            !item.isSNAPEligible &&
            item.getUnpaidAmount() > BigDecimal.ZERO
        }
    
    for (item in nonSnapItems) {
        if (remainingAmount <= BigDecimal.ZERO) break
        
        val unpaid = item.getUnpaidAmount()
        val applyAmount = minOf(remainingAmount, unpaid)
        item.nonSNAPTotal += applyAmount
        item.paidTotal += applyAmount
        remainingAmount -= applyAmount
    }
    
    // ════════════════════════════════════════════════════════════════
    // PRIORITY 2: SNAP-ELIGIBLE ITEMS (remaining unpaid portions)
    // ════════════════════════════════════════════════════════════════
    if (remainingAmount > BigDecimal.ZERO) {
        val snapItems = transaction.items
            .filter { item ->
                !item.isRemoved &&
                item.isSNAPEligible &&
                item.getUnpaidAmount() > BigDecimal.ZERO
            }
        
        for (item in snapItems) {
            if (remainingAmount <= BigDecimal.ZERO) break
            
            val unpaid = item.getUnpaidAmount()
            val applyAmount = minOf(remainingAmount, unpaid)
            item.nonSNAPTotal += applyAmount
            item.paidTotal += applyAmount
            remainingAmount -= applyAmount
        }
    }
    
    return remainingAmount  // Change due if positive
}

fun TransactionItemViewModel.getUnpaidAmount(): BigDecimal {
    val totalDue = this.subTotal + this.taxTotal
    val totalPaid = this.snapPaidAmount + this.wicPaidAmount + this.nonSNAPTotal
    return maxOf(BigDecimal.ZERO, totalDue - totalPaid)
}
```

---

## Split Tender Example

```
═══════════════════════════════════════════════════════════════
              SPLIT TENDER TRANSACTION
═══════════════════════════════════════════════════════════════

Items:
  Milk (WIC eligible, SNAP eligible)    $4.29  Tax: $0.00
  Cheerios (WIC eligible, SNAP elig.)   $4.99  Tax: $0.00
  Chips (SNAP eligible, taxable)        $3.99  Tax: $0.38
  Soda (SNAP eligible, taxable)         $2.69  Tax: $0.26
  Paper Towels (not eligible)           $5.99  Tax: $0.57

BEFORE PAYMENTS:
  Subtotal:           $21.95
  Tax:                 $1.21
  Grand Total:        $23.16

PAYMENT 1: WIC ($9.28)
  WIC covers Milk ($4.29) and Cheerios ($4.99)
  Tax unchanged (items were already tax-exempt food)
  Remaining: $13.88

PAYMENT 2: SNAP ($6.68)
  SNAP allocated to TAXABLE items first:
    Chips: $3.99 (100% SNAP) → Tax $0.38 EXEMPTED
    Soda:  $2.69 (100% SNAP) → Tax $0.26 EXEMPTED
  New Tax: $0.57 (Paper Towels only)
  New Grand Total: $22.52
  Remaining: $6.56

PAYMENT 3: Credit Card ($6.56)
  Credit covers Paper Towels: $5.99 + $0.57 tax

FINAL:
  Total Paid: $22.52
  Tax Collected: $0.57
  Tax Saved by SNAP: $0.64
═══════════════════════════════════════════════════════════════
```

---

## Payment Receipt Display

```
═══════════════════════════════════════════════
              GROCERY STORE
═══════════════════════════════════════════════

Milk 1 Gal                      $4.29  WIC
Cheerios 18oz                   $4.99  WIC
Chips Family Size               $3.99  F
  Tax Exempt (SNAP)
Soda 2-Liter                    $2.69  F
  CRV                           $0.10
  Tax Exempt (SNAP)
Paper Towels                    $5.99

───────────────────────────────────────────────
SUBTOTAL                       $21.95
TAX                             $0.57
───────────────────────────────────────────────
TOTAL                          $22.52

PAYMENTS:
  WIC                           $9.28
  EBT SNAP                      $6.68
  Visa ****1234                 $6.56
───────────────────────────────────────────────
TOTAL PAID                     $22.52
CHANGE DUE                      $0.00

TAX SAVED (SNAP):               $0.64

F = SNAP Eligible

WIC REMAINING BENEFITS:
  Milk:     3 gal
  Cereal:   18 oz
  CVB:      $8.00
═══════════════════════════════════════════════
```

---

[← Back to Index](./INDEX.md) | [Previous: Deposits & Fees](./DEPOSITS_FEES.md) | [Next: Returns & Adjustments →](./RETURNS_ADJUSTMENTS.md)

