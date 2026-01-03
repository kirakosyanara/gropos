package com.unisight.gropos.core.hardware.scale

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * TDD Test Suite for CAS PD-II Protocol Parser.
 * 
 * **Per DESKTOP_HARDWARE.md:**
 * CAS PD-II scales use a standard 18-byte ASCII frame format:
 * 
 * Frame Structure:
 * | Byte | Description |
 * |------|-------------|
 * | 0    | STX (0x02) - Start of Text |
 * | 1-5  | Status flags (ST, US, OL, etc.) |
 * | 6    | Comma separator |
 * | 7    | Polarity (+/-) |
 * | 8-13 | Weight digits (6 characters, right-justified, space-padded) |
 * | 14   | Space separator |
 * | 15-16| Unit indicator (lb, kg, oz) |
 * | 17   | CR (0x0D) - Carriage Return |
 * 
 * Example valid frame: STX + "ST,GS,+  0.00 lb" + CR
 * 
 * Per testing-strategy.mdc: Write tests BEFORE implementation.
 */
class CasProtocolParserTest {
    
    // ========================================================================
    // Valid Weight Parsing
    // ========================================================================
    
    @Test
    fun `parse stable weight 0_00 lb`() {
        // ST = Stable, GS = Gross weight
        val frame = buildFrame(status = "ST,GS", polarity = "+", weight = "  0.00", unit = "lb")
        
        val result = CasProtocolParser.parse(frame)
        
        assertIs<CasParseResult.Success>(result)
        assertEquals(BigDecimal("0.00"), result.weight)
        assertEquals(true, result.isStable)
        assertEquals(CasWeightUnit.POUNDS, result.unit)
    }
    
    @Test
    fun `parse stable weight 1_25 lb`() {
        val frame = buildFrame(status = "ST,GS", polarity = "+", weight = "  1.25", unit = "lb")
        
        val result = CasProtocolParser.parse(frame)
        
        assertIs<CasParseResult.Success>(result)
        assertEquals(BigDecimal("1.25"), result.weight)
        assertEquals(true, result.isStable)
    }
    
    @Test
    fun `parse stable weight 10_50 lb`() {
        val frame = buildFrame(status = "ST,GS", polarity = "+", weight = " 10.50", unit = "lb")
        
        val result = CasProtocolParser.parse(frame)
        
        assertIs<CasParseResult.Success>(result)
        assertEquals(BigDecimal("10.50"), result.weight)
    }
    
    @Test
    fun `parse stable weight 100_00 lb - max common weight`() {
        val frame = buildFrame(status = "ST,GS", polarity = "+", weight = "100.00", unit = "lb")
        
        val result = CasProtocolParser.parse(frame)
        
        assertIs<CasParseResult.Success>(result)
        assertEquals(BigDecimal("100.00"), result.weight)
    }
    
    @Test
    fun `parse stable weight 999_99 lb - near capacity`() {
        val frame = buildFrame(status = "ST,GS", polarity = "+", weight = "999.99", unit = "lb")
        
        val result = CasProtocolParser.parse(frame)
        
        assertIs<CasParseResult.Success>(result)
        assertEquals(BigDecimal("999.99"), result.weight)
    }
    
    // ========================================================================
    // Unstable Weight (US flag)
    // ========================================================================
    
    @Test
    fun `parse unstable weight - motion detected`() {
        // US = Unstable (motion detected)
        val frame = buildFrame(status = "US,GS", polarity = "+", weight = "  5.12", unit = "lb")
        
        val result = CasProtocolParser.parse(frame)
        
        assertIs<CasParseResult.Success>(result)
        assertEquals(BigDecimal("5.12"), result.weight)
        assertEquals(false, result.isStable)
    }
    
    // ========================================================================
    // Negative Weight (Tare underweight)
    // ========================================================================
    
    @Test
    fun `parse negative weight after tare`() {
        // Negative polarity occurs when container removed after tare
        val frame = buildFrame(status = "ST,GS", polarity = "-", weight = "  0.50", unit = "lb")
        
        val result = CasProtocolParser.parse(frame)
        
        assertIs<CasParseResult.Success>(result)
        assertEquals(BigDecimal("-0.50"), result.weight)
    }
    
    // ========================================================================
    // Unit Conversion
    // ========================================================================
    
    @Test
    fun `parse weight in kilograms`() {
        val frame = buildFrame(status = "ST,GS", polarity = "+", weight = "  2.50", unit = "kg")
        
        val result = CasProtocolParser.parse(frame)
        
        assertIs<CasParseResult.Success>(result)
        assertEquals(BigDecimal("2.50"), result.weight)
        assertEquals(CasWeightUnit.KILOGRAMS, result.unit)
    }
    
    @Test
    fun `parse weight in ounces`() {
        val frame = buildFrame(status = "ST,GS", polarity = "+", weight = "  8.00", unit = "oz")
        
        val result = CasProtocolParser.parse(frame)
        
        assertIs<CasParseResult.Success>(result)
        assertEquals(BigDecimal("8.00"), result.weight)
        assertEquals(CasWeightUnit.OUNCES, result.unit)
    }
    
    // ========================================================================
    // Error Conditions
    // ========================================================================
    
    @Test
    fun `detect overweight condition (OL flag)`() {
        // OL = Overload
        val frame = buildFrame(status = "OL,GS", polarity = "+", weight = "------", unit = "lb")
        
        val result = CasProtocolParser.parse(frame)
        
        assertIs<CasParseResult.Overweight>(result)
    }
    
    @Test
    fun `detect underweight condition (negative overload)`() {
        // Underweight typically shows as negative OL
        val frame = buildFrame(status = "OL,GS", polarity = "-", weight = "------", unit = "lb")
        
        val result = CasProtocolParser.parse(frame)
        
        assertIs<CasParseResult.Underweight>(result)
    }
    
    // ========================================================================
    // Invalid Frame Handling
    // ========================================================================
    
    @Test
    fun `reject empty frame`() {
        val result = CasProtocolParser.parse(byteArrayOf())
        
        assertIs<CasParseResult.InvalidFrame>(result)
    }
    
    @Test
    fun `reject frame too short`() {
        val frame = "SHORT".toByteArray()
        
        val result = CasProtocolParser.parse(frame)
        
        assertIs<CasParseResult.InvalidFrame>(result)
    }
    
    @Test
    fun `reject frame without STX marker`() {
        // Missing start byte
        val frame = "  ST,GS,+  0.00 lb\r".toByteArray()
        
        val result = CasProtocolParser.parse(frame)
        
        assertIs<CasParseResult.InvalidFrame>(result)
    }
    
    @Test
    fun `reject frame without CR terminator`() {
        // Missing carriage return
        val frame = byteArrayOf(0x02) + "ST,GS,+  0.00 lb".toByteArray()
        
        val result = CasProtocolParser.parse(frame)
        
        assertIs<CasParseResult.InvalidFrame>(result)
    }
    
    @Test
    fun `reject frame with non-numeric weight`() {
        val frame = buildFrame(status = "ST,GS", polarity = "+", weight = " ERROR", unit = "lb")
        
        val result = CasProtocolParser.parse(frame)
        
        assertIs<CasParseResult.InvalidFrame>(result)
    }
    
    // ========================================================================
    // Real-World Frame Examples
    // ========================================================================
    
    @Test
    fun `parse real frame - zero weight stable`() {
        // Actual byte sequence from CAS PD-II scale
        val frame = byteArrayOf(
            0x02,                                           // STX
            'S'.code.toByte(), 'T'.code.toByte(),           // ST = Stable
            ','.code.toByte(),
            'G'.code.toByte(), 'S'.code.toByte(),           // GS = Gross
            ','.code.toByte(),
            '+'.code.toByte(),                              // Positive
            ' '.code.toByte(), ' '.code.toByte(),           // Padding
            '0'.code.toByte(), '.'.code.toByte(),           // Weight
            '0'.code.toByte(), '0'.code.toByte(),
            ' '.code.toByte(),
            'l'.code.toByte(), 'b'.code.toByte(),           // Unit
            0x0D                                            // CR
        )
        
        val result = CasProtocolParser.parse(frame)
        
        assertIs<CasParseResult.Success>(result)
        assertEquals(BigDecimal("0.00"), result.weight)
        assertEquals(true, result.isStable)
    }
    
    // ========================================================================
    // Frame Extraction Tests
    // ========================================================================
    
    @Test
    fun `isValidFrame returns true for valid frame`() {
        val frame = buildFrame(status = "ST,GS", polarity = "+", weight = "  0.00", unit = "lb")
        
        assertEquals(true, CasProtocolParser.isValidFrame(frame))
    }
    
    @Test
    fun `isValidFrame returns false for empty array`() {
        assertEquals(false, CasProtocolParser.isValidFrame(byteArrayOf()))
    }
    
    @Test
    fun `extractFrame finds complete frame in buffer`() {
        val frame = buildFrame(status = "ST,GS", polarity = "+", weight = "  5.00", unit = "lb")
        val garbage = "noise".toByteArray()
        val buffer = garbage + frame + garbage
        
        val result = CasProtocolParser.extractFrame(buffer)
        
        assertEquals(frame.toList(), result?.first?.toList())
    }
    
    // ========================================================================
    // Helper Functions
    // ========================================================================
    
    /**
     * Builds a CAS PD-II protocol frame for testing.
     * 
     * @param status Status flags (e.g., "ST,GS" for stable gross)
     * @param polarity Weight polarity ("+" or "-")
     * @param weight Weight value (6 chars, right-justified)
     * @param unit Unit indicator ("lb", "kg", "oz")
     * @return ByteArray containing the complete frame
     */
    private fun buildFrame(
        status: String,
        polarity: String,
        weight: String,
        unit: String
    ): ByteArray {
        // Frame: STX + Status(5) + , + Polarity(1) + Weight(6) + Space + Unit(2) + CR
        val frameContent = "$status,$polarity$weight $unit"
        return byteArrayOf(0x02) + frameContent.toByteArray() + byteArrayOf(0x0D)
    }
}
