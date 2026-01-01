import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.unisight.gropos.App
import com.unisight.gropos.core.di.appModules
import com.unisight.gropos.core.theme.GroPOSTheme
import com.unisight.gropos.features.checkout.presentation.CheckoutUiState
import com.unisight.gropos.features.customer.presentation.CustomerDisplayContent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.context.startKoin
import java.awt.GraphicsEnvironment

/**
 * Desktop entry point for GroPOS.
 * 
 * Per ARCHITECTURE_BLUEPRINT.md (Section 6, Phase 3):
 * - Multi-window support: Primary window (Cashier) + Customer Display (Secondary monitor)
 * - Detects available monitors and positions windows accordingly
 * 
 * Window Configuration:
 * - Primary Window: 1280x800 on primary monitor
 * - Customer Display: Full screen on secondary monitor (if available)
 * 
 * Dev Mode:
 * - Set DEV_FORCE_CUSTOMER_DISPLAY=true to force customer display window
 *   even on single-monitor setups (for testing)
 */

// ============================================================================
// Configuration
// ============================================================================

/**
 * Development flag to force Customer Display window even on single monitor.
 * Set to true for testing dual-window logic on a single monitor.
 */
private const val DEV_FORCE_CUSTOMER_DISPLAY = true

// ============================================================================
// Shared State for Cross-Window Communication
// ============================================================================

/**
 * Shared checkout state between Cashier and Customer Display windows.
 * 
 * Per ARCHITECTURE_BLUEPRINT.md: Both windows observe the same transaction state.
 * In production, this would be provided by Koin and shared via DI.
 * 
 * TODO: Move to DI module and inject into CheckoutViewModel
 */
object SharedAppState {
    private val _checkoutState = MutableStateFlow(CheckoutUiState())
    val checkoutState: StateFlow<CheckoutUiState> = _checkoutState
    
    fun updateCheckoutState(state: CheckoutUiState) {
        _checkoutState.value = state
    }
}

// ============================================================================
// Main Entry Point
// ============================================================================

fun main() = application {
    // Initialize Koin DI before any Compose code
    initKoin()
    
    // Detect available monitors
    val graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment()
    val screenDevices = graphicsEnvironment.screenDevices
    val hasMultipleMonitors = screenDevices.size > 1
    
    // Determine if we should show customer display
    val showCustomerDisplay = hasMultipleMonitors || DEV_FORCE_CUSTOMER_DISPLAY
    
    // Track window visibility state
    var isCustomerDisplayVisible by remember { mutableStateOf(showCustomerDisplay) }
    
    // ========================================================================
    // Primary Window (Cashier Screen)
    // ========================================================================
    val primaryWindowState = rememberWindowState(
        size = DpSize(1280.dp, 800.dp),
        position = if (hasMultipleMonitors) {
            // Position on primary monitor
            val primaryBounds = screenDevices[0].defaultConfiguration.bounds
            WindowPosition(
                x = primaryBounds.x.dp + 50.dp,
                y = primaryBounds.y.dp + 50.dp
            )
        } else {
            WindowPosition(Alignment.Center)
        }
    )
    
    Window(
        onCloseRequest = ::exitApplication,
        state = primaryWindowState,
        title = "GroPOS - Cashier",
        resizable = true
    ) {
        App()
    }
    
    // ========================================================================
    // Customer Display Window (Secondary Monitor)
    // Per SCREEN_LAYOUTS.md: Customer Screen for secondary display
    // ========================================================================
    if (isCustomerDisplayVisible) {
        val customerWindowState = rememberWindowState(
            size = if (hasMultipleMonitors) {
                // Full screen on secondary monitor
                val secondaryBounds = screenDevices[1].defaultConfiguration.bounds
                DpSize(secondaryBounds.width.dp, secondaryBounds.height.dp)
            } else {
                // Dev mode: Smaller window on same monitor
                DpSize(800.dp, 600.dp)
            },
            position = if (hasMultipleMonitors) {
                // Position on secondary monitor
                val secondaryBounds = screenDevices[1].defaultConfiguration.bounds
                WindowPosition(
                    x = secondaryBounds.x.dp,
                    y = secondaryBounds.y.dp
                )
            } else {
                // Dev mode: Offset from primary window
                WindowPosition(
                    x = 1300.dp,
                    y = 100.dp
                )
            }
        )
        
        Window(
            onCloseRequest = { isCustomerDisplayVisible = false },
            state = customerWindowState,
            title = "GroPOS - Customer Display",
            resizable = true,
            alwaysOnTop = false // Can be true in production
        ) {
            GroPOSTheme {
                CustomerDisplayContent(
                    items = SharedAppState.checkoutState.value.items,
                    totals = SharedAppState.checkoutState.value.totals,
                    storeName = "GroPOS Store",
                    isEmpty = SharedAppState.checkoutState.value.isEmpty
                )
            }
        }
    }
}

// ============================================================================
// Koin Initialization
// ============================================================================

/**
 * Initializes Koin dependency injection.
 * Checks if Koin is already started to prevent crashes during development hot reload.
 */
private fun initKoin() {
    try {
        startKoin {
            modules(appModules())
        }
    } catch (e: IllegalStateException) {
        // Koin already started (happens during hot reload in development)
        // This is expected behavior, ignore
    }
}
