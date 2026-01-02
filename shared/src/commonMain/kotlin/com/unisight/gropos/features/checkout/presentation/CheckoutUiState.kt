package com.unisight.gropos.features.checkout.presentation

import com.unisight.gropos.core.security.ManagerInfo
import com.unisight.gropos.core.security.RequestAction
import com.unisight.gropos.features.checkout.presentation.components.ProductLookupState
import com.unisight.gropos.features.checkout.presentation.components.dialogs.HeldTransactionUiModel

/**
 * UI model for displaying a cart item.
 * 
 * Contains pre-formatted strings for display.
 * Per Governance: No math in UI - all calculations done in Domain.
 * 
 * @property branchProductId Unique identifier for the item
 * @property productName Display name of the product
 * @property quantity Formatted quantity string (e.g., "2x")
 * @property unitPrice Formatted unit price (e.g., "$5.99")
 * @property lineTotal Formatted line total (e.g., "$11.98")
 * @property isSnapEligible Whether item is SNAP eligible (show badge)
 * @property taxIndicator Tax indicator for receipt (F = SNAP, T = Taxable)
 * @property hasSavings Whether there are savings on this item
 * @property savingsAmount Formatted savings amount if any
 * @property soldById How the product is sold (Quantity, Weight, etc.)
 * @property rawQuantity Raw quantity value for display in modification mode
 */
data class CheckoutItemUiModel(
    val branchProductId: Int,
    val productName: String,
    val quantity: String,
    val unitPrice: String,
    val lineTotal: String,
    val isSnapEligible: Boolean,
    val taxIndicator: String,
    val hasSavings: Boolean = false,
    val savingsAmount: String? = null,
    val soldById: String = "Quantity",
    val rawQuantity: Int = 1
)

/**
 * TenKey mode when in modification mode.
 * 
 * Per SCREEN_LAYOUTS.md: Modification Mode has different TenKey behaviors.
 */
enum class ModificationTenKeyMode {
    /** Enter new quantity (1-99) */
    QUANTITY,
    /** Enter percentage discount (0-100) */
    DISCOUNT,
    /** Enter new unit price */
    PRICE
}

/**
 * UI model for the selected item in modification mode.
 * 
 * Contains the details needed for the modification panel.
 */
data class SelectedItemUiModel(
    val branchProductId: Int,
    val productName: String,
    val currentQuantity: String,
    val currentPrice: String,
    val lineTotal: String,
    val isWeighted: Boolean,
    val rawQuantity: Int
)

/**
 * UI model for the totals panel.
 * 
 * Pre-formatted strings for display.
 */
data class CheckoutTotalsUiModel(
    val subtotal: String,
    val taxTotal: String,
    val crvTotal: String,
    val grandTotal: String,
    val itemCount: String,
    val savingsTotal: String? = null
)

/**
 * Represents a scan event result for UI feedback.
 */
sealed interface ScanEvent {
    data class ProductAdded(val productName: String) : ScanEvent
    data class ProductNotFound(val barcode: String) : ScanEvent
    data class Error(val message: String) : ScanEvent
}

/**
 * State for the Manager Approval Dialog.
 * 
 * Per ROLES_AND_PERMISSIONS.md: Manager approval is required for certain actions.
 */
data class ManagerApprovalDialogState(
    val isVisible: Boolean = false,
    val action: RequestAction = RequestAction.VOID_TRANSACTION,
    val managers: List<ManagerInfo> = emptyList(),
    val isProcessing: Boolean = false,
    val errorMessage: String? = null,
    val pendingItemId: Int? = null  // For line-item specific approvals
)

/**
 * State for the Hold Transaction Dialog.
 * 
 * Per TRANSACTION_FLOW.md: Hold dialog asks for optional name/note.
 */
data class HoldDialogState(
    val isVisible: Boolean = false
)

/**
 * State for the Recall Transactions Dialog.
 * 
 * Per TRANSACTION_FLOW.md: Recall shows list of held transactions.
 */
data class RecallDialogState(
    val isVisible: Boolean = false,
    val isLoading: Boolean = false,
    val heldTransactions: List<HeldTransactionUiModel> = emptyList()
)

/**
 * State for the Cash Pickup Dialog.
 * 
 * Per FUNCTIONS_MENU.md:
 * - Cash Pickup removes cash from drawer for safe deposit
 * - Requires Manager approval
 * - Cannot pickup more than current drawer balance
 */
data class CashPickupDialogState(
    val isVisible: Boolean = false,
    val inputValue: String = "",
    val currentBalance: String = "$0.00",
    val errorMessage: String? = null,
    val isProcessing: Boolean = false,
    val approvalPending: Boolean = false
)

/**
 * UI State for the Checkout screen.
 * 
 * Per kotlin-standards.mdc: Use sealed interface for strict typing.
 * Per code-quality.mdc: State modeling with sealed classes.
 * 
 * @property items List of cart items for display
 * @property totals Totals panel data
 * @property isLoading Whether a scan operation is in progress
 * @property lastScanEvent The last scan event for user feedback
 * @property isEmpty Whether the cart is empty
 * @property lookupState State for the Product Lookup Dialog
 * @property selectedItemId ID of the currently selected item (null = no selection)
 * @property selectedItem Details of the selected item for the modification panel
 * @property modificationTenKeyMode Current TenKey mode in modification mode
 * @property modificationInputValue Current input value in modification mode
 * @property managerApprovalState State for the Manager Approval Dialog
 */
data class CheckoutUiState(
    val items: List<CheckoutItemUiModel> = emptyList(),
    val totals: CheckoutTotalsUiModel = CheckoutTotalsUiModel(
        subtotal = "$0.00",
        taxTotal = "$0.00",
        crvTotal = "$0.00",
        grandTotal = "$0.00",
        itemCount = "0 items"
    ),
    val isLoading: Boolean = false,
    val lastScanEvent: ScanEvent? = null,
    val isEmpty: Boolean = true,
    val lookupState: ProductLookupState = ProductLookupState(),
    val selectedItemId: Int? = null,
    val selectedItem: SelectedItemUiModel? = null,
    val modificationTenKeyMode: ModificationTenKeyMode = ModificationTenKeyMode.QUANTITY,
    val modificationInputValue: String = "",
    val managerApprovalState: ManagerApprovalDialogState = ManagerApprovalDialogState(),
    val showVoidConfirmationDialog: Boolean = false,
    val showLogoutDialog: Boolean = false,
    val logoutFeedback: String? = null,
    // Hold/Recall state (per TRANSACTION_FLOW.md)
    val holdDialogState: HoldDialogState = HoldDialogState(),
    val recallDialogState: RecallDialogState = RecallDialogState(),
    val holdRecallFeedback: String? = null,
    
    // Cash Pickup state (per FUNCTIONS_MENU.md: Cash Pickup)
    val cashPickupDialogState: CashPickupDialogState = CashPickupDialogState(),
    val cashPickupFeedback: String? = null
) {
    /**
     * Whether the screen is in modification mode.
     * 
     * Per SCREEN_LAYOUTS.md: When a line item is selected, the right panel
     * transforms to show modification options instead of normal TenKey.
     */
    val isModificationMode: Boolean
        get() = selectedItemId != null
    
    companion object {
        /**
         * Creates the initial empty state.
         */
        fun initial(): CheckoutUiState = CheckoutUiState()
    }
}

