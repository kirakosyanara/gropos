package com.unisight.gropos.core.sync

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Service for maintaining heartbeat and background data synchronization.
 * 
 * Per REMEDIATION_CHECKLIST: Heartbeat/Sync Service - Implement background data synchronization.
 * Per DEVICE_REGISTRATION.md: Devices should maintain periodic heartbeat with server.
 * 
 * Responsibilities:
 * - Periodic server ping to confirm connectivity
 * - Background data sync (products, prices, promotions)
 * - Pending transaction upload when online
 * - Status reporting for UI indicators
 */
interface HeartbeatService {
    
    /**
     * Current heartbeat status.
     */
    val status: StateFlow<HeartbeatStatus>
    
    /**
     * Whether the service is currently running.
     */
    val isRunning: StateFlow<Boolean>
    
    /**
     * Start the heartbeat service.
     */
    suspend fun start()
    
    /**
     * Stop the heartbeat service.
     */
    suspend fun stop()
    
    /**
     * Force an immediate sync cycle.
     */
    suspend fun syncNow()
    
    /**
     * Force a heartbeat ping.
     */
    suspend fun pingNow(): Boolean
    
    /**
     * Get pending items count.
     */
    suspend fun getPendingCount(): Int
}

/**
 * Current status of the heartbeat service.
 */
data class HeartbeatStatus(
    /** Whether we can reach the server */
    val isOnline: Boolean = true,
    
    /** Last successful heartbeat time */
    val lastHeartbeat: Instant? = null,
    
    /** Last successful sync time */
    val lastSync: Instant? = null,
    
    /** Number of pending items to sync */
    val pendingItems: Int = 0,
    
    /** Whether sync is in progress */
    val isSyncing: Boolean = false,
    
    /** Last error message if any */
    val lastError: String? = null,
    
    /** Consecutive failed heartbeat attempts */
    val failedAttempts: Int = 0
) {
    /** Status summary for display */
    val displayStatus: String
        get() = when {
            isSyncing -> "Syncing..."
            !isOnline -> "Offline"
            pendingItems > 0 -> "$pendingItems pending"
            else -> "Connected"
        }
    
    /** Whether status indicates a problem */
    val hasWarning: Boolean
        get() = !isOnline || pendingItems > 10 || failedAttempts > 0
}

/**
 * Configuration for heartbeat service.
 */
data class HeartbeatConfig(
    /** How often to send heartbeat pings */
    val heartbeatInterval: Duration = 30.seconds,
    
    /** How often to run background sync */
    val syncInterval: Duration = 5.minutes,
    
    /** Max retries before marking offline */
    val maxRetries: Int = 3,
    
    /** Timeout for heartbeat requests */
    val requestTimeout: Duration = 10.seconds,
    
    /** Whether to sync immediately on start */
    val syncOnStart: Boolean = true
)

/**
 * Default implementation of HeartbeatService.
 */
class DefaultHeartbeatService(
    private val syncEngine: SyncEngine,
    private val offlineQueue: OfflineQueueService,
    private val config: HeartbeatConfig = HeartbeatConfig(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : HeartbeatService {
    
    private val scope = CoroutineScope(dispatcher + SupervisorJob())
    
    private val _status = MutableStateFlow(HeartbeatStatus())
    override val status: StateFlow<HeartbeatStatus> = _status.asStateFlow()
    
    private val _isRunning = MutableStateFlow(false)
    override val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    
    private var heartbeatJob: Job? = null
    private var syncJob: Job? = null
    
    override suspend fun start() {
        if (_isRunning.value) return
        
        _isRunning.value = true
        println("HeartbeatService: Starting...")
        
        // Initial sync if configured
        if (config.syncOnStart) {
            syncNow()
        }
        
        // Start heartbeat loop
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(config.heartbeatInterval)
                try {
                    val success = pingNow()
                    if (success) {
                        _status.value = _status.value.copy(
                            isOnline = true,
                            failedAttempts = 0,
                            lastHeartbeat = Clock.System.now()
                        )
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    handleHeartbeatFailure(e)
                }
            }
        }
        
        // Start sync loop
        syncJob = scope.launch {
            while (isActive) {
                delay(config.syncInterval)
                try {
                    syncNow()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    println("HeartbeatService: Sync failed - ${e.message}")
                }
            }
        }
    }
    
    override suspend fun stop() {
        println("HeartbeatService: Stopping...")
        heartbeatJob?.cancel()
        syncJob?.cancel()
        _isRunning.value = false
    }
    
    override suspend fun syncNow() {
        if (_status.value.isSyncing) {
            println("HeartbeatService: Sync already in progress, skipping")
            return
        }
        
        _status.value = _status.value.copy(isSyncing = true)
        
        try {
            // 1. Upload pending transactions
            val uploadResult = offlineQueue.processQueue()
            
            // 2. Download updated data
            val downloadResult = syncEngine.downloadUpdates()
            
            // 3. Update status
            val pendingCount = offlineQueue.getPendingCount()
            
            _status.value = _status.value.copy(
                isSyncing = false,
                lastSync = Clock.System.now(),
                pendingItems = pendingCount,
                lastError = null
            )
            
            println("HeartbeatService: Sync complete. Pending: $pendingCount")
            
        } catch (e: Exception) {
            _status.value = _status.value.copy(
                isSyncing = false,
                lastError = e.message
            )
            println("HeartbeatService: Sync failed - ${e.message}")
        }
    }
    
    override suspend fun pingNow(): Boolean {
        return try {
            val success = syncEngine.ping()
            if (success) {
                _status.value = _status.value.copy(
                    isOnline = true,
                    lastHeartbeat = Clock.System.now(),
                    failedAttempts = 0
                )
            }
            success
        } catch (e: Exception) {
            handleHeartbeatFailure(e)
            false
        }
    }
    
    override suspend fun getPendingCount(): Int {
        return offlineQueue.getPendingCount()
    }
    
    private fun handleHeartbeatFailure(e: Exception) {
        val newFailedCount = _status.value.failedAttempts + 1
        val isOffline = newFailedCount >= config.maxRetries
        
        _status.value = _status.value.copy(
            isOnline = !isOffline,
            failedAttempts = newFailedCount,
            lastError = e.message
        )
        
        if (isOffline) {
            println("HeartbeatService: Server unreachable after $newFailedCount attempts")
        }
    }
}

/**
 * Interface for the sync engine that handles data upload/download.
 */
interface SyncEngine {
    /** Ping the server to check connectivity */
    suspend fun ping(): Boolean
    
    /** Download updated data from server */
    suspend fun downloadUpdates(): SyncResult
    
    /** Get last download timestamp */
    fun getLastDownloadTime(): Instant?
}

/**
 * Result of a sync operation.
 */
data class SyncResult(
    val success: Boolean,
    val itemsSynced: Int = 0,
    val errors: List<String> = emptyList()
)

/**
 * Interface for offline queue service.
 */
interface OfflineQueueService {
    /** Process pending items in the queue */
    suspend fun processQueue(): Int
    
    /** Get count of pending items */
    suspend fun getPendingCount(): Int
    
    /** Add item to queue */
    suspend fun enqueue(item: QueuedItem)
}

/**
 * Item in the offline queue.
 */
data class QueuedItem(
    val id: Long,
    val type: QueueItemType,
    val payload: String,
    val createdAt: Instant,
    val attempts: Int = 0,
    val lastAttempt: Instant? = null
)

enum class QueueItemType {
    TRANSACTION,
    RETURN,
    ADJUSTMENT,
    CLOCK_EVENT,
    APPROVAL_AUDIT
}

/**
 * Simulated implementation for testing/development.
 */
class SimulatedHeartbeatService : HeartbeatService {
    
    private val _status = MutableStateFlow(HeartbeatStatus(
        isOnline = true,
        lastHeartbeat = Clock.System.now(),
        lastSync = Clock.System.now(),
        pendingItems = 0
    ))
    override val status: StateFlow<HeartbeatStatus> = _status.asStateFlow()
    
    private val _isRunning = MutableStateFlow(false)
    override val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    
    override suspend fun start() {
        _isRunning.value = true
        println("SimulatedHeartbeatService: Started")
    }
    
    override suspend fun stop() {
        _isRunning.value = false
        println("SimulatedHeartbeatService: Stopped")
    }
    
    override suspend fun syncNow() {
        _status.value = _status.value.copy(isSyncing = true)
        delay(500)
        _status.value = _status.value.copy(
            isSyncing = false,
            lastSync = Clock.System.now(),
            pendingItems = 0
        )
        println("SimulatedHeartbeatService: Sync complete")
    }
    
    override suspend fun pingNow(): Boolean {
        delay(100)
        _status.value = _status.value.copy(
            lastHeartbeat = Clock.System.now(),
            isOnline = true
        )
        return true
    }
    
    override suspend fun getPendingCount(): Int = _status.value.pendingItems
    
    // Test helpers
    fun setOnline(online: Boolean) {
        _status.value = _status.value.copy(isOnline = online)
    }
    
    fun setPendingItems(count: Int) {
        _status.value = _status.value.copy(pendingItems = count)
    }
}

