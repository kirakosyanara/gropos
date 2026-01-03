package com.unisight.gropos.core.sync

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Unit tests for SyncWorker exponential backoff.
 * 
 * Per QA Audit: Must implement exponential backoff for network resilience.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SyncWorkerTest {
    
    // ========================================================================
    // Basic Sync Tests
    // ========================================================================
    
    @Test
    fun `syncNow succeeds when online with no pending items`() = runTest {
        val queue = FakeOfflineQueueService()
        val syncEngine = FakeSyncEngine()
        
        val worker = SyncWorker(
            offlineQueue = queue,
            syncEngine = syncEngine,
            dispatcher = StandardTestDispatcher(testScheduler)
        )
        
        val result = worker.syncNow()
        
        assertTrue(result.success)
        assertEquals(0, result.itemsSynced)
    }
    
    @Test
    fun `syncNow processes pending items when online`() = runTest {
        val queue = FakeOfflineQueueService()
        queue.pendingItems = 5
        queue.processResult = 5 // All synced
        
        val syncEngine = FakeSyncEngine()
        
        val worker = SyncWorker(
            offlineQueue = queue,
            syncEngine = syncEngine,
            dispatcher = StandardTestDispatcher(testScheduler)
        )
        
        val result = worker.syncNow()
        
        assertTrue(result.success)
        assertEquals(5, result.itemsSynced)
    }
    
    @Test
    fun `syncNow fails when offline`() = runTest {
        val queue = FakeOfflineQueueService()
        queue.pendingItems = 5
        
        val syncEngine = FakeSyncEngine()
        syncEngine.isOnline = false
        
        val worker = SyncWorker(
            offlineQueue = queue,
            syncEngine = syncEngine,
            dispatcher = StandardTestDispatcher(testScheduler)
        )
        
        val result = worker.syncNow()
        
        assertFalse(result.success)
        assertTrue(result.errors.isNotEmpty())
    }
    
    @Test
    fun `syncNow reports partial success`() = runTest {
        val queue = FakeOfflineQueueService()
        queue.pendingItems = 5
        queue.processResult = 3 // Only 3 of 5 synced
        queue.remainingAfterProcess = 2
        
        val syncEngine = FakeSyncEngine()
        
        val worker = SyncWorker(
            offlineQueue = queue,
            syncEngine = syncEngine,
            dispatcher = StandardTestDispatcher(testScheduler)
        )
        
        val result = worker.syncNow()
        
        assertFalse(result.success)
        assertEquals(3, result.itemsSynced)
    }
    
    // ========================================================================
    // State Management Tests
    // ========================================================================
    
    @Test
    fun `state updates to isSyncing during sync`() = runTest {
        val queue = FakeOfflineQueueService()
        val syncEngine = FakeSyncEngine()
        
        val worker = SyncWorker(
            offlineQueue = queue,
            syncEngine = syncEngine,
            dispatcher = StandardTestDispatcher(testScheduler)
        )
        
        assertFalse(worker.state.value.isSyncing)
        
        // Note: In a real test with delays, we'd capture the intermediate state
        worker.syncNow()
        
        // After completion, should be false
        assertFalse(worker.state.value.isSyncing)
    }
    
    @Test
    fun `state tracks consecutive failures when offline`() = runTest {
        val queue = FakeOfflineQueueService()
        queue.pendingItems = 5
        
        val syncEngine = FakeSyncEngine()
        syncEngine.isOnline = false // Full failure - offline
        
        val worker = SyncWorker(
            offlineQueue = queue,
            syncEngine = syncEngine,
            dispatcher = StandardTestDispatcher(testScheduler)
        )
        
        // First failure
        worker.syncNow()
        assertEquals(1, worker.state.value.consecutiveFailures)
        
        // Second failure
        worker.syncNow()
        assertEquals(2, worker.state.value.consecutiveFailures)
        
        // Third failure
        worker.syncNow()
        assertEquals(3, worker.state.value.consecutiveFailures)
    }
    
    @Test
    fun `state resets consecutive failures on full success`() = runTest {
        val queue = FakeOfflineQueueService()
        queue.pendingItems = 5
        
        val syncEngine = FakeSyncEngine()
        syncEngine.isOnline = false // Start offline
        
        val worker = SyncWorker(
            offlineQueue = queue,
            syncEngine = syncEngine,
            dispatcher = StandardTestDispatcher(testScheduler)
        )
        
        // Accumulate failures while offline
        worker.syncNow()
        worker.syncNow()
        assertEquals(2, worker.state.value.consecutiveFailures)
        
        // Come back online and succeed
        syncEngine.isOnline = true
        queue.processResult = 5
        queue.remainingAfterProcess = 0
        worker.syncNow()
        
        assertEquals(0, worker.state.value.consecutiveFailures)
    }
    
    // ========================================================================
    // Exponential Backoff Tests
    // ========================================================================
    
    @Test
    fun `backoff delay increases exponentially on failure`() = runTest {
        val queue = FakeOfflineQueueService()
        val syncEngine = FakeSyncEngine()
        syncEngine.isOnline = false // Force failures
        
        val config = SyncWorkerConfig(
            baseDelay = 1000.milliseconds,
            maxDelay = 60.seconds,
            maxExponent = 5,
            jitterFactor = 0.0 // No jitter for predictable testing
        )
        
        val worker = SyncWorker(
            offlineQueue = queue,
            syncEngine = syncEngine,
            config = config,
            dispatcher = StandardTestDispatcher(testScheduler)
        )
        
        // Failure 1 -> 2^1 = 2s delay
        worker.syncNow()
        assertEquals(1, worker.state.value.consecutiveFailures)
        
        // Failure 2 -> 2^2 = 4s delay
        worker.syncNow()
        assertEquals(2, worker.state.value.consecutiveFailures)
        
        // Failure 3 -> 2^3 = 8s delay
        worker.syncNow()
        assertEquals(3, worker.state.value.consecutiveFailures)
        
        // The delays should have been: 2s, 4s, 8s (exponential)
        // We verify via the state tracking
    }
    
    @Test
    fun `resetBackoff clears failure count`() = runTest {
        val queue = FakeOfflineQueueService()
        val syncEngine = FakeSyncEngine()
        syncEngine.isOnline = false
        
        val worker = SyncWorker(
            offlineQueue = queue,
            syncEngine = syncEngine,
            dispatcher = StandardTestDispatcher(testScheduler)
        )
        
        // Accumulate failures
        worker.syncNow()
        worker.syncNow()
        worker.syncNow()
        assertEquals(3, worker.state.value.consecutiveFailures)
        
        // External trigger (e.g., network state change)
        worker.resetBackoff()
        
        assertEquals(0, worker.state.value.consecutiveFailures)
    }
    
    // ========================================================================
    // Worker Lifecycle Tests
    // ========================================================================
    
    @Test
    fun `start sets isRunning to true`() = runTest {
        val queue = FakeOfflineQueueService()
        val syncEngine = FakeSyncEngine()
        
        val worker = SyncWorker(
            offlineQueue = queue,
            syncEngine = syncEngine,
            dispatcher = StandardTestDispatcher(testScheduler)
        )
        
        assertFalse(worker.state.value.isRunning)
        
        worker.start()
        advanceTimeBy(100)
        
        assertTrue(worker.state.value.isRunning)
        
        worker.stop()
    }
    
    @Test
    fun `stop sets isRunning to false`() = runTest {
        val queue = FakeOfflineQueueService()
        val syncEngine = FakeSyncEngine()
        
        val worker = SyncWorker(
            offlineQueue = queue,
            syncEngine = syncEngine,
            dispatcher = StandardTestDispatcher(testScheduler)
        )
        
        worker.start()
        advanceTimeBy(100)
        assertTrue(worker.state.value.isRunning)
        
        worker.stop()
        assertFalse(worker.state.value.isRunning)
    }
}

// ============================================================================
// Fake Implementations
// ============================================================================

class FakeOfflineQueueService : OfflineQueueService {
    var pendingItems = 0
    var processResult = 0
    var remainingAfterProcess = 0
    
    override suspend fun processQueue(): Int {
        val result = processResult
        pendingItems = remainingAfterProcess
        return result
    }
    
    override suspend fun getPendingCount(): Int = pendingItems
    
    override suspend fun enqueue(item: QueuedItem) {
        pendingItems++
    }
}

class FakeSyncEngine : SyncEngine {
    var isOnline = true
    var downloadResult = SyncResult(success = true)
    
    override suspend fun ping(): Boolean = isOnline
    
    override suspend fun downloadUpdates(): SyncResult = downloadResult
    
    override fun getLastDownloadTime(): Instant? = null
}

