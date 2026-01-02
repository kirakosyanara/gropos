package com.unisight.gropos.core.di

import org.koin.dsl.module

/**
 * Root application module that aggregates all feature modules.
 * 
 * This module should be used to initialize Koin in the app entry points.
 * Per project-structure.mdc: Core module only contains universal code.
 */
val appModule = module {
    includes(authModule)
    includes(checkoutModule)
    includes(paymentModule)
    includes(transactionModule)
    includes(returnsModule)
    
    // Future feature modules will be added here:
    // includes(reportsModule)
}

/**
 * Returns all Koin modules for the application.
 * Use this in platform-specific entry points (MainActivity, Main.kt).
 */
fun appModules() = listOf(appModule)

