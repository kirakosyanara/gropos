package com.unisight.gropos.features.device.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.Image
import org.jetbrains.skia.Image as SkiaImage
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unisight.gropos.core.components.SecretTriggerFooter
import com.unisight.gropos.core.components.WhiteBox
import com.unisight.gropos.core.theme.GroPOSColors
import com.unisight.gropos.core.theme.GroPOSRadius
import com.unisight.gropos.core.theme.GroPOSSpacing
import com.unisight.gropos.features.device.domain.model.RegistrationState
import com.unisight.gropos.features.device.presentation.RegistrationUiState

/**
 * Registration screen content with hoisted state.
 * 
 * Per DEVICE_REGISTRATION.md & SCREEN_LAYOUTS.md:
 * - Layout: 50/50 horizontal split (same as Login)
 * - LEFT: Branding with GroPOS logo
 * - RIGHT: Registration content (QR code, pairing code)
 * 
 * Visual Elements:
 * - Header: "Welcome to GroPOS"
 * - Body: Pairing Code (large typography), QR Code placeholder
 * - Instructions: "Go to admin.gropos.com to activate this terminal."
 * - Footer: SecretTriggerFooter for admin settings access
 */
@Composable
fun RegistrationContent(
    state: RegistrationUiState,
    onRefreshPairingCode: () -> Unit,
    onSimulateActivation: () -> Unit,
    onAdminSettingsClick: () -> Unit,
    onErrorDismiss: () -> Unit,
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
            onAdminSettingsClick = onAdminSettingsClick,
            modifier = Modifier
                .weight(0.5f)
                .fillMaxHeight()
        )
        
        // RIGHT SECTION (50%) - Registration
        RightRegistrationSection(
            state = state,
            onRefreshPairingCode = onRefreshPairingCode,
            onSimulateActivation = onSimulateActivation,
            onErrorDismiss = onErrorDismiss,
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
            // Header Row: Station Name
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
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
// RIGHT SECTION - Registration Content
// ============================================================================

@Composable
private fun RightRegistrationSection(
    state: RegistrationUiState,
    onRefreshPairingCode: () -> Unit,
    onSimulateActivation: () -> Unit,
    onErrorDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(GroPOSColors.LightGray1)
            .padding(GroPOSSpacing.XL),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (state.registrationState) {
            RegistrationState.REGISTERED -> RegistrationSuccessContent(
                branchName = state.branchName
            )
            RegistrationState.IN_PROGRESS -> RegistrationInProgressContent(
                branchName = state.branchName
            )
            RegistrationState.ERROR -> RegistrationErrorContent(
                errorMessage = state.errorMessage ?: "Registration failed",
                onRetry = onRefreshPairingCode,
                onDismiss = onErrorDismiss
            )
            else -> RegistrationPendingContent(
                state = state,
                onRefreshPairingCode = onRefreshPairingCode,
                onSimulateActivation = onSimulateActivation
            )
        }
    }
}

// ============================================================================
// Registration States
// ============================================================================

@Composable
private fun RegistrationPendingContent(
    state: RegistrationUiState,
    onRefreshPairingCode: () -> Unit,
    onSimulateActivation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(GroPOSSpacing.L),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            text = "Welcome to GroPOS",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = GroPOSColors.TextPrimary,
            modifier = Modifier.testTag("registration_header")
        )
        
        Spacer(modifier = Modifier.height(GroPOSSpacing.S))
        
        Text(
            text = "To register this station, scan the QR code or enter the pairing code.",
            style = MaterialTheme.typography.bodyLarge,
            color = GroPOSColors.TextSecondary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(GroPOSSpacing.XL))
        
        // QR Code Placeholder
        QrCodePlaceholder(
            qrCodeImage = state.qrCodeImage,
            modifier = Modifier.size(200.dp)
        )
        
        Spacer(modifier = Modifier.height(GroPOSSpacing.L))
        
        // Pairing Code Display
        PairingCodeDisplay(
            pairingCode = state.pairingCode,
            isLoading = state.isLoading
        )
        
        Spacer(modifier = Modifier.height(GroPOSSpacing.M))
        
        // Activation URL
        Text(
            text = "Go to ${state.activationUrl} to activate",
            style = MaterialTheme.typography.bodyMedium,
            color = GroPOSColors.TextSecondary
        )
        
        Spacer(modifier = Modifier.height(GroPOSSpacing.L))
        
        // Refresh Button
        TextButton(
            onClick = onRefreshPairingCode,
            enabled = !state.isLoading
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(GroPOSSpacing.S))
            Text("Generate New Code")
        }
        
        // Dev Mode: Simulate Activation Button
        if (state.isDevMode) {
            Spacer(modifier = Modifier.height(GroPOSSpacing.XL))
            
            Button(
                onClick = onSimulateActivation,
                enabled = !state.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GroPOSColors.WarningOrange
                ),
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(48.dp)
                    .testTag("simulate_activation_button")
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "⚡ Simulate Activation (Dev Only)",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(GroPOSSpacing.S))
            
            Text(
                text = "This button is only visible in development mode",
                style = MaterialTheme.typography.labelSmall,
                color = GroPOSColors.TextSecondary.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * Decodes a Base64 PNG image string to an ImageBitmap.
 * 
 * Per DEVICE_REGISTRATION.md: Backend returns qrCodeImage as Base64 PNG.
 */
@OptIn(ExperimentalEncodingApi::class)
private fun decodeBase64Image(base64String: String): ImageBitmap? {
    return try {
        val imageBytes = Base64.decode(base64String)
        val skiaImage = SkiaImage.makeFromEncoded(imageBytes)
        skiaImage.toComposeImageBitmap()
    } catch (e: Exception) {
        println("[QR] Failed to decode Base64 image: ${e.message}")
        null
    }
}

@Composable
private fun QrCodePlaceholder(
    qrCodeImage: String?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(GroPOSRadius.Medium))
            .border(
                width = 2.dp,
                color = GroPOSColors.LightGray2,
                shape = RoundedCornerShape(GroPOSRadius.Medium)
            ),
        color = Color.White
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            if (qrCodeImage != null) {
                // Decode and display Base64 QR image from backend
                val imageBitmap = decodeBase64Image(qrCodeImage)
                
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = "QR Code for device registration",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(GroPOSSpacing.S)
                    )
                } else {
                    // Fallback if decoding fails
                    Text(
                        text = "[QR Error]",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = GroPOSColors.DangerRed
                    )
                }
            } else {
                // Loading/placeholder state
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        color = GroPOSColors.PrimaryGreen
                    )
                    Spacer(modifier = Modifier.height(GroPOSSpacing.S))
                    Text(
                        text = "Loading QR Code...",
                        style = MaterialTheme.typography.labelMedium,
                        color = GroPOSColors.TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun PairingCodeDisplay(
    pairingCode: String,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Pairing Code",
            style = MaterialTheme.typography.labelLarge,
            color = GroPOSColors.TextSecondary
        )
        
        Spacer(modifier = Modifier.height(GroPOSSpacing.S))
        
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(GroPOSRadius.Medium)),
            color = GroPOSColors.PrimaryGreen.copy(alpha = 0.1f)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.padding(horizontal = GroPOSSpacing.XL, vertical = GroPOSSpacing.M),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = GroPOSColors.PrimaryGreen,
                        strokeWidth = 3.dp
                    )
                }
            } else {
                Text(
                    text = pairingCode.ifEmpty { "XXXX-XXXX" },
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 4.sp,
                    color = GroPOSColors.PrimaryGreen,
                    modifier = Modifier
                        .padding(horizontal = GroPOSSpacing.XL, vertical = GroPOSSpacing.M)
                        .testTag("pairing_code_display")
                )
            }
        }
    }
}

@Composable
private fun RegistrationInProgressContent(
    branchName: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(GroPOSSpacing.XL),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp),
            color = GroPOSColors.PrimaryGreen,
            strokeWidth = 4.dp
        )
        
        Spacer(modifier = Modifier.height(GroPOSSpacing.L))
        
        Text(
            text = "Configuring...",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = GroPOSColors.TextPrimary
        )
        
        if (branchName.isNotEmpty()) {
            Spacer(modifier = Modifier.height(GroPOSSpacing.M))
            
            Text(
                text = branchName,
                style = MaterialTheme.typography.titleMedium,
                color = GroPOSColors.TextSecondary
            )
        }
    }
}

@Composable
private fun RegistrationSuccessContent(
    branchName: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(GroPOSSpacing.XL),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Success",
            modifier = Modifier.size(80.dp),
            tint = GroPOSColors.PrimaryGreen
        )
        
        Spacer(modifier = Modifier.height(GroPOSSpacing.L))
        
        Text(
            text = "Registration Complete!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = GroPOSColors.PrimaryGreen
        )
        
        Spacer(modifier = Modifier.height(GroPOSSpacing.M))
        
        Text(
            text = branchName,
            style = MaterialTheme.typography.titleMedium,
            color = GroPOSColors.TextSecondary
        )
        
        Spacer(modifier = Modifier.height(GroPOSSpacing.L))
        
        Text(
            text = "Redirecting to login...",
            style = MaterialTheme.typography.bodyMedium,
            color = GroPOSColors.TextSecondary
        )
    }
}

@Composable
private fun RegistrationErrorContent(
    errorMessage: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(GroPOSSpacing.XL),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Error",
            modifier = Modifier.size(64.dp),
            tint = GroPOSColors.DangerRed
        )
        
        Spacer(modifier = Modifier.height(GroPOSSpacing.L))
        
        Text(
            text = "Registration Failed",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = GroPOSColors.DangerRed
        )
        
        Spacer(modifier = Modifier.height(GroPOSSpacing.M))
        
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = GroPOSColors.TextSecondary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(GroPOSSpacing.XL))
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(GroPOSSpacing.M)
        ) {
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
            
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GroPOSColors.PrimaryGreen
                )
            ) {
                Text("Try Again")
            }
        }
    }
}

