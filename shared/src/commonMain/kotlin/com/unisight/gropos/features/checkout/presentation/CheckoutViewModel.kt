package com.unisight.gropos.features.checkout.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.unisight.gropos.core.util.CurrencyFormatter
import com.unisight.gropos.features.checkout.domain.model.Cart
import com.unisight.gropos.features.checkout.domain.model.CartItem
import com.unisight.gropos.features.checkout.domain.repository.ScannerRepository
import com.unisight.gropos.features.checkout.domain.usecase.ScanItemUseCase
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
 * ViewModel for the Checkout screen.
 * 
 * Manages the checkout UI state by:
 * 1. Reactively collecting scanned barcodes from hardware
 * 2. Delegating product lookup to ScanItemUseCase
 * 3. Mapping Cart state to UI-friendly CheckoutUiState
 * 
 * Per project-structure.mdc: Named [Feature]ViewModel
 * Per kotlin-standards.mdc: Uses ScreenModel for Voyager compatibility
 * Per code-quality.mdc: Unidirectional Data Flow - UI observes state
 */
class CheckoutViewModel(
    private val scanItemUseCase: ScanItemUseCase,
    private val scannerRepository: ScannerRepository,
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
        
        // Observe cart changes from UseCase
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
     * Observes cart state changes and updates UI accordingly.
     */
    private fun observeCartChanges() {
        scanItemUseCase.cart
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
        scanItemUseCase.removeProduct(branchProductId)
    }
    
    /**
     * Voids an item (marks as removed but keeps in history).
     * 
     * @param branchProductId The product ID to void
     */
    fun onVoidItem(branchProductId: Int) {
        scanItemUseCase.voidProduct(branchProductId)
    }
    
    /**
     * Clears the entire cart.
     * Called after transaction completion or void all.
     */
    fun onClearCart() {
        scanItemUseCase.clearCart()
    }
    
    /**
     * Dismisses the last scan event notification.
     */
    fun onDismissScanEvent() {
        _state.value = _state.value.copy(lastScanEvent = null)
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

