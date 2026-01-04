package com.unisight.gropos.core.sync

import com.unisight.gropos.core.network.ApiClient
import io.ktor.client.request.get
import io.ktor.client.request.url
import io.ktor.http.HttpMethod
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Production implementation of SyncEngine.
 * 
 * **Per SYNC_MECHANISM.md:**
 * - Pings server via heartbeat endpoint to check connectivity
 * - Downloads updated data (products, categories, taxes)
 * - Tracks last download time for incremental sync
 * 
 * **Per API.md - Heartbeat:**
 * - GET /heartbeat on APIM gateway (baseUrl)
 * - Returns status to confirm server is reachable
 * 
 * **Per DEVICE_REGISTRATION.md:**
 * - Heartbeat maintains device presence with server
 * - Downloads pending updates for the device
 */
class DefaultSyncEngine(
    private val apiClient: ApiClient,
    private val productSyncService: ProductSyncService
) : SyncEngine {
    
    private var _lastDownloadTime: Instant? = null
    
    /**
     * Ping the server to check connectivity.
     * 
     * **Per SYNC_MECHANISM.md:** Uses GET /heartbeat on APIM gateway.
     * 
     * @return true if server is reachable, false otherwise
     */
    override suspend fun ping(): Boolean {
        println("[DefaultSyncEngine] Pinging server...")
        
        return try {
            // Try a simple request to check connectivity
            // Using the base APIM gateway URL for heartbeat
            val result = apiClient.request<String> {
                method = HttpMethod.Get
                url(apiClient.config.baseUrl + "/heartbeat")
            }
            
            val isSuccess = result.isSuccess
            println("[DefaultSyncEngine] Ping result: $isSuccess")
            isSuccess
        } catch (e: Exception) {
            println("[DefaultSyncEngine] Ping failed: ${e.message}")
            false
        }
    }
    
    /**
     * Download updated data from server.
     * 
     * **Per SYNC_MECHANISM.md:**
     * - Syncs products with pagination
     * - Updates local database
     * - Tracks sync timestamp
     * 
     * **Current Implementation:**
     * - Product sync via ProductSyncService
     * - Future: Categories, Taxes, CRV, etc.
     * 
     * @return SyncResult indicating success/failure and items synced
     */
    override suspend fun downloadUpdates(): SyncResult {
        println("[DefaultSyncEngine] Starting download updates...")
        
        val errors = mutableListOf<String>()
        var totalSynced = 0
        
        try {
            // Step 1: Sync products
            println("[DefaultSyncEngine] Syncing products...")
            val productResult = productSyncService.syncAllProducts()
            
            productResult.fold(
                onSuccess = { count ->
                    println("[DefaultSyncEngine] Products synced: $count")
                    totalSynced += count
                },
                onFailure = { error ->
                    println("[DefaultSyncEngine] Product sync failed: ${error.message}")
                    errors.add("Products: ${error.message}")
                }
            )
            
            // TODO: Step 2: Sync categories
            // TODO: Step 3: Sync taxes
            // TODO: Step 4: Sync CRV rates
            // TODO: Step 5: Sync customer groups
            
            // Update last download time on success
            if (errors.isEmpty()) {
                _lastDownloadTime = Clock.System.now()
            }
            
            val success = errors.isEmpty()
            println("[DefaultSyncEngine] Download complete. Success: $success, Items: $totalSynced")
            
            return SyncResult(
                success = success,
                itemsSynced = totalSynced,
                errors = errors
            )
            
        } catch (e: Exception) {
            println("[DefaultSyncEngine] Download failed with exception: ${e.message}")
            return SyncResult(
                success = false,
                itemsSynced = totalSynced,
                errors = listOf(e.message ?: "Unknown error")
            )
        }
    }
    
    /**
     * Get the timestamp of the last successful download.
     * 
     * Used for incremental sync - only fetch updates since this time.
     * 
     * @return Last download timestamp, or null if never synced
     */
    override fun getLastDownloadTime(): Instant? {
        return _lastDownloadTime
    }
}

