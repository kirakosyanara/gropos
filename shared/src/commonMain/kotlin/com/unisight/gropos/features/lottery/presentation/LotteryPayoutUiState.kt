package com.unisight.gropos.features.lottery.presentation

import androidx.compose.runtime.Immutable
import com.unisight.gropos.features.lottery.domain.model.PayoutStatus
import com.unisight.gropos.features.lottery.domain.model.PayoutValidationResult
import java.math.BigDecimal

/**
 * UI State for the Lottery Payout screen.
 * 
 * **Per LOTTERY_PAYOUTS.md:**
 * - Enter payout amount
 * - Display tier validation result
 * - Show approval/rejection status
 * 
 * Per kotlin-compose.mdc: All data classes in Composable params must be @Immutable.
 */
@Immutable
data class LotteryPayoutUiState(
    /** The entered amount (in dollars). */
    val amount: BigDecimal = BigDecimal.ZERO,
    
    /** Display string for the keypad input. */
    val displayAmount: String = "0",
    
    /** Validation result from PayoutTierCalculator. */
    val validationResult: PayoutValidationResult? = null,
    
    /** Loading state during payout processing. */
    val isProcessing: Boolean = false,
    
    /** Error message to display. */
    val errorMessage: String? = null,
    
    /** Success message after payout. */
    val successMessage: String? = null
) {
    /**
     * Whether the payout can be processed.
     * True if amount > 0 and tier allows processing.
     */
    val canProcess: Boolean
        get() = amount > BigDecimal.ZERO && 
                validationResult?.canProcess == true &&
                !isProcessing
    
    /**
     * The current tier number (1, 2, or 3) or null if no amount.
     */
    val tierNumber: Int?
        get() = validationResult?.status?.tier
    
    /**
     * Whether to show the rejection message (Tier 3).
     */
    val showRejection: Boolean
        get() = validationResult?.status == PayoutStatus.REJECTED_OVER_LIMIT
    
    /**
     * Status message for the current tier.
     */
    val statusMessage: String?
        get() = validationResult?.message
}

