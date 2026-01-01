package com.unisight.gropos.core.di

import com.unisight.gropos.features.payment.presentation.PaymentViewModel
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin module for the Payment feature.
 * 
 * Per ARCHITECTURE_BLUEPRINT.md:
 * - PaymentViewModel observes the same CartRepository singleton
 * - Ensures cart totals are synchronized with Checkout screen
 */
val paymentModule: Module = module {

    /**
     * Payment ViewModel/ScreenModel.
     * 
     * Factory scope - each screen gets a fresh instance.
     * Observes the shared CartRepository singleton for totals.
     * 
     * Dependencies:
     * - CartRepository: Shared cart state (singleton)
     * - CurrencyFormatter: For price formatting
     */
    factory { PaymentViewModel(get(), get()) }
}

