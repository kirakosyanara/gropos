package com.unisight.gropos.core.hardware.scanner

import android.content.Context
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.unisight.gropos.features.checkout.domain.repository.ScannerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Android implementation of ScannerRepository using CameraX + MLKit.
 * 
 * **Per ANDROID_HARDWARE_GUIDE.md:**
 * - Uses CameraX for camera access (lifecycle-aware, handles permissions)
 * - Uses MLKit for on-device barcode recognition (no network needed)
 * - Supports UPC-A, UPC-E, EAN-13, EAN-8, Code 128, Code 39
 * 
 * **Usage Pattern:**
 * 1. Call `bindToLifecycle()` with a LifecycleOwner and PreviewView
 * 2. Collect `scannedCodes` Flow to receive barcode data
 * 3. Camera unbinds automatically when lifecycle is destroyed
 * 
 * **Performance:**
 * - Image analysis runs on background thread (Executors pool)
 * - Rate limiting prevents duplicate scans (500ms cooldown)
 * - Memory efficient: reuses analyzer instance
 * 
 * Per testing-strategy.mdc: For testing, use FakeScannerRepository.
 */
class CameraBarcodeScanner(
    private val context: Context
) : ScannerRepository {
    
    private var _isActive = false
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraExecutor: ExecutorService? = null
    
    /**
     * SharedFlow for emitting scanned barcodes.
     */
    private val _scannedCodes = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 16
    )
    override val scannedCodes: Flow<String> = _scannedCodes.asSharedFlow()
    
    override val isActive: Boolean
        get() = _isActive
    
    // Debounce duplicate scans
    private var lastScannedCode: String? = null
    private var lastScanTime: Long = 0
    private val scanCooldownMs = 500L
    
    // MLKit barcode scanner
    private val barcodeScanner: BarcodeScanner by lazy {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_QR_CODE
            )
            .build()
        
        BarcodeScanning.getClient(options)
    }
    
    // ========================================================================
    // ScannerRepository Implementation
    // ========================================================================
    
    override suspend fun startScanning() {
        // Note: For camera, actual binding requires LifecycleOwner
        // See bindToLifecycle() for full initialization
        _isActive = true
        println("[CAMERA_SCANNER] Scanner activated (call bindToLifecycle to start camera)")
    }
    
    override suspend fun stopScanning() {
        unbindCamera()
        _isActive = false
        lastScannedCode = null
        println("[CAMERA_SCANNER] Scanner deactivated")
    }
    
    // ========================================================================
    // Camera Lifecycle Management
    // ========================================================================
    
    /**
     * Binds camera to lifecycle and starts barcode analysis.
     * 
     * **Must be called from Main thread.**
     * 
     * @param lifecycleOwner The activity/fragment lifecycle
     * @param previewView Optional PreviewView to show camera feed
     * @param onError Callback for camera errors
     */
    fun bindToLifecycle(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView? = null,
        onError: ((Exception) -> Unit)? = null
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                
                // Create camera executor
                cameraExecutor = Executors.newSingleThreadExecutor()
                
                // Build use cases
                val preview = previewView?.let { view ->
                    Preview.Builder()
                        .build()
                        .also { previewInstance ->
                            previewInstance.setSurfaceProvider(view.surfaceProvider)
                        }
                }
                
                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(1280, 720))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(cameraExecutor!!, BarcodeAnalyzer())
                    }
                
                // Select back camera
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                
                // Unbind existing use cases before rebinding
                cameraProvider?.unbindAll()
                
                // Bind use cases to camera
                if (preview != null) {
                    cameraProvider?.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } else {
                    cameraProvider?.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        imageAnalysis
                    )
                }
                
                _isActive = true
                println("[CAMERA_SCANNER] Camera bound to lifecycle")
                
            } catch (e: Exception) {
                println("[CAMERA_SCANNER] Failed to bind camera: ${e.message}")
                onError?.invoke(e)
            }
        }, context.mainExecutor)
    }
    
    /**
     * Unbinds camera from lifecycle.
     */
    fun unbindCamera() {
        cameraProvider?.unbindAll()
        cameraExecutor?.shutdown()
        cameraExecutor = null
        _isActive = false
        println("[CAMERA_SCANNER] Camera unbound")
    }
    
    // ========================================================================
    // Image Analysis
    // ========================================================================
    
    /**
     * MLKit barcode analyzer for CameraX.
     */
    private inner class BarcodeAnalyzer : ImageAnalysis.Analyzer {
        
        @androidx.camera.core.ExperimentalGetImage
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            
            if (mediaImage == null) {
                imageProxy.close()
                return
            }
            
            val image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )
            
            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    processBarcodes(barcodes)
                }
                .addOnFailureListener { e ->
                    println("[CAMERA_SCANNER] Scan error: ${e.message}")
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }
    
    /**
     * Processes detected barcodes with deduplication.
     */
    private fun processBarcodes(barcodes: List<Barcode>) {
        for (barcode in barcodes) {
            val rawValue = barcode.rawValue ?: continue
            
            // Skip if same barcode scanned too recently
            val now = System.currentTimeMillis()
            if (rawValue == lastScannedCode && (now - lastScanTime) < scanCooldownMs) {
                continue
            }
            
            // Validate barcode
            if (rawValue.length < 3 || rawValue.length > 128) {
                println("[CAMERA_SCANNER] Invalid barcode length: ${rawValue.length}")
                continue
            }
            
            // Emit barcode
            lastScannedCode = rawValue
            lastScanTime = now
            
            val emitted = _scannedCodes.tryEmit(rawValue)
            
            if (emitted) {
                println("[CAMERA_SCANNER] Barcode: $rawValue (${barcode.format.formatName()})")
            } else {
                println("[CAMERA_SCANNER] Buffer full, barcode dropped")
            }
            
            // Only process first valid barcode per frame
            break
        }
    }
    
    /**
     * Extension to get human-readable format name.
     */
    private fun Int.formatName(): String {
        return when (this) {
            Barcode.FORMAT_UPC_A -> "UPC-A"
            Barcode.FORMAT_UPC_E -> "UPC-E"
            Barcode.FORMAT_EAN_13 -> "EAN-13"
            Barcode.FORMAT_EAN_8 -> "EAN-8"
            Barcode.FORMAT_CODE_128 -> "CODE-128"
            Barcode.FORMAT_CODE_39 -> "CODE-39"
            Barcode.FORMAT_QR_CODE -> "QR"
            else -> "UNKNOWN"
        }
    }
    
    /**
     * Releases resources.
     */
    fun release() {
        unbindCamera()
        barcodeScanner.close()
        println("[CAMERA_SCANNER] Resources released")
    }
}

