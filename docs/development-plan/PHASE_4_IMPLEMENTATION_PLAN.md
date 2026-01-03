# Phase 4 Implementation Plan - Production Hardening

**Created:** January 2, 2026  
**Status:** Ready for Implementation  
**Prerequisite:** Phase 4, Steps 1-6 Complete (API Integration Core)  
**Estimated Duration:** 4-5 weeks

---

## Overview

This document provides a detailed, step-by-step implementation plan for completing Phase 4 (Production Hardening) and Phase 5 (Lottery Module). Each step includes:

- 🌿 **Git Control** - Branch naming, commits, PRs
- 📝 **Documentation** - Updates required per governance
- ✅ **Testing** - Unit tests, integration tests, previews
- 📋 **Acceptance Criteria** - Definition of done

---

## Phase Structure

```
Phase 4.5: Production Hardening (Steps 7-9)     ~2-3 weeks
├── Step 7: Hardware Drivers
├── Step 8: Token Refresh Logic
└── Step 9: Offline Queue

Phase 4.6: API Completeness (Steps 10-12)       ~1 week
├── Step 10: Remote Till Repository
├── Step 11: Remote Vendor Repository
└── Step 12: Remote Device Repository

Phase 5: Lottery Module (Steps 13-17)           ~2 weeks
├── Step 13: Lottery Data Layer
├── Step 14: Lottery Sales Screen
├── Step 15: Lottery Payout Screen
├── Step 16: Lottery Reports
└── Step 17: Integration & Polish
```

---

# Phase 4.5: Production Hardening

## Step 7: Hardware Drivers

**Priority:** Critical  
**Effort:** High (1-2 weeks)  
**Dependencies:** None (interfaces already exist)

### 7.1 Receipt Printer Driver

#### 🌿 Git Control

```bash
# Create feature branch from main
git checkout main
git pull origin main
git checkout -b feature/receipt-printer-driver

# Commit pattern (atomic commits):
git commit -m "feat(hardware): add PrinterService interface with ESC/POS commands"
git commit -m "feat(hardware): implement DesktopPrinter with USB connection"
git commit -m "feat(hardware): implement AndroidPrinter with Sunmi SDK"
git commit -m "test(hardware): add PrinterServiceTest with mock device"
git commit -m "docs: update REMEDIATION_CHECKLIST printer status"
```

#### 📁 Files to Create/Modify

```
shared/src/
├── commonMain/kotlin/com/unisight/gropos/
│   └── core/hardware/
│       ├── PrinterService.kt              # NEW: Interface
│       ├── PrinterCommand.kt              # NEW: ESC/POS command builder
│       └── PrinterStatus.kt               # NEW: Status enum (Ready, OutOfPaper, etc.)
├── desktopMain/kotlin/com/unisight/gropos/
│   └── core/hardware/
│       └── DesktopPrinter.kt              # NEW: USB/Network printer via javax.usb
└── androidMain/kotlin/com/unisight/gropos/
    └── core/hardware/
        └── AndroidPrinter.kt              # NEW: Sunmi/PAX printer SDK
```

#### ✅ Testing Requirements

| Test Type | File | Coverage |
|-----------|------|----------|
| Unit Test | `PrinterServiceTest.kt` | Interface contract, command generation |
| Integration | `DesktopPrinterIntegrationTest.kt` | USB connection (requires hardware) |
| Fake | `FakePrinterService.kt` | In-memory job list for testing |

**Test Cases:**
```kotlin
// PrinterServiceTest.kt
class PrinterServiceTest {
    @Test fun `printReceipt generates correct ESC-POS commands`()
    @Test fun `openDrawer sends pulse command`()
    @Test fun `getStatus returns OutOfPaper when paper low`()
    @Test fun `printReceipt handles special characters`()
    @Test fun `printReceipt supports thermal width 80mm and 58mm`()
}
```

#### 📝 Documentation Updates

1. **REMEDIATION_CHECKLIST.md** (Section 13):
   - Update "Receipt Printer Driver" from `⚠️ Partial` to `✅ Match`
   - Update Hardware Drivers summary statistics

2. **CHANGELOG.md**:
   ```markdown
   ### Added
   - **Receipt Printer Driver (Step 7.1)**: ESC/POS hardware abstraction
     - `PrinterService` interface with `printReceipt()`, `openDrawer()`, `getStatus()`
     - `DesktopPrinter` using javax.usb for USB thermal printers
     - `AndroidPrinter` using Sunmi SDK for integrated printers
   ```

3. **hardware/DESKTOP_HARDWARE.md**:
   - Add "Printer Configuration" section with COM port/USB settings

#### 📋 Acceptance Criteria

- [ ] `PrinterService` interface defined with `printReceipt()`, `openDrawer()`, `getStatus()`
- [ ] `DesktopPrinter` can print to USB thermal printer (tested with Epson TM-T88)
- [ ] `AndroidPrinter` can print on Sunmi device (tested on V2 Pro)
- [ ] `FakePrinterService` available for test injection
- [ ] Receipt format matches `RECEIPT_FORMAT.md` specification
- [ ] Cash drawer pulse works via printer (RJ-11 connection)
- [ ] 5+ unit tests passing
- [ ] Documentation updated

#### 🔀 Pull Request

**Title:** `feat(hardware): Add receipt printer driver with ESC/POS support`

**Description:**
```markdown
## Summary
Implements real receipt printer integration for Desktop (USB) and Android (Sunmi SDK).

## Type of Change
- [x] New Feature

## Changes
- Created `PrinterService` interface in commonMain
- Implemented `DesktopPrinter` using javax.usb
- Implemented `AndroidPrinter` using Sunmi SDK
- Added `FakePrinterService` for testing

## Testing
- [x] Unit tests for ESC/POS command generation
- [x] Integration test with physical Epson TM-T88
- [x] Manual verification on Sunmi V2 Pro

## Documentation
- [x] REMEDIATION_CHECKLIST.md updated
- [x] CHANGELOG.md updated
- [x] DESKTOP_HARDWARE.md updated
```

---

### 7.2 Barcode Scanner Driver

#### 🌿 Git Control

```bash
git checkout main
git pull origin main
git checkout -b feature/barcode-scanner-driver

git commit -m "feat(hardware): add BarcodeScanner interface with Flow<String>"
git commit -m "feat(hardware): implement DesktopBarcodeScanner with jSerialComm"
git commit -m "feat(hardware): implement AndroidBarcodeScanner with ZXing"
git commit -m "test(hardware): add BarcodeScannerTest with mock serial port"
git commit -m "docs: update REMEDIATION_CHECKLIST scanner status"
```

#### 📁 Files to Create/Modify

```
shared/src/
├── commonMain/kotlin/com/unisight/gropos/
│   └── core/hardware/
│       └── BarcodeScanner.kt              # MODIFY: Add real implementations
├── desktopMain/kotlin/com/unisight/gropos/
│   └── core/hardware/
│       └── DesktopBarcodeScanner.kt       # NEW: Serial port via jSerialComm
└── androidMain/kotlin/com/unisight/gropos/
    └── core/hardware/
        ├── AndroidBarcodeScanner.kt       # NEW: Camera via ZXing
        └── SunmiBarcodeScanner.kt         # NEW: Built-in scanner SDK
```

#### ✅ Testing Requirements

| Test Type | File | Coverage |
|-----------|------|----------|
| Unit Test | `BarcodeScannerTest.kt` | Flow emission, barcode validation |
| Integration | `DesktopScannerIntegrationTest.kt` | Serial port (requires hardware) |
| Fake | `FakeScannerRepository.kt` | Already exists, enhance with timing |

**Test Cases:**
```kotlin
class BarcodeScannerTest {
    @Test fun `emits barcode when scanner reads`()
    @Test fun `filters invalid barcodes`()
    @Test fun `handles rapid successive scans`()
    @Test fun `reconnects on port disconnection`()
}
```

#### 📋 Acceptance Criteria

- [ ] `DesktopBarcodeScanner` reads from serial port (COM1-COM9)
- [ ] `AndroidBarcodeScanner` uses camera with ZXing
- [ ] `SunmiBarcodeScanner` uses built-in hardware scanner
- [ ] Barcode Flow emits within 50ms of scan
- [ ] Handles EAN-13, UPC-A, Code128, QR codes
- [ ] Auto-reconnect on USB disconnect
- [ ] 4+ unit tests passing

---

### 7.3 Payment Terminal Driver

#### 🌿 Git Control

```bash
git checkout main
git pull origin main
git checkout -b feature/payment-terminal-driver

git commit -m "feat(hardware): implement PAX payment terminal integration"
git commit -m "feat(hardware): implement Verifone terminal integration"
git commit -m "feat(hardware): add terminal status polling"
git commit -m "test(hardware): add PaymentTerminalTest with mock responses"
git commit -m "docs: update REMEDIATION_CHECKLIST terminal status"
```

#### 📁 Files to Create/Modify

```
shared/src/
├── commonMain/kotlin/com/unisight/gropos/
│   └── features/payment/domain/terminal/
│       └── PaymentTerminal.kt             # EXISTS: Add status polling
├── desktopMain/kotlin/com/unisight/gropos/
│   └── features/payment/data/
│       ├── PaxPaymentTerminal.kt          # NEW: PAX PosLink integration
│       └── VerifonePaymentTerminal.kt     # NEW: Verifone SDK
└── androidMain/kotlin/com/unisight/gropos/
    └── features/payment/data/
        └── SunmiPaymentTerminal.kt        # NEW: Sunmi payment module
```

#### ✅ Testing Requirements

**Test Cases:**
```kotlin
class PaymentTerminalTest {
    @Test fun `processPayment returns Approved for valid card`()
    @Test fun `processPayment returns Declined with reason`()
    @Test fun `cancelTransaction stops pending payment`()
    @Test fun `handles timeout after 120 seconds`()
    @Test fun `getStatus returns Disconnected when terminal offline`()
}
```

#### 📋 Acceptance Criteria

- [ ] PAX terminal integration via PosLink SDK
- [ ] EMV chip card processing works
- [ ] Magnetic stripe fallback works
- [ ] Contactless (NFC) payments work
- [ ] Void/refund transactions work
- [ ] 120-second timeout per reliability rules
- [ ] 5+ unit tests passing

---

## Step 8: Token Refresh Logic

**Priority:** High (Security)  
**Effort:** Medium (2-3 days)  
**Dependencies:** Step 2 (RemoteEmployeeRepository) complete

### 🌿 Git Control

```bash
git checkout main
git pull origin main
git checkout -b feature/token-refresh

git commit -m "feat(auth): add TokenRefreshService with automatic refresh"
git commit -m "feat(auth): add AuthInterceptor retry on 401"
git commit -m "feat(auth): persist tokens to secure storage"
git commit -m "test(auth): add TokenRefreshServiceTest"
git commit -m "docs: update REMEDIATION_CHECKLIST token refresh status"
```

### 📁 Files to Create/Modify

```
shared/src/commonMain/kotlin/com/unisight/gropos/
├── core/auth/
│   ├── TokenStorage.kt                    # MODIFY: Add expiration tracking
│   ├── TokenRefreshService.kt             # NEW: Background refresh logic
│   └── SecureTokenStorage.kt              # NEW: Platform-specific secure storage
├── core/network/
│   └── AuthInterceptor.kt                 # MODIFY: Add 401 retry with refresh
└── core/di/
    └── NetworkModule.kt                   # MODIFY: Provide TokenRefreshService

shared/src/desktopMain/kotlin/
└── core/auth/
    └── DesktopSecureStorage.kt            # NEW: Java Keystore storage

shared/src/androidMain/kotlin/
└── core/auth/
    └── AndroidSecureStorage.kt            # NEW: EncryptedSharedPreferences
```

### ✅ Testing Requirements

| Test Type | File | Coverage |
|-----------|------|----------|
| Unit Test | `TokenRefreshServiceTest.kt` | Refresh timing, expiration detection |
| Unit Test | `AuthInterceptorTest.kt` | 401 retry, concurrent request handling |
| Integration | `AuthFlowIntegrationTest.kt` | Full login → expire → refresh → continue |

**Test Cases:**
```kotlin
class TokenRefreshServiceTest {
    @Test fun `refreshes token 60 seconds before expiration`()
    @Test fun `does not refresh when token is fresh`()
    @Test fun `handles refresh failure gracefully`()
    @Test fun `queues concurrent refresh requests`()
    @Test fun `clears tokens on refresh 401 response`()
}

class AuthInterceptorTest {
    @Test fun `retries request after 401 with refreshed token`()
    @Test fun `does not retry non-401 errors`()
    @Test fun `does not retry if refresh also fails`()
    @Test fun `handles concurrent 401s with single refresh`()
}
```

### 📝 Documentation Updates

1. **REMEDIATION_CHECKLIST.md** (Section 12):
   - Update "Token Refresh Logic" from `❌ Missing` to `✅ Match`

2. **CHANGELOG.md**:
   ```markdown
   ### Added
   - **Token Refresh Logic (Step 8)**: Automatic JWT token refresh
     - `TokenRefreshService` refreshes tokens 60s before expiration
     - `AuthInterceptor` retries failed requests after token refresh
     - `SecureTokenStorage` persists tokens to platform secure storage
   ```

3. **architecture/API_INTEGRATION.md**:
   - Add "Token Lifecycle" section documenting refresh flow

### 📋 Acceptance Criteria

- [ ] Tokens refresh automatically 60 seconds before expiration
- [ ] 401 responses trigger automatic retry with refreshed token
- [ ] Tokens persist across app restarts (secure storage)
- [ ] Concurrent requests during refresh are queued, not duplicated
- [ ] Refresh failure triggers logout and navigation to LoginScreen
- [ ] 8+ unit tests passing
- [ ] No security audit findings

---

## Step 9: Offline Queue / Retry Mechanism

**Priority:** High  
**Effort:** Medium (3-4 days)  
**Dependencies:** Step 6 (RemoteTransactionRepository) complete

### 🌿 Git Control

```bash
git checkout main
git pull origin main
git checkout -b feature/offline-queue

git commit -m "feat(sync): add OfflineQueue for failed transaction storage"
git commit -m "feat(sync): add SyncWorker with exponential backoff"
git commit -m "feat(sync): add SyncStatusIndicator UI component"
git commit -m "test(sync): add OfflineQueueTest with network simulation"
git commit -m "docs: update REMEDIATION_CHECKLIST offline queue status"
```

### 📁 Files to Create/Modify

```
shared/src/commonMain/kotlin/com/unisight/gropos/
├── core/sync/
│   ├── OfflineQueue.kt                    # NEW: Transaction queue interface
│   ├── SyncWorker.kt                      # NEW: Background sync logic
│   ├── SyncStatus.kt                      # NEW: Status model (Synced, Pending, Failed)
│   └── SyncConfig.kt                      # NEW: Retry policy configuration
├── core/components/
│   └── SyncStatusIndicator.kt             # NEW: UI badge for pending syncs

shared/src/desktopMain/kotlin/
└── core/sync/
    └── DesktopSyncWorker.kt               # NEW: Coroutine-based worker

shared/src/androidMain/kotlin/
└── core/sync/
    └── AndroidSyncWorker.kt               # NEW: WorkManager integration
```

### ✅ Testing Requirements

| Test Type | File | Coverage |
|-----------|------|----------|
| Unit Test | `OfflineQueueTest.kt` | Queue operations, persistence |
| Unit Test | `SyncWorkerTest.kt` | Retry logic, backoff timing |
| Integration | `OfflineModeIntegrationTest.kt` | Full offline → online sync |

**Test Cases:**
```kotlin
class OfflineQueueTest {
    @Test fun `enqueues failed transaction`()
    @Test fun `persists queue across app restart`()
    @Test fun `dequeues in FIFO order`()
    @Test fun `marks transaction as synced after success`()
    @Test fun `increments retry count on failure`()
    @Test fun `abandons after max retries`()
}

class SyncWorkerTest {
    @Test fun `uses exponential backoff on failure`()
    @Test fun `adds jitter to backoff`()
    @Test fun `syncs all pending transactions`()
    @Test fun `respects network availability`()
}
```

### 📝 Documentation Updates

1. **REMEDIATION_CHECKLIST.md** (Section 12):
   - Update "Offline Queue (WorkManager)" from `❌ Missing` to `✅ Match`
   - Update "Sync Service" from `❌ Missing` to `✅ Match`

2. **CHANGELOG.md**:
   ```markdown
   ### Added
   - **Offline Queue (Step 9)**: Transaction retry mechanism
     - `OfflineQueue` stores failed transactions for retry
     - `SyncWorker` with exponential backoff (1s → 2s → 4s → 8s → max 5min)
     - `SyncStatusIndicator` shows pending count in header
   ```

3. **architecture/DATA_FLOW.md**:
   - Add "Offline Sync Flow" diagram

### 📋 Acceptance Criteria

- [ ] Failed transactions are queued for retry
- [ ] Queue persists to local database (survives app kill)
- [ ] Exponential backoff: 1s, 2s, 4s, 8s, 16s, 32s, 64s, 128s, 256s, 300s (max)
- [ ] Random jitter (±500ms) prevents thundering herd
- [ ] Max 10 retry attempts before abandoning
- [ ] UI indicator shows pending sync count
- [ ] Auto-sync when network becomes available
- [ ] 10+ unit tests passing

---

# Phase 4.6: API Completeness

## Step 10: Remote Till Repository

**Priority:** Medium  
**Effort:** Low (1 day)  
**Dependencies:** Step 2 network layer

### 🌿 Git Control

```bash
git checkout main
git pull origin main
git checkout -b feature/remote-till-repository

git commit -m "feat(till): add TillDto with domain mapper"
git commit -m "feat(till): implement RemoteTillRepository"
git commit -m "test(till): add RemoteTillRepositoryTest"
git commit -m "docs: update REMEDIATION_CHECKLIST till status"
```

### 📁 Files to Create/Modify

```
shared/src/commonMain/kotlin/com/unisight/gropos/
└── features/cashier/data/
    ├── remote/
    │   ├── dtos/
    │   │   └── TillDto.kt                 # NEW: API response DTOs
    │   └── RemoteTillRepository.kt        # NEW: API implementation
    └── FakeTillRepository.kt              # EXISTS: Keep as fallback
```

### ✅ Testing Requirements

```kotlin
class RemoteTillRepositoryTest {
    @Test fun `getTills returns available tills`()
    @Test fun `assignTill marks till as occupied`()
    @Test fun `releaseTill marks till as available`()
    @Test fun `handles 401 with auth error`()
    @Test fun `handles network timeout`()
}
```

### 📋 Acceptance Criteria

- [ ] `GET /tills` returns list of tills
- [ ] `POST /tills/{id}/assign` assigns till to employee
- [ ] `POST /tills/{id}/release` releases till
- [ ] Feature flag `USE_REMOTE_TILL_REPOSITORY` controls binding
- [ ] 5 unit tests passing

---

## Step 11: Remote Vendor Repository

**Priority:** Low  
**Effort:** Low (0.5 days)  
**Dependencies:** Step 2 network layer

### 🌿 Git Control

```bash
git checkout main
git pull origin main
git checkout -b feature/remote-vendor-repository

git commit -m "feat(vendor): add VendorDto with domain mapper"
git commit -m "feat(vendor): implement RemoteVendorRepository"
git commit -m "test(vendor): add RemoteVendorRepositoryTest"
git commit -m "docs: update REMEDIATION_CHECKLIST vendor status"
```

### 📁 Files to Create/Modify

```
shared/src/commonMain/kotlin/com/unisight/gropos/
└── features/cashier/data/
    ├── remote/
    │   ├── dtos/
    │   │   └── VendorDto.kt               # NEW
    │   └── RemoteVendorRepository.kt      # NEW
    └── FakeVendorRepository.kt            # EXISTS
```

### 📋 Acceptance Criteria

- [ ] `GET /vendors` returns vendor list
- [ ] Feature flag controls binding
- [ ] 3 unit tests passing

---

## Step 12: Remote Device Repository

**Priority:** Medium  
**Effort:** Medium (1 day)  
**Dependencies:** Step 2 network layer

### 🌿 Git Control

```bash
git checkout main
git pull origin main
git checkout -b feature/remote-device-repository

git commit -m "feat(device): add DeviceRegistrationDto"
git commit -m "feat(device): implement RemoteDeviceRepository"
git commit -m "feat(device): add device heartbeat endpoint"
git commit -m "test(device): add RemoteDeviceRepositoryTest"
git commit -m "docs: update REMEDIATION_CHECKLIST device status"
```

### 📁 Files to Create/Modify

```
shared/src/commonMain/kotlin/com/unisight/gropos/
└── features/device/data/
    ├── remote/
    │   ├── dtos/
    │   │   └── DeviceDto.kt               # NEW
    │   └── RemoteDeviceRepository.kt      # NEW
    └── FakeDeviceRepository.kt            # EXISTS
```

### 📋 Acceptance Criteria

- [ ] `POST /device/register` registers new device
- [ ] `GET /device/status` checks registration status
- [ ] `POST /device/heartbeat` sends periodic health check
- [ ] Stores `stationId` and `apiKey` securely
- [ ] 5 unit tests passing

---

# Phase 5: Lottery Module

## Step 13: Lottery Data Layer

**Priority:** High  
**Effort:** Medium (2-3 days)  
**Dependencies:** None (new feature)

### 🌿 Git Control

```bash
git checkout main
git pull origin main
git checkout -b feature/lottery-data-layer

git commit -m "feat(lottery): add LotteryGame domain model"
git commit -m "feat(lottery): add LotteryTransaction domain model"
git commit -m "feat(lottery): add LotteryRepository interface"
git commit -m "feat(lottery): implement FakeLotteryRepository"
git commit -m "feat(lottery): add LotteryModule DI configuration"
git commit -m "test(lottery): add LotteryRepositoryTest"
git commit -m "docs: update REMEDIATION_CHECKLIST lottery data layer"
```

### 📁 Files to Create/Modify

```
shared/src/commonMain/kotlin/com/unisight/gropos/
└── features/lottery/
    ├── domain/
    │   ├── model/
    │   │   ├── LotteryGame.kt             # NEW: Scratcher, Draw game models
    │   │   ├── LotteryTransaction.kt      # NEW: Sale, Payout records
    │   │   ├── LotteryPayout.kt           # NEW: Payout tiers, W-2G
    │   │   └── LotteryReport.kt           # NEW: Daily summary model
    │   ├── repository/
    │   │   └── LotteryRepository.kt       # NEW: Interface
    │   └── service/
    │       ├── LotteryService.kt          # NEW: Business logic
    │       └── PayoutTierCalculator.kt    # NEW: Tier 1/2/3 logic
    └── data/
        └── FakeLotteryRepository.kt       # NEW: Development data
```

### ✅ Testing Requirements

```kotlin
class LotteryServiceTest {
    @Test fun `calculatePayoutTier returns Tier1 for under 50`()
    @Test fun `calculatePayoutTier returns Tier2 for 50-599`()
    @Test fun `calculatePayoutTier returns Tier3 for 600 plus`()
    @Test fun `requiresW2G returns true for Tier3`()
}

class LotteryRepositoryTest {
    @Test fun `getGames returns active lottery games`()
    @Test fun `recordSale creates sale transaction`()
    @Test fun `recordPayout creates payout transaction`()
}
```

### 📋 Acceptance Criteria

- [ ] `LotteryGame` model with game type, price, prize info
- [ ] `LotteryTransaction` with sale/payout type distinction
- [ ] `PayoutTierCalculator` correctly categorizes $0-49.99, $50-599.99, $600+
- [ ] `FakeLotteryRepository` seeded with sample games
- [ ] `LotteryModule` registered in `AppModule`
- [ ] 8+ unit tests passing

---

## Step 14: Lottery Sales Screen

**Priority:** High  
**Effort:** Medium (2-3 days)  
**Dependencies:** Step 13

### 🌿 Git Control

```bash
git checkout main
git pull origin main
git checkout -b feature/lottery-sales-screen

git commit -m "feat(lottery): add LotterySaleScreen with game grid"
git commit -m "feat(lottery): add LotterySaleViewModel with cart logic"
git commit -m "feat(lottery): add LotterySaleUiState"
git commit -m "feat(lottery): add age verification on entry"
git commit -m "feat(lottery): wire Lotto Pay button in Functions Menu"
git commit -m "test(lottery): add LotterySaleViewModelTest"
git commit -m "docs: update REMEDIATION_CHECKLIST lottery sales"
```

### 📁 Files to Create/Modify

```
shared/src/commonMain/kotlin/com/unisight/gropos/
└── features/lottery/presentation/
    ├── sale/
    │   ├── LotterySaleScreen.kt           # NEW: Voyager screen
    │   ├── LotterySaleContent.kt          # NEW: UI composables
    │   ├── LotterySaleViewModel.kt        # NEW: State management
    │   └── LotterySaleUiState.kt          # NEW: UI state
    └── components/
        ├── LotteryGameCard.kt             # NEW: Game display card
        └── LotterySummaryPanel.kt         # NEW: Sale total panel
```

### ✅ Testing Requirements

```kotlin
class LotterySaleViewModelTest {
    @Test fun `shows age verification on entry`()
    @Test fun `adds game to cart`()
    @Test fun `removes game from cart`()
    @Test fun `calculates total correctly`()
    @Test fun `rejects non-cash payment`()
    @Test fun `completes sale and prints receipt`()
}
```

### 📋 Acceptance Criteria

- [ ] Age verification dialog on lottery mode entry
- [ ] Game grid with scratcher denominations
- [ ] Cash-only payment enforcement
- [ ] Sale receipt printing
- [ ] "Lotto Pay" button in Functions Menu navigates to screen
- [ ] Back button returns to Checkout
- [ ] 6+ unit tests passing

---

## Step 15: Lottery Payout Screen

**Priority:** High  
**Effort:** Medium (2-3 days)  
**Dependencies:** Step 13

### 🌿 Git Control

```bash
git checkout main
git pull origin main
git checkout -b feature/lottery-payout-screen

git commit -m "feat(lottery): add LotteryPayoutScreen"
git commit -m "feat(lottery): add LotteryPayoutViewModel with tier logic"
git commit -m "feat(lottery): add manager approval for Tier 3"
git commit -m "feat(lottery): add W-2G tax form generation"
git commit -m "test(lottery): add LotteryPayoutViewModelTest"
git commit -m "docs: update REMEDIATION_CHECKLIST lottery payouts"
```

### 📁 Files to Create/Modify

```
shared/src/commonMain/kotlin/com/unisight/gropos/
└── features/lottery/presentation/
    └── payout/
        ├── LotteryPayoutScreen.kt         # NEW: Voyager screen
        ├── LotteryPayoutContent.kt        # NEW: UI composables
        ├── LotteryPayoutViewModel.kt      # NEW: State management
        ├── LotteryPayoutUiState.kt        # NEW: UI state
        └── components/
            ├── PayoutTierBadge.kt         # NEW: Tier indicator
            └── W2GFormDialog.kt           # NEW: Tax form entry
```

### ✅ Testing Requirements

```kotlin
class LotteryPayoutViewModelTest {
    @Test fun `Tier 1 payout requires no approval`()
    @Test fun `Tier 2 payout is logged`()
    @Test fun `Tier 3 payout requires manager approval`()
    @Test fun `Tier 3 payout shows W-2G form`()
    @Test fun `validates SSN format for W-2G`()
    @Test fun `prints payout receipt`()
}
```

### 📋 Acceptance Criteria

- [ ] Amount entry via TenKey
- [ ] Automatic tier calculation and display
- [ ] Tier 1 ($0-49.99): Cashier processes, no approval
- [ ] Tier 2 ($50-599.99): Cashier processes, logged
- [ ] Tier 3 ($600+): Manager approval + W-2G form
- [ ] W-2G form collects SSN, name, address
- [ ] Payout receipt printing
- [ ] 6+ unit tests passing

---

## Step 16: Lottery Reports

**Priority:** Medium  
**Effort:** Low (1-2 days)  
**Dependencies:** Steps 14-15

### 🌿 Git Control

```bash
git checkout main
git pull origin main
git checkout -b feature/lottery-reports

git commit -m "feat(lottery): add LotteryReportScreen"
git commit -m "feat(lottery): add daily summary report"
git commit -m "feat(lottery): add payout detail report"
git commit -m "feat(lottery): add commission calculation"
git commit -m "test(lottery): add LotteryReportViewModelTest"
git commit -m "docs: update REMEDIATION_CHECKLIST lottery reports"
```

### 📁 Files to Create/Modify

```
shared/src/commonMain/kotlin/com/unisight/gropos/
└── features/lottery/presentation/
    └── report/
        ├── LotteryReportScreen.kt         # NEW
        ├── LotteryReportContent.kt        # NEW
        ├── LotteryReportViewModel.kt      # NEW
        └── LotteryReportUiState.kt        # NEW
```

### 📋 Acceptance Criteria

- [ ] Daily summary: sales, payouts, net, commission
- [ ] Payout detail list with tier breakdown
- [ ] Commission calculation (5-6% of sales)
- [ ] Print report to receipt printer
- [ ] 4+ unit tests passing

---

## Step 17: Lottery Integration & Polish

**Priority:** Medium  
**Effort:** Low (1 day)  
**Dependencies:** Steps 13-16

### 🌿 Git Control

```bash
git checkout main
git pull origin main
git checkout -b feature/lottery-integration

git commit -m "feat(lottery): add HasStateLottery branch setting"
git commit -m "feat(lottery): hide lottery when disabled"
git commit -m "feat(lottery): add keyboard shortcuts"
git commit -m "test(lottery): add integration tests"
git commit -m "docs: finalize lottery documentation"
```

### 📋 Acceptance Criteria

- [ ] `HasStateLottery` branch setting controls visibility
- [ ] Lottery hidden in states without lottery
- [ ] F9 keyboard shortcut opens lottery mode
- [ ] Full integration test: sale → payout → report
- [ ] All 22 lottery checklist items resolved

---

# Final Documentation Updates

After all steps complete, perform a final documentation sync:

### 🌿 Git Control

```bash
git checkout main
git pull origin main
git checkout -b docs/phase-4-complete

git commit -m "docs: update REMEDIATION_CHECKLIST Phase 4 complete"
git commit -m "docs: update PHASE_4_GAP_ANALYSIS final status"
git commit -m "docs: archive PHASE_4_IMPLEMENTATION_PLAN as complete"
```

### Updates Required

1. **REMEDIATION_CHECKLIST.md**:
   - All Phase 4 items marked complete
   - Summary statistics updated
   - Status line: "Phase 4 Complete"

2. **PHASE_4_GAP_ANALYSIS.md**:
   - All success criteria checked
   - Status: "Phase 4 Complete"

3. **CHANGELOG.md**:
   - Create version tag `[v1.1.0]` - Phase 4: Production Ready

4. **README.md**:
   - Update features list
   - Add hardware requirements section

---

# Quality Gates

Before merging any PR, ensure:

| Gate | Requirement |
|------|-------------|
| **Build** | `./gradlew build` passes |
| **Unit Tests** | `./gradlew testDebugUnitTest` passes |
| **Lint** | No new lint errors |
| **Coverage** | New code has 80%+ coverage |
| **Docs** | CHANGELOG.md updated |
| **Review** | At least 1 approval |

---

# Timeline Summary

| Phase | Steps | Duration | Cumulative |
|-------|-------|----------|------------|
| 4.5 Production Hardening | 7-9 | 2-3 weeks | 2-3 weeks |
| 4.6 API Completeness | 10-12 | 1 week | 3-4 weeks |
| 5 Lottery Module | 13-17 | 2 weeks | 5-6 weeks |

---

*Created: January 2, 2026*
*Related: [REMEDIATION_CHECKLIST.md](./REMEDIATION_CHECKLIST.md), [PHASE_4_GAP_ANALYSIS.md](./PHASE_4_GAP_ANALYSIS.md)*

