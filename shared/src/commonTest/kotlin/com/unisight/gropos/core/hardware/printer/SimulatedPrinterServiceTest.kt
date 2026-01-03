package com.unisight.gropos.core.hardware.printer

import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Tests for SimulatedPrinterService demonstrating safe print patterns.
 * 
 * Per QA Audit: Printer operations must never crash.
 * These tests verify that all failure modes are handled gracefully.
 */
class SimulatedPrinterServiceTest {
    
    private lateinit var printerService: SimulatedPrinterService
    
    private val testReceipt = Receipt(
        header = "TEST STORE\n123 Main St\n555-1234",
        items = listOf(
            ReceiptItem("Apple", "1", "$1.00", "$1.00", "F"),
            ReceiptItem("Soda", "2", "$2.50", "$5.00", "T")
        ),
        totals = "Subtotal: $6.00\nTax: $0.48\nTotal: $6.48",
        payments = "Cash: $10.00\nChange: $3.52",
        footer = "Thank you for shopping!",
        transactionId = "TXN-001"
    )
    
    @BeforeTest
    fun setup() {
        printerService = SimulatedPrinterService()
    }
    
    // ========================================================================
    // Connection Tests
    // ========================================================================
    
    @Test
    fun `initial status is Disconnected`() {
        assertEquals(ConnectionStatus.Disconnected, printerService.connectionStatus.value)
    }
    
    @Test
    fun `connect succeeds and updates status`() = runTest {
        val result = printerService.connect()
        
        assertIs<PrintResult.Success>(result)
        assertEquals(ConnectionStatus.Connected, printerService.connectionStatus.value)
    }
    
    @Test
    fun `connect fails when simulating disconnect`() = runTest {
        printerService.simulateDisconnect = true
        
        val result = printerService.connect()
        
        assertIs<PrintResult.Error>(result)
        assertEquals(PrintErrorCode.NOT_CONNECTED, result.errorCode)
        assertEquals(ConnectionStatus.Error, printerService.connectionStatus.value)
    }
    
    @Test
    fun `disconnect updates status`() = runTest {
        printerService.connect()
        printerService.disconnect()
        
        assertEquals(ConnectionStatus.Disconnected, printerService.connectionStatus.value)
    }
    
    // ========================================================================
    // Print Success Tests
    // ========================================================================
    
    @Test
    fun `printReceipt succeeds when connected`() = runTest {
        printerService.connect()
        printerService.printDelayMs = 10 // Speed up test
        
        val result = printerService.printReceipt(testReceipt)
        
        assertIs<PrintResult.Success>(result)
    }
    
    @Test
    fun `printReceipt fails when not connected`() = runTest {
        // Don't connect
        val result = printerService.printReceipt(testReceipt)
        
        assertIs<PrintResult.Error>(result)
        assertEquals(PrintErrorCode.NOT_CONNECTED, result.errorCode)
        assertTrue(result.isRecoverable)
    }
    
    // ========================================================================
    // Paper Out Tests
    // ========================================================================
    
    @Test
    fun `printReceipt fails when paper is empty`() = runTest {
        printerService.connect()
        printerService.simulatePaperEmpty = true
        
        val result = printerService.printReceipt(testReceipt)
        
        assertIs<PrintResult.Error>(result)
        assertEquals(PrintErrorCode.PAPER_EMPTY, result.errorCode)
        assertTrue(result.isRecoverable)
    }
    
    @Test
    fun `paper empty failure stores job for retry`() = runTest {
        printerService.connect()
        printerService.simulatePaperEmpty = true
        
        printerService.printReceipt(testReceipt)
        
        val failedJobs = printerService.getFailedPrintJobs()
        assertEquals(1, failedJobs.size)
        assertEquals(PrintErrorCode.PAPER_EMPTY, failedJobs[0].errorCode)
        assertEquals("TXN-001", failedJobs[0].receipt.transactionId)
    }
    
    // ========================================================================
    // Mid-Print Disconnect Tests (CRITICAL)
    // ========================================================================
    
    @Test
    fun `printReceipt handles mid-print disconnect gracefully`() = runTest {
        printerService.connect()
        printerService.simulateMidPrintDisconnect = true
        printerService.printDelayMs = 10
        
        val result = printerService.printReceipt(testReceipt)
        
        // Should return error, NOT crash
        assertIs<PrintResult.Error>(result)
        assertEquals(PrintErrorCode.DISCONNECT_MID_PRINT, result.errorCode)
        assertTrue(result.isRecoverable)
        
        // Connection status should reflect disconnect
        assertEquals(ConnectionStatus.Disconnected, printerService.connectionStatus.value)
    }
    
    @Test
    fun `mid-print disconnect stores job for retry`() = runTest {
        printerService.connect()
        printerService.simulateMidPrintDisconnect = true
        
        printerService.printReceipt(testReceipt)
        
        val failedJobs = printerService.getFailedPrintJobs()
        assertEquals(1, failedJobs.size)
        assertEquals(PrintErrorCode.DISCONNECT_MID_PRINT, failedJobs[0].errorCode)
    }
    
    // ========================================================================
    // Retry Tests
    // ========================================================================
    
    @Test
    fun `retryPrintJob succeeds after recovery`() = runTest {
        // First: Create a failed job
        printerService.connect()
        printerService.simulatePaperEmpty = true
        printerService.printReceipt(testReceipt)
        
        val failedJobs = printerService.getFailedPrintJobs()
        assertEquals(1, failedJobs.size)
        val jobId = failedJobs[0].id
        
        // Second: Fix the issue and retry
        printerService.simulatePaperEmpty = false
        printerService.printDelayMs = 10
        
        val result = printerService.retryPrintJob(jobId)
        
        assertIs<PrintResult.Success>(result)
        
        // Job should be removed from failed queue
        assertEquals(0, printerService.getFailedPrintJobs().size)
    }
    
    @Test
    fun `retryPrintJob increments retry count on failure`() = runTest {
        printerService.connect()
        printerService.simulatePaperEmpty = true
        printerService.printReceipt(testReceipt)
        
        val jobId = printerService.getFailedPrintJobs()[0].id
        
        // Retry still fails
        printerService.retryPrintJob(jobId)
        
        val updatedJob = printerService.getFailedPrintJobs()[0]
        assertEquals(1, updatedJob.retryCount)
    }
    
    @Test
    fun `clearFailedPrintJob removes job from queue`() = runTest {
        printerService.connect()
        printerService.simulatePaperEmpty = true
        printerService.printReceipt(testReceipt)
        
        val jobId = printerService.getFailedPrintJobs()[0].id
        
        printerService.clearFailedPrintJob(jobId)
        
        assertEquals(0, printerService.getFailedPrintJobs().size)
    }
    
    // ========================================================================
    // Cash Drawer Tests
    // ========================================================================
    
    @Test
    fun `openCashDrawer succeeds when connected`() = runTest {
        printerService.connect()
        
        val result = printerService.openCashDrawer()
        
        assertIs<PrintResult.Success>(result)
    }
    
    @Test
    fun `openCashDrawer fails when not connected`() = runTest {
        val result = printerService.openCashDrawer()
        
        assertIs<PrintResult.Error>(result)
        assertEquals(PrintErrorCode.NOT_CONNECTED, result.errorCode)
    }
    
    // ========================================================================
    // Paper Status Tests
    // ========================================================================
    
    @Test
    fun `checkPaperStatus returns OK when connected`() = runTest {
        printerService.connect()
        
        val status = printerService.checkPaperStatus()
        
        assertEquals(PaperStatus.OK, status)
    }
    
    @Test
    fun `checkPaperStatus returns Empty when simulated`() = runTest {
        printerService.connect()
        printerService.simulatePaperEmpty = true
        
        val status = printerService.checkPaperStatus()
        
        assertEquals(PaperStatus.Empty, status)
    }
    
    @Test
    fun `checkPaperStatus returns Unknown when disconnected`() = runTest {
        val status = printerService.checkPaperStatus()
        
        assertEquals(PaperStatus.Unknown, status)
    }
}

