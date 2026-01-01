package com.unisight.gropos.features.checkout.domain.model

import java.math.BigDecimal

/**
 * Represents a product in the POS system.
 * 
 * Per code-quality.mdc: Use BigDecimal for currency, never Float/Double.
 * This prevents rounding errors in financial calculations.
 *
 * @property id Unique identifier for the product
 * @property name Display name of the product
 * @property price Unit price in dollars (BigDecimal for precision)
 * @property sku Stock Keeping Unit - barcode/scannable identifier
 */
data class Product(
    val id: String,
    val name: String,
    val price: BigDecimal,
    val sku: String
) {
    init {
        require(price >= BigDecimal.ZERO) { "Price cannot be negative" }
    }
}

