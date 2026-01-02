# Device Registration

**Version:** 2.0 (Kotlin)  
**Status:** Complete Specification Document

This document describes the complete device registration flow for GroPOS, including QR code generation, status polling, API key provisioning, and the impact on application functionality.

---

## Table of Contents

- [Overview](#overview)
- [Registration Flow](#registration-flow)
- [Architecture](#architecture)
- [API Endpoints](#api-endpoints)
- [Data Models](#data-models)
- [Implementation Details](#implementation-details)
- [Post-Registration Setup](#post-registration-setup)
- [Error Handling](#error-handling)
- [Security Considerations](#security-considerations)

---

## Overview

GroPOS devices must be registered with the backend before they can process transactions. Registration associates the device with a specific **branch** (store location) and provides an **API key** that authenticates all subsequent API requests.

### Registration States

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      DEVICE REGISTRATION STATES                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   ┌──────────────┐    ┌──────────────┐    ┌──────────────┐                 │
│   │  UNREGISTERED│───▶│  IN-PROGRESS │───▶│  REGISTERED  │                 │
│   │              │    │              │    │              │                 │
│   │ • No API key │    │ • Admin      │    │ • API key    │                 │
│   │ • QR shown   │    │   scanning   │    │   received   │                 │
│   │ • Polling    │    │ • Branch     │    │ • Ready      │                 │
│   │   started    │    │   assigned   │    │   for login  │                 │
│   └──────────────┘    └──────────────┘    └──────────────┘                 │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### What Registration Provides

| Component | Before Registration | After Registration |
|-----------|--------------------|--------------------|
| API Key | None | Unique device API key |
| Branch Assignment | None | Associated with specific store |
| API Access | Registration endpoints only | Full API access |
| Data Sync | Not available | Products, taxes, categories, etc. |
| Transaction Processing | Not available | Full POS functionality |

---

## Registration Flow

### Complete Flow Diagram

```
┌────────────────────────────────────────────────────────────────────────────┐
│                        DEVICE REGISTRATION FLOW                             │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ STEP 1: App Launch (First Time)                                     │   │
│  │                                                                     │   │
│  │   • Check local database for existing API key                       │   │
│  │   • If no PosSystemViewModel found → Start registration             │   │
│  │   • If API key exists → Skip to login flow                          │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                              │                                              │
│                              ▼                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ STEP 2: Request QR Code                                             │   │
│  │                                                                     │   │
│  │   POST /device-registration/qr-registration                         │   │
│  │   Body: { version: "1.0" }                                          │   │
│  │                                                                     │   │
│  │   Response:                                                         │   │
│  │   {                                                                 │   │
│  │     "url": "https://admin.gropos.com/register/ABC123",              │   │
│  │     "qrCodeImage": "base64EncodedPngImage...",                      │   │
│  │     "accessToken": "tempToken123...",                               │   │
│  │     "assignedGuid": "550e8400-e29b-41d4-a716-446655440000"          │   │
│  │   }                                                                 │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                              │                                              │
│                              ▼                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ STEP 3: Display QR Code                                             │   │
│  │                                                                     │   │
│  │   • Decode base64 QR image and display on screen                    │   │
│  │   • Show activation code (last part of URL) for manual entry        │   │
│  │   • Start status polling (every 10 seconds for 10 minutes)          │   │
│  │                                                                     │   │
│  │   ┌─────────────────────────────────────────────────────┐           │   │
│  │   │          Welcome to GroPOS                          │           │   │
│  │   │                                                     │           │   │
│  │   │           ┌───────────────┐                         │           │   │
│  │   │           │   [QR Code]   │                         │           │   │
│  │   │           └───────────────┘                         │           │   │
│  │   │                                                     │           │   │
│  │   │        Activation Code: ABC123                      │           │   │
│  │   │                                                     │           │   │
│  │   │   Scan with admin app or visit URL                  │           │   │
│  │   └─────────────────────────────────────────────────────┘           │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                              │                                              │
│                              ▼                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ STEP 4: Admin Scans QR / Enters Code                                │   │
│  │                                                                     │   │
│  │   • Store manager/admin scans QR with admin mobile app              │   │
│  │   • Or enters activation code at admin.gropos.com                   │   │
│  │   • Selects which branch to assign the device to                    │   │
│  │   • Backend updates device status to "In-Progress"                  │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                              │                                              │
│                              ▼                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ STEP 5: Poll for Status                                             │   │
│  │                                                                     │   │
│  │   GET /device-registration/device-status/{assignedGuid}             │   │
│  │                                                                     │   │
│  │   Response (In-Progress):                                           │   │
│  │   {                                                                 │   │
│  │     "deviceStatus": "In-Progress",                                  │   │
│  │     "branch": "Main Street Store"                                   │   │
│  │   }                                                                 │   │
│  │                                                                     │   │
│  │   Response (Complete):                                              │   │
│  │   {                                                                 │   │
│  │     "deviceStatus": "Registered",                                   │   │
│  │     "apiKey": "YOUR_API_KEY_HERE",                            │   │
│  │     "branchId": 1,                                                  │   │
│  │     "branch": "Main Street Store"                                   │   │
│  │   }                                                                 │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                              │                                              │
│                              ▼                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ STEP 6: Save API Key Locally                                        │   │
│  │                                                                     │   │
│  │   • Stop status polling scheduler                                   │   │
│  │   • Save PosSystemViewModel to CouchbaseLite:                       │   │
│  │     {                                                               │   │
│  │       id: "PRODUCTION",                                             │   │
│  │       documentName: "appKey",                                       │   │
│  │       branchName: "Main Street Store",                              │   │
│  │       apiKey: "YOUR_API_KEY_HERE"                             │   │
│  │     }                                                               │   │
│  │   • Configure API client with API key header                        │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                              │                                              │
│                              ▼                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ STEP 7: Initial Data Load                                           │   │
│  │                                                                     │   │
│  │   • Show "Initializing Database..." progress dialog                 │   │
│  │   • Load all data from backend:                                     │   │
│  │     - Products, Categories, Taxes, CRV                              │   │
│  │     - Customer Groups, Lookup Categories                            │   │
│  │     - Conditional Sales (age restrictions)                          │   │
│  │   • Start HeartbeatJobScheduler                                     │   │
│  │   • Start SyncFailedTransactionScheduler                            │   │
│  │   • Proceed to employee login screen                                │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└────────────────────────────────────────────────────────────────────────────┘
```

---

## Architecture

### Component Diagram

```
┌────────────────────────────────────────────────────────────────────────────┐
│                      REGISTRATION ARCHITECTURE                              │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                         UI LAYER                                     │   │
│  │                                                                      │   │
│  │   ┌─────────────────┐    ┌─────────────────┐                        │   │
│  │   │ RegistrationView│    │   QR Code       │                        │   │
│  │   │ (Compose)       │    │   Display       │                        │   │
│  │   │                 │    │                 │                        │   │
│  │   │ • QR image      │    │ • Activation    │                        │   │
│  │   │ • Status text   │    │   code          │                        │   │
│  │   │ • Refresh btn   │    │ • Branch name   │                        │   │
│  │   └────────┬────────┘    └─────────────────┘                        │   │
│  └────────────┼─────────────────────────────────────────────────────────┘   │
│               │                                                             │
│               ▼                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                       VIEW MODEL LAYER                               │   │
│  │                                                                      │   │
│  │   ┌─────────────────────────────────────────────────────────────┐   │   │
│  │   │                 RegistrationViewModel                        │   │   │
│  │   │                                                              │   │   │
│  │   │   • _registrationState: StateFlow<RegistrationState>         │   │   │
│  │   │   • _qrCodeImage: StateFlow<String?>                         │   │   │
│  │   │   • _activationCode: StateFlow<String>                       │   │   │
│  │   │   • _branchName: StateFlow<String>                           │   │   │
│  │   │                                                              │   │   │
│  │   │   + startRegistration()                                      │   │   │
│  │   │   + refreshQrCode()                                          │   │   │
│  │   │   + stopPolling()                                            │   │   │
│  │   └──────────────────────────────┬──────────────────────────────┘   │   │
│  └──────────────────────────────────┼───────────────────────────────────┘   │
│                                     │                                       │
│                                     ▼                                       │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                       SERVICE LAYER                                  │   │
│  │                                                                      │   │
│  │   ┌─────────────────────────────────────────────────────────────┐   │   │
│  │   │                 RegistrationService                          │   │   │
│  │   │                                                              │   │   │
│  │   │   + requestQrCode(): CreateQRCodeRegistrationResponse        │   │   │
│  │   │   + checkStatus(guid): GetDeviceRegistrationStatusByIdResponse│  │   │
│  │   │   + saveApiKey(apiKey, branchName)                           │   │   │
│  │   │   + configureApiClient(apiKey)                               │   │   │
│  │   └──────────────────────────────┬──────────────────────────────┘   │   │
│  └──────────────────────────────────┼───────────────────────────────────┘   │
│                                     │                                       │
│              ┌──────────────────────┴────────────────────┐                  │
│              ▼                                           ▼                  │
│  ┌────────────────────────────┐          ┌────────────────────────────┐    │
│  │        Backend API         │          │      CouchbaseLite         │    │
│  │                            │          │                            │    │
│  │  /device-registration/     │          │  PosSystem collection      │    │
│  │    • qr-registration       │          │  { apiKey, branchName }    │    │
│  │    • device-status/{guid}  │          │                            │    │
│  │    • heartbeat             │          │                            │    │
│  └────────────────────────────┘          └────────────────────────────┘    │
│                                                                             │
└────────────────────────────────────────────────────────────────────────────┘
```

---

## API Endpoints

### 1. Create QR Code Registration

**POST** `/device-registration/qr-registration`

Generates a new QR code for device registration.

**Request:**
```json
{
  "version": "1.0",
  "deviceType": "GroPOS"  // Optional, defaults to "GroPOS"
}
```

**Response:**
```json
{
  "url": "https://admin.gropos.com/register/ABC123XYZ",
  "qrCodeImage": "iVBORw0KGgoAAAANSUhEUgAA...",  // Base64-encoded PNG
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",      // Temporary token
  "assignedGuid": "550e8400-e29b-41d4-a716-446655440000"
}
```

### 2. Get Device Registration Status

**GET** `/device-registration/device-status/{deviceGuid}`

Checks the registration status of a device.

**Response (Pending):**
```json
{
  "deviceStatus": "Pending"
}
```

**Response (In-Progress):**
```json
{
  "deviceStatus": "In-Progress",
  "branch": "Main Street Store"
}
```

**Response (Registered):**
```json
{
  "deviceStatus": "Registered",
  "apiKey": "YOUR_API_KEY_HERE",
  "branchId": 1,
  "branch": "Main Street Store"
}
```

### 3. Device Heartbeat

**GET** `/device-registration/heartbeat`

Used after registration to check for pending data updates.

**Headers:**
```
x-api-key: YOUR_API_KEY_HERE
version: 1.0
```

**Response:**
```json
{
  "messageCount": 5
}
```

---

## Data Models

### PosSystemViewModel

Stored in CouchbaseLite after successful registration:

```kotlin
data class PosSystemViewModel(
    val id: String,              // Environment name (e.g., "PRODUCTION", "STAGING")
    val documentName: String,    // "appKey"
    val branchName: String,      // Display name of the branch
    val apiKey: String,          // The device API key
    val ipAddress: String = "",  // Camera device IP (if assigned)
    val entityId: Int = -1,      // Camera entity ID
    val cameraId: Int = -1,      // Camera device ID
    val onePayEntityId: Int = -1, // Payment terminal entity ID
    val onePayId: Int = -1,       // Payment terminal device ID
    val onePayIpAddress: String = "", // Payment terminal IP
    val refreshToken: String? = null  // For token refresh
)
```

### Registration States

```kotlin
enum class RegistrationState {
    UNREGISTERED,   // No API key, need to show QR
    PENDING,        // QR displayed, waiting for admin scan
    IN_PROGRESS,    // Admin has scanned, assigning branch
    REGISTERED,     // API key received, ready for login
    ERROR           // Registration failed
}
```

### API Responses

```kotlin
data class CreateQRCodeRegistrationResponse(
    val url: String?,           // Admin portal URL
    val qrCodeImage: String?,   // Base64-encoded PNG image
    val accessToken: String?,   // Temporary access token for status polling
    val assignedGuid: String?   // Device GUID for status checks
)

data class GetDeviceRegistrationStatusByIdResponse(
    val deviceStatus: String?,  // "Pending", "In-Progress", "Registered"
    val apiKey: String?,        // Provided when status is "Registered"
    val branchId: Int?,         // Branch ID
    val branch: String?         // Branch display name
)
```

---

## Implementation Details

### Kotlin Implementation

#### RegistrationViewModel

```kotlin
class RegistrationViewModel(
    private val registrationService: RegistrationService,
    private val deviceRepository: DeviceRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RegistrationState.UNREGISTERED)
    val state: StateFlow<RegistrationState> = _state.asStateFlow()

    private val _qrCodeImage = MutableStateFlow<String?>(null)
    val qrCodeImage: StateFlow<String?> = _qrCodeImage.asStateFlow()

    private val _activationCode = MutableStateFlow("")
    val activationCode: StateFlow<String> = _activationCode.asStateFlow()

    private val _branchName = MutableStateFlow("")
    val branchName: StateFlow<String> = _branchName.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var pollingJob: Job? = null
    private var assignedGuid: String? = null
    
    companion object {
        private const val POLL_INTERVAL_SECONDS = 10L
        private const val POLL_TIMEOUT_MINUTES = 10
    }

    init {
        checkExistingRegistration()
    }

    private fun checkExistingRegistration() {
        viewModelScope.launch {
            val existingDevice = deviceRepository.getDeviceInfo()
            if (existingDevice?.apiKey != null) {
                _state.value = RegistrationState.REGISTERED
            } else {
                startRegistration()
            }
        }
    }

    fun startRegistration() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = registrationService.requestQrCode()
                
                response.qrCodeImage?.let { _qrCodeImage.value = it }
                response.url?.let { _activationCode.value = extractActivationCode(it) }
                response.assignedGuid?.let { 
                    assignedGuid = it
                    startPolling(it)
                }
                
                _state.value = RegistrationState.PENDING
            } catch (e: Exception) {
                _state.value = RegistrationState.ERROR
                logger.error("Registration failed: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun startPolling(guid: String) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val timeout = POLL_TIMEOUT_MINUTES * 60 * 1000L
            
            while (isActive && (System.currentTimeMillis() - startTime) < timeout) {
                try {
                    val statusResponse = registrationService.checkStatus(guid)
                    
                    when (statusResponse.deviceStatus) {
                        "In-Progress" -> {
                            _state.value = RegistrationState.IN_PROGRESS
                            _branchName.value = statusResponse.branch ?: ""
                        }
                        "Registered" -> {
                            handleRegistrationComplete(statusResponse)
                            break
                        }
                    }
                } catch (e: Exception) {
                    logger.warn("Status check failed: ${e.message}")
                }
                
                delay(POLL_INTERVAL_SECONDS * 1000)
            }
        }
    }

    private suspend fun handleRegistrationComplete(
        response: GetDeviceRegistrationStatusByIdResponse
    ) {
        val apiKey = response.apiKey ?: return
        val branchName = response.branch ?: ""
        
        // Save to local database
        deviceRepository.saveDeviceInfo(
            PosSystemViewModel(
                id = Environment.current.name,
                documentName = "appKey",
                branchName = branchName,
                apiKey = apiKey
            )
        )
        
        // Configure API client with new API key
        registrationService.configureApiClient(apiKey)
        
        _state.value = RegistrationState.REGISTERED
    }

    fun refreshQrCode() {
        pollingJob?.cancel()
        startRegistration()
    }

    fun stopPolling() {
        pollingJob?.cancel()
    }

    private fun extractActivationCode(url: String): String {
        return url.substringAfterLast("/")
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}
```

#### RegistrationService

```kotlin
class RegistrationService(
    private val registrationApi: RegistrationApi,
    private val apiManager: ApiManager
) {

    suspend fun requestQrCode(): CreateQRCodeRegistrationResponse {
        return withContext(Dispatchers.IO) {
            registrationApi.createQRCodeRegistration(
                version = BuildConfig.VERSION_NAME,
                createQRCodeRegistrationDTO = null
            )
        }
    }

    suspend fun checkStatus(guid: String): GetDeviceRegistrationStatusByIdResponse {
        return withContext(Dispatchers.IO) {
            registrationApi.getDeviceRegistrationStatusById(
                deviceGuid = guid,
                version = BuildConfig.VERSION_NAME
            )
        }
    }

    fun configureApiClient(apiKey: String) {
        apiManager.setApiKey(apiKey)
    }
}
```

#### ApiManager Configuration

```kotlin
object ApiManager {
    private var _apiClient: ApiClient? = null
    val apiClient: ApiClient get() = _apiClient ?: createDefaultClient()
    
    private var apiKey: String? = null

    private fun createDefaultClient(): ApiClient {
        return ApiClient().apply {
            updateBaseUri(BuildConfig.API_BASE_URL)
        }
    }

    fun setApiKey(key: String) {
        apiKey = key
        _apiClient = ApiClient().apply {
            updateBaseUri(BuildConfig.API_BASE_URL)
            setRequestInterceptor { builder ->
                builder.header("x-api-key", key)
                builder.header("version", BuildConfig.VERSION_NAME)
            }
        }
    }

    fun setBearerToken(token: String) {
        _apiClient?.setBearerToken(token)
    }

    fun clearApiKey() {
        apiKey = null
        _apiClient = createDefaultClient()
    }
}
```

---

## Post-Registration Setup

### What Happens After API Key is Received

```
┌────────────────────────────────────────────────────────────────────────────┐
│                    POST-REGISTRATION INITIALIZATION                         │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  1. CONFIGURE API CLIENT                                                    │
│     ┌─────────────────────────────────────────────────────────────────┐    │
│     │ apiClient.setRequestInterceptor { builder ->                    │    │
│     │     builder.header("x-api-key", apiKey)                         │    │
│     │     builder.header("version", BuildConfig.VERSION_NAME)         │    │
│     │ }                                                               │    │
│     └─────────────────────────────────────────────────────────────────┘    │
│                                                                             │
│  2. LOAD ALL DATA (First Time Only)                                        │
│     ┌─────────────────────────────────────────────────────────────────┐    │
│     │ • Products (paginated, ~100 per request)                        │    │
│     │ • Categories                                                    │    │
│     │ • Taxes, CRV values                                             │    │
│     │ • Customer Groups                                               │    │
│     │ • Lookup Categories (quick buttons)                             │    │
│     │ • Conditional Sales (age restrictions)                          │    │
│     │ • Product Images                                                │    │
│     │ • Product Taxes                                                 │    │
│     └─────────────────────────────────────────────────────────────────┘    │
│                                                                             │
│  3. START BACKGROUND SCHEDULERS                                             │
│     ┌─────────────────────────────────────────────────────────────────┐    │
│     │ HeartbeatJobScheduler.start()     // Every 1 minute             │    │
│     │ SyncFailedTransactionScheduler.start() // Retry failed syncs    │    │
│     └─────────────────────────────────────────────────────────────────┘    │
│                                                                             │
│  4. FETCH EMPLOYEE LIST                                                     │
│     ┌─────────────────────────────────────────────────────────────────┐    │
│     │ employeeApi.employeeGetCashierEmployees()                       │    │
│     │ → Display login screen with cashier list                        │    │
│     └─────────────────────────────────────────────────────────────────┘    │
│                                                                             │
└────────────────────────────────────────────────────────────────────────────┘
```

### API Key Usage

After registration, the API key is included in **every API request**:

```kotlin
// All subsequent API calls include the API key
apiClient.setRequestInterceptor { builder ->
    builder.header("x-api-key", apiKey)
    builder.header("version", BuildConfig.VERSION_NAME)
}

// Employee login
employeeApi.employeeLogin(loginRequest)

// Product sync
productApi.productGetAll(offset, limit, lastUpdate)

// Transaction submission
transactionsApi.createTransaction(version, transactionRequest)

// Heartbeat
registrationApi.getDeviceHeartbeat(version)
```

---

## Error Handling

### Registration Errors

```kotlin
sealed class RegistrationError {
    object NetworkError : RegistrationError()
    object ServerError : RegistrationError()
    object Timeout : RegistrationError()
    data class ApiError(val code: Int, val message: String) : RegistrationError()
}

fun handleRegistrationError(error: RegistrationError) {
    when (error) {
        is RegistrationError.NetworkError -> {
            showError("No internet connection. Please check your network.")
        }
        is RegistrationError.ServerError -> {
            showError("Server error. Please try again later.")
        }
        is RegistrationError.Timeout -> {
            showRefreshButton()
            showMessage("QR code expired. Tap to refresh.")
        }
        is RegistrationError.ApiError -> {
            logger.error("API error: ${error.code} - ${error.message}")
            showError("Registration failed. Please contact support.")
        }
    }
}
```

### QR Code Expiration

The QR code expires after **10 minutes** of polling:

```kotlin
companion object {
    private const val POLL_TIMEOUT_MINUTES = 10
}

// When timeout is reached:
if ((System.currentTimeMillis() - startTime) >= timeout) {
    _showRefreshButton.value = true
    pollingJob?.cancel()
}
```

---

## Security Considerations

### API Key Protection

1. **Storage**: API key is stored in CouchbaseLite (encrypted at rest on device)
2. **Transmission**: Always sent via HTTPS with TLS 1.2+
3. **Header Only**: Sent as `x-api-key` header, never in URL or body
4. **No Logging**: API key is never logged or displayed in UI

### QR Code Security

1. **Time-Limited**: QR codes expire after 10 minutes
2. **Single-Use**: Each QR code can only register one device
3. **Admin Required**: Only authorized admins can complete registration
4. **Temporary Token**: Initial API calls use a temporary token, not the final API key

### Device Binding

1. **Branch Lock**: Once registered, device is locked to specific branch
2. **API Key Revocation**: Admin can revoke API key to deauthorize device
3. **Heartbeat Monitoring**: Backend tracks device activity via heartbeat

---

## UI Specifications

### Registration Screen (Compose)

```kotlin
@Composable
fun RegistrationScreen(
    viewModel: RegistrationViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val qrCode by viewModel.qrCodeImage.collectAsState()
    val activationCode by viewModel.activationCode.collectAsState()
    val branchName by viewModel.branchName.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo
            Image(
                painter = painterResource(R.drawable.gropos_logo),
                contentDescription = "GroPOS Logo"
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            when (state) {
                RegistrationState.PENDING -> {
                    Text(
                        text = "Welcome to GroPOS",
                        style = MaterialTheme.typography.headlineLarge
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // QR Code
                    qrCode?.let { base64Image ->
                        QrCodeImage(
                            base64 = base64Image,
                            modifier = Modifier.size(200.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Activation Code: $activationCode",
                        style = MaterialTheme.typography.titleLarge
                    )
                    
                    Text(
                        text = "Scan with admin app or visit URL",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                RegistrationState.IN_PROGRESS -> {
                    CircularProgressIndicator()
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Configuring for: $branchName",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                
                RegistrationState.ERROR -> {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(onClick = { viewModel.refreshQrCode() }) {
                        Text("Try Again")
                    }
                }
                
                else -> {
                    if (isLoading) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}
```

---

## Related Documentation

- [Authentication](./AUTHENTICATION.md) - Employee login after registration
- [Data Synchronization](../data/SYNC_MECHANISM.md) - How data is loaded after registration
- [API Integration](../architecture/API_INTEGRATION.md) - API client configuration

---

*Last Updated: January 2026*

