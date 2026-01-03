package com.unisight.gropos.features.device.domain.repository

import com.unisight.gropos.core.device.HardwareConfig

/**
 * Repository interface for local device configuration stored in Couchbase.
 * 
 * Per COUCHBASE_LOCAL_STORAGE.md: Reads from PosSystem collection in the `pos` scope.
 * Provides camera config, OnePay config, and other hardware settings.
 */
interface LocalDeviceConfigRepository {
    
    /**
     * Gets the hardware configuration from the PosSystem collection.
     * 
     * @param environment The environment to load config for ("Production", "Development")
     * @return The hardware config, or null if not found
     */
    suspend fun getHardwareConfig(environment: String = "Production"): HardwareConfig?
    
    /**
     * Gets the branch ID from the PosSystem collection.
     * 
     * @param environment The environment to load from
     * @return The branch ID, or null if not found
     */
    suspend fun getBranchId(environment: String = "Production"): Int?
    
    /**
     * Gets the branch name from the PosSystem collection.
     * 
     * @param environment The environment to load from
     * @return The branch name, or null if not found
     */
    suspend fun getBranchName(environment: String = "Production"): String?
    
    /**
     * Gets the API key from the PosSystem collection.
     * 
     * @param environment The environment to load from
     * @return The API key, or null if not registered
     */
    suspend fun getApiKey(environment: String = "Production"): String?
    
    /**
     * Gets the refresh token for session persistence.
     * 
     * @param environment The environment to load from
     * @return The refresh token, or null if not stored
     */
    suspend fun getRefreshToken(environment: String = "Production"): String?
    
    /**
     * Saves/updates the hardware configuration in the PosSystem collection.
     * 
     * @param config The hardware configuration to save
     * @param environment The environment to save to
     * @return Result indicating success or failure
     */
    suspend fun saveHardwareConfig(config: HardwareConfig, environment: String = "Production"): Result<Unit>
    
    /**
     * Saves the refresh token for session persistence.
     * 
     * @param refreshToken The refresh token to save
     * @param environment The environment to save to
     * @return Result indicating success or failure
     */
    suspend fun saveRefreshToken(refreshToken: String, environment: String = "Production"): Result<Unit>
}

