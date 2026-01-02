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
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinScreenModel<CheckoutViewModel>()
        val state by viewModel.state.collectAsState()
        
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
}
