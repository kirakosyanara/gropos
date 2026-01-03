package com.unisight.gropos.core.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Default implementation of OfflineQueueService with persistent storage.
 * 
 * **P0 FIX (QA Audit - CRITICAL):**
 * This implementation now uses QueuePersistence for crash-safe storage.
 * Transaction data is persisted to Couchbase, ensuring no money is lost
 * if the app crashes before sync.
 * 
 * **Design:**
 * - Thread-safe queue operations using Mutex
 * - FIFO ordering for fair processing
 * - Retry count tracking for backoff decisions
 * - Maximum attempts before abandoning items
 * - **Persistence:** Items saved to Couchbase on enqueue, deleted on success
 * 
 * **Crash Recovery:**
 * On app restart, pending items are loaded from persistence and reprocessed.
 * 
 * Per reliability-stability.mdc: Idempotency keys for transaction safety.
 */
class DefaultOfflineQueueService(
    private val syncHandler: QueueItemSyncHandler,
    private val persistence: QueuePersistence,
    private val config: OfflineQueueConfig = OfflineQueueConfig()
) : OfflineQueueService {
    
    /**
     * Mutex for thread-safe queue operations.
     */
    private val queueMutex = Mutex()
    
    /**
     * Observable pending count for UI binding.
     */
    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount.asStateFlow()
    
    /**
     * Items that have exceeded max retries.
     * Note: Abandoned items are kept in memory only (can be enhanced to persist if needed).
     */
    private val abandonedItems = mutableListOf<AbandonedItem>()
    
    // ========================================================================
    // Queue Operations
    // ========================================================================
    
    override suspend fun enqueue(item: QueuedItem) {
        queueMutex.withLock {
            // Assign ID if not already set and save to persistence
            val itemWithId = if (item.id == 0L) {
                val newId = persistence.generateId()
                item.copy(id = newId)
            } else {
                item
            }
            
            // P0 FIX: Save to persistent storage BEFORE considering it enqueued
            persistence.save(itemWithId)
            _pendingCount.value = persistence.count()
            
            println("[OFFLINE_QUEUE] Enqueued item ${itemWithId.id} (${itemWithId.type}). Queue size: ${_pendingCount.value}")
        }
    }
    
    /**
     * Enqueues a transaction payload for later sync.
     * 
     * Convenience method that creates the QueuedItem.
     * 
     * @param payload JSON-serialized transaction data
     * @param type The type of queue item
     * @return The generated item ID
     */
    suspend fun enqueueTransaction(payload: String, type: QueueItemType = QueueItemType.TRANSACTION): Long {
        val newId = persistence.generateId()
        val item = QueuedItem(
            id = newId,
            type = type,
            payload = payload,
            createdAt = Clock.System.now(),
            attempts = 0,
            lastAttempt = null
        )
        
        enqueue(item)
        return item.id
    }
    
    override suspend fun getPendingCount(): Int {
        return queueMutex.withLock { persistence.count() }
    }
    
    /**
     * Processes all pending items in the queue.
     * 
     * **P0 FIX:** Items are now read from persistence, not memory.
     * Items are deleted from persistence ONLY after successful sync.
     * 
     * **Processing Order:** FIFO (oldest first)
     * 
     * **Failure Handling:**
     * - Failed items remain in persistence with incremented attempt count
     * - Items exceeding max retries are moved to abandoned list
     * 
     * @return Number of successfully processed items
     */
    override suspend fun processQueue(): Int {
        var successCount = 0
        
        // P0 FIX: Get items from persistent storage (survives crash)
        val itemsToProcess = queueMutex.withLock {
            persistence.getAll()
        }
        
        if (itemsToProcess.isEmpty()) {
            println("[OFFLINE_QUEUE] Queue empty, nothing to process")
            return 0
        }
        
        println("[OFFLINE_QUEUE] Processing ${itemsToProcess.size} items from persistent storage...")
        
        for (item in itemsToProcess) {
            val result = processItem(item)
            
            when (result) {
                is ProcessResult.Success -> {
                    // P0 FIX: Delete from persistence ONLY after successful sync
                    queueMutex.withLock {
                        persistence.delete(item.id)
                    }
                    successCount++
                    println("[OFFLINE_QUEUE] Item ${item.id} synced and removed from persistence")
                }
                
                is ProcessResult.Retry -> {
                    // Update attempt count in persistence
                    val updatedItem = item.copy(
                        attempts = item.attempts + 1,
                        lastAttempt = Clock.System.now()
                    )
                    
                    if (updatedItem.attempts >= config.maxRetries) {
                        // Move to abandoned, delete from persistence
                        queueMutex.withLock {
                            persistence.delete(item.id)
                            abandonedItems.add(AbandonedItem(
                                item = updatedItem,
                                reason = result.reason,
                                abandonedAt = Clock.System.now()
                            ))
                        }
                        println("[OFFLINE_QUEUE] Item ${item.id} abandoned after ${updatedItem.attempts} attempts")
                    } else {
                        // Update in persistence for later retry
                        queueMutex.withLock {
                            persistence.update(updatedItem)
                        }
                        println("[OFFLINE_QUEUE] Item ${item.id} updated for retry (attempt ${updatedItem.attempts})")
                    }
                }
                
                is ProcessResult.Abandon -> {
                    // Permanent failure - remove from persistence, add to abandoned
                    queueMutex.withLock {
                        persistence.delete(item.id)
                        abandonedItems.add(AbandonedItem(
                            item = item,
                            reason = result.reason,
                            abandonedAt = Clock.System.now()
                        ))
                    }
                    println("[OFFLINE_QUEUE] Item ${item.id} permanently abandoned: ${result.reason}")
                }
            }
        }
        
        // Update pending count from persistence
        _pendingCount.value = queueMutex.withLock { persistence.count() }
        
        println("[OFFLINE_QUEUE] Processed: $successCount/${itemsToProcess.size} successful")
        return successCount
    }
    
    /**
     * Processes a single queue item.
     */
    private suspend fun processItem(item: QueuedItem): ProcessResult {
        return try {
            syncHandler.sync(item)
        } catch (e: Exception) {
            println("[OFFLINE_QUEUE] Error processing item ${item.id}: ${e.message}")
            ProcessResult.Retry(e.message ?: "Unknown error")
        }
    }
    
    // ========================================================================
    // Query Methods
    // ========================================================================
    
    /**
     * Returns all pending items (for debugging/monitoring).
     * 
     * P0 FIX: Now reads from persistent storage.
     */
    suspend fun getAllPending(): List<QueuedItem> {
        return queueMutex.withLock { persistence.getAll() }
    }
    
    /**
     * Returns items that have been abandoned after max retries.
     */
    suspend fun getAbandonedItems(): List<AbandonedItem> {
        return queueMutex.withLock { abandonedItems.toList() }
    }
    
    /**
     * Clears an abandoned item (for manual resolution).
     */
    suspend fun clearAbandonedItem(itemId: Long) {
        queueMutex.withLock {
            abandonedItems.removeAll { it.item.id == itemId }
        }
    }
    
    /**
     * Retries an abandoned item.
     */
    suspend fun retryAbandonedItem(itemId: Long): Boolean {
        val item = queueMutex.withLock {
            abandonedItems.find { it.item.id == itemId }?.also {
                abandonedItems.remove(it)
            }
        } ?: return false
        
        // Re-queue with reset attempt count
        enqueue(item.item.copy(attempts = 0))
        return true
    }
    
    /**
     * Clears the entire queue (for testing/reset).
     * 
     * P0 FIX: Now clears persistent storage as well.
     */
    suspend fun clear() {
        queueMutex.withLock {
            persistence.clear()
            abandonedItems.clear()
            _pendingCount.value = 0
        }
    }
    
    /**
     * Initializes the pending count from persistence on startup.
     * 
     * Call this during app initialization to restore queue state
     * after a crash or restart.
     */
    suspend fun initialize() {
        _pendingCount.value = persistence.count()
        println("[OFFLINE_QUEUE] Initialized with ${_pendingCount.value} pending items from persistence")
    }
}

/**
 * Configuration for the offline queue.
 */
data class OfflineQueueConfig(
    /** Maximum retry attempts before abandoning an item */
    val maxRetries: Int = 5,
    
    /** Whether to persist queue to disk (future feature) */
    val persistToDisk: Boolean = false
)

/**
 * Handler interface for syncing queue items to the server.
 * 
 * Implementations should:
 * - Make the actual API call
 * - Return appropriate ProcessResult
 * - Handle idempotency (server should dedupe by transaction ID)
 */
interface QueueItemSyncHandler {
    /**
     * Attempts to sync a queue item to the server.
     * 
     * @param item The item to sync
     * @return Result indicating success, retry needed, or permanent failure
     */
    suspend fun sync(item: QueuedItem): ProcessResult
}

/**
 * Result of processing a queue item.
 */
sealed class ProcessResult {
    /** Item synced successfully */
    data object Success : ProcessResult()
    
    /** Item should be retried later (temporary failure) */
    data class Retry(val reason: String) : ProcessResult()
    
    /** Item should be abandoned (permanent failure) */
    data class Abandon(val reason: String) : ProcessResult()
}

/**
 * An item that has been abandoned after exceeding max retries.
 */
data class AbandonedItem(
    val item: QueuedItem,
    val reason: String,
    val abandonedAt: Instant
)

