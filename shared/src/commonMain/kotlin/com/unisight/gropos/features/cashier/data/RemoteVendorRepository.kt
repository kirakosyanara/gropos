package com.unisight.gropos.features.cashier.data

import com.unisight.gropos.core.network.ApiClient
import com.unisight.gropos.features.cashier.data.dto.VendorDomainMapper.toDomainList
import com.unisight.gropos.features.cashier.data.dto.VendorListResponse
import com.unisight.gropos.features.cashier.domain.model.Vendor
import com.unisight.gropos.features.cashier.domain.repository.VendorRepository
import io.ktor.client.request.get

/**
 * Remote implementation of VendorRepository using REST API.
 * 
 * **Per API_INTEGRATION.md:**
 * - Uses authenticatedClient for Bearer token auth
 * - Integrates with TokenRefreshManager for automatic 401 handling
 * 
 * **API Endpoint:**
 * - GET /vendor - Get all vendors for current branch
 * 
 * **Per project-structure.mdc:**
 * - Repository implementation in Data layer
 * - Returns domain models (Vendor), not DTOs
 * 
 * **Per reliability-stability.mdc:**
 * - Returns empty list on error (graceful degradation)
 * - Logs errors for debugging
 */
class RemoteVendorRepository(
    private val apiClient: ApiClient
) : VendorRepository {
    
    companion object {
        private const val ENDPOINT_VENDORS = "/vendor"
    }
    
    /**
     * In-memory cache to avoid repeated API calls.
     * Vendors don't change frequently during a session.
     */
    private var cachedVendors: List<Vendor>? = null
    
    /**
     * Fetches all vendors from the API.
     * 
     * **API:** GET /vendor
     * **Response:** VendorListResponse with list of VendorDto
     * 
     * @return List of vendors, or empty list on error
     */
    override suspend fun getVendors(): List<Vendor> {
        // Return cached if available
        cachedVendors?.let { return it }
        
        val result = apiClient.authenticatedRequest<VendorListResponse> {
            get(ENDPOINT_VENDORS)
        }
        
        return result.fold(
            onSuccess = { response ->
                val vendors = response.vendors.toDomainList()
                cachedVendors = vendors
                vendors
            },
            onFailure = { error ->
                // Log error but return empty list for graceful degradation
                println("RemoteVendorRepository: Failed to fetch vendors - ${error.message}")
                emptyList()
            }
        )
    }
    
    /**
     * Gets a vendor by ID.
     * 
     * Fetches all vendors and filters locally.
     * Could be optimized with a dedicated API endpoint if needed.
     * 
     * @param vendorId The vendor ID to look up
     * @return The vendor if found, null otherwise
     */
    override suspend fun getVendorById(vendorId: String): Vendor? {
        return getVendors().find { it.id == vendorId }
    }
    
    /**
     * Clears the vendor cache.
     * Call this when vendor data may have changed (e.g., after sync).
     */
    fun clearCache() {
        cachedVendors = null
    }
}

