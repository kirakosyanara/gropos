package com.unisight.gropos.core.di

import com.unisight.gropos.features.device.data.FakeDeviceRepository
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
 * Per project-structure.mdc:
 * - Interface in Domain, Implementation in Data
 * - ViewModels are factory-scoped (fresh instance per screen)
 */
val deviceModule = module {
    // Repository - singleton since it holds device state
    // Using FakeDeviceRepository for P2 (in-memory)
    // TODO: Replace with persistent implementation in P3
    single<DeviceRepository> { FakeDeviceRepository() }
    
    // Also expose as FakeDeviceRepository for dev tools (simulate activation)
    single { get<DeviceRepository>() as FakeDeviceRepository }
    
    // ViewModel - factory for fresh state per screen
    factory { RegistrationViewModel(deviceRepository = get()) }
}

