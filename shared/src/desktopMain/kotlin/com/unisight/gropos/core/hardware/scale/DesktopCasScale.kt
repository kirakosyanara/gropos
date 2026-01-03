package com.unisight.gropos.core.hardware.scale

import com.fazecast.jSerialComm.SerialPort
import com.fazecast.jSerialComm.SerialPortDataListener
import com.fazecast.jSerialComm.SerialPortEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.math.BigDecimal

/**
 * Desktop implementation of ScaleService for CAS PD-II scales.
 * 
 * **Per DESKTOP_HARDWARE.md:**
 * - Uses jSerialComm for USB/Serial scale communication
 * - Supports CAS PD-II protocol (18-byte ASCII frame)
 * - Handles cable disconnects gracefully
 * 
 * **Supported Scales:**
 * - CAS PD-II series
 * - Compatible scales using CAS protocol
 * 
 * **Communication:**
 * - Default baud rate: 9600
 * - 8 data bits, 1 stop bit, no parity
 * - Scale continuously streams weight data
 * 
 * **Thread Safety:**
 * - Serial port reading happens on jSerialComm's background thread
 * - State updates via StateFlow are thread-safe
 * 
 * Per reliability-stability.mdc: All operations return sealed results, never throw.
 * Per testing-strategy.mdc: Use SimulatedScaleService for unit tests.
 */
class DesktopCasScale(
    private val config: ScaleConfig = ScaleConfig()
) : ScaleService {
    
    // ========================================================================
    // State
    // ========================================================================
    
    private var serialPort: SerialPort? = null
    private var buffer = ByteArray(0)
    
    private val _currentWeight = MutableStateFlow(BigDecimal.ZERO)
    override val currentWeight: StateFlow<BigDecimal> = _currentWeight.asStateFlow()
    
    private val _status = MutableStateFlow(ScaleStatus.Disconnected)
    override val status: StateFlow<ScaleStatus> = _status.asStateFlow()
    
    private val _isStable = MutableStateFlow(false)
    override val isStable: StateFlow<Boolean> = _isStable.asStateFlow()
    
    // ========================================================================
    // Connection Management
    // ========================================================================
    
    /**
     * Connects to the CAS scale via serial port.
     * 
     * **Connection Process:**
     * 1. Find or use configured port
     * 2. Configure serial parameters
     * 3. Open port and attach data listener
     * 4. Wait for first valid weight frame
     * 
     * @return ScaleResult.Success if connected, ScaleResult.Error otherwise
     */
    override suspend fun connect(): ScaleResult = withContext(Dispatchers.IO) {
        if (_status.value == ScaleStatus.Connected) {
            return@withContext ScaleResult.Success
        }
        
        _status.value = ScaleStatus.Connecting
        
        try {
            val port = findScalePort()
            
            if (port == null) {
                _status.value = ScaleStatus.Disconnected
                return@withContext ScaleResult.Error("No scale port available")
            }
            
            serialPort = port
            configurePort(port)
            
            if (!port.openPort()) {
                _status.value = ScaleStatus.Error
                return@withContext ScaleResult.Error(
                    "Failed to open port ${port.systemPortName}"
                )
            }
            
            // Attach data listener for continuous weight updates
            attachDataListener(port)
            
            // Wait for first valid frame (with timeout)
            val connected = withTimeoutOrNull(config.connectionTimeoutMs) {
                waitForFirstFrame()
            }
            
            if (connected == true) {
                _status.value = ScaleStatus.Connected
                println("[SCALE] Connected to ${port.systemPortName}")
                ScaleResult.Success
            } else {
                port.closePort()
                _status.value = ScaleStatus.Error
                ScaleResult.Error("Scale not responding on ${port.systemPortName}")
            }
            
        } catch (e: Exception) {
            _status.value = ScaleStatus.Error
            ScaleResult.Error(e.message ?: "Connection failed")
        }
    }
    
    /**
     * Disconnects from the scale and releases resources.
     */
    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            serialPort?.let { port ->
                port.removeDataListener()
                port.closePort()
            }
            serialPort = null
            buffer = ByteArray(0)
            _status.value = ScaleStatus.Disconnected
            _currentWeight.value = BigDecimal.ZERO
            _isStable.value = false
            println("[SCALE] Disconnected")
        } catch (e: Exception) {
            println("[SCALE] ERROR disconnecting: ${e.message}")
        }
    }
    
    // ========================================================================
    // Scale Operations
    // ========================================================================
    
    /**
     * Zeros (tares) the scale.
     * 
     * **CAS Protocol:**
     * Send 'Z' command to zero the scale.
     * 
     * @return ScaleResult indicating success or failure
     */
    override suspend fun zero(): ScaleResult = withContext(Dispatchers.IO) {
        val port = serialPort
        
        if (port == null || !port.isOpen) {
            return@withContext ScaleResult.Error("Scale not connected")
        }
        
        if (_status.value != ScaleStatus.Connected) {
            return@withContext ScaleResult.Error("Scale not ready")
        }
        
        try {
            // Send zero command (ASCII 'Z')
            val zeroCommand = byteArrayOf('Z'.code.toByte(), 0x0D)
            val bytesWritten = port.writeBytes(zeroCommand, zeroCommand.size)
            
            if (bytesWritten != zeroCommand.size) {
                return@withContext ScaleResult.Error("Failed to send zero command")
            }
            
            // Wait for scale to stabilize
            _isStable.value = false
            delay(config.zeroSettleTimeMs)
            
            println("[SCALE] Zero/Tare sent")
            ScaleResult.Success
            
        } catch (e: Exception) {
            ScaleResult.Error("Zero failed: ${e.message}")
        }
    }
    
    /**
     * Gets the current weight reading.
     * 
     * **Behavior:**
     * - If not connected, returns NotConnected
     * - If overweight, returns Overweight
     * - Returns last known weight with stability flag
     * 
     * @return WeightResult with weight data or error
     */
    override suspend fun getWeight(): WeightResult = withContext(Dispatchers.IO) {
        val port = serialPort
        
        if (port == null || !port.isOpen) {
            return@withContext WeightResult.NotConnected
        }
        
        when (_status.value) {
            ScaleStatus.Disconnected -> return@withContext WeightResult.NotConnected
            ScaleStatus.Overweight -> return@withContext WeightResult.Overweight
            ScaleStatus.Error -> return@withContext WeightResult.Error("Scale error")
            else -> { /* Continue */ }
        }
        
        // If not stable, wait briefly for stability
        if (!_isStable.value) {
            delay(config.stabilityWaitMs)
        }
        
        WeightResult.Success(
            weight = _currentWeight.value,
            isStable = _isStable.value
        )
    }
    
    // ========================================================================
    // Serial Port Management
    // ========================================================================
    
    /**
     * Finds the scale serial port.
     * 
     * Priority:
     * 1. Use explicitly configured port name
     * 2. Auto-detect by common scale descriptors
     */
    private fun findScalePort(): SerialPort? {
        if (config.portName.isNotBlank()) {
            return SerialPort.getCommPort(config.portName)
        }
        
        val availablePorts = SerialPort.getCommPorts()
        
        if (availablePorts.isEmpty()) {
            println("[SCALE] No serial ports available")
            return null
        }
        
        availablePorts.forEach { port ->
            println("[SCALE] Found port: ${port.systemPortName} - ${port.descriptivePortName}")
        }
        
        // Try to find scale by common descriptors
        val scalePort = availablePorts.find { port ->
            val desc = port.descriptivePortName.lowercase()
            desc.contains("scale") ||
            desc.contains("cas") ||
            desc.contains("weight") ||
            desc.contains("usb serial")
        }
        
        return scalePort ?: availablePorts.firstOrNull()
    }
    
    /**
     * Configures serial port parameters for CAS scale.
     */
    private fun configurePort(port: SerialPort) {
        port.baudRate = config.baudRate
        port.numDataBits = 8
        port.numStopBits = SerialPort.ONE_STOP_BIT
        port.parity = SerialPort.NO_PARITY
        port.setComPortTimeouts(
            SerialPort.TIMEOUT_READ_SEMI_BLOCKING,
            config.readTimeoutMs.toInt(),
            0
        )
    }
    
    /**
     * Attaches data listener for incoming scale data.
     */
    private fun attachDataListener(port: SerialPort) {
        port.addDataListener(object : SerialPortDataListener {
            
            override fun getListeningEvents(): Int {
                return SerialPort.LISTENING_EVENT_DATA_AVAILABLE or
                       SerialPort.LISTENING_EVENT_PORT_DISCONNECTED
            }
            
            override fun serialEvent(event: SerialPortEvent) {
                when (event.eventType) {
                    SerialPort.LISTENING_EVENT_DATA_AVAILABLE -> {
                        handleDataAvailable(port)
                    }
                    SerialPort.LISTENING_EVENT_PORT_DISCONNECTED -> {
                        handleDisconnect()
                    }
                }
            }
        })
    }
    
    /**
     * Handles incoming data from the scale.
     */
    private fun handleDataAvailable(port: SerialPort) {
        val bytesAvailable = port.bytesAvailable()
        if (bytesAvailable <= 0) return
        
        val newData = ByteArray(bytesAvailable)
        port.readBytes(newData, bytesAvailable)
        
        // Append to buffer
        buffer = buffer + newData
        
        // Try to extract and parse complete frames
        processBuffer()
    }
    
    /**
     * Processes the buffer to extract complete frames.
     */
    private fun processBuffer() {
        while (true) {
            val frameResult = CasProtocolParser.extractFrame(buffer)
            
            if (frameResult == null) {
                // No complete frame yet
                
                // Safety: Prevent buffer overflow
                if (buffer.size > 256) {
                    buffer = buffer.takeLast(64).toByteArray()
                }
                break
            }
            
            val (frame, remaining) = frameResult
            buffer = remaining
            
            // Parse the frame
            val parseResult = CasProtocolParser.parse(frame)
            handleParseResult(parseResult)
        }
    }
    
    /**
     * Handles a parsed frame result.
     */
    private fun handleParseResult(result: CasParseResult) {
        when (result) {
            is CasParseResult.Success -> {
                _currentWeight.value = result.weight
                _isStable.value = result.isStable
                
                // Clear overweight/underweight if previously set
                if (_status.value == ScaleStatus.Overweight || 
                    _status.value == ScaleStatus.Underweight) {
                    _status.value = ScaleStatus.Connected
                }
            }
            
            is CasParseResult.Overweight -> {
                _status.value = ScaleStatus.Overweight
                _isStable.value = false
            }
            
            is CasParseResult.Underweight -> {
                _status.value = ScaleStatus.Underweight
                _isStable.value = false
            }
            
            is CasParseResult.InvalidFrame -> {
                // Log but don't change status - could be noise
                println("[SCALE] Invalid frame: ${result.reason}")
            }
        }
    }
    
    /**
     * Handles port disconnect event.
     */
    private fun handleDisconnect() {
        _status.value = ScaleStatus.Disconnected
        _isStable.value = false
        serialPort = null
        println("[SCALE] Port disconnected")
    }
    
    /**
     * Waits for the first valid weight frame.
     */
    private suspend fun waitForFirstFrame(): Boolean {
        repeat(50) { // 50 x 100ms = 5 seconds max
            if (_currentWeight.value != BigDecimal.ZERO || 
                _status.value == ScaleStatus.Overweight) {
                return true
            }
            delay(100)
        }
        
        // Even if weight is zero, check if we're receiving data
        return _isStable.value
    }
    
    // ========================================================================
    // Port Discovery Utilities
    // ========================================================================
    
    /**
     * Lists all available serial ports.
     * 
     * Useful for settings screen where user selects scale port.
     */
    fun getAvailablePorts(): List<ScalePortInfo> {
        return SerialPort.getCommPorts().map { port ->
            ScalePortInfo(
                name = port.systemPortName,
                description = port.descriptivePortName
            )
        }
    }
}

/**
 * Configuration for the CAS scale connection.
 */
data class ScaleConfig(
    /** COM port name (e.g., "COM5" on Windows, "/dev/ttyUSB0" on Linux) */
    val portName: String = "",
    
    /** Baud rate (typically 9600 for CAS scales) */
    val baudRate: Int = 9600,
    
    /** Timeout for initial connection (ms) */
    val connectionTimeoutMs: Long = 5000,
    
    /** Read timeout (ms) */
    val readTimeoutMs: Long = 100,
    
    /** Time to wait for scale to settle after zero command (ms) */
    val zeroSettleTimeMs: Long = 500,
    
    /** Time to wait for stability before returning weight (ms) */
    val stabilityWaitMs: Long = 300
)

/**
 * Information about an available scale port.
 */
data class ScalePortInfo(
    val name: String,
    val description: String
)

