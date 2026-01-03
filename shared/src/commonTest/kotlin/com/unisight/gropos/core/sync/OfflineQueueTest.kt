package com.unisight.gropos.core.sync

import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for DefaultOfflineQueueService.
 * 
 * Per QA Audit: Offline queue must persist failed transactions.
 */
class OfflineQueueTest {
    
    // ========================================================================
    // Enqueue Tests
    // ========================================================================
    
    @Test
    fun `enqueue adds item to queue`() = runTest {
        val syncHandler = FakeSyncHandler()
        val queue = DefaultOfflineQueueService(syncHandler)
        
        val item = createQueuedItem(type = QueueItemType.TRANSACTION)
        queue.enqueue(item)
        
        assertEquals(1, queue.getPendingCount())
    }
    
    @Test
    fun `enqueue multiple items increases count`() = runTest {
        val syncHandler = FakeSyncHandler()
        val queue = DefaultOfflineQueueService(syncHandler)
        
        queue.enqueue(createQueuedItem())
        queue.enqueue(createQueuedItem())
        queue.enqueue(createQueuedItem())
        
        assertEquals(3, queue.getPendingCount())
    }
    
    @Test
    fun `enqueueTransaction assigns ID`() = runTest {
        val syncHandler = FakeSyncHandler()
        val queue = DefaultOfflineQueueService(syncHandler)
        
        val id = queue.enqueueTransaction(payload = """{"txnId": "123"}""")
        
        assertTrue(id > 0)
        assertEquals(1, queue.getPendingCount())
    }
    
    // ========================================================================
    // Process Queue Tests
    // ========================================================================
    
    @Test
    fun `processQueue syncs all items on success`() = runTest {
        val syncHandler = FakeSyncHandler()
        val queue = DefaultOfflineQueueService(syncHandler)
        
        queue.enqueue(createQueuedItem())
        queue.enqueue(createQueuedItem())
        
        val syncedCount = queue.processQueue()
        
        assertEquals(2, syncedCount)
        assertEquals(0, queue.getPendingCount())
        assertEquals(2, syncHandler.syncCallCount)
    }
    
    @Test
    fun `processQueue returns zero for empty queue`() = runTest {
        val syncHandler = FakeSyncHandler()
        val queue = DefaultOfflineQueueService(syncHandler)
        
        val syncedCount = queue.processQueue()
        
        assertEquals(0, syncedCount)
        assertEquals(0, syncHandler.syncCallCount)
    }
    
    @Test
    fun `failed item is re-queued with incremented attempt`() = runTest {
        val syncHandler = FakeSyncHandler()
        syncHandler.simulateFailure = true
        
        val queue = DefaultOfflineQueueService(
            syncHandler = syncHandler,
            config = OfflineQueueConfig(maxRetries = 5)
        )
        
        queue.enqueue(createQueuedItem())
        
        // First process - should fail and re-queue
        queue.processQueue()
        
        assertEquals(1, queue.getPendingCount())
        val pending = queue.getAllPending()
        assertEquals(1, pending[0].attempts)
    }
    
    @Test
    fun `item abandoned after max retries`() = runTest {
        val syncHandler = FakeSyncHandler()
        syncHandler.simulateFailure = true
        
        val queue = DefaultOfflineQueueService(
            syncHandler = syncHandler,
            config = OfflineQueueConfig(maxRetries = 3)
        )
        
        // Create item with 2 attempts already
        val item = createQueuedItem().copy(attempts = 2)
        queue.enqueue(item)
        
        // Third failure should abandon
        queue.processQueue()
        
        assertEquals(0, queue.getPendingCount())
        assertEquals(1, queue.getAbandonedItems().size)
    }
    
    @Test
    fun `permanently failed item goes directly to abandoned`() = runTest {
        val syncHandler = FakeSyncHandler()
        syncHandler.simulatePermanentFailure = true
        
        val queue = DefaultOfflineQueueService(syncHandler)
        
        queue.enqueue(createQueuedItem())
        queue.processQueue()
        
        assertEquals(0, queue.getPendingCount())
        assertEquals(1, queue.getAbandonedItems().size)
    }
    
    // ========================================================================
    // FIFO Order Tests
    // ========================================================================
    
    @Test
    fun `items processed in FIFO order`() = runTest {
        val syncHandler = FakeSyncHandler()
        val queue = DefaultOfflineQueueService(syncHandler)
        
        queue.enqueue(createQueuedItem(payload = "first"))
        queue.enqueue(createQueuedItem(payload = "second"))
        queue.enqueue(createQueuedItem(payload = "third"))
        
        queue.processQueue()
        
        assertEquals(listOf("first", "second", "third"), syncHandler.syncedPayloads)
    }
    
    // ========================================================================
    // Abandoned Item Management Tests
    // ========================================================================
    
    @Test
    fun `clearAbandonedItem removes from list`() = runTest {
        val syncHandler = FakeSyncHandler()
        syncHandler.simulatePermanentFailure = true
        
        val queue = DefaultOfflineQueueService(syncHandler)
        
        queue.enqueue(createQueuedItem())
        queue.processQueue()
        
        val abandonedId = queue.getAbandonedItems()[0].item.id
        queue.clearAbandonedItem(abandonedId)
        
        assertEquals(0, queue.getAbandonedItems().size)
    }
    
    @Test
    fun `retryAbandonedItem moves back to queue`() = runTest {
        val syncHandler = FakeSyncHandler()
        syncHandler.simulatePermanentFailure = true
        
        val queue = DefaultOfflineQueueService(syncHandler)
        
        queue.enqueue(createQueuedItem())
        queue.processQueue()
        
        val abandonedId = queue.getAbandonedItems()[0].item.id
        
        // Fix the issue and retry
        syncHandler.simulatePermanentFailure = false
        queue.retryAbandonedItem(abandonedId)
        
        assertEquals(0, queue.getAbandonedItems().size)
        assertEquals(1, queue.getPendingCount())
    }
    
    // ========================================================================
    // Pending Count Observable Tests
    // ========================================================================
    
    @Test
    fun `pendingCount flow updates on enqueue`() = runTest {
        val syncHandler = FakeSyncHandler()
        val queue = DefaultOfflineQueueService(syncHandler)
        
        assertEquals(0, queue.pendingCount.value)
        
        queue.enqueue(createQueuedItem())
        assertEquals(1, queue.pendingCount.value)
        
        queue.enqueue(createQueuedItem())
        assertEquals(2, queue.pendingCount.value)
    }
    
    @Test
    fun `pendingCount flow updates after process`() = runTest {
        val syncHandler = FakeSyncHandler()
        val queue = DefaultOfflineQueueService(syncHandler)
        
        queue.enqueue(createQueuedItem())
        queue.enqueue(createQueuedItem())
        assertEquals(2, queue.pendingCount.value)
        
        queue.processQueue()
        assertEquals(0, queue.pendingCount.value)
    }
    
    // ========================================================================
    // Helpers
    // ========================================================================
    
    private fun createQueuedItem(
        type: QueueItemType = QueueItemType.TRANSACTION,
        payload: String = """{"test": true}"""
    ): QueuedItem {
        return QueuedItem(
            id = 0, // Will be assigned
            type = type,
            payload = payload,
            createdAt = Clock.System.now(),
            attempts = 0,
            lastAttempt = null
        )
    }
}

/**
 * Fake sync handler for testing.
 */
class FakeSyncHandler : QueueItemSyncHandler {
    var simulateFailure = false
    var simulatePermanentFailure = false
    var syncCallCount = 0
    val syncedPayloads = mutableListOf<String>()
    
    override suspend fun sync(item: QueuedItem): ProcessResult {
        syncCallCount++
        syncedPayloads.add(item.payload)
        
        return when {
            simulatePermanentFailure -> ProcessResult.Abandon("Permanent failure")
            simulateFailure -> ProcessResult.Retry("Temporary failure")
            else -> ProcessResult.Success
        }
    }
}

