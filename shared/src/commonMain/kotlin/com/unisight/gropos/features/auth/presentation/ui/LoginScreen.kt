package com.unisight.gropos.features.auth.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.unisight.gropos.features.auth.presentation.LoginUiState
import com.unisight.gropos.features.auth.presentation.LoginViewModel
import com.unisight.gropos.features.checkout.presentation.ui.CheckoutScreen

/**
 * Login screen using Voyager navigation.
 * 
 * This is the entry point for the auth flow.
 * Per project-structure.mdc: Screen classes are in presentation/ui/.
 * 
 * Navigation:
 * - On successful login, navigates to CheckoutScreen
 * - Uses LaunchedEffect to ensure navigation only happens once
 */
class LoginScreen : Screen {
    
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinScreenModel<LoginViewModel>()
        val state by viewModel.state.collectAsState()
        
        // Navigate to CheckoutScreen on successful login
        // Using LaunchedEffect with state as key ensures this only fires once per Success state
        LaunchedEffect(state) {
            if (state is LoginUiState.Success) {
                // Replace entire navigation stack to prevent back navigation to login
                navigator.replaceAll(CheckoutScreen())
            }
        }
        
        LoginContent(
            state = state,
            onLoginClick = viewModel::onLoginClick,
            onErrorDismissed = viewModel::onErrorDismissed
        )
    }
}
