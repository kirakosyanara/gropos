# Desktop Hardware Integration

**Version:** 2.0 (Kotlin/Compose Multiplatform)  
**Status:** Specification Document

This document specifies desktop hardware integration for GroPOS using Kotlin and the expect/actual pattern for cross-platform support.

---

## Overview

GroPOS desktop hardware integration is built on two main technologies:

| Technology | Devices | Use Case |
|------------|---------|----------|
| **JavaPOS** | Datalogic scanner/scale, Epson printer | Integrated combo devices |
| **Serial/jSerialComm** | CAS scales, generic USB scanners, coin dispensers | Standalone devices |
| **PosLink SDK** | PAX terminals | Payment processing |

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        GroPOS Application (Desktop)                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                    Kotlin Multiplatform expect/actual                   │ │
│  │                                                                         │ │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌────────────────┐ │ │
│  │  │ PrinterSvc  │  │ ScannerSvc  │  │  ScaleSvc   │  │ PaymentTermSvc │ │ │
│  │  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └───────┬────────┘ │ │
│  └─────────┼────────────────┼────────────────┼─────────────────┼──────────┘ │
│            │                │                │                 │            │
│            ▼                ▼                ▼                 ▼            │
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │                      HardwareManager (Singleton)                         ││
│  └─────────────────────────────────────────────────────────────────────────┘│
│                                    │                                         │
└────────────────────────────────────┼─────────────────────────────────────────┘
                                     │
          ┌──────────────────────────┼──────────────────────────┐
          │                          │                          │
          ▼                          ▼                          ▼
┌─────────────────┐       ┌─────────────────┐       ┌─────────────────┐
│    JavaPOS      │       │   jSerialComm   │       │   PAX PosLink   │
│                 │       │                 │       │                 │
│ • Scanner       │       │ • CAS Scale     │       │ • Credit/Debit  │
│ • Scale         │       │ • USB Scanner   │       │ • SNAP/WIC      │
│ • Printer       │       │ • Coin Display  │       │ • EBT Cash      │
│ • Cash Drawer   │       │                 │       │                 │
└─────────────────┘       └─────────────────┘       └─────────────────┘
          │                          │                          │
          ▼                          ▼                          ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                            Physical Hardware                                 │
│  Datalogic Magellan  │  CAS Scale  │  Epson TM-T88VI  │  PAX A920/S300     │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Supported Hardware

### Barcode Scanners

| Device | Integration | Features |
|--------|-------------|----------|
| **Datalogic Magellan** | JavaPOS | Built-in with scale, symbology detection |
| **Generic USB Scanner** | Serial (jSerialComm) | Any scanner outputting via COM port |

### Weight Scales

| Device | Integration | Features |
|--------|-------------|----------|
| **Datalogic Magellan** | JavaPOS | Built-in with scanner, live weight |
| **CAS Scale** | Serial (jSerialComm) | Standalone scale, proprietary protocol |

### Receipt Printers

| Device | Integration | Features |
|--------|-------------|----------|
| **Epson TM-T88VI** | JavaPOS | 80mm thermal, auto-cut, cash drawer port |
| **Epson TM-T88V** | JavaPOS | 80mm thermal, auto-cut |

### Cash Drawers

| Device | Integration | Features |
|--------|-------------|----------|
| **Standard DK Drawer** | JavaPOS (via Printer) | Connected to Epson DK port |

### Payment Terminals

| Device | Integration | Features |
|--------|-------------|----------|
| **PAX A920** | PosLink SDK | Android, WiFi/4G, touchscreen |
| **PAX A80** | PosLink SDK | Android, Ethernet/WiFi |
| **PAX S300** | PosLink SDK | PIN pad, compact |

---

## Kotlin Multiplatform Interface

### Common Interface Definitions

```kotlin
// commonMain/hardware/HardwareManager.kt
expect class HardwareManager {
    val printer: PrinterService
    val scanner: ScannerService
    val paymentTerminal: PaymentTerminalService
    val cashDrawer: CashDrawerService
    val scale: ScaleService?
}

// commonMain/hardware/PrinterService.kt
expect class PrinterService {
    suspend fun initialize(): Result<Unit>
    suspend fun printReceipt(receipt: Receipt): Result<Unit>
    suspend fun printBarcode(data: String): Result<Unit>
    suspend fun openCashDrawer(): Result<Unit>
    fun getPaperStatus(): PaperStatus
    fun isConnected(): Boolean
}

enum class PaperStatus { OK, LOW, EMPTY, UNKNOWN }

// commonMain/hardware/ScannerService.kt
expect class ScannerService {
    fun startListening(onBarcode: (String) -> Unit)
    fun stopListening()
    fun isConnected(): Boolean
}

// commonMain/hardware/ScaleService.kt
expect class ScaleService {
    fun startListening(onWeight: (WeightData) -> Unit)
    fun stopListening()
    fun getCurrentWeight(): WeightData?
    fun tare()
    fun zero()
    fun isConnected(): Boolean
}

data class WeightData(
    val weight: BigDecimal,
    val unit: WeightUnit,
    val isStable: Boolean,
    val isOverweight: Boolean,
    val isUnderZero: Boolean
)

enum class WeightUnit { LB, KG, OZ }

// commonMain/hardware/PaymentTerminalService.kt
expect class PaymentTerminalService {
    suspend fun processCreditSale(amount: BigDecimal): PaymentResult
    suspend fun processDebitSale(amount: BigDecimal): PaymentResult
    suspend fun processSnapPayment(amount: BigDecimal): PaymentResult
    suspend fun processEbtCash(amount: BigDecimal): PaymentResult
    suspend fun voidPayment(referenceNumber: String): PaymentResult
    suspend fun checkEbtBalance(): EbtBalanceResult
}

data class PaymentResult(
    val isSuccessful: Boolean,
    val approvalCode: String? = null,
    val referenceNumber: String? = null,
    val cardType: String? = null,
    val lastFour: String? = null,
    val amount: BigDecimal? = null,
    val errorMessage: String? = null,
    val ebtBalance: BigDecimal? = null
)

data class EbtBalanceResult(
    val snapBalance: BigDecimal,
    val cashBalance: BigDecimal
)
```

---

## Desktop Implementation

### Hardware Manager

```kotlin
// desktopMain/hardware/HardwareManager.desktop.kt
actual class HardwareManager private constructor() {
    
    actual val printer: PrinterService
    actual val scanner: ScannerService
    actual val paymentTerminal: PaymentTerminalService
    actual val cashDrawer: CashDrawerService
    actual val scale: ScaleService?
    
    init {
        // Initialize JavaPOS system
        JposInitializer.initialize()
        
        // Select devices based on configuration
        val useIndividualScale = Configurator.loadIndividualScalePort().isNotEmpty()
        
        printer = EpsonPrinterService()
        cashDrawer = printer // Cash drawer via printer DK port
        
        if (useIndividualScale) {
            scanner = DatalogicScannerService()
            scale = CasScaleService(Configurator.loadIndividualScalePort())
        } else {
            // Combined scanner/scale (Datalogic Magellan)
            scanner = DatalogicScannerService()
            scale = DatalogicScaleService()
        }
        
        // Additional serial scanner (can run alongside JavaPOS)
        val serialScannerPort = Configurator.loadBarcodeScannerPort()
        if (serialScannerPort.isNotEmpty()) {
            serialScanner = SerialBarcodeScanner(serialScannerPort)
        }
        
        paymentTerminal = PaxPaymentService()
    }
    
    private var serialScanner: SerialBarcodeScanner? = null
    
    companion object {
        private var instance: HardwareManager? = null
        
        fun getInstance(): HardwareManager {
            if (instance == null) {
                instance = HardwareManager()
            }
            return instance!!
        }
    }
    
    fun shutdown() {
        printer.close()
        scanner.stopListening()
        scale?.stopListening()
        serialScanner?.close()
    }
}
```

### Barcode Scanner (Datalogic JavaPOS)

```kotlin
// desktopMain/hardware/DatalogicScannerService.kt
actual class ScannerService {
    private var scanner: Scanner? = null
    private var dataListener: ((String) -> Unit)? = null
    
    init {
        try {
            scanner = Scanner().apply {
                open("POSScanner")
                claim(1000)
                deviceEnabled = true
                decodeData = true
                dataEventEnabled = true
                
                addDataListener { event ->
                    val barcode = String(
                        scanner?.scanDataLabel ?: byteArrayOf(),
                        Charsets.UTF_8
                    ).trim()
                    
                    dataListener?.invoke(barcode)
                    dataEventEnabled = true // Re-enable for next scan
                }
            }
        } catch (e: JposException) {
            logger.error("Failed to initialize scanner: ${e.message}")
        }
    }
    
    actual fun startListening(onBarcode: (String) -> Unit) {
        dataListener = onBarcode
        scanner?.dataEventEnabled = true
    }
    
    actual fun stopListening() {
        dataListener = null
        scanner?.dataEventEnabled = false
    }
    
    actual fun isConnected(): Boolean {
        return scanner?.claimed == true
    }
    
    fun close() {
        scanner?.close()
    }
}
```

### Weight Scale (CAS Serial)

```kotlin
// desktopMain/hardware/CasScaleService.kt
class CasScaleService(private val portName: String) : ScaleService {
    private var serialPort: SerialPort? = null
    private var readerThread: Thread? = null
    private var isRunning = false
    private var weightListener: ((WeightData) -> Unit)? = null
    private var lastWeight: WeightData? = null
    
    init {
        connect()
    }
    
    private fun connect() {
        serialPort = SerialPort.getCommPort(portName).apply {
            baudRate = 9600
            numDataBits = 8
            numStopBits = 1
            parity = SerialPort.NO_PARITY
            setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 100, 0)
        }
        
        if (!serialPort!!.openPort()) {
            logger.error("Failed to open scale port: $portName")
        }
    }
    
    actual fun startListening(onWeight: (WeightData) -> Unit) {
        weightListener = onWeight
        isRunning = true
        
        readerThread = thread(isDaemon = true) {
            val buffer = ByteArray(32)
            
            while (isRunning) {
                try {
                    val bytesRead = serialPort?.readBytes(buffer, buffer.size) ?: 0
                    if (bytesRead > 0) {
                        val weightData = parseCasProtocol(buffer, bytesRead)
                        lastWeight = weightData
                        weightListener?.invoke(weightData)
                    }
                } catch (e: Exception) {
                    logger.error("Scale read error: ${e.message}")
                }
            }
        }
    }
    
    private fun parseCasProtocol(data: ByteArray, length: Int): WeightData {
        // CAS protocol: [STX][Status][Weight x 6][Unit][ETX]
        val status = data[1].toInt()
        val weightStr = String(data.sliceArray(2..7))
        val unit = when (data[8].toInt().toChar()) {
            'L' -> WeightUnit.LB
            'K' -> WeightUnit.KG
            else -> WeightUnit.LB
        }
        
        val weight = BigDecimal(weightStr.trim()).setScale(3, RoundingMode.HALF_UP)
        
        return WeightData(
            weight = weight,
            unit = unit,
            isStable = (status and 0x01) == 0,
            isOverweight = (status and 0x04) != 0,
            isUnderZero = weight < BigDecimal.ZERO
        )
    }
    
    actual fun stopListening() {
        isRunning = false
        readerThread?.interrupt()
    }
    
    actual fun getCurrentWeight(): WeightData? = lastWeight
    
    actual fun tare() {
        // Send tare command
        serialPort?.writeBytes(byteArrayOf(0x54), 1) // 'T'
    }
    
    actual fun zero() {
        // Send zero command
        serialPort?.writeBytes(byteArrayOf(0x5A), 1) // 'Z'
    }
    
    actual fun isConnected(): Boolean = serialPort?.isOpen == true
    
    fun close() {
        stopListening()
        serialPort?.closePort()
    }
}
```

### Receipt Printer (Epson JavaPOS)

```kotlin
// desktopMain/hardware/EpsonPrinterService.kt
actual class PrinterService {
    private var printer: POSPrinter? = null
    private var cashDrawer: CashDrawer? = null
    
    init {
        try {
            printer = POSPrinter().apply {
                open("POSPrinter")
                claim(1000)
                deviceEnabled = true
                asyncMode = false
            }
            
            cashDrawer = CashDrawer().apply {
                open("CashDrawer")
                claim(1000)
                deviceEnabled = true
            }
        } catch (e: JposException) {
            logger.error("Failed to initialize printer: ${e.message}")
        }
    }
    
    actual suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            printer?.deviceEnabled = true
        }
    }
    
    actual suspend fun printReceipt(receipt: Receipt): Result<Unit> = 
        withContext(Dispatchers.IO) {
            runCatching {
                val p = printer ?: throw Exception("Printer not connected")
                
                // Header
                p.printNormal(POSPrinterConst.PTR_S_RECEIPT, receipt.header)
                
                // Items
                for (item in receipt.items) {
                    p.printNormal(POSPrinterConst.PTR_S_RECEIPT, item.formatLine())
                }
                
                // Totals
                p.printNormal(POSPrinterConst.PTR_S_RECEIPT, receipt.totals)
                
                // Barcode
                if (receipt.barcode.isNotEmpty()) {
                    p.printBarCode(
                        POSPrinterConst.PTR_S_RECEIPT,
                        receipt.barcode,
                        POSPrinterConst.PTR_BCS_Code128,
                        60, // height
                        p.recLineWidth,
                        POSPrinterConst.PTR_BC_CENTER,
                        POSPrinterConst.PTR_BC_TEXT_BELOW
                    )
                }
                
                // Footer
                p.printNormal(POSPrinterConst.PTR_S_RECEIPT, receipt.footer)
                
                // Cut paper
                p.cutPaper(90)
            }
        }
    
    actual suspend fun printBarcode(data: String): Result<Unit> = 
        withContext(Dispatchers.IO) {
            runCatching {
                printer?.printBarCode(
                    POSPrinterConst.PTR_S_RECEIPT,
                    data,
                    POSPrinterConst.PTR_BCS_Code128,
                    60,
                    printer?.recLineWidth ?: 400,
                    POSPrinterConst.PTR_BC_CENTER,
                    POSPrinterConst.PTR_BC_TEXT_BELOW
                )
            }
        }
    
    actual suspend fun openCashDrawer(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            cashDrawer?.openDrawer()
        }
    }
    
    actual fun getPaperStatus(): PaperStatus {
        return when {
            printer == null -> PaperStatus.UNKNOWN
            printer?.recEmpty == true -> PaperStatus.EMPTY
            printer?.recNearEnd == true -> PaperStatus.LOW
            else -> PaperStatus.OK
        }
    }
    
    actual fun isConnected(): Boolean = printer?.claimed == true
    
    fun close() {
        printer?.close()
        cashDrawer?.close()
    }
}
```

### Payment Terminal (PAX PosLink)

```kotlin
// desktopMain/hardware/PaxPaymentService.kt
actual class PaymentTerminalService {
    private val posLink: PosLink
    
    init {
        val commSetting = CommSetting().apply {
            type = CommSetting.TCP
            destIP = Configurator.loadPaymentTerminalIP()
            destPort = Configurator.loadPaymentTerminalPort()
            timeout = "60000"
        }
        posLink = PosLink(commSetting)
    }
    
    actual suspend fun processCreditSale(amount: BigDecimal): PaymentResult = 
        withContext(Dispatchers.IO) {
            val request = PaymentRequest().apply {
                transType = PaymentRequest.TransType.SALE
                amount = formatAmount(amount)
                tenderType = PaymentRequest.TenderType.CREDIT
            }
            
            posLink.paymentRequest = request
            val result = posLink.processTransaction()
            
            parsePaymentResponse(result, posLink.paymentResponse)
        }
    
    actual suspend fun processDebitSale(amount: BigDecimal): PaymentResult = 
        withContext(Dispatchers.IO) {
            val request = PaymentRequest().apply {
                transType = PaymentRequest.TransType.SALE
                amount = formatAmount(amount)
                tenderType = PaymentRequest.TenderType.DEBIT
            }
            
            posLink.paymentRequest = request
            val result = posLink.processTransaction()
            
            parsePaymentResponse(result, posLink.paymentResponse)
        }
    
    actual suspend fun processSnapPayment(amount: BigDecimal): PaymentResult = 
        withContext(Dispatchers.IO) {
            val request = PaymentRequest().apply {
                transType = PaymentRequest.TransType.SALE
                amount = formatAmount(amount)
                tenderType = PaymentRequest.TenderType.EBT
                ebtType = PaymentRequest.EbtType.FOODSTAMP
            }
            
            posLink.paymentRequest = request
            val result = posLink.processTransaction()
            
            val response = parsePaymentResponse(result, posLink.paymentResponse)
            
            // Include remaining balance
            response.copy(
                ebtBalance = posLink.paymentResponse?.remainingBalance?.let { 
                    BigDecimal(it).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
                }
            )
        }
    
    actual suspend fun processEbtCash(amount: BigDecimal): PaymentResult = 
        withContext(Dispatchers.IO) {
            val request = PaymentRequest().apply {
                transType = PaymentRequest.TransType.SALE
                amount = formatAmount(amount)
                tenderType = PaymentRequest.TenderType.EBT
                ebtType = PaymentRequest.EbtType.CASHBENEFIT
            }
            
            posLink.paymentRequest = request
            val result = posLink.processTransaction()
            
            parsePaymentResponse(result, posLink.paymentResponse)
        }
    
    actual suspend fun voidPayment(referenceNumber: String): PaymentResult = 
        withContext(Dispatchers.IO) {
            val request = PaymentRequest().apply {
                transType = PaymentRequest.TransType.VOID
                origRefNumber = referenceNumber
            }
            
            posLink.paymentRequest = request
            val result = posLink.processTransaction()
            
            parsePaymentResponse(result, posLink.paymentResponse)
        }
    
    actual suspend fun checkEbtBalance(): EbtBalanceResult = 
        withContext(Dispatchers.IO) {
            val request = PaymentRequest().apply {
                transType = PaymentRequest.TransType.BALANCEINQUIRY
                tenderType = PaymentRequest.TenderType.EBT
                ebtType = PaymentRequest.EbtType.FOODSTAMP
            }
            
            posLink.paymentRequest = request
            posLink.processTransaction()
            
            val response = posLink.paymentResponse
            
            EbtBalanceResult(
                snapBalance = response?.remainingBalance?.let {
                    BigDecimal(it).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
                } ?: BigDecimal.ZERO,
                cashBalance = response?.cashBackBalance?.let {
                    BigDecimal(it).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
                } ?: BigDecimal.ZERO
            )
        }
    
    private fun formatAmount(amount: BigDecimal): String {
        return amount.multiply(BigDecimal(100)).toLong().toString()
    }
    
    private fun parsePaymentResponse(
        result: ProcessTransResult, 
        response: PaymentResponse?
    ): PaymentResult {
        return if (result == ProcessTransResult.OK && 
                   response?.resultCode == "000000") {
            PaymentResult(
                isSuccessful = true,
                approvalCode = response.authCode,
                referenceNumber = response.refNum,
                cardType = response.cardType,
                lastFour = response.maskedPAN?.takeLast(4),
                amount = response.approvedAmount?.let {
                    BigDecimal(it).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
                }
            )
        } else {
            PaymentResult(
                isSuccessful = false,
                errorMessage = response?.resultTxt ?: result.name
            )
        }
    }
}
```

---

## Configuration

### JavaPOS Configuration (jpos.xml)

**Location:** `C:\Program Files\GroPOS\jpos.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<JposEntries>
    <JposEntry logicalName="POSScanner">
        <creation factoryClass="..." serviceClass="..."/>
        <vendor name="Datalogic" url="..."/>
        <jpos category="Scanner" version="1.14"/>
        <product description="Datalogic Magellan Scanner" name="Magellan" url="..."/>
    </JposEntry>
    
    <JposEntry logicalName="POSScale">
        <creation factoryClass="..." serviceClass="..."/>
        <vendor name="Datalogic" url="..."/>
        <jpos category="Scale" version="1.14"/>
    </JposEntry>
    
    <JposEntry logicalName="POSPrinter">
        <creation factoryClass="..." serviceClass="..."/>
        <vendor name="Epson" url="..."/>
        <jpos category="POSPrinter" version="1.14"/>
    </JposEntry>
    
    <JposEntry logicalName="CashDrawer">
        <creation factoryClass="..." serviceClass="..."/>
        <vendor name="Epson" url="..."/>
        <jpos category="CashDrawer" version="1.14"/>
    </JposEntry>
</JposEntries>
```

### PAX Terminal Configuration

**Location:** `C:\Program Files\GroPOS\comm_setting.json`

```json
{
    "commType": "TCP",
    "destIP": "192.168.1.100",
    "destPort": "10009",
    "timeOut": "60000"
}
```

### Serial Port Configuration

```kotlin
// Hardware configuration stored in local preferences
object Configurator {
    fun loadBarcodeScannerPort(): String = preferences.getString("scanner_port", "")
    fun saveBarcodeScannerPort(port: String) = preferences.putString("scanner_port", port)
    
    fun loadIndividualScalePort(): String = preferences.getString("scale_port", "")
    fun saveIndividualScalePort(port: String) = preferences.putString("scale_port", port)
    
    fun loadPaymentTerminalIP(): String = preferences.getString("terminal_ip", "192.168.1.100")
    fun loadPaymentTerminalPort(): String = preferences.getString("terminal_port", "10009")
}
```

---

## Callback System

### Scanner Callback Flow

```kotlin
// ViewModel registers for scanner events
class HomeViewModel(
    private val hardwareManager: HardwareManager
) : ViewModel() {
    
    init {
        hardwareManager.scanner.startListening { barcode ->
            viewModelScope.launch(Dispatchers.Main) {
                handleBarcodeScanned(barcode)
            }
        }
    }
    
    private fun handleBarcodeScanned(barcode: String) {
        // Process barcode...
    }
    
    override fun onCleared() {
        hardwareManager.scanner.stopListening()
        super.onCleared()
    }
}
```

### Scale Callback Flow

```kotlin
class ScaleViewModel(
    private val hardwareManager: HardwareManager
) : ViewModel() {
    
    private val _weightState = MutableStateFlow(WeightState())
    val weightState: StateFlow<WeightState> = _weightState.asStateFlow()
    
    init {
        hardwareManager.scale?.startListening { weightData ->
            viewModelScope.launch(Dispatchers.Main) {
                _weightState.value = WeightState(
                    weight = weightData.weight,
                    unit = weightData.unit.displayName,
                    isStable = weightData.isStable,
                    showOverweightWarning = weightData.isOverweight
                )
            }
        }
    }
}

data class WeightState(
    val weight: BigDecimal = BigDecimal.ZERO,
    val unit: String = "lb",
    val isStable: Boolean = false,
    val showOverweightWarning: Boolean = false
)
```

---

## Related Documentation

- [Android Hardware Guide](./ANDROID_HARDWARE_GUIDE.md) - Android device integration
- [Payment Processing](../features/PAYMENT_PROCESSING.md) - Payment flow
- [Transaction Flow](../features/TRANSACTION_FLOW.md) - Overall transaction handling

---

*Last Updated: January 2026*

