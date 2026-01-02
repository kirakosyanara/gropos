# GroPOS Cashier Operations

**Version:** 2.0 (Kotlin)  
**Status:** Complete Specification Document

> Complete reference for cashier session management, station claiming, till assignment, login/logout flows, lock states, and daily operations.

---

## Table of Contents

- [Overview](#overview)
- [Session Lifecycle](#session-lifecycle)
- [Station Claiming Flow](#station-claiming-flow)
- [Fetching Cashier List](#fetching-cashier-list)
- [Till Assignment](#till-assignment)
- [Login Flow](#login-flow)
- [Screen Lock Mechanism](#screen-lock-mechanism)
- [Unlock Flow](#unlock-flow)
- [Logout Flow](#logout-flow)
- [Session Tracking](#session-tracking)
- [API Endpoints](#api-endpoints)
- [UI Specifications](#ui-specifications)

---

## Overview

The cashier session lifecycle manages how employees claim stations, authenticate, maintain sessions, and release stations. This is critical for:

- **Security**: Ensuring only authorized employees can process transactions
- **Accountability**: Tracking who processed which transactions
- **Compliance**: Maintaining audit trails for all operations
- **Till Management**: Associating cash drawer with the correct employee

### Key Concepts

| Concept | Description |
|---------|-------------|
| **Station** | Physical POS terminal (device) |
| **Till** | Cash drawer/account associated with an employee |
| **Session** | Period between login and logout |
| **Lock** | Temporary suspension requiring PIN to resume |

---

## Session Lifecycle

### Complete State Machine

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    COMPLETE CASHIER SESSION STATE MACHINE                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│     ┌─────────────────────────────────────────────────────────────────┐     │
│     │                        DEVICE STATE                             │     │
│     │                                                                 │     │
│     │   ┌───────────────┐                                            │     │
│     │   │  UNREGISTERED │ ──────── Device Registration ──────┐       │     │
│     │   │  (No API Key) │                                    │       │     │
│     │   └───────────────┘                                    │       │     │
│     │                                                        ▼       │     │
│     │   ┌───────────────────────────────────────────────────────┐   │     │
│     │   │                    REGISTERED                          │   │     │
│     │   │               (API Key Stored)                         │   │     │
│     │   └────────────────────────┬──────────────────────────────┘   │     │
│     └────────────────────────────┼──────────────────────────────────┘     │
│                                  │                                         │
│                                  ▼                                         │
│     ┌─────────────────────────────────────────────────────────────────┐   │
│     │                      CASHIER STATE                               │   │
│     │                                                                  │   │
│     │   ┌───────────────┐     Select Employee                         │   │
│     │   │  LOGGED OUT   │ ◀───────────────────────────────────────┐   │   │
│     │   │ (Login Screen)│                                          │   │   │
│     │   └───────┬───────┘                                          │   │   │
│     │           │                                                   │   │   │
│     │           ▼ Employee Selected                                 │   │   │
│     │   ┌───────────────┐                                          │   │   │
│     │   │  PIN ENTRY    │ ◀─── Invalid PIN                         │   │   │
│     │   │               │                                          │   │   │
│     │   └───────┬───────┘                                          │   │   │
│     │           │ PIN Entered                                       │   │   │
│     │           ▼                                                   │   │   │
│     │   ┌───────────────┐    No Till                               │   │   │
│     │   │ TILL CHECK    │────────────────┐                         │   │   │
│     │   │               │                │                         │   │   │
│     │   └───────┬───────┘                ▼                         │   │   │
│     │           │ Has Till      ┌───────────────┐                  │   │   │
│     │           │               │TILL ASSIGNMENT│                  │   │   │
│     │           │               │(Scan/Select)  │                  │   │   │
│     │           │               └───────┬───────┘                  │   │   │
│     │           │                       │ Till Selected            │   │   │
│     │           ◀───────────────────────┘                          │   │   │
│     │           │                                                   │   │   │
│     │           ▼ Authentication Success                            │   │   │
│     │   ┌───────────────┐                                          │   │   │
│     │   │    ACTIVE     │ ◀─── Unlock (PIN Verified)               │   │   │
│     │   │ (Home Screen) │                                          │   │   │
│     │   └───────┬───────┘                                          │   │   │
│     │           │                                                   │   │   │
│     │           ├─── F4 Key ───────────┐                           │   │   │
│     │           │                      │                           │   │   │
│     │           ├─── Inactivity ───────┤                           │   │   │
│     │           │    (5 min)           │                           │   │   │
│     │           │                      ▼                           │   │   │
│     │           │              ┌───────────────┐                   │   │   │
│     │           │              │    LOCKED     │                   │   │   │
│     │           │              │ (Lock Screen) │                   │   │   │
│     │           │              └───────┬───────┘                   │   │   │
│     │           │                      │                           │   │   │
│     │           │                      ├─── Unlock ────────────────┘   │   │
│     │           │                      │    (PIN Verified)             │   │
│     │           │                      │                               │   │
│     │           │                      └─── Sign Out ──────────────────┘   │
│     │           │                           (Manager Approval)             │
│     │           │                                                          │
│     │           └─── Sign Out ─────────────────────────────────────────────┘
│     │                (Functions Menu)                                       │
│     │                                                                       │
│     └───────────────────────────────────────────────────────────────────────┘
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Session States

| State | Description | UI Location | Can Transact | Actions Available |
|-------|-------------|-------------|--------------|-------------------|
| `LOGGED_OUT` | No active session | Login Screen | No | Select employee |
| `PIN_ENTRY` | Employee selected, awaiting PIN | Login Screen (PIN mode) | No | Enter PIN, back |
| `TILL_ASSIGNMENT` | Need to assign till | Till Selection Dialog | No | Scan/select till |
| `ACTIVE` | Normal operation | Home Screen | Yes | All functions |
| `LOCKED` | Inactivity or manual lock | Lock Screen | No | Unlock, sign out |
| `SIGNING_OUT` | Logout in progress | Logout Dialog | No | Confirm, cancel |

---

## Station Claiming Flow

### How a Station is Claimed

When a cashier logs in, they "claim" the station. This associates:
1. The **employee** with the **device**
2. The **till (cash drawer)** with the **session**
3. The **bearer token** for authenticated API calls

### Device Info at Login

```kotlin
/**
 * Device information retrieved at login
 * Shows current station status and any pre-assigned employee
 */
data class DeviceInfoViewModel(
    val id: Int,                    // Station/device ID
    val name: String,               // "Register 1", "Till 2"
    val location: String,           // "Front Counter", "Express Lane"
    val branch: String,             // "Main Street Store"
    val branchId: Int,              // Branch ID for API calls
    val employeeId: Int?,           // Currently assigned employee (if any)
    val locationAccountId: Int?,    // Currently assigned till/account
    val ipAddress: String?          // For peripheral devices
)
```

### Pre-Assigned Employee Detection

When a device has a previously assigned employee, the system auto-selects them:

```kotlin
fun startLoginProcess() {
    val deviceInfo = deviceApi.deviceGetCurrentDeviceInfo()
    
    // Store device/branch info
    val branchDto = BranchDto(
        id = deviceInfo.branchId,
        name = deviceInfo.name,
        stationId = deviceInfo.id
    )
    AppStore.instance.branchProperty.set(branchDto)
    
    // Check for pre-assigned employee
    if (deviceInfo.employeeId != null) {
        val preAssignedEmployee = employees.find { 
            it.userId == deviceInfo.employeeId 
        }
        preAssignedEmployee?.let { employee ->
            // Auto-select the employee
            employee.assignedAccountId = deviceInfo.locationAccountId
            selectEmployee(employee)  // Goes directly to PIN entry
        }
    }
}
```

---

## Fetching Cashier List

### API Call

The cashier list is fetched immediately after device registration is confirmed and API key is configured:

```kotlin
// EmployeeApi.employeeGetCashierEmployees()
// GET /employee/cashiers
// Header: x-api-key: {deviceApiKey}

val cashiers: List<EmployeeListViewModel> = employeeApi.employeeGetCashierEmployees()
```

### EmployeeListViewModel

```kotlin
/**
 * Cashier data returned by the API
 * This is transformed to UserProfileViewModel for display
 */
data class EmployeeListViewModel(
    val userId: Int?,
    val email: String?,
    val firstName: String?,
    val lastName: String?,
    val imageUrl: String?,
    val role: String?,
    val assignedAccountId: Int?   // Pre-assigned till (if any)
)

// Transformed for UI display
data class UserProfileViewModel(
    val userId: Int?,
    val email: String?,
    val firstName: String?,
    val lastName: String?,
    val imageUrl: String?,
    val role: String?,
    var assignedAccountId: Int?,   // Mutable - can be assigned during login
    val permissions: List<String>? = null
) {
    val fullName: String get() = "$firstName $lastName"
}
```

### Which Cashiers Appear

The API returns cashiers based on:
1. **Scheduled for this location** - Cashiers scheduled to work at this branch today
2. **Role-based** - Employees with cashier/register permissions
3. **Active status** - Only active (not terminated) employees

---

## Till Assignment

### When Till Assignment is Required

Till assignment occurs when:
1. Employee has no `assignedAccountId` (null or -1)
2. Employee's previous till was released
3. New employee logging in for first time

### Till Assignment Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          TILL ASSIGNMENT FLOW                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   Employee enters PIN but has no till assigned                               │
│                           │                                                  │
│                           ▼                                                  │
│   ┌───────────────────────────────────────────────────────────────────────┐ │
│   │                    TILL SELECTION DIALOG                               │ │
│   │                                                                        │ │
│   │   ┌────────────────────────────────────────────────────────────────┐  │ │
│   │   │  Desktop Mode (with hardware):                                  │  │ │
│   │   │                                                                 │  │ │
│   │   │   Select a Till from the list:                                  │  │ │
│   │   │   ┌─────────────────────────────────────────────────────────┐  │  │ │
│   │   │   │ [✓] Till 1 - Available                                   │  │  │ │
│   │   │   │ [x] Till 2 - Assigned to: John Doe                       │  │  │ │
│   │   │   │ [✓] Till 3 - Available                                   │  │  │ │
│   │   │   │ [✓] Till 4 - Available                                   │  │  │ │
│   │   │   └─────────────────────────────────────────────────────────┘  │  │ │
│   │   │                                                                 │  │ │
│   │   │   [ Cancel ]                                                    │  │ │
│   │   └────────────────────────────────────────────────────────────────┘  │ │
│   │                                                                        │ │
│   │   ┌────────────────────────────────────────────────────────────────┐  │ │
│   │   │  Mobile/Tablet Mode (scan till):                                │  │ │
│   │   │                                                                 │  │ │
│   │   │   ┌─────────────────────────────────────────────────────────┐  │  │ │
│   │   │   │                   SCAN TILL                              │  │  │ │
│   │   │   │                                                          │  │  │ │
│   │   │   │   [Barcode Scanner Icon]                                 │  │  │ │
│   │   │   │                                                          │  │  │ │
│   │   │   │   Scan the barcode on your assigned till drawer          │  │  │ │
│   │   │   │                                                          │  │  │ │
│   │   │   │   [ Cash drawer opens automatically ]                    │  │  │ │
│   │   │   │                                                          │  │  │ │
│   │   │   └─────────────────────────────────────────────────────────┘  │  │ │
│   │   │                                                                 │  │ │
│   │   │   Error: "Till not found. Please scan a valid till barcode."   │  │ │
│   │   │                                                                 │  │ │
│   │   │   [ Cancel ]                                                    │  │ │
│   │   └────────────────────────────────────────────────────────────────┘  │ │
│   └────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
│   Till selected/scanned → Continue with authentication                       │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Till List API

```kotlin
// AccountApi.accountGetTillAccountList()
// GET /account/till-list
// Returns: GridDataOfLocationAccountListViewModel

data class LocationAccountListViewModel(
    val locationAccountId: Int,    // Till ID
    val name: String,              // "Till 1", "Drawer A"
    val locationId: Int,           // Branch/location
    val assignedEmployeeId: Int?,  // Currently assigned (null = available)
    val assignedEmployeeName: String?
)

// Fetching available tills
fun loadAvailableTills(): List<LocationAccountListViewModel> {
    val response = accountApi.accountGetTillAccountList()
    return response.rows ?: emptyList()
}
```

### Till Assignment Implementation

```kotlin
// AccountListDialog.kt equivalent in Kotlin

class TillAssignmentViewModel : ViewModel() {
    
    private val accountApi = AccountApi(Manager.apiClient)
    
    private val _tills = MutableStateFlow<List<TillItem>>(emptyList())
    val tills: StateFlow<List<TillItem>> = _tills.asStateFlow()
    
    private val _selectedTillId = MutableStateFlow<Int?>(null)
    val selectedTillId: StateFlow<Int?> = _selectedTillId.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        loadTills()
    }
    
    private fun loadTills() {
        viewModelScope.launch {
            try {
                val response = accountApi.accountGetTillAccountList()
                _tills.value = response.rows?.map { till ->
                    TillItem(
                        id = till.locationAccountId,
                        name = till.name,
                        isAvailable = till.assignedEmployeeId == null,
                        assignedTo = till.assignedEmployeeName
                    )
                } ?: emptyList()
            } catch (e: ApiException) {
                _error.value = e.message
            }
        }
    }
    
    fun selectTill(tillId: Int) {
        val till = _tills.value.find { it.id == tillId }
        if (till?.isAvailable == true) {
            _selectedTillId.value = tillId
        } else {
            _error.value = "This till is already assigned"
        }
    }
    
    /**
     * Handle till barcode scan
     * Opens cash drawer on mobile devices
     */
    fun onTillScanned(barcode: String) {
        try {
            val tillId = barcode.toInt()
            val validTill = _tills.value.find { it.id == tillId }
            
            if (validTill != null && validTill.isAvailable) {
                _selectedTillId.value = tillId
            } else if (validTill != null) {
                _error.value = "Till is already assigned to ${validTill.assignedTo}"
            } else {
                _error.value = "Till not found. Please scan a valid till barcode."
            }
        } catch (e: NumberFormatException) {
            _error.value = "Invalid till barcode format"
        }
    }
}

data class TillItem(
    val id: Int,
    val name: String,
    val isAvailable: Boolean,
    val assignedTo: String?
)
```

---

## Login Flow

### Complete Login Sequence

```kotlin
class LoginViewModel : ViewModel() {
    
    private val employeeApi = EmployeeApi(Manager.apiClient)
    private val deviceApi = DeviceApi(Manager.apiClient)
    
    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()
    
    /**
     * STEP 1: Initialize - Load cashiers and device info
     */
    fun initialize() {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true) }
                
                // Fetch scheduled cashiers
                val employees = employeeApi.employeeGetCashierEmployees()
                
                // Fetch device info
                val deviceInfo = deviceApi.deviceGetCurrentDeviceInfo()
                
                // Store branch info globally
                val branchDto = BranchDto(
                    id = deviceInfo.branchId,
                    name = deviceInfo.name,
                    stationId = deviceInfo.id
                )
                AppStore.instance.branchProperty.set(branchDto)
                
                _state.update { 
                    it.copy(
                        isLoading = false,
                        employees = employees.map { emp -> emp.toUiModel() },
                        stationName = "${deviceInfo.location}\n${deviceInfo.name}"
                    )
                }
                
                // Check for pre-assigned employee
                deviceInfo.employeeId?.let { employeeId ->
                    val preAssigned = employees.find { it.userId == employeeId }
                    preAssigned?.let { emp ->
                        val uiModel = emp.toUiModel().copy(
                            assignedAccountId = deviceInfo.locationAccountId
                        )
                        selectEmployee(uiModel)
                    }
                }
                
            } catch (e: ApiException) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
    
    /**
     * STEP 2: User selects employee from list
     */
    fun selectEmployee(employee: EmployeeUiModel) {
        _state.update { 
            it.copy(
                selectedEmployee = employee,
                screenMode = LoginScreenMode.PIN_ENTRY,
                pinInput = ""
            )
        }
    }
    
    /**
     * STEP 3: User enters PIN digits
     */
    fun onPinDigit(digit: String) {
        val currentPin = _state.value.pinInput
        if (currentPin.length < 8) { // Max 8 digits
            _state.update { it.copy(pinInput = currentPin + digit) }
        }
    }
    
    fun onPinBackspace() {
        _state.update { it.copy(pinInput = it.pinInput.dropLast(1)) }
    }
    
    fun onPinClear() {
        _state.update { it.copy(pinInput = "") }
    }
    
    /**
     * STEP 4: Submit login
     */
    fun submitLogin() {
        val employee = _state.value.selectedEmployee ?: return
        val pin = _state.value.pinInput
        
        if (pin.length < 4) {
            _state.update { it.copy(error = "PIN must be at least 4 digits") }
            return
        }
        
        // Check if till assignment needed
        if (employee.assignedAccountId == null || employee.assignedAccountId <= 0) {
            _state.update { it.copy(screenMode = LoginScreenMode.TILL_ASSIGNMENT) }
            return
        }
        
        // Proceed with authentication
        authenticate(employee, pin)
    }
    
    /**
     * STEP 5: Authenticate with backend
     */
    private fun authenticate(employee: EmployeeUiModel, pin: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            try {
                // Build login request
                val request = CashierLoginRequest(
                    username = employee.email,
                    password = pin,
                    tillId = employee.assignedAccountId,
                    branchId = AppStore.instance.branch.id,
                    stationId = AppStore.instance.branch.stationId
                )
                
                // Call login API
                val tokenResponse = employeeApi.employeeGroPOSLogin(request)
                
                // Save refresh token locally
                Manager.posSystem.getDocumentById(Environment.current.name)?.let { posSystem ->
                    posSystem.refreshToken = tokenResponse.refreshToken
                    Manager.posSystem.save(posSystem)
                }
                
                // Set bearer token for authenticated requests
                Manager.setBearerToken(tokenResponse.accessToken)
                
                // Fetch and store user profile (includes permissions)
                val profile = employeeApi.employeeGetProfile()
                
                // Filter to only GroPOS permissions
                val groposPermissions = profile.permissions
                    ?.filter { it.startsWith("GrowPOS") || it.startsWith("GroPOS") }
                    ?: emptyList()
                profile.permissions = groposPermissions
                
                AppStore.instance.employeeProperty.set(profile)
                
                // Load branch settings
                val branchApi = BranchApi(Manager.defaultClient)
                val settings = branchApi.branchGetAllSettingsRoute()
                settings.forEach { Manager.posBranchSettings.save(it) }
                
                // Navigate to home
                Router.changeLayout("HomeView").goTo("HomeView")
                
            } catch (e: ApiException) {
                _state.update { 
                    it.copy(
                        isLoading = false,
                        pinInput = "",
                        error = e.message ?: "Login failed"
                    )
                }
            }
        }
    }
    
    /**
     * Called when till is selected/scanned
     */
    fun onTillSelected(tillId: Int) {
        _state.update { state ->
            state.copy(
                selectedEmployee = state.selectedEmployee?.copy(assignedAccountId = tillId),
                screenMode = LoginScreenMode.PIN_ENTRY
            )
        }
        // Re-submit login with till now assigned
        submitLogin()
    }
    
    fun goBack() {
        when (_state.value.screenMode) {
            LoginScreenMode.PIN_ENTRY -> {
                _state.update { 
                    it.copy(
                        screenMode = LoginScreenMode.EMPLOYEE_LIST,
                        selectedEmployee = null,
                        pinInput = ""
                    )
                }
            }
            LoginScreenMode.TILL_ASSIGNMENT -> {
                _state.update { it.copy(screenMode = LoginScreenMode.PIN_ENTRY) }
            }
            else -> { /* No-op */ }
        }
    }
}

data class LoginState(
    val isLoading: Boolean = false,
    val stationName: String = "",
    val employees: List<EmployeeUiModel> = emptyList(),
    val selectedEmployee: EmployeeUiModel? = null,
    val screenMode: LoginScreenMode = LoginScreenMode.EMPLOYEE_LIST,
    val pinInput: String = "",
    val error: String? = null
)

enum class LoginScreenMode {
    EMPLOYEE_LIST,    // Show list of scheduled cashiers
    PIN_ENTRY,        // Selected employee, enter PIN
    TILL_ASSIGNMENT   // Need to select/scan till
}

data class EmployeeUiModel(
    val id: Int,
    val email: String,
    val firstName: String,
    val lastName: String,
    val imageUrl: String?,
    val role: String,
    val assignedAccountId: Int?
) {
    val fullName: String get() = "$firstName $lastName"
}
```

---

## Screen Lock Mechanism

### Lock Types

| Lock Type | Trigger | API Event Type | Description |
|-----------|---------|----------------|-------------|
| `AutoLocked` | 5 min inactivity | `DeviceEventType.AutoLocked` | Automatic timeout |
| `Locked` | F4 key press | `DeviceEventType.Locked` | Manual lock |
| `Unlocked` | PIN verified | `DeviceEventType.Unlocked` | Resume session |

### Inactivity Timer Implementation

```kotlin
object InactivityManager {
    
    private const val INACTIVITY_LIMIT_MS = 5 * 60 * 1000L  // 5 minutes
    
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var inactivityJob: Job? = null
    private val employeeApi = EmployeeApi(Manager.defaultClient)
    
    /**
     * Reset timer on any user activity
     * Called from event filters on mouse/keyboard events
     */
    fun resetTimer() {
        inactivityJob?.cancel()
        inactivityJob = scope.launch {
            delay(INACTIVITY_LIMIT_MS)
            triggerAutoLock()
        }
    }
    
    /**
     * Trigger automatic screen lock
     */
    private suspend fun triggerAutoLock() {
        // Don't lock on these screens
        val currentLayout = Router.getCurrentLayout()
        if (currentLayout in listOf("LockView", "LoginView", "CustomerScreenView")) {
            return
        }
        
        // Don't lock if payment in progress
        if (OrderStore.paymentList.isNotEmpty()) {
            // Reschedule - user is in middle of payment
            resetTimer()
            return
        }
        
        // Report lock event to backend
        try {
            val request = DeviceLockRequest(
                lockType = DeviceEventType.AutoLocked
            )
            employeeApi.employeeLockDevice(request)
        } catch (e: ApiException) {
            // Handle token refresh if needed
            if (e.code == 401) {
                refreshTokenAndRetry {
                    employeeApi.employeeLockDevice(DeviceLockRequest(
                        lockType = DeviceEventType.AutoLocked
                    ))
                }
            }
        }
        
        // Navigate to lock screen
        withContext(Dispatchers.Main) {
            Router.changeLayout("LockView").goTo("LockView", false)
        }
    }
    
    /**
     * Manual lock (F4 key)
     */
    fun manualLock() {
        // Prevent lock during payment
        if (OrderStore.paymentList.isNotEmpty()) {
            showError("Cannot lock with active payments")
            return
        }
        
        scope.launch {
            try {
                employeeApi.employeeLockDevice(
                    DeviceLockRequest(lockType = DeviceEventType.Locked)
                )
            } catch (e: Exception) {
                logger.error("Manual lock failed", e)
            }
            
            withContext(Dispatchers.Main) {
                Router.changeLayout("LockView").goTo("LockView", false)
            }
        }
    }
    
    /**
     * Start monitoring on login
     */
    fun start() {
        resetTimer()
    }
    
    /**
     * Stop monitoring on logout
     */
    fun stop() {
        inactivityJob?.cancel()
    }
}

// Integration with Compose
@Composable
fun MainApp() {
    val activity = LocalContext.current as? Activity
    
    DisposableEffect(Unit) {
        // Reset timer on any user interaction
        activity?.window?.decorView?.setOnTouchListener { _, _ ->
            InactivityManager.resetTimer()
            false
        }
        
        onDispose { }
    }
    
    // ... rest of app
}
```

### Lock Screen State

The lock screen displays:
- Currently locked employee info
- PIN entry pad
- Sign Out option (requires manager approval)

```kotlin
data class LockScreenState(
    val employeeName: String,
    val employeeRole: String,
    val employeeImageUrl: String?,
    val stationName: String,
    val pinInput: String = "",
    val isVerifying: Boolean = false,
    val error: String? = null
)
```

---

## Unlock Flow

### PIN Verification

```kotlin
class LockViewModel : ViewModel() {
    
    private val employeeApi = EmployeeApi(Manager.defaultClient)
    
    private val _state = MutableStateFlow(LockScreenState())
    val state: StateFlow<LockScreenState> = _state.asStateFlow()
    
    fun initialize() {
        val employee = AppStore.instance.employee
        val branch = AppStore.instance.branch
        
        _state.update {
            it.copy(
                employeeName = employee.fullName,
                employeeRole = employee.role,
                employeeImageUrl = employee.imageUrl,
                stationName = "${branch.location}\n${branch.name}"
            )
        }
    }
    
    fun onPinDigit(digit: String) {
        if (_state.value.pinInput.length < 8) {
            _state.update { it.copy(pinInput = it.pinInput + digit, error = null) }
        }
    }
    
    fun onPinClear() {
        _state.update { it.copy(pinInput = "", error = null) }
    }
    
    /**
     * Verify PIN and unlock
     */
    fun verify() {
        val pin = _state.value.pinInput
        
        if (pin.isEmpty()) {
            _state.update { it.copy(error = "Please enter your PIN") }
            return
        }
        
        viewModelScope.launch {
            _state.update { it.copy(isVerifying = true) }
            
            try {
                // Verify PIN with backend
                val request = LoginRequest(
                    userName = AppStore.instance.employee.email,
                    password = pin,
                    clientName = "device",
                    branchId = AppStore.instance.branch.id,
                    deviceId = AppStore.instance.branch.stationId
                )
                
                val success = employeeApi.employeeVerifyPassword(request)
                
                if (success) {
                    // Report unlock event
                    employeeApi.employeeLockDevice(
                        DeviceLockRequest(lockType = DeviceEventType.Unlocked)
                    )
                    
                    // Resume - check if we were in a return flow
                    val destination = if (OrderStore.transactionSearchDto != null) {
                        "ReturnItemView"
                    } else {
                        "HomeView"
                    }
                    
                    Router.changeLayout(destination).goTo(destination, false)
                } else {
                    _state.update { 
                        it.copy(isVerifying = false, pinInput = "", error = "Invalid PIN")
                    }
                }
                
            } catch (e: ApiException) {
                when (e.code) {
                    401 -> {
                        // Token expired - refresh and retry
                        refreshTokenAndRetry()
                    }
                    else -> {
                        _state.update { 
                            it.copy(isVerifying = false, pinInput = "", error = e.message)
                        }
                    }
                }
            }
        }
    }
    
    private suspend fun refreshTokenAndRetry() {
        try {
            val posSystem = Manager.posSystem.getDocumentById(Environment.current.name)
            val refreshRequest = RefreshTokenModel(
                token = posSystem?.refreshToken,
                clientName = "device"
            )
            
            val newToken = employeeApi.employeeRefreshToken(refreshRequest)
            
            newToken?.let { token ->
                posSystem?.refreshToken = token.refreshToken
                Manager.posSystem.save(posSystem)
                Manager.setBearerToken(token.accessToken)
                
                // Retry verification
                verify()
            }
        } catch (e: Exception) {
            _state.update { 
                it.copy(isVerifying = false, error = "Session expired. Please sign out and log in again.")
            }
        }
    }
}
```

---

## Logout Flow

### Logout Options

| Option | Description | Actions Performed |
|--------|-------------|-------------------|
| **Release Till** | Quick logout | API logout, clear local state, return to login |
| **End of Shift** | Full close-out | Print shift report, open drawer for count, API logout |

### Logout Dialog

```
┌─────────────────────────────────────────────────────────────────┐
│                         SIGN OUT                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │  Release Till: Sign out of this station.                    ││
│  │  End of Shift: End your shift, count out.                   ││
│  └─────────────────────────────────────────────────────────────┘│
│                                                                  │
│  [ Release Till ]    [ End of Shift ]    [ Cancel ]              │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Logout from Lock Screen

Requires **Manager Approval** because the current employee cannot verify themselves:

```kotlin
// In LockView
fun onSignOutClick() {
    // Show logout options dialog
    LogoutDialog.show { option ->
        when (option) {
            LogoutOption.RELEASE_TILL -> {
                // Requires manager PIN
                showManagerApproval(RequestAction.LOGOUT)
            }
            LogoutOption.END_OF_SHIFT -> {
                // Requires manager PIN
                showManagerApproval(RequestAction.END_OF_SHIFT)
            }
        }
    }
}

// Manager approval callback
fun onManagerApprovalSuccess(action: RequestAction) {
    when (action) {
        RequestAction.LOGOUT -> performLogout(endOfShift = false)
        RequestAction.END_OF_SHIFT -> performLogout(endOfShift = true)
    }
}
```

### Logout Implementation

```kotlin
class LogoutViewModel : ViewModel() {
    
    private val employeeApi = EmployeeApi(Manager.defaultClient)
    
    /**
     * Release Till - Simple logout
     */
    fun releaseTill(onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                // Report unlock first
                employeeApi.employeeLockDevice(
                    DeviceLockRequest(lockType = DeviceEventType.Unlocked)
                )
                
                // Logout
                employeeApi.employeeLogout()
            } catch (e: Exception) {
                logger.warn("Logout API failed, proceeding anyway", e)
            } finally {
                clearSessionAndNavigate()
                onComplete()
            }
        }
    }
    
    /**
     * End of Shift - Full close with report
     */
    fun endOfShift(onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                // Report unlock first
                employeeApi.employeeLockDevice(
                    DeviceLockRequest(lockType = DeviceEventType.Unlocked)
                )
                
                // Logout with shift report generation
                val shiftReport = employeeApi.employeeLogoutWithEndOfShift()
                
                // Print shift report
                shiftReport?.let { PrintService.printShiftReport(it) }
                
                // Open drawer for cash count
                HardwareManager.printer.openDrawer()
                
            } catch (e: Exception) {
                logger.warn("End of shift failed, proceeding anyway", e)
            } finally {
                clearSessionAndNavigate()
                onComplete()
            }
        }
    }
    
    private fun clearSessionAndNavigate() {
        // Clear all session state
        AppStore.instance.clearStore()
        OrderStore.clearStore()
        Manager.clearAllPendingUpdates()
        
        // Navigate to login
        Router.changeLayout("LoginView").goTo("LoginView", false)
    }
    
    /**
     * Pre-logout validation
     */
    fun canSignOut(): Boolean {
        // Cannot sign out with items in cart
        if (OrderStore.orderProductList.isNotEmpty()) {
            showError("Please void or complete the transaction before Sign Out")
            return false
        }
        
        // Cannot sign out with active payments
        if (OrderStore.paymentList.isNotEmpty()) {
            showError("Please remove active payments before Sign Out")
            return false
        }
        
        return true
    }
}
```

---

## Session Tracking

### Session Data Model

```kotlin
data class CashierSession(
    val sessionId: String,
    val employeeId: Int,
    val employeeName: String,
    val registerId: Int,
    val tillId: Int,
    val signInTime: Instant,
    val signOutTime: Instant? = null,
    val status: SessionStatus,
    
    // Metrics (populated at end of shift)
    val transactionCount: Int = 0,
    val totalSales: BigDecimal = BigDecimal.ZERO,
    val cashSales: BigDecimal = BigDecimal.ZERO,
    val creditSales: BigDecimal = BigDecimal.ZERO,
    val debitSales: BigDecimal = BigDecimal.ZERO,
    val snapSales: BigDecimal = BigDecimal.ZERO,
    
    // Cash drawer
    val openingFloat: BigDecimal = BigDecimal.ZERO,
    val expectedCash: BigDecimal = BigDecimal.ZERO,
    val actualCash: BigDecimal? = null,
    val variance: BigDecimal? = null,
    
    // Activity
    val lockEvents: List<LockEvent> = emptyList(),
    val breakRecords: List<BreakRecord> = emptyList()
)

enum class SessionStatus {
    ACTIVE,
    LOCKED,
    ON_BREAK,
    COMPLETED
}

data class LockEvent(
    val timestamp: Instant,
    val lockType: DeviceEventType,
    val duration: Duration? = null
)
```

---

## API Endpoints

### Authentication

| Endpoint | Method | Purpose | Auth |
|----------|--------|---------|------|
| `/employee/cashiers` | GET | Get scheduled cashiers | API Key |
| `/employee/gropos-login` | POST | Authenticate cashier | API Key |
| `/employee/profile` | GET | Get user profile & permissions | Bearer |
| `/employee/verify-password` | POST | Verify PIN for unlock | Bearer |
| `/employee/refresh-token` | POST | Refresh access token | Refresh Token |

### Device Events

| Endpoint | Method | Purpose | Auth |
|----------|--------|---------|------|
| `/employee/lock-device` | POST | Report lock/unlock event | Bearer |
| `/employee/logout` | POST | Simple logout | Bearer |
| `/employee/logout-end-of-shift` | POST | Logout with shift report | Bearer |
| `/device/info` | GET | Get current device info | API Key |

### Till Management

| Endpoint | Method | Purpose | Auth |
|----------|--------|---------|------|
| `/account/till-list` | GET | Get available tills | API Key |

---

## UI Specifications

### Login Screen Modes

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         LOGIN SCREEN - EMPLOYEE LIST                         │
├────────────────────────────┬────────────────────────────────────────────────┤
│                            │                                                 │
│   Station Name             │   On Site Cashiers                             │
│   Register 1               │   ┌─────────────────────────────────────────┐  │
│                            │   │ [Photo] Jane Doe - Cashier              │  │
│   [Company Logo]           │   │ [Photo] John Smith - Supervisor         │  │
│                            │   │ [Photo] Mary Johnson - Cashier          │  │
│   12:30 PM                 │   └─────────────────────────────────────────┘  │
│                            │                                                 │
│   ©Unisight BIT 2024       │   Tap to select employee                       │
│                            │                                                 │
└────────────────────────────┴────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                         LOGIN SCREEN - PIN ENTRY                             │
├────────────────────────────┬────────────────────────────────────────────────┤
│                            │                                                 │
│   Station Name             │   Sign In using your hardware token or key code│
│   Register 1               │                                                 │
│                            │   [Photo] Jane Doe                              │
│   [Company Logo]           │           Cashier                               │
│                            │                                                 │
│   12:30 PM                 │   ┌──── PIN Entry ────┐                        │
│                            │   │ ● ● ● ●           │                        │
│   ©Unisight BIT 2024       │   │                    │                        │
│                            │   │ [1] [2] [3]        │                        │
│                            │   │ [4] [5] [6]        │                        │
│   [Back]                   │   │ [7] [8] [9]        │                        │
│                            │   │ [C] [0] [←]        │                        │
│                            │   └────────────────────┘                        │
│                            │                                                 │
│                            │   [ Sign In ]                                   │
│                            │                                                 │
└────────────────────────────┴────────────────────────────────────────────────┘
```

### Lock Screen

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              LOCK SCREEN                                     │
├────────────────────────────┬────────────────────────────────────────────────┤
│                            │                                                 │
│   Station Name             │   [Photo] Jane Doe                              │
│   Register 1               │           Cashier                               │
│                            │                                                 │
│   [Company Logo]           │   Enter PIN to unlock                           │
│                            │                                                 │
│   12:30 PM                 │   ┌──── PIN Entry ────┐                        │
│                            │   │ ● ● ● ●           │                        │
│                            │   │                    │                        │
│                            │   │ [1] [2] [3]        │                        │
│                            │   │ [4] [5] [6]        │                        │
│                            │   │ [7] [8] [9]        │                        │
│                            │   │ [C] [0] [←]        │                        │
│                            │   └────────────────────┘                        │
│                            │                                                 │
│   [Sign Out]               │   [ Verify ]                                    │
│                            │                                                 │
└────────────────────────────┴────────────────────────────────────────────────┘
```

---

## Related Documentation

- [Authentication](./AUTHENTICATION.md) - Token management details
- [Device Registration](./DEVICE_REGISTRATION.md) - Initial device setup
- [Roles and Permissions](./ROLES_AND_PERMISSIONS.md) - Manager approval
- [Screen Layouts](../ui-ux/SCREEN_LAYOUTS.md) - UI specifications
- [Dialogs](../ui-ux/DIALOGS.md) - Logout, till selection dialogs

---

*Last Updated: January 2026*
