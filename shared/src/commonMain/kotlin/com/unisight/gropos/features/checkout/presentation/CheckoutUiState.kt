package com.unisight.gropos.features.checkout.presentation

import com.unisight.gropos.features.checkout.domain.model.Cart
import com.unisight.gropos.features.checkout.domain.model.CartItem

/**
 * UI model for displaying a cart item.
 * 
 * Contains pre-formatted strings for display.
 * Per Governance: No math in UI - all calculations done in Domain.
 * 
 * @property branchProductId Unique identifier for the item
 * @property productName Display name of the product
 * @property quantity Formatted quantity string (e.g., "2x")
 * @property unitPrice Formatted unit price (e.g., "$5.99")
 * @property lineTotal Formatted line total (e.g., "$11.98")
 * @property isSnapEligible Whether item is SNAP eligible (show badge)
 * @property taxIndicator Tax indicator for receipt (F = SNAP, T = Taxable)
 * @property hasSavings Whether there are savings on this item
 * @property savingsAmount Formatted savings amount if any
 */
data class CheckoutItemUiModel(
    val branchProductId: Int,
    val productName: String,
    val quantity: String,
    val unitPrice: String,
    val lineTotal: String,
    val isSnapEligible: Boolean,
    val taxIndicator: String,
    val hasSavings: Boolean = false,
    val savingsAmount: String? = null
)

/**
 * UI model for the totals panel.
 * 
 * Pre-formatted strings for display.
 */
data class CheckoutTotalsUiModel(
    val subtotal: String,
    val taxTotal: String,
    val crvTotal: String,
    val grandTotal: String,
    val itemCount: String,
    val savingsTotal: String? = null
)

/**
 * Represents a scan event result for UI feedback.
 */
sealed interface ScanEvent {
    data class ProductAdded(val productName: String) : ScanEvent
    data class ProductNotFound(val barcode: String) : ScanEvent
    data class Error(val message: String) : ScanEvent
}

/**
 * UI State for the Checkout screen.
 * 
 * Per kotlin-standards.mdc: Use sealed interface for strict typing.
 * Per code-quality.mdc: State modeling with sealed classes.
 * 
 * @property items List of cart items for display
 * @property totals Totals panel data
 * @property isLoading Whether a scan operation is in progress
 * @property lastScanEvent The last scan event for user feedback
 * @property isEmpty Whether the cart is empty
 */
data class CheckoutUiState(
    val items: List<CheckoutItemUiModel> = emptyList(),
    val totals: CheckoutTotalsUiModel = CheckoutTotalsUiModel(
        subtotal = "$0.00",
        taxTotal = "$0.00",
        crvTotal = "$0.00",
        grandTotal = "$0.00",
        itemCount = "0 items"
    ),
    val isLoading: Boolean = false,
    val lastScanEvent: ScanEvent? = null,
    val isEmpty: Boolean = true
) {
    companion object {
        /**
         * Creates the initial empty state.
         */
        fun initial(): CheckoutUiState = CheckoutUiState()
    }
}

