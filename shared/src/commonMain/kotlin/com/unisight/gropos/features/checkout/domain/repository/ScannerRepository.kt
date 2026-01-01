package com.unisight.gropos.features.checkout.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Hardware abstraction for barcode scanner.
 * 
 * This interface defines the contract for scanner hardware.
 * Per ARCHITECTURE_BLUEPRINT.md: Hardware abstractions use expect/actual pattern.
 * 
 * Implementation Notes:
 * - Desktop: JavaPOS scanner or USB-HID keyboard wedge
 * - Android: Built-in scanner (Sunmi/PAX) or camera-based
 * 
 * Per code-quality.mdc: Use Flow for reactive streams, not callbacks.
 */
interface ScannerRepository {
    
    /**
     * Flow of scanned barcode/SKU strings.
     * 
     * Emits each time a barcode is successfully scanned.
     * Consumers should collect this flow to react to scans.
     * 
     * The flow is hot - it represents real-time scanner events.
     */
    val scannedCodes: Flow<String>
    
    /**
     * Starts the scanner hardware.
     * Call this when entering a scanning context (e.g., checkout screen).
     */
    suspend fun startScanning()
    
    /**
     * Stops the scanner hardware.
     * Call this when leaving a scanning context to conserve resources.
     */
    suspend fun stopScanning()
    
    /**
     * Whether the scanner is currently active and ready to scan.
     */
    val isActive: Boolean
}

