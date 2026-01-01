package com.unisight.gropos.features.auth.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.unisight.gropos.core.components.OutlineButton
import com.unisight.gropos.core.components.SuccessButton
import com.unisight.gropos.core.components.TenKey
import com.unisight.gropos.core.components.TenKeyMode
import com.unisight.gropos.core.components.TenKeyState
import com.unisight.gropos.core.components.WhiteBox
import com.unisight.gropos.core.theme.GroPOSColors
import com.unisight.gropos.core.theme.GroPOSRadius
import com.unisight.gropos.core.theme.GroPOSSpacing
import com.unisight.gropos.features.auth.domain.model.AuthUser
import com.unisight.gropos.features.auth.domain.model.UserRole
import com.unisight.gropos.features.auth.presentation.LoginUiState

/**
 * Login screen content with hoisted state.
 * 
 * Per SCREEN_LAYOUTS.md (Login Screen):
 * - Layout: 50/50 horizontal split
 * - LEFT: Branding (Station Name, Logo, Time, Footer)
 * - RIGHT: Authentication (QR Registration, Employee List, PIN Entry)
 * 
 * Per code-quality.mdc: State hoisting - this composable is stateless
 * and receives data via parameters, emits events via lambdas.
 * 
 * @param state Current UI state
 * @param onLoginClick Called when user taps login button
 * @param onErrorDismissed Called when user dismisses error
 */
@Composable
fun LoginContent(
    state: LoginUiState,
    onLoginClick: (username: String, pin: String) -> Unit,
    onErrorDismissed: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Local state for text fields
    var username by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    
    val isLoading = state is LoginUiState.Loading
    
    Row(
        modifier = modifier.fillMaxSize()
    ) {
        // ====================================================================
        // LEFT SECTION (50%) - Branding
        // Per UI_DESIGN_SYSTEM.md: PrimaryGreen background
        // ====================================================================
        LeftBrandingSection(
            modifier = Modifier
                .weight(0.5f)
                .fillMaxHeight()
        )
        
        // ====================================================================
        // RIGHT SECTION (50%) - Authentication
        // Per UI_DESIGN_SYSTEM.md: LightGray1 background
        // ====================================================================
        RightAuthSection(
            username = username,
            pin = pin,
            state = state,
            isLoading = isLoading,
            onUsernameChange = { username = it },
            onPinChange = { newPin ->
                // Only allow digits, max 4 characters
                if (newPin.length <= 4 && newPin.all { it.isDigit() }) {
                    pin = newPin
                }
            },
            onPinDigit = { digit ->
                if (pin.length < 4 && digit.all { it.isDigit() }) {
                    pin += digit
                }
            },
            onPinClear = { pin = "" },
            onPinBackspace = { 
                if (pin.isNotEmpty()) {
                    pin = pin.dropLast(1)
                }
            },
            onLoginClick = {
                onLoginClick(username, pin)
                pin = "" // Clear PIN after submission (security)
            },
            onErrorDismissed = onErrorDismissed,
            modifier = Modifier
                .weight(0.5f)
                .fillMaxHeight()
        )
    }
}

// ============================================================================
// LEFT SECTION - Branding
// ============================================================================

@Composable
private fun LeftBrandingSection(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(GroPOSColors.PrimaryGreen)
            .padding(GroPOSSpacing.XXL)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Station Name Box
            WhiteBox {
                Text(
                    text = "Station 1",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = GroPOSColors.TextPrimary
                )
            }
            
            // Logo Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Logo Placeholder
                    Surface(
                        modifier = Modifier.size(180.dp),
                        shape = RoundedCornerShape(GroPOSRadius.Pill),
                        color = GroPOSColors.White.copy(alpha = 0.15f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "GroPOS",
                                style = MaterialTheme.typography.displayLarge,
                                fontWeight = FontWeight.Bold,
                                color = GroPOSColors.White
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(GroPOSSpacing.L))
                    
                    Text(
                        text = "Point of Sale System",
                        style = MaterialTheme.typography.headlineMedium,
                        color = GroPOSColors.White.copy(alpha = 0.9f)
                    )
                }
            }
            
            // Footer
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Time Display
                Text(
                    text = "12:00 PM", // TODO: Replace with actual time
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = GroPOSColors.White
                )
                
                Spacer(modifier = Modifier.height(GroPOSSpacing.M))
                
                // Copyright
                Text(
                    text = "© Unisight BIT 2024",
                    style = MaterialTheme.typography.bodySmall,
                    color = GroPOSColors.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ============================================================================
// RIGHT SECTION - Authentication
// ============================================================================

@Composable
private fun RightAuthSection(
    username: String,
    pin: String,
    state: LoginUiState,
    isLoading: Boolean,
    onUsernameChange: (String) -> Unit,
    onPinChange: (String) -> Unit,
    onPinDigit: (String) -> Unit,
    onPinClear: () -> Unit,
    onPinBackspace: () -> Unit,
    onLoginClick: () -> Unit,
    onErrorDismissed: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(GroPOSColors.LightGray1)
            .padding(GroPOSSpacing.XXL),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Welcome Header
        Text(
            text = "Welcome to GroPOS",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = GroPOSColors.TextPrimary
        )
        
        Spacer(modifier = Modifier.height(GroPOSSpacing.XL))
        
        // Username Input
        WhiteBox(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(
                    text = "Username",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GroPOSColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(GroPOSSpacing.S))
                androidx.compose.material3.OutlinedTextField(
                    value = username,
                    onValueChange = onUsernameChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("username_field"),
                    enabled = !isLoading,
                    placeholder = { Text("Enter username") },
                    singleLine = true,
                    shape = RoundedCornerShape(GroPOSRadius.Small)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(GroPOSSpacing.M))
        
        // PIN Display
        WhiteBox(modifier = Modifier.fillMaxWidth()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Enter PIN",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GroPOSColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(GroPOSSpacing.S))
                
                // PIN Dots Display
                Row(
                    horizontalArrangement = Arrangement.spacedBy(GroPOSSpacing.M),
                    modifier = Modifier.padding(vertical = GroPOSSpacing.M)
                ) {
                    repeat(4) { index ->
                        PinDot(filled = index < pin.length)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(GroPOSSpacing.M))
        
        // Error/Success Message
        when (state) {
            is LoginUiState.Error -> {
                ErrorCard(
                    message = state.message,
                    onDismiss = onErrorDismissed,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(GroPOSSpacing.M))
            }
            is LoginUiState.Success -> {
                SuccessCard(
                    username = state.user.username,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(GroPOSSpacing.M))
            }
            else -> {}
        }
        
        // Ten-Key Pad for PIN Entry
        TenKey(
            state = TenKeyState(inputValue = pin, mode = TenKeyMode.Login),
            onDigitClick = onPinDigit,
            onOkClick = { 
                if (username.isNotBlank() && pin.length == 4) {
                    onLoginClick()
                }
            },
            onClearClick = onPinClear,
            onBackspaceClick = onPinBackspace,
            showQtyButton = false,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("pin_keypad")
        )
        
        Spacer(modifier = Modifier.height(GroPOSSpacing.M))
        
        // Sign In Button
        SuccessButton(
            onClick = onLoginClick,
            enabled = !isLoading && username.isNotBlank() && pin.length == 4,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("login_button")
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = GroPOSColors.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Sign In",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Footer Actions (Keypad/NFC/Back)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlineButton(onClick = { /* TODO: Keypad toggle */ }) {
                Text("Keypad")
            }
            OutlineButton(onClick = { /* TODO: NFC toggle */ }) {
                Text("NFC")
            }
            OutlineButton(onClick = { /* TODO: Back */ }) {
                Text("Back")
            }
        }
    }
}

// ============================================================================
// Helper Components
// ============================================================================

@Composable
private fun PinDot(filled: Boolean) {
    Surface(
        modifier = Modifier.size(20.dp),
        shape = RoundedCornerShape(GroPOSRadius.Round),
        color = if (filled) GroPOSColors.PrimaryGreen else GroPOSColors.DisabledGray
    ) {}
}

@Composable
private fun ErrorCard(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(GroPOSRadius.Medium),
        color = Color(0xFFFFDAD6) // Light red
    ) {
        Column(
            modifier = Modifier.padding(GroPOSSpacing.M),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = GroPOSColors.DangerRed,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(GroPOSSpacing.S))
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Dismiss",
                    color = GroPOSColors.DangerRed
                )
            }
        }
    }
}

@Composable
private fun SuccessCard(
    username: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(GroPOSRadius.Medium),
        color = GroPOSColors.SnapBadgeBackground // Light green
    ) {
        Text(
            text = "Welcome, $username!",
            style = MaterialTheme.typography.bodyLarge,
            color = GroPOSColors.PrimaryGreen,
            modifier = Modifier
                .padding(GroPOSSpacing.M)
                .fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

// ============================================================================
// Previews (per agent-behavior.mdc: Must generate @Preview for distinct states)
// ============================================================================

@Composable
fun LoginContentPreviewIdle() {
    Surface {
        LoginContent(
            state = LoginUiState.Idle,
            onLoginClick = { _, _ -> },
            onErrorDismissed = {}
        )
    }
}

@Composable
fun LoginContentPreviewLoading() {
    Surface {
        LoginContent(
            state = LoginUiState.Loading,
            onLoginClick = { _, _ -> },
            onErrorDismissed = {}
        )
    }
}

@Composable
fun LoginContentPreviewError() {
    Surface {
        LoginContent(
            state = LoginUiState.Error(message = "Invalid username or PIN"),
            onLoginClick = { _, _ -> },
            onErrorDismissed = {}
        )
    }
}

@Composable
fun LoginContentPreviewSuccess() {
    Surface {
        LoginContent(
            state = LoginUiState.Success(
                user = AuthUser(
                    id = "1",
                    username = "admin",
                    role = UserRole.ADMIN
                )
            ),
            onLoginClick = { _, _ -> },
            onErrorDismissed = {}
        )
    }
}
