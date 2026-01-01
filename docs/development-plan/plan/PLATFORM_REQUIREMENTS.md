# 🖥️ Platform Requirements & Configuration

Everything you need to know about Windows, Linux, and Android — your target platforms for the new GroPOS.

---

## Table of Contents

1. [Windows Desktop](#1-windows-desktop)
2. [Linux Desktop](#2-linux-desktop)
3. [Android POS Devices](#3-android-pos-devices)
4. [Cross-Platform Considerations](#4-cross-platform-considerations)

---

## 1. Windows Desktop

### 1.1 System Requirements

#### Minimum Requirements

| Component | Requirement |
|-----------|-------------|
| **Operating System** | Windows 10 version 1809 or later |
| **Architecture** | x64 (AMD64) |
| **Processor** | 2 GHz dual-core |
| **RAM** | 4 GB |
| **Storage** | 500 MB for application + data |
| **Display** | 1024x768 minimum |
| **Java Runtime** | Bundled (JDK 21) |

#### Recommended Requirements

| Component | Requirement |
|-----------|-------------|
| **Operating System** | Windows 11 |
| **Architecture** | x64 (AMD64) |
| **Processor** | 2.5 GHz quad-core |
| **RAM** | 8 GB |
| **Storage** | 1 GB SSD |
| **Display** | 1920x1080 or higher |
| **Secondary Display** | For customer screen |

### 1.2 Supported Windows Versions

| Version | Support Level | Notes |
|---------|---------------|-------|
| Windows 11 23H2 | ✅ Full | Recommended |
| Windows 11 22H2 | ✅ Full | Supported |
| Windows 11 21H2 | ✅ Full | Supported |
| Windows 10 22H2 | ✅ Full | LTS, supported until 2025 |
| Windows 10 21H2 | ✅ Full | Supported |
| Windows 10 1809 | ⚠️ Limited | Minimum version, EOL approaching |
| Windows Server 2022 | ✅ Full | For terminal server deployments |
| Windows Server 2019 | ✅ Full | Supported |

### 1.3 Hardware Peripherals (Windows)

#### Receipt Printers

| Manufacturer | Models | Driver | Connection |
|--------------|--------|--------|------------|
| **Epson** | TM-T88VI, TM-T88V, TM-T20III | Epson OPOS/JavaPOS | USB, Network |
| **Star** | TSP143, TSP654 | Star JavaPOS | USB, Network |
| **Bixolon** | SRP-350plusIII | Bixolon JavaPOS | USB |

**Configuration:**
```xml
<!-- javapos.xml -->
<JposEntries>
    <JposEntry logicalName="POSPrinter">
        <prop name="deviceCategory" value="POSPrinter"/>
        <prop name="jposServiceInstanceFactory" 
              value="jp.co.epson.upos.UPOSServiceInstanceFactory"/>
        <prop name="productUrl" value="http://www.epson-pos.com"/>
        <prop name="productName" value="TM-T88VI"/>
        <prop name="deviceBus" value="USB"/>
    </JposEntry>
</JposEntries>
```

#### Barcode Scanners

| Manufacturer | Models | Interface | Mode |
|--------------|--------|-----------|------|
| **Datalogic** | Magellan 9800i, 9600i | USB-HID / Serial | Presentation |
| **Honeywell** | Voyager 1470g, Xenon 1950g | USB-HID | Hand-held |
| **Zebra** | DS9908, DS2208 | USB-HID | Both |

**Configuration:**
- USB-HID mode: Keyboard wedge, no driver required
- Serial mode: JavaPOS driver required
- Recommended: USB-HID for simplicity

#### Scales

| Manufacturer | Models | Interface | Protocol |
|--------------|--------|-----------|----------|
| **Datalogic** | Magellan 9800i (integrated) | JavaPOS | SASI |
| **CAS** | PD-II, SW-1 | RS-232 | CAS Protocol |
| **Mettler Toledo** | Ariva-S | RS-232/USB | MT-SICS |

**Serial Port Configuration:**
```kotlin
// CAS Scale settings
val serialSettings = SerialPortSettings(
    baudRate = 9600,
    dataBits = 8,
    stopBits = 1,
    parity = Parity.NONE,
    flowControl = FlowControl.NONE
)
```

#### Payment Terminals

| Manufacturer | Models | SDK | Connection |
|--------------|--------|-----|------------|
| **PAX** | S300, PX7 | PosLink | USB/TCP/IP |
| **Verifone** | Vx520, Vx820 | Verifone SDK | USB/TCP/IP |
| **Ingenico** | Lane 3000, Desk 3500 | Tetra SDK | USB/TCP/IP |

**PAX Configuration:**
```json
{
  "CommType": "USB",
  "DestIP": "",
  "DestPort": "",
  "Timeout": 60000,
  "SerialPort": "",
  "BaudRate": 115200
}
```

### 1.4 Windows Installation

#### MSI Installer

```powershell
# Silent installation
msiexec /i GroPOS-2.0.0.msi /quiet /norestart

# Installation with custom path
msiexec /i GroPOS-2.0.0.msi INSTALLDIR="D:\POS\GroPOS" /quiet

# Uninstallation
msiexec /x GroPOS-2.0.0.msi /quiet
```

#### Auto-Start Configuration

**Registry Entry:**
```reg
Windows Registry Editor Version 5.00

[HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows\CurrentVersion\Run]
"GroPOS"="\"C:\\Program Files\\GroPOS\\GroPOS.exe\" --minimized"
```

**Task Scheduler (Recommended):**
```xml
<?xml version="1.0" encoding="UTF-16"?>
<Task version="1.4">
  <Triggers>
    <LogonTrigger>
      <Enabled>true</Enabled>
    </LogonTrigger>
  </Triggers>
  <Actions>
    <Exec>
      <Command>C:\Program Files\GroPOS\GroPOS.exe</Command>
    </Exec>
  </Actions>
  <Settings>
    <DisallowStartIfOnBatteries>false</DisallowStartIfOnBatteries>
    <StopIfGoingOnBatteries>false</StopIfGoingOnBatteries>
    <RunOnlyIfNetworkAvailable>false</RunOnlyIfNetworkAvailable>
    <RestartOnFailure>
      <Interval>PT1M</Interval>
      <Count>3</Count>
    </RestartOnFailure>
  </Settings>
</Task>
```

### 1.5 Windows Firewall Rules

```powershell
# Allow GroPOS through firewall
New-NetFirewallRule -DisplayName "GroPOS" -Direction Inbound -Program "C:\Program Files\GroPOS\GroPOS.exe" -Action Allow

# Allow PAX terminal communication
New-NetFirewallRule -DisplayName "PAX Terminal" -Direction Inbound -LocalPort 10009 -Protocol TCP -Action Allow
```

---

## 2. Linux Desktop

### 2.1 System Requirements

#### Minimum Requirements

| Component | Requirement |
|-----------|-------------|
| **Kernel** | Linux 4.15+ |
| **Architecture** | x86_64, ARM64 |
| **Processor** | 2 GHz dual-core |
| **RAM** | 4 GB |
| **Storage** | 500 MB |
| **Display Server** | X11 or Wayland |
| **Desktop** | GTK3 compatible |

#### Recommended Requirements

| Component | Requirement |
|-----------|-------------|
| **Kernel** | Linux 5.15+ (LTS) |
| **Architecture** | x86_64 |
| **Processor** | 2.5 GHz quad-core |
| **RAM** | 8 GB |
| **Storage** | 1 GB SSD |
| **Display** | 1920x1080 |
| **Desktop** | GNOME 40+ or KDE Plasma 5.24+ |

### 2.2 Supported Distributions

| Distribution | Versions | Support Level | Package Format |
|--------------|----------|---------------|----------------|
| **Ubuntu** | 22.04 LTS, 24.04 LTS | ✅ Full | DEB |
| **Debian** | 11 (Bullseye), 12 (Bookworm) | ✅ Full | DEB |
| **Fedora** | 38, 39, 40 | ✅ Full | RPM |
| **RHEL/CentOS** | 8, 9 | ✅ Full | RPM |
| **Rocky Linux** | 8, 9 | ✅ Full | RPM |
| **Linux Mint** | 21.x | ✅ Full | DEB |
| **Pop!_OS** | 22.04 | ✅ Full | DEB |
| **Raspberry Pi OS** | 64-bit (Bookworm) | ⚠️ ARM64 | DEB |

### 2.3 Required Packages

#### Ubuntu/Debian

```bash
# Runtime dependencies
sudo apt update
sudo apt install -y \
    libgtk-3-0 \
    libgl1 \
    libasound2 \
    libxtst6 \
    libxrender1 \
    libxi6 \
    libfreetype6 \
    fontconfig

# Serial port access
sudo apt install -y \
    libudev-dev \
    libserialport0
```

#### Fedora/RHEL

```bash
# Runtime dependencies
sudo dnf install -y \
    gtk3 \
    mesa-libGL \
    alsa-lib \
    libXtst \
    libXrender \
    libXi \
    freetype \
    fontconfig

# Serial port access
sudo dnf install -y \
    libudev-devel \
    libserialport
```

### 2.4 Hardware Access Permissions

#### Serial Port Access

```bash
# Add user to dialout group (required for serial devices)
sudo usermod -a -G dialout $USER

# Verify group membership (logout/login required)
groups $USER
```

#### USB Device Access (udev rules)

Create `/etc/udev/rules.d/99-pos-hardware.rules`:

```udev
# Epson receipt printers
SUBSYSTEM=="usb", ATTR{idVendor}=="04b8", MODE="0666", GROUP="dialout"

# Datalogic scanners/scales
SUBSYSTEM=="usb", ATTR{idVendor}=="05f9", MODE="0666", GROUP="dialout"

# PAX payment terminals
SUBSYSTEM=="usb", ATTR{idVendor}=="1f22", MODE="0666", GROUP="dialout"

# Generic serial ports
KERNEL=="ttyUSB[0-9]*", MODE="0666", GROUP="dialout"
KERNEL=="ttyACM[0-9]*", MODE="0666", GROUP="dialout"
```

Apply rules:
```bash
sudo udevadm control --reload-rules
sudo udevadm trigger
```

### 2.5 Linux Installation

#### DEB Package (Ubuntu/Debian)

```bash
# Install
sudo dpkg -i growpos_2.0.0_amd64.deb
sudo apt-get install -f  # Fix dependencies if needed

# Uninstall
sudo apt remove growpos
```

#### RPM Package (Fedora/RHEL)

```bash
# Install
sudo rpm -i growpos-2.0.0.x86_64.rpm
# or
sudo dnf install growpos-2.0.0.x86_64.rpm

# Uninstall
sudo dnf remove growpos
```

#### AppImage (Universal)

```bash
# Make executable
chmod +x GroPOS-2.0.0.AppImage

# Run
./GroPOS-2.0.0.AppImage

# Optional: Integrate with desktop
./GroPOS-2.0.0.AppImage --install
```

### 2.6 Systemd Service

Create `/etc/systemd/system/growpos.service`:

```ini
[Unit]
Description=GroPOS Point of Sale Application
After=network.target graphical.target
Wants=graphical.target

[Service]
Type=simple
User=pos
Group=pos
Environment=DISPLAY=:0
Environment=XAUTHORITY=/home/pos/.Xauthority
WorkingDirectory=/opt/growpos
ExecStart=/opt/growpos/bin/growpos
Restart=always
RestartSec=5

[Install]
WantedBy=graphical.target
```

Enable and start:
```bash
sudo systemctl daemon-reload
sudo systemctl enable growpos
sudo systemctl start growpos
```

### 2.7 Kiosk Mode (Linux)

#### GNOME Kiosk

```bash
# Install GNOME Kiosk
sudo apt install gnome-kiosk

# Create kiosk user
sudo useradd -m -s /bin/bash poskiosk

# Configure auto-login in /etc/gdm3/custom.conf
[daemon]
AutomaticLoginEnable=true
AutomaticLogin=poskiosk
```

#### Cage Wayland Kiosk

```bash
# Install Cage
sudo apt install cage

# Create systemd service for Cage
# /etc/systemd/system/cage@.service
[Unit]
Description=Cage Wayland kiosk
After=systemd-user-sessions.service

[Service]
User=%i
PAMName=login
TTYPath=/dev/tty7
Environment=XDG_SESSION_TYPE=wayland
ExecStart=/usr/bin/cage /opt/growpos/bin/growpos

[Install]
WantedBy=multi-user.target
```

---

## 3. Android POS Devices

### 3.1 System Requirements

| Component | Minimum | Recommended |
|-----------|---------|-------------|
| **Android Version** | 7.0 (API 24) | 10.0+ (API 29+) |
| **RAM** | 2 GB | 3 GB+ |
| **Storage** | 100 MB free | 500 MB+ free |
| **Screen** | 5" 720p | 7"+ 1080p |
| **Connectivity** | WiFi | WiFi + LTE |

### 3.2 Supported POS Devices

#### Sunmi Devices

| Model | Screen | Printer | Scanner | NFC | Android |
|-------|--------|---------|---------|-----|---------|
| **V2 Pro** | 5.99" | Yes (58mm) | Yes | Yes | 7.1 |
| **V2s** | 6.0" | Yes (58mm) | Yes | Yes | 11 |
| **T2** | 15.6" + 10.1" | External | External | No | 7.1 |
| **T2 Mini** | 11.6" | Yes (80mm) | Yes | Yes | 9 |
| **D3** | 10.1" + 10.1" | Yes (80mm) | External | Yes | 11 |

**Sunmi SDK Integration:**

```kotlin
// build.gradle.kts (androidMain)
dependencies {
    implementation("com.sunmi:printerlibrary:1.0.18")
    implementation("com.sunmi:SunmiUILibrary:1.0.10")
}
```

```kotlin
// SunmiPrinterService.kt
class SunmiPrinterService(private val context: Context) {
    private var sunmiPrinterService: SunmiPrinterService? = null
    
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            sunmiPrinterService = SunmiPrinterService.Stub.asInterface(service)
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            sunmiPrinterService = null
        }
    }
    
    fun bindService() {
        val intent = Intent().apply {
            setPackage("woyou.aidlservice.jiuiv5")
            action = "woyou.aidlservice.jiuiv5.IWoyouService"
        }
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }
    
    fun printText(text: String) {
        sunmiPrinterService?.printText(text, null)
    }
    
    fun cutPaper() {
        sunmiPrinterService?.cutPaper(null)
    }
}
```

#### PAX Android Devices

| Model | Screen | Printer | Scanner | NFC | Android |
|-------|--------|---------|---------|-----|---------|
| **A920** | 5" | Yes (58mm) | Camera | Yes | 5.1 / 7.1 |
| **A920 Pro** | 5.5" | Yes (58mm) | Camera | Yes | 10 |
| **A930** | 4" | Yes (58mm) | Camera | Yes | 7.1 |
| **A77** | 7" | Yes (80mm) | External | Yes | 9 |
| **A35** | 5.5" | Yes (58mm) | Camera | Yes | 10 |

**PAX SDK Integration:**

```kotlin
// PAX Android SDK is typically provided as AAR
dependencies {
    implementation(files("libs/pax-payment-sdk.aar"))
    implementation(files("libs/pax-printer-sdk.aar"))
}
```

### 3.3 Android Permissions

```xml
<!-- AndroidManifest.xml -->
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    
    <!-- Core permissions -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" 
                     android:maxSdkVersion="28" />
    
    <!-- Hardware permissions -->
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.BLUETOOTH" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
    <uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
    <uses-permission android:name="android.permission.NFC" />
    
    <!-- Kiosk mode -->
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    <uses-permission android:name="android.permission.REORDER_TASKS" />
    
    <!-- Features -->
    <uses-feature android:name="android.hardware.camera" android:required="false" />
    <uses-feature android:name="android.hardware.camera.autofocus" android:required="false" />
    <uses-feature android:name="android.hardware.bluetooth" android:required="false" />
    <uses-feature android:name="android.hardware.nfc" android:required="false" />
    
    <application
        android:name=".GroPOSApplication"
        android:label="GroPOS"
        android:icon="@mipmap/ic_launcher"
        android:allowBackup="false"
        android:theme="@style/Theme.GroPOS">
        
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:launchMode="singleTask"
            android:screenOrientation="landscape"
            android:configChanges="orientation|screenSize|keyboardHidden">
            
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
                <category android:name="android.intent.category.HOME" />
                <category android:name="android.intent.category.DEFAULT" />
            </intent-filter>
        </activity>
        
        <!-- Boot receiver for auto-start -->
        <receiver
            android:name=".BootReceiver"
            android:enabled="true"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
                <action android:name="android.intent.action.QUICKBOOT_POWERON" />
            </intent-filter>
        </receiver>
        
    </application>
</manifest>
```

### 3.4 Android Kiosk Mode

```kotlin
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable immersive mode
        enableImmersiveMode()
        
        // Lock task mode (Android 5.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            startLockTask()
        }
        
        setContent {
            GroPOSTheme {
                App()
            }
        }
    }
    
    private fun enableImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
            )
        }
    }
    
    override fun onBackPressed() {
        // Disable back button in kiosk mode
    }
}
```

### 3.5 Android MDM Integration

For enterprise deployment, integrate with MDM solutions:

| MDM Solution | Device Admin API | Managed Config |
|--------------|------------------|----------------|
| **Google Endpoint** | Yes | Yes |
| **VMware Workspace ONE** | Yes | Yes |
| **Microsoft Intune** | Yes | Yes |
| **Sunmi Device Management** | Native | Yes |
| **PAX Store** | Native | Yes |

---

## 4. Cross-Platform Considerations

### 4.1 Screen Size Adaptations

```kotlin
// Responsive layout composable
@Composable
fun ResponsiveLayout(
    content: @Composable () -> Unit
) {
    val windowInfo = LocalWindowInfo.current
    
    when {
        windowInfo.screenWidth < 600.dp -> {
            // Phone layout (single column)
            PhoneLayout { content() }
        }
        windowInfo.screenWidth < 1200.dp -> {
            // Tablet layout (two columns)
            TabletLayout { content() }
        }
        else -> {
            // Desktop layout (multi-panel)
            DesktopLayout { content() }
        }
    }
}
```

### 4.2 Input Method Handling

| Platform | Primary Input | Secondary Input | Considerations |
|----------|---------------|-----------------|----------------|
| Windows | Keyboard + Mouse | Touch (optional) | Function keys, shortcuts |
| Linux | Keyboard + Mouse | Touch (optional) | Same as Windows |
| Android | Touch | Hardware buttons | Virtual keyboard, gestures |

```kotlin
// Platform-aware keyboard handling
expect fun getPlatformKeyboardShortcuts(): KeyboardShortcuts

// Desktop implementation
actual fun getPlatformKeyboardShortcuts() = KeyboardShortcuts(
    newTransaction = KeyShortcut(Key.F1),
    productLookup = KeyShortcut(Key.F2),
    payment = KeyShortcut(Key.F3),
    lockScreen = KeyShortcut(Key.F4),
    // ...
)

// Android implementation
actual fun getPlatformKeyboardShortcuts() = KeyboardShortcuts(
    // Android typically doesn't use keyboard shortcuts
    // Map to button actions instead
)
```

### 4.3 Data Storage Paths

| Platform | App Data | Cache | Database |
|----------|----------|-------|----------|
| Windows | `%APPDATA%\GroPOS` | `%LOCALAPPDATA%\GroPOS\cache` | `%APPDATA%\GroPOS\db` |
| Linux | `~/.local/share/growpos` | `~/.cache/growpos` | `~/.local/share/growpos/db` |
| Android | `context.filesDir` | `context.cacheDir` | `context.getDatabasePath()` |

```kotlin
// Platform-aware file paths
expect fun getAppDataDirectory(): Path
expect fun getCacheDirectory(): Path
expect fun getDatabasePath(): Path
```

### 4.4 Network Configuration

```kotlin
// Ktor client with platform-specific engine
expect fun createHttpClient(): HttpClient

// Desktop (CIO engine)
actual fun createHttpClient() = HttpClient(CIO) {
    install(ContentNegotiation) { json() }
    install(Logging) { level = LogLevel.INFO }
    engine {
        requestTimeout = 30_000
    }
}

// Android (Android engine with OkHttp)
actual fun createHttpClient() = HttpClient(Android) {
    install(ContentNegotiation) { json() }
    install(Logging) { level = LogLevel.INFO }
    engine {
        connectTimeout = 30_000
        socketTimeout = 30_000
    }
}
```

---

## Appendix: Version Compatibility Matrix

| Component | Windows 10 | Windows 11 | Ubuntu 22.04 | Fedora 39 | Android 10 | Android 14 |
|-----------|------------|------------|--------------|-----------|------------|------------|
| JDK 21 | ✅ | ✅ | ✅ | ✅ | N/A | N/A |
| JDK 17 | ✅ | ✅ | ✅ | ✅ | N/A | N/A |
| Compose 1.6 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| JavaPOS 1.15 | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ |
| PAX PosLink | ✅ | ✅ | ⚠️ | ⚠️ | ❌ | ❌ |
| PAX Android SDK | ❌ | ❌ | ❌ | ❌ | ✅ | ✅ |
| Sunmi SDK | ❌ | ❌ | ❌ | ❌ | ✅ | ✅ |
| SQLDelight 2.0 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Ktor 2.3 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |

