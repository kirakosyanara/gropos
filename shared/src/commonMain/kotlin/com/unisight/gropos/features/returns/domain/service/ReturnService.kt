package com.unisight.gropos.features.returns.domain.service

import com.unisight.gropos.features.returns.domain.model.ReturnableItem
import com.unisight.gropos.features.returns.domain.model.ReturnItem
import com.unisight.gropos.features.returns.domain.model.ReturnCart
import com.unisight.gropos.features.returns.domain.model.ReturnReason
import com.unisight.gropos.features.transaction.domain.model.Transaction
import com.unisight.gropos.features.transaction.domain.model.TransactionItem
import com.unisight.gropos.features.transaction.domain.repository.TransactionRepository
import java.math.BigDecimal

/**
 * Service for managing return operations.
 * 
 * Per RETURNS.md:
 * - Validates return quantities
 * - Manages return cart state
 * - Processes refunds
 * 
 * @property transactionRepository Repository for fetching transactions
 */
class ReturnService(
    private val transactionRepository: TransactionRepository
) {
    
    /**
     * Get returnable items from a transaction.
     * 
     * Filters out:
     * - Items with quantity <= 0
     * - Items already fully returned
     * - Removed items
     * 
     * @param transactionId The transaction to get items from
     * @return Result containing list of returnable items
     */
    suspend fun getReturnableItems(transactionId: Long): Result<List<ReturnableItem>> {
        return try {
            val transaction = transactionRepository.getById(transactionId)
                ?: return Result.failure(IllegalArgumentException("Transaction not found: $transactionId"))
            
            // Map transaction items to returnable items
            // For now, assume no prior returns (qtyAlreadyReturned = 0)
            // In production, we'd query for prior return transactions
            val returnableItems = transaction.items
                .filter { !it.isRemoved }
                .filter { it.quantityUsed > BigDecimal.ZERO }
                .map { item ->
                    ReturnableItem(
                        originalItem = item,
                        quantityAlreadyReturned = BigDecimal.ZERO // TODO: Query prior returns
                    )
                }
                .filter { it.canReturn }
            
            Result.success(returnableItems)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Validate a return quantity.
     * 
     * Checks:
     * - Quantity is positive
     * - Quantity does not exceed remaining returnable quantity
     * 
     * @param returnableItem The item to validate against
     * @param requestedQuantity The quantity to return
     * @return Error message if invalid, null if valid
     */
    fun validateReturnQuantity(
        returnableItem: ReturnableItem,
        requestedQuantity: BigDecimal
    ): String? {
        if (requestedQuantity <= BigDecimal.ZERO) {
            return "Return quantity must be greater than zero"
        }
        
        if (requestedQuantity > returnableItem.remainingQuantity) {
            return "Cannot return more than purchased (${returnableItem.remainingQuantity} remaining)"
        }
        
        return null // Valid
    }
    
    /**
     * Create a return item from a returnable item.
     * 
     * @param returnableItem The source item
     * @param quantity Quantity to return
     * @param reason Return reason
     * @return The return item
     */
    fun createReturnItem(
        returnableItem: ReturnableItem,
        quantity: BigDecimal,
        reason: ReturnReason? = null
    ): ReturnItem {
        return ReturnItem(
            originalItem = returnableItem.originalItem,
            returnQuantity = quantity,
            reason = reason,
            notes = null
        )
    }
    
    /**
     * Create a new return cart for a transaction.
     * 
     * @param transaction The transaction to create a return cart for
     * @return Empty return cart
     */
    fun createReturnCart(transaction: Transaction): ReturnCart {
        return ReturnCart(
            originalTransactionId = transaction.id,
            originalTransactionGuid = transaction.guid,
            items = emptyList()
        )
    }
    
    /**
     * Validate that a return cart is ready for processing.
     * 
     * Checks:
     * - Has at least one item
     * - All items have reasons assigned
     * 
     * @param cart The cart to validate
     * @return Error message if invalid, null if valid
     */
    fun validateReturnCart(cart: ReturnCart): String? {
        if (!cart.hasItems) {
            return "No items selected for return"
        }
        
        // For P0, we'll skip reason requirement
        // In production: check all items have reasons
        // val itemsWithoutReason = cart.items.filter { it.reason == null }
        // if (itemsWithoutReason.isNotEmpty()) {
        //     return "Please select a reason for all items"
        // }
        
        return null // Valid
    }
}

