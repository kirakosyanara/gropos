# GroPOS 2.0

**Kotlin Multiplatform Point-of-Sale System**

A modern, offline-first POS application built with Kotlin Multiplatform and Jetpack Compose. Designed for grocery retail environments with support for Desktop (Windows/Linux/macOS) and Android.

## 🚀 Version 0.9.0 - Feature Complete Alpha

All P0 critical features are implemented:

- ✅ **Lock Screen & Inactivity Timer** - Auto-lock after 5 minutes
- ✅ **Manager Approval Flow** - RBAC permissions system
- ✅ **Checkout Modification Mode** - Edit quantities, void items
- ✅ **Employee List + Till Assignment** - Full login flow
- ✅ **Returns Processing** - Return items with manager approval

## 📋 Requirements

- **JDK 17+** (for Desktop)
- **Android Studio** (for Android development)
- **Gradle 8.5+**

## 🏃 How to Run

### Desktop (Development)

```bash
./gradlew :desktopApp:run
```

### Desktop (Distributable)

```bash
# Create native distributable
./gradlew :desktopApp:packageDmg      # macOS
./gradlew :desktopApp:packageMsi      # Windows
./gradlew :desktopApp:packageDeb      # Linux
```

### Android

```bash
./gradlew :androidApp:installDebug
```

## 🔐 Test Credentials

| Employee | PIN | Role | Notes |
|----------|-----|------|-------|
| Jane Doe | 1234 | Cashier | Standard cashier login |
| John Smith | 5678 | Supervisor | Can approve some actions |
| Mary Johnson | 9999 | Manager | Can approve all actions |

## 📖 Full Application Flow

### 1. Start the Application

```bash
./gradlew :desktopApp:run
```

### 2. Lock Screen → Login

- Application starts on the **Lock Screen**
- Click anywhere or press a key to unlock
- You're taken to the **Login Screen**

### 3. Login Flow

1. **Select Employee** - Click on an employee card (e.g., "Jane Doe")
2. **Enter PIN** - Use the TenKey pad to enter the 4-digit PIN
3. **Select Till** - Choose an available cash drawer
4. You're now on the **Checkout Screen**

### 4. Sell Items

1. **Scan Barcode** - Enter a barcode in the scan field:
   - `100001` - Organic Milk ($5.99)
   - `100002` - Fresh Apple ($1.50)
   - `100003` - Cola ($2.49, taxable)
   - `100004` - Potato Chips ($3.99)
2. Items appear in the **Order List** on the left
3. **Modify Items** - Click a line item to enter modification mode:
   - Update quantity
   - Void line (requires manager approval)
4. Click **PAY** to proceed to payment

### 5. Process Payment

1. On the **Payment Screen**, view the order total
2. Select **Cash** and enter the amount tendered
3. Click **Process** to complete the sale
4. Transaction is saved to the local database

### 6. View Transaction History (Recall)

1. From the Checkout Screen, click **Recall** in the Functions panel
2. View list of recent transactions
3. Select a transaction to see details
4. Click **Return Items** to process a return

### 7. Process Returns

1. From Transaction Detail, click **Return Items**
2. Click **+ Add to Return** on items to return
3. Adjust quantity if needed
4. Click **Process Return**
5. Manager approval is required

## 🏗️ Architecture

```
shared/                 # Kotlin Multiplatform shared code
├── commonMain/         # Common business logic
│   ├── core/           # Shared utilities, DI, themes
│   └── features/       # Feature modules
│       ├── auth/       # Authentication & session
│       ├── checkout/   # Product scanning, cart
│       ├── payment/    # Payment processing
│       ├── returns/    # Returns processing
│       └── transaction # Transaction history
├── desktopMain/        # Desktop-specific code
└── androidMain/        # Android-specific code

desktopApp/             # Desktop entry point
androidApp/             # Android entry point
```

## 🧪 Running Tests

```bash
# Run all shared tests
./gradlew :shared:desktopTest

# Run with test report
./gradlew :shared:desktopTest --info
```

## 📚 Documentation

- **Architecture Blueprint**: `docs/development-plan/plan/ARCHITECTURE_BLUEPRINT.md`
- **Screen Layouts**: `docs/development-plan/ui-ux/SCREEN_LAYOUTS.md`
- **API Reference**: `docs/development-plan/reference/API_ENDPOINTS.md`
- **Database Schema**: `docs/development-plan/reference/DATABASE_SCHEMA.md`

## 📝 License

Copyright © 2026 Unisight BIT. All rights reserved.
