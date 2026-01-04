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
import com.unisight.gropos.features.checkout.presentation.ui.CheckoutScreen

/**
 * Lock Screen - Voyager Screen implementation.
 *
 * Per LOCK_SCREEN_AND_CASHIER_LOGIN.md: Displayed when:
 * - Inactivity timeout (5 minutes)
 * - Manual lock (F4 key)
 * - Manager lock
 *
 * PIN verification now uses async API call:
 * - onVerify() starts the verification
 * - UI observes state.unlockSuccess for navigation
 *
 * Sign-out flow (L4, U1, U2):
 * - onSignOut() shows LogoutOptionsDialog
 * - User selects Release Till or End of Shift
 * - ManagerApprovalDialog shown for PIN verification
 * - On success, navigates to LoginScreen
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
        
        // Observe unlockSuccess and navigate when true
        LaunchedEffect(state.unlockSuccess) {
            if (state.unlockSuccess) {
                if (navigator.canPop) {
                    navigator.pop()
                } else {
                    navigator.replaceAll(CheckoutScreen())
                }
            }
        }
        
        // Observe signOutSuccess and navigate to login
        LaunchedEffect(state.signOutSuccess) {
            if (state.signOutSuccess) {
                navigator.replaceAll(LoginScreen())
            }
        }
        
        // Main lock screen content
        LockContent(
            state = state,
            onPinDigit = viewModel::onPinDigit,
            onPinClear = viewModel::onPinClear,
            onPinBackspace = viewModel::onPinBackspace,
            onVerify = { viewModel.onVerify() },
            onSignOut = { viewModel.onSignOut() }
        )
        
        // Logout Options Dialog (U1)
        if (state.showLogoutOptions) {
            LogoutOptionsDialog(
                employeeName = state.employeeName,
                onReleaseTill = viewModel::onReleaseTillSelected,
                onEndOfShift = viewModel::onEndOfShiftSelected,
                onDismiss = viewModel::onDismissLogoutOptions
            )
        }
        
        // Manager Approval Dialog (U2)
        if (state.showManagerApproval) {
            ManagerApprovalDialog(
                actionDescription = state.managerApprovalAction,
                onApprove = viewModel::onManagerApproval,
                onDismiss = viewModel::onDismissManagerApproval,
                isVerifying = state.isVerifyingManager,
                errorMessage = state.managerApprovalError
            )
        }
    }
}

