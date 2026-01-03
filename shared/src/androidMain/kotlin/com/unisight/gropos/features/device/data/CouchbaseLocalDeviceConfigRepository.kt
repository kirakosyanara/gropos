package com.unisight.gropos.features.device.data

import com.couchbase.lite.Collection
import com.couchbase.lite.MutableDocument
import com.unisight.gropos.core.database.DatabaseConfig
import com.unisight.gropos.core.database.DatabaseProvider
import com.unisight.gropos.core.device.HardwareConfig
import com.unisight.gropos.features.device.data.dto.LegacyPosSystemDto
import com.unisight.gropos.features.device.domain.repository.LocalDeviceConfigRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * CouchbaseLite implementation of LocalDeviceConfigRepository for Android.
 * 
 * Per COUCHBASE_LOCAL_STORAGE.md:
 * - Collection: PosSystem in scope "pos"
 * - Document ID: Environment name ("Production", "Development")
 * - Contains camera config, OnePay config, and device registration info
 * 
 * Per BACKEND_INTEGRATION_STATUS.md:
 * - Uses LegacyPosSystemDto for parsing legacy JSON structure
 * - Reads from legacy "pos" scope
 */
class CouchbaseLocalDeviceConfigRepository(
    private val databaseProvider: DatabaseProvider
) : LocalDeviceConfigRepository {
    
    /**
     * PosSystem collection in "pos" scope.
     */
    private val collection: Collection by lazy {
        val db = databaseProvider.getTypedDatabase()
        db.createCollection(
            DatabaseConfig.COLLECTION_POS_SYSTEM,
            DatabaseConfig.SCOPE_POS
        )
    }
    
    override suspend fun getHardwareConfig(environment: String): HardwareConfig? = withContext(Dispatchers.IO) {
        try {
            val doc = collection.getDocument(environment)
            doc?.let {
                LegacyPosSystemDto.fromMap(it.toMap())?.toHardwareConfig()
            }
        } catch (e: Exception) {
            android.util.Log.e("LocalDeviceConfigRepo", "Error getting hardware config: ${e.message}")
            null
        }
    }
    
    override suspend fun getBranchId(environment: String): Int? = withContext(Dispatchers.IO) {
        try {
            val doc = collection.getDocument(environment)
            doc?.getInt("branchId")?.takeIf { it > 0 }
        } catch (e: Exception) {
            android.util.Log.e("LocalDeviceConfigRepo", "Error getting branch ID: ${e.message}")
            null
        }
    }
    
    override suspend fun getBranchName(environment: String): String? = withContext(Dispatchers.IO) {
        try {
            val doc = collection.getDocument(environment)
            doc?.getString("branchName")
        } catch (e: Exception) {
            android.util.Log.e("LocalDeviceConfigRepo", "Error getting branch name: ${e.message}")
            null
        }
    }
    
    override suspend fun getApiKey(environment: String): String? = withContext(Dispatchers.IO) {
        try {
            val doc = collection.getDocument(environment)
            doc?.getString("apiKey")
        } catch (e: Exception) {
            android.util.Log.e("LocalDeviceConfigRepo", "Error getting API key: ${e.message}")
            null
        }
    }
    
    override suspend fun getRefreshToken(environment: String): String? = withContext(Dispatchers.IO) {
        try {
            val doc = collection.getDocument(environment)
            doc?.getString("refreshToken")
        } catch (e: Exception) {
            android.util.Log.e("LocalDeviceConfigRepo", "Error getting refresh token: ${e.message}")
            null
        }
    }
    
    override suspend fun saveHardwareConfig(config: HardwareConfig, environment: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val existingDoc = collection.getDocument(environment)
            val doc = existingDoc?.toMutable() ?: MutableDocument(environment)
            
            // Camera config
            config.cameraIp?.let { doc.setString("ipAddress", it) }
            config.cameraEntityId?.let { doc.setInt("entityId", it) }
            config.cameraId?.let { doc.setInt("cameraId", it) }
            
            // OnePay config
            config.onePayIp?.let { doc.setString("onePayIpAddress", it) }
            config.onePayEntityId?.let { doc.setInt("onePayEntityId", it) }
            config.onePayId?.let { doc.setInt("onePayId", it) }
            
            collection.save(doc)
            android.util.Log.d("LocalDeviceConfigRepo", "Saved hardware config for $environment")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("LocalDeviceConfigRepo", "Error saving hardware config: ${e.message}")
            Result.failure(e)
        }
    }
    
    override suspend fun saveRefreshToken(refreshToken: String, environment: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val existingDoc = collection.getDocument(environment)
            val doc = existingDoc?.toMutable() ?: MutableDocument(environment)
            
            doc.setString("refreshToken", refreshToken)
            
            collection.save(doc)
            android.util.Log.d("LocalDeviceConfigRepo", "Saved refresh token for $environment")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("LocalDeviceConfigRepo", "Error saving refresh token: ${e.message}")
            Result.failure(e)
        }
    }
}

/**
 * Extension function to get typed Database from DatabaseProvider.
 */
private fun DatabaseProvider.getTypedDatabase(): com.couchbase.lite.Database {
    return this.getDatabase() as com.couchbase.lite.Database
}

