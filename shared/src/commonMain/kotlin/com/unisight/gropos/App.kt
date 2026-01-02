package com.unisight.gropos

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
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
import com.unisight.gropos.features.device.domain.repository.DeviceRepository
import com.unisight.gropos.features.device.presentation.ui.RegistrationScreen
import org.koin.compose.koinInject

/**
 * Root application composable.
 * 
 * Sets up:
 * - GroPOSTheme for consistent styling
 * - Voyager Navigator for screen navigation
 * - Device registration check on launch
 * - Inactivity detection for automatic screen lock
 * 
 * Per DEVICE_REGISTRATION.md:
 * - App Launch → Check isRegistered()
 * - If true → Go to LoginScreen
 * - If false → Go to RegistrationScreen
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
        // Check device registration status
        val deviceRepository: DeviceRepository = koinInject()
        var isCheckingRegistration by remember { mutableStateOf(true) }
        var isDeviceRegistered by remember { mutableStateOf(false) }
        
        // Perform registration check on app launch
        LaunchedEffect(Unit) {
            isDeviceRegistered = deviceRepository.isRegistered()
            isCheckingRegistration = false
        }
        
        // Wait for registration check before showing any screen
        if (isCheckingRegistration) {
            // Could show a splash/loading screen here
            // For now, just empty box (instant check)
            Box(modifier = Modifier.fillMaxSize())
            return@GroPOSTheme
        }
        
        // Determine start screen based on registration status
        val startScreen = if (isDeviceRegistered) {
            LoginScreen()
        } else {
            RegistrationScreen()
        }
        
        Navigator(
            screen = startScreen,
            onBackPressed = { currentScreen ->
                // Disable back navigation from Login, Lock, and Registration screens
                currentScreen !is LoginScreen && 
                currentScreen !is LockScreen &&
                currentScreen !is RegistrationScreen
            }
        ) { navigator ->
            
            // Listen for lock events from InactivityManager
            LaunchedEffect(Unit) {
                InactivityManager.lockEvent.collect { event ->
                    // Navigate to LockScreen when lock event is received
                    // Only push if not already on LockScreen, LoginScreen, or RegistrationScreen
                    val currentScreen = navigator.lastItem
                    if (currentScreen !is LockScreen && 
                        currentScreen !is LoginScreen &&
                        currentScreen !is RegistrationScreen) {
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
