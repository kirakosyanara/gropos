# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased] - 2026-01-01

### Added
- Initial Kotlin Multiplatform project scaffolding
- Gradle version catalog (`libs.versions.toml`) with all core dependencies
- Root `build.gradle.kts` and `settings.gradle.kts` configuration
- Module structure: `shared/`, `desktopApp/`, `androidApp/`
- Shared module configured for commonMain, desktopMain, androidMain source sets
- CouchbaseLite database dependency configured for offline-first architecture
- ProGuard rules for Android release builds
- Comprehensive `.gitignore` for KMP projects
- Auth feature domain layer: `AuthUser`, `UserRole`, `AuthError` models
- Auth feature domain layer: `AuthRepository` interface
- Auth feature domain layer: `ValidateLoginUseCase` with PIN validation (4-digit rule)
- Auth feature data layer: `FakeAuthRepository` with simulated 500ms network delay
- Unit tests for `ValidateLoginUseCase` (7 test cases, TDD approach)
- `gradle.properties` with AndroidX and KMP configuration
- Auth feature presentation layer: `LoginUiState` sealed interface (Idle, Loading, Success, Error)
- Auth feature presentation layer: `LoginViewModel` using Voyager ScreenModel
- Auth feature presentation layer: `LoginScreen` with Voyager navigation
- Auth feature presentation layer: `LoginContent` composable with hoisted state
- Auth feature presentation layer: Preview functions for all UI states
- Koin dependency injection: `AuthModule` providing repository, use case, and viewmodel
- Koin dependency injection: `AppModule` aggregating all feature modules
- Integration tests for `LoginViewModel` (6 test cases)
- Checkout feature domain models: `Product`, `CartItem`, `Cart` with BigDecimal precision
- Checkout feature domain model: `ItemNumber`, `ProductTax`, `ProductSale` supporting types
- Checkout feature domain model: `Cart.addProduct()` with quantity increment by branchProductId
- Checkout feature hardware abstraction: `ScannerRepository` interface with `Flow<String>`
- Checkout feature data interface: `ProductRepository` with `getByBarcode()` method (per DATABASE_SCHEMA.md)
- Checkout feature business logic: `ScanItemUseCase` reactive scanning with cart management
- Checkout feature data layer: `FakeProductRepository` with schema-compliant sample data
- Checkout feature data layer: `FakeScannerRepository` with `emitScan()` for testing
- Unit tests for `ScanItemUseCase` (12 test cases including schema compliance tests)
- Core utility: `CurrencyFormatter` interface with `UsdCurrencyFormatter` implementation
- Checkout feature presentation layer: `CheckoutUiState` with `CheckoutItemUiModel`, `CheckoutTotalsUiModel`
- Checkout feature presentation layer: `CheckoutViewModel` with reactive scanner flow collection
- Checkout feature presentation layer: `CheckoutScreen` (Voyager) with standard POS layout
- Checkout feature presentation layer: `CheckoutContent` composable with LazyColumn and totals panel
- Checkout feature UI: SNAP eligibility badge displayed for `isSnapEligible` items
- Checkout feature UI: Scan feedback snackbar for ProductAdded/NotFound events
- Checkout feature UI: Empty cart state with placeholder
- Checkout feature UI: Preview functions for all screen states (5 previews)
- Koin dependency injection: `CheckoutModule` providing all checkout feature dependencies
- Unit tests for `CheckoutViewModel` (14 test cases)
- Core theme: `GroPOSTheme` Material3 theme with professional POS color palette
- Navigation: Root `App.kt` with Voyager Navigator and GroPOSTheme wrapper
- Navigation: `LoginScreen` → `CheckoutScreen` transition on successful login
- Navigation: TopAppBar with logout button on `CheckoutScreen`
- Navigation: `CheckoutEvent.Logout` event for logout action
- Desktop entry point: `Main.kt` with Koin initialization and 1280x800 window
- Android entry point: `MainActivity.kt` with Koin initialization and edge-to-edge display
- Android manifest with network, camera, and Bluetooth permissions
- Android resources: strings.xml, themes.xml for app configuration

### Changed
- Renamed application from "GrowPOS" to "GroPOS" across all documentation files (39 occurrences in 8 files)
- Modernized "Food Stamp" terminology to "SNAP" across entire codebase
  - Documentation: Updated DATABASE_SCHEMA.md, ARCHITECTURE_BLUEPRINT.md, TEST_SCENARIOS.md, ANDROID_HARDWARE_GUIDE.md
  - Code: Renamed `isFoodStampEligible` to `isSnapEligible` in Product, CartItem models
  - Hardware: Renamed `processEbtFoodStamp()` to `processEbtSnap()`, `EBT_FOODSTAMP` to `EBT_SNAP`

### Refactored
- **BREAKING**: Refactored `Product` model to align with DATABASE_SCHEMA.md
  - Changed `id` to `branchProductId` (Int)
  - Changed `name` to `productName`
  - Changed `price` to `retailPrice`
  - Changed `sku` to `itemNumbers: List<ItemNumber>` (supports multiple barcodes)
  - Added schema-required fields: category, department, taxes, currentSale, etc.
- **BREAKING**: Refactored `CartItem` to align with TransactionItem schema
  - Changed `quantity` to `quantityUsed`
  - Added `priceUsed`, `taxPerUnit`, `taxTotal`, `crvRatePerUnit` fields
  - Added computed properties matching schema: subTotal, savingsTotal, lineTotal
- **BREAKING**: Refactored `ProductRepository` interface
  - Renamed `findBySku()` to `getByBarcode()` per schema specification
  - Added `getByCategory(categoryId: Int)` method
  - Changed return types to nullable (null instead of Result.failure)
- Updated `FakeProductRepository` to use schema example data (Milk: branchProductId=12345)