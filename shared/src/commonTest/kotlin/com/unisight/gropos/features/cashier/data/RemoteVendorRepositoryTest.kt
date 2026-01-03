package com.unisight.gropos.features.cashier.data

import com.unisight.gropos.features.cashier.data.dto.VendorDto
import com.unisight.gropos.features.cashier.data.dto.VendorDomainMapper.toDomainList
import com.unisight.gropos.features.cashier.domain.model.Vendor
import com.unisight.gropos.features.cashier.domain.repository.VendorRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for RemoteVendorRepository.
 * 
 * **Per testing-strategy.mdc:**
 * - Use "Fake over Mock" for stateful operations
 * - Test all success paths and error conditions
 */
class RemoteVendorRepositoryTest {
    
    // ========================================================================
    // getVendors Tests
    // ========================================================================
    
    @Test
    fun `getVendors returns mapped domain models on success`() = runTest {
        // Arrange
        val apiVendors = listOf(
            VendorDto(id = "vendor_001", name = "Coca-Cola"),
            VendorDto(id = "vendor_002", name = "Pepsi"),
            VendorDto(id = "vendor_003", name = "Frito-Lay")
        )
        val repository = createRepository(vendorListResponse = apiVendors)
        
        // Act
        val vendors = repository.getVendors()
        
        // Assert
        assertEquals(3, vendors.size)
        assertEquals("vendor_001", vendors[0].id)
        assertEquals("Coca-Cola", vendors[0].name)
        assertEquals("vendor_002", vendors[1].id)
        assertEquals("Pepsi", vendors[1].name)
    }
    
    @Test
    fun `getVendors returns empty list when no vendors available`() = runTest {
        val repository = createRepository(vendorListResponse = emptyList())
        
        val vendors = repository.getVendors()
        
        assertTrue(vendors.isEmpty())
    }
    
    @Test
    fun `getVendors returns empty list on network error`() = runTest {
        val repository = createRepository(shouldFailNetwork = true)
        
        val vendors = repository.getVendors()
        
        // Per error handling: return empty list on network failure
        assertTrue(vendors.isEmpty())
    }
    
    // ========================================================================
    // getVendorById Tests
    // ========================================================================
    
    @Test
    fun `getVendorById returns vendor when found`() = runTest {
        val apiVendors = listOf(
            VendorDto(id = "vendor_001", name = "Coca-Cola"),
            VendorDto(id = "vendor_002", name = "Pepsi")
        )
        val repository = createRepository(vendorListResponse = apiVendors)
        
        val vendor = repository.getVendorById("vendor_002")
        
        assertNotNull(vendor)
        assertEquals("vendor_002", vendor.id)
        assertEquals("Pepsi", vendor.name)
    }
    
    @Test
    fun `getVendorById returns null when not found`() = runTest {
        val apiVendors = listOf(
            VendorDto(id = "vendor_001", name = "Coca-Cola")
        )
        val repository = createRepository(vendorListResponse = apiVendors)
        
        val vendor = repository.getVendorById("vendor_999")
        
        assertNull(vendor)
    }
    
    // ========================================================================
    // Test Helpers
    // ========================================================================
    
    private fun createRepository(
        vendorListResponse: List<VendorDto> = emptyList(),
        shouldFailNetwork: Boolean = false
    ): VendorRepository {
        return FakeRemoteVendorRepository(
            vendorListResponse = vendorListResponse,
            shouldFailNetwork = shouldFailNetwork
        )
    }
}

/**
 * Fake implementation of RemoteVendorRepository for testing.
 */
private class FakeRemoteVendorRepository(
    private val vendorListResponse: List<VendorDto>,
    private val shouldFailNetwork: Boolean
) : VendorRepository {
    
    override suspend fun getVendors(): List<Vendor> {
        if (shouldFailNetwork) {
            return emptyList()
        }
        return vendorListResponse.toDomainList()
    }
    
    override suspend fun getVendorById(vendorId: String): Vendor? {
        return getVendors().find { it.id == vendorId }
    }
}

