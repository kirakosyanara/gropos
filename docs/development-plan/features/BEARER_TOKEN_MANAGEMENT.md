# Bearer Token / Access Token Management

**Version:** 2.0 (Kotlin/Compose Multiplatform)  
**Target Platforms:** Windows, Linux, Android  
**Status:** Specification Document

This document provides a comprehensive overview of how bearer tokens and access tokens are created, stored, refreshed, and used throughout the GroPOS application using Kotlin, Compose Multiplatform, and Couchbase Lite.

## Table of Contents

1. [Overview](#overview)
2. [Authentication Architecture](#authentication-architecture)
3. [Token Types](#token-types)
4. [Token Lifecycle](#token-lifecycle)
5. [Token Creation (Login Flow)](#token-creation-login-flow)
6. [Token Storage](#token-storage)
7. [Token Usage in API Calls](#token-usage-in-api-calls)
8. [Token Refresh Mechanism](#token-refresh-mechanism)
9. [Token Invalidation (Logout Flow)](#token-invalidation-logout-flow)
10. [Implementation Details](#implementation-details)
11. [Error Handling](#error-handling)
12. [Security Considerations](#security-considerations)
13. [Platform-Specific Considerations](#platform-specific-considerations)

---

## Overview

GroPOS uses a dual-authentication system:

1. **Bearer Token (JWT)** - Used for authenticated employee/user operations after login
2. **API Key** - Used for device-level operations (heartbeat, data sync, device registration)

The bearer token is a JWT (JSON Web Token) obtained during the login process and attached to all authenticated API requests via the `Authorization: Bearer <token>` header.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Authentication Flow Overview                         │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌──────────────┐    Login     ┌──────────────┐    Token    ┌────────────┐  │
│  │   Employee   │ ──────────▶  │  Backend API │ ──────────▶ │  GroPOS    │  │
│  │  (PIN Entry) │              │  (Auth)      │             │  (Stores)  │  │
│  └──────────────┘              └──────────────┘             └────────────┘  │
│                                                                      │       │
│                                                                      ▼       │
│  ┌──────────────┐   Bearer     ┌──────────────┐   Uses      ┌────────────┐  │
│  │  Backend API │ ◀───────────  │  ApiManager  │ ◀────────  │ All Views  │  │
│  │  (Protected) │   Token       │ (HttpClient) │   Client   │  & Repos   │  │
│  └──────────────┘              └──────────────┘             └────────────┘  │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Authentication Architecture

### Technology Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin 2.0+ |
| UI Framework | Compose Multiplatform |
| HTTP Client | Ktor Client |
| Local Database | Couchbase Lite |
| DI Framework | Hilt (Android) / Koin (Desktop) |
| Async | Kotlin Coroutines + Flow |
| Serialization | Kotlinx Serialization |

### API Client Configuration

The application maintains two separate HTTP clients in `ApiManager.kt`:

```kotlin
// File: shared/src/commonMain/kotlin/com/unisight/gropos/network/ApiManager.kt

/**
 * Central API client manager for all network operations.
 * 
 * Maintains two clients:
 * - authenticatedClient: Bearer token auth for user operations
 * - deviceClient: API key auth for device-level operations
 */
object ApiManager {
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        coerceInputValues = true
    }
    
    /**
     * Primary HTTP client using Bearer Token authentication.
     * Used for authenticated user operations after login.
     */
    private var authenticatedClient: HttpClient? = null
    
    /**
     * Secondary HTTP client using API Key authentication.
     * Used for device-level operations (heartbeat, data sync).
     */
    private var deviceClient: HttpClient? = null
    
    private var baseUrl: String = ""
    private var transactionsBaseUrl: String = ""
    private var currentApiKey: String? = null
    private var currentBearerToken: String? = null
    
    // API instances
    val employeeApi: EmployeeApi get() = EmployeeApi(requireAuthenticatedClient())
    val employeeApiDevice: EmployeeApi get() = EmployeeApi(requireDeviceClient())
    val transactionApi: TransactionApi get() = TransactionApi(requireAuthenticatedClient())
    val productApi: ProductApi get() = ProductApi(requireDeviceClient())
    val deviceApi: DeviceApi get() = DeviceApi(requireDeviceClient())
}
```

### Client Initialization

During application startup, both clients are initialized:

```kotlin
/**
 * Initialize API clients with environment configuration.
 * Called once during app startup after environment is determined.
 */
fun initialize(environment: Environment) {
    baseUrl = when (environment) {
        Environment.DEV -> "https://apim-service-unisight-dev.azure-api.net"
        Environment.STAGING -> "https://apim-service-unisight-staging.azure-api.net"
        Environment.PROD -> "https://apim-service-unisight-prod.azure-api.net"
    }
    
    transactionsBaseUrl = when (environment) {
        Environment.DEV -> "https://func-transactions-dev.azure-api.net"
        Environment.STAGING -> "https://func-transactions-staging.azure-api.net"
        Environment.PROD -> "https://func-transactions-prod.azure-api.net"
    }
    
    // Initialize base device client (no auth yet)
    deviceClient = createBaseClient(baseUrl)
}

private fun createBaseClient(url: String): HttpClient {
    return HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
        install(Logging) {
            level = if (BuildConfig.DEBUG) LogLevel.BODY else LogLevel.NONE
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 10_000
        }
        defaultRequest {
            url(url)
            header("Content-Type", "application/json")
            header("version", BuildConfig.VERSION)
        }
    }
}
```

---

## Token Types

### 1. Access Token (Bearer Token)

| Property | Value |
|----------|-------|
| **Type** | JWT (JSON Web Token) |
| **Purpose** | Authenticate employee operations |
| **Header** | `Authorization: Bearer <token>` |
| **Expiration** | Limited lifespan (managed by backend) |
| **Storage** | In-memory (via `ApiManager.authenticatedClient` interceptor) |

### 2. Refresh Token

| Property | Value |
|----------|-------|
| **Purpose** | Obtain new access token when current one expires |
| **Storage** | Persisted in Couchbase Lite (`PosSystem.refreshToken`) |
| **Usage** | Used with `employeeApi.refreshToken()` suspend function |

### 3. API Key

| Property | Value |
|----------|-------|
| **Purpose** | Device-level authentication (heartbeat, data sync) |
| **Header** | `x-api-key: <apiKey>` |
| **Storage** | Persisted in Couchbase Lite (`PosSystem.apiKey`) |
| **Obtained** | During device registration |

---

## Token Lifecycle

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Token Lifecycle                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   ┌───────────────┐                                                          │
│   │ Device        │                                                          │
│   │ Registration  │──▶ Receives API Key ──▶ Stored in Couchbase Lite        │
│   └───────────────┘                                                          │
│           │                                                                  │
│           ▼                                                                  │
│   ┌───────────────┐                                                          │
│   │ Employee      │──▶ Receives AccessToken + RefreshToken                   │
│   │ Login         │         │                    │                           │
│   └───────────────┘         │                    │                           │
│                             ▼                    ▼                           │
│                    ┌────────────────┐   ┌─────────────────┐                 │
│                    │ In-Memory      │   │ Couchbase Lite  │                 │
│                    │ (ApiManager)   │   │ (PosSystem)     │                 │
│                    └────────────────┘   └─────────────────┘                 │
│                             │                    │                           │
│                             ▼                    │                           │
│   ┌───────────────┐  API Request ──▶ 401 Unauthorized?                      │
│   │ Authenticated │         │                    │                           │
│   │ API Calls     │         │                    ▼                           │
│   └───────────────┘         │           ┌─────────────────┐                 │
│                             │           │ Token Refresh   │                 │
│                             │           │ (Coroutine)     │                 │
│                             │           └─────────────────┘                 │
│                             │                    │                           │
│                             ◀────────────────────┘                           │
│                                                                              │
│   ┌───────────────┐                                                          │
│   │ Logout /      │──▶ Clear state ──▶ Token becomes invalid                │
│   │ Lock Screen   │                                                          │
│   └───────────────┘                                                          │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Token Creation (Login Flow)

### AuthRepository Implementation

The bearer token is created when an employee logs in via `AuthRepository.login()`:

```kotlin
// File: shared/src/commonMain/kotlin/com/unisight/gropos/repository/AuthRepository.kt

/**
 * Repository handling all authentication operations.
 * Uses dependency injection for testability.
 */
class AuthRepository @Inject constructor(
    private val tokenStorage: TokenStorage,
    private val posSystemRepository: PosSystemRepository,
    private val appStore: AppStore
) {
    private val logger = Logger.withTag("AuthRepository")
    
    /**
     * Authenticate employee with PIN and branch information.
     * 
     * @param username Employee email address
     * @param password Employee PIN code
     * @param tillId Assigned till/register account ID
     * @return UserProfileViewModel on success
     * @throws AuthException on authentication failure
     */
    suspend fun login(
        username: String,
        password: String,
        tillId: Int
    ): UserProfileViewModel = withContext(Dispatchers.IO) {
        
        logger.info { "Making login request for user: $username" }
        
        // 1. Build login request
        val loginRequest = CashierLoginRequest(
            userName = username,
            password = password,
            clientName = "device",
            authenticationKey = null,
            locationAccountId = tillId,
            branchId = appStore.branch.value?.id ?: throw AuthException("Branch not set"),
            deviceId = appStore.branch.value?.stationId ?: throw AuthException("Station not set")
        )
        
        // 2. Call login API using device client (API key auth)
        val tokenResponse = ApiManager.employeeApiDevice.login(loginRequest)
        
        // 3. Store refresh token persistently
        posSystemRepository.updateRefreshToken(tokenResponse.refreshToken)
        
        // 4. Set bearer token for authenticated requests
        ApiManager.setBearerToken(tokenResponse.accessToken)
        
        // 5. Fetch user profile using authenticated client
        val profile = ApiManager.employeeApi.getProfile()
        
        // 6. Filter to only GroPOS permissions
        val filteredProfile = profile.copy(
            permissions = profile.permissions?.filter { 
                it.startsWith("GroPOS") || it.startsWith("GrowPOS")
            }
        )
        
        // 7. Store employee in AppStore
        appStore.setEmployee(filteredProfile)
        
        // 8. Load branch settings
        val branchSettings = ApiManager.branchApi.getAllSettings()
        branchSettings.forEach { setting ->
            posSystemRepository.saveBranchSetting(setting)
        }
        
        logger.info { "Login successful for user: ${profile.firstName} ${profile.lastName}" }
        
        return@withContext filteredProfile
    }
}
```

### LoginViewModel (Compose)

```kotlin
// File: shared/src/commonMain/kotlin/com/unisight/gropos/ui/login/LoginViewModel.kt

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val appStore: AppStore
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()
    
    /**
     * Attempt login with current form values.
     */
    fun login() {
        val currentEmployee = _uiState.value.selectedEmployee ?: return
        val pin = _uiState.value.pin
        val tillId = _uiState.value.selectedTillId ?: return
        
        if (pin.length < 5) {
            _uiState.update { it.copy(error = "Please enter a valid PIN") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                val profile = authRepository.login(
                    username = currentEmployee.email,
                    password = pin,
                    tillId = tillId
                )
                
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        loginSuccess = true
                    )
                }
            } catch (e: AuthException) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Authentication failed",
                        pin = ""  // Clear PIN on failure
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Connection error. Please try again."
                    )
                }
            }
        }
    }
}
```

### Login Request Model

The `CashierLoginRequest` contains:

| Field | Type | Description |
|-------|------|-------------|
| `userName` | String | Employee email address |
| `password` | String | Employee PIN code |
| `clientName` | String | Always "device" |
| `authenticationKey` | String? | Hardware token (optional) |
| `locationAccountId` | Int | Assigned till/register account ID |
| `branchId` | Int | Current branch/store ID |
| `deviceId` | Int | POS station/device ID |

### Login Response Model

```kotlin
@Serializable
data class TokenViewModel(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long? = null,
    val tokenType: String = "Bearer"
)
```

---

## Token Storage

### In-Memory Storage (Access Token)

The access token is stored in-memory by configuring the `authenticatedClient`:

```kotlin
// File: shared/src/commonMain/kotlin/com/unisight/gropos/network/ApiManager.kt

/**
 * Sets the Bearer token for authenticated API requests.
 * 
 * Called after successful user login. Configures the authenticated HTTP client
 * to include the Authorization header in all subsequent requests.
 * 
 * @param token JWT token obtained from login API
 */
fun setBearerToken(token: String) {
    currentBearerToken = token
    
    authenticatedClient = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
        install(Logging) {
            level = if (BuildConfig.DEBUG) LogLevel.BODY else LogLevel.NONE
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 10_000
        }
        defaultRequest {
            url(baseUrl)
            header("Content-Type", "application/json")
            header("Authorization", "Bearer $token")
            header("version", BuildConfig.VERSION)
        }
    }
}

/**
 * Clears the bearer token (on logout).
 */
fun clearBearerToken() {
    currentBearerToken = null
    authenticatedClient?.close()
    authenticatedClient = null
}
```

**Key Points:**
- Token is NOT persisted to disk
- Token is lost on application restart
- Token is applied to all requests made via `authenticatedClient`

### Persistent Storage (Refresh Token & API Key)

The refresh token and API key are persisted in Couchbase Lite via `PosSystem` document:

```kotlin
// File: shared/src/commonMain/kotlin/com/unisight/gropos/data/model/PosSystem.kt

/**
 * System configuration stored in Couchbase Lite.
 * Persists across application restarts.
 */
@Serializable
data class PosSystem(
    val id: String,                    // Environment identifier (e.g., "Production")
    val documentName: String = "appKey",
    val branchName: String = "",
    val apiKey: String = "",           // Device API key
    val refreshToken: String = "",     // Persistent refresh token
    val ipAddress: String = "",        // Camera IP
    val cameraId: Int = -1,
    val entityId: Int = -1,
    val onePayIpAddress: String = "",  // OnePay terminal IP
    val onePayId: Int = -1,
    val onePayEntityId: Int = -1
)
```

### TokenStorage Interface

```kotlin
// File: shared/src/commonMain/kotlin/com/unisight/gropos/data/TokenStorage.kt

/**
 * Interface for token persistence operations.
 * Implemented using Couchbase Lite.
 */
interface TokenStorage {
    
    /**
     * Save refresh token to persistent storage.
     */
    suspend fun saveRefreshToken(token: String)
    
    /**
     * Get stored refresh token.
     * @return Refresh token or null if not stored
     */
    suspend fun getRefreshToken(): String?
    
    /**
     * Save API key to persistent storage.
     */
    suspend fun saveApiKey(apiKey: String)
    
    /**
     * Get stored API key.
     * @return API key or null if not registered
     */
    suspend fun getApiKey(): String?
    
    /**
     * Clear all tokens (on logout or environment switch).
     */
    suspend fun clearTokens()
}

/**
 * Couchbase Lite implementation of TokenStorage.
 */
class CouchbaseTokenStorage(
    private val database: Database,
    private val environment: Environment
) : TokenStorage {
    
    private val collection = database.getCollection("PosSystem", "pos")
    private val documentId = environment.name
    
    override suspend fun saveRefreshToken(token: String) = withContext(Dispatchers.IO) {
        val doc = collection?.getDocument(documentId)?.toMutable() 
            ?: MutableDocument(documentId)
        doc.setString("refreshToken", token)
        collection?.save(doc)
    }
    
    override suspend fun getRefreshToken(): String? = withContext(Dispatchers.IO) {
        collection?.getDocument(documentId)?.getString("refreshToken")
    }
    
    override suspend fun saveApiKey(apiKey: String) = withContext(Dispatchers.IO) {
        val doc = collection?.getDocument(documentId)?.toMutable() 
            ?: MutableDocument(documentId)
        doc.setString("apiKey", apiKey)
        collection?.save(doc)
    }
    
    override suspend fun getApiKey(): String? = withContext(Dispatchers.IO) {
        collection?.getDocument(documentId)?.getString("apiKey")
    }
    
    override suspend fun clearTokens() = withContext(Dispatchers.IO) {
        collection?.getDocument(documentId)?.let { doc ->
            val mutable = doc.toMutable()
            mutable.remove("refreshToken")
            collection?.save(mutable)
        }
    }
}
```

**Storage Location:** Couchbase Lite database (`PosSystem` collection in `pos` scope)

---

## Token Usage in API Calls

### Choosing the Right API Client

| Client | Authentication | Use Cases |
|--------|---------------|-----------|
| `authenticatedClient` | Bearer Token | Employee profile, Lock/Unlock, Logout, Transactions |
| `deviceClient` | API Key | Device registration, Heartbeat, Data sync, Cashier employee list, Token refresh |

### Usage Pattern Examples

#### Authenticated Requests (Bearer Token)

```kotlin
// After login, use authenticated APIs
class EmployeeRepository @Inject constructor(
    private val appStore: AppStore
) {
    /**
     * Lock the device screen.
     */
    suspend fun lockDevice(lockType: DeviceEventType) {
        val request = DeviceLockRequest(lockType = lockType)
        ApiManager.employeeApi.lockDevice(request)
    }
    
    /**
     * Get current user profile.
     */
    suspend fun getProfile(): UserProfileViewModel {
        return ApiManager.employeeApi.getProfile()
    }
    
    /**
     * Logout current session.
     */
    suspend fun logout() {
        ApiManager.employeeApi.logout()
        ApiManager.clearBearerToken()
        appStore.clearStore()
    }
}
```

#### Device-Level Requests (API Key)

```kotlin
// Use device client for device-level operations
class DeviceRepository @Inject constructor() {
    
    /**
     * Get list of cashier employees (before login).
     */
    suspend fun getCashierEmployees(): List<EmployeeListViewModel> {
        return ApiManager.deviceApi.getCashierEmployees()
    }
    
    /**
     * Refresh expired access token.
     */
    suspend fun refreshToken(refreshToken: String): TokenViewModel {
        val request = RefreshTokenRequest(
            token = refreshToken,
            clientName = "device"
        )
        return ApiManager.employeeApiDevice.refreshToken(request)
    }
}
```

### Transaction API Client Configuration

Transaction-related APIs use a custom client with the transactions base URL:

```kotlin
// File: shared/src/commonMain/kotlin/com/unisight/gropos/network/ApiManager.kt

/**
 * Creates a client for transaction API endpoints.
 * Uses bearer token authentication with transactions-specific base URL.
 */
fun createTransactionClient(): HttpClient {
    return HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
        defaultRequest {
            url(transactionsBaseUrl)
            header("Content-Type", "application/json")
            header("Authorization", "Bearer ${currentBearerToken ?: ""}")
            header("version", BuildConfig.VERSION)
        }
    }
}
```

### Components Using Bearer Token

The following components use authenticated API calls:

| Component | File | Operations |
|-----------|------|------------|
| `LockViewModel` | `ui/lock/LockViewModel.kt` | Verify password, Unlock device |
| `HomeViewModel` | `ui/home/HomeViewModel.kt` | Lock screen action |
| `ManagerApprovalViewModel` | `ui/approval/ManagerApprovalViewModel.kt` | Get employees with permission |
| `InactivityManager` | `util/InactivityManager.kt` | Auto-lock timeout |
| `TransactionRepository` | `repository/TransactionRepository.kt` | Create/Update transactions |
| `CashRepository` | `repository/CashRepository.kt` | Cash pickup operations |
| `VendorPayoutRepository` | `repository/VendorPayoutRepository.kt` | Vendor payout operations |

---

## Token Refresh Mechanism

### When Token Refresh Occurs

Token refresh is triggered when an API call returns a **401 Unauthorized** error, indicating the access token has expired.

### TokenRefreshInterceptor

```kotlin
// File: shared/src/commonMain/kotlin/com/unisight/gropos/network/TokenRefreshInterceptor.kt

/**
 * Interceptor that handles automatic token refresh on 401 errors.
 */
class TokenRefreshInterceptor(
    private val tokenStorage: TokenStorage,
    private val onTokenRefreshed: (TokenViewModel) -> Unit,
    private val onRefreshFailed: () -> Unit
) {
    
    private val mutex = Mutex()
    private var isRefreshing = false
    
    /**
     * Wraps an API call with automatic token refresh on 401.
     */
    suspend fun <T> withTokenRefresh(
        call: suspend () -> T
    ): T {
        return try {
            call()
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.Unauthorized) {
                refreshTokenAndRetry(call)
            } else {
                throw e
            }
        }
    }
    
    private suspend fun <T> refreshTokenAndRetry(
        originalCall: suspend () -> T
    ): T {
        // Use mutex to prevent multiple simultaneous refresh attempts
        mutex.withLock {
            if (isRefreshing) {
                // Wait for ongoing refresh to complete
                delay(1000)
                return originalCall()
            }
            
            isRefreshing = true
        }
        
        try {
            val refreshToken = tokenStorage.getRefreshToken()
                ?: throw AuthException("No refresh token available")
            
            val request = RefreshTokenRequest(
                token = refreshToken,
                clientName = "device"
            )
            
            // Use device client for token refresh
            val newToken = ApiManager.employeeApiDevice.refreshToken(request)
            
            // Update stored tokens
            tokenStorage.saveRefreshToken(newToken.refreshToken)
            ApiManager.setBearerToken(newToken.accessToken)
            
            onTokenRefreshed(newToken)
            
            // Retry original call
            return originalCall()
            
        } catch (e: Exception) {
            onRefreshFailed()
            throw AuthException("Token refresh failed: ${e.message}")
        } finally {
            mutex.withLock {
                isRefreshing = false
            }
        }
    }
}
```

### Repository Usage Pattern

```kotlin
class TransactionRepository @Inject constructor(
    private val tokenRefreshInterceptor: TokenRefreshInterceptor
) {
    
    suspend fun createTransaction(
        transaction: TransactionViewModel,
        products: List<TransactionItemViewModel>,
        payments: List<TransactionPaymentViewModel>
    ): CreateTransactionResponse {
        
        return tokenRefreshInterceptor.withTokenRefresh {
            val request = CreateTransactionRequest(
                transaction = transaction.toAddEditRequest(),
                items = products.map { it.toAddEditRequest() },
                payments = payments.map { it.toAddEditRequest() }
            )
            
            ApiManager.transactionApi.createTransaction(request)
        }
    }
}
```

### Components Implementing Token Refresh

| Component | Scenario |
|-----------|----------|
| `LockViewModel` | Password verification after lock |
| `HomeViewModel` | Lock screen action |
| `ManagerApprovalViewModel` | Getting employees with approval permission |
| `InactivityManager` | Auto-lock timeout |
| `TransactionRepository` | Creating transactions |
| `CashRepository` | Cash pickup operations |

---

## Token Invalidation (Logout Flow)

### Logout Process

```kotlin
// File: shared/src/commonMain/kotlin/com/unisight/gropos/repository/AuthRepository.kt

/**
 * Logout current user session.
 * 
 * @param isEndOfShift Whether to trigger end-of-shift procedures
 */
suspend fun logout(isEndOfShift: Boolean = false) = withContext(Dispatchers.IO) {
    try {
        // Unlock device first
        val unlockRequest = DeviceLockRequest(lockType = DeviceEventType.Unlocked)
        ApiManager.employeeApi.lockDevice(unlockRequest)
        
        // Call appropriate logout endpoint
        if (isEndOfShift) {
            ApiManager.employeeApi.logoutWithEndOfShift()
        } else {
            ApiManager.employeeApi.logout()
        }
    } catch (e: Exception) {
        logger.error(e) { "Error during logout API call" }
        // Continue with local cleanup even if API fails
    }
    
    // Clear application state
    ApiManager.clearBearerToken()
    appStore.clearStore()
    
    logger.info { "Logout completed" }
}
```

### LockScreen Logout Flow

```kotlin
// File: shared/src/commonMain/kotlin/com/unisight/gropos/ui/lock/LockViewModel.kt

@HiltViewModel
class LockViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val hardwareManager: HardwareManager,
    private val appStore: AppStore
) : ViewModel() {
    
    /**
     * Force logout with manager approval.
     */
    fun forceLogout(isEndOfShift: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                authRepository.logout(isEndOfShift)
                
                // Open cash drawer for end of shift
                if (isEndOfShift) {
                    hardwareManager.openCashDrawer()
                }
                
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        navigateToLogin = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Logout failed: ${e.message}"
                    )
                }
            }
        }
    }
}
```

### State Cleared on Logout

1. **AppStore**: Employee profile set to null
2. **OrderStore**: Transaction data cleared
3. **ApiManager**: Bearer token cleared from authenticated client
4. **Note**: Refresh token remains in Couchbase Lite but is invalidated server-side

### Lock Screen vs Logout

| Action | Token Status | User Data | Re-authentication |
|--------|-------------|-----------|-------------------|
| **Lock Screen** | Preserved | Preserved | PIN verification only |
| **Logout** | Invalidated (server-side) | Cleared | Full login required |
| **End of Shift** | Invalidated | Cleared | Opens cash drawer + Full login |

---

## Implementation Details

### ApiManager Key Methods

```kotlin
// File: shared/src/commonMain/kotlin/com/unisight/gropos/network/ApiManager.kt

/**
 * Set bearer token after login.
 */
fun setBearerToken(token: String) {
    currentBearerToken = token
    authenticatedClient = createAuthenticatedClient(token)
}

/**
 * Set API key after device registration.
 */
fun setApiKey(apiKey: String) {
    currentApiKey = apiKey
    deviceClient = HttpClient {
        install(ContentNegotiation) { json(json) }
        defaultRequest {
            url(baseUrl)
            header("Content-Type", "application/json")
            header("x-api-key", apiKey)
            header("version", BuildConfig.VERSION)
        }
    }
}

/**
 * Clear bearer token on logout.
 */
fun clearBearerToken() {
    currentBearerToken = null
    authenticatedClient?.close()
    authenticatedClient = null
}

/**
 * Reinitialize clients (for environment switching).
 */
fun reinitialize(environment: Environment) {
    authenticatedClient?.close()
    deviceClient?.close()
    initialize(environment)
    
    // Restore API key if available
    currentApiKey?.let { setApiKey(it) }
    
    // Restore bearer token if available
    currentBearerToken?.let { setBearerToken(it) }
}
```

### Registration API Client Helper

```kotlin
/**
 * Create a client for device registration with optional token.
 */
fun createRegistrationClient(token: String? = null): HttpClient {
    return HttpClient {
        install(ContentNegotiation) { json(json) }
        defaultRequest {
            url(transactionsBaseUrl)
            header("Content-Type", "application/json")
            header("version", BuildConfig.VERSION)
            if (token != null) {
                header("Authorization", "Bearer $token")
            } else {
                // Use API key interceptor from device client
                currentApiKey?.let { header("x-api-key", it) }
            }
        }
    }
}
```

---

## Error Handling

### Common Token-Related Errors

| HTTP Code | Meaning | Application Response |
|-----------|---------|---------------------|
| 401 | Token expired/invalid | Attempt token refresh |
| 403 | Insufficient permissions | Show error, may require manager approval |
| 410 | Resource deleted | Handle gracefully (entity removed) |

### ApiResult Sealed Class

```kotlin
// File: shared/src/commonMain/kotlin/com/unisight/gropos/network/ApiResult.kt

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val code: Int, val message: String) : ApiResult<Nothing>()
    data object NetworkError : ApiResult<Nothing>()
    data object Unauthorized : ApiResult<Nothing>()
}

/**
 * Wrap API calls with standardized error handling.
 */
suspend fun <T> safeApiCall(call: suspend () -> T): ApiResult<T> {
    return try {
        ApiResult.Success(call())
    } catch (e: ClientRequestException) {
        when (e.response.status) {
            HttpStatusCode.Unauthorized -> ApiResult.Unauthorized
            HttpStatusCode.BadRequest -> ApiResult.Error(400, "Validation error")
            HttpStatusCode.Forbidden -> ApiResult.Error(403, "Forbidden")
            HttpStatusCode.NotFound -> ApiResult.Error(404, "Not found")
            HttpStatusCode.Gone -> ApiResult.Error(410, "Entity deleted")
            else -> ApiResult.Error(
                e.response.status.value, 
                e.message ?: "Unknown error"
            )
        }
    } catch (e: ConnectTimeoutException) {
        ApiResult.NetworkError
    } catch (e: Exception) {
        ApiResult.Error(-1, e.message ?: "Unknown error")
    }
}
```

### Error Handling Example

```kotlin
suspend fun verifyPassword(pin: String): Boolean {
    val result = safeApiCall {
        val request = LoginRequest(
            userName = appStore.employee.value?.email ?: "",
            password = pin,
            clientName = "device",
            branchId = appStore.branch.value?.id ?: 0,
            deviceId = appStore.branch.value?.stationId ?: 0
        )
        ApiManager.employeeApi.verifyPassword(request)
    }
    
    return when (result) {
        is ApiResult.Success -> result.data
        is ApiResult.Unauthorized -> {
            // Attempt token refresh
            val refreshed = tokenRefreshInterceptor.withTokenRefresh {
                val request = LoginRequest(/* ... */)
                ApiManager.employeeApi.verifyPassword(request)
            }
            refreshed
        }
        is ApiResult.Error -> {
            logger.error { "Verify password error ${result.code}: ${result.message}" }
            false
        }
        ApiResult.NetworkError -> {
            logger.error { "Network error during password verification" }
            false
        }
    }
}
```

---

## Security Considerations

### Token Security Best Practices

1. **Access Token in Memory Only**
   - Not persisted to disk
   - Lost on application restart (requires re-login)
   - Kotlin `private var` prevents external access

2. **Refresh Token Persistence**
   - Stored in Couchbase Lite (local database)
   - Database file protected by OS-level file permissions
   - Enables session recovery after brief disconnections

3. **API Key Security**
   - Obtained during one-time device registration
   - Stored locally, never transmitted after initial setup
   - Identifies the device, not the user

### Token Transmission

- All token transmission uses HTTPS (SSL/TLS)
- Tokens are never logged in production (`BuildConfig.DEBUG == false`)
- Certificate pinning recommended for production

### Secure Storage by Platform

| Platform | Implementation |
|----------|---------------|
| **Android** | Couchbase Lite with encrypted SharedPreferences for keys |
| **Windows** | Couchbase Lite stored in AppData with NTFS permissions |
| **Linux** | Couchbase Lite stored in ~/.local/share with 600 permissions |

### Session Management

- Auto-lock timeout invalidates active session UI but preserves token
- Manual lock requires PIN verification but token remains valid
- Logout invalidates token server-side

---

## Platform-Specific Considerations

### Android

```kotlin
// Android-specific Couchbase initialization
class AndroidTokenStorage(
    context: Context,
    environment: Environment
) : TokenStorage {
    
    private val database: Database by lazy {
        val config = DatabaseConfiguration()
        config.directory = context.filesDir.absolutePath
        Database("gropos", config)
    }
    
    // ... implementation
}
```

### Windows/Linux (Desktop)

```kotlin
// Desktop-specific Couchbase initialization  
class DesktopTokenStorage(
    environment: Environment
) : TokenStorage {
    
    private val database: Database by lazy {
        val config = DatabaseConfiguration()
        config.directory = getAppDataDirectory()
        Database("gropos", config)
    }
    
    private fun getAppDataDirectory(): String {
        return when {
            System.getProperty("os.name").contains("Windows") -> {
                "${System.getenv("APPDATA")}/GroPOS"
            }
            else -> {
                "${System.getProperty("user.home")}/.local/share/gropos"
            }
        }
    }
    
    // ... implementation
}
```

### Compose Multiplatform Expect/Actual

```kotlin
// commonMain
expect class TokenStorageFactory {
    fun create(environment: Environment): TokenStorage
}

// androidMain
actual class TokenStorageFactory(private val context: Context) {
    actual fun create(environment: Environment): TokenStorage {
        return AndroidTokenStorage(context, environment)
    }
}

// desktopMain
actual class TokenStorageFactory {
    actual fun create(environment: Environment): TokenStorage {
        return DesktopTokenStorage(environment)
    }
}
```

---

## Summary

### Quick Reference

| Item | Value/Location |
|------|----------------|
| **Access Token Header** | `Authorization: Bearer <token>` |
| **API Key Header** | `x-api-key: <apiKey>` |
| **Token Setting Method** | `ApiManager.setBearerToken(token)` |
| **Authenticated Client** | `ApiManager.authenticatedClient` |
| **Device Client** | `ApiManager.deviceClient` |
| **Refresh Token Storage** | `PosSystem.refreshToken` → Couchbase Lite |
| **API Key Storage** | `PosSystem.apiKey` → Couchbase Lite |
| **Login Suspend Function** | `AuthRepository.login()` |
| **Refresh Suspend Function** | `employeeApiDevice.refreshToken()` |
| **Token Response Model** | `TokenViewModel` (accessToken, refreshToken) |

### File Locations

| Purpose | File |
|---------|------|
| API Client Management | `shared/.../network/ApiManager.kt` |
| Auth Repository | `shared/.../repository/AuthRepository.kt` |
| Token Storage | `shared/.../data/TokenStorage.kt` |
| Login ViewModel | `shared/.../ui/login/LoginViewModel.kt` |
| Lock ViewModel | `shared/.../ui/lock/LockViewModel.kt` |
| PosSystem Model | `shared/.../data/model/PosSystem.kt` |
| App State | `shared/.../store/AppStore.kt` |
| Token Refresh | `shared/.../network/TokenRefreshInterceptor.kt` |

### API Endpoints

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/employee/login` | POST | Authenticate employee |
| `/employee/profile` | GET | Get user profile |
| `/employee/lock` | POST | Report device lock |
| `/employee/verify` | POST | Validate unlock PIN |
| `/employee/refresh` | POST | Refresh access token |
| `/employee/logout` | POST | End session |
| `/employee/logout/endofshift` | POST | End session with shift close |

---

## Related Documentation

- [Authentication](./AUTHENTICATION.md)
- [Lock Screen and Cashier Login](./LOCK_SCREEN_AND_CASHIER_LOGIN.md)
- [API Integration](../architecture/API_INTEGRATION.md)
- [State Management](../architecture/STATE_MANAGEMENT.md)
- [Device Registration](./DEVICE_REGISTRATION.md)

---

*Last Updated: January 2026*

