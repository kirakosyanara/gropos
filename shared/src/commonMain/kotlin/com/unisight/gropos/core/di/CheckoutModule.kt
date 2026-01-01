package com.unisight.gropos.core.di

import com.unisight.gropos.core.util.CurrencyFormatter
import com.unisight.gropos.core.util.UsdCurrencyFormatter
import com.unisight.gropos.features.checkout.data.FakeProductRepository
import com.unisight.gropos.features.checkout.data.FakeScannerRepository
import com.unisight.gropos.features.checkout.domain.repository.ProductRepository
import com.unisight.gropos.features.checkout.domain.repository.ScannerRepository
import com.unisight.gropos.features.checkout.domain.usecase.ScanItemUseCase
import com.unisight.gropos.features.checkout.presentation.CheckoutViewModel
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin module for the Checkout feature.
 * 
 * Per project-structure.mdc: DI modules are grouped by feature.
 * 
 * Provides:
 * - Core utilities (CurrencyFormatter)
 * - Domain layer (ScanItemUseCase)
 * - Data layer (Repositories - currently Fakes)
 * - Presentation layer (CheckoutViewModel)
 */
val checkoutModule: Module = module {
    
    // ========================================================================
    // Core Utilities
    // ========================================================================
    
    /**
     * Currency formatter for USD.
     * TODO: Replace with expect/actual for locale-aware formatting.
     */
    single<CurrencyFormatter> { UsdCurrencyFormatter() }
    
    // ========================================================================
    // Data Layer (Fakes for Walking Skeleton)
    // ========================================================================
    
    /**
     * Fake scanner repository for development.
     * TODO: Replace with actual hardware integration (expect/actual pattern).
     */
    single<ScannerRepository> { FakeScannerRepository() }
    
    /**
     * Fake product repository for development.
     * TODO: Replace with CouchbaseLiteProductRepository.
     */
    single<ProductRepository> { FakeProductRepository() }
    
    // ========================================================================
    // Domain Layer
    // ========================================================================
    
    /**
     * Scan item use case - business logic for scanning products.
     */
    factory { ScanItemUseCase(get(), get()) }
    
    // ========================================================================
    // Presentation Layer
    // ========================================================================
    
    /**
     * Checkout ViewModel/ScreenModel.
     * Factory scope so each screen gets a fresh instance.
     */
    factory { CheckoutViewModel(get(), get(), get()) }
}

