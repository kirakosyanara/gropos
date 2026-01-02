package com.unisight.gropos.core.di

import com.unisight.gropos.core.security.ManagerApprovalService
import com.unisight.gropos.core.util.CurrencyFormatter
import com.unisight.gropos.core.util.UsdCurrencyFormatter
import com.unisight.gropos.features.checkout.data.CartRepositoryImpl
// FakeProductRepository replaced by CouchbaseProductRepository in platform-specific DatabaseModule
import com.unisight.gropos.features.checkout.data.FakeScannerRepository
import com.unisight.gropos.features.checkout.domain.repository.CartRepository
import com.unisight.gropos.features.checkout.domain.repository.ProductRepository
import com.unisight.gropos.features.checkout.domain.repository.ScannerRepository
import com.unisight.gropos.features.checkout.domain.service.CRVCalculator
import com.unisight.gropos.features.checkout.domain.service.DiscountCalculator
import com.unisight.gropos.features.checkout.domain.service.TaxCalculator
import com.unisight.gropos.features.checkout.domain.usecase.ScanItemUseCase
import com.unisight.gropos.features.checkout.presentation.CheckoutViewModel
import com.unisight.gropos.features.customer.presentation.CustomerDisplayViewModel
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin module for the Checkout feature.
 * 
 * Per project-structure.mdc: DI modules are grouped by feature.
 * 
 * Per ARCHITECTURE_BLUEPRINT.md (Section 6):
 * - CartRepository is a SINGLETON for multi-window state sharing
 * - Both CheckoutViewModel (Cashier) and CustomerDisplayViewModel (Customer)
 *   observe the same CartRepository instance
 * 
 * Provides:
 * - Core utilities (CurrencyFormatter)
 * - Domain layer (CartRepository, ScanItemUseCase)
 * - Data layer (Repositories - currently Fakes)
 * - Presentation layer (CheckoutViewModel, CustomerDisplayViewModel)
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
    
    // ProductRepository is now provided by platform-specific DatabaseModule
    // (CouchbaseProductRepository on Desktop/Android)
    
    /**
     * Cart repository - SINGLETON.
     * 
     * CRITICAL: This MUST be a singleton to ensure both Cashier and Customer
     * Display windows observe the same cart state.
     * 
     * Per ARCHITECTURE_BLUEPRINT.md: Single Source of Truth for cart state.
     */
    single<CartRepository> { CartRepositoryImpl() }
    
    // ========================================================================
    // Domain Layer - Calculation Services
    // Per SERVICES.md: All calculators are singletons with pure functions
    // ========================================================================
    
    /**
     * Tax calculator service.
     * Per TAX_CALCULATIONS.md: Handles SNAP exemption and CRV in taxable base.
     */
    single { TaxCalculator() }
    
    /**
     * CRV calculator service.
     * Per DEPOSITS_FEES.md: California Redemption Value calculations.
     */
    single { CRVCalculator() }
    
    /**
     * Discount calculator service.
     * Per SERVICES.md: Handles percentage/fixed discounts and floor price.
     */
    single { DiscountCalculator() }
    
    // ========================================================================
    // Security Services
    // Per ROLES_AND_PERMISSIONS.md: Manager approval for sensitive actions
    // ========================================================================
    
    /**
     * Manager approval service.
     * Handles permission checks and manager PIN validation.
     */
    single { ManagerApprovalService(get()) }
    
    // ========================================================================
    // Domain Layer - Use Cases
    // ========================================================================
    
    /**
     * Scan item use case - business logic for scanning products.
     * 
     * Now uses CartRepository for cart state management.
     * Factory scope - each screen gets its own instance, but they all
     * share the same CartRepository singleton.
     */
    factory { ScanItemUseCase(get(), get(), get()) }
    
    // ========================================================================
    // Presentation Layer
    // ========================================================================
    
    /**
     * Checkout ViewModel/ScreenModel for Cashier window.
     * 
     * Factory scope - each screen gets a fresh instance.
     * All instances share the same CartRepository singleton.
     * 
     * Dependencies:
     * - ScanItemUseCase: For processing scanned barcodes
     * - ScannerRepository: For hardware scanner flow
     * - CartRepository: Shared cart state (singleton)
     * - ProductRepository: For product lookup dialog
     * - CurrencyFormatter: For price formatting
     * - AuthRepository: For current user and permission checks
     * - ManagerApprovalService: For manager approval flow
     * - CashierSessionManager: For logout/session management
     * - TransactionRepository: For hold/recall operations
     */
    factory { CheckoutViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    
    /**
     * Customer Display ViewModel/ScreenModel.
     * 
     * Factory scope - each screen gets a fresh instance.
     * Observes the same CartRepository as CheckoutViewModel.
     * 
     * Per ARCHITECTURE_BLUEPRINT.md: Customer Display is read-only view
     * of the cart state managed by Cashier.
     */
    factory { CustomerDisplayViewModel(get(), get()) }
}
