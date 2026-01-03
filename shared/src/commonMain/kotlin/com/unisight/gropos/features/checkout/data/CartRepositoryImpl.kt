package com.unisight.gropos.features.checkout.data

import com.unisight.gropos.features.checkout.domain.model.Cart
import com.unisight.gropos.features.checkout.domain.model.Product
import com.unisight.gropos.features.checkout.domain.repository.CartRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.math.BigDecimal

/**
 * In-memory implementation of CartRepository.
 * 
 * This is a SINGLETON that holds the cart state for the entire application.
 * Both Cashier and Customer Display windows observe this same instance.
 * 
 * Per ARCHITECTURE_BLUEPRINT.md:
 * - Single Source of Truth for cart state
 * - Multi-window state synchronization
 * 
 * Per reliability-rules.mdc:
 * - All operations are thread-safe via MutableStateFlow
 * - State updates are atomic
 * 
 * TODO: In production, this would integrate with local database for
 * persistence across app restarts (crash recovery per reliability-rules.mdc).
 */
class CartRepositoryImpl : CartRepository {
    
    private val _cart = MutableStateFlow(Cart.empty())
    
    /**
     * Observable cart state.
     * 
     * All ViewModels observing this flow will receive updates automatically
     * when cart changes occur.
     */
    override val cart: StateFlow<Cart> = _cart.asStateFlow()
    
    /**
     * Adds a product to the cart.
     * 
     * Uses atomic update to ensure thread safety.
     */
    override suspend fun addToCart(product: Product, quantity: BigDecimal) {
        _cart.update { currentCart ->
            currentCart.addProduct(product, quantity)
        }
    }
    
    /**
     * Removes a product from the cart completely.
     */
    override suspend fun removeFromCart(branchProductId: Int) {
        _cart.update { currentCart ->
            currentCart.removeProduct(branchProductId)
        }
    }
    
    /**
     * Voids a product (marks as removed but keeps in history).
     */
    override suspend fun voidItem(branchProductId: Int) {
        _cart.update { currentCart ->
            currentCart.voidProduct(branchProductId)
        }
    }
    
    /**
     * Updates the quantity of an item in the cart.
     */
    override suspend fun updateQuantity(branchProductId: Int, newQuantity: BigDecimal) {
        _cart.update { currentCart ->
            currentCart.updateQuantity(branchProductId, newQuantity)
        }
    }
    
    /**
     * Clears all items from the cart.
     */
    override suspend fun clearCart() {
        _cart.value = Cart.empty()
    }
    
    /**
     * Gets the current cart value (non-reactive snapshot).
     */
    override fun getCurrentCart(): Cart = _cart.value
    
    /**
     * Applies a percentage discount to all items in the cart.
     */
    override suspend fun applyTransactionDiscount(discountPercent: BigDecimal) {
        _cart.update { currentCart ->
            currentCart.applyTransactionDiscount(discountPercent)
        }
    }
}

