package com.unisight.gropos.core.di

import com.unisight.gropos.features.auth.data.FakeAuthRepository
import com.unisight.gropos.features.auth.domain.repository.AuthRepository
import com.unisight.gropos.features.auth.domain.usecase.ValidateLoginUseCase
import com.unisight.gropos.features.auth.presentation.LockViewModel
import com.unisight.gropos.features.auth.presentation.LoginViewModel
import com.unisight.gropos.features.cashier.data.FakeEmployeeRepository
import com.unisight.gropos.features.cashier.data.FakeTillRepository
import com.unisight.gropos.features.cashier.domain.repository.EmployeeRepository
import com.unisight.gropos.features.cashier.domain.repository.TillRepository
import com.unisight.gropos.features.cashier.domain.service.CashierSessionManager
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Koin module for Authentication & Cashier features.
 * 
 * Per CASHIER_OPERATIONS.md:
 * - Provides employee list for login screen
 * - Provides till management for session assignment
 * 
 * Provides:
 * - AuthRepository (FakeAuthRepository for now)
 * - EmployeeRepository (FakeEmployeeRepository for now)
 * - TillRepository (FakeTillRepository for now)
 * - ValidateLoginUseCase
 * - LoginViewModel (with state machine flow)
 * - LockViewModel
 */
val authModule = module {
    
    // Data Layer - Auth
    // TODO: Replace FakeAuthRepository with CouchbaseLiteAuthRepository
    singleOf(::FakeAuthRepository) bind AuthRepository::class
    
    // Data Layer - Employee
    // TODO: Replace with API-backed implementation
    singleOf(::FakeEmployeeRepository) bind EmployeeRepository::class
    
    // Data Layer - Till
    // TODO: Replace with Couchbase-backed implementation
    singleOf(::FakeTillRepository) bind TillRepository::class
    
    // Domain Layer - Session Manager
    // Per CASHIER_OPERATIONS.md: Tracks active session, handles logout
    single { CashierSessionManager(get()) }
    
    // Domain Layer
    factory { ValidateLoginUseCase(get()) }
    
    // Presentation Layer
    // LoginViewModel now uses EmployeeRepository and TillRepository
    // per CASHIER_OPERATIONS.md state machine flow
    factory { LoginViewModel(get(), get()) }
    
    // Lock Screen ViewModel
    // Per SCREEN_LAYOUTS.md: Displays when session is locked
    factory { LockViewModel() }
}
