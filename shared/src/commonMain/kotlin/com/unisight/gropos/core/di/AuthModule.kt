package com.unisight.gropos.core.di

import com.unisight.gropos.features.auth.data.FakeAuthRepository
import com.unisight.gropos.features.auth.data.SimulatedNfcScanner
import com.unisight.gropos.features.auth.domain.hardware.NfcScanner
import com.unisight.gropos.features.auth.domain.repository.AuthRepository
import com.unisight.gropos.features.auth.domain.usecase.ValidateLoginUseCase
import com.unisight.gropos.features.auth.presentation.LockViewModel
import com.unisight.gropos.features.auth.presentation.LoginViewModel
import com.unisight.gropos.features.cashier.data.FakeEmployeeRepository
import com.unisight.gropos.features.cashier.data.RemoteTillRepository
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
 * **P0 FIX (QA Audit):**
 * - TillRepository now uses RemoteTillRepository (API-backed)
 * - EmployeeRepository still uses Fake (API endpoint not ready)
 * 
 * Provides:
 * - AuthRepository (FakeAuthRepository for now)
 * - EmployeeRepository (FakeEmployeeRepository for now)
 * - TillRepository (RemoteTillRepository - production-ready)
 * - ValidateLoginUseCase
 * - LoginViewModel (with state machine flow)
 * - LockViewModel
 */
val authModule = module {
    
    // Data Layer - Auth
    // TODO: Replace FakeAuthRepository with CouchbaseLiteAuthRepository
    singleOf(::FakeAuthRepository) bind AuthRepository::class
    
    // Data Layer - Employee
    // TODO: Replace with API-backed implementation when endpoint is ready
    singleOf(::FakeEmployeeRepository) bind EmployeeRepository::class
    
    // Data Layer - Till
    // P0 FIX: Now uses RemoteTillRepository with real API calls
    // Per API_INTEGRATION.md: Uses ApiClient for /till endpoints
    single<TillRepository> { RemoteTillRepository(get()) }
    
    // Hardware Layer - NFC Scanner
    // Per ANDROID_HARDWARE_GUIDE.md: Hardware abstraction for badge authentication
    // TODO: Replace SimulatedNfcScanner with platform-specific implementation
    //       (SunmiNfcScanner, AndroidNfcScanner, DesktopNfcScanner)
    singleOf(::SimulatedNfcScanner) bind NfcScanner::class
    
    // Domain Layer - Session Manager
    // Per CASHIER_OPERATIONS.md: Tracks active session, handles logout
    single { CashierSessionManager(get()) }
    
    // Domain Layer
    factory { ValidateLoginUseCase(get()) }
    
    // Presentation Layer
    // LoginViewModel now uses EmployeeRepository, TillRepository, and NfcScanner
    // per CASHIER_OPERATIONS.md state machine flow and ANDROID_HARDWARE_GUIDE.md NFC support
    factory { LoginViewModel(get(), get(), get()) }
    
    // Lock Screen ViewModel
    // Per SCREEN_LAYOUTS.md: Displays when session is locked
    factory { LockViewModel() }
}
