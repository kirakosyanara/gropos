# Tax Calculations

[← Back to Index](./INDEX.md) | [Previous: Customer Pricing](./CUSTOMER_PRICING.md) | [Next: Government Benefits →](./GOVERNMENT_BENEFITS.md)

---

## Overview

Tax calculation is one of the most complex aspects of POS systems, requiring support for multiple jurisdictions, varying rates by product category, exemptions, and special programs.

---

## Tax Structure

### Tax Jurisdiction Hierarchy

```
┌─────────────────────────────────────────────────────────────────────────┐
│                       TAX JURISDICTION HIERARCHY                        │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  FEDERAL TAXES (if applicable)                                          │
│  ═════════════════════════════                                          │
│  • Federal Excise Tax (alcohol, tobacco, fuel)                          │
│                                                                         │
│  STATE TAXES                                                            │
│  ═══════════                                                            │
│  • State Sales Tax (e.g., CA 7.25%)                                     │
│  • State Excise Taxes                                                   │
│                                                                         │
│  COUNTY TAXES                                                           │
│  ════════════                                                           │
│  • County Sales Tax (e.g., 0.25%)                                       │
│  • County Special Taxes                                                 │
│                                                                         │
│  CITY/LOCAL TAXES                                                       │
│  ════════════════                                                       │
│  • City Sales Tax (e.g., 1.00%)                                         │
│  • District Taxes (e.g., transit, tourism)                              │
│                                                                         │
│  SPECIAL DISTRICT TAXES                                                 │
│  ══════════════════════                                                 │
│  • Transportation District                                              │
│  • Stadium/Convention                                                   │
│  • Hospital District                                                    │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### Tax Definition Model

```kotlin
data class TaxJurisdiction(
    val id: Int,
    val name: String,                      // "California State"
    val code: String,                      // "CA"
    val level: JurisdictionLevel,          // Federal, State, County, City, District
    val parentJurisdictionId: String?,     // For hierarchy
    
    // Location
    val fipsCode: String?,
    val zipCodes: List<String>,
    val boundary: GeoPolygon?              // Geographic boundary
)

data class TaxRate(
    val id: Int,
    val jurisdictionId: Int,
    val taxName: String,                   // "CA State Sales Tax"
    val taxCode: String,                   // "CA_STATE"
    
    // Rate
    val rate: BigDecimal,                  // 7.250 (percentage)
    val isCompounded: Boolean,             // Tax on tax
    val compoundOrder: Int?,               // Order in compound calculation
    
    // Applicability
    val applicability: TaxApplicability,   // AllItems, Category, Specific
    val applicableCategoryIds: List<Int>,  // Categories this applies to
    val excludedCategoryIds: List<Int>,    // Categories exempt
    
    // Effective Dates
    val effectiveDate: OffsetDateTime,
    val expirationDate: OffsetDateTime?,
    
    // Reporting
    val glAccountCode: String?,            // For accounting
    val reportingCode: String?             // For tax filing
)

enum class JurisdictionLevel {
    FEDERAL,
    STATE,
    COUNTY,
    CITY,
    DISTRICT
}
```

### Product Tax Configuration

```kotlin
data class ProductTax(
    val id: Int,
    val productId: Int,
    val taxId: Int,                        // Link to TaxRate
    val isTaxable: Boolean,                // Is this tax applied
    val isIncluded: Boolean,               // Tax included in price
    val overrideRate: BigDecimal?          // Product-specific rate override
)

data class ProductViewModel(
    // ... other fields ...
    
    val taxes: List<ProductTaxViewModel>,  // All applicable taxes
    val taxPercentSum: BigDecimal,         // Pre-calculated sum
    val isTaxExempt: Boolean,              // Fully exempt from sales tax
    val taxExemptReason: String?           // "Food item", "Medicine"
)
```

---

## Tax Calculation Process

### The Tax Consistency Rule

> **CRITICAL: Tax Per Unit First, Then Multiply by Quantity**
> 
> Tax MUST be calculated on a SINGLE UNIT first, rounded, THEN multiplied 
> by quantity. This ensures fairness and consistency:
> - 1 customer buying 3 items pays the SAME total tax as
> - 3 customers each buying 1 item

**Why This Matters:**

```
CORRECT: Tax Per Unit × Quantity
════════════════════════════════════════════════════════════════════
Item: Soda $2.59 + $0.10 CRV = $2.69 taxable, Tax Rate: 9.5%

Per Unit Tax: $2.69 × 9.5% = $0.25555 → ROUND → $0.26

Customer A buys 3:  $0.26 × 3 = $0.78 tax
Customer B buys 1:  $0.26 tax
Customer C buys 1:  $0.26 tax
Customer D buys 1:  $0.26 tax
Total (B+C+D):      $0.78 tax  ✓ MATCHES Customer A

════════════════════════════════════════════════════════════════════
WRONG: Tax on Line Total (DO NOT USE)
════════════════════════════════════════════════════════════════════
Customer A buys 3:  $2.69 × 3 = $8.07 × 9.5% = $0.7667 → $0.77 tax
Customers B+C+D:    $0.26 × 3 = $0.78 tax  ✗ MISMATCH!

The $0.01 "bulk tax discount" is unfair and creates audit issues.
```

### Per-Item Tax Calculation

```kotlin
fun calculateItemTax(item: TransactionItemViewModel) {
    val product = getProduct(item.branchProductId)
    
    // Check product-level exemption
    if (product.isTaxExempt) {
        item.taxPercentSum = BigDecimal.ZERO
        item.taxPerUnit = BigDecimal.ZERO
        item.taxTotal = BigDecimal.ZERO
        item.taxes = emptyList()
        return
    }
    
    // Get applicable taxes
    val applicableTaxes = getApplicableTaxes(product)
    
    // Calculate combined rate
    item.taxPercentSum = applicableTaxes.sumOf { it.rate }
    
    // ══════════════════════════════════════════════════════════════════
    // STEP 1: Calculate taxable amount for ONE UNIT
    // ══════════════════════════════════════════════════════════════════
    // Final price includes CRV (required by CA law to be taxed)
    val taxableAmountPerUnit = item.finalPrice
    
    // ══════════════════════════════════════════════════════════════════
    // STEP 2: Calculate tax for ONE UNIT and ROUND
    // ══════════════════════════════════════════════════════════════════
    item.taxPerUnit = (taxableAmountPerUnit * item.taxPercentSum / BigDecimal(100))
        .setScale(2, RoundingMode.HALF_UP)
    
    // ══════════════════════════════════════════════════════════════════
    // STEP 3: Multiply ROUNDED per-unit tax by quantity
    // ══════════════════════════════════════════════════════════════════
    var taxTotal = item.taxPerUnit * item.quantityUsed
    
    // For items with SNAP payment, reduce tax proportionally
    if (item.snapPaidPercent > BigDecimal.ZERO) {
        val snapFactor = BigDecimal.ONE - (item.snapPaidPercent / BigDecimal(100))
        taxTotal *= snapFactor
    }
    
    // Final rounding of line total
    item.taxTotal = taxTotal.setScale(2, RoundingMode.HALF_UP)
    
    // Build tax breakdown by jurisdiction
    item.taxes = buildTaxBreakdown(applicableTaxes, item.taxTotal, item.taxPercentSum)
}

fun calculateTaxPerUnit(taxableAmount: BigDecimal?, taxRate: BigDecimal?): BigDecimal {
    if (taxableAmount == null || taxableAmount <= BigDecimal.ZERO) {
        return BigDecimal.ZERO
    }
    if (taxRate == null || taxRate <= BigDecimal.ZERO) {
        return BigDecimal.ZERO
    }
    
    // Calculate tax for ONE unit
    val tax = taxableAmount * (taxRate / BigDecimal(100))
    
    // Round the per-unit tax BEFORE multiplying by quantity
    return tax.setScale(2, RoundingMode.HALF_UP)
}
```

### Tax Breakdown by Jurisdiction

```kotlin
fun buildTaxBreakdown(
    applicableTaxes: List<TaxRate>,
    totalTax: BigDecimal,
    totalRate: BigDecimal
): List<TransactionItemTaxViewModel> {
    
    val breakdown = mutableListOf<TransactionItemTaxViewModel>()
    
    // Calculate value of 1% of tax
    val onePercentValue = if (totalRate > BigDecimal.ZERO) {
        totalTax / totalRate
    } else {
        BigDecimal.ZERO
    }
    
    for (tax in applicableTaxes) {
        val itemTax = TransactionItemTaxViewModel(
            taxId = tax.id,
            taxName = tax.taxName,
            taxRate = tax.rate,
            jurisdictionId = tax.jurisdictionId,
            amount = (onePercentValue * tax.rate).setScale(2, RoundingMode.HALF_UP)
        )
        breakdown.add(itemTax)
    }
    
    // Adjust for rounding differences
    val calculatedTotal = breakdown.sumOf { it.amount }
    if (calculatedTotal != totalTax) {
        // Add/subtract difference to largest component
        val difference = totalTax - calculatedTotal
        val largest = breakdown.maxByOrNull { it.amount }
        largest?.let { it.amount += difference }
    }
    
    return breakdown
}
```

---

## Tax Exemptions

### Exemption Types

```
┌─────────────────────────────────────────────────────────────────────────┐
│                       TAX EXEMPTION TYPES                               │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  PRODUCT-LEVEL EXEMPTIONS                                               │
│  ═════════════════════════                                              │
│  • Unprepared Food (groceries)                                          │
│  • Prescription Medication                                              │
│  • Medical Equipment (DME)                                              │
│  • Baby Formula/Diapers (some states)                                   │
│  • Feminine Hygiene (some states)                                       │
│  • Newspapers/Magazines                                                 │
│  • Seeds for Gardens                                                    │
│                                                                         │
│  CUSTOMER-LEVEL EXEMPTIONS                                              │
│  ═════════════════════════                                              │
│  • Nonprofit Organizations (501c3)                                      │
│  • Government Agencies                                                  │
│  • Resale (with resale certificate)                                     │
│  • Agricultural Use                                                     │
│  • Diplomatic (foreign diplomats)                                       │
│                                                                         │
│  PAYMENT-LEVEL EXEMPTIONS                                               │
│  ═════════════════════════                                              │
│  • SNAP/EBT Payments (food portion)                                     │
│  • WIC Payments                                                         │
│                                                                         │
│  TIME-BASED EXEMPTIONS                                                  │
│  ═════════════════════                                                  │
│  • Tax Holidays (back-to-school, disaster prep)                         │
│  • Promotional Periods                                                  │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### Customer Tax Exemption

```kotlin
data class CustomerTaxExemption(
    val customerId: Int,
    val certificateNumber: String,         // Tax exempt certificate #
    val exemptionType: ExemptionType,      // Nonprofit, Resale, etc.
    
    // Validity
    val effectiveDate: OffsetDateTime,
    val expirationDate: OffsetDateTime?,
    val isActive: Boolean,
    
    // Scope
    val scope: TaxExemptionScope,          // AllTaxes, StateTaxOnly, etc.
    val exemptJurisdictions: List<String>, // Specific jurisdictions
    val exemptCategories: List<Int>,       // Specific categories
    
    // Documentation
    val certificateImagePath: String?,     // Scanned certificate
    val verifiedDate: OffsetDateTime?,
    val verifiedBy: Int?
)

enum class ExemptionType {
    NONPROFIT,               // 501(c)(3)
    GOVERNMENT,              // Federal/State/Local government
    RESALE,                  // Buying for resale
    MANUFACTURING,           // Raw materials
    AGRICULTURAL,            // Farm use
    DIPLOMATIC,              // Foreign diplomats
    RELIGIOUS,               // Religious organizations
    EDUCATIONAL              // Schools
}

fun applyCustomerTaxExemption(transaction: Transaction, customer: CustomerProfile) {
    val exemption = getActiveTaxExemption(customer) ?: return
    
    // Validate exemption
    if (!isExemptionValid(exemption)) return
    
    for (item in transaction.items) {
        if (item.isRemoved) continue
        
        // Check if item category is exempt
        if (exemption.exemptCategories.isNotEmpty()) {
            val product = getProduct(item.branchProductId)
            if (!exemption.exemptCategories.contains(product.categoryId)) {
                continue
            }
        }
        
        // Apply exemption
        item.isTaxExempt = true
        item.taxExemptReason = exemption.exemptionType.description
        item.taxPerUnit = BigDecimal.ZERO
        item.taxTotal = BigDecimal.ZERO
        item.taxes = emptyList()
    }
    
    // Store exemption info on transaction
    transaction.taxExemptCertificate = exemption.certificateNumber
    transaction.taxExemptType = exemption.exemptionType
}
```

---

## Tax Holidays

### Tax Holiday Definition

```kotlin
data class TaxHoliday(
    val id: Int,
    val name: String,                      // "Back to School Tax Holiday"
    val description: String?,
    
    // Timing
    val startDate: OffsetDateTime,
    val endDate: OffsetDateTime,
    
    // Scope
    val scope: TaxHolidayScope,            // AllItems, Categories, Threshold
    val qualifyingCategoryIds: List<Int>,
    val maxItemPrice: BigDecimal?,         // Items under $X qualify
    
    // Tax Relief
    val exemptStateTax: Boolean,
    val exemptLocalTax: Boolean,
    val exemptAllTax: Boolean,
    val reducedRate: BigDecimal?,          // Or reduced rate instead of exempt
    
    // Jurisdiction
    val applicableJurisdictions: List<String>
)

fun applyTaxHoliday(item: TransactionItemViewModel) {
    val activeHolidays = getActiveTaxHolidays()
    
    for (holiday in activeHolidays) {
        if (qualifiesForHoliday(item, holiday)) {
            when {
                holiday.exemptAllTax -> {
                    item.taxPercentSum = BigDecimal.ZERO
                    item.taxPerUnit = BigDecimal.ZERO
                    item.taxes = emptyList()
                }
                holiday.reducedRate != null -> {
                    item.taxPercentSum = holiday.reducedRate
                    recalculateTax(item)
                }
                else -> {
                    // Selectively remove taxes
                    val adjustedTaxes = item.taxes.filter { tax ->
                        !(holiday.exemptStateTax && tax.isStateTax) &&
                        !(holiday.exemptLocalTax && tax.isLocalTax)
                    }
                    
                    item.taxes = adjustedTaxes
                    item.taxPercentSum = adjustedTaxes.sumOf { it.rate }
                    recalculateTax(item)
                }
            }
            
            item.taxHolidayApplied = holiday.name
            break  // Only one holiday applies
        }
    }
}

fun qualifiesForHoliday(item: TransactionItemViewModel, holiday: TaxHoliday): Boolean {
    // Check price threshold
    if (holiday.maxItemPrice != null && item.priceUsed > holiday.maxItemPrice) {
        return false
    }
    
    // Check category
    if (holiday.qualifyingCategoryIds.isNotEmpty()) {
        val product = getProduct(item.branchProductId)
        if (!holiday.qualifyingCategoryIds.contains(product.categoryId)) {
            return false
        }
    }
    
    return true
}
```

---

## Special Tax Scenarios

### Tax-Inclusive Pricing

Some items have tax included in the displayed price:

```kotlin
fun handleTaxInclusiveItem(item: TransactionItemViewModel) {
    val product = getProduct(item.branchProductId)
    
    if (!product.isTaxInclusive) return  // Normal tax calculation
    
    // Displayed price includes tax
    // Back-calculate the pre-tax price
    val taxRate = item.taxPercentSum / BigDecimal(100)
    
    val preTaxPrice = item.priceUsed / (BigDecimal.ONE + taxRate)
    val includedTax = item.priceUsed - preTaxPrice
    
    // Adjust item values
    item.finalPriceExcludingTax = preTaxPrice
    item.includedTax = includedTax
    item.taxPerUnit = includedTax
    item.subTotal = preTaxPrice * item.quantityUsed
    item.taxTotal = includedTax * item.quantityUsed
    
    // Note: Grand total remains same (tax was already in price)
}
```

### Compound Taxes

Some jurisdictions tax on top of other taxes:

```kotlin
fun calculateCompoundTax(item: TransactionItemViewModel, taxes: List<TaxRate>) {
    // Separate compound and simple taxes
    val simpleTaxes = taxes.filter { !it.isCompounded }
    val compoundTaxes = taxes.filter { it.isCompounded }.sortedBy { it.compoundOrder }
    
    // Calculate simple taxes first
    var simpleTaxTotal = BigDecimal.ZERO
    for (tax in simpleTaxes) {
        val taxAmount = item.finalPrice * (tax.rate / BigDecimal(100))
        addToBreakdown(item, tax, taxAmount)
        simpleTaxTotal += taxAmount
    }
    
    // Apply compound taxes
    var compoundBase = item.finalPrice + simpleTaxTotal
    
    for (tax in compoundTaxes) {
        val taxAmount = compoundBase * (tax.rate / BigDecimal(100))
        addToBreakdown(item, tax, taxAmount)
        compoundBase += taxAmount  // Next compound tax includes this
    }
    
    // Calculate totals
    item.taxTotal = item.taxes.sumOf { it.amount }
    item.taxPerUnit = item.taxTotal / item.quantityUsed
}
```

### Deposits and Tax

Tax treatment of deposits varies by type and jurisdiction:

```kotlin
data class DepositTaxRules(
    // CRV (California): ALWAYS TAXABLE
    // CRV is part of gross receipts per CDTFA - not configurable
    
    // Bottle Deposits: Jurisdiction-dependent
    val taxBottleDeposit: Boolean,         // Varies by state
    
    // Bag Fees: Jurisdiction-dependent  
    val taxBagFee: Boolean,                // Some cities exempt
    
    // Delivery Fees: Typically taxable
    val taxDeliveryFee: Boolean = true
)

fun calculateTaxableAmount(item: TransactionItemViewModel, rules: DepositTaxRules): BigDecimal {
    // Start with product price after discounts
    var taxableAmount = item.finalPriceExcludingDeposits
    
    // CRV is ALWAYS included in taxable amount (California law)
    // This is NOT optional - CRV is considered part of the sale price
    taxableAmount += item.crvRatePerUnit
    
    // Other deposits depend on jurisdiction rules
    if (rules.taxBottleDeposit) {
        taxableAmount += item.bottleDepositPerUnit
    }
    
    if (rules.taxBagFee) {
        taxableAmount += item.bagFeePerUnit
    }
    
    return taxableAmount
}
```

> **California CRV Note:** The CDTFA considers CRV as part of the gross 
> receipts from the sale, not a separate fee. CRV MUST be included in 
> the taxable amount. This is a legal requirement, not a configuration option.

---

## Transaction Tax Totals

### Aggregating Line Taxes

```kotlin
fun calculateTransactionTax(items: List<TransactionItemViewModel>): TransactionTaxSummary {
    val taxSummary = TransactionTaxSummary()
    val jurisdictionTotals = mutableMapOf<Int, BigDecimal>()
    
    for (item in items) {
        if (item.isRemoved) continue
        
        taxSummary.totalTax += item.taxTotal
        
        // Aggregate by jurisdiction
        for (itemTax in item.taxes) {
            val current = jurisdictionTotals[itemTax.jurisdictionId] ?: BigDecimal.ZERO
            jurisdictionTotals[itemTax.jurisdictionId] = current + itemTax.amount
        }
    }
    
    // Build jurisdiction breakdown
    for ((jurisdictionId, amount) in jurisdictionTotals) {
        val jurisdiction = getJurisdiction(jurisdictionId)
        taxSummary.jurisdictionBreakdown.add(
            TaxJurisdictionTotal(
                jurisdictionId = jurisdictionId,
                jurisdictionName = jurisdiction.name,
                taxCode = jurisdiction.code,
                amount = amount
            )
        )
    }
    
    return taxSummary
}
```

### Tax Reporting Structure

```kotlin
data class TransactionTaxSummary(
    var totalTax: BigDecimal = BigDecimal.ZERO,       // Grand total of all taxes
    var stateTax: BigDecimal = BigDecimal.ZERO,       // State portion
    var countyTax: BigDecimal = BigDecimal.ZERO,      // County portion
    var cityTax: BigDecimal = BigDecimal.ZERO,        // City portion
    var districtTax: BigDecimal = BigDecimal.ZERO,    // Special districts
    
    var taxableSubtotal: BigDecimal = BigDecimal.ZERO,   // Amount subject to tax
    var exemptSubtotal: BigDecimal = BigDecimal.ZERO,    // Exempt amount
    
    val jurisdictionBreakdown: MutableList<TaxJurisdictionTotal> = mutableListOf(),
    
    // For reporting
    var stateTaxCode: String? = null,
    var countyTaxCode: String? = null,
    var cityTaxCode: String? = null
)

data class TaxJurisdictionTotal(
    val jurisdictionId: Int,
    val jurisdictionName: String,
    val taxCode: String,
    var taxRate: BigDecimal = BigDecimal.ZERO,
    var taxableAmount: BigDecimal = BigDecimal.ZERO,
    var amount: BigDecimal = BigDecimal.ZERO
)
```

---

## Tax Adjustments for Payments

### SNAP Tax Exemption

```kotlin
fun adjustTaxForSNAP(item: TransactionItemViewModel, snapPaymentAmount: BigDecimal) {
    // SNAP payments make the paid portion tax-exempt
    if (!item.isSNAPEligible) return  // Not eligible for SNAP
    
    // Calculate what portion of item is paid by SNAP
    val itemTotal = item.subjectToTaxTotal + item.taxTotal
    val snapPortion = minOf(snapPaymentAmount, itemTotal)
    val snapPercent = (snapPortion / itemTotal) * BigDecimal(100)
    
    // Update SNAP tracking
    item.snapPaidAmount = snapPortion
    item.snapPaidPercent = snapPercent
    
    // Recalculate tax (SNAP portion is exempt)
    val taxableQuantity = item.quantityUsed * (BigDecimal.ONE - snapPercent / BigDecimal(100))
    item.taxTotal = item.taxPerUnit * taxableQuantity
    item.subjectToTaxTotal = item.finalPrice * taxableQuantity
    
    // Adjust breakdown proportionally
    for (tax in item.taxes) {
        tax.amount = tax.amount * (BigDecimal.ONE - snapPercent / BigDecimal(100))
    }
}
```

---

## Tax Calculation Examples

### Example 1: Standard Taxable Item

```
Product: Soda 2-Liter
Price: $2.99
CRV: $0.10
Tax Rate: 9.5% (includes 7.25% state, 1.00% county, 1.25% city)

Taxable Amount = $2.99 + $0.10 = $3.09
Tax = $3.09 × 9.5% = $0.29

Breakdown:
  State (7.25%): $0.22
  County (1.00%): $0.03
  City (1.25%): $0.04
  Total Tax: $0.29

Line Total: $3.09 + $0.29 = $3.38
```

### Example 2: Exempt Food Item

```
Product: Bread (unprepared food)
Price: $3.50
Tax Status: Exempt (grocery item)

Tax = $0.00

Line Total: $3.50
```

### Example 3: SNAP Payment Tax Adjustment

```
Product: Chips (SNAP eligible but taxable)
Price: $4.99
Tax Rate: 9.5%

BEFORE SNAP:
  Tax = $4.99 × 9.5% = $0.47
  Total = $5.46

SNAP Payment: $3.00 (54.9% of total)

AFTER SNAP:
  SNAP Portion: $3.00 (tax-exempt)
  Non-SNAP Portion: $2.46
  Adjusted Tax = $0.47 × (1 - 54.9%) = $0.21
  
  Total Due: $2.46 - $0.26 tax saved = $2.46 (non-SNAP portion only)
```

### Example 4: Tax Holiday

```
Tax Holiday: Back to School (clothing under $100)

Product: Jeans
Price: $49.99
Normal Tax: 9.5%

During Holiday:
  State Tax (7.25%): EXEMPT
  Local Tax (2.25%): Still applies
  
  Tax = $49.99 × 2.25% = $1.12
  Total = $51.11 (vs $54.74 normally)
  Savings: $3.63
```

---

[← Back to Index](./INDEX.md) | [Previous: Customer Pricing](./CUSTOMER_PRICING.md) | [Next: Government Benefits →](./GOVERNMENT_BENEFITS.md)

