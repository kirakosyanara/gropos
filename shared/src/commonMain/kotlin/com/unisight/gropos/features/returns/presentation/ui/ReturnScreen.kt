package com.unisight.gropos.features.returns.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.unisight.gropos.features.returns.presentation.ReturnViewModel

/**
 * Return Item Screen using Voyager navigation.
 * 
 * Per RETURNS.md & SCREEN_LAYOUTS.md:
 * - Displays returnable items from a transaction
 * - Allows selecting items to return
 * - Shows refund totals
 * - Requires manager approval to process
 * 
 * @param transactionId The transaction to return items from
 */
class ReturnScreen(
    private val transactionId: Long
) : Screen {
    
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinScreenModel<ReturnViewModel>()
        val state by viewModel.state.collectAsState()
        
        // Load transaction on first composition
        LaunchedEffect(transactionId) {
            viewModel.loadTransaction(transactionId)
        }
        
        // Handle completion - navigate back
        LaunchedEffect(state.isComplete) {
            if (state.isComplete) {
                navigator.pop()
            }
        }
        
        ReturnContent(
            state = state,
            onAddToReturnClick = viewModel::onAddToReturnClick,
            onRemoveFromReturn = viewModel::onRemoveFromReturn,
            onQuantityInputChange = viewModel::onQuantityInputChange,
            onQuantityConfirm = viewModel::onQuantityConfirm,
            onQuantityDialogDismiss = viewModel::onQuantityDialogDismiss,
            onProcessReturnClick = viewModel::onProcessReturnClick,
            onManagerApprovalGranted = viewModel::onManagerApprovalGranted,
            onManagerApprovalDenied = viewModel::onManagerApprovalDenied,
            onManagerApprovalDismiss = viewModel::onManagerApprovalDismiss,
            onErrorDismissed = viewModel::onErrorDismissed,
            onBackClick = { navigator.pop() }
        )
    }
}

