package com.unisight.gropos.core.session

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Singleton manager for tracking user inactivity and triggering screen lock.
 *
 * Per CASHIER_OPERATIONS.md:
 * - Inactivity timeout: 5 minutes (300,000 ms)
 * - Lock type: AutoLocked when triggered by inactivity
 * - Timer resets on any user interaction (click, keypress)
 *
 * Per reliability-rules.mdc:
 * - Uses SupervisorJob for fault tolerance
 * - Structured concurrency prevents leaks
 *
 * Usage:
 * 1. Call start() after successful login
 * 2. Call recordActivity() on any user interaction
 * 3. Observe lockEvent flow for lock triggers
 * 4. Call stop() on logout
 */
object InactivityManager {
    
    /** Inactivity timeout in milliseconds (5 minutes) */
    private const val INACTIVITY_TIMEOUT_MS = 5 * 60 * 1000L
    
    /** For testing/development: shorter timeout (uncomment to use) */
    // private const val INACTIVITY_TIMEOUT_MS = 30 * 1000L // 30 seconds
    
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var timerJob: Job? = null
    
    private val _lockEvent = MutableSharedFlow<LockEvent>(extraBufferCapacity = 1)
    
    /**
     * Flow of lock events. Observers should navigate to LockScreen when received.
     */
    val lockEvent: SharedFlow<LockEvent> = _lockEvent.asSharedFlow()
    
    /** Whether the manager is currently active */
    private var isActive = false
    
    /** Screens that should not trigger lock */
    private val exemptScreens = setOf(
        "LockScreen",
        "LoginScreen",
        "CustomerDisplayScreen"
    )
    
    /** Current screen name (set by navigation) */
    var currentScreen: String = ""
    
    /**
     * Start inactivity monitoring.
     * Call after successful login.
     */
    fun start() {
        if (isActive) return
        isActive = true
        resetTimer()
        println("InactivityManager: Started (timeout: ${INACTIVITY_TIMEOUT_MS / 1000}s)")
    }
    
    /**
     * Stop inactivity monitoring.
     * Call on logout.
     */
    fun stop() {
        isActive = false
        timerJob?.cancel()
        timerJob = null
        println("InactivityManager: Stopped")
    }
    
    /**
     * Record user activity to reset the inactivity timer.
     * Call from PointerInput or KeyEvent handlers.
     *
     * Per CASHIER_OPERATIONS.md: Any click or keypress resets the timer.
     */
    fun recordActivity() {
        if (!isActive) return
        resetTimer()
    }
    
    /**
     * Trigger manual lock (e.g., F4 key press).
     *
     * Per CASHIER_OPERATIONS.md: Manual lock uses LockType.Locked
     */
    fun manualLock() {
        if (!isActive) return
        if (isExemptScreen()) return
        
        timerJob?.cancel()
        scope.launch {
            _lockEvent.emit(LockEvent(LockEventType.Manual))
        }
        println("InactivityManager: Manual lock triggered")
    }
    
    /**
     * Reset the inactivity timer.
     */
    private fun resetTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            delay(INACTIVITY_TIMEOUT_MS)
            triggerAutoLock()
        }
    }
    
    /**
     * Trigger automatic lock due to inactivity.
     */
    private suspend fun triggerAutoLock() {
        if (!isActive) return
        if (isExemptScreen()) {
            // Reschedule timer for exempt screens
            resetTimer()
            return
        }
        
        _lockEvent.emit(LockEvent(LockEventType.Inactivity))
        println("InactivityManager: Auto-lock triggered after ${INACTIVITY_TIMEOUT_MS / 1000}s of inactivity")
    }
    
    /**
     * Check if current screen is exempt from locking.
     */
    private fun isExemptScreen(): Boolean {
        return exemptScreens.any { currentScreen.contains(it, ignoreCase = true) }
    }
}

/**
 * Event emitted when screen should be locked.
 */
data class LockEvent(
    val type: LockEventType
)

/**
 * Type of lock event.
 */
enum class LockEventType {
    /** Triggered by inactivity timeout */
    Inactivity,
    
    /** Triggered by manual action (F4 key) */
    Manual,
    
    /** Triggered by manager */
    Manager
}

