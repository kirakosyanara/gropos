package com.unisight.gropos.core.di

import android.content.Context
import android.os.Build
import com.unisight.gropos.core.hardware.printer.PrinterService
import com.unisight.gropos.core.hardware.printer.SimulatedPrinterService
import com.unisight.gropos.core.hardware.printer.SunmiPrinterService
import com.unisight.gropos.core.hardware.scanner.CameraBarcodeScanner
import com.unisight.gropos.core.hardware.scanner.SunmiHardwareScanner
import com.unisight.gropos.features.checkout.data.FakeScannerRepository
import com.unisight.gropos.features.checkout.data.SafeScannerRepository
import com.unisight.gropos.features.checkout.domain.repository.ScannerRepository
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Android-specific hardware module.
 * 
 * **Per ANDROID_HARDWARE_GUIDE.md:**
 * Provides hardware implementations for:
 * - PrinterService: Sunmi built-in thermal printer
 * - ScannerRepository: Sunmi hardware scanner or Camera+MLKit
 * 
 * **Device Detection:**
 * Automatically detects device type:
 * - Sunmi devices: Uses native Sunmi SDKs
 * - PAX devices: TODO (requires AAR files)
 * - Generic Android: Uses Camera+MLKit for scanning
 * 
 * **Build Variants:**
 * - Release (!BuildConfig.DEBUG): Real hardware drivers
 * - Debug (BuildConfig.DEBUG): Simulated hardware for development
 * 
 * Per project-structure.mdc: Android-specific implementations in androidMain.
 * Per testing-strategy.mdc: Use simulated hardware in development/CI.
 */
val hardwareModule: Module = module {
    
    // ========================================================================
    // Device Detection
    // ========================================================================
    
    /**
     * Detected Android device type.
     */
    single(named("deviceType")) {
        detectDeviceType()
    }
    
    // ========================================================================
    // Build Configuration
    // ========================================================================
    
    /**
     * Flag to switch between real and simulated hardware.
     * 
     * In production builds, this should be true.
     * For development and testing, use simulated hardware.
     */
    val useRealHardware = !isDebugBuild()
    
    // ========================================================================
    // Scanner Implementations
    // ========================================================================
    
    if (useRealHardware) {
        /**
         * PRODUCTION: Real scanner based on device type.
         */
        single<ScannerRepository> {
            val context: Context = get()
            val deviceType: AndroidDeviceType = get(named("deviceType"))
            
            val rawScanner: ScannerRepository = when (deviceType) {
                AndroidDeviceType.SUNMI -> {
                    println("[HARDWARE] Using Sunmi hardware scanner")
                    SunmiHardwareScanner(context)
                }
                AndroidDeviceType.PAX -> {
                    // TODO: Implement PAX scanner when AAR is available
                    println("[HARDWARE] PAX scanner not implemented, falling back to camera")
                    CameraBarcodeScanner(context)
                }
                AndroidDeviceType.GENERIC -> {
                    println("[HARDWARE] Using camera barcode scanner")
                    CameraBarcodeScanner(context)
                }
            }
            
            // Wrap with SafeScannerRepository for input validation
            SafeScannerRepository(rawScanner)
        }
        
        // Also provide raw CameraBarcodeScanner for UI binding
        single(named("cameraScanner")) {
            CameraBarcodeScanner(get())
        }
        
        println("[HARDWARE] Using REAL scanner")
        
    } else {
        /**
         * DEVELOPMENT: Simulated scanner.
         */
        single { FakeScannerRepository() }
        
        single<ScannerRepository> {
            SafeScannerRepository(get<FakeScannerRepository>())
        }
        
        println("[HARDWARE] Using SIMULATED scanner")
    }
    
    // ========================================================================
    // Printer Implementations
    // ========================================================================
    
    if (useRealHardware) {
        /**
         * PRODUCTION: Real printer based on device type.
         */
        single<PrinterService> {
            val context: Context = get()
            val deviceType: AndroidDeviceType = get(named("deviceType"))
            
            when (deviceType) {
                AndroidDeviceType.SUNMI -> {
                    println("[HARDWARE] Using Sunmi printer")
                    SunmiPrinterService(context)
                }
                AndroidDeviceType.PAX -> {
                    // TODO: Implement PAX printer when AAR is available
                    println("[HARDWARE] PAX printer not implemented, using simulated")
                    SimulatedPrinterService()
                }
                AndroidDeviceType.GENERIC -> {
                    // Generic devices need external Bluetooth printer
                    // For now, use simulated
                    println("[HARDWARE] No built-in printer, using simulated")
                    SimulatedPrinterService()
                }
            }
        }
        
        println("[HARDWARE] Using REAL printer")
        
    } else {
        /**
         * DEVELOPMENT: Simulated printer.
         */
        single<PrinterService> {
            SimulatedPrinterService()
        }
        
        println("[HARDWARE] Using SIMULATED printer")
    }
}

// ============================================================================
// Device Detection
// ============================================================================

/**
 * Supported Android device types.
 */
enum class AndroidDeviceType {
    /** Sunmi POS devices (V2 Pro, T2, etc.) */
    SUNMI,
    
    /** PAX Android terminals (A920, A930, etc.) */
    PAX,
    
    /** Generic Android tablet/phone */
    GENERIC
}

/**
 * Detects the Android device type based on manufacturer and model.
 */
private fun detectDeviceType(): AndroidDeviceType {
    val manufacturer = Build.MANUFACTURER.lowercase()
    val model = Build.MODEL.lowercase()
    val brand = Build.BRAND.lowercase()
    
    println("[HARDWARE] Device: $manufacturer / $model / $brand")
    
    return when {
        // Sunmi detection
        manufacturer.contains("sunmi") ||
        brand.contains("sunmi") -> {
            AndroidDeviceType.SUNMI
        }
        
        // PAX detection
        manufacturer.contains("pax") ||
        model.contains("a920") ||
        model.contains("a930") ||
        model.contains("a35") ||
        model.contains("a77") -> {
            AndroidDeviceType.PAX
        }
        
        // Default to generic
        else -> AndroidDeviceType.GENERIC
    }
}

/**
 * Checks if this is a debug build.
 * 
 * Note: In a real app, this would check BuildConfig.DEBUG.
 * Since we're in a library module, we use a simple heuristic.
 */
private fun isDebugBuild(): Boolean {
    // Check for common debug indicators
    return try {
        // If we can find this class, we're likely in debug
        Class.forName("dalvik.system.VMDebug")
            .getMethod("isDebuggingEnabled")
            .invoke(null) as? Boolean ?: false
    } catch (e: Exception) {
        // Fallback: check if debuggable flag is set
        // This is a simple heuristic; in production, use BuildConfig.DEBUG
        android.os.Debug.isDebuggerConnected()
    }
}

