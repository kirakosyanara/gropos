# Lock Screen & Till Flow Remediation

> **Branch:** `fix/lock-screen-till-remediation`  
> **Status:** 🟡 IN PROGRESS  
> **Last Updated:** 2026-01-04  
> **Source of Truth:** `docs/development-plan/features/LOCK_SCREEN_AND_CASHIER_LOGIN.md`

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Gap Analysis Summary](#gap-analysis-summary)
3. [Implementation Checklist](#implementation-checklist)
4. [Detailed Fix Specifications](#detailed-fix-specifications)
5. [Files Affected](#files-affected)
6. [Data Models Required](#data-models-required)
7. [Testing Requirements](#testing-requirements)
8. [Progress Log](#progress-log)

---

## Executive Summary

This document tracks the remediation of the **Lock Screen**, **Cashier Login**, and **Till Assignment** workflows to align with the Source of Truth documentation in `LOCK_SCREEN_AND_CASHIER_LOGIN.md`.

### Key Issues Identified

| Category | Severity | Count |
|----------|----------|-------|
| Wrong API Endpoints | 🔴 Critical | 3 |
| Missing API Endpoints | 🔴 Critical | 4 |
| Missing Business Logic | 🔴 Critical | 3 |
| Hardcoded Values | 🔴 Critical | 1 |
| Missing UI Components | 🟡 Medium | 3 |
| Missing Data Models | 🟡 Medium | 5 |

### Compliance Score

| Component | Before | Target |
|-----------|--------|--------|
| LoginScreen | 40% | 100% |
| LockScreen | 20% | 100% |
| TillRepository | 50% | 100% |
| Environment Switching | 90% | 100% |

---

## Gap Analysis Summary

### 1. API Endpoint Issues

#### 1.1 Wrong Endpoints

| # | Current Endpoint | Correct Endpoint | File | Status |
|---|------------------|------------------|------|--------|
| E1 | `/employee/cashiers` | `/api/Employee/GetCashierEmployees` | `RemoteEmployeeRepository.kt` | ✅ FIXED |
| E2 | `/till` | `/api/account/GetTillAccountList` | `RemoteTillRepository.kt` | ✅ FIXED |
| E3 | `/employee/login` | `/api/Employee/Login` | `ApiAuthService.kt` | ✅ FIXED |

#### 1.2 Missing Endpoints

| # | Endpoint | Purpose | File to Modify | Status |
|---|----------|---------|----------------|--------|
| M1 | `GET /api/v1/devices/current` | Get device info with claimed employee | NEW: `DeviceApi.kt` | ⬜ TODO |
| M2 | `POST /api/Employee/VerifyPassword` | Verify PIN on lock screen | `ApiAuthService.kt` | ⬜ TODO |
| M3 | `POST /api/Employee/LockDevice` | Report lock/unlock events | `ApiAuthService.kt` | ⬜ TODO |
| M4 | `POST /api/Employee/LogoutWithEndOfShift` | End-of-shift logout | `ApiAuthService.kt` | ⬜ TODO |

### 2. Business Logic Issues

| # | Issue | Description | File | Status |
|---|-------|-------------|------|--------|
| L1 | Station Claiming | Not checking `deviceInfo.employeeId` to pre-select cashier | `LoginViewModel.kt` | ⬜ TODO |
| L2 | Hardcoded PIN | Lock screen uses `"1234"` instead of API verification | `LockViewModel.kt` | ⬜ TODO |
| L3 | Session Data | LockViewModel uses placeholder instead of real employee | `LockViewModel.kt` | ⬜ TODO |
| L4 | Manager Approval | Sign-out flow missing permission check | `LockViewModel.kt` | ⬜ TODO |
| L5 | Lock Event Reporting | Lock/unlock events not sent to backend | `LockViewModel.kt` | ⬜ TODO |

### 3. Data Model Issues

| # | Model | Issue | Status |
|---|-------|-------|--------|
| D1 | `CurrentDeviceInfoDto` | Missing backend-format device info with `employeeId`, `locationAccountId` | ✅ DONE |
| D2 | `CashierLoginRequest` | Current `LoginRequest` missing required fields | ✅ DONE |
| D3 | `DeviceLockRequest` | Updated to use `DeviceEventType` enum | ✅ DONE |
| D4 | `DeviceEventType` | Enum with `Locked(4)`, `Unlocked(5)`, `AutoLocked(6)` | ✅ DONE |
| D5 | `LocationAccountDto` / `GridDataOfLocationAccountListViewModel` | Till list response format | ✅ DONE |

### 4. UI Component Issues

| # | Component | Issue | Status |
|---|-----------|-------|--------|
| U1 | `LogoutOptionsDialog` | Not implemented | ⬜ TODO |
| U2 | `ManagerApprovalDialog` | Referenced but not implemented | ⬜ TODO |
| U3 | Scanner mode in `TillSelectionDialog` | Not implemented | ⬜ TODO |

### 5. Dependency Injection Issues

| # | Issue | Description | Status |
|---|-------|-------------|--------|
| DI1 | `LockViewModel` | No dependencies injected | ⬜ TODO |

---

## Implementation Checklist

### Phase 1: Git Setup
- [x] **1.1** Create branch `fix/lock-screen-till-remediation` ✅
- [x] **1.2** Verify clean working directory ✅

### Phase 2: Data Models (Commit: `fix(auth): add required DTOs for lock screen and till flow`)
- [x] **2.1** Create `CurrentDeviceInfoDto` with backend field structure ✅
- [x] **2.2** Create `CashierLoginRequest` with all required fields ✅
- [x] **2.3** Update `DeviceLockRequest` model to use enum ✅
- [x] **2.4** Create `DeviceEventType` enum ✅
- [x] **2.5** Create `LocationAccountDto` and `GridDataOfLocationAccountListViewModel` ✅
- [x] **2.6** Create `VerifyPasswordRequest` model ✅

### Phase 3: Fix Existing Endpoints (Commit: `fix(auth): correct API endpoint paths`)
- [x] **3.1** Fix `RemoteEmployeeRepository` endpoint: `/employee/cashiers` → `/api/Employee/GetCashierEmployees` ✅
- [x] **3.2** Fix `RemoteTillRepository` endpoint: `/till` → `/api/account/GetTillAccountList` ✅
- [x] **3.3** Fix `ApiAuthService` login endpoint: `/employee/login` → `/api/Employee/Login` ✅
- [ ] **3.4** Update login request to use `CashierLoginRequest` (deferred to Phase 5 - needs till context)

### Phase 4: Add Missing Endpoints (Commit: `feat(auth): add device current and lock device endpoints`)
- [ ] **4.1** Create `DeviceApi` interface
- [ ] **4.2** Implement `getCurrentDevice()` for `GET /api/v1/devices/current`
- [ ] **4.3** Add `verifyPassword()` to `ApiAuthService` for `POST /api/Employee/VerifyPassword`
- [ ] **4.4** Add `lockDevice()` to `ApiAuthService` for `POST /api/Employee/LockDevice`
- [ ] **4.5** Add `logoutWithEndOfShift()` to `ApiAuthService`

### Phase 5: Station Claiming Logic (Commit: `feat(auth): implement station claiming on login`)
- [ ] **5.1** Inject `DeviceApi` into `LoginViewModel`
- [ ] **5.2** Call `getCurrentDevice()` in `loadEmployees()`
- [ ] **5.3** Check `deviceInfo.employeeId` for claimed status
- [ ] **5.4** Auto-select claimed employee and populate till assignment
- [ ] **5.5** Update `LoginUiState` to reflect claimed status

### Phase 6: Lock Screen Fixes (Commit: `fix(auth): implement proper lock screen verification`)
- [ ] **6.1** Inject dependencies into `LockViewModel` (employeeApi, appState, sessionState)
- [ ] **6.2** Update `AuthModule.kt` to wire dependencies
- [ ] **6.3** Replace hardcoded PIN "1234" with `verifyPassword()` API call
- [ ] **6.4** Get actual employee/station data from `AppState`
- [ ] **6.5** Report lock events via `lockDevice()` when locking
- [ ] **6.6** Report unlock events via `lockDevice()` when unlocking

### Phase 7: Manager Approval Flow (Commit: `feat(auth): implement manager approval for sign out`)
- [ ] **7.1** Create `LogoutOptionsDialog` composable
- [ ] **7.2** Create `ManagerApprovalDialog` composable
- [ ] **7.3** Implement manager PIN verification in dialog
- [ ] **7.4** Add permission check for `GrowPOS.Store.Force Sign Out`
- [ ] **7.5** Connect dialogs to `LockViewModel` sign-out flow
- [ ] **7.6** Implement `logoutWithEndOfShift()` call

### Phase 8: Till Selection Enhancements (Commit: `feat(auth): add scanner mode for till selection`)
- [ ] **8.1** Update `TillSelectionDialog` with scanner mode support
- [ ] **8.2** Integrate with `HardwareManager.getBarcodeScanner()`
- [ ] **8.3** Add till barcode validation logic

### Phase 9: Testing (Commit: `test(auth): add tests for lock screen and login flows`)
- [ ] **9.1** Update `LoginViewModelTest` with station claiming tests
- [ ] **9.2** Update `LockViewModelTest` with PIN verification tests
- [ ] **9.3** Add tests for manager approval flow
- [ ] **9.4** Add tests for environment switching (dev/staging/prod)

### Phase 10: Documentation & Cleanup
- [ ] **10.1** Update CHANGELOG.md
- [ ] **10.2** Mark this document as COMPLETE
- [ ] **10.3** Create PR description

---

## Detailed Fix Specifications

### FIX-E1: Employee Cashiers Endpoint

**File:** `shared/src/commonMain/kotlin/com/unisight/gropos/features/cashier/data/RemoteEmployeeRepository.kt`

**Current (Line 151):**
```kotlin
private const val ENDPOINT_CASHIERS = "/employee/cashiers"
```

**Target:**
```kotlin
private const val ENDPOINT_CASHIERS = "/api/Employee/GetCashierEmployees"
```

**Verification:** GET request returns list of `EmployeeListViewModel` objects

---

### FIX-E2: Till Account List Endpoint

**File:** `shared/src/commonMain/kotlin/com/unisight/gropos/features/cashier/data/RemoteTillRepository.kt`

**Current (Line 39):**
```kotlin
private const val ENDPOINT_TILLS = "/till"
```

**Target:**
```kotlin
private const val ENDPOINT_TILLS = "/api/account/GetTillAccountList"
```

**Response Model Update Required:**
```kotlin
@Serializable
data class GridDataOfLocationAccountListViewModel(
    val rows: List<LocationAccountDto>? = null,
    val totalCount: Int? = null
)

@Serializable
data class LocationAccountDto(
    val locationAccountId: Int,
    val accountName: String?,
    val assignedEmployeeId: Int? = null,
    val employeeName: String? = null,
    val currentBalance: Double? = null
)
```

---

### FIX-E3: Login Endpoint and Request

**File:** `shared/src/commonMain/kotlin/com/unisight/gropos/core/auth/ApiAuthService.kt`

**Current (Line 155):**
```kotlin
private const val ENDPOINT_LOGIN = "/employee/login"
```

**Target:**
```kotlin
private const val ENDPOINT_LOGIN = "/api/Employee/Login"
```

**Request Model Update:**
```kotlin
@Serializable
data class CashierLoginRequest(
    val userName: String,
    val password: String,
    val clientName: String = "device",
    val authenticationKey: String? = null,
    val locationAccountId: Int,  // Till ID (REQUIRED)
    val branchId: Int,
    val deviceId: Int
)
```

---

### FIX-M1: Device Current Endpoint

**New File:** `shared/src/commonMain/kotlin/com/unisight/gropos/features/device/data/DeviceApi.kt`

```kotlin
interface DeviceApi {
    /**
     * Get current device info including claimed employee.
     * 
     * Per LOCK_SCREEN_AND_CASHIER_LOGIN.md:
     * - Returns employeeId if station is claimed
     * - Returns locationAccountId if till is assigned
     */
    suspend fun getCurrentDevice(): Result<DeviceInfoDto>
}

class RemoteDeviceApi(
    private val apiClient: ApiClient
) : DeviceApi {
    
    companion object {
        private const val ENDPOINT_DEVICE_CURRENT = "/api/v1/devices/current"
    }
    
    override suspend fun getCurrentDevice(): Result<DeviceInfoDto> {
        return apiClient.authenticatedRequest {
            get(ENDPOINT_DEVICE_CURRENT)
        }
    }
}
```

**DTO:**
```kotlin
@Serializable
data class DeviceInfoDto(
    val id: Int,
    val branchId: Int,
    val branch: String? = null,
    val name: String? = null,
    val location: String? = null,
    val employeeId: Int? = null,        // ID of claimed employee (null if free)
    val employee: String? = null,        // Name of claimed employee
    val locationAccountId: Int? = null,  // Assigned till account ID
    val locationAccount: String? = null, // Till account name
    val lastHeartbeat: String? = null
)
```

---

### FIX-M2: Verify Password Endpoint

**File:** `shared/src/commonMain/kotlin/com/unisight/gropos/core/auth/ApiAuthService.kt`

**Add Method:**
```kotlin
companion object {
    // ... existing constants ...
    private const val ENDPOINT_VERIFY_PASSWORD = "/api/Employee/VerifyPassword"
}

/**
 * Verifies employee PIN on lock screen.
 * 
 * Per LOCK_SCREEN_AND_CASHIER_LOGIN.md:
 * - Used to unlock a locked station
 * - Does NOT refresh tokens (session remains active)
 */
suspend fun verifyPassword(request: VerifyPasswordRequest): Result<Boolean> {
    return apiClient.authenticatedRequest<Boolean> {
        post(ENDPOINT_VERIFY_PASSWORD) {
            setBody(request)
        }
    }
}
```

**Request Model:**
```kotlin
@Serializable
data class VerifyPasswordRequest(
    val userName: String,
    val password: String,
    val clientName: String = "device",
    val branchId: Int? = null,
    val deviceId: Int? = null
)
```

---

### FIX-M3: Lock Device Endpoint

**File:** `shared/src/commonMain/kotlin/com/unisight/gropos/core/auth/ApiAuthService.kt`

**Add Method:**
```kotlin
companion object {
    // ... existing constants ...
    private const val ENDPOINT_LOCK_DEVICE = "/api/Employee/LockDevice"
}

/**
 * Reports lock/unlock event to backend.
 * 
 * Per LOCK_SCREEN_AND_CASHIER_LOGIN.md:
 * - Locked(4): Manual lock
 * - Unlocked(5): Successful PIN verification
 * - AutoLocked(6): Inactivity timeout
 */
suspend fun lockDevice(request: DeviceLockRequest): Result<DeviceEventResponse> {
    return apiClient.authenticatedRequest<DeviceEventResponse> {
        post(ENDPOINT_LOCK_DEVICE) {
            setBody(request)
        }
    }
}
```

**Models:**
```kotlin
@Serializable
data class DeviceLockRequest(
    val lockType: DeviceEventType
)

@Serializable
enum class DeviceEventType(val value: Int) {
    @SerialName("0") SignedIn(0),
    @SerialName("1") SignedOut(1),
    @SerialName("2") ClockedIn(2),
    @SerialName("3") ClockedOut(3),
    @SerialName("4") Locked(4),
    @SerialName("5") Unlocked(5),
    @SerialName("6") AutoLocked(6)
}

@Serializable
data class DeviceEventResponse(
    val success: Boolean,
    val eventId: Long? = null
)
```

---

### FIX-M4: Logout With End Of Shift

**File:** `shared/src/commonMain/kotlin/com/unisight/gropos/core/auth/ApiAuthService.kt`

**Add Method:**
```kotlin
companion object {
    // ... existing constants ...
    private const val ENDPOINT_LOGOUT_END_OF_SHIFT = "/api/Employee/LogoutWithEndOfShift"
}

/**
 * Logs out with end-of-shift flag.
 * 
 * Per LOCK_SCREEN_AND_CASHIER_LOGIN.md:
 * - Triggers till count prompt
 * - Opens cash drawer for counting
 */
suspend fun logoutWithEndOfShift(): Result<Unit> {
    return apiClient.authenticatedRequest<Unit> {
        post(ENDPOINT_LOGOUT_END_OF_SHIFT)
    }
}
```

---

### FIX-L1: Station Claiming Logic

**File:** `shared/src/commonMain/kotlin/com/unisight/gropos/features/auth/presentation/LoginViewModel.kt`

**Add to Constructor:**
```kotlin
class LoginViewModel(
    private val employeeRepository: EmployeeRepository,
    private val tillRepository: TillRepository,
    private val nfcScanner: NfcScanner,
    private val deviceApi: DeviceApi,  // NEW
    private val coroutineScope: CoroutineScope? = null
) : ScreenModel
```

**Update `loadEmployees()`:**
```kotlin
private fun loadEmployees() {
    scope.launch {
        _state.update { it.copy(isLoading = true, stage = LoginStage.LOADING) }
        
        // STEP 1: Get device info to check for claimed employee
        val deviceInfoResult = deviceApi.getCurrentDevice()
        val deviceInfo = deviceInfoResult.getOrNull()
        
        // STEP 2: Load employees
        employeeRepository.getEmployees()
            .onSuccess { employees ->
                val employeeUiModels = employees.map { it.toUiModel() }
                
                // STEP 3: Check if station is claimed
                if (deviceInfo?.employeeId != null) {
                    val claimedEmployee = employeeUiModels.find { 
                        it.id == deviceInfo.employeeId 
                    }
                    
                    if (claimedEmployee != null) {
                        // Pre-select claimed employee with till assignment
                        val employeeWithTill = claimedEmployee.copy(
                            assignedTillId = deviceInfo.locationAccountId
                        )
                        
                        _state.update {
                            it.copy(
                                employees = employeeUiModels,
                                selectedEmployee = employeeWithTill,
                                stage = LoginStage.PIN_ENTRY,
                                isLoading = false,
                                isStationClaimed = true,
                                currentTime = getCurrentTime()
                            )
                        }
                        return@onSuccess
                    }
                }
                
                // Station is FREE - show employee selection
                _state.update { 
                    it.copy(
                        employees = employeeUiModels,
                        stage = LoginStage.EMPLOYEE_SELECT,
                        isLoading = false,
                        isStationClaimed = false,
                        currentTime = getCurrentTime()
                    )
                }
            }
            .onFailure { error ->
                _state.update { 
                    it.copy(
                        errorMessage = error.message ?: "Failed to load employees",
                        isLoading = false,
                        stage = LoginStage.EMPLOYEE_SELECT
                    )
                }
            }
    }
}
```

**Add to `LoginUiState`:**
```kotlin
data class LoginUiState(
    // ... existing fields ...
    val isStationClaimed: Boolean = false,
)
```

---

### FIX-L2: Replace Hardcoded PIN

**File:** `shared/src/commonMain/kotlin/com/unisight/gropos/features/auth/presentation/LockViewModel.kt`

**Current (Lines 171-206):**
```kotlin
fun onVerify(): UnlockResult {
    // ...
    return if (pin == "1234") {  // ❌ HARDCODED
        UnlockResult.Success
    } else {
        UnlockResult.Error
    }
}
```

**Target:**
```kotlin
fun onVerify() {
    val pin = _state.value.pinInput
    val employee = appState.employee.value ?: run {
        _state.value = _state.value.copy(errorMessage = "No active session")
        return
    }
    
    if (pin.isEmpty()) {
        _state.value = _state.value.copy(errorMessage = "Please enter your PIN")
        return
    }
    
    if (pin.length < 4) {
        _state.value = _state.value.copy(
            errorMessage = "PIN must be at least 4 digits",
            pinInput = ""
        )
        return
    }
    
    _state.value = _state.value.copy(isVerifying = true)
    
    effectiveScope.launch {
        val request = VerifyPasswordRequest(
            userName = employee.email,
            password = pin,
            branchId = appState.branch.value?.id,
            deviceId = appState.branch.value?.stationId
        )
        
        authService.verifyPassword(request)
            .onSuccess { success ->
                if (success) {
                    // Report unlock event
                    authService.lockDevice(
                        DeviceLockRequest(lockType = DeviceEventType.Unlocked)
                    )
                    
                    // Restart inactivity timer
                    InactivityManager.start()
                    
                    _state.value = _state.value.copy(
                        isVerifying = false,
                        unlockSuccess = true
                    )
                } else {
                    _state.value = _state.value.copy(
                        isVerifying = false,
                        pinInput = "",
                        errorMessage = "Invalid PIN. Please try again."
                    )
                }
            }
            .onFailure { error ->
                _state.value = _state.value.copy(
                    isVerifying = false,
                    pinInput = "",
                    errorMessage = error.message ?: "Verification failed"
                )
            }
    }
}
```

---

### FIX-L3: Inject Real Session Data

**File:** `shared/src/commonMain/kotlin/com/unisight/gropos/features/auth/presentation/LockViewModel.kt`

**Update Constructor:**
```kotlin
class LockViewModel(
    private val authService: ApiAuthService,  // NEW
    private val appState: AppState,           // NEW
    private val sessionState: SessionState,   // NEW
    private val lockEventType: LockEventType = LockEventType.Inactivity,
    private val scope: CoroutineScope? = null
) : ScreenModel
```

**Update `initialize()`:**
```kotlin
private fun initialize() {
    val employee = appState.employee.value
    val branch = appState.branch.value
    val deviceInfo = appState.deviceInfo.value
    
    val lockType = when (lockEventType) {
        LockEventType.Inactivity -> LockType.AutoLocked
        LockEventType.Manual -> LockType.Locked
        LockEventType.Manager -> LockType.ManagerLocked
    }
    
    _state.value = _state.value.copy(
        stationName = deviceInfo?.name ?: branch?.name ?: "Unknown Station",
        employeeName = employee?.let { "${it.firstName} ${it.lastName}" } ?: "Unknown",
        employeeRole = employee?.role ?: "Cashier",
        employeeImageUrl = employee?.imageUrl,
        lockType = lockType
    )
    
    updateClock()
}
```

**Update `AuthModule.kt`:**
```kotlin
factory { 
    LockViewModel(
        authService = get(),
        appState = get(),
        sessionState = get()
    )
}
```

---

### FIX-DI1: LockViewModel Dependency Injection

**File:** `shared/src/commonMain/kotlin/com/unisight/gropos/core/di/AuthModule.kt`

**Current (Line 74):**
```kotlin
factory { LockViewModel() }
```

**Target:**
```kotlin
factory { 
    LockViewModel(
        authService = get(),
        appState = get(),
        sessionState = get()
    )
}
```

---

## Files Affected

### Files to Modify

| File | Changes |
|------|---------|
| `shared/src/commonMain/kotlin/com/unisight/gropos/features/cashier/data/RemoteEmployeeRepository.kt` | Fix endpoint path |
| `shared/src/commonMain/kotlin/com/unisight/gropos/features/cashier/data/RemoteTillRepository.kt` | Fix endpoint path, update response model |
| `shared/src/commonMain/kotlin/com/unisight/gropos/core/auth/ApiAuthService.kt` | Add verifyPassword, lockDevice, logoutWithEndOfShift |
| `shared/src/commonMain/kotlin/com/unisight/gropos/core/auth/AuthDtos.kt` | Add new DTOs |
| `shared/src/commonMain/kotlin/com/unisight/gropos/features/auth/presentation/LoginViewModel.kt` | Add device API, station claiming |
| `shared/src/commonMain/kotlin/com/unisight/gropos/features/auth/presentation/LockViewModel.kt` | Full refactor with API calls |
| `shared/src/commonMain/kotlin/com/unisight/gropos/features/auth/presentation/LoginUiState.kt` | Add `isStationClaimed` |
| `shared/src/commonMain/kotlin/com/unisight/gropos/features/auth/presentation/LockUiState.kt` | Add `unlockSuccess`, `employeeImageUrl` |
| `shared/src/commonMain/kotlin/com/unisight/gropos/core/di/AuthModule.kt` | Wire LockViewModel dependencies |
| `shared/src/commonMain/kotlin/com/unisight/gropos/features/cashier/data/dto/TillDto.kt` | Update to match API response |

### Files to Create

| File | Purpose |
|------|---------|
| `shared/src/commonMain/kotlin/com/unisight/gropos/features/device/data/DeviceApi.kt` | Device current endpoint |
| `shared/src/commonMain/kotlin/com/unisight/gropos/features/device/data/dto/DeviceInfoDto.kt` | Device info DTO |
| `shared/src/commonMain/kotlin/com/unisight/gropos/features/auth/presentation/ui/LogoutOptionsDialog.kt` | Logout options UI |
| `shared/src/commonMain/kotlin/com/unisight/gropos/features/auth/presentation/ui/ManagerApprovalDialog.kt` | Manager approval UI |

---

## Data Models Required

### 1. DeviceInfoDto (Backend Format)

```kotlin
@Serializable
data class DeviceInfoDto(
    val id: Int,
    val branchId: Int,
    val branch: String? = null,
    val name: String? = null,
    val location: String? = null,
    val employeeId: Int? = null,
    val employee: String? = null,
    val locationAccountId: Int? = null,
    val locationAccount: String? = null,
    val lastHeartbeat: String? = null
)
```

### 2. CashierLoginRequest

```kotlin
@Serializable
data class CashierLoginRequest(
    val userName: String,
    val password: String,
    val clientName: String = "device",
    val authenticationKey: String? = null,
    val locationAccountId: Int,
    val branchId: Int,
    val deviceId: Int
)
```

### 3. VerifyPasswordRequest

```kotlin
@Serializable
data class VerifyPasswordRequest(
    val userName: String,
    val password: String,
    val clientName: String = "device",
    val branchId: Int? = null,
    val deviceId: Int? = null
)
```

### 4. DeviceLockRequest & DeviceEventType

```kotlin
@Serializable
data class DeviceLockRequest(
    val lockType: DeviceEventType
)

@Serializable
enum class DeviceEventType(val value: Int) {
    @SerialName("0") SignedIn(0),
    @SerialName("1") SignedOut(1),
    @SerialName("2") ClockedIn(2),
    @SerialName("3") ClockedOut(3),
    @SerialName("4") Locked(4),
    @SerialName("5") Unlocked(5),
    @SerialName("6") AutoLocked(6)
}
```

### 5. LocationAccountListResponse

```kotlin
@Serializable
data class GridDataOfLocationAccountListViewModel(
    val rows: List<LocationAccountDto>? = null,
    val totalCount: Int? = null
)

@Serializable
data class LocationAccountDto(
    val locationAccountId: Int,
    val accountName: String? = null,
    val assignedEmployeeId: Int? = null,
    val employeeName: String? = null,
    val currentBalance: Double? = null
)
```

---

## Testing Requirements

### Unit Tests

| Test File | Test Cases |
|-----------|------------|
| `LoginViewModelTest.kt` | - `loadEmployees_withClaimedStation_preSelectsEmployee()` |
| | - `loadEmployees_withFreStation_showsEmployeeList()` |
| | - `submitPin_withAssignedTill_completesLogin()` |
| | - `submitPin_withoutTill_showsTillSelection()` |
| `LockViewModelTest.kt` | - `verifyPin_withValidPin_unlocksScreen()` |
| | - `verifyPin_withInvalidPin_showsError()` |
| | - `verifyPin_reportsUnlockEvent()` |
| | - `signOut_showsLogoutOptions()` |
| | - `signOut_withManagerApproval_logsOut()` |
| `RemoteEmployeeRepositoryTest.kt` | - `getEmployees_usesCorrectEndpoint()` |
| `RemoteTillRepositoryTest.kt` | - `getTills_usesCorrectEndpoint()` |

### Integration Tests

| Scenario | Steps |
|----------|-------|
| Station Claiming Flow | 1. Device has claimed employee<br>2. Open login screen<br>3. Verify employee is pre-selected<br>4. Enter PIN<br>5. Verify till is pre-assigned |
| Lock/Unlock Flow | 1. User is logged in<br>2. Trigger lock (F4 or timeout)<br>3. Verify lock event sent<br>4. Enter PIN<br>5. Verify unlock event sent |
| Manager Approval Flow | 1. User is locked<br>2. Click Sign Out<br>3. Verify options dialog shown<br>4. Select "End of Shift"<br>5. Verify manager approval required |

---

## Progress Log

| Date | Commit | Description | Status |
|------|--------|-------------|--------|
| 2026-01-03 | - | Document created | ✅ |
| 2026-01-04 | `fix(auth): D1-D5 - add required DTOs` | Added DeviceEventType, CashierLoginRequest, VerifyPasswordRequest, CurrentDeviceInfoDto, LocationAccountDto | ✅ |
| 2026-01-04 | `fix(auth): E1-E3 - correct API endpoints` | Fixed endpoints in RemoteEmployeeRepository, RemoteTillRepository, ApiAuthService | ✅ |
| | | | |

---

## Commit History (To Be Updated)

```
(commits will be logged here as they are made)
```

---

## Notes & Decisions

### Decision Log

| # | Decision | Rationale | Date |
|---|----------|-----------|------|
| 1 | Environment change requires restart | ApiClient is configured at startup; hot-swap adds complexity without significant benefit | 2026-01-03 |
| 2 | | | |

### Open Questions

| # | Question | Status | Resolution |
|---|----------|--------|------------|
| 1 | Does `/api/Employee/VerifyPassword` return boolean or object? | ⬜ OPEN | |
| 2 | Is `locationAccountId` required for login, or optional? | ⬜ OPEN | |

---

## Related Documents

- **Source of Truth:** `docs/development-plan/features/LOCK_SCREEN_AND_CASHIER_LOGIN.md`
- **API Reference:** `docs/development-plan/architecture/API.md`
- **Architecture:** `docs/development-plan/architecture/API_INTEGRATION.md`
- **Device Registration:** `docs/development-plan/features/DEVICE_REGISTRATION.md`

