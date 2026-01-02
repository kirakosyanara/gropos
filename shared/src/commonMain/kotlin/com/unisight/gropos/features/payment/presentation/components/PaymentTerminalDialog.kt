package com.unisight.gropos.features.payment.presentation.components

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
 * Modal dialog shown when waiting for payment terminal input.
 * 
 * Per SCREEN_LAYOUTS.md (Payment Screen):
 * - Modal overlay while waiting for card input
 * - Shows amount, spinner, and cancel button
 * 
 * Per PAYMENT_PROCESSING.md:
 * - Non-blocking UI using coroutines
 * - User can cancel while waiting
 * 
 * Testability:
 * - Uses testTag for UI testing per testing-strategy.mdc
 */
@Composable
fun PaymentTerminalDialog(
    amount: String,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Pulsing animation for the card icon
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .testTag("payment_terminal_dialog"),
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
                // Card Icon with pulsing animation
                Text(
                    text = "💳",
                    style = MaterialTheme.typography.displayLarge,
                    modifier = Modifier
                        .alpha(alpha)
                        .testTag("terminal_card_icon")
                )
                
                Spacer(modifier = Modifier.height(GroPOSSpacing.L))
                
                // Title
                Text(
                    text = "Please Insert Card on Terminal",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("terminal_instruction_text")
                )
                
                Spacer(modifier = Modifier.height(GroPOSSpacing.M))
                
                // Amount
                Text(
                    text = "Amount: $amount",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = GroPOSColors.PrimaryGreen,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("terminal_amount_text")
                )
                
                Spacer(modifier = Modifier.height(GroPOSSpacing.XL))
                
                // Spinner
                CircularProgressIndicator(
                    color = GroPOSColors.PrimaryGreen,
                    strokeWidth = 4.dp,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("terminal_progress_indicator")
                )
                
                Spacer(modifier = Modifier.height(GroPOSSpacing.M))
                
                Text(
                    text = "Waiting for card...",
                    style = MaterialTheme.typography.bodyLarge,
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
                        .testTag("terminal_cancel_button")
                ) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

