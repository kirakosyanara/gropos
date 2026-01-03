package com.unisight.gropos.core.hardware.scale

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.math.BigDecimal

/**
 * Hardware abstraction interface for scale/weighing operations.
 *
 * Per DESKTOP_HARDWARE.md: This interface abstracts the digital scale
 * hardware, allowing for implementations that support various scale models.
 *
 * Per REMEDIATION_CHECKLIST: Weight Display feature.
 *
 * Implementations:
 * - SimulatedScaleService: Development/testing (returns mock weights)
 * - SerialScaleService: Desktop scales using RS-232/USB serial
 * - SunmiScaleService: Sunmi Android devices with built-in scale
 */
interface ScaleService {

    /**
     * Current weight reading as a reactive flow.
     *
     * Emits updates when the weight on the scale changes.
     * Value is in pounds (BigDecimal for precision).
     */
    val currentWeight: StateFlow<BigDecimal>

    /**
     * Current status of the scale.
     */
    val status: StateFlow<ScaleStatus>

    /**
     * Whether the current weight is stable.
     *
     * Per weighing regulations: Price should only be calculated
     * when weight reading is stable (not fluctuating).
     */
    val isStable: StateFlow<Boolean>

    /**
     * Connect to the scale hardware.
     */
    suspend fun connect(): ScaleResult

    /**
     * Disconnect from the scale hardware.
     */
    suspend fun disconnect()

    /**
     * Zero/tare the scale.
     *
     * Resets the current weight reading to zero,
     * accounting for container weight.
     */
    suspend fun zero(): ScaleResult

    /**
     * Request a weight reading.
     *
     * Returns the current stable weight, waiting for
     * stability if necessary.
     */
    suspend fun getWeight(): WeightResult
}

/**
 * Scale connection status.
 */
enum class ScaleStatus {
    Connected,
    Disconnected,
    Connecting,
    Error,
    Overweight,
    Underweight
}

/**
 * Result of scale operations.
 */
sealed class ScaleResult {
    data object Success : ScaleResult()
    data class Error(val message: String) : ScaleResult()
    data object Timeout : ScaleResult()
}

/**
 * Result of a weight reading request.
 */
sealed class WeightResult {
    /**
     * Successful weight reading.
     *
     * @property weight The weight in pounds
     * @property isStable Whether the reading is stable
     */
    data class Success(
        val weight: BigDecimal,
        val isStable: Boolean
    ) : WeightResult()

    data class Error(val message: String) : WeightResult()
    data object NotConnected : WeightResult()
    data object Overweight : WeightResult()
}

