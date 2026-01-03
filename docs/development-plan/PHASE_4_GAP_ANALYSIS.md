# Phase 4 Gap Analysis - Post-Simulator Edition

**Generated:** January 2, 2026  
**Status:** Planning Document  
**Purpose:** Identify top missing areas for Phase 4 development after Milestone 1 (Simulator Edition) completion

---

## Current State Summary

Milestone 1 (Simulator Edition) is complete. The GroPOS application now has:

- ✅ Full authentication flow (Login, Lock Screen, Logout)
- ✅ Complete checkout UI with modification mode
- ✅ Payment processing via simulated terminal
- ✅ Returns processing workflow
- ✅ Till operations (Cash Pickup, Vendor Payout)
- ✅ Hold/Recall transactions
- ✅ Manager Approval infrastructure
- ✅ Device Registration UI
- ✅ Customer Display with ad overlay
- ✅ NFC badge scanning (simulated)

**All above features work against simulated/fake backends and hardware.**

---

## Top 3 Missing Areas

### 1. API Integration (Replacing Fakes)

**Priority:** Critical  
**Effort:** High  
**Source:** `REMEDIATION_CHECKLIST.md` Section 12

#### Current State

All repositories use `Fake*Repository` implementations with hardcoded data:

| Repository | Fake Implementation | Real Implementation |
|------------|---------------------|---------------------|
| `EmployeeRepository` | `FakeEmployeeRepository` | ⚠️ Pending (auth layer ready) |
| `TillRepository` | `FakeTillRepository` | ✅ `RemoteTillRepository` (Jan 2026) |
| `VendorRepository` | `FakeVendorRepository` | ✅ `RemoteVendorRepository` (Jan 2026) |
| `DeviceRepository` | `FakeDeviceRepository` | ✅ `RemoteDeviceRepository` + `SecureStorage` (Jan 2026) |
| `TransactionRepository` | Partial (CouchbaseLite) | ⚠️ Sync pending |

#### Required Work

1. **HTTP Client Setup**
   - Configure Ktor client with base URL from environment
   - Add authentication interceptor for bearer tokens
   - Implement request/response logging

2. **Authentication Flow**
   - `POST /employee/gropos-login` - PIN authentication
   - `POST /employee/verify-password` - Unlock verification
   - Token storage in secure preferences
   - Token refresh logic

3. **Error Handling**
   - Parse backend error format: `{ code, message, requestId, timestamp }`
   - Map to domain exceptions
   - Retry logic with exponential backoff

4. **Offline Support**
   - Transaction queue for failed requests
   - Background sync via WorkManager (Android) / CoroutineWorker (Desktop)
   - Conflict resolution strategy

#### Files to Create/Modify

```
shared/src/commonMain/kotlin/
├── core/
│   ├── network/
│   │   ├── ApiClient.kt              # Ktor HTTP client
│   │   ├── AuthInterceptor.kt        # Bearer token injection
│   │   └── ErrorParser.kt            # Backend error mapping
│   └── sync/
│       ├── SyncWorker.kt             # Background sync
│       └── OfflineQueue.kt           # Transaction queue
├── features/
│   ├── auth/data/
│   │   └── RemoteEmployeeRepository.kt
│   ├── till/data/
│   │   └── RemoteTillRepository.kt
│   └── ...
```

---

### 2. Hardware Drivers (Replacing Simulators)

**Priority:** Critical (for production)  
**Effort:** High  
**Source:** `REMEDIATION_CHECKLIST.md` Section 13

#### Current State

All hardware uses simulated implementations:

| Hardware | Simulator | Real Driver |
|----------|-----------|-------------|
| Payment Terminal | `SimulatedPaymentTerminal` | ⚠️ PAX PosLink interface ready |
| Receipt Printer | Virtual console output | ✅ `DesktopEscPosPrinter` + `SunmiPrinterService` (Jan 2026) |
| Barcode Scanner | `SimulatedBarcodeScanner` | ✅ `DesktopSerialScanner` + `CameraBarcodeScanner` + `SunmiHardwareScanner` (Jan 2026) |
| NFC Reader | `SimulatedNfcScanner` | ⚠️ Interface ready, platform impl pending |
| Scale | None | ✅ `DesktopCasScale` (Jan 2026) |
| Cash Drawer | None | ✅ Integrated in `DesktopEscPosPrinter` (Jan 2026) |
| Customer Display | Compose secondary window | ⚠️ Partial |

#### Required Work

1. **Payment Terminal**
   - Verifone/Ingenico SDK integration
   - EMV transaction flow
   - Signature capture
   - Tip adjustment

2. **Receipt Printer**
   - ESC/POS command generation
   - USB/Bluetooth/Network connection
   - Drawer kick pulse
   - Paper status detection

3. **Barcode Scanner**
   - Serial COM port (RS-232) - Desktop
   - USB HID mode - Desktop
   - Camera-based - Android
   - Zebra SDK integration

4. **Scale**
   - Serial protocol parsing
   - Weight stabilization detection
   - Tare function

5. **NFC Reader**
   - Android NFC API
   - PCSC for Desktop

#### Architecture

```
shared/src/
├── commonMain/kotlin/core/hardware/
│   ├── PaymentTerminal.kt         # Interface (exists)
│   ├── PrinterService.kt          # Interface
│   ├── BarcodeScanner.kt          # Interface (exists)
│   ├── ScaleService.kt            # Interface
│   └── NfcScanner.kt              # Interface (exists)
├── androidMain/kotlin/core/hardware/
│   ├── AndroidPaymentTerminal.kt
│   ├── AndroidPrinter.kt
│   └── AndroidNfcScanner.kt
└── desktopMain/kotlin/core/hardware/
    ├── DesktopPaymentTerminal.kt
    ├── DesktopPrinter.kt          # ESC/POS via javax.usb
    ├── DesktopBarcodeScanner.kt   # jSerialComm
    └── DesktopScale.kt            # Serial port
```

---

### 3. Lottery Function (The Overlooked Feature)

**Priority:** High  
**Effort:** Medium-High  
**Source:** `features/lottery/INDEX.md`, `FUNCTIONS_MENU.md`

#### Current State

The Lottery module is **fully documented** (8 specification files) but **completely unimplemented**:

- `FUNCTIONS_MENU.md` shows "Lotto Pay" button as placeholder
- No `LotteryScreen`, `LotterySaleScreen`, or `LotteryPayoutScreen` exist
- No lottery data models or repositories
- No API integration for lottery endpoints

#### Requirements Summary

| Component | Description | Status |
|-----------|-------------|--------|
| **Lottery Mode Entry** | Age verification, then isolated mode | ❌ |
| **Scratcher Sales** | Scan/lookup scratchers, cash-only | ❌ |
| **Draw Game Sales** | Select game, configure options | ❌ |
| **Tier 1-2 Payouts** | Cashier processes $0-$599.99 | ❌ |
| **Tier 3 Payouts** | Manager approval + W-2G for $600+ | ❌ |
| **Daily Reports** | Sales, payouts, net, commission | ❌ |
| **Branch Setting** | `HasStateLottery` feature flag | ❌ |

#### Key Design Decisions

1. **Isolated Transaction Mode** - Lottery is NOT mixed with retail
2. **Cash-Only** - No EBT/SNAP/Credit allowed
3. **Tiered Payouts** - $0-49.99 (cashier), $50-599.99 (logged), $600+ (manager + W-2G)
4. **Separate Audit Trail** - State lottery compliance

#### Required Work

1. **UI Screens**
   - `LotteryScreen.kt` - Hub with Sell/Payout/Report
   - `LotterySaleScreen.kt` - Scratcher and draw game sales
   - `LotteryPayoutScreen.kt` - Winnings redemption
   - `LotteryReportScreen.kt` - Daily summary (manager)

2. **ViewModels**
   - `LotteryViewModel.kt` - Main state management
   - `LotterySaleViewModel.kt` - Sale transaction state
   - `LotteryPayoutViewModel.kt` - Payout state with tier logic

3. **Data Layer**
   - `LotteryGame` model + repository
   - `LotteryTransaction` model + repository
   - API endpoints: `/lottery/sale`, `/lottery/payout`, `/lottery/report/*`

4. **Integration**
   - Wire "Lotto Pay" button in Functions Menu
   - Age verification on lottery mode entry
   - Manager approval for Tier 3 payouts
   - Receipt printing for lottery transactions

---

## Phase 4 Recommended Order

| Step | Area | Rationale |
|------|------|-----------|
| 1 | **API Integration** | Foundation - all features need real data |
| 2 | **Hardware Drivers** | Required for production deployment |
| 3 | **Lottery Module** | Feature complete - can be parallelized |

### Dependency Graph

```
API Integration ─────┬────▶ Hardware Drivers ────▶ Production Ready
                     │
                     └────▶ Lottery Module ──────▶ Feature Complete
```

---

## Success Criteria

### Phase 4 Complete When:

- [x] At least one `RemoteXxxRepository` replaces a `FakeXxxRepository` ✅ (Till, Vendor, Device repos - Jan 2026)
- [x] Bearer token authentication works against staging API ✅ (ApiClient + TokenRefreshManager - Jan 2026)
- [x] At least one real hardware device connects (printer or scanner) ✅ (Desktop ESC/POS + Serial Scanner + CameraX/MLKit - Jan 2026)
- [ ] Lottery Mode screen is accessible from Functions Menu
- [ ] Lottery sale and payout transactions can be processed (fake backend OK)

### Phase 4.6 API Completeness - COMPLETE ✅

- [x] `RemoteTillRepository` with TDD tests
- [x] `RemoteVendorRepository` with caching
- [x] `RemoteDeviceRepository` with SecureStorage integration
- [x] `CameraPreview` Composable for Android barcode scanning

---

## Estimated Timeline

| Area | Effort | Team Weeks |
|------|--------|------------|
| API Integration (core) | High | 2-3 weeks |
| Hardware Drivers (1-2 devices) | High | 2 weeks |
| Lottery Module (MVP) | Medium | 1-2 weeks |
| **Total** | | **5-7 weeks** |

---

*Last Updated: January 2, 2026*
*Related: [REMEDIATION_CHECKLIST.md](./REMEDIATION_CHECKLIST.md), [features/lottery/INDEX.md](./features/lottery/INDEX.md)*

