# GroPOS Business Rules Reference

> Consolidated validation rules and business logic constraints for Kotlin/Compose implementation

---

## Table of Contents

- [Product Rules](#product-rules)
- [Transaction Rules](#transaction-rules)
- [Payment Rules](#payment-rules)
- [Discount Rules](#discount-rules)
- [Tax Rules](#tax-rules)
- [Return Rules](#return-rules)
- [Manager Approval Rules](#manager-approval-rules)
- [Cash Drawer Rules](#cash-drawer-rules)
- [Calculation Formulas](#calculation-formulas)
- [Session Rules](#session-rules)

---

## Product Rules

### Quantity Limits

| Rule | Value | Enforcement |
|------|-------|-------------|
| Minimum quantity | 1 | Cannot add 0 quantity |
| Maximum quantity | 99 | Hard limit per line item |
| Weight minimum | 0.01 lb | Below triggers error dialog |
| Weight maximum | 30.0 lb | Scale capacity |

### Sold By Types

| Type | Behavior |
|------|----------|
| `Quantity` | Standard quantity entry |
| `PromptForQty` | Ask for quantity before adding |
| `PromptForPrice` | Ask for price (open-price items) |
| `WeightOnScale` | Must be weighed on scale |
| `WeightOnScalePostTare` | Weigh after tare weight |
| `EmbeddedBarcode` | Price embedded in barcode |
| `QuantityEmbeddedBarcode` | Quantity embedded in barcode |

```kotlin
enum class SoldByType {
    Quantity,
    PromptForQty,
    PromptForPrice,
    WeightOnScale,
    WeightOnScalePostTare,
    EmbeddedBarcode,
    QuantityEmbeddedBarcode
}
```

### Age Restrictions

| Type | Age Required | Products |
|------|--------------|----------|
| `NONE` | None | Standard products |
| `AGE_18` | 18+ | Tobacco, vape |
| `AGE_21` | 21+ | Alcohol |

```kotlin
enum class AgeType { NONE, AGE_18, AGE_21 }

fun requiresAgeVerification(product: Product): Boolean {
    return product.ageRestriction != AgeType.NONE
}
```

**Verification:** Customer birthdate checked, popup shown on scan.

### SNAP Eligibility

```kotlin
// Product must have isSNAPEligible = true
// AND payment must be EBT SNAP (Food Stamp)
// Tax is NOT applied to SNAP-paid portion

data class Product(
    // ...
    val isSNAPEligible: Boolean,  // Eligible for EBT SNAP payment
    // ...
)

fun isEligibleForSNAP(item: TransactionItem): Boolean {
    return item.isSNAPEligible && !item.isRemoved
}
```

---

## Transaction Rules

### Transaction States

| State | Allowed Actions |
|-------|-----------------|
| `InProgress` | Add items, apply discounts, modify |
| `OnHold` | Recall to resume |
| `Completed` | Return only |
| `Voided` | No actions |

```kotlin
enum class TransactionStatus {
    InProgress,
    OnHold,
    Completed,
    Voided
}

fun canModifyTransaction(status: TransactionStatus): Boolean {
    return status == TransactionStatus.InProgress
}
```

### Hold/Recall Rules

- Cannot hold if payments applied
- Hold name optional (defaults to timestamp)
- Recall list shows last year's holds
- Recalled items restore to `InProgress`

```kotlin
fun canHoldTransaction(payments: List<Payment>): Boolean {
    return payments.isEmpty()
}
```

### Void Rules

- Cannot void if any payment applied
- All items removed on void
- Transaction marked as `Voided`

```kotlin
fun canVoidTransaction(payments: List<Payment>): Boolean {
    return payments.isEmpty()
}
```

---

## Payment Rules

### Payment Order

1. EBT SNAP (applied to SNAP-eligible items first)
2. EBT Cash Benefit
3. Credit/Debit
4. Cash
5. Other (Check, On Account)

```kotlin
enum class PaymentType {
    EBT_SNAP,      // Applied first to SNAP-eligible items
    EBT_CASH,      // Applied second
    CREDIT,        // Card payments
    DEBIT,
    CASH,          // Applied last
    CHECK
}
```

### Split Tender

```
Grand Total = $100.00
SNAP Eligible = $60.00

EBT SNAP = $50.00       → Applied to SNAP items
EBT Cash = $20.00       → Applied to remaining
Credit = $30.00         → Applied to remaining
─────────────────────────
Remaining = $0.00
```

### Tax on SNAP

```kotlin
// Tax calculated on non-SNAP portion
// snapPaidPercent = (SNAP payment / SNAP eligible) * 100
// taxableAmount = totalAmount * (1 - snapPaidPercent)

fun calculateAdjustedTax(
    taxTotal: BigDecimal,
    snapPaidPercent: BigDecimal
): BigDecimal {
    val snapFraction = snapPaidPercent.divide(BigDecimal(100))
    return taxTotal * (BigDecimal.ONE - snapFraction)
}
```

### Cash Rules

| Rule | Value |
|------|-------|
| Quick cash buttons | $1, $5, $10, $20, $50, $100 |
| Maximum cash tendered | No limit |
| Change due | Calculated automatically |
| Drawer opens on | Any cash in transaction |

### Card Payment Rules

| Type | Terminal Action |
|------|-----------------|
| Credit | Route to PAX, chip preferred |
| Debit | Route to PAX, PIN required |
| EBT | Route to PAX, PIN required |

---

## Discount Rules

### Line Item Discounts

| Type | Limit | Approval |
|------|-------|----------|
| Percentage | 0-99% | Manager if > threshold |
| Dollar amount | Floor price | Manager if below floor |

### Price Change Rules

```kotlin
fun requiresApproval(newPrice: BigDecimal, product: Product): ApprovalType? {
    return when {
        newPrice < product.floorPrice -> ApprovalType.FLOOR_PRICE_OVERRIDE
        else -> null  // No approval needed
    }
}
```

### Transaction (Invoice) Discount

| Rule | Value |
|------|-------|
| Maximum | 99% |
| Approval | Always requires manager |
| Application | Applied to all items proportionally |

### Discount Stacking

```
Order of application:
1. Sale price (if active)
2. Promotion price (Mix & Match, BOGO)
3. Line item discount
4. Transaction discount

Discounts compound, not additive.
```

```kotlin
// Line discounts override invoice discounts
fun applyDiscounts(item: TransactionItem, invoiceDiscount: BigDecimal): TransactionItem {
    return if (item.hasLineDiscount) {
        // Line discount takes precedence, no invoice discount
        item
    } else {
        // Apply invoice discount
        item.copy(
            transactionDiscountAmountPerUnit = item.priceUsed * (invoiceDiscount / 100)
        )
    }
}
```

---

## Tax Rules

### Tax Calculation Formula

```kotlin
// Tax is calculated on (finalPrice) which includes CRV
// SNAP-paid portions are tax-exempt

fun calculateTax(
    finalPrice: BigDecimal,
    taxPercent: BigDecimal,
    snapPaidPercent: BigDecimal = BigDecimal.ZERO
): BigDecimal {
    val baseTax = finalPrice * taxPercent / BigDecimal(100)
    val snapFraction = snapPaidPercent / BigDecimal(100)
    return (baseTax * (BigDecimal.ONE - snapFraction))
        .setScale(2, RoundingMode.HALF_UP)
}
```

### Multi-Tax Support

- Products can have multiple taxes (e.g., state + local)
- Each tax calculated independently
- Displayed as separate lines on receipt

```kotlin
data class TaxBreakdown(
    val taxId: Int,
    val taxName: String,
    val taxRate: BigDecimal,
    val amount: BigDecimal
)

fun calculateTaxBreakdown(
    item: TransactionItem,
    taxes: List<Tax>
): List<TaxBreakdown> {
    val onePercentValue = item.taxTotal / item.taxPercentSum
    return taxes.map { tax ->
        TaxBreakdown(
            taxId = tax.id,
            taxName = tax.name,
            taxRate = tax.percent,
            amount = onePercentValue * tax.percent
        )
    }
}
```

### Tax Types

| Indicator | Meaning |
|-----------|---------|
| `T` | Taxable |
| `F` | Food (SNAP-eligible, may be taxable) |
| `N` | Non-taxable |
| ` ` | No indicator |

---

## Return Rules

### Return Eligibility

| Condition | Allowed |
|-----------|---------|
| Original receipt | ✓ Required |
| Within return window | ✓ Store configurable |
| Same payment method | ✓ Preferred |
| Manager approval | ✓ If above threshold |

### Return Restrictions

- No returns during active transaction (cart must be empty)
- EBT returns must go back to EBT
- Cash refunds may require manager approval

```kotlin
fun canProcessReturn(currentOrder: List<TransactionItem>): Boolean {
    return currentOrder.isEmpty()
}

fun getRefundPaymentType(originalPayment: PaymentType): PaymentType {
    return when (originalPayment) {
        PaymentType.EBT_SNAP -> PaymentType.EBT_SNAP  // Must refund to EBT
        PaymentType.EBT_CASH -> PaymentType.EBT_CASH
        else -> originalPayment
    }
}
```

### Return Flow

1. Scan receipt barcode
2. Select items to return
3. Enter return reason
4. Manager approval (if required)
5. Process refund to original payment

---

## Manager Approval Rules

### Approval Required Actions

| Action | Trigger |
|--------|---------|
| `LINE_DISCOUNT` | Discount exceeds threshold |
| `FLOOR_PRICE_OVERRIDE` | Price below floor |
| `TRANSACTION_DISCOUNT` | Any invoice discount |
| `ADD_CASH` | Adding cash to drawer |
| `HIGH_VALUE_RETURN` | Return over threshold |

```kotlin
enum class ApprovalType {
    LINE_DISCOUNT,
    FLOOR_PRICE_OVERRIDE,
    TRANSACTION_DISCOUNT,
    ADD_CASH,
    HIGH_VALUE_RETURN,
    VOID_TRANSACTION
}
```

### Approval Process

1. Action triggers approval panel
2. Manager selects their name
3. Manager enters PIN
4. System validates PIN
5. Action proceeds or denied

```kotlin
suspend fun requestManagerApproval(
    approvalType: ApprovalType,
    onApproved: () -> Unit
) {
    // Show approval dialog
    val result = showApprovalDialog(approvalType)
    if (result.approved) {
        // Log approval
        logApproval(approvalType, result.managerId)
        onApproved()
    }
}
```

### Manager PIN Rules

- PIN length: Configurable (typically 4-6 digits)
- PIN storage: Hashed on server (never stored locally)
- Failed attempts: Logged for audit

---

## Cash Drawer Rules

### Auto-Open Triggers

| Event | Opens Drawer |
|-------|--------------|
| Cash in transaction | ✓ |
| End of shift | ✓ |
| Open Drawer function | ✓ |
| No cash transaction | ✗ |

### Drawer State Blocking

- Drawer open blocks all scanning
- "Close drawer to continue" dialog shown
- State detected via hardware callback

```kotlin
class DrawerStateHandler {
    private val _drawerOpen = MutableStateFlow(false)
    val drawerOpen: StateFlow<Boolean> = _drawerOpen.asStateFlow()
    
    fun onDrawerOpened() {
        _drawerOpen.value = true
    }
    
    fun onDrawerClosed() {
        _drawerOpen.value = false
    }
    
    fun canScan(): Boolean = !_drawerOpen.value
}
```

### Cash Tracking

```
Starting Cash + Sales - Pickups - Payouts = Expected Cash
Actual Cash - Expected Cash = Over/Short
```

---

## Calculation Formulas

### Subtotal Calculation

```kotlin
fun calculateSubtotal(items: List<TransactionItem>): BigDecimal {
    return items
        .filter { !it.isRemoved }
        .sumOf { it.finalPrice * it.quantityUsed }
        .setScale(2, RoundingMode.HALF_UP)
}
```

### Grand Total Calculation

```kotlin
fun calculateGrandTotal(
    subtotal: BigDecimal,
    taxTotal: BigDecimal,
    crvTotal: BigDecimal,
    bagFee: BigDecimal = BigDecimal.ZERO
): BigDecimal {
    return (subtotal + taxTotal + crvTotal + bagFee)
        .setScale(2, RoundingMode.HALF_UP)
}
```

### Savings Calculation

```kotlin
fun calculateSavings(items: List<TransactionItem>): BigDecimal {
    return items
        .filter { !it.isRemoved }
        .sumOf { (it.retailPrice - it.finalPrice + it.crvAmount) * it.quantityUsed }
        .coerceAtLeast(BigDecimal.ZERO)
        .setScale(2, RoundingMode.HALF_UP)
}
```

### SNAP Eligible Calculation

```kotlin
fun calculateSNAPEligible(items: List<TransactionItem>): BigDecimal {
    return items
        .filter { !it.isRemoved && it.isSNAPEligible }
        .sumOf { it.subTotal }
        .setScale(2, RoundingMode.HALF_UP)
}
```

### Remaining Calculation

```kotlin
fun calculateRemaining(
    grandTotal: BigDecimal,
    payments: List<Payment>
): BigDecimal {
    val totalPaid = payments.sumOf { it.amount }
    return (grandTotal - totalPaid).setScale(2, RoundingMode.HALF_UP)
}
```

### Change Due Calculation

```kotlin
fun calculateChangeDue(
    cashTendered: BigDecimal,
    remaining: BigDecimal
): BigDecimal {
    return if (remaining <= BigDecimal.ZERO) {
        BigDecimal.ZERO
    } else {
        (cashTendered - remaining).coerceAtLeast(BigDecimal.ZERO)
    }
}
```

---

## Rounding Rules

All monetary calculations use:
- `RoundingMode.HALF_UP`
- Scale of 2 decimal places

```kotlin
fun BigDecimal.toMoney(): BigDecimal = 
    this.setScale(2, RoundingMode.HALF_UP)

// Tax percentages use 3 decimal places internally
fun BigDecimal.toTaxPercent(): BigDecimal = 
    this.setScale(3, RoundingMode.HALF_UP)
```

---

## Session Rules

### Inactivity Timeout

| Setting | Value |
|---------|-------|
| Timeout | 5 minutes |
| Action | Auto-lock screen |
| Exception | Payment in progress |

```kotlin
private const val INACTIVITY_TIMEOUT_MS = 5 * 60 * 1000L  // 5 minutes

fun shouldAutoLock(
    lastActivityTime: Long,
    hasPaymentsInProgress: Boolean
): Boolean {
    if (hasPaymentsInProgress) return false
    val elapsed = System.currentTimeMillis() - lastActivityTime
    return elapsed >= INACTIVITY_TIMEOUT_MS
}
```

### Login Rules

- Employee must be "on-site"
- PIN or NFC authentication
- Session stored in AppStore
- Logout clears all state

### End of Shift

1. All transactions complete
2. No items in cart
3. Cash drawer count
4. Shift report printed
5. Session ended

```kotlin
fun canEndShift(
    currentItems: List<TransactionItem>,
    pendingTransactions: Int
): Boolean {
    return currentItems.isEmpty() && pendingTransactions == 0
}
```

---

## Related Documentation

- [TRANSACTION_FLOW.md](./TRANSACTION_FLOW.md) - Transaction lifecycle
- [PAYMENT_PROCESSING.md](./PAYMENT_PROCESSING.md) - Payment workflows
- [advanced-calculations/](./advanced-calculations/) - Detailed calculation specs
- [../architecture/STATE_MANAGEMENT.md](../architecture/STATE_MANAGEMENT.md) - State patterns

---

*Last Updated: January 2026*

