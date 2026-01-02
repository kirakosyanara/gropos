package com.unisight.gropos.features.auth.domain.hardware

/**
 * Hardware abstraction for NFC/RFID badge readers.
 *
 * Per ANDROID_HARDWARE_GUIDE.md: NFC reader emits token (badge ID) which app uses
 * for authentication. This interface provides a platform-agnostic way to interact
 * with NFC hardware.
 *
 * Implementations:
 * - SimulatedNfcScanner: Development/testing (returns mock token after delay)
 * - SunmiNfcScanner: Sunmi Android devices with built-in NFC
 * - AndroidNfcScanner: Generic Android devices using NfcAdapter
 * - DesktopNfcScanner: USB NFC readers via serial/HID
 */
interface NfcScanner {
    
    /**
     * Starts listening for NFC badge tap.
     *
     * This is a suspending function that blocks until:
     * - A badge is successfully read -> Returns [NfcResult.Success]
     * - An error occurs -> Returns [NfcResult.Error]
     * - The scan is cancelled -> Returns [NfcResult.Cancelled]
     *
     * The UI should remain responsive during the scan; this should be called
     * from a coroutine scope.
     *
     * @return [NfcResult] indicating the outcome of the scan operation
     */
    suspend fun startScan(): NfcResult
    
    /**
     * Cancels an ongoing scan operation.
     *
     * If a scan is in progress (startScan is suspended), this will cause
     * startScan to return [NfcResult.Cancelled].
     *
     * Safe to call even if no scan is in progress (no-op in that case).
     */
    fun cancelScan()
}

/**
 * Result of an NFC badge scan operation.
 *
 * Per AUTHENTICATION.md: Badge token is used for employee authentication.
 * The token is typically a unique identifier stored on the NFC badge.
 */
sealed class NfcResult {
    
    /**
     * Badge successfully read.
     *
     * @param token The badge ID/token string read from the NFC tag.
     *              This is typically a hexadecimal string representation
     *              of the tag's UID (e.g., "9999" for Manager badge).
     */
    data class Success(val token: String) : NfcResult()
    
    /**
     * Error during scan operation.
     *
     * @param message Human-readable error description for debugging.
     *                Should NOT be shown directly to users.
     */
    data class Error(val message: String) : NfcResult()
    
    /**
     * Scan was cancelled by user action.
     *
     * This occurs when [NfcScanner.cancelScan] is called while a scan
     * is in progress (e.g., user presses Cancel button in UI).
     */
    data object Cancelled : NfcResult()
}

