# Navigation and Routing

**Version:** 2.0 (Kotlin/Compose Multiplatform)  
**Status:** Specification Document

This document describes the navigation system for GroPOS using Compose Multiplatform.

---

## Overview

The navigation system manages:
- Screen transitions
- Layout changes
- Multi-screen support (cashier + customer display)
- Back stack management

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           Navigator (Singleton)                          │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────┐  ┌─────────────────────┐  ┌─────────────────┐  │
│  │    NavController    │  │     Screen State    │  │   Back Stack    │  │
│  │                     │  │                     │  │                 │  │
│  │  - currentScreen    │  │  - HomeScreen       │  │  List<Screen>   │  │
│  │  - navigate()       │  │  - PayScreen        │  │                 │  │
│  │  - goBack()         │  │  - LoginScreen      │  │                 │  │
│  └─────────────────────┘  └─────────────────────┘  └─────────────────┘  │
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────────┐│
│  │                        Screen Definitions                            ││
│  │                                                                      ││
│  │  sealed class Screen {                                               ││
│  │      object Home : Screen()                                          ││
│  │      object Pay : Screen()                                           ││
│  │      object Login : Screen()                                         ││
│  │      data class ProductSearch(val query: String) : Screen()          ││
│  │  }                                                                   ││
│  └─────────────────────────────────────────────────────────────────────┘│
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Core Components

### Screen Definitions

```kotlin
// screens/Screen.kt
sealed class Screen(val route: String) {
    // Main Layout Screens
    object Home : Screen("home")
    object Pay : Screen("pay")
    object OrderReport : Screen("order_report")
    object CashPickup : Screen("cash_pickup")
    object VendorPayout : Screen("vendor_payout")
    object ReturnItem : Screen("return_item")
    
    // Authentication Screens
    object Login : Screen("login")
    object Lock : Screen("lock")
    
    // Lottery Screens
    object Lottery : Screen("lottery")
    object LotterySale : Screen("lottery_sale")
    object LotteryPayout : Screen("lottery_payout")
    
    // Customer Screen (Secondary Display)
    object CustomerDisplay : Screen("customer_display")
}
```

### Navigator

```kotlin
// navigation/Navigator.kt
class Navigator {
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Login)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()
    
    private val _currentLayout = MutableStateFlow<Layout>(Layout.Index)
    val currentLayout: StateFlow<Layout> = _currentLayout.asStateFlow()
    
    private val backStack = mutableListOf<Screen>()
    
    fun navigate(screen: Screen) {
        // Add current to back stack
        backStack.add(_currentScreen.value)
        _currentScreen.value = screen
    }
    
    fun goBack(): Boolean {
        return if (backStack.isNotEmpty()) {
            _currentScreen.value = backStack.removeLast()
            true
        } else {
            false
        }
    }
    
    fun changeLayout(layout: Layout): Navigator {
        _currentLayout.value = layout
        return this
    }
    
    fun goTo(screen: Screen): Navigator {
        _currentScreen.value = screen
        return this
    }
}

enum class Layout {
    Index,    // Login/Lock screens
    Main,     // Transaction screens
    Customer  // Customer display
}
```

---

## Screen Registration

### Main Application Setup

```kotlin
@Composable
fun GroPOSApp() {
    val navigator = remember { Navigator() }
    val currentLayout by navigator.currentLayout.collectAsState()
    val currentScreen by navigator.currentScreen.collectAsState()
    
    CompositionLocalProvider(LocalNavigator provides navigator) {
        when (currentLayout) {
            Layout.Index -> IndexLayout(currentScreen)
            Layout.Main -> MainLayout(currentScreen)
            Layout.Customer -> CustomerLayout()
        }
    }
}

val LocalNavigator = staticCompositionLocalOf<Navigator> {
    error("Navigator not provided")
}
```

### Layout Composables

```kotlin
@Composable
fun MainLayout(currentScreen: Screen) {
    Row(modifier = Modifier.fillMaxSize()) {
        // Sidebar
        Sidebar(
            onNavigate = { screen -> 
                LocalNavigator.current.navigate(screen) 
            }
        )
        
        // Content Area
        Box(modifier = Modifier.weight(1f)) {
            when (currentScreen) {
                Screen.Home -> HomeScreen()
                Screen.Pay -> PayScreen()
                Screen.OrderReport -> OrderReportScreen()
                Screen.CashPickup -> CashPickupScreen()
                Screen.VendorPayout -> VendorPayoutScreen()
                Screen.ReturnItem -> ReturnItemScreen()
                Screen.Lottery -> LotteryScreen()
                Screen.LotterySale -> LotterySaleScreen()
                Screen.LotteryPayout -> LotteryPayoutScreen()
                else -> HomeScreen()
            }
        }
    }
}

@Composable
fun IndexLayout(currentScreen: Screen) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (currentScreen) {
            Screen.Login -> LoginScreen()
            Screen.Lock -> LockScreen()
            else -> LoginScreen()
        }
    }
}
```

---

## Registered Screens

| Screen | Layout | Purpose |
|--------|--------|---------|
| `Home` | Main | Main transaction screen |
| `Pay` | Main | Payment processing |
| `OrderReport` | Main | Order reports |
| `CashPickup` | Main | Cash pickup |
| `VendorPayout` | Main | Vendor payout |
| `ReturnItem` | Main | Item returns |
| `Lottery` | Main | Lottery hub |
| `LotterySale` | Main | Lottery ticket sales |
| `LotteryPayout` | Main | Lottery payout |
| `Login` | Index | Employee login |
| `Lock` | Index | Screen lock |
| `CustomerDisplay` | Customer | Customer display |

---

## Layouts

### Main Layout

The primary transaction layout with header, sidebar, and content area:

```
┌─────────────────────────────────────────────────────────┐
│                        Header                            │
│  Employee Info  │  Date/Time  │  Functions              │
├─────────────────┼───────────────────────────────────────┤
│                 │                                        │
│    Sidebar      │           Content Area                 │
│                 │                                        │
│   - Functions   │    Dynamic content based on            │
│   - Navigation  │    currentScreen                       │
│                 │                                        │
│                 │                                        │
└─────────────────┴───────────────────────────────────────┘
```

### Index Layout

Login and lock screen layout:

```
┌─────────────────────────────────────────────────────────┐
│                                                          │
│                      Content Area                        │
│                                                          │
│                   Login or Lock View                     │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

### Customer Screen Layout

Secondary display for customers:

```
┌─────────────────────────────────────────────────────────┐
│                                                          │
│                    Order Items List                      │
│                                                          │
├──────────────────────────────────────────────────────────┤
│                    Totals Display                        │
├──────────────────────────────────────────────────────────┤
│                 Promotional Content                      │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

---

## Navigation Patterns

### Layout + Screen Navigation

Common pattern for navigating to a different layout and screen:

```kotlin
// Navigate to login
navigator.changeLayout(Layout.Index).goTo(Screen.Login)

// Navigate to home after login
navigator.changeLayout(Layout.Main).goTo(Screen.Home)

// Navigate to lock screen
navigator.changeLayout(Layout.Index).goTo(Screen.Lock)
```

### Same Layout Navigation

When switching screens within the same layout:

```kotlin
// Already in main layout, switch to payment
navigator.navigate(Screen.Pay)

// Switch to reports
navigator.navigate(Screen.OrderReport)
```

### Navigation with Arguments

```kotlin
sealed class Screen {
    data class ProductDetail(val productId: Int) : Screen("product_detail")
    data class TransactionDetail(val transactionGuid: String) : Screen("transaction_detail")
}

// Navigate with arguments
navigator.navigate(Screen.ProductDetail(productId = 12345))

// Handle in composable
@Composable
fun MainLayout(currentScreen: Screen) {
    when (currentScreen) {
        is Screen.ProductDetail -> ProductDetailScreen(currentScreen.productId)
        is Screen.TransactionDetail -> TransactionDetailScreen(currentScreen.transactionGuid)
        // ...
    }
}
```

---

## Multi-Screen Support

### Customer Screen Setup

```kotlin
@Composable
fun CustomerScreenWindow() {
    // This runs on secondary monitor
    Window(
        onCloseRequest = { /* Prevent close */ },
        title = "Customer Display",
        state = rememberWindowState(
            position = WindowPosition(getSecondaryMonitorX(), 0.dp),
            size = DpSize(getSecondaryMonitorWidth(), getSecondaryMonitorHeight())
        )
    ) {
        CustomerLayout()
    }
}

@Composable
fun CustomerLayout() {
    // Observes same OrderStore as main screen
    val items by OrderStore.items.collectAsState()
    val totals by OrderStore.totals.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Order Items
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(items) { item ->
                CustomerOrderItemRow(item)
            }
        }
        
        // Totals
        TotalsDisplay(totals)
        
        // Promotional Content
        PromotionalBanner()
    }
}
```

---

## Observable State

### Current Screen State

```kotlin
// In Navigator
private val _isHomeScreenShown = MutableStateFlow(false)
val isHomeScreenShown: StateFlow<Boolean> = _isHomeScreenShown.asStateFlow()

fun navigate(screen: Screen) {
    _currentScreen.value = screen
    _isHomeScreenShown.value = screen == Screen.Home
}

// Usage: show/hide elements based on current screen
@Composable
fun SomeComponent() {
    val isHome by navigator.isHomeScreenShown.collectAsState()
    
    if (isHome) {
        // Show home-specific content
    }
}
```

---

## Best Practices

### Navigation Guidelines

1. **Use Sealed Classes for Screens**
   - Type-safe navigation
   - Compile-time checking

2. **Layout Changes for Context Switch**
   - Login/Logout → `changeLayout()`
   - Lock/Unlock → `changeLayout()`
   - Within transaction → `navigate()` only

3. **State Preparation**
   - Set store state before navigation
   - Screen can access state on composition

4. **Deep Linking Support**
   - Use route strings for external navigation
   - Handle arguments via data classes

### Adding New Screens

1. Add to `Screen` sealed class
2. Add handling in appropriate Layout composable
3. Navigate using `navigator.navigate(Screen.NewScreen)`

---

## Related Documentation

- [State Management](./STATE_MANAGEMENT.md)
- [Data Flow](./DATA_FLOW.md)
- [Compose Implementation](../ui-ux/COMPOSE_IMPLEMENTATION.md)

---

*Last Updated: January 2026*

