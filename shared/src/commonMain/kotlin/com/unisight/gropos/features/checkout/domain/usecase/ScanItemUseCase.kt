package com.unisight.gropos.features.checkout.domain.usecase

import com.unisight.gropos.features.checkout.domain.model.Cart
import com.unisight.gropos.features.checkout.domain.repository.CartRepository
import com.unisight.gropos.features.checkout.domain.repository.ProductRepository
import com.unisight.gropos.features.checkout.domain.repository.ScannerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

/**
 * Result of a scan operation.
 */
sealed interface ScanResult {
    /**
     * Product was found and added to cart.
     */
    data class Success(val cart: Cart) : ScanResult
    
    /**
     * Product was not found for the scanned barcode.
     */
    data class ProductNotFound(val barcode: String) : ScanResult
    
    /**
     * An error occurred during the scan operation.
     */
    data class Error(val message: String) : ScanResult
}

/**
 * Use case for scanning items and adding them to the cart.
 * 
 * This use case:
 * 1. Listens to the scanner flow for barcodes
 * 2. Looks up products using getByBarcode (per DATABASE_SCHEMA.md)
 * 3. Adds found products to the cart via CartRepository
 * 4. Handles quantity incrementing for duplicate scans
 * 
 * Per ARCHITECTURE_BLUEPRINT.md:
 * - Cart state is managed by CartRepository (Singleton)
 * - This UseCase delegates cart operations to the repository
 * - Multiple ViewModels can observe the same cart state
 * 
 * Per project-structure.mdc: UseCase naming is Verb-based.
 * Per code-quality.mdc: Use Flow for reactive streams.
 */
class ScanItemUseCase(
    private val scannerRepository: ScannerRepository,
    private val productRepository: ProductRepository,
    private val cartRepository: CartRepository
) {
    
    /**
     * Current cart state.
     * Observe this to react to cart changes.
     * 
     * This delegates to CartRepository.cart - the single source of truth.
     * Both Cashier and Customer Display observe this same state.
     */
    val cart: StateFlow<Cart> = cartRepository.cart
    
    /**
     * Flow that emits scan results for each scanned barcode.
     * 
     * Transforms scanner events into cart updates:
     * - On valid barcode: looks up product via getByBarcode, adds to cart, emits Success
     * - On unknown barcode: emits ProductNotFound
     * - On error: emits Error
     */
    val scanResults: Flow<ScanResult> = scannerRepository.scannedCodes.map { barcode ->
        processScan(barcode)
    }
    
    /**
     * Processes a single scan event.
     * 
     * Business Logic:
     * 1. Look up product by barcode (checks itemNumbers array per schema)
     * 2. If found, add to cart via CartRepository (or increment if already present)
     * 3. Return appropriate result
     * 
     * Per DATABASE_SCHEMA.md: Uses getByBarcode which searches itemNumbers[].itemNumber
     * 
     * @param barcode The scanned barcode
     * @return ScanResult indicating success or failure
     */
    suspend fun processScan(barcode: String): ScanResult {
        val product = productRepository.getByBarcode(barcode)
        
        return if (product != null) {
            // Add product to cart via repository (handles quantity increment)
            cartRepository.addToCart(product)
            ScanResult.Success(cartRepository.getCurrentCart())
        } else {
            ScanResult.ProductNotFound(barcode)
        }
    }
    
    /**
     * Manually adds a product to the cart by barcode.
     * 
     * Used for manual product lookup/entry.
     * 
     * @param barcode The product barcode to add
     * @return ScanResult indicating success or failure
     */
    suspend fun addProductByBarcode(barcode: String): ScanResult {
        return processScan(barcode)
    }
    
    /**
     * Clears the current cart.
     * 
     * Called after transaction completion or void.
     */
    suspend fun clearCart() {
        cartRepository.clearCart()
    }
    
    /**
     * Removes a product from the cart by branchProductId.
     * 
     * Per DATABASE_SCHEMA.md: Products identified by branchProductId.
     * 
     * @param branchProductId The branchProductId of the product to remove
     */
    suspend fun removeProduct(branchProductId: Int) {
        cartRepository.removeFromCart(branchProductId)
    }
    
    /**
     * Voids a product in the cart (marks as removed but keeps in history).
     * 
     * @param branchProductId The branchProductId of the product to void
     */
    suspend fun voidProduct(branchProductId: Int) {
        cartRepository.voidItem(branchProductId)
    }
    
    /**
     * Gets the current cart value (non-reactive).
     * 
     * Prefer observing [cart] StateFlow for reactive updates.
     */
    fun getCurrentCart(): Cart = cartRepository.getCurrentCart()
}
