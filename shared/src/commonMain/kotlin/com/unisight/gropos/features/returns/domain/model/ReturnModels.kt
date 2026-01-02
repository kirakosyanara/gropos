package com.unisight.gropos.features.returns.domain.model

import com.unisight.gropos.features.transaction.domain.model.TransactionItem
import java.math.BigDecimal

/**
 * Return reason codes per RETURNS.md
 */
enum class ReturnReason(val code: String, val description: String) {
    DEFECTIVE("DEF", "Defective Product"),
    WRONG_ITEM("WRG", "Wrong Item"),
    CHANGED_MIND("CHM", "Changed Mind"),
    QUALITY("QLT", "Quality Issue"),
    OTHER("OTH", "Other")
}

/**
 * Represents an item that can be returned from a transaction.
 * 
 * Per RETURNS.md:
 * - Tracks original quantity sold
 * - Tracks quantity already returned
 * - Calculates remaining returnable quantity
 * 
 * @property originalItem The original transaction line item
 * @property quantityAlreadyReturned Quantity previously returned
 */
data class ReturnableItem(
    val originalItem: TransactionItem,
    val quantityAlreadyReturned: BigDecimal = BigDecimal.ZERO
) {
    /**
     * Quantity that can still be returned
     */
    val remainingQuantity: BigDecimal
        get() = (originalItem.quantityUsed - quantityAlreadyReturned).coerceAtLeast(BigDecimal.ZERO)
    
    /**
     * Whether any items can still be returned
     */
    val canReturn: Boolean
        get() = remainingQuantity > BigDecimal.ZERO
    
    /**
     * Unit price to refund
     */
    val refundPricePerUnit: BigDecimal
        get() = originalItem.priceUsed
    
    /**
     * Display name
     */
    val productName: String
        get() = originalItem.branchProductName
}

/**
 * Represents an item selected for return.
 * 
 * Per RETURNS.md ReturnItem data model.
 * 
 * @property originalItem The original transaction item
 * @property returnQuantity How many to return
 * @property reason Return reason (required before processing)
 * @property notes Optional notes
 */
data class ReturnItem(
    val originalItem: TransactionItem,
    val returnQuantity: BigDecimal,
    val reason: ReturnReason? = null,
    val notes: String? = null
) {
    /**
     * Calculate refund amount for this item.
     * Returns a positive BigDecimal (will be displayed/stored as negative in UI/DB).
     */
    val refundAmount: BigDecimal
        get() = originalItem.priceUsed * returnQuantity
    
    /**
     * Calculate tax refund for this item
     */
    val taxRefundAmount: BigDecimal
        get() = originalItem.taxPerUnit * returnQuantity
    
    /**
     * Calculate CRV refund for this item
     */
    val crvRefundAmount: BigDecimal
        get() = originalItem.crvRatePerUnit * returnQuantity
    
    /**
     * Total refund including tax and CRV
     */
    val totalRefundAmount: BigDecimal
        get() = refundAmount + taxRefundAmount + crvRefundAmount
    
    /**
     * Display name
     */
    val productName: String
        get() = originalItem.branchProductName
}

/**
 * Return cart - holds items selected for return.
 * 
 * Similar to Cart but for negative (refund) amounts.
 * 
 * @property originalTransactionId The transaction being returned from
 * @property originalTransactionGuid GUID of original transaction
 * @property items Items selected for return
 */
data class ReturnCart(
    val originalTransactionId: Long,
    val originalTransactionGuid: String,
    val items: List<ReturnItem> = emptyList()
) {
    /**
     * Total refund subtotal (before tax/CRV)
     */
    val subtotalRefund: BigDecimal
        get() = items.sumOf { it.refundAmount }
    
    /**
     * Total tax to refund
     */
    val taxRefund: BigDecimal
        get() = items.sumOf { it.taxRefundAmount }
    
    /**
     * Total CRV to refund
     */
    val crvRefund: BigDecimal
        get() = items.sumOf { it.crvRefundAmount }
    
    /**
     * Grand total to refund
     */
    val totalRefund: BigDecimal
        get() = items.sumOf { it.totalRefundAmount }
    
    /**
     * Count of items in return cart
     */
    val itemCount: Int
        get() = items.size
    
    /**
     * Total quantity being returned
     */
    val totalQuantity: BigDecimal
        get() = items.sumOf { it.returnQuantity }
    
    /**
     * Whether cart has items
     */
    val hasItems: Boolean
        get() = items.isNotEmpty()
    
    /**
     * Add an item to the return cart
     */
    fun addItem(item: ReturnItem): ReturnCart {
        // Check if this product is already in the cart
        val existingIndex = items.indexOfFirst { 
            it.originalItem.id == item.originalItem.id 
        }
        
        return if (existingIndex >= 0) {
            // Merge quantities
            val existing = items[existingIndex]
            val merged = existing.copy(
                returnQuantity = existing.returnQuantity + item.returnQuantity
            )
            copy(items = items.toMutableList().apply { 
                this[existingIndex] = merged 
            })
        } else {
            copy(items = items + item)
        }
    }
    
    /**
     * Remove an item from the return cart
     */
    fun removeItem(itemId: Long): ReturnCart {
        return copy(items = items.filter { it.originalItem.id != itemId })
    }
    
    /**
     * Clear all items
     */
    fun clear(): ReturnCart {
        return copy(items = emptyList())
    }
}

