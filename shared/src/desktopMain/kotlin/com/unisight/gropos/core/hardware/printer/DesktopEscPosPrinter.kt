package com.unisight.gropos.core.hardware.printer

import com.fazecast.jSerialComm.SerialPort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.nio.charset.Charset
import java.util.UUID
import kotlinx.datetime.Clock

/**
 * Desktop implementation of PrinterService using ESC/POS over serial/USB.
 * 
 * **Per DESKTOP_HARDWARE.md:**
 * - Implements ESC/POS command protocol for thermal receipt printers
 * - Works with Epson TM-T88, Star TSP, and compatible printers
 * - Uses jSerialComm for USB-to-Serial communication
 * 
 * **ESC/POS Protocol:**
 * Standard command set for thermal printers. Commands are byte sequences
 * starting with ESC (0x1B) or GS (0x1D).
 * 
 * **Connection Types Supported:**
 * - USB (appears as virtual COM port)
 * - Serial RS-232
 * 
 * **Thread Safety:**
 * All I/O operations run on Dispatchers.IO to avoid blocking Main.
 * 
 * Per reliability-stability.mdc: All operations return PrintResult, never throw.
 */
class DesktopEscPosPrinter(
    private val config: PrinterConfig = PrinterConfig()
) : PrinterService {
    
    private var serialPort: SerialPort? = null
    
    private val _connectionStatus = MutableStateFlow(ConnectionStatus.Unknown)
    override val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()
    
    private val _printerEvents = MutableSharedFlow<PrinterEvent>(
        replay = 0,
        extraBufferCapacity = 8
    )
    override val printerEvents: Flow<PrinterEvent> = _printerEvents.asSharedFlow()
    
    private val failedJobs = mutableListOf<FailedPrintJob>()
    
    // ========================================================================
    // Connection Management
    // ========================================================================
    
    override suspend fun connect(): PrintResult = withContext(Dispatchers.IO) {
        try {
            _connectionStatus.value = ConnectionStatus.Connecting
            
            val port = findPrinterPort()
            
            if (port == null) {
                _connectionStatus.value = ConnectionStatus.Disconnected
                return@withContext PrintResult.Error(
                    errorCode = PrintErrorCode.NOT_CONNECTED,
                    message = "No printer port available",
                    isRecoverable = true
                )
            }
            
            serialPort = port
            configurePort(port)
            
            if (!port.openPort()) {
                _connectionStatus.value = ConnectionStatus.Error
                return@withContext PrintResult.Error(
                    errorCode = PrintErrorCode.COMMUNICATION_ERROR,
                    message = "Failed to open port ${port.systemPortName}",
                    isRecoverable = true
                )
            }
            
            // Initialize printer with ESC/POS reset command
            val initSuccess = sendCommand(EscPosCommands.INITIALIZE)
            
            if (initSuccess) {
                _connectionStatus.value = ConnectionStatus.Connected
                _printerEvents.tryEmit(PrinterEvent.Reconnected)
                println("[PRINTER] Connected to ${port.systemPortName}")
                PrintResult.Success
            } else {
                port.closePort()
                _connectionStatus.value = ConnectionStatus.Error
                PrintResult.Error(
                    errorCode = PrintErrorCode.COMMUNICATION_ERROR,
                    message = "Failed to initialize printer",
                    isRecoverable = true
                )
            }
            
        } catch (e: Exception) {
            _connectionStatus.value = ConnectionStatus.Error
            PrintResult.Error(
                errorCode = PrintErrorCode.UNKNOWN,
                message = e.message ?: "Unknown error",
                isRecoverable = true
            )
        }
    }
    
    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            serialPort?.closePort()
            serialPort = null
            _connectionStatus.value = ConnectionStatus.Disconnected
            _printerEvents.tryEmit(PrinterEvent.Disconnected)
            println("[PRINTER] Disconnected")
        } catch (e: Exception) {
            println("[PRINTER] ERROR disconnecting: ${e.message}")
        }
    }
    
    // ========================================================================
    // Printing Operations
    // ========================================================================
    
    override suspend fun printReceipt(receipt: Receipt): PrintResult = withContext(Dispatchers.IO) {
        if (_connectionStatus.value != ConnectionStatus.Connected) {
            // Try to reconnect
            val connectResult = connect()
            if (connectResult is PrintResult.Error) {
                storeFailedJob(receipt, PrintErrorCode.NOT_CONNECTED)
                return@withContext connectResult
            }
        }
        
        try {
            // Build ESC/POS command sequence
            val commands = buildReceiptCommands(receipt)
            
            // Send to printer
            val success = sendCommand(commands)
            
            if (success) {
                println("[PRINTER] Receipt printed: ${receipt.transactionId}")
                PrintResult.Success
            } else {
                storeFailedJob(receipt, PrintErrorCode.COMMUNICATION_ERROR)
                PrintResult.Error(
                    errorCode = PrintErrorCode.COMMUNICATION_ERROR,
                    message = "Failed to send print data",
                    isRecoverable = true
                )
            }
            
        } catch (e: Exception) {
            // Check if disconnect happened mid-print
            if (serialPort?.isOpen == false) {
                _connectionStatus.value = ConnectionStatus.Disconnected
                _printerEvents.tryEmit(PrinterEvent.Disconnected)
                storeFailedJob(receipt, PrintErrorCode.DISCONNECT_MID_PRINT)
                return@withContext PrintResult.Error(
                    errorCode = PrintErrorCode.DISCONNECT_MID_PRINT,
                    message = "Printer disconnected during print",
                    isRecoverable = true
                )
            }
            
            storeFailedJob(receipt, PrintErrorCode.UNKNOWN)
            PrintResult.Error(
                errorCode = PrintErrorCode.UNKNOWN,
                message = e.message ?: "Unknown print error",
                isRecoverable = true
            )
        }
    }
    
    override suspend fun openCashDrawer(): PrintResult = withContext(Dispatchers.IO) {
        if (_connectionStatus.value != ConnectionStatus.Connected) {
            return@withContext PrintResult.Error(
                errorCode = PrintErrorCode.NOT_CONNECTED,
                message = "Printer not connected",
                isRecoverable = true
            )
        }
        
        try {
            val success = sendCommand(EscPosCommands.OPEN_DRAWER)
            
            if (success) {
                println("[PRINTER] Cash drawer opened")
                PrintResult.Success
            } else {
                PrintResult.Error(
                    errorCode = PrintErrorCode.COMMUNICATION_ERROR,
                    message = "Failed to open cash drawer",
                    isRecoverable = true
                )
            }
        } catch (e: Exception) {
            PrintResult.Error(
                errorCode = PrintErrorCode.UNKNOWN,
                message = e.message ?: "Unknown error",
                isRecoverable = true
            )
        }
    }
    
    override suspend fun checkPaperStatus(): PaperStatus = withContext(Dispatchers.IO) {
        if (serialPort?.isOpen != true) {
            return@withContext PaperStatus.Unknown
        }
        
        try {
            // Send status request command
            sendCommand(EscPosCommands.STATUS_REQUEST)
            
            // Read response (with timeout)
            delay(100)
            val bytesAvailable = serialPort?.bytesAvailable() ?: 0
            
            if (bytesAvailable <= 0) {
                return@withContext PaperStatus.Unknown
            }
            
            val response = ByteArray(bytesAvailable)
            serialPort?.readBytes(response, bytesAvailable)
            
            // Parse paper status from response
            // Bit 5: Paper near end, Bit 6: Paper end
            if (response.isNotEmpty()) {
                val status = response[0].toInt()
                return@withContext when {
                    status and 0x60 == 0x60 -> PaperStatus.Empty
                    status and 0x20 == 0x20 -> PaperStatus.Low
                    else -> PaperStatus.OK
                }
            }
            
            PaperStatus.Unknown
            
        } catch (e: Exception) {
            println("[PRINTER] ERROR checking paper: ${e.message}")
            PaperStatus.Unknown
        }
    }
    
    // ========================================================================
    // Failed Job Management
    // ========================================================================
    
    override fun getFailedPrintJobs(): List<FailedPrintJob> {
        return failedJobs.toList()
    }
    
    override suspend fun retryPrintJob(jobId: String): PrintResult {
        val job = failedJobs.find { it.id == jobId }
            ?: return PrintResult.Error(
                errorCode = PrintErrorCode.UNKNOWN,
                message = "Job not found: $jobId",
                isRecoverable = false
            )
        
        val result = printReceipt(job.receipt)
        
        if (result is PrintResult.Success) {
            failedJobs.removeAll { it.id == jobId }
        } else {
            // Update retry count
            val index = failedJobs.indexOfFirst { it.id == jobId }
            if (index >= 0) {
                failedJobs[index] = job.copy(retryCount = job.retryCount + 1)
            }
        }
        
        return result
    }
    
    override fun clearFailedPrintJob(jobId: String) {
        failedJobs.removeAll { it.id == jobId }
    }
    
    private fun storeFailedJob(receipt: Receipt, errorCode: PrintErrorCode) {
        failedJobs.add(FailedPrintJob(
            id = UUID.randomUUID().toString(),
            receipt = receipt,
            errorCode = errorCode,
            failedAt = Clock.System.now().toString(),
            retryCount = 0
        ))
    }
    
    // ========================================================================
    // ESC/POS Command Building
    // ========================================================================
    
    private fun buildReceiptCommands(receipt: Receipt): ByteArray {
        val output = mutableListOf<Byte>()
        
        // Initialize
        output.addAll(EscPosCommands.INITIALIZE.toList())
        
        // Center align for header
        output.addAll(EscPosCommands.ALIGN_CENTER.toList())
        
        // Bold for store name
        output.addAll(EscPosCommands.BOLD_ON.toList())
        output.addAll(receipt.header.toByteArray(config.charset).toList())
        output.addAll(EscPosCommands.BOLD_OFF.toList())
        output.add(EscPosCommands.LF)
        output.add(EscPosCommands.LF)
        
        // Left align for items
        output.addAll(EscPosCommands.ALIGN_LEFT.toList())
        
        // Print items
        for (item in receipt.items) {
            val line = formatItemLine(item)
            output.addAll(line.toByteArray(config.charset).toList())
            output.add(EscPosCommands.LF)
        }
        
        output.add(EscPosCommands.LF)
        
        // Totals
        output.addAll(receipt.totals.toByteArray(config.charset).toList())
        output.add(EscPosCommands.LF)
        output.add(EscPosCommands.LF)
        
        // Payments
        output.addAll(receipt.payments.toByteArray(config.charset).toList())
        output.add(EscPosCommands.LF)
        output.add(EscPosCommands.LF)
        
        // Barcode (if present)
        receipt.barcode?.let { barcode ->
            output.addAll(EscPosCommands.ALIGN_CENTER.toList())
            output.addAll(buildBarcodeCommands(barcode))
            output.add(EscPosCommands.LF)
        }
        
        // Footer
        output.addAll(EscPosCommands.ALIGN_CENTER.toList())
        output.addAll(receipt.footer.toByteArray(config.charset).toList())
        output.add(EscPosCommands.LF)
        output.add(EscPosCommands.LF)
        output.add(EscPosCommands.LF)
        
        // Cut paper
        output.addAll(EscPosCommands.PARTIAL_CUT.toList())
        
        return output.toByteArray()
    }
    
    private fun formatItemLine(item: ReceiptItem): String {
        // Format: "Product Name            $10.00"
        val nameWidth = config.lineWidth - item.lineTotal.length - 1
        val truncatedName = if (item.name.length > nameWidth) {
            item.name.take(nameWidth - 3) + "..."
        } else {
            item.name
        }
        
        val padding = " ".repeat((nameWidth - truncatedName.length).coerceAtLeast(0))
        return "$truncatedName$padding ${item.lineTotal}${item.taxIndicator}"
    }
    
    private fun buildBarcodeCommands(data: String): List<Byte> {
        val commands = mutableListOf<Byte>()
        
        // Set barcode height (64 dots)
        commands.addAll(byteArrayOf(0x1D, 0x68, 0x40).toList())
        
        // Set barcode width (2 = medium)
        commands.addAll(byteArrayOf(0x1D, 0x77, 0x02).toList())
        
        // Print text below barcode
        commands.addAll(byteArrayOf(0x1D, 0x48, 0x02).toList())
        
        // Print CODE128 barcode
        commands.addAll(byteArrayOf(0x1D, 0x6B, 0x49).toList())
        commands.add((data.length + 2).toByte())  // Length
        commands.add(0x7B.toByte())  // Code set selector
        commands.add(0x42.toByte())  // Code B
        commands.addAll(data.toByteArray().toList())
        
        return commands
    }
    
    // ========================================================================
    // Serial Port Management
    // ========================================================================
    
    private fun findPrinterPort(): SerialPort? {
        if (config.portName.isNotBlank()) {
            return SerialPort.getCommPort(config.portName)
        }
        
        val availablePorts = SerialPort.getCommPorts()
        
        if (availablePorts.isEmpty()) {
            println("[PRINTER] No serial ports available")
            return null
        }
        
        availablePorts.forEach { port ->
            println("[PRINTER] Found port: ${port.systemPortName} - ${port.descriptivePortName}")
        }
        
        // Try to find printer by common descriptors
        val printerPort = availablePorts.find { port ->
            val desc = port.descriptivePortName.lowercase()
            desc.contains("printer") ||
            desc.contains("epson") ||
            desc.contains("star") ||
            desc.contains("thermal")
        }
        
        return printerPort ?: availablePorts.firstOrNull()
    }
    
    private fun configurePort(port: SerialPort) {
        port.baudRate = config.baudRate
        port.numDataBits = 8
        port.numStopBits = SerialPort.ONE_STOP_BIT
        port.parity = SerialPort.NO_PARITY
        port.setComPortTimeouts(
            SerialPort.TIMEOUT_WRITE_BLOCKING,
            0,
            config.writeTimeoutMs
        )
    }
    
    private fun sendCommand(command: ByteArray): Boolean {
        val port = serialPort ?: return false
        
        if (!port.isOpen) {
            return false
        }
        
        val bytesWritten = port.writeBytes(command, command.size)
        return bytesWritten == command.size
    }
    
    /**
     * Lists all available serial ports.
     */
    fun getAvailablePorts(): List<PrinterPortInfo> {
        return SerialPort.getCommPorts().map { port ->
            PrinterPortInfo(
                name = port.systemPortName,
                description = port.descriptivePortName
            )
        }
    }
}

/**
 * Configuration for the ESC/POS printer.
 */
data class PrinterConfig(
    /** COM port name (e.g., "COM4" on Windows, "/dev/ttyUSB1" on Linux) */
    val portName: String = "",
    
    /** Baud rate (typically 9600, 19200, or 115200) */
    val baudRate: Int = 9600,
    
    /** Character set for text encoding */
    val charset: Charset = Charsets.UTF_8,
    
    /** Receipt line width in characters (42 for 80mm paper) */
    val lineWidth: Int = 42,
    
    /** Write timeout in milliseconds */
    val writeTimeoutMs: Int = 3000
)

/**
 * Information about an available printer port.
 */
data class PrinterPortInfo(
    val name: String,
    val description: String
)

/**
 * ESC/POS command constants.
 * 
 * Per DESKTOP_HARDWARE.md: Standard ESC/POS command set.
 */
object EscPosCommands {
    /** Line feed */
    const val LF: Byte = 0x0A
    
    /** Initialize printer */
    val INITIALIZE = byteArrayOf(0x1B, 0x40)
    
    /** Select center justification */
    val ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01)
    
    /** Select left justification */
    val ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00)
    
    /** Select right justification */
    val ALIGN_RIGHT = byteArrayOf(0x1B, 0x61, 0x02)
    
    /** Bold on */
    val BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01)
    
    /** Bold off */
    val BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00)
    
    /** Double height on */
    val DOUBLE_HEIGHT_ON = byteArrayOf(0x1B, 0x21, 0x10)
    
    /** Double height off */
    val DOUBLE_HEIGHT_OFF = byteArrayOf(0x1B, 0x21, 0x00)
    
    /** Partial cut */
    val PARTIAL_CUT = byteArrayOf(0x1D, 0x56, 0x01)
    
    /** Full cut */
    val FULL_CUT = byteArrayOf(0x1D, 0x56, 0x00)
    
    /** Open cash drawer (pulse pin 2) */
    val OPEN_DRAWER = byteArrayOf(0x1B, 0x70, 0x00, 0x19, 0xFA.toByte())
    
    /** Status request */
    val STATUS_REQUEST = byteArrayOf(0x10, 0x04, 0x01)
}

