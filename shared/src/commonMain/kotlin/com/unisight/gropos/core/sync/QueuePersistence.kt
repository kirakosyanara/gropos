package com.unisight.gropos.core.sync

/**
 * Interface for persistent storage of offline queue items.
 * 
 * **Per QA Audit P0:**
 * Replaces in-memory MutableList with persistent storage to prevent
 * data loss on app crash. Money must never be lost.
 * 
 * **Implementations:**
 * - InMemoryQueuePersistence: For testing only
 * - CouchbaseQueuePersistence: Production implementation (platform-specific)
 * 
 * **Per reliability-stability.mdc:**
 * - Write-ahead: Items are saved BEFORE sync attempt
 * - Delete-on-success: Items are deleted ONLY after successful sync
 * - Crash recovery: Unsynced items survive app restart
 */
interface QueuePersistence {
    
    /**
     * Saves a queued item to persistent storage.
     * 
     * @param item The item to save
     * @return The item with assigned ID if none was set
     */
    suspend fun save(item: QueuedItem): QueuedItem
    
    /**
     * Gets all pending items ordered by creation time (oldest first).
     * 
     * @return List of pending items in FIFO order
     */
    suspend fun getAll(): List<QueuedItem>
    
    /**
     * Gets the count of pending items.
     * 
     * @return Number of items in queue
     */
    suspend fun count(): Int
    
    /**
     * Deletes an item from persistent storage.
     * 
     * Called ONLY after successful sync.
     * 
     * @param itemId The ID of the item to delete
     * @return true if deleted, false if not found
     */
    suspend fun delete(itemId: Long): Boolean
    
    /**
     * Updates an item in persistent storage.
     * 
     * Used to update attempt count and last attempt time.
     * 
     * @param item The updated item
     * @return true if updated, false if not found
     */
    suspend fun update(item: QueuedItem): Boolean
    
    /**
     * Clears all items from storage.
     * 
     * Use with caution - typically only for testing or reset.
     */
    suspend fun clear()
    
    /**
     * Generates a unique ID for a new item.
     * 
     * @return Unique long ID
     */
    suspend fun generateId(): Long
}

/**
 * In-memory implementation for testing and fallback.
 * 
 * **WARNING:** Data is lost on app restart. Use CouchbaseQueuePersistence in production.
 */
class InMemoryQueuePersistence : QueuePersistence {
    
    private val items = mutableListOf<QueuedItem>()
    private var nextId = 1L
    
    override suspend fun save(item: QueuedItem): QueuedItem {
        val itemWithId = if (item.id == 0L) {
            item.copy(id = generateId())
        } else {
            item
        }
        items.add(itemWithId)
        return itemWithId
    }
    
    override suspend fun getAll(): List<QueuedItem> {
        return items.sortedBy { it.createdAt }
    }
    
    override suspend fun count(): Int = items.size
    
    override suspend fun delete(itemId: Long): Boolean {
        return items.removeAll { it.id == itemId }
    }
    
    override suspend fun update(item: QueuedItem): Boolean {
        val index = items.indexOfFirst { it.id == item.id }
        if (index >= 0) {
            items[index] = item
            return true
        }
        return false
    }
    
    override suspend fun clear() {
        items.clear()
    }
    
    override suspend fun generateId(): Long {
        return nextId++
    }
}

