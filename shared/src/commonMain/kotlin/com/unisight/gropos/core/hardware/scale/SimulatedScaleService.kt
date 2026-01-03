package com.unisight.gropos.core.hardware.scale

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Simulated scale service for development and testing.
 *
 * Per DESKTOP_HARDWARE.md: This implementation simulates the behavior
 * of a real digital scale without requiring physical hardware.
 *
 * Behavior:
 * - Always reports 0.00 lb as initial weight
 * - Can simulate weight changes programmatically
 * - Simulates stability after a brief delay
 */
class SimulatedScaleService : ScaleService {

    private val _currentWeight = MutableStateFlow(BigDecimal.ZERO)
    override val currentWeight: StateFlow<BigDecimal> = _currentWeight.asStateFlow()

    private val _status = MutableStateFlow(ScaleStatus.Disconnected)
    override val status: StateFlow<ScaleStatus> = _status.asStateFlow()

    private val _isStable = MutableStateFlow(true)
    override val isStable: StateFlow<Boolean> = _isStable.asStateFlow()

    override suspend fun connect(): ScaleResult {
        _status.value = ScaleStatus.Connecting
        delay(500) // Simulate connection time
        _status.value = ScaleStatus.Connected
        _currentWeight.value = BigDecimal.ZERO
        _isStable.value = true
        return ScaleResult.Success
    }

    override suspend fun disconnect() {
        _status.value = ScaleStatus.Disconnected
        _currentWeight.value = BigDecimal.ZERO
    }

    override suspend fun zero(): ScaleResult {
        if (_status.value != ScaleStatus.Connected) {
            return ScaleResult.Error("Scale not connected")
        }
        
        _isStable.value = false
        delay(200) // Simulate tare time
        _currentWeight.value = BigDecimal.ZERO
        _isStable.value = true
        
        return ScaleResult.Success
    }

    override suspend fun getWeight(): WeightResult {
        if (_status.value != ScaleStatus.Connected) {
            return WeightResult.NotConnected
        }
        
        // Wait for stability if not stable
        if (!_isStable.value) {
            delay(300)
            _isStable.value = true
        }
        
        return WeightResult.Success(
            weight = _currentWeight.value,
            isStable = _isStable.value
        )
    }

    // ========================================================================
    // Test Helper Methods
    // ========================================================================

    /**
     * Simulate placing an item on the scale.
     *
     * @param weight The weight of the item in pounds
     */
    fun simulatePlaceItem(weight: BigDecimal) {
        if (_status.value == ScaleStatus.Connected) {
            _isStable.value = false
            _currentWeight.value = weight.setScale(2, RoundingMode.HALF_UP)
            // Stability will be set by getWeight() call
        }
    }

    /**
     * Simulate removing item from the scale.
     */
    fun simulateRemoveItem() {
        if (_status.value == ScaleStatus.Connected) {
            _isStable.value = false
            _currentWeight.value = BigDecimal.ZERO
        }
    }

    /**
     * Simulate weight fluctuation (instability).
     */
    fun simulateInstability() {
        _isStable.value = false
    }

    /**
     * Simulate weight becoming stable.
     */
    fun simulateStability() {
        _isStable.value = true
    }

    /**
     * Simulate overweight condition.
     */
    fun simulateOverweight() {
        _status.value = ScaleStatus.Overweight
    }

    /**
     * Get the simulated weight for testing.
     */
    fun getSimulatedWeight(): BigDecimal = _currentWeight.value
}

