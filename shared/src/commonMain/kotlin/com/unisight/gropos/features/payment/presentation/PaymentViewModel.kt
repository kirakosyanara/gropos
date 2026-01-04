package com.unisight.gropos.features.payment.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.unisight.gropos.core.sync.OfflineQueueService
import com.unisight.gropos.core.sync.QueuedItem
import com.unisight.gropos.core.sync.QueueItemType
import com.unisight.gropos.core.util.CurrencyFormatter
import com.unisight.gropos.features.checkout.domain.model.Cart
import com.unisight.gropos.features.checkout.domain.repository.CartRepository
import com.unisight.gropos.features.payment.domain.model.AppliedPayment
import com.unisight.gropos.features.payment.domain.model.PaymentType
import com.unisight.gropos.features.payment.domain.terminal.PaymentResult
import com.unisight.gropos.features.payment.domain.terminal.PaymentTerminal
import com.unisight.gropos.features.transaction.data.api.TransactionApiService
import com.unisight.gropos.features.transaction.data.mapper.toCreateTransactionRequest
import com.unisight.gropos.features.transaction.domain.mapper.toTransaction
import com.unisight.gropos.features.transaction.domain.model.Transaction
import com.unisight.gropos.features.transaction.domain.repository.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

/**
 * ViewModel for the Payment screen.
 * 
 * Per ARCHITECTURE_BLUEPRINT.md:
 * - Observes the shared CartRepository singleton for cart totals
 * - Same cart state is visible in Checkout and Customer Display
 * 
 * Per PAYMENT_PROCESSING.md:
 * - Supports Cash, Credit, Debit, EBT payments
 * - Handles split tender (multiple payment types)
 * - Tracks SNAP eligible vs non-SNAP totals
 * 
 * Per DESKTOP_HARDWARE.md:
 * - Uses PaymentTerminal abstraction for card payments
 * - Non-blocking UI during terminal operations
 * 
 * Per DATABASE_SCHEMA.md:
 * - Saves completed transactions to LocalTransaction collection
 * 
 * Per END_OF_TRANSACTION_API_SUBMISSION.md:
 * - Transaction is POSTed to API IMMEDIATELY after completion
 * - Only queued for retry if immediate POST fails (offline/error)
 * - HeartbeatService is for RECEIVING updates, NOT sending transactions
 */
class PaymentViewModel(
    private val cartRepository: CartRepository,
    private val currencyFormatter: CurrencyFormatter,
    private val transactionRepository: TransactionRepository,
    private val paymentTerminal: PaymentTerminal,
    private val transactionApiService: TransactionApiService,
    private val offlineQueue: OfflineQueueService? = null,
    private val json: Json = Json { ignoreUnknownKeys = true },
    private val scope: CoroutineScope? = null
) : ScreenModel {
    
    private val _state = MutableStateFlow(PaymentUiState.initial())
    val state: StateFlow<PaymentUiState> = _state.asStateFlow()
    
    // Internal list of applied payments (for split tender)
    private val appliedPayments = mutableListOf<AppliedPayment>()
    private var totalPaid = BigDecimal.ZERO
    
    // Store cart snapshot for transaction creation
    private var cartSnapshot: Cart? = null
    
    // Track current terminal payment job for cancellation
    private var terminalPaymentJob: Job? = null
    
    // Track payment type for current terminal transaction
    private var currentPaymentType: PaymentType? = null
    
    private val effectiveScope: CoroutineScope
        get() = scope ?: screenModelScope
    
    init {
        observeCartChanges()
    }
    
    /**
     * Observes cart changes from the shared CartRepository.
     * Updates the summary when cart changes.
     */
    private fun observeCartChanges() {
        cartRepository.cart
            .onEach { cart -> 
                cartSnapshot = cart
                updateStateFromCart(cart) 
            }
            .launchIn(effectiveScope)
    }
    
    /**
     * Updates the UI state from cart data.
     */
    private fun updateStateFromCart(cart: Cart) {
        val snapEligibleTotal = calculateSnapEligible(cart)
        val nonSnapTotal = cart.grandTotal - snapEligibleTotal
        val remainingAmount = cart.grandTotal - totalPaid
        
        val summary = PaymentSummaryUiModel(
            itemSubtotal = currencyFormatter.format(cart.subTotal),
            discountTotal = if (cart.discountTotal > BigDecimal.ZERO) {
                currencyFormatter.formatWithSign(cart.discountTotal.negate(), false)
            } else null,
            subtotal = currencyFormatter.format(cart.subTotal - cart.discountTotal),
            taxTotal = currencyFormatter.format(cart.taxTotal),
            crvTotal = if (cart.crvTotal > BigDecimal.ZERO) {
                currencyFormatter.format(cart.crvTotal)
            } else null,
            grandTotal = currencyFormatter.format(cart.grandTotal),
            itemCount = formatItemCount(cart.itemCount),
            snapEligibleTotal = currencyFormatter.format(snapEligibleTotal),
            nonSnapTotal = currencyFormatter.format(nonSnapTotal)
        )
        
        _state.value = _state.value.copy(
            summary = summary,
            remainingAmount = currencyFormatter.format(remainingAmount.coerceAtLeast(BigDecimal.ZERO)),
            remainingAmountRaw = remainingAmount.coerceAtLeast(BigDecimal.ZERO),
            isCartEmpty = cart.isEmpty
        )
    }
    
    /**
     * Calculates the SNAP-eligible total from cart items.
     * 
     * Per BUSINESS_RULES.md: SNAP eligible items have isSNAPEligible = true.
     */
    private fun calculateSnapEligible(cart: Cart): BigDecimal {
        return cart.items
            .filterNot { it.isRemoved }
            .filter { it.isSnapEligible }
            .fold(BigDecimal.ZERO) { acc, item -> acc + item.subTotal }
            .setScale(2, RoundingMode.HALF_UP)
    }
    
    // ========================================================================
    // Tab Selection
    // ========================================================================
    
    fun onTabSelect(tab: PaymentTab) {
        _state.value = _state.value.copy(selectedTab = tab)
    }
    
    // ========================================================================
    // TenKey Input
    // ========================================================================
    
    fun onDigitPress(digit: String) {
        val current = _state.value.enteredAmount
        val newAmount = current + digit
        _state.value = _state.value.copy(enteredAmount = newAmount)
    }
    
    fun onClearPress() {
        _state.value = _state.value.copy(enteredAmount = "")
    }
    
    fun onBackspacePress() {
        val current = _state.value.enteredAmount
        if (current.isNotEmpty()) {
            _state.value = _state.value.copy(enteredAmount = current.dropLast(1))
        }
    }
    
    // ========================================================================
    // Cash Payment (Walking Skeleton)
    // Per requirement: Assume "Exact Change" for this Skeleton
    // ========================================================================
    
    /**
     * Process a cash payment.
     * 
     * For the Walking Skeleton, this assumes exact change:
     * - Tendered amount = remaining amount
     * - Transaction completes immediately
     */
    fun onCashExactChange() {
        processCashPayment(_state.value.remainingAmountRaw)
    }
    
    /**
     * Process a cash payment with a specific quick amount.
     */
    fun onCashQuickAmount(amount: BigDecimal) {
        processCashPayment(amount)
    }
    
    /**
     * Process a cash payment with the entered amount.
     */
    fun onCashEnteredAmount() {
        val enteredString = _state.value.enteredAmount
        if (enteredString.isBlank()) return
        
        val amount = try {
            BigDecimal(enteredString).setScale(2, RoundingMode.HALF_UP)
        } catch (e: Exception) {
            _state.value = _state.value.copy(errorMessage = "Invalid amount")
            return
        }
        
        processCashPayment(amount)
    }
    
    private fun processCashPayment(tendered: BigDecimal) {
        val remaining = _state.value.remainingAmountRaw
        if (remaining <= BigDecimal.ZERO) {
            _state.value = _state.value.copy(errorMessage = "No amount due")
            return
        }
        
        _state.value = _state.value.copy(isProcessing = true)
        
        effectiveScope.launch {
            // Calculate payment
            val paymentAmount = minOf(tendered, remaining)
            val change = (tendered - remaining).coerceAtLeast(BigDecimal.ZERO)
            
            // Create payment record
            val payment = AppliedPayment(
                id = UUID.randomUUID().toString(),
                type = PaymentType.Cash,
                amount = paymentAmount,
                displayName = "Cash"
            )
            
            appliedPayments.add(payment)
            totalPaid += paymentAmount
            
            // Update state
            val newRemainingRaw = remaining - paymentAmount
            val isComplete = newRemainingRaw <= BigDecimal.ZERO
            
            _state.value = _state.value.copy(
                isProcessing = false,
                appliedPayments = appliedPayments.map { mapPaymentToUiModel(it) },
                remainingAmount = currencyFormatter.format(newRemainingRaw.coerceAtLeast(BigDecimal.ZERO)),
                remainingAmountRaw = newRemainingRaw.coerceAtLeast(BigDecimal.ZERO),
                enteredAmount = "",
                showChangeDialog = isComplete && change > BigDecimal.ZERO,
                changeAmount = currencyFormatter.format(change)
            )
            
            // Complete transaction if fully paid
            if (isComplete) {
                completeTransaction()
            }
        }
    }
    
    /**
     * Completes the transaction by:
     * 1. Converting cart to Transaction
     * 2. Saving to local database (crash safety)
     * 3. IMMEDIATELY posting to backend API
     * 4. On success: mark as synced
     * 5. On failure: queue for retry later
     * 6. Printing virtual receipt
     * 7. Clearing cart
     * 
     * Per END_OF_TRANSACTION_API_SUBMISSION.md:
     * - Transaction is POSTed to API IMMEDIATELY after completion
     * - Only queued for retry if immediate POST fails (offline/error)
     * - HeartbeatService is for RECEIVING updates, NOT sending transactions
     */
    private suspend fun completeTransaction() {
        val cart = cartSnapshot ?: return
        
        // Step 1: Convert cart to transaction
        val transaction = cart.toTransaction(
            appliedPayments = appliedPayments.toList()
        )
        
        // Step 2: Save transaction to local database first (crash safety)
        val saveResult = transactionRepository.saveTransaction(transaction)
        
        saveResult.fold(
            onSuccess = {
                println("PaymentViewModel: Transaction ${transaction.guid} saved locally")
                
                // Step 3: IMMEDIATELY post to backend API
                submitTransactionToApi(transaction)
                
                // Step 4: Print virtual receipt to console
                printVirtualReceipt(transaction)
                
                // Step 5: Mark as complete and clear cart
                _state.value = _state.value.copy(isComplete = true)
                cartRepository.clearCart()
            },
            onFailure = { error ->
                println("PaymentViewModel: Failed to save transaction - ${error.message}")
                _state.value = _state.value.copy(
                    errorMessage = "Failed to save transaction. Please try again.",
                    isProcessing = false
                )
            }
        )
    }
    
    /**
     * Submits transaction to backend API IMMEDIATELY.
     * 
     * Per END_OF_TRANSACTION_API_SUBMISSION.md:
     * - POST to /transactions/create-transaction immediately
     * - On success: mark transaction as synced in local DB
     * - On failure: queue for retry later (offline mode)
     * 
     * The UI does NOT wait for API response - transaction is already saved locally.
     */
    private suspend fun submitTransactionToApi(transaction: Transaction) {
        try {
            // Create API request DTO
            val request = transaction.toCreateTransactionRequest()
            
            println("[TransactionSubmit] POSTing transaction ${transaction.guid} to API...")
            
            // Debug: Log payload summary (no sensitive amounts)
            println("[TransactionSubmit] Payload Summary:")
            println("[TransactionSubmit]   - guid: ${request.transaction.guid}")
            println("[TransactionSubmit]   - status: ${request.transaction.transactionStatusId}")
            println("[TransactionSubmit]   - items: ${request.items.size}")
            println("[TransactionSubmit]   - payments: ${request.payments.size}")
            println("[TransactionSubmit]   - rowCount: ${request.transaction.rowCount}")
            println("[TransactionSubmit]   - itemCount: ${request.transaction.itemCount}")
            
            // Debug: Log full JSON payload for troubleshooting
            try {
                val payloadJson = json.encodeToString(request)
                println("[TransactionSubmit] Full JSON payload:")
                println(payloadJson)
            } catch (e: Exception) {
                println("[TransactionSubmit] Failed to serialize payload for logging: ${e.message}")
            }
            
            // IMMEDIATELY call API
            val result = transactionApiService.createTransaction(request)
            
            result.fold(
                onSuccess = { response ->
                    // Check if actually successful (backend may return 202 with "Failure" status)
                    if (response.isSuccess) {
                        println("[TransactionSubmit] SUCCESS: Transaction ${transaction.guid} synced (remoteId: ${response.id})")
                        
                        try {
                            transactionRepository.markAsSynced(transaction.guid, response.id ?: 0)
                        } catch (e: Exception) {
                            println("[TransactionSubmit] Warning: Failed to update local sync status: ${e.message}")
                        }
                    } else {
                        // Backend returned success HTTP code but "Failure" status
                        println("[TransactionSubmit] BACKEND FAILURE: ${response.message}")
                        println("[TransactionSubmit] Status: ${response.status}, ID: ${response.id}")
                        queueForRetry(transaction)
                    }
                },
                onFailure = { error ->
                    // API call failed - queue for retry
                    println("[TransactionSubmit] FAILED: ${error.message} - queueing for retry")
                    queueForRetry(transaction)
                }
            )
        } catch (e: Exception) {
            // Network or other error - queue for retry
            println("[TransactionSubmit] EXCEPTION: ${e::class.simpleName} - ${e.message}")
            queueForRetry(transaction)
        }
    }
    
    /**
     * Queues a failed transaction for retry later.
     * 
     * Per END_OF_TRANSACTION_API_SUBMISSION.md:
     * - Only called when immediate POST fails
     * - Transaction already saved locally with PENDING status
     * - Will be retried by background sync mechanism
     */
    private suspend fun queueForRetry(transaction: Transaction) {
        val queue = offlineQueue ?: run {
            println("[TransactionSubmit] OfflineQueue not available - will retry via getUnsynced()")
            // Mark as failed so it can be picked up later
            try {
                transactionRepository.markSyncFailed(transaction.guid, "Immediate sync failed, no queue available")
            } catch (e: Exception) {
                println("[TransactionSubmit] Failed to mark sync failed: ${e.message}")
            }
            return
        }
        
        try {
            // Create API request DTO
            val request = transaction.toCreateTransactionRequest()
            
            // Serialize to JSON
            val payload = json.encodeToString(request)
            
            // Create queue item
            val queueItem = QueuedItem(
                id = 0L,
                type = QueueItemType.TRANSACTION,
                payload = payload,
                createdAt = Clock.System.now(),
                attempts = 1, // Already attempted once
                lastAttempt = Clock.System.now()
            )
            
            // Enqueue for retry
            queue.enqueue(queueItem)
            
            println("[TransactionSubmit] Transaction ${transaction.guid} queued for retry")
            
        } catch (e: Exception) {
            println("[TransactionSubmit] Failed to queue for retry: ${e::class.simpleName}")
            // Transaction is still saved locally, will be picked up by getUnsynced()
        }
    }
    
    /**
     * Prints a virtual receipt to the console.
     * 
     * Per requirement: Log the "Receipt" text to the console (simulating a print job).
     * This simulates what would be sent to a physical receipt printer.
     */
    private fun printVirtualReceipt(transaction: Transaction) {
        val receiptBuilder = StringBuilder()
        val divider = "=".repeat(40)
        val thinDivider = "-".repeat(40)
        
        receiptBuilder.appendLine()
        receiptBuilder.appendLine(divider)
        receiptBuilder.appendLine("         *** VIRTUAL RECEIPT ***")
        receiptBuilder.appendLine(divider)
        receiptBuilder.appendLine()
        receiptBuilder.appendLine("Transaction #: ${transaction.id}")
        receiptBuilder.appendLine("Date: ${transaction.completedDateTime}")
        receiptBuilder.appendLine("Station: ${transaction.stationId}")
        receiptBuilder.appendLine()
        receiptBuilder.appendLine(thinDivider)
        receiptBuilder.appendLine("ITEMS:")
        receiptBuilder.appendLine(thinDivider)
        
        transaction.items.forEach { item ->
            val qty = if (item.quantityUsed.scale() == 0 || item.quantityUsed.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0) {
                item.quantityUsed.toInt().toString()
            } else {
                item.quantityUsed.setScale(2, RoundingMode.HALF_UP).toString()
            }
            val priceStr = currencyFormatter.format(item.priceUsed)
            val lineTotal = currencyFormatter.format(item.subTotal)
            val taxFlag = item.taxIndicator
            
            receiptBuilder.appendLine("${item.branchProductName}")
            receiptBuilder.appendLine("  $qty x $priceStr = $lineTotal $taxFlag")
            
            if (item.taxTotal > BigDecimal.ZERO) {
                receiptBuilder.appendLine("    Tax: ${currencyFormatter.format(item.taxTotal)}")
            }
            if (item.crvRatePerUnit > BigDecimal.ZERO) {
                val crvTotal = item.crvRatePerUnit.multiply(item.quantityUsed).setScale(2, RoundingMode.HALF_UP)
                receiptBuilder.appendLine("    CRV: ${currencyFormatter.format(crvTotal)}")
            }
        }
        
        receiptBuilder.appendLine()
        receiptBuilder.appendLine(thinDivider)
        receiptBuilder.appendLine("TOTALS:")
        receiptBuilder.appendLine(thinDivider)
        receiptBuilder.appendLine("Subtotal:       ${currencyFormatter.format(transaction.subTotal)}")
        
        if (transaction.discountTotal > BigDecimal.ZERO) {
            receiptBuilder.appendLine("Discount:      -${currencyFormatter.format(transaction.discountTotal)}")
        }
        
        receiptBuilder.appendLine("Tax:            ${currencyFormatter.format(transaction.taxTotal)}")
        
        if (transaction.crvTotal > BigDecimal.ZERO) {
            receiptBuilder.appendLine("CRV:            ${currencyFormatter.format(transaction.crvTotal)}")
        }
        
        receiptBuilder.appendLine(thinDivider)
        receiptBuilder.appendLine("GRAND TOTAL:    ${currencyFormatter.format(transaction.grandTotal)}")
        receiptBuilder.appendLine(thinDivider)
        
        receiptBuilder.appendLine()
        receiptBuilder.appendLine("PAYMENTS:")
        transaction.payments.forEach { payment ->
            receiptBuilder.appendLine("  ${payment.paymentMethodName}: ${currencyFormatter.format(payment.value)}")
        }
        
        receiptBuilder.appendLine()
        receiptBuilder.appendLine(divider)
        receiptBuilder.appendLine("       Thank you for shopping!")
        receiptBuilder.appendLine(divider)
        receiptBuilder.appendLine()
        
        // Output to console
        println(receiptBuilder.toString())
    }
    
    // ========================================================================
    // Card Payments via Payment Terminal
    // Per DESKTOP_HARDWARE.md: Uses PaymentTerminal abstraction
    // ========================================================================
    
    /**
     * Initiates a credit card payment via the payment terminal.
     * 
     * Per PAYMENT_PROCESSING.md: 
     * - Shows terminal dialog
     * - Waits for card input
     * - Processes authorization
     */
    fun onCreditPayment() {
        processCardPayment(PaymentType.Credit, "Credit")
    }
    
    /**
     * Initiates a debit card payment via the payment terminal.
     */
    fun onDebitPayment() {
        processCardPayment(PaymentType.Debit, "Debit")
    }
    
    /**
     * Initiates an EBT SNAP payment via the payment terminal.
     */
    fun onEbtSnapPayment() {
        processCardPayment(PaymentType.EbtSnap, "EBT SNAP")
    }
    
    /**
     * Initiates an EBT Cash payment via the payment terminal.
     */
    fun onEbtCashPayment() {
        processCardPayment(PaymentType.EbtCash, "EBT Cash")
    }
    
    /**
     * Balance check for EBT cards.
     * 
     * Not yet implemented - placeholder for future functionality.
     */
    fun onBalanceCheck() {
        _state.value = _state.value.copy(
            errorMessage = "Balance check not yet implemented."
        )
    }
    
    /**
     * Processes a card payment using the PaymentTerminal.
     * 
     * Per DESKTOP_HARDWARE.md:
     * - Non-blocking operation using coroutines
     * - UI shows terminal dialog while waiting
     * - ViewModel does NOT know if terminal is simulated or real
     * 
     * @param paymentType The type of card payment
     * @param displayName Display name for the payment type
     */
    private fun processCardPayment(paymentType: PaymentType, displayName: String) {
        val remaining = _state.value.remainingAmountRaw
        if (remaining <= BigDecimal.ZERO) {
            _state.value = _state.value.copy(errorMessage = "No amount due")
            return
        }
        
        // Determine amount to charge
        val amount = getPaymentAmount()
        currentPaymentType = paymentType
        
        // Show terminal dialog
        _state.value = _state.value.copy(
            showTerminalDialog = true,
            terminalDialogAmount = currencyFormatter.format(amount)
        )
        
        // Start terminal processing
        terminalPaymentJob = effectiveScope.launch {
            val result = paymentTerminal.processPayment(amount)
            handleTerminalResult(result, amount, paymentType, displayName)
        }
    }
    
    /**
     * Handles the result from the payment terminal.
     * 
     * Per PAYMENT_PROCESSING.md:
     * - Approved: Add payment -> Close Dialog -> Finish Sale (if paid in full)
     * - Declined: Show Error Toast -> Close Dialog -> Stay on Pay Screen
     * - Cancelled: Close Dialog -> Stay on Pay Screen
     */
    private suspend fun handleTerminalResult(
        result: PaymentResult,
        amount: BigDecimal,
        paymentType: PaymentType,
        displayName: String
    ) {
        // Always hide the terminal dialog
        _state.value = _state.value.copy(showTerminalDialog = false)
        
        when (result) {
            is PaymentResult.Approved -> {
                // Create payment record with card details
                val payment = AppliedPayment(
                    id = UUID.randomUUID().toString(),
                    type = paymentType,
                    amount = amount,
                    displayName = "$displayName (${result.cardType})",
                    authCode = result.authCode,
                    lastFour = result.lastFour
                )
                
                appliedPayments.add(payment)
                totalPaid += amount
                
                // Update state
                val newRemainingRaw = _state.value.remainingAmountRaw - amount
                val isComplete = newRemainingRaw <= BigDecimal.ZERO
                
                _state.value = _state.value.copy(
                    appliedPayments = appliedPayments.map { mapPaymentToUiModel(it) },
                    remainingAmount = currencyFormatter.format(newRemainingRaw.coerceAtLeast(BigDecimal.ZERO)),
                    remainingAmountRaw = newRemainingRaw.coerceAtLeast(BigDecimal.ZERO),
                    enteredAmount = ""
                )
                
                println("PaymentViewModel: Card payment approved - ${result.cardType} ****${result.lastFour}, Auth: ${result.authCode}")
                
                // Complete transaction if fully paid
                if (isComplete) {
                    completeTransaction()
                }
            }
            
            is PaymentResult.Declined -> {
                // Show error message, stay on pay screen
                _state.value = _state.value.copy(
                    errorMessage = "Card Declined: ${result.reason}"
                )
                println("PaymentViewModel: Card payment declined - ${result.reason}")
            }
            
            is PaymentResult.Error -> {
                // Show error message, stay on pay screen
                _state.value = _state.value.copy(
                    errorMessage = "Terminal Error: ${result.message}"
                )
                println("PaymentViewModel: Terminal error - ${result.message}")
            }
            
            is PaymentResult.Cancelled -> {
                // User cancelled, just close dialog (no error message)
                println("PaymentViewModel: Card payment cancelled by user")
            }
        }
        
        terminalPaymentJob = null
        currentPaymentType = null
    }
    
    /**
     * Cancel the current terminal transaction.
     * 
     * Called when user presses "Cancel" on the terminal dialog.
     */
    fun onCancelTerminalTransaction() {
        effectiveScope.launch {
            paymentTerminal.cancelTransaction()
            terminalPaymentJob?.cancel()
            terminalPaymentJob = null
            
            _state.value = _state.value.copy(showTerminalDialog = false)
        }
    }
    
    /**
     * Gets the payment amount based on entered amount or remaining amount.
     */
    private fun getPaymentAmount(): BigDecimal {
        val enteredString = _state.value.enteredAmount
        if (enteredString.isNotBlank()) {
            try {
                val entered = BigDecimal(enteredString).setScale(2, RoundingMode.HALF_UP)
                if (entered > BigDecimal.ZERO) {
                    return minOf(entered, _state.value.remainingAmountRaw)
                }
            } catch (e: Exception) {
                // Fall through to use remaining amount
            }
        }
        return _state.value.remainingAmountRaw
    }
    
    // ========================================================================
    // Check/Other Payments
    // Per PAYMENT_PROCESSING.md: Check tender type
    // ========================================================================
    
    /**
     * Processes a check payment.
     * 
     * Per PAYMENT_PROCESSING.md: Check tender type.
     * Check payments are applied for the entered amount (or remaining balance).
     */
    fun onCheckPayment() {
        val remaining = _state.value.remainingAmountRaw
        if (remaining <= BigDecimal.ZERO) {
            _state.value = _state.value.copy(errorMessage = "No amount due")
            return
        }
        
        _state.value = _state.value.copy(isProcessing = true)
        
        effectiveScope.launch {
            // Determine amount (entered or remaining)
            val paymentAmount = getPaymentAmount()
            
            // Create the check payment
            val payment = AppliedPayment(
                id = UUID.randomUUID().toString(),
                type = PaymentType.Check,
                amount = paymentAmount,
                displayName = "Check"
            )
            
            // Add to applied payments
            appliedPayments.add(payment)
            totalPaid += paymentAmount
            
            // Calculate new remaining
            val newRemainingRaw = remaining - paymentAmount
            val isComplete = newRemainingRaw <= BigDecimal.ZERO
            
            // Log for audit
            println("[AUDIT] CHECK PAYMENT APPLIED")
            println("  Amount: ${currencyFormatter.format(paymentAmount)}")
            println("  Timestamp: ${java.time.LocalDateTime.now()}")
            
            // Update state
            _state.value = _state.value.copy(
                isProcessing = false,
                appliedPayments = appliedPayments.map { mapPaymentToUiModel(it) },
                remainingAmount = currencyFormatter.format(newRemainingRaw.coerceAtLeast(BigDecimal.ZERO)),
                remainingAmountRaw = newRemainingRaw.coerceAtLeast(BigDecimal.ZERO),
                enteredAmount = ""
            )
            
            // If complete, save transaction
            if (isComplete) {
                completeTransaction()
            }
        }
    }
    
    // ========================================================================
    // Dialogs
    // ========================================================================
    
    fun dismissChangeDialog() {
        _state.value = _state.value.copy(showChangeDialog = false)
    }
    
    fun dismissError() {
        _state.value = _state.value.copy(errorMessage = null)
    }
    
    // ========================================================================
    // Helpers
    // ========================================================================
    
    private fun mapPaymentToUiModel(payment: AppliedPayment): AppliedPaymentUiModel {
        return AppliedPaymentUiModel(
            id = payment.id,
            displayName = payment.displayName,
            amount = currencyFormatter.format(payment.amount),
            details = payment.lastFour?.let { "****$it" }
        )
    }
    
    private fun formatItemCount(count: BigDecimal): String {
        val intCount = count.toInt()
        return if (intCount == 1) "1 item" else "$intCount items"
    }
}
