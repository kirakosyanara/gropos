package com.unisight.gropos.features.checkout.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.unisight.gropos.core.security.ApprovalDetails
import com.unisight.gropos.core.security.ApprovalResult
import com.unisight.gropos.core.security.ManagerApprovalService
import com.unisight.gropos.core.security.PermissionCheckResult
import com.unisight.gropos.core.session.InactivityManager
import com.unisight.gropos.features.cashier.domain.service.CashierSessionManager
import com.unisight.gropos.core.security.PermissionManager
import com.unisight.gropos.core.security.RequestAction
import com.unisight.gropos.core.util.CurrencyFormatter
import com.unisight.gropos.features.auth.domain.model.AuthUser
import com.unisight.gropos.features.auth.domain.repository.AuthRepository
import com.unisight.gropos.features.cashier.domain.repository.VendorRepository
import com.unisight.gropos.features.checkout.domain.model.Cart
import com.unisight.gropos.features.checkout.domain.model.CartItem
import com.unisight.gropos.features.checkout.domain.model.Product
import com.unisight.gropos.features.checkout.domain.repository.CartRepository
import com.unisight.gropos.features.checkout.domain.repository.ProductRepository
import com.unisight.gropos.features.checkout.domain.repository.ScannerRepository
import com.unisight.gropos.features.checkout.domain.usecase.ScanItemUseCase
import com.unisight.gropos.features.checkout.presentation.components.ProductLookupState
import com.unisight.gropos.features.checkout.presentation.components.ProductLookupUiModel
import com.unisight.gropos.features.checkout.presentation.components.dialogs.HeldTransactionUiModel
import com.unisight.gropos.features.checkout.domain.usecase.ScanResult
import com.unisight.gropos.features.transaction.domain.model.HeldTransaction
import com.unisight.gropos.features.transaction.domain.model.HeldTransactionItem
import com.unisight.gropos.features.transaction.domain.repository.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * ViewModel for the Checkout screen (Cashier window).
 * 
 * Manages the checkout UI state by:
 * 1. Reactively collecting scanned barcodes from hardware
 * 2. Delegating product lookup to ScanItemUseCase
 * 3. Observing cart state from CartRepository (shared with Customer Display)
 * 
 * Per ARCHITECTURE_BLUEPRINT.md:
 * - Cart state is managed by CartRepository (Singleton)
 * - This ViewModel OBSERVES the repository, it does NOT own the state
 * - Any changes here are automatically reflected on Customer Display
 * 
 * Per project-structure.mdc: Named [Feature]ViewModel
 * Per kotlin-standards.mdc: Uses ScreenModel for Voyager compatibility
 * Per code-quality.mdc: Unidirectional Data Flow - UI observes state
 */
class CheckoutViewModel(
    private val scanItemUseCase: ScanItemUseCase,
    private val scannerRepository: ScannerRepository,
    private val cartRepository: CartRepository,
    private val productRepository: ProductRepository,
    private val currencyFormatter: CurrencyFormatter,
    private val authRepository: AuthRepository,
    private val managerApprovalService: ManagerApprovalService,
    private val cashierSessionManager: CashierSessionManager,
    private val transactionRepository: TransactionRepository,
    private val vendorRepository: VendorRepository,
    // Inject scope for testability (per testing-strategy.mdc)
    private val scope: CoroutineScope? = null
) : ScreenModel {
    
    // Cached current user for permission checks
    private var currentUser: AuthUser? = null
    
    private val _state = MutableStateFlow(CheckoutUiState.initial())
    
    /**
     * Current checkout UI state.
     * Observe this in Compose using collectAsState().
     */
    val state: StateFlow<CheckoutUiState> = _state.asStateFlow()
    
    private val effectiveScope: CoroutineScope
        get() = scope ?: screenModelScope
    
    init {
        // Reactive Hardware Collection
        // Per requirement: In init{}, collect scannerRepository.scannedCodes
        observeScannerFlow()
        
        // Observe cart changes from the SHARED CartRepository
        // This is the key change - we observe the repository, not use case
        observeCartChanges()
        
        // Load current user for permission checks
        loadCurrentUser()
    }
    
    /**
     * Loads the current user for permission checks.
     */
    private fun loadCurrentUser() {
        effectiveScope.launch {
            currentUser = authRepository.getCurrentUser()
        }
    }
    
    /**
     * Collects scanned barcodes from hardware and processes them automatically.
     */
    private fun observeScannerFlow() {
        scannerRepository.scannedCodes
            .onEach { barcode -> processScan(barcode) }
            .launchIn(effectiveScope)
    }
    
    /**
     * Observes cart state changes from the SHARED CartRepository.
     * 
     * Since CartRepository is a singleton, when Customer Display observes
     * the same repository, both windows stay in sync.
     */
    private fun observeCartChanges() {
        cartRepository.cart
            .onEach { cart -> updateStateFromCart(cart) }
            .launchIn(effectiveScope)
    }
    
    /**
     * Processes a scanned barcode.
     * Called automatically from hardware scanner flow.
     * 
     * Per DIALOGS.md: If product is age-restricted, show Age Verification Dialog
     * BEFORE adding to cart. The item MUST NOT be added until verified.
     */
    private suspend fun processScan(barcode: String) {
        _state.value = _state.value.copy(isLoading = true)
        
        // First, look up the product to check for age restriction
        val product = productRepository.getByBarcode(barcode)
        
        if (product == null) {
            _state.value = _state.value.copy(
                isLoading = false,
                lastScanEvent = ScanEvent.ProductNotFound(barcode)
            )
            return
        }
        
        // Check for age restriction BEFORE adding to cart
        if (product.isAgeRestricted) {
            _state.value = _state.value.copy(isLoading = false)
            checkAndHandleAgeRestriction(product)
            return
        }
        
        // Product is not age-restricted, add normally via use case
        when (val result = scanItemUseCase.processScan(barcode)) {
            is ScanResult.Success -> {
                val lastItem = result.cart.items.lastOrNull()
                _state.value = _state.value.copy(
                    isLoading = false,
                    lastScanEvent = lastItem?.let {
                        ScanEvent.ProductAdded(it.branchProductName)
                    }
                )
            }
            is ScanResult.ProductNotFound -> {
                _state.value = _state.value.copy(
                    isLoading = false,
                    lastScanEvent = ScanEvent.ProductNotFound(result.barcode)
                )
            }
            is ScanResult.Error -> {
                _state.value = _state.value.copy(
                    isLoading = false,
                    lastScanEvent = ScanEvent.Error(result.message)
                )
            }
        }
    }
    
    /**
     * Manual barcode entry.
     * For keyboard/touchscreen input when hardware scanner unavailable.
     * 
     * @param barcode The barcode string entered manually
     */
    fun onManualBarcodeEnter(barcode: String) {
        if (barcode.isBlank()) return
        
        effectiveScope.launch {
            processScan(barcode.trim())
        }
    }
    
    /**
     * Removes an item from the cart.
     * 
     * @param branchProductId The product ID to remove
     */
    fun onRemoveItem(branchProductId: Int) {
        effectiveScope.launch {
            scanItemUseCase.removeProduct(branchProductId)
        }
    }
    
    /**
     * Voids an item (marks as removed but keeps in history).
     * 
     * @param branchProductId The product ID to void
     */
    fun onVoidItem(branchProductId: Int) {
        effectiveScope.launch {
            scanItemUseCase.voidProduct(branchProductId)
        }
    }
    
    /**
     * Clears the entire cart.
     * Called after transaction completion or void all.
     */
    fun onClearCart() {
        effectiveScope.launch {
            scanItemUseCase.clearCart()
        }
    }
    
    /**
     * Dismisses the last scan event notification.
     */
    fun onDismissScanEvent() {
        _state.value = _state.value.copy(lastScanEvent = null)
    }
    
    // ========================================================================
    // Error Dialog - Critical Alert System
    // Per DIALOGS.md (Error Message Dialog):
    // Use for CRITICAL errors that STOP THE FLOW: Payments, Hardware, Legal
    // Non-critical errors (Product Not Found) should use Snackbar
    // ========================================================================
    
    /**
     * Shows a critical error dialog.
     * 
     * Per DIALOGS.md: Error dialogs are for flow-stopping errors like:
     * - Payment Declined
     * - Printer Error
     * - Hardware Failure
     * - Age Verification Failed
     * 
     * Do NOT use for minor errors - use Snackbar via lastScanEvent instead.
     * 
     * @param title Dialog title (displayed in red header)
     * @param message Error message body text
     * @param actionLabel Custom action button label (default: "Dismiss")
     */
    fun showCriticalError(
        title: String,
        message: String,
        actionLabel: String = "Dismiss"
    ) {
        _state.value = _state.value.copy(
            errorDialog = com.unisight.gropos.core.components.dialogs.ErrorDialogState(
                title = title,
                message = message,
                actionLabel = actionLabel
            )
        )
    }
    
    /**
     * Dismisses the critical error dialog.
     * 
     * Called when user clicks the dismiss button or closes the dialog.
     */
    fun dismissError() {
        _state.value = _state.value.copy(errorDialog = null)
    }
    
    // ========================================================================
    // Modification Mode
    // Per SCREEN_LAYOUTS.md: When a line item is selected, the right panel
    // transforms to show modification options.
    // ========================================================================
    
    /**
     * Selects a line item to enter modification mode.
     * 
     * Per SCREEN_LAYOUTS.md: The right panel transforms when an item is selected.
     * Weighted items start in DISCOUNT mode (can't change qty for weighted items).
     * 
     * @param branchProductId The product ID to select
     */
    fun onSelectLineItem(branchProductId: Int) {
        val currentCart = cartRepository.getCurrentCart()
        val cartItem = currentCart.items.find { it.branchProductId == branchProductId }
        
        if (cartItem == null || cartItem.isRemoved) {
            return
        }
        
        val isWeighted = cartItem.soldById == "Weight" || cartItem.soldById == "WeightOnScale"
        val initialMode = if (isWeighted) {
            ModificationTenKeyMode.DISCOUNT
        } else {
            ModificationTenKeyMode.QUANTITY
        }
        
        val selectedItemUiModel = SelectedItemUiModel(
            branchProductId = cartItem.branchProductId,
            productName = cartItem.branchProductName,
            currentQuantity = formatQuantity(cartItem),
            currentPrice = currencyFormatter.format(cartItem.effectivePrice),
            lineTotal = currencyFormatter.format(cartItem.subTotal),
            isWeighted = isWeighted,
            rawQuantity = cartItem.quantityUsed.toInt()
        )
        
        _state.value = _state.value.copy(
            selectedItemId = branchProductId,
            selectedItem = selectedItemUiModel,
            modificationTenKeyMode = initialMode,
            modificationInputValue = ""
        )
    }
    
    /**
     * Deselects the current item and exits modification mode.
     */
    fun onDeselectLineItem() {
        _state.value = _state.value.copy(
            selectedItemId = null,
            selectedItem = null,
            modificationInputValue = ""
        )
    }
    
    /**
     * Changes the TenKey mode in modification mode.
     * 
     * @param mode The new mode (QUANTITY, DISCOUNT, PRICE)
     */
    fun onChangeModificationMode(mode: ModificationTenKeyMode) {
        _state.value = _state.value.copy(
            modificationTenKeyMode = mode,
            modificationInputValue = "" // Clear input when switching modes
        )
    }
    
    /**
     * Handles digit press in modification mode.
     */
    fun onModificationDigitPress(digit: String) {
        val currentInput = _state.value.modificationInputValue
        val newInput = currentInput + digit
        _state.value = _state.value.copy(modificationInputValue = newInput)
    }
    
    /**
     * Clears the modification input.
     */
    fun onModificationClear() {
        _state.value = _state.value.copy(modificationInputValue = "")
    }
    
    /**
     * Handles backspace in modification mode.
     */
    fun onModificationBackspace() {
        val currentInput = _state.value.modificationInputValue
        if (currentInput.isNotEmpty()) {
            _state.value = _state.value.copy(
                modificationInputValue = currentInput.dropLast(1)
            )
        }
    }
    
    /**
     * Confirms and applies the modification.
     * 
     * Per SCREEN_LAYOUTS.md:
     * - QUANTITY: Update quantity (1-99)
     * - DISCOUNT: Apply % discount (placeholder for now)
     * - PRICE: Override price (placeholder for now)
     */
    fun onModificationConfirm() {
        val selectedItemId = _state.value.selectedItemId ?: return
        val inputValue = _state.value.modificationInputValue
        
        if (inputValue.isBlank()) {
            onDeselectLineItem()
            return
        }
        
        effectiveScope.launch {
            when (_state.value.modificationTenKeyMode) {
                ModificationTenKeyMode.QUANTITY -> {
                    val newQty = inputValue.toIntOrNull()
                    if (newQty != null && newQty in 1..99) {
                        cartRepository.updateQuantity(
                            selectedItemId,
                            BigDecimal(newQty)
                        )
                    }
                }
                ModificationTenKeyMode.DISCOUNT -> {
                    // Placeholder: Discount requires manager approval logic
                    // For now, just show a message
                    _state.value = _state.value.copy(
                        lastScanEvent = ScanEvent.Error("Discount requires manager approval (not implemented)")
                    )
                }
                ModificationTenKeyMode.PRICE -> {
                    // Placeholder: Price override requires floor price check
                    // For now, just show a message
                    _state.value = _state.value.copy(
                        lastScanEvent = ScanEvent.Error("Price override requires approval (not implemented)")
                    )
                }
            }
            
            // Exit modification mode after applying
            onDeselectLineItem()
        }
    }
    
    /**
     * Voids the selected line item.
     * 
     * Per SCREEN_LAYOUTS.md: Remove Item marks as isRemoved = true,
     * item remains visible with strikethrough styling.
     * 
     * Per ROLES_AND_PERMISSIONS.md: Voiding requires manager approval
     * for cashiers, managers can self-approve.
     */
    fun onVoidSelectedLineItem() {
        val selectedItemId = _state.value.selectedItemId ?: return
        val user = currentUser ?: return
        
        // Check permission before voiding
        val permissionResult = PermissionManager.checkPermission(user, RequestAction.VOID_TRANSACTION)
        
        when (permissionResult) {
            PermissionCheckResult.GRANTED,
            PermissionCheckResult.SELF_APPROVAL_ALLOWED -> {
                // Can void directly (manager) or self-approve
                effectiveScope.launch {
                    cartRepository.voidItem(selectedItemId)
                    onDeselectLineItem()
                }
            }
            PermissionCheckResult.REQUIRES_APPROVAL -> {
                // Show manager approval dialog
                showManagerApprovalDialog(
                    action = RequestAction.VOID_TRANSACTION,
                    pendingItemId = selectedItemId
                )
            }
            PermissionCheckResult.DENIED -> {
                // User has no permission - show error
                _state.value = _state.value.copy(
                    lastScanEvent = ScanEvent.Error("You do not have permission to void items.")
                )
            }
        }
    }
    
    // ========================================================================
    // Manager Approval Dialog
    // Per ROLES_AND_PERMISSIONS.md: Manager approval for sensitive actions
    // ========================================================================
    
    /**
     * Shows the manager approval dialog.
     * 
     * @param action The action requiring approval
     * @param pendingItemId Optional item ID for line-item specific approvals
     */
    private fun showManagerApprovalDialog(
        action: RequestAction,
        pendingItemId: Int? = null
    ) {
        val user = currentUser ?: return
        
        effectiveScope.launch {
            // Get available managers
            val managers = managerApprovalService.getApprovers(action, user)
            
            _state.value = _state.value.copy(
                managerApprovalState = ManagerApprovalDialogState(
                    isVisible = true,
                    action = action,
                    managers = managers,
                    isProcessing = false,
                    errorMessage = null,
                    pendingItemId = pendingItemId
                )
            )
        }
    }
    
    /**
     * Handles manager approval dialog dismissal.
     */
    fun onDismissManagerApproval() {
        _state.value = _state.value.copy(
            managerApprovalState = ManagerApprovalDialogState()
        )
    }
    
    /**
     * Submits a manager PIN for approval.
     * 
     * @param managerId The ID of the manager approving
     * @param pin The PIN entered by the manager
     */
    fun onSubmitManagerApproval(managerId: Int, pin: String) {
        val approvalState = _state.value.managerApprovalState
        if (!approvalState.isVisible) return
        
        val user = currentUser ?: return
        
        // Set processing state
        _state.value = _state.value.copy(
            managerApprovalState = approvalState.copy(
                isProcessing = true,
                errorMessage = null
            )
        )
        
        effectiveScope.launch {
            val result = managerApprovalService.validateApproval(
                managerId = managerId,
                pin = pin,
                action = approvalState.action,
                details = ApprovalDetails(
                    itemId = approvalState.pendingItemId
                ),
                requesterId = user.id.toIntOrNull() ?: 0
            )
            
            when (result) {
                is ApprovalResult.Approved -> {
                    // Execute the pending action
                    executeApprovedAction(approvalState)
                    
                    // Close dialog
                    _state.value = _state.value.copy(
                        managerApprovalState = ManagerApprovalDialogState()
                    )
                }
                is ApprovalResult.Denied -> {
                    _state.value = _state.value.copy(
                        managerApprovalState = approvalState.copy(
                            isProcessing = false,
                            errorMessage = result.reason
                        )
                    )
                }
                is ApprovalResult.Error -> {
                    _state.value = _state.value.copy(
                        managerApprovalState = approvalState.copy(
                            isProcessing = false,
                            errorMessage = result.message
                        )
                    )
                }
            }
        }
    }
    
    /**
     * Executes the action that was pending manager approval.
     */
    private suspend fun executeApprovedAction(approvalState: ManagerApprovalDialogState) {
        when (approvalState.action) {
            RequestAction.VOID_TRANSACTION -> {
                if (approvalState.pendingItemId != null) {
                    // Line item void
                    cartRepository.voidItem(approvalState.pendingItemId)
                    onDeselectLineItem()
                } else {
                    // Full transaction void - show confirmation dialog
                    showVoidConfirmationDialog()
                }
            }
            RequestAction.CASH_PICKUP -> {
                // Cash pickup was approved - execute with pending amount
                val inputValue = _state.value.cashPickupDialogState.inputValue
                val amountCents = inputValue.toLongOrNull() ?: 0
                val amount = BigDecimal(amountCents).divide(BigDecimal(100))
                
                // Get the approving manager ID
                val approverId = approvalState.managers.firstOrNull()?.id?.toString() ?: "Unknown"
                
                executeCashPickup(amount, approverId)
            }
            RequestAction.VENDOR_PAYOUT -> {
                // Vendor payout was approved - execute with pending vendor and amount
                val payoutState = _state.value.vendorPayoutDialogState
                val inputValue = payoutState.inputValue
                val amountCents = inputValue.toLongOrNull() ?: 0
                val amount = BigDecimal(amountCents).divide(BigDecimal(100))
                
                // Get the approving manager ID
                val approverId = approvalState.managers.firstOrNull()?.id?.toString() ?: "Unknown"
                
                executeVendorPayout(
                    amount = amount,
                    vendorId = payoutState.selectedVendorId ?: "",
                    vendorName = payoutState.selectedVendorName ?: "Unknown",
                    approverId = approverId
                )
            }
            RequestAction.LINE_DISCOUNT -> {
                // TODO: Apply discount once discount flow is implemented
            }
            RequestAction.PRICE_OVERRIDE -> {
                // TODO: Apply price override once implemented
            }
            else -> {
                // Other actions to be implemented
            }
        }
    }
    
    // ========================================================================
    // Void Transaction
    // Per FUNCTIONS_MENU.md: Void Transaction cancels entire transaction
    // ========================================================================
    
    /**
     * Handles the "Void Transaction" button press from the Functions Panel.
     * 
     * Per FUNCTIONS_MENU.md:
     * - Only available when cart has items
     * - Not available when payment already applied
     * 
     * Per ROLES_AND_PERMISSIONS.md:
     * - Requires `GroPOS.Transactions.Void` permission
     * - Cashiers need manager approval, managers can self-approve
     */
    fun onVoidTransactionRequest() {
        // Check if cart is empty - can't void empty transaction
        if (_state.value.isEmpty) {
            _state.value = _state.value.copy(
                lastScanEvent = ScanEvent.Error("No transaction to void.")
            )
            return
        }
        
        // Cancel modification mode if active
        if (_state.value.isModificationMode) {
            onDeselectLineItem()
        }
        
        val user = currentUser
        if (user == null) {
            _state.value = _state.value.copy(
                lastScanEvent = ScanEvent.Error("User session not found.")
            )
            return
        }
        
        // Check permission
        val permissionResult = PermissionManager.checkPermission(user, RequestAction.VOID_TRANSACTION)
        
        when (permissionResult) {
            PermissionCheckResult.GRANTED,
            PermissionCheckResult.SELF_APPROVAL_ALLOWED -> {
                // Can void directly - show confirmation dialog
                showVoidConfirmationDialog()
            }
            PermissionCheckResult.REQUIRES_APPROVAL -> {
                // Show manager approval dialog (no pending item ID for full transaction)
                showManagerApprovalDialog(
                    action = RequestAction.VOID_TRANSACTION,
                    pendingItemId = null
                )
            }
            PermissionCheckResult.DENIED -> {
                _state.value = _state.value.copy(
                    lastScanEvent = ScanEvent.Error("You do not have permission to void transactions.")
                )
            }
        }
    }
    
    /**
     * Shows the void confirmation dialog.
     */
    private fun showVoidConfirmationDialog() {
        _state.value = _state.value.copy(
            showVoidConfirmationDialog = true
        )
    }
    
    /**
     * Confirms and executes the void transaction.
     * 
     * Called when user clicks "Yes, Void It" in the confirmation dialog.
     */
    fun onConfirmVoidTransaction() {
        effectiveScope.launch {
            // Log the void (audit trail)
            val cart = cartRepository.getCurrentCart()
            val user = currentUser
            println("================================================================================")
            println("[AUDIT] VOID TRANSACTION")
            println("================================================================================")
            println("Timestamp: ${java.time.LocalDateTime.now()}")
            println("Operator: ${user?.username ?: "Unknown"} (ID: ${user?.id ?: "?"})")
            println("Items Voided: ${cart.itemCount}")
            println("Total Voided: ${currencyFormatter.format(cart.grandTotal)}")
            println("================================================================================")
            
            // Clear the cart
            cartRepository.clearCart()
            
            // Close dialog and reset state
            _state.value = _state.value.copy(
                showVoidConfirmationDialog = false,
                lastScanEvent = ScanEvent.Error("Transaction voided.") // Use Error type for visibility
            )
        }
    }
    
    /**
     * Cancels the void transaction dialog.
     * 
     * Called when user clicks "No, Keep It" in the confirmation dialog.
     */
    fun onCancelVoidTransaction() {
        _state.value = _state.value.copy(
            showVoidConfirmationDialog = false
        )
    }
    
    // ========================================================================
    // Logout Flow
    // Per CASHIER_OPERATIONS.md: Logout options (Lock, Release Till, End Shift)
    // ========================================================================
    
    /**
     * Opens the logout dialog.
     * 
     * Per CASHIER_OPERATIONS.md: Show logout options when "Sign Out" clicked.
     */
    fun onOpenLogoutDialog() {
        _state.value = _state.value.copy(
            showLogoutDialog = true
        )
    }
    
    /**
     * Dismisses the logout dialog.
     */
    fun onDismissLogoutDialog() {
        _state.value = _state.value.copy(
            showLogoutDialog = false
        )
    }
    
    /**
     * Locks the station (keeps session active).
     * 
     * Per CASHIER_OPERATIONS.md: User can lock without fully logging out.
     */
    fun onLockStation() {
        _state.value = _state.value.copy(
            showLogoutDialog = false
        )
        
        // Trigger the inactivity manager to lock (manual lock)
        InactivityManager.manualLock()
    }
    
    /**
     * Releases the till and logs out.
     * 
     * Per CASHIER_OPERATIONS.md: Quick logout that frees the drawer.
     */
    fun onReleaseTill() {
        // Safety check: cart must be empty
        if (!_state.value.isEmpty) {
            _state.value = _state.value.copy(
                showLogoutDialog = false,
                lastScanEvent = ScanEvent.Error("Clear the cart before signing out.")
            )
            return
        }
        
        _state.value = _state.value.copy(
            showLogoutDialog = false
        )
        
        effectiveScope.launch {
            val result = cashierSessionManager.releaseTill()
            result.onSuccess { tillName ->
                _state.value = _state.value.copy(
                    logoutFeedback = "Till Released: $tillName"
                )
            }.onFailure { error ->
                _state.value = _state.value.copy(
                    lastScanEvent = ScanEvent.Error("Failed to release till: ${error.message}")
                )
            }
        }
    }
    
    /**
     * Ends the shift with a report.
     * 
     * Per CASHIER_OPERATIONS.md: Full close-out with Z-Report.
     */
    fun onEndShift() {
        // Safety check: cart must be empty
        if (!_state.value.isEmpty) {
            _state.value = _state.value.copy(
                showLogoutDialog = false,
                lastScanEvent = ScanEvent.Error("Clear the cart before ending shift.")
            )
            return
        }
        
        _state.value = _state.value.copy(
            showLogoutDialog = false
        )
        
        effectiveScope.launch {
            val result = cashierSessionManager.endShift()
            result.onSuccess { report ->
                // Print the shift report to console (virtual printer)
                println(report.formatForPrint())
                
                _state.value = _state.value.copy(
                    logoutFeedback = "Shift ended. Report printed."
                )
            }.onFailure { error ->
                _state.value = _state.value.copy(
                    lastScanEvent = ScanEvent.Error("Failed to end shift: ${error.message}")
                )
            }
        }
    }
    
    /**
     * Dismisses the logout feedback message.
     */
    fun onDismissLogoutFeedback() {
        _state.value = _state.value.copy(
            logoutFeedback = null
        )
    }
    
    // ========================================================================
    // Hold/Recall Transaction
    // Per TRANSACTION_FLOW.md: Suspend and Resume transactions
    // ========================================================================
    
    /**
     * Opens the hold transaction dialog.
     * 
     * Per TRANSACTION_FLOW.md: Hold button opens dialog for optional name/note.
     * Per Governance: Cannot hold an empty cart.
     */
    fun onOpenHoldDialog() {
        // Validation: Cannot hold an empty cart
        if (_state.value.isEmpty) {
            _state.value = _state.value.copy(
                lastScanEvent = ScanEvent.Error("Cannot hold an empty cart.")
            )
            return
        }
        
        _state.value = _state.value.copy(
            holdDialogState = HoldDialogState(isVisible = true)
        )
    }
    
    /**
     * Dismisses the hold transaction dialog.
     */
    fun onDismissHoldDialog() {
        _state.value = _state.value.copy(
            holdDialogState = HoldDialogState(isVisible = false)
        )
    }
    
    /**
     * Confirms and executes the hold transaction.
     * 
     * Per TRANSACTION_FLOW.md:
     * - Creates a HeldTransaction record
     * - Clears the active cart
     * - Shows "Transaction Held" feedback
     * 
     * @param holdName Optional name/note for the held transaction
     */
    fun onConfirmHold(holdName: String?) {
        effectiveScope.launch {
            val cart = cartRepository.getCurrentCart()
            val user = currentUser
            
            // Generate hold name if not provided
            val now = LocalDateTime.now()
            val generatedName = holdName ?: "Hold-${now.format(DateTimeFormatter.ofPattern("HH:mm"))}"
            
            // Create HeldTransaction from cart
            val heldTransaction = HeldTransaction(
                id = UUID.randomUUID().toString(),
                holdName = generatedName,
                heldDateTime = now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                employeeId = user?.id?.toIntOrNull(),
                employeeName = user?.username,
                stationId = 1,
                branchId = 1,
                itemCount = cart.itemCount.toInt(),
                grandTotal = cart.grandTotal,
                subTotal = cart.subTotal,
                taxTotal = cart.taxTotal,
                crvTotal = cart.crvTotal,
                items = cart.items.filterNot { it.isRemoved }.map { cartItem ->
                    HeldTransactionItem(
                        branchProductId = cartItem.branchProductId,
                        productName = cartItem.branchProductName,
                        quantityUsed = cartItem.quantityUsed,
                        priceUsed = cartItem.effectivePrice,
                        discountAmountPerUnit = cartItem.discountAmountPerUnit,
                        transactionDiscountAmountPerUnit = cartItem.transactionDiscountAmountPerUnit,
                        isRemoved = cartItem.isRemoved,
                        isPromptedPrice = cartItem.isPromptedPrice,
                        isFloorPriceOverridden = cartItem.isFloorPriceOverridden,
                        scanDateTime = cartItem.scanDateTime
                    )
                }
            )
            
            // Save to repository
            val result = transactionRepository.holdTransaction(heldTransaction)
            
            result.onSuccess {
                // Clear the cart
                cartRepository.clearCart()
                
                // Close dialog and show feedback
                _state.value = _state.value.copy(
                    holdDialogState = HoldDialogState(isVisible = false),
                    holdRecallFeedback = "Transaction Held: $generatedName"
                )
                
                println("[HOLD] Transaction held: $generatedName with ${heldTransaction.itemCount} items")
            }.onFailure { error ->
                _state.value = _state.value.copy(
                    holdDialogState = HoldDialogState(isVisible = false),
                    lastScanEvent = ScanEvent.Error("Failed to hold transaction: ${error.message}")
                )
            }
        }
    }
    
    /**
     * Opens the recall transactions dialog.
     * 
     * Per TRANSACTION_FLOW.md: Show list of HELD transactions.
     */
    fun onOpenRecallDialog() {
        _state.value = _state.value.copy(
            recallDialogState = RecallDialogState(
                isVisible = true,
                isLoading = true,
                heldTransactions = emptyList()
            )
        )
        
        // Load held transactions
        effectiveScope.launch {
            val heldTransactions = transactionRepository.getHeldTransactions()
            
            val uiModels = heldTransactions.map { held ->
                HeldTransactionUiModel(
                    id = held.id,
                    holdName = held.holdName,
                    heldDateTime = formatHeldDateTime(held.heldDateTime),
                    itemCount = formatItemCount(BigDecimal(held.itemCount)),
                    grandTotal = currencyFormatter.format(held.grandTotal),
                    employeeName = held.employeeName
                )
            }
            
            _state.value = _state.value.copy(
                recallDialogState = RecallDialogState(
                    isVisible = true,
                    isLoading = false,
                    heldTransactions = uiModels
                )
            )
        }
    }
    
    /**
     * Dismisses the recall transactions dialog.
     */
    fun onDismissRecallDialog() {
        _state.value = _state.value.copy(
            recallDialogState = RecallDialogState(isVisible = false)
        )
    }
    
    /**
     * Restores a held transaction to the cart.
     * 
     * Per TRANSACTION_FLOW.md:
     * - Loads items back into CartRepository
     * - Deletes the HELD record
     * - Shows "Transaction Restored" feedback
     * 
     * Per Governance: Restoring must strictly match original items.
     * 
     * @param heldTransactionId The ID of the held transaction to restore
     */
    fun onRestoreTransaction(heldTransactionId: String) {
        effectiveScope.launch {
            // Check if current cart is not empty
            if (!_state.value.isEmpty) {
                _state.value = _state.value.copy(
                    recallDialogState = RecallDialogState(isVisible = false),
                    lastScanEvent = ScanEvent.Error("Clear the current cart before recalling a transaction.")
                )
                return@launch
            }
            
            // Get the held transaction
            val heldTransaction = transactionRepository.getHeldTransactionById(heldTransactionId)
            
            if (heldTransaction == null) {
                _state.value = _state.value.copy(
                    recallDialogState = RecallDialogState(isVisible = false),
                    lastScanEvent = ScanEvent.Error("Held transaction not found.")
                )
                return@launch
            }
            
            // Restore items to cart
            var restoredCount = 0
            for (heldItem in heldTransaction.items) {
                if (heldItem.isRemoved) continue
                
                // Load product from repository
                val product = productRepository.getById(heldItem.branchProductId)
                if (product != null) {
                    // Add to cart with original quantity
                    cartRepository.addToCart(product, heldItem.quantityUsed)
                    restoredCount++
                } else {
                    println("[RECALL] Warning: Product ${heldItem.branchProductId} not found, skipping")
                }
            }
            
            // Delete the held record
            transactionRepository.deleteHeldTransaction(heldTransactionId)
            
            // Close dialog and show feedback
            _state.value = _state.value.copy(
                recallDialogState = RecallDialogState(isVisible = false),
                holdRecallFeedback = "Transaction Restored: ${heldTransaction.holdName}"
            )
            
            println("[RECALL] Restored ${restoredCount} items from: ${heldTransaction.holdName}")
        }
    }
    
    /**
     * Deletes a held transaction without restoring.
     * 
     * @param heldTransactionId The ID of the held transaction to delete
     */
    fun onDeleteHeldTransaction(heldTransactionId: String) {
        effectiveScope.launch {
            val result = transactionRepository.deleteHeldTransaction(heldTransactionId)
            
            result.onSuccess {
                // Refresh the list
                onOpenRecallDialog()
            }.onFailure { error ->
                _state.value = _state.value.copy(
                    lastScanEvent = ScanEvent.Error("Failed to delete: ${error.message}")
                )
            }
        }
    }
    
    /**
     * Dismisses the hold/recall feedback message.
     */
    fun onDismissHoldRecallFeedback() {
        _state.value = _state.value.copy(
            holdRecallFeedback = null
        )
    }
    
    /**
     * Formats the held date/time for display.
     */
    private fun formatHeldDateTime(isoDateTime: String): String {
        return try {
            val dateTime = LocalDateTime.parse(isoDateTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            dateTime.format(DateTimeFormatter.ofPattern("MMM d, h:mm a"))
        } catch (e: Exception) {
            isoDateTime
        }
    }
    
    // ========================================================================
    // Product Lookup Dialog
    // Per SCREEN_LAYOUTS.md: Product Lookup Dialog for manual product selection
    // ========================================================================
    
    /**
     * Opens the Product Lookup Dialog.
     * 
     * Loads categories and initial product list.
     */
    fun onOpenLookup() {
        effectiveScope.launch {
            _state.value = _state.value.copy(
                lookupState = _state.value.lookupState.copy(
                    isVisible = true,
                    isLoading = true,
                    searchQuery = "",
                    selectedCategoryId = null
                )
            )
            
            try {
                val categories = productRepository.getCategories()
                val products = productRepository.searchProducts("")
                
                _state.value = _state.value.copy(
                    lookupState = _state.value.lookupState.copy(
                        categories = categories,
                        products = products.map { mapProductToLookupUiModel(it) },
                        isLoading = false
                    )
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    lookupState = _state.value.lookupState.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load products"
                    )
                )
            }
        }
    }
    
    /**
     * Closes the Product Lookup Dialog.
     */
    fun onCloseLookup() {
        _state.value = _state.value.copy(
            lookupState = ProductLookupState()
        )
    }
    
    /**
     * Handles search query changes in the lookup dialog.
     * 
     * @param query The search query (product name or barcode)
     */
    fun onLookupSearchChange(query: String) {
        _state.value = _state.value.copy(
            lookupState = _state.value.lookupState.copy(
                searchQuery = query,
                isLoading = true
            )
        )
        
        effectiveScope.launch {
            try {
                val products = if (query.isBlank() && _state.value.lookupState.selectedCategoryId != null) {
                    productRepository.getByCategory(_state.value.lookupState.selectedCategoryId!!)
                } else {
                    productRepository.searchProducts(query)
                }
                
                _state.value = _state.value.copy(
                    lookupState = _state.value.lookupState.copy(
                        products = products.map { mapProductToLookupUiModel(it) },
                        isLoading = false
                    )
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    lookupState = _state.value.lookupState.copy(
                        isLoading = false,
                        error = e.message ?: "Search failed"
                    )
                )
            }
        }
    }
    
    /**
     * Handles category selection in the lookup dialog.
     * 
     * @param categoryId The selected category ID (null for "All Products")
     */
    fun onLookupCategorySelect(categoryId: Int?) {
        _state.value = _state.value.copy(
            lookupState = _state.value.lookupState.copy(
                selectedCategoryId = categoryId,
                isLoading = true,
                searchQuery = "" // Clear search when switching categories
            )
        )
        
        effectiveScope.launch {
            try {
                val products = if (categoryId != null) {
                    productRepository.getByCategory(categoryId)
                } else {
                    productRepository.searchProducts("")
                }
                
                _state.value = _state.value.copy(
                    lookupState = _state.value.lookupState.copy(
                        products = products.map { mapProductToLookupUiModel(it) },
                        isLoading = false
                    )
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    lookupState = _state.value.lookupState.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load category"
                    )
                )
            }
        }
    }
    
    /**
     * Handles product selection from the lookup dialog.
     * 
     * Per requirement: Call cartRepository.addToCart(product) -> Close Dialog.
     * Per DIALOGS.md: If age-restricted, show Age Verification Dialog first.
     * 
     * @param productUiModel The selected product
     */
    fun onProductSelected(productUiModel: ProductLookupUiModel) {
        effectiveScope.launch {
            try {
                // Get the full product by ID
                val product = productRepository.getById(productUiModel.branchProductId)
                if (product != null) {
                    // Close lookup dialog first
                    onCloseLookup()
                    
                    // Check for age restriction BEFORE adding to cart
                    if (product.isAgeRestricted) {
                        checkAndHandleAgeRestriction(product)
                        return@launch
                    }
                    
                    // Add to cart (not age-restricted)
                    cartRepository.addToCart(product)
                    
                    // Show success feedback
                    _state.value = _state.value.copy(
                        lastScanEvent = ScanEvent.ProductAdded(product.productName)
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    lastScanEvent = ScanEvent.Error(e.message ?: "Failed to add product")
                )
                // Close the dialog on error
                onCloseLookup()
            }
        }
    }
    
    /**
     * Maps a Product domain model to ProductLookupUiModel.
     */
    private fun mapProductToLookupUiModel(product: Product): ProductLookupUiModel {
        return ProductLookupUiModel(
            branchProductId = product.branchProductId,
            name = product.productName,
            price = currencyFormatter.format(product.retailPrice),
            // imageUrl is not in our current Product model, default to null
            imageUrl = null,
            isSnapEligible = product.isSnapEligible,
            barcode = product.itemNumbers.firstOrNull()?.itemNumber
        )
    }
    
    // ========================================================================
    // Cash Pickup
    // Per FUNCTIONS_MENU.md: Cash Pickup removes cash from drawer for safe
    // ========================================================================
    
    /**
     * Opens the Cash Pickup dialog.
     * 
     * Per FUNCTIONS_MENU.md:
     * - Prerequisites: No active payments in current transaction
     * - Requires Manager approval
     */
    fun onOpenCashPickupDialog() {
        // Validation: Cannot do cash pickup with items in cart
        if (!_state.value.isEmpty) {
            _state.value = _state.value.copy(
                lastScanEvent = ScanEvent.Error("Complete or void the current transaction before cash pickup.")
            )
            return
        }
        
        // Get current drawer balance
        val currentBalance = cashierSessionManager.getCurrentDrawerBalance()
        
        _state.value = _state.value.copy(
            cashPickupDialogState = CashPickupDialogState(
                isVisible = true,
                inputValue = "",
                currentBalance = currencyFormatter.format(currentBalance),
                errorMessage = null
            )
        )
    }
    
    /**
     * Handles digit press in Cash Pickup dialog.
     */
    fun onCashPickupDigitPress(digit: String) {
        val currentInput = _state.value.cashPickupDialogState.inputValue
        val newInput = currentInput + digit
        
        // Limit to reasonable input length
        if (newInput.length <= 10) {
            _state.value = _state.value.copy(
                cashPickupDialogState = _state.value.cashPickupDialogState.copy(
                    inputValue = newInput,
                    errorMessage = null
                )
            )
        }
    }
    
    /**
     * Clears the Cash Pickup input.
     */
    fun onCashPickupClear() {
        _state.value = _state.value.copy(
            cashPickupDialogState = _state.value.cashPickupDialogState.copy(
                inputValue = "",
                errorMessage = null
            )
        )
    }
    
    /**
     * Handles backspace in Cash Pickup dialog.
     */
    fun onCashPickupBackspace() {
        val currentInput = _state.value.cashPickupDialogState.inputValue
        if (currentInput.isNotEmpty()) {
            _state.value = _state.value.copy(
                cashPickupDialogState = _state.value.cashPickupDialogState.copy(
                    inputValue = currentInput.dropLast(1),
                    errorMessage = null
                )
            )
        }
    }
    
    /**
     * Initiates the cash pickup with permission check.
     * 
     * Per FUNCTIONS_MENU.md: Manager approval required.
     * Per Governance: Cannot pickup more than drawer balance.
     */
    fun onCashPickupConfirm() {
        val inputValue = _state.value.cashPickupDialogState.inputValue
        if (inputValue.isBlank()) {
            _state.value = _state.value.copy(
                cashPickupDialogState = _state.value.cashPickupDialogState.copy(
                    errorMessage = "Please enter an amount"
                )
            )
            return
        }
        
        // Parse the amount (input is in cents, e.g., "5000" = $50.00)
        val amountCents = inputValue.toLongOrNull() ?: 0
        val amount = BigDecimal(amountCents).divide(BigDecimal(100))
        
        if (amount <= BigDecimal.ZERO) {
            _state.value = _state.value.copy(
                cashPickupDialogState = _state.value.cashPickupDialogState.copy(
                    errorMessage = "Amount must be greater than zero"
                )
            )
            return
        }
        
        // Validate against drawer balance
        val currentBalance = cashierSessionManager.getCurrentDrawerBalance()
        if (amount > currentBalance) {
            _state.value = _state.value.copy(
                cashPickupDialogState = _state.value.cashPickupDialogState.copy(
                    errorMessage = "Cannot pickup more than drawer balance (${currencyFormatter.format(currentBalance)})"
                )
            )
            return
        }
        
        val user = currentUser
        if (user == null) {
            _state.value = _state.value.copy(
                cashPickupDialogState = _state.value.cashPickupDialogState.copy(
                    errorMessage = "User session not found"
                )
            )
            return
        }
        
        // Check permission - Cash Pickup ALWAYS requires manager approval for cashiers
        val permissionResult = PermissionManager.checkPermission(user, RequestAction.CASH_PICKUP)
        
        when (permissionResult) {
            PermissionCheckResult.GRANTED,
            PermissionCheckResult.SELF_APPROVAL_ALLOWED -> {
                // Manager can self-approve - proceed directly
                executeCashPickup(amount, user.id)
            }
            PermissionCheckResult.REQUIRES_APPROVAL -> {
                // Close pickup dialog and show manager approval
                _state.value = _state.value.copy(
                    cashPickupDialogState = _state.value.cashPickupDialogState.copy(
                        isVisible = false,
                        approvalPending = true
                    )
                )
                showManagerApprovalDialogForCashPickup(amount)
            }
            PermissionCheckResult.DENIED -> {
                _state.value = _state.value.copy(
                    cashPickupDialogState = _state.value.cashPickupDialogState.copy(
                        errorMessage = "You do not have permission for cash pickup"
                    )
                )
            }
        }
    }
    
    /**
     * Shows manager approval dialog for cash pickup.
     */
    private fun showManagerApprovalDialogForCashPickup(amount: BigDecimal) {
        val user = currentUser ?: return
        
        effectiveScope.launch {
            val managers = managerApprovalService.getApprovers(RequestAction.CASH_PICKUP, user)
            
            _state.value = _state.value.copy(
                managerApprovalState = ManagerApprovalDialogState(
                    isVisible = true,
                    action = RequestAction.CASH_PICKUP,
                    managers = managers,
                    isProcessing = false,
                    errorMessage = null
                )
            )
        }
    }
    
    /**
     * Executes the cash pickup after approval.
     * 
     * @param amount The amount to pick up
     * @param approverId The ID of the approving manager (or self for managers)
     */
    private fun executeCashPickup(amount: BigDecimal, approverId: String) {
        effectiveScope.launch {
            _state.value = _state.value.copy(
                cashPickupDialogState = _state.value.cashPickupDialogState.copy(
                    isProcessing = true
                )
            )
            
            val result = cashierSessionManager.cashPickup(amount, approverId)
            
            result.onSuccess {
                // Print virtual receipt
                printCashPickupReceipt(amount, approverId)
                
                // Close dialog and show success feedback
                _state.value = _state.value.copy(
                    cashPickupDialogState = CashPickupDialogState(),
                    managerApprovalState = ManagerApprovalDialogState(),
                    cashPickupFeedback = "Pickup Recorded: ${currencyFormatter.format(amount)}"
                )
            }.onFailure { error ->
                _state.value = _state.value.copy(
                    cashPickupDialogState = _state.value.cashPickupDialogState.copy(
                        isVisible = true,
                        isProcessing = false,
                        errorMessage = error.message ?: "Failed to record pickup"
                    )
                )
            }
        }
    }
    
    /**
     * Prints a virtual receipt for the cash pickup.
     */
    private fun printCashPickupReceipt(amount: BigDecimal, approverId: String) {
        val session = cashierSessionManager.getCurrentSession()
        
        println("================================================================================")
        println("                           CASH PICKUP RECEIPT")
        println("================================================================================")
        println()
        println("Date/Time:      ${java.time.LocalDateTime.now()}")
        println("Employee:       ${session?.employeeName ?: "Unknown"}")
        println("Till:           Till ${session?.tillId ?: "?"}")
        println("Manager Approval: $approverId")
        println()
        println("--------------------------------------------------------------------------------")
        println("PICKUP AMOUNT:  ${currencyFormatter.format(amount)}")
        println("--------------------------------------------------------------------------------")
        println()
        println("New Drawer Balance: ${currencyFormatter.format(cashierSessionManager.getCurrentDrawerBalance())}")
        println()
        println("________________________________________________________________________________")
        println("Cashier Signature")
        println()
        println("________________________________________________________________________________")
        println("Manager Signature")
        println()
        println("================================================================================")
    }
    
    /**
     * Dismisses the Cash Pickup dialog.
     */
    fun onDismissCashPickupDialog() {
        _state.value = _state.value.copy(
            cashPickupDialogState = CashPickupDialogState()
        )
    }
    
    /**
     * Dismisses the cash pickup feedback message.
     */
    fun onDismissCashPickupFeedback() {
        _state.value = _state.value.copy(
            cashPickupFeedback = null
        )
    }
    
    // ========================================================================
    // Vendor Payout
    // Per FUNCTIONS_MENU.md: Vendor Payout pays vendors directly from the till
    // ========================================================================
    
    /**
     * Opens the Vendor Payout dialog.
     * 
     * Per FUNCTIONS_MENU.md:
     * - Prerequisites: No active payments in current transaction
     * - Flow: Select vendor → Enter amount → Manager approval → Cash dispensed
     */
    fun onOpenVendorPayoutDialog() {
        // Validation: Cannot do vendor payout with items in cart
        if (!_state.value.isEmpty) {
            _state.value = _state.value.copy(
                lastScanEvent = ScanEvent.Error("Complete or void the current transaction before vendor payout.")
            )
            return
        }
        
        // Get current drawer balance
        val currentBalance = cashierSessionManager.getCurrentDrawerBalance()
        
        // Load vendors
        effectiveScope.launch {
            val vendors = vendorRepository.getVendors()
            
            _state.value = _state.value.copy(
                vendorPayoutDialogState = VendorPayoutDialogState(
                    isVisible = true,
                    step = VendorPayoutStep.VENDOR_SELECTION,
                    vendors = vendors.map { VendorUiModel(it.id, it.name) },
                    currentBalance = currencyFormatter.format(currentBalance),
                    inputValue = "",
                    errorMessage = null
                )
            )
        }
    }
    
    /**
     * Handles vendor selection in step 1.
     * 
     * @param vendorId The ID of the selected vendor
     * @param vendorName The name of the selected vendor
     */
    fun onVendorPayoutSelectVendor(vendorId: String, vendorName: String) {
        _state.value = _state.value.copy(
            vendorPayoutDialogState = _state.value.vendorPayoutDialogState.copy(
                step = VendorPayoutStep.AMOUNT_INPUT,
                selectedVendorId = vendorId,
                selectedVendorName = vendorName,
                inputValue = "",
                errorMessage = null
            )
        )
    }
    
    /**
     * Handles back button in step 2 (returns to step 1).
     */
    fun onVendorPayoutBack() {
        _state.value = _state.value.copy(
            vendorPayoutDialogState = _state.value.vendorPayoutDialogState.copy(
                step = VendorPayoutStep.VENDOR_SELECTION,
                selectedVendorId = null,
                selectedVendorName = null,
                inputValue = "",
                errorMessage = null
            )
        )
    }
    
    /**
     * Handles digit press in Vendor Payout dialog.
     */
    fun onVendorPayoutDigitPress(digit: String) {
        val currentInput = _state.value.vendorPayoutDialogState.inputValue
        val newInput = currentInput + digit
        
        // Limit to reasonable input length
        if (newInput.length <= 10) {
            _state.value = _state.value.copy(
                vendorPayoutDialogState = _state.value.vendorPayoutDialogState.copy(
                    inputValue = newInput,
                    errorMessage = null
                )
            )
        }
    }
    
    /**
     * Clears the Vendor Payout input.
     */
    fun onVendorPayoutClear() {
        _state.value = _state.value.copy(
            vendorPayoutDialogState = _state.value.vendorPayoutDialogState.copy(
                inputValue = "",
                errorMessage = null
            )
        )
    }
    
    /**
     * Handles backspace in Vendor Payout dialog.
     */
    fun onVendorPayoutBackspace() {
        val currentInput = _state.value.vendorPayoutDialogState.inputValue
        if (currentInput.isNotEmpty()) {
            _state.value = _state.value.copy(
                vendorPayoutDialogState = _state.value.vendorPayoutDialogState.copy(
                    inputValue = currentInput.dropLast(1),
                    errorMessage = null
                )
            )
        }
    }
    
    /**
     * Initiates the vendor payout with permission check.
     * 
     * Per FUNCTIONS_MENU.md: Manager approval required.
     * Per Governance: Cannot pay out more than drawer balance.
     */
    fun onVendorPayoutConfirm() {
        val payoutState = _state.value.vendorPayoutDialogState
        val inputValue = payoutState.inputValue
        
        if (inputValue.isBlank()) {
            _state.value = _state.value.copy(
                vendorPayoutDialogState = payoutState.copy(
                    errorMessage = "Please enter an amount"
                )
            )
            return
        }
        
        if (payoutState.selectedVendorId == null) {
            _state.value = _state.value.copy(
                vendorPayoutDialogState = payoutState.copy(
                    errorMessage = "Please select a vendor"
                )
            )
            return
        }
        
        // Parse the amount (input is in cents, e.g., "5000" = $50.00)
        val amountCents = inputValue.toLongOrNull() ?: 0
        val amount = BigDecimal(amountCents).divide(BigDecimal(100))
        
        if (amount <= BigDecimal.ZERO) {
            _state.value = _state.value.copy(
                vendorPayoutDialogState = payoutState.copy(
                    errorMessage = "Amount must be greater than zero"
                )
            )
            return
        }
        
        // Validate against drawer balance
        val currentBalance = cashierSessionManager.getCurrentDrawerBalance()
        if (amount > currentBalance) {
            _state.value = _state.value.copy(
                vendorPayoutDialogState = payoutState.copy(
                    errorMessage = "Cannot pay out more than drawer balance (${currencyFormatter.format(currentBalance)})"
                )
            )
            return
        }
        
        val user = currentUser
        if (user == null) {
            _state.value = _state.value.copy(
                vendorPayoutDialogState = payoutState.copy(
                    errorMessage = "User session not found"
                )
            )
            return
        }
        
        // Check permission - Vendor Payout requires manager approval for cashiers
        val permissionResult = PermissionManager.checkPermission(user, RequestAction.VENDOR_PAYOUT)
        
        when (permissionResult) {
            PermissionCheckResult.GRANTED,
            PermissionCheckResult.SELF_APPROVAL_ALLOWED -> {
                // Manager can self-approve - proceed directly
                executeVendorPayout(
                    amount = amount,
                    vendorId = payoutState.selectedVendorId,
                    vendorName = payoutState.selectedVendorName ?: "Unknown",
                    approverId = user.id
                )
            }
            PermissionCheckResult.REQUIRES_APPROVAL -> {
                // Close payout dialog and show manager approval
                _state.value = _state.value.copy(
                    vendorPayoutDialogState = payoutState.copy(
                        isVisible = false,
                        approvalPending = true
                    )
                )
                showManagerApprovalDialogForVendorPayout(amount)
            }
            PermissionCheckResult.DENIED -> {
                _state.value = _state.value.copy(
                    vendorPayoutDialogState = payoutState.copy(
                        errorMessage = "You do not have permission for vendor payout"
                    )
                )
            }
        }
    }
    
    /**
     * Shows manager approval dialog for vendor payout.
     */
    private fun showManagerApprovalDialogForVendorPayout(amount: BigDecimal) {
        val user = currentUser ?: return
        
        effectiveScope.launch {
            val managers = managerApprovalService.getApprovers(RequestAction.VENDOR_PAYOUT, user)
            
            _state.value = _state.value.copy(
                managerApprovalState = ManagerApprovalDialogState(
                    isVisible = true,
                    action = RequestAction.VENDOR_PAYOUT,
                    managers = managers,
                    isProcessing = false,
                    errorMessage = null
                )
            )
        }
    }
    
    /**
     * Executes the vendor payout after approval.
     * 
     * @param amount The amount to pay out
     * @param vendorId The ID of the vendor receiving the payout
     * @param vendorName The name of the vendor for audit logging
     * @param approverId The ID of the approving manager (or self for managers)
     */
    private fun executeVendorPayout(
        amount: BigDecimal,
        vendorId: String,
        vendorName: String,
        approverId: String
    ) {
        effectiveScope.launch {
            _state.value = _state.value.copy(
                vendorPayoutDialogState = _state.value.vendorPayoutDialogState.copy(
                    isProcessing = true
                )
            )
            
            val result = cashierSessionManager.vendorPayout(
                amount = amount,
                vendorId = vendorId,
                vendorName = vendorName,
                managerId = approverId
            )
            
            result.onSuccess {
                // Print virtual receipt
                printVendorPayoutReceipt(amount, vendorId, vendorName, approverId)
                
                // Close dialog and show success feedback
                _state.value = _state.value.copy(
                    vendorPayoutDialogState = VendorPayoutDialogState(),
                    managerApprovalState = ManagerApprovalDialogState(),
                    vendorPayoutFeedback = "Payout Recorded: ${currencyFormatter.format(amount)} to $vendorName"
                )
            }.onFailure { error ->
                _state.value = _state.value.copy(
                    vendorPayoutDialogState = _state.value.vendorPayoutDialogState.copy(
                        isVisible = true,
                        isProcessing = false,
                        errorMessage = error.message ?: "Failed to record payout"
                    )
                )
            }
        }
    }
    
    /**
     * Prints a virtual receipt for the vendor payout.
     */
    private fun printVendorPayoutReceipt(
        amount: BigDecimal,
        vendorId: String,
        vendorName: String,
        approverId: String
    ) {
        val session = cashierSessionManager.getCurrentSession()
        
        println("================================================================================")
        println("                          VENDOR PAYOUT RECEIPT")
        println("================================================================================")
        println()
        println("Date/Time:       ${java.time.LocalDateTime.now()}")
        println("Employee:        ${session?.employeeName ?: "Unknown"}")
        println("Till:            Till ${session?.tillId ?: "?"}")
        println("Manager Approval: $approverId")
        println()
        println("--------------------------------------------------------------------------------")
        println("VENDOR:          $vendorName (ID: $vendorId)")
        println("PAYOUT AMOUNT:   ${currencyFormatter.format(amount)}")
        println("--------------------------------------------------------------------------------")
        println()
        println("New Drawer Balance: ${currencyFormatter.format(cashierSessionManager.getCurrentDrawerBalance())}")
        println()
        println("________________________________________________________________________________")
        println("Cashier Signature")
        println()
        println("________________________________________________________________________________")
        println("Manager Signature")
        println()
        println("________________________________________________________________________________")
        println("Vendor Signature")
        println()
        println("================================================================================")
    }
    
    /**
     * Dismisses the Vendor Payout dialog.
     */
    fun onDismissVendorPayoutDialog() {
        _state.value = _state.value.copy(
            vendorPayoutDialogState = VendorPayoutDialogState()
        )
    }
    
    /**
     * Dismisses the vendor payout feedback message.
     */
    fun onDismissVendorPayoutFeedback() {
        _state.value = _state.value.copy(
            vendorPayoutFeedback = null
        )
    }
    
    /**
     * Maps Cart domain model to CheckoutUiState.
     * 
     * Per Governance: All formatting done here, not in UI.
     */
    private fun updateStateFromCart(cart: Cart) {
        val items = cart.items
            .filterNot { it.isRemoved }
            .map { cartItem -> mapToUiModel(cartItem) }
        
        val totals = CheckoutTotalsUiModel(
            subtotal = currencyFormatter.format(cart.subTotal),
            taxTotal = currencyFormatter.format(cart.taxTotal),
            crvTotal = currencyFormatter.format(cart.crvTotal),
            grandTotal = currencyFormatter.format(cart.grandTotal),
            itemCount = formatItemCount(cart.itemCount),
            savingsTotal = if (cart.discountTotal > BigDecimal.ZERO) {
                currencyFormatter.formatWithSign(cart.discountTotal.negate(), false)
            } else null
        )
        
        _state.value = _state.value.copy(
            items = items,
            totals = totals,
            isEmpty = cart.isEmpty
        )
    }
    
    /**
     * Maps a CartItem to CheckoutItemUiModel with formatted strings.
     */
    private fun mapToUiModel(cartItem: CartItem): CheckoutItemUiModel {
        val hasSavings = cartItem.savingsTotal > BigDecimal.ZERO
        
        return CheckoutItemUiModel(
            branchProductId = cartItem.branchProductId,
            productName = cartItem.branchProductName,
            quantity = formatQuantity(cartItem),
            unitPrice = currencyFormatter.format(cartItem.effectivePrice),
            lineTotal = currencyFormatter.format(cartItem.subTotal),
            isSnapEligible = cartItem.isSnapEligible,
            taxIndicator = cartItem.taxIndicator,
            hasSavings = hasSavings,
            savingsAmount = if (hasSavings) {
                currencyFormatter.formatWithSign(cartItem.savingsTotal.negate(), false)
            } else null,
            soldById = cartItem.soldById,
            rawQuantity = cartItem.quantityUsed.toInt()
        )
    }
    
    /**
     * Formats quantity for display.
     */
    private fun formatQuantity(cartItem: CartItem): String {
        return if (cartItem.soldById == "Weight") {
            "${cartItem.quantityUsed} lb"
        } else {
            "${cartItem.quantityUsed.toInt()}x"
        }
    }
    
    /**
     * Formats item count for display.
     */
    private fun formatItemCount(count: BigDecimal): String {
        val intCount = count.toInt()
        return if (intCount == 1) "1 item" else "$intCount items"
    }
    
    // ========================================================================
    // Age Verification
    // Per DIALOGS.md: Age Verification Dialog for alcohol/tobacco products
    // ========================================================================
    
    /**
     * Checks if a product requires age verification and shows the dialog if needed.
     * 
     * Per DIALOGS.md: Age-restricted products MUST trigger verification before
     * being added to cart.
     * 
     * @param product The product to check
     * @return true if the product was handled (dialog shown or added to cart),
     *         false if further processing is needed
     */
    private fun checkAndHandleAgeRestriction(product: Product): Boolean {
        if (!product.isAgeRestricted) {
            return false
        }
        
        // Show age verification dialog
        _state.value = _state.value.copy(
            ageVerificationDialogState = AgeVerificationDialogState(
                isVisible = true,
                productName = product.productName,
                requiredAge = product.ageRestriction ?: 21,
                branchProductId = product.branchProductId,
                monthInput = "",
                dayInput = "",
                yearInput = "",
                activeField = DateField.MONTH,
                calculatedAge = null,
                errorMessage = null
            )
        )
        
        return true
    }
    
    /**
     * Handles digit input in the Age Verification dialog.
     * 
     * Per DIALOGS.md: Input fills MM/DD/YYYY fields sequentially.
     * When a field is full, automatically moves to the next field.
     */
    fun onAgeVerificationDigitPress(digit: String) {
        val currentState = _state.value.ageVerificationDialogState
        if (!currentState.isVisible) return
        
        // Filter out non-numeric input
        if (!digit.all { it.isDigit() }) return
        
        val newState = when (currentState.activeField) {
            DateField.MONTH -> {
                val newMonth = currentState.monthInput + digit
                if (newMonth.length >= 2) {
                    // Move to day field
                    currentState.copy(
                        monthInput = newMonth.take(2),
                        activeField = DateField.DAY
                    )
                } else {
                    currentState.copy(monthInput = newMonth)
                }
            }
            DateField.DAY -> {
                val newDay = currentState.dayInput + digit
                if (newDay.length >= 2) {
                    // Move to year field
                    currentState.copy(
                        dayInput = newDay.take(2),
                        activeField = DateField.YEAR
                    )
                } else {
                    currentState.copy(dayInput = newDay)
                }
            }
            DateField.YEAR -> {
                val newYear = currentState.yearInput + digit
                currentState.copy(yearInput = newYear.take(4))
            }
        }
        
        // Calculate age if date is complete
        val calculatedAge = calculateAgeFromDOB(
            newState.monthInput,
            newState.dayInput,
            newState.yearInput
        )
        
        _state.value = _state.value.copy(
            ageVerificationDialogState = newState.copy(
                calculatedAge = calculatedAge,
                errorMessage = null
            )
        )
    }
    
    /**
     * Clears the current date field input.
     */
    fun onAgeVerificationClear() {
        val currentState = _state.value.ageVerificationDialogState
        if (!currentState.isVisible) return
        
        _state.value = _state.value.copy(
            ageVerificationDialogState = currentState.copy(
                monthInput = "",
                dayInput = "",
                yearInput = "",
                activeField = DateField.MONTH,
                calculatedAge = null,
                errorMessage = null
            )
        )
    }
    
    /**
     * Handles backspace in the Age Verification dialog.
     */
    fun onAgeVerificationBackspace() {
        val currentState = _state.value.ageVerificationDialogState
        if (!currentState.isVisible) return
        
        val newState = when (currentState.activeField) {
            DateField.MONTH -> {
                if (currentState.monthInput.isNotEmpty()) {
                    currentState.copy(monthInput = currentState.monthInput.dropLast(1))
                } else {
                    currentState
                }
            }
            DateField.DAY -> {
                if (currentState.dayInput.isNotEmpty()) {
                    currentState.copy(dayInput = currentState.dayInput.dropLast(1))
                } else {
                    // Move back to month field
                    currentState.copy(activeField = DateField.MONTH)
                }
            }
            DateField.YEAR -> {
                if (currentState.yearInput.isNotEmpty()) {
                    currentState.copy(yearInput = currentState.yearInput.dropLast(1))
                } else {
                    // Move back to day field
                    currentState.copy(activeField = DateField.DAY)
                }
            }
        }
        
        // Recalculate age
        val calculatedAge = calculateAgeFromDOB(
            newState.monthInput,
            newState.dayInput,
            newState.yearInput
        )
        
        _state.value = _state.value.copy(
            ageVerificationDialogState = newState.copy(calculatedAge = calculatedAge)
        )
    }
    
    /**
     * Selects a date field for input.
     */
    fun onAgeVerificationFieldSelect(field: DateField) {
        val currentState = _state.value.ageVerificationDialogState
        if (!currentState.isVisible) return
        
        _state.value = _state.value.copy(
            ageVerificationDialogState = currentState.copy(activeField = field)
        )
    }
    
    /**
     * Confirms age verification and adds product to cart.
     * 
     * Per Governance: Item MUST NOT be added until verification passes.
     */
    fun onAgeVerificationConfirm() {
        val currentState = _state.value.ageVerificationDialogState
        if (!currentState.isVisible) return
        
        if (!currentState.meetsAgeRequirement) {
            _state.value = _state.value.copy(
                ageVerificationDialogState = currentState.copy(
                    errorMessage = "Customer does not meet the minimum age requirement of ${currentState.requiredAge}"
                )
            )
            return
        }
        
        // Add product to cart
        effectiveScope.launch {
            val product = productRepository.getById(currentState.branchProductId)
            if (product != null) {
                cartRepository.addToCart(product)
                
                // Log verification for audit
                println("[AUDIT] AGE VERIFICATION PASSED")
                println("  Product: ${product.productName}")
                println("  Required Age: ${currentState.requiredAge}")
                println("  Verified Age: ${currentState.calculatedAge}")
                println("  DOB: ${currentState.formattedDate}")
                println("  Timestamp: ${LocalDateTime.now()}")
                
                _state.value = _state.value.copy(
                    ageVerificationDialogState = AgeVerificationDialogState(),
                    lastScanEvent = ScanEvent.ProductAdded(product.productName)
                )
            }
        }
    }
    
    /**
     * Cancels age verification without adding product.
     */
    fun onAgeVerificationCancel() {
        val currentState = _state.value.ageVerificationDialogState
        if (currentState.isVisible) {
            println("[AUDIT] AGE VERIFICATION CANCELLED for ${currentState.productName}")
        }
        
        _state.value = _state.value.copy(
            ageVerificationDialogState = AgeVerificationDialogState()
        )
    }
    
    /**
     * Initiates manager override for age verification.
     * 
     * Per DIALOGS.md: Manager Override button allows managers to override
     * age verification in special cases (e.g., ID already checked).
     */
    fun onAgeVerificationManagerOverride() {
        val currentState = _state.value.ageVerificationDialogState
        if (!currentState.isVisible) return
        
        val user = currentUser ?: return
        
        // Check if user has permission to override
        val permissionResult = PermissionManager.checkPermission(user, RequestAction.LINE_DISCOUNT)
        
        when (permissionResult) {
            PermissionCheckResult.GRANTED,
            PermissionCheckResult.SELF_APPROVAL_ALLOWED -> {
                // Manager can self-approve - add product directly
                executeAgeVerificationOverride()
            }
            PermissionCheckResult.REQUIRES_APPROVAL -> {
                // Set processing state and show manager approval
                _state.value = _state.value.copy(
                    ageVerificationDialogState = currentState.copy(isProcessing = true)
                )
                showManagerApprovalForAgeOverride()
            }
            PermissionCheckResult.DENIED -> {
                _state.value = _state.value.copy(
                    ageVerificationDialogState = currentState.copy(
                        errorMessage = "You do not have permission for manager override"
                    )
                )
            }
        }
    }
    
    /**
     * Shows manager approval dialog for age verification override.
     */
    private fun showManagerApprovalForAgeOverride() {
        val user = currentUser ?: return
        
        effectiveScope.launch {
            val managers = managerApprovalService.getApprovers(RequestAction.LINE_DISCOUNT, user)
            
            _state.value = _state.value.copy(
                managerApprovalState = ManagerApprovalDialogState(
                    isVisible = true,
                    action = RequestAction.LINE_DISCOUNT,  // Using existing action type
                    managers = managers,
                    isProcessing = false,
                    errorMessage = null,
                    pendingItemId = _state.value.ageVerificationDialogState.branchProductId
                )
            )
        }
    }
    
    /**
     * Executes age verification override after approval.
     */
    private fun executeAgeVerificationOverride() {
        val currentState = _state.value.ageVerificationDialogState
        
        effectiveScope.launch {
            val product = productRepository.getById(currentState.branchProductId)
            if (product != null) {
                cartRepository.addToCart(product)
                
                // Log override for audit
                println("[AUDIT] AGE VERIFICATION OVERRIDE")
                println("  Product: ${product.productName}")
                println("  Required Age: ${currentState.requiredAge}")
                println("  Override By: ${currentUser?.username ?: "Unknown"}")
                println("  Timestamp: ${LocalDateTime.now()}")
                
                _state.value = _state.value.copy(
                    ageVerificationDialogState = AgeVerificationDialogState(),
                    lastScanEvent = ScanEvent.ProductAdded(product.productName)
                )
            }
        }
    }
    
    /**
     * Calculates age from date of birth components.
     * 
     * Per Governance: Must calculate age correctly including leap years.
     * Uses java.time.Period for accurate age calculation.
     * 
     * @param month Month (MM)
     * @param day Day (DD)
     * @param year Year (YYYY)
     * @return Age in years, or null if date is invalid/incomplete
     */
    private fun calculateAgeFromDOB(month: String, day: String, year: String): Int? {
        if (month.length != 2 || day.length != 2 || year.length != 4) {
            return null
        }
        
        val monthInt = month.toIntOrNull() ?: return null
        val dayInt = day.toIntOrNull() ?: return null
        val yearInt = year.toIntOrNull() ?: return null
        
        // Basic validation
        if (monthInt !in 1..12) return null
        if (dayInt !in 1..31) return null
        if (yearInt < 1900 || yearInt > LocalDate.now().year) return null
        
        return try {
            val birthDate = LocalDate.of(yearInt, monthInt, dayInt)
            val today = LocalDate.now()
            
            // Ensure birth date is in the past
            if (birthDate.isAfter(today)) return null
            
            Period.between(birthDate, today).years
        } catch (e: Exception) {
            // Invalid date (e.g., Feb 30)
            null
        }
    }
}
