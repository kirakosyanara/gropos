package com.unisight.gropos.core.sync

import com.unisight.gropos.core.network.ApiClient
import com.unisight.gropos.features.checkout.data.dto.ProductApiDto
import com.unisight.gropos.features.checkout.data.dto.ProductApiDtoMapper.toDomainList
import com.unisight.gropos.features.checkout.domain.model.Product
import com.unisight.gropos.features.checkout.domain.repository.ProductRepository
import io.ktor.http.HttpMethod
import io.ktor.http.path
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Service for synchronizing products from backend API to local Couchbase database.
 * 
 * **Per SYNC_MECHANISM.md Section "Initial Data Load":**
 * - Uses paginated API calls: GET /product?offset=&limit=100
 * - Saves each product to CouchbaseLite
 * - Continues until no more data
 * 
 * **Per DEVICE_REGISTRATION.md Section 7:**
 * - Called after successful device registration
 * - Show "Initializing Database Please Wait..." during sync
 * 
 * **Per API.md Authentication Section:**
 * - x-api-key: Device API key from registration (added by ApiClient)
 * - version: v1 header (added by ApiClient)
 */
class ProductSyncService(
    private val apiClient: ApiClient,
    private val productRepository: ProductRepository
) {
    companion object {
        private const val ENDPOINT_PRODUCTS = "/product"
        private const val PAGE_SIZE = 100
    }
    
    private val _syncProgress = MutableStateFlow(ProductSyncProgress())
    val syncProgress: StateFlow<ProductSyncProgress> = _syncProgress.asStateFlow()
    
    /**
     * Sync all products from backend to local database.
     * 
     * Uses pagination to handle large catalogs efficiently.
     * 
     * @return Result with total number of products synced
     */
    suspend fun syncAllProducts(): Result<Int> {
        _syncProgress.value = ProductSyncProgress(isActive = true, phase = "Starting...")
        
        var totalSynced = 0
        var offset = ""
        var hasMore = true
        var pageNumber = 0
        
        return try {
            while (hasMore) {
                pageNumber++
                _syncProgress.value = ProductSyncProgress(
                    isActive = true,
                    phase = "Fetching page $pageNumber...",
                    productsSynced = totalSynced
                )
                
                // Fetch page from API
                val pageResult = fetchProductPage(offset)
                
                pageResult.fold(
                    onSuccess = { products ->
                        if (products.isEmpty()) {
                            hasMore = false
                        } else {
                            // Save products to local database
                            val saveResult = saveProducts(products)
                            totalSynced += saveResult
                            
                            // Check if there are more pages
                            if (products.size < PAGE_SIZE) {
                                hasMore = false
                            } else {
                                // Use last product ID as offset for next page
                                offset = products.last().branchProductId.toString()
                            }
                            
                            _syncProgress.value = ProductSyncProgress(
                                isActive = true,
                                phase = "Saved $totalSynced products...",
                                productsSynced = totalSynced
                            )
                        }
                    },
                    onFailure = { error ->
                        println("[ProductSyncService] Error fetching page $pageNumber: ${error.message}")
                        hasMore = false
                        return Result.failure(error)
                    }
                )
            }
            
            _syncProgress.value = ProductSyncProgress(
                isActive = false,
                phase = "Complete",
                productsSynced = totalSynced
            )
            
            println("[ProductSyncService] Sync complete: $totalSynced products")
            Result.success(totalSynced)
            
        } catch (e: Exception) {
            _syncProgress.value = ProductSyncProgress(
                isActive = false,
                phase = "Error: ${e.message}",
                productsSynced = totalSynced
            )
            println("[ProductSyncService] Sync failed: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Fetch a single page of products from the API.
     * 
     * **Per SYNC_MECHANISM.md:** GET /product?offset=&limit=100
     * **Per API.md:** Headers x-api-key, version: v1 added by ApiClient
     */
    private suspend fun fetchProductPage(offset: String): Result<List<Product>> {
        return try {
            // Use the new request method which adds x-api-key header dynamically
            // NOTE: Use pathSegments to set the path on the base URL
            val response = apiClient.request<List<ProductApiDto>> {
                method = HttpMethod.Get
                url.pathSegments = ENDPOINT_PRODUCTS.split("/").filter { it.isNotEmpty() }
                if (offset.isNotEmpty()) {
                    url.parameters.append("offset", offset)
                }
                url.parameters.append("limit", PAGE_SIZE.toString())
            }
            
            response.map { dtos ->
                dtos.toDomainList()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Save products to local database.
     * 
     * @return Number of products successfully saved
     */
    private suspend fun saveProducts(products: List<Product>): Int {
        var successCount = 0
        
        for (product in products) {
            try {
                val success = productRepository.insertProduct(product)
                if (success) {
                    successCount++
                }
            } catch (e: Exception) {
                println("[ProductSyncService] Failed to save product ${product.branchProductId}: ${e.message}")
            }
        }
        
        return successCount
    }
    
    /**
     * Check if product database is empty and needs initial sync.
     * 
     * @return true if sync is needed
     */
    suspend fun isSyncNeeded(): Boolean {
        return try {
            productRepository.getProductCount() == 0L
        } catch (e: Exception) {
            true // Assume sync needed on error
        }
    }
}

/**
 * Progress information for product sync.
 */
data class ProductSyncProgress(
    val isActive: Boolean = false,
    val phase: String = "",
    val productsSynced: Int = 0
)

