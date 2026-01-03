package com.unisight.gropos.features.settings.domain.repository

import com.unisight.gropos.features.settings.domain.model.BranchSetting
import com.unisight.gropos.features.settings.domain.model.BranchSettings

/**
 * Repository interface for branch settings.
 * 
 * Per COUCHBASE_LOCAL_STORAGE.md: Reads from PosBranchSettings collection in the `pos` scope.
 * Used for branch-specific configuration and feature flags.
 */
interface BranchSettingsRepository {
    
    /**
     * Gets all branch settings.
     * 
     * @return BranchSettings wrapper with all settings
     */
    suspend fun getAllSettings(): BranchSettings
    
    /**
     * Gets a specific setting by type.
     * 
     * @param type The setting type/key
     * @return The setting, or null if not found
     */
    suspend fun getSettingByType(type: String): BranchSetting?
    
    /**
     * Gets settings for a specific branch.
     * 
     * @param branchId The branch ID
     * @return List of settings for the branch
     */
    suspend fun getSettingsForBranch(branchId: Int): List<BranchSetting>
    
    /**
     * Saves or updates a setting.
     * 
     * @param setting The setting to save
     * @return Result indicating success or failure
     */
    suspend fun saveSetting(setting: BranchSetting): Result<Unit>
    
    /**
     * Refreshes settings from the database.
     * Useful after a sync operation.
     */
    suspend fun refreshSettings()
}

