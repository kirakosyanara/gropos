# GroPOS UI Design System

> Comprehensive design system documentation for the GroPOS application

## Table of Contents

- [Color Palette](#color-palette)
- [Typography](#typography)
- [Spacing System](#spacing-system)
- [Button Styles](#button-styles)
- [Form Elements](#form-elements)
- [Cards and Containers](#cards-and-containers)
- [Icons](#icons)
- [Responsive Design](#responsive-design)

---

## Color Palette

### Primary Colors

| Color Name | Hex Code | Usage |
|------------|----------|-------|
| Primary Green | `#04571B` | Primary actions, success states, branding |
| Primary Green Hover | `#1C5733` | Selected states, button hover |
| Accent Green | `#327D4F` | Secondary accents, text highlights |

### Secondary Colors

| Color Name | Hex Code | Usage |
|------------|----------|-------|
| Primary Blue | `#2073BE` | Primary alternative actions |
| Primary Blue Hover | `#135B9C` | Button hover state |
| Warning Orange | `#FE793A` | Manager requests, lock button, alerts |
| Danger Red | `#FA1B1B` | Cancel, delete, danger actions |
| Danger Red Hover | `#D20707` | Danger button hover |

### Neutral Colors

| Color Name | Hex Code | Usage |
|------------|----------|-------|
| White | `#FFFFFF` | Backgrounds, cards |
| Light Gray 1 | `#EFF1F1` | Right panel background |
| Light Gray 2 | `#E1E3E3` | Left panel background |
| Light Gray 3 | `#ECEFF6` | Scroll bar background |
| Border Gray | `#857370` | Borders, secondary buttons |
| Disabled Gray | `#D9D9D9` | Disabled states |
| Text Secondary | `#A0A0A0` | Secondary text |
| Overlay Black | `#000000B2` | Modal overlays (70% opacity) |

### Multi-Row Button Accent

| Color Name | Hex Code | Usage |
|------------|----------|-------|
| Purple Border | `#7A55B3` | Multi-row button borders (lookup items) |

### Compose Color Definition

```kotlin
// Color.kt
object GroPOSColors {
    // Primary
    val PrimaryGreen = Color(0xFF04571B)
    val PrimaryGreenHover = Color(0xFF1C5733)
    val AccentGreen = Color(0xFF327D4F)
    
    // Secondary
    val PrimaryBlue = Color(0xFF2073BE)
    val PrimaryBlueHover = Color(0xFF135B9C)
    val WarningOrange = Color(0xFFFE793A)
    val DangerRed = Color(0xFFFA1B1B)
    val DangerRedHover = Color(0xFFD20707)
    
    // Neutral
    val White = Color(0xFFFFFFFF)
    val LightGray1 = Color(0xFFEFF1F1)
    val LightGray2 = Color(0xFFE1E3E3)
    val LightGray3 = Color(0xFFECEFF6)
    val BorderGray = Color(0xFF857370)
    val DisabledGray = Color(0xFFD9D9D9)
    val TextSecondary = Color(0xFFA0A0A0)
    val OverlayBlack = Color(0xB2000000)
    
    // Accent
    val PurpleBorder = Color(0xFF7A55B3)
}
```

---

## Typography

### Font Family

```kotlin
// Typography.kt
val ArchivoFontFamily = FontFamily(
    Font(Res.font.archivo_regular, FontWeight.Normal),
    Font(Res.font.archivo_medium, FontWeight.Medium),
    Font(Res.font.archivo_bold, FontWeight.Bold)
)
```

### Font Sizes

| Size Name | Pixel Size | Usage |
|-----------|------------|-------|
| Base | `16.sp` | Default text |
| Small | `12.sp` | Multi-row button text |
| Medium | `20.sp` | Secondary buttons, lock button |
| Large | `24.sp` | Button large, labels |
| XL | `26.sp` | Price info text, discount info |
| XXL | `32.sp` | Ten-key buttons |
| XXXL | `35.sp` | Error dialog text, change dialog labels |

### Typography Definition

```kotlin
// Typography.kt
val GroPOSTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = ArchivoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 35.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = ArchivoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = ArchivoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 26.sp
    ),
    titleLarge = TextStyle(
        fontFamily = ArchivoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = ArchivoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = ArchivoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    bodySmall = TextStyle(
        fontFamily = ArchivoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    )
)
```

---

## Spacing System

### Padding Scale

| Size | Value | Usage |
|------|-------|-------|
| XS | `5.dp` | Grid gaps, small spacing |
| S | `10.dp` | Button padding, icon spacing |
| M | `16.dp` | Box padding, button large |
| L | `20.dp` | Section padding |
| XL | `24.dp` | Dialog padding, header padding |
| XXL | `38.dp` | Page sections, login form |
| XXXL | `48.dp` | Right block horizontal padding |

### Border Radius

| Size | Value | Usage |
|------|-------|-------|
| Small | `3.dp` | Search fields, quantity fields |
| Medium | `16.dp` | List views, boxes |
| Large | `20.dp` | Buttons |
| Round | `28.dp` | Avatar, circular elements |
| Pill | `50.dp` | Logo box, request area |

### Spacing Definition

```kotlin
// Spacing.kt
object GroPOSSpacing {
    val XS = 5.dp
    val S = 10.dp
    val M = 16.dp
    val L = 20.dp
    val XL = 24.dp
    val XXL = 38.dp
    val XXXL = 48.dp
}

object GroPOSRadius {
    val Small = 3.dp
    val Medium = 16.dp
    val Large = 20.dp
    val Round = 28.dp
    val Pill = 50.dp
}
```

---

## Button Styles

### Primary Buttons

```kotlin
@Composable
fun SuccessButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = GroPOSColors.PrimaryGreen,
            contentColor = Color.White,
            disabledContainerColor = GroPOSColors.DisabledGray,
            disabledContentColor = Color.White
        ),
        contentPadding = PaddingValues(14.dp),
        content = content
    )
}

@Composable
fun PrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = GroPOSColors.PrimaryBlue,
            contentColor = Color.White
        ),
        content = content
    )
}

@Composable
fun DangerButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = GroPOSColors.DangerRed,
            contentColor = Color.White
        ),
        content = content
    )
}
```

### Secondary Buttons

```kotlin
@Composable
fun OutlineButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, GroPOSColors.BorderGray),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = GroPOSColors.PrimaryGreen
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
        content = content
    )
}

@Composable
fun BackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, GroPOSColors.BorderGray),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 18.dp)
    ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        Spacer(Modifier.width(8.dp))
        Text("Back")
    }
}
```

### Button Size Variants

```kotlin
@Composable
fun LargeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(14.dp)
    ) {
        ProvideTextStyle(
            MaterialTheme.typography.titleLarge // 24.sp
        ) {
            content()
        }
    }
}

@Composable
fun ExtraLargeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(18.dp)
    ) {
        ProvideTextStyle(
            MaterialTheme.typography.titleLarge // 24.sp
        ) {
            content()
        }
    }
}
```

---

## Form Elements

### Search Field

```kotlin
@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search..."
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = { Text(placeholder) },
        leadingIcon = {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                modifier = Modifier.size(35.dp)
            )
        },
        shape = RoundedCornerShape(3.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = Color.Gray
        ),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 15.dp)
    )
}
```

### Quantity Field

```kotlin
@Composable
fun QuantityField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.width(75.dp),
        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
        shape = RoundedCornerShape(3.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true
    )
}
```

### Password Field

```kotlin
@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var passwordVisible by remember { mutableStateOf(false) }
    
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        visualTransformation = if (passwordVisible) 
            VisualTransformation.None 
        else 
            PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                    if (passwordVisible) Icons.Filled.Visibility 
                    else Icons.Filled.VisibilityOff,
                    contentDescription = null
                )
            }
        },
        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
    )
}
```

---

## Cards and Containers

### White Box (Info Card)

```kotlin
@Composable
fun WhiteBox(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.White
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            content = content
        )
    }
}
```

### Green Box (Highlight Card)

```kotlin
@Composable
fun GreenBox(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = GroPOSColors.PrimaryGreen
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            content = content
        )
    }
}
```

### Page Sections

```kotlin
@Composable
fun LeftBlock(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(GroPOSColors.LightGray2)
            .padding(38.dp),
        content = content
    )
}

@Composable
fun RightBlock(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(GroPOSColors.LightGray1)
            .padding(horizontal = 48.dp, vertical = 8.dp),
        content = content
    )
}
```

---

## Icons

### Icon System

The application uses Material Icons via Compose Material Icons Extended.

```kotlin
// Common icons used in the app
Icon(Icons.Filled.Search, contentDescription = "Search")
Icon(Icons.Filled.Person, contentDescription = "Customer")
Icon(Icons.Filled.ShoppingCart, contentDescription = "Cart")
Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
Icon(Icons.Filled.Delete, contentDescription = "Delete")
Icon(Icons.Filled.Add, contentDescription = "Add")
Icon(Icons.Filled.Check, contentDescription = "Confirm")
Icon(Icons.Filled.Close, contentDescription = "Close")
Icon(Icons.Filled.Lock, contentDescription = "Lock")
Icon(Icons.Filled.Nfc, contentDescription = "NFC")
```

### Custom Icons

```kotlin
// For custom icons, use painterResource
Image(
    painter = painterResource(Res.drawable.logo),
    contentDescription = "Company Logo"
)

Image(
    painter = painterResource(Res.drawable.back_icon),
    contentDescription = "Back"
)
```

### Icon Sizes

```kotlin
object GroPOSIconSize {
    val Small = 24.dp
    val Medium = 35.dp
    val Large = 45.dp
}
```

---

## Responsive Design

### Window Size Classes

```kotlin
enum class WindowWidthSizeClass {
    Compact,   // < 1024px
    Medium,    // 1024px - 1800px
    Expanded   // > 1800px
}

@Composable
fun calculateWindowSizeClass(): WindowSizeClass {
    val configuration = LocalConfiguration.current
    return when {
        configuration.screenWidthDp < 1024 -> WindowWidthSizeClass.Compact
        configuration.screenWidthDp < 1800 -> WindowWidthSizeClass.Medium
        else -> WindowWidthSizeClass.Expanded
    }
}
```

### Responsive Layouts

```kotlin
// Home Screen: 70/30 Split
@Composable
fun HomeLayout() {
    Row(modifier = Modifier.fillMaxSize()) {
        LeftBlock(modifier = Modifier.weight(0.7f)) {
            // Order list content
        }
        RightBlock(modifier = Modifier.weight(0.3f)) {
            // Actions panel
        }
    }
}

// Login Screen: 50/50 Split
@Composable
fun LoginLayout() {
    Row(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(0.5f)) {
            // Branding section
        }
        Box(modifier = Modifier.weight(0.5f)) {
            // Authentication section
        }
    }
}

// Payment Tabs: 4 Equal Columns
@Composable
fun PaymentTabsRow() {
    Row(modifier = Modifier.fillMaxWidth()) {
        PaymentTab(Modifier.weight(1f), "Charge")
        PaymentTab(Modifier.weight(1f), "EBT")
        PaymentTab(Modifier.weight(1f), "Cash")
        PaymentTab(Modifier.weight(1f), "Other")
    }
}
```

---

## Dialog Styling

### Error Message Dialog

```kotlin
@Composable
fun ErrorMessageDialog(
    message: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.width(550.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(25.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Filled.Error,
                    contentDescription = null,
                    tint = GroPOSColors.DangerRed,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.displayLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
                SuccessButton(onClick = onDismiss) {
                    Text("OK")
                }
            }
        }
    }
}
```

### Change Dialog

```kotlin
@Composable
fun ChangeDialog(
    changeDue: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.width(650.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 25.dp, vertical = 35.dp),
                verticalArrangement = Arrangement.spacedBy(15.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Change Due",
                    style = MaterialTheme.typography.displayLarge
                )
                Text(
                    text = changeDue,
                    style = MaterialTheme.typography.displayLarge,
                    color = GroPOSColors.PrimaryGreen
                )
                PrimaryButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("OK", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
```

---

## Status Indicators

### Request Status Box

```kotlin
@Composable
fun RequestStatusBox(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(30.dp),
        color = GroPOSColors.WarningOrange
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
```

### Status Text Style

```kotlin
@Composable
fun StatusText(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = GroPOSColors.PrimaryGreen
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 15.dp),
            color = Color.White,
            style = MaterialTheme.typography.titleLarge
        )
    }
}
```

---

## Animation Support

The application uses Compose animations for:
- Product scanning feedback
- Price display transitions
- Customer screen advertisements
- Dialog transitions

```kotlin
// Fade animation example
AnimatedVisibility(
    visible = isVisible,
    enter = fadeIn() + slideInVertically(),
    exit = fadeOut() + slideOutVertically()
) {
    ProductScanFeedback()
}

// Price change animation
val price by animateFloatAsState(
    targetValue = currentPrice,
    animationSpec = tween(durationMillis = 300)
)
```

---

## CSS to Compose Mapping Reference

This section provides a comprehensive mapping from JavaFX CSS classes to Compose Multiplatform equivalents.

### Layout Classes

| JavaFX CSS Class | CSS Properties | Compose Equivalent |
|------------------|----------------|-------------------|
| `.left-block` | `background: #E1E3E3; padding: 38px` | `Modifier.background(LightGray2).padding(38.dp)` |
| `.right-block` | `background: #EFF1F1; padding: 0 48 8 48` | `Modifier.background(LightGray1).padding(start=48.dp, end=48.dp, bottom=8.dp)` |
| `.hover-block` | `background: #000000B2; padding: 38px` | `Modifier.background(OverlayBlack).padding(38.dp)` |
| `.list-view` | `background: white; border-radius: 16px` | `Modifier.background(White, RoundedCornerShape(16.dp))` |
| `.whiteBox` | `background: white; border-radius: 8px` | `Surface(color = White, shape = RoundedCornerShape(8.dp))` |
| `.greenBox` | `background: #04571B` | `Surface(color = PrimaryGreen)` |

### Button Classes

| JavaFX CSS Class | Compose Component |
|------------------|-------------------|
| `.btn-success` | `SuccessButton()` |
| `.btn-primary` | `PrimaryButton()` |
| `.delete-cancel` | `DangerButton()` |
| `.secondary` | `OutlineButton()` |
| `.back-button` | `BackButton()` |
| `.tab-buttons-bg` | `TabButton()` |
| `.btn-xl` | `Modifier.padding(18.dp, 24.dp)` |
| `.btn-lg` | `Modifier.padding(14.dp, 20.dp)` |

### Text Classes

| JavaFX CSS Class | Compose TextStyle |
|------------------|-------------------|
| `.pay-value-field` | `titleLarge` (24sp) |
| `.b, .h3` | `titleMedium` + `FontWeight.Bold` |
| `.white-box-label` | `bodyLarge` (16sp) |
| `.product-name` | `titleSmall` + `FontWeight.Bold` |
| `.info-details-text` | `bodyMedium` (14sp) |
| `.subtotal_text` | `bodyLarge` (16sp) |
| `.subtotal_value` | `titleMedium` + `FontWeight.Bold` |
| `.station-text` | `headlineSmall` (20sp) |
| `.header-text` | `headlineMedium` (22sp) |
| `.dialog-header-text` | `titleMedium` (18sp) |
| `.discount-info-text` | `headlineMedium` + White |
| `.error-message` | `bodyMedium` + `DangerRed` |

### Form Element Classes

| JavaFX CSS Class | Compose Component |
|------------------|-------------------|
| `.input-field` | `OutlinedTextField()` |
| `.search-field` | `SearchTextField()` |
| `.quantity-field` | `OutlinedTextField(textAlign = Center)` |
| `.password-input` | `OutlinedTextField(visualTransformation = PasswordVisualTransformation())` |

### Dialog Classes

| JavaFX CSS Class | Compose Equivalent |
|------------------|-------------------|
| `.recall-dialog-header-background` | `Modifier.background(PrimaryGreen).padding(16.dp, 20.dp)` |
| `.details-dialog-header-background` | `Modifier.background(PrimaryGreen).padding(16.dp, 20.dp)` |
| `.lookup-dialog-header-background` | `Modifier.background(PrimaryGreen).padding(16.dp, 20.dp)` |
| `.close-button` | `IconButton` with close icon |
| `.buttonBox` | `Row(horizontalArrangement = spacedBy(15.dp))` |

### Payment Classes

| JavaFX CSS Class | Compose Equivalent |
|------------------|-------------------|
| `.pay-info-item` | `Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween)` |
| `.credit-section` | Payment type section container |
| `.ebt-section` | EBT payment section container |
| `.cash-section` | Cash payment section container |
| `.cash-item-section` | Cash quick-amount grid |
| `.food-stamp-button` | EBT Food Stamp button styling |
| `.ebt-cash-button` | EBT Cash button styling |
| `.balance-check-button` | Balance check button styling |

### Customer Screen Classes

| JavaFX CSS Class | Compose Equivalent |
|------------------|-------------------|
| `.customer-screen-view` | Root Column with `fillMaxSize()` |
| `.header` | Header Row with green background |
| `.total-box` | Summary card with white background |
| `.ad-box` | Advertisement container |
| `.saving_value` | Savings amount text (bold, 18sp) |
| `.name-label` | Store name text |

### Common Patterns

**JavaFX Separator → Compose Spacer/Divider:**
```kotlin
// JavaFX: <Separator opacity="0.0" HBox.hgrow="ALWAYS"/>
// Compose:
Spacer(modifier = Modifier.weight(1f))

// JavaFX: <Separator prefHeight="1"/>
// Compose:
Divider(modifier = Modifier.height(1.dp))
```

**JavaFX Region → Compose Spacer:**
```kotlin
// JavaFX: <Region VBox.vgrow="ALWAYS"/>
// Compose:
Spacer(modifier = Modifier.weight(1f))
```

**JavaFX GridPane → Compose Row/Column with weights:**
```kotlin
// JavaFX GridPane with percentWidth
// <ColumnConstraints percentWidth="70"/>
// <ColumnConstraints percentWidth="30"/>

// Compose:
Row(modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.weight(0.7f)) { /* 70% */ }
    Column(modifier = Modifier.weight(0.3f)) { /* 30% */ }
}
```

**JavaFX HBox/VBox → Compose Row/Column:**
```kotlin
// JavaFX: <HBox alignment="CENTER" spacing="10">
// Compose:
Row(
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    verticalAlignment = Alignment.CenterVertically
)

// JavaFX: <VBox spacing="20" alignment="TOP_CENTER">
// Compose:
Column(
    verticalArrangement = Arrangement.spacedBy(20.dp),
    horizontalAlignment = Alignment.CenterHorizontally
)
```

**JavaFX fx:id → Compose state:**
```kotlin
// JavaFX: <Text fx:id="grandTotal" text="$0.00"/>
// Compose:
val state by viewModel.state.collectAsState()
Text(text = state.grandTotal.formatCurrency())
```

**JavaFX onAction → Compose onClick:**
```kotlin
// JavaFX: <Button onAction="#onPayAction" text="Pay"/>
// Compose:
Button(onClick = viewModel::onPayAction) {
    Text("Pay")
}
```

---

## Related Documentation

- [INDEX.md](./INDEX.md) - Complete component inventory
- [SCREEN_LAYOUTS.md](./SCREEN_LAYOUTS.md) - Screen wireframes
- [DIALOGS.md](./DIALOGS.md) - Dialog specifications
- [COMPONENTS.md](./COMPONENTS.md) - Component details
