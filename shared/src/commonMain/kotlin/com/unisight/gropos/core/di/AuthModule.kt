package com.unisight.gropos.core.di

import com.unisight.gropos.features.auth.data.FakeAuthRepository
import com.unisight.gropos.features.auth.data.SimulatedNfcScanner
import com.unisight.gropos.features.auth.domain.hardware.NfcScanner
import com.unisight.gropos.features.auth.domain.repository.AuthRepository
import com.unisight.gropos.features.auth.domain.usecase.ValidateLoginUseCase
import com.unisight.gropos.features.auth.presentation.LockViewModel
import com.unisight.gropos.features.auth.presentation.LoginViewModel
import com.unisight.gropos.features.cashier.data.RemoteEmployeeRepository
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
 * Per LOCK_SCREEN_AND_CASHIER_LOGIN.md:
 * - Provides employee list for login screen
 * - Provides till management for session assignment
 * - Provides station claiming logic (L1)
 * 
 * **DATA SYNC IMPLEMENTATION:**
 * - EmployeeRepository now uses RemoteEmployeeRepository (API-backed)
 * - TillRepository uses RemoteTillRepository (API-backed)
 * 
 * Provides:
 * - AuthRepository (FakeAuthRepository for now)
 * - EmployeeRepository (RemoteEmployeeRepository - fetches from /api/Employee/GetCashierEmployees)
 * - TillRepository (RemoteTillRepository - fetches from /api/account/GetTillAccountList)
 * - ValidateLoginUseCase
 * - LoginViewModel (with station claiming and state machine flow)
 * - LockViewModel (with API PIN verification)
 */
val authModule = module {
    
    // Data Layer - Auth
    // TODO: Replace FakeAuthRepository with CouchbaseLiteAuthRepository
    singleOf(::FakeAuthRepository) bind AuthRepository::class
    
    // Data Layer - Employee
    // Per LOCK_SCREEN_AND_CASHIER_LOGIN.md: GET /api/Employee/GetCashierEmployees
    single<EmployeeRepository> { RemoteEmployeeRepository(get()) }
    
    // Data Layer - Till
    // Per LOCK_SCREEN_AND_CASHIER_LOGIN.md: GET /api/account/GetTillAccountList
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
    
    // Presentation Layer - LoginViewModel
    // Per LOCK_SCREEN_AND_CASHIER_LOGIN.md:
    // - EmployeeRepository: GET /api/Employee/GetCashierEmployees
    // - TillRepository: GET /api/account/GetTillAccountList
    // - NfcScanner: Badge authentication
    // - DeviceApi: GET /api/v1/devices/current for station claiming (L1)
    factory { LoginViewModel(get(), get(), get(), get()) }
    
    // Presentation Layer - LockViewModel
    // Per LOCK_SCREEN_AND_CASHIER_LOGIN.md:
    // TODO (Phase 6): Inject ApiAuthService for verifyPassword(), lockDevice()
    factory { LockViewModel() }
}
