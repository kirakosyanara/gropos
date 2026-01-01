package com.unisight.gropos.features.checkout.domain.repository

import com.unisight.gropos.features.checkout.domain.model.Cart
import com.unisight.gropos.features.checkout.domain.model.Product
import kotlinx.coroutines.flow.StateFlow
import java.math.BigDecimal

/**
 * Repository for managing the shopping cart state.
 * 
 * Per ARCHITECTURE_BLUEPRINT.md (Section 6):
 * - Cart state must be shared between Cashier and Customer Display windows
 * - Single Source of Truth pattern: One cart instance observed by multiple ViewModels
 * 
 * This is the central repository that holds cart state. Both CheckoutViewModel
 * (Cashier window) and CustomerDisplayViewModel (Customer window) observe this.
 * 
 * Implementation must be a SINGLETON in DI to ensure state synchronization.
 * 
 * Per reliability-rules.mdc: All write operations are idempotent.
 * Per code-quality.mdc: Use BigDecimal for monetary calculations.
 */
interface CartRepository {
    
    /**
     * Current cart state as an observable flow.
     * 
     * Both Cashier and Customer Display screens observe this same flow.
     * Any changes made via addToCart/removeFromCart are immediately 
     * reflected to all observers.
     */
    val cart: StateFlow<Cart>
    
    /**
     * Adds a product to the cart.
     * 
     * Business Logic:
     * - If product exists (by branchProductId), increment quantity
     * - If new product, add as new line item
     * 
     * @param product The product to add
     * @param quantity The quantity to add (defaults to 1)
     */
    suspend fun addToCart(product: Product, quantity: BigDecimal = BigDecimal.ONE)
    
    /**
     * Removes a product from the cart completely.
     * 
     * @param branchProductId The product ID to remove
     */
    suspend fun removeFromCart(branchProductId: Int)
    
    /**
     * Voids a product in the cart (marks as removed but keeps in history).
     * 
     * Per DATABASE_SCHEMA.md: Voided items remain in transaction for audit.
     * 
     * @param branchProductId The product ID to void
     */
    suspend fun voidItem(branchProductId: Int)
    
    /**
     * Updates the quantity of an item in the cart.
     * 
     * @param branchProductId The product ID
     * @param newQuantity The new quantity
     */
    suspend fun updateQuantity(branchProductId: Int, newQuantity: BigDecimal)
    
    /**
     * Clears all items from the cart.
     * 
     * Called after transaction completion or void all.
     */
    suspend fun clearCart()
    
    /**
     * Gets the current cart value (non-reactive).
     * 
     * Prefer observing [cart] StateFlow for reactive updates.
     */
    fun getCurrentCart(): Cart
}

