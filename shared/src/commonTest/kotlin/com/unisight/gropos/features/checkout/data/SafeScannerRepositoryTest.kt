package com.unisight.gropos.features.checkout.data

import com.unisight.gropos.features.checkout.domain.service.BarcodeInputValidator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration tests for SafeScannerRepository.
 * 
 * Verifies that the safety wrapper correctly filters and validates
 * barcode input from the underlying scanner.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SafeScannerRepositoryTest {
    
    // ========================================================================
    // Valid Barcode Passthrough Tests
    // ========================================================================
    
    @Test
    fun `valid barcode passes through unchanged`() = runTest {
        val fakeScanner = FakeScannerRepository()
        val safeScanner = SafeScannerRepository(fakeScanner)
        
        safeScanner.startScanning()
        
        val result = mutableListOf<String>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            safeScanner.scannedCodes.collect { result.add(it) }
        }
        
        fakeScanner.emitScan("1234567890123")
        
        assertEquals(1, result.size)
        assertEquals("1234567890123", result[0])
        
        job.cancel()
    }
    
    @Test
    fun `multiple valid barcodes pass through`() = runTest {
        val fakeScanner = FakeScannerRepository()
        var mockTime = 0L
        val safeScanner = SafeScannerRepository(
            delegate = fakeScanner,
            clock = { mockTime }
        )
        
        safeScanner.startScanning()
        
        val result = mutableListOf<String>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            safeScanner.scannedCodes.collect { result.add(it) }
        }
        
        // First scan at t=0
        mockTime = 0L
        fakeScanner.emitScan("AAA")
        
        // Second scan at t=100 (after rate limit)
        mockTime = 100L
        fakeScanner.emitScan("BBB")
        
        // Third scan at t=200
        mockTime = 200L
        fakeScanner.emitScan("CCC")
        
        assertEquals(3, result.size)
        assertEquals(listOf("AAA", "BBB", "CCC"), result)
        
        job.cancel()
    }
    
    // ========================================================================
    // Sanitization Tests
    // ========================================================================
    
    @Test
    fun `barcode with control characters is sanitized`() = runTest {
        val fakeScanner = FakeScannerRepository()
        val safeScanner = SafeScannerRepository(fakeScanner)
        
        safeScanner.startScanning()
        
        val result = mutableListOf<String>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            safeScanner.scannedCodes.collect { result.add(it) }
        }
        
        // Send barcode with null character and newline
        fakeScanner.emitScan("123\u0000456\n789")
        
        assertEquals(1, result.size)
        assertEquals("123456789", result[0])
        
        job.cancel()
    }
    
    @Test
    fun `barcode with leading and trailing whitespace is trimmed`() = runTest {
        val fakeScanner = FakeScannerRepository()
        val safeScanner = SafeScannerRepository(fakeScanner)
        
        safeScanner.startScanning()
        
        val result = mutableListOf<String>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            safeScanner.scannedCodes.collect { result.add(it) }
        }
        
        fakeScanner.emitScan("   12345   ")
        
        assertEquals(1, result.size)
        assertEquals("12345", result[0])
        
        job.cancel()
    }
    
    // ========================================================================
    // Rejection Tests
    // ========================================================================
    
    @Test
    fun `barcode too short is rejected`() = runTest {
        val fakeScanner = FakeScannerRepository()
        val safeScanner = SafeScannerRepository(fakeScanner)
        
        safeScanner.startScanning()
        
        val validCodes = mutableListOf<String>()
        val rejections = mutableListOf<RejectedScan>()
        
        val job1 = launch(UnconfinedTestDispatcher(testScheduler)) {
            safeScanner.scannedCodes.collect { validCodes.add(it) }
        }
        val job2 = launch(UnconfinedTestDispatcher(testScheduler)) {
            safeScanner.rejectedScans.collect { rejections.add(it) }
        }
        
        // Send barcode that's too short (2 chars)
        fakeScanner.emitScan("AB")
        
        assertTrue(validCodes.isEmpty())
        assertEquals(1, rejections.size)
        assertEquals(RejectionReason.TOO_SHORT, rejections[0].reason)
        
        job1.cancel()
        job2.cancel()
    }
    
    @Test
    fun `barcode too long is rejected`() = runTest {
        val fakeScanner = FakeScannerRepository()
        val safeScanner = SafeScannerRepository(fakeScanner)
        
        safeScanner.startScanning()
        
        val validCodes = mutableListOf<String>()
        val rejections = mutableListOf<RejectedScan>()
        
        val job1 = launch(UnconfinedTestDispatcher(testScheduler)) {
            safeScanner.scannedCodes.collect { validCodes.add(it) }
        }
        val job2 = launch(UnconfinedTestDispatcher(testScheduler)) {
            safeScanner.rejectedScans.collect { rejections.add(it) }
        }
        
        // Send barcode that's too long (200 chars)
        fakeScanner.emitScan("A".repeat(200))
        
        assertTrue(validCodes.isEmpty())
        assertEquals(1, rejections.size)
        assertEquals(RejectionReason.TOO_LONG, rejections[0].reason)
        
        job1.cancel()
        job2.cancel()
    }
    
    @Test
    fun `empty barcode after sanitization is rejected`() = runTest {
        val fakeScanner = FakeScannerRepository()
        val safeScanner = SafeScannerRepository(fakeScanner)
        
        safeScanner.startScanning()
        
        val validCodes = mutableListOf<String>()
        val rejections = mutableListOf<RejectedScan>()
        
        val job1 = launch(UnconfinedTestDispatcher(testScheduler)) {
            safeScanner.scannedCodes.collect { validCodes.add(it) }
        }
        val job2 = launch(UnconfinedTestDispatcher(testScheduler)) {
            safeScanner.rejectedScans.collect { rejections.add(it) }
        }
        
        // Send barcode that's only control characters
        fakeScanner.emitScan("\u0000\u0001\u0002")
        
        assertTrue(validCodes.isEmpty())
        assertEquals(1, rejections.size)
        assertEquals(RejectionReason.EMPTY, rejections[0].reason)
        
        job1.cancel()
        job2.cancel()
    }
    
    // ========================================================================
    // Rate Limiting Tests
    // ========================================================================
    
    @Test
    fun `rapid scans are rate limited`() = runTest {
        val fakeScanner = FakeScannerRepository()
        var mockTime = 0L
        val safeScanner = SafeScannerRepository(
            delegate = fakeScanner,
            clock = { mockTime }
        )
        
        safeScanner.startScanning()
        
        val validCodes = mutableListOf<String>()
        val rejections = mutableListOf<RejectedScan>()
        
        val job1 = launch(UnconfinedTestDispatcher(testScheduler)) {
            safeScanner.scannedCodes.collect { validCodes.add(it) }
        }
        val job2 = launch(UnconfinedTestDispatcher(testScheduler)) {
            safeScanner.rejectedScans.collect { rejections.add(it) }
        }
        
        // First scan at t=0 (should pass)
        mockTime = 0L
        fakeScanner.emitScan("FIRST123")
        
        // Second scan at t=10ms (should be rate limited)
        mockTime = 10L
        fakeScanner.emitScan("SECOND456")
        
        // Third scan at t=20ms (should be rate limited)
        mockTime = 20L
        fakeScanner.emitScan("THIRD789")
        
        assertEquals(1, validCodes.size)
        assertEquals("FIRST123", validCodes[0])
        
        assertEquals(2, rejections.size)
        assertTrue(rejections.all { it.reason == RejectionReason.RATE_LIMITED })
        
        job1.cancel()
        job2.cancel()
    }
    
    @Test
    fun `scan after rate limit window passes through`() = runTest {
        val fakeScanner = FakeScannerRepository()
        var mockTime = 0L
        val safeScanner = SafeScannerRepository(
            delegate = fakeScanner,
            clock = { mockTime }
        )
        
        safeScanner.startScanning()
        
        val validCodes = mutableListOf<String>()
        
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            safeScanner.scannedCodes.collect { validCodes.add(it) }
        }
        
        // First scan at t=0
        mockTime = 0L
        fakeScanner.emitScan("FIRST123")
        
        // Second scan at t=60ms (after 50ms window)
        mockTime = 60L
        fakeScanner.emitScan("SECOND456")
        
        assertEquals(2, validCodes.size)
        assertEquals("FIRST123", validCodes[0])
        assertEquals("SECOND456", validCodes[1])
        
        job.cancel()
    }
    
    // ========================================================================
    // Attack Simulation Tests
    // ========================================================================
    
    @Test
    fun `buffer overflow attack is blocked`() = runTest {
        val fakeScanner = FakeScannerRepository()
        val safeScanner = SafeScannerRepository(fakeScanner)
        
        safeScanner.startScanning()
        
        val validCodes = mutableListOf<String>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            safeScanner.scannedCodes.collect { validCodes.add(it) }
        }
        
        // Attempt 10KB barcode attack
        fakeScanner.emitScan("A".repeat(10_000))
        
        assertTrue(validCodes.isEmpty())
        
        job.cancel()
    }
    
    @Test
    fun `rapid fire attack is mostly blocked`() = runTest {
        val fakeScanner = FakeScannerRepository()
        var mockTime = 0L
        val safeScanner = SafeScannerRepository(
            delegate = fakeScanner,
            clock = { mockTime }
        )
        
        safeScanner.startScanning()
        
        val validCodes = mutableListOf<String>()
        val rejections = mutableListOf<RejectedScan>()
        
        val job1 = launch(UnconfinedTestDispatcher(testScheduler)) {
            safeScanner.scannedCodes.collect { validCodes.add(it) }
        }
        val job2 = launch(UnconfinedTestDispatcher(testScheduler)) {
            safeScanner.rejectedScans.collect { rejections.add(it) }
        }
        
        // Simulate 100 scans in 100ms (attack pattern)
        repeat(100) { i ->
            mockTime = i.toLong()
            fakeScanner.emitScan("ATTACK$i")
        }
        
        // With 50ms rate limit window:
        // t=0 passes, t=1-49 blocked, t=50 passes, t=51-99 blocked
        // So 2 valid codes: at t=0 and t=50
        assertEquals(2, validCodes.size)
        assertEquals("ATTACK0", validCodes[0])
        assertEquals("ATTACK50", validCodes[1])
        
        // The remaining 98 should be rate limited
        val rateLimitedCount = rejections.count { it.reason == RejectionReason.RATE_LIMITED }
        assertEquals(98, rateLimitedCount)
        
        job1.cancel()
        job2.cancel()
    }
}

