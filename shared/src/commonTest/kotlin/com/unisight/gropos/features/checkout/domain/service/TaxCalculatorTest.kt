package com.unisight.gropos.features.checkout.domain.service

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for TaxCalculator.
 * 
 * Per TAX_CALCULATIONS.md - The Tax Consistency Rule:
 * - Tax per unit is calculated and ROUNDED first, THEN multiplied by quantity
 * - CRV is ALWAYS included in taxable amount
 * - SNAP items are tax-exempt
 * 
 * Per testing-strategy.mdc:
 * - Use BigDecimal for exact matching
 * - Cover edge cases
 */
class TaxCalculatorTest {
    
    private val calculator = TaxCalculator()
    
    // ========================================================================
    // Per-Unit Tax Calculation Tests
    // ========================================================================
    
    @Test
    fun `calculateTaxPerUnit returns correct tax for taxable item`() {
        // Soda example from TAX_CALCULATIONS.md:
        // Price: $2.99, CRV: $0.10 → Taxable: $3.09, Tax Rate: 9.5%
        // Tax = $3.09 × 9.5% = $0.29355 → $0.29
        val taxableAmount = BigDecimal("3.09")
        val taxRate = BigDecimal("9.5")
        
        val result = calculator.calculateTaxPerUnit(taxableAmount, taxRate, isSnapEligible = false)
        
        assertEquals(BigDecimal("0.29"), result)
    }
    
    @Test
    fun `calculateTaxPerUnit returns zero for SNAP eligible item`() {
        // SNAP items are tax-exempt regardless of tax rate
        val taxableAmount = BigDecimal("10.00")
        val taxRate = BigDecimal("9.5")
        
        val result = calculator.calculateTaxPerUnit(taxableAmount, taxRate, isSnapEligible = true)
        
        assertEquals(BigDecimal.ZERO, result)
    }
    
    @Test
    fun `calculateTaxPerUnit returns zero for zero taxable amount`() {
        val taxableAmount = BigDecimal.ZERO
        val taxRate = BigDecimal("9.5")
        
        val result = calculator.calculateTaxPerUnit(taxableAmount, taxRate, isSnapEligible = false)
        
        assertEquals(BigDecimal.ZERO, result)
    }
    
    @Test
    fun `calculateTaxPerUnit returns zero for zero tax rate`() {
        val taxableAmount = BigDecimal("10.00")
        val taxRate = BigDecimal.ZERO
        
        val result = calculator.calculateTaxPerUnit(taxableAmount, taxRate, isSnapEligible = false)
        
        assertEquals(BigDecimal.ZERO, result)
    }
    
    @Test
    fun `calculateTaxPerUnit returns zero for negative taxable amount`() {
        val taxableAmount = BigDecimal("-5.00")
        val taxRate = BigDecimal("9.5")
        
        val result = calculator.calculateTaxPerUnit(taxableAmount, taxRate, isSnapEligible = false)
        
        assertEquals(BigDecimal.ZERO, result)
    }
    
    // ========================================================================
    // Line Tax Calculation Tests (The Tax Consistency Rule)
    // ========================================================================
    
    @Test
    fun `calculateLineTax multiplies rounded per-unit tax by quantity`() {
        // Per TAX_CALCULATIONS.md: Tax per unit × quantity
        // Customer buying 3 items should pay same as 3 customers buying 1 each
        val taxPerUnit = BigDecimal("0.29")
        val quantity = BigDecimal("3")
        
        val result = calculator.calculateLineTax(taxPerUnit, quantity)
        
        assertEquals(BigDecimal("0.87"), result)
    }
    
    @Test
    fun `tax consistency rule ensures fairness`() {
        // Scenario from TAX_CALCULATIONS.md:
        // Item: Soda $2.69 taxable (after CRV), Tax Rate: 9.5%
        // Tax per unit = $0.26 (rounded)
        // 
        // Customer A buys 3: $0.26 × 3 = $0.78
        // Customers B+C+D buy 1 each: $0.26 × 3 = $0.78
        // Total matches!
        
        val taxableAmount = BigDecimal("2.69")
        val taxRate = BigDecimal("9.5")
        
        // Calculate per-unit tax (rounded)
        val taxPerUnit = calculator.calculateTaxPerUnit(taxableAmount, taxRate, isSnapEligible = false)
        assertEquals(BigDecimal("0.26"), taxPerUnit)
        
        // Customer A: 3 items
        val customerATax = calculator.calculateLineTax(taxPerUnit, BigDecimal("3"))
        assertEquals(BigDecimal("0.78"), customerATax)
        
        // Customers B, C, D: 1 item each
        val customerBTax = calculator.calculateLineTax(taxPerUnit, BigDecimal.ONE)
        val customerCTax = calculator.calculateLineTax(taxPerUnit, BigDecimal.ONE)
        val customerDTax = calculator.calculateLineTax(taxPerUnit, BigDecimal.ONE)
        
        val combinedTax = customerBTax.add(customerCTax).add(customerDTax)
        assertEquals(BigDecimal("0.78"), combinedTax)
        
        // They should match
        assertEquals(customerATax, combinedTax)
    }
    
    // ========================================================================
    // Full Item Tax Calculation Tests
    // ========================================================================
    
    @Test
    fun `calculateItemTax includes CRV in taxable amount`() {
        // Per DEPOSITS_FEES.md: CRV is ALWAYS included in taxable amount
        // Soda: $2.99 + $0.10 CRV = $3.09 taxable
        val priceAfterDiscount = BigDecimal("2.99")
        val crvAmount = BigDecimal("0.10")
        val taxRate = BigDecimal("9.5")
        val quantity = BigDecimal.ONE
        
        val (taxPerUnit, taxTotal) = calculator.calculateItemTax(
            priceAfterDiscount = priceAfterDiscount,
            crvAmount = crvAmount,
            taxPercentSum = taxRate,
            quantity = quantity,
            isSnapEligible = false
        )
        
        // Tax on $3.09 at 9.5% = $0.29
        assertEquals(BigDecimal("0.29"), taxPerUnit)
        assertEquals(BigDecimal("0.29"), taxTotal)
    }
    
    @Test
    fun `calculateItemTax returns zero for SNAP items even with CRV`() {
        // Water bottle: SNAP eligible, has CRV, but no tax
        val priceAfterDiscount = BigDecimal("1.49")
        val crvAmount = BigDecimal("0.05")
        val taxRate = BigDecimal("9.5")
        val quantity = BigDecimal.ONE
        
        val (taxPerUnit, taxTotal) = calculator.calculateItemTax(
            priceAfterDiscount = priceAfterDiscount,
            crvAmount = crvAmount,
            taxPercentSum = taxRate,
            quantity = quantity,
            isSnapEligible = true
        )
        
        assertEquals(0, taxPerUnit.compareTo(BigDecimal.ZERO))
        assertEquals(0, taxTotal.compareTo(BigDecimal.ZERO))
    }
    
    @Test
    fun `calculateItemTax handles multiple quantities correctly`() {
        // 3 sodas: each taxed at rounded per-unit, then multiplied
        val priceAfterDiscount = BigDecimal("2.99")
        val crvAmount = BigDecimal("0.10")
        val taxRate = BigDecimal("9.5")
        val quantity = BigDecimal("3")
        
        val (taxPerUnit, taxTotal) = calculator.calculateItemTax(
            priceAfterDiscount = priceAfterDiscount,
            crvAmount = crvAmount,
            taxPercentSum = taxRate,
            quantity = quantity,
            isSnapEligible = false
        )
        
        // Tax per unit: $0.29
        assertEquals(BigDecimal("0.29"), taxPerUnit)
        // Total: $0.29 × 3 = $0.87
        assertEquals(BigDecimal("0.87"), taxTotal)
    }
}

