package com.unisight.gropos.core.di

import com.unisight.gropos.features.auth.data.FakeAuthRepository
import com.unisight.gropos.features.auth.domain.repository.AuthRepository
import com.unisight.gropos.features.auth.domain.usecase.ValidateLoginUseCase
import com.unisight.gropos.features.auth.presentation.LoginViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Koin module for Authentication feature.
 * 
 * Provides:
 * - AuthRepository (FakeAuthRepository for now, will be replaced with real impl)
 * - ValidateLoginUseCase
 * - LoginViewModel
 * 
 * Per project-structure.mdc: DI modules live in core/di/
 */
val authModule = module {
    
    // Data Layer
    // TODO: Replace FakeAuthRepository with CouchbaseLiteAuthRepository
    singleOf(::FakeAuthRepository) bind AuthRepository::class
    
    // Domain Layer
    factoryOf(::ValidateLoginUseCase)
    
    // Presentation Layer
    factoryOf(::LoginViewModel)
}

