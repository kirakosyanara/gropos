# Transaction Flow

> Complete lifecycle of a transaction in GroPOS (Kotlin/Compose implementation)

---

## Transaction States

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Empty     │────▶│   Active    │────▶│   Payment   │────▶│  Complete   │
│             │     │   (Items)   │     │  Processing │     │             │
└─────────────┘     └──────┬──────┘     └─────────────┘     └─────────────┘
                           │
                    ┌──────┴──────┐
                    ▼             ▼
             ┌─────────────┐ ┌─────────────┐
             │    Hold     │ │    Void     │
             │             │ │             │
             └─────────────┘ └─────────────┘
```

---

## Flow Overview

### 1. Start Transaction

```kotlin
class HomeViewModel(
    private val orderStore: OrderStore,
    private val appStore: AppStore
) : ViewModel() {
    
    fun addFirstItem(product: Product) {
        viewModelScope.launch {
            // Create new transaction
            val transaction = Transaction(
                guid = UUID.randomUUID().toString(),
                startDate = Clock.System.now(),
                cashierId = appStore.currentEmployee.value?.id ?: 0,
                storeId = appStore.currentBranch.value?.id ?: 0,
                registerId = appStore.deviceInfo.value?.registerId ?: 0
            )
            
            orderStore.setTransaction(transaction)
            
            // Add first item
            addItem(product)
        }
    }
}
```

### 2. Add Items

```kotlin
fun addItem(product: Product, quantity: BigDecimal = BigDecimal.ONE) {
    viewModelScope.launch {
        val currentItems = orderStore.orderItems.value
        
        // Check if product already in order
        val existingItem = currentItems.find { 
            it.branchProductId == product.branchProductId && !product.isWeighted 
        }
        
        if (existingItem != null) {
            // Increment quantity
            orderStore.updateItemQuantity(
                itemId = existingItem.transactionItemGuid,
                newQuantity = existingItem.quantityUsed + quantity
            )
        } else {
            // Create new item
            val item = buildTransactionItem(product, quantity)
            orderStore.addItem(item)
        }
        
        // Recalculate totals
        recalculateTotals()
    }
}

private fun buildTransactionItem(
    product: Product,
    quantity: BigDecimal
): TransactionItem {
    val priceUsed = priceCalculator.getPriceUsed(product)
    val crvRate = crvCalculator.getCRVRate(product)
    val finalPrice = priceUsed + crvRate
    val taxPercentSum = product.taxes.sumOf { it.percent }
    val taxPerUnit = taxCalculator.calculateTaxPerUnit(finalPrice, taxPercentSum)
    
    return TransactionItem(
        transactionGuid = orderStore.currentTransaction.value?.guid ?: "",
        transactionItemGuid = UUID.randomUUID().toString(),
        branchProductId = product.branchProductId,
        branchProductName = product.name,
        itemNumber = product.barcode,
        retailPrice = product.retailPrice,
        salePrice = product.salePrice,
        priceUsed = priceUsed,
        floorPrice = product.floorPrice,
        cost = product.cost,
        crvRatePerUnit = crvRate,
        quantityUsed = quantity,
        finalPrice = finalPrice,
        taxPercentSum = taxPercentSum,
        taxPerUnit = taxPerUnit,
        taxTotal = (taxPerUnit * quantity).toMoney(),
        subTotal = (finalPrice * quantity).toMoney(),
        subjectToTaxTotal = (finalPrice * quantity).toMoney(),
        isSNAPEligible = product.isSNAPEligible,
        isWICEligible = product.isWICApproved,
        ageRestriction = product.ageRestriction,
        isWeighted = product.isWeighted
    )
}
```

### 3. Modify Items

```kotlin
// Change quantity
fun changeQuantity(itemId: String, newQuantity: BigDecimal) {
    if (newQuantity <= BigDecimal.ZERO) {
        voidItem(itemId)
    } else {
        orderStore.updateItemQuantity(itemId, newQuantity)
        recalculateTotals()
    }
}

// Void item (mark as removed)
fun voidItem(itemId: String) {
    orderStore.removeItem(itemId)
    recalculateTotals()
}

// Apply discount
fun applyItemDiscount(itemId: String, discountAmount: BigDecimal) {
    orderStore.updateItem(itemId) { item ->
        val newPrice = (item.priceUsed - discountAmount).coerceAtLeast(BigDecimal.ZERO)
        item.copy(
            discountAmountPerUnit = discountAmount,
            discountTypeId = DiscountType.ItemAmountPerUnit,
            // Recalculate derived fields
            finalPrice = newPrice + item.crvRatePerUnit
        )
    }
    recalculateTotals()
}
```

### 4. Calculate Totals

```kotlin
fun recalculateTotals() {
    val items = orderStore.orderItems.value.filter { !it.isRemoved }
    
    // Calculate subtotal
    val subtotal = priceCalculator.calculateSubtotal(items)
    
    // Calculate savings
    val savings = discountCalculator.calculateTotalSavings(items)
    
    // Calculate tax
    val tax = taxCalculator.calculateTotalTax(items)
    
    // Calculate CRV
    val crv = crvCalculator.calculateTotalCRV(items)
    
    // Calculate grand total
    val grandTotal = (subtotal + tax).toMoney()
    
    // Update transaction
    orderStore.updateTransaction { transaction ->
        transaction.copy(
            subTotal = subtotal,
            savingsTotal = savings,
            taxTotal = tax,
            crvTotal = crv,
            grandTotal = grandTotal,
            rowCount = orderStore.calculateRowCount(),
            itemCount = orderStore.calculateItemCount(),
            totalPurchaseCount = orderStore.calculateTotalPurchaseCount(),
            uniqueProductCount = orderStore.calculateUniqueProductCount()
        )
    }
}
```

### 5. Process Payment

```kotlin
class PayViewModel(
    private val orderStore: OrderStore,
    private val paymentService: PaymentService
) : ViewModel() {
    
    private val _amountDue = MutableStateFlow(BigDecimal.ZERO)
    val amountDue: StateFlow<BigDecimal> = _amountDue.asStateFlow()
    
    fun processPayment(type: PaymentType, amount: BigDecimal) {
        viewModelScope.launch {
            val response = when (type) {
                PaymentType.CASH -> 
                    paymentService.processCashPayment(amount, _amountDue.value)
                    
                PaymentType.CREDIT, PaymentType.DEBIT ->
                    paymentService.processCardPayment(amount, type)
                    
                PaymentType.EBT_SNAP, PaymentType.EBT_CASH ->
                    paymentService.processEBTPayment(amount, type.toEBTType())
                    
                else -> throw IllegalArgumentException("Unknown payment type")
            }
            
            if (response.status == PaymentStatus.Success) {
                // Create payment record
                val payment = Payment(
                    paymentType = type,
                    amount = response.approvedAmount,
                    authCode = response.authCode,
                    referenceNumber = response.referenceNumber,
                    cardType = response.cardType,
                    lastFour = response.lastFour
                )
                orderStore.addPayment(payment)
                
                // Update amount due
                updateAmountDue()
                
                // Check if fully paid
                if (_amountDue.value <= BigDecimal.ZERO) {
                    completeTransaction(response.changeAmount)
                }
            } else {
                _paymentError.value = response.errorMessage
            }
        }
    }
    
    private fun updateAmountDue() {
        val grandTotal = orderStore.currentTransaction.value?.grandTotal ?: BigDecimal.ZERO
        val totalPaid = orderStore.payments.value.sumOf { it.amount }
        _amountDue.value = (grandTotal - totalPaid).coerceAtLeast(BigDecimal.ZERO)
    }
}
```

### 6. Complete Transaction

```kotlin
suspend fun completeTransaction(changeAmount: BigDecimal) {
    val transaction = orderStore.currentTransaction.value ?: return
    val items = orderStore.orderItems.value
    val payments = orderStore.payments.value
    
    // Calculate cost total
    val costTotal = items
        .filter { !it.isRemoved }
        .sumOf { it.costTotal }
    
    // Map to API request
    val request = TransactionMapper.toCreateRequest(
        transaction = transaction.copy(
            status = TransactionStatus.Completed,
            completedDate = Clock.System.now(),
            costTotal = costTotal
        ),
        items = items,
        payments = payments
    )
    
    try {
        // Save to API
        val response = transactionRepository.createTransaction(request)
        
        // Store receipt number
        appStore.setLastReceipt(
            receipt = response.receiptData,
            barcode = response.receiptNumber
        )
        
        // Print receipt
        printService.printReceipt(transaction, items, payments)
        
        // Show change
        if (changeAmount > BigDecimal.ZERO) {
            orderStore.setChangeDue(changeAmount)
            _showChangeDialog.value = true
        }
        
        // Clear and navigate home
        clearTransaction()
        navigator.navigateTo(Screen.Home)
        
    } catch (e: Exception) {
        _transactionError.value = e.message
    }
}
```

### 7. Hold Transaction

```kotlin
suspend fun holdTransaction(holdName: String?) {
    val transaction = orderStore.currentTransaction.value ?: return
    recalculateTotals()
    
    val holdRequest = TransactionHoldRequest(
        holdName = holdName ?: generateHoldName(),
        transaction = TransactionMapper.toRequest(transaction),
        items = TransactionMapper.toItemRequests(orderStore.orderItems.value)
    )
    
    try {
        val response = transactionRepository.holdTransaction(holdRequest)
        
        if (response.holdId != 0) {
            // Print hold receipt
            printService.printHoldReceipt(transaction)
            
            // Clear and navigate
            clearTransaction()
            navigator.navigateTo(Screen.Home)
        }
    } catch (e: Exception) {
        _error.value = "Failed to hold transaction: ${e.message}"
    }
}

private fun generateHoldName(): String {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    return "Hold-${now.hour}:${now.minute}"
}
```

### 8. Recall Transaction

```kotlin
suspend fun recallTransaction(hold: TransactionHold) {
    // Clear current order
    clearTransaction()
    
    // Load held transaction
    val transaction = TransactionMapper.fromHold(hold)
    orderStore.setTransaction(transaction)
    
    // Load items
    hold.items.forEach { holdItem ->
        val orderItem = TransactionMapper.fromHoldItem(holdItem)
        orderStore.addItem(orderItem)
    }
    
    // Recalculate totals
    recalculateTotals()
    
    // Delete the hold
    transactionRepository.deleteHold(hold.holdId)
    
    // Navigate to home
    navigator.navigateTo(Screen.Home)
}
```

### 9. Void Transaction

```kotlin
suspend fun voidTransaction() {
    // Require manager approval
    val approved = requestManagerApproval(ApprovalType.VOID_TRANSACTION)
    if (!approved) return
    
    // Void any card payments
    for (payment in orderStore.payments.value) {
        if (payment.paymentType != PaymentType.CASH) {
            val response = paymentService.voidPayment(payment)
            if (response.status != PaymentStatus.Success) {
                _error.value = "Failed to void payment: ${response.errorMessage}"
                return
            }
        }
    }
    
    // Clear transaction
    clearTransaction()
    navigator.navigateTo(Screen.Home)
}

private fun clearTransaction() {
    orderStore.clear()
}
```

---

## Transaction Data Model

### Transaction

```kotlin
data class Transaction(
    val guid: String,
    val startDate: Instant,
    val completedDate: Instant? = null,
    val employeeId: Int,
    val branchId: Int,
    val status: TransactionStatus = TransactionStatus.InProgress,
    val subTotal: BigDecimal = BigDecimal.ZERO,
    val taxTotal: BigDecimal = BigDecimal.ZERO,
    val crvTotal: BigDecimal = BigDecimal.ZERO,
    val savingsTotal: BigDecimal = BigDecimal.ZERO,
    val grandTotal: BigDecimal = BigDecimal.ZERO,
    val rowCount: Int = 0,
    val itemCount: Int = 0,
    val totalPurchaseCount: BigDecimal = BigDecimal.ZERO,
    val uniqueProductCount: Int = 0
)
```

### TransactionItem

See [CORE_CONCEPTS.md](./advanced-calculations/CORE_CONCEPTS.md) for the complete TransactionItem model.

---

## UI Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              HomeScreen                                      │
│  ┌─────────────────────────────────────┐  ┌────────────────────────────┐    │
│  │         Order List                   │  │      Function Panel        │    │
│  │                                      │  │                            │    │
│  │  - Product 1        $5.99 x2        │  │  [Qty] [Void] [Discount]   │    │
│  │  - Product 2        $3.49 x1        │  │  [Lookup] [Hold] [Pay]     │    │
│  │  - Product 3        $12.99 x1       │  │                            │    │
│  │                                      │  │                            │    │
│  ├──────────────────────────────────────┤  │                            │    │
│  │  Subtotal:           $28.46         │  │                            │    │
│  │  Tax:                 $2.28         │  │                            │    │
│  │  Total:              $30.74         │  │                            │    │
│  └──────────────────────────────────────┘  └────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                              [Pay Button]
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                               PayScreen                                      │
│  ┌─────────────────────────────────────┐  ┌────────────────────────────┐    │
│  │       Payment Methods               │  │      Amount Entry          │    │
│  │                                      │  │                            │    │
│  │  [Cash] [Credit] [Debit] [EBT]      │  │      $ 30.74              │    │
│  │                                      │  │                            │    │
│  │  Payments Applied:                   │  │  [7] [8] [9]              │    │
│  │  - Cash: $20.00                     │  │  [4] [5] [6]              │    │
│  │                                      │  │  [1] [2] [3]              │    │
│  │  Amount Due: $10.74                 │  │  [0] [.] [Enter]          │    │
│  └──────────────────────────────────────┘  └────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Compose Implementation

### HomeScreen

```kotlin
@Composable
fun HomeScreen(viewModel: HomeViewModel = koinViewModel()) {
    val orderItems by viewModel.orderItems.collectAsState()
    val totals by viewModel.transactionTotals.collectAsState()
    val showAgeVerification by viewModel.showAgeVerification.collectAsState()
    
    Row(modifier = Modifier.fillMaxSize()) {
        // Order Panel (left side)
        Column(
            modifier = Modifier
                .weight(0.6f)
                .fillMaxHeight()
        ) {
            OrderHeader(
                itemCount = totals.itemCount,
                employee = viewModel.currentEmployee.collectAsState().value
            )
            
            OrderItemList(
                items = orderItems,
                onItemClick = viewModel::selectItem,
                onRemoveItem = viewModel::voidItem,
                modifier = Modifier.weight(1f)
            )
            
            TotalsPanel(
                subtotal = totals.subtotal,
                tax = totals.taxTotal,
                savings = totals.savingsTotal,
                total = totals.grandTotal
            )
        }
        
        // Function Panel (right side)
        FunctionPanel(
            onQuantityClick = { /* ... */ },
            onVoidClick = { /* ... */ },
            onDiscountClick = { /* ... */ },
            onLookupClick = { /* ... */ },
            onHoldClick = viewModel::holdTransaction,
            onPayClick = { navigator.navigateTo(Screen.Pay) },
            modifier = Modifier.weight(0.4f)
        )
    }
    
    // Dialogs
    if (showAgeVerification != null) {
        AgeVerificationDialog(
            ageType = showAgeVerification!!,
            onVerified = viewModel::onAgeVerified,
            onDenied = viewModel::onAgeVerificationDenied
        )
    }
}
```

---

## Related Documentation

- [BUSINESS_RULES.md](./BUSINESS_RULES.md) - Validation rules
- [PAYMENT_PROCESSING.md](./PAYMENT_PROCESSING.md) - Payment workflows
- [RETURNS.md](./RETURNS.md) - Return processing
- [../architecture/STATE_MANAGEMENT.md](../architecture/STATE_MANAGEMENT.md) - State patterns
- [../modules/SERVICES.md](../modules/SERVICES.md) - Calculator services

---

*Last Updated: January 2026*

