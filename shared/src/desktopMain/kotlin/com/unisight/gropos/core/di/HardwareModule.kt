package com.unisight.gropos.core.di

import com.unisight.gropos.core.hardware.printer.DesktopEscPosPrinter
import com.unisight.gropos.core.hardware.printer.PrinterConfig
import com.unisight.gropos.core.hardware.printer.PrinterService
import com.unisight.gropos.core.hardware.printer.SimulatedPrinterService
import com.unisight.gropos.core.hardware.scale.DesktopCasScale
import com.unisight.gropos.core.hardware.scale.ScaleConfig
import com.unisight.gropos.core.hardware.scale.ScaleService
import com.unisight.gropos.core.hardware.scale.SimulatedScaleService
import com.unisight.gropos.core.hardware.scanner.DesktopSerialScanner
import com.unisight.gropos.core.hardware.scanner.ScannerConfig
import com.unisight.gropos.features.checkout.data.FakeScannerRepository
import com.unisight.gropos.features.checkout.data.SafeScannerRepository
import com.unisight.gropos.features.checkout.domain.repository.ScannerRepository
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Desktop-specific hardware module.
 * 
 * **Per DESKTOP_HARDWARE.md:**
 * Provides hardware implementations for:
 * - PrinterService: ESC/POS thermal receipt printer
 * - ScannerRepository: Serial barcode scanner
 * 
 * **Build Variants:**
 * - Production (USE_REAL_HARDWARE = true): Real hardware drivers
 * - Development (USE_REAL_HARDWARE = false): Simulated hardware for testing
 * 
 * **Configuration:**
 * Hardware ports are loaded from:
 * - Environment variables (SCANNER_PORT, PRINTER_PORT)
 * - User preferences (stored in local config)
 * 
 * Per project-structure.mdc: Desktop-specific implementations in desktopMain.
 * Per testing-strategy.mdc: Use simulated hardware in development/CI.
 */
val hardwareModule: Module = module {
    
    // ========================================================================
    // Build Configuration
    // ========================================================================
    
    /**
     * Flag to switch between real and simulated hardware.
     * 
     * Set via system property or environment variable:
     * - System property: -DUSE_REAL_HARDWARE=true
     * - Environment: USE_REAL_HARDWARE=true
     * 
     * Default: false (use simulated for safety in development)
     */
    val useRealHardware = System.getProperty("USE_REAL_HARDWARE")?.toBoolean()
        ?: System.getenv("USE_REAL_HARDWARE")?.toBoolean()
        ?: false
    
    // ========================================================================
    // Scanner Configuration
    // ========================================================================
    
    /**
     * Scanner configuration loaded from environment or preferences.
     */
    single(named("scannerConfig")) {
        ScannerConfig(
            portName = System.getenv("SCANNER_PORT") ?: "",
            baudRate = System.getenv("SCANNER_BAUD")?.toIntOrNull() ?: 9600
        )
    }
    
    /**
     * Printer configuration loaded from environment or preferences.
     */
    single(named("printerConfig")) {
        PrinterConfig(
            portName = System.getenv("PRINTER_PORT") ?: "",
            baudRate = System.getenv("PRINTER_BAUD")?.toIntOrNull() ?: 9600,
            lineWidth = System.getenv("PRINTER_LINE_WIDTH")?.toIntOrNull() ?: 42
        )
    }
    
    /**
     * Scale configuration loaded from environment or preferences.
     * 
     * Per DESKTOP_HARDWARE.md: CAS PD-II scales use 9600 baud.
     */
    single(named("scaleConfig")) {
        ScaleConfig(
            portName = System.getenv("SCALE_PORT") ?: "",
            baudRate = System.getenv("SCALE_BAUD")?.toIntOrNull() ?: 9600
        )
    }
    
    // ========================================================================
    // Scanner Implementations
    // ========================================================================
    
    if (useRealHardware) {
        /**
         * PRODUCTION: Real serial scanner.
         * 
         * Uses jSerialComm for USB/Serial barcode scanner communication.
         */
        single<ScannerRepository> {
            val rawScanner = DesktopSerialScanner(get(named("scannerConfig")))
            
            // Wrap with SafeScannerRepository for input validation
            SafeScannerRepository(rawScanner)
        }
        
        println("[HARDWARE] Using REAL scanner")
        
    } else {
        /**
         * DEVELOPMENT: Simulated scanner.
         * 
         * For testing without physical hardware.
         * Inject barcodes programmatically via FakeScannerRepository.
         */
        single { FakeScannerRepository() }
        
        single<ScannerRepository> {
            val fakeScanner = get<FakeScannerRepository>()
            
            // Still wrap with SafeScannerRepository for consistent behavior
            SafeScannerRepository(fakeScanner)
        }
        
        println("[HARDWARE] Using SIMULATED scanner")
    }
    
    // ========================================================================
    // Printer Implementations
    // ========================================================================
    
    if (useRealHardware) {
        /**
         * PRODUCTION: Real ESC/POS printer.
         * 
         * Uses jSerialComm for USB/Serial thermal printer communication.
         */
        single<PrinterService> {
            DesktopEscPosPrinter(get(named("printerConfig")))
        }
        
        println("[HARDWARE] Using REAL printer")
        
    } else {
        /**
         * DEVELOPMENT: Simulated printer.
         * 
         * Logs print operations to console.
         * Useful for testing receipt formatting without hardware.
         */
        single<PrinterService> {
            SimulatedPrinterService()
        }
        
        println("[HARDWARE] Using SIMULATED printer")
    }
    
    // ========================================================================
    // Scale Implementations
    // ========================================================================
    
    if (useRealHardware) {
        /**
         * PRODUCTION: Real CAS PD-II scale.
         * 
         * Uses jSerialComm for USB/Serial scale communication.
         */
        single<ScaleService> {
            DesktopCasScale(get(named("scaleConfig")))
        }
        
        println("[HARDWARE] Using REAL scale")
        
    } else {
        /**
         * DEVELOPMENT: Simulated scale.
         * 
         * For testing weight-based workflows without hardware.
         * Inject weights programmatically via SimulatedScaleService.
         */
        single { SimulatedScaleService() }
        
        single<ScaleService> {
            get<SimulatedScaleService>()
        }
        
        println("[HARDWARE] Using SIMULATED scale")
    }
    
    // ========================================================================
    // Raw Hardware Access (for settings screens)
    // ========================================================================
    
    /**
     * Raw scanner for port enumeration (not wrapped).
     * 
     * Use for hardware settings screen where user selects port.
     */
    single(named("rawScanner")) {
        DesktopSerialScanner(ScannerConfig())
    }
    
    /**
     * Raw printer for port enumeration (not wrapped).
     * 
     * Use for hardware settings screen where user selects port.
     */
    single(named("rawPrinter")) {
        DesktopEscPosPrinter(PrinterConfig())
    }
    
    /**
     * Raw scale for port enumeration (not wrapped).
     * 
     * Use for hardware settings screen where user selects port.
     */
    single(named("rawScale")) {
        DesktopCasScale(ScaleConfig())
    }
}

/**
 * Helper to get available scanner ports for UI.
 */
fun getAvailableScannerPorts(): List<String> {
    val scanner = DesktopSerialScanner()
    return scanner.getAvailablePorts().map { "${it.name} - ${it.description}" }
}

/**
 * Helper to get available printer ports for UI.
 */
fun getAvailablePrinterPorts(): List<String> {
    val printer = DesktopEscPosPrinter()
    return printer.getAvailablePorts().map { "${it.name} - ${it.description}" }
}

/**
 * Helper to get available scale ports for UI.
 */
fun getAvailableScalePorts(): List<String> {
    val scale = DesktopCasScale()
    return scale.getAvailablePorts().map { "${it.name} - ${it.description}" }
}

