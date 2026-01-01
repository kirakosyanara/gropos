# Deposits and Fees

[← Back to Index](./INDEX.md) | [Previous: Government Benefits](./GOVERNMENT_BENEFITS.md) | [Next: Payment Processing →](../PAYMENT_PROCESSING.md)

---

## Overview

Deposits and fees are additional charges added to product prices for regulatory, environmental, or operational purposes in GroPOS.

---

## Fee Types

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          DEPOSIT/FEE TYPES                              │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ENVIRONMENTAL DEPOSITS                                                 │
│  ══════════════════════                                                 │
│  • CRV (California Redemption Value)                                    │
│  • Bottle Deposit (returnable containers)                               │
│  • Battery Recycling Fee                                                │
│  • E-Waste Fee (electronics)                                            │
│  • Tire Disposal Fee                                                    │
│                                                                         │
│  REGULATORY FEES                                                        │
│  ═══════════════                                                        │
│  • Bag Fee (plastic/paper bags)                                         │
│  • Sugar Tax / Sweetened Beverage Tax                                   │
│  • Cannabis Excise Tax                                                  │
│                                                                         │
│  OPERATIONAL FEES                                                       │
│  ════════════════                                                       │
│  • Delivery Fee                                                         │
│  • Curbside Pickup Fee                                                  │
│  • Processing Fee                                                       │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## CRV (California Redemption Value)

### CRV Configuration

```kotlin
data class CRVViewModel(
    val id: Int,
    val name: String,                     // "CRV 5¢", "CRV 10¢"
    val price: BigDecimal,                // 0.05, 0.10
    val description: String,              // "Under 24oz", "24oz and larger"
    
    // Size thresholds
    val minOunces: BigDecimal? = null,
    val maxOunces: BigDecimal? = null,
    
    // Container types
    val applicableTypes: List<ContainerType> = emptyList()
)

enum class ContainerType {
    ALUMINUM_CAN,
    GLASS_BOTTLE,
    PLASTIC_BOTTLE,
    BI_METAL,
    CARDBOARD_JUICE
}
```

### CRV Calculation

```kotlin
fun calculateCRV(product: ProductViewModel): BigDecimal {
    if (product.crvId == null) {
        return BigDecimal.ZERO
    }
    
    val crvRecord = Manager.crv.getCrvById(product.crvId)
        ?: return BigDecimal.ZERO
    
    return crvRecord.price
}
```

### CRV Tax Treatment (California)

> **IMPORTANT:** CRV is NOT a separate fee or deposit for tax purposes. 
> Per the California Department of Tax and Fee Administration (CDTFA), 
> CRV is considered part of the **gross receipts** from the sale of the 
> beverage and **IS SUBJECT TO SALES TAX**.

The correct calculation order is:

```kotlin
fun calculateFinalPriceWithCRV(item: TransactionItemViewModel, product: ProductViewModel) {
    val priceBeforeDiscounts = item.priceUsed
    val discounts = item.discountAmountPerUnit + item.transactionDiscountAmountPerUnit
    
    val priceAfterDiscounts = priceBeforeDiscounts - discounts
    
    // CRV is added BEFORE tax calculation
    item.crvRatePerUnit = calculateCRV(product)
    item.finalPrice = priceAfterDiscounts + item.crvRatePerUnit
    
    // Tax is calculated on the COMBINED amount (product price + CRV)
    // This is required by California tax law
    item.taxPerUnit = item.finalPrice * (item.taxPercentSum / BigDecimal(100))
}
```

### Example: CRV Taxability

```
Soda 2-Liter
  Product Price:        $2.49
  CRV (24oz+):         +$0.10
  ─────────────────────────────
  Taxable Amount:       $2.59   ← Tax calculated on THIS amount
  Sales Tax (9.5%):    +$0.25
  ─────────────────────────────
  Customer Pays:        $2.84

WRONG Approach (DO NOT USE):
  Product Price:        $2.49
  Sales Tax (9.5%):    +$0.24   ← Wrong! Missing CRV in taxable base
  CRV:                 +$0.10
  Total:                $2.83   ← Undertaxed by $0.01
```

### Total CRV Calculation

```kotlin
fun calculateTotalCRV(orderList: List<TransactionItemViewModel>): BigDecimal {
    return orderList
        .filter { !it.isRemoved }
        .filter { it.crvRatePerUnit != null }
        .sumOf { it.crvRatePerUnit * it.quantityUsed }
        .setScale(2, RoundingMode.HALF_UP)
}
```

---

## Bottle Deposits

### Bottle Deposit System

```kotlin
data class BottleDeposit(
    val id: Int,
    val name: String,                     // "Bottle Deposit 30¢"
    val depositAmount: BigDecimal,        // 0.30
    
    // Container info
    val containerType: String,            // "Glass", "Plastic"
    val containerSize: String,            // "12oz", "2L"
    
    // Return info
    val isReturnable: Boolean,
    val returnValue: BigDecimal,          // What customer gets back
    val returnLocation: String            // "Customer Service", "Machine"
)

fun applyBottleDeposit(item: TransactionItemViewModel, product: ProductViewModel) {
    if (product.bottleDepositId == null) {
        item.bottleDepositPerUnit = BigDecimal.ZERO
        return
    }
    
    val deposit = getBottleDeposit(product.bottleDepositId)
    item.bottleDepositPerUnit = deposit.depositAmount
}

// Bottle deposit returns (customer returning empties)
fun processBottleReturn(quantity: Int, deposit: BottleDeposit): TransactionItemViewModel {
    val returnAmount = BigDecimal(quantity) * deposit.returnValue
    
    return TransactionItemViewModel().apply {
        branchProductName = "Bottle Return - ${deposit.name}"
        quantityUsed = BigDecimal(quantity)
        finalPrice = -deposit.returnValue  // Negative (credit)
        subTotal = -returnAmount
        taxPerUnit = BigDecimal.ZERO  // Returns not taxed
        taxTotal = BigDecimal.ZERO
    }
}
```

---

## Bag Fees

### Bag Fee Configuration

```kotlin
data class BagFeeConfig(
    val bagFeeEnabled: Boolean,
    val paperBagFee: BigDecimal,          // $0.10
    val plasticBagFee: BigDecimal,        // $0.10
    val reusableBagFee: BigDecimal,       // $2.00 (if selling bags)
    
    // Exemptions
    val exemptSnapCustomers: Boolean,     // Free bags for SNAP
    val exemptWicCustomers: Boolean,      // Free bags for WIC
    val bagFeeTaxable: Boolean
)

data class BagProduct(
    val id: Int,
    val type: BagType,                    // Paper, Plastic, Reusable
    val price: BigDecimal,
    val isTaxable: Boolean
)

enum class BagType { PAPER, PLASTIC, REUSABLE }
```

### Bag Fee Processing

```kotlin
fun addBagFee(transaction: Transaction, type: BagType, quantity: Int) {
    val config = getBagFeeConfig()
    
    // Check exemptions
    if (transaction.hasSnapPayment && config.exemptSnapCustomers) {
        return  // No bag fee for SNAP
    }
    
    if (transaction.hasWicPayment && config.exemptWicCustomers) {
        return
    }
    
    val feePerBag = when (type) {
        BagType.PAPER -> config.paperBagFee
        BagType.PLASTIC -> config.plasticBagFee
        BagType.REUSABLE -> config.reusableBagFee
    }
    
    val bagItem = TransactionItemViewModel().apply {
        branchProductName = "${type.name} Bag"
        quantityUsed = BigDecimal(quantity)
        priceUsed = feePerBag
        finalPrice = feePerBag
        subTotal = feePerBag * BigDecimal(quantity)
        
        if (config.bagFeeTaxable) {
            taxPerUnit = feePerBag * (taxRate / BigDecimal(100))
            taxTotal = taxPerUnit * BigDecimal(quantity)
        }
    }
    
    // Special handling for SNAP transactions
    OrderStore.bagProduct = bagItem
    transaction.items.add(bagItem)
}

// Handle bag product when SNAP is used
fun handleBagProductForSnap(bagItem: TransactionItemViewModel) {
    // When customer uses SNAP, bag fee is typically waived
    bagItem.discountTypeId = TransactionDiscountType.ItemPercentage
    bagItem.discountTypeAmount = BigDecimal(100)  // 100% off
    bagItem.discountAmountPerUnit = bagItem.priceUsed
    bagItem.isFloorPriceOverridden = true
    
    bagItem.finalPrice = BigDecimal.ZERO
    bagItem.subTotal = BigDecimal.ZERO
    bagItem.taxTotal = BigDecimal.ZERO
}
```

---

## Other Fees

### Sweetened Beverage Tax

```kotlin
data class SweetenedBeverageTaxConfig(
    val enabled: Boolean,
    val ratePerOunce: BigDecimal,         // $0.01 per ounce
    val applicableCategories: List<Int>,
    val exemptProductIds: List<Int>,
    val exemptMilkBased: Boolean,         // Milk-based drinks exempt
    val exemptInfantFormula: Boolean,     // Baby formula exempt
    val sugarThreshold: BigDecimal        // Min sugar to trigger
)

fun calculateSweetenedBeverageTax(product: ProductViewModel): BigDecimal {
    val config = getSweetenedBeverageTaxConfig()
    
    if (!config.enabled) return BigDecimal.ZERO
    if (product.categoryId !in config.applicableCategories) return BigDecimal.ZERO
    if (product.id in config.exemptProductIds) return BigDecimal.ZERO
    if (config.exemptMilkBased && product.isMilkBased) return BigDecimal.ZERO
    
    // Calculate based on ounces
    val tax = product.sizeOunces * config.ratePerOunce
    
    return tax.setScale(2, RoundingMode.HALF_UP)
}
```

### Delivery Fee

```kotlin
data class DeliveryFeeConfig(
    val enabled: Boolean,
    val flatFee: BigDecimal,              // $5.99 flat
    val perItemFee: BigDecimal,           // $0.50 per item
    val percentOfOrder: BigDecimal,       // 3% of order
    val minimumOrderForFree: BigDecimal,  // Free delivery over $50
    val minimumFee: BigDecimal,           // At least $2.99
    val maximumFee: BigDecimal,           // Cap at $9.99
    val isTaxable: Boolean
)

fun calculateDeliveryFee(transaction: Transaction, config: DeliveryFeeConfig): BigDecimal {
    if (!config.enabled) return BigDecimal.ZERO
    
    val orderTotal = transaction.subTotal
    
    // Check free delivery threshold
    if (orderTotal >= config.minimumOrderForFree) {
        return BigDecimal.ZERO
    }
    
    var fee = BigDecimal.ZERO
    
    // Flat fee
    if (config.flatFee > BigDecimal.ZERO) {
        fee += config.flatFee
    }
    
    // Per item fee
    if (config.perItemFee > BigDecimal.ZERO) {
        fee += transaction.itemCount.toBigDecimal() * config.perItemFee
    }
    
    // Percentage fee
    if (config.percentOfOrder > BigDecimal.ZERO) {
        fee += orderTotal * (config.percentOfOrder / BigDecimal(100))
    }
    
    // Apply min/max
    fee = maxOf(fee, config.minimumFee)
    fee = minOf(fee, config.maximumFee)
    
    return fee.setScale(2, RoundingMode.HALF_UP)
}
```

---

## Fee Display and Reporting

### Receipt Display

```
=== RECEIPT ===
Soda 2-Liter              $2.99
  CRV                     $0.10
Chips                     $3.99
Paper Bag (2)             $0.20
----------------------------------
Subtotal                  $7.28
Tax                       $0.69
----------------------------------
TOTAL                     $7.97

Deposits & Fees:
  CRV Total:              $0.10
  Bag Fees:               $0.20
```

### Fee Reporting

```kotlin
data class FeeReport(
    val reportDate: OffsetDateTime,
    
    // CRV
    val totalCrvCollected: BigDecimal,
    val crvContainerCount: Int,
    
    // Bottle Deposits
    val depositsCollected: BigDecimal,
    val depositsRefunded: BigDecimal,
    val netDeposits: BigDecimal,
    
    // Bag Fees
    val bagsSold: Int,
    val bagFeesCollected: BigDecimal,
    
    // Other Fees
    val deliveryFeesCollected: BigDecimal,
    val sweetenedBevTaxCollected: BigDecimal
)
```

---

[← Back to Index](./INDEX.md) | [Previous: Government Benefits](./GOVERNMENT_BENEFITS.md) | [Next: Payment Processing →](../PAYMENT_PROCESSING.md)

