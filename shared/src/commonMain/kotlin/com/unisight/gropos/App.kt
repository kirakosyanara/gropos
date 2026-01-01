package com.unisight.gropos

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.unisight.gropos.core.theme.GroPOSTheme
import com.unisight.gropos.features.auth.presentation.ui.LoginScreen

/**
 * Root application composable.
 * 
 * Sets up:
 * - GroPOSTheme for consistent styling
 * - Voyager Navigator for screen navigation
 * - LoginScreen as the start destination
 * 
 * Per project-structure.mdc: DI Graph, Navigation Root are in app/ or core.
 */
@Composable
fun App() {
    GroPOSTheme {
        Navigator(
            screen = LoginScreen(),
            onBackPressed = { currentScreen ->
                // Disable back navigation from LoginScreen
                currentScreen !is LoginScreen
            }
        ) { navigator ->
            SlideTransition(navigator)
        }
    }
}

