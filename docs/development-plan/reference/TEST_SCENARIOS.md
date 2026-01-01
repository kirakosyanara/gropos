# GrowPOS Test Scenarios

> Complete test scenarios for validating calculation accuracy and feature functionality

## Table of Contents

- [Overview](#overview)
- [Simple Transaction Tests](#simple-transaction-tests)
- [Tax Calculation Tests](#tax-calculation-tests)
- [Discount Tests](#discount-tests)
- [SNAP/EBT Tests](#snapebt-tests)
- [Mixed Payment Tests](#mixed-payment-tests)
- [Return Tests](#return-tests)
- [Promotion Tests](#promotion-tests)
- [Edge Case Tests](#edge-case-tests)
- [Hardware Integration Tests](#hardware-integration-tests)

---

## Overview

### Test Data Convention

All test scenarios use this format:

```
┌─────────────────────────────────────────────────────────────────┐
│  SCENARIO: [Name]                                               │
├─────────────────────────────────────────────────────────────────┤
│  INPUT:                                                         │
│    - Products: [list with prices, tax, SNAP eligibility]       │
│    - Payments: [payment methods and amounts]                   │
│                                                                 │
│  EXPECTED:                                                      │
│    - Subtotal: $X.XX                                           │
│    - Tax: $X.XX                                                │
│    - Total: $X.XX                                              │
│    - Change: $X.XX                                             │
│                                                                 │
│  CALCULATION BREAKDOWN:                                         │
│    [step-by-step calculation]                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Standard Test Products

| Product ID | Name | Price | Tax Rate | SNAP | CRV | Floor Price |
|------------|------|-------|----------|------|-----|-------------|
| P001 | Milk 1 Gallon | $4.99 | 0% | ✓ | $0.00 | $3.00 |
| P002 | Bread | $3.49 | 0% | ✓ | $0.00 | $2.00 |
| P003 | Coca-Cola 2L | $2.99 | 8.5% | ✗ | $0.10 | $1.50 |
| P004 | Chips | $4.49 | 8.5% | ✗ | $0.00 | $2.50 |
| P005 | Orange Juice 1L | $5.99 | 0% | ✓ | $0.10 | $4.00 |
| P006 | Cigarettes | $12.99 | 8.5% | ✗ | $0.00 | $12.00 |
| P007 | Deli Sandwich | $7.99 | 8.5% | ✗ | $0.00 | $5.00 |
| P008 | Bananas (per lb) | $0.69 | 0% | ✓ | $0.00 | $0.40 |
| P009 | Beer 6-Pack | $9.99 | 8.5% | ✗ | $0.30 | $8.00 |
| P010 | Candy Bar | $1.99 | 8.5% | ✗ | $0.00 | $1.00 |

---

## Simple Transaction Tests

### Test 1: Single Item Cash Sale

```
┌─────────────────────────────────────────────────────────────────┐
│  SCENARIO: Single taxable item paid with cash                  │
├─────────────────────────────────────────────────────────────────┤
│  INPUT:                                                         │
│    Products:                                                    │
│      - P003 Coca-Cola 2L × 1 @ $2.99, Tax 8.5%, CRV $0.10     │
│    Payment:                                                     │
│      - Cash: $5.00                                             │
│                                                                 │
│  EXPECTED:                                                      │
│    Subtotal:    $2.99                                          │
│    CRV:         $0.10                                          │
│    Tax:         $0.26  (8.5% of $2.99 + $0.10 = $0.2627)       │
│    Grand Total: $3.35                                          │
│    Change Due:  $1.65                                          │
│                                                                 │
│  CALCULATION:                                                   │
│    Taxable Amount = Price + CRV = $2.99 + $0.10 = $3.09        │
│    Tax = $3.09 × 0.085 = $0.26 (HALF_UP)                       │
│    Total = $2.99 + $0.10 + $0.26 = $3.35                       │
│    Change = $5.00 - $3.35 = $1.65                              │
└─────────────────────────────────────────────────────────────────┘
```

### Test 2: Multiple Items No Tax

```
┌─────────────────────────────────────────────────────────────────┐
│  SCENARIO: Multiple non-taxable items                          │
├─────────────────────────────────────────────────────────────────┤
│  INPUT:                                                         │
│    Products:                                                    │
│      - P001 Milk × 1 @ $4.99, Tax 0%                           │
│      - P002 Bread × 2 @ $3.49, Tax 0%                          │
│    Payment:                                                     │
│      - Cash: $20.00                                            │
│                                                                 │
│  EXPECTED:                                                      │
│    Subtotal:    $11.97                                         │
│    Tax:         $0.00                                          │
│    Grand Total: $11.97                                         │
│    Change Due:  $8.03                                          │
│                                                                 │
│  CALCULATION:                                                   │
│    Milk: $4.99 × 1 = $4.99                                     │
│    Bread: $3.49 × 2 = $6.98                                    │
│    Subtotal = $4.99 + $6.98 = $11.97                           │
│    Tax = $0.00 (no taxable items)                              │
│    Change = $20.00 - $11.97 = $8.03                            │
└─────────────────────────────────────────────────────────────────┘
```

### Test 3: Mixed Tax Items

```
┌─────────────────────────────────────────────────────────────────┐
│  SCENARIO: Mix of taxable and non-taxable items                │
├─────────────────────────────────────────────────────────────────┤
│  INPUT:                                                         │
│    Products:                                                    │
│      - P001 Milk × 1 @ $4.99, Tax 0%, SNAP ✓                   │
│      - P004 Chips × 1 @ $4.49, Tax 8.5%, SNAP ✗                │
│      - P010 Candy × 2 @ $1.99, Tax 8.5%, SNAP ✗                │
│    Payment:                                                     │
│      - Credit Card: $13.66                                     │
│                                                                 │
│  EXPECTED:                                                      │
│    Subtotal:    $13.46                                         │
│    Tax:         $0.72                                          │
│    Grand Total: $14.18                                         │
│                                                                 │
│  CALCULATION:                                                   │
│    Milk: $4.99 (no tax)                                        │
│    Chips: $4.49 (taxable)                                      │
│    Candy: $1.99 × 2 = $3.98 (taxable)                          │
│    Subtotal = $4.99 + $4.49 + $3.98 = $13.46                   │
│    Taxable = $4.49 + $3.98 = $8.47                             │
│    Tax = $8.47 × 0.085 = $0.72 (HALF_UP)                       │
│    Total = $13.46 + $0.72 = $14.18                             │
└─────────────────────────────────────────────────────────────────┘
```

---

## Tax Calculation Tests

### Test 4: Multiple Tax Rates

```
┌─────────────────────────────────────────────────────────────────┐
│  SCENARIO: Product with multiple taxes                         │
├─────────────────────────────────────────────────────────────────┤
│  INPUT:                                                         │
│    Products:                                                    │
│      - P006 Cigarettes × 1 @ $12.99                            │
│        Taxes: State 8.5% + Local 1.0% = 9.5% total             │
│    Payment:                                                     │
│      - Cash: $15.00                                            │
│                                                                 │
│  EXPECTED:                                                      │
│    Subtotal:    $12.99                                         │
│    State Tax:   $1.10                                          │
│    Local Tax:   $0.13                                          │
│    Tax Total:   $1.23                                          │
│    Grand Total: $14.22                                         │
│    Change Due:  $0.78                                          │
│                                                                 │
│  CALCULATION:                                                   │
│    State Tax = $12.99 × 0.085 = $1.10                          │
│    Local Tax = $12.99 × 0.010 = $0.13                          │
│    Total Tax = $1.10 + $0.13 = $1.23                           │
│    OR: $12.99 × 0.095 = $1.23 (combined rate)                  │
└─────────────────────────────────────────────────────────────────┘
```

### Test 5: CRV Affects Tax Base

```
┌─────────────────────────────────────────────────────────────────┐
│  SCENARIO: Tax calculated on price + CRV                       │
├─────────────────────────────────────────────────────────────────┤
│  INPUT:                                                         │
│    Products:                                                    │
│      - P009 Beer 6-Pack × 1 @ $9.99, Tax 8.5%, CRV $0.30      │
│    Payment:                                                     │
│      - Cash: $12.00                                            │
│                                                                 │
│  EXPECTED:                                                      │
│    Subtotal:    $9.99                                          │
│    CRV:         $0.30                                          │
│    Tax:         $0.87                                          │
│    Grand Total: $11.16                                         │
│    Change Due:  $0.84                                          │
│                                                                 │
│  CALCULATION:                                                   │
│    Tax Base = Price + CRV = $9.99 + $0.30 = $10.29             │
│    Tax = $10.29 × 0.085 = $0.87 (HALF_UP)                      │
│    Total = $9.99 + $0.30 + $0.87 = $11.16                      │
└─────────────────────────────────────────────────────────────────┘
```

---

## Discount Tests

### Test 6: Percentage Line Discount

```
┌─────────────────────────────────────────────────────────────────┐
│  SCENARIO: 10% discount on single item                         │
├─────────────────────────────────────────────────────────────────┤
│  INPUT:                                                         │
│    Products:                                                    │
│      - P007 Deli Sandwich × 1 @ $7.99, Tax 8.5%                │
│        Discount: 10% line discount                             │
│    Payment:                                                     │
│      - Cash: $10.00                                            │
│                                                                 │
│  EXPECTED:                                                      │
│    Original:    $7.99                                          │
│    Discount:    -$0.80                                         │
│    Subtotal:    $7.19                                          │
│    Tax:         $0.61                                          │
│    Grand Total: $7.80                                          │
│    Savings:     $0.80                                          │
│    Change Due:  $2.20                                          │
│                                                                 │
│  CALCULATION:                                                   │
│    Discount = $7.99 × 0.10 = $0.80                             │
│    Price After Discount = $7.99 - $0.80 = $7.19                │
│    Tax = $7.19 × 0.085 = $0.61 (HALF_UP)                       │
│    Total = $7.19 + $0.61 = $7.80                               │
└─────────────────────────────────────────────────────────────────┘
```

### Test 7: Dollar Amount Discount

```
┌─────────────────────────────────────────────────────────────────┐
│  SCENARIO: $2.00 off item                                      │
├─────────────────────────────────────────────────────────────────┤
│  INPUT:                                                         │
│    Products:                                                    │
│      - P007 Deli Sandwich × 1 @ $7.99, Tax 8.5%                │
│        Discount: $2.00 off                                     │
│    Payment:                                                     │
│      - Credit: $6.49                                           │
│                                                                 │
│  EXPECTED:                                                      │
│    Original:    $7.99                                          │
│    Discount:    -$2.00                                         │
│    Subtotal:    $5.99                                          │
│    Tax:         $0.51                                          │
│    Grand Total: $6.50                                          │
│    Savings:     $2.00                                          │
│                                                                 │
│  CALCULATION:                                                   │
│    Price After Discount = $7.99 - $2.00 = $5.99                │
│    Tax = $5.99 × 0.085 = $0.51 (HALF_UP)                       │
│    Total = $5.99 + $0.51 = $6.50                               │
└─────────────────────────────────────────────────────────────────┘
```

### Test 8: Floor Price Enforcement

```
┌─────────────────────────────────────────────────────────────────┐
│  SCENARIO: Discount cannot go below floor price                │
├─────────────────────────────────────────────────────────────────┤
│  INPUT:                                                         │
│    Products:                                                    │
│      - P007 Deli Sandwich × 1 @ $7.99, Tax 8.5%                │
│        Floor Price: $5.00                                      │
│        Attempted Discount: $4.00 off                           │
│                                                                 │
│  EXPECTED (WITHOUT OVERRIDE):                                   │
│    Original:    $7.99                                          │
│    Discount:    -$2.99 (capped to floor)                       │
│    Subtotal:    $5.00 (floor price)                            │
│    Tax:         $0.43                                          │
│    Grand Total: $5.43                                          │
│                                                                 │
│  EXPECTED (WITH MANAGER OVERRIDE):                              │
│    Original:    $7.99                                          │
│    Discount:    -$4.00                                         │
│    Subtotal:    $3.99                                          │
│    Tax:         $0.34                                          │
│    Grand Total: $4.33                                          │
│                                                                 │
│  CALCULATION:                                                   │
│    Without Override:                                           │
│      Discount = min($4.00, $7.99 - $5.00) = $2.99              │
│    With Override:                                              │
│      Discount = $4.00 (floor bypassed)                         │
└─────────────────────────────────────────────────────────────────┘
```

### Test 9: Transaction (Invoice) Discount

```
┌─────────────────────────────────────────────────────────────────┐
│  SCENARIO: 5% discount on entire transaction                   │
├─────────────────────────────────────────────────────────────────┤
│  INPUT:                                                         │
│    Products:                                                    │
│      - P001 Milk × 1 @ $4.99, Tax 0%                           │
│      - P004 Chips × 1 @ $4.49, Tax 8.5%                        │
│      Transaction Discount: 5%                                  │
│    Payment:                                                     │
│      - Cash: $10.00                                            │
│                                                                 │
│  EXPECTED:                                                      │
│    Original Subtotal: $9.48                                    │
│    Transaction Discount: -$0.47                                │
│    Subtotal After: $9.01                                       │
│    Tax: $0.36                                                  │
│    Grand Total: $9.37                                          │
│    Change Due: $0.63                                           │
│                                                                 │
│  CALCULATION:                                                   │
│    Original = $4.99 + $4.49 = $9.48                            │
│    Trans Discount = $9.48 × 0.05 = $0.47                       │
│    │                                                           │
│    ├─ Milk share: $4.99/$9.48 × $0.47 = $0.25                 │
│    └─ Chips share: $4.49/$9.48 × $0.47 = $0.22                │
│    │                                                           │
│    Milk after: $4.99 - $0.25 = $4.74 (non-taxable)            │
│    Chips after: $4.49 - $0.22 = $4.27 (taxable)               │
│    Tax = $4.27 × 0.085 = $0.36                                 │
│    Total = $4.74 + $4.27 + $0.36 = $9.37                       │
└─────────────────────────────────────────────────────────────────┘
```

---

## SNAP/EBT Tests

### Test 10: Full SNAP Payment

```
┌─────────────────────────────────────────────────────────────────┐
│  SCENARIO: All SNAP-eligible paid with EBT                     │
├─────────────────────────────────────────────────────────────────┤
│  INPUT:                                                         │
│    Products:                                                    │
│      - P001 Milk × 1 @ $4.99, Tax 0%, SNAP ✓                   │
│      - P002 Bread × 1 @ $3.49, Tax 0%, SNAP ✓                  │
│    Payment:                                                     │
│      - EBT SNAP: $8.48                                         │
│                                                                 │
│  EXPECTED:                                                      │
│    SNAP Eligible: $8.48                                        │
│    Subtotal:      $8.48                                        │
│    Tax:           $0.00 (SNAP items not taxed)                 │
│    Grand Total:   $8.48                                        │
│    EBT Applied:   $8.48                                        │
│    Remaining:     $0.00                                        │
│                                                                 │
│  NOTE: When paid 100% with SNAP, no tax applies                │
└─────────────────────────────────────────────────────────────────┘
```

### Test 11: Partial SNAP Payment (Split Tender)

```
┌─────────────────────────────────────────────────────────────────┐
│  SCENARIO: SNAP and non-SNAP items, split payment              │
├─────────────────────────────────────────────────────────────────┤
│  INPUT:                                                         │
│    Products:                                                    │
│      - P001 Milk × 1 @ $4.99, Tax 0%, SNAP ✓                   │
│      - P004 Chips × 1 @ $4.49, Tax 8.5%, SNAP ✗                │
│    Payment:                                                     │
│      - EBT SNAP: $4.99 (covers SNAP items)                     │
│      - Cash: $5.00                                             │
│                                                                 │
│  EXPECTED:                                                      │
│    SNAP Eligible:  $4.99                                       │
│    Non-SNAP:       $4.49                                       │
│    Subtotal:       $9.48                                       │
│    Tax:            $0.38 (on non-SNAP only)                    │
│    Grand Total:    $9.86                                       │
│    EBT Applied:    $4.99                                       │
│    Cash Applied:   $4.87                                       │
│    Change Due:     $0.13                                       │
│                                                                 │
│  CALCULATION:                                                   │
│    SNAP items: $4.99 (no tax when paid with EBT)               │
│    Non-SNAP: $4.49 + ($4.49 × 0.085) = $4.87                   │
│    Total = $4.99 + $4.87 = $9.86                               │
│    Cash remaining = $9.86 - $4.99 = $4.87                      │
│    Change = $5.00 - $4.87 = $0.13                              │
└─────────────────────────────────────────────────────────────────┘
```

### Test 12: SNAP Partial Payment (Less Than Eligible)

```
┌─────────────────────────────────────────────────────────────────┐
│  SCENARIO: SNAP balance less than SNAP-eligible amount         │
├─────────────────────────────────────────────────────────────────┤
│  INPUT:                                                         │
│    Products:                                                    │
│      - P005 Orange Juice × 2 @ $5.99, Tax 0%, SNAP ✓, CRV $0.10│
│    Payment:                                                     │
│      - EBT SNAP: $8.00 (partial - customer's remaining balance)│
│      - Cash: $10.00                                            │
│                                                                 │
│  EXPECTED:                                                      │
│    SNAP Eligible: $11.98                                       │
│    Subtotal:      $11.98                                       │
│    CRV:           $0.20                                        │
│    SNAP Paid:     $8.00 (66.78% of eligible)                   │
│    Non-SNAP Paid: $3.98 (33.22% of eligible)                   │
│    Tax:           $0.00 (OJ is non-taxable)                    │
│    Grand Total:   $12.18                                       │
│    EBT Applied:   $8.00                                        │
│    Cash Applied:  $4.18                                        │
│    Change Due:    $5.82                                        │
│                                                                 │
│  NOTE: Even though partially paid with non-EBT,                │
│        OJ is food and not taxable regardless of payment        │
└─────────────────────────────────────────────────────────────────┘
```

### Test 13: SNAP with Taxable Items (Tax Reduction)

```
┌─────────────────────────────────────────────────────────────────┐
│  SCENARIO: SNAP partially covers taxable SNAP-eligible food    │
├─────────────────────────────────────────────────────────────────┤
│  NOTE: This tests the edge case where a food item is both      │
│        SNAP-eligible AND subject to local food tax             │
├─────────────────────────────────────────────────────────────────┤
│  INPUT:                                                         │
│    Products:                                                    │
│      - Prepared Food @ $10.00, Tax 2%, SNAP ✓                  │
│    Payment:                                                     │
│      - EBT SNAP: $6.00 (60% of item)                           │
│      - Cash: $5.00                                             │
│                                                                 │
│  EXPECTED:                                                      │
│    Subtotal:      $10.00                                       │
│    SNAP Paid %:   60%                                          │
│    Taxable %:     40%                                          │
│    Taxable Amount:$4.00                                        │
│    Tax:           $0.08 (2% of $4.00)                          │
│    Grand Total:   $10.08                                       │
│    EBT Applied:   $6.00                                        │
│    Cash Applied:  $4.08                                        │
│    Change Due:    $0.92                                        │
│                                                                 │
│  CALCULATION:                                                   │
│    SNAP Paid Fraction = $6.00 / $10.00 = 0.60                  │
│    Taxable Quantity = 1.0 × (1 - 0.60) = 0.40                  │
│    Subject to Tax = $10.00 × 0.40 = $4.00                      │
│    Tax = $4.00 × 0.02 = $0.08                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Mixed Payment Tests

### Test 14: Three-Way Split Payment

```
┌─────────────────────────────────────────────────────────────────┐
│  SCENARIO: EBT + Credit + Cash                                 │
├─────────────────────────────────────────────────────────────────┤
│  INPUT:                                                         │
│    Products:                                                    │
│      - P001 Milk × 2 @ $4.99, SNAP ✓                           │
│      - P003 Coca-Cola × 1 @ $2.99, Tax 8.5%, CRV $0.10         │
│    Payment:                                                     │
│      1. EBT SNAP: $9.98 (all milk)                             │
│      2. Credit: $2.00                                          │
│      3. Cash: $1.45                                            │
│                                                                 │
│  EXPECTED:                                                      │
│    Subtotal:      $12.97                                       │
│    CRV:           $0.10                                        │
│    Tax:           $0.26                                        │
│    Grand Total:   $13.33                                       │
│    │                                                           │
│    Payment Applied:                                            │
│    ├─ EBT SNAP:   $9.98                                        │
│    ├─ Credit:     $2.00                                        │
│    └─ Cash:       $1.35                                        │
│    Change Due:    $0.10                                        │
│                                                                 │
│  CALCULATION:                                                   │
│    SNAP portion: $9.98 (milk, no tax)                          │
│    Non-SNAP: $2.99 + $0.10 + $0.26 = $3.35                     │
│    Remaining after EBT: $13.33 - $9.98 = $3.35                 │
│    Remaining after Credit: $3.35 - $2.00 = $1.35               │
│    Change = $1.45 - $1.35 = $0.10                              │
└─────────────────────────────────────────────────────────────────┘
```

---

## Return Tests

### Test 15: Full Return with Original Receipt

```
┌─────────────────────────────────────────────────────────────────┐
│  SCENARIO: Return entire transaction                           │
├─────────────────────────────────────────────────────────────────┤
│  ORIGINAL TRANSACTION:                                          │
│    - P001 Milk × 1 @ $4.99                                     │
│    - P004 Chips × 1 @ $4.49, Tax $0.38                         │
│    - Total: $9.86                                              │
│    - Paid: Credit $9.86                                        │
│                                                                 │
│  RETURN INPUT:                                                  │
│    - Return all items                                          │
│    - Refund to original payment (Credit)                       │
│                                                                 │
│  EXPECTED:                                                      │
│    Subtotal:    -$9.48                                         │
│    Tax:         -$0.38                                         │
│    Refund:      $9.86                                          │
│    Method:      Credit (to original card)                      │
│                                                                 │
│  NOTE: Tax is refunded proportionally                          │
└─────────────────────────────────────────────────────────────────┘
```

### Test 16: Partial Return

```
┌─────────────────────────────────────────────────────────────────┐
│  SCENARIO: Return one item from multi-item transaction         │
├─────────────────────────────────────────────────────────────────┤
│  ORIGINAL TRANSACTION:                                          │
│    - P001 Milk × 2 @ $4.99 = $9.98                             │
│    - P004 Chips × 1 @ $4.49, Tax $0.38 = $4.87                 │
│    - Total: $14.85                                             │
│    - Paid: Cash $20.00, Change $5.15                           │
│                                                                 │
│  RETURN INPUT:                                                  │
│    - Return: P001 Milk × 1                                     │
│    - Refund: Cash                                              │
│                                                                 │
│  EXPECTED:                                                      │
│    Returned Item: Milk $4.99                                   │
│    Tax Refund:    $0.00 (milk was non-taxable)                 │
│    Cash Refund:   $4.99                                        │
│                                                                 │
│  DRAWER ACTION: Open, dispense $4.99                           │
└─────────────────────────────────────────────────────────────────┘
```

---

## Promotion Tests

### Test 17: Buy 2 Get 1 Free (BOGO)

```
┌─────────────────────────────────────────────────────────────────┐
│  SCENARIO: Buy 2 Get 1 Free promotion                          │
├─────────────────────────────────────────────────────────────────┤
│  INPUT:                                                         │
│    Products:                                                    │
│      - P010 Candy Bar × 3 @ $1.99, Tax 8.5%                    │
│    Promotion:                                                   │
│      - Buy 2 Get 1 Free on Candy Bars                          │
│    Payment:                                                     │
│      - Cash: $5.00                                             │
│                                                                 │
│  EXPECTED:                                                      │
│    Original Total: $5.97 (3 × $1.99)                           │
│    Promo Discount: -$1.99 (1 free)                             │
│    Subtotal:       $3.98                                       │
│    Tax:            $0.34                                       │
│    Grand Total:    $4.32                                       │
│    Savings:        $1.99                                       │
│    Change Due:     $0.68                                       │
│                                                                 │
│  CALCULATION:                                                   │
│    2 paid @ $1.99 = $3.98                                      │
│    1 free = -$1.99                                             │
│    Tax = $3.98 × 0.085 = $0.34                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Test 18: Mix and Match

```
┌─────────────────────────────────────────────────────────────────┐
│  SCENARIO: Any 3 for $5.00 promotion                           │
├─────────────────────────────────────────────────────────────────┤
│  INPUT:                                                         │
│    Products (Mix & Match eligible):                            │
│      - P010 Candy Bar × 1 @ $1.99                              │
│      - Another Candy × 1 @ $2.29                               │
│      - Third Candy × 1 @ $1.79                                 │
│    Promotion:                                                   │
│      - Any 3 eligible items for $5.00                          │
│    Payment:                                                     │
│      - Cash: $6.00                                             │
│                                                                 │
│  EXPECTED:                                                      │
│    Original Total: $6.07                                       │
│    Promo Price:    $5.00                                       │
│    Discount:       -$1.07                                      │
│    Tax:            $0.43                                       │
│    Grand Total:    $5.43                                       │
│    Savings:        $1.07                                       │
│    Change Due:     $0.57                                       │
└─────────────────────────────────────────────────────────────────┘
```

---

## Edge Case Tests

### Test 19: Weighted Item

```
┌─────────────────────────────────────────────────────────────────┐
│  SCENARIO: Product sold by weight                              │
├─────────────────────────────────────────────────────────────────┤
│  INPUT:                                                         │
│    Products:                                                    │
│      - P008 Bananas @ $0.69/lb, 2.35 lbs                       │
│    Payment:                                                     │
│      - Cash: $2.00                                             │
│                                                                 │
│  EXPECTED:                                                      │
│    Weight:      2.350 lbs                                      │
│    Price/lb:    $0.69                                          │
│    Subtotal:    $1.62                                          │
│    Tax:         $0.00 (produce)                                │
│    Grand Total: $1.62                                          │
│    Change Due:  $0.38                                          │
│                                                                 │
│  CALCULATION:                                                   │
│    Total = $0.69 × 2.35 = $1.6215 → $1.62 (HALF_UP)           │
└─────────────────────────────────────────────────────────────────┘
```

### Test 20: Embedded Price Barcode

```
┌─────────────────────────────────────────────────────────────────┐
│  SCENARIO: Price embedded in barcode                           │
├─────────────────────────────────────────────────────────────────┤
│  INPUT:                                                         │
│    Barcode: 212345004997                                       │
│    Parsed:                                                      │
│      - Item Number: 12345                                      │
│      - Embedded Price: $4.99                                   │
│    Product lookup returns: Deli Meat, Tax 0%, SNAP ✓           │
│    Payment:                                                     │
│      - Cash: $5.00                                             │
│                                                                 │
│  EXPECTED:                                                      │
│    Price (from barcode): $4.99                                 │
│    Tax:                  $0.00                                 │
│    Grand Total:          $4.99                                 │
│    Change Due:           $0.01                                 │
│                                                                 │
│  NOTE: Price comes from barcode, not product database          │
└─────────────────────────────────────────────────────────────────┘
```

### Test 21: Zero Quantity Edge Case

```
┌─────────────────────────────────────────────────────────────────┐
│  SCENARIO: Attempt to add zero quantity                        │
├─────────────────────────────────────────────────────────────────┤
│  INPUT:                                                         │
│    - Scan P001 Milk                                            │
│    - Enter quantity: 0                                         │
│                                                                 │
│  EXPECTED:                                                      │
│    - Error: "Invalid quantity"                                 │
│    - Item NOT added to cart                                    │
│    - Cart remains unchanged                                    │
│                                                                 │
│  VALIDATION RULE: Quantity must be > 0                         │
└─────────────────────────────────────────────────────────────────┘
```

### Test 22: Very Large Transaction

```
┌─────────────────────────────────────────────────────────────────┐
│  SCENARIO: 50+ items in single transaction                     │
├─────────────────────────────────────────────────────────────────┤
│  INPUT:                                                         │
│    Products:                                                    │
│      - 25 × P001 Milk @ $4.99 = $124.75                        │
│      - 25 × P004 Chips @ $4.49 = $112.25 + tax                 │
│    Payment:                                                     │
│      - Credit: $246.54                                         │
│                                                                 │
│  EXPECTED:                                                      │
│    Item Count:  50                                             │
│    Subtotal:    $237.00                                        │
│    Tax:         $9.54 ($112.25 × 0.085)                        │
│    Grand Total: $246.54                                        │
│                                                                 │
│  PERFORMANCE: Should complete < 2 seconds                      │
└─────────────────────────────────────────────────────────────────┘
```

---

## Hardware Integration Tests

### Test 23: Drawer Open Blocking

```
┌─────────────────────────────────────────────────────────────────┐
│  SCENARIO: Cash drawer open blocks scanning                    │
├─────────────────────────────────────────────────────────────────┤
│  SEQUENCE:                                                      │
│    1. Complete cash transaction                                │
│    2. Drawer opens for change                                  │
│    3. Attempt to scan new item                                 │
│                                                                 │
│  EXPECTED:                                                      │
│    - Scan is BLOCKED                                           │
│    - Dialog: "Close drawer to continue"                        │
│    - UI: Drawer status indicator shows OPEN                    │
│    - After drawer closes:                                      │
│      - Dialog dismisses                                        │
│      - Scanning resumes                                        │
└─────────────────────────────────────────────────────────────────┘
```

### Test 24: Scale Weight Under Minimum

```
┌─────────────────────────────────────────────────────────────────┐
│  SCENARIO: Weight below minimum threshold                      │
├─────────────────────────────────────────────────────────────────┤
│  SEQUENCE:                                                      │
│    1. Scan weighted item (sold by weight)                      │
│    2. Scale returns 0.005 lbs                                  │
│                                                                 │
│  EXPECTED:                                                      │
│    - Error: "Weight Under Zero"                                │
│    - Item NOT added to cart                                    │
│    - Prompt: "Place product on scale"                          │
│                                                                 │
│  THRESHOLD: Minimum weight = 0.01 lbs                          │
└─────────────────────────────────────────────────────────────────┘
```

### Test 25: Payment Terminal Timeout

```
┌─────────────────────────────────────────────────────────────────┐
│  SCENARIO: Card payment times out                              │
├─────────────────────────────────────────────────────────────────┤
│  SEQUENCE:                                                      │
│    1. Select Credit payment                                    │
│    2. Terminal prompts "Insert/Swipe Card"                     │
│    3. Customer does not insert card for 60 seconds             │
│                                                                 │
│  EXPECTED:                                                      │
│    - Payment cancelled automatically                           │
│    - Error: "Payment timeout - try again"                      │
│    - Return to payment selection screen                        │
│    - No payment recorded                                       │
│    - Terminal returns to ready state                           │
└─────────────────────────────────────────────────────────────────┘
```

---

## Automated Test Template

### Kotlin Test Example

```kotlin
@Test
fun `single taxable item with CRV calculates correctly`() {
    // Arrange
    val product = ProductViewModel(
        branchProductId = 3,
        productName = "Coca-Cola 2L",
        retailPrice = BigDecimal("2.99"),
        taxes = listOf(
            ProductTaxViewModel(taxId = 1, tax = "Sales Tax", percent = BigDecimal("8.5"))
        ),
        crvRatePerUnit = BigDecimal("0.10")
    )
    
    val orderStore = OrderStore()
    orderStore.addProduct(product, quantity = 1)
    
    // Act
    val calculator = TransactionCalculator()
    val result = calculator.calculate(orderStore.items)
    
    // Assert
    assertEquals(BigDecimal("2.99"), result.subtotal)
    assertEquals(BigDecimal("0.10"), result.crvTotal)
    assertEquals(BigDecimal("0.26"), result.taxTotal)
    assertEquals(BigDecimal("3.35"), result.grandTotal)
}

@Test
fun `SNAP payment reduces tax on partially covered items`() {
    // Arrange
    val product = ProductViewModel(
        branchProductId = 100,
        productName = "Prepared Food",
        retailPrice = BigDecimal("10.00"),
        isFoodStampEligible = true,
        taxes = listOf(
            ProductTaxViewModel(taxId = 2, tax = "Food Tax", percent = BigDecimal("2.0"))
        )
    )
    
    val orderStore = OrderStore()
    orderStore.addProduct(product, quantity = 1)
    
    val payment = TransactionPaymentViewModel(
        paymentMethodId = PaymentMethod.EBT_SNAP.id,
        value = BigDecimal("6.00")
    )
    
    // Act
    val calculator = TransactionCalculator()
    calculator.applyPayment(payment)
    val result = calculator.calculate(orderStore.items)
    
    // Assert
    assertEquals(BigDecimal("6.00"), result.snapPaidAmount)
    assertEquals(BigDecimal("0.60"), result.snapPaidPercent)
    assertEquals(BigDecimal("4.00"), result.taxableAmount)
    assertEquals(BigDecimal("0.08"), result.taxTotal)
    assertEquals(BigDecimal("10.08"), result.grandTotal)
}
```

---

## Summary

| Category | Test Count | Coverage |
|----------|------------|----------|
| Simple Transactions | 3 | Basic flow |
| Tax Calculations | 2 | Multi-tax, CRV |
| Discounts | 4 | Line, transaction, floor |
| SNAP/EBT | 4 | Full, partial, split |
| Mixed Payments | 1 | Three-way split |
| Returns | 2 | Full, partial |
| Promotions | 2 | BOGO, Mix & Match |
| Edge Cases | 4 | Weight, barcode, limits |
| Hardware | 3 | Drawer, scale, terminal |

**Total: 25 test scenarios**

