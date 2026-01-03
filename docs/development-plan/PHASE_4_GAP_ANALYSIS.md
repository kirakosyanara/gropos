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
| Scale | `SimulatedScaleService` | ⚠️ Interface ready (`ScaleService.kt`), desktop impl pending |
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

### 3. Lottery Function (Presentation Layer Remaining)

**Priority:** High  
**Effort:** Medium (domain layer complete)  
**Source:** `features/lottery/INDEX.md`, `FUNCTIONS_MENU.md`

#### Current State

The Lottery module **domain layer is complete** (Step 13 done):

- ✅ `LotteryModels.kt` - All domain models (LotteryGame, LotteryTransaction, etc.)
- ✅ `LotteryRepository.kt` - Interface with all CRUD operations
- ✅ `PayoutTierCalculator.kt` - Tier 1/2/3 business logic (23 tests)
- ✅ `FakeLotteryRepository.kt` - Seeded with 10 games (19 tests)
- ❌ Presentation layer (Steps 14-17) - NOT started

#### Requirements Summary

| Component | Description | Status |
|-----------|-------------|--------|
| **Domain Models** | LotteryGame, LotteryTransaction, PayoutStatus | ✅ Complete |
| **PayoutTierCalculator** | Tier 1/2/3 logic with BigDecimal precision | ✅ Complete |
| **FakeLotteryRepository** | 5 scratchers + 5 draw games seeded | ✅ Complete |
| **Lottery Mode Entry** | Age verification, then isolated mode | ❌ Pending |
| **Scratcher Sales** | Scan/lookup scratchers, cash-only | ❌ Pending |
| **Draw Game Sales** | Select game, configure options | ❌ Pending |
| **Tier 1-2 Payouts** | Cashier processes $0-$599.99 | ❌ Pending |
| **Tier 3 Payouts** | Manager approval + W-2G for $600+ | ❌ Pending (W-2G deferred) |
| **Daily Reports** | Sales, payouts, net, commission | ❌ Pending |
| **Branch Setting** | `HasStateLottery` feature flag | ❌ Pending |

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

### Step 9: Offline Sync - COMPLETE ✅

- [x] `DefaultOfflineQueueService` with thread-safe Mutex
- [x] `SyncWorker` with exponential backoff + jitter
- [x] `OfflineQueueTest.kt` and `SyncWorkerTest.kt` passing

### Step 13: Lottery Domain Layer - COMPLETE ✅

- [x] `LotteryModels.kt` - All domain models
- [x] `PayoutTierCalculator.kt` - Tier logic with 23 tests
- [x] `LotteryRepository.kt` - Interface
- [x] `FakeLotteryRepository.kt` - 10 seeded games with 19 tests

---

## Estimated Timeline

| Area | Effort | Status |
|------|--------|--------|
| API Integration (core) | High | ✅ COMPLETE (ApiClient, RemoteRepositories) |
| Hardware Drivers | High | ✅ MOSTLY COMPLETE (Printer, Scanner done; Scale pending) |
| Offline Sync | Medium | ✅ COMPLETE (OfflineQueue, SyncWorker) |
| Lottery Domain Layer | Medium | ✅ COMPLETE (Step 13) |
| Lottery Presentation | Medium | ⏳ REMAINING (Steps 14-17, ~1-2 weeks) |

### Remaining Work

| Step | Description | Effort |
|------|-------------|--------|
| 7.3 | Desktop Scale Driver (`DesktopCasScale`) | 1-2 days |
| 14 | Lottery Sales Screen | 2-3 days |
| 15 | Lottery Payout Screen | 2-3 days |
| 16 | Lottery Reports | 1-2 days |
| 17 | Lottery Integration & Polish | 1 day |
| **Total Remaining** | | **~1-2 weeks** |

---

*Last Updated: January 2, 2026*
*Related: [REMEDIATION_CHECKLIST.md](./REMEDIATION_CHECKLIST.md), [features/lottery/INDEX.md](./features/lottery/INDEX.md)*

