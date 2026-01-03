package com.unisight.gropos.features.lottery.domain.repository

import com.unisight.gropos.features.lottery.domain.model.LotteryDailySummary
import com.unisight.gropos.features.lottery.domain.model.LotteryGame
import com.unisight.gropos.features.lottery.domain.model.LotteryTransaction
import com.unisight.gropos.features.lottery.domain.model.PayoutStatus
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

/**
 * Repository interface for lottery operations.
 * 
 * **Per INDEX.md (Lottery Module):**
 * - Manages lottery game catalog
 * - Records sales and payout transactions
 * - Enforces payout tier limits
 * 
 * **Payout Processing Rules:**
 * - Tier 1 ($0 - $49.99): APPROVED - Process immediately
 * - Tier 2 ($50.00 - $599.99): LOGGED_ONLY - Process and log for audit
 * - Tier 3 ($600.00+): REJECTED_OVER_LIMIT - CANNOT process
 * 
 * Per project-structure.mdc:
 * - Interface defined in Domain layer
 * - Implementations in Data layer (FakeLotteryRepository, RemoteLotteryRepository)
 */
interface LotteryRepository {
    
    // ========================================================================
    // Game Catalog
    // ========================================================================
    
    /**
     * Gets all active lottery games as a Flow.
     * 
     * @return Flow emitting list of active games
     */
    fun getActiveGames(): Flow<List<LotteryGame>>
    
    /**
     * Gets a specific game by ID.
     * 
     * @param gameId The game identifier
     * @return The game if found, null otherwise
     */
    suspend fun getGameById(gameId: String): LotteryGame?
    
    /**
     * Searches games by name.
     * 
     * @param query Search query
     * @return List of matching games
     */
    suspend fun searchGames(query: String): List<LotteryGame>
    
    // ========================================================================
    // Sales
    // ========================================================================
    
    /**
     * Records a lottery ticket sale.
     * 
     * **Per LOTTERY_SALES.md:**
     * - Cash-only transactions
     * - Updates daily sales total
     * - Prints ticket receipt
     * 
     * @param gameId ID of the game being purchased
     * @param quantity Number of tickets
     * @param staffId Employee processing the sale
     * @return Result containing the transaction or error
     */
    suspend fun recordSale(
        gameId: String,
        quantity: Int,
        staffId: Int
    ): Result<LotteryTransaction>
    
    // ========================================================================
    // Payouts
    // ========================================================================
    
    /**
     * Checks if a payout amount can be processed.
     * 
     * **Tier Logic:**
     * - $0 - $49.99: APPROVED
     * - $50.00 - $599.99: LOGGED_ONLY
     * - $600.00+: REJECTED_OVER_LIMIT
     * 
     * @param amount The payout amount in dollars
     * @return PayoutStatus indicating whether processing is allowed
     */
    fun checkPayoutFeasibility(amount: Double): PayoutStatus
    
    /**
     * Checks payout feasibility with BigDecimal precision.
     */
    fun checkPayoutFeasibility(amount: BigDecimal): PayoutStatus
    
    /**
     * Processes a lottery payout.
     * 
     * **CRITICAL CONSTRAINT:**
     * This method MUST fail if `checkPayoutFeasibility()` returns `REJECTED_OVER_LIMIT`.
     * 
     * @param amount The payout amount in dollars
     * @param gameId ID of the game being paid out (optional for generic payouts)
     * @param staffId Employee processing the payout
     * @return Result containing the transaction or error
     * @throws LotteryPayoutRejectedException if amount >= $600.00
     */
    suspend fun processPayout(
        amount: Double,
        gameId: String?,
        staffId: Int
    ): Result<LotteryTransaction>
    
    /**
     * Processes a lottery payout with BigDecimal precision.
     */
    suspend fun processPayout(
        amount: BigDecimal,
        gameId: String?,
        staffId: Int
    ): Result<LotteryTransaction>
    
    // ========================================================================
    // Transaction History
    // ========================================================================
    
    /**
     * Gets lottery transactions for the current day.
     * 
     * @return Flow of today's transactions
     */
    fun getTodaysTransactions(): Flow<List<LotteryTransaction>>
    
    /**
     * Gets the daily summary for a specific date.
     * 
     * @param date ISO-8601 date string (YYYY-MM-DD)
     * @return Daily summary or null if no transactions
     */
    suspend fun getDailySummary(date: String): LotteryDailySummary?
    
    /**
     * Gets the current day's running totals.
     * 
     * @return Current daily summary
     */
    suspend fun getCurrentDaySummary(): LotteryDailySummary
}

/**
 * Exception thrown when a payout is rejected due to exceeding the limit.
 * 
 * Per Phase 5 Requirements: $600+ payouts require manual claim.
 */
class LotteryPayoutRejectedException(
    val amount: BigDecimal,
    message: String = "Payout of $$amount exceeds $600.00 limit. Customer must claim at lottery office."
) : Exception(message)

/**
 * Exception thrown when a game is not found.
 */
class LotteryGameNotFoundException(
    val gameId: String,
    message: String = "Lottery game not found: $gameId"
) : Exception(message)

