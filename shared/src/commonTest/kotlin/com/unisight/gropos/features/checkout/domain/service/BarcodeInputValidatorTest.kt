package com.unisight.gropos.features.checkout.domain.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertNull

/**
 * Unit tests for BarcodeInputValidator.
 * 
 * Per QA Audit Finding:
 * - Scanner buffer overflow risk from garbage data
 * - No input sanitization on raw barcode strings
 * - Rapid-fire scans can flood the system
 * 
 * Per testing-strategy.mdc:
 * - Test edge cases and attack vectors
 * - Use TDD: Tests written BEFORE implementation
 */
class BarcodeInputValidatorTest {
    
    private val validator = BarcodeInputValidator()
    
    // ========================================================================
    // Sanitization Tests - Strip Control Characters
    // ========================================================================
    
    @Test
    fun `sanitize removes null characters`() {
        val raw = "123\u0000456"
        val result = validator.sanitize(raw)
        assertEquals("123456", result)
    }
    
    @Test
    fun `sanitize removes escape sequences`() {
        val raw = "123\u001B[0m456"  // ANSI escape code
        val result = validator.sanitize(raw)
        // Only the ESC character (0x1B) is removed; [, 0, m are printable and remain
        assertEquals("123[0m456", result)
    }
    
    @Test
    fun `sanitize removes newlines and carriage returns`() {
        val raw = "123\n456\r789"
        val result = validator.sanitize(raw)
        assertEquals("123456789", result)
    }
    
    @Test
    fun `sanitize removes tabs`() {
        val raw = "123\t456"
        val result = validator.sanitize(raw)
        assertEquals("123456", result)
    }
    
    @Test
    fun `sanitize removes all control characters (0x00-0x1F)`() {
        // Build a string with all control characters
        val controlChars = (0..31).map { it.toChar() }.joinToString("")
        val raw = "ABC${controlChars}DEF"
        val result = validator.sanitize(raw)
        assertEquals("ABCDEF", result)
    }
    
    @Test
    fun `sanitize preserves valid barcode characters`() {
        val validBarcode = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-."
        val result = validator.sanitize(validBarcode)
        assertEquals(validBarcode, result)
    }
    
    @Test
    fun `sanitize trims leading and trailing whitespace`() {
        val raw = "   123456   "
        val result = validator.sanitize(raw)
        assertEquals("123456", result)
    }
    
    @Test
    fun `sanitize handles empty string`() {
        val result = validator.sanitize("")
        assertEquals("", result)
    }
    
    @Test
    fun `sanitize handles string of only control characters`() {
        val raw = "\u0000\u0001\u0002\u001B"
        val result = validator.sanitize(raw)
        assertEquals("", result)
    }
    
    // ========================================================================
    // Length Validation Tests
    // ========================================================================
    
    @Test
    fun `isValidLength returns false for empty string`() {
        assertFalse(validator.isValidLength(""))
    }
    
    @Test
    fun `isValidLength returns false for 1 character`() {
        assertFalse(validator.isValidLength("A"))
    }
    
    @Test
    fun `isValidLength returns false for 2 characters`() {
        assertFalse(validator.isValidLength("AB"))
    }
    
    @Test
    fun `isValidLength returns true for 3 characters (minimum valid)`() {
        assertTrue(validator.isValidLength("ABC"))
    }
    
    @Test
    fun `isValidLength returns true for 12 characters (UPC-A)`() {
        assertTrue(validator.isValidLength("012345678905"))
    }
    
    @Test
    fun `isValidLength returns true for 13 characters (EAN-13)`() {
        assertTrue(validator.isValidLength("5901234123457"))
    }
    
    @Test
    fun `isValidLength returns true for 128 characters (maximum valid)`() {
        val maxLength = "A".repeat(128)
        assertTrue(validator.isValidLength(maxLength))
    }
    
    @Test
    fun `isValidLength returns false for 129 characters`() {
        val tooLong = "A".repeat(129)
        assertFalse(validator.isValidLength(tooLong))
    }
    
    @Test
    fun `isValidLength returns false for 1000 characters (buffer attack)`() {
        val bufferAttack = "A".repeat(1000)
        assertFalse(validator.isValidLength(bufferAttack))
    }
    
    // ========================================================================
    // Rate Limiting Tests
    // ========================================================================
    
    @Test
    fun `shouldRateLimit returns false for first scan`() {
        val validator = BarcodeInputValidator()
        val now = System.currentTimeMillis()
        
        assertFalse(validator.shouldRateLimit(now))
    }
    
    @Test
    fun `shouldRateLimit returns true for scan within 50ms`() {
        val validator = BarcodeInputValidator()
        val firstScan = System.currentTimeMillis()
        
        // First scan should pass
        assertFalse(validator.shouldRateLimit(firstScan))
        
        // Second scan 10ms later should be rate limited
        assertTrue(validator.shouldRateLimit(firstScan + 10))
    }
    
    @Test
    fun `shouldRateLimit returns true for scan at exactly 49ms`() {
        val validator = BarcodeInputValidator()
        val firstScan = 1000L
        
        assertFalse(validator.shouldRateLimit(firstScan))
        assertTrue(validator.shouldRateLimit(firstScan + 49))
    }
    
    @Test
    fun `shouldRateLimit returns false for scan at exactly 50ms`() {
        val validator = BarcodeInputValidator()
        val firstScan = 1000L
        
        assertFalse(validator.shouldRateLimit(firstScan))
        assertFalse(validator.shouldRateLimit(firstScan + 50))
    }
    
    @Test
    fun `shouldRateLimit returns false for scan after 100ms`() {
        val validator = BarcodeInputValidator()
        val firstScan = 1000L
        
        assertFalse(validator.shouldRateLimit(firstScan))
        assertFalse(validator.shouldRateLimit(firstScan + 100))
    }
    
    @Test
    fun `shouldRateLimit correctly tracks multiple scans`() {
        val validator = BarcodeInputValidator()
        
        // First scan at t=0
        assertFalse(validator.shouldRateLimit(0))
        
        // Second scan at t=60 (passes, updates last scan time to 60)
        assertFalse(validator.shouldRateLimit(60))
        
        // Third scan at t=70 (should be rate limited, only 10ms after previous)
        // Rate-limited scans do NOT update the timestamp
        assertTrue(validator.shouldRateLimit(70))
        
        // Fourth scan at t=120 (passes, 60ms after last successful at t=60)
        assertFalse(validator.shouldRateLimit(120))
    }
    
    // ========================================================================
    // Full Validation Pipeline Tests
    // ========================================================================
    
    @Test
    fun `validate returns Valid for clean barcode`() {
        val validator = BarcodeInputValidator()
        val result = validator.validate("1234567890123", System.currentTimeMillis())
        
        assertTrue(result is ValidationResult.Valid)
        assertEquals("1234567890123", (result as ValidationResult.Valid).code)
    }
    
    @Test
    fun `validate returns Valid after sanitization`() {
        val validator = BarcodeInputValidator()
        val raw = "123\u0000456\n789"
        val result = validator.validate(raw, System.currentTimeMillis())
        
        assertTrue(result is ValidationResult.Valid)
        assertEquals("123456789", (result as ValidationResult.Valid).code)
    }
    
    @Test
    fun `validate returns TooShort for barcode under 3 chars after sanitization`() {
        val validator = BarcodeInputValidator()
        val raw = "AB\u0000"  // Only 2 chars after sanitization
        val result = validator.validate(raw, System.currentTimeMillis())
        
        assertTrue(result is ValidationResult.TooShort)
    }
    
    @Test
    fun `validate returns TooLong for barcode over 128 chars`() {
        val validator = BarcodeInputValidator()
        val raw = "A".repeat(200)
        val result = validator.validate(raw, System.currentTimeMillis())
        
        assertTrue(result is ValidationResult.TooLong)
    }
    
    @Test
    fun `validate returns RateLimited for rapid scans`() {
        val validator = BarcodeInputValidator()
        val timestamp = System.currentTimeMillis()
        
        // First scan passes
        val first = validator.validate("1234567890", timestamp)
        assertTrue(first is ValidationResult.Valid)
        
        // Second scan 10ms later is rate limited
        val second = validator.validate("1234567890", timestamp + 10)
        assertTrue(second is ValidationResult.RateLimited)
    }
    
    @Test
    fun `validate returns Empty for whitespace-only input`() {
        val validator = BarcodeInputValidator()
        val raw = "   \t\n   "
        val result = validator.validate(raw, System.currentTimeMillis())
        
        assertTrue(result is ValidationResult.Empty)
    }
    
    // ========================================================================
    // Attack Vector Tests (Security)
    // ========================================================================
    
    @Test
    fun `validate handles null character injection attack`() {
        val validator = BarcodeInputValidator()
        val attack = "\u0000\u0000\u0000123\u0000\u0000"
        val result = validator.validate(attack, System.currentTimeMillis())
        
        assertTrue(result is ValidationResult.Valid)
        assertEquals("123", (result as ValidationResult.Valid).code)
    }
    
    @Test
    fun `validate handles ANSI escape sequence attack`() {
        val validator = BarcodeInputValidator()
        // Attempt to inject terminal commands
        val attack = "\u001B[2J\u001B[H123456"  // Clear screen + home cursor
        val result = validator.validate(attack, System.currentTimeMillis())
        
        assertTrue(result is ValidationResult.Valid)
        // Only ESC control chars stripped; [, 2, J, H are printable and remain
        val validResult = result as ValidationResult.Valid
        assertEquals("[2J[H123456", validResult.code)
        assertFalse(validResult.code.contains("\u001B"))
    }
    
    @Test
    fun `validate handles extremely long barcode attack`() {
        val validator = BarcodeInputValidator()
        // Attempt 10KB barcode to exhaust memory
        val attack = "A".repeat(10_000)
        val result = validator.validate(attack, System.currentTimeMillis())
        
        assertTrue(result is ValidationResult.TooLong)
    }
    
    @Test
    fun `validate handles rapid-fire scan attack`() {
        val validator = BarcodeInputValidator()
        val baseTime = System.currentTimeMillis()
        
        // Simulate 100 scans in 100ms (attack pattern)
        var blocked = 0
        for (i in 0 until 100) {
            val result = validator.validate("ATTACK$i", baseTime + i)
            if (result is ValidationResult.RateLimited) {
                blocked++
            }
        }
        
        // Should block most scans (at least 98 of 100)
        assertTrue(blocked >= 98, "Expected at least 98 blocked, got $blocked")
    }
}

