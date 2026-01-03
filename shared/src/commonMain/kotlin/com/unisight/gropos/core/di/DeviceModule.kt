package com.unisight.gropos.core.di

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
 * 
 * **P0 FIX (QA Audit):**
 * - Now uses RemoteDeviceRepository with real API calls and SecureStorage
 * - Device credentials persist across app restarts
 * 
 * Per project-structure.mdc:
 * - Interface in Domain, Implementation in Data
 * - ViewModels are factory-scoped (fresh instance per screen)
 */
val deviceModule = module {
    // Repository - singleton since it holds device state
    // P0 FIX: Now uses RemoteDeviceRepository with SecureStorage persistence
    // Per API_INTEGRATION.md: Uses ApiClient for /device-registration endpoints
    // Per zero-trust-security.mdc: Uses SecureStorage for API key persistence
    single<DeviceRepository> { RemoteDeviceRepository(get(), get()) }
    
    // ViewModel - factory for fresh state per screen
    factory { RegistrationViewModel(deviceRepository = get()) }
}

