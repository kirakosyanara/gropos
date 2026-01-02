package com.unisight.gropos.core.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock

/**
 * IdleDetector composable wrapper that tracks user inactivity.
 *
 * Per SCREEN_LAYOUTS.md - Advertisement Overlay:
 * "Full-screen ad display when transaction is idle"
 * 
 * This component wraps content and detects user inactivity by tracking
 * touch/mouse events. When idle for the specified timeout, it triggers
 * the onIdle callback. Any user interaction resets the timer.
 *
 * Per governance/testing-strategy.mdc:
 * - Uses structured concurrency (LaunchedEffect for lifecycle safety)
 * - No memory leaks (coroutine cancellation on composable exit)
 * - State hoisting for testability
 *
 * @param timeoutMs Idle timeout in milliseconds. Default: 30 seconds for testing.
 *                  Production would use 60 seconds or more.
 * @param enabled Whether idle detection is active.
 * @param isIdle Current idle state (hoisted for parent control).
 * @param onIdleChange Callback when idle state changes.
 * @param onActivity Callback when user activity is detected (for external timer resets).
 * @param content The content to wrap and monitor for inactivity.
 */
@Composable
fun IdleDetector(
    timeoutMs: Long = 30_000L, // 30 seconds for testing (per requirement)
    enabled: Boolean = true,
    isIdle: Boolean,
    onIdleChange: (Boolean) -> Unit,
    onActivity: () -> Unit = {},
    content: @Composable () -> Unit
) {
    // Track last interaction timestamp
    var lastInteractionTime by remember { mutableStateOf(Clock.System.now()) }
    
    // Effect to monitor inactivity and trigger idle state
    // Per kotlin-standards.mdc: Uses LaunchedEffect with proper key management
    LaunchedEffect(enabled, lastInteractionTime) {
        if (!enabled) {
            // If disabled, ensure we're not in idle state
            if (isIdle) {
                onIdleChange(false)
            }
            return@LaunchedEffect
        }
        
        // Wait for the timeout period
        delay(timeoutMs)
        
        // If we reach here without cancellation, user has been idle
        if (!isIdle) {
            onIdleChange(true)
        }
    }
    
    // Wrap content in a Box that intercepts all pointer events
    Box(
        modifier = Modifier
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                
                // Intercept all pointer events at the Initial pass
                // This catches events before children consume them
                awaitEachGesture {
                    // Wait for any pointer down event
                    awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
                    
                    // Record the interaction
                    lastInteractionTime = Clock.System.now()
                    
                    // If we were idle, dismiss the overlay
                    if (isIdle) {
                        onIdleChange(false)
                    }
                    
                    // Notify external listeners
                    onActivity()
                }
            }
    ) {
        content()
    }
}

/**
 * Configuration for the IdleDetector.
 *
 * Per governance/code-quality.mdc: Using data class for configuration
 * to enable easy testing and future configurability.
 */
data class IdleDetectorConfig(
    /** Timeout in milliseconds before showing idle overlay */
    val timeoutMs: Long = 30_000L,
    
    /** Whether idle detection is enabled */
    val enabled: Boolean = true
) {
    companion object {
        /** Testing configuration with 30-second timeout */
        val Testing = IdleDetectorConfig(timeoutMs = 30_000L)
        
        /** Production configuration with 60-second timeout (per requirement) */
        val Production = IdleDetectorConfig(timeoutMs = 60_000L)
        
        /** Extended timeout for customer-facing displays (2 minutes) */
        val CustomerDisplay = IdleDetectorConfig(timeoutMs = 120_000L)
    }
}

