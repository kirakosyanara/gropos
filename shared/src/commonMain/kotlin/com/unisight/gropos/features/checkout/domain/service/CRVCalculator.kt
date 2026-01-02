package com.unisight.gropos.features.checkout.domain.service

import com.unisight.gropos.features.checkout.domain.model.CartItem
import com.unisight.gropos.features.checkout.domain.model.Product
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * CRV (California Redemption Value) calculation service.
 * 
 * Per DEPOSITS_FEES.md:
 * - CRV is a mandatory deposit on beverage containers in California
 * - CRV is ALWAYS subject to sales tax (California law)
 * - CRV rates: $0.05 for containers under 24oz, $0.10 for 24oz+
 * 
 * Per SERVICES.md: Pure function service with no side effects.
 * 
 * IMPORTANT: CRV is NOT a separate fee for tax purposes.
 * Per CDTFA, CRV is considered part of gross receipts and MUST be included
 * in the taxable amount.
 */
class CRVCalculator {
    
    /**
     * Get CRV rate for a product.
     * 
     * The rate is stored on the product. Future versions may look up from
     * a CRV repository based on container size/type.
     * 
     * @param product The product to get CRV rate for
     * @return CRV rate per unit
     */
    fun getCRVRate(product: Product): BigDecimal {
        return product.crvRatePerUnit
    }
    
    /**
     * Calculate CRV for a single line item.
     * 
     * @param crvPerUnit CRV rate per unit
     * @param quantity Number of units
     * @return Total CRV for the line
     */
    fun calculateLineCRV(
        crvPerUnit: BigDecimal,
        quantity: BigDecimal
    ): BigDecimal {
        if (crvPerUnit <= BigDecimal.ZERO) {
            return BigDecimal.ZERO
        }
        
        return crvPerUnit.multiply(quantity).setScale(2, RoundingMode.HALF_UP)
    }
    
    /**
     * Calculate total CRV for all items in an order.
     * 
     * Per DEPOSITS_FEES.md: CRV is tracked separately for reporting
     * but is included in the grand total.
     * 
     * @param items List of cart items
     * @return Total CRV amount
     */
    fun calculateTotalCRV(items: List<CartItem>): BigDecimal {
        return items
            .filterNot { it.isRemoved }
            .filter { it.crvRatePerUnit > BigDecimal.ZERO }
            .fold(BigDecimal.ZERO) { acc, item ->
                acc.add(item.crvTotal)
            }
            .setScale(2, RoundingMode.HALF_UP)
    }
    
    /**
     * Calculate item CRV for a product.
     * 
     * @param product The product
     * @param quantity Number of units
     * @return Pair of (crvPerUnit, crvTotal)
     */
    fun calculateItemCRV(
        product: Product,
        quantity: BigDecimal
    ): Pair<BigDecimal, BigDecimal> {
        val crvPerUnit = getCRVRate(product)
        val crvTotal = calculateLineCRV(crvPerUnit, quantity)
        return Pair(crvPerUnit, crvTotal)
    }
}

