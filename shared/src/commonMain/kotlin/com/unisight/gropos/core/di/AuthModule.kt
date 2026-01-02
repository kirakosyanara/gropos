package com.unisight.gropos.core.di

import com.unisight.gropos.features.auth.data.FakeAuthRepository
import com.unisight.gropos.features.auth.domain.repository.AuthRepository
import com.unisight.gropos.features.auth.domain.usecase.ValidateLoginUseCase
import com.unisight.gropos.features.auth.presentation.LockViewModel
import com.unisight.gropos.features.auth.presentation.LoginViewModel
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
    factory { ValidateLoginUseCase(get()) }
    
    // Presentation Layer
    // Note: ViewModels have optional CoroutineScope for testing, but we don't provide it
    // at runtime - they will use screenModelScope internally
    factory { LoginViewModel(get()) }
    
    // Lock Screen ViewModel
    // Per SCREEN_LAYOUTS.md: Displays when session is locked
    factory { LockViewModel() }
}
