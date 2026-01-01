package com.unisight.gropos.core.util

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Interface for formatting currency values.
 * 
 * Abstraction for currency formatting to support multiplatform.
 * In commonMain we cannot use Java's NumberFormat directly,
 * so we define this interface for potential expect/actual pattern later.
 * 
 * Per code-quality.mdc: Use BigDecimal for monetary values.
 */
interface CurrencyFormatter {
    /**
     * Formats a BigDecimal amount as a currency string.
     * 
     * @param amount The monetary amount to format
     * @return Formatted currency string (e.g., "$5.99")
     */
    fun format(amount: BigDecimal): String
    
    /**
     * Formats a BigDecimal amount with sign indicator.
     * Useful for displaying discounts or credits.
     * 
     * @param amount The monetary amount to format
     * @param showPositiveSign Whether to show "+" for positive amounts
     * @return Formatted currency string with sign
     */
    fun formatWithSign(amount: BigDecimal, showPositiveSign: Boolean = false): String
}

/**
 * Basic USD currency formatter implementation.
 * 
 * This is a simple implementation for the Walking Skeleton.
 * TODO: Replace with expect/actual for proper locale-aware formatting.
 */
class UsdCurrencyFormatter : CurrencyFormatter {
    
    override fun format(amount: BigDecimal): String {
        val scaled = amount.setScale(2, RoundingMode.HALF_UP)
        val isNegative = scaled < BigDecimal.ZERO
        val absoluteValue = scaled.abs()
        
        return if (isNegative) {
            "-$$absoluteValue"
        } else {
            "$$absoluteValue"
        }
    }
    
    override fun formatWithSign(amount: BigDecimal, showPositiveSign: Boolean): String {
        val scaled = amount.setScale(2, RoundingMode.HALF_UP)
        val isNegative = scaled < BigDecimal.ZERO
        val absoluteValue = scaled.abs()
        
        return when {
            isNegative -> "-$$absoluteValue"
            showPositiveSign && scaled > BigDecimal.ZERO -> "+$$absoluteValue"
            else -> "$$absoluteValue"
        }
    }
}

