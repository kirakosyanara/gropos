package com.unisight.gropos.features.lottery.presentation

import com.unisight.gropos.features.cashier.data.FakeTillRepository
import com.unisight.gropos.features.cashier.domain.service.CashierSessionManager
import com.unisight.gropos.features.lottery.data.FakeLotteryRepository
import com.unisight.gropos.features.lottery.domain.model.PayoutStatus
import com.unisight.gropos.features.lottery.domain.service.PayoutTierCalculator
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
 * TDD Test Suite for LotteryPayoutViewModel.
 * 
 * **Per LOTTERY_PAYOUTS.md:**
 * - Tier 1 ($0-$49.99): APPROVED - Process immediately
 * - Tier 2 ($50-$599.99): LOGGED_ONLY - Process and log
 * - Tier 3 ($600+): REJECTED_OVER_LIMIT - Cannot process
 * 
 * Per testing-strategy.mdc: Write tests BEFORE implementation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LotteryPayoutViewModelTest {
    
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeLotteryRepository
    private lateinit var sessionManager: CashierSessionManager
    private lateinit var viewModel: LotteryPayoutViewModel
    
    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeLotteryRepository()
        sessionManager = CashierSessionManager(FakeTillRepository())
        // Start a mock session with employeeId = 100
        sessionManager.startSession(
            employeeId = 100,
            employeeName = "Test Cashier",
            tillId = 1,
            tillName = "Till 1"
        )
        viewModel = LotteryPayoutViewModel(repository, sessionManager)
    }
    
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    // ========================================================================
    // Initial State
    // ========================================================================
    
    @Test
    fun `initial state has zero amount`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        
        assertEquals(BigDecimal.ZERO, state.amount)
        assertEquals("0", state.displayAmount)
    }
    
    @Test
    fun `initial state has no validation result`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        
        assertEquals(null, state.validationResult)
        assertFalse(state.canProcess)
    }
    
    // ========================================================================
    // Amount Entry
    // ========================================================================
    
    @Test
    fun `digit entry updates display amount`() = runTest {
        viewModel.onDigitPress("5")
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        assertEquals("5", state.displayAmount)
    }
    
    @Test
    fun `multiple digits build up amount`() = runTest {
        viewModel.onDigitPress("4")
        viewModel.onDigitPress("0")
        viewModel.onDigitPress("0")
        viewModel.onDigitPress("0") // 40.00 (cents input)
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        // Display should show amount in dollars
        assertEquals(BigDecimal("40.00"), state.amount)
    }
    
    @Test
    fun `clear resets amount to zero`() = runTest {
        viewModel.onDigitPress("5")
        viewModel.onDigitPress("0")
        viewModel.onClear()
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        assertEquals(0, state.amount.compareTo(BigDecimal.ZERO))
    }
    
    @Test
    fun `backspace removes last digit`() = runTest {
        viewModel.onDigitPress("1")
        viewModel.onDigitPress("2")
        viewModel.onDigitPress("3")
        viewModel.onBackspace()
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        assertEquals("12", state.displayAmount)
    }
    
    // ========================================================================
    // Tier Validation
    // ========================================================================
    
    @Test
    fun `$40 shows Tier 1 APPROVED`() = runTest {
        // Enter $40.00 as cents: 4000
        viewModel.onDigitPress("4")
        viewModel.onDigitPress("0")
        viewModel.onDigitPress("0")
        viewModel.onDigitPress("0")
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        
        assertEquals(PayoutStatus.APPROVED, state.validationResult?.status)
        assertTrue(state.canProcess)
    }
    
    @Test
    fun `$100 shows Tier 2 LOGGED_ONLY`() = runTest {
        // Enter $100.00 as cents: 10000
        viewModel.onDigitPress("1")
        viewModel.onDigitPress("0")
        viewModel.onDigitPress("0")
        viewModel.onDigitPress("0")
        viewModel.onDigitPress("0")
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        
        assertEquals(PayoutStatus.LOGGED_ONLY, state.validationResult?.status)
        assertTrue(state.canProcess)
    }
    
    @Test
    fun `$600 shows Tier 3 REJECTED`() = runTest {
        // Enter $600.00 as cents: 60000
        viewModel.onDigitPress("6")
        viewModel.onDigitPress("0")
        viewModel.onDigitPress("0")
        viewModel.onDigitPress("0")
        viewModel.onDigitPress("0")
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        
        assertEquals(PayoutStatus.REJECTED_OVER_LIMIT, state.validationResult?.status)
        assertFalse(state.canProcess)
    }
    
    @Test
    fun `$1000 shows Tier 3 REJECTED`() = runTest {
        // Enter $1000.00 as cents: 100000
        viewModel.onDigitPress("1")
        viewModel.onDigitPress("0")
        viewModel.onDigitPress("0")
        viewModel.onDigitPress("0")
        viewModel.onDigitPress("0")
        viewModel.onDigitPress("0")
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        
        assertEquals(PayoutStatus.REJECTED_OVER_LIMIT, state.validationResult?.status)
        assertFalse(state.canProcess)
    }
    
    // ========================================================================
    // Process Payout
    // ========================================================================
    
    @Test
    fun `processPayout succeeds for Tier 1`() = runTest {
        // Enter $25.00
        viewModel.onDigitPress("2")
        viewModel.onDigitPress("5")
        viewModel.onDigitPress("0")
        viewModel.onDigitPress("0")
        testDispatcher.scheduler.advanceUntilIdle()
        
        val result = viewModel.processPayout()
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertTrue(result.isSuccess)
        
        // Amount should be reset after success
        val state = viewModel.uiState.first()
        assertEquals(BigDecimal.ZERO, state.amount)
    }
    
    @Test
    fun `processPayout succeeds for Tier 2`() = runTest {
        // Enter $200.00
        viewModel.onDigitPress("2")
        viewModel.onDigitPress("0")
        viewModel.onDigitPress("0")
        viewModel.onDigitPress("0")
        viewModel.onDigitPress("0")
        testDispatcher.scheduler.advanceUntilIdle()
        
        val result = viewModel.processPayout()
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun `processPayout fails for Tier 3`() = runTest {
        // Enter $600.00
        viewModel.onDigitPress("6")
        viewModel.onDigitPress("0")
        viewModel.onDigitPress("0")
        viewModel.onDigitPress("0")
        viewModel.onDigitPress("0")
        testDispatcher.scheduler.advanceUntilIdle()
        
        val result = viewModel.processPayout()
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertTrue(result.isFailure)
    }
    
    @Test
    fun `processPayout with zero amount fails`() = runTest {
        val result = viewModel.processPayout()
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertTrue(result.isFailure)
    }
    
    // ========================================================================
    // Tier Badge Display
    // ========================================================================
    
    @Test
    fun `tier badge shows correct tier number`() = runTest {
        // Tier 1
        viewModel.onDigitPress("1")
        viewModel.onDigitPress("0")
        viewModel.onDigitPress("0")
        viewModel.onDigitPress("0") // $10.00
        testDispatcher.scheduler.advanceUntilIdle()
        
        var state = viewModel.uiState.first()
        assertEquals(1, state.tierNumber)
        
        // Add more to reach Tier 2
        viewModel.onClear()
        viewModel.onDigitPress("7")
        viewModel.onDigitPress("5")
        viewModel.onDigitPress("0")
        viewModel.onDigitPress("0") // $75.00
        testDispatcher.scheduler.advanceUntilIdle()
        
        state = viewModel.uiState.first()
        assertEquals(2, state.tierNumber)
    }
}


