package com.unisight.gropos.core.sync

import com.unisight.gropos.core.network.ApiClient
import com.unisight.gropos.features.checkout.data.dto.LookupGroupDto
import com.unisight.gropos.features.checkout.data.dto.LookupGroupItemDto
import com.unisight.gropos.features.checkout.domain.model.LookupCategoryWithItems
import com.unisight.gropos.features.checkout.domain.model.LookupProduct
import com.unisight.gropos.features.checkout.domain.repository.LookupCategoryRepository
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json

/**
 * Lookup Category Sync Service.
 * 
 * Per LOOKUP_TABLE.md - Data Population Flow:
 * - Calls /api/posLookUpCategory/GetAllForPOS with pagination
 * - Saves each LookupGroupResponse to Couchbase PosLookupCategory collection
 * - Uses minId (1-based page number) and count (page size) parameters
 */
class LookupCategorySyncService(
    private val apiClient: ApiClient,
    private val lookupCategoryRepository: LookupCategoryRepository
) {
    
    companion object {
        private const val ENDPOINT_LOOKUP_CATEGORIES = "/api/posLookUpCategory/GetAllForPOS"
        private const val PAGE_SIZE = 250
        private const val SYNC_TIMEOUT_MS = 60_000L
    }
    
    private val json = Json { ignoreUnknownKeys = true }
    
    /**
     * Progress state for sync operations.
     */
    data class LookupSyncProgress(
        val isActive: Boolean = false,
        val totalCategories: Int = 0,
        val savedCategories: Int = 0,
        val phase: String = ""
    )
    
    private val _syncProgress = MutableStateFlow(LookupSyncProgress())
    val syncProgress: StateFlow<LookupSyncProgress> = _syncProgress
    
    /**
     * Syncs all lookup categories from the API.
     * 
     * Per LOOKUP_TABLE.md - Data Population Flow:
     * 1. Paginated API calls to GetAllForPOS
     * 2. Convert DTOs to domain models
     * 3. Save to Couchbase using upsert
     * 
     * @return Result with total categories synced
     */
    suspend fun syncAllCategories(): Result<Int> {
        _syncProgress.value = LookupSyncProgress(isActive = true, phase = "Starting...")
        
        var totalSynced = 0
        var pageNumber = 1  // API uses 1-based pagination
        var hasMore = true
        
        return try {
            while (hasMore) {
                _syncProgress.value = _syncProgress.value.copy(
                    phase = "Fetching page $pageNumber..."
                )
                
                val pageResult = fetchCategoryPage(pageNumber)
                
                pageResult.fold(
                    onSuccess = { categories ->
                        if (categories.isEmpty()) {
                            hasMore = false
                            println("[LookupCategorySyncService] Page $pageNumber empty, sync complete")
                        } else {
                            // Convert and save
                            val savedCount = lookupCategoryRepository.saveCategories(categories)
                            totalSynced += savedCount
                            
                            _syncProgress.value = _syncProgress.value.copy(
                                savedCategories = totalSynced,
                                phase = "Saved $totalSynced categories..."
                            )
                            
                            println("[LookupCategorySyncService] Page $pageNumber: ${categories.size} categories, $savedCount saved")
                            
                            // Check if more pages
                            hasMore = categories.size >= PAGE_SIZE
                            pageNumber++
                        }
                    },
                    onFailure = { error ->
                        println("[LookupCategorySyncService] Page $pageNumber failed: ${error.message}")
                        _syncProgress.value = _syncProgress.value.copy(
                            isActive = false,
                            phase = "Error: ${error.message}"
                        )
                        return Result.failure(error)
                    }
                )
            }
            
            _syncProgress.value = _syncProgress.value.copy(
                isActive = false,
                totalCategories = totalSynced,
                phase = "Complete: $totalSynced categories"
            )
            
            println("[LookupCategorySyncService] Sync complete: $totalSynced categories")
            Result.success(totalSynced)
            
        } catch (e: Exception) {
            println("[LookupCategorySyncService] Sync failed: ${e.message}")
            e.printStackTrace()
            _syncProgress.value = _syncProgress.value.copy(
                isActive = false,
                phase = "Failed: ${e.message}"
            )
            Result.failure(e)
        }
    }
    
    /**
     * Fetches a single page of lookup categories from the API.
     * 
     * Per LOOKUP_TABLE.md - API Reference:
     * - Endpoint: /api/posLookUpCategory/GetAllForPOS
     * - Parameters: minId (page number), count (page size)
     */
    private suspend fun fetchCategoryPage(pageNumber: Int): Result<List<LookupCategoryWithItems>> {
        return try {
            val fullUrl = apiClient.config.posApiBaseUrl + ENDPOINT_LOOKUP_CATEGORIES
            println("[LookupCategorySyncService] Fetching from: $fullUrl (page=$pageNumber, count=$PAGE_SIZE)")
            
            val httpResponse = apiClient.httpClient.get(fullUrl) {
                apiClient.apiKeyProvider()?.let { apiKey ->
                    headers.append("x-api-key", apiKey)
                }
                apiClient.tokenProvider()?.let { token ->
                    headers.append("Authorization", "Bearer $token")
                }
                headers.append("version", "v1")
                
                parameter("minId", pageNumber)
                parameter("count", PAGE_SIZE)
            }
            
            val status = httpResponse.status
            println("[LookupCategorySyncService] Response status: $status")
            
            if (!status.isSuccess()) {
                val rawBody = httpResponse.body<String>()
                println("[LookupCategorySyncService] ERROR Response: $rawBody")
                return Result.failure(Exception("API Error ($status): $rawBody"))
            }
            
            val categories: List<LookupGroupDto> = httpResponse.body()
            println("[LookupCategorySyncService] Received ${categories.size} categories from API")
            
            // Convert to domain models
            val domainCategories = categories.map { it.toDomain() }
            Result.success(domainCategories)
            
        } catch (e: Exception) {
            println("[LookupCategorySyncService] Exception during fetch: ${e::class.simpleName} - ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
}

/**
 * Extension to convert DTO to domain model.
 */
private fun LookupGroupDto.toDomain(): LookupCategoryWithItems {
    return LookupCategoryWithItems(
        id = id,
        name = name ?: "Category $id",
        order = order,
        items = items?.map { it.toDomain() }?.sortedBy { it.order } ?: emptyList()
    )
}

/**
 * Extension to convert item DTO to domain model.
 */
private fun LookupGroupItemDto.toDomain(): LookupProduct {
    return LookupProduct(
        id = id,
        categoryId = lookupGroupId,
        productId = productId,
        name = product ?: "Unknown Product",
        itemNumber = itemNumber ?: "",
        order = order,
        imageUrl = fileUrl
    )
}

