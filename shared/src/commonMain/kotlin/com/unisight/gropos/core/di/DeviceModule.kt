package com.unisight.gropos.core.di

import com.unisight.gropos.core.sync.InitialSyncService
import com.unisight.gropos.features.device.data.RemoteDeviceRepository
import com.unisight.gropos.features.device.domain.repository.DeviceRepository
import com.unisight.gropos.features.device.presentation.RegistrationViewModel
import org.koin.dsl.module

/**
 * Koin DI module for the Device Registration feature.
 * 
 * Per DEVICE_REGISTRATION.md:
 * - DeviceRepository: Manages device registration state and persistence
 * - RegistrationViewModel: Handles registration UI state
 * - InitialSyncService: Syncs data after registration
 * 
 * **DATA SYNC IMPLEMENTATION:**
 * - InitialSyncService syncs employees and data after registration
 * - RegistrationViewModel triggers sync after successful registration
 * 
 * Per project-structure.mdc:
 * - Interface in Domain, Implementation in Data
 * - ViewModels are factory-scoped (fresh instance per screen)
 */
val deviceModule = module {
    // Repository - singleton since it holds device state
    // Per API_INTEGRATION.md: Uses ApiClient for /device-registration endpoints
    // Per zero-trust-security.mdc: Uses SecureStorage for API key persistence
    single<DeviceRepository> { RemoteDeviceRepository(get(), get()) }
    
    // Initial Sync Service - handles data sync after registration
    // Per SYNC_MECHANISM.md: Pull-based sync of employees, products, etc.
    single { InitialSyncService(get()) }
    
    // ViewModel - factory for fresh state per screen
    // Now includes InitialSyncService for post-registration sync
    factory { RegistrationViewModel(deviceRepository = get(), initialSyncService = get()) }
}

