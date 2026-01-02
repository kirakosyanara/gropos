package com.unisight.gropos.core.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.unisight.gropos.core.AppConstants
import com.unisight.gropos.core.theme.GroPOSColors
import com.unisight.gropos.core.theme.GroPOSSpacing

/**
 * Reusable footer with hidden admin settings trigger.
 * 
 * Per SCREEN_LAYOUTS.md - Hidden Administration Menu Trigger:
 * - Location: Footer copyright text "©Unisight BIT, 2024"
 * - Trigger: Click 5 times rapidly (within 2 seconds)
 * - Opens: Administration Settings Dialog
 * 
 * Used by:
 * - LoginScreen: Access hidden settings before login
 * - RegistrationScreen: Access hidden settings to set environment before registering
 */
@Composable
fun SecretTriggerFooter(
    currentTime: String,
    version: String,
    onAdminSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Track click count and timing for secret trigger
    var clickCount by remember { mutableIntStateOf(0) }
    var lastClickTime by remember { mutableLongStateOf(0L) }
    
    val requiredClicks = 5
    val timeWindowMs = 2000L // 2 seconds
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = currentTime.ifEmpty { "12:00 PM" },
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = GroPOSColors.White
        )
        
        Spacer(modifier = Modifier.height(GroPOSSpacing.M))
        
        // Copyright text with secret trigger
        Text(
            text = "$version • ${AppConstants.COPYRIGHT_NOTICE}",
            style = MaterialTheme.typography.bodySmall,
            color = GroPOSColors.White.copy(alpha = 0.7f),
            modifier = Modifier
                .clickable {
                    val currentTimeMs = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                    
                    // Reset counter if too much time passed
                    if (currentTimeMs - lastClickTime > timeWindowMs) {
                        clickCount = 0
                    }
                    
                    clickCount++
                    lastClickTime = currentTimeMs
                    
                    // Trigger admin settings on 5 rapid clicks
                    if (clickCount >= requiredClicks) {
                        clickCount = 0
                        onAdminSettingsClick()
                    }
                }
                .testTag("secret_admin_trigger")
        )
    }
}

