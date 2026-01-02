package com.unisight.gropos.features.checkout.domain.service

import com.unisight.gropos.features.checkout.domain.model.ItemNumber
import com.unisight.gropos.features.checkout.domain.model.Product
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for CRVCalculator.
 * 
 * Per DEPOSITS_FEES.md:
 * - CRV (California Redemption Value) is mandatory on beverage containers
 * - CRV rates: $0.05 (<24oz), $0.10 (>=24oz)
 * - CRV is ALWAYS subject to sales tax (California law)
 * 
 * Per testing-strategy.mdc:
 * - Use BigDecimal for exact matching
 */
class CRVCalculatorTest {
    
    private val calculator = CRVCalculator()
    
    // ========================================================================
    // CRV Rate Lookup Tests
    // ========================================================================
    
    @Test
    fun `getCRVRate returns product CRV rate`() {
        val product = createProduct(crvRatePerUnit = BigDecimal("0.10"))
        
        val result = calculator.getCRVRate(product)
        
        assertEquals(BigDecimal("0.10"), result)
    }
    
    @Test
    fun `getCRVRate returns zero for products without CRV`() {
        val product = createProduct(crvRatePerUnit = BigDecimal.ZERO)
        
        val result = calculator.getCRVRate(product)
        
        assertEquals(BigDecimal.ZERO, result)
    }
    
    // ========================================================================
    // Line CRV Calculation Tests
    // ========================================================================
    
    @Test
    fun `calculateLineCRV multiplies rate by quantity`() {
        val crvPerUnit = BigDecimal("0.10")
        val quantity = BigDecimal("3")
        
        val result = calculator.calculateLineCRV(crvPerUnit, quantity)
        
        assertEquals(BigDecimal("0.30"), result)
    }
    
    @Test
    fun `calculateLineCRV returns zero for zero CRV rate`() {
        val crvPerUnit = BigDecimal.ZERO
        val quantity = BigDecimal("5")
        
        val result = calculator.calculateLineCRV(crvPerUnit, quantity)
        
        assertEquals(BigDecimal.ZERO, result)
    }
    
    @Test
    fun `calculateLineCRV handles small CRV correctly`() {
        // 5 cent CRV × 2 items = 10 cents
        val crvPerUnit = BigDecimal("0.05")
        val quantity = BigDecimal("2")
        
        val result = calculator.calculateLineCRV(crvPerUnit, quantity)
        
        assertEquals(BigDecimal("0.10"), result)
    }
    
    // ========================================================================
    // Item CRV Calculation Tests
    // ========================================================================
    
    @Test
    fun `calculateItemCRV returns pair of per unit and total`() {
        val product = createProduct(crvRatePerUnit = BigDecimal("0.10"))
        val quantity = BigDecimal("3")
        
        val (crvPerUnit, crvTotal) = calculator.calculateItemCRV(product, quantity)
        
        assertEquals(BigDecimal("0.10"), crvPerUnit)
        assertEquals(BigDecimal("0.30"), crvTotal)
    }
    
    // ========================================================================
    // Helper Functions
    // ========================================================================
    
    private fun createProduct(
        branchProductId: Int = 1,
        productName: String = "Test Product",
        retailPrice: BigDecimal = BigDecimal("1.00"),
        crvRatePerUnit: BigDecimal = BigDecimal.ZERO
    ): Product {
        return Product(
            branchProductId = branchProductId,
            productId = branchProductId,
            productName = productName,
            retailPrice = retailPrice,
            itemNumbers = listOf(ItemNumber("123", isPrimary = true)),
            crvRatePerUnit = crvRatePerUnit
        )
    }
}

