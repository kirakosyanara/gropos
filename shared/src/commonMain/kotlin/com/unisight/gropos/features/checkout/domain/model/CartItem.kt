package com.unisight.gropos.features.checkout.domain.model

import java.math.BigDecimal

/**
 * Represents a line item in the shopping cart.
 * 
 * Per code-quality.mdc: Use BigDecimal for all monetary calculations.
 *
 * @property product The product being purchased
 * @property quantity Number of units (BigDecimal to support weighted items)
 */
data class CartItem(
    val product: Product,
    val quantity: BigDecimal = BigDecimal.ONE
) {
    init {
        require(quantity > BigDecimal.ZERO) { "Quantity must be positive" }
    }
    
    /**
     * Line total = unit price * quantity.
     * 
     * Per testing-strategy.mdc: Money calculations must use BigDecimal
     * to ensure exact matching in tests.
     */
    val lineTotal: BigDecimal
        get() = product.price.multiply(quantity)
    
    /**
     * Creates a new CartItem with incremented quantity.
     * Immutable pattern - returns new instance.
     */
    fun incrementQuantity(amount: BigDecimal = BigDecimal.ONE): CartItem {
        return copy(quantity = quantity.add(amount))
    }
}

