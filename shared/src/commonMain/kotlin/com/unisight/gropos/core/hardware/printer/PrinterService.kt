package com.unisight.gropos.core.hardware.printer

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Hardware abstraction interface for receipt printer operations.
 * 
 * **Per QA Audit Finding (CRITICAL):**
 * Previous implementation had no error handling for hardware disconnects.
 * This interface provides:
 * 1. Connection status monitoring via [connectionStatus] Flow
 * 2. Safe operation wrappers that return [PrintResult] instead of throwing
 * 3. Recovery mechanisms for mid-print failures
 * 
 * **Design Principles:**
 * - All operations return [PrintResult] (never throw hardware exceptions)
 * - Connection state is always observable via [connectionStatus]
 * - Failed print jobs are trackable for retry
 * 
 * **Implementations:**
 * - SimulatedPrinterService: Development/testing (logs to console)
 * - DesktopPrinterService: USB/Serial ESC/POS printers
 * - AndroidPrinterService: Sunmi/PAX built-in printers
 * 
 * Per DESKTOP_HARDWARE.md: Receipt printers use ESC/POS command protocol.
 * Per reliability-stability.mdc: Resource cleanup with use blocks.
 */
interface PrinterService {
    
    /**
     * Current printer connection status.
     * 
     * Observe this flow to update UI indicators and handle disconnects.
     * Emits [ConnectionStatus.Disconnected] when cable is unplugged.
     */
    val connectionStatus: StateFlow<ConnectionStatus>
    
    /**
     * Flow of printer events (paper low, cover open, etc.).
     * 
     * Useful for proactive alerts before failure occurs.
     */
    val printerEvents: Flow<PrinterEvent>
    
    /**
     * Connects to the printer hardware.
     * 
     * Call this when:
     * - Application starts
     * - User navigates to payment screen
     * - After recovering from disconnect
     * 
     * @return [PrintResult.Success] if connected, [PrintResult.Error] otherwise
     */
    suspend fun connect(): PrintResult
    
    /**
     * Disconnects from the printer hardware.
     * 
     * Call this when:
     * - Application exits
     * - User logs out
     * - Switching to different printer
     */
    suspend fun disconnect()
    
    /**
     * Prints a receipt safely.
     * 
     * This method NEVER throws exceptions. All hardware errors are captured
     * and returned as [PrintResult.Error].
     * 
     * **Recovery Behavior:**
     * If printer disconnects mid-print:
     * 1. Returns [PrintResult.Error] with [PrintErrorCode.DISCONNECT_MID_PRINT]
     * 2. Stores failed [Receipt] for retry via [getFailedPrintJobs]
     * 3. Attempts to restore connection state
     * 
     * @param receipt The receipt to print
     * @return [PrintResult.Success] or [PrintResult.Error] with failure details
     */
    suspend fun printReceipt(receipt: Receipt): PrintResult
    
    /**
     * Opens the cash drawer (if connected via printer).
     * 
     * Many cash drawers connect through the RJ-11 port on thermal printers.
     * 
     * @return [PrintResult.Success] or [PrintResult.Error]
     */
    suspend fun openCashDrawer(): PrintResult
    
    /**
     * Checks paper status.
     * 
     * @return Current paper status (OK, Low, Empty, Unknown)
     */
    suspend fun checkPaperStatus(): PaperStatus
    
    /**
     * Gets list of print jobs that failed and need retry.
     * 
     * Failed jobs are stored when:
     * - Printer disconnects mid-print
     * - Paper runs out during print
     * - Hardware error occurs
     * 
     * @return List of failed print jobs
     */
    fun getFailedPrintJobs(): List<FailedPrintJob>
    
    /**
     * Retries a previously failed print job.
     * 
     * @param jobId The ID of the failed job to retry
     * @return [PrintResult.Success] or [PrintResult.Error]
     */
    suspend fun retryPrintJob(jobId: String): PrintResult
    
    /**
     * Clears a failed print job from the retry queue.
     * 
     * Call this when:
     * - User chooses to skip reprinting
     * - Job is no longer needed
     * 
     * @param jobId The ID of the job to clear
     */
    fun clearFailedPrintJob(jobId: String)
}

/**
 * Current connection status of the printer.
 */
enum class ConnectionStatus {
    /** Printer is connected and ready */
    Connected,
    
    /** Printer is disconnected (cable unplugged, power off, etc.) */
    Disconnected,
    
    /** Attempting to connect */
    Connecting,
    
    /** Connection failed (error state) */
    Error,
    
    /** Connection status unknown (initial state) */
    Unknown
}

/**
 * Events emitted by the printer for proactive monitoring.
 */
sealed class PrinterEvent {
    /** Paper is running low (warning) */
    data object PaperLow : PrinterEvent()
    
    /** Paper has run out */
    data object PaperEmpty : PrinterEvent()
    
    /** Printer cover is open */
    data object CoverOpen : PrinterEvent()
    
    /** Printer cover is closed */
    data object CoverClosed : PrinterEvent()
    
    /** Printer is overheating (thermal protection) */
    data object Overheating : PrinterEvent()
    
    /** Printer has recovered from error state */
    data object Recovered : PrinterEvent()
    
    /** Printer cable was disconnected */
    data object Disconnected : PrinterEvent()
    
    /** Printer cable was reconnected */
    data object Reconnected : PrinterEvent()
}

/**
 * Result of a print operation.
 * 
 * Per code-quality.mdc: Use sealed classes for exhaustive error handling.
 */
sealed class PrintResult {
    
    /** Print operation completed successfully */
    data object Success : PrintResult()
    
    /**
     * Print operation failed.
     * 
     * @property errorCode Specific error type for handling
     * @property message Human-readable error description
     * @property isRecoverable Whether retry might succeed
     */
    data class Error(
        val errorCode: PrintErrorCode,
        val message: String,
        val isRecoverable: Boolean
    ) : PrintResult()
}

/**
 * Specific error codes for print failures.
 */
enum class PrintErrorCode {
    /** Printer is not connected */
    NOT_CONNECTED,
    
    /** Printer disconnected during print operation */
    DISCONNECT_MID_PRINT,
    
    /** Paper has run out */
    PAPER_EMPTY,
    
    /** Printer cover is open */
    COVER_OPEN,
    
    /** Printer is overheating */
    OVERHEATING,
    
    /** Generic hardware communication error */
    COMMUNICATION_ERROR,
    
    /** Printer returned unexpected response */
    PROTOCOL_ERROR,
    
    /** Operation timed out */
    TIMEOUT,
    
    /** Unknown error */
    UNKNOWN
}

/**
 * Current paper status.
 */
enum class PaperStatus {
    /** Paper is available (normal operation) */
    OK,
    
    /** Paper is running low (warning) */
    Low,
    
    /** Paper has run out (cannot print) */
    Empty,
    
    /** Status could not be determined */
    Unknown
}

/**
 * A print job that failed and is queued for retry.
 */
data class FailedPrintJob(
    /** Unique identifier for this job */
    val id: String,
    
    /** The receipt that failed to print */
    val receipt: Receipt,
    
    /** Why the job failed */
    val errorCode: PrintErrorCode,
    
    /** When the failure occurred (ISO-8601) */
    val failedAt: String,
    
    /** Number of retry attempts so far */
    val retryCount: Int
)

/**
 * Data model for a printable receipt.
 * 
 * Per RECEIPT_FORMAT.md: Standard receipt layout.
 */
data class Receipt(
    /** Store header (name, address, phone) */
    val header: String,
    
    /** List of items to print */
    val items: List<ReceiptItem>,
    
    /** Totals section (subtotal, tax, grand total) */
    val totals: String,
    
    /** Payment information */
    val payments: String,
    
    /** Footer (thank you, return policy, etc.) */
    val footer: String,
    
    /** Optional barcode for transaction lookup */
    val barcode: String? = null,
    
    /** Transaction ID for retry tracking */
    val transactionId: String
)

/**
 * A single line item on the receipt.
 */
data class ReceiptItem(
    val name: String,
    val quantity: String,
    val price: String,
    val lineTotal: String,
    val taxIndicator: String = ""
)

