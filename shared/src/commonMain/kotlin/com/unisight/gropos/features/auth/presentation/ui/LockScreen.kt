package com.unisight.gropos.features.auth.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.unisight.gropos.core.session.InactivityManager
import com.unisight.gropos.core.session.LockEventType
import com.unisight.gropos.features.auth.presentation.LockViewModel
import com.unisight.gropos.features.auth.presentation.SignOutResult
import com.unisight.gropos.features.auth.presentation.UnlockResult
import com.unisight.gropos.features.checkout.presentation.ui.CheckoutScreen

/**
 * Lock Screen - Voyager Screen implementation.
 *
 * Per SCREEN_LAYOUTS.md: Displayed when:
 * - Inactivity timeout (5 minutes)
 * - Manual lock (F4 key)
 * - Manager lock
 *
 * Per CASHIER_OPERATIONS.md:
 * - PIN entry unlocks and resumes session
 * - Sign Out returns to LoginScreen
 *
 * Navigation:
 * - Unlock success → Back to previous screen (CheckoutScreen)
 * - Sign Out → LoginScreen
 */
class LockScreen(
    private val lockEventType: LockEventType = LockEventType.Inactivity
) : Screen {
    
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinScreenModel<LockViewModel>()
        val state by viewModel.state.collectAsState()
        
        // Set current screen for InactivityManager
        LaunchedEffect(Unit) {
            InactivityManager.currentScreen = "LockScreen"
            viewModel.onScreenVisible()
        }
        
        LockContent(
            state = state,
            onPinDigit = viewModel::onPinDigit,
            onPinClear = viewModel::onPinClear,
            onPinBackspace = viewModel::onPinBackspace,
            onVerify = {
                when (viewModel.onVerify()) {
                    UnlockResult.Success -> {
                        // Return to previous screen (usually CheckoutScreen)
                        // Using pop if we have history, otherwise replace with CheckoutScreen
                        if (navigator.canPop) {
                            navigator.pop()
                        } else {
                            navigator.replaceAll(CheckoutScreen())
                        }
                    }
                    UnlockResult.Error -> {
                        // Error is shown in state, do nothing
                    }
                }
            },
            onSignOut = {
                when (viewModel.onSignOut()) {
                    SignOutResult.Proceed -> {
                        navigator.replaceAll(LoginScreen())
                    }
                    SignOutResult.RequiresApproval -> {
                        // TODO: Show manager approval dialog
                    }
                }
            }
        )
    }
}

