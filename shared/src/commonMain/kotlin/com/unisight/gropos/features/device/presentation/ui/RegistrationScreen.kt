package com.unisight.gropos.features.device.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.unisight.gropos.features.auth.presentation.ui.LoginScreen
import com.unisight.gropos.features.device.domain.model.RegistrationState
import com.unisight.gropos.features.device.presentation.RegistrationEvent
import com.unisight.gropos.features.device.presentation.RegistrationViewModel
import com.unisight.gropos.features.settings.presentation.AdminSettingsDialog
import com.unisight.gropos.features.settings.presentation.SettingsViewModel
import org.koin.compose.koinInject

/**
 * Registration screen for device activation.
 * 
 * Per DEVICE_REGISTRATION.md:
 * - Shown on first launch when device is not registered
 * - Displays pairing code and QR code for admin activation
 * - Transitions to LoginScreen after successful registration
 * 
 * Per Governance:
 * - Hidden Settings must be accessible from this screen
 * - Technicians need to set environment BEFORE registering
 */
class RegistrationScreen : Screen {
    
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinScreenModel<RegistrationViewModel>()
        val state by viewModel.state.collectAsState()
        
        // Navigate to LoginScreen when registration is complete
        LaunchedEffect(state.registrationState) {
            if (state.registrationState == RegistrationState.REGISTERED) {
                // Small delay for UX (show "Registered!" state briefly)
                kotlinx.coroutines.delay(1000)
                
                // Replace entire navigation stack to prevent back navigation
                navigator.replaceAll(LoginScreen())
            }
        }
        
        RegistrationContent(
            state = state,
            onRefreshPairingCode = { viewModel.onEvent(RegistrationEvent.RefreshPairingCode) },
            onSimulateActivation = { viewModel.onEvent(RegistrationEvent.SimulateActivation) },
            onAdminSettingsClick = { viewModel.onEvent(RegistrationEvent.ShowAdminSettings) },
            onErrorDismiss = { viewModel.onEvent(RegistrationEvent.DismissError) }
        )
        
        // Admin Settings Dialog (Hidden Menu)
        // Per Governance: Accessible from Registration Screen for environment setup
        if (state.showAdminSettings) {
            val settingsViewModel: SettingsViewModel = koinInject()
            AdminSettingsDialog(
                viewModel = settingsViewModel,
                onDismiss = { viewModel.onEvent(RegistrationEvent.HideAdminSettings) }
            )
        }
    }
}

