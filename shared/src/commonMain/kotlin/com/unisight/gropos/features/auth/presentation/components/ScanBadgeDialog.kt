package com.unisight.gropos.features.auth.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.unisight.gropos.core.components.DangerButton
import com.unisight.gropos.core.theme.GroPOSColors
import com.unisight.gropos.core.theme.GroPOSRadius
import com.unisight.gropos.core.theme.GroPOSSpacing

/**
 * Modal dialog shown when waiting for NFC badge scan.
 *
 * Per ANDROID_HARDWARE_GUIDE.md:
 * - NFC reader emits token (badge ID) for authentication
 * - Dialog provides visual feedback during scan
 *
 * Visuals:
 * - Icon of a Card tapping a Reader with pulse animation
 * - Text: "Please Tap Employee Badge"
 * - Cancel button for fallback to PIN entry
 *
 * Governance:
 * - Non-Blocking: UI remains responsive during scan
 * - Fallback: Users can always Cancel and use PIN pad
 */
@Composable
fun ScanBadgeDialog(
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Pulsing animation for the NFC icon
    val infiniteTransition = rememberInfiniteTransition(label = "nfc_pulse")
    
    // Alpha pulse for breathing effect
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha_pulse"
    )
    
    // Scale pulse for "tap" effect
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale_pulse"
    )
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .testTag("scan_badge_dialog"),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(GroPOSRadius.Large),
            color = GroPOSColors.White,
            shadowElevation = 16.dp,
            modifier = Modifier.padding(GroPOSSpacing.XL)
        ) {
            Column(
                modifier = Modifier
                    .padding(GroPOSSpacing.XXL)
                    .fillMaxWidth(0.5f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // NFC Tap Icon Area with pulse animation
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .scale(scale)
                        .testTag("scan_badge_icon_container"),
                    contentAlignment = Alignment.Center
                ) {
                    // Pulsing ring effect
                    Surface(
                        modifier = Modifier
                            .size(140.dp)
                            .alpha(alpha * 0.3f),
                        shape = CircleShape,
                        color = GroPOSColors.PrimaryBlue.copy(alpha = 0.2f)
                    ) {}
                    
                    Surface(
                        modifier = Modifier
                            .size(120.dp)
                            .alpha(alpha * 0.5f),
                        shape = CircleShape,
                        color = GroPOSColors.PrimaryBlue.copy(alpha = 0.3f)
                    ) {}
                    
                    // Main icon container with emoji
                    Surface(
                        modifier = Modifier.size(100.dp),
                        shape = CircleShape,
                        color = GroPOSColors.PrimaryBlue.copy(alpha = 0.15f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            // Badge/ID Card emoji with pulse
                            Text(
                                text = "🪪",
                                style = MaterialTheme.typography.displayLarge,
                                modifier = Modifier.alpha(alpha)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(GroPOSSpacing.L))
                
                // Title
                Text(
                    text = "Please Tap Employee Badge",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = GroPOSColors.TextPrimary,
                    modifier = Modifier.testTag("scan_badge_title")
                )
                
                Spacer(modifier = Modifier.height(GroPOSSpacing.M))
                
                // Instructions
                Text(
                    text = "Hold your badge near the reader",
                    style = MaterialTheme.typography.bodyLarge,
                    color = GroPOSColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("scan_badge_instruction")
                )
                
                Spacer(modifier = Modifier.height(GroPOSSpacing.XL))
                
                // Spinner
                CircularProgressIndicator(
                    color = GroPOSColors.PrimaryBlue,
                    strokeWidth = 4.dp,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("scan_badge_progress")
                )
                
                Spacer(modifier = Modifier.height(GroPOSSpacing.M))
                
                Text(
                    text = "Waiting for badge...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GroPOSColors.TextSecondary,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(GroPOSSpacing.XXL))
                
                // Cancel button
                DangerButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("scan_badge_cancel_button")
                ) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(GroPOSSpacing.S))
                
                // Fallback hint
                Text(
                    text = "or use PIN to sign in",
                    style = MaterialTheme.typography.bodySmall,
                    color = GroPOSColors.TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

