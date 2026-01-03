package com.unisight.gropos.core.di

import org.koin.dsl.module

/**
 * Root application module that aggregates all feature modules.
 * 
 * This module should be used to initialize Koin in the app entry points.
 * Per project-structure.mdc: Core module only contains universal code.
 * 
 * **P0 FIX (QA Audit):**
 * - Added networkModule for ApiClient, TokenStorage, SecureStorage
 * - Remote repositories now receive proper dependencies
 */
val appModule = module {
    includes(networkModule)   // MUST be first - provides ApiClient, TokenStorage, SecureStorage
    includes(authModule)
    includes(checkoutModule)
    includes(paymentModule)
    includes(transactionModule)
    includes(returnsModule)
    includes(settingsModule)
    includes(deviceModule)
    includes(lotteryModule)  // Phase 5: Lottery Module
}

/**
 * Returns all Koin modules for the application.
 * Use this in platform-specific entry points (MainActivity, Main.kt).
 */
fun appModules() = listOf(appModule)

