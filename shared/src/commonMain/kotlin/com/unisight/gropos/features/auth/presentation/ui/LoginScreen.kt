package com.unisight.gropos.features.auth.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.unisight.gropos.features.auth.presentation.LoginViewModel

/**
 * Login screen using Voyager navigation.
 * 
 * This is the entry point for the auth flow.
 * Per project-structure.mdc: Screen classes are in presentation/ui/.
 */
class LoginScreen : Screen {
    
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<LoginViewModel>()
        val state by viewModel.state.collectAsState()
        
        LoginContent(
            state = state,
            onLoginClick = viewModel::onLoginClick,
            onErrorDismissed = viewModel::onErrorDismissed
        )
    }
}

