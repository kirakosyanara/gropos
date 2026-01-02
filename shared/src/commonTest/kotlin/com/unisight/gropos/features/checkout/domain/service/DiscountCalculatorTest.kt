package com.unisight.gropos.features.checkout.domain.service

import com.unisight.gropos.features.checkout.domain.model.ItemNumber
import com.unisight.gropos.features.checkout.domain.model.Product
import com.unisight.gropos.features.checkout.domain.model.ProductSale
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for DiscountCalculator.
 * 
 * Per SERVICES.md:
 * - Price hierarchy: Prompted → Customer → Sale → Bulk → Retail
 * - Floor price enforcement
 * - Percentage and fixed amount discounts
 * 
 * Per testing-strategy.mdc:
 * - Use BigDecimal for exact matching
 */
class DiscountCalculatorTest {
    
    private val calculator = DiscountCalculator()
    
    // ========================================================================
    // Effective Price Tests (Price Hierarchy)
    // ========================================================================
    
    @Test
    fun `getEffectivePrice returns prompted price if provided`() {
        val product = createProduct(retailPrice = BigDecimal("10.00"))
        
        val result = calculator.getEffectivePrice(product, promptedPrice = BigDecimal("8.00"))
        
        assertEquals(BigDecimal("8.00"), result)
    }
    
    @Test
    fun `getEffectivePrice returns sale price when no prompted price`() {
        val product = createProduct(
            retailPrice = BigDecimal("10.00"),
            salePrice = BigDecimal("7.00")
        )
        
        val result = calculator.getEffectivePrice(product, promptedPrice = null)
        
        assertEquals(BigDecimal("7.00"), result)
    }
    
    @Test
    fun `getEffectivePrice returns retail price when no sale or prompted`() {
        val product = createProduct(retailPrice = BigDecimal("10.00"))
        
        val result = calculator.getEffectivePrice(product, promptedPrice = null)
        
        assertEquals(BigDecimal("10.00"), result)
    }
    
    @Test
    fun `getEffectivePrice ignores zero prompted price`() {
        val product = createProduct(
            retailPrice = BigDecimal("10.00"),
            salePrice = BigDecimal("7.00")
        )
        
        val result = calculator.getEffectivePrice(product, promptedPrice = BigDecimal.ZERO)
        
        assertEquals(BigDecimal("7.00"), result)
    }
    
    // ========================================================================
    // Percentage Discount Tests
    // ========================================================================
    
    @Test
    fun `applyPercentageDiscount applies correct percentage`() {
        val price = BigDecimal("100.00")
        val percentage = BigDecimal("10")
        
        val result = calculator.applyPercentageDiscount(price, percentage)
        
        assertEquals(BigDecimal("90.00"), result)
    }
    
    @Test
    fun `applyPercentageDiscount handles 50 percent`() {
        val price = BigDecimal("50.00")
        val percentage = BigDecimal("50")
        
        val result = calculator.applyPercentageDiscount(price, percentage)
        
        assertEquals(BigDecimal("25.00"), result)
    }
    
    @Test
    fun `applyPercentageDiscount rounds to 2 decimal places`() {
        // $10.00 at 33% off = $10 * 0.33 = $3.30, result = $6.70
        val price = BigDecimal("10.00")
        val percentage = BigDecimal("33")
        
        val result = calculator.applyPercentageDiscount(price, percentage)
        
        assertEquals(BigDecimal("6.70"), result)
    }
    
    // ========================================================================
    // Fixed Discount Tests
    // ========================================================================
    
    @Test
    fun `applyFixedDiscount subtracts discount from price`() {
        val price = BigDecimal("10.00")
        val discount = BigDecimal("2.50")
        
        val result = calculator.applyFixedDiscount(price, discount)
        
        assertEquals(BigDecimal("7.50"), result)
    }
    
    @Test
    fun `applyFixedDiscount does not go below zero`() {
        val price = BigDecimal("5.00")
        val discount = BigDecimal("10.00")
        
        val result = calculator.applyFixedDiscount(price, discount)
        
        assertEquals(BigDecimal("0.00"), result)
    }
    
    // ========================================================================
    // Floor Price Tests
    // ========================================================================
    
    @Test
    fun `calculateFinalPrice enforces floor price`() {
        val priceUsed = BigDecimal("10.00")
        val lineDiscount = BigDecimal("8.00")
        val floorPrice = BigDecimal("5.00")
        
        val result = calculator.calculateFinalPrice(
            priceUsed = priceUsed,
            lineDiscount = lineDiscount,
            floorPrice = floorPrice
        )
        
        // $10 - $8 = $2, but floor is $5, so result is $5
        assertEquals(BigDecimal("5.00"), result)
    }
    
    @Test
    fun `calculateFinalPrice allows price above floor`() {
        val priceUsed = BigDecimal("10.00")
        val lineDiscount = BigDecimal("3.00")
        val floorPrice = BigDecimal("5.00")
        
        val result = calculator.calculateFinalPrice(
            priceUsed = priceUsed,
            lineDiscount = lineDiscount,
            floorPrice = floorPrice
        )
        
        // $10 - $3 = $7, which is above $5 floor
        assertEquals(BigDecimal("7.00"), result)
    }
    
    @Test
    fun `calculateFinalPrice allows sale price below floor`() {
        val priceUsed = BigDecimal("4.00")  // Sale price below floor
        val floorPrice = BigDecimal("5.00")
        
        val result = calculator.calculateFinalPrice(
            priceUsed = priceUsed,
            floorPrice = floorPrice,
            isSalePrice = true
        )
        
        // Sale prices can be below floor (corporate-set)
        assertEquals(BigDecimal("4.00"), result)
    }
    
    @Test
    fun `calculateFinalPrice allows floor override by manager`() {
        val priceUsed = BigDecimal("10.00")
        val lineDiscount = BigDecimal("8.00")
        val floorPrice = BigDecimal("5.00")
        
        val result = calculator.calculateFinalPrice(
            priceUsed = priceUsed,
            lineDiscount = lineDiscount,
            floorPrice = floorPrice,
            isFloorOverridden = true
        )
        
        // With override, can go below floor: $10 - $8 = $2
        assertEquals(BigDecimal("2.00"), result)
    }
    
    // ========================================================================
    // Savings Calculation Tests
    // ========================================================================
    
    @Test
    fun `calculateSavingsPerUnit calculates price difference`() {
        val retailPrice = BigDecimal("10.00")
        val effectivePrice = BigDecimal("7.00")
        
        val result = calculator.calculateSavingsPerUnit(retailPrice, effectivePrice)
        
        assertEquals(BigDecimal("3.00"), result)
    }
    
    @Test
    fun `calculateSavingsPerUnit includes discounts`() {
        val retailPrice = BigDecimal("10.00")
        val effectivePrice = BigDecimal("10.00")  // No sale price
        val lineDiscount = BigDecimal("2.00")
        val transactionDiscount = BigDecimal("1.00")
        
        val result = calculator.calculateSavingsPerUnit(
            retailPrice = retailPrice,
            effectivePrice = effectivePrice,
            lineDiscount = lineDiscount,
            transactionDiscount = transactionDiscount
        )
        
        // $10 - $10 + $2 + $1 = $3
        assertEquals(BigDecimal("3.00"), result)
    }
    
    @Test
    fun `calculateSavingsPerUnit never returns negative`() {
        val retailPrice = BigDecimal("5.00")
        val effectivePrice = BigDecimal("10.00")  // Price increase
        
        val result = calculator.calculateSavingsPerUnit(retailPrice, effectivePrice)
        
        assertEquals(BigDecimal("0.00"), result)
    }
    
    // ========================================================================
    // Helper Functions
    // ========================================================================
    
    private fun createProduct(
        branchProductId: Int = 1,
        productName: String = "Test Product",
        retailPrice: BigDecimal = BigDecimal("1.00"),
        salePrice: BigDecimal? = null
    ): Product {
        val currentSale = salePrice?.let {
            ProductSale(
                id = 1,
                retailPrice = retailPrice,
                discountedPrice = it,
                discountAmount = retailPrice.subtract(it),
                startDate = "2026-01-01",
                endDate = "2026-12-31"
            )
        }
        
        return Product(
            branchProductId = branchProductId,
            productId = branchProductId,
            productName = productName,
            retailPrice = retailPrice,
            itemNumbers = listOf(ItemNumber("123", isPrimary = true)),
            currentSale = currentSale
        )
    }
}

