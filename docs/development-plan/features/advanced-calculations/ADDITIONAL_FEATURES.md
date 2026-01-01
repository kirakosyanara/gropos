# Additional Features and Operational Procedures

[← Back to Index](./INDEX.md)

---

## Overview

This document provides comprehensive coverage of operational features, edge cases, and procedures that complement the core calculation logic in GroPOS. It covers everything from cash drawer management to end-of-day reconciliation.

---

## Table of Contents

1. [Cash Drawer Management](#cash-drawer-management)
2. [Register Operations](#register-operations)
3. [Cashier Management](#cashier-management)
4. [Age-Restricted Items](#age-restricted-items)
5. [Quantity Limits](#quantity-limits)
6. [Weighted/Scale Items](#weightedscale-items)
7. [Gift Cards](#gift-cards)
8. [Store Credit](#store-credit)
9. [Void Operations](#void-operations)
10. [Suspend and Resume](#suspend-and-resume)
11. [Price Override](#price-override)
12. [Tax Exempt Sales](#tax-exempt-sales)
13. [Offline Mode](#offline-mode)
14. [Training Mode](#training-mode)
15. [Manager Operations](#manager-operations)
16. [End of Day Procedures](#end-of-day-procedures)

---

## Cash Drawer Management

### Cash Drawer Data Model

```kotlin
data class CashDrawer(
    val id: Int,
    val registerId: Int,
    val drawerNumber: String,
    
    // Status
    var status: DrawerStatus,               // CLOSED, OPEN, COUNTED
    var openedAt: OffsetDateTime? = null,
    var closedAt: OffsetDateTime? = null,
    var openedBy: Int? = null,
    var closedBy: Int? = null,
    
    // Amounts
    var openingFloat: BigDecimal = BigDecimal.ZERO,
    var expectedCash: BigDecimal = BigDecimal.ZERO,
    var actualCash: BigDecimal = BigDecimal.ZERO,
    var variance: BigDecimal = BigDecimal.ZERO,
    
    // Breakdown
    var openingCount: Map<Denomination, Int> = emptyMap(),
    var closingCount: Map<Denomination, Int> = emptyMap(),
    
    // Activity
    val events: MutableList<CashDrawerEvent> = mutableListOf()
)

enum class Denomination(val value: BigDecimal) {
    PENNY(BigDecimal("0.01")),
    NICKEL(BigDecimal("0.05")),
    DIME(BigDecimal("0.10")),
    QUARTER(BigDecimal("0.25")),
    HALF_DOLLAR(BigDecimal("0.50")),
    DOLLAR_COIN(BigDecimal("1.00")),
    ONE(BigDecimal("1.00")),
    TWO(BigDecimal("2.00")),
    FIVE(BigDecimal("5.00")),
    TEN(BigDecimal("10.00")),
    TWENTY(BigDecimal("20.00")),
    FIFTY(BigDecimal("50.00")),
    HUNDRED(BigDecimal("100.00"))
}

enum class DrawerStatus { CLOSED, OPEN, COUNTED }
```

### Opening the Drawer

```kotlin
fun openDrawer(registerId: Int, cashierId: Int, floatAmount: BigDecimal): DrawerOpenResult {
    val drawer = getDrawerForRegister(registerId)
    
    if (drawer.status == DrawerStatus.OPEN) {
        throw DrawerAlreadyOpenException()
    }
    
    // Validate float amount
    if (floatAmount < config.minFloat || floatAmount > config.maxFloat) {
        val approval = requestManagerOverride(
            ManagerOverrideRequest(
                operationType = "ABNORMAL_FLOAT",
                reason = "Float amount outside normal range",
                requestedValue = floatAmount
            )
        )
        if (!approval.approved) {
            throw FloatAmountNotApprovedException()
        }
    }
    
    // Count opening float if required
    if (config.requireOpeningCount) {
        val countResult = performDenominationCount("OPENING")
        if (countResult.total != floatAmount) {
            throw FloatCountMismatchException(
                expected = floatAmount,
                counted = countResult.total
            )
        }
        drawer.openingCount = countResult.breakdown
    }
    
    drawer.status = DrawerStatus.OPEN
    drawer.openedAt = OffsetDateTime.now()
    drawer.openedBy = cashierId
    drawer.openingFloat = floatAmount
    drawer.expectedCash = floatAmount
    
    saveDrawer(drawer)
    
    logDrawerEvent(
        CashDrawerEvent(
            type = DrawerEventType.DRAWER_OPEN,
            amount = floatAmount,
            performedBy = cashierId,
            time = OffsetDateTime.now()
        )
    )
    
    return DrawerOpenResult(success = true, drawer = drawer)
}
```

### Cash Pickup (Mid-Shift)

```kotlin
fun performCashPickup(registerId: Int, pickupAmount: BigDecimal): PickupResult {
    val drawer = getDrawerForRegister(registerId)
    
    if (drawer.status != DrawerStatus.OPEN) {
        throw DrawerNotOpenException()
    }
    
    if (pickupAmount > drawer.expectedCash - config.minDrawerCash) {
        throw PickupWouldLeaveDrawerBelowMinimumException()
    }
    
    // Manager must perform or verify pickup
    val manager = authenticateManager()
        ?: throw ManagerRequiredForPickupException()
    
    drawer.expectedCash -= pickupAmount
    
    val pickup = CashDrawerEvent(
        type = DrawerEventType.CASH_PICKUP,
        amount = pickupAmount,
        performedBy = manager.id,
        verifiedBy = getCurrentCashier(),
        time = OffsetDateTime.now(),
        bagNumber = generatePickupBagNumber()
    )
    drawer.events.add(pickup)
    
    saveDrawer(drawer)
    
    return PickupResult(
        success = true,
        amount = pickupAmount,
        bagNumber = pickup.bagNumber,
        newExpectedCash = drawer.expectedCash
    )
}
```

---

## Register Operations

### X Report (Mid-Day Summary)

```kotlin
fun generateXReport(registerId: Int): XReport {
    // X Report: Read-only summary, does NOT reset
    val register = getRegister(registerId)
    val drawer = getDrawerForRegister(registerId)
    
    val report = XReport(
        registerId = registerId,
        reportTime = OffsetDateTime.now(),
        reportType = "X",
        isEndOfDay = false
    )
    
    // Sales Summary
    val transactions = getTransactionsSinceOpen(registerId, register.openedAt)
    
    report.transactionCount = transactions.size
    report.grossSales = transactions.sumOf { it.subTotal }
    report.netSales = report.grossSales - transactions.sumOf { it.discountTotal }
    report.taxCollected = transactions.sumOf { it.taxTotal }
    report.totalSales = transactions.sumOf { it.grandTotal }
    
    // By Payment Type
    report.paymentBreakdown = transactions
        .flatMap { it.payments }
        .groupBy { it.paymentTypeId }
        .mapValues { (_, payments) -> payments.sumOf { it.value } }
    
    // Government Benefits
    report.snapTotal = report.paymentBreakdown[PaymentType.SNAP] ?: BigDecimal.ZERO
    report.wicTotal = report.paymentBreakdown[PaymentType.WIC] ?: BigDecimal.ZERO
    report.ebtCashTotal = report.paymentBreakdown[PaymentType.EBTCashBenefit] ?: BigDecimal.ZERO
    
    // Print report - do NOT reset counters
    printXReport(report)
    
    return report
}
```

### Z Report (End of Day)

```kotlin
fun generateZReport(registerId: Int, managerId: Int): ZReport {
    // Z Report: Final summary, RESETS counters
    val manager = authenticateManager(managerId)
        ?: throw ManagerRequiredForZReportException()
    
    val register = getRegister(registerId)
    val drawer = getDrawerForRegister(registerId)
    
    if (drawer.status != DrawerStatus.COUNTED) {
        throw DrawerMustBeCountedForZReportException()
    }
    
    val report = ZReport(
        registerId = registerId,
        reportTime = OffsetDateTime.now(),
        reportType = "Z",
        isEndOfDay = true,
        periodStart = register.openedAt,
        periodEnd = OffsetDateTime.now()
    )
    
    // All the same calculations as X Report
    // ... [calculations omitted for brevity]
    
    // Cash Variance
    report.countedCash = drawer.actualCash
    report.expectedCash = drawer.expectedCash
    report.variance = report.countedCash - report.expectedCash
    
    if (report.variance.abs() > config.varianceAlertThreshold) {
        report.varianceAlert = true
        createVarianceAlert(report, manager)
    }
    
    // Print report (2 copies typically)
    printZReport(report)
    printZReport(report) // Second copy for safe
    
    // RESET counters
    resetRegisterCounters(registerId)
    
    register.status = RegisterStatus.CLOSED
    register.closedAt = OffsetDateTime.now()
    register.closedBy = manager.id
    register.lastZReport = report.id
    register.requiresZReport = false
    
    saveRegister(register)
    archiveZReport(report)
    
    return report
}
```

---

## Age-Restricted Items

### Age Restriction Types

```kotlin
enum class AgeRestriction {
    NONE,           // No restriction
    AGE_18,         // Tobacco, lottery
    AGE_21,         // Alcohol
    AGE_25          // Some states for certain items
}

data class ProductViewModel(
    // ... other fields ...
    val ageRestriction: AgeRestriction,
    val requiresIdScan: Boolean           // Must scan ID vs visual verify
)
```

### Age Verification Flow

```kotlin
fun verifyAgeForItem(item: TransactionItemViewModel, transaction: Transaction): Boolean {
    if (item.ageRestriction == AgeRestriction.NONE) {
        return true
    }
    
    val requiredAge = getRequiredAge(item.ageRestriction)
    
    // Check if already verified this transaction
    if (transaction.ageVerified && transaction.verifiedAge >= requiredAge) {
        item.ageVerified = true
        return true
    }
    
    // Check customer profile (if known)
    transaction.customer?.dateOfBirth?.let { dob ->
        val age = calculateAge(dob)
        if (age >= requiredAge) {
            item.ageVerified = true
            transaction.ageVerified = true
            transaction.verifiedAge = age
            return true
        }
    }
    
    // Require manual verification
    val verification = requestAgeVerification(requiredAge)
    
    return if (verification.approved) {
        item.ageVerified = true
        transaction.ageVerified = true
        transaction.verifiedAge = verification.verifiedAge
        transaction.verificationMethod = verification.method
        true
    } else {
        false // Item cannot be sold
    }
}
```

---

## Weighted/Scale Items

### Scale Item Configuration

```kotlin
data class ProductViewModel(
    // ... other fields ...
    val isSoldByWeight: Boolean,          // Weight-based pricing
    val requiresScale: Boolean,           // Must use scale
    val weightUnit: String,               // "lb", "kg", "oz"
    val tareWeight: BigDecimal,           // Container weight to subtract
    val minWeight: BigDecimal,            // Minimum sellable weight
    val maxWeight: BigDecimal             // Maximum reasonable weight
)
```

### Scale Integration

```kotlin
fun processScaleItem(plu: String, scaleWeight: BigDecimal): TransactionItemViewModel {
    val product = lookupByPLU(plu)
    
    if (!product.isSoldByWeight) {
        throw ProductNotWeightBasedException()
    }
    
    // Subtract tare weight
    val netWeight = scaleWeight - product.tareWeight
    
    if (netWeight < product.minWeight) {
        throw WeightBelowMinimumException()
    }
    
    if (netWeight > product.maxWeight) {
        val confirmation = requestWeightConfirmation(netWeight, product.maxWeight)
        if (!confirmation.approved) {
            throw WeightExceedsMaximumException()
        }
    }
    
    val item = createTransactionItem(product).apply {
        quantitySold = netWeight
        quantityUsed = netWeight
        isManualQuantity = false
        this.scaleWeight = scaleWeight
        this.tareWeight = product.tareWeight
        this.netWeight = netWeight
        
        // Calculate price
        priceUsed = product.retailPrice  // Price per unit (lb/kg)
        subTotal = priceUsed * netWeight
    }
    
    return item
}
```

---

## Gift Cards

### Gift Card Purchase

```kotlin
fun processGiftCardPurchase(loadAmount: BigDecimal, recipientName: String?): GiftCardPurchaseResult {
    val giftCard = GiftCard(
        cardNumber = generateGiftCardNumber(),
        pin = generatePin(),
        balance = loadAmount,
        originalAmount = loadAmount,
        purchaseDate = OffsetDateTime.now(),
        expirationDate = OffsetDateTime.now().plusYears(GIFT_CARD_EXPIRY_YEARS),
        isActive = true,
        recipientName = recipientName
    )
    
    val item = TransactionItemViewModel().apply {
        branchProductName = "Gift Card - $$loadAmount"
        priceUsed = loadAmount
        finalPrice = loadAmount
        quantityUsed = BigDecimal.ONE
        subTotal = loadAmount
        
        // Gift cards are NOT taxable
        taxPerUnit = BigDecimal.ZERO
        taxTotal = BigDecimal.ZERO
        isTaxExempt = true
        
        // Gift cards are NOT SNAP eligible
        isSNAPEligible = false
        
        // Cannot be discounted
        excludeFromDiscounts = true
    }
    
    giftCardService.save(giftCard)
    
    return GiftCardPurchaseResult(item = item, giftCard = giftCard)
}
```

---

## Void Operations

### Void Item (Before Tender)

```kotlin
fun voidItem(transaction: Transaction, item: TransactionItemViewModel): VoidResult {
    // Before any payment, can simply remove
    if (transaction.payments.isEmpty()) {
        item.isRemoved = true
        item.voidReason = getVoidReason()
        item.voidedBy = getCurrentCashier()
        item.voidDate = OffsetDateTime.now()
        
        // Re-evaluate promotions (item removal may affect)
        evaluatePromotions(transaction)
        recalculateTransactionTotals(transaction)
        
        return VoidResult(success = true)
    }
    
    // After payment started, more complex
    throw CannotVoidAfterPaymentStartedException()
}
```

### Void Transaction

```kotlin
fun voidTransaction(transaction: Transaction, reason: String): VoidResult {
    // Check if any payments processed
    val processedPayments = transaction.payments.filter { it.statusId == PaymentStatus.Success }
    
    if (processedPayments.isNotEmpty()) {
        // Must reverse all payments
        processedPayments.forEach { payment -> reversePayment(payment) }
    }
    
    transaction.transactionStatusId = TransactionStatus.Voided
    transaction.voidReason = reason
    transaction.voidedBy = getCurrentCashier()
    transaction.voidDate = OffsetDateTime.now()
    
    if (requiresManagerApproval(transaction)) {
        val approval = requestManagerApproval("Void transaction")
        if (!approval.approved) {
            throw VoidNotApprovedException()
        }
        transaction.voidApprovedBy = approval.managerId
    }
    
    saveTransaction(transaction)
    OrderStore.clearStore()
    
    return VoidResult(success = true)
}
```

---

## Suspend and Resume

### Suspend Transaction

```kotlin
fun suspendTransaction(transaction: Transaction, suspendCode: String): SuspendResult {
    if (transaction.items.isEmpty()) {
        throw CannotSuspendEmptyTransactionException()
    }
    
    if (hasPartialPayments(transaction) && !config.allowSuspendWithPayments) {
        throw CannotSuspendWithPaymentsException()
    }
    
    transaction.transactionStatusId = TransactionStatus.Held
    transaction.suspendCode = suspendCode
    transaction.suspendedDate = OffsetDateTime.now()
    transaction.suspendedBy = getCurrentCashier()
    
    saveTransaction(transaction)
    OrderStore.clearStore()
    
    return SuspendResult(
        success = true,
        suspendCode = suspendCode,
        itemCount = transaction.itemCount,
        total = transaction.grandTotal
    )
}
```

### Resume Transaction

```kotlin
fun resumeTransaction(suspendCode: String): Transaction {
    val transaction = findSuspendedTransaction(suspendCode)
        ?: throw SuspendedTransactionNotFoundException()
    
    if (transaction.transactionStatusId != TransactionStatus.Held) {
        throw TransactionNotSuspendedException()
    }
    
    if (isSuspendExpired(transaction)) {
        throw SuspendedTransactionExpiredException()
    }
    
    // Restore to OrderStore
    OrderStore.loadFromTransaction(transaction)
    
    transaction.transactionStatusId = TransactionStatus.InProgress
    transaction.resumedDate = OffsetDateTime.now()
    transaction.resumedBy = getCurrentCashier()
    
    // Re-evaluate pricing (may have changed)
    transaction.items.forEach { item -> refreshItemPricing(item) }
    recalculateTransactionTotals(transaction)
    
    return transaction
}
```

---

## Offline Mode

### Offline Calculation Rules

```kotlin
data class OfflineCalculationConfig(
    // Pricing
    val useLastKnownPrices: Boolean = true,
    val allowPromotions: Boolean = false,     // Too complex offline
    val allowCoupons: Boolean = false,        // Need validation
    
    // Payments
    val allowCash: Boolean = true,
    val allowCards: Boolean = false,          // Need authorization
    val allowEbt: Boolean = false,            // Need real-time
    val allowGiftCards: Boolean = false,      // Need balance check
    
    // Limits
    val maxTransactionAmount: BigDecimal = BigDecimal("100.00"),
    val maxItems: Int = 20
)

fun processOfflineTransaction(transaction: Transaction): Transaction {
    val config = getOfflineConfig()
    
    if (transaction.grandTotal > config.maxTransactionAmount) {
        throw OfflineLimitExceededException()
    }
    
    if (transaction.itemCount > config.maxItems) {
        throw OfflineItemLimitExceededException()
    }
    
    // Simplify calculations
    transaction.items.forEach { item ->
        item.priceUsed = getCachedPrice(item.branchProductId)
        item.promotionId = null
        item.promotionDiscountPerUnit = BigDecimal.ZERO
        item.taxPercentSum = getCachedTaxRate(item.branchProductId)
        calculateItem(item, transaction)
    }
    
    recalculateTransactionTotals(transaction)
    
    transaction.isOfflineTransaction = true
    transaction.pendingSync = true
    
    return transaction
}
```

---

## Training Mode

### Training Mode Configuration

```kotlin
data class TrainingModeConfig(
    val saveTransactions: Boolean = false,    // Don't save to real database
    val printReceipts: Boolean = true,        // Print with TRAINING watermark
    val processPayments: Boolean = false,     // Simulate payments
    val updateInventory: Boolean = false,     // Don't affect stock
    val affectReporting: Boolean = false      // Exclude from reports
)

fun isTrainingMode(): Boolean =
    currentRegister.mode == RegisterMode.TRAINING

fun processTrainingTransaction(transaction: Transaction): Transaction {
    // All calculations work normally
    recalculateTransactionTotals(transaction)
    
    // But mark as training
    transaction.isTrainingTransaction = true
    
    // Simulate payments
    transaction.payments.forEach { payment ->
        payment.statusId = TransactionPaymentStatus.Success
        payment.isSimulated = true
    }
    
    // Print with watermark
    if (TrainingModeConfig.printReceipts) {
        printReceiptWithWatermark(transaction, "*** TRAINING - NOT A VALID RECEIPT ***")
    }
    
    return transaction
}
```

---

## Manager Operations

### Manager Override Workflow

```kotlin
data class ManagerOverrideRequest(
    val operationType: String,             // "FloorPrice", "Void", "Return", etc.
    val reason: String,
    val originalValue: BigDecimal? = null,
    val requestedValue: BigDecimal? = null,
    val requestedBy: Int? = null,
    val requestTime: OffsetDateTime = OffsetDateTime.now()
)

fun requestManagerOverride(request: ManagerOverrideRequest): ManagerOverrideResult {
    // Option 1: Manager at register
    if (isManagerLoggedIn()) {
        return ManagerOverrideResult(approved = true, managerId = currentManagerId())
    }
    
    // Option 2: Manager swipe/scan
    val managerAuth = promptForManagerCredentials()
    
    if (managerAuth == null || managerAuth.cancelled) {
        return ManagerOverrideResult(approved = false, reason = "Cancelled")
    }
    
    val manager = authenticateManager(managerAuth.credentials)
        ?: return ManagerOverrideResult(approved = false, reason = "Invalid credentials")
    
    if (!hasPermission(manager, request.operationType)) {
        return ManagerOverrideResult(approved = false, reason = "Insufficient permissions")
    }
    
    logManagerOverride(request, manager)
    
    return ManagerOverrideResult(
        approved = true,
        managerId = manager.id,
        managerName = manager.name
    )
}
```

---

## End of Day Procedures

### Daily Closing Checklist

```kotlin
data class EndOfDayChecklist(
    var allRegistersZReportComplete: Boolean = false,
    var allDrawersCounted: Boolean = false,
    var allVariancesExplained: Boolean = false,
    var safeDropComplete: Boolean = false,
    var depositPrepared: Boolean = false,
    var creditBatchSettled: Boolean = false,
    var salesReportGenerated: Boolean = false
)

fun performEndOfDay(storeId: Int, managerId: Int): EndOfDayResult {
    val manager = authenticateManager(managerId)
    if (manager == null || !hasPermission(manager, "END_OF_DAY")) {
        throw NotAuthorizedForEndOfDayException()
    }
    
    val checklist = EndOfDayChecklist()
    val errors = mutableListOf<String>()
    
    // STEP 1: CLOSE ALL REGISTERS
    val registers = getOpenRegisters(storeId)
    
    registers.forEach { register ->
        val openTrans = getOpenTransactions(register.id)
        if (openTrans.isNotEmpty()) {
            errors.add("Register ${register.id} has ${openTrans.size} open transactions")
            openTrans.forEach { trans ->
                voidTransaction(trans, "End of day - open transaction")
            }
        }
        
        if (register.status == RegisterStatus.OPEN) {
            generateZReport(register.id, managerId)
        }
    }
    
    checklist.allRegistersZReportComplete = true
    
    // STEP 2: RECONCILE CASH
    val totalExpectedCash = registers.sumOf { it.drawer.expectedCash }
    val totalCountedCash = registers.sumOf { it.drawer.actualCash }
    val totalVariance = totalCountedCash - totalExpectedCash
    
    if (totalVariance.abs() > config.acceptableStoreVariance) {
        errors.add("Store cash variance of $$totalVariance exceeds threshold")
    }
    
    checklist.allDrawersCounted = true
    
    // STEP 3: CREDIT CARD BATCH SETTLEMENT
    val batchResult = creditCardService.settleBatch(storeId, today())
    
    if (!batchResult.success) {
        errors.add("Credit batch settlement failed: ${batchResult.error}")
    } else {
        checklist.creditBatchSettled = true
    }
    
    // STEP 4: GENERATE REPORTS
    val salesReport = generateDailySalesReport(storeId, today())
    printReport(salesReport)
    checklist.salesReportGenerated = true
    
    // STEP 5: FINALIZE
    val eodRecord = EndOfDayRecord(
        storeId = storeId,
        businessDate = today(),
        closedBy = managerId,
        closedAt = OffsetDateTime.now(),
        checklist = checklist,
        errors = errors,
        salesTotal = salesReport.netSales,
        transactionCount = salesReport.transactionCount,
        variance = totalVariance
    )
    
    saveEndOfDayRecord(eodRecord)
    sendEndOfDaySummary(eodRecord, config.eodSummaryRecipients)
    
    return EndOfDayResult(
        success = errors.isEmpty(),
        errors = errors,
        checklist = checklist,
        record = eodRecord
    )
}
```

---

[← Back to Index](./INDEX.md)

