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
}
