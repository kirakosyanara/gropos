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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unisight.gropos.core.components.DangerButton
import com.unisight.gropos.core.components.OutlineButton
import com.unisight.gropos.core.components.SuccessButton
import com.unisight.gropos.core.components.TenKey
import com.unisight.gropos.core.components.TenKeyState
import com.unisight.gropos.core.components.WhiteBox
import com.unisight.gropos.core.theme.GroPOSColors
import com.unisight.gropos.core.theme.GroPOSRadius
import com.unisight.gropos.core.theme.GroPOSSpacing
import com.unisight.gropos.features.auth.presentation.LockType
import com.unisight.gropos.features.auth.presentation.LockUiState

/**
 * Test tags for Lock Screen elements.
 */
object LockScreenTestTags {
    const val SCREEN = "lock_screen"
    const val LEFT_PANEL = "lock_left_panel"
    const val RIGHT_PANEL = "lock_right_panel"
    const val CLOCK = "lock_clock"
    const val DATE = "lock_date"
    const val STATION_NAME = "lock_station_name"
    const val EMPLOYEE_NAME = "lock_employee_name"
    const val PIN_DISPLAY = "lock_pin_display"
    const val TEN_KEY = "lock_ten_key"
    const val VERIFY_BUTTON = "lock_verify_button"
    const val SIGN_OUT_BUTTON = "lock_sign_out_button"
}

/**
 * Lock Screen Content.
 *
 * Per SCREEN_LAYOUTS.md: Two-panel layout
 * - Left: Station info, large clock, version footer
 * - Right: Employee info, PIN entry, Verify/Sign Out buttons
 *
 * Per UI_DESIGN_SYSTEM.md: Dark theme for lock screen creates
 * visual distinction from main app and reduces burn-in.
 */
@Composable
fun LockContent(
    state: LockUiState,
    onPinDigit: (String) -> Unit,
    onPinClear: () -> Unit,
    onPinBackspace: () -> Unit,
    onVerify: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1A2E),  // Deep navy
                        Color(0xFF16213E),  // Dark blue
                        Color(0xFF0F3460)   // Medium blue
                    )
                )
            )
            .testTag(LockScreenTestTags.SCREEN)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // ================================================================
            // LEFT SECTION (40%) - Station Info & Clock
            // ================================================================
            LeftPanel(
                stationName = state.stationName,
                currentTime = state.currentTime,
                currentDate = state.currentDate,
                lockType = state.lockType,
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight()
                    .testTag(LockScreenTestTags.LEFT_PANEL)
            )
            
            // ================================================================
            // RIGHT SECTION (60%) - Unlock Controls
            // ================================================================
            RightPanel(
                state = state,
                onPinDigit = onPinDigit,
                onPinClear = onPinClear,
                onPinBackspace = onPinBackspace,
                onVerify = onVerify,
                onSignOut = onSignOut,
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxHeight()
                    .testTag(LockScreenTestTags.RIGHT_PANEL)
            )
        }
    }
}

// ============================================================================
// LEFT PANEL - Station Info & Clock
// ============================================================================

@Composable
private fun LeftPanel(
    stationName: String,
    currentTime: String,
    currentDate: String,
    lockType: LockType,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(GroPOSSpacing.XXXL),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top: Station Name & Lock Status
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Lock Icon
            Surface(
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                color = GroPOSColors.WarningOrange.copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = GroPOSColors.WarningOrange,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(GroPOSSpacing.L))
            
            Text(
                text = "LOCKED",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = GroPOSColors.WarningOrange,
                letterSpacing = 4.sp
            )
            
            Spacer(modifier = Modifier.height(GroPOSSpacing.S))
            
            Text(
                text = when (lockType) {
                    LockType.AutoLocked -> "Session timed out"
                    LockType.Locked -> "Manually locked"
                    LockType.ManagerLocked -> "Locked by manager"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(GroPOSSpacing.XXL))
            
            // Station Name
            Text(
                text = stationName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.testTag(LockScreenTestTags.STATION_NAME)
            )
        }
        
        // Center: Large Clock
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = currentTime,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 72.sp,
                    letterSpacing = 2.sp
                ),
                fontWeight = FontWeight.Light,
                color = Color.White,
                modifier = Modifier.testTag(LockScreenTestTags.CLOCK)
            )
            
            Spacer(modifier = Modifier.height(GroPOSSpacing.S))
            
            Text(
                text = currentDate,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.testTag(LockScreenTestTags.DATE)
            )
        }
        
        // Bottom: Version & Copyright
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "v1.0.0",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f)
            )
            
            Spacer(modifier = Modifier.height(GroPOSSpacing.XS))
            
            Text(
                text = "©Unisight BIT 2026",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f)
            )
            
            Spacer(modifier = Modifier.height(GroPOSSpacing.M))
            
            Text(
                text = "Touch to unlock",
                style = MaterialTheme.typography.bodyMedium,
                color = GroPOSColors.PrimaryGreen,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ============================================================================
// RIGHT PANEL - Unlock Controls
// ============================================================================

@Composable
private fun RightPanel(
    state: LockUiState,
    onPinDigit: (String) -> Unit,
    onPinClear: () -> Unit,
    onPinBackspace: () -> Unit,
    onVerify: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.05f))
            .padding(GroPOSSpacing.XXXL),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Header
        Text(
            text = "Enter PIN to unlock",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
        
        Spacer(modifier = Modifier.height(GroPOSSpacing.XXL))
        
        // Employee Info Card
        WhiteBox(
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(GroPOSSpacing.M),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Surface(
                    modifier = Modifier.size(60.dp),
                    shape = CircleShape,
                    color = GroPOSColors.PrimaryGreen.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Employee",
                            tint = GroPOSColors.PrimaryGreen,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(GroPOSSpacing.M))
                
                Column {
                    Text(
                        text = state.employeeName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GroPOSColors.TextPrimary,
                        modifier = Modifier.testTag(LockScreenTestTags.EMPLOYEE_NAME)
                    )
                    Text(
                        text = state.employeeRole,
                        style = MaterialTheme.typography.bodyMedium,
                        color = GroPOSColors.TextSecondary
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(GroPOSSpacing.XL))
        
        // PIN Display
        PinDisplay(
            pinLength = state.pinDotCount,
            maxLength = 8,
            modifier = Modifier.testTag(LockScreenTestTags.PIN_DISPLAY)
        )
        
        // Error Message
        state.errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(GroPOSSpacing.S))
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = GroPOSColors.DangerRed,
                textAlign = TextAlign.Center
            )
        }
        
        Spacer(modifier = Modifier.height(GroPOSSpacing.L))
        
        // TenKey Pad
        Surface(
            modifier = Modifier.fillMaxWidth(0.6f),
            shape = RoundedCornerShape(GroPOSRadius.Medium),
            color = Color.White
        ) {
            TenKey(
                state = TenKeyState(inputValue = state.pinInput),
                onDigitClick = onPinDigit,
                onOkClick = { onVerify() },
                onClearClick = onPinClear,
                onBackspaceClick = onPinBackspace,
                showQtyButton = false,
                modifier = Modifier
                    .padding(GroPOSSpacing.M)
                    .testTag(LockScreenTestTags.TEN_KEY)
            )
        }
        
        Spacer(modifier = Modifier.height(GroPOSSpacing.XL))
        
        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(0.7f),
            horizontalArrangement = Arrangement.spacedBy(GroPOSSpacing.M)
        ) {
            // Sign Out Button
            OutlineButton(
                onClick = onSignOut,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .testTag(LockScreenTestTags.SIGN_OUT_BUTTON)
            ) {
                Text(
                    text = "Sign Out",
                    color = Color.White
                )
            }
            
            // Verify Button
            SuccessButton(
                onClick = onVerify,
                enabled = !state.isVerifying && state.pinInput.isNotEmpty(),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .testTag(LockScreenTestTags.VERIFY_BUTTON)
            ) {
                if (state.isVerifying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Unlock",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ============================================================================
// PIN Display Component
// ============================================================================

@Composable
private fun PinDisplay(
    pinLength: Int,
    maxLength: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(GroPOSSpacing.S)
    ) {
        repeat(maxLength) { index ->
            val isFilled = index < pinLength
            Surface(
                modifier = Modifier.size(16.dp),
                shape = CircleShape,
                color = if (isFilled) GroPOSColors.PrimaryGreen else Color.White.copy(alpha = 0.3f)
            ) {}
        }
    }
}

