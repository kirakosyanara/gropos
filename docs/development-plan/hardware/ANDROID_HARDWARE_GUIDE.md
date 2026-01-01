# Android POS Device Hardware Integration Guide

> Complete SDK integration guide for Android-based POS terminals

## Table of Contents

- [Overview](#overview)
- [Supported Devices](#supported-devices)
- [Sunmi Devices](#sunmi-devices)
- [PAX Android Devices](#pax-android-devices)
- [Generic Android Integration](#generic-android-integration)
- [Kotlin Multiplatform Integration](#kotlin-multiplatform-integration)
- [Testing](#testing)

---

## Overview

Android POS devices have built-in hardware (printer, scanner, NFC) that require vendor-specific SDKs. This guide provides integration patterns for each supported manufacturer.

### Hardware Feature Comparison: Desktop vs Android

| Feature | Desktop (JavaPOS) | Android |
|---------|-------------------|---------|
| Receipt Printer | Epson JavaPOS SDK | Built-in (vendor SDK) |
| Barcode Scanner | Datalogic/Serial | Built-in camera or laser |
| Cash Drawer | Printer DK port | USB or Bluetooth |
| Payment Terminal | PAX PosLink (serial/TCP) | Built-in or PAX Android SDK |
| NFC Reader | USB reader | Built-in |
| Scale | jSerialComm | USB or Bluetooth |

---

## Supported Devices

### Tier 1 (Primary Support)

| Manufacturer | Models | Built-in Features |
|--------------|--------|-------------------|
| **Sunmi** | V2 Pro, V2s, T2, T2 Mini, D3 | Printer, Scanner, NFC |
| **PAX** | A920, A930, A35, A77, A60 | Printer, Scanner, NFC, Payment |

### Tier 2 (Secondary Support)

| Manufacturer | Models | Built-in Features |
|--------------|--------|-------------------|
| **Ingenico** | AXIUM series | Printer, Scanner, NFC, Payment |
| **Verifone** | V400c, T400 | Printer, Scanner, Payment |
| **Newland** | N910, N700 | Printer, Scanner, NFC |
| **Telpo** | TPS900, M1 | Printer, Scanner, NFC |

### Tier 3 (Generic Android)

| Configuration | Hardware |
|---------------|----------|
| Tablet + External | Bluetooth printer, USB scanner |
| Elo Android Terminal | Standard Android APIs |
| Custom Devices | Requires vendor SDK |

---

## Sunmi Devices

### SDK Setup

**Gradle Dependency:**
```kotlin
// build.gradle.kts (androidMain)
dependencies {
    implementation("com.sunmi:printerlibrary:1.0.18")
    implementation("com.sunmi:devicelib:1.0.13")
}
```

**Manifest Permissions:**
```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.NFC" />
<uses-feature android:name="android.hardware.nfc" android:required="false" />
```

### Printer Integration

```kotlin
// SunmiPrinterService.kt
class SunmiPrinterService(private val context: Context) {
    private var printerService: SunmiPrinterService? = null
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            printerService = SunmiPrinterService.Stub.asInterface(service)
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            printerService = null
        }
    }
    
    fun initialize() {
        val intent = Intent().apply {
            setPackage("woyou.aidlservice.jiuiv5")
            action = "woyou.aidlservice.jiuiv5.IWoyouService"
        }
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }
    
    suspend fun printText(text: String) = withContext(Dispatchers.IO) {
        printerService?.printText(text, null)
    }
    
    suspend fun printBitmap(bitmap: Bitmap) = withContext(Dispatchers.IO) {
        printerService?.printBitmap(bitmap, null)
    }
    
    suspend fun printBarcode(data: String, symbology: Int, height: Int, width: Int) = 
        withContext(Dispatchers.IO) {
            printerService?.printBarCode(data, symbology, height, width, 1, null)
        }
    
    suspend fun feedPaper() = withContext(Dispatchers.IO) {
        printerService?.lineWrap(3, null)
    }
    
    suspend fun cutPaper() = withContext(Dispatchers.IO) {
        printerService?.cutPaper(null)
    }
    
    fun getPaperStatus(): Int {
        return printerService?.updatePrinterState() ?: -1
        // 1 = OK, 2 = Paper ending, 3 = Paper out
    }
    
    suspend fun openCashDrawer() = withContext(Dispatchers.IO) {
        printerService?.openDrawer(null)
    }
    
    fun release() {
        context.unbindService(connection)
    }
}
```

### Scanner Integration

```kotlin
// SunmiScannerService.kt
class SunmiScannerService(private val context: Context) {
    private val scanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val barcode = intent?.getStringExtra("data") ?: return
            onBarcodeScanned?.invoke(barcode)
        }
    }
    
    var onBarcodeScanned: ((String) -> Unit)? = null
    
    fun startListening() {
        val filter = IntentFilter("com.sunmi.scanner.ACTION_DATA_CODE_RECEIVED")
        context.registerReceiver(scanReceiver, filter)
    }
    
    fun stopListening() {
        context.unregisterReceiver(scanReceiver)
    }
    
    // For camera-based scanning
    fun startCameraScanner(activity: Activity) {
        val intent = Intent("com.sunmi.scan").apply {
            setPackage("com.sunmi.sunmiqrcodescanner")
            putExtra("CURRENT_PPI", 1) // 0=low, 1=medium, 2=high
            putExtra("PLAY_SOUND", true)
            putExtra("PLAY_VIBRATE", true)
        }
        activity.startActivityForResult(intent, SCAN_REQUEST_CODE)
    }
    
    companion object {
        const val SCAN_REQUEST_CODE = 1001
    }
}
```

### NFC Integration

```kotlin
// SunmiNfcService.kt
class SunmiNfcService(private val activity: Activity) {
    private val nfcAdapter = NfcAdapter.getDefaultAdapter(activity)
    
    private val pendingIntent = PendingIntent.getActivity(
        activity, 0,
        Intent(activity, activity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_MUTABLE
    )
    
    fun enableNfcForeground() {
        nfcAdapter?.enableForegroundDispatch(activity, pendingIntent, null, null)
    }
    
    fun disableNfcForeground() {
        nfcAdapter?.disableForegroundDispatch(activity)
    }
    
    fun processNfcIntent(intent: Intent): String? {
        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
            
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            return tag?.id?.joinToString("") { "%02X".format(it) }
        }
        return null
    }
}
```

---

## PAX Android Devices

PAX Android devices have integrated payment terminals, making them ideal for full POS functionality.

### SDK Setup

**Gradle Dependency:**
```kotlin
// PAX SDK is typically provided as AAR files
dependencies {
    implementation(files("libs/pax-payment-sdk.aar"))
    implementation(files("libs/pax-printer-sdk.aar"))
}
```

### Payment Integration

```kotlin
// PaxPaymentService.kt
class PaxPaymentService(private val context: Context) {
    
    suspend fun processCreditSale(amount: BigDecimal): PaymentResult = 
        withContext(Dispatchers.IO) {
            val request = PaymentRequest().apply {
                transType = TransType.SALE
                amountValue = (amount * BigDecimal(100)).toLong() // Cents
                cardReadModes = CardReadMode.ICC or CardReadMode.MAGNETIC or CardReadMode.NFC
            }
            
            val response = PaymentManager.getInstance().startTransaction(request)
            
            PaymentResult(
                isSuccessful = response.resultCode == "000000",
                approvalCode = response.approvalCode,
                referenceNumber = response.referenceNumber,
                cardType = response.cardBrand,
                lastFour = response.maskedPan?.takeLast(4),
                amount = amount,
                errorMessage = response.resultMessage
            )
        }
    
    suspend fun processDebitSale(amount: BigDecimal): PaymentResult = 
        withContext(Dispatchers.IO) {
            val request = PaymentRequest().apply {
                transType = TransType.SALE
                amountValue = (amount * BigDecimal(100)).toLong()
                cardReadModes = CardReadMode.ICC or CardReadMode.MAGNETIC
                isPinRequired = true
            }
            
            val response = PaymentManager.getInstance().startTransaction(request)
            
            PaymentResult(
                isSuccessful = response.resultCode == "000000",
                approvalCode = response.approvalCode,
                referenceNumber = response.referenceNumber,
                cardType = "DEBIT",
                lastFour = response.maskedPan?.takeLast(4),
                amount = amount,
                errorMessage = response.resultMessage
            )
        }
    
    suspend fun processEbtSnap(amount: BigDecimal): PaymentResult = 
        withContext(Dispatchers.IO) {
            val request = PaymentRequest().apply {
                transType = TransType.SALE
                amountValue = (amount * BigDecimal(100)).toLong()
                cardReadModes = CardReadMode.MAGNETIC // EBT typically swipe only
                paymentType = PaymentType.EBT_SNAP
                isPinRequired = true
            }
            
            val response = PaymentManager.getInstance().startTransaction(request)
            
            PaymentResult(
                isSuccessful = response.resultCode == "000000",
                approvalCode = response.approvalCode,
                referenceNumber = response.referenceNumber,
                cardType = "EBT SNAP",
                lastFour = response.maskedPan?.takeLast(4),
                amount = amount,
                errorMessage = response.resultMessage,
                ebtBalance = response.availableBalance?.let { 
                    BigDecimal(it).divide(BigDecimal(100)) 
                }
            )
        }
    
    suspend fun voidTransaction(referenceNumber: String): PaymentResult = 
        withContext(Dispatchers.IO) {
            val request = PaymentRequest().apply {
                transType = TransType.VOID
                origRefNumber = referenceNumber
            }
            
            val response = PaymentManager.getInstance().startTransaction(request)
            
            PaymentResult(
                isSuccessful = response.resultCode == "000000",
                errorMessage = response.resultMessage
            )
        }
    
    suspend fun checkEbtBalance(): EbtBalanceResult = withContext(Dispatchers.IO) {
        val request = PaymentRequest().apply {
            transType = TransType.BALANCE_INQUIRY
            paymentType = PaymentType.EBT_SNAP
            cardReadModes = CardReadMode.MAGNETIC
            isPinRequired = true
        }
        
        val response = PaymentManager.getInstance().startTransaction(request)
        
        EbtBalanceResult(
            snapBalance = response.snapBalance?.let { 
                BigDecimal(it).divide(BigDecimal(100)) 
            } ?: BigDecimal.ZERO,
            cashBenefitBalance = response.cashBalance?.let {
                BigDecimal(it).divide(BigDecimal(100))
            } ?: BigDecimal.ZERO
        )
    }
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
    val cashBenefitBalance: BigDecimal
)
```

### PAX Printer Integration

```kotlin
// PaxPrinterService.kt
class PaxPrinterService(private val context: Context) {
    private val printer = PrinterManager.getInstance()
    
    fun initialize() {
        printer.init()
    }
    
    suspend fun printReceipt(receipt: Receipt) = withContext(Dispatchers.IO) {
        printer.printStr(receipt.header, null)
        printer.step(20)
        
        receipt.items.forEach { item ->
            printer.printStr(item.formatLine(), null)
        }
        
        printer.step(20)
        printer.printStr(receipt.totals, null)
        printer.step(20)
        printer.printStr(receipt.footer, null)
        
        printer.step(100)
        printer.cutPaper(0) // 0 = full cut
    }
    
    suspend fun printBarcode(data: String) = withContext(Dispatchers.IO) {
        printer.printBarCode(
            data,
            200, // height
            300, // width
            PrinterManager.BARCODE_TYPE_CODE128,
            PrinterManager.ALIGN_CENTER
        )
    }
    
    suspend fun openCashDrawer() = withContext(Dispatchers.IO) {
        printer.sendEscCommand(byteArrayOf(0x1B, 0x70, 0x00, 0x19, 0xFA))
    }
    
    fun getPaperStatus(): Int = printer.status
    // 0 = OK, 1 = Busy, 2 = Paper out, 3 = Overheat
}
```

---

## Generic Android Integration

For tablets or devices without built-in POS hardware.

### Bluetooth Printer

```kotlin
// BluetoothPrinterService.kt
class BluetoothPrinterService(private val context: Context) {
    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    
    suspend fun connect(macAddress: String) = withContext(Dispatchers.IO) {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        val device = adapter.getRemoteDevice(macAddress)
        val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        
        socket = device.createRfcommSocketToServiceRecord(uuid)
        socket?.connect()
        outputStream = socket?.outputStream
    }
    
    suspend fun print(data: ByteArray) = withContext(Dispatchers.IO) {
        outputStream?.write(data)
        outputStream?.flush()
    }
    
    suspend fun printText(text: String) {
        print(text.toByteArray(Charsets.UTF_8))
    }
    
    suspend fun feedAndCut() {
        print(byteArrayOf(0x1D, 0x56, 0x00)) // ESC/POS cut command
    }
    
    fun disconnect() {
        outputStream?.close()
        socket?.close()
    }
}
```

### Camera Barcode Scanner (ML Kit)

```kotlin
// CameraScannerService.kt
class CameraScannerService(private val context: Context) {
    private val barcodeScanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39
            )
            .build()
    )
    
    suspend fun processImage(image: InputImage): List<String> = 
        withContext(Dispatchers.IO) {
            suspendCoroutine { continuation ->
                barcodeScanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        val results = barcodes.mapNotNull { it.rawValue }
                        continuation.resume(results)
                    }
                    .addOnFailureListener { e ->
                        continuation.resumeWithException(e)
                    }
            }
        }
}
```

### USB Scale (OTG)

```kotlin
// UsbScaleService.kt
class UsbScaleService(private val context: Context) {
    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private var connection: UsbDeviceConnection? = null
    
    fun findScale(): UsbDevice? {
        return usbManager.deviceList.values.find { device ->
            // CAS Scale vendor/product ID (example)
            device.vendorId == 0x0922 && device.productId == 0x8003
        }
    }
    
    suspend fun readWeight(): BigDecimal = withContext(Dispatchers.IO) {
        val device = findScale() ?: throw Exception("Scale not found")
        connection = usbManager.openDevice(device)
        
        val endpoint = device.getInterface(0).getEndpoint(0)
        val buffer = ByteArray(8)
        
        connection?.bulkTransfer(endpoint, buffer, buffer.size, 1000)
        
        // Parse CAS scale protocol
        val weight = parseWeight(buffer)
        
        weight
    }
    
    private fun parseWeight(data: ByteArray): BigDecimal {
        // CAS scale data format: [status, weight_lb, weight_oz, ...]
        val pounds = data[1].toInt()
        val ounces = data[2].toInt()
        return BigDecimal(pounds + ounces / 16.0).setScale(3, RoundingMode.HALF_UP)
    }
}
```

---

## Kotlin Multiplatform Integration

### Hardware Interface (expect/actual)

```kotlin
// commonMain/hardware/HardwareManager.kt
expect class HardwareManager {
    val printer: PrinterService
    val scanner: ScannerService
    val paymentTerminal: PaymentService
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
}

// commonMain/hardware/PaymentService.kt
expect class PaymentService {
    suspend fun processCreditSale(amount: BigDecimal): PaymentResult
    suspend fun processDebitSale(amount: BigDecimal): PaymentResult
    suspend fun processEbtSnap(amount: BigDecimal): PaymentResult
    suspend fun processEbtCash(amount: BigDecimal): PaymentResult
    suspend fun voidPayment(referenceNumber: String): PaymentResult
    suspend fun checkEbtBalance(): EbtBalanceResult
}
```

### Desktop Implementation

```kotlin
// desktopMain/hardware/HardwareManager.desktop.kt
actual class HardwareManager(
    private val jposConfig: JposConfig
) {
    actual val printer: PrinterService = PrinterServiceDesktop(jposConfig)
    actual val scanner: ScannerService = ScannerServiceDesktop(jposConfig)
    actual val paymentTerminal: PaymentService = PaymentServiceDesktop(jposConfig)
    actual val cashDrawer: CashDrawerService = CashDrawerServiceDesktop(jposConfig)
    actual val scale: ScaleService = ScaleServiceDesktop(jposConfig)
}

// Uses existing hardware module (JavaPOS + PAX PosLink)
actual class PrinterServiceDesktop(config: JposConfig) : PrinterService {
    private val epsonPrinter = MPOSPrinter(config)
    
    actual suspend fun printReceipt(receipt: Receipt): Result<Unit> = 
        withContext(Dispatchers.IO) {
            runCatching {
                epsonPrinter.print(receipt.toJson())
            }
        }
    // ... other methods wrap existing Java code
}
```

### Android Implementation (Sunmi)

```kotlin
// androidMain/hardware/HardwareManager.android.kt
actual class HardwareManager(
    private val context: Context,
    private val deviceType: AndroidDeviceType
) {
    actual val printer: PrinterService = when (deviceType) {
        AndroidDeviceType.SUNMI -> SunmiPrinterServiceImpl(context)
        AndroidDeviceType.PAX -> PaxPrinterServiceImpl(context)
        AndroidDeviceType.GENERIC -> BluetoothPrinterServiceImpl(context)
    }
    
    actual val scanner: ScannerService = when (deviceType) {
        AndroidDeviceType.SUNMI -> SunmiScannerServiceImpl(context)
        AndroidDeviceType.PAX -> PaxScannerServiceImpl(context)
        AndroidDeviceType.GENERIC -> CameraScannerServiceImpl(context)
    }
    
    actual val paymentTerminal: PaymentService = when (deviceType) {
        AndroidDeviceType.PAX -> PaxPaymentServiceImpl(context)
        else -> ExternalTerminalService(context) // Bluetooth/network terminal
    }
    
    actual val cashDrawer: CashDrawerService = printer // Usually via printer
    actual val scale: ScaleService? = UsbScaleServiceImpl(context)
}

enum class AndroidDeviceType {
    SUNMI, PAX, GENERIC
}
```

---

## Testing

### Mock Hardware for Testing

```kotlin
// commonTest/hardware/MockHardwareManager.kt
class MockHardwareManager : HardwareManager {
    override val printer = MockPrinterService()
    override val scanner = MockScannerService()
    override val paymentTerminal = MockPaymentService()
    override val cashDrawer = MockCashDrawerService()
    override val scale = MockScaleService()
}

class MockPrinterService : PrinterService {
    val printedReceipts = mutableListOf<Receipt>()
    var paperStatus = PaperStatus.OK
    
    override suspend fun printReceipt(receipt: Receipt): Result<Unit> {
        printedReceipts.add(receipt)
        return Result.success(Unit)
    }
    
    override fun getPaperStatus() = paperStatus
    override fun isConnected() = true
    // ...
}
```

### Device Detection

```kotlin
// androidMain/DeviceDetector.kt
object DeviceDetector {
    fun detectDeviceType(context: Context): AndroidDeviceType {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val model = Build.MODEL.lowercase()
        
        return when {
            manufacturer.contains("sunmi") -> AndroidDeviceType.SUNMI
            manufacturer.contains("pax") -> AndroidDeviceType.PAX
            model.contains("a920") || model.contains("a930") -> AndroidDeviceType.PAX
            else -> AndroidDeviceType.GENERIC
        }
    }
}
```

---

## Summary

| Device Type | Printer | Scanner | Payment | Scale |
|-------------|---------|---------|---------|-------|
| **Desktop** | Epson JavaPOS | Datalogic Serial | PAX PosLink | CAS Serial |
| **Sunmi** | Sunmi SDK | Sunmi SDK | External | USB OTG |
| **PAX Android** | PAX SDK | PAX SDK | PAX SDK (built-in) | USB OTG |
| **Generic Android** | Bluetooth | ML Kit Camera | External | USB OTG |

The key principle is using `expect/actual` declarations to provide a unified API in shared code while implementing platform-specific hardware access.

