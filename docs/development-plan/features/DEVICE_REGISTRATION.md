# Device Registration Process - Complete Guide (Kotlin + Compose)

**Version:** 1.0  
**Status:** Complete Documentation  
**Last Updated:** January 2026  
**Platform:** Kotlin Multiplatform (Windows, Linux, Android)  
**UI Framework:** Jetpack Compose / Compose Multiplatform  
**Local Database:** Couchbase Lite

This document provides a comprehensive guide to the entire device registration process in GrowPOS, covering all scenarios from initial launch to post-registration operations, including frontend implementation, backend endpoints, and error handling.

---

## Table of Contents

1. [Overview](#1-overview)
2. [Registration States](#2-registration-states)
3. [Complete Registration Flow](#3-complete-registration-flow)
4. [API Endpoints](#4-api-endpoints)
5. [Frontend Implementation](#5-frontend-implementation)
6. [Backend Integration](#6-backend-integration)
7. [Data Models](#7-data-models)
8. [QR Code Generation & Display](#8-qr-code-generation--display)
9. [Status Polling Mechanism](#9-status-polling-mechanism)
10. [Post-Registration Setup](#10-post-registration-setup)
11. [Heartbeat & Ongoing Device Health](#11-heartbeat--ongoing-device-health)
12. [Unregistration & Re-registration](#12-unregistration--re-registration)
13. [Error Handling](#13-error-handling)
14. [Security Considerations](#14-security-considerations)
15. [Platform-Specific Considerations](#15-platform-specific-considerations)

---

## 1. Overview

### What is Device Registration?

GrowPOS devices must be registered with the backend before they can process transactions. The registration process:

- Associates the device with a specific **branch** (store location)
- Provides an **API key** that authenticates all subsequent API requests
- Enables data synchronization and transaction processing

### Registration Participants

| Participant | Role |
|------------|------|
| **POS Device** | Displays QR code, polls for status, stores API key |
| **Admin User** | Scans QR code or enters activation code to approve registration |
| **Backend Server** | Generates QR codes, manages device status, issues API keys |
| **Admin Portal** | Web/mobile interface where admin completes registration |

### What Registration Provides

| Component | Before Registration | After Registration |
|-----------|--------------------|--------------------|
| API Key | None | Unique device API key |
| Branch Assignment | None | Associated with specific store |
| API Access | Registration endpoints only | Full API access |
| Data Sync | Not available | Products, taxes, categories, etc. |
| Transaction Processing | Not available | Full POS functionality |
| Employee Login | Not available | Full authentication |

### Supported Platforms

| Platform | UI Framework | Local Storage |
|----------|--------------|---------------|
| **Android** | Jetpack Compose | Couchbase Lite Android |
| **Windows** | Compose for Desktop | Couchbase Lite Java |
| **Linux** | Compose for Desktop | Couchbase Lite Java |

---

## 2. Registration States

The device moves through specific states during the registration process:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        DEVICE REGISTRATION STATES                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   ┌──────────────┐    ┌──────────────┐    ┌──────────────┐                 │
│   │ UNREGISTERED │───▶│ IN_PROGRESS  │───▶│  REGISTERED  │                 │
│   │              │    │              │    │              │                 │
│   │ • No API key │    │ • Admin has  │    │ • API key    │                 │
│   │ • QR shown   │    │   scanned QR │    │   received   │                 │
│   │ • Polling    │    │ • Branch     │    │ • Ready for  │                 │
│   │   active     │    │   assigned   │    │   login      │                 │
│   └──────────────┘    └──────────────┘    └──────────────┘                 │
│          │                   │                   │                          │
│          │                   │                   │                          │
│          ▼                   ▼                   ▼                          │
│   ┌──────────────┐    ┌──────────────┐    ┌──────────────┐                 │
│   │    TIMEOUT   │    │    ERROR     │    │ UNREGISTERED │                 │
│   │              │    │              │    │  (after      │                 │
│   │ • QR expired │    │ • API error  │    │   revocation)│                 │
│   │ • Show       │    │ • Network    │    │              │                 │
│   │   refresh    │    │   failure    │    │              │                 │
│   │   button     │    │              │    │              │                 │
│   └──────────────┘    └──────────────┘    └──────────────┘                 │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### State Enum Definition

```kotlin
enum class RegistrationState {
    LOADING,        // Initial state, checking local database
    UNREGISTERED,   // No API key exists locally
    PENDING,        // QR code displayed, waiting for admin scan
    IN_PROGRESS,    // Admin has scanned, assigning branch
    REGISTERED,     // API key received and saved
    TIMEOUT,        // QR code expired
    ERROR           // API/Network failure
}
```

### State Descriptions

| State | Description | UI Display | Duration |
|-------|-------------|------------|----------|
| **LOADING** | Checking local database | Splash screen | Brief (milliseconds) |
| **UNREGISTERED** | No API key exists locally | QR code + activation code | Until admin scans |
| **PENDING** | QR requested, waiting | QR code visible | Until status changes |
| **IN_PROGRESS** | Admin has scanned, assigning branch | "Configuring for: [Branch Name]" | Brief (seconds) |
| **REGISTERED** | API key received and saved | Employee login screen | Permanent |
| **TIMEOUT** | QR code expired (10 min default) | Refresh button visible | Until user refreshes |
| **ERROR** | API/Network failure | Error message + retry option | Until resolved |

---

## 3. Complete Registration Flow

### Step-by-Step Flow Diagram

```
┌────────────────────────────────────────────────────────────────────────────┐
│                        DEVICE REGISTRATION FLOW                             │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ STEP 1: Application Launch                                          │   │
│  │                                                                      │   │
│  │   • Couchbase Lite database initializes                             │   │
│  │   • DeviceRepository checks for PosSystemEntity                     │   │
│  │   • Display "Welcome to GroPOS" splash screen                       │   │
│  │                                                                      │   │
│  │   Decision Point:                                                    │   │
│  │   ├── PosSystemEntity exists with API key? → Go to STEP 7           │   │
│  │   └── No PosSystemEntity? → Continue to STEP 2                      │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                              │                                              │
│                              ▼                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ STEP 2: Request QR Code from Backend                                │   │
│  │                                                                      │   │
│  │   POST /device-registration/qr-registration                         │   │
│  │   Headers: { version: "v1" }                                        │   │
│  │   Body: { deviceType: 0 }  // 0 = GrowPOS (optional)                │   │
│  │                                                                      │   │
│  │   Backend generates:                                                 │   │
│  │   • Unique device GUID (assignedGuid)                               │   │
│  │   • Registration URL with activation code                           │   │
│  │   • Base64-encoded QR code image                                    │   │
│  │   • Temporary access token for status polling                       │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                              │                                              │
│                              ▼                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ STEP 3: Display QR Code and Activation Code                         │   │
│  │                                                                      │   │
│  │   • Decode base64 QR image and display via Compose Image            │   │
│  │   • Extract activation code from URL (last path segment)            │   │
│  │   • Update RegistrationRepository with temporary access token       │   │
│  │   • Start status polling coroutine                                  │   │
│  │                                                                      │   │
│  │   ┌─────────────────────────────────────────────────────┐           │   │
│  │   │          Welcome to GroPOS                          │           │   │
│  │   │                                                     │           │   │
│  │   │  To register this station with your Unisight       │           │   │
│  │   │  account, scan the QR code below with a mobile     │           │   │
│  │   │  device, or visit the following URL.               │           │   │
│  │   │                                                     │           │   │
│  │   │           ┌───────────────┐                         │           │   │
│  │   │           │   [QR Code]   │                         │           │   │
│  │   │           └───────────────┘                         │           │   │
│  │   │                                                     │           │   │
│  │   │   https://www.unisight.io/activate                  │           │   │
│  │   │                                                     │           │   │
│  │   │        Activation Code: ABC123XYZ                   │           │   │
│  │   └─────────────────────────────────────────────────────┘           │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                              │                                              │
│                              ▼                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ STEP 4: Admin Scans QR Code / Enters Activation Code                │   │
│  │                                                                      │   │
│  │   • Store manager/admin uses mobile device to scan QR               │   │
│  │   • OR enters activation code at admin portal                       │   │
│  │   • Admin selects which branch to assign the device                 │   │
│  │   • Backend updates device status to "In-Progress"                  │   │
│  │                                                                      │   │
│  │   Admin Actions:                                                     │   │
│  │   1. Scan QR code or enter activation code                          │   │
│  │   2. Authenticate (if not already logged in)                        │   │
│  │   3. Select branch/store from available list                        │   │
│  │   4. Confirm registration                                           │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                              │                                              │
│                              ▼                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ STEP 5: Poll for Registration Status                                │   │
│  │                                                                      │   │
│  │   GET /device-registration/device-status/{assignedGuid}             │   │
│  │   Headers: { Authorization: Bearer <accessToken>, version: "v1" }   │   │
│  │                                                                      │   │
│  │   Polling Configuration:                                             │   │
│  │   • Interval: 10 seconds (using Kotlin coroutines delay)            │   │
│  │   • Timeout: 10 minutes (extended to 60 min when In-Progress)       │   │
│  │   • Initial delay: 0 seconds                                        │   │
│  │                                                                      │   │
│  │   Status Responses:                                                  │   │
│  │   ├── "Pending" → Continue polling                                  │   │
│  │   ├── "In-Progress" → Show branch name, extend timeout              │   │
│  │   └── "Registered" → API key received, proceed to STEP 6            │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                              │                                              │
│                              ▼                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ STEP 6: Save API Key and Configure Client                           │   │
│  │                                                                      │   │
│  │   • Cancel status polling coroutine                                 │   │
│  │   • Delete existing PosSystemEntity (if any)                        │   │
│  │   • Create new PosSystemEntity with received data                   │   │
│  │   • Save to Couchbase Lite local database                           │   │
│  │   • Configure Ktor/Retrofit client with x-api-key header            │   │
│  │                                                                      │   │
│  │   Saved Data:                                                        │   │
│  │   {                                                                  │   │
│  │     "id": "PRODUCTION",  // Environment name                        │   │
│  │     "documentName": "appKey",                                       │   │
│  │     "branchName": "Main Street Store",                              │   │
│  │     "apiKey": "<DEVICE_API_KEY>",                            │   │
│  │     "ipAddress": ""                                                 │   │
│  │   }                                                                  │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                              │                                              │
│                              ▼                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ STEP 7: Initial Data Load & Login Screen                           │   │
│  │                                                                      │   │
│  │   If First Registration:                                             │   │
│  │   • Show "Initializing Database Please Wait..." dialog              │   │
│  │   • Load all data from backend (products, taxes, categories, etc.) │   │
│  │                                                                      │   │
│  │   Always:                                                            │   │
│  │   • Start HeartbeatWorker (WorkManager / coroutine job)             │   │
│  │   • Start SyncFailedTransactionWorker                               │   │
│  │   • Fetch employee list for login                                   │   │
│  │   • Get current device info (station name, location)                │   │
│  │   • Navigate to employee login screen                               │   │
│  │                                                                      │   │
│  │   ✓ Device is now fully registered and ready for use               │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└────────────────────────────────────────────────────────────────────────────┘
```

---

## 4. API Endpoints

### 4.1 Create QR Code Registration

Generates a new QR code for device registration.

**Endpoint:** `POST /device-registration/qr-registration`

**Base URL:** Configured via `BuildConfig.TRANSACTIONS_API_URL`

**Headers:**
```
version: v1
Content-Type: application/json
Ocp-Apim-Subscription-Key: <subscription-key>  (optional, or via query param)
```

> **⚠️ IMPORTANT:** The `version` header must be `v1` (not `"1.0"`). This was confirmed via Postman API testing on 2026-01-03. Using `1.0` returns HTTP 404.

**Request Body (Optional):**
```json
{
  "deviceType": 0
}
```

**Device Types:**
| Value | Type |
|-------|------|
| 0 | GrowPOS (default) |
| 1 | oneTime |
| 2 | oneStore |
| 3 | oneServer |
| 4 | Scale |
| 5 | ESLServer |
| 6 | ESLTag |
| 7 | Dashboard |
| 8 | RegisterScaleCamera |
| 9 | PrintingScaleCamera |
| 10 | ShrinkProductionCamera |
| 11 | oneScanner |
| 12 | onePay |
| 13 | onePoint |

**Success Response (201):**
```json
{
  "url": "https://www.unisight.io/activate/ABC123XYZ",
  "qrCodeImage": "iVBORw0KGgoAAAANSUhEUgAAAQAAAAEACAYAAABccqhmA...",
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "assignedGuid": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response Fields:**
| Field | Type | Description |
|-------|------|-------------|
| `url` | String | Admin portal URL with activation code path |
| `qrCodeImage` | String | Base64-encoded PNG image of QR code |
| `accessToken` | String | Temporary bearer token for status polling |
| `assignedGuid` | String | UUID assigned to this registration request |

**Error Response (400/500):**
```json
{
  "message": "Validation failed",
  "innerException": {},
  "errors": [
    {
      "propertyName": "deviceType",
      "errorMessage": "Invalid device type",
      "attemptedValue": 99,
      "severity": 0,
      "errorCode": "InvalidValue"
    }
  ],
  "stackTrace": "..."
}
```

---

### 4.2 Get Device Registration Status

Checks the current registration status of a device.

**Endpoint:** `GET /device-registration/device-status/{deviceGuid}`

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `deviceGuid` | String | Full GUID or last 20 characters of device identifier |

**Headers:**
```
Authorization: Bearer <accessToken>
version: v1
```

> **⚠️ NOTE:** Backend returns `"Complete"` (not `"Registered"`) when registration finishes. Client code must accept both values.

**Response - Pending:**
```json
{
  "deviceStatus": "Pending",
  "apiKey": null,
  "branchId": null,
  "branch": null
}
```

**Response - In-Progress:**
```json
{
  "deviceStatus": "In-Progress",
  "apiKey": null,
  "branchId": null,
  "branch": "Main Street Store"
}
```

**Response - Registered:**
```json
{
  "deviceStatus": "Registered",
  "apiKey": "<DEVICE_API_KEY>",
  "branchId": 42,
  "branch": "Main Street Store"
}
```

**Response Fields:**
| Field | Type | Description |
|-------|------|-------------|
| `deviceStatus` | String | Current status: "Pending", "In-Progress", or "Registered" |
| `apiKey` | String | Device API key (only provided when Registered) |
| `branchId` | Integer | Numeric branch ID (only when In-Progress or Registered) |
| `branch` | String | Branch display name (only when In-Progress or Registered) |

---

### 4.3 Device Heartbeat

Used after registration to check for pending data updates.

**Endpoint:** `GET /device-registration/heartbeat`

**Headers:**
```
x-api-key: <DEVICE_API_KEY>
version: v1
```

**Success Response (200):**
```json
{
  "messageCount": 5
}
```

**Response Fields:**
| Field | Type | Description |
|-------|------|-------------|
| `messageCount` | Integer | Number of pending update messages for this device |

**Usage:** Called every 1 minute by `HeartbeatWorker` to check for data updates (product changes, tax updates, etc.)

---

## 5. Frontend Implementation

### 5.1 Project Structure

```
app/
├── src/
│   ├── commonMain/kotlin/
│   │   └── com/unisight/gropos/
│   │       ├── di/                    # Dependency Injection (Koin)
│   │       ├── data/
│   │       │   ├── local/             # Couchbase Lite
│   │       │   │   ├── entity/
│   │       │   │   │   └── PosSystemEntity.kt
│   │       │   │   └── dao/
│   │       │   │       └── PosSystemDao.kt
│   │       │   ├── remote/            # Ktor API clients
│   │       │   │   └── api/
│   │       │   │       └── RegistrationApi.kt
│   │       │   └── repository/
│   │       │       └── DeviceRepository.kt
│   │       ├── domain/
│   │       │   ├── model/
│   │       │   │   └── RegistrationState.kt
│   │       │   └── usecase/
│   │       │       └── RegisterDeviceUseCase.kt
│   │       └── presentation/
│   │           ├── registration/
│   │           │   ├── RegistrationScreen.kt
│   │           │   └── RegistrationViewModel.kt
│   │           └── theme/
│   │               └── Theme.kt
│   ├── androidMain/kotlin/           # Android-specific
│   └── desktopMain/kotlin/           # Windows/Linux-specific
```

### 5.2 Registration Screen (Compose)

**File:** `presentation/registration/RegistrationScreen.kt`

```kotlin
@Composable
fun RegistrationScreen(
    viewModel: RegistrationViewModel = koinViewModel(),
    onRegistrationComplete: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(uiState.state) {
        if (uiState.state == RegistrationState.REGISTERED) {
            onRegistrationComplete()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        when (uiState.state) {
            RegistrationState.LOADING -> {
                LoadingContent()
            }
            
            RegistrationState.PENDING,
            RegistrationState.UNREGISTERED -> {
                QrCodeContent(
                    qrCodeImage = uiState.qrCodeImage,
                    activationCode = uiState.activationCode,
                    onRefresh = viewModel::refreshQrCode
                )
            }
            
            RegistrationState.IN_PROGRESS -> {
                InProgressContent(
                    branchName = uiState.branchName
                )
            }
            
            RegistrationState.TIMEOUT -> {
                TimeoutContent(
                    onRefresh = viewModel::refreshQrCode
                )
            }
            
            RegistrationState.ERROR -> {
                ErrorContent(
                    errorMessage = uiState.errorMessage,
                    onRetry = viewModel::refreshQrCode
                )
            }
            
            RegistrationState.REGISTERED -> {
                // Will navigate away via LaunchedEffect
                LoadingContent()
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(Res.drawable.gropos_logo),
            contentDescription = "GroPOS Logo",
            modifier = Modifier.size(200.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Welcome to",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Text(
            text = "GroPOS",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun QrCodeContent(
    qrCodeImage: String?,
    activationCode: String,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome to GroPOS",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "To register this station with your Unisight account, " +
                   "scan the QR code below with a mobile device, or visit " +
                   "the following URL, and provide the code as shown.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 48.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // QR Code Image
        qrCodeImage?.let { base64Image ->
            QrCodeImage(
                base64 = base64Image,
                modifier = Modifier
                    .size(250.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(16.dp)
                    )
            )
        } ?: Box(
            modifier = Modifier
                .size(250.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "https://www.unisight.io/activate",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Activation Code
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Text(
                text = activationCode.ifEmpty { "Loading..." },
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        TextButton(onClick = onRefresh) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Refresh Code")
        }
    }
}

@Composable
private fun InProgressContent(branchName: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Connected",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Configuring this station for:",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = branchName,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun TimeoutContent(onRefresh: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "QR Code Expired",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "The registration code has expired. Please refresh to get a new code.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 48.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onRefresh,
            modifier = Modifier.height(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Get New Code")
        }
    }
}

@Composable
private fun ErrorContent(
    errorMessage: String?,
    onRetry: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Registration Failed",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = errorMessage ?: "An unexpected error occurred. Please try again.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 48.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onRetry,
            modifier = Modifier.height(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Try Again")
        }
    }
}
```

### 5.3 QR Code Image Component

```kotlin
@Composable
fun QrCodeImage(
    base64: String,
    modifier: Modifier = Modifier
) {
    val imageBitmap = remember(base64) {
        try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }
    
    imageBitmap?.let { bitmap ->
        Image(
            bitmap = bitmap,
            contentDescription = "Registration QR Code",
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    }
}

// For Desktop (Windows/Linux) - use Skia
@Composable
actual fun QrCodeImage(
    base64: String,
    modifier: Modifier
) {
    val image = remember(base64) {
        try {
            val bytes = java.util.Base64.getDecoder().decode(base64)
            org.jetbrains.skia.Image.makeFromEncoded(bytes)
        } catch (e: Exception) {
            null
        }
    }
    
    image?.let { skiaImage ->
        Image(
            bitmap = skiaImage.toComposeImageBitmap(),
            contentDescription = "Registration QR Code",
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    }
}
```

---

## 6. Backend Integration

### 6.1 Registration ViewModel

**File:** `presentation/registration/RegistrationViewModel.kt`

```kotlin
@HiltViewModel
class RegistrationViewModel @Inject constructor(
    private val registerDeviceUseCase: RegisterDeviceUseCase,
    private val deviceRepository: DeviceRepository,
    private val apiManager: ApiManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(RegistrationUiState())
    val uiState: StateFlow<RegistrationUiState> = _uiState.asStateFlow()
    
    private var pollingJob: Job? = null
    private var assignedGuid: String? = null
    private var accessToken: String? = null
    
    companion object {
        private const val POLL_INTERVAL_MS = 10_000L  // 10 seconds
        private const val DEFAULT_TIMEOUT_MS = 10 * 60 * 1000L  // 10 minutes
        private const val EXTENDED_TIMEOUT_MS = 60 * 60 * 1000L  // 60 minutes
    }
    
    init {
        checkExistingRegistration()
    }
    
    private fun checkExistingRegistration() {
        viewModelScope.launch {
            _uiState.update { it.copy(state = RegistrationState.LOADING) }
            
            try {
                val existingDevice = deviceRepository.getDeviceInfo()
                if (existingDevice?.apiKey != null) {
                    apiManager.setApiKey(existingDevice.apiKey)
                    _uiState.update { it.copy(state = RegistrationState.REGISTERED) }
                } else {
                    startRegistration()
                }
            } catch (e: Exception) {
                startRegistration()
            }
        }
    }
    
    fun startRegistration() {
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    state = RegistrationState.PENDING,
                    isLoading = true,
                    errorMessage = null
                ) 
            }
            
            try {
                val response = registerDeviceUseCase.requestQrCode()
                
                response.qrCodeImage?.let { qrCode ->
                    _uiState.update { it.copy(qrCodeImage = qrCode) }
                }
                
                response.url?.let { url ->
                    _uiState.update { it.copy(activationCode = extractActivationCode(url)) }
                }
                
                response.assignedGuid?.let { guid ->
                    assignedGuid = guid
                }
                
                response.accessToken?.let { token ->
                    accessToken = token
                    apiManager.setTemporaryToken(token)
                }
                
                // Start polling
                assignedGuid?.let { guid ->
                    startPolling(guid)
                }
                
                _uiState.update { 
                    it.copy(
                        state = RegistrationState.PENDING,
                        isLoading = false
                    ) 
                }
                
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        state = RegistrationState.ERROR,
                        isLoading = false,
                        errorMessage = e.message ?: "Registration failed"
                    ) 
                }
            }
        }
    }
    
    private fun startPolling(guid: String) {
        pollingJob?.cancel()
        
        pollingJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            var timeout = DEFAULT_TIMEOUT_MS
            
            while (isActive) {
                val elapsed = System.currentTimeMillis() - startTime
                
                // Check for timeout
                if (elapsed >= timeout) {
                    _uiState.update { it.copy(state = RegistrationState.TIMEOUT) }
                    break
                }
                
                try {
                    val statusResponse = registerDeviceUseCase.checkStatus(guid)
                    
                    when (statusResponse.deviceStatus) {
                        "In-Progress" -> {
                            timeout = EXTENDED_TIMEOUT_MS  // Extend timeout
                            _uiState.update { 
                                it.copy(
                                    state = RegistrationState.IN_PROGRESS,
                                    branchName = statusResponse.branch ?: ""
                                ) 
                            }
                        }
                        
                        "Registered" -> {
                            handleRegistrationComplete(statusResponse)
                            break
                        }
                        
                        // "Pending" - continue polling
                    }
                    
                } catch (e: Exception) {
                    // Log error but continue polling
                    println("Status check failed: ${e.message}")
                }
                
                delay(POLL_INTERVAL_MS)
            }
        }
    }
    
    private suspend fun handleRegistrationComplete(
        response: DeviceRegistrationStatusResponse
    ) {
        val apiKey = response.apiKey ?: return
        val branchName = response.branch ?: ""
        
        try {
            // Save to local database
            deviceRepository.saveDeviceInfo(
                PosSystemEntity(
                    id = BuildConfig.ENVIRONMENT.name,
                    documentName = "appKey",
                    branchName = branchName,
                    apiKey = apiKey,
                    ipAddress = "",
                    entityId = -1,
                    cameraId = -1,
                    onePayEntityId = -1,
                    onePayId = -1,
                    onePayIpAddress = "",
                    refreshToken = null
                )
            )
            
            // Configure API client with new API key
            apiManager.setApiKey(apiKey)
            
            _uiState.update { 
                it.copy(
                    state = RegistrationState.REGISTERED,
                    branchName = branchName
                ) 
            }
            
        } catch (e: Exception) {
            _uiState.update { 
                it.copy(
                    state = RegistrationState.ERROR,
                    errorMessage = "Failed to save registration: ${e.message}"
                ) 
            }
        }
    }
    
    fun refreshQrCode() {
        pollingJob?.cancel()
        startRegistration()
    }
    
    private fun extractActivationCode(url: String): String {
        return url.substringAfterLast("/").takeIf { it.isNotEmpty() } ?: ""
    }
    
    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}

data class RegistrationUiState(
    val state: RegistrationState = RegistrationState.LOADING,
    val qrCodeImage: String? = null,
    val activationCode: String = "",
    val branchName: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
```

### 6.2 API Manager

**File:** `data/remote/ApiManager.kt`

```kotlin
class ApiManager(
    private val baseUrl: String
) {
    private var apiKey: String? = null
    private var bearerToken: String? = null
    private var temporaryToken: String? = null
    
    val httpClient: HttpClient by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
            
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.INFO
            }
            
            defaultRequest {
                url(baseUrl)
                contentType(ContentType.Application.Json)
                header("version", BuildConfig.VERSION_NAME)
                
                // Add authentication headers
                apiKey?.let { header("x-api-key", it) }
                bearerToken?.let { header("Authorization", "Bearer $it") }
                temporaryToken?.let { header("Authorization", "Bearer $it") }
            }
        }
    }
    
    fun setApiKey(key: String) {
        apiKey = key
        temporaryToken = null  // Clear temporary token
    }
    
    fun setTemporaryToken(token: String) {
        temporaryToken = token
    }
    
    fun setBearerToken(token: String) {
        bearerToken = token
    }
    
    fun clearApiKey() {
        apiKey = null
        bearerToken = null
        temporaryToken = null
    }
    
    fun isRegistered(): Boolean = apiKey != null
}
```

### 6.3 Registration API Service

**File:** `data/remote/api/RegistrationApi.kt`

```kotlin
interface RegistrationApi {
    
    suspend fun createQrCodeRegistration(
        deviceType: Int? = null
    ): QrCodeRegistrationResponse
    
    suspend fun getDeviceStatus(
        deviceGuid: String
    ): DeviceRegistrationStatusResponse
    
    suspend fun getHeartbeat(): HeartbeatResponse
}

class RegistrationApiImpl(
    private val httpClient: HttpClient,
    private val baseUrl: String
) : RegistrationApi {
    
    override suspend fun createQrCodeRegistration(
        deviceType: Int?
    ): QrCodeRegistrationResponse {
        return httpClient.post("$baseUrl/device-registration/qr-registration") {
            deviceType?.let {
                setBody(CreateQrCodeRequest(deviceType = it))
            }
        }.body()
    }
    
    override suspend fun getDeviceStatus(
        deviceGuid: String
    ): DeviceRegistrationStatusResponse {
        return httpClient.get("$baseUrl/device-registration/device-status/$deviceGuid").body()
    }
    
    override suspend fun getHeartbeat(): HeartbeatResponse {
        return httpClient.get("$baseUrl/device-registration/heartbeat").body()
    }
}

@Serializable
data class CreateQrCodeRequest(
    val deviceType: Int
)

@Serializable
data class QrCodeRegistrationResponse(
    val url: String? = null,
    val qrCodeImage: String? = null,
    val accessToken: String? = null,
    val assignedGuid: String? = null
)

@Serializable
data class DeviceRegistrationStatusResponse(
    val deviceStatus: String? = null,
    val apiKey: String? = null,
    val branchId: Int? = null,
    val branch: String? = null
)

@Serializable
data class HeartbeatResponse(
    val messageCount: Int? = null
)
```

---

## 7. Data Models

### 7.1 PosSystemEntity

Stored in Couchbase Lite after successful registration:

**File:** `data/local/entity/PosSystemEntity.kt`

```kotlin
@Serializable
data class PosSystemEntity(
    val id: String,                    // Environment name (e.g., "PRODUCTION")
    val documentName: String,          // "appKey"
    val branchName: String,            // Display name of the branch
    val apiKey: String,                // The device API key
    val ipAddress: String = "",        // Camera device IP (if assigned)
    val entityId: Int = -1,            // Camera entity ID
    val cameraId: Int = -1,            // Camera device ID
    val onePayEntityId: Int = -1,      // Payment terminal entity ID
    val onePayId: Int = -1,            // Payment terminal device ID
    val onePayIpAddress: String = "",  // Payment terminal IP
    val refreshToken: String? = null   // For token refresh
) {
    companion object {
        const val COLLECTION_NAME = "PosSystem"
        const val DOCUMENT_NAME = "appKey"
    }
}
```

### 7.2 PosSystem Repository

**File:** `data/repository/DeviceRepository.kt`

```kotlin
interface DeviceRepository {
    suspend fun getDeviceInfo(): PosSystemEntity?
    suspend fun saveDeviceInfo(entity: PosSystemEntity)
    suspend fun deleteDeviceInfo()
    suspend fun updateRefreshToken(token: String)
}

class DeviceRepositoryImpl(
    private val database: Database
) : DeviceRepository {
    
    private val collection: Collection by lazy {
        database.createCollection(PosSystemEntity.COLLECTION_NAME, "pos")
    }
    
    override suspend fun getDeviceInfo(): PosSystemEntity? = withContext(Dispatchers.IO) {
        val environmentId = BuildConfig.ENVIRONMENT.name
        
        val query = QueryBuilder
            .select(SelectResult.all())
            .from(DataSource.collection(collection))
            .where(Expression.property("id").equalTo(Expression.string(environmentId)))
        
        query.execute().allResults().firstOrNull()?.let { result ->
            result.getDictionary(collection.name)?.toEntity()
        }
    }
    
    override suspend fun saveDeviceInfo(entity: PosSystemEntity) = withContext(Dispatchers.IO) {
        // Delete existing document if present
        deleteDeviceInfo()
        
        // Create new document
        val document = MutableDocument(entity.id).apply {
            setString("id", entity.id)
            setString("documentName", entity.documentName)
            setString("branchName", entity.branchName)
            setString("apiKey", entity.apiKey)
            setString("ipAddress", entity.ipAddress)
            setInt("entityId", entity.entityId)
            setInt("cameraId", entity.cameraId)
            setInt("onePayEntityId", entity.onePayEntityId)
            setInt("onePayId", entity.onePayId)
            setString("onePayIpAddress", entity.onePayIpAddress)
            entity.refreshToken?.let { setString("refreshToken", it) }
        }
        
        collection.save(document)
    }
    
    override suspend fun deleteDeviceInfo() = withContext(Dispatchers.IO) {
        val environmentId = BuildConfig.ENVIRONMENT.name
        collection.getDocument(environmentId)?.let { doc ->
            collection.delete(doc)
        }
    }
    
    override suspend fun updateRefreshToken(token: String) = withContext(Dispatchers.IO) {
        getDeviceInfo()?.let { entity ->
            saveDeviceInfo(entity.copy(refreshToken = token))
        }
    }
    
    private fun Dictionary.toEntity(): PosSystemEntity {
        return PosSystemEntity(
            id = getString("id") ?: "",
            documentName = getString("documentName") ?: "",
            branchName = getString("branchName") ?: "",
            apiKey = getString("apiKey") ?: "",
            ipAddress = getString("ipAddress") ?: "",
            entityId = getInt("entityId"),
            cameraId = getInt("cameraId"),
            onePayEntityId = getInt("onePayEntityId"),
            onePayId = getInt("onePayId"),
            onePayIpAddress = getString("onePayIpAddress") ?: "",
            refreshToken = getString("refreshToken")
        )
    }
}
```

### 7.3 Registration State Sealed Class

```kotlin
sealed class RegistrationResult {
    data class Success(
        val apiKey: String,
        val branchName: String,
        val branchId: Int
    ) : RegistrationResult()
    
    data class Error(
        val message: String,
        val code: Int? = null
    ) : RegistrationResult()
    
    object Pending : RegistrationResult()
    
    data class InProgress(
        val branchName: String
    ) : RegistrationResult()
}
```

---

## 8. QR Code Generation & Display

### 8.1 QR Code Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                      QR CODE GENERATION FLOW                         │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  1. BACKEND GENERATES QR CODE                                        │
│     ┌───────────────────────────────────────────────────────────┐   │
│     │ • Generate unique device GUID                              │   │
│     │ • Create registration URL with activation code             │   │
│     │   Example: https://www.unisight.io/activate/ABC123XYZ     │   │
│     │ • Encode URL into QR code image (PNG format)               │   │
│     │ • Convert PNG to Base64 string                             │   │
│     │ • Generate temporary JWT access token                      │   │
│     └───────────────────────────────────────────────────────────┘   │
│                              │                                       │
│                              ▼                                       │
│  2. FRONTEND RECEIVES RESPONSE                                       │
│     ┌───────────────────────────────────────────────────────────┐   │
│     │ Response JSON:                                             │   │
│     │ {                                                          │   │
│     │   "url": "https://www.unisight.io/activate/ABC123XYZ",   │   │
│     │   "qrCodeImage": "iVBORw0KGgoAAAANSUhEUgAA...",           │   │
│     │   "accessToken": "eyJhbGciOiJIUzI1NiIs...",               │   │
│     │   "assignedGuid": "550e8400-e29b-41d4-..."               │   │
│     │ }                                                          │   │
│     └───────────────────────────────────────────────────────────┘   │
│                              │                                       │
│                              ▼                                       │
│  3. KOTLIN DECODES AND COMPOSE DISPLAYS                              │
│     ┌───────────────────────────────────────────────────────────┐   │
│     │ // Android                                                 │   │
│     │ val bytes = Base64.decode(base64, Base64.DEFAULT)         │   │
│     │ val bitmap = BitmapFactory.decodeByteArray(bytes, 0, len) │   │
│     │ Image(bitmap = bitmap.asImageBitmap(), ...)               │   │
│     │                                                            │   │
│     │ // Desktop (Windows/Linux)                                 │   │
│     │ val bytes = java.util.Base64.getDecoder().decode(base64)  │   │
│     │ val image = org.jetbrains.skia.Image.makeFromEncoded(bytes)│  │
│     │ Image(bitmap = image.toComposeImageBitmap(), ...)         │   │
│     └───────────────────────────────────────────────────────────┘   │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 8.2 Platform-Specific QR Code Decoding

```kotlin
// commonMain - expect declaration
expect fun decodeBase64ToImageBitmap(base64: String): ImageBitmap?

// androidMain - actual implementation
actual fun decodeBase64ToImageBitmap(base64: String): ImageBitmap? {
    return try {
        val bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?.asImageBitmap()
    } catch (e: Exception) {
        null
    }
}

// desktopMain - actual implementation
actual fun decodeBase64ToImageBitmap(base64: String): ImageBitmap? {
    return try {
        val bytes = java.util.Base64.getDecoder().decode(base64)
        org.jetbrains.skia.Image.makeFromEncoded(bytes).toComposeImageBitmap()
    } catch (e: Exception) {
        null
    }
}
```

### 8.3 Activation Code Extraction

```kotlin
/**
 * Extracts the activation code from the registration URL.
 * 
 * @param url The full URL (e.g., "https://www.unisight.io/activate/ABC123XYZ")
 * @return The activation code (e.g., "ABC123XYZ")
 */
fun extractActivationCode(url: String): String {
    if (url.isBlank()) return ""
    
    return url.split("/")
        .lastOrNull { it.isNotEmpty() }
        ?: ""
}

// Usage:
// Input: "https://www.unisight.io/activate/ABC123XYZ"
// Output: "ABC123XYZ"
```

---

## 9. Status Polling Mechanism

### 9.1 Polling Configuration

| Parameter | Value | Description |
|-----------|-------|-------------|
| Initial Delay | 0 seconds | Start polling immediately |
| Poll Interval | 10 seconds | Check status every 10 seconds |
| Default Timeout | 10 minutes | Show refresh button after timeout |
| Extended Timeout | 60 minutes | Extended when admin has scanned (In-Progress) |

### 9.2 Polling Implementation with Coroutines

```kotlin
class RegistrationPollingManager(
    private val registrationApi: RegistrationApi,
    private val scope: CoroutineScope
) {
    private var pollingJob: Job? = null
    
    companion object {
        private const val POLL_INTERVAL_MS = 10_000L
        private const val DEFAULT_TIMEOUT_MS = 10 * 60 * 1000L
        private const val EXTENDED_TIMEOUT_MS = 60 * 60 * 1000L
    }
    
    fun startPolling(
        guid: String,
        onStatusChange: (RegistrationResult) -> Unit
    ) {
        pollingJob?.cancel()
        
        pollingJob = scope.launch {
            val startTime = System.currentTimeMillis()
            var timeout = DEFAULT_TIMEOUT_MS
            
            while (isActive) {
                val elapsed = System.currentTimeMillis() - startTime
                
                if (elapsed >= timeout) {
                    onStatusChange(RegistrationResult.Error("QR code expired"))
                    break
                }
                
                try {
                    val response = registrationApi.getDeviceStatus(guid)
                    
                    when (response.deviceStatus) {
                        "Pending" -> {
                            onStatusChange(RegistrationResult.Pending)
                        }
                        
                        "In-Progress" -> {
                            timeout = EXTENDED_TIMEOUT_MS
                            onStatusChange(
                                RegistrationResult.InProgress(
                                    branchName = response.branch ?: ""
                                )
                            )
                        }
                        
                        "Registered" -> {
                            onStatusChange(
                                RegistrationResult.Success(
                                    apiKey = response.apiKey ?: "",
                                    branchName = response.branch ?: "",
                                    branchId = response.branchId ?: -1
                                )
                            )
                            break
                        }
                    }
                    
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Log error but continue polling
                    println("Status check failed: ${e.message}")
                }
                
                delay(POLL_INTERVAL_MS)
            }
        }
    }
    
    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }
}
```

### 9.3 Flow-Based Polling Alternative

```kotlin
fun pollRegistrationStatus(
    guid: String,
    intervalMs: Long = 10_000L,
    timeoutMs: Long = 10 * 60 * 1000L
): Flow<RegistrationResult> = flow {
    val startTime = System.currentTimeMillis()
    var currentTimeout = timeoutMs
    
    while (true) {
        val elapsed = System.currentTimeMillis() - startTime
        
        if (elapsed >= currentTimeout) {
            emit(RegistrationResult.Error("QR code expired"))
            break
        }
        
        try {
            val response = registrationApi.getDeviceStatus(guid)
            
            when (response.deviceStatus) {
                "Pending" -> emit(RegistrationResult.Pending)
                
                "In-Progress" -> {
                    currentTimeout = 60 * 60 * 1000L  // Extend to 60 minutes
                    emit(RegistrationResult.InProgress(response.branch ?: ""))
                }
                
                "Registered" -> {
                    emit(
                        RegistrationResult.Success(
                            apiKey = response.apiKey ?: "",
                            branchName = response.branch ?: "",
                            branchId = response.branchId ?: -1
                        )
                    )
                    break
                }
            }
        } catch (e: Exception) {
            // Continue polling on error
        }
        
        delay(intervalMs)
    }
}.flowOn(Dispatchers.IO)
```

---

## 10. Post-Registration Setup

### 10.1 What Happens After API Key is Received

```
┌────────────────────────────────────────────────────────────────────────────┐
│                    POST-REGISTRATION INITIALIZATION                         │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  1. CONFIGURE API CLIENT                                                    │
│     ┌─────────────────────────────────────────────────────────────────┐    │
│     │ apiManager.setApiKey(apiKey)                                     │    │
│     │                                                                  │    │
│     │ // All subsequent requests include:                             │    │
│     │ header("x-api-key", apiKey)                                     │    │
│     │ header("version", BuildConfig.VERSION_NAME)                     │    │
│     └─────────────────────────────────────────────────────────────────┘    │
│                                                                             │
│  2. SAVE TO LOCAL DATABASE                                                  │
│     ┌─────────────────────────────────────────────────────────────────┐    │
│     │ deviceRepository.saveDeviceInfo(                                 │    │
│     │     PosSystemEntity(                                            │    │
│     │         id = Environment.PRODUCTION.name,                       │    │
│     │         documentName = "appKey",                                │    │
│     │         branchName = branchName,                                │    │
│     │         apiKey = apiKey,                                        │    │
│     │         ...                                                     │    │
│     │     )                                                           │    │
│     │ )                                                               │    │
│     └─────────────────────────────────────────────────────────────────┘    │
│                                                                             │
│  3. LOAD ALL DATA (First Registration Only)                                │
│     ┌─────────────────────────────────────────────────────────────────┐    │
│     │ dataRepository.loadAllData() fetches:                            │    │
│     │                                                                  │    │
│     │ • Products (paginated, ~100 per request)                        │    │
│     │ • Categories                                                    │    │
│     │ • Taxes                                                         │    │
│     │ • CRV (California Redemption Values)                            │    │
│     │ • Customer Groups                                               │    │
│     │ • Lookup Categories (quick buttons)                             │    │
│     │ • Conditional Sales (age restrictions)                          │    │
│     │ • Product Images                                                │    │
│     │ • Product Tax mappings                                          │    │
│     └─────────────────────────────────────────────────────────────────┘    │
│                                                                             │
│  4. START BACKGROUND WORKERS                                                │
│     ┌─────────────────────────────────────────────────────────────────┐    │
│     │ // Android - WorkManager                                         │    │
│     │ HeartbeatWorker.enqueue(workManager)                            │    │
│     │ SyncFailedTransactionWorker.enqueue(workManager)                │    │
│     │                                                                  │    │
│     │ // Desktop - Coroutine Jobs                                      │    │
│     │ heartbeatJob = scope.launch { heartbeatLoop() }                 │    │
│     │ syncJob = scope.launch { syncFailedTransactionsLoop() }         │    │
│     └─────────────────────────────────────────────────────────────────┘    │
│                                                                             │
│  5. FETCH EMPLOYEE LIST                                                     │
│     ┌─────────────────────────────────────────────────────────────────┐    │
│     │ val employees = employeeApi.getCashierEmployees()               │    │
│     │                                                                  │    │
│     │ → Navigate to login screen with employee list                   │    │
│     └─────────────────────────────────────────────────────────────────┘    │
│                                                                             │
│  6. FETCH DEVICE INFO                                                       │
│     ┌─────────────────────────────────────────────────────────────────┐    │
│     │ val deviceInfo = deviceApi.getCurrentDeviceInfo()               │    │
│     │                                                                  │    │
│     │ → Update station name, location, branch info                    │    │
│     │ → Store in AppState for application-wide access                 │    │
│     └─────────────────────────────────────────────────────────────────┘    │
│                                                                             │
└────────────────────────────────────────────────────────────────────────────┘
```

### 10.2 Initial Data Loading Implementation

```kotlin
class DataLoadingManager(
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
    private val taxRepository: TaxRepository,
    private val lookupRepository: LookupRepository
) {
    
    suspend fun loadAllData(
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val steps = listOf(
                "Categories" to { categoryRepository.syncAll() },
                "Taxes" to { taxRepository.syncAll() },
                "Products" to { productRepository.syncAll() },
                "Lookup Groups" to { lookupRepository.syncAll() }
            )
            
            steps.forEachIndexed { index, (name, action) ->
                onProgress(index.toFloat() / steps.size, "Loading $name...")
                action()
            }
            
            onProgress(1f, "Complete")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

---

## 11. Heartbeat & Ongoing Device Health

### 11.1 Android HeartbeatWorker (WorkManager)

```kotlin
class HeartbeatWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    @Inject
    lateinit var registrationApi: RegistrationApi
    
    @Inject
    lateinit var deviceUpdateApi: DeviceUpdateApi
    
    @Inject
    lateinit var updateProcessor: UpdateProcessor
    
    override suspend fun doWork(): Result {
        return try {
            val response = registrationApi.getHeartbeat()
            
            if ((response.messageCount ?: 0) > 0) {
                val updates = deviceUpdateApi.getUpdates()
                updates.forEach { update ->
                    updateProcessor.processUpdate(update)
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
    
    companion object {
        private const val WORK_NAME = "heartbeat_worker"
        
        fun enqueue(workManager: WorkManager) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val request = PeriodicWorkRequestBuilder<HeartbeatWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()
            
            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
        
        fun cancel(workManager: WorkManager) {
            workManager.cancelUniqueWork(WORK_NAME)
        }
    }
}
```

### 11.2 Desktop Heartbeat Manager (Coroutines)

```kotlin
class HeartbeatManager(
    private val registrationApi: RegistrationApi,
    private val deviceUpdateApi: DeviceUpdateApi,
    private val updateProcessor: UpdateProcessor,
    private val scope: CoroutineScope
) {
    private var heartbeatJob: Job? = null
    
    companion object {
        private const val HEARTBEAT_INTERVAL_MS = 60_000L  // 1 minute
    }
    
    fun start() {
        if (heartbeatJob?.isActive == true) {
            println("HeartbeatManager is already running")
            return
        }
        
        heartbeatJob = scope.launch {
            while (isActive) {
                try {
                    sendHeartbeat()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    println("Heartbeat failed: ${e.message}")
                }
                
                delay(HEARTBEAT_INTERVAL_MS)
            }
        }
        
        println("HeartbeatManager started")
    }
    
    private suspend fun sendHeartbeat() {
        val response = registrationApi.getHeartbeat()
        println("Heartbeat: ${response.messageCount} pending messages")
        
        if ((response.messageCount ?: 0) > 0) {
            val updates = deviceUpdateApi.getUpdates()
            
            updates.forEach { update ->
                try {
                    updateProcessor.processUpdate(update)
                } catch (e: Exception) {
                    println("Failed to process update: ${e.message}")
                }
            }
        }
    }
    
    fun stop() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        println("HeartbeatManager stopped")
    }
}
```

### 11.3 Supported Update Types

| Entity | Description |
|--------|-------------|
| Product | Product catalog changes |
| BranchProduct | Branch-specific product data |
| Category | Category hierarchy changes |
| Tax | Tax rate changes |
| CRV | California redemption value changes |
| LookupGroup | Quick-access button changes |
| ProductImage | Product image updates |
| ProductTax | Product-tax mapping changes |
| DeviceInfo | Hardware configuration updates |
| DeviceAttribute | Device attribute updates |
| ConditionalSale | Age restriction changes |

---

## 12. Unregistration & Re-registration

### 12.1 Scenarios That Trigger Re-registration

| Scenario | Trigger | Action |
|----------|---------|--------|
| Environment Change | User changes environment (Dev/Staging/Prod) | Delete existing data, restart registration |
| Database Delete | Admin deletes database via settings | Keep PosSystem, reinitialize other tables |
| API Key Revocation | Backend revokes device API key | API calls fail with 401, requires new registration |
| Factory Reset | User performs factory reset | Clear all local data, restart registration |

### 12.2 Environment Change Implementation

```kotlin
class EnvironmentManager(
    private val deviceRepository: DeviceRepository,
    private val dataRepository: DataRepository,
    private val apiManager: ApiManager,
    private val database: Database
) {
    
    suspend fun changeEnvironment(newEnvironment: Environment) {
        withContext(Dispatchers.IO) {
            // Clear API credentials
            apiManager.clearApiKey()
            
            // Delete all collections except PosSystem
            val collections = database.getCollections("pos")
            collections.forEach { collection ->
                if (collection.name != PosSystemEntity.COLLECTION_NAME) {
                    database.deleteCollection(collection.name, "pos")
                }
            }
            
            // Update environment configuration
            BuildConfig.setEnvironment(newEnvironment)
            
            // Reinitialize data tables
            dataRepository.initializeTables()
        }
    }
    
    suspend fun factoryReset() {
        withContext(Dispatchers.IO) {
            // Clear everything including registration
            apiManager.clearApiKey()
            
            val collections = database.getCollections("pos")
            collections.forEach { collection ->
                database.deleteCollection(collection.name, "pos")
            }
            
            dataRepository.initializeTables()
        }
    }
}
```

### 12.3 Handling Invalid/Revoked API Key

```kotlin
class AuthInterceptor(
    private val deviceRepository: DeviceRepository,
    private val employeeApi: EmployeeApi,
    private val apiManager: ApiManager
) {
    
    suspend fun handleUnauthorized(): Boolean {
        return try {
            val deviceInfo = deviceRepository.getDeviceInfo()
            val refreshToken = deviceInfo?.refreshToken ?: return false
            
            val tokenResponse = employeeApi.refreshToken(
                RefreshTokenRequest(
                    token = refreshToken,
                    clientName = "device"
                )
            )
            
            if (tokenResponse != null) {
                // Save new refresh token
                deviceRepository.updateRefreshToken(tokenResponse.refreshToken)
                
                // Update bearer token
                apiManager.setBearerToken(tokenResponse.accessToken)
                
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}

// Ktor interceptor
class AuthPlugin : HttpClientPlugin<Unit, AuthPlugin> {
    
    override val key = AttributeKey<AuthPlugin>("AuthPlugin")
    
    override fun prepare(block: Unit.() -> Unit) = AuthPlugin()
    
    override fun install(plugin: AuthPlugin, scope: HttpClient) {
        scope.receivePipeline.intercept(HttpReceivePipeline.After) { response ->
            if (response.status == HttpStatusCode.Unauthorized) {
                val success = authInterceptor.handleUnauthorized()
                if (success) {
                    // Retry the request
                    proceedWith(scope.request(response.request))
                }
            }
        }
    }
}
```

### 12.4 Settings Screen for Re-registration

```kotlin
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel(),
    onEnvironmentChanged: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Environment Selection
        SettingsSection(title = "Environment") {
            Environment.entries.forEach { env ->
                EnvironmentOption(
                    environment = env,
                    isSelected = uiState.currentEnvironment == env,
                    onClick = {
                        viewModel.changeEnvironment(env)
                        onEnvironmentChanged()
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Database Actions
        SettingsSection(title = "Database") {
            SettingsButton(
                text = "Clear Data (Keep Registration)",
                icon = Icons.Default.Delete,
                onClick = viewModel::clearData
            )
            
            SettingsButton(
                text = "Re-download All Data",
                icon = Icons.Default.CloudDownload,
                onClick = viewModel::redownloadData
            )
            
            SettingsButton(
                text = "Factory Reset",
                icon = Icons.Default.RestartAlt,
                destructive = true,
                onClick = viewModel::factoryReset
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Heartbeat Status
        SettingsSection(title = "Device Status") {
            Text("Last Heartbeat: ${uiState.lastHeartbeat}")
            Text("Pending Updates: ${uiState.pendingUpdates}")
            
            SettingsButton(
                text = "Send Heartbeat Now",
                icon = Icons.Default.Sync,
                onClick = viewModel::sendHeartbeat
            )
        }
    }
}
```

---

## 13. Error Handling

### 13.1 Registration API Errors

| HTTP Code | Cause | Handling |
|-----------|-------|----------|
| 400 | Invalid request data | Show error, allow retry |
| 401 | Unauthorized (token expired) | Request new QR code |
| 500 | Server error | Show error, allow refresh |
| Network Error | No connectivity | Show offline message |

### 13.2 Error Handling with Result Type

```kotlin
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(
        val message: String,
        val code: Int? = null,
        val exception: Exception? = null
    ) : ApiResult<Nothing>()
    
    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    
    fun getOrNull(): T? = (this as? Success)?.data
    fun exceptionOrNull(): Exception? = (this as? Error)?.exception
}

suspend fun <T> safeApiCall(
    call: suspend () -> T
): ApiResult<T> {
    return try {
        ApiResult.Success(call())
    } catch (e: ClientRequestException) {
        ApiResult.Error(
            message = e.message,
            code = e.response.status.value,
            exception = e
        )
    } catch (e: ServerResponseException) {
        ApiResult.Error(
            message = "Server error: ${e.message}",
            code = e.response.status.value,
            exception = e
        )
    } catch (e: IOException) {
        ApiResult.Error(
            message = "Network error: ${e.message}",
            exception = e
        )
    } catch (e: Exception) {
        ApiResult.Error(
            message = e.message ?: "Unknown error",
            exception = e
        )
    }
}
```

### 13.3 Error UI State

```kotlin
data class ErrorState(
    val message: String,
    val code: Int? = null,
    val isRetryable: Boolean = true,
    val action: ErrorAction = ErrorAction.RETRY
)

enum class ErrorAction {
    RETRY,
    REFRESH_QR,
    RESTART_APP,
    CONTACT_SUPPORT
}

@Composable
fun ErrorDialog(
    errorState: ErrorState,
    onDismiss: () -> Unit,
    onAction: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text("Error")
        },
        text = {
            Text(errorState.message)
        },
        confirmButton = {
            if (errorState.isRetryable) {
                Button(onClick = onAction) {
                    Text(
                        when (errorState.action) {
                            ErrorAction.RETRY -> "Try Again"
                            ErrorAction.REFRESH_QR -> "Get New Code"
                            ErrorAction.RESTART_APP -> "Restart"
                            ErrorAction.CONTACT_SUPPORT -> "Contact Support"
                        }
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    )
}
```

---

## 14. Security Considerations

### 14.1 API Key Protection

| Aspect | Implementation |
|--------|----------------|
| **Storage** | Couchbase Lite database (encrypted at rest on device) |
| **Transmission** | Always HTTPS with TLS 1.2+ |
| **Header Only** | Sent as `x-api-key` header, never in URL |
| **No Logging** | API key is never logged or displayed in UI |
| **Environment Isolation** | Separate keys per environment (Dev/Staging/Prod) |

### 14.2 Encrypted Storage

```kotlin
// Android - EncryptedSharedPreferences for sensitive data
class SecureStorage(context: Context) {
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    fun saveApiKey(apiKey: String) {
        sharedPreferences.edit().putString("api_key", apiKey).apply()
    }
    
    fun getApiKey(): String? {
        return sharedPreferences.getString("api_key", null)
    }
    
    fun clearApiKey() {
        sharedPreferences.edit().remove("api_key").apply()
    }
}

// Desktop - Store in Couchbase Lite with encryption config
class DatabaseManager {
    
    fun createDatabase(path: String): Database {
        val config = DatabaseConfiguration().apply {
            directory = path
            // Enable encryption if available
            encryptionKey = EncryptionKey("your-encryption-key")
        }
        
        return Database("gropos", config)
    }
}
```

### 14.3 QR Code Security

| Feature | Description |
|---------|-------------|
| **Time-Limited** | QR codes expire after 10 minutes |
| **Single-Use** | Each QR code can only register one device |
| **Admin Required** | Only authorized admins can complete registration |
| **Temporary Token** | Initial API calls use temporary bearer token |

### 14.4 Authentication Flow After Registration

```
┌─────────────────────────────────────────────────────────────────────┐
│                   AUTHENTICATION HEADERS                             │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  DURING REGISTRATION:                                                │
│  ┌────────────────────────────────────────────────────────────┐     │
│  │ Authorization: Bearer <temporaryAccessToken>               │     │
│  │ version: v1                                                │     │
│  │ Ocp-Apim-Subscription-Key: <subscriptionKey> (optional)   │     │
│  └────────────────────────────────────────────────────────────┘     │
│                                                                      │
│  AFTER REGISTRATION (All API Calls):                                │
│  ┌────────────────────────────────────────────────────────────┐     │
│  │ x-api-key: <deviceApiKey>                                  │     │
│  │ version: v1                                                │     │
│  └────────────────────────────────────────────────────────────┘     │
│                                                                      │
│  AFTER EMPLOYEE LOGIN:                                               │
│  ┌────────────────────────────────────────────────────────────┐     │
│  │ x-api-key: <deviceApiKey>                                  │     │
│  │ Authorization: Bearer <employeeAccessToken>                │     │
│  │ version: v1                                                │     │
│  └────────────────────────────────────────────────────────────┘     │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 15. Platform-Specific Considerations

### 15.1 Android-Specific

| Feature | Implementation |
|---------|----------------|
| Background Work | WorkManager for reliable scheduling |
| Secure Storage | EncryptedSharedPreferences |
| Network State | ConnectivityManager |
| Lifecycle | ViewModel with SavedStateHandle |
| Database Path | `context.filesDir` |

```kotlin
// androidMain
actual class PlatformConfig actual constructor() {
    actual val databasePath: String
        get() = ApplicationContext.get().filesDir.absolutePath
    
    actual fun scheduleHeartbeat() {
        HeartbeatWorker.enqueue(WorkManager.getInstance(ApplicationContext.get()))
    }
    
    actual fun cancelHeartbeat() {
        HeartbeatWorker.cancel(WorkManager.getInstance(ApplicationContext.get()))
    }
}
```

### 15.2 Desktop-Specific (Windows/Linux)

| Feature | Implementation |
|---------|----------------|
| Background Work | Coroutine Jobs with SupervisorScope |
| Secure Storage | Couchbase Lite encryption |
| Network State | Socket connectivity check |
| Lifecycle | Application scope |
| Database Path | User app data directory |

```kotlin
// desktopMain
actual class PlatformConfig actual constructor() {
    actual val databasePath: String
        get() {
            val os = System.getProperty("os.name").lowercase()
            return when {
                os.contains("win") -> {
                    "${System.getenv("APPDATA")}\\GroPOS"
                }
                os.contains("linux") -> {
                    "${System.getProperty("user.home")}/.gropos"
                }
                else -> {
                    "${System.getProperty("user.home")}/.gropos"
                }
            }
        }
    
    private var heartbeatJob: Job? = null
    
    actual fun scheduleHeartbeat() {
        heartbeatJob = applicationScope.launch {
            while (isActive) {
                try {
                    heartbeatManager.sendHeartbeat()
                } catch (e: Exception) {
                    println("Heartbeat failed: ${e.message}")
                }
                delay(60_000L)
            }
        }
    }
    
    actual fun cancelHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }
}
```

### 15.3 Compose Multiplatform Considerations

```kotlin
// Common theme configuration
@Composable
expect fun GrowPOSTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
)

// Android implementation
@Composable
actual fun GrowPOSTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Desktop implementation
@Composable
actual fun GrowPOSTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

---

## Appendix A: Dependency Injection Setup (Koin)

```kotlin
val appModule = module {
    // Database
    single { DatabaseManager().createDatabase(get<PlatformConfig>().databasePath) }
    
    // Repositories
    single<DeviceRepository> { DeviceRepositoryImpl(get()) }
    single<DataRepository> { DataRepositoryImpl(get()) }
    
    // API
    single { ApiManager(BuildConfig.TRANSACTIONS_API_URL) }
    single { RegistrationApiImpl(get<ApiManager>().httpClient, BuildConfig.TRANSACTIONS_API_URL) }
    
    // Use Cases
    factory { RegisterDeviceUseCase(get(), get()) }
    
    // ViewModels
    viewModelOf(::RegistrationViewModel)
    viewModelOf(::LoginViewModel)
    viewModelOf(::SettingsViewModel)
    
    // Managers
    single { HeartbeatManager(get(), get(), get(), get()) }
    single { EnvironmentManager(get(), get(), get(), get()) }
}
```

---

## Appendix B: Quick Reference

### Registration Endpoints Summary

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/device-registration/qr-registration` | POST | Request new QR code |
| `/device-registration/device-status/{guid}` | GET | Check registration status |
| `/device-registration/heartbeat` | GET | Check for pending updates |

### Polling Configuration

| Parameter | Value |
|-----------|-------|
| Poll Interval | 10 seconds |
| Default Timeout | 10 minutes |
| Extended Timeout | 60 minutes |
| Heartbeat Interval | 1 minute |

### Device Types

| Value | Name |
|-------|------|
| 0 | GrowPOS |
| 1 | oneTime |
| 2 | oneStore |
| 3 | oneServer |
| 4 | Scale |
| 5 | ESLServer |
| 6 | ESLTag |
| 7 | Dashboard |
| 8 | RegisterScaleCamera |
| 9 | PrintingScaleCamera |
| 10 | ShrinkProductionCamera |
| 11 | oneScanner |
| 12 | onePay |
| 13 | onePoint |

---

## Appendix C: Migration Checklist

### From Java/JavaFX to Kotlin/Compose

- [ ] Convert `LoginView.java` → `RegistrationScreen.kt` (Compose)
- [ ] Convert `LoginViewModel.java` → `RegistrationViewModel.kt` (ViewModel + StateFlow)
- [ ] Convert `PosSystemViewModel.java` → `PosSystemEntity.kt` (data class)
- [ ] Convert `PosSystem.java` → `DeviceRepository.kt` (Couchbase operations)
- [ ] Convert `ScheduledExecutorService` → Kotlin Coroutines
- [ ] Convert `Platform.runLater` → `LaunchedEffect` / `viewModelScope`
- [ ] Convert FXML layouts → Compose `@Composable` functions
- [ ] Convert `RegistrationApi` (generated) → Ktor client
- [ ] Add WorkManager for Android background tasks
- [ ] Add multiplatform expect/actual for platform-specific code

---

*Last Updated: January 2026*

