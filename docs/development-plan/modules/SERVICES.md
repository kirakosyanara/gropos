# GroPOS Services

> Business logic services for Kotlin/Compose implementation

---

## Overview

The service layer contains pure business logic for GroPOS operations. All services are:

- **Pure functions** with no side effects (easy to test)
- **Singleton scope** via Koin DI
- **Platform-agnostic** (shared in commonMain)

---

## Service Overview

| Service | Purpose | Scope |
|---------|---------|-------|
| `PriceCalculator` | Calculate item and order prices | Singleton |
| `TaxCalculator` | Calculate taxes | Singleton |
| `CRVCalculator` | Calculate CRV (California Redemption Value) | Singleton |
| `DiscountCalculator` | Calculate and apply discounts | Singleton |
| `PaymentService` | Process payments | Singleton |
| `PrintService` | Print receipts | Singleton |
| `DiscountValidator` | Validate discount eligibility | Singleton |

---

## Price Calculator

Calculates product and order prices:

```kotlin
class PriceCalculator {
    
    /**
     * Calculate the subtotal for all items in the order.
     * Excludes removed items.
     */
    fun calculateSubtotal(items: List<TransactionItem>): BigDecimal {
        return items
            .filter { !it.isRemoved }
            .fold(BigDecimal.ZERO) { acc, item ->
                acc + (item.finalPrice * item.quantityUsed)
            }
            .setScale(2, RoundingMode.HALF_UP)
    }
    
    /**
     * Calculate the total price for a single item including quantity.
     */
    fun calculateItemTotal(item: TransactionItem): BigDecimal {
        return (item.finalPrice * item.quantityUsed)
            .setScale(2, RoundingMode.HALF_UP)
    }
    
    /**
     * Calculate the price per unit for weighted items.
     */
    fun calculateWeightedPrice(pricePerUnit: BigDecimal, weight: BigDecimal): BigDecimal {
        return (pricePerUnit * weight).setScale(2, RoundingMode.HALF_UP)
    }
    
    /**
     * Calculate the grand total including all components.
     */
    fun calculateGrandTotal(items: List<TransactionItem>): BigDecimal {
        return items
            .filter { !it.isRemoved }
            .fold(BigDecimal.ZERO) { acc, item ->
                acc + item.subTotal + item.taxTotal
            }
            .setScale(2, RoundingMode.HALF_UP)
    }
    
    /**
     * Determine the price to use based on price hierarchy.
     * Priority: Prompted → Customer → Sale → Bulk → Retail
     */
    fun getPriceUsed(item: TransactionItem): BigDecimal {
        return when {
            item.isPromptedPrice && item.promptedPrice != null -> item.promptedPrice
            item.salePrice != null && item.salePrice > BigDecimal.ZERO -> item.salePrice
            else -> item.retailPrice
        }
    }
    
    /**
     * Calculate final price after discounts, respecting floor price.
     */
    fun getFinalPrice(item: TransactionItem): BigDecimal {
        val calculatedPrice = item.priceUsed - 
            item.discountAmountPerUnit - 
            item.transactionDiscountAmountPerUnit
        
        val finalPrice = when {
            item.isFloorPriceOverridden -> calculatedPrice
            // Sale price can be below floor (corporate-set)
            item.priceUsed == item.salePrice && item.priceUsed < item.floorPrice -> item.priceUsed
            // Normal case: enforce floor price
            else -> maxOf(calculatedPrice, item.floorPrice)
        }
        
        // Add CRV to final price (CRV is taxable)
        return finalPrice + item.crvRatePerUnit
    }
}
```

---

## Tax Calculator

Calculates applicable taxes:

```kotlin
class TaxCalculator {
    
    /**
     * Calculate total tax for all items.
     */
    fun calculateTotalTax(items: List<TransactionItem>): BigDecimal {
        return items
            .filter { !it.isRemoved }
            .fold(BigDecimal.ZERO) { acc, item ->
                acc + calculateItemTax(item)
            }
            .setScale(2, RoundingMode.HALF_UP)
    }
    
    /**
     * Calculate tax for a single item.
     * Tax is calculated on final price (after discounts, including CRV).
     * SNAP-paid portions are tax-exempt.
     */
    fun calculateItemTax(item: TransactionItem): BigDecimal {
        if (item.isTaxExempt) return BigDecimal.ZERO
        
        val taxPerUnit = calculateTaxPerUnit(item.finalPrice, item.taxPercentSum)
        val snapFraction = item.snapPaidPercent / BigDecimal(100)
        
        return (taxPerUnit * item.quantityUsed * (BigDecimal.ONE - snapFraction))
            .setScale(2, RoundingMode.HALF_UP)
    }
    
    /**
     * Calculate tax per unit.
     */
    fun calculateTaxPerUnit(finalPrice: BigDecimal, taxPercentSum: BigDecimal): BigDecimal {
        return (finalPrice * taxPercentSum / BigDecimal(100))
            .setScale(2, RoundingMode.HALF_UP)
    }
    
    /**
     * Get tax breakdown by tax type.
     */
    fun getTaxBreakdown(items: List<TransactionItem>): Map<String, BigDecimal> {
        val breakdown = mutableMapOf<String, BigDecimal>()
        
        items.filter { !it.isRemoved && !it.isTaxExempt }.forEach { item ->
            item.taxes.forEach { tax ->
                val current = breakdown.getOrDefault(tax.taxName, BigDecimal.ZERO)
                breakdown[tax.taxName] = current + tax.amount
            }
        }
        
        return breakdown.mapValues { it.value.setScale(2, RoundingMode.HALF_UP) }
    }
    
    /**
     * Calculate the taxable amount after SNAP payments.
     */
    fun calculateSubjectToTaxTotal(
        finalPrice: BigDecimal,
        quantityUsed: BigDecimal,
        snapPaidPercent: BigDecimal
    ): BigDecimal {
        val snapFraction = snapPaidPercent / BigDecimal(100)
        return (finalPrice * quantityUsed * (BigDecimal.ONE - snapFraction))
            .setScale(2, RoundingMode.HALF_UP)
    }
}
```

---

## CRV Calculator

Calculates California Redemption Value (bottle deposits):

```kotlin
class CRVCalculator(
    private val crvRepository: CRVRepository
) {
    
    /**
     * Calculate total CRV for all items.
     */
    fun calculateTotalCRV(items: List<TransactionItem>): BigDecimal {
        return items
            .filter { !it.isRemoved }
            .filter { it.crvRatePerUnit > BigDecimal.ZERO }
            .fold(BigDecimal.ZERO) { acc, item ->
                acc + (item.crvRatePerUnit * item.quantityUsed)
            }
            .setScale(2, RoundingMode.HALF_UP)
    }
    
    /**
     * Calculate CRV for a single item.
     */
    fun calculateItemCRV(item: TransactionItem): BigDecimal {
        return (item.crvRatePerUnit * item.quantityUsed)
            .setScale(2, RoundingMode.HALF_UP)
    }
    
    /**
     * Get CRV rate for a product.
     */
    suspend fun getCRVRate(product: Product): BigDecimal {
        val crvId = product.crvId ?: return BigDecimal.ZERO
        val crvRecord = crvRepository.getById(crvId) ?: return BigDecimal.ZERO
        return crvRecord.price
    }
}
```

---

## Discount Calculator

Calculates and applies discounts:

```kotlin
class DiscountCalculator {
    
    /**
     * Calculate total savings from discounts.
     */
    fun calculateTotalSavings(items: List<TransactionItem>): BigDecimal {
        return items
            .filter { !it.isRemoved }
            .fold(BigDecimal.ZERO) { acc, item ->
                acc + (item.savingsPerUnit * item.quantityUsed)
            }
            .setScale(2, RoundingMode.HALF_UP)
    }
    
    /**
     * Calculate savings per unit.
     */
    fun calculateSavingsPerUnit(item: TransactionItem): BigDecimal {
        val basePrice = if (item.isPromptedPrice) item.promptedPrice else item.retailPrice
        return ((basePrice ?: BigDecimal.ZERO) + item.crvRatePerUnit - item.finalPrice)
            .coerceAtLeast(BigDecimal.ZERO)
    }
    
    /**
     * Apply percentage discount to item.
     */
    fun applyPercentageDiscount(price: BigDecimal, percentage: BigDecimal): BigDecimal {
        val discountAmount = price * (percentage / BigDecimal(100))
        return (price - discountAmount).setScale(2, RoundingMode.HALF_UP)
    }
    
    /**
     * Apply fixed amount discount to item.
     */
    fun applyFixedDiscount(price: BigDecimal, discountAmount: BigDecimal): BigDecimal {
        return (price - discountAmount)
            .coerceAtLeast(BigDecimal.ZERO)
            .setScale(2, RoundingMode.HALF_UP)
    }
    
    /**
     * Apply invoice-level discount to all eligible items.
     * Line discounts take precedence over invoice discounts.
     */
    fun applyInvoiceDiscount(
        items: List<TransactionItem>,
        discountPercent: BigDecimal
    ): List<TransactionItem> {
        return items.map { item ->
            if (item.hasLineDiscount) {
                // Line discount takes precedence
                item
            } else {
                val discountAmount = item.priceUsed * (discountPercent / BigDecimal(100))
                item.copy(
                    transactionDiscountTypeId = DiscountType.TransactionPercentTotal,
                    transactionDiscountTypeAmount = discountPercent,
                    transactionDiscountAmountPerUnit = discountAmount
                )
            }
        }
    }
}
```

---

## Discount Validator

Validates discount eligibility:

```kotlin
class DiscountValidator {
    
    /**
     * Validate if a sale price is currently active.
     */
    fun isSalePriceValid(sale: ProductSalePrice): Boolean {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        
        return isAfterStartDate(sale, now) &&
               sale.active &&
               isWeekdayValid(sale, now.dayOfWeek) &&
               isTimeValid(sale, now.time)
    }
    
    private fun isAfterStartDate(sale: ProductSalePrice, now: LocalDateTime): Boolean {
        return now.date >= sale.startDate
    }
    
    private fun isWeekdayValid(sale: ProductSalePrice, dayOfWeek: DayOfWeek): Boolean {
        // Bitmask check for valid weekdays
        val dayBit = 1 shl dayOfWeek.ordinal
        return (sale.weekdayMask and dayBit) != 0
    }
    
    private fun isTimeValid(sale: ProductSalePrice, time: LocalTime): Boolean {
        if (sale.startTime == null || sale.endTime == null) return true
        
        // Handle overnight sales (e.g., 10 PM to 6 AM)
        return if (sale.startTime > sale.endTime) {
            time >= sale.startTime || time <= sale.endTime
        } else {
            time in sale.startTime..sale.endTime
        }
    }
    
    /**
     * Validate if a discount can be applied to an item.
     */
    fun canApplyDiscount(
        item: TransactionItem,
        discountType: DiscountType,
        discountValue: BigDecimal
    ): Boolean {
        // Check if item already has discount
        if (item.salePrice != null && item.salePrice > BigDecimal.ZERO) {
            return false
        }
        
        // Check discount limits
        if (discountType == DiscountType.ItemPercentage) {
            if (discountValue > BigDecimal(100)) return false
        }
        
        // Check if item is discountable
        if (item.isNonDiscountable) return false
        
        return true
    }
    
    /**
     * Check if discount would breach floor price.
     */
    fun wouldBreachFloorPrice(
        item: TransactionItem,
        discountAmount: BigDecimal
    ): Boolean {
        val newPrice = item.priceUsed - discountAmount
        return newPrice < item.floorPrice
    }
}
```

---

## Payment Service

Handles payment processing:

```kotlin
class PaymentService(
    private val paymentTerminal: PaymentTerminal,
    private val orderStore: OrderStore
) {
    
    /**
     * Process a cash payment.
     */
    fun processCashPayment(
        amountTendered: BigDecimal,
        amountDue: BigDecimal
    ): PaymentResponse {
        return if (amountTendered >= amountDue) {
            val change = amountTendered - amountDue
            PaymentResponse(
                status = PaymentStatus.APPROVED,
                approvedAmount = amountDue,
                changeAmount = change
            )
        } else {
            PaymentResponse(
                status = PaymentStatus.PARTIAL,
                approvedAmount = amountTendered,
                changeAmount = BigDecimal.ZERO
            )
        }
    }
    
    /**
     * Process a card payment via PAX terminal.
     */
    suspend fun processCardPayment(
        amount: BigDecimal,
        paymentType: PaymentType
    ): PaymentResponse {
        return try {
            val response = paymentTerminal.doSale(
                amount = amount,
                transactionType = paymentType.toTransactionType()
            )
            
            PaymentResponse(
                status = response.resultCode.toPaymentStatus(),
                approvedAmount = response.approvedAmount,
                authCode = response.authCode,
                cardType = response.cardType,
                lastFour = response.maskedCardNumber?.takeLast(4),
                referenceNumber = response.referenceNumber
            )
        } catch (e: Exception) {
            PaymentResponse(
                status = PaymentStatus.DECLINED,
                errorMessage = e.message
            )
        }
    }
    
    /**
     * Process EBT/SNAP payment.
     * Updates item tax calculations for SNAP-paid portions.
     */
    suspend fun processEBTPayment(
        amount: BigDecimal,
        ebtType: EBTType
    ): PaymentResponse {
        val response = paymentTerminal.doEBTSale(amount, ebtType)
        
        if (response.status == PaymentStatus.APPROVED && ebtType == EBTType.SNAP) {
            // Update items with SNAP payment allocation
            updateItemsAfterSNAPPayment(response.approvedAmount)
        }
        
        return response
    }
    
    /**
     * Update items after SNAP payment to recalculate tax.
     * SNAP payments reduce taxable amount.
     */
    private fun updateItemsAfterSNAPPayment(paymentAmount: BigDecimal) {
        val items = orderStore.orderItems.value
        val eligibleItems = items
            .filter { !it.isRemoved && it.isSNAPEligible && it.snapPaidPercent < BigDecimal(100) }
            .sortedByDescending { it.taxPercentSum }  // Prioritize taxable items
        
        var remaining = paymentAmount
        
        for (item in eligibleItems) {
            if (remaining <= BigDecimal.ZERO) break
            
            val unpaidAmount = item.subjectToTaxTotal
            val amountToApply = minOf(remaining, unpaidAmount)
            val paidPercent = (amountToApply / item.subTotal) * BigDecimal(100)
            
            // Update item with SNAP payment info
            orderStore.updateItemSNAPPayment(
                itemId = item.transactionGuid,
                snapPaidAmount = item.snapPaidAmount + amountToApply,
                snapPaidPercent = item.snapPaidPercent + paidPercent
            )
            
            remaining -= amountToApply
        }
    }
    
    /**
     * Void a previous payment.
     */
    suspend fun voidPayment(payment: Payment): PaymentResponse {
        return if (payment.paymentType == PaymentType.CASH) {
            PaymentResponse(status = PaymentStatus.APPROVED)
        } else {
            paymentTerminal.doVoid(payment.referenceNumber!!)
        }
    }
    
    /**
     * Calculate EBT-eligible amount (SNAP-eligible items).
     */
    fun calculateSNAPEligibleTotal(items: List<TransactionItem>): BigDecimal {
        return items
            .filter { !it.isRemoved && it.isSNAPEligible }
            .fold(BigDecimal.ZERO) { acc, item -> acc + item.subjectToTaxTotal }
            .setScale(2, RoundingMode.HALF_UP)
    }
}
```

---

## Print Service

Handles receipt printing:

```kotlin
class PrintService(
    private val printer: ReceiptPrinter
) {
    
    /**
     * Print a transaction receipt.
     */
    suspend fun printReceipt(
        transaction: Transaction,
        items: List<TransactionItem>,
        payments: List<Payment>,
        branch: Branch
    ): Boolean {
        val receipt = buildReceipt(transaction, items, payments, branch)
        return printer.print(receipt, transaction.receiptBarcode)
    }
    
    private fun buildReceipt(
        transaction: Transaction,
        items: List<TransactionItem>,
        payments: List<Payment>,
        branch: Branch
    ): Receipt {
        return Receipt(
            header = ReceiptHeader(
                storeName = branch.name,
                storeAddress = branch.address,
                storePhone = branch.phone,
                dateTime = transaction.transactionDate,
                cashierName = transaction.employeeName,
                receiptNumber = transaction.receiptNumber
            ),
            items = items.map { item ->
                ReceiptItem(
                    name = item.productName,
                    quantity = item.quantityUsed,
                    price = item.finalPrice,
                    total = item.subTotal,
                    taxIndicator = item.taxIndicator,
                    savings = item.savingsTotal.takeIf { it > BigDecimal.ZERO }
                )
            },
            totals = ReceiptTotals(
                subtotal = transaction.subTotal,
                taxTotal = transaction.taxTotal,
                crvTotal = transaction.crvTotal,
                savingsTotal = transaction.savingsTotal,
                grandTotal = transaction.grandTotal
            ),
            payments = payments.map { payment ->
                ReceiptPayment(
                    type = payment.paymentType.displayName,
                    amount = payment.amount,
                    cardType = payment.cardType,
                    lastFour = payment.lastFour
                )
            },
            changeDue = transaction.changeDue,
            barcode = transaction.receiptBarcode
        )
    }
    
    /**
     * Print cash pickup receipt.
     */
    suspend fun printCashPickupReceipt(pickup: CashPickup): Boolean {
        // Build and print pickup receipt
        return printer.print(buildCashPickupReceipt(pickup))
    }
    
    /**
     * Print held transaction receipt.
     */
    suspend fun printHoldReceipt(transaction: Transaction): Boolean {
        return printer.print(buildHoldReceipt(transaction))
    }
}
```

---

## Koin Module Definition

```kotlin
val serviceModule = module {
    // Calculators
    single { PriceCalculator() }
    single { TaxCalculator() }
    single { CRVCalculator(get()) }
    single { DiscountCalculator() }
    single { DiscountValidator() }
    
    // Business services
    single { PaymentService(get(), get()) }
    single { PrintService(get()) }
}
```

---

## Usage in ViewModels

```kotlin
class HomeViewModel(
    private val orderStore: OrderStore,
    private val priceCalculator: PriceCalculator,
    private val taxCalculator: TaxCalculator,
    private val crvCalculator: CRVCalculator,
    private val discountCalculator: DiscountCalculator
) : ViewModel() {
    
    /**
     * Recalculate all totals when items change.
     */
    val transactionTotals: StateFlow<TransactionTotals> = orderStore.activeItems.map { items ->
        TransactionTotals(
            subtotal = priceCalculator.calculateSubtotal(items),
            taxTotal = taxCalculator.calculateTotalTax(items),
            crvTotal = crvCalculator.calculateTotalCRV(items),
            savingsTotal = discountCalculator.calculateTotalSavings(items),
            grandTotal = priceCalculator.calculateGrandTotal(items),
            itemCount = items.size,
            uniqueProductCount = items.distinctBy { it.branchProductId }.size
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, TransactionTotals.EMPTY)
}
```

---

## Testing Services

```kotlin
class PriceCalculatorTest {
    
    private val calculator = PriceCalculator()
    
    @Test
    fun `calculateSubtotal returns sum of active items`() {
        val items = listOf(
            createItem(finalPrice = 10.00.toBigDecimal(), quantity = 2),
            createItem(finalPrice = 5.00.toBigDecimal(), quantity = 1),
            createItem(finalPrice = 100.00.toBigDecimal(), quantity = 1, isRemoved = true)
        )
        
        val result = calculator.calculateSubtotal(items)
        
        assertEquals(25.00.toBigDecimal(), result)
    }
    
    @Test
    fun `getFinalPrice respects floor price`() {
        val item = createItem(
            priceUsed = 10.00.toBigDecimal(),
            discountAmountPerUnit = 8.00.toBigDecimal(),
            floorPrice = 5.00.toBigDecimal()
        )
        
        val result = calculator.getFinalPrice(item)
        
        assertEquals(5.00.toBigDecimal(), result)  // Floor enforced
    }
}

class TaxCalculatorTest {
    
    private val calculator = TaxCalculator()
    
    @Test
    fun `SNAP payment reduces taxable amount`() {
        val item = createItem(
            finalPrice = 10.00.toBigDecimal(),
            taxPercentSum = 10.00.toBigDecimal(),  // 10%
            quantityUsed = 1.toBigDecimal(),
            snapPaidPercent = 50.00.toBigDecimal()  // 50% SNAP
        )
        
        val result = calculator.calculateItemTax(item)
        
        // Tax on 50% = $10 * 10% * 0.5 = $0.50
        assertEquals(0.50.toBigDecimal(), result)
    }
}
```

---

## Related Documentation

- [../features/BUSINESS_RULES.md](../features/BUSINESS_RULES.md) - Business rules
- [../features/advanced-calculations/](../features/advanced-calculations/) - Detailed calculations
- [../architecture/STATE_MANAGEMENT.md](../architecture/STATE_MANAGEMENT.md) - State patterns
- [STORES.md](./STORES.md) - Store implementations

---

*Last Updated: January 2026*

