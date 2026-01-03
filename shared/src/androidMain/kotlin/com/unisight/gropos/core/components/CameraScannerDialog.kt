package com.unisight.gropos.core.components

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.unisight.gropos.core.hardware.scanner.CameraBarcodeScanner
import kotlinx.coroutines.flow.collectLatest

/**
 * Full-screen dialog for camera barcode scanning.
 * 
 * **Per ANDROID_HARDWARE_GUIDE.md:**
 * - Shows camera preview in a modal dialog
 * - Automatically collects scanned barcodes and passes to callback
 * - Closes after successful scan (single-scan mode)
 * 
 * **Usage in CheckoutScreen:**
 * ```kotlin
 * var showCameraScanner by remember { mutableStateOf(false) }
 * 
 * if (showCameraScanner) {
 *     CameraScannerDialog(
 *         scanner = cameraBarcodeScanner,
 *         onBarcodeScanned = { barcode ->
 *             viewModel.onManualBarcodeEnter(barcode)
 *             showCameraScanner = false
 *         },
 *         onDismiss = { showCameraScanner = false }
 *     )
 * }
 * ```
 */
@Composable
fun CameraScannerDialog(
    scanner: CameraBarcodeScanner,
    onBarcodeScanned: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    continuousScanning: Boolean = false
) {
    // Collect scanned codes
    LaunchedEffect(scanner) {
        scanner.startScanning()
        
        scanner.scannedCodes.collectLatest { barcode ->
            onBarcodeScanned(barcode)
            
            // In single-scan mode, dismiss after first scan
            if (!continuousScanning) {
                onDismiss()
            }
        }
    }
    
    // Stop scanning when dialog closes
    DisposableEffect(Unit) {
        onDispose {
            // Unbind is handled by CameraPreview's DisposableEffect
        }
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.7f)
                .testTag("camera_scanner_dialog"),
            shape = RoundedCornerShape(16.dp)
        ) {
            CameraPreview(
                scanner = scanner,
                onClose = onDismiss
            )
        }
    }
}

/**
 * Inline camera preview (not a dialog) for embedding directly in a screen.
 * 
 * Useful for screens where the camera should be always visible rather than
 * shown as a modal overlay.
 * 
 * **Usage:**
 * ```kotlin
 * InlineCameraScanner(
 *     scanner = cameraBarcodeScanner,
 *     onBarcodeScanned = { barcode -> /* handle */ },
 *     modifier = Modifier
 *         .fillMaxWidth()
 *         .height(300.dp)
 * )
 * ```
 */
@Composable
fun InlineCameraScanner(
    scanner: CameraBarcodeScanner,
    onBarcodeScanned: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Collect scanned codes
    LaunchedEffect(scanner) {
        scanner.startScanning()
        
        scanner.scannedCodes.collectLatest { barcode ->
            onBarcodeScanned(barcode)
        }
    }
    
    Surface(
        modifier = modifier
            .testTag("inline_camera_scanner"),
        shape = RoundedCornerShape(12.dp)
    ) {
        CameraPreview(
            scanner = scanner,
            onClose = { /* Inline version doesn't close */ }
        )
    }
}

