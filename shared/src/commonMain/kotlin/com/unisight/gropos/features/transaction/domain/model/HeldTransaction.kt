package com.unisight.gropos.features.transaction.domain.model

import java.math.BigDecimal

/**
 * Represents a held (suspended) transaction.
 * 
 * Per TRANSACTION_FLOW.md: "Holding" creates a Transaction record with status HELD.
 * Per FUNCTIONS_MENU.md: Recall retrieves held transactions for resumption.
 * 
 * A held transaction stores the complete cart state including all items,
 * their quantities, prices, and discounts. When recalled, this data is
 * restored to the active cart identically.
 * 
 * Per code-quality.mdc: Use BigDecimal for all monetary calculations.
 */
data class HeldTransaction(
    /**
     * Unique identifier for this held transaction.
     * Generated when the transaction is held.
     */
    val id: String,
    
    /**
     * Optional name/note for identifying this held transaction.
     * Examples: "Guy in blue shirt", "Hold-14:32"
     */
    val holdName: String,
    
    /**
     * Timestamp when the transaction was held (ISO-8601 format).
     */
    val heldDateTime: String,
    
    /**
     * Employee ID who held the transaction.
     */
    val employeeId: Int?,
    
    /**
     * Employee name who held the transaction.
     */
    val employeeName: String?,
    
    /**
     * Station/Register ID where the transaction was held.
     */
    val stationId: Int,
    
    /**
     * Branch ID where the transaction was held.
     */
    val branchId: Int,
    
    /**
     * Total number of items in the held transaction.
     */
    val itemCount: Int,
    
    /**
     * Grand total of the held transaction.
     */
    val grandTotal: BigDecimal,
    
    /**
     * Subtotal before tax.
     */
    val subTotal: BigDecimal,
    
    /**
     * Total tax amount.
     */
    val taxTotal: BigDecimal,
    
    /**
     * Total CRV amount.
     */
    val crvTotal: BigDecimal,
    
    /**
     * Serialized cart items for restoration.
     * Contains all information needed to fully restore the cart.
     */
    val items: List<HeldTransactionItem>
)

/**
 * Represents an item in a held transaction.
 * 
 * Contains all data needed to restore a CartItem when the transaction is recalled.
 */
data class HeldTransactionItem(
    /**
     * Branch product ID - used to reload product details.
     */
    val branchProductId: Int,
    
    /**
     * Product name at time of hold.
     */
    val productName: String,
    
    /**
     * Quantity of the item.
     */
    val quantityUsed: BigDecimal,
    
    /**
     * Price used for this item (may differ from retail if on sale/overridden).
     */
    val priceUsed: BigDecimal,
    
    /**
     * Discount amount per unit.
     */
    val discountAmountPerUnit: BigDecimal,
    
    /**
     * Transaction-level discount per unit.
     */
    val transactionDiscountAmountPerUnit: BigDecimal,
    
    /**
     * Whether this item was voided/removed.
     */
    val isRemoved: Boolean,
    
    /**
     * Whether price was manually prompted.
     */
    val isPromptedPrice: Boolean,
    
    /**
     * Whether floor price was overridden.
     */
    val isFloorPriceOverridden: Boolean,
    
    /**
     * Scan timestamp (ISO-8601 format).
     */
    val scanDateTime: String?
)

