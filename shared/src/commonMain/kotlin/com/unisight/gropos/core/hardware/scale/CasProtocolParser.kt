package com.unisight.gropos.core.hardware.scale

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Parser for CAS PD-II scale protocol frames.
 * 
 * **Per DESKTOP_HARDWARE.md:**
 * CAS PD-II scales communicate via RS-232/USB serial using an 18-byte ASCII frame.
 * 
 * **Frame Structure (18 bytes):**
 * | Position | Bytes | Description |
 * |----------|-------|-------------|
 * | 0        | 1     | STX (0x02) - Start of transmission |
 * | 1-5      | 5     | Status flags: "ST,GS" or "US,GS" or "OL,GS" |
 * | 6        | 1     | Comma separator |
 * | 7        | 1     | Polarity: "+" or "-" |
 * | 8-13     | 6     | Weight value (right-justified, space-padded) |
 * | 14       | 1     | Space separator |
 * | 15-16    | 2     | Unit: "lb", "kg", "oz" |
 * | 17       | 1     | CR (0x0D) - Carriage return terminator |
 * 
 * **Status Flags:**
 * - ST: Stable (weight is settled)
 * - US: Unstable (motion detected)
 * - OL: Overload (weight exceeds capacity)
 * - GS: Gross weight (vs. net after tare)
 * 
 * Per code-quality.mdc: Pure function with no side effects.
 * Per reliability-stability.mdc: Defensive parsing with error results.
 */
object CasProtocolParser {
    
    // ========================================================================
    // Frame Constants
    // ========================================================================
    
    /** Minimum valid frame length */
    private const val MIN_FRAME_LENGTH = 18
    
    /** Start of Text marker */
    private const val STX: Byte = 0x02
    
    /** Carriage Return terminator */
    private const val CR: Byte = 0x0D
    
    /** Stable weight flag */
    private const val STATUS_STABLE = "ST"
    
    /** Unstable/motion flag */
    private const val STATUS_UNSTABLE = "US"
    
    /** Overload flag */
    private const val STATUS_OVERLOAD = "OL"
    
    // ========================================================================
    // Frame Position Constants
    // ========================================================================
    
    private const val POS_STX = 0
    private const val POS_STATUS_START = 1
    private const val POS_STATUS_END = 6  // exclusive
    private const val POS_POLARITY = 7
    private const val POS_WEIGHT_START = 8
    private const val POS_WEIGHT_END = 14  // exclusive
    private const val POS_UNIT_START = 15
    private const val POS_UNIT_END = 17  // exclusive
    
    // ========================================================================
    // Core Parsing
    // ========================================================================
    
    /**
     * Parses a CAS PD-II protocol frame.
     * 
     * **Usage:**
     * ```kotlin
     * val result = CasProtocolParser.parse(serialBuffer)
     * when (result) {
     *     is CasParseResult.Success -> {
     *         updateWeight(result.weight, result.isStable)
     *     }
     *     is CasParseResult.Overweight -> showOverweightError()
     *     is CasParseResult.InvalidFrame -> logParseError(result.reason)
     * }
     * ```
     * 
     * @param frame Raw bytes from serial port
     * @return Parsed result with weight data or error condition
     */
    fun parse(frame: ByteArray): CasParseResult {
        // Validate frame length
        if (frame.size < MIN_FRAME_LENGTH) {
            return CasParseResult.InvalidFrame(
                "Frame too short: ${frame.size} bytes (expected $MIN_FRAME_LENGTH)"
            )
        }
        
        // Validate STX start marker
        if (frame[POS_STX] != STX) {
            return CasParseResult.InvalidFrame(
                "Missing STX marker at position 0 (got 0x${frame[POS_STX].toString(16)})"
            )
        }
        
        // Validate CR terminator
        if (frame[frame.size - 1] != CR) {
            return CasParseResult.InvalidFrame(
                "Missing CR terminator at end (got 0x${frame[frame.size - 1].toString(16)})"
            )
        }
        
        // Extract status field
        val statusField = extractString(frame, POS_STATUS_START, POS_STATUS_END)
        val statusCode = statusField.take(2)
        
        // Extract polarity
        val polarity = frame[POS_POLARITY].toInt().toChar()
        val isNegative = polarity == '-'
        
        // Check for overload condition
        if (statusCode == STATUS_OVERLOAD) {
            return if (isNegative) {
                CasParseResult.Underweight
            } else {
                CasParseResult.Overweight
            }
        }
        
        // Determine stability from status code
        val isStable = statusCode == STATUS_STABLE
        
        // Extract and parse weight value
        val weightString = extractString(frame, POS_WEIGHT_START, POS_WEIGHT_END).trim()
        
        // Handle dashes (overload indicator in weight field)
        if (weightString.contains("-") && weightString.all { it == '-' || it == ' ' }) {
            return CasParseResult.Overweight
        }
        
        val weight = try {
            val absWeight = BigDecimal(weightString).setScale(2, RoundingMode.HALF_UP)
            if (isNegative) absWeight.negate() else absWeight
        } catch (e: NumberFormatException) {
            return CasParseResult.InvalidFrame(
                "Invalid weight value: '$weightString'"
            )
        }
        
        // Extract unit
        val unitString = extractString(frame, POS_UNIT_START, POS_UNIT_END).lowercase()
        val unit = when (unitString) {
            "lb" -> CasWeightUnit.POUNDS
            "kg" -> CasWeightUnit.KILOGRAMS
            "oz" -> CasWeightUnit.OUNCES
            else -> CasWeightUnit.POUNDS // Default to pounds
        }
        
        return CasParseResult.Success(
            weight = weight,
            isStable = isStable,
            unit = unit
        )
    }
    
    // ========================================================================
    // Helper Methods
    // ========================================================================
    
    /**
     * Extracts a string from a byte array at the specified positions.
     */
    private fun extractString(frame: ByteArray, start: Int, end: Int): String {
        val safeEnd = minOf(end, frame.size)
        return frame.sliceArray(start until safeEnd)
            .map { it.toInt().toChar() }
            .joinToString("")
    }
    
    /**
     * Validates if a byte array contains a potentially valid CAS frame.
     * 
     * Use this for quick validation before full parsing.
     * 
     * @param data Raw byte buffer
     * @return true if data looks like a valid CAS frame
     */
    fun isValidFrame(data: ByteArray): Boolean {
        if (data.size < MIN_FRAME_LENGTH) return false
        if (data[0] != STX) return false
        if (data[data.size - 1] != CR) return false
        return true
    }
    
    /**
     * Finds a complete frame within a buffer.
     * 
     * Useful for extracting frames from a stream with partial data.
     * 
     * @param buffer Raw serial buffer
     * @return Pair of (frame bytes, remaining bytes) or null if no complete frame
     */
    fun extractFrame(buffer: ByteArray): Pair<ByteArray, ByteArray>? {
        val stxIndex = buffer.indexOf(STX)
        if (stxIndex < 0) return null
        
        val crIndex = buffer.indexOf(CR, stxIndex)
        if (crIndex < 0) return null
        
        val frameEnd = crIndex + 1
        if (frameEnd - stxIndex < MIN_FRAME_LENGTH) return null
        
        val frame = buffer.sliceArray(stxIndex until frameEnd)
        val remaining = buffer.sliceArray(frameEnd until buffer.size)
        
        return Pair(frame, remaining)
    }
    
    /**
     * Extension to find byte in array starting from offset.
     */
    private fun ByteArray.indexOf(byte: Byte, startIndex: Int = 0): Int {
        for (i in startIndex until size) {
            if (this[i] == byte) return i
        }
        return -1
    }
}

/**
 * Result of parsing a CAS scale frame.
 * 
 * Per code-quality.mdc: Use sealed classes for domain results.
 */
sealed class CasParseResult {
    
    /**
     * Successfully parsed weight reading.
     * 
     * @property weight The weight value in the scale's current unit
     * @property isStable Whether the weight is stable (not moving)
     * @property unit The unit of measurement
     */
    data class Success(
        val weight: BigDecimal,
        val isStable: Boolean,
        val unit: CasWeightUnit
    ) : CasParseResult()
    
    /**
     * Scale is overloaded (weight exceeds capacity).
     */
    data object Overweight : CasParseResult()
    
    /**
     * Scale is underweight (negative overload after tare).
     */
    data object Underweight : CasParseResult()
    
    /**
     * Frame could not be parsed.
     * 
     * @property reason Human-readable error description
     */
    data class InvalidFrame(val reason: String) : CasParseResult()
}

/**
 * Weight units supported by CAS scales.
 */
enum class CasWeightUnit {
    /** Pounds (lb) - US standard */
    POUNDS,
    
    /** Kilograms (kg) - Metric */
    KILOGRAMS,
    
    /** Ounces (oz) - For small weights */
    OUNCES
}

