# Comprehensive Calculation Examples

[← Back to Index](./INDEX.md) | [Previous: Calculation Engine](./CALCULATION_ENGINE.md)

---

## Overview

This document provides complete, worked examples covering common and complex transaction scenarios in GroPOS.

---

## Example 1: Basic Transaction

### Scenario
Simple grocery purchase with taxable and non-taxable items.

### Items
| Product | Qty | Retail | SNAP Eligible | Tax Rate |
|---------|-----|--------|---------------|----------|
| Bread | 1 | $3.49 | Yes | 0% (food exempt) |
| Milk 1 Gal | 1 | $4.29 | Yes | 0% (food exempt) |
| Soda 2L | 2 | $2.49 | Yes | 9.5% |
| Paper Towels | 1 | $5.99 | No | 9.5% |

### CRV
- Soda 2L: $0.10 each

### Calculations

```
BREAD (1 × $3.49)
  Price Used:     $3.49
  CRV:            $0.00
  Final Price:    $3.49
  Tax (0%):       $0.00
  Subtotal:       $3.49
  Tax Total:      $0.00
  Line Total:     $3.49

MILK (1 × $4.29)
  Price Used:     $4.29
  CRV:            $0.00
  Final Price:    $4.29
  Tax (0%):       $0.00
  Subtotal:       $4.29
  Tax Total:      $0.00
  Line Total:     $4.29

SODA 2L (2 × $2.49)
  Price Used:     $2.49
  CRV:            $0.10
  Final Price:    $2.59
  Tax (9.5%):     $0.25 ($2.59 × 0.095)
  Subtotal:       $5.18 ($2.59 × 2)
  Tax Total:      $0.49 ($0.25 × 2)
  Line Total:     $5.67

PAPER TOWELS (1 × $5.99)
  Price Used:     $5.99
  CRV:            $0.00
  Final Price:    $5.99
  Tax (9.5%):     $0.57 ($5.99 × 0.095)
  Subtotal:       $5.99
  Tax Total:      $0.57
  Line Total:     $6.56

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
TRANSACTION SUMMARY
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Subtotal:        $18.95
CRV Total:       $0.20
Tax Total:       $1.06
Grand Total:     $20.01

SNAP Eligible:    $12.96 (Bread + Milk + Soda subtotals)
```

---

## Example 2: Mix & Match Promotion

### Scenario
"Pick Any 5 Yogurts for $5" promotion

### Items
| Product | Retail | Quantity |
|---------|--------|----------|
| Greek Yogurt | $1.49 | 2 |
| Strawberry Yogurt | $1.29 | 2 |
| Vanilla Yogurt | $0.99 | 3 |

### Calculations

```
Total Quantity: 7 yogurts
Promotion: 5 for $5.00

STEP 1: Determine eligible sets
  - 7 yogurts available
  - 5 required for promotion
  - 1 complete set
  - 2 leftover at regular price

STEP 2: Select items for promotion (cheapest first)
  Promotion items:
    - 3 × Vanilla Yogurt @ $0.99 = $2.97
    - 2 × Strawberry Yogurt @ $1.29 = $2.58
    Total regular: $5.55
    Promotion price: $5.00
    Discount: $0.55

STEP 3: Allocate discount proportionally
  Vanilla (3 × $0.99 = $2.97):
    Proportion: $2.97 / $5.55 = 53.5%
    Discount: $0.55 × 0.535 = $0.29
    New price: $2.97 - $0.29 = $2.68
    Per unit: $0.89
    
  Strawberry (2 × $1.29 = $2.58):
    Proportion: $2.58 / $5.55 = 46.5%
    Discount: $0.55 × 0.465 = $0.26
    New price: $2.58 - $0.26 = $2.32
    Per unit: $1.16

STEP 4: Leftover items at regular price
  - 2 × Greek Yogurt @ $1.49 = $2.98

STEP 5: Calculate tax (all yogurt 0% - food)
  Tax: $0.00

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
LINE ITEMS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Greek Yogurt (2)        $2.98
Strawberry Yogurt (2)   $2.32
  Mix & Match 5/$5     -$0.26
Vanilla Yogurt (3)      $2.68
  Mix & Match 5/$5     -$0.29

Subtotal:               $7.98
Savings:                $0.55
Tax:                    $0.00
Grand Total:            $7.98
```

---

## Example 3: SNAP + Card Split Payment

### Scenario
Mixed transaction paid with EBT SNAP and credit card

### Items
| Product | Retail | CRV | SNAP Eligible | Tax Rate |
|---------|--------|-----|---------------|----------|
| Bread | $3.49 | $0.00 | Yes | 0% |
| Chips | $4.29 | $0.00 | Yes | 9.5% |
| Soda 6-pack | $5.99 | $0.30 | Yes | 9.5% |
| Laundry Detergent | $8.99 | $0.00 | No | 9.5% |

### Calculations

```
STEP 1: Calculate item totals

BREAD
  Final Price:    $3.49
  Tax:            $0.00
  Line Total:     $3.49
  SNAP Eligible:   Yes

CHIPS  
  Final Price:    $4.29
  Tax:            $0.41 ($4.29 × 9.5%)
  Line Total:     $4.70
  SNAP Eligible:   Yes (subtotal only, not tax)

SODA 6-PACK
  Price + CRV:    $5.99 + $0.30 = $6.29
  Tax:            $0.60 ($6.29 × 9.5%)
  Line Total:     $6.89
  SNAP Eligible:   Yes

LAUNDRY DETERGENT
  Final Price:    $8.99
  Tax:            $0.85
  Line Total:     $9.84
  SNAP Eligible:   No

BEFORE PAYMENT:
  Subtotal:       $23.06
  Tax Total:      $1.86
  Grand Total:    $24.92
  SNAP Eligible:   $14.07 (Bread + Chips + Soda subtotals)

STEP 2: Apply SNAP Payment ($14.00)

Payment Application Order (taxable food first for tax optimization):
  1. Chips ($4.29 subtotal) → Fully paid by SNAP
     - snapPaidAmount: $4.29
     - snapPaidPercent: 100%
     - Tax: $0.41 → $0.00 (exempt!)
     
  2. Soda ($6.29 subtotal) → Fully paid by SNAP
     - snapPaidAmount: $6.29
     - snapPaidPercent: 100%
     - Tax: $0.60 → $0.00 (exempt!)
     
  3. Bread ($3.42 from remaining $3.42)
     - SNAP remaining: $14.00 - $4.29 - $6.29 = $3.42
     - snapPaidAmount: $3.42
     - snapPaidPercent: 98.0%

AFTER SNAP:
  Remaining Subtotal:  $23.06 - $14.00 = $9.06
  Tax on Bread (2%):   $0.00 (exempt)
  Tax on Chips:        $0.00 (100% SNAP)
  Tax on Soda:         $0.00 (100% SNAP)
  Tax on Detergent:    $0.85
  New Tax Total:       $0.85
  
  Tax Savings from SNAP: $1.01

STEP 3: Credit Card for Remaining

  Remaining Due:      $9.06 + $0.85 = $9.91
  
  Applied to:
    - Bread unpaid:    $0.07
    - Detergent:       $9.84

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
FINAL RECEIPT
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Bread                   $3.49
Chips                   $4.29
Soda 6-pack             $6.29
  (includes $0.30 CRV)
Laundry Detergent       $8.99

Subtotal:              $23.06
Tax:                    $0.85

TOTAL:                 $23.91

PAYMENTS:
  EBT SNAP             $14.00
    Tax Saved:          $1.01
  Visa *4242            $9.91

CHANGE DUE:             $0.00
```

---

## Example 4: Customer Group + Coupon Stack

### Scenario
Senior customer (10% off) with manufacturer coupon ($1 off cereal)

### Customer
- Customer Group: Senior Citizens
- Discount: 10% on all items
- Stacking: Can stack with manufacturer coupons

### Items
| Product | Retail | Tax Rate |
|---------|--------|----------|
| Cheerios 12oz | $4.99 | 0% (food) |
| Orange Juice | $3.99 | 0% (food) |
| Dish Soap | $2.99 | 9.5% |

### Coupon
- Type: Manufacturer
- Value: $1.00 off Cheerios
- Stacking: Allowed with customer discount

### Calculations

```
CHEERIOS
  Retail Price:         $4.99
  Manufacturer Coupon: -$1.00
  After Coupon:         $3.99
  Senior Discount (10%): -$0.40 (10% of $3.99)
  Final Price:          $3.59
  Tax (0%):             $0.00

ORANGE JUICE
  Retail Price:         $3.99
  Senior Discount (10%): -$0.40
  Final Price:          $3.59
  Tax (0%):             $0.00

DISH SOAP
  Retail Price:         $2.99
  Senior Discount (10%): -$0.30
  Final Price:          $2.69
  Tax (9.5%):           $0.26

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
TRANSACTION SUMMARY
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Subtotal:               $9.87
Coupon Savings:         $1.00
Senior Savings:         $1.10
Total Savings:          $2.10
Tax:                    $0.26
Grand Total:            $10.13

(Original total would be: $12.23)
```

---

## Example 5: WIC Transaction

### Scenario
WIC customer with specific category allowances

### WIC Benefits
- Milk: 4 gallons remaining
- Cereal: 36oz remaining  
- Eggs: 1 dozen remaining
- CVB (Produce): $8.00 remaining

### Items
| Product | Category | Size | Price | WIC Approved |
|---------|----------|------|-------|--------------|
| 1% Milk | Milk | 1 gal | $4.29 | Yes |
| Cheerios | Cereal | 18oz | $4.99 | Yes |
| Large Eggs | Eggs | 1 doz | $3.49 | Yes |
| Bananas | Produce | 2 lb | $1.38 | Yes (CVB) |
| Grapes | Produce | 1 lb | $3.99 | Yes (CVB) |
| Cookies | Snacks | - | $3.99 | No |

### Calculations

```
STEP 1: Apply WIC Category Benefits

MILK (1 gallon)
  Price:          $4.29
  WIC Allowance:  4 gallons (1 used)
  WIC Pays:       $4.29 (100%)
  Customer Pays:  $0.00
  Remaining:      3 gallons

CHEERIOS (18oz)
  Price:          $4.99
  WIC Allowance:  36oz (18oz used)
  WIC Pays:       $4.99 (100%)
  Customer Pays:  $0.00
  Remaining:      18oz

LARGE EGGS (1 dozen)
  Price:          $3.49
  WIC Allowance:  1 dozen (1 used)
  WIC Pays:       $3.49 (100%)
  Customer Pays:  $0.00
  Remaining:      0 dozen

STEP 2: Apply CVB (Cash Value Benefit) for Produce

CVB Balance: $8.00

BANANAS ($1.38)
  CVB Applied:    $1.38
  Remaining CVB:  $6.62

GRAPES ($3.99)
  CVB Applied:    $3.99
  Remaining CVB:  $2.63

STEP 3: Non-WIC Items

COOKIES
  Price:          $3.99
  Tax (9.5%):     $0.38
  Customer Pays:  $4.37

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
TRANSACTION SUMMARY
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
1% Milk 1 Gal           $4.29   WIC
Cheerios 18oz           $4.99   WIC
Large Eggs 1 Doz        $3.49   WIC
Bananas 2 lb            $1.38   CVB
Grapes 1 lb             $3.99   CVB
Cookies                 $3.99

Subtotal:              $22.13
Tax:                    $0.38
Grand Total:           $22.51

PAYMENTS:
  WIC Categories:      $12.77
  WIC CVB:              $5.37
  
  Customer Due:         $4.37
  
CVB Remaining:          $2.63
```

---

## Example 6: Multi-Buy with Partial Quantity

### Scenario
"3 for $10" soda deal, customer buys 7

### Items
| Product | Retail | CRV | Tax Rate |
|---------|--------|-----|----------|
| Soda 2L (7) | $3.99 each | $0.10 | 9.5% |

### Calculations

```
PROMOTION: 3 for $10.00

STEP 1: Calculate promotion sets
  Quantity: 7
  Sets: 7 ÷ 3 = 2 complete sets (6 items)
  Leftover: 1 item at regular price

STEP 2: Calculate promotional items (6 units)
  Regular price: 6 × $3.99 = $23.94
  Promotional price: 2 sets × $10.00 = $20.00
  Discount: $23.94 - $20.00 = $3.94
  Per unit discount: $3.94 ÷ 6 = $0.66
  
  Promotional unit price: $3.99 - $0.66 = $3.33

STEP 3: Calculate leftover item (1 unit)
  Regular price: $3.99

STEP 4: Add CRV and tax

PROMOTIONAL SODA (6 units)
  Base Price:     $3.33 × 6 = $19.98
  CRV:            $0.10 × 6 = $0.60
  Taxable:        $20.58
  Tax (9.5%):     $1.96
  Line Total:     $22.54

REGULAR SODA (1 unit)
  Base Price:     $3.99
  CRV:            $0.10
  Taxable:        $4.09
  Tax (9.5%):     $0.39
  Line Total:     $4.48

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
TRANSACTION SUMMARY
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Soda 2L (6)             $20.58
  3 for $10 (×2)       -$3.94
Soda 2L (1)              $4.09

Subtotal:               $24.67
CRV Total:               $0.70
Savings:                 $3.94
Tax:                     $2.35
Grand Total:            $27.02

(Without promo: $30.96)
```

---

## Example 7: Employee Purchase with Exclusions

### Scenario
Employee buying groceries with 15% discount (excludes alcohol, tobacco, sale items)

### Items
| Product | Retail | On Sale | Category | Discount Applies |
|---------|--------|---------|----------|------------------|
| Steak | $12.99 | No | Meat | Yes |
| Bread | $3.49 | $2.99 | Bakery | No (sale item) |
| Beer 6-pack | $9.99 | No | Alcohol | No (excluded) |
| Chips | $4.29 | No | Snacks | Yes |

### Calculations

```
STEAK
  Retail:           $12.99
  Employee 15%:     -$1.95
  Final:            $11.04
  Tax (0%):         $0.00

BREAD (On Sale)
  Sale Price:       $2.99
  Employee:         $0.00 (excluded - sale item)
  Final:            $2.99
  Tax (0%):         $0.00

BEER 6-PACK
  Retail:           $9.99
  Employee:         $0.00 (excluded - alcohol)
  Final:            $9.99
  CRV:              $0.30
  Tax (9.5%):       $0.98

CHIPS
  Retail:           $4.29
  Employee 15%:     -$0.64
  Final:            $3.65
  Tax (9.5%):       $0.35

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
TRANSACTION SUMMARY
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Steak                  $11.04
  Employee Discount:   -$1.95
Bread                   $2.99
  (Sale Price)
Beer 6-pack            $10.29
  (includes CRV)
Chips                   $3.65
  Employee Discount:   -$0.64

Subtotal:              $27.97
Employee Savings:       $2.59
CRV:                    $0.30
Tax:                    $1.33
Grand Total:           $29.30
```

---

## Calculation Verification Checklist

Use this checklist to verify transaction calculations:

```
□ Price Used determined correctly (prompted > customer > sale > bulk > retail)
□ Bulk pricing applied if quantity threshold met
□ All deposits added (CRV, bottle, etc.)
□ Promotions evaluated and best applied
□ Coupons validated and applied
□ Customer discounts calculated
□ Manual/invoice discounts applied (mutually exclusive)
□ Floor price enforced (or override approved)
□ Final price = Price - All Discounts + Deposits
□ Tax calculated on final price (not retail)
□ SNAP/WIC eligibility calculated
□ Line totals = Final Price × Quantity
□ Line tax = Tax Per Unit × Quantity × (1 - SNAP%)
□ Transaction totals = Sum of all line totals
□ Grand total = Subtotal + Tax
□ Payments applied in correct order
□ SNAP tax exemption applied
□ Change calculated correctly
□ All savings tracked and displayed
```

---

[← Back to Index](./INDEX.md) | [Previous: Calculation Engine](./CALCULATION_ENGINE.md)

