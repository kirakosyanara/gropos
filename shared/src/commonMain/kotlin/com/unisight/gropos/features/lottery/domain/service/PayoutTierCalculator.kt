package com.unisight.gropos.features.lottery.domain.service

import com.unisight.gropos.features.lottery.domain.model.PayoutStatus
import com.unisight.gropos.features.lottery.domain.model.PayoutValidationResult
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Calculates the payout tier for lottery winnings.
 * 
 * **Per Phase 5 Requirements (W-2G Deferred):**
 * 
 * | Tier | Amount Range | Status | Action |
 * |------|--------------|--------|--------|
 * | 1 | $0 - $49.99 | APPROVED | Process immediately |
 * | 2 | $50.00 - $599.99 | LOGGED_ONLY | Process and log for audit |
 * | 3 | $600.00+ | REJECTED_OVER_LIMIT | Reject - manual claim required |
 * 
 * **CRITICAL: $600.00 Threshold**
 * This is the IRS W-2G reporting threshold. Since the POS does NOT
 * generate tax forms, payouts at or above this amount are rejected.
 * The customer must claim at a lottery office.
 * 
 * **Precision:**
 * Uses BigDecimal internally for financial calculations.
 * Input values are rounded to 2 decimal places.
 * 
 * Per code-quality.mdc: Business logic is pure (no side effects).
 */
object PayoutTierCalculator {
    
    // ========================================================================
    // Tier Boundaries (in dollars)
    // ========================================================================
    
    /** Maximum amount for Tier 1 (APPROVED) */
    const val TIER_1_MAX: Double = 49.99
    
    /** Maximum amount for Tier 2 (LOGGED_ONLY) */
    const val TIER_2_MAX: Double = 599.99
    
    /** Minimum amount requiring manual claim (Tier 3) */
    const val MANUAL_CLAIM_THRESHOLD: Double = 600.00
    
    // BigDecimal versions for precise comparison
    private val TIER_1_MAX_BD = BigDecimal("49.99")
    private val TIER_2_MAX_BD = BigDecimal("599.99")
    private val MANUAL_CLAIM_THRESHOLD_BD = BigDecimal("600.00")
    
    // ========================================================================
    // Core Calculation
    // ========================================================================
    
    /**
     * Calculates the payout tier for a given amount.
     * 
     * @param amount The payout amount in dollars (Double for convenience)
     * @return PayoutStatus indicating the tier and whether processing is allowed
     */
    fun calculateTier(amount: Double): PayoutStatus {
        // Convert to BigDecimal with 2 decimal places
        val amountBd = BigDecimal(amount)
            .setScale(2, RoundingMode.HALF_UP)
        
        return calculateTier(amountBd)
    }
    
    /**
     * Calculates the payout tier for a given amount (BigDecimal version).
     * 
     * **Logic:**
     * - amount <= 49.99 → APPROVED
     * - amount <= 599.99 → LOGGED_ONLY
     * - amount >= 600.00 → REJECTED_OVER_LIMIT
     * 
     * @param amount The payout amount as BigDecimal
     * @return PayoutStatus indicating the tier and whether processing is allowed
     */
    fun calculateTier(amount: BigDecimal): PayoutStatus {
        // Ensure 2 decimal places for comparison
        val normalizedAmount = amount.setScale(2, RoundingMode.HALF_UP)
        
        return when {
            // Tier 1: $0 - $49.99
            normalizedAmount <= TIER_1_MAX_BD -> PayoutStatus.APPROVED
            
            // Tier 2: $50.00 - $599.99
            normalizedAmount <= TIER_2_MAX_BD -> PayoutStatus.LOGGED_ONLY
            
            // Tier 3: $600.00+
            else -> PayoutStatus.REJECTED_OVER_LIMIT
        }
    }
    
    // ========================================================================
    // Validation with Context
    // ========================================================================
    
    /**
     * Validates a payout and returns a result with context.
     * 
     * @param amount The payout amount in dollars
     * @return PayoutValidationResult with status and user-friendly message
     */
    fun validatePayout(amount: Double): PayoutValidationResult {
        val amountBd = BigDecimal(amount).setScale(2, RoundingMode.HALF_UP)
        return validatePayout(amountBd)
    }
    
    /**
     * Validates a payout and returns a result with context (BigDecimal version).
     * 
     * @param amount The payout amount as BigDecimal
     * @return PayoutValidationResult with status and user-friendly message
     */
    fun validatePayout(amount: BigDecimal): PayoutValidationResult {
        val status = calculateTier(amount)
        
        val message = when (status) {
            PayoutStatus.APPROVED -> 
                "Payout approved. No additional logging required."
            
            PayoutStatus.LOGGED_ONLY -> 
                "Payout approved. Transaction will be logged for audit."
            
            PayoutStatus.REJECTED_OVER_LIMIT -> 
                "Amount exceeds $${MANUAL_CLAIM_THRESHOLD.toInt()} limit. " +
                "Customer must claim at a lottery office with valid ID."
        }
        
        return PayoutValidationResult(
            status = status,
            amount = amount.setScale(2, RoundingMode.HALF_UP),
            message = message
        )
    }
    
    // ========================================================================
    // Helper Methods
    // ========================================================================
    
    /**
     * Checks if an amount requires manager approval.
     * 
     * Per LOTTERY_PAYOUTS.md: Manager approval required for Tier 2+.
     * 
     * @param amount The payout amount
     * @return true if manager approval is recommended
     */
    fun requiresManagerApproval(amount: Double): Boolean {
        val amountBd = BigDecimal(amount).setScale(2, RoundingMode.HALF_UP)
        return amountBd > TIER_1_MAX_BD
    }
    
    /**
     * Checks if an amount can be processed by the POS.
     * 
     * @param amount The payout amount
     * @return true if the POS can process this payout
     */
    fun canProcess(amount: Double): Boolean {
        val status = calculateTier(amount)
        return status.allowsProcessing
    }
    
    /**
     * Gets the tier number for an amount.
     * 
     * @param amount The payout amount
     * @return Tier number (1, 2, or 3)
     */
    fun getTierNumber(amount: Double): Int {
        return calculateTier(amount).tier
    }
    
    /**
     * Formats the tier limits for display.
     * 
     * @return Human-readable tier description
     */
    fun getTierDescription(): String {
        return """
            Tier 1: $0.00 - $${String.format("%.2f", TIER_1_MAX)} (Approved)
            Tier 2: $${String.format("%.2f", TIER_1_MAX + 0.01)} - $${String.format("%.2f", TIER_2_MAX)} (Logged)
            Tier 3: $${String.format("%.2f", MANUAL_CLAIM_THRESHOLD)}+ (Manual Claim Required)
        """.trimIndent()
    }
}

