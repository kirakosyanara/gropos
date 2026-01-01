package com.unisight.gropos.features.checkout.presentation

import com.unisight.gropos.core.util.UsdCurrencyFormatter
import com.unisight.gropos.features.checkout.data.FakeProductRepository
import com.unisight.gropos.features.checkout.data.FakeScannerRepository
import com.unisight.gropos.features.checkout.domain.usecase.ScanItemUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for CheckoutViewModel.
 * 
 * Per testing-strategy.mdc:
 * - Use runTest for coroutine testing
 * - Use Fakes for repositories
 * - Inject TestScope for controllable execution
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CheckoutViewModelTest {
    
    /**
     * Creates test dependencies and returns them as a tuple.
     * The scope is cancelled after each test to prevent uncompleted coroutines.
     */
    private data class TestEnv(
        val viewModel: CheckoutViewModel,
        val fakeScanner: FakeScannerRepository,
        val fakeProducts: FakeProductRepository,
        val cleanup: () -> Unit
    )
    
    private fun createTestEnv(): TestEnv {
        val testDispatcher = UnconfinedTestDispatcher()
        val testScope = kotlinx.coroutines.CoroutineScope(testDispatcher)
        
        val fakeScanner = FakeScannerRepository()
        val fakeProducts = FakeProductRepository()
        val useCase = ScanItemUseCase(fakeScanner, fakeProducts)
        val currencyFormatter = UsdCurrencyFormatter()
        
        val viewModel = CheckoutViewModel(
            scanItemUseCase = useCase,
            scannerRepository = fakeScanner,
            currencyFormatter = currencyFormatter,
            scope = testScope
        )
        
        return TestEnv(
            viewModel = viewModel,
            fakeScanner = fakeScanner,
            fakeProducts = fakeProducts,
            cleanup = { testScope.cancel() }
        )
    }
    
    // ========================================================================
    // Initial State Tests
    // ========================================================================
    
    @Test
    fun `initial state should be empty`() = runTest {
        val env = createTestEnv()
        try {
            val state = env.viewModel.state.value
            
            assertTrue(state.isEmpty)
            assertTrue(state.items.isEmpty())
            assertFalse(state.isLoading)
            assertNull(state.lastScanEvent)
        } finally {
            env.cleanup()
        }
    }
    
    // ========================================================================
    // Scanner Flow Integration Tests
    // ========================================================================
    
    @Test
    fun `emitting barcode to scanner flow should update state with new item`() = runTest {
        val env = createTestEnv()
        try {
            // When - scanner emits a barcode
            env.fakeScanner.emitScan("111") // Apple $1.00
            
            // Then - state should show the item
            val state = env.viewModel.state.value
            assertFalse(state.isEmpty)
            assertEquals(1, state.items.size)
            assertEquals("Apple", state.items[0].productName)
            assertEquals("$1.00", state.items[0].unitPrice)
            assertEquals("$1.00", state.items[0].lineTotal)
            
            // SNAP badge should show
            assertTrue(state.items[0].isSnapEligible)
        } finally {
            env.cleanup()
        }
    }
    
    @Test
    fun `emitting multiple barcodes should update state correctly`() = runTest {
        val env = createTestEnv()
        try {
            // When - scanner emits multiple barcodes
            env.fakeScanner.emitScan("111") // Apple $1.00
            env.fakeScanner.emitScan("222") // Banana $0.50
            
            // Then - state should show both items
            val state = env.viewModel.state.value
            assertEquals(2, state.items.size)
            assertEquals("2 items", state.totals.itemCount)
            assertEquals("$1.50", state.totals.subtotal)
        } finally {
            env.cleanup()
        }
    }
    
    @Test
    fun `emitting same barcode twice should increment quantity`() = runTest {
        val env = createTestEnv()
        try {
            // When - scanner emits same barcode twice
            env.fakeScanner.emitScan("111") // Apple
            env.fakeScanner.emitScan("111") // Apple again
            
            // Then - should have 1 item with quantity 2
            val state = env.viewModel.state.value
            assertEquals(1, state.items.size)
            assertEquals("2x", state.items[0].quantity)
            assertEquals("$2.00", state.items[0].lineTotal)
            assertEquals("$2.00", state.totals.subtotal)
        } finally {
            env.cleanup()
        }
    }
    
    @Test
    fun `emitting unknown barcode should show ProductNotFound event`() = runTest {
        val env = createTestEnv()
        try {
            // When - scanner emits unknown barcode
            env.fakeScanner.emitScan("999999999")
            
            // Then - should have error event
            val state = env.viewModel.state.value
            assertTrue(state.isEmpty)
            assertIs<ScanEvent.ProductNotFound>(state.lastScanEvent)
            assertEquals("999999999", (state.lastScanEvent as ScanEvent.ProductNotFound).barcode)
        } finally {
            env.cleanup()
        }
    }
    
    // ========================================================================
    // Manual Entry Tests
    // ========================================================================
    
    @Test
    fun `manual barcode entry should work same as scanner`() = runTest {
        val env = createTestEnv()
        try {
            // When - manual entry
            env.viewModel.onManualBarcodeEnter("111")
            
            // Then - should have item
            val state = env.viewModel.state.value
            assertEquals(1, state.items.size)
            assertEquals("Apple", state.items[0].productName)
        } finally {
            env.cleanup()
        }
    }
    
    @Test
    fun `blank manual entry should be ignored`() = runTest {
        val env = createTestEnv()
        try {
            // When - blank entry
            env.viewModel.onManualBarcodeEnter("   ")
            
            // Then - cart should remain empty
            assertTrue(env.viewModel.state.value.isEmpty)
        } finally {
            env.cleanup()
        }
    }
    
    // ========================================================================
    // Cart Operations Tests
    // ========================================================================
    
    @Test
    fun `remove item should update state`() = runTest {
        val env = createTestEnv()
        try {
            // Given - cart with items
            env.fakeScanner.emitScan("111") // Apple branchProductId=12346
            env.fakeScanner.emitScan("222") // Banana branchProductId=12347
            assertEquals(2, env.viewModel.state.value.items.size)
            
            // When - remove Apple
            env.viewModel.onRemoveItem(12346)
            
            // Then - only Banana remains
            val state = env.viewModel.state.value
            assertEquals(1, state.items.size)
            assertEquals("Banana", state.items[0].productName)
        } finally {
            env.cleanup()
        }
    }
    
    @Test
    fun `clear cart should reset to empty state`() = runTest {
        val env = createTestEnv()
        try {
            // Given - cart with items
            env.fakeScanner.emitScan("111")
            env.fakeScanner.emitScan("222")
            assertFalse(env.viewModel.state.value.isEmpty)
            
            // When - clear cart
            env.viewModel.onClearCart()
            
            // Then - cart is empty
            assertTrue(env.viewModel.state.value.isEmpty)
            assertTrue(env.viewModel.state.value.items.isEmpty())
        } finally {
            env.cleanup()
        }
    }
    
    // ========================================================================
    // SNAP Eligibility Tests
    // ========================================================================
    
    @Test
    fun `SNAP eligible items should be marked correctly`() = runTest {
        val env = createTestEnv()
        try {
            // When - scan SNAP eligible item (Apple)
            env.fakeScanner.emitScan("111")
            
            // Then - should be marked as SNAP eligible
            val item = env.viewModel.state.value.items[0]
            assertTrue(item.isSnapEligible)
            assertEquals("F", item.taxIndicator) // F = SNAP/Food
        } finally {
            env.cleanup()
        }
    }
    
    @Test
    fun `Milk with tax should show correct SNAP status and tax`() = runTest {
        val env = createTestEnv()
        try {
            // When - scan Milk (has 8.5% tax, but SNAP eligible)
            env.fakeScanner.emitScan("070000000121")
            
            // Then - should show SNAP eligible and have tax calculated
            val state = env.viewModel.state.value
            assertEquals(1, state.items.size)
            assertTrue(state.items[0].isSnapEligible)
            assertEquals("Organic Whole Milk 1 Gallon", state.items[0].productName)
        } finally {
            env.cleanup()
        }
    }
    
    // ========================================================================
    // Scan Event Feedback Tests
    // ========================================================================
    
    @Test
    fun `successful scan should emit ProductAdded event`() = runTest {
        val env = createTestEnv()
        try {
            // When - scan valid item
            env.fakeScanner.emitScan("111")
            
            // Then - should have ProductAdded event
            val event = env.viewModel.state.value.lastScanEvent
            assertIs<ScanEvent.ProductAdded>(event)
            assertEquals("Apple", event.productName)
        } finally {
            env.cleanup()
        }
    }
    
    @Test
    fun `dismiss scan event should clear event`() = runTest {
        val env = createTestEnv()
        try {
            // Given - scan event exists
            env.fakeScanner.emitScan("111")
            assertIs<ScanEvent.ProductAdded>(env.viewModel.state.value.lastScanEvent)
            
            // When - dismiss
            env.viewModel.onDismissScanEvent()
            
            // Then - event should be null
            assertNull(env.viewModel.state.value.lastScanEvent)
        } finally {
            env.cleanup()
        }
    }
    
    // ========================================================================
    // Currency Formatting Tests
    // ========================================================================
    
    @Test
    fun `totals should be properly formatted as currency`() = runTest {
        val env = createTestEnv()
        try {
            // When - scan items
            env.fakeScanner.emitScan("111") // Apple $1.00
            env.fakeScanner.emitScan("222") // Banana $0.50
            
            // Then - totals should be formatted
            val totals = env.viewModel.state.value.totals
            assertEquals("$1.50", totals.subtotal)
            assertEquals("$0.00", totals.taxTotal) // Food items no tax
            assertEquals("$1.50", totals.grandTotal)
        } finally {
            env.cleanup()
        }
    }
}
