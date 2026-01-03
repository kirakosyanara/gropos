package com.unisight.gropos.core.components

import android.Manifest
import android.content.pm.PackageManager
import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.unisight.gropos.core.hardware.scanner.CameraBarcodeScanner

/**
 * Android-specific Composable for camera barcode scanning.
 * 
 * **Per ANDROID_HARDWARE_GUIDE.md:**
 * - Uses CameraX PreviewView for camera viewfinder
 * - Integrates with CameraBarcodeScanner for MLKit analysis
 * 
 * **UI Components:**
 * - Camera preview with scanning overlay
 * - Permission required message when camera access not granted
 * - Close button for dismissing the scanner
 * 
 * **Note:** Permission handling should be done at the Activity level.
 * This composable assumes permission is already granted or shows a message.
 * 
 * **Usage:**
 * ```kotlin
 * CameraPreview(
 *     scanner = koinGet<ScannerRepository>() as CameraBarcodeScanner,
 *     onClose = { /* Hide camera */ },
 *     modifier = Modifier.fillMaxSize()
 * )
 * ```
 * 
 * Per testing-strategy.mdc: This is an Android-specific UI component.
 * Integration tests should use Robolectric or emulator.
 */
@Composable
fun CameraPreview(
    scanner: CameraBarcodeScanner,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onPermissionRequired: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val hasPermission = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("camera_preview_container")
    ) {
        if (hasPermission) {
            // Camera Preview
            CameraViewfinder(
                scanner = scanner,
                lifecycleOwner = lifecycleOwner,
                modifier = Modifier.fillMaxSize()
            )
            
            // Scanning Overlay
            ScanningOverlay(
                modifier = Modifier.fillMaxSize()
            )
            
            // Close Button
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .testTag("camera_close_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close scanner",
                    tint = Color.White
                )
            }
            
            // Instructions
            ScanningInstructions(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp)
            )
            
        } else {
            // Permission Required UI
            PermissionRequiredContent(
                onRequestPermission = onPermissionRequired,
                onClose = onClose,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
    
    // Cleanup when composable leaves composition
    DisposableEffect(Unit) {
        onDispose {
            scanner.unbindCamera()
        }
    }
}

/**
 * The actual camera viewfinder using CameraX PreviewView.
 */
@Composable
private fun CameraViewfinder(
    scanner: CameraBarcodeScanner,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    modifier: Modifier = Modifier
) {
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    
    // Bind camera when PreviewView is ready
    LaunchedEffect(previewView) {
        previewView?.let { view ->
            scanner.bindToLifecycle(
                lifecycleOwner = lifecycleOwner,
                previewView = view,
                onError = { e ->
                    println("[CAMERA_PREVIEW] Error: ${e.message}")
                }
            )
        }
    }
    
    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = PreviewView.ScaleType.FILL_CENTER
                previewView = this
            }
        },
        modifier = modifier.testTag("camera_viewfinder")
    )
}

/**
 * Scanning overlay with targeting box.
 */
@Composable
private fun ScanningOverlay(
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // Semi-transparent overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
        )
        
        // Targeting box in center
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(280.dp, 180.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Transparent)
                .border(
                    width = 3.dp,
                    color = Color.White.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(12.dp)
                )
                .testTag("scanning_target_box")
        )
    }
}

/**
 * Instructions displayed at bottom of camera preview.
 */
@Composable
private fun ScanningInstructions(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth(0.8f)
            .testTag("scanning_instructions"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.7f)
        )
    ) {
        Text(
            text = "Position barcode within the frame to scan",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        )
    }
}

/**
 * UI shown when camera permission is required.
 * 
 * Note: Actual permission request should be handled at the Activity level.
 */
@Composable
private fun PermissionRequiredContent(
    onRequestPermission: (() -> Unit)?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.Black
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Camera icon using text (avoids icon dependency issues)
            Text(
                text = "📷",
                fontSize = 48.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Camera Permission Required",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "To scan barcodes with the camera, please grant camera permission in Settings.",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            if (onRequestPermission != null) {
                androidx.compose.material3.Button(
                    onClick = onRequestPermission,
                    modifier = Modifier.testTag("request_permission_button")
                ) {
                    Text("Grant Permission")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            androidx.compose.material3.TextButton(
                onClick = onClose,
                modifier = Modifier.testTag("cancel_permission_button")
            ) {
                Text("Cancel", color = Color.White.copy(alpha = 0.7f))
            }
        }
    }
}
