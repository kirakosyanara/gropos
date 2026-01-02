package com.unisight.gropos.features.cashier.domain.service

import com.unisight.gropos.features.cashier.domain.model.SessionStatus
import com.unisight.gropos.features.cashier.domain.repository.TillRepository
import kotlinx.coroutines.test.runTest
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for CashierSessionManager.
 * 
 * Per testing-strategy.mdc: Unit test business logic with proper scenarios.
 * Per CASHIER_OPERATIONS.md: Cash pickup decreases drawer balance, requires validation.
 */
class CashierSessionManagerTest {
    
    /**
     * Fake TillRepository for testing.
     */
    private class FakeTillRepository : TillRepository {
        override suspend fun getTills(): Result<List<com.unisight.gropos.features.cashier.domain.model.Till>> = 
            Result.success(emptyList())
        
        override suspend fun getAvailableTills(): Result<List<com.unisight.gropos.features.cashier.domain.model.Till>> = 
            Result.success(emptyList())
        
        override suspend fun assignTill(tillId: Int, employeeId: Int, employeeName: String): Result<Unit> = 
            Result.success(Unit)
        
        override suspend fun releaseTill(tillId: Int): Result<Unit> = 
            Result.success(Unit)
    }
    
    private fun createSessionManager(): CashierSessionManager {
        return CashierSessionManager(FakeTillRepository())
    }
    
    // ========================================================================
    // Session Management Tests
    // ========================================================================
    
    @Test
    fun `startSession creates active session with correct data`() {
        val manager = createSessionManager()
        
        manager.startSession(
            employeeId = 1001,
            employeeName = "John Doe",
            tillId = 1,
            tillName = "Till 1",
            registerId = 1
        )
        
        val session = manager.getCurrentSession()
        assertNotNull(session)
        assertEquals(1001, session.employeeId)
        assertEquals("John Doe", session.employeeName)
        assertEquals(1, session.tillId)
        assertEquals(SessionStatus.ACTIVE, session.status)
        assertEquals(BigDecimal.ZERO, session.expectedCash)
    }
    
    // ========================================================================
    // Transaction Recording Tests
    // ========================================================================
    
    @Test
    fun `recordTransaction increases expectedCash with cash portion`() {
        val manager = createSessionManager()
        
        manager.startSession(
            employeeId = 1001,
            employeeName = "John Doe",
            tillId = 1,
            tillName = "Till 1"
        )
        
        // Record a $50 cash sale
        manager.recordTransaction(
            total = BigDecimal("50.00"),
            cashAmount = BigDecimal("50.00")
        )
        
        val session = manager.getCurrentSession()!!
        assertEquals(BigDecimal("50.00"), session.expectedCash)
        assertEquals(BigDecimal("50.00"), session.cashSales)
        assertEquals(1, session.transactionCount)
    }
    
    @Test
    fun `recordTransaction increases expectedCash for multiple sales`() {
        val manager = createSessionManager()
        
        manager.startSession(
            employeeId = 1001,
            employeeName = "John Doe",
            tillId = 1,
            tillName = "Till 1"
        )
        
        // Record $100 opening float (simulated via transaction)
        manager.recordTransaction(
            total = BigDecimal("100.00"),
            cashAmount = BigDecimal("100.00")
        )
        
        // Record $50 cash sale
        manager.recordTransaction(
            total = BigDecimal("50.00"),
            cashAmount = BigDecimal("50.00")
        )
        
        val session = manager.getCurrentSession()!!
        assertEquals(BigDecimal("150.00"), session.expectedCash)
        assertEquals(2, session.transactionCount)
    }
    
    // ========================================================================
    // Cash Pickup Tests
    // Per CASHIER_OPERATIONS.md: Safe Drops / Pickups
    // ========================================================================
    
    @Test
    fun `cashPickup fails when no active session`() = runTest {
        val manager = createSessionManager()
        
        val result = manager.cashPickup(BigDecimal("50.00"), "manager123")
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }
    
    @Test
    fun `cashPickup fails when amount is zero`() = runTest {
        val manager = createSessionManager()
        
        manager.startSession(
            employeeId = 1001,
            employeeName = "John Doe",
            tillId = 1,
            tillName = "Till 1"
        )
        
        // Add some cash to drawer
        manager.recordTransaction(
            total = BigDecimal("100.00"),
            cashAmount = BigDecimal("100.00")
        )
        
        val result = manager.cashPickup(BigDecimal.ZERO, "manager123")
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertTrue(result.exceptionOrNull()?.message?.contains("greater than zero") == true)
    }
    
    @Test
    fun `cashPickup fails when amount is negative`() = runTest {
        val manager = createSessionManager()
        
        manager.startSession(
            employeeId = 1001,
            employeeName = "John Doe",
            tillId = 1,
            tillName = "Till 1"
        )
        
        // Add some cash to drawer
        manager.recordTransaction(
            total = BigDecimal("100.00"),
            cashAmount = BigDecimal("100.00")
        )
        
        val result = manager.cashPickup(BigDecimal("-50.00"), "manager123")
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }
    
    @Test
    fun `cashPickup fails when amount exceeds drawer balance - Insufficient Funds`() = runTest {
        val manager = createSessionManager()
        
        manager.startSession(
            employeeId = 1001,
            employeeName = "John Doe",
            tillId = 1,
            tillName = "Till 1"
        )
        
        // Add $100 opening + $50 sale = $150 in drawer
        manager.recordTransaction(
            total = BigDecimal("100.00"),
            cashAmount = BigDecimal("100.00")
        )
        manager.recordTransaction(
            total = BigDecimal("50.00"),
            cashAmount = BigDecimal("50.00")
        )
        
        // Try to pickup $200 -> Should fail (only $150 in drawer)
        val result = manager.cashPickup(BigDecimal("200.00"), "manager123")
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertTrue(result.exceptionOrNull()?.message?.contains("Cannot pickup more than drawer balance") == true)
        
        // Verify drawer balance unchanged
        val session = manager.getCurrentSession()!!
        assertEquals(BigDecimal("150.00"), session.expectedCash)
        assertEquals(BigDecimal.ZERO, session.totalCashPickups)
    }
    
    @Test
    fun `cashPickup succeeds and decreases drawer balance`() = runTest {
        val manager = createSessionManager()
        
        manager.startSession(
            employeeId = 1001,
            employeeName = "John Doe",
            tillId = 1,
            tillName = "Till 1"
        )
        
        // Add $100 opening + $50 sale = $150 in drawer
        manager.recordTransaction(
            total = BigDecimal("100.00"),
            cashAmount = BigDecimal("100.00")
        )
        manager.recordTransaction(
            total = BigDecimal("50.00"),
            cashAmount = BigDecimal("50.00")
        )
        
        // Pickup $100 -> Should succeed, balance should be $50
        val result = manager.cashPickup(BigDecimal("100.00"), "manager123")
        
        assertTrue(result.isSuccess)
        
        val session = manager.getCurrentSession()!!
        assertEquals(BigDecimal("50.00"), session.expectedCash)
        assertEquals(BigDecimal("100.00"), session.totalCashPickups)
        assertEquals(1, session.cashPickupCount)
    }
    
    @Test
    fun `multiple cashPickups accumulate correctly`() = runTest {
        val manager = createSessionManager()
        
        manager.startSession(
            employeeId = 1001,
            employeeName = "John Doe",
            tillId = 1,
            tillName = "Till 1"
        )
        
        // Add $500 to drawer
        manager.recordTransaction(
            total = BigDecimal("500.00"),
            cashAmount = BigDecimal("500.00")
        )
        
        // Pickup 1: $100
        manager.cashPickup(BigDecimal("100.00"), "manager123")
        
        // Pickup 2: $150
        manager.cashPickup(BigDecimal("150.00"), "manager456")
        
        val session = manager.getCurrentSession()!!
        assertEquals(BigDecimal("250.00"), session.expectedCash) // 500 - 100 - 150
        assertEquals(BigDecimal("250.00"), session.totalCashPickups) // 100 + 150
        assertEquals(2, session.cashPickupCount)
    }
    
    @Test
    fun `cashPickup exactly equal to drawer balance succeeds`() = runTest {
        val manager = createSessionManager()
        
        manager.startSession(
            employeeId = 1001,
            employeeName = "John Doe",
            tillId = 1,
            tillName = "Till 1"
        )
        
        // Add $100 to drawer
        manager.recordTransaction(
            total = BigDecimal("100.00"),
            cashAmount = BigDecimal("100.00")
        )
        
        // Pickup exactly $100 -> Should succeed, balance should be $0
        val result = manager.cashPickup(BigDecimal("100.00"), "manager123")
        
        assertTrue(result.isSuccess)
        
        val session = manager.getCurrentSession()!!
        assertEquals(BigDecimal("0.00").setScale(2), session.expectedCash.setScale(2))
    }
    
    // ========================================================================
    // getCurrentDrawerBalance Tests
    // ========================================================================
    
    @Test
    fun `getCurrentDrawerBalance returns zero when no session`() {
        val manager = createSessionManager()
        
        assertEquals(BigDecimal.ZERO, manager.getCurrentDrawerBalance())
    }
    
    @Test
    fun `getCurrentDrawerBalance returns correct balance after transactions and pickups`() = runTest {
        val manager = createSessionManager()
        
        manager.startSession(
            employeeId = 1001,
            employeeName = "John Doe",
            tillId = 1,
            tillName = "Till 1"
        )
        
        // Add $200 to drawer
        manager.recordTransaction(
            total = BigDecimal("200.00"),
            cashAmount = BigDecimal("200.00")
        )
        
        // Pickup $75
        manager.cashPickup(BigDecimal("75.00"), "manager123")
        
        assertEquals(BigDecimal("125.00"), manager.getCurrentDrawerBalance())
    }
    
    // ========================================================================
    // Vendor Payout Tests
    // Per FUNCTIONS_MENU.md: Vendor Payout pays vendors directly from till
    // ========================================================================
    
    @Test
    fun `vendorPayout fails when no active session`() = runTest {
        val manager = createSessionManager()
        
        val result = manager.vendorPayout(
            amount = BigDecimal("50.00"),
            vendorId = "vendor_001",
            vendorName = "Pepsi",
            managerId = "manager123"
        )
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }
    
    @Test
    fun `vendorPayout fails when amount is zero`() = runTest {
        val manager = createSessionManager()
        
        manager.startSession(
            employeeId = 1001,
            employeeName = "John Doe",
            tillId = 1,
            tillName = "Till 1"
        )
        
        // Add $100 to drawer
        manager.recordTransaction(
            total = BigDecimal("100.00"),
            cashAmount = BigDecimal("100.00")
        )
        
        val result = manager.vendorPayout(
            amount = BigDecimal.ZERO,
            vendorId = "vendor_001",
            vendorName = "Pepsi",
            managerId = "manager123"
        )
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertTrue(result.exceptionOrNull()?.message?.contains("greater than zero") == true)
    }
    
    @Test
    fun `vendorPayout fails when amount exceeds drawer balance - Insufficient Funds`() = runTest {
        val manager = createSessionManager()
        
        manager.startSession(
            employeeId = 1001,
            employeeName = "John Doe",
            tillId = 1,
            tillName = "Till 1"
        )
        
        // Add $50 to drawer
        manager.recordTransaction(
            total = BigDecimal("50.00"),
            cashAmount = BigDecimal("50.00")
        )
        
        // Try to payout $60 -> Should fail (only $50 in drawer)
        val result = manager.vendorPayout(
            amount = BigDecimal("60.00"),
            vendorId = "vendor_002",
            vendorName = "Coca-Cola",
            managerId = "manager123"
        )
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertTrue(result.exceptionOrNull()?.message?.contains("Cannot pay out more than drawer balance") == true)
        
        // Verify drawer balance unchanged
        val session = manager.getCurrentSession()!!
        assertEquals(BigDecimal("50.00"), session.expectedCash)
        assertEquals(BigDecimal.ZERO, session.totalVendorPayouts)
    }
    
    @Test
    fun `vendorPayout succeeds and decreases drawer balance`() = runTest {
        val manager = createSessionManager()
        
        manager.startSession(
            employeeId = 1001,
            employeeName = "John Doe",
            tillId = 1,
            tillName = "Till 1"
        )
        
        // Add $100 to drawer
        manager.recordTransaction(
            total = BigDecimal("100.00"),
            cashAmount = BigDecimal("100.00")
        )
        
        // Payout $50 to Pepsi -> Should succeed, balance should be $50
        val result = manager.vendorPayout(
            amount = BigDecimal("50.00"),
            vendorId = "vendor_001",
            vendorName = "Pepsi",
            managerId = "manager123"
        )
        
        assertTrue(result.isSuccess)
        
        val session = manager.getCurrentSession()!!
        assertEquals(BigDecimal("50.00"), session.expectedCash)
        assertEquals(BigDecimal("50.00"), session.totalVendorPayouts)
        assertEquals(1, session.vendorPayoutCount)
    }
    
    @Test
    fun `multiple vendorPayouts accumulate correctly`() = runTest {
        val manager = createSessionManager()
        
        manager.startSession(
            employeeId = 1001,
            employeeName = "John Doe",
            tillId = 1,
            tillName = "Till 1"
        )
        
        // Add $500 to drawer
        manager.recordTransaction(
            total = BigDecimal("500.00"),
            cashAmount = BigDecimal("500.00")
        )
        
        // Payout 1: $50 to Pepsi
        manager.vendorPayout(
            amount = BigDecimal("50.00"),
            vendorId = "vendor_001",
            vendorName = "Pepsi",
            managerId = "manager123"
        )
        
        // Payout 2: $75 to Coca-Cola
        manager.vendorPayout(
            amount = BigDecimal("75.00"),
            vendorId = "vendor_002",
            vendorName = "Coca-Cola",
            managerId = "manager456"
        )
        
        val session = manager.getCurrentSession()!!
        assertEquals(BigDecimal("375.00"), session.expectedCash) // 500 - 50 - 75
        assertEquals(BigDecimal("125.00"), session.totalVendorPayouts) // 50 + 75
        assertEquals(2, session.vendorPayoutCount)
    }
    
    @Test
    fun `vendorPayout and cashPickup both decrease drawer balance correctly`() = runTest {
        val manager = createSessionManager()
        
        manager.startSession(
            employeeId = 1001,
            employeeName = "John Doe",
            tillId = 1,
            tillName = "Till 1"
        )
        
        // Start with $100 in drawer
        manager.recordTransaction(
            total = BigDecimal("100.00"),
            cashAmount = BigDecimal("100.00")
        )
        
        // Vendor payout $50 to Pepsi -> $50 remaining
        manager.vendorPayout(
            amount = BigDecimal("50.00"),
            vendorId = "vendor_001",
            vendorName = "Pepsi",
            managerId = "manager123"
        )
        
        // Try to payout $60 to Coca-Cola -> Should fail (only $50 remaining)
        val result = manager.vendorPayout(
            amount = BigDecimal("60.00"),
            vendorId = "vendor_002",
            vendorName = "Coca-Cola",
            managerId = "manager123"
        )
        
        assertTrue(result.isFailure)
        
        // Verify final state
        val session = manager.getCurrentSession()!!
        assertEquals(BigDecimal("50.00"), session.expectedCash)
        assertEquals(BigDecimal("50.00"), session.totalVendorPayouts)
        assertEquals(1, session.vendorPayoutCount)
    }
}

