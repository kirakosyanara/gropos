package com.unisight.gropos.core.di

import com.unisight.gropos.core.sync.InitialSyncService
import com.unisight.gropos.core.sync.ProductSyncService
import com.unisight.gropos.features.device.data.DeviceApi
import com.unisight.gropos.features.device.data.RemoteDeviceApi
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
 * **Per LOCK_SCREEN_AND_CASHIER_LOGIN.md:**
 * - DeviceApi: GET /api/v1/devices/current for station claiming
 * 
 * **DATA SYNC IMPLEMENTATION:**
 * - ProductSyncService: Syncs products with pagination
 * - InitialSyncService: Orchestrates employee and product sync
 * - RegistrationViewModel triggers sync after successful registration
 * 
 * Per project-structure.mdc:
 * - Interface in Domain, Implementation in Data
 * - ViewModels are factory-scoped (fresh instance per screen)
 */
val deviceModule = module {
    // Device API - for station status and claiming
    // Per LOCK_SCREEN_AND_CASHIER_LOGIN.md: GET /api/v1/devices/current
    single<DeviceApi> { RemoteDeviceApi(get()) }
    
    // Repository - singleton since it holds device state
    // Per API_INTEGRATION.md: Uses ApiClient for /device-registration endpoints
    // Per zero-trust-security.mdc: Uses SecureStorage for API key persistence
    single<DeviceRepository> { RemoteDeviceRepository(get(), get()) }
    
    // Product Sync Service - handles paginated product sync
    // Per SYNC_MECHANISM.md: GET /product?offset=&limit=100
    single { ProductSyncService(get(), get()) }
    
    // Initial Sync Service - orchestrates all data sync
    // Per SYNC_MECHANISM.md: Pull-based sync of employees, products, etc.
    single { InitialSyncService(get(), get()) }
    
    // ViewModel - factory for fresh state per screen
    // Now includes InitialSyncService for post-registration sync
    factory { RegistrationViewModel(deviceRepository = get(), initialSyncService = get()) }
}

