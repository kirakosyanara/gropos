package com.unisight.gropos.features.payment.presentation

import com.unisight.gropos.core.util.UsdCurrencyFormatter
import com.unisight.gropos.features.checkout.domain.model.Cart
import com.unisight.gropos.features.checkout.domain.model.CartItem
import com.unisight.gropos.features.checkout.domain.model.Product
import com.unisight.gropos.features.checkout.domain.repository.CartRepository
import com.unisight.gropos.features.payment.domain.terminal.PaymentResult
import com.unisight.gropos.features.payment.domain.terminal.PaymentTerminal
import com.unisight.gropos.features.payment.domain.terminal.VoidResult
import com.unisight.gropos.features.returns.domain.service.PullbackItemForCreate
import com.unisight.gropos.features.transaction.domain.model.HeldTransaction
import com.unisight.gropos.features.transaction.domain.model.Transaction
import com.unisight.gropos.features.transaction.domain.repository.TransactionRepository
import com.unisight.gropos.features.transaction.domain.repository.TransactionSearchCriteria
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import java.math.BigDecimal
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for PaymentViewModel.
 * 
 * **Per QA Audit Finding (CRITICAL):**
 * PaymentViewModel had ZERO test coverage. This is a critical path for:
 * - Cash handling (money reconciliation)
 * - Card payment processing (PCI compliance)
 * - Split tender (complex state management)
 * - Transaction persistence (data integrity)
 * 
 * Per testing-strategy.mdc:
 * - Test all payment types: Cash, Credit, Debit, EBT
 * - Test split tender scenarios
 * - Test terminal failures (declined, error, cancel)
 * - Test transaction save failures
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PaymentViewModelTest {
    
    /**
     * Test dispatcher for controlled coroutine execution.
     * 
     * **Fix for UncompletedCoroutinesError:**
     * - StandardTestDispatcher gives us control over time advancement
     * - Setting it as Main dispatcher ensures ViewModel coroutines use our dispatcher
     */
    private lateinit var testDispatcher: TestDispatcher
    
    /**
     * Cancellable scope for ViewModel.
     * 
     * **Key Fix:** The ViewModel's `observeCartChanges()` launches an infinite Flow collection.
     * We must cancel this scope explicitly after each test to avoid UncompletedCoroutinesError.
     */
    private lateinit var viewModelScope: CoroutineScope
    
    @BeforeTest
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
    }
    
    @AfterTest
    fun tearDown() {
        // Cancel the ViewModel scope to stop any lingering coroutines
        if (::viewModelScope.isInitialized) {
            viewModelScope.cancel()
        }
        Dispatchers.resetMain()
    }
    
    private fun createViewModel(
        testScope: TestScope,
        cart: Cart = createTestCart(),
        terminal: PaymentTerminal = FakePaymentTerminal(),
        transactionRepo: TransactionRepository = FakeTransactionRepository()
    ): PaymentViewModel {
        val cartRepository = FakeCartRepository().also { it.setCart(cart) }
        
        // Create a cancellable scope that uses the test dispatcher
        viewModelScope = CoroutineScope(testDispatcher + Job())
        
        return PaymentViewModel(
            cartRepository = cartRepository,
            currencyFormatter = UsdCurrencyFormatter(),
            transactionRepository = transactionRepo,
            paymentTerminal = terminal,
            scope = viewModelScope
        )
    }
    
    // ========================================================================
    // Initial State Tests
    // ========================================================================
    
    @Test
    fun `initial state shows cart total as remaining amount`() = runTest(testDispatcher) {
        val cart = createTestCart(price = BigDecimal("25.00"))
        val viewModel = createViewModel(this, cart)
        
        advanceUntilIdle()
        
        assertEquals(BigDecimal("25.00").setScale(2), viewModel.state.value.remainingAmountRaw.setScale(2))
    }
    
    @Test
    fun `initial state shows empty cart flag when cart is empty`() = runTest(testDispatcher) {
        val cart = Cart.empty()
        val viewModel = createViewModel(this, cart)
        
        advanceUntilIdle()
        
        assertTrue(viewModel.state.value.isCartEmpty)
    }
    
    // ========================================================================
    // Cash Payment Tests
    // ========================================================================
    
    @Test
    fun `onCashExactChange processes exact amount payment`() = runTest(testDispatcher) {
        val cart = createTestCart(price = BigDecimal("10.00"))
        val transactionRepo = FakeTransactionRepository()
        val viewModel = createViewModel(this, cart, transactionRepo = transactionRepo)
        
        advanceUntilIdle()
        
        viewModel.onCashExactChange()
        advanceUntilIdle()
        
        assertTrue(viewModel.state.value.isComplete)
        assertEquals(1, transactionRepo.savedTransactions.size)
    }
    
    @Test
    fun `cash payment over amount shows change dialog`() = runTest(testDispatcher) {
        val cart = createTestCart(price = BigDecimal("8.50"))
        val viewModel = createViewModel(this, cart)
        
        advanceUntilIdle()
        
        viewModel.onCashQuickAmount(BigDecimal("10.00"))
        advanceUntilIdle()
        
        assertTrue(viewModel.state.value.showChangeDialog)
        assertEquals("$1.50", viewModel.state.value.changeAmount)
    }
    
    @Test
    fun `cash payment with entered amount works`() = runTest(testDispatcher) {
        val cart = createTestCart(price = BigDecimal("15.00"))
        val viewModel = createViewModel(this, cart)
        
        advanceUntilIdle()
        
        viewModel.onDigitPress("2")
        viewModel.onDigitPress("0")
        viewModel.onCashEnteredAmount()
        advanceUntilIdle()
        
        assertTrue(viewModel.state.value.isComplete)
        assertTrue(viewModel.state.value.showChangeDialog)
        assertEquals("$5.00", viewModel.state.value.changeAmount)
    }
    
    @Test
    fun `cash payment with no amount due shows error`() = runTest(testDispatcher) {
        val cart = Cart.empty()
        val viewModel = createViewModel(this, cart)
        
        advanceUntilIdle()
        
        viewModel.onCashExactChange()
        advanceUntilIdle()
        
        assertEquals("No amount due", viewModel.state.value.errorMessage)
        assertFalse(viewModel.state.value.isComplete)
    }
    
    // ========================================================================
    // Split Tender Tests (CRITICAL)
    // ========================================================================
    
    @Test
    fun `split tender with cash then card completes transaction`() = runTest(testDispatcher) {
        val cart = createTestCart(price = BigDecimal("50.00"))
        val terminal = FakePaymentTerminal()
        val transactionRepo = FakeTransactionRepository()
        val viewModel = createViewModel(this, cart, terminal, transactionRepo)
        
        advanceUntilIdle()
        
        // Pay $20 cash first
        viewModel.onCashQuickAmount(BigDecimal("20.00"))
        advanceUntilIdle()
        
        assertFalse(viewModel.state.value.isComplete)
        assertEquals(BigDecimal("30.00").setScale(2), viewModel.state.value.remainingAmountRaw.setScale(2))
        assertEquals(1, viewModel.state.value.appliedPayments.size)
        
        // Pay remaining $30 with card
        viewModel.onCreditPayment()
        advanceUntilIdle()
        
        assertTrue(viewModel.state.value.isComplete)
        assertEquals(2, viewModel.state.value.appliedPayments.size)
        assertEquals(1, transactionRepo.savedTransactions.size)
    }
    
    @Test
    fun `split tender with three payments tracks all correctly`() = runTest(testDispatcher) {
        val cart = createTestCart(price = BigDecimal("100.00"))
        val terminal = FakePaymentTerminal()
        val transactionRepo = FakeTransactionRepository()
        val viewModel = createViewModel(this, cart, terminal, transactionRepo)
        
        advanceUntilIdle()
        
        // Payment 1: $25 cash
        viewModel.onCashQuickAmount(BigDecimal("25.00"))
        advanceUntilIdle()
        assertEquals(BigDecimal("75.00").setScale(2), viewModel.state.value.remainingAmountRaw.setScale(2))
        
        // Payment 2: $50 credit
        viewModel.onDigitPress("5")
        viewModel.onDigitPress("0")
        viewModel.onCreditPayment()
        advanceUntilIdle()
        assertEquals(BigDecimal("25.00").setScale(2), viewModel.state.value.remainingAmountRaw.setScale(2))
        
        // Payment 3: $25 debit
        viewModel.onDebitPayment()
        advanceUntilIdle()
        
        assertTrue(viewModel.state.value.isComplete)
        assertEquals(3, viewModel.state.value.appliedPayments.size)
    }
    
    // ========================================================================
    // Card Payment Tests
    // ========================================================================
    
    @Test
    fun `credit payment approved completes transaction`() = runTest(testDispatcher) {
        val cart = createTestCart(price = BigDecimal("25.00"))
        val terminal = FakePaymentTerminal()
        val transactionRepo = FakeTransactionRepository()
        val viewModel = createViewModel(this, cart, terminal, transactionRepo)
        
        advanceUntilIdle()
        
        viewModel.onCreditPayment()
        advanceUntilIdle()
        
        assertTrue(viewModel.state.value.isComplete)
        assertEquals(1, transactionRepo.savedTransactions.size)
        assertFalse(viewModel.state.value.showTerminalDialog)
    }
    
    @Test
    fun `card payment shows terminal dialog while processing`() = runTest(testDispatcher) {
        val cart = createTestCart(price = BigDecimal("25.00"))
        val terminal = FakePaymentTerminal(processingDelay = 500)
        val viewModel = createViewModel(this, cart, terminal)
        
        advanceUntilIdle()
        
        viewModel.onCreditPayment()
        // Don't advance until idle - check intermediate state
        
        assertTrue(viewModel.state.value.showTerminalDialog)
        assertEquals("$25.00", viewModel.state.value.terminalDialogAmount)
    }
    
    @Test
    fun `card declined shows error and stays on payment screen`() = runTest(testDispatcher) {
        val cart = createTestCart(price = BigDecimal("25.00"))
        val terminal = FakePaymentTerminal()
        terminal.simulateDecline = true
        val viewModel = createViewModel(this, cart, terminal)
        
        advanceUntilIdle()
        
        viewModel.onCreditPayment()
        advanceUntilIdle()
        
        assertFalse(viewModel.state.value.isComplete)
        assertTrue(viewModel.state.value.errorMessage?.contains("Declined") == true)
        assertEquals(0, viewModel.state.value.appliedPayments.size)
    }
    
    @Test
    fun `terminal error shows error message`() = runTest(testDispatcher) {
        val cart = createTestCart(price = BigDecimal("25.00"))
        val terminal = FakePaymentTerminal()
        terminal.simulateError = true
        val viewModel = createViewModel(this, cart, terminal)
        
        advanceUntilIdle()
        
        viewModel.onCreditPayment()
        advanceUntilIdle()
        
        assertFalse(viewModel.state.value.isComplete)
        assertTrue(viewModel.state.value.errorMessage?.contains("Terminal Error") == true)
    }
    
    @Test
    fun `cancelled payment closes dialog without error`() = runTest(testDispatcher) {
        val cart = createTestCart(price = BigDecimal("25.00"))
        val terminal = FakePaymentTerminal()
        terminal.simulateCancel = true
        val viewModel = createViewModel(this, cart, terminal)
        
        advanceUntilIdle()
        
        viewModel.onCreditPayment()
        advanceUntilIdle()
        
        assertFalse(viewModel.state.value.isComplete)
        assertNull(viewModel.state.value.errorMessage)
        assertFalse(viewModel.state.value.showTerminalDialog)
    }
    
    // ========================================================================
    // Transaction Save Failure Tests
    // ========================================================================
    
    @Test
    fun `transaction save failure shows error message`() = runTest(testDispatcher) {
        val cart = createTestCart(price = BigDecimal("10.00"))
        val transactionRepo = FakeTransactionRepository()
        transactionRepo.simulateSaveFailure = true
        val viewModel = createViewModel(this, cart, transactionRepo = transactionRepo)
        
        advanceUntilIdle()
        
        viewModel.onCashExactChange()
        advanceUntilIdle()
        
        assertFalse(viewModel.state.value.isComplete)
        assertTrue(viewModel.state.value.errorMessage?.contains("Failed to save") == true)
    }
    
    // ========================================================================
    // TenKey Input Tests
    // ========================================================================
    
    @Test
    fun `digit press appends to entered amount`() = runTest(testDispatcher) {
        val viewModel = createViewModel(this)
        
        advanceUntilIdle()
        
        viewModel.onDigitPress("1")
        viewModel.onDigitPress("2")
        viewModel.onDigitPress("3")
        
        assertEquals("123", viewModel.state.value.enteredAmount)
    }
    
    @Test
    fun `clear press clears entered amount`() = runTest(testDispatcher) {
        val viewModel = createViewModel(this)
        
        advanceUntilIdle()
        
        viewModel.onDigitPress("1")
        viewModel.onDigitPress("2")
        viewModel.onClearPress()
        
        assertEquals("", viewModel.state.value.enteredAmount)
    }
    
    @Test
    fun `backspace removes last digit`() = runTest(testDispatcher) {
        val viewModel = createViewModel(this)
        
        advanceUntilIdle()
        
        viewModel.onDigitPress("1")
        viewModel.onDigitPress("2")
        viewModel.onDigitPress("3")
        viewModel.onBackspacePress()
        
        assertEquals("12", viewModel.state.value.enteredAmount)
    }
    
    // ========================================================================
    // EBT Payment Tests
    // ========================================================================
    
    @Test
    fun `EBT SNAP payment works for SNAP eligible amount`() = runTest(testDispatcher) {
        val cart = createTestCart(
            price = BigDecimal("30.00"),
            snapEligible = true
        )
        val terminal = FakePaymentTerminal()
        val transactionRepo = FakeTransactionRepository()
        val viewModel = createViewModel(this, cart, terminal, transactionRepo)
        
        advanceUntilIdle()
        
        viewModel.onEbtSnapPayment()
        advanceUntilIdle()
        
        assertTrue(viewModel.state.value.isComplete)
    }
    
    // ========================================================================
    // Helpers
    // ========================================================================
    
    private fun createTestCart(
        price: BigDecimal = BigDecimal("25.00"),
        snapEligible: Boolean = false
    ): Cart {
        if (price == BigDecimal.ZERO) {
            return Cart.empty()
        }
        
        val product = Product(
            branchProductId = 1001,
            productId = 1001,
            productName = "Test Product",
            retailPrice = price,
            isSnapEligible = snapEligible
        )
        
        val item = CartItem(
            product = product,
            quantityUsed = BigDecimal.ONE
        )
        
        return Cart(items = listOf(item))
    }
}

// ============================================================================
// Fake Implementations
// ============================================================================

class FakeCartRepository : CartRepository {
    private val _cart = MutableStateFlow(Cart.empty())
    override val cart: StateFlow<Cart> = _cart
    
    fun setCart(cart: Cart) {
        _cart.value = cart
    }
    
    override suspend fun addToCart(product: Product, quantity: BigDecimal) {
        // Not implemented for payment tests
    }
    
    override suspend fun removeFromCart(branchProductId: Int) {
        // Not implemented for payment tests
    }
    
    override suspend fun voidItem(branchProductId: Int) {
        // Not implemented for payment tests
    }
    
    override suspend fun updateQuantity(branchProductId: Int, newQuantity: BigDecimal) {
        // Not implemented for payment tests
    }
    
    override suspend fun clearCart() {
        _cart.value = Cart.empty()
    }
    
    override fun getCurrentCart(): Cart = _cart.value
    
    override suspend fun applyTransactionDiscount(discountPercent: BigDecimal) {
        // Not implemented for payment tests
    }
}

class FakePaymentTerminal(
    private val processingDelay: Long = 0
) : PaymentTerminal {
    var simulateDecline = false
    var simulateError = false
    var simulateCancel = false
    var lastProcessedAmount: BigDecimal? = null
    
    override suspend fun processPayment(amount: BigDecimal): PaymentResult {
        lastProcessedAmount = amount
        
        if (processingDelay > 0) {
            delay(processingDelay)
        }
        
        return when {
            simulateDecline -> PaymentResult.Declined("Insufficient funds")
            simulateError -> PaymentResult.Error("Terminal communication failed")
            simulateCancel -> PaymentResult.Cancelled
            else -> PaymentResult.Approved(
                transactionId = "TXN-${System.currentTimeMillis()}",
                authCode = "AUTH123",
                cardType = "VISA",
                lastFour = "4242"
            )
        }
    }
    
    override suspend fun cancelTransaction() {
        // Cancelled
    }
    
    override suspend fun processVoid(transactionId: String, amount: BigDecimal): VoidResult {
        return VoidResult.Success(transactionId, "VOID123")
    }
}

class FakeTransactionRepository : TransactionRepository {
    val savedTransactions = mutableListOf<Transaction>()
    var simulateSaveFailure = false
    
    override suspend fun saveTransaction(transaction: Transaction): Result<Unit> {
        return if (simulateSaveFailure) {
            Result.failure(Exception("Database error"))
        } else {
            savedTransactions.add(transaction)
            Result.success(Unit)
        }
    }
    
    override suspend fun getById(id: Long): Transaction? {
        return savedTransactions.find { it.id == id }
    }
    
    override suspend fun getRecent(limit: Int): List<Transaction> {
        return savedTransactions.takeLast(limit)
    }
    
    override suspend fun getPending(): List<Transaction> {
        return emptyList()
    }
    
    override suspend fun holdTransaction(heldTransaction: HeldTransaction): Result<Unit> {
        return Result.success(Unit)
    }
    
    override suspend fun getHeldTransactions(): List<HeldTransaction> {
        return emptyList()
    }
    
    override suspend fun getHeldTransactionById(id: String): HeldTransaction? {
        return null
    }
    
    override suspend fun deleteHeldTransaction(id: String): Result<Unit> {
        return Result.success(Unit)
    }
    
    override suspend fun searchTransactions(criteria: TransactionSearchCriteria): List<Transaction> {
        return emptyList()
    }
    
    override suspend fun findByGuid(guid: String): Transaction? {
        return null
    }
    
    override suspend fun getReturnedQuantities(transactionId: Long): Map<Long, BigDecimal> {
        return emptyMap()
    }
    
    override suspend fun createPullbackTransaction(
        originalTransactionId: Long,
        items: List<PullbackItemForCreate>,
        totalValue: BigDecimal
    ): Long {
        return 0L
    }
}
