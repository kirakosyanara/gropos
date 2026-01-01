# API Integration

**Version:** 2.0 (Kotlin/Compose Multiplatform)  
**Status:** Specification Document

GroPOS integrates with backend APIs using OpenAPI-generated Kotlin clients. This document explains the API architecture and usage.

---

## Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          API Integration Architecture                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                        OpenAPI Specifications                        │   │
│  │                                                                      │   │
│  │  APIs/                                                               │   │
│  │  ├── Pos.API.json          (Main POS API)                           │   │
│  │  ├── Transactions.API.json (Transaction processing)                 │   │
│  │  ├── Cash.API.json         (Cash management)                        │   │
│  │  ├── Lottery.API.json      (Lottery operations)                     │   │
│  │  └── DeviceRegistration.API.json (Device registration)              │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                    │                                         │
│                        OpenAPI Generator (Kotlin)                            │
│                                    │                                         │
│                                    ▼                                         │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                      Generated Client Code                           │   │
│  │                                                                      │   │
│  │  shared/build/generated/                                             │   │
│  │  ├── api/           (API interfaces)                                │   │
│  │  │   ├── ProductApi.kt                                              │   │
│  │  │   ├── EmployeeApi.kt                                             │   │
│  │  │   ├── TransactionApi.kt                                          │   │
│  │  │   └── ...                                                        │   │
│  │  ├── model/         (Data models)                                   │   │
│  │  │   ├── ProductViewModel.kt                                        │   │
│  │  │   ├── TransactionViewModel.kt                                    │   │
│  │  │   └── ...                                                        │   │
│  │  └── ApiClient.kt   (HTTP client)                                   │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                    │                                         │
│                                    ▼                                         │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                         ApiManager.kt                                │   │
│  │                                                                      │   │
│  │  - authenticatedClient (Bearer token auth)                          │   │
│  │  - deviceClient (API key auth)                                      │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## API Specifications

### Location

API specifications are stored in the `APIs/` directory:

| File | Purpose |
|------|---------|
| `Pos.API.json` | Main POS operations (products, categories, employees) |
| `Transactions.API.json` | Transaction creation and management |
| `Cash.API.json` | Cash pickup, payout operations |
| `Lottery.API.json` | Lottery sales and payouts |
| `DeviceRegistration.API.json` | Device registration and heartbeat |

### Regenerating Clients

When API specs change:

```bash
./gradlew generateAllApis
./gradlew compileKotlin
```

---

## API Clients

### Client Configuration

```kotlin
// ApiManager.kt
object ApiManager {
    private var authenticatedClient: HttpClient? = null
    private var deviceClient: HttpClient? = null
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
    
    fun initialize(baseUrl: String) {
        deviceClient = HttpClient {
            install(ContentNegotiation) {
                json(json)
            }
            install(Logging) {
                level = LogLevel.BODY
            }
            defaultRequest {
                url(baseUrl)
                header("Content-Type", "application/json")
                header("version", BuildConfig.VERSION)
            }
        }
    }
    
    fun setApiKey(apiKey: String) {
        deviceClient = deviceClient?.config {
            defaultRequest {
                header("x-api-key", apiKey)
            }
        }
    }
    
    fun setBearerToken(token: String) {
        authenticatedClient = HttpClient {
            install(ContentNegotiation) {
                json(json)
            }
            defaultRequest {
                url(baseUrl)
                header("Authorization", "Bearer $token")
                header("Content-Type", "application/json")
            }
        }
    }
}
```

---

## Authentication

### Bearer Token Authentication

Used for employee-authenticated requests:

```kotlin
suspend fun login(pin: String, branchId: Int): TokenViewModel {
    val response = deviceClient.post("/employee/login") {
        setBody(LoginRequest(pin = pin, branch = branchId))
    }
    
    val token = response.body<TokenViewModel>()
    
    // Store token and configure authenticated client
    ApiManager.setBearerToken(token.accessToken)
    tokenManager.saveTokens(token)
    
    return token
}
```

### API Key Authentication

Used for device-level requests:

```kotlin
suspend fun registerDevice(deviceInfo: DeviceInfo): DeviceRegistrationResponse {
    // Uses device client with API key
    return deviceClient.post("/device/register") {
        setBody(deviceInfo)
    }.body()
}
```

### Token Refresh

```kotlin
suspend fun <T> authenticatedCall(block: suspend () -> T): T {
    return try {
        block()
    } catch (e: ClientRequestException) {
        if (e.response.status == HttpStatusCode.Unauthorized) {
            // Refresh token
            val refreshToken = tokenManager.getRefreshToken()
            val newToken = deviceClient.post("/employee/refresh") {
                setBody(RefreshTokenRequest(token = refreshToken))
            }.body<TokenViewModel>()
            
            tokenManager.saveTokens(newToken)
            ApiManager.setBearerToken(newToken.accessToken)
            
            // Retry original call
            block()
        } else {
            throw e
        }
    }
}
```

---

## Generated API Classes

### Product API

```kotlin
class ProductApi(private val client: HttpClient) {
    
    suspend fun getProduct(productId: Int): ProductViewModel {
        return client.get("/product/$productId").body()
    }
    
    suspend fun getProducts(offset: String?, limit: Int): List<ProductViewModel> {
        return client.get("/product") {
            parameter("offset", offset)
            parameter("limit", limit)
        }.body()
    }
    
    suspend fun getProductAtTime(
        productId: Int, 
        dateTime: OffsetDateTime
    ): ProductViewModel {
        return client.get("/product/$productId/temporal") {
            parameter("dateTime", dateTime.toString())
        }.body()
    }
}
```

### Employee API

```kotlin
class EmployeeApi(private val client: HttpClient) {
    
    suspend fun login(request: LoginRequest): TokenViewModel {
        return client.post("/employee/login") {
            setBody(request)
        }.body()
    }
    
    suspend fun getProfile(): UserProfileViewModel {
        return client.get("/employee/profile").body()
    }
    
    suspend fun lockDevice(request: DeviceLockRequest) {
        client.post("/employee/lock") {
            setBody(request)
        }
    }
    
    suspend fun refreshToken(request: RefreshTokenRequest): TokenViewModel {
        return client.post("/employee/refresh") {
            setBody(request)
        }.body()
    }
}
```

### Transaction API

```kotlin
class TransactionApi(private val client: HttpClient) {
    
    suspend fun createTransaction(
        request: AddEditTransactionRequest
    ): TransactionViewModel {
        return client.post("/transaction") {
            setBody(request)
        }.body()
    }
    
    suspend fun searchTransactions(
        params: TransactionSearchParams
    ): List<TransactionViewModel> {
        return client.get("/transaction") {
            parameter("startDate", params.startDate)
            parameter("endDate", params.endDate)
            parameter("employeeId", params.employeeId)
        }.body()
    }
    
    suspend fun holdTransaction(request: CreateTransactionHoldDto): CreateTransactionHoldResponse {
        return client.post("/transaction/hold") {
            setBody(request)
        }.body()
    }
    
    suspend fun getHeldTransactions(): List<TransactionHoldViewModel> {
        return client.get("/transaction/recall").body()
    }
}
```

---

## API Endpoints by Category

### Product Management

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/product/{id}` | GET | Get product by ID |
| `/product` | GET | List products |
| `/product/{id}/temporal` | GET | Get product at time |
| `/product/image/{id}` | GET | Get product image |
| `/product/tax/{id}` | GET | Get product taxes |

### Employee Management

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/employee/login` | POST | Employee login |
| `/employee/profile` | GET | Get current user profile |
| `/employee/lock` | POST | Lock device |
| `/employee/refresh` | POST | Refresh token |

### Transactions

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/transaction` | POST | Create transaction |
| `/transaction` | GET | Search transactions |
| `/transaction/hold` | POST | Hold transaction |
| `/transaction/recall` | GET | Get held transactions |

### Cash Management

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/cash/pickup` | POST | Cash pickup |
| `/cash/payout` | POST | Vendor payout |
| `/cash/available` | GET | Get available cash |

### Lottery

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/lottery/sale` | POST | Create lottery sale |
| `/lottery/payout` | POST | Create lottery payout |
| `/lottery/games` | GET | Get available games |
| `/lottery/report/daily` | GET | Daily lottery report |

### Device Management

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/device/heartbeat` | GET | Device heartbeat |
| `/device/updates` | GET | Get pending updates |
| `/device/update/success` | POST | Report update success |
| `/device/update/failure` | POST | Report update failure |

---

## Error Handling

### Result Wrapper

```kotlin
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val code: Int, val message: String) : ApiResult<Nothing>()
    object NetworkError : ApiResult<Nothing>()
}

suspend fun <T> safeApiCall(call: suspend () -> T): ApiResult<T> {
    return try {
        ApiResult.Success(call())
    } catch (e: ClientRequestException) {
        when (e.response.status.value) {
            400 -> ApiResult.Error(400, "Validation error")
            401 -> ApiResult.Error(401, "Unauthorized")
            403 -> ApiResult.Error(403, "Forbidden")
            404 -> ApiResult.Error(404, "Not found")
            410 -> ApiResult.Error(410, "Entity deleted")
            else -> ApiResult.Error(e.response.status.value, e.message)
        }
    } catch (e: Exception) {
        ApiResult.NetworkError
    }
}
```

### Usage Example

```kotlin
class ProductRepository(private val productApi: ProductApi) {
    
    suspend fun getProduct(id: Int): ProductViewModel? {
        return when (val result = safeApiCall { productApi.getProduct(id) }) {
            is ApiResult.Success -> result.data
            is ApiResult.Error -> {
                logger.error("API error ${result.code}: ${result.message}")
                null
            }
            ApiResult.NetworkError -> {
                logger.error("Network error")
                null
            }
        }
    }
}
```

---

## Configuration

### Environment URLs

```kotlin
// BuildConfig.kt (generated)
object BuildConfig {
    const val VERSION = "2.0.0"
    
    val BASE_URL = when (Environment.current) {
        Environment.DEV -> "https://apim-service-unisight-dev.azure-api.net"
        Environment.STAGING -> "https://apim-service-unisight-staging.azure-api.net"
        Environment.PROD -> "https://apim-service-unisight-prod.azure-api.net"
    }
}

enum class Environment {
    DEV, STAGING, PROD;
    
    companion object {
        val current: Environment = DEV // Set at build time
    }
}
```

---

## Best Practices

### Do's

1. **Use Repository Pattern**
   - Wrap API calls in repositories
   - Handle caching and offline

2. **Handle All Error Codes**
   - Implement proper error handling for each case

3. **Use Coroutines**
   - All API calls should be suspend functions
   - Use appropriate dispatchers

4. **Retry with Backoff**
   - For transient failures

### Don'ts

1. **Don't Create New Client Instances**
   - Use the configured instances from ApiManager

2. **Don't Ignore 401 Errors**
   - Always attempt token refresh

3. **Don't Block Main Thread**
   - Run API calls on IO dispatcher

---

## Related Documentation

- [Data Flow](./DATA_FLOW.md)
- [State Management](./STATE_MANAGEMENT.md)
- [Sync Mechanism](../data/SYNC_MECHANISM.md)

---

*Last Updated: January 2026*

