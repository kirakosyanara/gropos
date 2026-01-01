package com.unisight.gropos.features.customer.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.unisight.gropos.core.util.CurrencyFormatter
import com.unisight.gropos.features.checkout.domain.model.Cart
import com.unisight.gropos.features.checkout.domain.model.CartItem
import com.unisight.gropos.features.checkout.domain.repository.CartRepository
import com.unisight.gropos.features.checkout.presentation.CheckoutItemUiModel
import com.unisight.gropos.features.checkout.presentation.CheckoutTotalsUiModel
import com.unisight.gropos.features.checkout.presentation.CheckoutUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.math.BigDecimal

/**
 * ViewModel for the Customer Display screen.
 * 
 * Per ARCHITECTURE_BLUEPRINT.md:
 * - Customer Display observes the SAME cart state as Cashier screen
 * - Read-only view - customers cannot modify the cart
 * 
 * This ViewModel observes CartRepository.cart and maps it to UI state.
 * Since CartRepository is a singleton, any changes from the Cashier
 * are automatically reflected on the Customer Display.
 * 
 * Per code-quality.mdc: Unidirectional Data Flow - UI only observes state.
 */
class CustomerDisplayViewModel(
    private val cartRepository: CartRepository,
    private val currencyFormatter: CurrencyFormatter,
    // Inject scope for testability (per testing-strategy.mdc)
    private val scope: CoroutineScope? = null
) : ScreenModel {
    
    private val _state = MutableStateFlow(CheckoutUiState.initial())
    
    /**
     * Current UI state for the customer display.
     * Observe this in Compose using collectAsState().
     */
    val state: StateFlow<CheckoutUiState> = _state.asStateFlow()
    
    private val effectiveScope: CoroutineScope
        get() = scope ?: screenModelScope
    
    init {
        observeCartChanges()
    }
    
    /**
     * Observes cart state changes from the shared CartRepository.
     * 
     * When the Cashier adds/removes items, this flow automatically
     * updates the Customer Display.
     */
    private fun observeCartChanges() {
        cartRepository.cart
            .onEach { cart -> updateStateFromCart(cart) }
            .launchIn(effectiveScope)
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

