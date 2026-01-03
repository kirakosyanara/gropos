package com.unisight.gropos.features.pricing.domain.model

import java.math.BigDecimal

/**
 * Represents a tax definition.
 * 
 * Per COUCHBASE_LOCAL_STORAGE.md: Tax collection schema.
 * Used for standalone tax rate lookups and updates.
 * 
 * @property id Unique tax identifier
 * @property name Tax display name (e.g., "State Sales Tax", "County Tax")
 * @property percent Tax rate as decimal (0.0875 = 8.75%)
 * @property createdDate Record creation timestamp
 * @property updatedDate Last update timestamp
 */
data class Tax(
    val id: Int,
    val name: String,
    val percent: BigDecimal,
    val createdDate: String? = null,
    val updatedDate: String? = null
) {
    /**
     * Calculates tax amount for a given taxable value.
     * Uses HALF_UP rounding for proper currency handling.
     * 
     * @param taxableAmount The amount to calculate tax on
     * @return The tax amount rounded to 2 decimal places
     */
    fun calculateTax(taxableAmount: BigDecimal): BigDecimal {
        return taxableAmount.multiply(percent)
            .setScale(2, java.math.RoundingMode.HALF_UP)
    }
    
    /**
     * Returns the tax rate as a percentage (e.g., 8.75 for 8.75%).
     */
    val percentAsDisplay: BigDecimal
        get() = percent.multiply(BigDecimal(100))
}

/**
 * Represents a California Redemption Value (CRV) tier.
 * 
 * Per COUCHBASE_LOCAL_STORAGE.md: CRV collection schema.
 * Container deposit rates for beverages.
 * 
 * @property id Unique CRV tier identifier
 * @property name CRV tier name (e.g., "CRV Under 24oz", "CRV 24oz and Over")
 * @property rate CRV rate per container (e.g., 0.05 for $0.05)
 */
data class Crv(
    val id: Int,
    val name: String,
    val rate: BigDecimal
) {
    /**
     * Calculates total CRV for a given quantity.
     * 
     * @param quantity Number of containers
     * @return Total CRV amount
     */
    fun calculateTotal(quantity: BigDecimal): BigDecimal {
        return rate.multiply(quantity)
            .setScale(2, java.math.RoundingMode.HALF_UP)
    }
    
    /**
     * Returns the rate formatted for display (e.g., "$0.05").
     */
    val rateFormatted: String
        get() = "\$${rate.setScale(2, java.math.RoundingMode.HALF_UP)}"
}

