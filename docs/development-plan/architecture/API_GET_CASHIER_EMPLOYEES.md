# API: Get Cashier Employees

**Version:** 2.0 (Kotlin/Compose)  
**Platform:** Windows, Linux, Android  
**Last Updated:** January 2026

---

## Overview

This document details the endpoint used to fetch the list of cashiers displayed on the initial sign-in screen in the GroPOS application.

When the POS device is registered and the login screen is displayed, this endpoint is called to retrieve the list of scheduled cashiers for the current branch/location. The cashiers are displayed in a list for the user to select before entering their PIN.

---

## Technology Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin |
| UI Framework | Jetpack Compose Multiplatform |
| HTTP Client | Ktor Client |
| Local Database | Couchbase Lite |
| State Management | StateFlow |
| Serialization | Kotlinx.serialization |
| DI | Koin |

### Platform Support

| Platform | UI Framework | Database |
|----------|--------------|----------|
| **Android** | Jetpack Compose | Couchbase Lite Android |
| **Windows** | Compose for Desktop | Couchbase Lite Java |
| **Linux** | Compose for Desktop | Couchbase Lite Java |

---

## Endpoint Details

### Request

| Property | Value |
|----------|-------|
| **Method** | `GET` |
| **Path** | `/api/Employee/GetCashierEmployees` |
| **Operation ID** | `Employee_GetCashierEmployees` |

### Full URLs by Environment

> **IMPORTANT:** All requests go through the APIM Gateway. The App Service URLs are for internal backend reference only and should NOT be called directly from the client.

| Environment | APIM Gateway (Client Uses) | Full Endpoint |
|-------------|---------------------------|---------------|
| **Development** | `https://apim-service-unisight-dev.azure-api.net` | `https://apim-service-unisight-dev.azure-api.net/api/Employee/GetCashierEmployees` |
| **Staging** | `https://apim-service-unisight-staging.azure-api.net` | `https://apim-service-unisight-staging.azure-api.net/api/Employee/GetCashierEmployees` |
| **Production** | `https://apim-service-unisight-prod.azure-api.net` | `https://apim-service-unisight-prod.azure-api.net/api/Employee/GetCashierEmployees` |

### Backend App Service URLs (Internal Reference Only)

| Environment | App Service (Backend) |
|-------------|-----------------------|
| **Development** | `https://app-pos-api-dev-001.azurewebsites.net` |
| **Staging** | `https://app-pos-api-staging-001.azurewebsites.net` |
| **Production** | `https://app-pos-api-prod-001.azurewebsites.net` |

---

## Headers

| Header | Value | Required | Description |
|--------|-------|----------|-------------|
| `x-api-key` | `{deviceApiKey}` | **Yes** | The API key obtained during device registration. Stored locally in Couchbase Lite as `PosSystemDocument.apiKey` |
| `version` | `v1` | **Yes** | API version identifier. Must be exactly `v1`. |
| `Content-Type` | `application/json` | No | Standard JSON content type |

### Header Configuration (Ktor)

Headers are configured in the `ApiClient` singleton using Ktor's request interceptor:

```kotlin
// ApiClient.kt
object ApiClient {
    private var apiKey: String = ""
    
    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        
        install(DefaultRequest) {
            header("x-api-key", apiKey)
            header("version", "v1")
            contentType(ContentType.Application.Json)
        }
        
        install(Logging) {
            level = LogLevel.INFO
        }
    }
    
    fun updateApiKey(key: String) {
        apiKey = key
    }
}
```

---

## Request Body

**None** - This is a GET request with no request body or query parameters.

---

## Response

### Success Response

| Property | Value |
|----------|-------|
| **HTTP Status** | `200 OK` |
| **Content-Type** | `application/json` |
| **Body** | `List<EmployeeListResponse>` |

### Example Response

```json
[
  {
    "id": 123,
    "imageUrl": "https://storage.example.com/employees/123.jpg",
    "name": "John Doe",
    "mobilePhone": "555-123-4567",
    "email": "john.doe@company.com",
    "jobTitle": "Cashier",
    "department": "Front End",
    "supervisorName": "Jane Smith",
    "firstName": "John",
    "lastName": "Doe",
    "supFirstName": "Jane",
    "supLastName": "Smith",
    "multiBranch": false
  },
  {
    "id": 456,
    "imageUrl": null,
    "name": "Mary Johnson",
    "mobilePhone": null,
    "email": "mary.johnson@company.com",
    "jobTitle": "Lead Cashier",
    "department": "Front End",
    "supervisorName": "Jane Smith",
    "firstName": "Mary",
    "lastName": "Johnson",
    "supFirstName": "Jane",
    "supLastName": "Smith",
    "multiBranch": true
  }
]
```

---

## Data Models (Kotlin)

### EmployeeListResponse

```kotlin
// data/model/EmployeeListResponse.kt
@Serializable
data class EmployeeListResponse(
    val id: Int,
    val imageUrl: String? = null,
    val name: String? = null,
    val mobilePhone: String? = null,
    val email: String? = null,
    val jobTitle: String? = null,
    val department: String? = null,
    val supervisorName: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val supFirstName: String? = null,
    val supLastName: String? = null,
    val multiBranch: Boolean = false
)
```

### Domain Model (for UI)

```kotlin
// domain/model/Cashier.kt
data class Cashier(
    val id: Int,
    val name: String,
    val email: String,
    val imageUrl: String?,
    val jobTitle: String,
    val department: String,
    val multiBranch: Boolean,
    var assignedTillId: Int? = null
)
```

### Mapper Extension

```kotlin
// data/mapper/EmployeeMapper.kt
fun EmployeeListResponse.toCashier(): Cashier = Cashier(
    id = id,
    name = name ?: "${firstName.orEmpty()} ${lastName.orEmpty()}".trim(),
    email = email.orEmpty(),
    imageUrl = imageUrl,
    jobTitle = jobTitle.orEmpty(),
    department = department.orEmpty(),
    multiBranch = multiBranch
)

fun List<EmployeeListResponse>.toCashierList(): List<Cashier> = map { it.toCashier() }
```

### Schema Reference

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| `id` | `Int` | No | Unique employee ID |
| `imageUrl` | `String?` | Yes | URL to employee's avatar/profile image |
| `name` | `String?` | Yes | Full display name (typically "FirstName LastName") |
| `mobilePhone` | `String?` | Yes | Employee's mobile phone number |
| `email` | `String?` | Yes | Employee's email address (**used as `userName` for login**) |
| `jobTitle` | `String?` | Yes | Job title/position (e.g., "Cashier", "Lead Cashier") |
| `department` | `String?` | Yes | Department name (e.g., "Front End") |
| `supervisorName` | `String?` | Yes | Supervisor's full name |
| `firstName` | `String?` | Yes | Employee's first name |
| `lastName` | `String?` | Yes | Employee's last name |
| `supFirstName` | `String?` | Yes | Supervisor's first name |
| `supLastName` | `String?` | Yes | Supervisor's last name |
| `multiBranch` | `Boolean` | No | Whether this employee has access to multiple branches |

---

## Code Implementation

### Repository Layer

```kotlin
// data/repository/EmployeeRepository.kt
interface EmployeeRepository {
    suspend fun getCashierEmployees(): Result<List<Cashier>>
}

class EmployeeRepositoryImpl(
    private val apiClient: HttpClient,
    private val baseUrl: String
) : EmployeeRepository {
    
    override suspend fun getCashierEmployees(): Result<List<Cashier>> {
        return try {
            val response: List<EmployeeListResponse> = apiClient.get(
                "$baseUrl/api/Employee/GetCashierEmployees"
            ).body()
            
            Result.success(response.toCashierList())
        } catch (e: ResponseException) {
            Result.failure(ApiException(e.response.status.value, e.message))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

### Use Case Layer

```kotlin
// domain/usecase/GetCashierEmployeesUseCase.kt
class GetCashierEmployeesUseCase(
    private val employeeRepository: EmployeeRepository
) {
    suspend operator fun invoke(): Result<List<Cashier>> {
        return employeeRepository.getCashierEmployees()
    }
}
```

### ViewModel Integration

```kotlin
// ui/login/LoginViewModel.kt
class LoginViewModel(
    private val getCashierEmployeesUseCase: GetCashierEmployeesUseCase,
    private val appState: AppState
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()
    
    fun loadCashierEmployees() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            getCashierEmployeesUseCase()
                .onSuccess { cashiers ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            cashiers = cashiers
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            error = error.message
                        )
                    }
                }
        }
    }
}

data class LoginUiState(
    val isLoading: Boolean = false,
    val cashiers: List<Cashier> = emptyList(),
    val selectedCashier: Cashier? = null,
    val error: String? = null
)
```

### Compose UI

```kotlin
// ui/login/LoginScreen.kt
@Composable
fun LoginScreen(
    viewModel: LoginViewModel = koinViewModel(),
    onNavigateToHome: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadCashierEmployees()
    }
    
    when {
        uiState.isLoading -> {
            LoadingIndicator()
        }
        uiState.error != null -> {
            ErrorMessage(
                message = uiState.error!!,
                onRetry = { viewModel.loadCashierEmployees() }
            )
        }
        else -> {
            CashierListContent(
                cashiers = uiState.cashiers,
                selectedCashier = uiState.selectedCashier,
                onCashierSelected = { cashier ->
                    viewModel.selectCashier(cashier)
                }
            )
        }
    }
}

@Composable
private fun CashierListContent(
    cashiers: List<Cashier>,
    selectedCashier: Cashier?,
    onCashierSelected: (Cashier) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(cashiers) { cashier ->
            CashierListItem(
                cashier = cashier,
                isSelected = cashier == selectedCashier,
                onClick = { onCashierSelected(cashier) }
            )
        }
    }
}
```

### Dependency Injection (Koin)

```kotlin
// di/EmployeeModule.kt
val employeeModule = module {
    single<EmployeeRepository> { 
        EmployeeRepositoryImpl(
            apiClient = get(),
            baseUrl = get<EnvironmentConfig>().baseUrl
        )
    }
    
    factory { GetCashierEmployeesUseCase(get()) }
    
    viewModel { LoginViewModel(get(), get()) }
}
```

---

## Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Device Registration Complete                      │
│                    (API Key stored in Couchbase Lite)               │
└─────────────────────────────┬───────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│              Navigate to LoginScreen                                 │
│                                                                      │
│  1. ApiClient.updateApiKey(apiKey)                                  │
│     → Configures x-api-key and version headers                      │
│                                                                      │
│  2. LoginViewModel initialized via Koin                             │
│     → Dependency injection provides repository                      │
└─────────────────────────────┬───────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│              LaunchedEffect triggers loadCashierEmployees()          │
│                                                                      │
│  GET /api/Employee/GetCashierEmployees                              │
│  Headers:                                                            │
│    x-api-key: {deviceApiKey}                                        │
│    version: v1                                                      │
└─────────────────────────────┬───────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│              Response: List<EmployeeListResponse>                    │
│                                                                      │
│  1. Map to List<Cashier> domain model                               │
│  2. Update LoginUiState via StateFlow                               │
│  3. Compose recomposes CashierListContent                           │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Error Handling

| HTTP Status | Meaning | App Behavior |
|-------------|---------|--------------|
| `200` | Success | Display cashier list via StateFlow update |
| `401` | Unauthorized (invalid/expired API key) | Trigger token refresh or navigate to registration |
| `500` | Server Error | Show error message with retry option |

### Error Handling Implementation

```kotlin
// util/ApiException.kt
class ApiException(
    val code: Int,
    override val message: String?
) : Exception(message)

// Repository error handling
override suspend fun getCashierEmployees(): Result<List<Cashier>> {
    return try {
        val response: List<EmployeeListResponse> = apiClient.get(
            "$baseUrl/api/Employee/GetCashierEmployees"
        ).body()
        Result.success(response.toCashierList())
    } catch (e: ClientRequestException) {
        when (e.response.status) {
            HttpStatusCode.Unauthorized -> {
                Result.failure(ApiException(401, "Session expired. Please re-register device."))
            }
            else -> Result.failure(ApiException(e.response.status.value, e.message))
        }
    } catch (e: ServerResponseException) {
        Result.failure(ApiException(e.response.status.value, "Server error. Please try again."))
    } catch (e: Exception) {
        Result.failure(ApiException(-1, e.message ?: "Unknown error occurred"))
    }
}
```

---

## File Structure

```
app/
├── src/
│   ├── commonMain/kotlin/com/unisight/gropos/
│   │   ├── data/
│   │   │   ├── api/
│   │   │   │   └── ApiClient.kt
│   │   │   ├── model/
│   │   │   │   └── EmployeeListResponse.kt
│   │   │   ├── mapper/
│   │   │   │   └── EmployeeMapper.kt
│   │   │   └── repository/
│   │   │       └── EmployeeRepository.kt
│   │   ├── domain/
│   │   │   ├── model/
│   │   │   │   └── Cashier.kt
│   │   │   └── usecase/
│   │   │       └── GetCashierEmployeesUseCase.kt
│   │   ├── di/
│   │   │   └── EmployeeModule.kt
│   │   └── ui/
│   │       └── login/
│   │           ├── LoginScreen.kt
│   │           ├── LoginViewModel.kt
│   │           ├── LoginUiState.kt
│   │           └── components/
│   │               └── CashierListItem.kt
│   ├── androidMain/kotlin/              # Android-specific implementations
│   └── desktopMain/kotlin/              # Windows/Linux-specific implementations
```

---

## Related Endpoints

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/v1/devices/current` | GET | Get current device info (called after cashier list) |
| `/api/Employee/Login` | POST | Authenticate selected cashier with PIN |
| `/api/Employee/GetProfile` | GET | Get logged-in user's full profile |
| `/api/account/GetTillAccountList` | GET | Get available tills for assignment |

---

## Platform-Specific Notes

### Android

```kotlin
// androidMain/kotlin/.../ApiClient.android.kt
actual fun createHttpClient(): HttpClient = HttpClient(Android) {
    install(ContentNegotiation) {
        json()
    }
}
```

### Desktop (Windows/Linux)

```kotlin
// desktopMain/kotlin/.../ApiClient.desktop.kt
actual fun createHttpClient(): HttpClient = HttpClient(CIO) {
    install(ContentNegotiation) {
        json()
    }
}
```

---

## Migration Notes

### Changes from Java/JavaFX Implementation

| Aspect | Previous (Java/JavaFX) | Current (Kotlin/Compose) |
|--------|------------------------|--------------------------|
| Language | Java | Kotlin |
| UI Framework | JavaFX + FXML | Jetpack Compose Multiplatform |
| HTTP Client | Generated OpenAPI Client | Ktor Client |
| Async | `Platform.runLater()` | Coroutines + StateFlow |
| DI | Manual instantiation | Koin |
| State Management | JavaFX Properties | StateFlow |
| Navigation | Custom Router | Compose Navigation |

### Key Migration Points

1. **API Call**: `employeeApi.employeeGetCashierEmployees()` → `employeeRepository.getCashierEmployees()`
2. **Threading**: `Platform.runLater()` → Automatic with Compose + StateFlow
3. **Error Handling**: Try-catch with `AppException` → `Result<T>` pattern
4. **UI Updates**: JavaFX `ObservableList` → Compose `LazyColumn` with `collectAsState()`

---

## Related Documentation

- [Lock Screen and Cashier Login (Kotlin)](./LOCK_SCREEN_AND_CASHIER_LOGIN_KOTLIN.md)
- [Device Registration (Kotlin)](./DEVICE_REGISTRATION.md)
- [Architecture Blueprint](../../plan/ARCHITECTURE_BLUEPRINT.md)
- [API Integration](../../architecture/API_INTEGRATION.md)

