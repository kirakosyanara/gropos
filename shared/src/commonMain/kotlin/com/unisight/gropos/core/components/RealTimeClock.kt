package com.unisight.gropos.core.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.unisight.gropos.core.theme.GroPOSColors
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Real-time clock display component.
 * 
 * Per SCREEN_LAYOUTS.md (Status Bar / Top Bar):
 * - Shows current time (hh:mm a) in the top right or center
 * - Auto-updates every second
 * 
 * Uses LaunchedEffect with a while(true) loop to update time.
 * Compose's lifecycle automatically cleans up the coroutine when the
 * composable leaves composition.
 * 
 * @param modifier Modifier to apply to the text
 * @param color Text color (defaults to theme-appropriate color)
 */
@Composable
fun RealTimeClock(
    modifier: Modifier = Modifier,
    color: Color = GroPOSColors.TextPrimary
) {
    var currentTime by remember { mutableStateOf(formatCurrentTime()) }
    
    // Update every second
    // Per Governance (Performance): LaunchedEffect cleans up when composable leaves screen
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = formatCurrentTime()
            delay(1000L)
        }
    }
    
    Text(
        text = currentTime,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = modifier.testTag("realtime_clock")
    )
}

/**
 * Formats the current time as "h:mm a" (e.g., "2:30 PM").
 * 
 * Why this format: Per SCREEN_LAYOUTS.md documentation, the time should be
 * displayed in 12-hour format with AM/PM indicator.
 */
private fun formatCurrentTime(): String {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val hour = when {
        now.hour == 0 -> 12
        now.hour > 12 -> now.hour - 12
        else -> now.hour
    }
    val minute = now.minute.toString().padStart(2, '0')
    val amPm = if (now.hour >= 12) "PM" else "AM"
    return "$hour:$minute $amPm"
}

