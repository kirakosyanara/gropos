# GroPOS Frontend Documentation

> Complete documentation of the Compose Multiplatform user interface

## Overview

GroPOS uses Kotlin with Compose Multiplatform for declarative UI. The application follows the MVVM (Model-View-ViewModel) pattern with Koin dependency injection.

## Documentation Index

### Design System
- [UI Design System](./UI_DESIGN_SYSTEM.md) - Complete design system reference
  - Color palette
  - Typography
  - Spacing system
  - Button styles
  - Form elements
  - Cards and containers
  - Icons
  - Responsive design

### Layouts & Screens
- [Screen Layouts](./SCREEN_LAYOUTS.md) - All application screens
  - Login Screen
  - Home Screen (Transaction)
  - Payment Screen
  - Customer Screen
  - Lock Screen
  - Functions Panel
  - Dialogs and Modals
  - Reports

### Components
- [Components Reference](./COMPONENTS.md) - Custom Compose components
  - Ten-Key Component
  - Order List Cell
  - Lookup Grid
  - Manager Approval Controller
  - Employee Info
  - Dialog System
  - All dialog components

### User Interaction
- [Keyboard Shortcuts](./KEYBOARD_SHORTCUTS.md) - Keyboard and function key reference
  - Global shortcuts (F1-F12)
  - Home screen shortcuts
  - Ten-key operations
  - Touch considerations

- [Functions Menu](./FUNCTIONS_MENU.md) - POS function operations
  - Recall functions
  - Payment functions
  - Till functions
  - Manager operations

### Technical
- [Compose Implementation](./COMPOSE_IMPLEMENTATION.md) - Technical implementation details
  - Navigation
  - Screen + ViewModel pattern
  - Responsive system
  - Platform-specific code

---

## Quick Reference

### File Organization

```
shared/src/commonMain/kotlin/com/unisight/gropos/
├── ui/
│   ├── theme/                 # Theme definitions
│   │   ├── Color.kt           # Color palette
│   │   ├── Typography.kt      # Font styles
│   │   └── Theme.kt           # Theme composable
│   ├── screens/               # Screen composables
│   │   ├── home/              # Transaction screen
│   │   ├── pay/               # Payment screen
│   │   ├── login/             # Login screen
│   │   ├── lock/              # Lock screen
│   │   ├── customerscreen/    # Customer display
│   │   ├── returnitem/        # Returns screen
│   │   ├── cashpickup/        # Cash pickup
│   │   ├── vendorpayout/      # Vendor payout
│   │   └── report/            # Reports
│   └── components/            # Reusable components
│       ├── tenkey/            # Ten-key pad
│       ├── orderlist/         # Order list
│       ├── lookup/            # Product lookup
│       ├── managerapproval/   # Manager approval
│       └── ...                # 60+ more components
```

### Theme Architecture

```kotlin
// Theme.kt
@Composable
fun GroPOSTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = groPOSColorScheme,
        typography = groPOSTypography,
        content = content
    )
}
```

### Color Quick Reference

| Purpose | Color | Hex |
|---------|-------|-----|
| Primary (Success) | Green | `#04571B` |
| Primary (Info) | Blue | `#2073BE` |
| Danger | Red | `#FA1B1B` |
| Warning | Orange | `#FE793A` |
| Background | Light Gray | `#E1E3E3` |
| Card | White | `#FFFFFF` |

### Button Quick Reference

```kotlin
// Success Button - Green primary actions
SuccessButton(onClick = { }) { Text("Pay") }

// Primary Button - Blue secondary actions
PrimaryButton(onClick = { }) { Text("Submit") }

// Danger Button - Red destructive actions
DangerButton(onClick = { }) { Text("Cancel") }

// Outline Button - Tertiary actions
OutlineButton(onClick = { }) { Text("Back") }
```

### Screen Dimensions

| Target | Resolution | WindowSize |
|--------|------------|------------|
| Standard | 1920x1080 | Large |
| Medium | ≤1800px | Medium |
| Compact | 1024x768 | Compact |
| Customer | 1920x1080 | (separate) |

---

## Key Concepts

### Composable Screen Pattern

Each screen consists of:
1. **Screen Composable** - UI layout
2. **ViewModel class** - UI state and logic
3. **UIState data class** - Immutable state holder

### State Management

```kotlin
@Composable
fun HomeScreen(viewModel: HomeViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()
    
    HomeScreenContent(
        state = state,
        onPayClick = viewModel::onPayClick,
        onItemSelect = viewModel::onItemSelect
    )
}
```

### Dialog Pattern

```kotlin
@Composable
fun ErrorDialog(
    message: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Filled.Error, contentDescription = null)
                Spacer(Modifier.height(16.dp))
                Text(message, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(24.dp))
                PrimaryButton(onClick = onDismiss) {
                    Text("OK")
                }
            }
        }
    }
}
```

### Responsive Layout

```kotlin
@Composable
fun ResponsiveLayout(content: @Composable () -> Unit) {
    val windowSizeClass = calculateWindowSizeClass()
    
    when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> CompactLayout(content)
        WindowWidthSizeClass.Medium -> MediumLayout(content)
        WindowWidthSizeClass.Expanded -> ExpandedLayout(content)
    }
}
```

---

## See Also

- [Architecture Overview](../../ARCHITECTURE.md)
- [MVVM Pattern](../../architecture/MVVM_PATTERN.md)
- [State Management](../../architecture/STATE_MANAGEMENT.md)
- [Error Messages](../../development/ERROR_MESSAGES.md)
