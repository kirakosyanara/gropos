package com.unisight.gropos.core.di

import com.unisight.gropos.features.payment.data.SimulatedPaymentTerminal
import com.unisight.gropos.features.payment.domain.terminal.PaymentTerminal
import com.unisight.gropos.features.payment.presentation.PaymentViewModel
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin module for the Payment feature.
 * 
 * Per ARCHITECTURE_BLUEPRINT.md:
 * - PaymentViewModel observes the same CartRepository singleton
 * - Ensures cart totals are synchronized with Checkout screen
 * 
 * Per DATABASE_SCHEMA.md:
 * - TransactionRepository for persisting completed transactions
 * 
 * Per DESKTOP_HARDWARE.md:
 * - PaymentTerminal abstraction for hardware integration
 * 
 * Per END_OF_TRANSACTION_API_SUBMISSION.md:
 * - TransactionApiService for IMMEDIATE transaction submission
 * - OfflineQueueService only for retry when immediate POST fails
 */
val paymentModule: Module = module {

    /**
     * Payment Terminal hardware abstraction.
     * 
     * Singleton scope - one terminal instance shared across the app.
     * 
     * Per DESKTOP_HARDWARE.md: This binds to SimulatedPaymentTerminal
     * for development. In production, this would bind to a real terminal
     * implementation (e.g., PaxPaymentTerminal, SunmiPaymentTerminal).
     * 
     * Why Singleton:
     * - Physical terminal hardware is a single resource
     * - Prevents concurrent access issues
     */
    single<PaymentTerminal> { SimulatedPaymentTerminal() }

    /**
     * Payment ViewModel/ScreenModel.
     * 
     * Factory scope - each screen gets a fresh instance.
     * Observes the shared CartRepository singleton for totals.
     * 
     * Dependencies:
     * - CartRepository: Shared cart state (singleton)
     * - CurrencyFormatter: For price formatting
     * - TransactionRepository: For persisting completed transactions
     * - PaymentTerminal: Hardware abstraction for card payments
     * - TransactionApiService: For IMMEDIATE transaction submission to backend
     * - OfflineQueueService: For retry when immediate POST fails (optional)
     * - Json: For payload serialization
     */
    factory { 
        PaymentViewModel(
            cartRepository = get(),
            currencyFormatter = get(),
            transactionRepository = get(),
            paymentTerminal = get(),
            transactionApiService = get(),  // IMMEDIATE submission to backend
            offlineQueue = getOrNull(),     // Only for retry on failure
            json = get()
        ) 
    }
}

