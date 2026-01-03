package com.unisight.gropos.features.lottery.presentation

import com.unisight.gropos.features.lottery.data.FakeLotteryRepository
import com.unisight.gropos.features.lottery.domain.model.LotteryGame
import com.unisight.gropos.features.lottery.domain.model.LotteryGameType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import java.math.BigDecimal
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * TDD Test Suite for LotterySaleViewModel.
 * 
 * **Per LOTTERY_SALES.md:**
 * - Display active lottery games (scratchers + draw)
 * - Add games to cart with quantity
 * - Calculate total due
 * - Process sale (cash only)
 * 
 * Per testing-strategy.mdc: Write tests BEFORE implementation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LotterySaleViewModelTest {
    
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeLotteryRepository
    private lateinit var viewModel: LotterySaleViewModel
    
    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeLotteryRepository()
        viewModel = LotterySaleViewModel(repository, staffId = 100)
    }
    
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    // ========================================================================
    // Initial State
    // ========================================================================
    
    @Test
    fun `initial state has empty cart`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        
        assertTrue(state.cart.isEmpty())
        assertEquals(BigDecimal.ZERO, state.totalDue)
    }
    
    @Test
    fun `initial state loads active games`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        
        assertTrue(state.activeGames.isNotEmpty())
        // FakeLotteryRepository has 10 seeded games
        assertEquals(10, state.activeGames.size)
    }
    
    @Test
    fun `games are grouped by type`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        
        val scratchers = state.activeGames.filter { it.type == LotteryGameType.SCRATCHER }
        val draws = state.activeGames.filter { it.type == LotteryGameType.DRAW }
        
        assertEquals(5, scratchers.size)
        assertEquals(5, draws.size)
    }
    
    // ========================================================================
    // Adding to Cart
    // ========================================================================
    
    @Test
    fun `addToCart adds game to cart`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        
        val games = viewModel.uiState.first().activeGames
        val game = games.first()
        
        viewModel.addToCart(game)
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        assertEquals(1, state.cart.size)
        assertEquals(game.id, state.cart.first().gameId)
    }
    
    @Test
    fun `addToCart with quantity adds multiple`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        
        val games = viewModel.uiState.first().activeGames
        val game = games.first()
        
        viewModel.addToCart(game, quantity = 3)
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        assertEquals(1, state.cart.size)
        assertEquals(3, state.cart.first().quantity)
    }
    
    @Test
    fun `addToCart same game increments quantity`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        
        val games = viewModel.uiState.first().activeGames
        val game = games.first()
        
        viewModel.addToCart(game, quantity = 2)
        viewModel.addToCart(game, quantity = 3)
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        assertEquals(1, state.cart.size)
        assertEquals(5, state.cart.first().quantity)
    }
    
    // ========================================================================
    // Total Calculation
    // ========================================================================
    
    @Test
    fun `total reflects cart items`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        
        val games = viewModel.uiState.first().activeGames
        // Get $2.00 Powerball
        val powerball = games.find { it.name == "Powerball" }!!
        
        viewModel.addToCart(powerball, quantity = 5)
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        assertEquals(BigDecimal("10.00"), state.totalDue)
    }
    
    @Test
    fun `total sums multiple games`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        
        val games = viewModel.uiState.first().activeGames
        val lucky7s = games.find { it.name == "Lucky 7s" }!! // $1.00
        val diamondJackpot = games.find { it.name == "Diamond Jackpot" }!! // $10.00
        
        viewModel.addToCart(lucky7s, quantity = 2) // $2.00
        viewModel.addToCart(diamondJackpot, quantity = 1) // $10.00
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        assertEquals(BigDecimal("12.00"), state.totalDue)
    }
    
    // ========================================================================
    // Remove from Cart
    // ========================================================================
    
    @Test
    fun `removeFromCart removes item`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        
        val games = viewModel.uiState.first().activeGames
        val game = games.first()
        
        viewModel.addToCart(game)
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.removeFromCart(game.id)
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        assertTrue(state.cart.isEmpty())
        assertEquals(BigDecimal.ZERO, state.totalDue)
    }
    
    @Test
    fun `clearCart removes all items`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        
        val games = viewModel.uiState.first().activeGames
        
        viewModel.addToCart(games[0])
        viewModel.addToCart(games[1])
        viewModel.addToCart(games[2])
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.clearCart()
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        assertTrue(state.cart.isEmpty())
        assertEquals(BigDecimal.ZERO, state.totalDue)
    }
    
    // ========================================================================
    // Process Sale
    // ========================================================================
    
    @Test
    fun `processSale records transactions`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        
        val games = viewModel.uiState.first().activeGames
        val powerball = games.find { it.name == "Powerball" }!!
        
        viewModel.addToCart(powerball, quantity = 3)
        testDispatcher.scheduler.advanceUntilIdle()
        
        val result = viewModel.processSale()
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertTrue(result.isSuccess)
        
        // Cart should be cleared after sale
        val state = viewModel.uiState.first()
        assertTrue(state.cart.isEmpty())
    }
    
    @Test
    fun `processSale with empty cart fails`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        
        val result = viewModel.processSale()
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertTrue(result.isFailure)
    }
    
    // ========================================================================
    // Update Quantity
    // ========================================================================
    
    @Test
    fun `updateQuantity modifies item quantity`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        
        val games = viewModel.uiState.first().activeGames
        val game = games.first()
        
        viewModel.addToCart(game, quantity = 2)
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.updateQuantity(game.id, newQuantity = 5)
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        assertEquals(5, state.cart.first().quantity)
    }
    
    @Test
    fun `updateQuantity to zero removes item`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        
        val games = viewModel.uiState.first().activeGames
        val game = games.first()
        
        viewModel.addToCart(game, quantity = 2)
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.updateQuantity(game.id, newQuantity = 0)
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        assertTrue(state.cart.isEmpty())
    }
    
    // ========================================================================
    // Search/Filter
    // ========================================================================
    
    @Test
    fun `filterByType shows only selected type`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.filterByType(LotteryGameType.SCRATCHER)
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        assertTrue(state.filteredGames.all { it.type == LotteryGameType.SCRATCHER })
    }
    
    @Test
    fun `clearFilter shows all games`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.filterByType(LotteryGameType.SCRATCHER)
        viewModel.clearFilter()
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        assertEquals(10, state.filteredGames.size)
    }
}

