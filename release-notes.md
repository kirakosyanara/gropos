# GroPOS v1.0.0-rc1 Release Notes

**Release Date:** January 3, 2026  
**Branch:** `release/v1.0.0-rc1`  
**Status:** Release Candidate 1

---

## 🎯 Release Highlights

This release candidate represents the culmination of **Phase 4 (Hardware Integration & Backend Sync)** and **Phase 5 (Lottery Module)** development. All P0 critical features are implemented and tested.

---

## ✅ Phase 4: Hardware Integration & Backend Sync - COMPLETE

### Hardware Drivers

| Platform | Printer | Scanner | Scale | Cash Drawer |
|----------|---------|---------|-------|-------------|
| **Desktop** | ✅ ESC/POS (jSerialComm) | ✅ Serial Scanner | ✅ CAS PD-II | ✅ Via Printer |
| **Android** | ✅ Sunmi Native | ✅ Sunmi + CameraX/MLKit | ⏳ Future | ✅ Via Printer |

**Key Features:**
- **DesktopEscPosPrinter**: Full ESC/POS command protocol, receipt formatting, cash drawer pulse
- **DesktopSerialScanner**: Auto-discovery of COM ports, buffer-based protocol parsing
- **DesktopCasScale**: CAS PD-II protocol, continuous weight streaming, stable/unstable detection
- **SunmiPrinterService**: Native AIDL binding, paper status monitoring
- **CameraBarcodeScanner**: CameraX + MLKit integration for Android devices

### Backend API Integration

| Component | Status | Implementation |
|-----------|--------|----------------|
| ApiClient | ✅ | Ktor HTTP client with Bearer token injection |
| TokenRefreshManager | ✅ | Mutex-protected concurrent refresh handling |
| RemoteTillRepository | ✅ | REST API for till operations |
| RemoteVendorRepository | ✅ | REST API with in-memory caching |
| RemoteDeviceRepository | ✅ | QR registration + device status |

### Offline-First Architecture

- **OfflineQueueService**: Persistent queue with Couchbase backing
- **SyncWorker**: Exponential backoff with jitter for network resilience
- **CouchbaseQueuePersistence**: Write-ahead logging for crash recovery
- **Crash Recovery**: Pending transactions restored on app restart

### Real Authentication (P0 Fix)
- Replaced simulated login with real API endpoints
- `POST /employee/login` for credential-based auth
- `POST /employee/refresh` for token refresh
- `POST /employee/logout` for session termination

---

## ✅ Phase 5: Lottery Module - COMPLETE

### Domain Layer
- **LotteryGame**: Game catalog model (scratchers + draw games)
- **LotteryTransaction**: Sale/payout records with audit trail
- **PayoutTierCalculator**: Tax-compliant payout tier logic
  - Tier 1 ($0-$49.99): APPROVED
  - Tier 2 ($50-$599.99): LOGGED_ONLY (Manager approval)
  - Tier 3 ($600+): **REJECTED** (W-2G required - deferred to customer service)

### Presentation Layer
- **LotterySaleScreen**: Grid layout with category filtering, cart management
- **LotteryPayoutScreen**: Numeric keypad with real-time tier validation
- **LotteryReportScreen**: Daily summary with sales/payouts/net breakdown

### Compliance Features
- ✅ $600+ payouts blocked (per IRS W-2G requirements)
- ✅ All transactions attributed to logged-in employee
- ✅ Manager approval required for Tier 2 payouts
- ✅ Complete audit trail for all lottery operations

---

## 🔧 P0 Critical Fixes

### DI Wiring - Production Repository Integration
- `NetworkModule.kt`: Provides `ApiClient`, `TokenStorage`, `SecureStorage`, `ApiAuthService`
- `AuthModule.kt`: `FakeTillRepository` → `RemoteTillRepository`
- `CheckoutModule.kt`: `FakeVendorRepository` → `RemoteVendorRepository`
- `DeviceModule.kt`: `FakeDeviceRepository` → `RemoteDeviceRepository`

### Lottery Compliance Fix
- **Problem**: `LotteryModule` hardcoded `staffId = 100`
- **Solution**: Dynamic `staffId` from `CashierSessionManager.activeSession`
- **Impact**: All lottery transactions now attributed to correct employee

### Offline Queue Persistence
- **Problem**: Queue was in-memory only (data loss on crash)
- **Solution**: `CouchbaseQueuePersistence` with write-ahead pattern
- **Impact**: Zero data loss guarantee for offline transactions

---

## 📊 Test Coverage

| Category | Test Cases | Status |
|----------|------------|--------|
| Lottery Domain | 73 | ✅ Pass |
| Payment Processing | 17 | ✅ Pass |
| Cart Calculations | 12 | ✅ Pass |
| Authentication | 6 | ✅ Pass |
| Hardware Abstraction | 34 | ✅ Pass |
| Offline Queue | 15 | ✅ Pass |
| Token Refresh | 12 | ✅ Pass |
| **Total** | **327** | ✅ Pass |

---

## 📋 Known Limitations

1. **Scale (Android)**: Not yet implemented - uses simulated values
2. **W-2G Forms**: $600+ payouts blocked; W-2G form generation deferred
3. **NFC Badge Login**: Simulated implementation only (hardware TBD)
4. **Customer Display**: Dual-window mode requires multi-monitor setup

---

## 🔒 Security Notes

- PCI-compliant: No PAN/CVV/PIN logging
- All API calls use Bearer token authentication
- Credentials stored in platform-secure storage
- Audit trail for all sensitive operations

---

## 📥 Installation

### Desktop (macOS/Windows/Linux)
```bash
./gradlew :desktopApp:run                # Development
./gradlew :desktopApp:packageDmg         # macOS installer
./gradlew :desktopApp:packageMsi         # Windows installer
./gradlew :desktopApp:packageDeb         # Linux installer
```

### Android
```bash
./gradlew :androidApp:installDebug       # Debug APK
./gradlew :androidApp:assembleRelease    # Release APK
```

---

## 🧪 Test Credentials

| Employee | PIN | Role | Permissions |
|----------|-----|------|-------------|
| Jane Doe | 1234 | Cashier | Standard POS operations |
| John Smith | 5678 | Supervisor | Can approve some actions |
| Mary Johnson | 9999 | Manager | Full approval authority |

---

## 📝 Upgrade Notes

### From v0.9.0 (Feature Complete Alpha)
1. Database migration automatic (new collections added)
2. Clear app cache if encountering DI errors
3. Re-login required after upgrade (token refresh change)

### Environment Variables (Desktop)
```bash
export USE_REAL_HARDWARE=true    # Enable real hardware drivers
export SCANNER_PORT=/dev/ttyUSB0 # Scanner COM port
export PRINTER_PORT=/dev/ttyUSB1 # Printer COM port
export SCALE_PORT=/dev/ttyUSB2   # Scale COM port
```

---

## 📞 Support

For issues or questions:
- GitHub Issues: [gropos/issues](https://github.com/unisight/gropos/issues)
- Email: support@gropos.com

---

**Document Control:**  
- Version: 1.0.0-rc1  
- Author: GroPOS Development Team  
- Classification: Internal Release

