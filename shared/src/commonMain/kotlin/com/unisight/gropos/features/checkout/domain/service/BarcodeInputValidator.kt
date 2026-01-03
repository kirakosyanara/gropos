package com.unisight.gropos.features.checkout.domain.service

/**
 * Validates and sanitizes barcode input from hardware scanners.
 * 
 * **Per QA Audit Finding (CRITICAL):**
 * - Scanner buffer overflow risk from garbage data
 * - No input sanitization on raw barcode strings
 * - Rapid-fire scans can flood the system
 * 
 * **Security Measures:**
 * 1. **Sanitization:** Strips all control characters (0x00-0x1F)
 * 2. **Length Validation:** Rejects codes < 3 or > 128 characters
 * 3. **Rate Limiting:** Ignores scans within 50ms of previous scan
 * 
 * **Why these limits:**
 * - 3 chars minimum: Shortest valid code (PLU codes can be 3-4 digits)
 * - 128 chars maximum: Longest standard is Code 128 extended (100+ chars)
 * - 50ms rate limit: Human cannot scan faster; prevents scanner malfunction floods
 * 
 * Per code-quality.mdc: Pure functions where possible, explicit state for rate limiting.
 */
class BarcodeInputValidator {
    
    companion object {
        /** Minimum valid barcode length */
        const val MIN_LENGTH = 3
        
        /** Maximum valid barcode length (prevents buffer attacks) */
        const val MAX_LENGTH = 128
        
        /** Minimum milliseconds between scans (rate limiting) */
        const val RATE_LIMIT_MS = 50L
    }
    
    /**
     * Timestamp of last successful (non-rate-limited) scan.
     * Used for rate limiting to prevent scanner malfunction floods.
     * 
     * Null indicates no scan has occurred yet (first scan always passes).
     */
    private var lastScanTimestamp: Long? = null
    
    /**
     * Sanitizes raw scanner input by removing unsafe characters.
     * 
     * Removes:
     * - Control characters (0x00 - 0x1F): Null, escape, newline, tab, etc.
     * - Leading/trailing whitespace
     * 
     * Preserves:
     * - Alphanumeric characters (0-9, A-Z, a-z)
     * - Common barcode punctuation (-, ., /, +, %, $, #, @, &, *, etc.)
     * 
     * @param raw The raw input string from the scanner hardware
     * @return Sanitized string safe for processing
     */
    fun sanitize(raw: String): String {
        return raw
            // Remove all control characters (0x00 - 0x1F)
            .filter { char -> char.code >= 0x20 }
            // Trim leading/trailing whitespace
            .trim()
    }
    
    /**
     * Validates that the barcode length is within acceptable bounds.
     * 
     * @param code The sanitized barcode string
     * @return true if length is between [MIN_LENGTH] and [MAX_LENGTH] inclusive
     */
    fun isValidLength(code: String): Boolean {
        return code.length in MIN_LENGTH..MAX_LENGTH
    }
    
    /**
     * Checks if the current scan should be rate-limited.
     * 
     * Rate limiting prevents:
     * - Scanner malfunction flooding (rapid-fire garbage data)
     * - Accidental double-scans from trigger bounce
     * - Denial of service from compromised scanner
     * 
     * @param currentTimestamp The timestamp of the current scan attempt
     * @return true if the scan should be blocked (too fast), false if allowed
     */
    fun shouldRateLimit(currentTimestamp: Long): Boolean {
        val lastScan = lastScanTimestamp
        
        // First scan ever - always allow
        if (lastScan == null) {
            lastScanTimestamp = currentTimestamp
            return false
        }
        
        val timeSinceLastScan = currentTimestamp - lastScan
        
        if (timeSinceLastScan < RATE_LIMIT_MS) {
            // Too fast - rate limit this scan (do NOT update timestamp)
            return true
        }
        
        // Scan allowed - update the timestamp
        lastScanTimestamp = currentTimestamp
        return false
    }
    
    /**
     * Full validation pipeline for barcode input.
     * 
     * Order of operations:
     * 1. Rate limit check (fastest rejection)
     * 2. Sanitization (remove control chars)
     * 3. Empty check (post-sanitization)
     * 4. Length validation (min/max bounds)
     * 
     * @param raw The raw input string from scanner hardware
     * @param timestamp The timestamp of this scan (for rate limiting)
     * @return [ValidationResult] indicating success or failure reason
     */
    fun validate(raw: String, timestamp: Long): ValidationResult {
        // Step 1: Rate limit check (fastest rejection path)
        if (shouldRateLimit(timestamp)) {
            return ValidationResult.RateLimited
        }
        
        // Step 2: Sanitize input
        val sanitized = sanitize(raw)
        
        // Step 3: Check for empty result
        if (sanitized.isEmpty()) {
            return ValidationResult.Empty
        }
        
        // Step 4: Check length bounds
        if (sanitized.length < MIN_LENGTH) {
            return ValidationResult.TooShort
        }
        
        if (sanitized.length > MAX_LENGTH) {
            return ValidationResult.TooLong
        }
        
        // All checks passed
        return ValidationResult.Valid(code = sanitized)
    }
    
    /**
     * Resets the rate limiting state.
     * 
     * Call this when:
     * - Scanner is restarted
     * - User navigates away and back
     * - Testing requires fresh state
     */
    fun resetRateLimit() {
        lastScanTimestamp = null
    }
}

/**
 * Result of barcode validation.
 * 
 * Sealed class ensures all rejection reasons are explicitly handled.
 * Per code-quality.mdc: Use sealed classes for exhaustive when expressions.
 */
sealed class ValidationResult {
    
    /**
     * Barcode passed all validation checks.
     * 
     * @property code The sanitized, validated barcode string
     */
    data class Valid(val code: String) : ValidationResult()
    
    /**
     * Barcode was empty after sanitization.
     * 
     * This occurs when input contains only control characters or whitespace.
     */
    data object Empty : ValidationResult()
    
    /**
     * Barcode was shorter than minimum length after sanitization.
     * 
     * Minimum: [BarcodeInputValidator.MIN_LENGTH] characters
     */
    data object TooShort : ValidationResult()
    
    /**
     * Barcode exceeded maximum allowed length.
     * 
     * Maximum: [BarcodeInputValidator.MAX_LENGTH] characters
     * This prevents buffer overflow attacks.
     */
    data object TooLong : ValidationResult()
    
    /**
     * Scan was blocked due to rate limiting.
     * 
     * Minimum interval: [BarcodeInputValidator.RATE_LIMIT_MS] milliseconds
     * This prevents scanner malfunction floods.
     */
    data object RateLimited : ValidationResult()
}

