# GrowPOS Architecture Blueprint: Kotlin + Compose Multiplatform

**Version:** 1.0  
**Status:** Development Plan  
**Date:** January 2026  
**Purpose:** Complete guide to build GrowPOS from scratch with Kotlin + Compose Multiplatform

---

## Executive Summary

This document is the complete blueprint for building GrowPOS from scratch using **Kotlin + Compose Multiplatform**. A single codebase will power **Windows**, **Linux**, and **Android** platforms with full hardware integration, modern architecture, and exceptional maintainability.

### Why This Will Be Awesome

| Benefit | Description |
|---------|-------------|
| **Single Codebase** | ~70-80% code shared across all platforms |
| **Modern UI** | Declarative, reactive UI with automatic state management |
| **Type Safety** | Kotlin's null safety eliminates NullPointerExceptions |
| **Hardware Reuse** | 100% compatibility with existing JavaPOS libraries |
| **Performance** | Skia-based rendering, efficient recomposition |
| **Maintainability** | Less boilerplate, clearer data flow |
| **Future-Proof** | Active development by JetBrains and Google |

---

## Table of Contents

1. [Platform Support Matrix](#1-platform-support-matrix)
2. [Technology Stack](#2-technology-stack)
3. [Project Architecture](#3-project-architecture)
4. [Code Sharing Strategy](#4-code-sharing-strategy)
5. [Hardware Integration Approach](#5-hardware-integration-approach)
6. [Build Phases](#6-build-phases)
7. [Platform-Specific Guidelines](#7-platform-specific-guidelines)
8. [Build & Deployment](#8-build--deployment)
9. [Testing Strategy](#9-testing-strategy)
10. [Risk Assessment & Mitigation](#10-risk-assessment--mitigation)

---

## 1. Platform Support Matrix

### 1.1 Compose Multiplatform Version Requirements

| Component | Minimum Version | Recommended Version | Notes |
|-----------|-----------------|---------------------|-------|
| **Compose Multiplatform** | 1.5.0 | 1.6.x (Latest Stable) | Desktop & Android stable |
| **Kotlin** | 1.9.0 | 2.0.x | K2 compiler recommended |
| **Gradle** | 8.0 | 8.5+ | Required for Kotlin 2.0 |

### 1.2 Desktop Platform Support (Windows & Linux)

#### Windows

| Aspect | Requirement | Notes |
|--------|-------------|-------|
| **OS Version** | Windows 10 (1809+) | Windows 11 fully supported |
| **Architecture** | x64, ARM64 | ARM64 support via JVM |
| **JVM** | JDK 17+ | JDK 21 LTS recommended |
| **Memory** | 4GB RAM minimum | 8GB recommended for development |
| **Display** | Multi-monitor supported | Customer display as secondary window |

**Windows-Specific Features:**
- Native window decorations
- System tray support
- Taskbar integration
- File system watchers
- Serial port access (via jSerialComm)
- USB device access (via usb4java)

#### Linux

| Aspect | Requirement | Notes |
|--------|-------------|-------|
| **Distributions** | Ubuntu 20.04+, Debian 11+, Fedora 35+, RHEL 8+ | Any distro with GTK3/X11 or Wayland |
| **Architecture** | x64, ARM64 | Raspberry Pi 4+ for ARM |
| **JVM** | JDK 17+ | OpenJDK or Adoptium recommended |
| **Desktop Environment** | GNOME, KDE, XFCE | Wayland and X11 supported |
| **Display Server** | X11 or Wayland | X11 more stable for multi-monitor |

**Linux-Specific Considerations:**
- Serial port permissions (`/dev/ttyUSB*`, `/dev/ttyACM*`)
- User must be in `dialout` group for hardware access
- AppImage or DEB/RPM packaging recommended
- Systemd service for auto-start

### 1.3 Android Platform Support

| Aspect | Requirement | Notes |
|--------|-------------|-------|
| **Android API** | API 24 (Android 7.0) minimum | API 26+ recommended |
| **Target API** | API 34 (Android 14) | Required for Play Store |
| **Architecture** | ARM64-v8a, armeabi-v7a, x86_64 | ARM64 primary for POS devices |
| **RAM** | 2GB minimum | 3GB+ recommended |

**Supported Android POS Devices:**

| Manufacturer | Models | SDK Available |
|--------------|--------|---------------|
| **Sunmi** | V2 Pro, V2s, T2, T2 Mini, D3 | Yes - Sunmi SDK |
| **PAX** | A920, A930, A35, A77 | Yes - PAX Android SDK |
| **Ingenico** | AXIUM series | Yes - Ingenico SDK |
| **Verifone** | V400c, T400 | Yes - Verifone SDK |
| **Newland** | N910, N700 | Yes - Newland SDK |
| **Telpo** | TPS900, M1 | Yes - Telpo SDK |
| **Elo** | Android POS terminals | Standard Android |

**Android Version Distribution for POS:**

| Android Version | API Level | POS Device Support |
|-----------------|-----------|-------------------|
| Android 7.x | API 24-25 | Legacy devices |
| Android 8.x | API 26-27 | Common on older POS |
| Android 9.x | API 28 | Widely supported |
| Android 10.x | API 29 | Current standard |
| Android 11.x | API 30 | Newer devices |
| Android 12.x | API 31-32 | Latest POS devices |
| Android 13.x | API 33 | Premium devices |
| Android 14.x | API 34 | Cutting edge |

---

## 2. Technology Stack

### 2.1 Core Technologies

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         TECHNOLOGY STACK                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │                         SHARED (commonMain)                           │   │
│  │                                                                       │   │
│  │  Language:        Kotlin 2.0+                                         │   │
│  │  UI Framework:    Compose Multiplatform 1.6+                          │   │
│  │  Async:           Kotlin Coroutines + Flow                            │   │
│  │  DI:              Koin 3.5+ (Multiplatform)                           │   │
│  │  Serialization:   Kotlinx.serialization                               │   │
│  │  HTTP:            Ktor Client 2.3+                                    │   │
│  │  DateTime:        Kotlinx.datetime                                    │   │
│  │  Navigation:      Compose Navigation (Voyager or Decompose)          │   │
│  │  State Mgmt:      ViewModel + StateFlow                               │   │
│  │                                                                       │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
│  ┌─────────────────────────┐  ┌─────────────────────────────────────────┐   │
│  │    DESKTOP (JVM)        │  │              ANDROID                     │   │
│  │                         │  │                                          │   │
│  │  Database:              │  │  Database:                               │   │
│  │    SQLDelight or        │  │    SQLDelight (Android driver)           │   │
│  │    CouchbaseLite        │  │    or Room (if Android-only DB)          │   │
│  │                         │  │                                          │   │
│  │  Hardware:              │  │  Hardware:                               │   │
│  │    JavaPOS 1.15         │  │    Sunmi SDK / PAX Android SDK           │   │
│  │    PAX PosLink          │  │    AIDL Printer Services                 │   │
│  │    Epson JPOS           │  │    Bluetooth/USB Peripherals             │   │
│  │    jSerialComm          │  │                                          │   │
│  │                         │  │  Permissions:                            │   │
│  │  Packaging:             │  │    USB_PERMISSION                        │   │
│  │    JLink / Conveyor     │  │    BLUETOOTH                             │   │
│  │    Installers (MSI/DEB) │  │    CAMERA (for scanning)                 │   │
│  │                         │  │                                          │   │
│  └─────────────────────────┘  └─────────────────────────────────────────┘   │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 Library Versions

| Library | Version | Purpose | Platform |
|---------|---------|---------|----------|
| **Kotlin** | 2.0.0+ | Language | All |
| **Compose Multiplatform** | 1.6.0+ | UI Framework | All |
| **Kotlin Coroutines** | 1.8.0+ | Async programming | All |
| **Koin** | 3.5.0+ | Dependency Injection | All |
| **Ktor Client** | 2.3.0+ | HTTP networking | All |
| **Kotlinx.serialization** | 1.6.0+ | JSON serialization | All |
| **SQLDelight** | 2.0.0+ | Database | All |
| **Voyager** | 1.0.0+ | Navigation | All |
| **CouchbaseLite** | 3.1.3 | Sync database (optional) | Desktop/Android |
| **JavaPOS** | 1.15.2 | POS hardware standard | Desktop only |
| **jSerialComm** | 2.10.4 | Serial communication | Desktop only |

### 2.3 Build Tools

| Tool | Version | Purpose |
|------|---------|---------|
| **Gradle** | 8.5+ | Build system |
| **Gradle Kotlin DSL** | - | Build script language |
| **Compose Gradle Plugin** | 1.6.0+ | Compose compilation |
| **Android Gradle Plugin** | 8.2.0+ | Android builds |
| **Conveyor** | Latest | Desktop packaging & updates |
| **ProGuard/R8** | - | Code shrinking (Android) |

---

## 3. Project Architecture

### 3.1 Module Structure

```
growpos-multiplatform/
├── gradle/
│   └── wrapper/
├── build.gradle.kts                    # Root build configuration
├── settings.gradle.kts                 # Module definitions
│
├── shared/                             # Shared code module
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/                 # Code shared across ALL platforms
│       │   └── kotlin/
│       │       └── com/unisight/growpos/
│       │           ├── App.kt                    # Main app composable
│       │           ├── di/                       # Dependency injection
│       │           │   ├── CommonModule.kt
│       │           │   └── PlatformModule.kt     # expect declarations
│       │           ├── domain/                   # Business logic
│       │           │   ├── model/                # Domain models
│       │           │   ├── usecase/              # Use cases
│       │           │   └── repository/           # Repository interfaces
│       │           ├── data/                     # Data layer
│       │           │   ├── local/                # Local database
│       │           │   ├── remote/               # API clients
│       │           │   └── repository/           # Repository implementations
│       │           ├── presentation/             # UI layer
│       │           │   ├── screens/              # Screen composables
│       │           │   │   ├── home/
│       │           │   │   ├── transaction/
│       │           │   │   ├── payment/
│       │           │   │   ├── returns/
│       │           │   │   ├── reports/
│       │           │   │   └── settings/
│       │           │   ├── components/           # Reusable UI components
│       │           │   │   ├── buttons/
│       │           │   │   ├── dialogs/
│       │           │   │   ├── inputs/
│       │           │   │   └── cards/
│       │           │   ├── theme/                # App theming
│       │           │   └── navigation/           # Navigation setup
│       │           ├── service/                  # Business services
│       │           │   ├── calculation/          # Price calculations
│       │           │   │   ├── RetailPriceCalculator.kt
│       │           │   │   ├── TaxCalculator.kt
│       │           │   │   ├── DiscountCalculator.kt
│       │           │   │   └── CrvCalculator.kt
│       │           │   ├── payment/              # Payment processing
│       │           │   └── sync/                 # Data synchronization
│       │           ├── hardware/                 # Hardware abstractions
│       │           │   ├── PrinterService.kt     # expect class
│       │           │   ├── ScannerService.kt     # expect class
│       │           │   ├── ScaleService.kt       # expect class
│       │           │   ├── PaymentTerminal.kt    # expect class
│       │           │   └── CashDrawer.kt         # expect class
│       │           └── util/                     # Utilities
│       │
│       ├── commonTest/                 # Shared tests
│       │
│       ├── desktopMain/                # Desktop-specific (Windows/Linux/macOS)
│       │   └── kotlin/
│       │       └── com/unisight/growpos/
│       │           ├── di/
│       │           │   └── PlatformModule.desktop.kt
│       │           ├── hardware/
│       │           │   ├── PrinterService.desktop.kt      # JavaPOS impl
│       │           │   ├── ScannerService.desktop.kt      # JavaPOS impl
│       │           │   ├── ScaleService.desktop.kt        # JavaPOS impl
│       │           │   ├── PaymentTerminal.desktop.kt     # PAX PosLink
│       │           │   └── CashDrawer.desktop.kt
│       │           └── platform/
│       │               └── DesktopPlatform.kt
│       │
│       └── androidMain/                # Android-specific
│           └── kotlin/
│               └── com/unisight/growpos/
│                   ├── di/
│                   │   └── PlatformModule.android.kt
│                   ├── hardware/
│                   │   ├── PrinterService.android.kt      # Sunmi/PAX SDK
│                   │   ├── ScannerService.android.kt      # Camera/Hardware
│                   │   ├── ScaleService.android.kt        # Bluetooth scale
│                   │   ├── PaymentTerminal.android.kt     # Integrated
│                   │   └── CashDrawer.android.kt          # May not exist
│                   └── platform/
│                       └── AndroidPlatform.kt
│
├── desktopApp/                         # Desktop application entry point
│   ├── build.gradle.kts
│   └── src/
│       └── main/
│           └── kotlin/
│               └── Main.kt              # Desktop main()
│
├── androidApp/                         # Android application entry point
│   ├── build.gradle.kts
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           └── kotlin/
│               └── MainActivity.kt      # Android entry point
│
└── hardware-legacy/                    # Legacy Java hardware code (reused)
    ├── build.gradle.kts
    └── src/main/java/                  # Existing JavaPOS code
        └── com/hardware/
            ├── epson/
            ├── pax/
            ├── datalogic/
            └── cas/
```

### 3.2 Dependency Graph

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         MODULE DEPENDENCY GRAPH                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│                              ┌───────────────────┐                          │
│                              │    desktopApp     │                          │
│                              │   (Desktop Entry) │                          │
│                              └─────────┬─────────┘                          │
│                                        │                                     │
│                                        │ depends on                          │
│                                        ▼                                     │
│    ┌───────────────────┐      ┌───────────────────┐      ┌────────────────┐ │
│    │    androidApp     │      │      shared       │      │hardware-legacy │ │
│    │ (Android Entry)   │─────▶│   (Multiplatform) │◀─────│  (Java/JVM)    │ │
│    └───────────────────┘      └───────────────────┘      └────────────────┘ │
│                                        │                         │           │
│                                        │                         │           │
│                               ┌────────┴────────┐               │           │
│                               │                 │               │           │
│                               ▼                 ▼               │           │
│                        ┌───────────┐     ┌───────────┐          │           │
│                        │commonMain │     │desktopMain│──────────┘           │
│                        │ (Shared)  │     │  (JVM)    │                      │
│                        └───────────┘     └───────────┘                      │
│                               │                                              │
│                               ▼                                              │
│                        ┌───────────┐                                        │
│                        │androidMain│                                        │
│                        │ (Android) │                                        │
│                        └───────────┘                                        │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Code Sharing Strategy

### 4.1 Sharing Breakdown

| Layer | Shared % | Platform-Specific |
|-------|----------|-------------------|
| **Domain Models** | 100% | None |
| **Business Logic** | 100% | None |
| **Calculation Engine** | 100% | None |
| **Repository Interfaces** | 100% | None |
| **UI Components** | 95% | Minor platform tweaks |
| **Screens/Views** | 90% | Platform-specific adaptations |
| **Navigation** | 90% | Entry points differ |
| **Database Schema** | 100% | Driver implementations differ |
| **Network/API** | 100% | None (Ktor is multiplatform) |
| **Hardware Abstractions** | 0% | 100% platform-specific |
| **Platform Utilities** | 0% | 100% platform-specific |

### 4.2 Expect/Actual Pattern

The `expect`/`actual` mechanism allows defining platform-agnostic interfaces with platform-specific implementations:

```kotlin
// ═══════════════════════════════════════════════════════════════════════════
// commonMain/kotlin/com/unisight/growpos/hardware/PrinterService.kt
// ═══════════════════════════════════════════════════════════════════════════

expect class PrinterService {
    suspend fun initialize(): Result<Unit>
    suspend fun printReceipt(receipt: Receipt): Result<Unit>
    suspend fun printReport(report: Report): Result<Unit>
    suspend fun openCashDrawer(): Result<Unit>
    suspend fun checkPaperStatus(): PaperStatus
    fun isConnected(): Boolean
}

enum class PaperStatus {
    OK, LOW, EMPTY, UNKNOWN
}

// ═══════════════════════════════════════════════════════════════════════════
// desktopMain/kotlin/com/unisight/growpos/hardware/PrinterService.desktop.kt
// ═══════════════════════════════════════════════════════════════════════════

actual class PrinterService {
    private val epsonPrinter = MPOSPrinter()  // Reuse existing Java code
    
    actual suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            epsonPrinter.initialize()
        }
    }
    
    actual suspend fun printReceipt(receipt: Receipt): Result<Unit> = 
        withContext(Dispatchers.IO) {
            runCatching {
                epsonPrinter.print(receipt.toJson())
            }
        }
    
    actual suspend fun printReport(report: Report): Result<Unit> = 
        withContext(Dispatchers.IO) {
            runCatching {
                epsonPrinter.printReport(report.toJson())
            }
        }
    
    actual suspend fun openCashDrawer(): Result<Unit> = 
        withContext(Dispatchers.IO) {
            runCatching {
                epsonPrinter.openDrawer()
            }
        }
    
    actual suspend fun checkPaperStatus(): PaperStatus {
        return when (epsonPrinter.getPaperStatus()) {
            0 -> PaperStatus.OK
            1 -> PaperStatus.LOW
            2 -> PaperStatus.EMPTY
            else -> PaperStatus.UNKNOWN
        }
    }
    
    actual fun isConnected(): Boolean = epsonPrinter.isConnected()
}

// ═══════════════════════════════════════════════════════════════════════════
// androidMain/kotlin/com/unisight/growpos/hardware/PrinterService.android.kt
// ═══════════════════════════════════════════════════════════════════════════

actual class PrinterService(private val context: Context) {
    private val sunmiPrinter = SunmiPrintHelper.getInstance()
    
    actual suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            sunmiPrinter.initSunmiPrinterService(context)
        }
    }
    
    actual suspend fun printReceipt(receipt: Receipt): Result<Unit> = 
        withContext(Dispatchers.IO) {
            runCatching {
                sunmiPrinter.printText(receipt.format())
                sunmiPrinter.feedPaper()
                sunmiPrinter.cutPaper()
            }
        }
    
    actual suspend fun printReport(report: Report): Result<Unit> = 
        withContext(Dispatchers.IO) {
            runCatching {
                sunmiPrinter.printText(report.format())
                sunmiPrinter.feedPaper()
                sunmiPrinter.cutPaper()
            }
        }
    
    actual suspend fun openCashDrawer(): Result<Unit> = 
        withContext(Dispatchers.IO) {
            runCatching {
                sunmiPrinter.openDrawer()
            }
        }
    
    actual suspend fun checkPaperStatus(): PaperStatus {
        return when (sunmiPrinter.paperStatus) {
            SunmiPrinter.PAPER_OK -> PaperStatus.OK
            SunmiPrinter.PAPER_NEAR_END -> PaperStatus.LOW
            SunmiPrinter.PAPER_END -> PaperStatus.EMPTY
            else -> PaperStatus.UNKNOWN
        }
    }
    
    actual fun isConnected(): Boolean = sunmiPrinter.isServiceConnected
}
```

### 4.3 Shared UI Example

```kotlin
// ═══════════════════════════════════════════════════════════════════════════
// commonMain - Shared across ALL platforms
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun TransactionScreen(
    viewModel: TransactionViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        TransactionHeader(
            cashier = state.cashier,
            transactionNumber = state.transactionNumber
        )
        
        // Main content - responsive layout
        Row(modifier = Modifier.weight(1f)) {
            // Product list - takes 2/3 on desktop, full on mobile
            TransactionItemList(
                items = state.items,
                onItemClick = viewModel::onItemClick,
                onQuantityChange = viewModel::onQuantityChange,
                onRemoveItem = viewModel::onRemoveItem,
                modifier = Modifier.weight(2f)
            )
            
            // Summary panel - visible on desktop, slide-up on mobile
            if (LocalWindowInfo.current.isDesktop) {
                TransactionSummary(
                    subtotal = state.subtotal,
                    tax = state.tax,
                    total = state.total,
                    onPayClick = viewModel::onPayClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        // Bottom bar for mobile
        if (!LocalWindowInfo.current.isDesktop) {
            MobileBottomBar(
                total = state.total,
                onPayClick = viewModel::onPayClick
            )
        }
    }
}

@Composable
fun TransactionItemList(
    items: List<TransactionItem>,
    onItemClick: (TransactionItem) -> Unit,
    onQuantityChange: (TransactionItem, Int) -> Unit,
    onRemoveItem: (TransactionItem) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxHeight(),
        contentPadding = PaddingValues(8.dp)
    ) {
        items(
            items = items,
            key = { it.id }
        ) { item ->
            TransactionItemCard(
                item = item,
                onClick = { onItemClick(item) },
                onQuantityChange = { onQuantityChange(item, it) },
                onRemove = { onRemoveItem(item) }
            )
        }
    }
}
```

---

## 5. Hardware Integration Approach

### 5.1 Desktop Hardware (Windows/Linux)

#### Reusing Existing Java Code

The existing `hardware` module can be used directly in the desktop application:

```kotlin
// desktopMain/build.gradle.kts
kotlin {
    jvm("desktop")
    
    sourceSets {
        val desktopMain by getting {
            dependencies {
                // Reuse existing hardware module
                implementation(project(":hardware-legacy"))
                
                // Serial port communication
                implementation("com.fazecast:jSerialComm:2.10.4")
                
                // USB access
                implementation("org.usb4java:usb4java-javax:1.2.0")
            }
        }
    }
}
```

#### Hardware Service Mapping

| Hardware | Current Java Class | Kotlin Wrapper |
|----------|-------------------|----------------|
| Receipt Printer | `MPOSPrinter` | `PrinterService.desktop.kt` |
| Cash Drawer | `MPOSPrinter` (integrated) | `CashDrawer.desktop.kt` |
| Barcode Scanner | `POSScanner` | `ScannerService.desktop.kt` |
| Scale | `POSScale`, `CasScaleReader` | `ScaleService.desktop.kt` |
| Payment Terminal | `PaymentController` | `PaymentTerminal.desktop.kt` |

### 5.2 Android Hardware

#### Android POS Device SDKs

```kotlin
// androidMain/build.gradle.kts
android {
    // ...
}

dependencies {
    // Sunmi devices
    implementation("com.sunmi:printerlibrary:1.0.18")
    
    // PAX Android devices
    implementation(files("libs/pax-android-sdk.aar"))
    
    // Generic Bluetooth printing
    implementation("com.github.AnyChart:escpos-android:1.0.1")
}
```

#### Android Hardware Service Example

```kotlin
// androidMain/kotlin/.../hardware/ScannerService.android.kt

actual class ScannerService(private val context: Context) {
    
    private var cameraScanner: CameraScanner? = null
    private var hardwareScanner: HardwareScanner? = null
    
    actual suspend fun initialize(): Result<Unit> = runCatching {
        // Check if device has built-in scanner (Sunmi, PAX, etc.)
        if (hasBuiltInScanner()) {
            hardwareScanner = HardwareScanner(context)
            hardwareScanner?.initialize()
        } else {
            // Fall back to camera scanning
            cameraScanner = CameraScanner(context)
        }
    }
    
    actual fun startScanning(onBarcode: (String) -> Unit) {
        hardwareScanner?.setCallback { barcode ->
            onBarcode(barcode)
        } ?: run {
            cameraScanner?.startScanning(onBarcode)
        }
    }
    
    actual fun stopScanning() {
        hardwareScanner?.stopScanning()
        cameraScanner?.stopScanning()
    }
    
    private fun hasBuiltInScanner(): Boolean {
        return Build.MANUFACTURER.equals("SUNMI", ignoreCase = true) ||
               Build.MANUFACTURER.equals("PAX", ignoreCase = true)
    }
}
```

### 5.3 Hardware Abstraction Summary

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         HARDWARE ABSTRACTION LAYER                           │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │                     COMMON INTERFACE (expect)                         │   │
│  │                                                                       │   │
│  │   PrinterService    ScannerService    ScaleService    PaymentTerminal │   │
│  │        │                  │                │                │         │   │
│  └────────┼──────────────────┼────────────────┼────────────────┼─────────┘   │
│           │                  │                │                │             │
│     ┌─────┴─────┐      ┌─────┴─────┐    ┌─────┴─────┐    ┌─────┴─────┐      │
│     ▼           ▼      ▼           ▼    ▼           ▼    ▼           ▼      │
│  ┌──────┐  ┌────────┐ ┌──────┐ ┌──────┐┌──────┐┌──────┐┌──────┐┌──────┐    │
│  │Desktop│  │Android │ │Desktop│ │Android││Desktop││Android││Desktop││Android│   │
│  │      │  │        │ │      │ │      ││      ││      ││      ││      │    │
│  │JavaPOS│  │Sunmi   │ │JavaPOS│ │Camera ││JavaPOS││BT/USB││PAX    ││PAX   │    │
│  │Epson │  │PAX     │ │Datalog│ │HW Scan││CAS   ││Scale ││PosLink││Android│   │
│  └──────┘  └────────┘ └──────┘ └──────┘└──────┘└──────┘└──────┘└──────┘    │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 6. Build Phases

### Phase Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           MIGRATION TIMELINE                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  Phase 1        Phase 2        Phase 3        Phase 4        Phase 5       │
│  Foundation     Core Logic     Desktop UI     Android        Polish         │
│  ─────────      ──────────     ──────────     ───────        ──────         │
│                                                                              │
│  ████████       ████████████   ████████████   ████████████   ████████       │
│  2 weeks        4-6 weeks      4-6 weeks      3-4 weeks      2-3 weeks      │
│                                                                              │
│  Total Estimated Duration: 15-21 weeks (4-5 months)                         │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Phase 1: Foundation (2 weeks)

**Objectives:**
- Set up multiplatform project structure
- Configure build system
- Establish CI/CD pipeline
- Create base architecture

**Deliverables:**

| Task | Duration | Details |
|------|----------|---------|
| Project setup | 2 days | Gradle KMP configuration |
| Build configuration | 2 days | Desktop + Android builds |
| DI setup (Koin) | 1 day | Multiplatform modules |
| Navigation setup | 1 day | Voyager or Decompose |
| Theme/styling | 2 days | Material 3 theming |
| CI/CD pipeline | 2 days | GitHub Actions |

**Gradle Configuration:**

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "GrowPOS"
include(":shared")
include(":desktopApp")
include(":androidApp")
include(":hardware-legacy")
```

```kotlin
// shared/build.gradle.kts
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }
    
    jvm("desktop")
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.json)
                implementation(libs.koin.core)
                implementation(libs.koin.compose)
                implementation(libs.voyager.navigator)
                implementation(libs.sqldelight.runtime)
            }
        }
        
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(project(":hardware-legacy"))
                implementation(libs.ktor.client.cio)
                implementation(libs.sqldelight.driver.jvm)
                implementation(libs.kotlinx.coroutines.swing)
            }
        }
        
        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.activity.compose)
                implementation(libs.ktor.client.android)
                implementation(libs.sqldelight.driver.android)
                implementation(libs.koin.android)
            }
        }
    }
}
```

### Phase 2: Core Business Logic (4-6 weeks)

**Objectives:**
- Migrate domain models to Kotlin
- Convert calculation engine
- Implement repositories
- Set up database layer

**Migration Priority:**

| Priority | Component | Complexity | Dependencies |
|----------|-----------|------------|--------------|
| 1 | Domain Models | Low | None |
| 2 | Price Calculators | Medium | Models |
| 3 | Tax Calculator | Medium | Models |
| 4 | Discount Calculator | High | Models, Tax |
| 5 | Payment Service | High | All calculators |
| 6 | Database Layer | Medium | Models |
| 7 | API Client | Low | Models |
| 8 | Sync Service | High | API, Database |

**Example Model Migration:**

```kotlin
// Before (Java)
public class TransactionItem {
    private String id;
    private String productId;
    private String name;
    private BigDecimal unitPrice;
    private BigDecimal quantity;
    private BigDecimal lineTotal;
    private BigDecimal taxAmount;
    private Boolean isFoodStampable;
    // ... getters, setters, etc.
}

// After (Kotlin)
@Serializable
data class TransactionItem(
    val id: String = UUID.randomUUID().toString(),
    val productId: String,
    val name: String,
    val unitPrice: BigDecimal,
    val quantity: BigDecimal = BigDecimal.ONE,
    val discounts: List<AppliedDiscount> = emptyList(),
    val isFoodStampable: Boolean = false,
    val isWicEligible: Boolean = false,
    val taxRates: List<TaxRate> = emptyList(),
    val crvAmount: BigDecimal = BigDecimal.ZERO
) {
    val lineSubtotal: BigDecimal
        get() = unitPrice * quantity
    
    val totalDiscounts: BigDecimal
        get() = discounts.sumOf { it.amount }
    
    val discountedPrice: BigDecimal
        get() = lineSubtotal - totalDiscounts
    
    val taxAmount: BigDecimal
        get() = taxRates.sumOf { rate ->
            (discountedPrice * rate.percentage / BigDecimal(100))
                .setScale(2, RoundingMode.HALF_UP)
        }
    
    val lineTotal: BigDecimal
        get() = discountedPrice + taxAmount + (crvAmount * quantity)
}
```

### Phase 3: Desktop UI (4-6 weeks)

**Objectives:**
- Implement all screens in Compose
- Integrate desktop hardware
- Multi-window support (customer display)
- Keyboard shortcuts

**Screen Migration Order:**

| Order | Screen | Complexity | Priority |
|-------|--------|------------|----------|
| 1 | Login/Lock | Low | Critical |
| 2 | Home/Transaction | High | Critical |
| 3 | Payment | High | Critical |
| 4 | Product Lookup | Medium | High |
| 5 | Returns | High | High |
| 6 | Reports | Medium | Medium |
| 7 | Settings | Low | Medium |
| 8 | Customer Display | Medium | Medium |

**Desktop Entry Point:**

```kotlin
// desktopApp/src/main/kotlin/Main.kt

fun main() = application {
    // Initialize DI
    val koinApp = startKoin {
        modules(
            commonModule,
            desktopModule,
            hardwareModule
        )
    }
    
    // Main POS window
    Window(
        onCloseRequest = ::exitApplication,
        title = "GrowPOS",
        state = rememberWindowState(
            width = 1280.dp,
            height = 800.dp,
            position = WindowPosition(Alignment.Center)
        )
    ) {
        GrowPOSTheme {
            App()
        }
    }
    
    // Customer display window (secondary monitor)
    val screens = java.awt.GraphicsEnvironment
        .getLocalGraphicsEnvironment()
        .screenDevices
    
    if (screens.size > 1) {
        Window(
            onCloseRequest = { },
            title = "Customer Display",
            undecorated = true,
            state = rememberWindowState(
                placement = WindowPlacement.Fullscreen
            )
        ) {
            GrowPOSTheme {
                CustomerDisplayScreen()
            }
        }
    }
}
```

### Phase 4: Android Implementation (3-4 weeks)

**Objectives:**
- Create Android app module
- Implement Android hardware services
- Test on POS devices (Sunmi, PAX)
- Handle touch-first UX

**Android-Specific Adaptations:**

| Feature | Desktop | Android |
|---------|---------|---------|
| Input | Keyboard + Mouse | Touch + Hardware buttons |
| Scanner | USB/Serial | Built-in or Camera |
| Printer | Network/USB | AIDL Service |
| Payment | External terminal | Integrated |
| Screen | Multi-window | Single window |
| Navigation | Keyboard shortcuts | Gestures + buttons |

**Android Entry Point:**

```kotlin
// androidApp/src/main/kotlin/MainActivity.kt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Koin
        startKoin {
            androidContext(this@MainActivity)
            modules(
                commonModule,
                androidModule,
                androidHardwareModule
            )
        }
        
        setContent {
            GrowPOSTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    App()
                }
            }
        }
    }
}
```

### Phase 5: Polish & Deployment (2-3 weeks)

**Objectives:**
- End-to-end testing
- Performance optimization
- Create installers/packages
- Set up auto-update system
- Documentation

**Packaging:**

| Platform | Format | Tool |
|----------|--------|------|
| Windows | MSI, EXE | Conveyor / JPackage |
| Linux | DEB, RPM, AppImage | Conveyor / JPackage |
| Android | APK, AAB | Gradle |

---

## 7. Platform-Specific Guidelines

### 7.1 Windows

**Installation Location:**
```
C:\Program Files\GrowPOS\
├── GrowPOS.exe
├── runtime\                 # Bundled JRE
├── lib\                     # Application JARs
├── hardware\                # Hardware drivers
│   ├── javapos.xml
│   └── epson\
└── data\                    # Local database
```

**Registry & Shortcuts:**
- Start menu entry
- Desktop shortcut (optional)
- Auto-start on boot (optional)
- File associations (none required)

**Windows Service (Optional):**
```batch
sc create GrowPOS binPath= "C:\Program Files\GrowPOS\GrowPOS.exe --service"
```

### 7.2 Linux

**Installation Location:**
```
/opt/growpos/
├── bin/
│   └── growpos
├── lib/
├── hardware/
└── share/
    ├── applications/
    │   └── growpos.desktop
    └── icons/

/var/lib/growpos/           # Data directory
~/.config/growpos/          # User config
```

**Permissions Setup:**
```bash
# Add user to dialout group for serial port access
sudo usermod -a -G dialout $USER

# Set permissions on hardware devices
sudo chmod 666 /dev/ttyUSB*
```

**Systemd Service:**
```ini
[Unit]
Description=GrowPOS Point of Sale
After=network.target

[Service]
Type=simple
User=pos
Environment=DISPLAY=:0
ExecStart=/opt/growpos/bin/growpos
Restart=always

[Install]
WantedBy=multi-user.target
```

### 7.3 Android

**Permissions Required:**
```xml
<manifest>
    <!-- Hardware access -->
    <uses-permission android:name="android.permission.BLUETOOTH" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
    <uses-permission android:name="android.permission.USB_PERMISSION" />
    
    <!-- Camera for barcode scanning -->
    <uses-permission android:name="android.permission.CAMERA" />
    
    <!-- Network for sync -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    
    <!-- Kiosk mode -->
    <uses-permission android:name="android.permission.REORDER_TASKS" />
    
    <!-- Features -->
    <uses-feature android:name="android.hardware.camera" android:required="false" />
    <uses-feature android:name="android.hardware.usb.host" android:required="false" />
</manifest>
```

**Kiosk Mode (Lock Task):**
```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable kiosk mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            startLockTask()
        }
    }
}
```

---

## 8. Build & Deployment

### 8.1 Build Commands

```bash
# Build all platforms
./gradlew build

# Desktop only
./gradlew :desktopApp:run              # Run
./gradlew :desktopApp:packageMsi       # Windows installer
./gradlew :desktopApp:packageDeb       # Debian package
./gradlew :desktopApp:packageRpm       # RPM package

# Android
./gradlew :androidApp:assembleDebug    # Debug APK
./gradlew :androidApp:assembleRelease  # Release APK
./gradlew :androidApp:bundleRelease    # AAB for Play Store
```

### 8.2 Conveyor Configuration (Desktop Auto-Updates)

```hocon
// conveyor.conf
include required("/stdlib/jdk/17/openjdk.conf")

app {
    display-name = "GrowPOS"
    fsname = growpos
    version = "2.0.0"
    
    site {
        base-url = "https://updates.growpos.com/"
    }
    
    windows {
        inputs += app/build/compose/binaries/main/app/windows/
        
        installer {
            type = msi
        }
    }
    
    linux {
        inputs += app/build/compose/binaries/main/app/linux/
    }
    
    mac {
        inputs += app/build/compose/binaries/main/app/macos/
        
        info-plist {
            LSUIElement = true
        }
    }
}
```

### 8.3 CI/CD Pipeline (GitHub Actions)

```yaml
name: Build & Release

on:
  push:
    tags:
      - 'v*'

jobs:
  build-desktop:
    strategy:
      matrix:
        os: [ubuntu-latest, windows-latest]
    runs-on: ${{ matrix.os }}
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      
      - name: Build with Gradle
        run: ./gradlew :desktopApp:packageDistributionForCurrentOS
      
      - name: Upload artifacts
        uses: actions/upload-artifact@v4
        with:
          name: desktop-${{ matrix.os }}
          path: desktopApp/build/compose/binaries/main/

  build-android:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Build Release APK
        run: ./gradlew :androidApp:assembleRelease
      
      - name: Upload APK
        uses: actions/upload-artifact@v4
        with:
          name: android-release
          path: androidApp/build/outputs/apk/release/
```

---

## 9. Testing Strategy

### 9.1 Test Types

| Test Type | Location | Tools | Coverage Target |
|-----------|----------|-------|-----------------|
| Unit Tests | `commonTest` | kotlin.test | 80%+ business logic |
| Integration | `desktopTest`, `androidTest` | JUnit, Espresso | Critical paths |
| UI Tests | Platform-specific | Compose Testing | Key screens |
| Hardware | Manual + Mocks | Custom mocks | All peripherals |
| E2E | Manual | Checklist | Full transactions |

### 9.2 Shared Tests

```kotlin
// shared/src/commonTest/kotlin/CalculatorTests.kt

class TaxCalculatorTest {
    private val calculator = TaxCalculator()
    
    @Test
    fun `should calculate single tax rate correctly`() {
        val item = TransactionItem(
            productId = "123",
            name = "Test Item",
            unitPrice = BigDecimal("10.00"),
            taxRates = listOf(TaxRate("CA", BigDecimal("9.5")))
        )
        
        assertEquals(BigDecimal("0.95"), calculator.calculateTax(item))
    }
    
    @Test
    fun `should exempt SNAP items from tax`() {
        val item = TransactionItem(
            productId = "123",
            name = "Food Item",
            unitPrice = BigDecimal("10.00"),
            isFoodStampable = true,
            taxRates = listOf(TaxRate("CA", BigDecimal("9.5")))
        )
        
        val snapPayment = Payment(type = PaymentType.EBT_SNAP, amount = BigDecimal("10.00"))
        
        assertEquals(BigDecimal.ZERO, calculator.calculateTax(item, listOf(snapPayment)))
    }
}
```

### 9.3 Hardware Mocking

```kotlin
// desktopTest/kotlin/MockPrinterService.kt

class MockPrinterService : PrinterService {
    val printedReceipts = mutableListOf<Receipt>()
    var isConnected = true
    var shouldFail = false
    
    override suspend fun printReceipt(receipt: Receipt): Result<Unit> {
        return if (shouldFail) {
            Result.failure(PrinterException("Mock failure"))
        } else {
            printedReceipts.add(receipt)
            Result.success(Unit)
        }
    }
    
    // ... other mock implementations
}
```

---

## 10. Risk Assessment & Mitigation

### 10.1 Risk Matrix

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| JavaPOS compatibility issues | Low | High | Test early, keep Java module separate |
| Compose Desktop bugs | Medium | Medium | Pin to stable version, avoid bleeding edge |
| Android POS device fragmentation | Medium | Medium | Test on target devices early |
| Learning curve for team | Medium | Medium | Training, gradual migration |
| Performance regression | Low | High | Benchmark critical paths |
| Third-party library issues | Low | Medium | Evaluate alternatives, vendor lock-in assessment |

### 10.2 Rollback Strategy

1. **Keep legacy system running** during migration
2. **Feature flags** for gradual rollout
3. **Database compatibility** between old and new systems
4. **Parallel deployment** option for critical locations

### 10.3 Success Criteria

| Metric | Target |
|--------|--------|
| Build time | < 3 minutes (incremental) |
| App startup | < 2 seconds |
| Memory usage | < 400MB (desktop), < 200MB (Android) |
| Transaction speed | Same or better than current |
| Code coverage | > 70% business logic |
| Crash rate | < 0.1% of sessions |

---

## Appendices

### A. Useful Resources

- [Compose Multiplatform Documentation](https://www.jetbrains.com/lp/compose-multiplatform/)
- [Kotlin Multiplatform Documentation](https://kotlinlang.org/docs/multiplatform.html)
- [SQLDelight Documentation](https://cashapp.github.io/sqldelight/)
- [Ktor Client Documentation](https://ktor.io/docs/client.html)
- [Koin Documentation](https://insert-koin.io/)

### B. Sample Project Structure Repository

A reference implementation will be available at: `github.com/unisight/growpos-kmp-template`

### C. Related Documentation

- [Current Architecture](../../ARCHITECTURE.md)
- [Hardware Integration](../../modules/hardware/README.md)
- [Calculation Engine](../../features/advanced-calculations/CALCULATION_ENGINE.md)

---

**Document History:**

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | January 2026 | Architecture Team | Initial document |

