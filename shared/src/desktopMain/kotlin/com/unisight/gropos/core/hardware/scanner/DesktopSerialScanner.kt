package com.unisight.gropos.core.hardware.scanner

import com.fazecast.jSerialComm.SerialPort
import com.fazecast.jSerialComm.SerialPortDataListener
import com.fazecast.jSerialComm.SerialPortEvent
import com.unisight.gropos.features.checkout.domain.repository.ScannerRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import java.nio.charset.StandardCharsets

/**
 * Desktop implementation of ScannerRepository using jSerialComm.
 * 
 * **Per DESKTOP_HARDWARE.md:**
 * - Uses jSerialComm for USB/Serial barcode scanner communication
 * - Works with keyboard wedge scanners configured as COM ports
 * - Works with dedicated serial scanners (Datalogic, Honeywell, etc.)
 * 
 * **Communication Protocol:**
 * Most barcode scanners output scanned data followed by a terminator:
 * - Common terminators: CR (0x0D), LF (0x0A), or CR+LF
 * - Data is typically ASCII or UTF-8 encoded
 * 
 * **Thread Safety:**
 * - Serial port reading happens on jSerialComm's background thread
 * - Emissions to Flow are thread-safe via SharedFlow
 * 
 * Per testing-strategy.mdc: For testing, use FakeScannerRepository or SafeScannerRepository.
 */
class DesktopSerialScanner(
    private val config: ScannerConfig = ScannerConfig()
) : ScannerRepository {
    
    private var serialPort: SerialPort? = null
    private var _isActive = false
    
    /**
     * SharedFlow for emitting scanned barcodes.
     * 
     * replay = 0: New subscribers only see new scans
     * extraBufferCapacity = 16: Buffer for burst scanning
     */
    private val _scannedCodes = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 16
    )
    
    override val scannedCodes: Flow<String> = _scannedCodes.asSharedFlow()
    
    override val isActive: Boolean
        get() = _isActive
    
    // Buffer for accumulating bytes until terminator is received
    private val buffer = StringBuilder()
    
    /**
     * Starts the scanner by opening the configured serial port.
     * 
     * **Port Discovery:**
     * If no port is configured, attempts to auto-detect common scanner ports.
     * 
     * **Error Handling:**
     * If port cannot be opened, logs error but doesn't throw.
     * Scanner will be in inactive state until successful connect.
     */
    override suspend fun startScanning() {
        if (_isActive) {
            println("[SCANNER] Already active")
            return
        }
        
        try {
            val port = findOrOpenPort()
            
            if (port == null) {
                println("[SCANNER] ERROR: No scanner port available")
                return
            }
            
            serialPort = port
            configurePort(port)
            
            if (port.openPort()) {
                attachDataListener(port)
                _isActive = true
                println("[SCANNER] Started on ${port.systemPortName}")
            } else {
                println("[SCANNER] ERROR: Failed to open port ${port.systemPortName}")
            }
            
        } catch (e: Exception) {
            println("[SCANNER] ERROR: ${e.message}")
            _isActive = false
        }
    }
    
    /**
     * Stops the scanner and releases the serial port.
     */
    override suspend fun stopScanning() {
        try {
            serialPort?.let { port ->
                port.removeDataListener()
                port.closePort()
            }
            serialPort = null
            _isActive = false
            buffer.clear()
            println("[SCANNER] Stopped")
        } catch (e: Exception) {
            println("[SCANNER] ERROR stopping: ${e.message}")
        }
    }
    
    // ========================================================================
    // Port Management
    // ========================================================================
    
    /**
     * Finds or opens the configured scanner port.
     * 
     * Priority:
     * 1. Use explicitly configured port name
     * 2. Auto-detect from available ports
     */
    private fun findOrOpenPort(): SerialPort? {
        // If port is explicitly configured, use it
        if (config.portName.isNotBlank()) {
            return SerialPort.getCommPort(config.portName)
        }
        
        // Auto-detect: Look for common scanner device names
        val availablePorts = SerialPort.getCommPorts()
        
        if (availablePorts.isEmpty()) {
            println("[SCANNER] No serial ports available")
            return null
        }
        
        // Log available ports for debugging
        availablePorts.forEach { port ->
            println("[SCANNER] Found port: ${port.systemPortName} - ${port.descriptivePortName}")
        }
        
        // Try to find a scanner by common descriptors
        val scannerPort = availablePorts.find { port ->
            val desc = port.descriptivePortName.lowercase()
            desc.contains("scanner") || 
            desc.contains("barcode") ||
            desc.contains("datalogic") ||
            desc.contains("honeywell") ||
            desc.contains("zebra") ||
            desc.contains("symbol")
        }
        
        return scannerPort ?: availablePorts.firstOrNull()
    }
    
    /**
     * Configures serial port parameters.
     * 
     * Default settings match common barcode scanner configurations:
     * - 9600 baud (most common), configurable up to 115200
     * - 8 data bits
     * - 1 stop bit
     * - No parity
     */
    private fun configurePort(port: SerialPort) {
        port.baudRate = config.baudRate
        port.numDataBits = 8
        port.numStopBits = SerialPort.ONE_STOP_BIT
        port.parity = SerialPort.NO_PARITY
        port.setComPortTimeouts(
            SerialPort.TIMEOUT_READ_SEMI_BLOCKING,
            100,  // Read timeout (ms)
            0     // Write timeout (not used)
        )
    }
    
    /**
     * Attaches the data listener for incoming barcode data.
     */
    private fun attachDataListener(port: SerialPort) {
        port.addDataListener(object : SerialPortDataListener {
            
            override fun getListeningEvents(): Int {
                return SerialPort.LISTENING_EVENT_DATA_AVAILABLE
            }
            
            override fun serialEvent(event: SerialPortEvent) {
                if (event.eventType != SerialPort.LISTENING_EVENT_DATA_AVAILABLE) return
                
                val bytesAvailable = port.bytesAvailable()
                if (bytesAvailable <= 0) return
                
                val data = ByteArray(bytesAvailable)
                port.readBytes(data, bytesAvailable)
                
                processIncomingData(data)
            }
        })
    }
    
    /**
     * Processes incoming serial data and emits complete barcodes.
     * 
     * **Protocol:**
     * - Accumulates bytes in buffer
     * - Detects terminator (CR, LF, or CR+LF)
     * - Emits complete barcode via SharedFlow
     * - Clears buffer after emission
     * 
     * **Safety:**
     * - Strips control characters
     * - Validates barcode length
     */
    private fun processIncomingData(data: ByteArray) {
        val text = String(data, StandardCharsets.UTF_8)
        
        for (char in text) {
            when (char) {
                '\r', '\n' -> {
                    // Terminator received - emit barcode if buffer has content
                    val barcode = buffer.toString().trim()
                    buffer.clear()
                    
                    if (barcode.isNotEmpty() && barcode.length >= 3) {
                        emitBarcode(barcode)
                    }
                }
                else -> {
                    // Accumulate printable characters
                    if (char.code >= 0x20 && char.code < 0x7F) {
                        buffer.append(char)
                    }
                    
                    // Safety: Prevent buffer overflow
                    if (buffer.length > 128) {
                        println("[SCANNER] WARNING: Buffer overflow, clearing")
                        buffer.clear()
                    }
                }
            }
        }
    }
    
    /**
     * Emits a barcode to subscribers.
     */
    private fun emitBarcode(barcode: String) {
        val success = _scannedCodes.tryEmit(barcode)
        
        if (success) {
            println("[SCANNER] Barcode: $barcode")
        } else {
            println("[SCANNER] WARNING: Buffer full, barcode dropped: $barcode")
        }
    }
    
    // ========================================================================
    // Port Discovery Utilities
    // ========================================================================
    
    /**
     * Lists all available serial ports.
     * 
     * Useful for UI settings screen where user selects scanner port.
     */
    fun getAvailablePorts(): List<PortInfo> {
        return SerialPort.getCommPorts().map { port ->
            PortInfo(
                name = port.systemPortName,
                description = port.descriptivePortName
            )
        }
    }
}

/**
 * Configuration for the desktop serial scanner.
 */
data class ScannerConfig(
    /** COM port name (e.g., "COM3" on Windows, "/dev/ttyUSB0" on Linux) */
    val portName: String = "",
    
    /** Baud rate (9600, 19200, 38400, 115200) */
    val baudRate: Int = 9600
)

/**
 * Information about an available serial port.
 */
data class PortInfo(
    val name: String,
    val description: String
)

