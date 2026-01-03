package com.unisight.gropos.core.hardware.printer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import java.util.UUID

/**
 * Android implementation of PrinterService for Sunmi POS devices.
 * 
 * **Per ANDROID_HARDWARE_GUIDE.md:**
 * - Binds to Sunmi's AIDL service (`IWoyouService`)
 * - Supports V2 Pro, V2s, T2, T2 Mini, D3 devices
 * - Built-in thermal printer (no external hardware needed)
 * 
 * **AIDL Integration:**
 * Sunmi printers expose `IWoyouService` AIDL interface.
 * We bind to this service and call print methods through the proxy.
 * 
 * **Note on Implementation:**
 * The Sunmi SDK AIDL stubs are generated at compile time from the AAR.
 * If the exact method signatures differ, this class provides fallback behavior.
 * 
 * Per reliability-stability.mdc: All operations return PrintResult, never throw.
 */
class SunmiPrinterService(
    private val context: Context
) : PrinterService {
    
    private var printerService: Any? = null  // IWoyouService - using Any to avoid compile-time dependency issues
    private var isBound = false
    
    private val _connectionStatus = MutableStateFlow(ConnectionStatus.Unknown)
    override val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()
    
    private val _printerEvents = MutableSharedFlow<PrinterEvent>(
        replay = 0,
        extraBufferCapacity = 8
    )
    override val printerEvents: Flow<PrinterEvent> = _printerEvents.asSharedFlow()
    
    private val failedJobs = mutableListOf<FailedPrintJob>()
    
    // ========================================================================
    // Service Connection
    // ========================================================================
    
    /**
     * ServiceConnection for binding to Sunmi's AIDL service.
     */
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            try {
                // Use reflection to get the IWoyouService.Stub.asInterface method
                val woyouServiceClass = Class.forName("woyou.aidlservice.jiuiv5.IWoyouService\$Stub")
                val asInterfaceMethod = woyouServiceClass.getMethod("asInterface", IBinder::class.java)
                printerService = asInterfaceMethod.invoke(null, service)
                
                isBound = true
                _connectionStatus.value = ConnectionStatus.Connected
                _printerEvents.tryEmit(PrinterEvent.Reconnected)
                println("[SUNMI] Printer service connected")
            } catch (e: Exception) {
                println("[SUNMI] Failed to get service interface: ${e.message}")
                _connectionStatus.value = ConnectionStatus.Error
            }
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            printerService = null
            isBound = false
            _connectionStatus.value = ConnectionStatus.Disconnected
            _printerEvents.tryEmit(PrinterEvent.Disconnected)
            println("[SUNMI] Printer service disconnected")
        }
    }
    
    // ========================================================================
    // PrinterService Implementation
    // ========================================================================
    
    override suspend fun connect(): PrintResult = withContext(Dispatchers.Main) {
        try {
            _connectionStatus.value = ConnectionStatus.Connecting
            
            val intent = Intent().apply {
                setPackage("woyou.aidlservice.jiuiv5")
                action = "woyou.aidlservice.jiuiv5.IWoyouService"
            }
            
            val bound = context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            
            if (bound) {
                println("[SUNMI] Binding to printer service...")
                PrintResult.Success
            } else {
                _connectionStatus.value = ConnectionStatus.Error
                PrintResult.Error(
                    errorCode = PrintErrorCode.NOT_CONNECTED,
                    message = "Failed to bind to Sunmi printer service. Is this a Sunmi device?",
                    isRecoverable = true
                )
            }
            
        } catch (e: Exception) {
            _connectionStatus.value = ConnectionStatus.Error
            PrintResult.Error(
                errorCode = PrintErrorCode.UNKNOWN,
                message = "Failed to connect: ${e.message}",
                isRecoverable = true
            )
        }
    }
    
    override suspend fun disconnect() {
        try {
            if (isBound) {
                context.unbindService(serviceConnection)
                isBound = false
                printerService = null
                _connectionStatus.value = ConnectionStatus.Disconnected
                println("[SUNMI] Printer service unbound")
            }
        } catch (e: Exception) {
            println("[SUNMI] Error disconnecting: ${e.message}")
        }
    }
    
    override suspend fun printReceipt(receipt: Receipt): PrintResult = withContext(Dispatchers.IO) {
        val service = printerService
        
        if (service == null) {
            storeFailedJob(receipt, PrintErrorCode.NOT_CONNECTED)
            return@withContext PrintResult.Error(
                errorCode = PrintErrorCode.NOT_CONNECTED,
                message = "Printer not connected",
                isRecoverable = true
            )
        }
        
        try {
            // Check printer status first
            val status = invokeMethod(service, "updatePrinterState") as? Int ?: 0
            when (status) {
                3 -> {
                    _printerEvents.tryEmit(PrinterEvent.PaperEmpty)
                    storeFailedJob(receipt, PrintErrorCode.PAPER_EMPTY)
                    return@withContext PrintResult.Error(
                        errorCode = PrintErrorCode.PAPER_EMPTY,
                        message = "Paper is out",
                        isRecoverable = true
                    )
                }
                4 -> {
                    _printerEvents.tryEmit(PrinterEvent.Overheating)
                    return@withContext PrintResult.Error(
                        errorCode = PrintErrorCode.OVERHEATING,
                        message = "Printer is overheating",
                        isRecoverable = true
                    )
                }
            }
            
            // Print header (centered, bold)
            invokeMethod(service, "setAlignment", 1, null) // Center
            invokeMethod(service, "printTextWithFont", receipt.header, null, 28f, null)
            invokeMethod(service, "lineWrap", 1, null)
            
            // Print items (left aligned)
            invokeMethod(service, "setAlignment", 0, null) // Left
            for (item in receipt.items) {
                val line = formatItemLine(item)
                invokeMethod(service, "printTextWithFont", line, null, 24f, null)
                invokeMethod(service, "lineWrap", 1, null)
            }
            
            invokeMethod(service, "lineWrap", 1, null)
            
            // Print totals
            invokeMethod(service, "printTextWithFont", receipt.totals, null, 24f, null)
            invokeMethod(service, "lineWrap", 1, null)
            
            // Print payments
            invokeMethod(service, "printTextWithFont", receipt.payments, null, 24f, null)
            invokeMethod(service, "lineWrap", 1, null)
            
            // Print barcode if present
            receipt.barcode?.let { barcode ->
                invokeMethod(service, "setAlignment", 1, null) // Center
                invokeMethod(service, "printBarCode", barcode, 8, 100, 3, 2, null)
                invokeMethod(service, "lineWrap", 1, null)
            }
            
            // Print footer
            invokeMethod(service, "setAlignment", 1, null) // Center
            invokeMethod(service, "printTextWithFont", receipt.footer, null, 22f, null)
            invokeMethod(service, "lineWrap", 3, null)
            
            // Cut paper (if cutter available)
            try {
                invokeMethod(service, "cutPaper", null)
            } catch (e: Exception) {
                // Some models don't have auto-cutter, ignore
            }
            
            println("[SUNMI] Receipt printed: ${receipt.transactionId}")
            PrintResult.Success
            
        } catch (e: RemoteException) {
            _connectionStatus.value = ConnectionStatus.Error
            _printerEvents.tryEmit(PrinterEvent.Disconnected)
            storeFailedJob(receipt, PrintErrorCode.DISCONNECT_MID_PRINT)
            
            PrintResult.Error(
                errorCode = PrintErrorCode.DISCONNECT_MID_PRINT,
                message = "Printer communication lost: ${e.message}",
                isRecoverable = true
            )
        } catch (e: Exception) {
            storeFailedJob(receipt, PrintErrorCode.UNKNOWN)
            PrintResult.Error(
                errorCode = PrintErrorCode.UNKNOWN,
                message = "Print failed: ${e.message}",
                isRecoverable = true
            )
        }
    }
    
    override suspend fun openCashDrawer(): PrintResult = withContext(Dispatchers.IO) {
        val service = printerService
        
        if (service == null) {
            return@withContext PrintResult.Error(
                errorCode = PrintErrorCode.NOT_CONNECTED,
                message = "Printer not connected",
                isRecoverable = true
            )
        }
        
        try {
            invokeMethod(service, "openDrawer", null)
            println("[SUNMI] Cash drawer opened")
            PrintResult.Success
        } catch (e: Exception) {
            PrintResult.Error(
                errorCode = PrintErrorCode.COMMUNICATION_ERROR,
                message = "Failed to open drawer: ${e.message}",
                isRecoverable = true
            )
        }
    }
    
    override suspend fun checkPaperStatus(): PaperStatus = withContext(Dispatchers.IO) {
        val service = printerService ?: return@withContext PaperStatus.Unknown
        
        try {
            when (invokeMethod(service, "updatePrinterState") as? Int) {
                1 -> PaperStatus.OK       // Normal
                2 -> PaperStatus.Low      // Paper ending
                3 -> PaperStatus.Empty    // Paper out
                else -> PaperStatus.Unknown
            }
        } catch (e: Exception) {
            PaperStatus.Unknown
        }
    }
    
    // ========================================================================
    // Failed Job Management
    // ========================================================================
    
    override fun getFailedPrintJobs(): List<FailedPrintJob> {
        return failedJobs.toList()
    }
    
    override suspend fun retryPrintJob(jobId: String): PrintResult {
        val job = failedJobs.find { it.id == jobId }
            ?: return PrintResult.Error(
                errorCode = PrintErrorCode.UNKNOWN,
                message = "Job not found: $jobId",
                isRecoverable = false
            )
        
        val result = printReceipt(job.receipt)
        
        if (result is PrintResult.Success) {
            failedJobs.removeAll { it.id == jobId }
        } else {
            val index = failedJobs.indexOfFirst { it.id == jobId }
            if (index >= 0) {
                failedJobs[index] = job.copy(retryCount = job.retryCount + 1)
            }
        }
        
        return result
    }
    
    override fun clearFailedPrintJob(jobId: String) {
        failedJobs.removeAll { it.id == jobId }
    }
    
    private fun storeFailedJob(receipt: Receipt, errorCode: PrintErrorCode) {
        failedJobs.add(FailedPrintJob(
            id = UUID.randomUUID().toString(),
            receipt = receipt,
            errorCode = errorCode,
            failedAt = Clock.System.now().toString(),
            retryCount = 0
        ))
    }
    
    // ========================================================================
    // Helpers
    // ========================================================================
    
    private fun formatItemLine(item: ReceiptItem): String {
        val lineWidth = 32 // Sunmi 58mm paper is ~32 chars
        val nameWidth = lineWidth - item.lineTotal.length - 1
        
        val truncatedName = if (item.name.length > nameWidth) {
            item.name.take(nameWidth - 3) + "..."
        } else {
            item.name
        }
        
        val padding = " ".repeat((nameWidth - truncatedName.length).coerceAtLeast(0))
        return "$truncatedName$padding ${item.lineTotal}${item.taxIndicator}"
    }
    
    /**
     * Invokes a method on the Sunmi service using reflection.
     * 
     * This allows us to work around compile-time issues with AIDL stubs
     * while still calling the service methods at runtime.
     */
    private fun invokeMethod(service: Any, methodName: String, vararg args: Any?): Any? {
        return try {
            val methods = service.javaClass.methods.filter { it.name == methodName }
            val method = methods.firstOrNull { it.parameterCount == args.size }
                ?: methods.firstOrNull()
                ?: throw NoSuchMethodException("Method $methodName not found")
            
            method.invoke(service, *args)
        } catch (e: Exception) {
            println("[SUNMI] Method invoke failed: $methodName - ${e.message}")
            null
        }
    }
}
