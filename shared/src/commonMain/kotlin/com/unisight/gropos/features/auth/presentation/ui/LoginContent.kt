package com.unisight.gropos.features.auth.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unisight.gropos.features.auth.domain.model.AuthUser
import com.unisight.gropos.features.auth.domain.model.UserRole
import com.unisight.gropos.features.auth.presentation.LoginUiState

/**
 * Login screen content with hoisted state.
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
    
    // Rich gradient background
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1A2E),
                        Color(0xFF16213E),
                        Color(0xFF0F3460)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1F1F3D)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Logo/Title area
                Text(
                    text = "GroPOS",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 36.sp,
                        letterSpacing = 2.sp
                    ),
                    color = Color(0xFF00D9FF)
                )
                
                Text(
                    text = "Point of Sale System",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF8892B0)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Username field
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    placeholder = { Text("Enter username") },
                    singleLine = true,
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    colors = loginTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("username_field")
                )
                
                // PIN field
                OutlinedTextField(
                    value = pin,
                    onValueChange = { newValue ->
                        // Only allow digits, max 4 characters
                        if (newValue.length <= 4 && newValue.all { it.isDigit() }) {
                            pin = newValue
                        }
                    },
                    label = { Text("PIN") },
                    placeholder = { Text("4-digit PIN") },
                    singleLine = true,
                    enabled = !isLoading,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (username.isNotBlank() && pin.length == 4) {
                                onLoginClick(username, pin)
                                pin = "" // Clear PIN after submission (security)
                            }
                        }
                    ),
                    colors = loginTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pin_field")
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Error message
                if (state is LoginUiState.Error) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF4A1C1C)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("error_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = state.message,
                                color = Color(0xFFFF6B6B),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                            TextButton(
                                onClick = onErrorDismissed
                            ) {
                                Text(
                                    text = "Dismiss",
                                    color = Color(0xFFFF6B6B)
                                )
                            }
                        }
                    }
                }
                
                // Success message
                if (state is LoginUiState.Success) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1C4A2E)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("success_card")
                    ) {
                        Text(
                            text = "Welcome, ${state.user.username}!",
                            color = Color(0xFF6BFF8E),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Login button
                Button(
                    onClick = {
                        onLoginClick(username, pin)
                        pin = "" // Clear PIN after submission (security)
                    },
                    enabled = !isLoading && username.isNotBlank() && pin.length == 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("login_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00D9FF),
                        contentColor = Color(0xFF1A1A2E),
                        disabledContainerColor = Color(0xFF3D3D5C),
                        disabledContentColor = Color(0xFF8892B0)
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color(0xFF1A1A2E),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Sign In",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun loginTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color(0xFFCCD6F6),
    focusedBorderColor = Color(0xFF00D9FF),
    unfocusedBorderColor = Color(0xFF3D3D5C),
    focusedLabelColor = Color(0xFF00D9FF),
    unfocusedLabelColor = Color(0xFF8892B0),
    cursorColor = Color(0xFF00D9FF),
    focusedPlaceholderColor = Color(0xFF5C5C7A),
    unfocusedPlaceholderColor = Color(0xFF5C5C7A)
)

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

