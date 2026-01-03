package com.unisight.gropos.core.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.concurrent.atomic.AtomicLong

/**
 * Default implementation of OfflineQueueService.
 * 
 * **Per QA Audit Finding (CRITICAL):**
 * This implementation addresses the missing offline queue that was causing
 * data loss when network requests failed.
 * 
 * **Design:**
 * - Thread-safe queue operations using Mutex
 * - FIFO ordering for fair processing
 * - Retry count tracking for backoff decisions
 * - Maximum attempts before abandoning items
 * 
 * **Upgrade Path:**
 * Current implementation uses in-memory storage.
 * For production, replace internal storage with Couchbase:
 * ```kotlin
 * class CouchbaseOfflineQueueService(
 *     private val database: Database
 * ) : OfflineQueueService { ... }
 * ```
 * 
 * Per reliability-stability.mdc: Idempotency keys for transaction safety.
 */
class DefaultOfflineQueueService(
    private val syncHandler: QueueItemSyncHandler,
    private val config: OfflineQueueConfig = OfflineQueueConfig()
) : OfflineQueueService {
    
    /**
     * Mutex for thread-safe queue operations.
     */
    private val queueMutex = Mutex()
    
    /**
     * In-memory queue storage.
     * 
     * TODO: Replace with Couchbase persistence for production.
     */
    private val queue = mutableListOf<QueuedItem>()
    
    /**
     * ID generator for new items.
     */
    private val idGenerator = AtomicLong(0)
    
    /**
     * Observable pending count for UI binding.
     */
    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount.asStateFlow()
    
    /**
     * Items that have exceeded max retries.
     */
    private val abandonedItems = mutableListOf<AbandonedItem>()
    
    // ========================================================================
    // Queue Operations
    // ========================================================================
    
    override suspend fun enqueue(item: QueuedItem) {
        queueMutex.withLock {
            // Assign ID if not already set
            val itemWithId = if (item.id == 0L) {
                item.copy(id = idGenerator.incrementAndGet())
            } else {
                item
            }
            
            queue.add(itemWithId)
            _pendingCount.value = queue.size
            
            println("[OFFLINE_QUEUE] Enqueued item ${itemWithId.id} (${itemWithId.type}). Queue size: ${queue.size}")
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
        val item = QueuedItem(
            id = idGenerator.incrementAndGet(),
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
        return queueMutex.withLock { queue.size }
    }
    
    /**
     * Processes all pending items in the queue.
     * 
     * **Processing Order:** FIFO (oldest first)
     * 
     * **Failure Handling:**
     * - Failed items are re-queued with incremented attempt count
     * - Items exceeding max retries are moved to abandoned list
     * 
     * @return Number of successfully processed items
     */
    override suspend fun processQueue(): Int {
        var successCount = 0
        
        // Take a snapshot of current queue
        val itemsToProcess = queueMutex.withLock {
            queue.toList().also { queue.clear() }
        }
        
        if (itemsToProcess.isEmpty()) {
            println("[OFFLINE_QUEUE] Queue empty, nothing to process")
            return 0
        }
        
        println("[OFFLINE_QUEUE] Processing ${itemsToProcess.size} items...")
        
        for (item in itemsToProcess) {
            val result = processItem(item)
            
            when (result) {
                is ProcessResult.Success -> {
                    successCount++
                    println("[OFFLINE_QUEUE] Item ${item.id} synced successfully")
                }
                
                is ProcessResult.Retry -> {
                    // Re-queue with updated attempt count
                    val updatedItem = item.copy(
                        attempts = item.attempts + 1,
                        lastAttempt = Clock.System.now()
                    )
                    
                    if (updatedItem.attempts >= config.maxRetries) {
                        // Move to abandoned
                        queueMutex.withLock {
                            abandonedItems.add(AbandonedItem(
                                item = updatedItem,
                                reason = result.reason,
                                abandonedAt = Clock.System.now()
                            ))
                        }
                        println("[OFFLINE_QUEUE] Item ${item.id} abandoned after ${updatedItem.attempts} attempts")
                    } else {
                        // Re-queue for later
                        enqueue(updatedItem)
                        println("[OFFLINE_QUEUE] Item ${item.id} re-queued (attempt ${updatedItem.attempts})")
                    }
                }
                
                is ProcessResult.Abandon -> {
                    // Permanent failure - don't retry
                    queueMutex.withLock {
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
        
        // Update pending count
        _pendingCount.value = queueMutex.withLock { queue.size }
        
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
     */
    suspend fun getAllPending(): List<QueuedItem> {
        return queueMutex.withLock { queue.toList() }
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
     */
    suspend fun clear() {
        queueMutex.withLock {
            queue.clear()
            abandonedItems.clear()
            _pendingCount.value = 0
        }
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

