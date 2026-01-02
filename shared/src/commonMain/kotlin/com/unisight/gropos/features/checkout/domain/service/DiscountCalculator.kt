package com.unisight.gropos.features.checkout.domain.service

import com.unisight.gropos.features.checkout.domain.model.CartItem
import com.unisight.gropos.features.checkout.domain.model.Product
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Discount calculation service.
 * 
 * Per SERVICES.md:
 * - Calculates total savings from discounts
 * - Applies percentage and fixed amount discounts
 * - Respects floor price constraints
 * 
 * Per SERVICES.md: Pure function service with no side effects.
 * 
 * Discount Priority (per SERVICES.md - PriceCalculator):
 * 1. Prompted Price (manual override)
 * 2. Customer-specific price
 * 3. Sale Price (promotion)
 * 4. Bulk/Volume discount
 * 5. Retail Price
 */
class DiscountCalculator {
    
    /**
     * Calculate the effective price for a product.
     * 
     * Per SERVICES.md - PriceCalculator.getPriceUsed():
     * Priority: Prompted → Customer → Sale → Bulk → Retail
     * 
     * @param product The product
     * @param promptedPrice Manually entered price (if any)
     * @return The effective unit price
     */
    fun getEffectivePrice(
        product: Product,
        promptedPrice: BigDecimal? = null
    ): BigDecimal {
        // Check for prompted price first
        if (promptedPrice != null && promptedPrice > BigDecimal.ZERO) {
            return promptedPrice
        }
        
        // Check for sale price
        val salePrice = product.currentSale?.discountedPrice
        if (salePrice != null && salePrice > BigDecimal.ZERO) {
            return salePrice
        }
        
        // Default to retail price
        return product.retailPrice
    }
    
    /**
     * Apply a percentage discount to a price.
     * 
     * @param price Original price
     * @param percentage Discount percentage (e.g., 10 for 10%)
     * @return Price after discount
     */
    fun applyPercentageDiscount(
        price: BigDecimal,
        percentage: BigDecimal
    ): BigDecimal {
        val discountAmount = price.multiply(percentage)
            .divide(BigDecimal("100"), 2, RoundingMode.HALF_UP)
        
        return price.subtract(discountAmount)
            .setScale(2, RoundingMode.HALF_UP)
    }
    
    /**
     * Apply a fixed amount discount to a price.
     * 
     * @param price Original price
     * @param discountAmount Fixed discount amount
     * @return Price after discount (minimum 0)
     */
    fun applyFixedDiscount(
        price: BigDecimal,
        discountAmount: BigDecimal
    ): BigDecimal {
        return price.subtract(discountAmount)
            .coerceAtLeast(BigDecimal.ZERO)
            .setScale(2, RoundingMode.HALF_UP)
    }
    
    /**
     * Calculate price after discount, respecting floor price.
     * 
     * Per SERVICES.md - PriceCalculator.getFinalPrice():
     * - Calculated price = priceUsed - lineDiscount - transactionDiscount
     * - If result < floor price, use floor price (unless overridden)
     * - Sale prices can be below floor (corporate-set)
     * 
     * @param priceUsed The price being used
     * @param lineDiscount Per-item discount amount
     * @param transactionDiscount Transaction-level discount amount
     * @param floorPrice Minimum allowed price
     * @param isSalePrice Whether this is a sale price (can be below floor)
     * @param isFloorOverridden Whether floor is overridden by manager
     * @return Final price after discounts, respecting floor
     */
    fun calculateFinalPrice(
        priceUsed: BigDecimal,
        lineDiscount: BigDecimal = BigDecimal.ZERO,
        transactionDiscount: BigDecimal = BigDecimal.ZERO,
        floorPrice: BigDecimal? = null,
        isSalePrice: Boolean = false,
        isFloorOverridden: Boolean = false
    ): BigDecimal {
        val calculatedPrice = priceUsed
            .subtract(lineDiscount)
            .subtract(transactionDiscount)
        
        // If floor is overridden, use calculated price
        if (isFloorOverridden) {
            return calculatedPrice.coerceAtLeast(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP)
        }
        
        // Sale prices can be below floor (set by corporate)
        if (isSalePrice && floorPrice != null && priceUsed < floorPrice) {
            return priceUsed.setScale(2, RoundingMode.HALF_UP)
        }
        
        // Enforce floor price
        if (floorPrice != null && calculatedPrice < floorPrice) {
            return floorPrice.setScale(2, RoundingMode.HALF_UP)
        }
        
        return calculatedPrice.coerceAtLeast(BigDecimal.ZERO)
            .setScale(2, RoundingMode.HALF_UP)
    }
    
    /**
     * Calculate savings per unit.
     * 
     * Per SERVICES.md - DiscountCalculator.calculateSavingsPerUnit():
     * Savings = (basePrice + CRV) - finalPrice
     * 
     * @param retailPrice Original retail price
     * @param effectivePrice Price being charged
     * @param lineDiscount Per-item discount
     * @param transactionDiscount Transaction discount
     * @return Savings amount per unit
     */
    fun calculateSavingsPerUnit(
        retailPrice: BigDecimal,
        effectivePrice: BigDecimal,
        lineDiscount: BigDecimal = BigDecimal.ZERO,
        transactionDiscount: BigDecimal = BigDecimal.ZERO
    ): BigDecimal {
        val savingsFromPrice = retailPrice.subtract(effectivePrice)
        val totalSavings = savingsFromPrice
            .add(lineDiscount)
            .add(transactionDiscount)
        
        return totalSavings.coerceAtLeast(BigDecimal.ZERO)
            .setScale(2, RoundingMode.HALF_UP)
    }
    
    /**
     * Calculate total savings for all items.
     * 
     * @param items List of cart items
     * @return Total savings amount
     */
    fun calculateTotalSavings(items: List<CartItem>): BigDecimal {
        return items
            .filterNot { it.isRemoved }
            .fold(BigDecimal.ZERO) { acc, item ->
                acc.add(item.savingsTotal)
            }
            .setScale(2, RoundingMode.HALF_UP)
    }
}

