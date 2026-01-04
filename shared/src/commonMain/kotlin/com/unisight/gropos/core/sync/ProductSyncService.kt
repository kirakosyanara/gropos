package com.unisight.gropos.core.sync

import com.unisight.gropos.core.network.ApiClient
import com.unisight.gropos.features.checkout.data.dto.ProductApiDto
import com.unisight.gropos.features.checkout.data.dto.ProductApiDtoMapper.toDomainList
import com.unisight.gropos.features.checkout.domain.model.Product
import com.unisight.gropos.features.checkout.domain.repository.ProductRepository
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Service for synchronizing products from backend API to local Couchbase database.
 * 
 * **Per pos.json (Swagger spec):**
 * - Uses GET /api/Product/GetAll for product sync
 * - Parameters: minid (page number, 0-based), count (page size, default 1000)
 * - Saves each product to CouchbaseLite
 * - Handles pagination for large catalogs
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
        // Per pos.json: GET /api/Product/GetAll
        private const val ENDPOINT_PRODUCTS = "/api/Product/GetAll"
        // Per pos.json: Default count is 1000, but we use 100 for progress visibility
        private const val PAGE_SIZE = 100
        // Per pos.json description: "PageNumber: first page is 1" (NOT 0-based!)
        private const val FIRST_PAGE_NUMBER = 1
        // Per reliability-stability.mdc: Bulk operations may need longer timeouts
        // Product catalogs can be large - 120 seconds is similar to payment terminal timeout
        private const val SYNC_TIMEOUT_MS = 120_000L
    }
    
    private val _syncProgress = MutableStateFlow(ProductSyncProgress())
    val syncProgress: StateFlow<ProductSyncProgress> = _syncProgress.asStateFlow()
    
    /**
     * Sync all products from backend to local database.
     * 
     * Uses pagination (minid/count) to handle large catalogs efficiently.
     * 
     * @return Result with total number of products synced
     */
    suspend fun syncAllProducts(): Result<Int> {
        _syncProgress.value = ProductSyncProgress(isActive = true, phase = "Starting...")
        
        var totalSynced = 0
        var pageNumber = FIRST_PAGE_NUMBER  // Backend uses 1-based page numbers (minid=1 is first page)
        var hasMore = true
        
        return try {
            while (hasMore) {
                _syncProgress.value = ProductSyncProgress(
                    isActive = true,
                    phase = "Fetching page $pageNumber...",
                    productsSynced = totalSynced
                )
                
                // Fetch page from API using minid/count pagination
                val pageResult = fetchProductPage(pageNumber)
                
                pageResult.fold(
                    onSuccess = { products ->
                        if (products.isEmpty()) {
                            hasMore = false
                            println("[ProductSyncService] Page $pageNumber returned 0 products - done")
                        } else {
                            println("[ProductSyncService] Page $pageNumber returned ${products.size} products")
                            
                            // Save products to local database
                            val saveResult = saveProducts(products)
                            totalSynced += saveResult
                            
                            // Check if there are more pages
                            if (products.size < PAGE_SIZE) {
                                hasMore = false
                                println("[ProductSyncService] Last page (${products.size} < $PAGE_SIZE)")
                            } else {
                                // Move to next page
                                pageNumber++
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
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    /**
     * Fetch a single page of products from the API.
     * 
     * **Per COUCHBASE_SYNCHRONIZATION_DETAILED.md Section 5.3:**
     * The API may return either:
     * - Direct array on success: [{ product1 }, { product2 }, ...]
     * - Error object on failure: { "Success": null, "Message": {...} }
     * 
     * **Per pos.json:** GET /api/Product/GetAll?minid={page}&count={size}
     * - minid: Page number (0-based)
     * - count: Number of items per page
     * 
     * **Per API.md:** Headers x-api-key, version: v1 added by ApiClient
     */
    private suspend fun fetchProductPage(pageNumber: Int): Result<List<Product>> {
        return try {
            // Build URL with query parameters per pos.json spec
            val fullUrl = apiClient.config.posApiBaseUrl + ENDPOINT_PRODUCTS
            println("[ProductSyncService] Fetching from: $fullUrl (page=$pageNumber, count=$PAGE_SIZE)")
            
            // Use low-level request to get raw response for debugging
            val httpResponse = apiClient.httpClient.get(fullUrl) {
                // Add authentication headers
                apiClient.apiKeyProvider()?.let { apiKey ->
                    headers.append("x-api-key", apiKey)
                    println("[ProductSyncService] Using API key: ${apiKey.take(8)}...")
                }
                apiClient.tokenProvider()?.let { token ->
                    headers.append("Authorization", "Bearer $token")
                }
                headers.append("version", "v1")
                
                // Query parameters
                parameter("minid", pageNumber)
                parameter("count", PAGE_SIZE)
                
                // Extend timeout for bulk data operations
                timeout {
                    requestTimeoutMillis = SYNC_TIMEOUT_MS
                    socketTimeoutMillis = SYNC_TIMEOUT_MS
                }
            }
            
            val status = httpResponse.status
            println("[ProductSyncService] Response status: $status")
            
            if (!status.isSuccess()) {
                // Read raw body to see the error message
                val rawBody = httpResponse.bodyAsText()
                println("[ProductSyncService] ERROR Response body: $rawBody")
                
                // Try to parse as error response
                try {
                    val errorResponse = json.decodeFromString<ApiErrorResponse>(rawBody)
                    val errorMsg = errorResponse.message?.message ?: "Unknown error"
                    println("[ProductSyncService] Parsed error message: $errorMsg")
                    return Result.failure(Exception("API Error ($status): $errorMsg"))
                } catch (e: Exception) {
                    return Result.failure(Exception("API Error ($status): $rawBody"))
                }
            }
            
            // Parse successful response - API returns wrapper object like Employee API
            // Per API response: {"success":[{...}, {...}], ...}
            val wrapper: ProductListResponse = httpResponse.body()
            val products = wrapper.success
            println("[ProductSyncService] Received ${products.size} products from API")
            
            Result.success(products.toDomainList())
        } catch (e: Exception) {
            println("[ProductSyncService] Exception during fetch: ${e::class.simpleName} - ${e.message}")
            e.printStackTrace()
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
        
        println("[ProductSyncService] Saved $successCount/${products.size} products to database")
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

/**
 * Wrapper for Product API success responses.
 * 
 * **Per API response format analysis:**
 * Server returns: { "success": [{ product1 }, { product2 }, ...] }
 * NOT a direct array like the Swagger spec suggests.
 * 
 * This matches the Employee API pattern (EmployeeListResponse).
 */
@Serializable
data class ProductListResponse(
    @SerialName("success")
    val success: List<ProductApiDto> = emptyList()
)

/**
 * Wrapper for API error responses.
 * 
 * Server returns: { "Success": null, "Message": { "Message": "...", ... } }
 * on error.
 */
@Serializable
data class ApiErrorResponse(
    @SerialName("Success")
    val success: String? = null,
    @SerialName("Message")
    val message: ApiErrorMessage? = null
)

@Serializable
data class ApiErrorMessage(
    @SerialName("Message")
    val message: String? = null,
    @SerialName("Details")
    val details: String? = null
)
