package com.unisight.gropos.features.payment.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.unisight.gropos.features.auth.presentation.ui.LoginScreen
import com.unisight.gropos.features.checkout.presentation.ui.CheckoutScreen
import com.unisight.gropos.features.payment.presentation.PaymentTab
import com.unisight.gropos.features.payment.presentation.PaymentViewModel
import java.math.BigDecimal

/**
 * Voyager Screen for the Payment feature.
 * 
 * Per SCREEN_LAYOUTS.md (Payment Screen):
 * - Horizontal split layout
 * - Left: Totals summary, SNAP eligible, payments applied
 * - Right: Payment method tabs, TenKey
 * 
 * Per ARCHITECTURE_BLUEPRINT.md:
 * - Observes the same CartRepository singleton as Checkout
 * - Cart totals are synchronized across screens
 */
class PayScreen : Screen {
    
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinScreenModel<PaymentViewModel>()
        val state by viewModel.state.collectAsState()
        
        // Handle transaction completion
        LaunchedEffect(state.isComplete) {
            if (state.isComplete && !state.showChangeDialog) {
                // Transaction complete, navigate back to checkout (or login)
                navigator.replaceAll(CheckoutScreen())
            }
        }
        
        PayContent(
            state = state,
            onEvent = { event ->
                when (event) {
                    // Navigation
                    PaymentEvent.ReturnToItems -> {
                        navigator.pop()
                    }
                    PaymentEvent.HoldTransaction -> {
                        // TODO: Implement hold
                        navigator.replaceAll(CheckoutScreen())
                    }
                    
                    // Tab selection
                    is PaymentEvent.SelectTab -> {
                        viewModel.onTabSelect(event.tab)
                    }
                    
                    // TenKey
                    is PaymentEvent.DigitPress -> {
                        viewModel.onDigitPress(event.digit)
                    }
                    PaymentEvent.ClearPress -> {
                        viewModel.onClearPress()
                    }
                    PaymentEvent.BackspacePress -> {
                        viewModel.onBackspacePress()
                    }
                    
                    // Cash payments
                    PaymentEvent.CashExactChange -> {
                        viewModel.onCashExactChange()
                    }
                    is PaymentEvent.CashQuickAmount -> {
                        viewModel.onCashQuickAmount(event.amount)
                    }
                    PaymentEvent.CashEnteredAmount -> {
                        viewModel.onCashEnteredAmount()
                    }
                    
                    // Card payments via terminal
                    PaymentEvent.CreditPayment -> {
                        viewModel.onCreditPayment()
                    }
                    PaymentEvent.DebitPayment -> {
                        viewModel.onDebitPayment()
                    }
                    PaymentEvent.EbtSnapPayment -> {
                        viewModel.onEbtSnapPayment()
                    }
                    PaymentEvent.EbtCashPayment -> {
                        viewModel.onEbtCashPayment()
                    }
                    PaymentEvent.BalanceCheck -> {
                        viewModel.onBalanceCheck()
                    }
                    
                    // Terminal dialog
                    PaymentEvent.CancelTerminalTransaction -> {
                        viewModel.onCancelTerminalTransaction()
                    }
                    
                    // Dialogs
                    PaymentEvent.DismissChangeDialog -> {
                        viewModel.dismissChangeDialog()
                        // After dismissing change dialog, go back to checkout
                        navigator.replaceAll(CheckoutScreen())
                    }
                    PaymentEvent.DismissError -> {
                        viewModel.dismissError()
                    }
                }
            }
        )
    }
}

/**
 * Events from the Payment UI.
 */
sealed interface PaymentEvent {
    // Navigation
    data object ReturnToItems : PaymentEvent
    data object HoldTransaction : PaymentEvent
    
    // Tab selection
    data class SelectTab(val tab: PaymentTab) : PaymentEvent
    
    // TenKey
    data class DigitPress(val digit: String) : PaymentEvent
    data object ClearPress : PaymentEvent
    data object BackspacePress : PaymentEvent
    
    // Cash payments
    data object CashExactChange : PaymentEvent
    data class CashQuickAmount(val amount: BigDecimal) : PaymentEvent
    data object CashEnteredAmount : PaymentEvent
    
    // Card payments
    data object CreditPayment : PaymentEvent
    data object DebitPayment : PaymentEvent
    data object EbtSnapPayment : PaymentEvent
    data object EbtCashPayment : PaymentEvent
    data object BalanceCheck : PaymentEvent
    
    // Terminal Dialog
    // Per DESKTOP_HARDWARE.md: Cancel button in terminal dialog
    data object CancelTerminalTransaction : PaymentEvent
    
    // Dialogs
    data object DismissChangeDialog : PaymentEvent
    data object DismissError : PaymentEvent
}

