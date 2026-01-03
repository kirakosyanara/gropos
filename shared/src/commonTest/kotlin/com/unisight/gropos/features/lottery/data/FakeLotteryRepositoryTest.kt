package com.unisight.gropos.features.lottery.data

import com.unisight.gropos.features.lottery.domain.model.LotteryGameType
import com.unisight.gropos.features.lottery.domain.model.LotteryTransactionType
import com.unisight.gropos.features.lottery.domain.model.PayoutStatus
import com.unisight.gropos.features.lottery.domain.repository.LotteryGameNotFoundException
import com.unisight.gropos.features.lottery.domain.repository.LotteryPayoutRejectedException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for FakeLotteryRepository.
 * 
 * Per testing-strategy.mdc: Fakes should maintain state correctly.
 */
class FakeLotteryRepositoryTest {
    
    private val repository = FakeLotteryRepository()
    
    // ========================================================================
    // Game Catalog Tests
    // ========================================================================
    
    @Test
    fun `getActiveGames returns seeded games`() = runTest {
        val games = repository.getActiveGames().first()
        
        // Should have 10 seeded games
        assertEquals(10, games.size)
        
        // Verify scratchers
        val scratchers = games.filter { it.type == LotteryGameType.SCRATCHER }
        assertEquals(5, scratchers.size)
        
        // Verify draw games
        val drawGames = games.filter { it.type == LotteryGameType.DRAW }
        assertEquals(5, drawGames.size)
    }
    
    @Test
    fun `getGameById returns correct game`() = runTest {
        val game = repository.getGameById("scratch_001")
        
        assertNotNull(game)
        assertEquals("Lucky 7s", game.name)
        assertEquals(0, BigDecimal("1.00").compareTo(game.ticketPrice))
        assertEquals(LotteryGameType.SCRATCHER, game.type)
    }
    
    @Test
    fun `getGameById returns null for unknown game`() = runTest {
        val game = repository.getGameById("unknown_999")
        assertNull(game)
    }
    
    @Test
    fun `searchGames finds matching games`() = runTest {
        val results = repository.searchGames("power")
        
        assertEquals(1, results.size)
        assertEquals("Powerball", results[0].name)
    }
    
    @Test
    fun `searchGames returns empty for no match`() = runTest {
        val results = repository.searchGames("nonexistent")
        assertTrue(results.isEmpty())
    }
    
    // ========================================================================
    // Sales Tests
    // ========================================================================
    
    @Test
    fun `recordSale creates transaction`() = runTest {
        repository.clearTransactions()
        
        val result = repository.recordSale(
            gameId = "scratch_003",
            quantity = 2,
            staffId = 100
        )
        
        assertTrue(result.isSuccess)
        val transaction = result.getOrNull()!!
        
        assertEquals(LotteryTransactionType.SALE, transaction.type)
        assertEquals("scratch_003", transaction.gameId)
        assertEquals("$100M Cash Explosion", transaction.gameName)
        assertEquals(0, BigDecimal("10.00").compareTo(transaction.amount)) // 5.00 * 2
        assertEquals(2, transaction.quantity)
        assertEquals(100, transaction.staffId)
    }
    
    @Test
    fun `recordSale fails for unknown game`() = runTest {
        val result = repository.recordSale(
            gameId = "unknown_999",
            quantity = 1,
            staffId = 100
        )
        
        assertTrue(result.isFailure)
        assertIs<LotteryGameNotFoundException>(result.exceptionOrNull())
    }
    
    @Test
    fun `recordSale fails for zero quantity`() = runTest {
        val result = repository.recordSale(
            gameId = "scratch_001",
            quantity = 0,
            staffId = 100
        )
        
        assertTrue(result.isFailure)
        assertIs<IllegalArgumentException>(result.exceptionOrNull())
    }
    
    // ========================================================================
    // Payout Feasibility Tests
    // ========================================================================
    
    @Test
    fun `checkPayoutFeasibility returns APPROVED for $49_99`() {
        val status = repository.checkPayoutFeasibility(49.99)
        assertEquals(PayoutStatus.APPROVED, status)
    }
    
    @Test
    fun `checkPayoutFeasibility returns LOGGED_ONLY for $50_00`() {
        val status = repository.checkPayoutFeasibility(50.00)
        assertEquals(PayoutStatus.LOGGED_ONLY, status)
    }
    
    @Test
    fun `checkPayoutFeasibility returns LOGGED_ONLY for $599_99`() {
        val status = repository.checkPayoutFeasibility(599.99)
        assertEquals(PayoutStatus.LOGGED_ONLY, status)
    }
    
    @Test
    fun `checkPayoutFeasibility returns REJECTED for $600_00`() {
        val status = repository.checkPayoutFeasibility(600.00)
        assertEquals(PayoutStatus.REJECTED_OVER_LIMIT, status)
    }
    
    // ========================================================================
    // Payout Processing Tests
    // ========================================================================
    
    @Test
    fun `processPayout succeeds for Tier 1 amount`() = runTest {
        repository.clearTransactions()
        
        val result = repository.processPayout(
            amount = 25.00,
            gameId = "scratch_001",
            staffId = 100
        )
        
        assertTrue(result.isSuccess)
        val transaction = result.getOrNull()!!
        
        assertEquals(LotteryTransactionType.PAYOUT, transaction.type)
        assertEquals(PayoutStatus.APPROVED, transaction.payoutStatus)
    }
    
    @Test
    fun `processPayout succeeds for Tier 2 amount and logs`() = runTest {
        repository.clearTransactions()
        
        val result = repository.processPayout(
            amount = 100.00,
            gameId = "scratch_003",
            staffId = 100
        )
        
        assertTrue(result.isSuccess)
        val transaction = result.getOrNull()!!
        
        assertEquals(LotteryTransactionType.PAYOUT, transaction.type)
        assertEquals(PayoutStatus.LOGGED_ONLY, transaction.payoutStatus)
    }
    
    @Test
    fun `processPayout FAILS for Tier 3 amount - CRITICAL`() = runTest {
        val result = repository.processPayout(
            amount = 600.00,
            gameId = "scratch_005",
            staffId = 100
        )
        
        // MUST fail
        assertTrue(result.isFailure)
        assertIs<LotteryPayoutRejectedException>(result.exceptionOrNull())
    }
    
    @Test
    fun `processPayout FAILS for large jackpot - CRITICAL`() = runTest {
        val result = repository.processPayout(
            amount = 1_000_000.00,
            gameId = null,
            staffId = 100
        )
        
        // MUST fail
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertIs<LotteryPayoutRejectedException>(exception)
        assertTrue(exception.message!!.contains("600.00"))
    }
    
    @Test
    fun `processPayout works without gameId`() = runTest {
        repository.clearTransactions()
        
        val result = repository.processPayout(
            amount = 50.00,
            gameId = null,
            staffId = 100
        )
        
        assertTrue(result.isSuccess)
        val transaction = result.getOrNull()!!
        assertEquals("generic_payout", transaction.gameId)
        assertEquals("Lottery Payout", transaction.gameName)
    }
    
    // ========================================================================
    // Transaction History Tests
    // ========================================================================
    
    @Test
    fun `transactions are recorded and retrievable`() = runTest {
        repository.clearTransactions()
        
        // Create some transactions
        repository.recordSale("scratch_001", 2, 100)
        repository.recordSale("draw_001", 1, 100)
        repository.processPayout(25.00, "scratch_001", 100)
        
        val transactions = repository.getAllTransactions()
        assertEquals(3, transactions.size)
        
        val sales = transactions.filter { it.type == LotteryTransactionType.SALE }
        val payouts = transactions.filter { it.type == LotteryTransactionType.PAYOUT }
        
        assertEquals(2, sales.size)
        assertEquals(1, payouts.size)
    }
    
    @Test
    fun `getCurrentDaySummary calculates correctly`() = runTest {
        repository.clearTransactions()
        
        // Sales: $2.00 + $5.00 = $7.00
        repository.recordSale("scratch_001", 2, 100) // $1.00 * 2 = $2.00
        repository.recordSale("scratch_003", 1, 100) // $5.00 * 1 = $5.00
        
        // Payout: $20.00
        repository.processPayout(20.00, null, 100)
        
        val summary = repository.getCurrentDaySummary()
        
        // Use compareTo for BigDecimal (scale-independent comparison)
        assertEquals(0, BigDecimal("7.00").compareTo(summary.totalSales))
        assertEquals(0, BigDecimal("20.00").compareTo(summary.totalPayouts))
        assertEquals(0, BigDecimal("-13.00").compareTo(summary.netAmount)) // 7 - 20 = -13
        assertEquals(3, summary.transactionCount)
    }
}

