package com.unisight.gropos.core.di

import com.unisight.gropos.core.sync.DefaultHeartbeatService
import com.unisight.gropos.core.sync.DefaultOfflineQueueService
import com.unisight.gropos.core.sync.DefaultSyncEngine
import com.unisight.gropos.core.sync.HeartbeatConfig
import com.unisight.gropos.core.sync.HeartbeatService
import com.unisight.gropos.core.sync.OfflineQueueConfig
import com.unisight.gropos.core.sync.OfflineQueueService
import com.unisight.gropos.core.sync.QueueItemSyncHandler
import com.unisight.gropos.core.sync.QueuePersistence
import com.unisight.gropos.core.sync.SyncEngine
import com.unisight.gropos.core.sync.TransactionSyncHandler
import com.unisight.gropos.features.transaction.data.api.DefaultTransactionApiService
import com.unisight.gropos.features.transaction.data.api.TransactionApiService
import kotlinx.serialization.json.Json
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
 * **Per TRANSACTION_API_SUBMISSION_IMPLEMENTATION_PLAN.md:**
 * - TransactionApiService handles POST to /transactions/create-transaction
 * - TransactionSyncHandler processes queued transactions
 * - OfflineQueueService provides reliable delivery
 * 
 * **Per DEVICE_REGISTRATION.md:**
 * - Services should start after successful login
 * - Initial sync happens during device registration
 * 
 * **Note:** HeartbeatService.start() must be called after login.
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
     * OfflineQueueConfig with production settings.
     * 
     * Per TRANSACTION_API_SUBMISSION_IMPLEMENTATION_PLAN.md:
     * - Max 5 retries before abandoning
     * - Exponential backoff for retries
     */
    single {
        OfflineQueueConfig(
            maxRetries = 5
        )
    }
    
    /**
     * TransactionApiService - HTTP layer for transaction submission.
     * 
     * Per TRANSACTION_API_SUBMISSION_IMPLEMENTATION_PLAN.md:
     * - POST /api/v1/transactions/create-transaction
     * - Handles success/error responses
     */
    single<TransactionApiService> {
        DefaultTransactionApiService(
            apiClient = get()
        )
    }
    
    /**
     * QueueItemSyncHandler - Processes queued items for sync.
     * 
     * Per TRANSACTION_API_SUBMISSION_IMPLEMENTATION_PLAN.md:
     * - Deserializes transaction payload
     * - Calls TransactionApiService
     * - Returns Success/Retry/Abandon
     */
    single<QueueItemSyncHandler> {
        TransactionSyncHandler(
            transactionApiService = get(),
            transactionRepository = get(),
            json = get()
        )
    }
    
    // NOTE: QueuePersistence is provided by DatabaseModule (platform-specific)
    // Desktop/Android use CouchbaseQueuePersistence for crash recovery
    
    /**
     * OfflineQueueService - Reliable delivery for transactions.
     * 
     * Per TRANSACTION_API_SUBMISSION_IMPLEMENTATION_PLAN.md:
     * - Enqueues items for background sync
     * - Processes queue with retry logic
     */
    single<OfflineQueueService> {
        DefaultOfflineQueueService(
            syncHandler = get(),
            persistence = get(),
            config = get()
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
    
    /**
     * JSON serializer for transaction payloads.
     * 
     * Configured for API compatibility:
     * - ignoreUnknownKeys for forward compatibility
     * - isLenient for minor format variations
     */
    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
            prettyPrint = false
        }
    }
}

