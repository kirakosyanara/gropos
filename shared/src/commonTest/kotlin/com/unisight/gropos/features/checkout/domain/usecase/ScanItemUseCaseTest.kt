package com.unisight.gropos.features.checkout.domain.usecase

import com.unisight.gropos.features.checkout.data.CartRepositoryImpl
import com.unisight.gropos.features.checkout.data.FakeProductRepository
import com.unisight.gropos.features.checkout.data.FakeScannerRepository
import com.unisight.gropos.features.checkout.domain.model.ItemNumber
import com.unisight.gropos.features.checkout.domain.model.Product
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Unit tests for ScanItemUseCase.
 * 
 * Per testing-strategy.mdc:
 * - Use BigDecimal for all money comparisons
 * - Use Fakes for repositories
 * - Test reactive flows with runTest
 * 
 * Per DATABASE_SCHEMA.md:
 * - Products identified by branchProductId
 * - Barcodes are in itemNumbers array
 * - Uses getByBarcode for lookup
 * 
 * Per ARCHITECTURE_BLUEPRINT.md:
 * - ScanItemUseCase uses CartRepository for cart state
 * - CartRepository is singleton in production, fresh instance per test
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ScanItemUseCaseTest {
    
    /**
     * Creates test dependencies with fresh CartRepository.
     */
    private fun createUseCase(
        fakeScanner: FakeScannerRepository = FakeScannerRepository(),
        fakeProducts: FakeProductRepository = FakeProductRepository()
    ): ScanItemUseCase {
        val cartRepository = CartRepositoryImpl()
        return ScanItemUseCase(fakeScanner, fakeProducts, cartRepository)
    }
    
    // ========================================================================
    // Test Scenario from Requirements (Using Schema Data)
    // ========================================================================
    
    @Test
    fun `scanning Apple barcode should result in cart total of 1_00`() = runTest {
        // Given - Using schema-compliant data
        val useCase = createUseCase()
        
        // When - scan Apple (barcode: "111", retailPrice: $1.00)
        val result = useCase.processScan("111")
        
        // Then
        assertIs<ScanResult.Success>(result)
        assertEquals(
            BigDecimal("1.00"),
            useCase.cart.value.subTotal,
            "Cart subTotal should be exactly $1.00"
        )
    }
    
    @Test
    fun `scanning Apple barcode twice should result in cart total of 2_00`() = runTest {
        // Given
        val useCase = createUseCase()
        
        // When - scan Apple twice (barcode: "111")
        useCase.processScan("111")
        useCase.processScan("111")
        
        // Then
        assertEquals(
            BigDecimal("2.00"),
            useCase.cart.value.subTotal,
            "Cart subTotal should be exactly $2.00 after scanning Apple twice"
        )
        
        // Verify quantity was incremented, not a duplicate item added
        assertEquals(
            1,
            useCase.cart.value.uniqueItemCount,
            "Should have only 1 unique item"
        )
        assertEquals(
            BigDecimal("2"),
            useCase.cart.value.itemCount,
            "Should have quantity of 2"
        )
    }
    
    // ========================================================================
    // Schema Example Test: Milk with itemNumbers array
    // ========================================================================
    
    @Test
    fun `scanning Milk primary barcode should find product`() = runTest {
        // Given - Per DATABASE_SCHEMA.md: Milk has branchProductId 12345
        // Primary barcode: 070000000121
        val useCase = createUseCase()
        
        // When - scan Milk primary barcode
        val result = useCase.processScan("070000000121")
        
        // Then
        assertIs<ScanResult.Success>(result)
        assertEquals(
            BigDecimal("5.99"),
            useCase.cart.value.subTotal,
            "Cart subTotal should be $5.99 (Milk retail price)"
        )
        
        // Verify product details match schema
        val cartItem = useCase.cart.value.items.first()
        assertEquals(12345, cartItem.branchProductId)
        assertEquals("Organic Whole Milk 1 Gallon", cartItem.branchProductName)
    }
    
    @Test
    fun `scanning Milk secondary barcode should also find product`() = runTest {
        // Given - Milk has secondary barcode: 070000000122
        val useCase = createUseCase()
        
        // When - scan secondary barcode
        val result = useCase.processScan("070000000122")
        
        // Then - should find same product
        assertIs<ScanResult.Success>(result)
        assertEquals(12345, useCase.cart.value.items.first().branchProductId)
    }
    
    // ========================================================================
    // Scanner Flow Tests
    // ========================================================================
    
    @Test
    fun `scanResults flow should emit when scanner emits`() = runTest(UnconfinedTestDispatcher()) {
        // Given
        val fakeScanner = FakeScannerRepository()
        val fakeProducts = FakeProductRepository()
        val useCase = createUseCase(fakeScanner, fakeProducts)
        
        // Collect scan results in background
        val results = mutableListOf<ScanResult>()
        val collectJob = launch {
            useCase.scanResults.collect { results.add(it) }
        }
        
        // When - emit scans
        fakeScanner.emitScan("111") // Apple $1.00
        fakeScanner.emitScan("222") // Banana $0.50
        
        advanceUntilIdle()
        
        // Then
        assertEquals(2, results.size)
        assertIs<ScanResult.Success>(results[0])
        assertIs<ScanResult.Success>(results[1])
        
        // Verify final cart state
        assertEquals(
            BigDecimal("1.50"), // $1.00 + $0.50
            useCase.cart.value.subTotal
        )
        
        collectJob.cancel()
    }
    
    // ========================================================================
    // Product Lookup Tests
    // ========================================================================
    
    @Test
    fun `scanning Banana should result in correct total`() = runTest {
        // Given
        val useCase = createUseCase()
        
        // When - scan Banana (barcode: "222", retailPrice: $0.50)
        val result = useCase.processScan("222")
        
        // Then
        assertIs<ScanResult.Success>(result)
        assertEquals(
            BigDecimal("0.50"),
            useCase.cart.value.subTotal
        )
    }
    
    @Test
    fun `scanning unknown barcode should return ProductNotFound`() = runTest {
        // Given
        val useCase = createUseCase()
        
        // When - scan unknown barcode
        val result = useCase.processScan("999999999")
        
        // Then
        assertIs<ScanResult.ProductNotFound>(result)
        assertEquals("999999999", result.barcode)
        
        // Cart should remain empty
        assertTrue(useCase.cart.value.isEmpty)
    }
    
    // ========================================================================
    // Cart Management Tests
    // ========================================================================
    
    @Test
    fun `clearCart should reset cart to empty`() = runTest {
        // Given
        val useCase = createUseCase()
        
        // Add some items
        useCase.processScan("111")
        useCase.processScan("222")
        assertEquals(2, useCase.cart.value.uniqueItemCount)
        
        // When
        useCase.clearCart()
        
        // Then
        assertTrue(useCase.cart.value.isEmpty)
        assertEquals(BigDecimal.ZERO, useCase.cart.value.subTotal)
    }
    
    @Test
    fun `removeProduct should remove item by branchProductId`() = runTest {
        // Given
        val useCase = createUseCase()
        
        // Add items
        useCase.processScan("111") // Apple branchProductId=12346, $1.00
        useCase.processScan("222") // Banana branchProductId=12347, $0.50
        assertEquals(BigDecimal("1.50"), useCase.cart.value.subTotal)
        
        // When - remove Apple by branchProductId
        useCase.removeProduct(12346)
        
        // Then - only Banana remains
        assertEquals(1, useCase.cart.value.uniqueItemCount)
        assertEquals(BigDecimal("0.50"), useCase.cart.value.subTotal)
    }
    
    // ========================================================================
    // Multiple Products Test
    // ========================================================================
    
    @Test
    fun `scanning multiple different products should calculate correct total`() = runTest {
        // Given
        val useCase = createUseCase()
        
        // When - scan various products
        useCase.processScan("111") // Apple $1.00
        useCase.processScan("222") // Banana $0.50
        useCase.processScan("333") // Orange $0.75
        useCase.processScan("111") // Apple again (quantity: 2)
        
        // Then
        assertEquals(3, useCase.cart.value.uniqueItemCount)
        assertEquals(
            BigDecimal("3.25"), // $1.00 + $0.50 + $0.75 + $1.00
            useCase.cart.value.subTotal
        )
    }
    
    // ========================================================================
    // BigDecimal Precision Tests
    // ========================================================================
    
    @Test
    fun `BigDecimal calculations should be precise for monetary values`() = runTest {
        // Given - create product with values that would cause floating point errors
        val fakeProducts = FakeProductRepository()
        
        // Add a product with a price that causes floating point issues
        fakeProducts.addProduct(
            Product(
                branchProductId = 99999,
                productId = 999,
                productName = "Problem Item",
                retailPrice = BigDecimal("0.10"),
                itemNumbers = listOf(
                    ItemNumber("999999", isPrimary = true)
                )
            )
        )
        
        val useCase = createUseCase(fakeProducts = fakeProducts)
        
        // When - scan 3 times (0.10 + 0.10 + 0.10 = 0.30 exactly)
        // Note: 0.1 * 3 = 0.30000000000000004 in floating point
        useCase.processScan("999999")
        useCase.processScan("999999")
        useCase.processScan("999999")
        
        // Then - should be exactly 0.30, not 0.30000000000000004
        assertEquals(
            BigDecimal("0.30"),
            useCase.cart.value.subTotal,
            "BigDecimal should prevent floating point precision errors"
        )
    }
    
    // ========================================================================
    // Void Product Test
    // ========================================================================
    
    @Test
    fun `voidProduct should mark item as removed but keep in history`() = runTest {
        // Given
        val useCase = createUseCase()
        
        // Add items
        useCase.processScan("111") // Apple
        useCase.processScan("222") // Banana
        
        // When - void Apple
        useCase.voidProduct(12346)
        
        // Then - item is voided but still in list
        assertEquals(1, useCase.cart.value.uniqueItemCount) // Only non-removed count
        assertEquals(2, useCase.cart.value.items.size) // Total items including voided
        assertEquals(BigDecimal("0.50"), useCase.cart.value.subTotal) // Only Banana
    }
}
