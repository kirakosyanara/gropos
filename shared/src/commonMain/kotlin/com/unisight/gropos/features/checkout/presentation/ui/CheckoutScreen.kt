package com.unisight.gropos.features.checkout.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.unisight.gropos.features.auth.presentation.ui.LoginScreen
import com.unisight.gropos.features.checkout.presentation.CheckoutViewModel
import com.unisight.gropos.features.checkout.presentation.components.ProductLookupUiModel
import com.unisight.gropos.features.lottery.presentation.ui.LotterySaleScreen
import com.unisight.gropos.features.payment.presentation.ui.PayScreen
import com.unisight.gropos.features.transaction.presentation.ui.TransactionHistoryScreen

/**
 * Voyager Screen for the Checkout feature.
 * 
 * Per project-structure.mdc: Screens are in presentation/ui.
 * 
 * Navigation:
 * - Logout button navigates back to LoginScreen
 */
class CheckoutScreen : Screen {
    
    @Composable
    override fun Content() {
        println("[CheckoutScreen] Content() composing...")
        val navigator = LocalNavigator.currentOrThrow
        println("[CheckoutScreen] Getting ViewModel...")
        val viewModel = koinScreenModel<CheckoutViewModel>()
        println("[CheckoutScreen] ViewModel obtained, collecting state...")
        val state by viewModel.state.collectAsState()
        println("[CheckoutScreen] State collected, rendering content...")
        
        CheckoutContent(
            state = state,
            onEvent = { event ->
                when (event) {
                    is CheckoutEvent.ManualBarcodeEnter -> {
                        viewModel.onManualBarcodeEnter(event.barcode)
                    }
                    is CheckoutEvent.RemoveItem -> {
                        viewModel.onRemoveItem(event.branchProductId)
                    }
                    is CheckoutEvent.VoidItem -> {
                        viewModel.onVoidItem(event.branchProductId)
                    }
                    CheckoutEvent.ClearCart -> {
                        viewModel.onClearCart()
                    }
                    CheckoutEvent.DismissScanEvent -> {
                        viewModel.onDismissScanEvent()
                    }
                    CheckoutEvent.Logout -> {
                        // Clear cart before logout
                        viewModel.onClearCart()
                        // Navigate back to LoginScreen, replacing the entire stack
                        navigator.replaceAll(LoginScreen())
                    }
                    // Product Lookup Events
                    CheckoutEvent.OpenLookup -> {
                        viewModel.onOpenLookup()
                    }
                    CheckoutEvent.CloseLookup -> {
                        viewModel.onCloseLookup()
                    }
                    is CheckoutEvent.LookupSearchChange -> {
                        viewModel.onLookupSearchChange(event.query)
                    }
                    is CheckoutEvent.LookupCategorySelect -> {
                        viewModel.onLookupCategorySelect(event.categoryId)
                    }
                    is CheckoutEvent.ProductSelected -> {
                        viewModel.onProductSelected(event.product)
                    }
                    // Navigation Events
                    CheckoutEvent.NavigateToPay -> {
                        // Navigate to Payment screen
                        // PayScreen observes the same CartRepository singleton
                        navigator.push(PayScreen())
                    }
                    CheckoutEvent.NavigateToRecall -> {
                        // Navigate to Transaction History (Recall) screen
                        navigator.push(TransactionHistoryScreen())
                    }
                    CheckoutEvent.NavigateToLottery -> {
                        // Navigate to Lottery Sales screen
                        navigator.push(LotterySaleScreen())
                    }
                    
                    // Modification Mode Events
                    is CheckoutEvent.SelectLineItem -> {
                        viewModel.onSelectLineItem(event.branchProductId)
                    }
                    CheckoutEvent.DeselectLineItem -> {
                        viewModel.onDeselectLineItem()
                    }
                    is CheckoutEvent.ChangeModificationMode -> {
                        viewModel.onChangeModificationMode(event.mode)
                    }
                    is CheckoutEvent.ModificationDigitPress -> {
                        viewModel.onModificationDigitPress(event.digit)
                    }
                    CheckoutEvent.ModificationClear -> {
                        viewModel.onModificationClear()
                    }
                    CheckoutEvent.ModificationBackspace -> {
                        viewModel.onModificationBackspace()
                    }
                    CheckoutEvent.ModificationConfirm -> {
                        viewModel.onModificationConfirm()
                    }
                    CheckoutEvent.VoidSelectedLineItem -> {
                        viewModel.onVoidSelectedLineItem()
                    }
                    
                    // Manager Approval Events
                    CheckoutEvent.DismissManagerApproval -> {
                        viewModel.onDismissManagerApproval()
                    }
                    is CheckoutEvent.SubmitManagerApproval -> {
                        viewModel.onSubmitManagerApproval(event.managerId, event.pin)
                    }
                    
                    // Void Transaction Events
                    CheckoutEvent.VoidTransactionRequest -> {
                        viewModel.onVoidTransactionRequest()
                    }
                    CheckoutEvent.ConfirmVoidTransaction -> {
                        viewModel.onConfirmVoidTransaction()
                    }
                    CheckoutEvent.CancelVoidTransaction -> {
                        viewModel.onCancelVoidTransaction()
                    }
                    
                    // Logout Events
                    CheckoutEvent.OpenLogoutDialog -> {
                        viewModel.onOpenLogoutDialog()
                    }
                    CheckoutEvent.DismissLogoutDialog -> {
                        viewModel.onDismissLogoutDialog()
                    }
                    CheckoutEvent.LockStation -> {
                        viewModel.onLockStation()
                    }
                    CheckoutEvent.ReleaseTill -> {
                        viewModel.onReleaseTill()
                    }
                    CheckoutEvent.EndShift -> {
                        viewModel.onEndShift()
                    }
                    CheckoutEvent.DismissLogoutFeedback -> {
                        viewModel.onDismissLogoutFeedback()
                    }
                    
                    // Hold/Recall Events
                    CheckoutEvent.OpenHoldDialog -> {
                        viewModel.onOpenHoldDialog()
                    }
                    CheckoutEvent.DismissHoldDialog -> {
                        viewModel.onDismissHoldDialog()
                    }
                    is CheckoutEvent.ConfirmHold -> {
                        viewModel.onConfirmHold(event.holdName)
                    }
                    CheckoutEvent.OpenRecallDialog -> {
                        viewModel.onOpenRecallDialog()
                    }
                    CheckoutEvent.DismissRecallDialog -> {
                        viewModel.onDismissRecallDialog()
                    }
                    is CheckoutEvent.RestoreTransaction -> {
                        viewModel.onRestoreTransaction(event.heldTransactionId)
                    }
                    is CheckoutEvent.DeleteHeldTransaction -> {
                        viewModel.onDeleteHeldTransaction(event.heldTransactionId)
                    }
                    CheckoutEvent.DismissHoldRecallFeedback -> {
                        viewModel.onDismissHoldRecallFeedback()
                    }
                    
                    // Cash Pickup Events
                    CheckoutEvent.OpenCashPickupDialog -> {
                        viewModel.onOpenCashPickupDialog()
                    }
                    CheckoutEvent.DismissCashPickupDialog -> {
                        viewModel.onDismissCashPickupDialog()
                    }
                    is CheckoutEvent.CashPickupDigitPress -> {
                        viewModel.onCashPickupDigitPress(event.digit)
                    }
                    CheckoutEvent.CashPickupClear -> {
                        viewModel.onCashPickupClear()
                    }
                    CheckoutEvent.CashPickupBackspace -> {
                        viewModel.onCashPickupBackspace()
                    }
                    CheckoutEvent.CashPickupConfirm -> {
                        viewModel.onCashPickupConfirm()
                    }
                    CheckoutEvent.DismissCashPickupFeedback -> {
                        viewModel.onDismissCashPickupFeedback()
                    }
                    
                    // Age Verification Events
                    is CheckoutEvent.AgeVerificationDigitPress -> {
                        viewModel.onAgeVerificationDigitPress(event.digit)
                    }
                    CheckoutEvent.AgeVerificationClear -> {
                        viewModel.onAgeVerificationClear()
                    }
                    CheckoutEvent.AgeVerificationBackspace -> {
                        viewModel.onAgeVerificationBackspace()
                    }
                    is CheckoutEvent.AgeVerificationFieldSelect -> {
                        viewModel.onAgeVerificationFieldSelect(event.field)
                    }
                    CheckoutEvent.AgeVerificationConfirm -> {
                        viewModel.onAgeVerificationConfirm()
                    }
                    CheckoutEvent.AgeVerificationCancel -> {
                        viewModel.onAgeVerificationCancel()
                    }
                    CheckoutEvent.AgeVerificationManagerOverride -> {
                        viewModel.onAgeVerificationManagerOverride()
                    }
                    
                    // Error Dialog Events
                    CheckoutEvent.DismissError -> {
                        viewModel.dismissError()
                    }
                    
                    // Functions Panel Events
                    CheckoutEvent.OpenFunctionsPanel -> {
                        viewModel.onOpenFunctionsPanel()
                    }
                    CheckoutEvent.DismissFunctionsPanel -> {
                        viewModel.onDismissFunctionsPanel()
                    }
                    
                    // Open Drawer Event
                    CheckoutEvent.OpenDrawer -> {
                        viewModel.onOpenDrawer()
                    }
                    
                    // Price Check Events
                    CheckoutEvent.OpenPriceCheckDialog -> {
                        viewModel.onOpenPriceCheckDialog()
                    }
                    CheckoutEvent.DismissPriceCheckDialog -> {
                        viewModel.onDismissPriceCheckDialog()
                    }
                    is CheckoutEvent.PriceCheckDigitPress -> {
                        viewModel.onPriceCheckDigitPress(event.digit)
                    }
                    CheckoutEvent.PriceCheckClear -> {
                        viewModel.onPriceCheckClear()
                    }
                    CheckoutEvent.PriceCheckBackspace -> {
                        viewModel.onPriceCheckBackspace()
                    }
                    CheckoutEvent.PriceCheckLookup -> {
                        viewModel.onPriceCheckLookup()
                    }
                    
                    // Add Cash Events
                    CheckoutEvent.OpenAddCashDialog -> {
                        viewModel.onOpenAddCashDialog()
                    }
                    CheckoutEvent.DismissAddCashDialog -> {
                        viewModel.onDismissAddCashDialog()
                    }
                    is CheckoutEvent.AddCashDigitPress -> {
                        viewModel.onAddCashDigitPress(event.digit)
                    }
                    CheckoutEvent.AddCashClear -> {
                        viewModel.onAddCashClear()
                    }
                    CheckoutEvent.AddCashBackspace -> {
                        viewModel.onAddCashBackspace()
                    }
                    CheckoutEvent.AddCashConfirm -> {
                        viewModel.onAddCashConfirm()
                    }
                    CheckoutEvent.DismissAddCashFeedback -> {
                        viewModel.onDismissScanEvent() // Reuse scan event dismissal
                    }
                    
                    // Vendor Payout Events
                    CheckoutEvent.OpenVendorPayoutDialog -> {
                        viewModel.onOpenVendorPayoutDialog()
                    }
                    CheckoutEvent.DismissVendorPayoutDialog -> {
                        viewModel.onDismissVendorPayoutDialog()
                    }
                    is CheckoutEvent.VendorPayoutSelectVendor -> {
                        viewModel.onVendorPayoutSelectVendor(event.vendorId, event.vendorName)
                    }
                    is CheckoutEvent.VendorPayoutDigitPress -> {
                        viewModel.onVendorPayoutDigitPress(event.digit)
                    }
                    CheckoutEvent.VendorPayoutClear -> {
                        viewModel.onVendorPayoutClear()
                    }
                    CheckoutEvent.VendorPayoutBackspace -> {
                        viewModel.onVendorPayoutBackspace()
                    }
                    CheckoutEvent.VendorPayoutConfirm -> {
                        viewModel.onVendorPayoutConfirm()
                    }
                    CheckoutEvent.VendorPayoutBack -> {
                        viewModel.onVendorPayoutBack()
                    }
                    CheckoutEvent.DismissVendorPayoutFeedback -> {
                        viewModel.onDismissVendorPayoutFeedback()
                    }
                    
                    // EBT Balance Check Events
                    CheckoutEvent.OpenEbtBalanceDialog -> {
                        viewModel.onOpenEbtBalanceDialog()
                    }
                    CheckoutEvent.DismissEbtBalanceDialog -> {
                        viewModel.onDismissEbtBalanceDialog()
                    }
                    CheckoutEvent.EbtBalanceInquiry -> {
                        viewModel.onEbtBalanceInquiry()
                    }
                    
                    // Transaction Discount Events
                    CheckoutEvent.OpenTransactionDiscountDialog -> {
                        viewModel.onOpenTransactionDiscountDialog()
                    }
                    CheckoutEvent.DismissTransactionDiscountDialog -> {
                        viewModel.onDismissTransactionDiscountDialog()
                    }
                    is CheckoutEvent.TransactionDiscountDigitPress -> {
                        viewModel.onTransactionDiscountDigitPress(event.digit)
                    }
                    CheckoutEvent.TransactionDiscountClear -> {
                        viewModel.onTransactionDiscountClear()
                    }
                    CheckoutEvent.TransactionDiscountBackspace -> {
                        viewModel.onTransactionDiscountBackspace()
                    }
                    CheckoutEvent.TransactionDiscountConfirm -> {
                        viewModel.onTransactionDiscountConfirm()
                    }
                    
                    // QTY Prefix Events
                    is CheckoutEvent.SetQuantityPrefix -> {
                        viewModel.onSetQuantityPrefix(event.quantity)
                    }
                    CheckoutEvent.ClearQuantityPrefix -> {
                        viewModel.onClearQuantityPrefix()
                    }
                    
                    // Product Info Dialog Events
                    CheckoutEvent.OpenProductInfoDialog -> {
                        viewModel.onOpenProductInfoDialog()
                    }
                    CheckoutEvent.DismissProductInfoDialog -> {
                        viewModel.onDismissProductInfoDialog()
                    }
                }
            }
        )
    }
}

/**
 * Events from the Checkout UI.
 * 
 * Per code-quality.mdc: Events flow up via lambdas.
 */
sealed interface CheckoutEvent {
    data class ManualBarcodeEnter(val barcode: String) : CheckoutEvent
    data class RemoveItem(val branchProductId: Int) : CheckoutEvent
    data class VoidItem(val branchProductId: Int) : CheckoutEvent
    data object ClearCart : CheckoutEvent
    data object DismissScanEvent : CheckoutEvent
    data object Logout : CheckoutEvent
    
    // Product Lookup Dialog Events
    data object OpenLookup : CheckoutEvent
    data object CloseLookup : CheckoutEvent
    data class LookupSearchChange(val query: String) : CheckoutEvent
    data class LookupCategorySelect(val categoryId: Int?) : CheckoutEvent
    data class ProductSelected(val product: ProductLookupUiModel) : CheckoutEvent
    
    // Navigation Events
    data object NavigateToPay : CheckoutEvent
    data object NavigateToRecall : CheckoutEvent
    data object NavigateToLottery : CheckoutEvent
    
    // ========================================================================
    // Modification Mode Events
    // Per SCREEN_LAYOUTS.md: When a line item is selected, right panel transforms
    // ========================================================================
    
    /** Select a line item to enter modification mode */
    data class SelectLineItem(val branchProductId: Int) : CheckoutEvent
    
    /** Deselect item and exit modification mode */
    data object DeselectLineItem : CheckoutEvent
    
    /** Change the TenKey mode in modification (QUANTITY, DISCOUNT, PRICE) */
    data class ChangeModificationMode(val mode: com.unisight.gropos.features.checkout.presentation.ModificationTenKeyMode) : CheckoutEvent
    
    /** Digit pressed in modification mode */
    data class ModificationDigitPress(val digit: String) : CheckoutEvent
    
    /** Clear pressed in modification mode */
    data object ModificationClear : CheckoutEvent
    
    /** Backspace pressed in modification mode */
    data object ModificationBackspace : CheckoutEvent
    
    /** OK/Confirm pressed in modification mode - applies the change */
    data object ModificationConfirm : CheckoutEvent
    
    /** Void the selected line item */
    data object VoidSelectedLineItem : CheckoutEvent
    
    // ========================================================================
    // Manager Approval Dialog Events
    // Per ROLES_AND_PERMISSIONS.md: Manager approval for sensitive actions
    // ========================================================================
    
    /** Dismiss the manager approval dialog */
    data object DismissManagerApproval : CheckoutEvent
    
    /** Submit manager PIN for approval */
    data class SubmitManagerApproval(val managerId: Int, val pin: String) : CheckoutEvent
    
    // ========================================================================
    // Void Transaction Events
    // Per FUNCTIONS_MENU.md: Void Transaction cancels entire transaction
    // ========================================================================
    
    /** Request to void the entire transaction */
    data object VoidTransactionRequest : CheckoutEvent
    
    /** Confirm void transaction after dialog */
    data object ConfirmVoidTransaction : CheckoutEvent
    
    /** Cancel the void transaction dialog */
    data object CancelVoidTransaction : CheckoutEvent
    
    // ========================================================================
    // Logout Events
    // Per CASHIER_OPERATIONS.md: Logout options (Lock, Release Till, End Shift)
    // ========================================================================
    
    /** Open the logout dialog */
    data object OpenLogoutDialog : CheckoutEvent
    
    /** Dismiss the logout dialog */
    data object DismissLogoutDialog : CheckoutEvent
    
    /** Lock the station (keep session active) */
    data object LockStation : CheckoutEvent
    
    /** Release the till and logout */
    data object ReleaseTill : CheckoutEvent
    
    /** End the shift with report */
    data object EndShift : CheckoutEvent
    
    /** Dismiss logout feedback */
    data object DismissLogoutFeedback : CheckoutEvent
    
    // ========================================================================
    // Hold/Recall Transaction Events
    // Per TRANSACTION_FLOW.md: Suspend and Resume transactions
    // ========================================================================
    
    /** Open the hold transaction dialog */
    data object OpenHoldDialog : CheckoutEvent
    
    /** Dismiss the hold transaction dialog */
    data object DismissHoldDialog : CheckoutEvent
    
    /** Confirm hold with optional name */
    data class ConfirmHold(val holdName: String?) : CheckoutEvent
    
    /** Open the recall transactions dialog */
    data object OpenRecallDialog : CheckoutEvent
    
    /** Dismiss the recall transactions dialog */
    data object DismissRecallDialog : CheckoutEvent
    
    /** Restore a held transaction */
    data class RestoreTransaction(val heldTransactionId: String) : CheckoutEvent
    
    /** Delete a held transaction */
    data class DeleteHeldTransaction(val heldTransactionId: String) : CheckoutEvent
    
    /** Dismiss hold/recall feedback */
    data object DismissHoldRecallFeedback : CheckoutEvent
    
    // ========================================================================
    // Cash Pickup Events
    // Per FUNCTIONS_MENU.md: Cash Pickup removes cash from drawer for safe
    // ========================================================================
    
    /** Open the cash pickup dialog */
    data object OpenCashPickupDialog : CheckoutEvent
    
    /** Dismiss the cash pickup dialog */
    data object DismissCashPickupDialog : CheckoutEvent
    
    /** Digit pressed in cash pickup dialog */
    data class CashPickupDigitPress(val digit: String) : CheckoutEvent
    
    /** Clear pressed in cash pickup dialog */
    data object CashPickupClear : CheckoutEvent
    
    /** Backspace pressed in cash pickup dialog */
    data object CashPickupBackspace : CheckoutEvent
    
    /** Confirm the cash pickup */
    data object CashPickupConfirm : CheckoutEvent
    
    /** Dismiss cash pickup feedback */
    data object DismissCashPickupFeedback : CheckoutEvent
    
    // ========================================================================
    // Age Verification Events
    // Per DIALOGS.md: Age Verification for alcohol/tobacco products
    // ========================================================================
    
    /** Digit pressed in age verification dialog */
    data class AgeVerificationDigitPress(val digit: String) : CheckoutEvent
    
    /** Clear pressed in age verification dialog */
    data object AgeVerificationClear : CheckoutEvent
    
    /** Backspace pressed in age verification dialog */
    data object AgeVerificationBackspace : CheckoutEvent
    
    /** Select a date field (Month, Day, Year) */
    data class AgeVerificationFieldSelect(val field: com.unisight.gropos.features.checkout.presentation.DateField) : CheckoutEvent
    
    /** Confirm age verification and add product to cart */
    data object AgeVerificationConfirm : CheckoutEvent
    
    /** Cancel age verification (product not added) */
    data object AgeVerificationCancel : CheckoutEvent
    
    /** Request manager override for age verification */
    data object AgeVerificationManagerOverride : CheckoutEvent
    
    // ========================================================================
    // Error Dialog Events
    // Per DIALOGS.md (Error Message Dialog): Critical Alert System
    // ========================================================================
    
    /** Dismiss the critical error dialog */
    data object DismissError : CheckoutEvent
    
    // ========================================================================
    // Functions Panel Events
    // Per FUNCTIONS_MENU.md: Full functions panel with tabs
    // ========================================================================
    
    /** Open the functions panel */
    data object OpenFunctionsPanel : CheckoutEvent
    
    /** Dismiss the functions panel */
    data object DismissFunctionsPanel : CheckoutEvent
    
    // ========================================================================
    // Open Drawer Event
    // Per FUNCTIONS_MENU.md: Open cash drawer without transaction
    // ========================================================================
    
    /** Open the cash drawer */
    data object OpenDrawer : CheckoutEvent
    
    // ========================================================================
    // Price Check Events
    // Per FUNCTIONS_MENU.md: Scan item to see price without adding to cart
    // ========================================================================
    
    /** Open the price check dialog */
    data object OpenPriceCheckDialog : CheckoutEvent
    
    /** Dismiss the price check dialog */
    data object DismissPriceCheckDialog : CheckoutEvent
    
    /** Digit pressed in price check barcode input */
    data class PriceCheckDigitPress(val digit: String) : CheckoutEvent
    
    /** Clear pressed in price check dialog */
    data object PriceCheckClear : CheckoutEvent
    
    /** Backspace pressed in price check dialog */
    data object PriceCheckBackspace : CheckoutEvent
    
    /** Look up the product for price check */
    data object PriceCheckLookup : CheckoutEvent
    
    // ========================================================================
    // Add Cash Events
    // Per FUNCTIONS_MENU.md: Add cash to drawer
    // ========================================================================
    
    /** Open the add cash dialog */
    data object OpenAddCashDialog : CheckoutEvent
    
    /** Dismiss the add cash dialog */
    data object DismissAddCashDialog : CheckoutEvent
    
    /** Digit pressed in add cash dialog */
    data class AddCashDigitPress(val digit: String) : CheckoutEvent
    
    /** Clear pressed in add cash dialog */
    data object AddCashClear : CheckoutEvent
    
    /** Backspace pressed in add cash dialog */
    data object AddCashBackspace : CheckoutEvent
    
    /** Confirm the add cash */
    data object AddCashConfirm : CheckoutEvent
    
    /** Dismiss add cash feedback */
    data object DismissAddCashFeedback : CheckoutEvent
    
    // ========================================================================
    // Vendor Payout Events
    // Per FUNCTIONS_MENU.md: Vendor Payout flow
    // ========================================================================
    
    /** Open the vendor payout dialog */
    data object OpenVendorPayoutDialog : CheckoutEvent
    
    /** Dismiss the vendor payout dialog */
    data object DismissVendorPayoutDialog : CheckoutEvent
    
    /** Select a vendor in step 1 */
    data class VendorPayoutSelectVendor(val vendorId: String, val vendorName: String) : CheckoutEvent
    
    /** Digit pressed in vendor payout amount input */
    data class VendorPayoutDigitPress(val digit: String) : CheckoutEvent
    
    /** Clear pressed in vendor payout dialog */
    data object VendorPayoutClear : CheckoutEvent
    
    /** Backspace pressed in vendor payout dialog */
    data object VendorPayoutBackspace : CheckoutEvent
    
    /** Confirm the vendor payout */
    data object VendorPayoutConfirm : CheckoutEvent
    
    /** Go back from amount to vendor selection */
    data object VendorPayoutBack : CheckoutEvent
    
    /** Dismiss vendor payout feedback */
    data object DismissVendorPayoutFeedback : CheckoutEvent
    
    // ========================================================================
    // EBT Balance Check Events
    // Per FUNCTIONS_MENU.md: Check customer's EBT balance
    // ========================================================================
    
    /** Open the EBT balance check dialog */
    data object OpenEbtBalanceDialog : CheckoutEvent
    
    /** Dismiss the EBT balance check dialog */
    data object DismissEbtBalanceDialog : CheckoutEvent
    
    /** Start the EBT balance inquiry */
    data object EbtBalanceInquiry : CheckoutEvent
    
    // ========================================================================
    // Transaction Discount Events
    // Per FUNCTIONS_MENU.md: Apply percentage discount to entire order
    // ========================================================================
    
    /** Open the transaction discount dialog */
    data object OpenTransactionDiscountDialog : CheckoutEvent
    
    /** Dismiss the transaction discount dialog */
    data object DismissTransactionDiscountDialog : CheckoutEvent
    
    /** Digit pressed in transaction discount dialog */
    data class TransactionDiscountDigitPress(val digit: String) : CheckoutEvent
    
    /** Clear pressed in transaction discount dialog */
    data object TransactionDiscountClear : CheckoutEvent
    
    /** Backspace pressed in transaction discount dialog */
    data object TransactionDiscountBackspace : CheckoutEvent
    
    /** Confirm the transaction discount */
    data object TransactionDiscountConfirm : CheckoutEvent
    
    // ========================================================================
    // QTY Prefix Events
    // Per CHECKOUT: Enter qty → press QTY → scan = single line with qty
    // ========================================================================
    
    /** Set quantity prefix for next scan */
    data class SetQuantityPrefix(val quantity: Int) : CheckoutEvent
    
    /** Clear the quantity prefix */
    data object ClearQuantityPrefix : CheckoutEvent
    
    // ========================================================================
    // Product Info Dialog Events
    // Per REMEDIATION_CHECKLIST: More Information Dialog
    // ========================================================================
    
    /** Open product info dialog for the selected item */
    data object OpenProductInfoDialog : CheckoutEvent
    
    /** Dismiss the product info dialog */
    data object DismissProductInfoDialog : CheckoutEvent
}
