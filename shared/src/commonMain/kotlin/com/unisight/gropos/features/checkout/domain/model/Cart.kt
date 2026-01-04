package com.unisight.gropos.features.checkout.domain.model

import java.math.BigDecimal
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * Represents the shopping cart containing all items in the current transaction.
 * 
 * This is an immutable data structure - all modifications return new instances.
 * Per DATABASE_SCHEMA.md - Aligns with LocalTransaction structure.
 * Per code-quality.mdc: Use BigDecimal for all monetary calculations.
 *
 * @property items List of cart items in the order they were added
 */
data class Cart(
    val items: List<CartItem> = emptyList()
) {
    /**
     * Subtotal before tax (sum of all line subtotals).
     */
    val subTotal: BigDecimal
        get() = items
            .filterNot { it.isRemoved }
            .fold(BigDecimal.ZERO) { acc, item -> acc.add(item.subTotal) }
    
    /**
     * Total discount amount.
     */
    val discountTotal: BigDecimal
        get() = items
            .filterNot { it.isRemoved }
            .fold(BigDecimal.ZERO) { acc, item -> acc.add(item.savingsTotal) }
    
    /**
     * Total tax amount.
     */
    val taxTotal: BigDecimal
        get() = items
            .filterNot { it.isRemoved }
            .fold(BigDecimal.ZERO) { acc, item -> acc.add(item.taxTotal) }
    
    /**
     * Total CRV amount.
     */
    val crvTotal: BigDecimal
        get() = items
            .filterNot { it.isRemoved }
            .fold(BigDecimal.ZERO) { acc, item -> acc.add(item.crvTotal) }
    
    /**
     * Total amount eligible for SNAP payment.
     * 
     * Per PAYMENT_PROCESSING.md: SNAP can only be used for SNAP-eligible items.
     * This is the subtotal of SNAP-eligible items (no tax since SNAP = tax-exempt).
     */
    val snapEligibleTotal: BigDecimal
        get() = items
            .filterNot { it.isRemoved }
            .filter { it.isSnapEligible }
            .fold(BigDecimal.ZERO) { acc, item -> acc.add(item.subTotal).add(item.crvTotal) }
    
    /**
     * Grand total (subtotal + tax + CRV).
     * 
     * Per DATABASE_SCHEMA.md: grandTotal field in Transaction.
     * Per testing-strategy.mdc: Must match exact BigDecimal values in tests.
     */
    val total: BigDecimal
        get() = subTotal.add(taxTotal).add(crvTotal)
    
    /**
     * Alias for total to match schema naming.
     */
    val grandTotal: BigDecimal
        get() = total
    
    /**
     * Total number of items (sum of all quantities).
     */
    val itemCount: BigDecimal
        get() = items
            .filterNot { it.isRemoved }
            .fold(BigDecimal.ZERO) { acc, item -> acc.add(item.quantityUsed) }
    
    /**
     * Number of unique products in the cart.
     */
    val uniqueItemCount: Int
        get() = items.filterNot { it.isRemoved }.size
    
    /**
     * Whether the cart is empty.
     */
    val isEmpty: Boolean
        get() = items.none { !it.isRemoved }
    
    /**
     * Adds a product to the cart.
     * 
     * Business Logic:
     * - If product already exists (by branchProductId), increment quantity
     * - If new product, add as new line item
     * 
     * Per DATABASE_SCHEMA.md: Products are identified by branchProductId.
     * 
     * @param product The product to add
     * @param quantity The quantity to add (defaults to 1)
     * @return New Cart instance with the product added
     */
    fun addProduct(product: Product, quantity: BigDecimal = BigDecimal.ONE): Cart {
        val existingIndex = items.indexOfFirst { 
            it.product.branchProductId == product.branchProductId && !it.isRemoved 
        }
        
        return if (existingIndex >= 0) {
            // Product exists - increment quantity
            val updatedItems = items.toMutableList()
            updatedItems[existingIndex] = items[existingIndex].incrementQuantity(quantity)
            copy(items = updatedItems)
        } else {
            // New product - add to list with current timestamp
            val scanDateTime = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
            copy(items = items + CartItem(
                product = product,
                quantityUsed = quantity,
                scanDateTime = scanDateTime
            ))
        }
    }
    
    /**
     * Removes an item from the cart by branchProductId.
     * 
     * Per DATABASE_SCHEMA.md: Uses branchProductId as identifier.
     * 
     * @param branchProductId The branchProductId of the product to remove
     * @return New Cart instance with the item removed
     */
    fun removeProduct(branchProductId: Int): Cart {
        return copy(items = items.filterNot { 
            it.product.branchProductId == branchProductId 
        })
    }
    
    /**
     * Voids an item in the cart (marks as removed but keeps in history).
     * 
     * @param branchProductId The branchProductId of the product to void
     * @return New Cart instance with the item voided
     */
    fun voidProduct(branchProductId: Int): Cart {
        return copy(items = items.map { 
            if (it.product.branchProductId == branchProductId) {
                it.copy(isRemoved = true)
            } else {
                it
            }
        })
    }
    
    /**
     * Updates quantity for an item.
     * 
     * @param branchProductId The branchProductId of the product
     * @param newQuantity The new quantity
     * @return New Cart instance with updated quantity
     */
    fun updateQuantity(branchProductId: Int, newQuantity: BigDecimal): Cart {
        return copy(items = items.map {
            if (it.product.branchProductId == branchProductId) {
                it.withQuantity(newQuantity)
            } else {
                it
            }
        })
    }
    
    /**
     * Clears all items from the cart.
     * 
     * @return New empty Cart instance
     */
    fun clear(): Cart = Cart()
    
    /**
     * Finds an item by branchProductId.
     */
    fun findItem(branchProductId: Int): CartItem? {
        return items.find { 
            it.product.branchProductId == branchProductId && !it.isRemoved 
        }
    }
    
    /**
     * Applies a percentage discount to all items in the cart.
     * 
     * Per FUNCTIONS_MENU.md: Transaction Discount applies to entire order.
     * The discount is applied as a transaction-level discount on each item.
     * 
     * @param discountPercent The discount as a decimal (e.g., 0.10 for 10%)
     * @return New Cart instance with the discount applied
     */
    fun applyTransactionDiscount(discountPercent: BigDecimal): Cart {
        return copy(items = items.map { item ->
            if (!item.isRemoved) {
                // Calculate discount per unit based on effective price
                val discountPerUnit = item.effectivePrice.multiply(discountPercent)
                item.copy(transactionDiscountAmountPerUnit = discountPerUnit)
            } else {
                item
            }
        })
    }
    
    companion object {
        /**
         * Creates an empty cart.
         */
        fun empty(): Cart = Cart()
    }
}
