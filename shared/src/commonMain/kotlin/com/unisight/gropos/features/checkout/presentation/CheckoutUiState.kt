package com.unisight.gropos.features.checkout.presentation

import com.unisight.gropos.core.components.dialogs.ErrorDialogState
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
    val rawQuantity: Int = 1,
    val isVoided: Boolean = false
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
 * State for the Vendor Payout Dialog.
 * 
 * Per FUNCTIONS_MENU.md (Vendor Payout section):
 * - Pay vendors directly from the till
 * - Flow: Select vendor → Enter amount → Manager approval → Cash dispensed
 * - Cannot pay out more than drawer balance
 * 
 * @property isVisible Whether the dialog is showing
 * @property step Current step (VENDOR_SELECTION or AMOUNT_INPUT)
 * @property vendors List of available vendors
 * @property selectedVendorId ID of the selected vendor (null in step 1)
 * @property selectedVendorName Name of the selected vendor for display
 * @property inputValue Current input value for the amount
 * @property currentBalance Current drawer balance (formatted)
 * @property errorMessage Validation error message
 * @property isProcessing Whether payout is being processed
 * @property approvalPending Whether manager approval is pending
 */
data class VendorPayoutDialogState(
    val isVisible: Boolean = false,
    val step: VendorPayoutStep = VendorPayoutStep.VENDOR_SELECTION,
    val vendors: List<VendorUiModel> = emptyList(),
    val selectedVendorId: String? = null,
    val selectedVendorName: String? = null,
    val inputValue: String = "",
    val currentBalance: String = "$0.00",
    val errorMessage: String? = null,
    val isProcessing: Boolean = false,
    val approvalPending: Boolean = false
)

/**
 * Steps in the Vendor Payout dialog flow.
 */
enum class VendorPayoutStep {
    /** Step 1: Select a vendor from the list */
    VENDOR_SELECTION,
    /** Step 2: Enter the payout amount */
    AMOUNT_INPUT
}

/**
 * UI model for displaying a vendor in the payout dialog.
 */
data class VendorUiModel(
    val id: String,
    val name: String
)

/**
 * State for the Age Verification Dialog.
 * 
 * Per DIALOGS.md (Age Verification section):
 * - Triggered when scanning age-restricted products (alcohol, tobacco)
 * - Requires date of birth entry to verify customer age
 * - Item MUST NOT be added to cart until verification passes
 * - Supports Manager Override for special cases
 *
 * @property isVisible Whether the dialog is showing
 * @property productName Name of the age-restricted product
 * @property requiredAge Minimum age required (18, 21)
 * @property branchProductId The product ID pending verification
 * @property monthInput Month component of DOB (MM)
 * @property dayInput Day component of DOB (DD)
 * @property yearInput Year component of DOB (YYYY)
 * @property activeField Which date field is currently being edited
 * @property calculatedAge Age calculated from entered DOB (null if DOB incomplete)
 * @property errorMessage Validation error message
 * @property isProcessing Whether manager override is being processed
 */
data class AgeVerificationDialogState(
    val isVisible: Boolean = false,
    val productName: String = "",
    val requiredAge: Int = 21,
    val branchProductId: Int = 0,
    val monthInput: String = "",
    val dayInput: String = "",
    val yearInput: String = "",
    val activeField: DateField = DateField.MONTH,
    val calculatedAge: Int? = null,
    val errorMessage: String? = null,
    val isProcessing: Boolean = false
) {
    /**
     * Whether the entered DOB is complete (all fields filled).
     */
    val isDateComplete: Boolean
        get() = monthInput.length == 2 && dayInput.length == 2 && yearInput.length == 4
    
    /**
     * Whether the customer meets the age requirement.
     */
    val meetsAgeRequirement: Boolean
        get() = calculatedAge != null && calculatedAge >= requiredAge
    
    /**
     * Formatted date string for display.
     */
    val formattedDate: String
        get() = "$monthInput/$dayInput/$yearInput"
}

/**
 * Which field of the date input is currently active.
 */
enum class DateField {
    MONTH,
    DAY,
    YEAR
}

/**
 * State for the Price Check Dialog.
 * 
 * Per FUNCTIONS_MENU.md (Price Check section):
 * - Scan or enter barcode to see price without adding to cart
 * - Shows product name, price, and SNAP eligibility
 * - No manager approval required
 * 
 * @property isVisible Whether the dialog is showing
 * @property barcodeInput Current barcode input
 * @property isLoading Whether a product lookup is in progress
 * @property productName Name of the found product (null if not found)
 * @property productPrice Formatted price string
 * @property isSnapEligible Whether product is SNAP eligible
 * @property errorMessage Error message if product not found
 */
data class PriceCheckDialogState(
    val isVisible: Boolean = false,
    val barcodeInput: String = "",
    val isLoading: Boolean = false,
    val productName: String? = null,
    val productPrice: String? = null,
    val isSnapEligible: Boolean = false,
    val errorMessage: String? = null
) {
    /**
     * Whether a product has been found.
     */
    val hasProduct: Boolean
        get() = productName != null
}

/**
 * State for the Add Cash Dialog.
 * 
 * Per FUNCTIONS_MENU.md (Add Cash section):
 * - Add cash to the till drawer (e.g., starting float)
 * - Requires Manager approval for amounts over $100
 * - Updates drawer balance
 * 
 * @property isVisible Whether the dialog is showing
 * @property inputValue Current input value (cents)
 * @property currentBalance Current drawer balance (formatted)
 * @property errorMessage Validation error message
 * @property isProcessing Whether add cash is being processed
 * @property approvalPending Whether manager approval is pending
 */
data class AddCashDialogState(
    val isVisible: Boolean = false,
    val inputValue: String = "",
    val currentBalance: String = "$0.00",
    val errorMessage: String? = null,
    val isProcessing: Boolean = false,
    val approvalPending: Boolean = false
)

/**
 * State for the EBT Balance Check Dialog.
 * 
 * Per FUNCTIONS_MENU.md (EBT Balance section):
 * - Check customer's EBT balance before payment
 * - Requires card swipe/insertion
 * - Shows food stamp and cash balances
 * 
 * @property isVisible Whether the dialog is showing
 * @property isProcessing Whether balance inquiry is in progress
 * @property foodStampBalance SNAP/Food Stamp balance (formatted)
 * @property cashBalance EBT Cash balance (formatted)
 * @property errorMessage Error message if inquiry failed
 * @property hasResult Whether a balance result has been received
 */
data class EbtBalanceDialogState(
    val isVisible: Boolean = false,
    val isProcessing: Boolean = false,
    val foodStampBalance: String? = null,
    val cashBalance: String? = null,
    val errorMessage: String? = null,
    val hasResult: Boolean = false
)

/**
 * State for the Transaction Discount Dialog.
 * 
 * Per FUNCTIONS_MENU.md (Discount section):
 * - Apply percentage discount to entire order
 * - Requires Manager approval
 * - Shows before/after totals
 * 
 * @property isVisible Whether the dialog is showing
 * @property inputValue Current input value (percentage)
 * @property currentTotal Current order total (formatted)
 * @property discountedTotal Discounted total preview (formatted)
 * @property errorMessage Validation error message
 * @property isProcessing Whether discount is being applied
 * @property approvalPending Whether manager approval is pending
 */
data class TransactionDiscountDialogState(
    val isVisible: Boolean = false,
    val inputValue: String = "",
    val currentTotal: String = "$0.00",
    val discountedTotal: String? = null,
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
    val cashPickupFeedback: String? = null,
    
    // Vendor Payout state (per FUNCTIONS_MENU.md: Vendor Payout)
    val vendorPayoutDialogState: VendorPayoutDialogState = VendorPayoutDialogState(),
    val vendorPayoutFeedback: String? = null,
    
    // Age Verification state (per DIALOGS.md: Age Verification Dialog)
    val ageVerificationDialogState: AgeVerificationDialogState = AgeVerificationDialogState(),
    
    // Error Dialog state (per DIALOGS.md: Error Message Dialog)
    // Use for CRITICAL errors that stop the flow: Payments, Hardware, Legal
    val errorDialog: ErrorDialogState? = null,
    
    // Price Check state (per FUNCTIONS_MENU.md: Price Check)
    val priceCheckDialogState: PriceCheckDialogState = PriceCheckDialogState(),
    
    // Add Cash state (per FUNCTIONS_MENU.md: Add Cash)
    val addCashDialogState: AddCashDialogState = AddCashDialogState(),
    
    // EBT Balance Check state (per FUNCTIONS_MENU.md: EBT Balance)
    val ebtBalanceDialogState: EbtBalanceDialogState = EbtBalanceDialogState(),
    
    // Transaction Discount state (per FUNCTIONS_MENU.md: Discount)
    val transactionDiscountDialogState: TransactionDiscountDialogState = TransactionDiscountDialogState(),
    
    // Functions Panel state
    val showFunctionsPanel: Boolean = false,
    
    // Quantity prefix for multiple scan (per CHECKOUT: QTY Prefix)
    val quantityPrefix: Int? = null,
    
    // Product Info Dialog (per REMEDIATION_CHECKLIST: More Information Dialog)
    val showProductInfoDialog: Boolean = false,
    val productInfoDialogProduct: com.unisight.gropos.features.checkout.domain.model.Product? = null
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

