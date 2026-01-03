package com.unisight.gropos.core.hardware.printer

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import java.util.UUID

/**
 * Simulated implementation of PrinterService for development and testing.
 * 
 * **Purpose:**
 * - Development without physical printer
 * - Unit/integration testing with controllable behavior
 * - Demonstration of safe exception handling patterns
 * 
 * **Test Scenarios Supported:**
 * - Normal printing (success)
 * - Printer disconnection mid-print
 * - Paper out simulation
 * - Timeout simulation
 * 
 * Per testing-strategy.mdc: Use Fakes for state, simulate edge cases.
 */
class SimulatedPrinterService : PrinterService {
    
    // ========================================================================
    // State
    // ========================================================================
    
    private val _connectionStatus = MutableStateFlow(ConnectionStatus.Disconnected)
    override val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()
    
    private val _printerEvents = MutableSharedFlow<PrinterEvent>(
        replay = 0,
        extraBufferCapacity = 10
    )
    override val printerEvents: Flow<PrinterEvent> = _printerEvents.asSharedFlow()
    
    private val failedJobs = mutableMapOf<String, FailedPrintJob>()
    
    // ========================================================================
    // Test Control (for simulating failures)
    // ========================================================================
    
    /** Set to true to simulate printer disconnection */
    var simulateDisconnect = false
    
    /** Set to true to simulate paper empty */
    var simulatePaperEmpty = false
    
    /** Set to true to simulate disconnect mid-print */
    var simulateMidPrintDisconnect = false
    
    /** Set to true to simulate timeout */
    var simulateTimeout = false
    
    /** Delay in ms for simulated printing */
    var printDelayMs = 200L
    
    // ========================================================================
    // Connection Management
    // ========================================================================
    
    override suspend fun connect(): PrintResult {
        if (simulateDisconnect) {
            _connectionStatus.value = ConnectionStatus.Error
            return PrintResult.Error(
                errorCode = PrintErrorCode.NOT_CONNECTED,
                message = "Simulated: Printer not found",
                isRecoverable = true
            )
        }
        
        _connectionStatus.value = ConnectionStatus.Connecting
        delay(100) // Simulate connection delay
        
        _connectionStatus.value = ConnectionStatus.Connected
        _printerEvents.tryEmit(PrinterEvent.Reconnected)
        
        println("[PRINTER] Connected (simulated)")
        return PrintResult.Success
    }
    
    override suspend fun disconnect() {
        _connectionStatus.value = ConnectionStatus.Disconnected
        _printerEvents.tryEmit(PrinterEvent.Disconnected)
        println("[PRINTER] Disconnected")
    }
    
    // ========================================================================
    // Safe Print Operations
    // ========================================================================
    
    /**
     * Prints a receipt with full exception safety.
     * 
     * This demonstrates the pattern that real implementations MUST follow:
     * 1. Check connection state FIRST
     * 2. Wrap all hardware calls in try/catch
     * 3. Return PrintResult.Error, never throw
     * 4. Store failed jobs for retry
     */
    override suspend fun printReceipt(receipt: Receipt): PrintResult {
        // Step 1: Verify connection
        if (_connectionStatus.value != ConnectionStatus.Connected) {
            return PrintResult.Error(
                errorCode = PrintErrorCode.NOT_CONNECTED,
                message = "Printer is not connected",
                isRecoverable = true
            )
        }
        
        // Step 2: Check paper status
        if (simulatePaperEmpty) {
            _printerEvents.tryEmit(PrinterEvent.PaperEmpty)
            storeFailedJob(receipt, PrintErrorCode.PAPER_EMPTY)
            return PrintResult.Error(
                errorCode = PrintErrorCode.PAPER_EMPTY,
                message = "Paper tray is empty",
                isRecoverable = true
            )
        }
        
        // Step 3: Attempt print with exception safety
        return safePrint(receipt)
    }
    
    /**
     * Safe print wrapper that catches ALL exceptions.
     * 
     * Per QA Audit: Hardware operations must NEVER crash the app.
     */
    private suspend fun safePrint(receipt: Receipt): PrintResult {
        return try {
            // Simulate printing delay
            delay(printDelayMs / 2)
            
            // Simulate mid-print disconnect
            if (simulateMidPrintDisconnect) {
                _connectionStatus.value = ConnectionStatus.Disconnected
                _printerEvents.tryEmit(PrinterEvent.Disconnected)
                
                storeFailedJob(receipt, PrintErrorCode.DISCONNECT_MID_PRINT)
                
                return PrintResult.Error(
                    errorCode = PrintErrorCode.DISCONNECT_MID_PRINT,
                    message = "Printer disconnected during print. Receipt saved for retry.",
                    isRecoverable = true
                )
            }
            
            // Simulate timeout
            if (simulateTimeout) {
                delay(5000) // Would timeout in real scenario
                return PrintResult.Error(
                    errorCode = PrintErrorCode.TIMEOUT,
                    message = "Print operation timed out",
                    isRecoverable = true
                )
            }
            
            delay(printDelayMs / 2)
            
            // Simulate successful print output
            printToConsole(receipt)
            
            PrintResult.Success
            
        } catch (e: Exception) {
            // CATCH ALL - never let hardware exceptions escape
            println("[PRINTER] EXCEPTION CAUGHT: ${e.message}")
            
            storeFailedJob(receipt, PrintErrorCode.UNKNOWN)
            
            PrintResult.Error(
                errorCode = PrintErrorCode.UNKNOWN,
                message = "Print failed: ${e.message}",
                isRecoverable = true
            )
        }
    }
    
    /**
     * Outputs receipt to console (simulating physical print).
     */
    private fun printToConsole(receipt: Receipt) {
        val separator = "=".repeat(42)
        val thinSeparator = "-".repeat(42)
        
        println()
        println(separator)
        println(receipt.header)
        println(thinSeparator)
        
        receipt.items.forEach { item ->
            println("${item.name}")
            println("  ${item.quantity} x ${item.price} = ${item.lineTotal} ${item.taxIndicator}")
        }
        
        println(thinSeparator)
        println(receipt.totals)
        println(thinSeparator)
        println(receipt.payments)
        println(separator)
        println(receipt.footer)
        
        if (receipt.barcode != null) {
            println("[BARCODE: ${receipt.barcode}]")
        }
        
        println(separator)
        println()
    }
    
    // ========================================================================
    // Cash Drawer
    // ========================================================================
    
    override suspend fun openCashDrawer(): PrintResult {
        if (_connectionStatus.value != ConnectionStatus.Connected) {
            return PrintResult.Error(
                errorCode = PrintErrorCode.NOT_CONNECTED,
                message = "Printer not connected (drawer connects via printer)",
                isRecoverable = true
            )
        }
        
        return try {
            delay(50) // Simulate pulse
            println("[CASH DRAWER] *CLICK* - Drawer opened")
            PrintResult.Success
        } catch (e: Exception) {
            PrintResult.Error(
                errorCode = PrintErrorCode.COMMUNICATION_ERROR,
                message = "Failed to open drawer: ${e.message}",
                isRecoverable = true
            )
        }
    }
    
    // ========================================================================
    // Paper Status
    // ========================================================================
    
    override suspend fun checkPaperStatus(): PaperStatus {
        return when {
            _connectionStatus.value != ConnectionStatus.Connected -> PaperStatus.Unknown
            simulatePaperEmpty -> PaperStatus.Empty
            else -> PaperStatus.OK
        }
    }
    
    // ========================================================================
    // Failed Job Management
    // ========================================================================
    
    private fun storeFailedJob(receipt: Receipt, errorCode: PrintErrorCode) {
        val jobId = UUID.randomUUID().toString()
        val failedJob = FailedPrintJob(
            id = jobId,
            receipt = receipt,
            errorCode = errorCode,
            failedAt = Clock.System.now().toString(),
            retryCount = 0
        )
        failedJobs[jobId] = failedJob
        println("[PRINTER] Stored failed job: $jobId (${errorCode.name})")
    }
    
    override fun getFailedPrintJobs(): List<FailedPrintJob> {
        return failedJobs.values.toList()
    }
    
    override suspend fun retryPrintJob(jobId: String): PrintResult {
        val job = failedJobs[jobId]
            ?: return PrintResult.Error(
                errorCode = PrintErrorCode.UNKNOWN,
                message = "Failed job not found: $jobId",
                isRecoverable = false
            )
        
        // Update retry count
        failedJobs[jobId] = job.copy(retryCount = job.retryCount + 1)
        
        // Attempt print
        val result = printReceipt(job.receipt)
        
        // If successful, remove from failed queue
        if (result is PrintResult.Success) {
            failedJobs.remove(jobId)
        }
        
        return result
    }
    
    override fun clearFailedPrintJob(jobId: String) {
        failedJobs.remove(jobId)
        println("[PRINTER] Cleared failed job: $jobId")
    }
    
    // ========================================================================
    // Test Helpers
    // ========================================================================
    
    /** Resets all simulation flags to default (success) state */
    fun reset() {
        simulateDisconnect = false
        simulatePaperEmpty = false
        simulateMidPrintDisconnect = false
        simulateTimeout = false
        printDelayMs = 200L
        failedJobs.clear()
        _connectionStatus.value = ConnectionStatus.Disconnected
    }
}

