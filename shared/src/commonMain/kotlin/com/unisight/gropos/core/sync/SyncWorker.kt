package com.unisight.gropos.core.sync

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Background worker for synchronizing offline queue with the server.
 * 
 * **Per QA Audit Finding (CRITICAL):**
 * Implements exponential backoff to prevent:
 * - Server overload during recovery
 * - Battery drain from aggressive retries
 * - Network congestion during outages
 * 
 * **Backoff Formula:**
 * ```
 * delay = baseDelay * 2^retryCount + jitter
 * ```
 * 
 * **Jitter:**
 * Random offset (+/- 20%) prevents "thundering herd" when multiple
 * devices reconnect simultaneously after an outage.
 * 
 * Per reliability-stability.mdc: Use WorkManager for Android, coroutines for Desktop.
 */
class SyncWorker(
    private val offlineQueue: OfflineQueueService,
    private val syncEngine: SyncEngine,
    private val config: SyncWorkerConfig = SyncWorkerConfig(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    
    private val scope = CoroutineScope(dispatcher + SupervisorJob())
    
    private val _state = MutableStateFlow(SyncWorkerState())
    val state: StateFlow<SyncWorkerState> = _state.asStateFlow()
    
    private var syncJob: Job? = null
    private var consecutiveFailures = 0
    
    // ========================================================================
    // Lifecycle
    // ========================================================================
    
    /**
     * Starts the background sync worker.
     * 
     * The worker will:
     * 1. Check for pending items periodically
     * 2. Attempt sync with exponential backoff on failure
     * 3. Reset backoff on success
     */
    fun start() {
        if (syncJob?.isActive == true) {
            println("[SYNC_WORKER] Already running")
            return
        }
        
        println("[SYNC_WORKER] Starting...")
        
        syncJob = scope.launch {
            _state.value = _state.value.copy(isRunning = true)
            
            while (isActive) {
                try {
                    performSync()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    println("[SYNC_WORKER] Unexpected error: ${e.message}")
                }
                
                // Calculate next sync delay
                val nextDelay = calculateNextDelay()
                _state.value = _state.value.copy(
                    nextSyncAt = Clock.System.now() + nextDelay
                )
                
                delay(nextDelay)
            }
        }
    }
    
    /**
     * Stops the background sync worker.
     */
    fun stop() {
        println("[SYNC_WORKER] Stopping...")
        syncJob?.cancel()
        syncJob = null
        _state.value = _state.value.copy(isRunning = false)
    }
    
    /**
     * Forces an immediate sync attempt.
     * 
     * Use sparingly - prefer letting the worker manage timing.
     */
    suspend fun syncNow(): SyncResult {
        return performSync()
    }
    
    // ========================================================================
    // Sync Logic
    // ========================================================================
    
    private suspend fun performSync(): SyncResult {
        _state.value = _state.value.copy(isSyncing = true)
        
        try {
            // Step 1: Check connectivity
            val isOnline = try {
                syncEngine.ping()
            } catch (e: Exception) {
                false
            }
            
            if (!isOnline) {
                println("[SYNC_WORKER] Offline - skipping sync")
                handleSyncFailure("Network unavailable")
                return SyncResult(success = false, errors = listOf("Network unavailable"))
            }
            
            // Step 2: Process offline queue
            val pendingCount = offlineQueue.getPendingCount()
            
            if (pendingCount == 0) {
                // Nothing to sync
                handleSyncSuccess(0)
                return SyncResult(success = true, itemsSynced = 0)
            }
            
            println("[SYNC_WORKER] Processing $pendingCount pending items...")
            
            val syncedCount = offlineQueue.processQueue()
            val remainingCount = offlineQueue.getPendingCount()
            
            if (remainingCount == 0) {
                // All items synced
                handleSyncSuccess(syncedCount)
                return SyncResult(success = true, itemsSynced = syncedCount)
            } else {
                // Some items failed
                handlePartialSuccess(syncedCount, remainingCount)
                return SyncResult(
                    success = false,
                    itemsSynced = syncedCount,
                    errors = listOf("$remainingCount items failed to sync")
                )
            }
            
        } catch (e: Exception) {
            handleSyncFailure(e.message ?: "Unknown error")
            return SyncResult(success = false, errors = listOf(e.message ?: "Unknown error"))
        } finally {
            _state.value = _state.value.copy(isSyncing = false)
        }
    }
    
    // ========================================================================
    // Backoff Logic
    // ========================================================================
    
    private fun handleSyncSuccess(itemsSynced: Int) {
        consecutiveFailures = 0
        _state.value = _state.value.copy(
            lastSyncAt = Clock.System.now(),
            lastSyncSuccess = true,
            lastSyncItemCount = itemsSynced,
            consecutiveFailures = 0
        )
        println("[SYNC_WORKER] Sync successful ($itemsSynced items). Backoff reset.")
    }
    
    private fun handlePartialSuccess(synced: Int, remaining: Int) {
        // Don't fully reset backoff on partial success
        consecutiveFailures = (consecutiveFailures / 2).coerceAtLeast(1)
        _state.value = _state.value.copy(
            lastSyncAt = Clock.System.now(),
            lastSyncSuccess = false,
            lastSyncItemCount = synced,
            pendingCount = remaining,
            consecutiveFailures = consecutiveFailures
        )
        println("[SYNC_WORKER] Partial sync: $synced synced, $remaining remaining")
    }
    
    private fun handleSyncFailure(error: String) {
        consecutiveFailures++
        _state.value = _state.value.copy(
            lastSyncAt = Clock.System.now(),
            lastSyncSuccess = false,
            lastError = error,
            consecutiveFailures = consecutiveFailures
        )
        println("[SYNC_WORKER] Sync failed ($consecutiveFailures consecutive). Error: $error")
    }
    
    /**
     * Calculates the next sync delay using exponential backoff with jitter.
     * 
     * **Formula:**
     * ```
     * baseDelay * 2^min(failures, maxExponent) + jitter
     * ```
     * 
     * **Example progression (with 5s base):**
     * - Failure 0: 5s (success)
     * - Failure 1: 10s
     * - Failure 2: 20s
     * - Failure 3: 40s
     * - Failure 4: 80s
     * - Failure 5+: 160s (capped)
     */
    private fun calculateNextDelay(): Duration {
        if (consecutiveFailures == 0) {
            return config.baseInterval
        }
        
        // Calculate exponential delay
        val exponent = min(consecutiveFailures, config.maxExponent)
        val exponentialMs = config.baseDelay.inWholeMilliseconds * 2.0.pow(exponent).toLong()
        
        // Add jitter (+/- 20%)
        val jitterRange = (exponentialMs * config.jitterFactor).toLong()
        val jitter = Random.nextLong(-jitterRange, jitterRange)
        
        // Apply cap
        val finalMs = min(exponentialMs + jitter, config.maxDelay.inWholeMilliseconds)
        
        println("[SYNC_WORKER] Next sync in ${finalMs}ms (backoff level: $consecutiveFailures)")
        
        return finalMs.milliseconds
    }
    
    /**
     * Resets the backoff counter.
     * 
     * Call this when external factors indicate connectivity is restored
     * (e.g., network state change broadcast).
     */
    fun resetBackoff() {
        consecutiveFailures = 0
        _state.value = _state.value.copy(consecutiveFailures = 0)
        println("[SYNC_WORKER] Backoff reset by external trigger")
    }
}

/**
 * Configuration for SyncWorker.
 */
data class SyncWorkerConfig(
    /** Normal interval between syncs when successful */
    val baseInterval: Duration = 30.seconds,
    
    /** Base delay for exponential backoff */
    val baseDelay: Duration = 5.seconds,
    
    /** Maximum backoff delay */
    val maxDelay: Duration = 5.minutes,
    
    /** Maximum exponent for backoff calculation */
    val maxExponent: Int = 5,
    
    /** Jitter factor (0.0 - 0.5 recommended) */
    val jitterFactor: Double = 0.2
)

/**
 * Current state of the SyncWorker.
 */
data class SyncWorkerState(
    /** Whether the worker is running */
    val isRunning: Boolean = false,
    
    /** Whether a sync is currently in progress */
    val isSyncing: Boolean = false,
    
    /** When the last sync occurred */
    val lastSyncAt: Instant? = null,
    
    /** Whether the last sync was successful */
    val lastSyncSuccess: Boolean = true,
    
    /** Number of items synced in last sync */
    val lastSyncItemCount: Int = 0,
    
    /** When the next sync is scheduled */
    val nextSyncAt: Instant? = null,
    
    /** Last error message if sync failed */
    val lastError: String? = null,
    
    /** Number of consecutive sync failures */
    val consecutiveFailures: Int = 0,
    
    /** Current pending item count */
    val pendingCount: Int = 0
)

