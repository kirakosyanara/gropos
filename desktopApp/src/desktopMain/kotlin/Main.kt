import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.unisight.gropos.App
import com.unisight.gropos.core.di.appModules
import org.koin.core.context.startKoin

/**
 * Desktop entry point for GroPOS.
 * 
 * Initializes:
 * - Koin dependency injection
 * - Compose Multiplatform window
 * - GroPOS application
 * 
 * Window Configuration:
 * - Size: 1280x800 (standard POS resolution)
 * - Title: "GroPOS"
 * - Centered on screen
 */
fun main() = application {
    // Initialize Koin DI before any Compose code
    // Only initialize once to prevent crashes on hot reload
    initKoin()
    
    // Window state with POS-appropriate resolution
    val windowState = rememberWindowState(
        size = DpSize(1280.dp, 800.dp),
        position = WindowPosition(Alignment.Center)
    )
    
    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "GroPOS",
        resizable = true
    ) {
        App()
    }
}

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

