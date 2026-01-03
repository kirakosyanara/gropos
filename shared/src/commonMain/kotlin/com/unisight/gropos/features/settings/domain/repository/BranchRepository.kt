package com.unisight.gropos.features.settings.domain.repository

import com.unisight.gropos.features.settings.domain.model.Branch

/**
 * Repository interface for branch/store data.
 * 
 * Per COUCHBASE_LOCAL_STORAGE.md: Reads from Branch collection in the `pos` scope.
 * Used for getting store name, address, and configuration dynamically.
 */
interface BranchRepository {
    
    /**
     * Gets all branches.
     * 
     * @return List of all branches
     */
    suspend fun getAllBranches(): List<Branch>
    
    /**
     * Gets a branch by ID.
     * 
     * @param branchId The branch ID
     * @return The branch, or null if not found
     */
    suspend fun getBranchById(branchId: Int): Branch?
    
    /**
     * Gets the current branch (the one this device is registered to).
     * Uses cached device registration info to determine current branch.
     * 
     * @return The current branch, or null if not found
     */
    suspend fun getCurrentBranch(): Branch?
    
    /**
     * Refreshes branch data from the database.
     */
    suspend fun refreshBranches()
}

