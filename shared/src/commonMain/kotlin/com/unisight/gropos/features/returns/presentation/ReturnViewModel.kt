package com.unisight.gropos.features.returns.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.unisight.gropos.core.util.CurrencyFormatter
import com.unisight.gropos.features.returns.domain.model.ReturnCart
import com.unisight.gropos.features.returns.domain.model.ReturnItem
import com.unisight.gropos.features.returns.domain.model.ReturnReason
import com.unisight.gropos.features.returns.domain.model.ReturnableItem
import com.unisight.gropos.features.returns.domain.service.ReturnService
import com.unisight.gropos.features.transaction.domain.repository.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

/**
 * ViewModel for the Return Item screen.
 * 
 * Per RETURNS.md:
 * - Loads returnable items from a transaction
 * - Manages return cart
 * - Validates quantities
 * - Processes refunds
 */
class ReturnViewModel(
    private val returnService: ReturnService,
    private val transactionRepository: TransactionRepository,
    private val currencyFormatter: CurrencyFormatter,
    private val coroutineScope: CoroutineScope? = null
) : ScreenModel {
    
    private val scope: CoroutineScope
        get() = coroutineScope ?: screenModelScope
    
    private val _state = MutableStateFlow(ReturnUiState())
    val state: StateFlow<ReturnUiState> = _state.asStateFlow()
    
    // Domain state
    private var returnableItems: List<ReturnableItem> = emptyList()
    private var returnCart: ReturnCart? = null
    
    /**
     * Load returnable items for a transaction.
     */
    fun loadTransaction(transactionId: Long) {
        scope.launch {
            _state.update { it.copy(isLoading = true, transactionId = transactionId) }
            
            // Get transaction for display info
            val transaction = transactionRepository.getById(transactionId)
            
            if (transaction == null) {
                _state.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = "Transaction not found"
                    )
                }
                return@launch
            }
            
            // Initialize return cart
            returnCart = returnService.createReturnCart(transaction)
            
            // Get returnable items
            returnService.getReturnableItems(transactionId)
                .onSuccess { items ->
                    returnableItems = items
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            transactionGuid = transaction.guid,
                            transactionDate = transaction.completedDateTime.take(10), // Just date part
                            returnableItems = items.map { item -> item.toUiModel() }
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to load items"
                        )
                    }
                }
        }
    }
    
    /**
     * User clicks "Add" on a returnable item.
     * Shows quantity dialog if qty > 1, otherwise adds 1.
     */
    fun onAddToReturnClick(itemId: Long) {
        val item = returnableItems.find { it.originalItem.id == itemId } ?: return
        
        if (item.remainingQuantity > BigDecimal.ONE) {
            // Show quantity dialog
            _state.update { 
                it.copy(
                    showQuantityDialog = true,
                    selectedItemForQuantity = item.toUiModel(),
                    quantityInput = "1"
                )
            }
        } else {
            // Add 1 directly
            addItemToReturn(item, BigDecimal.ONE)
        }
    }
    
    /**
     * User enters quantity in dialog.
     */
    fun onQuantityInputChange(value: String) {
        // Only allow digits
        if (value.all { it.isDigit() }) {
            _state.update { it.copy(quantityInput = value) }
        }
    }
    
    /**
     * User confirms quantity in dialog.
     */
    fun onQuantityConfirm() {
        val item = returnableItems.find { 
            it.originalItem.id == _state.value.selectedItemForQuantity?.id 
        } ?: return
        
        val quantity = _state.value.quantityInput.toBigDecimalOrNull() ?: BigDecimal.ONE
        
        // Validate
        val error = returnService.validateReturnQuantity(item, quantity)
        if (error != null) {
            _state.update { it.copy(errorMessage = error) }
            return
        }
        
        addItemToReturn(item, quantity)
        
        _state.update { 
            it.copy(
                showQuantityDialog = false,
                selectedItemForQuantity = null,
                quantityInput = "1"
            )
        }
    }
    
    /**
     * User dismisses quantity dialog.
     */
    fun onQuantityDialogDismiss() {
        _state.update { 
            it.copy(
                showQuantityDialog = false,
                selectedItemForQuantity = null,
                quantityInput = "1"
            )
        }
    }
    
    /**
     * Add an item to the return cart.
     */
    private fun addItemToReturn(returnableItem: ReturnableItem, quantity: BigDecimal) {
        val cart = returnCart ?: return
        
        val returnItem = returnService.createReturnItem(
            returnableItem = returnableItem,
            quantity = quantity
        )
        
        returnCart = cart.addItem(returnItem)
        
        // Update returnable items (reduce remaining quantity)
        updateReturnableItemQuantity(returnableItem.originalItem.id, quantity)
        
        // Update UI state
        updateUiFromCart()
    }
    
    /**
     * Update the remaining quantity for a returnable item after adding to cart.
     */
    private fun updateReturnableItemQuantity(itemId: Long, usedQuantity: BigDecimal) {
        returnableItems = returnableItems.map { item ->
            if (item.originalItem.id == itemId) {
                item.copy(
                    quantityAlreadyReturned = item.quantityAlreadyReturned + usedQuantity
                )
            } else {
                item
            }
        }
        
        _state.update { 
            it.copy(returnableItems = returnableItems.map { item -> item.toUiModel() })
        }
    }
    
    /**
     * User removes an item from return cart.
     */
    fun onRemoveFromReturn(itemId: Long) {
        val cart = returnCart ?: return
        val removedItem = cart.items.find { it.originalItem.id == itemId } ?: return
        
        // Restore quantity to returnable items
        returnableItems = returnableItems.map { item ->
            if (item.originalItem.id == itemId) {
                item.copy(
                    quantityAlreadyReturned = (item.quantityAlreadyReturned - removedItem.returnQuantity)
                        .coerceAtLeast(BigDecimal.ZERO)
                )
            } else {
                item
            }
        }
        
        returnCart = cart.removeItem(itemId)
        
        _state.update { 
            it.copy(returnableItems = returnableItems.map { item -> item.toUiModel() })
        }
        updateUiFromCart()
    }
    
    /**
     * User clicks "Process Return" button.
     * Per RETURNS.md: Requires manager approval.
     */
    fun onProcessReturnClick() {
        val cart = returnCart ?: return
        
        val error = returnService.validateReturnCart(cart)
        if (error != null) {
            _state.update { it.copy(errorMessage = error) }
            return
        }
        
        // Show manager approval dialog
        _state.update { it.copy(showManagerApproval = true) }
    }
    
    /**
     * Manager approval granted.
     * For P0: We just mark as complete and print virtual receipt.
     */
    fun onManagerApprovalGranted() {
        scope.launch {
            _state.update { 
                it.copy(
                    showManagerApproval = false,
                    isLoading = true
                )
            }
            
            // In production: call API to process refund
            // For P0: Just log and mark complete
            val cart = returnCart ?: return@launch
            
            println("=== REFUND RECEIPT ===")
            println("Original Transaction: ${cart.originalTransactionGuid}")
            println("Items Returned: ${cart.itemCount}")
            cart.items.forEach { item ->
                println("  ${item.productName} x${item.returnQuantity} = -${currencyFormatter.format(item.totalRefundAmount)}")
            }
            println("Subtotal Refund: -${currencyFormatter.format(cart.subtotalRefund)}")
            println("Tax Refund: -${currencyFormatter.format(cart.taxRefund)}")
            println("TOTAL REFUND: -${currencyFormatter.format(cart.totalRefund)}")
            println("=======================")
            
            _state.update { 
                it.copy(
                    isLoading = false,
                    isComplete = true
                )
            }
        }
    }
    
    /**
     * Manager approval denied.
     */
    fun onManagerApprovalDenied() {
        _state.update { 
            it.copy(
                showManagerApproval = false,
                errorMessage = "Manager approval denied"
            )
        }
    }
    
    /**
     * Dismiss manager approval dialog.
     */
    fun onManagerApprovalDismiss() {
        _state.update { it.copy(showManagerApproval = false) }
    }
    
    /**
     * Dismiss error message.
     */
    fun onErrorDismissed() {
        _state.update { it.copy(errorMessage = null) }
    }
    
    /**
     * Update UI state from return cart.
     */
    private fun updateUiFromCart() {
        val cart = returnCart ?: return
        
        _state.update { 
            it.copy(
                returnItems = cart.items.map { item -> item.toUiModel() },
                subtotalRefund = "-${currencyFormatter.format(cart.subtotalRefund)}",
                taxRefund = "-${currencyFormatter.format(cart.taxRefund)}",
                totalRefund = "-${currencyFormatter.format(cart.totalRefund)}",
                itemCount = cart.itemCount
            )
        }
    }
    
    /**
     * Map ReturnableItem to UI model.
     */
    private fun ReturnableItem.toUiModel() = ReturnableItemUiModel(
        id = originalItem.id,
        branchProductId = originalItem.branchProductId,
        productName = productName,
        quantityPurchased = originalItem.quantityUsed.toPlainString(),
        quantityRemaining = remainingQuantity.toPlainString(),
        unitPrice = currencyFormatter.format(refundPricePerUnit),
        totalPrice = currencyFormatter.format(originalItem.subTotal),
        canReturn = canReturn,
        isSnapEligible = originalItem.isFoodStampEligible
    )
    
    /**
     * Map ReturnItem to UI model.
     */
    private fun ReturnItem.toUiModel() = ReturnItemUiModel(
        id = originalItem.id,
        branchProductId = originalItem.branchProductId,
        productName = productName,
        returnQuantity = returnQuantity.toPlainString(),
        refundAmount = "-${currencyFormatter.format(refundAmount)}",
        taxRefund = "-${currencyFormatter.format(taxRefundAmount)}",
        totalRefund = "-${currencyFormatter.format(totalRefundAmount)}",
        reason = reason,
        reasonDisplay = reason?.description ?: "No reason",
        isSnapEligible = originalItem.isFoodStampEligible
    )
}

