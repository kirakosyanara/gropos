package com.unisight.gropos.features.checkout.data

import com.unisight.gropos.features.checkout.domain.repository.ScannerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Fake implementation of ScannerRepository for development and testing.
 * 
 * Allows programmatic simulation of barcode scans via [emitScan].
 * 
 * Per testing-strategy.mdc: "Use Fakes for State" - this fake
 * allows tests to simulate scanner events programmatically.
 * 
 * Per code-quality.mdc: Use Flow for reactive streams, not callbacks.
 * 
 * TODO: Replace with platform-specific implementations:
 * - Desktop: JavaPOSScannerRepository (USB/Serial scanner)
 * - Android: SunmiScannerRepository or CameraScannerRepository
 */
class FakeScannerRepository : ScannerRepository {
    
    /**
     * Shared flow for emitting scanned codes.
     * 
     * Uses replay=0 so only active collectors receive events.
     * Uses extraBufferCapacity=1 to handle rapid scans.
     */
    private val _scannedCodes = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 1
    )
    
    override val scannedCodes: Flow<String> = _scannedCodes.asSharedFlow()
    
    private var _isActive = false
    
    override val isActive: Boolean
        get() = _isActive
    
    override suspend fun startScanning() {
        _isActive = true
    }
    
    override suspend fun stopScanning() {
        _isActive = false
    }
    
    /**
     * Simulates a barcode scan event.
     * 
     * Call this method to emit a scan event as if a physical
     * barcode scanner had read a code.
     * 
     * @param sku The barcode/SKU that was "scanned"
     */
    suspend fun emitScan(sku: String) {
        _scannedCodes.emit(sku)
    }
    
    /**
     * Tries to emit a scan without suspending.
     * 
     * @param sku The barcode/SKU to emit
     * @return true if the emission was successful
     */
    fun tryEmitScan(sku: String): Boolean {
        return _scannedCodes.tryEmit(sku)
    }
}

