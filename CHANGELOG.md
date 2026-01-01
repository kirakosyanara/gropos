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
- Checkout feature domain model: `Cart.addProduct()` with quantity increment logic
- Checkout feature hardware abstraction: `ScannerRepository` interface with `Flow<String>`
- Checkout feature data interface: `ProductRepository` with `findBySku()` method
- Checkout feature business logic: `ScanItemUseCase` reactive scanning with cart management
- Checkout feature data layer: `FakeProductRepository` with sample products (Apple, Banana, etc.)
- Checkout feature data layer: `FakeScannerRepository` with `emitScan()` for testing
- Unit tests for `ScanItemUseCase` (9 test cases including BigDecimal precision tests)

### Changed
- Renamed application from "GrowPOS" to "GroPOS" across all documentation files (39 occurrences in 8 files)