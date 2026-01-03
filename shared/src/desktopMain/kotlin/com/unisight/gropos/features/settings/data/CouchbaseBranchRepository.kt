package com.unisight.gropos.features.settings.data

import com.couchbase.lite.Collection
import com.couchbase.lite.DataSource
import com.couchbase.lite.Expression
import com.couchbase.lite.QueryBuilder
import com.couchbase.lite.SelectResult
import com.unisight.gropos.core.database.DatabaseConfig
import com.unisight.gropos.core.database.DatabaseProvider
import com.unisight.gropos.features.settings.data.dto.LegacyBranchDto
import com.unisight.gropos.features.settings.domain.model.Branch
import com.unisight.gropos.features.settings.domain.repository.BranchRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * CouchbaseLite implementation of BranchRepository for Desktop (JVM).
 * 
 * Per COUCHBASE_LOCAL_STORAGE.md:
 * - Collection: Branch in scope "pos"
 * - Used for store name, address, and configuration
 * 
 * Per BACKEND_INTEGRATION_STATUS.md:
 * - Uses LegacyBranchDto for parsing legacy JSON structure
 * - Reads from legacy "pos" scope
 * 
 * Includes in-memory caching for performance.
 */
class CouchbaseBranchRepository(
    private val databaseProvider: DatabaseProvider
) : BranchRepository {
    
    /**
     * Branch collection in "pos" scope.
     */
    private val collection: Collection by lazy {
        val db = databaseProvider.getTypedDatabase()
        db.createCollection(
            DatabaseConfig.COLLECTION_BRANCH,
            DatabaseConfig.SCOPE_POS
        )
    }
    
    // In-memory cache for performance
    private var cachedBranches: Map<Int, Branch>? = null
    private val cacheMutex = Mutex()
    
    // Current branch ID (loaded from PosSystem or device registration)
    private var currentBranchId: Int? = null
    
    override suspend fun getAllBranches(): List<Branch> = withContext(Dispatchers.IO) {
        // Return cached if available
        cacheMutex.withLock {
            cachedBranches?.values?.toList()?.let { return@withContext it }
        }
        
        try {
            val query = QueryBuilder.select(SelectResult.all())
                .from(DataSource.collection(collection))
            
            val results = query.execute()
            val branchList = results.allResults().mapNotNull { result ->
                val map = result.getDictionary(DatabaseConfig.COLLECTION_BRANCH)?.toMap()
                map?.let { LegacyBranchDto.fromMap(it)?.toDomain() }
            }
            
            val branchMap = branchList.associateBy { it.id }
            
            cacheMutex.withLock {
                cachedBranches = branchMap
            }
            
            branchList
        } catch (e: Exception) {
            println("CouchbaseBranchRepository: Error getting all branches - ${e.message}")
            emptyList()
        }
    }
    
    override suspend fun getBranchById(branchId: Int): Branch? = withContext(Dispatchers.IO) {
        // Try cache first
        cacheMutex.withLock {
            cachedBranches?.get(branchId)?.let { return@withContext it }
        }
        
        try {
            val query = QueryBuilder.select(SelectResult.all())
                .from(DataSource.collection(collection))
                .where(Expression.property("id").equalTo(Expression.intValue(branchId)))
            
            val result = query.execute().allResults().firstOrNull()
            val map = result?.getDictionary(DatabaseConfig.COLLECTION_BRANCH)?.toMap()
            map?.let { LegacyBranchDto.fromMap(it)?.toDomain() }
        } catch (e: Exception) {
            println("CouchbaseBranchRepository: Error getting branch by ID - ${e.message}")
            null
        }
    }
    
    override suspend fun getCurrentBranch(): Branch? = withContext(Dispatchers.IO) {
        // If we have a current branch ID, use it
        currentBranchId?.let { id ->
            return@withContext getBranchById(id)
        }
        
        // Try to get from PosSystem collection
        try {
            val branchId = loadCurrentBranchIdFromPosSystem()
            if (branchId != null) {
                currentBranchId = branchId
                return@withContext getBranchById(branchId)
            }
        } catch (e: Exception) {
            println("CouchbaseBranchRepository: Error getting current branch - ${e.message}")
        }
        
        // Fall back to first active branch
        getAllBranches().firstOrNull { it.isActive }
    }
    
    override suspend fun refreshBranches() {
        cacheMutex.withLock {
            cachedBranches = null
        }
        getAllBranches()
    }
    
    /**
     * Loads the current branch ID from PosSystem collection.
     */
    private fun loadCurrentBranchIdFromPosSystem(): Int? {
        return try {
            val db = databaseProvider.getTypedDatabase()
            val posSystemCollection = db.createCollection(
                DatabaseConfig.COLLECTION_POS_SYSTEM,
                DatabaseConfig.SCOPE_POS
            )
            
            // Try Production environment first
            val doc = posSystemCollection.getDocument("Production")
                ?: posSystemCollection.getDocument("Development")
            
            doc?.getInt("branchId")?.takeIf { it > 0 }
        } catch (e: Exception) {
            println("CouchbaseBranchRepository: Error loading branch ID from PosSystem - ${e.message}")
            null
        }
    }
    
    /**
     * Sets the current branch ID (called after device registration).
     */
    fun setCurrentBranchId(branchId: Int) {
        currentBranchId = branchId
    }
}

/**
 * Extension function to get typed Database from DatabaseProvider.
 */
private fun DatabaseProvider.getTypedDatabase(): com.couchbase.lite.Database {
    return this.getDatabase() as com.couchbase.lite.Database
}

