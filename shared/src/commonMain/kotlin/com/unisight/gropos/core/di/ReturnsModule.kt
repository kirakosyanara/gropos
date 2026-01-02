package com.unisight.gropos.core.di

import com.unisight.gropos.features.returns.domain.service.ReturnService
import com.unisight.gropos.features.returns.presentation.ReturnViewModel
import org.koin.dsl.module

/**
 * Koin DI module for the Returns feature.
 * 
 * Per project-structure.mdc:
 * - Domain services as singletons
 * - ViewModels as factories
 */
val returnsModule = module {
    
    // Domain Layer - Services
    single { ReturnService(get()) }
    
    // Presentation Layer - ViewModels
    factory { ReturnViewModel(get(), get(), get()) }
}

