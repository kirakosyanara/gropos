package com.unisight.gropos.core.di

import com.unisight.gropos.features.transaction.presentation.TransactionHistoryViewModel
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin module for the Transaction feature.
 * 
 * Per FUNCTIONS_MENU.md:
 * - Transaction History (Recall) screen for viewing past transactions
 * - Return/Invoice functionality
 * 
 * Note: TransactionRepository is provided by platform-specific DatabaseModule.
 */
val transactionModule: Module = module {

    /**
     * Transaction History ViewModel/ScreenModel.
     * 
     * Factory scope - each screen gets a fresh instance.
     * 
     * Dependencies:
     * - TransactionRepository: For fetching transaction history (from DatabaseModule)
     * - CurrencyFormatter: For price formatting
     */
    factory { TransactionHistoryViewModel(get(), get()) }
}

