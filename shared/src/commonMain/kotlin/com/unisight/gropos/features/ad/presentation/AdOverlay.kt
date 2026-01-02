package com.unisight.gropos.features.ad.presentation

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.unisight.gropos.core.theme.GroPOSColors

/**
 * Advertisement Overlay / Screensaver Component
 *
 * Per SCREEN_LAYOUTS.md - Advertisement Overlay (Customer Screen section):
 * "Full-screen ad display when transaction is idle:
 *  - Image advertisements (cycles through configured images)
 *  - Animated discount labels
 *  - Brand promotion scrolling text
 *  - Controlled by fullScreenAdProperty in OrderStore"
 *
 * Per P3 Feature #1 requirement:
 * - Show branding/ads after idle timeout (30s for testing, 60s in production)
 * - Tap to dismiss instantly
 * - Non-blocking: disappears immediately on touch
 * - State preservation: cart/transaction state unchanged when dismissed
 *
 * Visual spec:
 * - Full-screen Box with z-index: 100 (above all content)
 * - Background: Gradient or placeholder image
 * - Foreground: "Welcome to GroPOS" / "Tap to Start"
 * - Animation: Gentle pulse/fade-in
 *
 * @param isVisible Whether the overlay should be shown.
 * @param onDismiss Callback when user taps anywhere to dismiss.
 */
@Composable
fun AdOverlay(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return
    
    // Animation setup for pulsing effect
    val infiniteTransition = rememberInfiniteTransition(label = "ad_overlay_animation")
    
    // Gentle pulse animation for the tap indicator
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    
    // Subtle breathing alpha animation for text
    val breathingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing_alpha"
    )
    
    // Interaction source to absorb ripple effects
    val interactionSource = remember { MutableInteractionSource() }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(100f) // Ensure it's on top of everything
            .testTag(AdOverlayTestTags.OVERLAY)
            .clickable(
                interactionSource = interactionSource,
                indication = null // No ripple - clean dismiss
            ) {
                onDismiss()
            }
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        AdOverlayColors.GradientTop,
                        AdOverlayColors.GradientBottom
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(48.dp)
        ) {
            // Promotional banner / placeholder
            PromotionalBanner()
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Welcome text
            Text(
                text = "Welcome to",
                style = MaterialTheme.typography.headlineMedium,
                color = GroPOSColors.White.copy(alpha = 0.9f),
                modifier = Modifier.alpha(breathingAlpha)
            )
            
            Text(
                text = "GroPOS",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = GroPOSColors.White,
                fontSize = 72.sp,
                modifier = Modifier.testTag(AdOverlayTestTags.TITLE)
            )
            
            Spacer(modifier = Modifier.height(64.dp))
            
            // Tap to start indicator with pulse animation
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .scale(pulseScale)
                    .testTag(AdOverlayTestTags.TAP_INDICATOR)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            color = GroPOSColors.White.copy(alpha = 0.2f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Tap to start",
                        tint = GroPOSColors.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Tap to Start",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    color = GroPOSColors.White,
                    modifier = Modifier.alpha(breathingAlpha)
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Footer tagline
            Text(
                text = "Fresh Groceries • Great Prices • Local Service",
                style = MaterialTheme.typography.bodyLarge,
                color = GroPOSColors.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Promotional banner placeholder.
 * 
 * In production, this would display rotating advertisements,
 * promotional images, or discount labels from a configured source.
 */
@Composable
private fun PromotionalBanner(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = GroPOSColors.White.copy(alpha = 0.15f),
                shape = MaterialTheme.shapes.large
            )
            .padding(horizontal = 48.dp, vertical = 24.dp)
            .testTag(AdOverlayTestTags.PROMO_BANNER),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🥬 Fresh Produce Special 🍎",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = GroPOSColors.White
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Local organic vegetables now in stock!",
                style = MaterialTheme.typography.bodyLarge,
                color = GroPOSColors.White.copy(alpha = 0.85f)
            )
        }
    }
}

/**
 * Color palette for the Ad Overlay.
 * 
 * Uses a professional green gradient consistent with GroPOS branding
 * while providing sufficient contrast for text readability.
 */
object AdOverlayColors {
    /** Top of gradient - darker forest green */
    val GradientTop = Color(0xFF0A3D1F)
    
    /** Bottom of gradient - slightly lighter emerald */
    val GradientBottom = Color(0xFF0D5B2A)
    
    /** Accent color for highlights */
    val Accent = Color(0xFF4CAF50)
}

/**
 * Test tags for UI automation.
 * 
 * Per testing-strategy.mdc: Use testTag for all critical interactive elements.
 */
object AdOverlayTestTags {
    const val OVERLAY = "ad_overlay"
    const val TITLE = "ad_overlay_title"
    const val TAP_INDICATOR = "ad_overlay_tap_indicator"
    const val PROMO_BANNER = "ad_overlay_promo_banner"
}

