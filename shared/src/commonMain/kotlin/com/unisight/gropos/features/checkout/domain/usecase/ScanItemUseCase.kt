package com.unisight.gropos.features.checkout.domain.usecase

import com.unisight.gropos.features.checkout.domain.model.Cart
import com.unisight.gropos.features.checkout.domain.repository.ProductRepository
import com.unisight.gropos.features.checkout.domain.repository.ScannerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
     * Product was not found for the scanned code.
     */
    data class ProductNotFound(val sku: String) : ScanResult
    
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
 * 2. Looks up products in the repository
 * 3. Adds found products to the cart
 * 4. Handles quantity incrementing for duplicate scans
 * 
 * Per project-structure.mdc: UseCase naming is Verb-based.
 * Per code-quality.mdc: Use Flow for reactive streams.
 */
class ScanItemUseCase(
    private val scannerRepository: ScannerRepository,
    private val productRepository: ProductRepository
) {
    
    private val _cart = MutableStateFlow(Cart.empty())
    
    /**
     * Current cart state.
     * Observe this to react to cart changes.
     */
    val cart: StateFlow<Cart> = _cart.asStateFlow()
    
    /**
     * Flow that emits scan results for each scanned barcode.
     * 
     * Transforms scanner events into cart updates:
     * - On valid barcode: looks up product, adds to cart, emits Success
     * - On unknown barcode: emits ProductNotFound
     * - On error: emits Error
     */
    val scanResults: Flow<ScanResult> = scannerRepository.scannedCodes.map { sku ->
        processScan(sku)
    }
    
    /**
     * Processes a single scan event.
     * 
     * Business Logic:
     * 1. Look up product by SKU
     * 2. If found, add to cart (or increment if already present)
     * 3. Return appropriate result
     * 
     * @param sku The scanned barcode/SKU
     * @return ScanResult indicating success or failure
     */
    suspend fun processScan(sku: String): ScanResult {
        return productRepository.findBySku(sku)
            .fold(
                onSuccess = { product ->
                    // Add product to cart (Cart handles quantity increment)
                    _cart.value = _cart.value.addProduct(product)
                    ScanResult.Success(_cart.value)
                },
                onFailure = { error ->
                    ScanResult.ProductNotFound(sku)
                }
            )
    }
    
    /**
     * Manually adds a product to the cart by SKU.
     * 
     * Used for manual product lookup/entry.
     * 
     * @param sku The product SKU to add
     * @return ScanResult indicating success or failure
     */
    suspend fun addProductBySku(sku: String): ScanResult {
        return processScan(sku)
    }
    
    /**
     * Clears the current cart.
     * 
     * Called after transaction completion or void.
     */
    fun clearCart() {
        _cart.value = Cart.empty()
    }
    
    /**
     * Removes a product from the cart by SKU.
     * 
     * @param sku The SKU of the product to remove
     */
    fun removeProduct(sku: String) {
        _cart.value = _cart.value.removeProduct(sku)
    }
    
    /**
     * Gets the current cart value (non-reactive).
     * 
     * Prefer observing [cart] StateFlow for reactive updates.
     */
    fun getCurrentCart(): Cart = _cart.value
}

