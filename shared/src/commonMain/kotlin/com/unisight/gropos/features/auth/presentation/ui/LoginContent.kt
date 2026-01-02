package com.unisight.gropos.features.auth.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unisight.gropos.core.components.OutlineButton
import com.unisight.gropos.core.components.SecretTriggerFooter
import com.unisight.gropos.core.components.SuccessButton
import com.unisight.gropos.core.components.TenKey
import com.unisight.gropos.core.components.TenKeyMode
import com.unisight.gropos.core.components.TenKeyState
import com.unisight.gropos.core.components.WhiteBox
import com.unisight.gropos.core.components.dialogs.TillSelectionDialog
import com.unisight.gropos.core.theme.GroPOSColors
import com.unisight.gropos.core.theme.GroPOSRadius
import com.unisight.gropos.core.theme.GroPOSSpacing
import com.unisight.gropos.features.auth.presentation.EmployeeUiModel
import com.unisight.gropos.features.auth.presentation.LoginStage
import com.unisight.gropos.features.auth.presentation.LoginUiState

/**
 * Login screen content with hoisted state.
 * 
 * Per SCREEN_LAYOUTS.md & CASHIER_OPERATIONS.md:
 * - State machine: LOADING -> EMPLOYEE_SELECT -> PIN_ENTRY -> TILL_ASSIGNMENT -> SUCCESS
 * - Layout: 50/50 horizontal split
 * - LEFT: Branding (Station Name, Logo, Time)
 * - RIGHT: Authentication (Employee Grid / PIN Entry)
 */
@Composable
fun LoginContent(
    state: LoginUiState,
    onEmployeeSelected: (EmployeeUiModel) -> Unit,
    onPinDigit: (String) -> Unit,
    onPinClear: () -> Unit,
    onPinBackspace: () -> Unit,
    onPinSubmit: () -> Unit,
    onTillSelected: (Int) -> Unit,
    onBackPressed: () -> Unit,
    onErrorDismissed: () -> Unit,
    onRefresh: () -> Unit,
    onAdminSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxSize()
    ) {
        // LEFT SECTION (50%) - Branding
        LeftBrandingSection(
            stationName = state.stationName,
            currentTime = state.currentTime,
            version = state.version,
            showBackButton = state.stage == LoginStage.PIN_ENTRY || state.stage == LoginStage.TILL_ASSIGNMENT,
            onBackPressed = onBackPressed,
            onAdminSettingsClick = onAdminSettingsClick,
            modifier = Modifier
                .weight(0.5f)
                .fillMaxHeight()
        )
        
        // RIGHT SECTION (50%) - Authentication
        RightAuthSection(
            state = state,
            onEmployeeSelected = onEmployeeSelected,
            onPinDigit = onPinDigit,
            onPinClear = onPinClear,
            onPinBackspace = onPinBackspace,
            onPinSubmit = onPinSubmit,
            onTillSelected = onTillSelected,
            onBackPressed = onBackPressed,
            onErrorDismissed = onErrorDismissed,
            onRefresh = onRefresh,
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
private fun LeftBrandingSection(
    stationName: String,
    currentTime: String,
    version: String,
    showBackButton: Boolean,
    onBackPressed: () -> Unit,
    onAdminSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(GroPOSColors.PrimaryGreen)
            .padding(GroPOSSpacing.XXL)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Row: Back Button + Station Name
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showBackButton) {
                    IconButton(
                        onClick = onBackPressed,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(GroPOSColors.White.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = GroPOSColors.White
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }
                
                WhiteBox {
                    Text(
                        text = stationName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = GroPOSColors.TextPrimary
                    )
                }
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
            
            // Footer with Secret Admin Trigger
            SecretTriggerFooter(
                currentTime = currentTime,
                version = version,
                onAdminSettingsClick = onAdminSettingsClick
            )
        }
    }
}

// ============================================================================
// RIGHT SECTION - Authentication (State Machine)
// ============================================================================

@Composable
private fun RightAuthSection(
    state: LoginUiState,
    onEmployeeSelected: (EmployeeUiModel) -> Unit,
    onPinDigit: (String) -> Unit,
    onPinClear: () -> Unit,
    onPinBackspace: () -> Unit,
    onPinSubmit: () -> Unit,
    onTillSelected: (Int) -> Unit,
    onBackPressed: () -> Unit,
    onErrorDismissed: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(GroPOSColors.LightGray1)
            .padding(GroPOSSpacing.XL),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (state.stage) {
            LoginStage.LOADING -> LoadingContent()
            LoginStage.EMPLOYEE_SELECT -> EmployeeSelectContent(
                employees = state.employees,
                errorMessage = state.errorMessage,
                onEmployeeSelected = onEmployeeSelected,
                onErrorDismissed = onErrorDismissed,
                onRefresh = onRefresh
            )
            LoginStage.PIN_ENTRY -> PinEntryContent(
                employee = state.selectedEmployee,
                pinDots = state.pinDots,
                canSubmit = state.canSubmitPin,
                isLoading = state.isLoading,
                errorMessage = state.errorMessage,
                onPinDigit = onPinDigit,
                onPinClear = onPinClear,
                onPinBackspace = onPinBackspace,
                onPinSubmit = onPinSubmit,
                onErrorDismissed = onErrorDismissed
            )
            LoginStage.TILL_ASSIGNMENT -> {
                // Show PIN entry in background with Till dialog overlay
                PinEntryContent(
                    employee = state.selectedEmployee,
                    pinDots = state.pinDots,
                    canSubmit = false,
                    isLoading = state.isLoading,
                    errorMessage = null,
                    onPinDigit = {},
                    onPinClear = {},
                    onPinBackspace = {},
                    onPinSubmit = {},
                    onErrorDismissed = {}
                )
                
                TillSelectionDialog(
                    tills = state.tills,
                    onTillSelected = onTillSelected,
                    onDismiss = onBackPressed
                )
            }
            LoginStage.SUCCESS -> SuccessContent()
        }
    }
}

// ============================================================================
// LOADING Stage
// ============================================================================

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                color = GroPOSColors.PrimaryGreen,
                strokeWidth = 4.dp
            )
            Spacer(modifier = Modifier.height(GroPOSSpacing.L))
            Text(
                text = "Loading employees...",
                style = MaterialTheme.typography.titleMedium,
                color = GroPOSColors.TextSecondary
            )
        }
    }
}

// ============================================================================
// EMPLOYEE_SELECT Stage
// ============================================================================

@Composable
private fun EmployeeSelectContent(
    employees: List<EmployeeUiModel>,
    errorMessage: String?,
    onEmployeeSelected: (EmployeeUiModel) -> Unit,
    onErrorDismissed: () -> Unit,
    onRefresh: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Text(
            text = "On Site Cashiers",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = GroPOSColors.TextPrimary
        )
        
        Spacer(modifier = Modifier.height(GroPOSSpacing.S))
        
        Text(
            text = "Tap to select your name",
            style = MaterialTheme.typography.bodyLarge,
            color = GroPOSColors.TextSecondary
        )
        
        Spacer(modifier = Modifier.height(GroPOSSpacing.L))
        
        // Error Message
        if (errorMessage != null) {
            ErrorCard(
                message = errorMessage,
                onDismiss = onErrorDismissed,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(GroPOSSpacing.M))
        }
        
        // Employee Grid
        if (employees.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No employees scheduled",
                        style = MaterialTheme.typography.titleMedium,
                        color = GroPOSColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(GroPOSSpacing.M))
                    OutlineButton(onClick = onRefresh) {
                        Text("Refresh")
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 140.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("employee_grid"),
                horizontalArrangement = Arrangement.spacedBy(GroPOSSpacing.M),
                verticalArrangement = Arrangement.spacedBy(GroPOSSpacing.M)
            ) {
                items(employees, key = { it.id }) { employee ->
                    EmployeeCard(
                        employee = employee,
                        onClick = { onEmployeeSelected(employee) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmployeeCard(
    employee: EmployeeUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag("employee_card_${employee.id}"),
        shape = RoundedCornerShape(GroPOSRadius.Medium),
        color = GroPOSColors.White,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(GroPOSSpacing.M),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar
            Surface(
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                color = GroPOSColors.PrimaryGreen.copy(alpha = 0.1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    if (employee.imageUrl != null) {
                        // TODO: AsyncImage when we have image loading
                        Text(
                            text = employee.initials,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = GroPOSColors.PrimaryGreen
                        )
                    } else {
                        Text(
                            text = employee.initials,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = GroPOSColors.PrimaryGreen
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(GroPOSSpacing.M))
            
            // Name
            Text(
                text = employee.fullName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = GroPOSColors.TextPrimary,
                textAlign = TextAlign.Center
            )
            
            // Role
            Text(
                text = employee.role,
                style = MaterialTheme.typography.bodySmall,
                color = GroPOSColors.TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ============================================================================
// PIN_ENTRY Stage
// ============================================================================

@Composable
private fun PinEntryContent(
    employee: EmployeeUiModel?,
    pinDots: String,
    canSubmit: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    onPinDigit: (String) -> Unit,
    onPinClear: () -> Unit,
    onPinBackspace: () -> Unit,
    onPinSubmit: () -> Unit,
    onErrorDismissed: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Selected Employee Info
        if (employee != null) {
            SelectedEmployeeHeader(employee = employee)
        }
        
        Spacer(modifier = Modifier.height(GroPOSSpacing.L))
        
        // PIN Entry Instructions
        Text(
            text = "Sign in using your hardware token or key code",
            style = MaterialTheme.typography.bodyLarge,
            color = GroPOSColors.TextSecondary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(GroPOSSpacing.XL))
        
        // PIN Display
        WhiteBox(modifier = Modifier.fillMaxWidth()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(GroPOSSpacing.M)
            ) {
                Text(
                    text = "Enter PIN",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GroPOSColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(GroPOSSpacing.S))
                
                // PIN Dots Display
                Text(
                    text = if (pinDots.isEmpty()) "_ _ _ _" else pinDots,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = GroPOSColors.TextPrimary,
                    letterSpacing = 8.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(GroPOSSpacing.M))
        
        // Error Message
        if (errorMessage != null) {
            ErrorCard(
                message = errorMessage,
                onDismiss = onErrorDismissed,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(GroPOSSpacing.M))
        }
        
        // Ten-Key Pad
        TenKey(
            state = TenKeyState(inputValue = "", mode = TenKeyMode.Login),
            onDigitClick = onPinDigit,
            onOkClick = { _ -> onPinSubmit() },
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
            onClick = onPinSubmit,
            enabled = canSubmit && !isLoading,
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
    }
}

@Composable
private fun SelectedEmployeeHeader(employee: EmployeeUiModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Surface(
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
            color = GroPOSColors.PrimaryGreen.copy(alpha = 0.1f)
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = employee.initials,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = GroPOSColors.PrimaryGreen
                )
            }
        }
        
        Spacer(modifier = Modifier.width(GroPOSSpacing.M))
        
        Column {
            Text(
                text = employee.fullName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = GroPOSColors.TextPrimary
            )
            Text(
                text = employee.role,
                style = MaterialTheme.typography.bodyMedium,
                color = GroPOSColors.TextSecondary
            )
        }
    }
}

// ============================================================================
// SUCCESS Stage
// ============================================================================

@Composable
private fun SuccessContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                color = GroPOSColors.PrimaryGreen,
                strokeWidth = 4.dp
            )
            Spacer(modifier = Modifier.height(GroPOSSpacing.L))
            Text(
                text = "Login successful!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = GroPOSColors.PrimaryGreen
            )
            Text(
                text = "Redirecting...",
                style = MaterialTheme.typography.bodyMedium,
                color = GroPOSColors.TextSecondary
            )
        }
    }
}

// ============================================================================
// Helper Components
// ============================================================================

@Composable
private fun ErrorCard(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(GroPOSRadius.Medium),
        color = Color(0xFFFFDAD6)
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

// ============================================================================
// Secret Trigger Footer
// ============================================================================
// Note: SecretTriggerFooter has been extracted to core/components/SecretTriggerFooter.kt
// for reuse across LoginScreen and RegistrationScreen
