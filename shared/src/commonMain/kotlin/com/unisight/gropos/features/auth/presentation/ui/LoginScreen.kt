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
import com.unisight.gropos.core.sync.HeartbeatService
import com.unisight.gropos.features.auth.presentation.LoginStage
import com.unisight.gropos.features.auth.presentation.LoginViewModel
import com.unisight.gropos.features.checkout.presentation.ui.CheckoutScreen
import com.unisight.gropos.features.settings.presentation.AdminSettingsDialog
import com.unisight.gropos.features.settings.presentation.SettingsViewModel
import org.koin.compose.koinInject

/**
 * Login screen using Voyager navigation.
 * 
 * Per CASHIER_OPERATIONS.md:
 * Implements the login state machine:
 * LOADING -> EMPLOYEE_SELECT -> PIN_ENTRY -> TILL_ASSIGNMENT -> SUCCESS
 * 
 * Navigation:
 * - On SUCCESS stage, navigates to CheckoutScreen and starts InactivityManager
 */
class LoginScreen : Screen {
    
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinScreenModel<LoginViewModel>()
        val state by viewModel.state.collectAsState()
        
        // Inject HeartbeatService for background sync
        val heartbeatService: HeartbeatService = koinInject()
        
        // Navigate to CheckoutScreen on successful login
        LaunchedEffect(state.stage) {
            if (state.stage == LoginStage.SUCCESS) {
                println("[LoginScreen] SUCCESS stage reached - starting navigation...")
                
                try {
                    // Start inactivity monitoring
                    // Per CASHIER_OPERATIONS.md: Timer starts after successful login
                    InactivityManager.start()
                    println("[LoginScreen] InactivityManager started")
                    
                    // Start HeartbeatService for background data sync
                    // Per SYNC_MECHANISM.md: Background sync starts after login
                    // Note: start() is non-blocking (launches a coroutine internally)
                    heartbeatService.start()
                    println("[LoginScreen] HeartbeatService started")
                    
                } catch (e: Exception) {
                    println("[LoginScreen] ERROR starting services: ${e.message}")
                    e.printStackTrace()
                }
                
                // Replace entire navigation stack to prevent back navigation to login
                println("[LoginScreen] Navigating to CheckoutScreen...")
                navigator.replaceAll(CheckoutScreen())
                println("[LoginScreen] Navigation complete")
            }
        }
        
        // Set current screen for InactivityManager (helps with exemption logic)
        LaunchedEffect(Unit) {
            InactivityManager.currentScreen = "LoginScreen"
        }
        
        LoginContent(
            state = state,
            onEmployeeSelected = viewModel::onEmployeeSelected,
            onPinDigit = viewModel::onPinDigit,
            onPinClear = viewModel::onPinClear,
            onPinBackspace = viewModel::onPinBackspace,
            onPinSubmit = viewModel::onPinSubmit,
            onTillSelected = viewModel::onTillSelected,
            onBackPressed = viewModel::onBackPressed,
            onErrorDismissed = viewModel::onErrorDismissed,
            onRefresh = viewModel::onRefresh,
            onAdminSettingsClick = viewModel::showAdminSettings,
            onBadgeLoginClick = viewModel::onBadgeLoginClick,
            onCancelNfcScan = viewModel::onCancelNfcScan
        )
        
        // Admin Settings Dialog (Hidden Menu)
        // Per SCREEN_LAYOUTS.md: Accessible via secret trigger on Login Screen footer
        if (state.showAdminSettings) {
            val settingsViewModel: SettingsViewModel = koinInject()
            AdminSettingsDialog(
                viewModel = settingsViewModel,
                onDismiss = viewModel::hideAdminSettings
            )
        }
    }
}
