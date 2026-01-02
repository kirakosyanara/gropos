package com.unisight.gropos.features.returns.presentation

import com.unisight.gropos.features.returns.domain.model.ReturnReason
import java.math.BigDecimal

/**
 * UI State for the Return Item screen.
 * 
 * Per RETURNS.md & SCREEN_LAYOUTS.md:
 * - Shows returnable items from original transaction
 * - Manages items selected for return
 * - Tracks refund totals
 */
data class ReturnUiState(
    // Transaction info
    val transactionId: Long = 0,
    val transactionGuid: String = "",
    val transactionDate: String = "",
    
    // Returnable items (from original transaction)
    val returnableItems: List<ReturnableItemUiModel> = emptyList(),
    
    // Items selected for return
    val returnItems: List<ReturnItemUiModel> = emptyList(),
    
    // Totals
    val subtotalRefund: String = "$0.00",
    val taxRefund: String = "$0.00",
    val totalRefund: String = "$0.00",
    val itemCount: Int = 0,
    
    // Dialogs
    val showQuantityDialog: Boolean = false,
    val selectedItemForQuantity: ReturnableItemUiModel? = null,
    val quantityInput: String = "1",
    
    val showReasonDialog: Boolean = false,
    val selectedItemForReason: ReturnItemUiModel? = null,
    
    val showManagerApproval: Boolean = false,
    
    // Status
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isComplete: Boolean = false
) {
    val canProcessReturn: Boolean
        get() = returnItems.isNotEmpty() && !isLoading
}

/**
 * UI Model for a returnable item (from original transaction).
 */
data class ReturnableItemUiModel(
    val id: Long,
    val branchProductId: Int,
    val productName: String,
    val quantityPurchased: String,
    val quantityRemaining: String,
    val unitPrice: String,
    val totalPrice: String,
    val canReturn: Boolean,
    val isSnapEligible: Boolean
)

/**
 * UI Model for an item selected for return.
 */
data class ReturnItemUiModel(
    val id: Long,
    val branchProductId: Int,
    val productName: String,
    val returnQuantity: String,
    val refundAmount: String,  // Displayed with negative sign
    val taxRefund: String,
    val totalRefund: String,   // Displayed with negative sign
    val reason: ReturnReason?,
    val reasonDisplay: String,
    val isSnapEligible: Boolean
)

