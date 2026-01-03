package com.unisight.gropos.features.lottery.domain.model

import kotlinx.datetime.Instant
import java.math.BigDecimal

/**
 * Represents a lottery game available for sale.
 * 
 * **Per INDEX.md (Lottery Module):**
 * - Scratchers: Pre-printed tickets with instant win
 * - Draw Games: Pick numbers for scheduled drawings (Powerball, Mega Millions, etc.)
 * 
 * @property id Unique identifier for the game
 * @property name Display name (e.g., "$100M Cash Explosion", "Powerball")
 * @property ticketPrice Price per ticket in dollars
 * @property isActive Whether the game is currently available for sale
 * @property type Type of lottery game (SCRATCHER or DRAW)
 * @property gameNumber State lottery game number for tracking
 */
data class LotteryGame(
    val id: String,
    val name: String,
    val ticketPrice: BigDecimal,
    val isActive: Boolean = true,
    val type: LotteryGameType,
    val gameNumber: String? = null
)

/**
 * Type of lottery game.
 * 
 * Per LOTTERY_SALES.md:
 * - SCRATCHER: Instant win tickets
 * - DRAW: Pick numbers for scheduled drawings
 */
enum class LotteryGameType {
    /** Scratch-off instant win tickets */
    SCRATCHER,
    
    /** Draw games with scheduled drawings (Powerball, Mega Millions, etc.) */
    DRAW
}

/**
 * Represents a lottery transaction (sale or payout).
 * 
 * **Per LOTTERY_SALES.md & LOTTERY_PAYOUTS.md:**
 * - Sales: Cash-only, adds to daily lottery sales total
 * - Payouts: Tiers 1-2 processed at POS, Tier 3 rejected
 * 
 * @property id Unique transaction identifier
 * @property type SALE or PAYOUT
 * @property amount Transaction amount in dollars
 * @property gameId ID of the lottery game
 * @property gameName Display name of the game (denormalized for receipts)
 * @property timestamp When the transaction occurred
 * @property staffId Employee who processed the transaction
 * @property quantity Number of tickets (for sales)
 * @property payoutStatus Status of payout (null for sales)
 */
data class LotteryTransaction(
    val id: String,
    val type: LotteryTransactionType,
    val amount: BigDecimal,
    val gameId: String,
    val gameName: String,
    val timestamp: Instant,
    val staffId: Int,
    val quantity: Int = 1,
    val payoutStatus: PayoutStatus? = null
)

/**
 * Type of lottery transaction.
 */
enum class LotteryTransactionType {
    /** Sale of lottery tickets */
    SALE,
    
    /** Payout of winnings */
    PAYOUT
}

/**
 * Status of a payout request based on tier calculation.
 * 
 * **Per Phase 5 Requirements (W-2G Deferred):**
 * - Tier 1 ($0 - $49.99): APPROVED - Cashier processes normally
 * - Tier 2 ($50.00 - $599.99): LOGGED_ONLY - Valid but recorded for audit
 * - Tier 3 ($600.00+): REJECTED_OVER_LIMIT - Requires manual claim at lottery office
 * 
 * **CRITICAL:** The $600.00 threshold aligns with IRS W-2G reporting requirements.
 * Since we're deferring W-2G generation, the POS rejects payouts at this level.
 * 
 * @property label Human-readable label for UI display
 * @property tier The tier number (1, 2, or 3)
 * @property allowsProcessing Whether the POS can process this payout
 */
enum class PayoutStatus(
    val label: String,
    val tier: Int,
    val allowsProcessing: Boolean
) {
    /** Tier 1: $0 - $49.99 - Approved for immediate processing */
    APPROVED(
        label = "Approved",
        tier = 1,
        allowsProcessing = true
    ),
    
    /** Tier 2: $50.00 - $599.99 - Approved but logged for audit trail */
    LOGGED_ONLY(
        label = "Logged for Audit",
        tier = 2,
        allowsProcessing = true
    ),
    
    /** Tier 3: $600.00+ - Rejected, requires manual claim at lottery office */
    REJECTED_OVER_LIMIT(
        label = "Requires Manual Claim",
        tier = 3,
        allowsProcessing = false
    )
}

/**
 * Result of a lottery payout validation.
 * 
 * Contains the status and additional context for UI display.
 */
data class PayoutValidationResult(
    val status: PayoutStatus,
    val amount: BigDecimal,
    val message: String
) {
    val canProcess: Boolean get() = status.allowsProcessing
}

/**
 * Daily lottery summary for reporting.
 * 
 * Per LOTTERY_REPORTS.md:
 * - Sales total
 * - Payouts total
 * - Net (Sales - Payouts)
 * - Commission (if applicable)
 */
data class LotteryDailySummary(
    val date: String, // ISO-8601 date
    val totalSales: BigDecimal,
    val totalPayouts: BigDecimal,
    val netAmount: BigDecimal,
    val transactionCount: Int,
    val scratcherSales: BigDecimal = BigDecimal.ZERO,
    val drawGameSales: BigDecimal = BigDecimal.ZERO
)

