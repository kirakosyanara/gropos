# GroPOS Keyboard Shortcuts

> Complete reference for keyboard shortcuts and function keys

## Table of Contents

- [Global Shortcuts](#global-shortcuts)
- [Home Screen Shortcuts](#home-screen-shortcuts)
- [Ten-Key Operations](#ten-key-operations)
- [Scanner Simulation](#scanner-simulation)

---

## Global Shortcuts

These shortcuts work throughout the application:

| Key | Action | Description |
|-----|--------|-------------|
| **F1** | Show Dialog | Display informational dialog |
| **F2** | Price Check | Open price check dialog (from HomeScreen) |
| **F4** | Lock Device | Manually lock the POS screen |
| **F9** | Restart Application | Full application restart |
| **F10** | Resize to 1024x768 | Switch to small screen mode |
| **F11** | Resize to 1920x1080 | Switch to full HD mode |
| **F12** | Heartbeat Sync | Trigger manual data synchronization |

### Implementation Reference

```kotlin
// From POSApplication.kt - Key event handling
@Composable
fun POSApp() {
    val keyboardHandler = LocalKeyboardHandler.current
    
    LaunchedEffect(Unit) {
        keyboardHandler.onKeyEvent { event ->
            when (event.key) {
                Key.F1 -> showDialog()
                Key.F4 -> lockDevice()
                Key.F9 -> restartApplication()
                Key.F10 -> resizeTo(1024, 768)
                Key.F11 -> resizeTo(1920, 1080)
                Key.F12 -> triggerHeartbeat()
                else -> false
            }
        }
    }
}
```

---

## Home Screen Shortcuts

Shortcuts specific to the main transaction screen:

| Key | Action | Context |
|-----|--------|---------|
| **F2** | Price Check | Opens price check dialog |
| **Enter** | Submit Input | Confirm ten-key entry |
| **Escape** | Cancel | Cancel current operation |

### Item Modification Mode

When an item is selected:

| Key | Action |
|-----|--------|
| **Q** | Quantity mode |
| **D** | Discount mode |
| **P** | Price change mode |

---

## Ten-Key Operations

The ten-key pad supports different modes:

### Default Mode (PLU Entry)

| Key | Action |
|-----|--------|
| **0-9** | Enter digits |
| **QTY** | Set quantity multiplier |
| **OK** | Submit PLU/barcode |
| **Clear** | Clear current input |
| **Backspace** | Delete last digit |

### Quantity Mode

| Key | Action |
|-----|--------|
| **0-9** | Enter quantity (1-99) |
| **OK** | Apply quantity |
| **Reset** | Reset to original |

### Discount Mode

| Key | Action |
|-----|--------|
| **0-9** | Enter percentage |
| **OK** | Apply discount |
| **Reset** | Remove discount |

### Price Mode

| Key | Action |
|-----|--------|
| **.** | Decimal point |
| **0-9** | Enter price |
| **OK** | Apply new price |
| **Reset** | Reset to original |

### Login Mode

| Key | Action |
|-----|--------|
| **0-9** | Enter PIN digits |
| **Clear** | Clear PIN |
| **OK** | Submit PIN |

### Ten-Key Mode Enum

```kotlin
enum class TenKeyMode {
    DIGIT,       // Default numeric entry
    LOGIN,       // PIN entry for login
    QTY,         // Quantity entry
    REFUND,      // Refund amounts
    PROMPT,      // Price/weight prompts
    DISCOUNT,    // Discount percentage
    CASH_PICKUP, // Cash pickup amounts
    VENDOR       // Vendor payout
}
```

---

## Scanner Simulation

### Barcode Entry

When the ten-key is focused, numeric entry followed by OK simulates a barcode scan:

1. Type barcode digits (e.g., `012345678901`)
2. Press **OK** to process as product lookup

### Weight Entry

For products sold by weight:

1. Place on scale (automatic weight capture)
2. Or manually enter weight when prompted

---

## Inactivity Timer

The application implements automatic screen locking:

| Timeout | Action |
|---------|--------|
| 5 minutes | Auto-lock screen |

**Exceptions:**
- Active payment in progress
- Dialog displayed
- Manager approval pending

### Reset Activity

Any of the following resets the inactivity timer:
- Key press
- Mouse click
- Touch input
- Barcode scan
- Scale weight change

---

## Hardware Integration Keys

### Drawer Operations

| Action | Trigger |
|--------|---------|
| Open Drawer | Transaction complete (auto) |
| Open Drawer | "Open Drawer" function button |
| Close Drawer | Physical close (detected) |

### Scanner Integration

The application listens for:
- Datalogic scanner events
- Serial barcode scanner events
- USB HID scanner input

### Scale Integration

Weight updates are automatic and continuous:
- Stable weight → Display and use
- Unstable → "Weighing..." indicator
- Under zero → Error dialog
- Overweight → Error dialog

---

## Development/Debug Keys

These are available in development mode:

| Key | Action |
|-----|--------|
| **F10** | Resize window (small) |
| **F11** | Resize window (large) |
| **Ctrl+Shift+D** | Open debug panel |

---

## Dialog Navigation

### Standard Dialog Shortcuts

| Key | Action |
|-----|--------|
| **Enter** | Confirm/OK |
| **Escape** | Cancel/Close |

### List Selection

| Key | Action |
|-----|--------|
| **Up/Down** | Navigate list items |
| **Enter** | Select item |

---

## Touchscreen Considerations

The application is optimized for touchscreen use:

- All buttons are touch-friendly (min 48dp tap targets)
- On-screen keyboard available for text entry
- Gesture support for scrolling lists
- No hover-dependent functionality

### Touch Gestures

| Gesture | Action |
|---------|--------|
| Tap | Select/Click |
| Swipe | Scroll lists |
| Long Press | Context menu (where applicable) |

### Compose Gesture Handling

```kotlin
@Composable
fun TouchOptimizedButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .clickable(onClick = onClick)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
```
