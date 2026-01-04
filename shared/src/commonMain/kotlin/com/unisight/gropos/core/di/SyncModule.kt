package com.unisight.gropos.core.di

import com.unisight.gropos.core.sync.DefaultHeartbeatService
import com.unisight.gropos.core.sync.DefaultSyncEngine
import com.unisight.gropos.core.sync.HeartbeatConfig
import com.unisight.gropos.core.sync.HeartbeatService
import com.unisight.gropos.core.sync.SyncEngine
import org.koin.dsl.module
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Koin module for data synchronization services.
 * 
 * **Per SYNC_MECHANISM.md:**
 * - HeartbeatService maintains periodic sync with backend
 * - SyncEngine handles download/upload of data
 * - Runs every 30 seconds for heartbeat, 5 minutes for full sync
 * 
 * **Per DEVICE_REGISTRATION.md:**
 * - Services should start after successful login
 * - Initial sync happens during device registration
 * 
 * **Components:**
 * - SyncEngine: Production implementation for API calls
 * - HeartbeatService: Background service for periodic sync
 * 
 * **Note:** HeartbeatService.start() must be called after login.
 * Call it from MainViewModel or navigation setup.
 */
val syncModule = module {
    
    /**
     * HeartbeatConfig with production settings.
     * 
     * Per SYNC_MECHANISM.md:
     * - Heartbeat every 30 seconds (connectivity check)
     * - Full sync every 5 minutes (data download)
     * - Max 3 retries before marking offline
     */
    single {
        HeartbeatConfig(
            heartbeatInterval = 30.seconds,
            syncInterval = 5.minutes,
            maxRetries = 3,
            requestTimeout = 10.seconds,
            syncOnStart = true  // Sync immediately when started
        )
    }
    
    /**
     * SyncEngine - Production implementation.
     * 
     * Uses ApiClient to communicate with backend:
     * - ping() calls heartbeat endpoint
     * - downloadUpdates() syncs products, categories, etc.
     */
    single<SyncEngine> { 
        DefaultSyncEngine(
            apiClient = get(),
            productSyncService = get()
        )
    }
    
    /**
     * HeartbeatService - Background sync manager.
     * 
     * Per SYNC_MECHANISM.md:
     * - Periodic heartbeat to confirm connectivity
     * - Background data sync at configurable intervals
     * - Processes offline queue when online
     * 
     * **IMPORTANT:** Call heartbeatService.start() after login!
     */
    single<HeartbeatService> {
        DefaultHeartbeatService(
            syncEngine = get(),
            offlineQueue = get(),
            config = get()
        )
    }
}

