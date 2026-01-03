package com.unisight.gropos.features.settings.data

import com.couchbase.lite.Collection
import com.couchbase.lite.DataSource
import com.couchbase.lite.Expression
import com.couchbase.lite.MutableDocument
import com.couchbase.lite.QueryBuilder
import com.couchbase.lite.SelectResult
import com.unisight.gropos.core.database.DatabaseConfig
import com.unisight.gropos.core.database.DatabaseProvider
import com.unisight.gropos.features.settings.data.dto.LegacyBranchSettingDto
import com.unisight.gropos.features.settings.domain.model.BranchSetting
import com.unisight.gropos.features.settings.domain.model.BranchSettings
import com.unisight.gropos.features.settings.domain.repository.BranchSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * CouchbaseLite implementation of BranchSettingsRepository for Android.
 * 
 * Per COUCHBASE_LOCAL_STORAGE.md:
 * - Collection: PosBranchSettings in scope "pos"
 * - Used for branch-specific configuration
 * 
 * Per BACKEND_INTEGRATION_STATUS.md:
 * - Uses LegacyBranchSettingDto for parsing legacy JSON structure
 * - Reads from legacy "pos" scope
 * 
 * Includes in-memory caching for performance.
 */
class CouchbaseBranchSettingsRepository(
    private val databaseProvider: DatabaseProvider
) : BranchSettingsRepository {
    
    /**
     * PosBranchSettings collection in "pos" scope.
     */
    private val collection: Collection by lazy {
        val db = databaseProvider.getTypedDatabase()
        db.createCollection(
            DatabaseConfig.COLLECTION_POS_BRANCH_SETTINGS,
            DatabaseConfig.SCOPE_POS
        )
    }
    
    // In-memory cache for performance
    private var cachedSettings: BranchSettings? = null
    private val cacheMutex = Mutex()
    
    override suspend fun getAllSettings(): BranchSettings = withContext(Dispatchers.IO) {
        // Return cached if available
        cacheMutex.withLock {
            cachedSettings?.let { return@withContext it }
        }
        
        try {
            val query = QueryBuilder.select(SelectResult.all())
                .from(DataSource.collection(collection))
            
            val results = query.execute()
            val settingsList = results.allResults().mapNotNull { result ->
                val map = result.getDictionary(DatabaseConfig.COLLECTION_POS_BRANCH_SETTINGS)?.toMap()
                map?.let { LegacyBranchSettingDto.fromMap(it)?.toDomain() }
            }
            
            val settings = BranchSettings.fromList(settingsList)
            
            cacheMutex.withLock {
                cachedSettings = settings
            }
            
            settings
        } catch (e: Exception) {
            android.util.Log.e("BranchSettingsRepo", "Error getting all settings: ${e.message}")
            BranchSettings.empty()
        }
    }
    
    override suspend fun getSettingByType(type: String): BranchSetting? = withContext(Dispatchers.IO) {
        // Try cache first
        getAllSettings().getSetting(type)
    }
    
    override suspend fun getSettingsForBranch(branchId: Int): List<BranchSetting> = withContext(Dispatchers.IO) {
        try {
            val query = QueryBuilder.select(SelectResult.all())
                .from(DataSource.collection(collection))
                .where(Expression.property("branchId").equalTo(Expression.intValue(branchId)))
            
            val results = query.execute()
            results.allResults().mapNotNull { result ->
                val map = result.getDictionary(DatabaseConfig.COLLECTION_POS_BRANCH_SETTINGS)?.toMap()
                map?.let { LegacyBranchSettingDto.fromMap(it)?.toDomain() }
            }
        } catch (e: Exception) {
            android.util.Log.e("BranchSettingsRepo", "Error getting settings for branch: ${e.message}")
            emptyList()
        }
    }
    
    override suspend fun saveSetting(setting: BranchSetting): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val doc = MutableDocument(setting.id.toString())
            
            doc.setInt("id", setting.id)
            doc.setString("type", setting.type)
            doc.setString("value", setting.value)
            setting.description?.let { doc.setString("description", it) }
            setting.branchId?.let { doc.setInt("branchId", it) }
            
            collection.save(doc)
            
            // Invalidate cache
            cacheMutex.withLock {
                cachedSettings = null
            }
            
            android.util.Log.d("BranchSettingsRepo", "Saved setting ${setting.type}")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("BranchSettingsRepo", "Error saving setting: ${e.message}")
            Result.failure(e)
        }
    }
    
    override suspend fun refreshSettings() {
        cacheMutex.withLock {
            cachedSettings = null
        }
        getAllSettings()
    }
}

/**
 * Extension function to get typed Database from DatabaseProvider.
 */
private fun DatabaseProvider.getTypedDatabase(): com.couchbase.lite.Database {
    return this.getDatabase() as com.couchbase.lite.Database
}

