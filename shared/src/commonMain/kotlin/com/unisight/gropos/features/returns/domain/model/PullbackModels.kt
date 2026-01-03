package com.unisight.gropos.features.returns.domain.model

import com.unisight.gropos.features.transaction.domain.model.Transaction
import java.math.BigDecimal

/**
 * Pullback Models
 * 
 * Per REMEDIATION_CHECKLIST: Pullback Flow - Implement pullback with receipt scan.
 * Per RETURNS.md: Pullback allows recalling items from a previous transaction.
 * 
 * A "pullback" is used when:
 * - Customer returns immediately after a transaction
 * - Need to add forgotten items to a just-completed transaction
 * - Correction needed after payment completed
 */

/**
 * Result of a pullback operation.
 */
sealed class PullbackResult {
    /**
     * Pullback successful - transaction found and items loaded.
     */
    data class Success(
        val transaction: Transaction,
        val items: List<PullbackItem>
    ) : PullbackResult()
    
    /**
     * Transaction not found by receipt number (guid).
     */
    data class NotFound(val receiptNumber: String) : PullbackResult()
    
    /**
     * Transaction found but cannot be pulled back (too old, already returned, etc.)
     */
    data class NotEligible(
        val transaction: Transaction,
        val reason: String
    ) : PullbackResult()
    
    /**
     * Error occurred during pullback.
     */
    data class Error(val message: String) : PullbackResult()
}

/**
 * An item that can be pulled back from a transaction.
 */
data class PullbackItem(
    /** Original transaction item ID */
    val originalItemId: Long,
    
    /** Product ID */
    val branchProductId: Int,
    
    /** Product name */
    val productName: String,
    
    /** Quantity sold in original transaction */
    val originalQuantity: BigDecimal,
    
    /** Quantity already returned (if any) */
    val returnedQuantity: BigDecimal = BigDecimal.ZERO,
    
    /** Quantity available for pullback */
    val availableQuantity: BigDecimal,
    
    /** Quantity selected for this pullback */
    val selectedQuantity: BigDecimal = BigDecimal.ZERO,
    
    /** Unit price used in original transaction */
    val priceUsed: BigDecimal,
    
    /** Whether item is eligible for pullback */
    val isEligible: Boolean = true,
    
    /** Reason if not eligible */
    val ineligibleReason: String? = null
) {
    /** Total value of selected quantity */
    val selectedValue: BigDecimal
        get() = selectedQuantity.multiply(priceUsed)
    
    /** Whether any quantity is selected */
    val isSelected: Boolean
        get() = selectedQuantity > BigDecimal.ZERO
    
    /** Maximum quantity that can be selected */
    val maxSelectable: BigDecimal
        get() = availableQuantity
}

/**
 * State for the pullback flow UI.
 */
data class PullbackState(
    /** Current step in the pullback flow */
    val step: PullbackStep = PullbackStep.SCAN_RECEIPT,
    
    /** Receipt number entered/scanned */
    val receiptNumber: String = "",
    
    /** Whether search is in progress */
    val isSearching: Boolean = false,
    
    /** The found transaction */
    val transaction: Transaction? = null,
    
    /** Items available for pullback */
    val items: List<PullbackItem> = emptyList(),
    
    /** Error message if any */
    val errorMessage: String? = null,
    
    /** Total value of selected items */
    val selectedTotal: BigDecimal = BigDecimal.ZERO,
    
    /** Number of items selected */
    val selectedItemCount: Int = 0
) {
    companion object {
        fun initial() = PullbackState()
    }
}

/**
 * Steps in the pullback flow.
 */
enum class PullbackStep {
    /** Scan or enter receipt number */
    SCAN_RECEIPT,
    
    /** Select items to pull back */
    SELECT_ITEMS,
    
    /** Confirm pullback */
    CONFIRM,
    
    /** Processing */
    PROCESSING,
    
    /** Completed */
    COMPLETE
}

/**
 * Configuration for pullback eligibility.
 */
data class PullbackConfig(
    /** Maximum days since transaction for pullback */
    val maxDaysOld: Int = 1,
    
    /** Whether to require manager approval */
    val requiresApproval: Boolean = true,
    
    /** Maximum transaction amount for pullback */
    val maxTransactionAmount: BigDecimal = BigDecimal("500.00"),
    
    /** Whether to allow partial pullback */
    val allowPartial: Boolean = true
)

