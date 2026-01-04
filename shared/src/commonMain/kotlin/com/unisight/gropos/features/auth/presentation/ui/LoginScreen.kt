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
import com.unisight.gropos.features.device.presentation.ui.RegistrationScreen
import com.unisight.gropos.features.settings.presentation.AdminSettingsDialog
import com.unisight.gropos.features.settings.presentation.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
        
        // Navigate to RegistrationScreen if device has been deleted (410 Gone)
        // Per device re-registration flow: Clear local data and show QR registration
        LaunchedEffect(state.requiresReRegistration) {
            if (state.requiresReRegistration) {
                println("[LoginScreen] ========================================")
                println("[LoginScreen] DEVICE DELETED - Navigating to RegistrationScreen")
                println("[LoginScreen] ========================================")
                
                // Replace navigation stack with RegistrationScreen
                navigator.replaceAll(RegistrationScreen())
            }
        }
        
        // Navigate to CheckoutScreen on successful login
        LaunchedEffect(state.stage) {
            if (state.stage == LoginStage.SUCCESS) {
                println("[LoginScreen] ========================================")
                println("[LoginScreen] SUCCESS stage reached - starting navigation...")
                println("[LoginScreen] ========================================")
                
                try {
                    // Start inactivity monitoring
                    // Per CASHIER_OPERATIONS.md: Timer starts after successful login
                    InactivityManager.start()
                    println("[LoginScreen] InactivityManager started")
                    
                    // Start HeartbeatService for background data sync
                    // Per SYNC_MECHANISM.md: Background sync starts after login
                    // IMPORTANT: start() is a suspend fun that can block - launch in background!
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            heartbeatService.start()
                            println("[LoginScreen] HeartbeatService started in background")
                        } catch (e: Exception) {
                            println("[LoginScreen] HeartbeatService FAILED: ${e.message}")
                        }
                    }
                    println("[LoginScreen] HeartbeatService launch initiated (non-blocking)")
                    
                    // Replace entire navigation stack to prevent back navigation to login
                    println("[LoginScreen] Navigating to CheckoutScreen...")
                    println("[LoginScreen] Creating CheckoutScreen instance...")
                    val checkoutScreen = CheckoutScreen()
                    println("[LoginScreen] CheckoutScreen created, calling replaceAll...")
                    navigator.replaceAll(checkoutScreen)
                    println("[LoginScreen] replaceAll() called successfully")
                    
                } catch (e: Exception) {
                    println("[LoginScreen] !!!! NAVIGATION ERROR !!!!")
                    println("[LoginScreen] Exception: ${e::class.simpleName}: ${e.message}")
                    e.printStackTrace()
                }
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
