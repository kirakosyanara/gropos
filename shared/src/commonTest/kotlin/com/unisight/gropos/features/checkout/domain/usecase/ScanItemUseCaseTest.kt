package com.unisight.gropos.features.checkout.domain.usecase

import com.unisight.gropos.features.checkout.data.FakeProductRepository
import com.unisight.gropos.features.checkout.data.FakeScannerRepository
import com.unisight.gropos.features.checkout.domain.model.Product
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ScanItemUseCaseTest {
    
    // ========================================================================
    // Test Scenario from Requirements
    // ========================================================================
    
    @Test
    fun `scanning Apple should result in cart total of 1_00`() = runTest {
        // Given
        val fakeScanner = FakeScannerRepository()
        val fakeProducts = FakeProductRepository()
        val useCase = ScanItemUseCase(fakeScanner, fakeProducts)
        
        // When - scan Apple (SKU: 111, Price: $1.00)
        val result = useCase.processScan("111")
        
        // Then
        assertIs<ScanResult.Success>(result)
        assertEquals(
            BigDecimal("1.00"),
            useCase.cart.value.total,
            "Cart total should be exactly $1.00"
        )
    }
    
    @Test
    fun `scanning Apple twice should result in cart total of 2_00`() = runTest {
        // Given
        val fakeScanner = FakeScannerRepository()
        val fakeProducts = FakeProductRepository()
        val useCase = ScanItemUseCase(fakeScanner, fakeProducts)
        
        // When - scan Apple twice
        useCase.processScan("111")
        useCase.processScan("111")
        
        // Then
        assertEquals(
            BigDecimal("2.00"),
            useCase.cart.value.total,
            "Cart total should be exactly $2.00 after scanning Apple twice"
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
    // Scanner Flow Tests
    // ========================================================================
    
    @Test
    fun `scanResults flow should emit when scanner emits`() = runTest(UnconfinedTestDispatcher()) {
        // Given
        val fakeScanner = FakeScannerRepository()
        val fakeProducts = FakeProductRepository()
        val useCase = ScanItemUseCase(fakeScanner, fakeProducts)
        
        // Collect scan results in background
        val results = mutableListOf<ScanResult>()
        val collectJob = launch {
            useCase.scanResults.collect { results.add(it) }
        }
        
        // When - emit scans
        fakeScanner.emitScan("111") // Apple
        fakeScanner.emitScan("222") // Banana
        
        advanceUntilIdle()
        
        // Then
        assertEquals(2, results.size)
        assertIs<ScanResult.Success>(results[0])
        assertIs<ScanResult.Success>(results[1])
        
        // Verify final cart state
        assertEquals(
            BigDecimal("1.50"), // $1.00 + $0.50
            useCase.cart.value.total
        )
        
        collectJob.cancel()
    }
    
    // ========================================================================
    // Product Lookup Tests
    // ========================================================================
    
    @Test
    fun `scanning Banana should result in cart total of 0_50`() = runTest {
        // Given
        val fakeScanner = FakeScannerRepository()
        val fakeProducts = FakeProductRepository()
        val useCase = ScanItemUseCase(fakeScanner, fakeProducts)
        
        // When - scan Banana (SKU: 222, Price: $0.50)
        val result = useCase.processScan("222")
        
        // Then
        assertIs<ScanResult.Success>(result)
        assertEquals(
            BigDecimal("0.50"),
            useCase.cart.value.total
        )
    }
    
    @Test
    fun `scanning unknown SKU should return ProductNotFound`() = runTest {
        // Given
        val fakeScanner = FakeScannerRepository()
        val fakeProducts = FakeProductRepository()
        val useCase = ScanItemUseCase(fakeScanner, fakeProducts)
        
        // When - scan unknown SKU
        val result = useCase.processScan("999")
        
        // Then
        assertIs<ScanResult.ProductNotFound>(result)
        assertEquals("999", result.sku)
        
        // Cart should remain empty
        assertTrue(useCase.cart.value.isEmpty)
    }
    
    // ========================================================================
    // Cart Management Tests
    // ========================================================================
    
    @Test
    fun `clearCart should reset cart to empty`() = runTest {
        // Given
        val fakeScanner = FakeScannerRepository()
        val fakeProducts = FakeProductRepository()
        val useCase = ScanItemUseCase(fakeScanner, fakeProducts)
        
        // Add some items
        useCase.processScan("111")
        useCase.processScan("222")
        assertEquals(2, useCase.cart.value.uniqueItemCount)
        
        // When
        useCase.clearCart()
        
        // Then
        assertTrue(useCase.cart.value.isEmpty)
        assertEquals(BigDecimal.ZERO, useCase.cart.value.total)
    }
    
    @Test
    fun `removeProduct should remove item from cart`() = runTest {
        // Given
        val fakeScanner = FakeScannerRepository()
        val fakeProducts = FakeProductRepository()
        val useCase = ScanItemUseCase(fakeScanner, fakeProducts)
        
        // Add items
        useCase.processScan("111") // Apple $1.00
        useCase.processScan("222") // Banana $0.50
        assertEquals(BigDecimal("1.50"), useCase.cart.value.total)
        
        // When - remove Apple
        useCase.removeProduct("111")
        
        // Then - only Banana remains
        assertEquals(1, useCase.cart.value.uniqueItemCount)
        assertEquals(BigDecimal("0.50"), useCase.cart.value.total)
    }
    
    // ========================================================================
    // Multiple Products Test
    // ========================================================================
    
    @Test
    fun `scanning multiple different products should calculate correct total`() = runTest {
        // Given
        val fakeScanner = FakeScannerRepository()
        val fakeProducts = FakeProductRepository()
        val useCase = ScanItemUseCase(fakeScanner, fakeProducts)
        
        // When - scan various products
        useCase.processScan("111") // Apple $1.00
        useCase.processScan("222") // Banana $0.50
        useCase.processScan("333") // Orange $0.75
        useCase.processScan("111") // Apple again (quantity: 2)
        
        // Then
        assertEquals(3, useCase.cart.value.uniqueItemCount)
        assertEquals(
            BigDecimal("3.25"), // $1.00 + $0.50 + $0.75 + $1.00
            useCase.cart.value.total
        )
    }
    
    // ========================================================================
    // BigDecimal Precision Tests
    // ========================================================================
    
    @Test
    fun `BigDecimal calculations should be precise for monetary values`() = runTest {
        // Given - create products with values that would cause floating point errors
        val fakeScanner = FakeScannerRepository()
        val fakeProducts = FakeProductRepository()
        
        // Add a product with a price that causes floating point issues
        fakeProducts.addProduct(
            Product(
                id = "99",
                name = "Problem Item",
                price = BigDecimal("0.10"),
                sku = "999"
            )
        )
        
        val useCase = ScanItemUseCase(fakeScanner, fakeProducts)
        
        // When - scan 3 times (0.10 + 0.10 + 0.10 = 0.30 exactly, but 0.1 * 3 = 0.30000000000000004 in floating point)
        useCase.processScan("999")
        useCase.processScan("999")
        useCase.processScan("999")
        
        // Then - should be exactly 0.30, not 0.30000000000000004
        assertEquals(
            BigDecimal("0.30"),
            useCase.cart.value.total,
            "BigDecimal should prevent floating point precision errors"
        )
    }
}

