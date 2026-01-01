package com.unisight.gropos.features.checkout.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.unisight.gropos.core.util.CurrencyFormatter
import com.unisight.gropos.features.checkout.domain.model.Cart
import com.unisight.gropos.features.checkout.domain.model.CartItem
import com.unisight.gropos.features.checkout.domain.model.Product
import com.unisight.gropos.features.checkout.domain.repository.CartRepository
import com.unisight.gropos.features.checkout.domain.repository.ProductRepository
import com.unisight.gropos.features.checkout.domain.repository.ScannerRepository
import com.unisight.gropos.features.checkout.domain.usecase.ScanItemUseCase
import com.unisight.gropos.features.checkout.presentation.components.ProductLookupState
import com.unisight.gropos.features.checkout.presentation.components.ProductLookupUiModel
import com.unisight.gropos.features.checkout.domain.usecase.ScanResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.math.BigDecimal

/**
 * ViewModel for the Checkout screen (Cashier window).
 * 
 * Manages the checkout UI state by:
 * 1. Reactively collecting scanned barcodes from hardware
 * 2. Delegating product lookup to ScanItemUseCase
 * 3. Observing cart state from CartRepository (shared with Customer Display)
 * 
 * Per ARCHITECTURE_BLUEPRINT.md:
 * - Cart state is managed by CartRepository (Singleton)
 * - This ViewModel OBSERVES the repository, it does NOT own the state
 * - Any changes here are automatically reflected on Customer Display
 * 
 * Per project-structure.mdc: Named [Feature]ViewModel
 * Per kotlin-standards.mdc: Uses ScreenModel for Voyager compatibility
 * Per code-quality.mdc: Unidirectional Data Flow - UI observes state
 */
class CheckoutViewModel(
    private val scanItemUseCase: ScanItemUseCase,
    private val scannerRepository: ScannerRepository,
    private val cartRepository: CartRepository,
    private val productRepository: ProductRepository,
    private val currencyFormatter: CurrencyFormatter,
    // Inject scope for testability (per testing-strategy.mdc)
    private val scope: CoroutineScope? = null
) : ScreenModel {
    
    private val _state = MutableStateFlow(CheckoutUiState.initial())
    
    /**
     * Current checkout UI state.
     * Observe this in Compose using collectAsState().
     */
    val state: StateFlow<CheckoutUiState> = _state.asStateFlow()
    
    private val effectiveScope: CoroutineScope
        get() = scope ?: screenModelScope
    
    init {
        // Reactive Hardware Collection
        // Per requirement: In init{}, collect scannerRepository.scannedCodes
        observeScannerFlow()
        
        // Observe cart changes from the SHARED CartRepository
        // This is the key change - we observe the repository, not use case
        observeCartChanges()
    }
    
    /**
     * Collects scanned barcodes from hardware and processes them automatically.
     */
    private fun observeScannerFlow() {
        scannerRepository.scannedCodes
            .onEach { barcode -> processScan(barcode) }
            .launchIn(effectiveScope)
    }
    
    /**
     * Observes cart state changes from the SHARED CartRepository.
     * 
     * Since CartRepository is a singleton, when Customer Display observes
     * the same repository, both windows stay in sync.
     */
    private fun observeCartChanges() {
        cartRepository.cart
            .onEach { cart -> updateStateFromCart(cart) }
            .launchIn(effectiveScope)
    }
    
    /**
     * Processes a scanned barcode.
     * Called automatically from hardware scanner flow.
     */
    private suspend fun processScan(barcode: String) {
        _state.value = _state.value.copy(isLoading = true)
        
        when (val result = scanItemUseCase.processScan(barcode)) {
            is ScanResult.Success -> {
                val lastItem = result.cart.items.lastOrNull()
                _state.value = _state.value.copy(
                    isLoading = false,
                    lastScanEvent = lastItem?.let {
                        ScanEvent.ProductAdded(it.branchProductName)
                    }
                )
            }
            is ScanResult.ProductNotFound -> {
                _state.value = _state.value.copy(
                    isLoading = false,
                    lastScanEvent = ScanEvent.ProductNotFound(result.barcode)
                )
            }
            is ScanResult.Error -> {
                _state.value = _state.value.copy(
                    isLoading = false,
                    lastScanEvent = ScanEvent.Error(result.message)
                )
            }
        }
    }
    
    /**
     * Manual barcode entry.
     * For keyboard/touchscreen input when hardware scanner unavailable.
     * 
     * @param barcode The barcode string entered manually
     */
    fun onManualBarcodeEnter(barcode: String) {
        if (barcode.isBlank()) return
        
        effectiveScope.launch {
            processScan(barcode.trim())
        }
    }
    
    /**
     * Removes an item from the cart.
     * 
     * @param branchProductId The product ID to remove
     */
    fun onRemoveItem(branchProductId: Int) {
        effectiveScope.launch {
            scanItemUseCase.removeProduct(branchProductId)
        }
    }
    
    /**
     * Voids an item (marks as removed but keeps in history).
     * 
     * @param branchProductId The product ID to void
     */
    fun onVoidItem(branchProductId: Int) {
        effectiveScope.launch {
            scanItemUseCase.voidProduct(branchProductId)
        }
    }
    
    /**
     * Clears the entire cart.
     * Called after transaction completion or void all.
     */
    fun onClearCart() {
        effectiveScope.launch {
            scanItemUseCase.clearCart()
        }
    }
    
    /**
     * Dismisses the last scan event notification.
     */
    fun onDismissScanEvent() {
        _state.value = _state.value.copy(lastScanEvent = null)
    }
    
    // ========================================================================
    // Product Lookup Dialog
    // Per SCREEN_LAYOUTS.md: Product Lookup Dialog for manual product selection
    // ========================================================================
    
    /**
     * Opens the Product Lookup Dialog.
     * 
     * Loads categories and initial product list.
     */
    fun onOpenLookup() {
        effectiveScope.launch {
            _state.value = _state.value.copy(
                lookupState = _state.value.lookupState.copy(
                    isVisible = true,
                    isLoading = true,
                    searchQuery = "",
                    selectedCategoryId = null
                )
            )
            
            try {
                val categories = productRepository.getCategories()
                val products = productRepository.searchProducts("")
                
                _state.value = _state.value.copy(
                    lookupState = _state.value.lookupState.copy(
                        categories = categories,
                        products = products.map { mapProductToLookupUiModel(it) },
                        isLoading = false
                    )
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    lookupState = _state.value.lookupState.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load products"
                    )
                )
            }
        }
    }
    
    /**
     * Closes the Product Lookup Dialog.
     */
    fun onCloseLookup() {
        _state.value = _state.value.copy(
            lookupState = ProductLookupState()
        )
    }
    
    /**
     * Handles search query changes in the lookup dialog.
     * 
     * @param query The search query (product name or barcode)
     */
    fun onLookupSearchChange(query: String) {
        _state.value = _state.value.copy(
            lookupState = _state.value.lookupState.copy(
                searchQuery = query,
                isLoading = true
            )
        )
        
        effectiveScope.launch {
            try {
                val products = if (query.isBlank() && _state.value.lookupState.selectedCategoryId != null) {
                    productRepository.getByCategory(_state.value.lookupState.selectedCategoryId!!)
                } else {
                    productRepository.searchProducts(query)
                }
                
                _state.value = _state.value.copy(
                    lookupState = _state.value.lookupState.copy(
                        products = products.map { mapProductToLookupUiModel(it) },
                        isLoading = false
                    )
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    lookupState = _state.value.lookupState.copy(
                        isLoading = false,
                        error = e.message ?: "Search failed"
                    )
                )
            }
        }
    }
    
    /**
     * Handles category selection in the lookup dialog.
     * 
     * @param categoryId The selected category ID (null for "All Products")
     */
    fun onLookupCategorySelect(categoryId: Int?) {
        _state.value = _state.value.copy(
            lookupState = _state.value.lookupState.copy(
                selectedCategoryId = categoryId,
                isLoading = true,
                searchQuery = "" // Clear search when switching categories
            )
        )
        
        effectiveScope.launch {
            try {
                val products = if (categoryId != null) {
                    productRepository.getByCategory(categoryId)
                } else {
                    productRepository.searchProducts("")
                }
                
                _state.value = _state.value.copy(
                    lookupState = _state.value.lookupState.copy(
                        products = products.map { mapProductToLookupUiModel(it) },
                        isLoading = false
                    )
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    lookupState = _state.value.lookupState.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load category"
                    )
                )
            }
        }
    }
    
    /**
     * Handles product selection from the lookup dialog.
     * 
     * Per requirement: Call cartRepository.addToCart(product) -> Close Dialog.
     * 
     * @param productUiModel The selected product
     */
    fun onProductSelected(productUiModel: ProductLookupUiModel) {
        effectiveScope.launch {
            try {
                // Get the full product by ID
                val product = productRepository.getById(productUiModel.branchProductId)
                if (product != null) {
                    // Add to cart
                    cartRepository.addToCart(product)
                    
                    // Show success feedback
                    _state.value = _state.value.copy(
                        lastScanEvent = ScanEvent.ProductAdded(product.productName)
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    lastScanEvent = ScanEvent.Error(e.message ?: "Failed to add product")
                )
            }
            
            // Close the dialog
            onCloseLookup()
        }
    }
    
    /**
     * Maps a Product domain model to ProductLookupUiModel.
     */
    private fun mapProductToLookupUiModel(product: Product): ProductLookupUiModel {
        return ProductLookupUiModel(
            branchProductId = product.branchProductId,
            name = product.productName,
            price = currencyFormatter.format(product.retailPrice),
            // imageUrl is not in our current Product model, default to null
            imageUrl = null,
            isSnapEligible = product.isSnapEligible,
            barcode = product.itemNumbers.firstOrNull()?.itemNumber
        )
    }
    
    /**
     * Maps Cart domain model to CheckoutUiState.
     * 
     * Per Governance: All formatting done here, not in UI.
     */
    private fun updateStateFromCart(cart: Cart) {
        val items = cart.items
            .filterNot { it.isRemoved }
            .map { cartItem -> mapToUiModel(cartItem) }
        
        val totals = CheckoutTotalsUiModel(
            subtotal = currencyFormatter.format(cart.subTotal),
            taxTotal = currencyFormatter.format(cart.taxTotal),
            crvTotal = currencyFormatter.format(cart.crvTotal),
            grandTotal = currencyFormatter.format(cart.grandTotal),
            itemCount = formatItemCount(cart.itemCount),
            savingsTotal = if (cart.discountTotal > BigDecimal.ZERO) {
                currencyFormatter.formatWithSign(cart.discountTotal.negate(), false)
            } else null
        )
        
        _state.value = _state.value.copy(
            items = items,
            totals = totals,
            isEmpty = cart.isEmpty
        )
    }
    
    /**
     * Maps a CartItem to CheckoutItemUiModel with formatted strings.
     */
    private fun mapToUiModel(cartItem: CartItem): CheckoutItemUiModel {
        val hasSavings = cartItem.savingsTotal > BigDecimal.ZERO
        
        return CheckoutItemUiModel(
            branchProductId = cartItem.branchProductId,
            productName = cartItem.branchProductName,
            quantity = formatQuantity(cartItem),
            unitPrice = currencyFormatter.format(cartItem.effectivePrice),
            lineTotal = currencyFormatter.format(cartItem.subTotal),
            isSnapEligible = cartItem.isSnapEligible,
            taxIndicator = cartItem.taxIndicator,
            hasSavings = hasSavings,
            savingsAmount = if (hasSavings) {
                currencyFormatter.formatWithSign(cartItem.savingsTotal.negate(), false)
            } else null
        )
    }
    
    /**
     * Formats quantity for display.
     */
    private fun formatQuantity(cartItem: CartItem): String {
        return if (cartItem.soldById == "Weight") {
            "${cartItem.quantityUsed} lb"
        } else {
            "${cartItem.quantityUsed.toInt()}x"
        }
    }
    
    /**
     * Formats item count for display.
     */
    private fun formatItemCount(count: BigDecimal): String {
        val intCount = count.toInt()
        return if (intCount == 1) "1 item" else "$intCount items"
    }
}
