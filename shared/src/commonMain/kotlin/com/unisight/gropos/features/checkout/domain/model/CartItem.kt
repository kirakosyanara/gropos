package com.unisight.gropos.features.checkout.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Represents a line item in the shopping cart.
 * 
 * Per DATABASE_SCHEMA.md - TransactionProduct (TransactionItem) structure.
 * Field names align with the schema for future serialization compatibility.
 *
 * Per code-quality.mdc: Use BigDecimal for all monetary calculations.
 *
 * @property product The product being purchased
 * @property quantityUsed Number of units (BigDecimal to support weighted items)
 * @property priceUsed The price used for this item (may differ from retail if overridden)
 * @property discountAmountPerUnit Discount applied per unit
 * @property isRemoved Whether item has been voided
 * @property isPromptedPrice Whether price was manually entered
 * @property isFloorPriceOverridden Whether floor price was overridden
 */
data class CartItem(
    val product: Product,
    val quantityUsed: BigDecimal = BigDecimal.ONE,
    val priceUsed: BigDecimal? = null,
    val discountAmountPerUnit: BigDecimal = BigDecimal.ZERO,
    val transactionDiscountAmountPerUnit: BigDecimal = BigDecimal.ZERO,
    val isRemoved: Boolean = false,
    val isPromptedPrice: Boolean = false,
    val isFloorPriceOverridden: Boolean = false,
    val scanDateTime: String? = null
) {
    init {
        require(quantityUsed > BigDecimal.ZERO) { "Quantity must be positive" }
    }
    
    // ========================================================================
    // Schema-aligned computed properties (per DATABASE_SCHEMA.md)
    // ========================================================================
    
    /**
     * branchProductId from the product.
     */
    val branchProductId: Int
        get() = product.branchProductId
    
    /**
     * Product name for display.
     */
    val branchProductName: String
        get() = product.productName
    
    /**
     * Unit type (e.g., "ea", "lb").
     */
    val unitType: String
        get() = if (product.soldById == "Weight") "lb" else "ea"
    
    /**
     * Retail price per unit.
     */
    val retailPrice: BigDecimal
        get() = product.retailPrice
    
    /**
     * Sale price per unit (if on sale).
     */
    val salePrice: BigDecimal?
        get() = product.currentSale?.discountedPrice
    
    /**
     * The effective price per unit used for this item.
     */
    val effectivePrice: BigDecimal
        get() = priceUsed ?: salePrice ?: retailPrice
    
    /**
     * Floor price from product.
     */
    val floorPrice: BigDecimal?
        get() = product.floorPrice
    
    /**
     * Tax per unit (calculated from product tax rates).
     */
    val taxPerUnit: BigDecimal
        get() {
            val taxableAmount = effectivePrice.subtract(discountAmountPerUnit)
            return taxableAmount.multiply(product.totalTaxPercent)
                .divide(BigDecimal("100"), 2, RoundingMode.HALF_UP)
        }
    
    /**
     * Total tax for this line item.
     */
    val taxTotal: BigDecimal
        get() = taxPerUnit.multiply(quantityUsed).setScale(2, RoundingMode.HALF_UP)
    
    /**
     * CRV rate per unit.
     */
    val crvRatePerUnit: BigDecimal
        get() = product.crvRatePerUnit
    
    /**
     * Total CRV for this line item.
     */
    val crvTotal: BigDecimal
        get() = crvRatePerUnit.multiply(quantityUsed).setScale(2, RoundingMode.HALF_UP)
    
    /**
     * Subtotal before tax (price * quantity - discounts).
     */
    val subTotal: BigDecimal
        get() {
            val priceAfterDiscount = effectivePrice.subtract(discountAmountPerUnit)
                .subtract(transactionDiscountAmountPerUnit)
            return priceAfterDiscount.multiply(quantityUsed).setScale(2, RoundingMode.HALF_UP)
        }
    
    /**
     * Total savings from sale price and discounts.
     */
    val savingsTotal: BigDecimal
        get() {
            val savingsPerUnit = retailPrice.subtract(effectivePrice)
                .add(discountAmountPerUnit)
                .add(transactionDiscountAmountPerUnit)
            return savingsPerUnit.multiply(quantityUsed).setScale(2, RoundingMode.HALF_UP)
        }
    
    /**
     * Line total = subtotal + tax + CRV.
     * 
     * Per testing-strategy.mdc: Money calculations must use BigDecimal
     * to ensure exact matching in tests.
     */
    val lineTotal: BigDecimal
        get() = subTotal.add(taxTotal).add(crvTotal)
    
    /**
     * Tax indicator for receipt (T = taxable, F = food stamp eligible).
     */
    val taxIndicator: String
        get() = if (product.isFoodStampEligible) "F" else "T"
    
    /**
     * Whether item is food stamp eligible.
     */
    val isFoodStampEligible: Boolean
        get() = product.isFoodStampEligible
    
    /**
     * Sold by type from product.
     */
    val soldById: String
        get() = product.soldById
    
    // ========================================================================
    // Cart operations
    // ========================================================================
    
    /**
     * Creates a new CartItem with incremented quantity.
     * Immutable pattern - returns new instance.
     */
    fun incrementQuantity(amount: BigDecimal = BigDecimal.ONE): CartItem {
        return copy(quantityUsed = quantityUsed.add(amount))
    }
    
    /**
     * Creates a new CartItem with updated quantity.
     */
    fun withQuantity(newQuantity: BigDecimal): CartItem {
        return copy(quantityUsed = newQuantity)
    }
    
    /**
     * Creates a new CartItem with applied discount.
     */
    fun withDiscount(discount: BigDecimal): CartItem {
        return copy(discountAmountPerUnit = discount)
    }
}
