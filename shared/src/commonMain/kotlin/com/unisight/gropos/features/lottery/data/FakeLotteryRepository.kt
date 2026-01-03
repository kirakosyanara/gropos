package com.unisight.gropos.features.lottery.data

import com.unisight.gropos.features.lottery.domain.model.LotteryDailySummary
import com.unisight.gropos.features.lottery.domain.model.LotteryGame
import com.unisight.gropos.features.lottery.domain.model.LotteryGameType
import com.unisight.gropos.features.lottery.domain.model.LotteryTransaction
import com.unisight.gropos.features.lottery.domain.model.LotteryTransactionType
import com.unisight.gropos.features.lottery.domain.model.PayoutStatus
import com.unisight.gropos.features.lottery.domain.repository.LotteryGameNotFoundException
import com.unisight.gropos.features.lottery.domain.repository.LotteryPayoutRejectedException
import com.unisight.gropos.features.lottery.domain.repository.LotteryRepository
import com.unisight.gropos.features.lottery.domain.service.PayoutTierCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal
import java.util.UUID

/**
 * Fake implementation of LotteryRepository for development and testing.
 * 
 * **Per testing-strategy.mdc:** "Use Fakes for State"
 * - Maintains in-memory state
 * - Seeded with realistic lottery games
 * - Simulates real repository behavior
 * 
 * **Seeded Games:**
 * - 5 Scratcher games at various price points
 * - 5 Draw games (Powerball, Mega Millions, etc.)
 * 
 * Per LOTTERY_SALES.md: Common game types and prices.
 */
class FakeLotteryRepository : LotteryRepository {
    
    // ========================================================================
    // In-Memory State
    // ========================================================================
    
    /**
     * Seeded lottery games per LOTTERY_SALES.md.
     * 
     * Price ranges:
     * - Scratchers: $1, $2, $5, $10, $20
     * - Draw games: $2 (most common)
     */
    private val games = mutableListOf(
        // Scratchers
        LotteryGame(
            id = "scratch_001",
            name = "Lucky 7s",
            ticketPrice = BigDecimal("1.00"),
            isActive = true,
            type = LotteryGameType.SCRATCHER,
            gameNumber = "1001"
        ),
        LotteryGame(
            id = "scratch_002",
            name = "Cash Blast",
            ticketPrice = BigDecimal("2.00"),
            isActive = true,
            type = LotteryGameType.SCRATCHER,
            gameNumber = "1002"
        ),
        LotteryGame(
            id = "scratch_003",
            name = "$100M Cash Explosion",
            ticketPrice = BigDecimal("5.00"),
            isActive = true,
            type = LotteryGameType.SCRATCHER,
            gameNumber = "1003"
        ),
        LotteryGame(
            id = "scratch_004",
            name = "Diamond Jackpot",
            ticketPrice = BigDecimal("10.00"),
            isActive = true,
            type = LotteryGameType.SCRATCHER,
            gameNumber = "1004"
        ),
        LotteryGame(
            id = "scratch_005",
            name = "Millionaire's Club",
            ticketPrice = BigDecimal("20.00"),
            isActive = true,
            type = LotteryGameType.SCRATCHER,
            gameNumber = "1005"
        ),
        
        // Draw Games
        LotteryGame(
            id = "draw_001",
            name = "Powerball",
            ticketPrice = BigDecimal("2.00"),
            isActive = true,
            type = LotteryGameType.DRAW,
            gameNumber = "2001"
        ),
        LotteryGame(
            id = "draw_002",
            name = "Mega Millions",
            ticketPrice = BigDecimal("2.00"),
            isActive = true,
            type = LotteryGameType.DRAW,
            gameNumber = "2002"
        ),
        LotteryGame(
            id = "draw_003",
            name = "Daily 3",
            ticketPrice = BigDecimal("1.00"),
            isActive = true,
            type = LotteryGameType.DRAW,
            gameNumber = "2003"
        ),
        LotteryGame(
            id = "draw_004",
            name = "Pick 4",
            ticketPrice = BigDecimal("1.00"),
            isActive = true,
            type = LotteryGameType.DRAW,
            gameNumber = "2004"
        ),
        LotteryGame(
            id = "draw_005",
            name = "Fantasy 5",
            ticketPrice = BigDecimal("1.00"),
            isActive = true,
            type = LotteryGameType.DRAW,
            gameNumber = "2005"
        )
    )
    
    /**
     * In-memory transaction storage.
     */
    private val transactions = mutableListOf<LotteryTransaction>()
    private val _transactionsFlow = MutableStateFlow<List<LotteryTransaction>>(emptyList())
    
    // ========================================================================
    // Game Catalog
    // ========================================================================
    
    override fun getActiveGames(): Flow<List<LotteryGame>> {
        return MutableStateFlow(games.filter { it.isActive }).asStateFlow()
    }
    
    override suspend fun getGameById(gameId: String): LotteryGame? {
        return games.find { it.id == gameId }
    }
    
    override suspend fun searchGames(query: String): List<LotteryGame> {
        if (query.isBlank()) return games.filter { it.isActive }
        
        val lowerQuery = query.lowercase()
        return games.filter { game ->
            game.isActive && (
                game.name.lowercase().contains(lowerQuery) ||
                game.gameNumber?.contains(query) == true
            )
        }
    }
    
    // ========================================================================
    // Sales
    // ========================================================================
    
    override suspend fun recordSale(
        gameId: String,
        quantity: Int,
        staffId: Int
    ): Result<LotteryTransaction> {
        val game = games.find { it.id == gameId }
            ?: return Result.failure(LotteryGameNotFoundException(gameId))
        
        if (!game.isActive) {
            return Result.failure(IllegalStateException("Game '${game.name}' is not currently active"))
        }
        
        if (quantity < 1) {
            return Result.failure(IllegalArgumentException("Quantity must be at least 1"))
        }
        
        val totalAmount = game.ticketPrice.multiply(BigDecimal(quantity))
        
        val transaction = LotteryTransaction(
            id = UUID.randomUUID().toString(),
            type = LotteryTransactionType.SALE,
            amount = totalAmount,
            gameId = game.id,
            gameName = game.name,
            timestamp = Clock.System.now(),
            staffId = staffId,
            quantity = quantity,
            payoutStatus = null
        )
        
        transactions.add(transaction)
        _transactionsFlow.value = transactions.toList()
        
        println("[LOTTERY_SALE] ${game.name} x$quantity = $$totalAmount (Staff: $staffId)")
        
        return Result.success(transaction)
    }
    
    // ========================================================================
    // Payouts
    // ========================================================================
    
    override fun checkPayoutFeasibility(amount: Double): PayoutStatus {
        return PayoutTierCalculator.calculateTier(amount)
    }
    
    override fun checkPayoutFeasibility(amount: BigDecimal): PayoutStatus {
        return PayoutTierCalculator.calculateTier(amount)
    }
    
    override suspend fun processPayout(
        amount: Double,
        gameId: String?,
        staffId: Int
    ): Result<LotteryTransaction> {
        return processPayout(BigDecimal(amount), gameId, staffId)
    }
    
    override suspend fun processPayout(
        amount: BigDecimal,
        gameId: String?,
        staffId: Int
    ): Result<LotteryTransaction> {
        // Check feasibility FIRST
        val status = checkPayoutFeasibility(amount)
        
        // CRITICAL: Reject if over limit
        if (status == PayoutStatus.REJECTED_OVER_LIMIT) {
            return Result.failure(LotteryPayoutRejectedException(amount))
        }
        
        // Resolve game if provided
        val game = if (gameId != null) {
            games.find { it.id == gameId }
        } else null
        
        val transaction = LotteryTransaction(
            id = UUID.randomUUID().toString(),
            type = LotteryTransactionType.PAYOUT,
            amount = amount,
            gameId = gameId ?: "generic_payout",
            gameName = game?.name ?: "Lottery Payout",
            timestamp = Clock.System.now(),
            staffId = staffId,
            quantity = 1,
            payoutStatus = status
        )
        
        transactions.add(transaction)
        _transactionsFlow.value = transactions.toList()
        
        println("[LOTTERY_PAYOUT] $$amount - Status: ${status.label} (Staff: $staffId)")
        
        return Result.success(transaction)
    }
    
    // ========================================================================
    // Transaction History
    // ========================================================================
    
    override fun getTodaysTransactions(): Flow<List<LotteryTransaction>> {
        val today = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date.toString()
        
        return _transactionsFlow.map { txns ->
            txns.filter { txn ->
                txn.timestamp.toLocalDateTime(TimeZone.currentSystemDefault())
                    .date.toString() == today
            }
        }
    }
    
    override suspend fun getDailySummary(date: String): LotteryDailySummary? {
        val dayTransactions = transactions.filter { txn ->
            txn.timestamp.toLocalDateTime(TimeZone.currentSystemDefault())
                .date.toString() == date
        }
        
        if (dayTransactions.isEmpty()) return null
        
        val sales = dayTransactions.filter { it.type == LotteryTransactionType.SALE }
        val payouts = dayTransactions.filter { it.type == LotteryTransactionType.PAYOUT }
        
        val totalSales = sales.sumOf { it.amount }
        val totalPayouts = payouts.sumOf { it.amount }
        
        return LotteryDailySummary(
            date = date,
            totalSales = totalSales,
            totalPayouts = totalPayouts,
            netAmount = totalSales.subtract(totalPayouts),
            transactionCount = dayTransactions.size,
            scratcherSales = sales
                .filter { games.find { g -> g.id == it.gameId }?.type == LotteryGameType.SCRATCHER }
                .sumOf { it.amount },
            drawGameSales = sales
                .filter { games.find { g -> g.id == it.gameId }?.type == LotteryGameType.DRAW }
                .sumOf { it.amount }
        )
    }
    
    override suspend fun getCurrentDaySummary(): LotteryDailySummary {
        val today = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date.toString()
        
        return getDailySummary(today) ?: LotteryDailySummary(
            date = today,
            totalSales = BigDecimal.ZERO,
            totalPayouts = BigDecimal.ZERO,
            netAmount = BigDecimal.ZERO,
            transactionCount = 0
        )
    }
    
    // ========================================================================
    // Test Helpers
    // ========================================================================
    
    /**
     * Clears all transactions (for testing).
     */
    fun clearTransactions() {
        transactions.clear()
        _transactionsFlow.value = emptyList()
    }
    
    /**
     * Adds a custom game (for testing).
     */
    fun addGame(game: LotteryGame) {
        games.add(game)
    }
    
    /**
     * Sets a game's active status (for testing).
     */
    fun setGameActive(gameId: String, isActive: Boolean) {
        val index = games.indexOfFirst { it.id == gameId }
        if (index >= 0) {
            games[index] = games[index].copy(isActive = isActive)
        }
    }
    
    /**
     * Gets all transactions (for testing).
     */
    fun getAllTransactions(): List<LotteryTransaction> = transactions.toList()
    
    /**
     * Gets all games including inactive (for testing).
     */
    fun getAllGames(): List<LotteryGame> = games.toList()
}

/**
 * Extension to sum BigDecimals.
 */
private fun Iterable<BigDecimal>.sum(): BigDecimal {
    var result = BigDecimal.ZERO
    for (value in this) {
        result = result.add(value)
    }
    return result
}

private fun <T> Iterable<T>.sumOf(selector: (T) -> BigDecimal): BigDecimal {
    var result = BigDecimal.ZERO
    for (item in this) {
        result = result.add(selector(item))
    }
    return result
}

