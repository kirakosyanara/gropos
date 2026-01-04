# Hardcoded Values Security Audit

**Date:** 2026-01-03  
**Auditor:** Security Audit Agent  
**Branch:** `fix/remove-hardcoded-values`  
**Scope:** `src/commonMain`, `src/androidMain`, `src/desktopMain` (excluding `src/commonTest`)

---

## Executive Summary

This forensic audit identified **37 instances** of hardcoded values that must be remediated before production deployment. The findings are categorized into three severity levels:

- 🔴 **HIGH (14 instances):** Authentication bypasses, hardcoded PINs, and entity IDs that break production integration
- 🟡 **MEDIUM (18 instances):** Fake repositories injected in production DI, simulated services used in main source sets
- 🟢 **LOW (5 instances):** Development delays and placeholder logic with TODO markers

---

## 🔴 HIGH SEVERITY FINDINGS

### H1. Hardcoded PIN Authentication Bypass

| File | Line | Value Found | Impact | Proposed Fix |
|:-----|:-----|:------------|:-------|:-------------|
| `features/auth/presentation/LockViewModel.kt` | 191 | `pin == "1234"` | Lock screen bypassed with magic PIN | Use `employeeRepository.verifyPin()` or backend API |
| `features/cashier/data/FakeEmployeeRepository.kt` | 91 | `VALID_PIN = "1234"` | All employees share one PIN | Replace with API call to `POST /employee/unlock` |
| `features/auth/data/FakeAuthRepository.kt` | 60 | `VALID_PIN = "1234"` | Authentication bypassed | Replace with real API auth flow |
| `core/security/ManagerApprovalService.kt` | 123 | `9999 -> pin == "1234"` | Manager approval bypassed | Integrate with backend manager verification |
| `core/security/ManagerApprovalService.kt` | 124 | `9998 -> pin == "5678"` | Manager approval bypassed | Integrate with backend manager verification |

### H2. Hardcoded Employee/Manager IDs

| File | Line | Value Found | Impact | Proposed Fix |
|:-----|:-----|:------------|:-------|:-------------|
| `features/cashier/data/FakeEmployeeRepository.kt` | 30 | `id = 9999` (Manager) | Wrong employee attributed | Get from `GET /employee/cashiers` API |
| `features/cashier/data/FakeEmployeeRepository.kt` | 39 | `id = 9998` (Supervisor) | Wrong employee attributed | Get from API |
| `features/auth/data/FakeAuthRepository.kt` | 87 | `id = "9999"` (Manager) | Auth maps to wrong user | Get from login response |
| `features/auth/data/FakeAuthRepository.kt` | 102 | `id = "9998"` (Supervisor) | Auth maps to wrong user | Get from login response |
| `core/security/ManagerApprovalService.kt` | 208 | `id = 9999` (John Smith) | Fake manager list | Get from `GET /employee/managers` API |
| `core/security/ManagerApprovalService.kt` | 216 | `id = 9998` (Jane Doe) | Fake manager list | Get from API |
| `features/auth/data/SimulatedNfcScanner.kt` | 35 | `MANAGER_BADGE_TOKEN = "9999"` | Simulated badge bypass | Remove; use real NFC hardware |

### H3. Hardcoded Station/Branch IDs

| File | Line | Value Found | Impact | Proposed Fix |
|:-----|:-----|:------------|:-------|:-------------|
| `features/checkout/presentation/CheckoutViewModel.kt` | 922-923 | `stationId = 1, branchId = 1` | Transactions attributed to wrong location | Inject `DeviceService.deviceInfo` |
| `core/security/ManagerApprovalService.kt` | 156-157 | `branchId = 1, stationId = 1` | Audit log has wrong location | Inject `DeviceService.deviceInfo` |
| `features/returns/domain/service/PullbackService.kt` | 263-265 | `branchId = 1, stationId = 1, employeeId = 1` | Returns attributed to wrong entity | Inject `DeviceService` + `SessionManager` |
| `features/transaction/data/dto/LegacyTransactionDto.kt` | 77 | `stationId = 1` | Legacy transactions lose station | Parse from API or use device info |
| `core/device/DeviceInfo.kt` | 141-145 | `"DEV-001", stationId = 1, branchId = 1` | Default overwrites real config | Remove defaults; require registration |
| `features/device/presentation/RegistrationViewModel.kt` | 159-163 | `branchId = 1, dev_api_key_*` | Development bypass | Remove; force real API registration |
| `features/device/data/FakeDeviceRepository.kt` | 111 | `dev_api_key_*` | Fake API key | Remove fake activation entirely |

---

## 🟡 MEDIUM SEVERITY FINDINGS

### M1. Fake Repositories Injected in Production DI

| File | Line | Fake Used | Production Impact | Proposed Fix |
|:-----|:-----|:----------|:------------------|:-------------|
| `core/di/AuthModule.kt` | 42 | `FakeAuthRepository` | No real authentication | Bind `RemoteAuthRepository` |
| `core/di/AuthModule.kt` | 46 | `FakeEmployeeRepository` | No real employee data | Bind `RemoteEmployeeRepository` |
| `core/di/AuthModule.kt` | 56 | `SimulatedNfcScanner` | NFC always returns fake badge | Bind platform-specific scanner |
| `core/di/CheckoutModule.kt` | 59 | `FakeScannerRepository` | Barcode scans don't work | Bind platform-specific scanner |
| `core/di/LotteryModule.kt` | 44 | `FakeLotteryRepository` | Lottery sales not persisted | Bind `RemoteLotteryRepository` |
| `androidMain/.../HardwareModule.kt` | 106 | `FakeScannerRepository` | Android scans don't work | Use `SunmiHardwareScanner` or `CameraBarcodeScanner` |
| `desktopMain/.../HardwareModule.kt` | 122 | `FakeScannerRepository` | Desktop scans don't work | Use `DesktopSerialScanner` |

### M2. Direct Cast to Fake in Production Code

| File | Line | Code | Impact | Proposed Fix |
|:-----|:-----|:-----|:-------|:-------------|
| `features/device/presentation/RegistrationViewModel.kt` | 140 | `deviceRepository as? FakeDeviceRepository` | Breaks if real repo injected | Use interface methods only |

### M3. Simulated Services in Main Source Sets

| File | Service | Impact | Proposed Fix |
|:-----|:--------|:-------|:-------------|
| `core/hardware/printer/SimulatedPrinterService.kt` | Printer | No real printing | Move to `commonTest` or behind interface |
| `core/hardware/scale/SimulatedScaleService.kt` | Scale | No real weighing | Move to `commonTest` or behind interface |
| `features/payment/data/SimulatedPaymentTerminal.kt` | Payment | No real payments | Move to `commonTest` or behind interface |
| `features/auth/data/SimulatedNfcScanner.kt` | NFC | No real badge scan | Move to `commonTest` |
| `features/payment/domain/service/PaymentService.kt` | Payment | Simulated delays/auth codes | Replace with real terminal integration |
| `core/sync/HeartbeatService.kt` | Heartbeat | `FakeHeartbeatService` class | Keep for tests but don't bind in prod |
| `core/auth/TokenRefreshManager.kt` | Token | `FakeTokenRefreshManager` class | Keep for tests but don't bind in prod |
| `core/device/PreAssignedEmployeeDetector.kt` | Employee | `SimulatedPreAssignedEmployeeDetector` | Keep for tests but don't bind in prod |

### M4. Hardcoded Simulated Values in Payment Logic

| File | Line | Value | Impact | Proposed Fix |
|:-----|:-----|:------|:-------|:-------------|
| `features/payment/domain/service/PaymentService.kt` | 130 | `"AUTH${random}"` | Fake auth codes | Use terminal response |
| `features/payment/domain/service/PaymentService.kt` | 133 | `"${(1000..9999).random()}"` | Fake last four digits | Use terminal response |
| `features/payment/data/SimulatedPaymentTerminal.kt` | 69 | `lastFour = "1234"` | Fake card data | Use real terminal |

---

## 🟢 LOW SEVERITY FINDINGS

### L1. Development Delays

| File | Line | Code | Proposed Fix |
|:-----|:-----|:-----|:-------------|
| `features/device/presentation/RegistrationViewModel.kt` | 137 | `delay(500)` | Remove or make configurable |
| `features/device/presentation/ui/RegistrationScreen.kt` | 43 | `delay(1000)` | Remove UX delay |
| Various `Fake*Repository` files | Multiple | `delay(200-500)` | Acceptable in fakes, but fakes shouldn't be in prod |

### L2. TODO Markers (For Tracking)

| File | Line | TODO |
|:-----|:-----|:-----|
| `LockViewModel.kt` | 189 | "Replace with actual API verification" |
| `NetworkModule.kt` | 40 | "Replace InMemoryTokenStorage with platform-specific" |
| `CheckoutModule.kt` | 57 | "Replace with actual hardware integration" |
| `AuthModule.kt` | 41-55 | Multiple "Replace with real implementation" |

---

## Remediation Priority

### Phase 1: Critical Security (MUST FIX)
1. **H1** - Remove all hardcoded PINs (`"1234"`, `"5678"`)
2. **H2** - Remove fake employee IDs (`9999`, `9998`)
3. **H3** - Replace hardcoded station/branch IDs with `DeviceService` injection

### Phase 2: DI Module Cleanup (MUST FIX)
4. **M1** - Replace Fake repositories with Remote implementations in DI modules
5. **M2** - Remove cast to `FakeDeviceRepository`

### Phase 3: Simulated Services (Should Fix)
6. **M3** - Move simulated services to `commonTest` or configure via build flags
7. **M4** - Replace simulated payment values with terminal integration

### Phase 4: Low Priority
8. **L1/L2** - Remove development delays and update TODOs

---

## Files Requiring Modification

```
HIGH PRIORITY:
├── shared/src/commonMain/kotlin/com/unisight/gropos/
│   ├── features/auth/presentation/LockViewModel.kt
│   ├── features/auth/data/FakeAuthRepository.kt
│   ├── features/cashier/data/FakeEmployeeRepository.kt
│   ├── core/security/ManagerApprovalService.kt
│   ├── core/device/DeviceInfo.kt
│   ├── features/checkout/presentation/CheckoutViewModel.kt
│   ├── features/returns/domain/service/PullbackService.kt
│   ├── features/device/presentation/RegistrationViewModel.kt
│   ├── features/device/data/FakeDeviceRepository.kt
│   └── features/transaction/data/dto/LegacyTransactionDto.kt
│
MEDIUM PRIORITY:
├── core/di/
│   ├── AuthModule.kt
│   ├── CheckoutModule.kt
│   └── LotteryModule.kt
│
├── shared/src/androidMain/kotlin/.../core/di/HardwareModule.kt
└── shared/src/desktopMain/kotlin/.../core/di/HardwareModule.kt
```

---

## Verification Checklist

After remediation, verify:

- [ ] Application connects to DEV environment successfully
- [ ] Login uses real API authentication (no "1234" PIN)
- [ ] Employee list comes from `/employee/cashiers` endpoint
- [ ] Station/Branch IDs match device registration
- [ ] Manager approval calls backend verification
- [ ] Unit tests still pass (mocks should provide test data)
- [ ] Barcode scanning works with real hardware or camera

---

## Appendix: Grep Commands Used

```bash
# PINs and passwords
grep -r '"1234"\|"0000"\|"admin"\|"password"' shared/src/*/

# Hardcoded IDs
grep -r 'stationId\s*=\s*1[^0-9]\|branchId\s*=\s*1[^0-9]' shared/src/*/

# Fake repositories in main
grep -r 'Fake.*Repository' shared/src/commonMain/ shared/src/androidMain/ shared/src/desktopMain/

# Walking skeleton markers
grep -ri 'Walking Skeleton\|hardcoded\|magic number' shared/src/*/
```

