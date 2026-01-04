# Till Management

**Version:** 2.0 (Kotlin/Compose)  
**Platform:** Windows, Linux, Android  
**Last Updated:** January 2026

---

## Table of Contents

1. [Overview](#overview)
2. [Technology Stack](#technology-stack)
3. [API Endpoints](#api-endpoints)
4. [Data Models](#data-models)
5. [Till Assignment Flow](#till-assignment-flow)
6. [Till Selection Modes](#till-selection-modes)
7. [Code Implementation](#code-implementation)
8. [Compose UI Components](#compose-ui-components)
9. [Local Caching with Couchbase](#local-caching-with-couchbase)
10. [Error Handling](#error-handling)
11. [Platform-Specific Notes](#platform-specific-notes)
12. [File Structure](#file-structure)
13. [Migration Notes](#migration-notes)

---

## Overview

A **Till** (also called LocationAccount) represents a cash drawer/register account that employees are assigned to for tracking cash accountability. Before a cashier can complete login, they must be assigned to an available till.

### What is a Till?

- Physical cash drawer associated with a register
- Tracks cash accountability per employee
- Required for cashier login
- Can be assigned/unassigned based on employee shifts

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

## API Endpoints

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/account/GetTillAccountList` | GET | Get all available tills for the location |
| `/api/v1/account/assignedTills` | GET | Get tills already assigned (same response schema) |
| `/api/account/GetAccountBalance` | GET | Get current till balance |

### Full URLs by Environment

| Environment | Base URL | Endpoint |
|-------------|----------|----------|
| **Development** | `https://app-pos-api-dev-001.azurewebsites.net` | `/api/account/GetTillAccountList` |
| **Staging** | `https://app-pos-api-staging-001.azurewebsites.net` | `/api/account/GetTillAccountList` |
| **Production** | `https://app-pos-api-prod-001.azurewebsites.net` | `/api/account/GetTillAccountList` |

### Headers

| Header | Value | Required | Description |
|--------|-------|----------|-------------|
| `x-api-key` | `{deviceApiKey}` | **Yes** | API key obtained during device registration |
| `version` | `{appVersion}` | **Yes** | Application version string |
| `Content-Type` | `application/json` | No | Standard JSON content type |

---

## Data Models

### API Response Models

#### GridDataOfLocationAccountListResponse

```kotlin
// data/model/TillListResponse.kt
@Serializable
data class GridDataOfLocationAccountListResponse(
    val totalRows: Int = 0,
    val rows: List<LocationAccountListResponse>? = null
)
```

#### LocationAccountListResponse

```kotlin
// data/model/LocationAccountListResponse.kt
@Serializable
data class LocationAccountListResponse(
    val locationAccountId: Int,
    val accountName: String? = null,
    val assignedEmployeeId: Int? = null,
    val employeeName: String? = null,
    val currentBalance: Double = 0.0
)
```

### Sample JSON Response

```json
{
  "totalRows": 5,
  "rows": [
    {
      "locationAccountId": 123,
      "accountName": "Register 1 Till",
      "assignedEmployeeId": null,
      "employeeName": null,
      "currentBalance": 500.00
    },
    {
      "locationAccountId": 124,
      "accountName": "Drawer A",
      "assignedEmployeeId": 456,
      "employeeName": "John Smith",
      "currentBalance": 350.75
    },
    {
      "locationAccountId": 125,
      "accountName": "Till 3",
      "assignedEmployeeId": null,
      "employeeName": null,
      "currentBalance": 200.00
    }
  ]
}
```

### Schema Definition

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| `locationAccountId` | Int | No | Unique Till ID (used for login & assignment) |
| `accountName` | String | Yes | Display name of the till (e.g., "Till 1", "Drawer A") |
| `assignedEmployeeId` | Int | Yes | Currently assigned employee ID (`null` = available) |
| `employeeName` | String | Yes | Name of assigned employee |
| `currentBalance` | Double | No | Current cash balance in the till |

### Domain Model

```kotlin
// domain/model/Till.kt
data class Till(
    val id: Int,
    val name: String,
    val assignedEmployeeId: Int? = null,
    val assignedEmployeeName: String? = null,
    val currentBalance: Double = 0.0
) {
    val isAvailable: Boolean
        get() = assignedEmployeeId == null
}
```

### Mapper Extension

```kotlin
// data/mapper/TillMapper.kt
fun LocationAccountListResponse.toTill(): Till = Till(
    id = locationAccountId,
    name = accountName ?: "Till $locationAccountId",
    assignedEmployeeId = assignedEmployeeId,
    assignedEmployeeName = employeeName,
    currentBalance = currentBalance
)

fun List<LocationAccountListResponse>.toTillList(): List<Till> = map { it.toTill() }

fun GridDataOfLocationAccountListResponse.toTillList(): List<Till> = 
    rows?.toTillList() ?: emptyList()
```

---

## Till Assignment Flow

### When Till Assignment is Required

Till assignment occurs when:
1. Employee has no `assignedTillId` (null or ≤ 0)
2. Employee's previous till was released
3. New employee logging in for the first time

### Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                     TILL ASSIGNMENT FLOW                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   Employee enters PIN                                           │
│         │                                                       │
│         ▼                                                       │
│   ┌─────────────────────────────────────────────┐              │
│   │ Check: assignedTillId != null && > 0 ?     │              │
│   └─────────────────────────────────────────────┘              │
│         │                           │                           │
│         │ YES                       │ NO                        │
│         ▼                           ▼                           │
│   ┌─────────────┐           ┌─────────────────────┐            │
│   │ Proceed to  │           │ Show Till Selection │            │
│   │   Login     │           │      Dialog         │            │
│   └─────────────┘           └─────────────────────┘            │
│                                     │                           │
│                                     ▼                           │
│                        ┌──────────────────────┐                │
│                        │ Platform Mode?       │                │
│                        └──────────────────────┘                │
│                              │           │                      │
│                       Desktop│           │ Mobile               │
│                              ▼           ▼                      │
│                        ┌──────────┐  ┌────────────────┐        │
│                        │ ListView │  │ Scanner Mode   │        │
│                        │ Selection│  │ (Scan Barcode) │        │
│                        └──────────┘  └────────────────┘        │
│                              │           │                      │
│                              └─────┬─────┘                      │
│                                    ▼                            │
│                        ┌──────────────────────┐                │
│                        │ Validate Till ID     │                │
│                        │ Against Available    │                │
│                        │      Tills           │                │
│                        └──────────────────────┘                │
│                                    │                            │
│                                    ▼                            │
│                        ┌──────────────────────┐                │
│                        │ Complete Login with  │                │
│                        │      Till ID         │                │
│                        └──────────────────────┘                │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## Till Selection Modes

The application supports two till selection modes based on platform and hardware configuration:

### 1. Touch/List Mode (Desktop & Tablet)

**Condition**: `PlatformConfig.useTouchSelection == true`

**Behavior**:
- Displays a Compose `LazyColumn` of available tills
- User taps to select a till
- Available tills highlighted, assigned tills shown as disabled

### 2. Scanner Mode (Mobile/Handheld)

**Condition**: `PlatformConfig.useTouchSelection == false`

**Behavior**:
- Opens the cash drawer automatically (via hardware service)
- Waits for barcode scan of till label
- Validates scanned ID against available tills

---

## Code Implementation

### Repository Layer

```kotlin
// data/repository/TillRepository.kt
interface TillRepository {
    suspend fun getTillAccountList(): Result<List<Till>>
    suspend fun getAssignedTills(): Result<List<Till>>
    suspend fun getAccountBalance(): Result<Double>
}

class TillRepositoryImpl(
    private val apiClient: HttpClient,
    private val baseUrl: String
) : TillRepository {
    
    override suspend fun getTillAccountList(): Result<List<Till>> {
        return try {
            val response: GridDataOfLocationAccountListResponse = apiClient.get(
                "$baseUrl/api/account/GetTillAccountList"
            ).body()
            
            Result.success(response.toTillList())
        } catch (e: ResponseException) {
            Result.failure(ApiException(e.response.status.value, e.message))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getAssignedTills(): Result<List<Till>> {
        return try {
            val response: GridDataOfLocationAccountListResponse = apiClient.get(
                "$baseUrl/api/v1/account/assignedTills"
            ).body()
            
            Result.success(response.toTillList())
        } catch (e: ResponseException) {
            Result.failure(ApiException(e.response.status.value, e.message))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getAccountBalance(): Result<Double> {
        return try {
            val balance: Double = apiClient.get(
                "$baseUrl/api/account/GetAccountBalance"
            ).body()
            
            Result.success(balance)
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
// domain/usecase/GetAvailableTillsUseCase.kt
class GetAvailableTillsUseCase(
    private val tillRepository: TillRepository
) {
    suspend operator fun invoke(): Result<List<Till>> {
        return tillRepository.getTillAccountList()
    }
}

// domain/usecase/ValidateTillSelectionUseCase.kt
class ValidateTillSelectionUseCase(
    private val tillRepository: TillRepository
) {
    suspend operator fun invoke(tillId: Int): Result<Till> {
        return tillRepository.getTillAccountList()
            .mapCatching { tills ->
                val till = tills.find { it.id == tillId }
                    ?: throw TillNotFoundException("Till $tillId not found")
                
                if (!till.isAvailable) {
                    throw TillNotAvailableException("Till ${till.name} is already assigned")
                }
                
                till
            }
    }
}

// domain/exception/TillExceptions.kt
class TillNotFoundException(message: String) : Exception(message)
class TillNotAvailableException(message: String) : Exception(message)
```

### ViewModel

```kotlin
// ui/till/TillSelectionViewModel.kt
class TillSelectionViewModel(
    private val getAvailableTillsUseCase: GetAvailableTillsUseCase,
    private val validateTillSelectionUseCase: ValidateTillSelectionUseCase,
    private val hardwareService: HardwareService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TillSelectionUiState())
    val uiState: StateFlow<TillSelectionUiState> = _uiState.asStateFlow()
    
    private val _events = MutableSharedFlow<TillSelectionEvent>()
    val events: SharedFlow<TillSelectionEvent> = _events.asSharedFlow()
    
    init {
        loadTills()
    }
    
    fun loadTills() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            getAvailableTillsUseCase()
                .onSuccess { tills ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            tills = tills,
                            availableTills = tills.filter { till -> till.isAvailable }
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
    
    fun selectTill(tillId: Int) {
        viewModelScope.launch {
            val till = _uiState.value.tills.find { it.id == tillId }
            
            if (till?.isAvailable == true) {
                _uiState.update { it.copy(selectedTillId = tillId) }
                _events.emit(TillSelectionEvent.TillSelected(tillId))
            } else {
                _uiState.update { 
                    it.copy(error = "This till is already assigned to another employee")
                }
            }
        }
    }
    
    fun onTillScanned(barcode: String) {
        viewModelScope.launch {
            try {
                val tillId = barcode.toInt()
                
                validateTillSelectionUseCase(tillId)
                    .onSuccess { till ->
                        _uiState.update { it.copy(selectedTillId = till.id) }
                        _events.emit(TillSelectionEvent.TillSelected(till.id))
                    }
                    .onFailure { error ->
                        _uiState.update { it.copy(error = error.message) }
                    }
            } catch (e: NumberFormatException) {
                _uiState.update { it.copy(error = "Invalid till barcode format") }
            }
        }
    }
    
    fun openCashDrawer() {
        viewModelScope.launch {
            hardwareService.openCashDrawer()
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class TillSelectionUiState(
    val isLoading: Boolean = false,
    val tills: List<Till> = emptyList(),
    val availableTills: List<Till> = emptyList(),
    val selectedTillId: Int? = null,
    val error: String? = null
)

sealed class TillSelectionEvent {
    data class TillSelected(val tillId: Int) : TillSelectionEvent()
    object Cancelled : TillSelectionEvent()
}
```

### Login ViewModel Integration

```kotlin
// ui/login/LoginViewModel.kt
class LoginViewModel(
    private val authRepository: AuthRepository,
    private val getAvailableTillsUseCase: GetAvailableTillsUseCase,
    private val appState: AppState
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()
    
    fun submitPin(pin: String) {
        viewModelScope.launch {
            val cashier = _uiState.value.selectedCashier ?: return@launch
            
            // Check if till assignment is needed
            val assignedTillId = cashier.assignedTillId
            
            if (assignedTillId != null && assignedTillId > 0) {
                // Already has till - proceed to login
                performLogin(cashier.email, pin, assignedTillId)
            } else {
                // Need till assignment - show dialog
                _uiState.update { 
                    it.copy(
                        showTillSelection = true,
                        pendingPin = pin
                    )
                }
            }
        }
    }
    
    fun onTillSelected(tillId: Int) {
        viewModelScope.launch {
            val cashier = _uiState.value.selectedCashier ?: return@launch
            val pin = _uiState.value.pendingPin ?: return@launch
            
            _uiState.update { 
                it.copy(
                    showTillSelection = false,
                    pendingPin = null
                )
            }
            
            // Update cashier with selected till
            cashier.assignedTillId = tillId
            
            // Proceed with login
            performLogin(cashier.email, pin, tillId)
        }
    }
    
    private suspend fun performLogin(email: String, pin: String, tillId: Int) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        
        authRepository.login(
            username = email,
            password = pin,
            tillId = tillId,
            branchId = appState.branch.value?.id ?: 0,
            stationId = appState.deviceInfo.value?.stationId ?: 0
        )
            .onSuccess { userProfile ->
                appState.setEmployee(userProfile)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        loginSuccess = true
                    )
                }
            }
            .onFailure { error ->
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = error.message,
                        pin = "" // Clear PIN on error
                    )
                }
            }
    }
}
```

---

## Compose UI Components

### Till Selection Dialog

```kotlin
// ui/till/TillSelectionDialog.kt
@Composable
fun TillSelectionDialog(
    viewModel: TillSelectionViewModel = koinViewModel(),
    onTillSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val platformConfig = LocalPlatformConfig.current
    
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TillSelectionEvent.TillSelected -> onTillSelected(event.tillId)
                is TillSelectionEvent.Cancelled -> onDismiss()
            }
        }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.7f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Header
                Text(
                    text = "Select Till",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = if (platformConfig.useTouchSelection) {
                        "Tap to select an available till"
                    } else {
                        "Scan the till barcode to assign"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Error message
                uiState.error?.let { error ->
                    ErrorBanner(
                        message = error,
                        onDismiss = { viewModel.clearError() }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // Content based on mode
                when {
                    uiState.isLoading -> {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    platformConfig.useTouchSelection -> {
                        TillListContent(
                            tills = uiState.tills,
                            selectedTillId = uiState.selectedTillId,
                            onTillClick = { viewModel.selectTill(it.id) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    else -> {
                        TillScanContent(
                            onBarcodeScanned = { viewModel.onTillScanned(it) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Cancel button
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}
```

### Till List Content (Touch Mode)

```kotlin
// ui/till/components/TillListContent.kt
@Composable
fun TillListContent(
    tills: List<Till>,
    selectedTillId: Int?,
    onTillClick: (Till) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tills, key = { it.id }) { till ->
            TillListItem(
                till = till,
                isSelected = till.id == selectedTillId,
                onClick = { onTillClick(till) }
            )
        }
    }
}

@Composable
fun TillListItem(
    till: Till,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        till.isAvailable -> MaterialTheme.colorScheme.surface
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    
    val contentColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
        till.isAvailable -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = till.isAvailable) { onClick() },
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = till.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor
                )
                
                if (!till.isAvailable && till.assignedEmployeeName != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Assigned to: ${till.assignedEmployeeName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.7f)
                    )
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$${String.format("%.2f", till.currentBalance)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Status badge
                StatusBadge(
                    text = if (till.isAvailable) "Available" else "In Use",
                    color = if (till.isAvailable) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            }
        }
    }
}

@Composable
fun StatusBadge(
    text: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}
```

### Till Scan Content (Scanner Mode)

```kotlin
// ui/till/components/TillScanContent.kt
@Composable
fun TillScanContent(
    onBarcodeScanned: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scannerService = LocalScannerService.current
    
    LaunchedEffect(Unit) {
        scannerService.barcodeScans.collect { barcode ->
            onBarcodeScanned(barcode)
        }
    }
    
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.QrCodeScanner,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Scan Till Barcode",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Position the till barcode in front of the scanner",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
```

---

## Local Caching with Couchbase

### Till Document Model

```kotlin
// data/local/model/TillDocument.kt
@Serializable
data class TillDocument(
    val id: String,
    val locationAccountId: Int,
    val accountName: String,
    val assignedEmployeeId: Int? = null,
    val employeeName: String? = null,
    val currentBalance: Double = 0.0,
    val lastSyncedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val TYPE = "till"
        
        fun createId(locationAccountId: Int) = "till::$locationAccountId"
    }
}
```

### Couchbase Till Manager

```kotlin
// data/local/TillCouchbaseManager.kt
class TillCouchbaseManager(
    private val database: Database
) {
    private val collection: Collection
        get() = database.defaultCollection
    
    fun saveTills(tills: List<Till>) {
        tills.forEach { till ->
            val document = TillDocument(
                id = TillDocument.createId(till.id),
                locationAccountId = till.id,
                accountName = till.name,
                assignedEmployeeId = till.assignedEmployeeId,
                employeeName = till.assignedEmployeeName,
                currentBalance = till.currentBalance
            )
            
            val mutableDoc = MutableDocument(document.id)
            mutableDoc.setString("type", TillDocument.TYPE)
            mutableDoc.setInt("locationAccountId", document.locationAccountId)
            mutableDoc.setString("accountName", document.accountName)
            document.assignedEmployeeId?.let { 
                mutableDoc.setInt("assignedEmployeeId", it) 
            }
            document.employeeName?.let { 
                mutableDoc.setString("employeeName", it) 
            }
            mutableDoc.setDouble("currentBalance", document.currentBalance)
            mutableDoc.setLong("lastSyncedAt", document.lastSyncedAt)
            
            collection.save(mutableDoc)
        }
    }
    
    fun getCachedTills(): List<Till> {
        val query = QueryBuilder
            .select(SelectResult.all())
            .from(DataSource.collection(collection))
            .where(Expression.property("type").equalTo(Expression.string(TillDocument.TYPE)))
        
        return query.execute().allResults().mapNotNull { result ->
            val dict = result.getDictionary(collection.name) ?: return@mapNotNull null
            
            Till(
                id = dict.getInt("locationAccountId"),
                name = dict.getString("accountName") ?: "",
                assignedEmployeeId = dict.getInt("assignedEmployeeId").takeIf { it > 0 },
                assignedEmployeeName = dict.getString("employeeName"),
                currentBalance = dict.getDouble("currentBalance")
            )
        }
    }
    
    fun clearCache() {
        val query = QueryBuilder
            .select(SelectResult.expression(Meta.id))
            .from(DataSource.collection(collection))
            .where(Expression.property("type").equalTo(Expression.string(TillDocument.TYPE)))
        
        query.execute().allResults().forEach { result ->
            result.getString(0)?.let { docId ->
                collection.getDocument(docId)?.let { doc ->
                    collection.delete(doc)
                }
            }
        }
    }
}
```

### Repository with Caching

```kotlin
// data/repository/TillRepositoryImpl.kt (with caching)
class TillRepositoryImpl(
    private val apiClient: HttpClient,
    private val baseUrl: String,
    private val tillCouchbaseManager: TillCouchbaseManager,
    private val networkMonitor: NetworkMonitor
) : TillRepository {
    
    override suspend fun getTillAccountList(): Result<List<Till>> {
        return if (networkMonitor.isConnected) {
            fetchFromApiAndCache()
        } else {
            Result.success(tillCouchbaseManager.getCachedTills())
        }
    }
    
    private suspend fun fetchFromApiAndCache(): Result<List<Till>> {
        return try {
            val response: GridDataOfLocationAccountListResponse = apiClient.get(
                "$baseUrl/api/account/GetTillAccountList"
            ).body()
            
            val tills = response.toTillList()
            
            // Cache for offline use
            tillCouchbaseManager.saveTills(tills)
            
            Result.success(tills)
        } catch (e: Exception) {
            // Fall back to cache on error
            val cached = tillCouchbaseManager.getCachedTills()
            if (cached.isNotEmpty()) {
                Result.success(cached)
            } else {
                Result.failure(e)
            }
        }
    }
}
```

---

## Error Handling

### Error Types

| Error | Cause | User Message |
|-------|-------|--------------|
| `TillNotFoundException` | Scanned/entered till ID not in list | "Till not found. Please scan a valid till barcode." |
| `TillNotAvailableException` | Selected till already assigned | "This till is already assigned to another employee." |
| `ApiException(401)` | Invalid/expired API key | "Session expired. Please re-register device." |
| `ApiException(500)` | Server error | "Server error. Please try again." |
| `NetworkException` | No connectivity | Falls back to cached tills |

### Error UI Component

```kotlin
// ui/common/ErrorBanner.kt
@Composable
fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}
```

---

## Platform-Specific Notes

### Android

```kotlin
// androidMain/kotlin/.../HardwareService.android.kt
actual class HardwareService {
    private var cashDrawer: CashDrawer? = null
    
    actual suspend fun openCashDrawer() {
        withContext(Dispatchers.IO) {
            cashDrawer?.open()
        }
    }
    
    actual fun initializeCashDrawer(context: Context) {
        // Initialize Android-specific cash drawer SDK
    }
}
```

### Desktop (Windows/Linux)

```kotlin
// desktopMain/kotlin/.../HardwareService.desktop.kt
actual class HardwareService {
    private var printer: Printer? = null
    
    actual suspend fun openCashDrawer() {
        withContext(Dispatchers.IO) {
            // Open drawer via receipt printer
            printer?.openCashDrawer()
        }
    }
    
    actual fun initializePrinter(config: PrinterConfig) {
        // Initialize JPOS printer with cash drawer
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
│   │   │   │   ├── GridDataOfLocationAccountListResponse.kt
│   │   │   │   └── LocationAccountListResponse.kt
│   │   │   ├── mapper/
│   │   │   │   └── TillMapper.kt
│   │   │   ├── local/
│   │   │   │   ├── model/
│   │   │   │   │   └── TillDocument.kt
│   │   │   │   └── TillCouchbaseManager.kt
│   │   │   └── repository/
│   │   │       └── TillRepository.kt
│   │   ├── domain/
│   │   │   ├── model/
│   │   │   │   └── Till.kt
│   │   │   ├── exception/
│   │   │   │   └── TillExceptions.kt
│   │   │   └── usecase/
│   │   │       ├── GetAvailableTillsUseCase.kt
│   │   │       └── ValidateTillSelectionUseCase.kt
│   │   ├── di/
│   │   │   └── TillModule.kt
│   │   └── ui/
│   │       ├── till/
│   │       │   ├── TillSelectionDialog.kt
│   │       │   ├── TillSelectionViewModel.kt
│   │       │   └── components/
│   │       │       ├── TillListContent.kt
│   │       │       ├── TillListItem.kt
│   │       │       └── TillScanContent.kt
│   │       └── common/
│   │           └── ErrorBanner.kt
│   ├── androidMain/kotlin/
│   │   └── com/unisight/gropos/
│   │       └── hardware/
│   │           └── HardwareService.android.kt
│   └── desktopMain/kotlin/
│       └── com/unisight/gropos/
│           └── hardware/
│               └── HardwareService.desktop.kt
```

---

## Dependency Injection (Koin)

```kotlin
// di/TillModule.kt
val tillModule = module {
    // Local storage
    single { TillCouchbaseManager(get<Database>()) }
    
    // Repository
    single<TillRepository> {
        TillRepositoryImpl(
            apiClient = get(),
            baseUrl = get<EnvironmentConfig>().baseUrl,
            tillCouchbaseManager = get(),
            networkMonitor = get()
        )
    }
    
    // Use cases
    factory { GetAvailableTillsUseCase(get()) }
    factory { ValidateTillSelectionUseCase(get()) }
    
    // ViewModel
    viewModel { 
        TillSelectionViewModel(
            getAvailableTillsUseCase = get(),
            validateTillSelectionUseCase = get(),
            hardwareService = get()
        )
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
| Local Cache | - | Couchbase Lite |
| Dialog | `AwesomeDialog` + `AccountListDialog` | Compose `Dialog` + `TillSelectionDialog` |
| List | JavaFX `ListView` | Compose `LazyColumn` |

### Key Migration Points

1. **API Call**: `accountApi.accountGetTillAccountList()` → `tillRepository.getTillAccountList()`
2. **Threading**: `Platform.runLater()` → Automatic with Compose + StateFlow
3. **Error Handling**: Try-catch with `AppException` → `Result<T>` pattern
4. **UI Updates**: JavaFX `ObservableList` → Compose `collectAsState()`
5. **Scanner**: Direct callback → `Flow<String>` from scanner service
6. **Hardware Mode Check**: `Configurator.loadNeedHardwareUse()` → `PlatformConfig.useTouchSelection`

---

## Related Documentation

- [Lock Screen and Cashier Login (Kotlin)](./LOCK_SCREEN_AND_CASHIER_LOGIN_KOTLIN.md)
- [API: Get Cashier Employees (Kotlin)](./API_GET_CASHIER_EMPLOYEES_KOTLIN.md)
- [Architecture Blueprint](../plan/ARCHITECTURE_BLUEPRINT.md)
- [Couchbase Local Storage](../../data/COUCHBASE_LOCAL_STORAGE.md)

