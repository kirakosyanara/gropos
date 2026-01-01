package com.unisight.gropos.features.checkout.domain.model

import java.math.BigDecimal

/**
 * Represents the shopping cart containing all items in the current transaction.
 * 
 * This is an immutable data structure - all modifications return new instances.
 * Per code-quality.mdc: Use BigDecimal for all monetary calculations.
 *
 * @property items List of cart items in the order they were added
 */
data class Cart(
    val items: List<CartItem> = emptyList()
) {
    /**
     * Total price of all items in the cart.
     * 
     * Sums all line totals using BigDecimal.add() for precision.
     * Per testing-strategy.mdc: Must match exact BigDecimal values in tests.
     */
    val total: BigDecimal
        get() = items.fold(BigDecimal.ZERO) { acc, item ->
            acc.add(item.lineTotal)
        }
    
    /**
     * Total number of items (sum of all quantities).
     */
    val itemCount: BigDecimal
        get() = items.fold(BigDecimal.ZERO) { acc, item ->
            acc.add(item.quantity)
        }
    
    /**
     * Number of unique products in the cart.
     */
    val uniqueItemCount: Int
        get() = items.size
    
    /**
     * Whether the cart is empty.
     */
    val isEmpty: Boolean
        get() = items.isEmpty()
    
    /**
     * Adds a product to the cart.
     * 
     * Business Logic:
     * - If product already exists (by SKU), increment quantity
     * - If new product, add as new line item
     * 
     * @param product The product to add
     * @param quantity The quantity to add (defaults to 1)
     * @return New Cart instance with the product added
     */
    fun addProduct(product: Product, quantity: BigDecimal = BigDecimal.ONE): Cart {
        val existingIndex = items.indexOfFirst { it.product.sku == product.sku }
        
        return if (existingIndex >= 0) {
            // Product exists - increment quantity
            val updatedItems = items.toMutableList()
            updatedItems[existingIndex] = items[existingIndex].incrementQuantity(quantity)
            copy(items = updatedItems)
        } else {
            // New product - add to list
            copy(items = items + CartItem(product, quantity))
        }
    }
    
    /**
     * Removes an item from the cart by SKU.
     * 
     * @param sku The SKU of the product to remove
     * @return New Cart instance with the item removed
     */
    fun removeProduct(sku: String): Cart {
        return copy(items = items.filterNot { it.product.sku == sku })
    }
    
    /**
     * Clears all items from the cart.
     * 
     * @return New empty Cart instance
     */
    fun clear(): Cart = Cart()
    
    companion object {
        /**
         * Creates an empty cart.
         */
        fun empty(): Cart = Cart()
    }
}

