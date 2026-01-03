package com.unisight.gropos.core.sync

import com.couchbase.lite.Collection
import com.couchbase.lite.DataSource
import com.couchbase.lite.MutableDocument
import com.couchbase.lite.Ordering
import com.couchbase.lite.QueryBuilder
import com.couchbase.lite.SelectResult
import com.unisight.gropos.core.database.DatabaseConfig
import com.unisight.gropos.core.database.DatabaseProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.concurrent.atomic.AtomicLong

/**
 * Couchbase implementation of QueuePersistence for Android.
 * 
 * **P0 FIX (QA Audit):**
 * Ensures transaction data survives app crashes by persisting to Couchbase.
 * 
 * **Collection:** OfflineQueue in "local" scope
 * 
 * **Document Structure:**
 * ```json
 * {
 *   "_id": "queue_123",
 *   "id": 123,
 *   "type": "TRANSACTION",
 *   "payload": "{ ... json data ... }",
 *   "createdAt": "2026-01-03T12:30:00Z",
 *   "attempts": 0,
 *   "lastAttempt": null
 * }
 * ```
 * 
 * Per reliability-stability.mdc: Write-ahead logging pattern.
 */
class CouchbaseQueuePersistence(
    private val databaseProvider: DatabaseProvider
) : QueuePersistence {
    
    private val mutex = Mutex()
    
    /**
     * OfflineQueue collection in "local" scope.
     * 
     * Per DATABASE_SCHEMA.md: Local scope for device-specific data.
     */
    private val collection: Collection by lazy {
        val db = databaseProvider.getTypedDatabase()
        db.createCollection(
            DatabaseConfig.COLLECTION_OFFLINE_QUEUE,
            DatabaseConfig.SCOPE_LOCAL
        )
    }
    
    /**
     * ID generator for queue items.
     * Initialized from max ID in collection on first access.
     */
    private val idGenerator: AtomicLong by lazy {
        AtomicLong(getMaxIdFromCollection())
    }
    
    /**
     * Gets the maximum ID currently in the collection.
     */
    private fun getMaxIdFromCollection(): Long {
        return try {
            val query = QueryBuilder.select(SelectResult.property("id"))
                .from(DataSource.collection(collection))
            
            val results = query.execute()
            results.allResults()
                .mapNotNull { it.getLong("id") }
                .maxOrNull() ?: 0L
        } catch (e: Exception) {
            println("CouchbaseQueuePersistence: Error getting max ID - ${e.message}")
            0L
        }
    }
    
    override suspend fun save(item: QueuedItem): QueuedItem = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                val docId = "queue_${item.id}"
                val doc = MutableDocument(docId)
                
                doc.setLong("id", item.id)
                doc.setString("type", item.type.name)
                doc.setString("payload", item.payload)
                doc.setString("createdAt", item.createdAt.toString())
                doc.setInt("attempts", item.attempts)
                item.lastAttempt?.let { doc.setString("lastAttempt", it.toString()) }
                
                collection.save(doc)
                
                println("CouchbaseQueuePersistence: Saved item ${item.id}")
                item
            } catch (e: Exception) {
                println("CouchbaseQueuePersistence: Error saving item ${item.id} - ${e.message}")
                throw e
            }
        }
    }
    
    override suspend fun getAll(): List<QueuedItem> = withContext(Dispatchers.IO) {
        try {
            val query = QueryBuilder.select(SelectResult.all())
                .from(DataSource.collection(collection))
                .orderBy(Ordering.property("createdAt").ascending())
            
            val results = query.execute()
            results.allResults().mapNotNull { result ->
                val dict = result.getDictionary(DatabaseConfig.COLLECTION_OFFLINE_QUEUE)
                dict?.let { parseQueuedItem(it.toMap()) }
            }
        } catch (e: Exception) {
            println("CouchbaseQueuePersistence: Error getting all items - ${e.message}")
            emptyList()
        }
    }
    
    override suspend fun count(): Int = withContext(Dispatchers.IO) {
        try {
            collection.count.toInt()
        } catch (e: Exception) {
            println("CouchbaseQueuePersistence: Error counting items - ${e.message}")
            0
        }
    }
    
    override suspend fun delete(itemId: Long): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                val docId = "queue_$itemId"
                val doc = collection.getDocument(docId)
                if (doc != null) {
                    collection.delete(doc)
                    println("CouchbaseQueuePersistence: Deleted item $itemId")
                    true
                } else {
                    println("CouchbaseQueuePersistence: Item $itemId not found for deletion")
                    false
                }
            } catch (e: Exception) {
                println("CouchbaseQueuePersistence: Error deleting item $itemId - ${e.message}")
                false
            }
        }
    }
    
    override suspend fun update(item: QueuedItem): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                val docId = "queue_${item.id}"
                val existingDoc = collection.getDocument(docId) ?: return@withContext false
                
                val doc = existingDoc.toMutable()
                doc.setInt("attempts", item.attempts)
                item.lastAttempt?.let { doc.setString("lastAttempt", it.toString()) }
                
                collection.save(doc)
                println("CouchbaseQueuePersistence: Updated item ${item.id}")
                true
            } catch (e: Exception) {
                println("CouchbaseQueuePersistence: Error updating item ${item.id} - ${e.message}")
                false
            }
        }
    }
    
    override suspend fun clear() = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                // Get all documents and delete them
                val query = QueryBuilder.select(SelectResult.property("id"))
                    .from(DataSource.collection(collection))
                
                val results = query.execute()
                val ids = results.allResults().mapNotNull { it.getLong("id") }
                
                ids.forEach { id ->
                    val docId = "queue_$id"
                    collection.getDocument(docId)?.let { collection.delete(it) }
                }
                
                println("CouchbaseQueuePersistence: Cleared ${ids.size} items")
            } catch (e: Exception) {
                println("CouchbaseQueuePersistence: Error clearing queue - ${e.message}")
            }
        }
    }
    
    override suspend fun generateId(): Long {
        return idGenerator.incrementAndGet()
    }
    
    /**
     * Parses a map from Couchbase into a QueuedItem.
     */
    private fun parseQueuedItem(map: Map<String, Any?>): QueuedItem? {
        return try {
            val id = (map["id"] as? Number)?.toLong() ?: return null
            val typeStr = map["type"] as? String ?: return null
            val type = try { QueueItemType.valueOf(typeStr) } catch (e: Exception) { return null }
            val payload = map["payload"] as? String ?: return null
            val createdAtStr = map["createdAt"] as? String ?: return null
            val createdAt = try { Instant.parse(createdAtStr) } catch (e: Exception) { Clock.System.now() }
            val attempts = (map["attempts"] as? Number)?.toInt() ?: 0
            val lastAttemptStr = map["lastAttempt"] as? String
            val lastAttempt = lastAttemptStr?.let { 
                try { Instant.parse(it) } catch (e: Exception) { null }
            }
            
            QueuedItem(
                id = id,
                type = type,
                payload = payload,
                createdAt = createdAt,
                attempts = attempts,
                lastAttempt = lastAttempt
            )
        } catch (e: Exception) {
            println("CouchbaseQueuePersistence: Error parsing item - ${e.message}")
            null
        }
    }
}

