package com.unisight.gropos.features.checkout.domain.service

import com.unisight.gropos.features.checkout.domain.model.CartItem
import com.unisight.gropos.features.checkout.domain.model.Product
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Tax calculation service.
 * 
 * Per TAX_CALCULATIONS.md - The Tax Consistency Rule:
 * Tax MUST be calculated on a SINGLE UNIT first, rounded, THEN multiplied 
 * by quantity. This ensures fairness and consistency.
 * 
 * Per SERVICES.md: Pure function service with no side effects.
 * 
 * Key Rules:
 * 1. CRV is ALWAYS included in taxable amount (California law)
 * 2. SNAP-eligible items are tax-exempt (per BUSINESS_RULES.md)
 * 3. Tax per unit is rounded BEFORE multiplying by quantity
 */
class TaxCalculator {
    
    /**
     * Calculate tax for a single unit of product.
     * 
     * Per TAX_CALCULATIONS.md:
     * - Taxable amount = finalPrice (includes CRV)
     * - SNAP items are tax-exempt
     * - Tax is rounded per-unit BEFORE multiplying by quantity
     * 
     * @param taxableAmount The amount to apply tax to (price + CRV)
     * @param taxPercentSum Combined tax rate (e.g., 9.5 for 9.5%)
     * @param isSnapEligible Whether item qualifies for SNAP (tax-exempt)
     * @return Tax amount per unit, rounded to 2 decimal places
     */
    fun calculateTaxPerUnit(
        taxableAmount: BigDecimal,
        taxPercentSum: BigDecimal,
        isSnapEligible: Boolean
    ): BigDecimal {
        // SNAP items are tax-exempt per BUSINESS_RULES.md
        if (isSnapEligible) {
            return BigDecimal.ZERO
        }
        
        if (taxableAmount <= BigDecimal.ZERO) {
            return BigDecimal.ZERO
        }
        
        if (taxPercentSum <= BigDecimal.ZERO) {
            return BigDecimal.ZERO
        }
        
        // Calculate tax for ONE unit
        val tax = taxableAmount.multiply(taxPercentSum)
            .divide(BigDecimal("100"), 2, RoundingMode.HALF_UP)
        
        return tax
    }
    
    /**
     * Calculate total tax for a line item.
     * 
     * Per TAX_CALCULATIONS.md - The Tax Consistency Rule:
     * Tax per unit is calculated and ROUNDED first, THEN multiplied by quantity.
     * This ensures 3 customers buying 1 item pay same total as 1 customer buying 3.
     * 
     * @param taxPerUnit Pre-calculated tax per unit (already rounded)
     * @param quantity Number of units
     * @return Total tax for the line
     */
    fun calculateLineTax(
        taxPerUnit: BigDecimal,
        quantity: BigDecimal
    ): BigDecimal {
        return taxPerUnit.multiply(quantity).setScale(2, RoundingMode.HALF_UP)
    }
    
    /**
     * Calculate tax for a product given its pricing components.
     * 
     * This is the main entry point for calculating item tax.
     * 
     * @param priceAfterDiscount Product price after discounts
     * @param crvAmount CRV amount per unit
     * @param taxPercentSum Combined tax rate percentage
     * @param quantity Number of units
     * @param isSnapEligible Whether item is SNAP eligible (tax-exempt)
     * @return Pair of (taxPerUnit, taxTotal)
     */
    fun calculateItemTax(
        priceAfterDiscount: BigDecimal,
        crvAmount: BigDecimal,
        taxPercentSum: BigDecimal,
        quantity: BigDecimal,
        isSnapEligible: Boolean
    ): Pair<BigDecimal, BigDecimal> {
        // Per TAX_CALCULATIONS.md: CRV is ALWAYS included in taxable amount
        // "CRV is considered part of the gross receipts from the sale"
        val taxableAmount = priceAfterDiscount.add(crvAmount)
        
        // Calculate per-unit tax (ROUNDED)
        val taxPerUnit = calculateTaxPerUnit(taxableAmount, taxPercentSum, isSnapEligible)
        
        // Multiply rounded per-unit tax by quantity
        val taxTotal = calculateLineTax(taxPerUnit, quantity)
        
        return Pair(taxPerUnit, taxTotal)
    }
    
    /**
     * Calculate total tax for all items in an order.
     * 
     * @param items List of cart items
     * @return Total tax amount
     */
    fun calculateTotalTax(items: List<CartItem>): BigDecimal {
        return items
            .filterNot { it.isRemoved }
            .fold(BigDecimal.ZERO) { acc, item ->
                acc.add(item.taxTotal)
            }
            .setScale(2, RoundingMode.HALF_UP)
    }
}

