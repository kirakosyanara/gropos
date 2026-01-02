import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.unisight.gropos.App
import com.unisight.gropos.core.database.seeder.DebugDataSeeder
import com.unisight.gropos.core.di.appModules
import com.unisight.gropos.core.di.databaseModule
import com.unisight.gropos.core.theme.GroPOSTheme
import com.unisight.gropos.core.util.CurrencyFormatter
import com.unisight.gropos.features.checkout.domain.repository.CartRepository
import com.unisight.gropos.features.customer.presentation.CustomerDisplayScreen
import com.unisight.gropos.features.customer.presentation.CustomerDisplayViewModel
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import java.awt.GraphicsEnvironment

/**
 * Desktop entry point for GroPOS.
 * 
 * Per ARCHITECTURE_BLUEPRINT.md (Section 6, Phase 3):
 * - Multi-window support: Primary window (Cashier) + Customer Display (Secondary monitor)
 * - Detects available monitors and positions windows accordingly
 * - Both windows share the same CartRepository (Singleton) via Koin DI
 * 
 * Window Configuration:
 * - Primary Window: 1280x800 on primary monitor
 * - Customer Display: Full screen on secondary monitor (if available)
 * 
 * State Synchronization:
 * - CartRepository is a SINGLETON in Koin
 * - CheckoutViewModel (Cashier) and CustomerDisplayViewModel (Customer) 
 *   both observe the same CartRepository
 * - Any changes from Cashier are instantly reflected on Customer Display
 * 
 * IMPORTANT: CustomerDisplayViewModel is used OUTSIDE of Voyager's Navigator,
 * so we must inject a CoroutineScope manually (screenModelScope won't work).
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
    // 
    // State Synchronization:
    // - Gets CartRepository from Koin (SINGLETON)
    // - Creates CustomerDisplayViewModel with injected CoroutineScope
    // - Same CartRepository is used by CheckoutViewModel in Cashier window
    // - Changes in Cashier window are instantly visible here
    // 
    // IMPORTANT: We must inject a CoroutineScope because this ViewModel
    // is used outside of Voyager's Navigator, so screenModelScope won't work.
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
                // Get a CoroutineScope that's tied to this Window's lifecycle
                // This is CRITICAL: CustomerDisplayViewModel extends ScreenModel,
                // but it's NOT managed by Voyager's Navigator here.
                // Without an injected scope, the screenModelScope is inactive
                // and observeCartChanges() silently fails.
                val coroutineScope = rememberCoroutineScope()
                
                // Get dependencies from Koin (CartRepository is SINGLETON)
                val cartRepository: CartRepository = remember { GlobalContext.get().get() }
                val currencyFormatter: CurrencyFormatter = remember { GlobalContext.get().get() }
                
                // Create ViewModel with injected scope
                // This ensures the cart observation coroutine actually runs
                val viewModel = remember(coroutineScope) {
                    CustomerDisplayViewModel(
                        cartRepository = cartRepository,
                        currencyFormatter = currencyFormatter,
                        scope = coroutineScope  // CRITICAL: Inject the scope!
                    )
                }
                
                CustomerDisplayScreen(
                    viewModel = viewModel,
                    storeName = "GroPOS Store"
                )
            }
        }
    }
}

// ============================================================================
// Koin Initialization
// ============================================================================

/**
 * Initializes Koin dependency injection with database support.
 * 
 * Per DATABASE_SCHEMA.md: CouchbaseLite replaces in-memory FakeProductRepository.
 * 
 * Initialization Order:
 * 1. Start Koin with databaseModule (provides DatabaseProvider, CouchbaseProductRepository)
 * 2. Start Koin with appModules (provides all other dependencies)
 * 3. Run DebugDataSeeder.seedIfEmpty() to populate database on first launch
 * 
 * Key DI Singletons for Multi-Window:
 * - DatabaseProvider: CouchbaseLite database instance
 * - CartRepository: Single source of truth for cart state
 * - ProductRepository: CouchbaseProductRepository (replaces Fake)
 * - ScannerRepository: Single source for scanner events
 * - CurrencyFormatter: Shared formatter
 */
private fun initKoin() {
    try {
        startKoin {
            // Database module FIRST (provides ProductRepository)
            modules(databaseModule)
            // Then app modules (consume ProductRepository)
            modules(appModules())
        }
        
        // Seed database with initial products if empty
        // Per ARCHITECTURE_BLUEPRINT.md: Offline-first requires local data immediately
        try {
            val seeder: DebugDataSeeder = GlobalContext.get().get()
            seeder.seedIfEmpty()
        } catch (e: Exception) {
            println("Warning: Could not seed database - ${e.message}")
        }
        
    } catch (e: IllegalStateException) {
        // Koin already started (happens during hot reload in development)
        // This is expected behavior, ignore
    }
}
