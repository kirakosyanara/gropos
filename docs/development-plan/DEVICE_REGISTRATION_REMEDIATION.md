# Device Registration Flow - Remediation Plan

**Created:** January 3, 2026  
**Branch:** `fix/device-registration-flow`  
**Status:** 🟡 In Progress  
**Last Updated:** January 3, 2026

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Source of Truth](#2-source-of-truth)
3. [Gap Analysis Findings](#3-gap-analysis-findings)
4. [Implementation Checklist](#4-implementation-checklist)
5. [Detailed Code Changes](#5-detailed-code-changes)
6. [Testing Requirements](#6-testing-requirements)
7. [Commit Plan](#7-commit-plan)
8. [Rollback Plan](#8-rollback-plan)
9. [Change Log](#9-change-log)

---

## 1. Executive Summary

### Problem Statement

The current device registration implementation does not match the architectural requirements specified in `DEVICE_REGISTRATION.md`. The implementation uses incorrect API payloads, missing authentication headers, and has disabled status polling.

### Scope

| Component | File | Changes Required |
|-----------|------|------------------|
| DTOs | `DeviceDto.kt` | Request/Response payload alignment |
| Repository | `RemoteDeviceRepository.kt` | API calls, token handling |
| ViewModel | `RegistrationViewModel.kt` | Polling implementation |
| Domain Model | `DeviceInfo.kt` | Add missing states |
| Tests | `DeviceRegistrationFlowTest.kt` | New test file |

### Success Criteria

- [ ] QR registration request matches documented payload
- [ ] Status polling uses Bearer token authentication
- [ ] Polling runs every 10 seconds with proper timeout handling
- [ ] API key and branch info saved to SecureStorage
- [ ] All unit tests pass
- [ ] No regression in existing functionality

---

## 2. Source of Truth

**Primary Document:** `docs/development-plan/features/DEVICE_REGISTRATION.md`

### Key API Endpoints

| Endpoint | Method | Purpose | Auth |
|----------|--------|---------|------|
| `/device-registration/qr-registration` | POST | Request QR code | None (version header) |
| `/device-registration/device-status/{guid}` | GET | Poll status | Bearer `<accessToken>` |
| `/device-registration/heartbeat` | GET | Health check | `x-api-key` header |

### Expected Payloads

**QR Registration Request:**
```json
{
  "deviceType": 0
}
```

**QR Registration Response:**
```json
{
  "url": "https://admin.gropos.com/register/ABC123XYZ",
  "qrCodeImage": "iVBORw0KGgoAAAANSUhEUg...",
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "assignedGuid": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Status Response (Registered):**
```json
{
  "deviceStatus": "Registered",
  "apiKey": "<DEVICE_API_KEY>",
  "branchId": 42,
  "branch": "Main Street Store"
}
```

---

## 3. Gap Analysis Findings

### 🔴 Critical Issues

| # | Issue | Current | Expected | Impact |
|---|-------|---------|----------|--------|
| C1 | Wrong request payload | `deviceName`, `platform` | `deviceType: Int` | API will reject requests |
| C2 | Missing Bearer token | No auth on status polling | `Authorization: Bearer <token>` | 401 Unauthorized |
| C3 | Polling disabled | Commented out | 10s interval, 10min timeout | Registration never completes |
| C4 | Wrong stationId source | Expects from API | Use `assignedGuid` | Device identity mismatch |

### 🟡 High Priority Issues

| # | Issue | Current | Expected | Impact |
|---|-------|---------|----------|--------|
| H1 | Missing states | 5 states | 7 states (add LOADING, TIMEOUT) | UX gaps |
| H2 | Heartbeat wrong | POST with body | GET, no body | Heartbeat fails |
| H3 | accessToken not stored | Captured but unused | Store for polling | Polling auth fails |
| H4 | Missing version header | Not set | `version: 1.0` | Potential API issues |

### ✅ Acceptable Divergence

| Item | Reason |
|------|--------|
| SecureStorage vs Couchbase | SecureStorage is more secure for credentials |

---

## 4. Implementation Checklist

### Phase 1: Data Layer Fixes

- [x] **4.1** Update `QrRegistrationRequest` DTO ✅ *Completed 2026-01-03*
  - [x] Remove `deviceName` field
  - [x] Remove `platform` field
  - [x] Add `deviceType: Int` field (default = 0 for GroPOS)
  - [x] Added `DeviceTypes` constants object

- [x] **4.2** Update `DeviceStatusResponseDto` ✅ *Completed 2026-01-03*
  - [x] Remove `stationId` field (not in API response)
  - [x] Document that `assignedGuid` from QR response is the stationId
  - [x] Updated `toDeviceInfo()` mapper to accept `assignedGuid` parameter

- [x] **4.3** Update `DeviceHeartbeatRequest`/`Response` ✅ *Completed 2026-01-03*
  - [x] Changed to match GET `/device-registration/heartbeat`
  - [x] Removed request body fields (renamed to `HeartbeatResponse`)
  - [x] Updated response to `{ messageCount: Int }`

- [x] **4.4** Update `RemoteDeviceRepository` ✅ *Completed 2026-01-03*
  - [x] Add `version: 1.0` header to all requests
  - [x] Add Bearer token support for status polling
  - [x] Store temporary `accessToken` for polling phase
  - [x] Added `getCurrentDeviceGuid()` and `clearTemporaryCredentials()` helpers

### Phase 2: Domain Layer Fixes

- [x] **4.5** Update `RegistrationState` enum ✅ *Completed 2026-01-03*
  - [x] Add `LOADING` state (initial state, checking local database)
  - [x] Add `TIMEOUT` state (QR expired: 10min default, 60min for IN_PROGRESS)

- [x] **4.6** Update `DeviceInfo` model ✅ *Completed 2026-01-03*
  - [x] Ensure `stationId` is populated from `assignedGuid` (done in 4.2 toDeviceInfo fix)

### Phase 3: Presentation Layer Fixes

- [x] **4.7** Implement status polling in `RegistrationViewModel` ✅ *Completed 2026-01-03*
  - [x] 10 second polling interval (POLL_INTERVAL_MS)
  - [x] 10 minute default timeout (DEFAULT_TIMEOUT_MS)
  - [x] 60 minute extended timeout when IN_PROGRESS (EXTENDED_TIMEOUT_MS)
  - [x] Proper cancellation on dispose (pollingJob.cancel())
  - [x] checkInitialState() for LOADING state on screen load
  - [x] handleRegistrationComplete() saves credentials via repository

- [x] **4.8** Add LOADING state handling in UI ✅ *Completed 2026-01-03*
  - [x] Initial state is now LOADING (was UNREGISTERED)
  - [x] Added RetryAfterTimeout event for timeout recovery

### Phase 4: Testing

- [x] **4.9** Create `RegistrationViewModelTest.kt` ✅ *Completed 2026-01-03*
  - [x] Test initial state is LOADING
  - [x] Test transitions to REGISTERED if already registered
  - [x] Test generates pairing code and transitions to PENDING
  - [x] Test stores device GUID from QR response
  - [x] Test transitions to IN_PROGRESS when admin starts assignment
  - [x] Test completes registration when status is Registered
  - [x] Test times out after 10 minutes if still PENDING
  - [x] Test handles QR request failure gracefully
  - [x] Test refresh event generates new pairing code
  - [x] Test retry after timeout generates new QR code
  - **Total: 10 test cases**

### Phase 5: Documentation & Cleanup

- [ ] **4.10** Update CHANGELOG.md
- [ ] **4.11** Update this remediation document
- [ ] **4.12** Remove any TODO comments related to fixed issues

---

## 5. Detailed Code Changes

### 5.1 QrRegistrationRequest (DeviceDto.kt)

**Before:**
```kotlin
@Serializable
data class QrRegistrationRequest(
    @SerialName("deviceName") val deviceName: String,
    @SerialName("platform") val platform: String
)
```

**After:**
```kotlin
@Serializable
data class QrRegistrationRequest(
    @SerialName("deviceType") val deviceType: Int = 0  // 0 = GroPOS
)
```

**Device Type Constants:**
```kotlin
object DeviceTypes {
    const val GROPOS = 0
    const val ONE_TIME = 1
    const val ONE_STORE = 2
    const val ONE_SERVER = 3
    const val SCALE = 4
    // ... etc (see DEVICE_REGISTRATION.md lines 296-314)
}
```

---

### 5.2 RemoteDeviceRepository Updates

**Add temporary token storage:**
```kotlin
class RemoteDeviceRepository(
    private val apiClient: ApiClient,
    private val secureStorage: SecureStorage
) : DeviceRepository {
    
    // Temporary token for status polling (not persisted)
    private var temporaryAccessToken: String? = null
    private var currentDeviceGuid: String? = null
```

**Update requestQrCode:**
```kotlin
override suspend fun requestQrCode(): Result<QrRegistrationResponse> {
    val request = QrRegistrationRequest(deviceType = DeviceTypes.GROPOS)
    
    return apiClient.deviceRequest<QrRegistrationResponseDto> {
        post(ENDPOINT_QR_REGISTRATION) {
            header("version", "1.0")
            setBody(request)
        }
    }.map { response ->
        // Store temporary token and GUID for polling
        temporaryAccessToken = response.accessToken
        currentDeviceGuid = response.assignedGuid
        response.toDomain()
    }
}
```

**Update checkRegistrationStatus:**
```kotlin
override suspend fun checkRegistrationStatus(deviceGuid: String): Result<DeviceStatusResponse> {
    val token = temporaryAccessToken 
        ?: return Result.failure(DeviceRegistrationException("No access token for polling"))
    
    val endpoint = ENDPOINT_DEVICE_STATUS.replace("{deviceGuid}", deviceGuid)
    
    return apiClient.deviceRequest<DeviceStatusResponseDto> {
        get(endpoint) {
            header("version", "1.0")
            header("Authorization", "Bearer $token")
        }
    }.map { it.toDomain() }
}
```

---

### 5.3 RegistrationState Enum (DeviceInfo.kt)

**After:**
```kotlin
enum class RegistrationState {
    LOADING,        // Initial state, checking local database
    UNREGISTERED,   // No API key, need to show QR/pairing code
    PENDING,        // QR/pairing code displayed, waiting for admin
    IN_PROGRESS,    // Admin has scanned/entered code, assigning branch
    REGISTERED,     // API key received, ready for login
    TIMEOUT,        // QR code expired (10 min default)
    ERROR           // Registration failed due to error
}
```

---

### 5.4 Status Polling Implementation (RegistrationViewModel.kt)

```kotlin
companion object {
    private const val POLL_INTERVAL_MS = 10_000L        // 10 seconds
    private const val DEFAULT_TIMEOUT_MS = 10 * 60 * 1000L   // 10 minutes
    private const val EXTENDED_TIMEOUT_MS = 60 * 60 * 1000L  // 60 minutes
}

private var pollingJob: Job? = null

private fun startStatusPolling(deviceGuid: String) {
    pollingJob?.cancel()
    
    pollingJob = scope.launch {
        val startTime = Clock.System.now().toEpochMilliseconds()
        var timeout = DEFAULT_TIMEOUT_MS
        
        while (isActive) {
            val elapsed = Clock.System.now().toEpochMilliseconds() - startTime
            
            // Check for timeout
            if (elapsed >= timeout) {
                _state.update { 
                    it.copy(registrationState = RegistrationState.TIMEOUT) 
                }
                break
            }
            
            try {
                val statusResult = deviceRepository.checkRegistrationStatus(deviceGuid)
                
                statusResult.onSuccess { response ->
                    when (response.deviceStatus) {
                        "Pending" -> {
                            // Continue polling
                        }
                        "In-Progress" -> {
                            timeout = EXTENDED_TIMEOUT_MS  // Extend timeout
                            _state.update {
                                it.copy(
                                    registrationState = RegistrationState.IN_PROGRESS,
                                    branchName = response.branch ?: ""
                                )
                            }
                        }
                        "Registered" -> {
                            handleRegistrationComplete(deviceGuid, response)
                            return@launch
                        }
                    }
                }.onFailure { error ->
                    // Log but continue polling
                    println("Status check failed: ${error.message}")
                }
                
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                println("Polling error: ${e.message}")
            }
            
            delay(POLL_INTERVAL_MS)
        }
    }
}

private suspend fun handleRegistrationComplete(
    deviceGuid: String,
    response: DeviceStatusResponse
) {
    val apiKey = response.apiKey ?: return
    val branchName = response.branch ?: "Unknown Branch"
    val branchId = response.branchId ?: -1
    
    val deviceInfo = DeviceInfo(
        stationId = deviceGuid,  // Use assignedGuid as stationId
        apiKey = apiKey,
        branchName = branchName,
        branchId = branchId,
        environment = "PRODUCTION",  // TODO: Get from config
        registeredAt = Clock.System.now().toString()
    )
    
    deviceRepository.registerDevice(deviceInfo).onSuccess {
        _state.update {
            it.copy(
                registrationState = RegistrationState.REGISTERED,
                branchName = branchName,
                stationName = "Station ${deviceGuid.take(8)}"
            )
        }
    }.onFailure { error ->
        _state.update {
            it.copy(
                registrationState = RegistrationState.ERROR,
                errorMessage = "Failed to save credentials: ${error.message}"
            )
        }
    }
}
```

---

### 5.5 Heartbeat Updates (DeviceDto.kt)

**After:**
```kotlin
/**
 * Response from device heartbeat.
 * 
 * **API Endpoint:** GET /device-registration/heartbeat
 * **Headers:** x-api-key: <apiKey>, version: 1.0
 */
@Serializable
data class HeartbeatResponse(
    @SerialName("messageCount")
    val messageCount: Int = 0
)

// Remove DeviceHeartbeatRequest - it's a GET with no body
```

---

## 6. Testing Requirements

### 6.1 Unit Tests (DeviceRegistrationFlowTest.kt)

| Test Case | Description | Priority |
|-----------|-------------|----------|
| `testQrCodeRequest_sendsCorrectPayload` | Verify `deviceType: 0` is sent | P0 |
| `testQrCodeRequest_storesAccessToken` | Verify temp token is captured | P0 |
| `testStatusPolling_usesBearerToken` | Verify auth header is set | P0 |
| `testStatusPolling_handlesRegistered` | Verify saves to SecureStorage | P0 |
| `testStatusPolling_extendsTimeoutOnInProgress` | Verify 60min timeout | P1 |
| `testStatusPolling_timesOutAfter10Minutes` | Verify TIMEOUT state | P1 |
| `testHeartbeat_usesApiKeyHeader` | Verify x-api-key header | P1 |

### 6.2 Integration Tests

| Test Case | Description | Priority |
|-----------|-------------|----------|
| `testFullRegistrationFlow` | End-to-end with mock API | P0 |
| `testRegistrationPersistsAcrossRestart` | Verify SecureStorage | P1 |

---

## 7. Commit Plan

Following Conventional Commits and atomic commit principles:

| Order | Commit Message | Files Changed |
|-------|---------------|---------------|
| 1 | `fix(device): correct QR registration request payload` | `DeviceDto.kt` |
| 2 | `fix(device): add Bearer token auth for status polling` | `RemoteDeviceRepository.kt` |
| 3 | `feat(device): add LOADING and TIMEOUT registration states` | `DeviceInfo.kt` |
| 4 | `feat(device): implement status polling mechanism` | `RegistrationViewModel.kt` |
| 5 | `fix(device): correct heartbeat endpoint to GET` | `DeviceDto.kt` |
| 6 | `test(device): add device registration flow tests` | `DeviceRegistrationFlowTest.kt` |
| 7 | `docs: update CHANGELOG with registration fixes` | `CHANGELOG.md` |

---

## 8. Rollback Plan

If issues are discovered after deployment:

1. **Immediate:** Revert to previous branch
   ```bash
   git revert --no-commit HEAD~7..HEAD
   git commit -m "revert(device): rollback registration fixes"
   ```

2. **Data:** SecureStorage changes are additive; no data migration needed

3. **Feature Flag:** Consider adding a feature flag for new polling behavior if needed

---

## 9. Change Log

| Date | Author | Change |
|------|--------|--------|
| 2026-01-03 | AI Architect | Initial remediation plan created |
| | | Gap analysis completed |
| | | Awaiting implementation approval |
| 2026-01-03 | AI Architect | **Phase 1 Complete:** Data Layer Fixes |
| | | - C1: QrRegistrationRequest fixed (deviceType instead of deviceName/platform) |
| | | - C2: Bearer token support added to status polling |
| | | - C4: stationId removed from DeviceStatusResponseDto |
| | | - H2: HeartbeatResponse created (GET, no request body) |
| | | - H3: temporaryAccessToken storage added to repository |
| | | - H4: version header added to all requests |
| | | - Created DeviceRegistrationDtoTest.kt with 9 test cases |

---

## Approval & Sign-off

- [ ] **Gap Analysis Reviewed** - Date: ___________
- [ ] **Implementation Plan Approved** - Date: ___________
- [ ] **Code Changes Completed** - Date: ___________
- [ ] **Tests Passing** - Date: ___________
- [ ] **Ready for Merge** - Date: ___________

---

*This is a living document. Update the checklist and change log as work progresses.*

