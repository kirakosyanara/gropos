package com.unisight.gropos.features.checkout.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.derivedStateOf
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.unisight.gropos.core.components.BarcodeInputField
import com.unisight.gropos.core.components.DangerButton
import com.unisight.gropos.core.components.ExtraLargeButton
import com.unisight.gropos.core.components.FunctionsGrid
import com.unisight.gropos.core.components.OutlineButton
import com.unisight.gropos.core.components.RealTimeClock
import com.unisight.gropos.core.components.SuccessButton
import com.unisight.gropos.core.components.TenKey
import com.unisight.gropos.core.components.TenKeyState
import com.unisight.gropos.core.components.WhiteBox
import com.unisight.gropos.core.theme.GroPOSColors
import com.unisight.gropos.core.theme.GroPOSRadius
import com.unisight.gropos.core.theme.GroPOSSpacing
import com.unisight.gropos.core.components.dialogs.ManagerApprovalDialog
import com.unisight.gropos.features.checkout.presentation.components.dialogs.AgeVerificationDialog
import com.unisight.gropos.features.checkout.presentation.components.dialogs.VoidConfirmationDialog
import com.unisight.gropos.features.checkout.presentation.components.dialogs.HoldTransactionDialog
import com.unisight.gropos.features.checkout.presentation.components.dialogs.RecallTransactionsDialog
import com.unisight.gropos.features.checkout.presentation.components.dialogs.CashPickupDialog
import com.unisight.gropos.features.auth.presentation.components.dialogs.LogoutDialog
import com.unisight.gropos.features.auth.presentation.components.dialogs.LogoutOption
import com.unisight.gropos.features.checkout.presentation.CheckoutItemUiModel
import com.unisight.gropos.features.checkout.presentation.CheckoutTotalsUiModel
import com.unisight.gropos.features.checkout.presentation.CheckoutUiState
import com.unisight.gropos.features.checkout.presentation.DateField
import com.unisight.gropos.features.checkout.presentation.ModificationTenKeyMode
import com.unisight.gropos.features.checkout.presentation.ScanEvent
import com.unisight.gropos.features.checkout.presentation.SelectedItemUiModel
import com.unisight.gropos.features.checkout.presentation.components.ProductLookupDialog

// ============================================================================
// Test Tags (per testing-strategy.mdc: Use testTag for UI testing)
// ============================================================================
object CheckoutTestTags {
    const val SCREEN = "checkout_screen"
    const val LEFT_PANEL = "checkout_left_panel"
    const val RIGHT_PANEL = "checkout_right_panel"
    const val ITEMS_LIST = "checkout_items_list"
    const val TOTALS_CARD = "checkout_totals_card"
    const val TEN_KEY = "checkout_ten_key"
    const val FUNCTIONS_GRID = "checkout_functions_grid"
    const val BARCODE_INPUT = "checkout_barcode_input"
    const val PAY_BUTTON = "checkout_pay_button"
    const val EMPTY_STATE = "checkout_empty_state"
    const val LOADING_INDICATOR = "checkout_loading"
    const val SCAN_FEEDBACK = "checkout_scan_feedback"
    const val GRAND_TOTAL = "checkout_grand_total"
    fun itemRow(branchProductId: Int) = "checkout_item_$branchProductId"
    fun snapBadge(branchProductId: Int) = "snap_badge_$branchProductId"
}

/**
 * Main content composable for the Checkout screen.
 * 
 * Per SCREEN_LAYOUTS.md (Home Screen): 70/30 horizontal split layout.
 * - LEFT (70%): Order list with info bar
 * - RIGHT (30%): Totals, Ten-Key, Actions
 * 
 * Per code-quality.mdc: State hoisting - receives state and emits events.
 * Per Governance: No math here - all values are pre-formatted strings.
 */
@Composable
fun CheckoutContent(
    state: CheckoutUiState,
    onEvent: (CheckoutEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    // Local state for barcode input
    var barcodeInput by remember { mutableStateOf("") }
    var tenKeyInput by remember { mutableStateOf("") }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag(CheckoutTestTags.SCREEN)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // ================================================================
            // LEFT SECTION (70%) - Order List
            // Per UI_DESIGN_SYSTEM.md: LightGray2 background (#E1E3E3)
            // ================================================================
            LeftPanel(
                items = state.items,
                isEmpty = state.isEmpty,
                selectedItemId = state.selectedItemId,
                barcodeInput = barcodeInput,
                onBarcodeChange = { barcodeInput = it },
                onBarcodeSubmit = {
                    if (barcodeInput.isNotBlank()) {
                        onEvent(CheckoutEvent.ManualBarcodeEnter(barcodeInput))
                        barcodeInput = ""
                    }
                },
                onItemClick = { id -> onEvent(CheckoutEvent.SelectLineItem(id)) },
                onRemoveItem = { id -> onEvent(CheckoutEvent.RemoveItem(id)) },
                onLogout = { onEvent(CheckoutEvent.Logout) },
                modifier = Modifier
                    .weight(0.7f)
                    .fillMaxHeight()
                    .testTag(CheckoutTestTags.LEFT_PANEL)
            )
            
            // ================================================================
            // RIGHT SECTION (30%) - Totals, Ten-Key, Actions OR Modification Panel
            // Per SCREEN_LAYOUTS.md: Panel swaps when item is selected
            // ================================================================
            if (state.isModificationMode && state.selectedItem != null) {
                // Modification Mode Panel
                ModificationPanel(
                    selectedItem = state.selectedItem,
                    currentMode = state.modificationTenKeyMode,
                    inputValue = state.modificationInputValue,
                    onModeChange = { mode -> onEvent(CheckoutEvent.ChangeModificationMode(mode)) },
                    onDigitPress = { digit -> onEvent(CheckoutEvent.ModificationDigitPress(digit)) },
                    onClearPress = { onEvent(CheckoutEvent.ModificationClear) },
                    onBackspacePress = { onEvent(CheckoutEvent.ModificationBackspace) },
                    onConfirmPress = { onEvent(CheckoutEvent.ModificationConfirm) },
                    onVoidPress = { onEvent(CheckoutEvent.VoidSelectedLineItem) },
                    onBackPress = { onEvent(CheckoutEvent.DeselectLineItem) },
                    modifier = Modifier
                        .weight(0.3f)
                        .fillMaxHeight()
                        .testTag(CheckoutTestTags.RIGHT_PANEL)
                )
            } else {
                // Normal Mode Panel
                RightPanel(
                    totals = state.totals,
                    tenKeyInput = tenKeyInput,
                    onTenKeyDigit = { digit -> tenKeyInput += digit },
                    onTenKeyClear = { tenKeyInput = "" },
                    onTenKeyBackspace = { 
                        if (tenKeyInput.isNotEmpty()) {
                            tenKeyInput = tenKeyInput.dropLast(1)
                        }
                    },
                    onTenKeyOk = { value ->
                        if (value.isNotBlank()) {
                            onEvent(CheckoutEvent.ManualBarcodeEnter(value))
                            tenKeyInput = ""
                        }
                    },
                    onPayClick = { 
                        // Only navigate if cart is not empty
                        if (!state.isEmpty) {
                            onEvent(CheckoutEvent.NavigateToPay)
                        }
                    },
                    onClearCart = { onEvent(CheckoutEvent.ClearCart) },
                    onLookupClick = { onEvent(CheckoutEvent.OpenLookup) },
                    onRecallClick = { onEvent(CheckoutEvent.OpenRecallDialog) },
                    onHoldClick = { onEvent(CheckoutEvent.OpenHoldDialog) },
                    onFunctionsClick = { /* TODO: Show functions panel */ },
                    onVoidTransactionClick = { onEvent(CheckoutEvent.VoidTransactionRequest) },
                    onSignOutClick = { onEvent(CheckoutEvent.OpenLogoutDialog) },
                    onCashPickupClick = { onEvent(CheckoutEvent.OpenCashPickupDialog) },
                    modifier = Modifier
                        .weight(0.3f)
                        .fillMaxHeight()
                        .testTag(CheckoutTestTags.RIGHT_PANEL)
                )
            }
        }
        
        // Loading Overlay
        if (state.isLoading) {
            LoadingOverlay()
        }
        
        // Scan Feedback Snackbar
        state.lastScanEvent?.let { event ->
            ScanFeedbackSnackbar(
                event = event,
                onDismiss = { onEvent(CheckoutEvent.DismissScanEvent) },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
        
        // Product Lookup Dialog
        // Per SCREEN_LAYOUTS.md: Product Lookup Dialog for manual product selection
        ProductLookupDialog(
            state = state.lookupState,
            onSearchQueryChange = { query -> onEvent(CheckoutEvent.LookupSearchChange(query)) },
            onCategorySelect = { categoryId -> onEvent(CheckoutEvent.LookupCategorySelect(categoryId)) },
            onProductSelect = { product -> onEvent(CheckoutEvent.ProductSelected(product)) },
            onDismiss = { onEvent(CheckoutEvent.CloseLookup) }
        )
        
        // Manager Approval Dialog
        // Per ROLES_AND_PERMISSIONS.md: Manager approval for sensitive actions
        if (state.managerApprovalState.isVisible) {
            ManagerApprovalDialog(
                action = state.managerApprovalState.action,
                amount = null,
                managers = state.managerApprovalState.managers,
                isProcessing = state.managerApprovalState.isProcessing,
                errorMessage = state.managerApprovalState.errorMessage,
                onApproved = { _, _ -> },  // Handled via state
                onDenied = { onEvent(CheckoutEvent.DismissManagerApproval) },
                onCancel = { onEvent(CheckoutEvent.DismissManagerApproval) },
                onPinSubmit = { managerId, pin ->
                    onEvent(CheckoutEvent.SubmitManagerApproval(managerId, pin))
                }
            )
        }
        
        // Void Transaction Confirmation Dialog
        // Per FUNCTIONS_MENU.md: Confirmation before voiding entire transaction
        if (state.showVoidConfirmationDialog) {
            VoidConfirmationDialog(
                onConfirm = { onEvent(CheckoutEvent.ConfirmVoidTransaction) },
                onCancel = { onEvent(CheckoutEvent.CancelVoidTransaction) }
            )
        }
        
        // Logout Dialog
        // Per CASHIER_OPERATIONS.md: Logout options (Lock, Release Till, End Shift)
        if (state.showLogoutDialog) {
            LogoutDialog(
                onOptionSelected = { option ->
                    when (option) {
                        LogoutOption.LOCK_STATION -> onEvent(CheckoutEvent.LockStation)
                        LogoutOption.RELEASE_TILL -> onEvent(CheckoutEvent.ReleaseTill)
                        LogoutOption.END_OF_SHIFT -> onEvent(CheckoutEvent.EndShift)
                    }
                },
                onDismiss = { onEvent(CheckoutEvent.DismissLogoutDialog) },
                isCartEmpty = state.isEmpty
            )
        }
        
        // Logout Feedback Snackbar
        state.logoutFeedback?.let { message ->
            Snackbar(
                modifier = Modifier
                    .padding(GroPOSSpacing.M)
                    .testTag("logout_feedback"),
                action = {
                    TextButton(onClick = { onEvent(CheckoutEvent.DismissLogoutFeedback) }) {
                        Text("OK")
                    }
                }
            ) {
                Text(message)
            }
        }
        
        // Hold Transaction Dialog
        // Per TRANSACTION_FLOW.md: Hold suspends current transaction with optional name
        if (state.holdDialogState.isVisible) {
            HoldTransactionDialog(
                grandTotal = state.totals.grandTotal,
                itemCount = state.totals.itemCount,
                onHold = { holdName -> onEvent(CheckoutEvent.ConfirmHold(holdName)) },
                onDismiss = { onEvent(CheckoutEvent.DismissHoldDialog) }
            )
        }
        
        // Recall Transactions Dialog
        // Per TRANSACTION_FLOW.md: Recall shows list of held transactions
        if (state.recallDialogState.isVisible) {
            RecallTransactionsDialog(
                heldTransactions = state.recallDialogState.heldTransactions,
                isLoading = state.recallDialogState.isLoading,
                onRestore = { id -> onEvent(CheckoutEvent.RestoreTransaction(id)) },
                onDelete = { id -> onEvent(CheckoutEvent.DeleteHeldTransaction(id)) },
                onDismiss = { onEvent(CheckoutEvent.DismissRecallDialog) }
            )
        }
        
        // Hold/Recall Feedback Snackbar
        state.holdRecallFeedback?.let { message ->
            Snackbar(
                modifier = Modifier
                    .padding(GroPOSSpacing.M)
                    .align(Alignment.TopCenter)
                    .testTag("hold_recall_feedback"),
                action = {
                    TextButton(onClick = { onEvent(CheckoutEvent.DismissHoldRecallFeedback) }) {
                        Text("OK", color = GroPOSColors.White)
                    }
                },
                containerColor = GroPOSColors.PrimaryGreen,
                contentColor = GroPOSColors.White
            ) {
                Text(message)
            }
        }
        
        // Cash Pickup Dialog
        // Per FUNCTIONS_MENU.md: Cash Pickup removes cash from drawer for safe deposit
        if (state.cashPickupDialogState.isVisible) {
            CashPickupDialog(
                currentBalance = state.cashPickupDialogState.currentBalance,
                inputValue = state.cashPickupDialogState.inputValue,
                errorMessage = state.cashPickupDialogState.errorMessage,
                onDigitClick = { digit -> onEvent(CheckoutEvent.CashPickupDigitPress(digit)) },
                onClearClick = { onEvent(CheckoutEvent.CashPickupClear) },
                onBackspaceClick = { onEvent(CheckoutEvent.CashPickupBackspace) },
                onPickupClick = { onEvent(CheckoutEvent.CashPickupConfirm) },
                onDismiss = { onEvent(CheckoutEvent.DismissCashPickupDialog) }
            )
        }
        
        // Cash Pickup Feedback Snackbar
        state.cashPickupFeedback?.let { message ->
            Snackbar(
                modifier = Modifier
                    .padding(GroPOSSpacing.M)
                    .align(Alignment.TopCenter)
                    .testTag("cash_pickup_feedback"),
                action = {
                    TextButton(onClick = { onEvent(CheckoutEvent.DismissCashPickupFeedback) }) {
                        Text("OK", color = GroPOSColors.White)
                    }
                },
                containerColor = GroPOSColors.PrimaryGreen,
                contentColor = GroPOSColors.White
            ) {
                Text(message)
            }
        }
        
        // Age Verification Dialog
        // Per DIALOGS.md: Age Verification for alcohol/tobacco products
        if (state.ageVerificationDialogState.isVisible) {
            AgeVerificationDialog(
                state = state.ageVerificationDialogState,
                onDigitClick = { digit -> onEvent(CheckoutEvent.AgeVerificationDigitPress(digit)) },
                onClear = { onEvent(CheckoutEvent.AgeVerificationClear) },
                onBackspace = { onEvent(CheckoutEvent.AgeVerificationBackspace) },
                onFieldSelect = { field -> onEvent(CheckoutEvent.AgeVerificationFieldSelect(field)) },
                onConfirm = { onEvent(CheckoutEvent.AgeVerificationConfirm) },
                onCancel = { onEvent(CheckoutEvent.AgeVerificationCancel) },
                onManagerOverride = { onEvent(CheckoutEvent.AgeVerificationManagerOverride) }
            )
        }
    }
}

// ============================================================================
// LEFT PANEL - Order List
// ============================================================================

@Composable
private fun LeftPanel(
    items: List<CheckoutItemUiModel>,
    isEmpty: Boolean,
    selectedItemId: Int?,
    barcodeInput: String,
    onBarcodeChange: (String) -> Unit,
    onBarcodeSubmit: () -> Unit,
    onItemClick: (Int) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(GroPOSColors.LightGray2)
            .padding(GroPOSSpacing.XXL)
    ) {
        // Header Row with Logo, Clock, and Logout
        // Per SCREEN_LAYOUTS.md: Show current time (hh:mm a) in top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "GroPOS",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = GroPOSColors.PrimaryGreen
            )
            
            // Real-time clock display (center position)
            RealTimeClock(color = GroPOSColors.TextSecondary)
            
            DangerButton(onClick = onLogout) {
                Text("Logout")
            }
        }
        
        Spacer(modifier = Modifier.height(GroPOSSpacing.M))
        
        // Barcode Input Field
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GroPOSSpacing.S),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BarcodeInputField(
                value = barcodeInput,
                onValueChange = onBarcodeChange,
                onSubmit = onBarcodeSubmit,
                modifier = Modifier
                    .weight(1f)
                    .testTag(CheckoutTestTags.BARCODE_INPUT)
            )
            SuccessButton(onClick = onBarcodeSubmit) {
                Text("Add")
            }
        }
        
        Spacer(modifier = Modifier.height(GroPOSSpacing.M))
        
        // Order List
        if (isEmpty) {
            EmptyCartState(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag(CheckoutTestTags.EMPTY_STATE)
            )
        } else {
            // Per SCREEN_LAYOUTS.md: Cashier Screen shows newest items at TOP (scanDate DESCENDING)
            // The Cart stores items in append order (oldest first).
            // We reverse only for VISUAL presentation - Cart data structure remains unchanged.
            val displayItems by remember(items) {
                derivedStateOf { items.asReversed() }
            }
            val listState = rememberLazyListState()
            
            WhiteBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(CheckoutTestTags.ITEMS_LIST),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(
                        items = displayItems,
                        key = { _, item -> item.branchProductId }
                    ) { index, item ->
                        val isSelected = item.branchProductId == selectedItemId
                        // Per SCREEN_LAYOUTS.md: "Most recently scanned item should be highlighted"
                        // After reversing, index 0 is the most recently added item
                        val isNewest = index == 0
                        
                        OrderListItem(
                            item = item,
                            isSelected = isSelected,
                            isNewest = isNewest,
                            onClick = { onItemClick(item.branchProductId) },
                            onRemove = { onRemoveItem(item.branchProductId) },
                            modifier = Modifier
                                .animateItem()
                                .testTag(CheckoutTestTags.itemRow(item.branchProductId))
                        )
                        HorizontalDivider(color = GroPOSColors.LightGray3)
                    }
                }
            }
        }
    }
}

// ============================================================================
// RIGHT PANEL - Totals, Ten-Key, Actions
// ============================================================================

@Composable
private fun RightPanel(
    totals: CheckoutTotalsUiModel,
    tenKeyInput: String,
    onTenKeyDigit: (String) -> Unit,
    onTenKeyClear: () -> Unit,
    onTenKeyBackspace: () -> Unit,
    onTenKeyOk: (String) -> Unit,
    onPayClick: () -> Unit,
    onClearCart: () -> Unit,
    onLookupClick: () -> Unit,
    onRecallClick: () -> Unit,
    onHoldClick: () -> Unit,
    onFunctionsClick: () -> Unit,
    onVoidTransactionClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onCashPickupClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(GroPOSColors.LightGray1)
            .padding(horizontal = GroPOSSpacing.XXXL, vertical = GroPOSSpacing.M)
    ) {
        // Totals Card
        TotalsCard(
            totals = totals,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(CheckoutTestTags.TOTALS_CARD)
        )
        
        Spacer(modifier = Modifier.height(GroPOSSpacing.M))
        
        // Pay Button (Large, prominent)
        ExtraLargeButton(
            onClick = onPayClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .testTag(CheckoutTestTags.PAY_BUTTON)
        ) {
            Text("PAY", fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(GroPOSSpacing.M))
        
        // Ten-Key Input Display
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(GroPOSRadius.Small),
            color = GroPOSColors.White
        ) {
            Text(
                text = tenKeyInput.ifEmpty { "0" },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(GroPOSSpacing.M)
            )
        }
        
        Spacer(modifier = Modifier.height(GroPOSSpacing.S))
        
        // Ten-Key Pad
        TenKey(
            state = TenKeyState(inputValue = tenKeyInput),
            onDigitClick = onTenKeyDigit,
            onOkClick = onTenKeyOk,
            onClearClick = onTenKeyClear,
            onBackspaceClick = onTenKeyBackspace,
            showQtyButton = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(CheckoutTestTags.TEN_KEY)
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Functions Grid
        FunctionsGrid(
            onFunctionsClick = onFunctionsClick,
            onLookupClick = onLookupClick,
            onRecallClick = onRecallClick,
            onHoldClick = onHoldClick,
            onVoidTransactionClick = onVoidTransactionClick,
            onSignOutClick = onSignOutClick,
            onCashPickupClick = onCashPickupClick,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(CheckoutTestTags.FUNCTIONS_GRID)
        )
        
        Spacer(modifier = Modifier.height(GroPOSSpacing.S))
        
        // Clear Cart Button
        OutlineButton(
            onClick = onClearCart,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Clear Cart")
        }
    }
}

// ============================================================================
// Totals Card Component
// ============================================================================

@Composable
private fun TotalsCard(
    totals: CheckoutTotalsUiModel,
    modifier: Modifier = Modifier
) {
    WhiteBox(modifier = modifier) {
        // Item Count
        Text(
            text = totals.itemCount,
            style = MaterialTheme.typography.bodyLarge,
            color = GroPOSColors.TextSecondary
        )
        
        Spacer(modifier = Modifier.height(GroPOSSpacing.S))
        
        // Grand Total (Large, prominent)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(CheckoutTestTags.GRAND_TOTAL),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Total",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = totals.grandTotal,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = GroPOSColors.PrimaryGreen
            )
        }
        
        Spacer(modifier = Modifier.height(GroPOSSpacing.S))
        
        HorizontalDivider(color = GroPOSColors.LightGray3)
        
        Spacer(modifier = Modifier.height(GroPOSSpacing.S))
        
        // Breakdown
        TotalRow(label = "Subtotal", value = totals.subtotal)
        
        totals.savingsTotal?.let { savings ->
            TotalRow(
                label = "Savings",
                value = "-$savings",
                valueColor = GroPOSColors.SavingsRed
            )
        }
        
        TotalRow(label = "Tax", value = totals.taxTotal)
        
        if (totals.crvTotal != "$0.00") {
            TotalRow(label = "CRV", value = totals.crvTotal)
        }
    }
}

@Composable
private fun TotalRow(
    label: String,
    value: String,
    valueColor: Color = GroPOSColors.TextPrimary,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = GroPOSColors.TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}

// ============================================================================
// Order List Item Component (per COMPONENTS.md)
// ============================================================================

/**
 * Order List Item Row
 * 
 * Per COMPONENTS.md (Order List Item):
 * Column weights: 10% | 10% | 60% | 5% | 15%
 * 
 * Layout:
 * [Qty] [Img] Product Name    [%] $Price
 *             Description         $XX.XX
 *             Tax+CRV        [Disc]
 *             "You saved..."
 * 
 * Per SCREEN_LAYOUTS.md: 
 * - Clicking a row enters modification mode.
 * - "Most recently scanned item should be highlighted"
 * - "Newest items should appear at the top of the list for immediate visibility"
 */
@Composable
private fun OrderListItem(
    item: CheckoutItemUiModel,
    isSelected: Boolean,
    isNewest: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Per SCREEN_LAYOUTS.md: Highlight the most recently scanned item
    // Priority: Selected (green border) > Newest (subtle ContainerHigh background) > Normal
    val backgroundColor = when {
        isSelected -> GroPOSColors.PrimaryGreen.copy(alpha = 0.15f)
        isNewest -> GroPOSColors.ContainerHigh
        else -> Color.Transparent
    }
    
    val borderModifier = if (isSelected) {
        Modifier.border(
            width = 2.dp,
            color = GroPOSColors.PrimaryGreen,
            shape = RoundedCornerShape(8.dp)
        )
    } else {
        Modifier
    }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(borderModifier)
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = GroPOSSpacing.S, horizontal = GroPOSSpacing.XS),
        verticalAlignment = Alignment.Top
    ) {
        // Quantity (10%)
        Column(
            modifier = Modifier.weight(0.1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = item.quantity,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            // Sale badge placeholder
        }
        
        // Image placeholder (10%)
        Box(
            modifier = Modifier
                .weight(0.1f)
                .height(60.dp)
                .background(
                    color = GroPOSColors.LightGray3,
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🛒",
                style = MaterialTheme.typography.titleLarge
            )
        }
        
        Spacer(modifier = Modifier.width(GroPOSSpacing.S))
        
        // Product Details (60%)
        Column(modifier = Modifier.weight(0.6f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.productName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                
                // SNAP Badge
                if (item.isSnapEligible) {
                    Spacer(modifier = Modifier.width(8.dp))
                    SnapBadge(
                        modifier = Modifier.testTag(
                            CheckoutTestTags.snapBadge(item.branchProductId)
                        )
                    )
                }
            }
            
            Text(
                text = "${item.unitPrice} × ${item.quantity}",
                style = MaterialTheme.typography.bodySmall,
                color = GroPOSColors.TextSecondary
            )
            
            // Savings
            if (item.hasSavings && item.savingsAmount != null) {
                Text(
                    text = "You saved ${item.savingsAmount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = GroPOSColors.SavingsRed
                )
            }
        }
        
        // Discount indicator (5%)
        if (item.hasSavings) {
            Text(
                text = "%",
                style = MaterialTheme.typography.bodySmall,
                color = GroPOSColors.PrimaryGreen,
                modifier = Modifier.weight(0.05f),
                textAlign = TextAlign.Center
            )
        } else {
            Spacer(modifier = Modifier.weight(0.05f))
        }
        
        // Price and Remove (15%)
        Column(
            modifier = Modifier.weight(0.15f),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = item.lineTotal,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End
            )
            
            TextButton(onClick = onRemove) {
                Text(
                    text = "Remove",
                    style = MaterialTheme.typography.bodySmall,
                    color = GroPOSColors.DangerRed
                )
            }
        }
    }
}

// ============================================================================
// SNAP Badge Component
// ============================================================================

@Composable
private fun SnapBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = GroPOSColors.SnapBadgeBackground
    ) {
        Text(
            text = "SNAP",
            style = MaterialTheme.typography.labelSmall,
            color = GroPOSColors.SnapGreen,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

// ============================================================================
// Empty Cart State
// ============================================================================

@Composable
private fun EmptyCartState(modifier: Modifier = Modifier) {
    WhiteBox(modifier = modifier) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🛒",
                    style = MaterialTheme.typography.displayLarge
                )
                Spacer(modifier = Modifier.height(GroPOSSpacing.M))
                Text(
                    text = "No items in cart",
                    style = MaterialTheme.typography.headlineMedium,
                    color = GroPOSColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(GroPOSSpacing.S))
                Text(
                    text = "Scan an item or use the keypad to begin",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GroPOSColors.TextSecondary.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ============================================================================
// Loading Overlay
// ============================================================================

@Composable
private fun LoadingOverlay(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GroPOSColors.OverlayBlack)
            .testTag(CheckoutTestTags.LOADING_INDICATOR),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = GroPOSColors.White)
    }
}

// ============================================================================
// Scan Feedback Snackbar
// ============================================================================

@Composable
private fun ScanFeedbackSnackbar(
    event: ScanEvent,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (message, isError) = when (event) {
        is ScanEvent.ProductAdded -> "Added: ${event.productName}" to false
        is ScanEvent.ProductNotFound -> "Product not found: ${event.barcode}" to true
        is ScanEvent.Error -> "Error: ${event.message}" to true
    }
    
    Snackbar(
        modifier = modifier
            .padding(GroPOSSpacing.M)
            .testTag(CheckoutTestTags.SCAN_FEEDBACK),
        action = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss", color = GroPOSColors.White)
            }
        },
        containerColor = if (isError) GroPOSColors.DangerRed else GroPOSColors.PrimaryGreen,
        contentColor = GroPOSColors.White
    ) {
        Text(message)
    }
}

// ============================================================================
// MODIFICATION PANEL
// Per SCREEN_LAYOUTS.md: When a line item is selected, the right panel transforms
// ============================================================================

/**
 * Modification Panel - shown when a line item is selected.
 * 
 * Per SCREEN_LAYOUTS.md:
 * - Top buttons: BACK, QUANTITY, DISCOUNT, PRICE CHANGE
 * - TenKey section with mode-specific behavior
 * - Action buttons: REMOVE ITEM, MORE INFORMATION
 */
@Composable
private fun ModificationPanel(
    selectedItem: SelectedItemUiModel,
    currentMode: ModificationTenKeyMode,
    inputValue: String,
    onModeChange: (ModificationTenKeyMode) -> Unit,
    onDigitPress: (String) -> Unit,
    onClearPress: () -> Unit,
    onBackspacePress: () -> Unit,
    onConfirmPress: () -> Unit,
    onVoidPress: () -> Unit,
    onBackPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(GroPOSColors.LightGray1)
            .padding(horizontal = GroPOSSpacing.XXXL, vertical = GroPOSSpacing.M)
    ) {
        // Selected Item Info Card
        WhiteBox(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = selectedItem.productName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(GroPOSSpacing.S))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Qty: ${selectedItem.currentQuantity}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = GroPOSColors.TextSecondary
                )
                Text(
                    text = selectedItem.lineTotal,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = GroPOSColors.PrimaryGreen
                )
            }
        }
        
        Spacer(modifier = Modifier.height(GroPOSSpacing.M))
        
        // Mode Selection Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GroPOSSpacing.S)
        ) {
            // Quantity button - disabled for weighted items
            ModeButton(
                text = "QTY",
                isSelected = currentMode == ModificationTenKeyMode.QUANTITY,
                enabled = !selectedItem.isWeighted,
                onClick = { onModeChange(ModificationTenKeyMode.QUANTITY) },
                modifier = Modifier.weight(1f)
            )
            
            ModeButton(
                text = "DISC",
                isSelected = currentMode == ModificationTenKeyMode.DISCOUNT,
                enabled = true,
                onClick = { onModeChange(ModificationTenKeyMode.DISCOUNT) },
                modifier = Modifier.weight(1f)
            )
            
            ModeButton(
                text = "PRICE",
                isSelected = currentMode == ModificationTenKeyMode.PRICE,
                enabled = true,
                onClick = { onModeChange(ModificationTenKeyMode.PRICE) },
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(GroPOSSpacing.M))
        
        // Input Display with Mode Label
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = when (currentMode) {
                    ModificationTenKeyMode.QUANTITY -> "New Quantity"
                    ModificationTenKeyMode.DISCOUNT -> "Discount %"
                    ModificationTenKeyMode.PRICE -> "New Price"
                },
                style = MaterialTheme.typography.labelMedium,
                color = GroPOSColors.TextSecondary
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(GroPOSRadius.Small),
                color = GroPOSColors.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(GroPOSSpacing.M),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Prefix/suffix based on mode
                    if (currentMode == ModificationTenKeyMode.PRICE) {
                        Text(
                            text = "$",
                            style = MaterialTheme.typography.headlineMedium,
                            color = GroPOSColors.TextSecondary
                        )
                    }
                    
                    Text(
                        text = inputValue.ifEmpty { "0" },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (currentMode == ModificationTenKeyMode.DISCOUNT) {
                        Text(
                            text = "%",
                            style = MaterialTheme.typography.headlineMedium,
                            color = GroPOSColors.TextSecondary
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(GroPOSSpacing.S))
        
        // Ten-Key Pad
        TenKey(
            state = TenKeyState(inputValue = inputValue),
            onDigitClick = onDigitPress,
            onOkClick = { onConfirmPress() },
            onClearClick = onClearPress,
            onBackspaceClick = onBackspacePress,
            showQtyButton = false,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Action Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(GroPOSSpacing.S)
        ) {
            // Void Line Button
            DangerButton(
                onClick = onVoidPress,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("REMOVE ITEM", fontWeight = FontWeight.Bold)
            }
            
            // Back Button
            OutlineButton(
                onClick = onBackPress,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("BACK")
            }
        }
    }
}

/**
 * Mode selection button for the modification panel.
 */
@Composable
private fun ModeButton(
    text: String,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        !enabled -> GroPOSColors.LightGray3
        isSelected -> GroPOSColors.PrimaryGreen
        else -> GroPOSColors.White
    }
    
    val textColor = when {
        !enabled -> GroPOSColors.TextSecondary.copy(alpha = 0.5f)
        isSelected -> GroPOSColors.White
        else -> GroPOSColors.TextPrimary
    }
    
    Surface(
        modifier = modifier
            .height(48.dp)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(GroPOSRadius.Small),
        color = backgroundColor
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}
