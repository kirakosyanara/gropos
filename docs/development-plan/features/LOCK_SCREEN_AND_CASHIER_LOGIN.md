# Lock Screen and Cashier Login System

This document details the lock screen and cashier login functionality for the GroPOS application built with Kotlin, Jetpack Compose, and Couchbase Lite.

---

## Table of Contents

1. [Overview](#overview)
2. [Screen Variations](#screen-variations)
3. [Station States](#station-states)
4. [Till Assignment](#till-assignment)
5. [API Endpoints](#api-endpoints)
6. [State Flow Diagrams](#state-flow-diagrams)
7. [Code References](#code-references)

---

## Overview

GroPOS is a cross-platform Point of Sale application designed to run on:
- **Windows** (desktop)
- **Linux** (desktop)
- **Android** (tablets/devices)

### Technology Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin |
| UI Framework | Jetpack Compose Multiplatform |
| Local Database | Couchbase Lite |
| Architecture | MVVM with StateFlow |
| Navigation | Compose Navigation |
| DI | Koin / Hilt |

### Primary View Components

| Screen | Purpose | Route |
|--------|---------|-------|
| `LoginScreen` | Initial login, device registration, cashier selection | `/login` |
| `LockScreen` | Screen lock after login (manual or auto-lock) | `/lock` |
| `HomeScreen` | Main POS interface | `/home` |

### Key Components

- **NavController**: Handles navigation between `LoginScreen`, `LockScreen`, and `HomeScreen`
- **AppState**: Global application state (logged-in employee, branch info)
- **SessionState**: Current transaction state, including payment list (used to prevent locking during payment)
- **CouchbaseManager**: Local database coordination, document management
- **ApiClient**: Ktor-based HTTP client for API communication

### State Management

```kotlin
// AppState.kt
@Singleton
class AppState @Inject constructor() {
    private val _employee = MutableStateFlow<UserProfile?>(null)
    val employee: StateFlow<UserProfile?> = _employee.asStateFlow()
    
    private val _branch = MutableStateFlow<Branch?>(null)
    val branch: StateFlow<Branch?> = _branch.asStateFlow()
    
    private val _deviceInfo = MutableStateFlow<DeviceInfo?>(null)
    val deviceInfo: StateFlow<DeviceInfo?> = _deviceInfo.asStateFlow()
    
    fun setEmployee(employee: UserProfile?) {
        _employee.value = employee
    }
    
    fun clearStore() {
        _employee.value = null
        _branch.value = null
    }
}
```

---

## Screen Variations

### 1. Splash Screen (Loading)

**When displayed**: Application startup, before database is initialized

**Compose Component**: `SplashScreen`

**Behavior**:
- Shows GroPOS logo and "Welcome to GroPOS" text
- Initializes Couchbase Lite database
- Checks for stored API key
- Transitions to either Registration or Login flow

```kotlin
// SplashScreen.kt
@Composable
fun SplashScreen(
    viewModel: SplashViewModel = koinViewModel(),
    onNavigateToLogin: () -> Unit,
    onNavigateToRegistration: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(uiState) {
        when (uiState) {
            is SplashState.Ready -> {
                if ((uiState as SplashState.Ready).hasApiKey) {
                    onNavigateToLogin()
                } else {
                    onNavigateToRegistration()
                }
            }
            else -> { /* Loading */ }
        }
    }
    
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Welcome to",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                text = "GroPOS",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}
```

```kotlin
// SplashViewModel.kt
class SplashViewModel(
    private val couchbaseManager: CouchbaseManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<SplashState>(SplashState.Loading)
    val uiState: StateFlow<SplashState> = _uiState.asStateFlow()
    
    init {
        viewModelScope.launch {
            initializeDatabase()
        }
    }
    
    private suspend fun initializeDatabase() {
        couchbaseManager.initialize()
        
        val posSystem = couchbaseManager.posSystem.getDocumentById(
            BuildConfig.ENVIRONMENT.name
        )
        
        _uiState.value = SplashState.Ready(hasApiKey = posSystem?.apiKey != null)
    }
}

sealed class SplashState {
    object Loading : SplashState()
    data class Ready(val hasApiKey: Boolean) : SplashState()
}
```

---

### 2. Device Registration Screen (Station is FREE/Unregistered)

**When displayed**: Device has never been registered with a Unisight account

**Compose Component**: `RegistrationScreen`

**Sub-states**:

#### 2a. QR Code Display
- Shows QR code for scanning
- Shows activation URL: `https://www.unisight.io/activate`
- Shows activation code
- User scans QR or enters code on another device

#### 2b. Registration In Progress
- Shows "Connected" header
- Shows "Configuring this station for: [Location Name]"
- Status is polled every 10 seconds

**API Endpoints Used**:

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/Registration/CreateQRCodeRegistration` | POST | Creates QR code and gets access token |
| `/api/Registration/GetDeviceRegistrationStatusById` | GET | Polls registration status |

**Response Models**:

```kotlin
@Serializable
data class CreateQRCodeRegistrationResponse(
    val accessToken: String?,
    val qrCodeImage: String?,  // Base64-encoded
    val url: String?,
    val assignedGuid: String?
)

@Serializable
data class GetDeviceRegistrationStatusByIdResponse(
    val deviceStatus: String?,  // "In-Progress" or completed
    val apiKey: String?,
    val branch: String?
)
```

**UI State**:

```kotlin
// RegistrationViewModel.kt
sealed class RegistrationState {
    object Loading : RegistrationState()
    data class ShowQRCode(
        val qrCodeBitmap: ImageBitmap,
        val activationCode: String,
        val activationUrl: String
    ) : RegistrationState()
    data class InProgress(val branchName: String) : RegistrationState()
    data class Completed(val apiKey: String) : RegistrationState()
    data class Error(val message: String) : RegistrationState()
}

class RegistrationViewModel(
    private val registrationApi: RegistrationApi,
    private val couchbaseManager: CouchbaseManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<RegistrationState>(RegistrationState.Loading)
    val uiState: StateFlow<RegistrationState> = _uiState.asStateFlow()
    
    private var pollingJob: Job? = null
    
    fun startRegistration() {
        viewModelScope.launch {
            try {
                val response = registrationApi.createQRCodeRegistration(
                    version = "v1",
                    macAddress = getDeviceMacAddress()
                )
                
                response.qrCodeImage?.let { base64Image ->
                    val bitmap = decodeBase64ToBitmap(base64Image)
                    _uiState.value = RegistrationState.ShowQRCode(
                        qrCodeBitmap = bitmap,
                        activationCode = extractActivationCode(response.url),
                        activationUrl = "https://www.unisight.io/activate"
                    )
                    
                    response.assignedGuid?.let { guid ->
                        startPolling(guid)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = RegistrationState.Error(e.message ?: "Registration failed")
            }
        }
    }
    
    private fun startPolling(assignedGuid: String) {
        pollingJob = viewModelScope.launch {
            while (isActive) {
                delay(10_000) // Poll every 10 seconds
                
                try {
                    val status = registrationApi.getDeviceRegistrationStatusById(
                        assignedGuid = assignedGuid,
                        version = "v1"
                    )
                    
                    when {
                        status.apiKey != null -> {
                            // Registration complete
                            saveApiKey(status.apiKey)
                            _uiState.value = RegistrationState.Completed(status.apiKey)
                            pollingJob?.cancel()
                        }
                        status.deviceStatus == "In-Progress" -> {
                            _uiState.value = RegistrationState.InProgress(
                                branchName = status.branch ?: ""
                            )
                        }
                    }
                } catch (e: Exception) {
                    // Continue polling on error
                }
            }
        }
    }
    
    private suspend fun saveApiKey(apiKey: String) {
        val posSystem = PosSystemDocument(
            id = BuildConfig.ENVIRONMENT.name,
            documentName = "appKey",
            apiKey = apiKey
        )
        couchbaseManager.posSystem.save(posSystem)
    }
}
```

**Registration Screen UI**:

```kotlin
@Composable
fun RegistrationScreen(
    viewModel: RegistrationViewModel = koinViewModel(),
    onRegistrationComplete: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.startRegistration()
    }
    
    LaunchedEffect(uiState) {
        if (uiState is RegistrationState.Completed) {
            onRegistrationComplete()
        }
    }
    
    Row(modifier = Modifier.fillMaxSize()) {
        // Left Panel - Station Info
        StationInfoPanel(modifier = Modifier.weight(1f))
        
        // Right Panel - Registration Content
        Box(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                is RegistrationState.Loading -> CircularProgressIndicator()
                
                is RegistrationState.ShowQRCode -> QRCodeContent(
                    qrCodeBitmap = state.qrCodeBitmap,
                    activationCode = state.activationCode,
                    activationUrl = state.activationUrl
                )
                
                is RegistrationState.InProgress -> InProgressContent(
                    branchName = state.branchName
                )
                
                is RegistrationState.Error -> ErrorContent(
                    message = state.message,
                    onRetry = { viewModel.startRegistration() }
                )
                
                is RegistrationState.Completed -> {
                    // Navigation handled by LaunchedEffect
                }
            }
        }
    }
}

@Composable
private fun QRCodeContent(
    qrCodeBitmap: ImageBitmap,
    activationCode: String,
    activationUrl: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Welcome to GroPOS",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "To register this station, scan the QR code below or visit:",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        
        Image(
            bitmap = qrCodeBitmap,
            contentDescription = "Registration QR Code",
            modifier = Modifier.size(200.dp)
        )
        
        Text(
            text = activationUrl,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = activationCode,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
    }
}
```

---

### 3. Login Screen (Station is REGISTERED but no user logged in)

**When displayed**: 
- Device is registered (has API key)
- No employee is currently logged in

**Compose Component**: `LoginScreen`

**Sub-states**:

#### 3a. Cashier List View
- Shows list of scheduled cashiers
- Header: "On Site Cashiers"
- Clicking a cashier transitions to PIN entry

#### 3b. PIN Entry View
- Shows selected employee info (name, avatar, role)
- Ten-key pad for PIN entry
- NFC reader option (platform-dependent)
- Back button to return to cashier list

**API Endpoints Used for Login**:

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `GET /api/Employee/GetCashierEmployees` | GET | Get list of cashiers for this location |
| `GET /api/v1/devices/current` | GET | Get current device info including claimed employee |
| `POST /api/Employee/Login` | POST | Authenticate cashier login |
| `GET /api/Employee/GetProfile` | GET | Get logged-in user profile |
| `GET /api/v1/branches/current/settings` | GET | Get branch settings |

**Request Model for Login**:

```kotlin
@Serializable
data class CashierLoginRequest(
    val userName: String,
    val password: String,
    val clientName: String = "device",
    val authenticationKey: String? = null,
    val locationAccountId: Int,  // Till ID (required)
    val branchId: Int,
    val deviceId: Int
)
```

**Login ViewModel**:

```kotlin
// LoginViewModel.kt
sealed class LoginState {
    object Loading : LoginState()
    data class CashierList(
        val cashiers: List<Employee>,
        val deviceInfo: DeviceInfo?
    ) : LoginState()
    data class PinEntry(
        val selectedEmployee: Employee,
        val hasTillAssigned: Boolean
    ) : LoginState()
    data class TillSelection(
        val tills: List<LocationAccount>,
        val selectedEmployee: Employee,
        val enteredPin: String
    ) : LoginState()
    object LoggingIn : LoginState()
    data class Error(val message: String) : LoginState()
}

class LoginViewModel(
    private val employeeApi: EmployeeApi,
    private val deviceApi: DeviceApi,
    private val accountApi: AccountApi,
    private val appState: AppState,
    private val couchbaseManager: CouchbaseManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<LoginState>(LoginState.Loading)
    val uiState: StateFlow<LoginState> = _uiState.asStateFlow()
    
    private var selectedEmployee: Employee? = null
    private var enteredPin: String = ""
    
    fun loadCashiers() {
        viewModelScope.launch {
            try {
                val cashiers = employeeApi.getCashierEmployees()
                val deviceInfo = deviceApi.getCurrentDeviceInfo()
                
                // Store branch info
                appState.setBranch(Branch(
                    id = deviceInfo.branchId,
                    stationId = deviceInfo.id,
                    name = deviceInfo.name
                ))
                
                // Check if station is claimed
                if (deviceInfo.employeeId != null) {
                    val claimedEmployee = cashiers.find { it.id == deviceInfo.employeeId }
                    if (claimedEmployee != null) {
                        selectedEmployee = claimedEmployee.copy(
                            assignedAccountId = deviceInfo.locationAccountId
                        )
                        _uiState.value = LoginState.PinEntry(
                            selectedEmployee = selectedEmployee!!,
                            hasTillAssigned = deviceInfo.locationAccountId != null
                        )
                        return@launch
                    }
                }
                
                _uiState.value = LoginState.CashierList(
                    cashiers = cashiers,
                    deviceInfo = deviceInfo
                )
                
            } catch (e: Exception) {
                _uiState.value = LoginState.Error(e.message ?: "Failed to load cashiers")
            }
        }
    }
    
    fun selectCashier(employee: Employee) {
        selectedEmployee = employee
        _uiState.value = LoginState.PinEntry(
            selectedEmployee = employee,
            hasTillAssigned = employee.assignedAccountId != null && employee.assignedAccountId > 0
        )
    }
    
    fun goBackToCashierList() {
        selectedEmployee = null
        enteredPin = ""
        loadCashiers()
    }
    
    fun submitPin(pin: String) {
        val employee = selectedEmployee ?: return
        enteredPin = pin
        
        if (employee.assignedAccountId != null && employee.assignedAccountId > 0) {
            // Has till assignment - proceed with login
            performLogin(employee.email, pin, employee.assignedAccountId)
        } else {
            // No till assigned - show till selection
            loadTillsForSelection(employee, pin)
        }
    }
    
    private fun loadTillsForSelection(employee: Employee, pin: String) {
        viewModelScope.launch {
            try {
                val tillsResponse = accountApi.getTillAccountList()
                _uiState.value = LoginState.TillSelection(
                    tills = tillsResponse.rows ?: emptyList(),
                    selectedEmployee = employee,
                    enteredPin = pin
                )
            } catch (e: Exception) {
                _uiState.value = LoginState.Error("Failed to load tills")
            }
        }
    }
    
    fun selectTill(tillId: Int) {
        val employee = selectedEmployee ?: return
        selectedEmployee = employee.copy(assignedAccountId = tillId)
        performLogin(employee.email, enteredPin, tillId)
    }
    
    private fun performLogin(email: String, password: String, tillId: Int) {
        viewModelScope.launch {
            _uiState.value = LoginState.LoggingIn
            
            try {
                val branch = appState.branch.value ?: throw Exception("Branch not set")
                
                val loginRequest = CashierLoginRequest(
                    userName = email,
                    password = password,
                    locationAccountId = tillId,
                    branchId = branch.id,
                    deviceId = branch.stationId
                )
                
                val tokenResponse = employeeApi.groPOSLogin(loginRequest)
                
                // Save tokens
                saveTokens(tokenResponse)
                ApiClient.setBearerToken(tokenResponse.accessToken)
                
                // Get profile
                val profile = employeeApi.getProfile()
                appState.setEmployee(profile)
                
                // Load branch settings
                val settings = branchApi.getAllSettings()
                couchbaseManager.branchSettings.saveAll(settings)
                
            } catch (e: Exception) {
                _uiState.value = LoginState.Error(e.message ?: "Login failed")
            }
        }
    }
    
    private suspend fun saveTokens(tokenResponse: TokenViewModel) {
        val posSystem = couchbaseManager.posSystem.getDocumentById(
            BuildConfig.ENVIRONMENT.name
        ) ?: PosSystemDocument(id = BuildConfig.ENVIRONMENT.name)
        
        couchbaseManager.posSystem.save(
            posSystem.copy(refreshToken = tokenResponse.refreshToken)
        )
    }
}
```

**Login Screen UI**:

```kotlin
@Composable
fun LoginScreen(
    viewModel: LoginViewModel = koinViewModel(),
    onLoginSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val employee by viewModel.appState.employee.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadCashiers()
    }
    
    LaunchedEffect(employee) {
        if (employee != null) {
            onLoginSuccess()
        }
    }
    
    Row(modifier = Modifier.fillMaxSize()) {
        // Left Panel - Station Info & Clock
        StationInfoPanel(
            modifier = Modifier.weight(1f)
        )
        
        // Right Panel - Login Content
        Box(
            modifier = Modifier.weight(1f).fillMaxHeight().background(
                MaterialTheme.colorScheme.surface
            ),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                is LoginState.Loading -> CircularProgressIndicator()
                
                is LoginState.CashierList -> CashierListContent(
                    cashiers = state.cashiers,
                    onCashierSelected = { viewModel.selectCashier(it) }
                )
                
                is LoginState.PinEntry -> PinEntryContent(
                    employee = state.selectedEmployee,
                    onPinSubmit = { viewModel.submitPin(it) },
                    onBack = { viewModel.goBackToCashierList() }
                )
                
                is LoginState.TillSelection -> TillSelectionDialog(
                    tills = state.tills,
                    onTillSelected = { viewModel.selectTill(it) },
                    onDismiss = { viewModel.goBackToCashierList() }
                )
                
                is LoginState.LoggingIn -> CircularProgressIndicator()
                
                is LoginState.Error -> ErrorContent(
                    message = state.message,
                    onRetry = { viewModel.loadCashiers() }
                )
            }
        }
    }
}

@Composable
private fun CashierListContent(
    cashiers: List<Employee>,
    onCashierSelected: (Employee) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp)
    ) {
        Text(
            text = "On Site Cashiers",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(cashiers) { cashier ->
                CashierListItem(
                    employee = cashier,
                    onClick = { onCashierSelected(cashier) }
                )
            }
        }
    }
}

@Composable
private fun PinEntryContent(
    employee: Employee,
    onPinSubmit: (String) -> Unit,
    onBack: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Sign In using your PIN",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        EmployeeInfoCard(employee = employee)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // PIN display (masked)
        PinDisplay(pinLength = pin.length)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Ten-key pad
        TenKeyPad(
            onDigit = { digit -> 
                if (pin.length < 8) pin += digit 
            },
            onClear = { pin = "" },
            onBackspace = { 
                if (pin.isNotEmpty()) pin = pin.dropLast(1) 
            }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(onClick = onBack) {
                Text("Back")
            }
            
            Button(
                onClick = { onPinSubmit(pin) },
                enabled = pin.length >= 4
            ) {
                Text("Sign In")
            }
        }
    }
}
```

---

### 4. Station Claimed (Employee Already Assigned)

**When displayed**: Device info shows an `employeeId` is already assigned to this station

**Behavior**:
When `deviceGetCurrentDeviceInfo()` returns a `DeviceInfo` with:
- `employeeId` != null
- `locationAccountId` != null

The login screen pre-selects the claimed employee and auto-populates their till assignment:

```kotlin
// In LoginViewModel.loadCashiers()
val deviceInfo = deviceApi.getCurrentDeviceInfo()

if (deviceInfo.employeeId != null) {
    val claimedEmployee = cashiers.find { it.id == deviceInfo.employeeId }
    if (claimedEmployee != null) {
        selectedEmployee = claimedEmployee.copy(
            assignedAccountId = deviceInfo.locationAccountId
        )
        _uiState.value = LoginState.PinEntry(
            selectedEmployee = selectedEmployee!!,
            hasTillAssigned = deviceInfo.locationAccountId != null
        )
        return@launch
    }
}
```

**DeviceInfo Fields for Claiming**:

```kotlin
@Serializable
data class DeviceInfo(
    val id: Int,
    val branchId: Int,
    val branch: String?,
    val name: String?,
    val location: String?,
    val employeeId: Int? = null,        // ID of claimed employee (null if free)
    val employee: String? = null,        // Name of claimed employee
    val locationAccountId: Int? = null,  // Assigned till account ID
    val locationAccount: String? = null, // Till account name
    val lastHeartbeat: String? = null
)
```

---

### 5. Lock Screen (Station is LOCKED)

**When displayed**:
- User manually presses lock button or keyboard shortcut
- Inactivity timeout (5 minutes) triggers auto-lock
- Always shows the **currently logged-in employee** (not a list)

**Compose Component**: `LockScreen`

**UI Components**:
- Left panel: Station name, current time, version info
- Right panel:
  - Employee info (current logged-in user)
  - Ten-key PIN entry
  - "Verify" button
  - "Sign Out" button (force logout)

**Lock Types** (DeviceEventType enum):

| Value | Code | Trigger |
|-------|------|---------|
| `Locked` | 4 | Manual lock (keyboard shortcut or lock button) |
| `Unlocked` | 5 | Successful PIN verification |
| `AutoLocked` | 6 | Inactivity timeout |

```kotlin
@Serializable
enum class DeviceEventType(val value: Int) {
    SignedIn(0),
    SignedOut(1),
    ClockedIn(2),
    ClockedOut(3),
    Locked(4),
    Unlocked(5),
    AutoLocked(6)
}
```

**Lock Screen ViewModel**:

```kotlin
// LockViewModel.kt
sealed class LockState {
    object Idle : LockState()
    object Verifying : LockState()
    data class Error(val message: String) : LockState()
    object Unlocked : LockState()
    object ShowLogoutDialog : LockState()
    object ShowManagerApproval : LockState()
    object LoggingOut : LockState()
}

class LockViewModel(
    private val employeeApi: EmployeeApi,
    private val deviceApi: DeviceApi,
    private val appState: AppState,
    private val sessionState: SessionState
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<LockState>(LockState.Idle)
    val uiState: StateFlow<LockState> = _uiState.asStateFlow()
    
    val currentEmployee: StateFlow<UserProfile?> = appState.employee
    val deviceInfo: StateFlow<DeviceInfo?> = appState.deviceInfo
    
    fun verifyPin(pin: String) {
        val employee = currentEmployee.value ?: return
        
        viewModelScope.launch {
            _uiState.value = LockState.Verifying
            
            try {
                val request = LoginRequest(
                    userName = employee.email,
                    password = pin,
                    clientName = "device",
                    branchId = appState.branch.value?.id,
                    deviceId = appState.branch.value?.stationId
                )
                
                val success = employeeApi.verifyPassword(request)
                
                if (success) {
                    // Unlock the device
                    unlockDevice()
                    _uiState.value = LockState.Unlocked
                } else {
                    _uiState.value = LockState.Error("Invalid PIN. Please try again.")
                }
            } catch (e: HttpException) {
                if (e.code() == 401) {
                    // Token expired - try to refresh
                    if (refreshToken()) {
                        verifyPin(pin) // Retry
                    } else {
                        _uiState.value = LockState.Error("Session expired. Please log in again.")
                    }
                } else {
                    _uiState.value = LockState.Error(e.message ?: "Verification failed")
                }
            }
        }
    }
    
    private suspend fun unlockDevice() {
        val request = DeviceLockRequest(lockType = DeviceEventType.Unlocked)
        employeeApi.lockDevice(request)
    }
    
    fun showLogoutDialog() {
        _uiState.value = LockState.ShowLogoutDialog
    }
    
    fun hideLogoutDialog() {
        _uiState.value = LockState.Idle
    }
    
    fun requestLogout(isEndOfShift: Boolean) {
        _uiState.value = LockState.ShowManagerApproval
    }
    
    fun onManagerApprovalSuccess(isEndOfShift: Boolean) {
        viewModelScope.launch {
            _uiState.value = LockState.LoggingOut
            
            try {
                unlockDevice()
                
                if (isEndOfShift) {
                    employeeApi.logoutWithEndOfShift()
                } else {
                    employeeApi.logout()
                }
                
                // Open cash drawer (platform-specific)
                HardwareManager.openCashDrawer()
                
                appState.clearStore()
                sessionState.clearSession()
                
            } catch (e: Exception) {
                _uiState.value = LockState.Error("Logout failed: ${e.message}")
            }
        }
    }
    
    private suspend fun refreshToken(): Boolean {
        return try {
            val posSystem = couchbaseManager.posSystem.getDocumentById(
                BuildConfig.ENVIRONMENT.name
            ) ?: return false
            
            val refreshTokenModel = RefreshTokenModel(
                token = posSystem.refreshToken ?: return false,
                clientName = "device"
            )
            
            val tokenResponse = employeeApi.refreshToken(refreshTokenModel)
            
            couchbaseManager.posSystem.save(
                posSystem.copy(refreshToken = tokenResponse.refreshToken)
            )
            ApiClient.setBearerToken(tokenResponse.accessToken)
            
            true
        } catch (e: Exception) {
            false
        }
    }
}
```

**Lock Screen UI**:

```kotlin
@Composable
fun LockScreen(
    viewModel: LockViewModel = koinViewModel(),
    onUnlock: () -> Unit,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val employee by viewModel.currentEmployee.collectAsState()
    val deviceInfo by viewModel.deviceInfo.collectAsState()
    
    var pin by remember { mutableStateOf("") }
    
    LaunchedEffect(uiState) {
        when (uiState) {
            is LockState.Unlocked -> onUnlock()
            is LockState.LoggingOut -> onLogout()
            else -> {}
        }
    }
    
    Row(modifier = Modifier.fillMaxSize()) {
        // Left Panel - Station Info & Clock
        StationInfoPanel(
            stationName = deviceInfo?.name ?: "",
            location = deviceInfo?.location ?: "",
            modifier = Modifier.weight(1f)
        )
        
        // Right Panel - Lock Content
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Station Locked",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            employee?.let { emp ->
                EmployeeInfoCard(
                    name = "${emp.firstName} ${emp.lastName}",
                    role = emp.role ?: "Cashier",
                    imageUrl = emp.imageUrl
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // PIN display
            PinDisplay(pinLength = pin.length)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Ten-key pad
            TenKeyPad(
                onDigit = { if (pin.length < 8) pin += it },
                onClear = { pin = "" },
                onBackspace = { if (pin.isNotEmpty()) pin = pin.dropLast(1) }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Error message
            if (uiState is LockState.Error) {
                Text(
                    text = (uiState as LockState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.showLogoutDialog() }
                ) {
                    Text("Sign Out")
                }
                
                Button(
                    onClick = { 
                        viewModel.verifyPin(pin)
                        pin = ""
                    },
                    enabled = pin.length >= 4 && uiState !is LockState.Verifying
                ) {
                    if (uiState is LockState.Verifying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Verify")
                    }
                }
            }
        }
    }
    
    // Logout Dialog
    if (uiState is LockState.ShowLogoutDialog) {
        LogoutOptionsDialog(
            onDismiss = { viewModel.hideLogoutDialog() },
            onLogout = { viewModel.requestLogout(isEndOfShift = false) },
            onEndOfShift = { viewModel.requestLogout(isEndOfShift = true) }
        )
    }
    
    // Manager Approval Dialog
    if (uiState is LockState.ShowManagerApproval) {
        ManagerApprovalDialog(
            permission = "GrowPOS.Store.Force Sign Out",
            onApproved = { isEndOfShift -> 
                viewModel.onManagerApprovalSuccess(isEndOfShift) 
            },
            onDismiss = { viewModel.hideLogoutDialog() }
        )
    }
}
```

---

### 6. Inactivity Auto-Lock

**Implementation**:

The inactivity timer is managed at the app level using coroutines:

```kotlin
// InactivityManager.kt
class InactivityManager(
    private val employeeApi: EmployeeApi,
    private val sessionState: SessionState,
    private val navController: NavController,
    private val coroutineScope: CoroutineScope
) {
    private var inactivityJob: Job? = null
    private val inactivityTimeout = 5.minutes
    
    fun resetTimer() {
        inactivityJob?.cancel()
        
        // Don't start timer if already on login/lock screen or payments in progress
        if (navController.currentDestination?.route in listOf("login", "lock", "registration")) {
            return
        }
        
        if (sessionState.paymentList.value.isNotEmpty()) {
            return
        }
        
        inactivityJob = coroutineScope.launch {
            delay(inactivityTimeout)
            autoLock()
        }
    }
    
    private suspend fun autoLock() {
        try {
            val request = DeviceLockRequest(lockType = DeviceEventType.AutoLocked)
            employeeApi.lockDevice(request)
            
            withContext(Dispatchers.Main) {
                navController.navigate("lock") {
                    popUpTo("home") { inclusive = false }
                }
            }
        } catch (e: Exception) {
            // Handle error - retry with token refresh if 401
        }
    }
}

// Usage in App.kt
@Composable
fun GroPOSApp() {
    val navController = rememberNavController()
    val inactivityManager = remember { InactivityManager(...) }
    
    // Reset timer on any user interaction
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent()
                        inactivityManager.resetTimer()
                    }
                }
            }
    ) {
        NavHost(navController = navController, startDestination = "splash") {
            composable("splash") { SplashScreen(...) }
            composable("registration") { RegistrationScreen(...) }
            composable("login") { LoginScreen(...) }
            composable("lock") { LockScreen(...) }
            composable("home") { HomeScreen(...) }
        }
    }
}
```

---

## Station States

| State | Description | UI Shown | Route |
|-------|-------------|----------|-------|
| **UNREGISTERED** | No API key in local storage | QR Registration | `/registration` |
| **FREE** | Registered, no employee claimed | Cashier List | `/login` |
| **CLAIMED** | `employeeId` set on device | Pre-selected cashier | `/login` (PIN entry) |
| **LOGGED_IN** | Active session | Home screen | `/home` |
| **LOCKED** | Session active but locked | Lock screen | `/lock` |

---

## Till Assignment

### What is a Till?

A Till (LocationAccount) represents a cash drawer/register account that employees are assigned to for tracking cash accountability.

### Till Assignment Check

Before a cashier can log in, they must be assigned to a till:

```kotlin
fun submitPin(pin: String) {
    val employee = selectedEmployee ?: return
    enteredPin = pin
    
    if (employee.assignedAccountId != null && employee.assignedAccountId > 0) {
        // Has till assignment - proceed with login
        performLogin(employee.email, pin, employee.assignedAccountId)
    } else {
        // No till assigned - show till selection
        loadTillsForSelection(employee, pin)
    }
}
```

### Getting Available Tills

**Endpoint**: `GET /api/account/GetTillAccountList`

**Response Model**:

```kotlin
@Serializable
data class GridDataOfLocationAccountListViewModel(
    val rows: List<LocationAccount>? = null,
    val totalCount: Int? = null
)

@Serializable
data class LocationAccount(
    val locationAccountId: Int,
    val accountName: String?,
    val assignedEmployeeId: Int? = null,
    val employeeName: String? = null,
    val currentBalance: Double? = null
)
```

### Till Selection UI

**Dialog**: `TillSelectionDialog`

**Two Modes**:

1. **Touch Mode** (Default on Android/Touch devices):
   - Shows list of available tills
   - User taps to select
   
2. **Scanner Mode** (Hardware with barcode scanner):
   - Opens cash drawer
   - Waits for barcode scan of till label
   - Validates scanned ID against available tills

```kotlin
@Composable
fun TillSelectionDialog(
    tills: List<LocationAccount>,
    onTillSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
    useScannerMode: Boolean = HardwareManager.hasBarcodeScanner()
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Till") },
        text = {
            if (useScannerMode) {
                ScannerTillSelection(
                    availableTills = tills,
                    onTillScanned = onTillSelected,
                    onError = { /* Show error */ }
                )
            } else {
                LazyColumn {
                    items(tills) { till ->
                        TillListItem(
                            till = till,
                            onClick = { onTillSelected(till.locationAccountId) }
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ScannerTillSelection(
    availableTills: List<LocationAccount>,
    onTillScanned: (Int) -> Unit,
    onError: (String) -> Unit
) {
    val barcodeScanner = remember { HardwareManager.getBarcodeScanner() }
    
    DisposableEffect(Unit) {
        barcodeScanner?.setCallback { scannedValue ->
            try {
                val tillId = scannedValue.toInt()
                val isValid = availableTills.any { it.locationAccountId == tillId }
                
                if (isValid) {
                    onTillScanned(tillId)
                } else {
                    onError("Invalid till. Please scan a valid till barcode.")
                }
            } catch (e: NumberFormatException) {
                onError("Invalid barcode format")
            }
        }
        
        // Open cash drawer
        HardwareManager.openCashDrawer()
        
        onDispose {
            barcodeScanner?.clearCallback()
        }
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_barcode),
            contentDescription = null,
            modifier = Modifier.size(64.dp)
        )
        Text("Scan till barcode to continue")
    }
}
```

---

## API Endpoints

### Device/Station Endpoints

| Endpoint | Method | Purpose | Request | Response |
|----------|--------|---------|---------|----------|
| `GET /api/v1/devices/current` | GET | Get current device info | - | `DeviceInfo` |
| `POST /api/Employee/LockDevice` | POST | Lock/unlock device | `DeviceLockRequest` | `DeviceEventViewModel` |

### Authentication Endpoints

| Endpoint | Method | Purpose | Request | Response |
|----------|--------|---------|---------|----------|
| `GET /api/Employee/GetCashierEmployees` | GET | Get scheduled cashiers | - | `List<EmployeeListViewModel>` |
| `POST /api/Employee/Login` | POST | Cashier login | `CashierLoginRequest` | `TokenViewModel` |
| `POST /api/Employee/VerifyPassword` | POST | Verify PIN on lock screen | `LoginRequest` | `Boolean` |
| `GET /api/Employee/GetProfile` | GET | Get logged-in user profile | - | `UserProfileViewModel` |
| `POST /api/Employee/Logout` | POST | Logout current session | - | - |
| `POST /api/Employee/LogoutWithEndOfShift` | POST | Logout with end-of-shift flag | - | - |
| `POST /api/Employee/RefreshToken` | POST | Refresh access token | `RefreshTokenModel` | `TokenViewModel` |

### Account/Till Endpoints

| Endpoint | Method | Purpose | Request | Response |
|----------|--------|---------|---------|----------|
| `GET /api/account/GetTillAccountList` | GET | Get available tills | - | `GridDataOfLocationAccountListViewModel` |

### Registration Endpoints

| Endpoint | Method | Purpose | Request | Response |
|----------|--------|---------|---------|----------|
| `POST /api/Registration/CreateQRCodeRegistration` | POST | Create QR code for registration | version, macAddress | `CreateQRCodeRegistrationResponse` |
| `GET /api/Registration/GetDeviceRegistrationStatusById` | GET | Poll registration status | assignedGuid, version | `GetDeviceRegistrationStatusByIdResponse` |

---

## State Flow Diagrams

### Application Startup Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                         APPLICATION START                           │
└─────────────────────────────┬───────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│              CouchbaseLite.init() / CouchbaseManager.start()        │
└─────────────────────────────┬───────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      Show Splash Screen                             │
│                  (Wait for database init)                           │
└─────────────────────────────┬───────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│             Check for ApiKey in CouchbaseLite                       │
└──────────────────┬──────────────────────────────────────────────────┘
                   │
       ┌───────────┴───────────┐
       │                       │
       ▼                       ▼
┌──────────────┐      ┌────────────────────┐
│ No ApiKey    │      │ Has ApiKey         │
│ (Unregistered)│      │ (Registered)       │
└──────┬───────┘      └─────────┬──────────┘
       │                        │
       ▼                        ▼
┌──────────────┐      ┌────────────────────┐
│ Navigate to  │      │ Navigate to        │
│ /registration│      │ /login             │
└──────────────┘      └────────────────────┘
```

### Login and Lock Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                         /login                                      │
│                    (Cashier List)                                   │
└─────────────────────────────┬───────────────────────────────────────┘
                              │ Select Cashier
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      PIN Entry State                                │
└─────────────────────────────┬───────────────────────────────────────┘
                              │ Enter PIN
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│             Check: assignedAccountId > 0 ?                          │
└──────────────────┬──────────────────────────────────────────────────┘
                   │
       ┌───────────┴───────────┐
       │                       │
       ▼                       ▼
┌──────────────┐      ┌────────────────────┐
│ Has Till     │      │ No Till            │
│ → Login      │      │ → TillSelection    │
└──────┬───────┘      │   Dialog           │
       │              └─────────┬──────────┘
       │                        │ Select Till
       │◄───────────────────────┘
       │
       ▼
┌──────────────────────────────────────────────────────────────────────┐
│                    POST /Employee/Login                              │
│                    (CashierLoginRequest with tillId)                 │
└─────────────────────────────┬────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────────┐
│                         Navigate to /home                            │
└─────────────────────────────┬────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
        ▼                     ▼                     ▼
┌───────────────┐    ┌─────────────────┐   ┌──────────────────┐
│ Lock Button   │    │ 5 min Inactivity│   │ Keyboard         │
│ in Header     │    │ (paymentList    │   │ Shortcut         │
│               │    │  empty)         │   │                  │
└───────┬───────┘    └────────┬────────┘   └────────┬─────────┘
        │                     │                     │
        ▼                     ▼                     ▼
┌──────────────────────────────────────────────────────────────────────┐
│            POST /Employee/LockDevice                                 │
│            DeviceLockRequest { lockType: Locked | AutoLocked }       │
└─────────────────────────────┬────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────────┐
│                         Navigate to /lock                            │
│                 (Shows current logged-in employee)                   │
└─────────────────────────────┬────────────────────────────────────────┘
                              │
            ┌─────────────────┴─────────────────┐
            │                                   │
            ▼                                   ▼
    ┌───────────────┐                  ┌────────────────┐
    │ Enter PIN     │                  │ Sign Out Button│
    │ Click Verify  │                  │                │
    └───────┬───────┘                  └────────┬───────┘
            │                                   │
            ▼                                   ▼
    ┌───────────────────────┐          ┌────────────────────┐
    │ POST /VerifyPassword  │          │ LogoutOptionsDialog│
    └───────┬───────────────┘          └────────┬───────────┘
            │                                   │
            ▼                                   ▼
    ┌───────────────────────┐          ┌────────────────────┐
    │ POST /LockDevice      │          │ ManagerApproval    │
    │ { lockType: Unlocked }│          │ Dialog             │
    └───────┬───────────────┘          └────────┬───────────┘
            │                                   │
            ▼                                   ▼
    ┌───────────────┐                  ┌────────────────────┐
    │ Navigate to   │                  │ POST /Logout or    │
    │ /home         │                  │ /LogoutWithEndOf   │
    └───────────────┘                  │ Shift              │
                                       └────────┬───────────┘
                                                │
                                                ▼
                                       ┌────────────────────┐
                                       │ appState.clearStore│
                                       │ Navigate to /login │
                                       └────────────────────┘
```

---

## Code References

### Main Classes

| Class | Path | Purpose |
|-------|------|---------|
| `SplashScreen` | `app/src/main/kotlin/com/unisight/gropos/ui/splash/SplashScreen.kt` | Splash screen composable |
| `SplashViewModel` | `app/src/main/kotlin/com/unisight/gropos/ui/splash/SplashViewModel.kt` | Splash logic |
| `RegistrationScreen` | `app/src/main/kotlin/com/unisight/gropos/ui/registration/RegistrationScreen.kt` | Registration composable |
| `RegistrationViewModel` | `app/src/main/kotlin/com/unisight/gropos/ui/registration/RegistrationViewModel.kt` | Registration logic |
| `LoginScreen` | `app/src/main/kotlin/com/unisight/gropos/ui/login/LoginScreen.kt` | Login screen composable |
| `LoginViewModel` | `app/src/main/kotlin/com/unisight/gropos/ui/login/LoginViewModel.kt` | Login logic |
| `LockScreen` | `app/src/main/kotlin/com/unisight/gropos/ui/lock/LockScreen.kt` | Lock screen composable |
| `LockViewModel` | `app/src/main/kotlin/com/unisight/gropos/ui/lock/LockViewModel.kt` | Lock/unlock logic |
| `AppState` | `app/src/main/kotlin/com/unisight/gropos/state/AppState.kt` | Global app state |
| `SessionState` | `app/src/main/kotlin/com/unisight/gropos/state/SessionState.kt` | Transaction state |
| `InactivityManager` | `app/src/main/kotlin/com/unisight/gropos/util/InactivityManager.kt` | Auto-lock timer |
| `CouchbaseManager` | `app/src/main/kotlin/com/unisight/gropos/data/local/CouchbaseManager.kt` | Local database |
| `HardwareManager` | `app/src/main/kotlin/com/unisight/gropos/hardware/HardwareManager.kt` | Hardware abstraction |

### Composable Components

| Component | Path | Purpose |
|-----------|------|---------|
| `StationInfoPanel` | `app/src/main/kotlin/com/unisight/gropos/ui/components/StationInfoPanel.kt` | Left panel with station info |
| `TenKeyPad` | `app/src/main/kotlin/com/unisight/gropos/ui/components/TenKeyPad.kt` | PIN entry keypad |
| `EmployeeInfoCard` | `app/src/main/kotlin/com/unisight/gropos/ui/components/EmployeeInfoCard.kt` | Employee display card |
| `CashierListItem` | `app/src/main/kotlin/com/unisight/gropos/ui/login/CashierListItem.kt` | Cashier list row |
| `TillSelectionDialog` | `app/src/main/kotlin/com/unisight/gropos/ui/login/TillSelectionDialog.kt` | Till selection dialog |
| `LogoutOptionsDialog` | `app/src/main/kotlin/com/unisight/gropos/ui/lock/LogoutOptionsDialog.kt` | Logout options |
| `ManagerApprovalDialog` | `app/src/main/kotlin/com/unisight/gropos/ui/components/ManagerApprovalDialog.kt` | Manager approval |

### Key Constants

```kotlin
// InactivityManager.kt
val INACTIVITY_TIMEOUT = 5.minutes

// RegistrationViewModel.kt  
const val POLLING_INTERVAL = 10_000L // 10 seconds
const val REGISTRATION_TIMEOUT = 10 * 60 * 1000L // 10 minutes

// API Endpoints
object Endpoints {
    const val CASHIER_EMPLOYEES = "/api/Employee/GetCashierEmployees"
    const val DEVICE_CURRENT = "/api/v1/devices/current"
    const val LOGIN = "/api/Employee/Login"
    const val VERIFY_PASSWORD = "/api/Employee/VerifyPassword"
    const val LOCK_DEVICE = "/api/Employee/LockDevice"
    const val LOGOUT = "/api/Employee/Logout"
    const val LOGOUT_END_OF_SHIFT = "/api/Employee/LogoutWithEndOfShift"
    const val TILL_LIST = "/api/account/GetTillAccountList"
}
```

---

## Permissions

| Permission | Purpose |
|------------|---------|
| `GrowPOS.Store.Force Sign Out` | Required for manager approval on force logout/end of shift |

---

## Platform-Specific Considerations

### Windows/Linux (Desktop)

```kotlin
// Keyboard shortcuts for desktop
@Composable
fun DesktopKeyboardShortcuts(
    onLock: () -> Unit,
    onShowFunctions: () -> Unit
) {
    LaunchedEffect(Unit) {
        window.addKeyEventDispatcher { event ->
            when {
                event.keyCode == KeyEvent.VK_F4 -> {
                    onLock()
                    true
                }
                event.keyCode == KeyEvent.VK_F1 -> {
                    onShowFunctions()
                    true
                }
                else -> false
            }
        }
    }
}
```

### Android

```kotlin
// Android-specific hardware access
actual object HardwareManager {
    actual fun openCashDrawer() {
        // Android USB/Bluetooth drawer communication
        val drawerService = CashDrawerService.getInstance()
        drawerService?.openDrawer()
    }
    
    actual fun hasBarcodeScanner(): Boolean {
        return context.packageManager.hasSystemFeature("android.hardware.usb.host")
    }
    
    actual fun getBarcodeScanner(): BarcodeScanner? {
        return AndroidBarcodeScanner(context)
    }
}
```

---

## Error Handling

### Token Expiration (401)

```kotlin
suspend fun <T> apiCallWithRetry(
    call: suspend () -> T,
    onTokenRefresh: suspend () -> Boolean
): T {
    return try {
        call()
    } catch (e: HttpException) {
        if (e.code() == 401) {
            if (onTokenRefresh()) {
                call() // Retry after token refresh
            } else {
                throw SessionExpiredException()
            }
        } else {
            throw e
        }
    }
}
```

### Payment in Progress

```kotlin
// InactivityManager.kt
fun resetTimer() {
    // Don't lock if payments in progress
    if (sessionState.paymentList.value.isNotEmpty()) {
        return
    }
    
    // ... start timer
}
```

---

## Related Documentation

- [Device Registration](./DEVICE_REGISTRATION.md)
- [Authentication](../../features/AUTHENTICATION.md)
- [Cashier Operations](./CASHIER_OPERATIONS.md)
- [Hardware Integration](../../modules/hardware/README.md)

