# GroPOS Compose Multiplatform Implementation

> Technical implementation guide for the Compose-based UI

## Technology Stack

| Technology | Purpose |
|------------|---------|
| Kotlin 2.0 | Programming Language |
| Compose Multiplatform 1.6+ | Declarative UI Framework |
| Koin | Dependency Injection |
| Ktor | HTTP Client |
| Coil | Image Loading |
| Material3 | Design System |

---

## Application Structure

```
┌─────────────────────────────────────────────────────────────┐
│                     POSApplication                           │
│  ┌───────────────────────┐  ┌───────────────────────────┐  │
│  │    Primary Window     │  │   Customer Window         │  │
│  │   (Cashier Screen)    │  │   (Customer Display)      │  │
│  │                       │  │                           │  │
│  │  ┌─────────────────┐  │  │  ┌─────────────────────┐  │  │
│  │  │     Header      │  │  │  │ CustomerScreen      │  │  │
│  │  ├─────────────────┤  │  │  │ (portrait/landscape)│  │  │
│  │  │   Main Panel    │  │  │  └─────────────────────┘  │  │
│  │  │  (HomeScreen)   │  │  │                           │  │
│  │  ├─────────────────┤  │  │                           │  │
│  │  │     Footer      │  │  │                           │  │
│  │  └─────────────────┘  │  │                           │  │
│  └───────────────────────┘  └───────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

## Navigation

Navigation is handled using a sealed class for type-safe routing:

```kotlin
sealed class Screen {
    object Login : Screen()
    object Home : Screen()
    object Pay : Screen()
    object Lock : Screen()
    object ReturnItem : Screen()
    object CashPickup : Screen()
    object VendorPayout : Screen()
    object OrderReport : Screen()
}

@Composable
fun POSNavHost(
    navController: NavController,
    startDestination: Screen = Screen.Login
) {
    NavHost(navController, startDestination) {
        composable<Screen.Login> { LoginScreen() }
        composable<Screen.Home> { HomeScreen() }
        composable<Screen.Pay> { PayScreen() }
        composable<Screen.Lock> { LockScreen() }
        composable<Screen.ReturnItem> { ReturnItemScreen() }
        composable<Screen.CashPickup> { CashPickupScreen() }
        composable<Screen.VendorPayout> { VendorPayoutScreen() }
        composable<Screen.OrderReport> { OrderReportScreen() }
    }
}
```

---

## Main Screens

| Screen | Composable | Purpose |
|--------|------------|---------|
| `LoginScreen` | `LoginScreen.kt` | Employee authentication |
| `HomeScreen` | `HomeScreen.kt` | Main POS transaction screen |
| `PayScreen` | `PayScreen.kt` | Payment processing |
| `LockScreen` | `LockScreen.kt` | Device lock screen |
| `ReturnItemScreen` | `ReturnItemScreen.kt` | Returns processing |
| `CashPickupScreen` | `CashPickupScreen.kt` | Cash management |
| `VendorPayoutScreen` | `VendorPayoutScreen.kt` | Vendor payouts |
| `CustomerScreen` | `CustomerScreen.kt` | Customer display |

---

## File Organization

```
shared/src/commonMain/kotlin/com/unisight/gropos/
├── ui/
│   ├── theme/
│   │   ├── Color.kt              # Color definitions
│   │   ├── Typography.kt         # Font styles
│   │   ├── Spacing.kt            # Spacing values
│   │   └── Theme.kt              # Theme composable
│   ├── screens/
│   │   ├── home/
│   │   │   ├── HomeScreen.kt
│   │   │   └── HomeViewModel.kt
│   │   ├── pay/
│   │   │   ├── PayScreen.kt
│   │   │   └── PayViewModel.kt
│   │   └── ...
│   └── components/
│       ├── buttons/
│       │   └── POSButtons.kt
│       ├── tenkey/
│       │   └── TenKey.kt
│       ├── dialogs/
│       │   └── POSDialogs.kt
│       └── ...
├── viewmodel/
│   └── BaseViewModel.kt
└── navigation/
    └── Navigation.kt
```

---

## Responsive System

### Window Size Classes

```kotlin
enum class WindowWidthSizeClass {
    Compact,   // < 1024dp
    Medium,    // 1024dp - 1800dp
    Expanded   // > 1800dp
}

@Composable
fun rememberWindowSizeClass(): WindowWidthSizeClass {
    val configuration = LocalConfiguration.current
    return remember(configuration.screenWidthDp) {
        when {
            configuration.screenWidthDp < 1024 -> WindowWidthSizeClass.Compact
            configuration.screenWidthDp < 1800 -> WindowWidthSizeClass.Medium
            else -> WindowWidthSizeClass.Expanded
        }
    }
}
```

### Responsive Composables

```kotlin
@Composable
fun ResponsiveLayout(
    compact: @Composable () -> Unit,
    medium: @Composable () -> Unit = compact,
    expanded: @Composable () -> Unit = medium
) {
    val windowSize = rememberWindowSizeClass()
    
    when (windowSize) {
        WindowWidthSizeClass.Compact -> compact()
        WindowWidthSizeClass.Medium -> medium()
        WindowWidthSizeClass.Expanded -> expanded()
    }
}
```

### Breakpoints Reference

| Width Range | Size Class | Notes |
|-------------|------------|-------|
| ≤ 1024dp | `Compact` | Small screens, Android tablets |
| 1024-1800dp | `Medium` | Medium displays |
| > 1800dp | `Expanded` | Standard HD, large displays |

---

## Key Patterns

### Screen + ViewModel Pattern

Each screen follows a consistent pattern:

```kotlin
// HomeScreen.kt
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    HomeScreenContent(
        state = state,
        onPayClick = viewModel::onPayClick,
        onItemSelect = viewModel::onItemSelect,
        onQuantityChange = viewModel::onQuantityChange
    )
}

@Composable
private fun HomeScreenContent(
    state: HomeUiState,
    onPayClick: () -> Unit,
    onItemSelect: (TransactionItem) -> Unit,
    onQuantityChange: (Int) -> Unit
) {
    // Pure UI, no business logic
    Row(modifier = Modifier.fillMaxSize()) {
        LeftBlock(modifier = Modifier.weight(0.7f)) {
            OrderList(
                items = state.orderItems,
                onItemSelect = onItemSelect
            )
        }
        RightBlock(modifier = Modifier.weight(0.3f)) {
            TotalsCard(
                itemCount = state.itemCount,
                grandTotal = state.grandTotal,
                onPayClick = onPayClick
            )
            TenKey(
                state = state.tenKeyState,
                onDigitClick = { /* ... */ }
            )
        }
    }
}
```

### ViewModel with StateFlow

```kotlin
// HomeViewModel.kt
class HomeViewModel(
    private val transactionRepository: TransactionRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()
    
    fun onItemSelect(item: TransactionItem) {
        _state.update { it.copy(selectedItem = item) }
    }
    
    fun onPayClick() {
        viewModelScope.launch {
            // Navigate to pay screen
        }
    }
    
    fun onQuantityChange(quantity: Int) {
        _state.update { 
            it.copy(currentQuantity = quantity) 
        }
    }
}

data class HomeUiState(
    val orderItems: List<TransactionItemUiModel> = emptyList(),
    val selectedItem: TransactionItem? = null,
    val currentQuantity: Int = 1,
    val grandTotal: BigDecimal = BigDecimal.ZERO,
    val itemCount: Int = 0,
    val tenKeyState: TenKeyState = TenKeyState(),
    val isLoading: Boolean = false,
    val error: String? = null
)
```

### Dialog Pattern

```kotlin
@Composable
fun DialogHost(
    dialogState: DialogState,
    onDismiss: () -> Unit
) {
    when (val dialog = dialogState.currentDialog) {
        is POSDialog.Error -> ErrorDialog(
            message = dialog.message,
            onDismiss = onDismiss
        )
        is POSDialog.ManagerApproval -> ManagerApprovalDialog(
            state = dialog.state,
            onApprove = dialog.onApprove,
            onDismiss = onDismiss
        )
        is POSDialog.AgeVerification -> AgeVerificationDialog(
            requiredAge = dialog.requiredAge,
            onConfirm = dialog.onConfirm,
            onDismiss = onDismiss
        )
        null -> { /* No dialog */ }
    }
}
```

---

## Layout Containers

| Container | Compose Equivalent | Usage |
|-----------|-------------------|-------|
| Stack | `Box` | Overlay dialogs, view stacking |
| VBox | `Column` | Vertical layouts |
| HBox | `Row` | Horizontal layouts |
| Grid | `LazyVerticalGrid` | Product grids |
| Split | `Row` with weights | Two-column layouts (70/30) |

---

## Best Practices

### Layout Guidelines

1. **Use weight modifiers** for flexible sizing
   ```kotlin
   Row {
       Column(Modifier.weight(0.7f)) { /* 70% */ }
       Column(Modifier.weight(0.3f)) { /* 30% */ }
   }
   ```

2. **Use `fillMaxWidth/Height`** appropriately
   ```kotlin
   Button(Modifier.fillMaxWidth()) { Text("Full Width") }
   ```

3. **Define minimum touch targets** (48dp)
   ```kotlin
   Box(Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp))
   ```

### State Guidelines

1. **Lift state up** - Keep state in ViewModels
2. **Use immutable data classes** for UI state
3. **Collect flows in Composables** with `collectAsState()`
4. **Avoid side effects in Composables** - Use `LaunchedEffect`

### Theme Guidelines

1. **Use theme values** instead of hardcoded colors
   ```kotlin
   // Good
   color = MaterialTheme.colorScheme.primary
   
   // Avoid
   color = Color(0xFF04571B)
   ```

2. **Define custom colors in Theme.kt**
3. **Use `LocalContentColor`** for text colors

---

## Dependency Injection

### Koin Module Setup

```kotlin
val appModule = module {
    // ViewModels
    viewModel { HomeViewModel(get()) }
    viewModel { PayViewModel(get(), get()) }
    viewModel { LoginViewModel(get()) }
    
    // Repositories
    single { TransactionRepository(get()) }
    single { ProductRepository(get()) }
    single { EmployeeRepository(get()) }
    
    // Hardware
    single { HardwareManager(get()) }
}

// Usage in Composable
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel()
) {
    // ...
}
```

---

## Platform-Specific Code

### expect/actual Pattern

```kotlin
// commonMain
expect class PlatformPrinter {
    fun print(receipt: Receipt)
    fun openDrawer()
}

// desktopMain
actual class PlatformPrinter {
    actual fun print(receipt: Receipt) {
        // JavaPOS implementation
    }
    actual fun openDrawer() {
        // JavaPOS drawer
    }
}

// androidMain
actual class PlatformPrinter {
    actual fun print(receipt: Receipt) {
        // Sunmi/PAX SDK
    }
    actual fun openDrawer() {
        // Android drawer
    }
}
```

---

## See Also

- [UI Design System](./UI_DESIGN_SYSTEM.md) - Colors, typography, spacing
- [Screen Layouts](./SCREEN_LAYOUTS.md) - All screen wireframes
- [Components](./COMPONENTS.md) - Custom component reference

