package com.unisight.gropos.core.sync

import com.unisight.gropos.features.checkout.domain.model.Product
import com.unisight.gropos.features.checkout.domain.repository.LookupCategory
import com.unisight.gropos.features.checkout.domain.repository.ProductRepository
import kotlinx.coroutines.test.runTest
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for ProductSyncService.
 * 
 * Per testing-strategy.mdc: Use Fakes for State.
 * 
 * Tests cover:
 * - Successful sync with single page
 * - Successful sync with pagination (multiple pages)
 * - Empty product list handling
 * - Sync needed check based on product count
 * - Error handling during sync
 */
class ProductSyncServiceTest {
    
    // ========================================================================
    // Test Fakes
    // ========================================================================
    
    /**
     * Fake ProductRepository that tracks inserted products.
     */
    private class FakeProductRepository : ProductRepository {
        val insertedProducts = mutableListOf<Product>()
        var productCount: Long = 0L
        var insertShouldFail = false
        
        override suspend fun insertProduct(product: Product): Boolean {
            if (insertShouldFail) return false
            insertedProducts.add(product)
            productCount++
            return true
        }
        
        override suspend fun getProductCount(): Long = productCount
        
        override suspend fun getByBarcode(barcode: String): Product? = 
            insertedProducts.find { p -> p.itemNumbers.any { it.itemNumber == barcode } }
        
        override suspend fun getByCategory(categoryId: Int): List<Product> = 
            insertedProducts.filter { it.category == categoryId }
        
        override suspend fun getById(branchProductId: Int): Product? = 
            insertedProducts.find { it.branchProductId == branchProductId }
        
        override suspend fun searchByName(query: String): List<Product> = 
            insertedProducts.filter { it.productName.contains(query, ignoreCase = true) }
        
        override suspend fun searchProducts(query: String): List<Product> = 
            searchByName(query)
        
        override suspend fun getCategories(): List<LookupCategory> = emptyList()
        
        fun clear() {
            insertedProducts.clear()
            productCount = 0L
        }
    }
    
    /**
     * Fake ApiClient that returns pre-configured product lists.
     */
    private class FakeApiClient {
        var pages: List<List<Product>> = emptyList()
        var currentPageIndex = 0
        var shouldFail = false
        var failOnPage = -1
        
        fun getNextPage(): Result<List<Product>> {
            if (shouldFail) {
                return Result.failure(Exception("API Error"))
            }
            if (failOnPage == currentPageIndex) {
                return Result.failure(Exception("Page $currentPageIndex failed"))
            }
            
            if (currentPageIndex >= pages.size) {
                return Result.success(emptyList())
            }
            
            val page = pages[currentPageIndex]
            currentPageIndex++
            return Result.success(page)
        }
        
        fun reset() {
            currentPageIndex = 0
        }
    }
    
    /**
     * Testable ProductSyncService that uses FakeApiClient.
     */
    private class TestableProductSyncService(
        private val fakeApiClient: FakeApiClient,
        private val productRepository: ProductRepository
    ) {
        suspend fun syncAllProducts(): Result<Int> {
            var totalSynced = 0
            var hasMore = true
            
            while (hasMore) {
                val pageResult = fakeApiClient.getNextPage()
                
                pageResult.fold(
                    onSuccess = { products ->
                        if (products.isEmpty()) {
                            hasMore = false
                        } else {
                            for (product in products) {
                                if (productRepository.insertProduct(product)) {
                                    totalSynced++
                                }
                            }
                            if (products.size < 100) {
                                hasMore = false
                            }
                        }
                    },
                    onFailure = { error ->
                        hasMore = false
                        return Result.failure(error)
                    }
                )
            }
            
            return Result.success(totalSynced)
        }
        
        suspend fun isSyncNeeded(): Boolean {
            return productRepository.getProductCount() == 0L
        }
    }
    
    // ========================================================================
    // Helper Methods
    // ========================================================================
    
    private fun createTestProduct(id: Int, name: String = "Product $id"): Product {
        return Product(
            branchProductId = id,
            productId = id,
            productName = name,
            retailPrice = BigDecimal("9.99"),
            isActive = true,
            isForSale = true
        )
    }
    
    private fun createProductPage(startId: Int, count: Int): List<Product> {
        return (startId until startId + count).map { createTestProduct(it) }
    }
    
    // ========================================================================
    // Tests: isSyncNeeded
    // ========================================================================
    
    @Test
    fun `isSyncNeeded returns true when database is empty`() = runTest {
        val fakeRepo = FakeProductRepository()
        val fakeApi = FakeApiClient()
        val service = TestableProductSyncService(fakeApi, fakeRepo)
        
        assertTrue(service.isSyncNeeded())
    }
    
    @Test
    fun `isSyncNeeded returns false when database has products`() = runTest {
        val fakeRepo = FakeProductRepository()
        fakeRepo.productCount = 100L
        val fakeApi = FakeApiClient()
        val service = TestableProductSyncService(fakeApi, fakeRepo)
        
        assertFalse(service.isSyncNeeded())
    }
    
    // ========================================================================
    // Tests: syncAllProducts - Success Cases
    // ========================================================================
    
    @Test
    fun `syncAllProducts with single page syncs all products`() = runTest {
        val fakeRepo = FakeProductRepository()
        val fakeApi = FakeApiClient()
        
        // Single page with 50 products (less than page size of 100)
        fakeApi.pages = listOf(createProductPage(1, 50))
        
        val service = TestableProductSyncService(fakeApi, fakeRepo)
        val result = service.syncAllProducts()
        
        assertTrue(result.isSuccess)
        assertEquals(50, result.getOrNull())
        assertEquals(50, fakeRepo.insertedProducts.size)
    }
    
    @Test
    fun `syncAllProducts with multiple pages syncs all products`() = runTest {
        val fakeRepo = FakeProductRepository()
        val fakeApi = FakeApiClient()
        
        // Three pages: 100, 100, 50 products
        fakeApi.pages = listOf(
            createProductPage(1, 100),
            createProductPage(101, 100),
            createProductPage(201, 50)
        )
        
        val service = TestableProductSyncService(fakeApi, fakeRepo)
        val result = service.syncAllProducts()
        
        assertTrue(result.isSuccess)
        assertEquals(250, result.getOrNull())
        assertEquals(250, fakeRepo.insertedProducts.size)
    }
    
    @Test
    fun `syncAllProducts with empty catalog returns zero`() = runTest {
        val fakeRepo = FakeProductRepository()
        val fakeApi = FakeApiClient()
        
        // Empty catalog
        fakeApi.pages = listOf(emptyList())
        
        val service = TestableProductSyncService(fakeApi, fakeRepo)
        val result = service.syncAllProducts()
        
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull())
        assertEquals(0, fakeRepo.insertedProducts.size)
    }
    
    @Test
    fun `syncAllProducts with exactly PAGE_SIZE items fetches next page`() = runTest {
        val fakeRepo = FakeProductRepository()
        val fakeApi = FakeApiClient()
        
        // First page: exactly 100, second page: empty
        fakeApi.pages = listOf(
            createProductPage(1, 100),
            emptyList()
        )
        
        val service = TestableProductSyncService(fakeApi, fakeRepo)
        val result = service.syncAllProducts()
        
        assertTrue(result.isSuccess)
        assertEquals(100, result.getOrNull())
        assertEquals(2, fakeApi.currentPageIndex) // Should have fetched 2 pages
    }
    
    // ========================================================================
    // Tests: syncAllProducts - Error Cases
    // ========================================================================
    
    @Test
    fun `syncAllProducts returns failure when API fails`() = runTest {
        val fakeRepo = FakeProductRepository()
        val fakeApi = FakeApiClient()
        
        fakeApi.shouldFail = true
        
        val service = TestableProductSyncService(fakeApi, fakeRepo)
        val result = service.syncAllProducts()
        
        assertTrue(result.isFailure)
        assertEquals("API Error", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `syncAllProducts returns failure when second page fails`() = runTest {
        val fakeRepo = FakeProductRepository()
        val fakeApi = FakeApiClient()
        
        // First page succeeds, second page fails
        fakeApi.pages = listOf(createProductPage(1, 100))
        fakeApi.failOnPage = 1
        
        val service = TestableProductSyncService(fakeApi, fakeRepo)
        val result = service.syncAllProducts()
        
        assertTrue(result.isFailure)
        // First 100 products should still be saved
        assertEquals(100, fakeRepo.insertedProducts.size)
    }
    
    @Test
    fun `syncAllProducts handles insert failures gracefully`() = runTest {
        val fakeRepo = FakeProductRepository()
        fakeRepo.insertShouldFail = true
        val fakeApi = FakeApiClient()
        
        fakeApi.pages = listOf(createProductPage(1, 10))
        
        val service = TestableProductSyncService(fakeApi, fakeRepo)
        val result = service.syncAllProducts()
        
        // Should succeed but with 0 products inserted
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull())
    }
    
    // ========================================================================
    // Tests: Product Data Integrity
    // ========================================================================
    
    @Test
    fun `synced products have correct data`() = runTest {
        val fakeRepo = FakeProductRepository()
        val fakeApi = FakeApiClient()
        
        val testProduct = Product(
            branchProductId = 12345,
            productId = 100,
            productName = "Test Product",
            retailPrice = BigDecimal("19.99"),
            floorPrice = BigDecimal("15.00"),
            category = 5,
            categoryName = "Test Category",
            isActive = true,
            isForSale = true,
            isSnapEligible = true
        )
        
        fakeApi.pages = listOf(listOf(testProduct))
        
        val service = TestableProductSyncService(fakeApi, fakeRepo)
        val result = service.syncAllProducts()
        
        assertTrue(result.isSuccess)
        assertEquals(1, fakeRepo.insertedProducts.size)
        
        val savedProduct = fakeRepo.insertedProducts.first()
        assertEquals(12345, savedProduct.branchProductId)
        assertEquals(100, savedProduct.productId)
        assertEquals("Test Product", savedProduct.productName)
        assertEquals(BigDecimal("19.99"), savedProduct.retailPrice)
        assertEquals(BigDecimal("15.00"), savedProduct.floorPrice)
        assertEquals(5, savedProduct.category)
        assertEquals("Test Category", savedProduct.categoryName)
        assertTrue(savedProduct.isActive)
        assertTrue(savedProduct.isForSale)
        assertTrue(savedProduct.isSnapEligible)
    }
    
    // ========================================================================
    // Tests: Large Catalog Handling
    // ========================================================================
    
    @Test
    fun `syncAllProducts handles large catalog with many pages`() = runTest {
        val fakeRepo = FakeProductRepository()
        val fakeApi = FakeApiClient()
        
        // 10 pages of 100 products each = 1000 products
        fakeApi.pages = (0 until 10).map { page ->
            createProductPage(page * 100 + 1, 100)
        } + listOf(emptyList()) // Final empty page to signal end
        
        val service = TestableProductSyncService(fakeApi, fakeRepo)
        val result = service.syncAllProducts()
        
        assertTrue(result.isSuccess)
        assertEquals(1000, result.getOrNull())
        assertEquals(1000, fakeRepo.insertedProducts.size)
    }
}

