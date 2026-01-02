package com.unisight.gropos.features.auth.data

import com.unisight.gropos.features.auth.domain.hardware.NfcResult
import com.unisight.gropos.features.auth.domain.hardware.NfcScanner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Simulated NFC scanner for development and testing.
 *
 * Behavior:
 * - Delays 2 seconds to simulate "bringing card to reader"
 * - Returns Success("9999") which maps to the Manager's badge ID
 * - Supports cancellation via cancelScan()
 *
 * Per AUTHENTICATION.md: Badge token (9999) can be used as a PIN
 * for the Manager user in FakeAuthRepository.
 *
 * Thread-safe: Uses Mutex to ensure only one scan operation at a time.
 */
class SimulatedNfcScanner : NfcScanner {
    
    companion object {
        /**
         * Simulated delay to mimic user bringing badge to reader.
         */
        private const val SCAN_DELAY_MS = 2000L
        
        /**
         * Manager's badge token.
         * This token maps to PIN "9999" in the employee system.
         */
        private const val MANAGER_BADGE_TOKEN = "9999"
    }
    
    /**
     * Mutex to ensure thread-safe access to scan state.
     */
    private val scanMutex = Mutex()
    
    /**
     * Flag to track if scan should be cancelled.
     */
    @Volatile
    private var isCancelled = false
    
    /**
     * Starts a simulated NFC scan.
     *
     * Waits 2 seconds, then returns the Manager's badge token.
     * Can be cancelled via [cancelScan].
     */
    override suspend fun startScan(): NfcResult {
        return scanMutex.withLock {
            isCancelled = false
            
            try {
                // Simulate user bringing badge to reader
                delay(SCAN_DELAY_MS)
                
                // Check if cancelled during delay
                if (isCancelled) {
                    NfcResult.Cancelled
                } else {
                    NfcResult.Success(MANAGER_BADGE_TOKEN)
                }
            } catch (e: CancellationException) {
                // Coroutine was cancelled externally
                NfcResult.Cancelled
            }
        }
    }
    
    /**
     * Cancels an ongoing scan operation.
     *
     * If a scan is in progress, it will return [NfcResult.Cancelled].
     */
    override fun cancelScan() {
        isCancelled = true
    }
}
