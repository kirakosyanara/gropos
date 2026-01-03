package com.unisight.gropos.core.hardware.scanner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.unisight.gropos.features.checkout.domain.repository.ScannerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Android implementation of ScannerRepository for Sunmi hardware scanner.
 * 
 * **Per ANDROID_HARDWARE_GUIDE.md:**
 * - Uses Sunmi's broadcast intent for hardware scanner events
 * - Works with built-in laser scanners on V2 Pro, T2, etc.
 * - Triggered by hardware scan button on device
 * 
 * **Broadcast Intent:**
 * Sunmi devices send `com.sunmi.scanner.ACTION_DATA_CODE_RECEIVED`
 * with barcode data in the "data" extra.
 * 
 * **Usage:**
 * 1. Call startScanning() to register receiver
 * 2. Collect scannedCodes Flow
 * 3. Call stopScanning() when leaving scan context
 * 
 * Per testing-strategy.mdc: For testing, use FakeScannerRepository.
 */
class SunmiHardwareScanner(
    private val context: Context
) : ScannerRepository {
    
    companion object {
        // Sunmi scanner broadcast action
        private const val ACTION_SCAN_RECEIVED = "com.sunmi.scanner.ACTION_DATA_CODE_RECEIVED"
        private const val EXTRA_BARCODE_DATA = "data"
        private const val EXTRA_SOURCE = "source_byte"
    }
    
    private var _isActive = false
    private var isReceiverRegistered = false
    
    private val _scannedCodes = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 16
    )
    override val scannedCodes: Flow<String> = _scannedCodes.asSharedFlow()
    
    override val isActive: Boolean
        get() = _isActive
    
    // Debounce duplicate scans
    private var lastScannedCode: String? = null
    private var lastScanTime: Long = 0
    private val scanCooldownMs = 300L
    
    // ========================================================================
    // Broadcast Receiver
    // ========================================================================
    
    /**
     * BroadcastReceiver for Sunmi scanner intents.
     */
    private val scanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != ACTION_SCAN_RECEIVED) return
            
            val barcode = intent.getStringExtra(EXTRA_BARCODE_DATA)
            
            if (barcode.isNullOrBlank()) {
                println("[SUNMI_SCANNER] Empty barcode received")
                return
            }
            
            processScan(barcode)
        }
    }
    
    // ========================================================================
    // ScannerRepository Implementation
    // ========================================================================
    
    override suspend fun startScanning() {
        if (isReceiverRegistered) {
            println("[SUNMI_SCANNER] Already registered")
            return
        }
        
        try {
            val filter = IntentFilter(ACTION_SCAN_RECEIVED)
            context.registerReceiver(scanReceiver, filter)
            isReceiverRegistered = true
            _isActive = true
            println("[SUNMI_SCANNER] Receiver registered")
        } catch (e: Exception) {
            println("[SUNMI_SCANNER] Failed to register: ${e.message}")
        }
    }
    
    override suspend fun stopScanning() {
        if (!isReceiverRegistered) {
            println("[SUNMI_SCANNER] Not registered")
            return
        }
        
        try {
            context.unregisterReceiver(scanReceiver)
            isReceiverRegistered = false
            _isActive = false
            lastScannedCode = null
            println("[SUNMI_SCANNER] Receiver unregistered")
        } catch (e: Exception) {
            println("[SUNMI_SCANNER] Failed to unregister: ${e.message}")
        }
    }
    
    // ========================================================================
    // Scan Processing
    // ========================================================================
    
    private fun processScan(barcode: String) {
        // Validate length
        if (barcode.length < 3 || barcode.length > 128) {
            println("[SUNMI_SCANNER] Invalid barcode length: ${barcode.length}")
            return
        }
        
        // Deduplicate rapid scans
        val now = System.currentTimeMillis()
        if (barcode == lastScannedCode && (now - lastScanTime) < scanCooldownMs) {
            println("[SUNMI_SCANNER] Duplicate scan ignored")
            return
        }
        
        lastScannedCode = barcode
        lastScanTime = now
        
        // Emit barcode
        val emitted = _scannedCodes.tryEmit(barcode)
        
        if (emitted) {
            println("[SUNMI_SCANNER] Barcode: $barcode")
        } else {
            println("[SUNMI_SCANNER] Buffer full, barcode dropped")
        }
    }
}

