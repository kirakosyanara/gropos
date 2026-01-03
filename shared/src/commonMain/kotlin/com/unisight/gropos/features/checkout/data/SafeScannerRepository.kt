package com.unisight.gropos.features.checkout.data

import com.unisight.gropos.features.checkout.domain.repository.ScannerRepository
import com.unisight.gropos.features.checkout.domain.service.BarcodeInputValidator
import com.unisight.gropos.features.checkout.domain.service.ValidationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.transform
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Safety wrapper for ScannerRepository implementations.
 * 
 * **Per QA Audit Finding (CRITICAL):**
 * This decorator applies input validation to ANY scanner implementation:
 * - FakeScannerRepository (development)
 * - DesktopScannerRepository (production USB/serial)
 * - AndroidScannerRepository (production Sunmi/PAX)
 * 
 * **Security Measures Applied:**
 * 1. Sanitization: Strips control characters (0x00-0x1F)
 * 2. Length validation: Rejects < 3 or > 128 chars
 * 3. Rate limiting: Blocks scans within 50ms
 * 
 * **Design Pattern:** Decorator - wraps underlying implementation.
 * 
 * Per code-quality.mdc: Compose safety features, don't modify original.
 */
class SafeScannerRepository(
    private val delegate: ScannerRepository,
    private val validator: BarcodeInputValidator = BarcodeInputValidator(),
    private val clock: () -> Long = { Clock.System.now().toEpochMilliseconds() }
) : ScannerRepository {
    
    /**
     * Flow of rejected scans for audit/debugging.
     * 
     * Emits whenever a scan is rejected due to:
     * - Invalid length (too short/too long)
     * - Rate limiting
     * - Empty after sanitization
     */
    private val _rejectedScans = MutableSharedFlow<RejectedScan>(
        replay = 0,
        extraBufferCapacity = 10
    )
    val rejectedScans: Flow<RejectedScan> = _rejectedScans.asSharedFlow()
    
    /**
     * Validated barcode flow.
     * 
     * Unlike the raw [delegate.scannedCodes], this flow:
     * 1. Sanitizes all input
     * 2. Rejects invalid barcodes (emits to [rejectedScans] instead)
     * 3. Rate-limits rapid scans
     * 
     * Only valid, safe barcodes are emitted.
     */
    override val scannedCodes: Flow<String> = delegate.scannedCodes.transform { raw ->
        val timestamp = clock()
        
        when (val result = validator.validate(raw, timestamp)) {
            is ValidationResult.Valid -> {
                // Log successful validation
                println("[SCANNER] Accepted: '${result.code}' (${result.code.length} chars)")
                emit(result.code)
            }
            
            is ValidationResult.Empty -> {
                val rejected = RejectedScan(
                    raw = raw,
                    reason = RejectionReason.EMPTY,
                    timestamp = Clock.System.now()
                )
                _rejectedScans.tryEmit(rejected)
                println("[SCANNER] Rejected (EMPTY): raw='$raw'")
            }
            
            is ValidationResult.TooShort -> {
                val rejected = RejectedScan(
                    raw = raw,
                    reason = RejectionReason.TOO_SHORT,
                    timestamp = Clock.System.now()
                )
                _rejectedScans.tryEmit(rejected)
                println("[SCANNER] Rejected (TOO_SHORT): raw='$raw' (${raw.length} chars)")
            }
            
            is ValidationResult.TooLong -> {
                val rejected = RejectedScan(
                    raw = raw,
                    reason = RejectionReason.TOO_LONG,
                    timestamp = Clock.System.now()
                )
                _rejectedScans.tryEmit(rejected)
                println("[SCANNER] Rejected (TOO_LONG): raw='${raw.take(50)}...' (${raw.length} chars)")
            }
            
            is ValidationResult.RateLimited -> {
                val rejected = RejectedScan(
                    raw = raw,
                    reason = RejectionReason.RATE_LIMITED,
                    timestamp = Clock.System.now()
                )
                _rejectedScans.tryEmit(rejected)
                // Don't log rate-limited scans to avoid log flooding
            }
        }
    }
    
    override val isActive: Boolean
        get() = delegate.isActive
    
    override suspend fun startScanning() {
        validator.resetRateLimit()
        delegate.startScanning()
        println("[SCANNER] Started with safety wrapper (validation enabled)")
    }
    
    override suspend fun stopScanning() {
        delegate.stopScanning()
        println("[SCANNER] Stopped")
    }
}

/**
 * Represents a rejected scan event for audit logging.
 */
data class RejectedScan(
    /** The raw input that was rejected */
    val raw: String,
    
    /** Why the scan was rejected */
    val reason: RejectionReason,
    
    /** When the rejection occurred */
    val timestamp: Instant
)

/**
 * Reasons a scan can be rejected.
 */
enum class RejectionReason {
    /** Input was empty after sanitization */
    EMPTY,
    
    /** Input was shorter than minimum (3 chars) */
    TOO_SHORT,
    
    /** Input exceeded maximum (128 chars) */
    TOO_LONG,
    
    /** Scan occurred too quickly after previous scan (< 50ms) */
    RATE_LIMITED
}

