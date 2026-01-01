package com.unisight.gropos.features.checkout.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import com.unisight.gropos.features.checkout.presentation.CheckoutViewModel

/**
 * Voyager Screen for the Checkout feature.
 * 
 * Per project-structure.mdc: Screens are in presentation/ui.
 */
class CheckoutScreen : Screen {
    
    @Composable
    override fun Content() {
        val viewModel = getScreenModel<CheckoutViewModel>()
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
}

