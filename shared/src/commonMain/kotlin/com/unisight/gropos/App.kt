package com.unisight.gropos

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.unisight.gropos.core.session.InactivityManager
import com.unisight.gropos.core.theme.GroPOSTheme
import com.unisight.gropos.features.auth.presentation.ui.LockScreen
import com.unisight.gropos.features.auth.presentation.ui.LoginScreen

/**
 * Root application composable.
 * 
 * Sets up:
 * - GroPOSTheme for consistent styling
 * - Voyager Navigator for screen navigation
 * - LoginScreen as the start destination
 * - Inactivity detection for automatic screen lock
 * 
 * Per CASHIER_OPERATIONS.md:
 * - Inactivity timer: 5 minutes → Lock Screen
 * - F4 key: Manual lock
 * - Any user interaction resets the timer
 * 
 * Per project-structure.mdc: DI Graph, Navigation Root are in app/ or core.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun App() {
    GroPOSTheme {
        Navigator(
            screen = LoginScreen(),
            onBackPressed = { currentScreen ->
                // Disable back navigation from LoginScreen and LockScreen
                currentScreen !is LoginScreen && currentScreen !is LockScreen
            }
        ) { navigator ->
            
            // Listen for lock events from InactivityManager
            LaunchedEffect(Unit) {
                InactivityManager.lockEvent.collect { event ->
                    // Navigate to LockScreen when lock event is received
                    // Only push if not already on LockScreen or LoginScreen
                    val currentScreen = navigator.lastItem
                    if (currentScreen !is LockScreen && currentScreen !is LoginScreen) {
                        navigator.push(LockScreen(event.type))
                    }
                }
            }
            
            // Wrap content in a Box that detects user interaction
            // Per CASHIER_OPERATIONS.md: Any click or keypress resets the timer
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // Detect pointer events (mouse clicks, touch)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                InactivityManager.recordActivity()
                                // Allow the event to propagate
                                tryAwaitRelease()
                            }
                        )
                    }
                    // Detect keyboard events
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown) {
                            // Record activity on any key press
                            InactivityManager.recordActivity()
                            
                            // F4 key = Manual lock
                            // Per CASHIER_OPERATIONS.md: F4 triggers manual lock
                            if (keyEvent.key == Key.F4) {
                                InactivityManager.manualLock()
                                return@onPreviewKeyEvent true
                            }
                        }
                        false // Don't consume the event
                    }
            ) {
                SlideTransition(navigator)
            }
        }
    }
}
