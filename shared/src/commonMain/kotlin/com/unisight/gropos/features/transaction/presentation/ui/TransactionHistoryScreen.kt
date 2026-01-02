package com.unisight.gropos.features.transaction.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.unisight.gropos.features.transaction.presentation.TransactionHistoryViewModel

/**
 * Voyager Screen for Transaction History (Recall).
 * 
 * Per FUNCTIONS_MENU.md - Return/Invoice:
 * - Opens the order report screen for viewing transaction history
 * - Supports transaction search and selection
 * 
 * Per SCREEN_LAYOUTS.md - Order Report Screen:
 * - Left pane: Transaction list
 * - Right pane: Transaction detail
 */
class TransactionHistoryScreen : Screen {
    
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinScreenModel<TransactionHistoryViewModel>()
        val state by viewModel.state.collectAsState()
        
        TransactionHistoryContent(
            state = state,
            onTransactionSelect = { viewModel.onTransactionSelect(it) },
            onRefresh = { viewModel.refresh() },
            onBack = { navigator.pop() },
            onDismissError = { viewModel.dismissError() }
        )
    }
}

