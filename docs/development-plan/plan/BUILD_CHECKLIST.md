# GroPOS Build Checklist

Use this checklist to track progress as we build GroPOS from scratch with Kotlin + Compose Multiplatform. Let's make this awesome! 🚀

---

## Phase 1: Foundation (Week 1-2)

### Project Setup
- [ ] Create new Git branch for migration
- [ ] Set up Kotlin Multiplatform project structure
- [ ] Configure Gradle with version catalogs
- [ ] Set up `shared`, `desktopApp`, `androidApp` modules
- [ ] Configure `hardware-legacy` module for Java code reuse

### Build Configuration
- [ ] Configure Compose Multiplatform plugin
- [ ] Set up Android target (API 24-34)
- [ ] Set up Desktop target (JVM)
- [ ] Configure Kotlin compiler options
- [ ] Set up resource management

### Core Dependencies
- [ ] Add Kotlin Coroutines
- [ ] Add Kotlinx Serialization
- [ ] Add Ktor Client
- [ ] Add Koin (Dependency Injection)
- [ ] Add Voyager (Navigation)
- [ ] Add SQLDelight (Database)

### CI/CD
- [ ] Set up GitHub Actions workflow
- [ ] Configure desktop build (Windows/Linux)
- [ ] Configure Android build
- [ ] Set up artifact publishing
- [ ] Configure code quality checks (Detekt, Ktlint)

### Base Architecture
- [ ] Create base composables structure
- [ ] Set up theme (colors, typography, shapes)
- [ ] Create navigation graph skeleton
- [ ] Set up Koin modules structure
- [ ] Create platform `expect`/`actual` declarations

---

## Phase 2: Core Business Logic (Week 3-8)

### Domain Models
- [ ] Migrate `Product` model
- [ ] Migrate `TransactionItem` model
- [ ] Migrate `Transaction` model
- [ ] Migrate `Payment` model
- [ ] Migrate `Customer` model
- [ ] Migrate `Employee` model
- [ ] Migrate `Branch` model
- [ ] Migrate `TaxRate` model
- [ ] Migrate `Discount` model
- [ ] Migrate `Promotion` model

### Calculation Engine
- [ ] Migrate `RetailPriceCalculator`
- [ ] Migrate `TaxPriceCalculator`
- [ ] Migrate `CrvPriceCalculator`
- [ ] Migrate `DiscountPriceCalculator`
- [ ] Migrate `DiscountValidator`
- [ ] Migrate promotion calculation logic
- [ ] Migrate EBT/SNAP calculation logic
- [ ] Migrate WIC calculation logic
- [ ] Write unit tests for all calculators (>80% coverage)

### Repository Layer
- [ ] Define repository interfaces (`commonMain`)
- [ ] Implement `ProductRepository`
- [ ] Implement `TransactionRepository`
- [ ] Implement `CustomerRepository`
- [ ] Implement `EmployeeRepository`
- [ ] Implement `SettingsRepository`

### Database Layer (SQLDelight)
- [ ] Define database schema
- [ ] Create Product queries
- [ ] Create Transaction queries
- [ ] Create Customer queries
- [ ] Create Settings queries
- [ ] Implement desktop driver
- [ ] Implement Android driver
- [ ] Test offline data persistence

### API Client (Ktor)
- [ ] Set up base HTTP client
- [ ] Implement authentication (token management)
- [ ] Implement POS API client
- [ ] Implement Transaction API client
- [ ] Implement Device API client
- [ ] Implement sync service
- [ ] Handle offline/online transitions

### State Management
- [ ] Implement `AppState` (employee, branch, settings)
- [ ] Implement `TransactionState` (current transaction)
- [ ] Implement `CartState` (order items)
- [ ] Implement `PaymentState`
- [ ] Create StateFlow-based ViewModels

---

## Phase 3: Desktop UI (Week 9-14)

### Core Components
- [ ] Create `Button` variants (primary, secondary, danger)
- [ ] Create `TextField` with validation
- [ ] Create `NumericKeypad` component
- [ ] Create `ProductCard` component
- [ ] Create `TransactionItemRow` component
- [ ] Create `PaymentMethodSelector`
- [ ] Create `CustomerDisplay` component
- [ ] Create `ReceiptPreview` component
- [ ] Create loading/error states

### Dialogs
- [ ] Create `ConfirmDialog`
- [ ] Create `AlertDialog`
- [ ] Create `ProductLookupDialog`
- [ ] Create `QuantityDialog`
- [ ] Create `DiscountDialog`
- [ ] Create `PriceOverrideDialog`
- [ ] Create `CustomerLookupDialog`
- [ ] Create `ManagerApprovalDialog`

### Screens
- [ ] Implement `LoginScreen`
- [ ] Implement `LockScreen`
- [ ] Implement `HomeScreen` (transaction entry)
- [ ] Implement `PaymentScreen`
- [ ] Implement `ProductLookupScreen`
- [ ] Implement `ReturnScreen`
- [ ] Implement `VoidScreen`
- [ ] Implement `ReportsScreen`
- [ ] Implement `SettingsScreen`
- [ ] Implement `CustomerDisplayScreen` (secondary window)

### Desktop-Specific Features
- [ ] Implement multi-window support
- [ ] Implement keyboard shortcuts (F1-F12)
- [ ] Implement barcode scanner input handling
- [ ] Configure window sizing and positioning
- [ ] Implement system tray (optional)
- [ ] Test on Windows 10/11
- [ ] Test on Ubuntu 22.04

### Hardware Integration (Desktop)
- [ ] Create `PrinterService.desktop.kt` (wrap existing JavaPOS)
- [ ] Create `ScannerService.desktop.kt`
- [ ] Create `ScaleService.desktop.kt`
- [ ] Create `PaymentTerminal.desktop.kt` (wrap PAX PosLink)
- [ ] Create `CashDrawer.desktop.kt`
- [ ] Test all hardware on Windows
- [ ] Test all hardware on Linux (where applicable)

---

## Phase 4: Android Implementation (Week 15-18)

### Android Setup
- [ ] Configure Android manifest
- [ ] Set up permissions
- [ ] Configure ProGuard/R8 rules
- [ ] Set up app signing

### Android Hardware Services
- [ ] Implement `PrinterService.android.kt` (Sunmi/PAX)
- [ ] Implement `ScannerService.android.kt` (camera + hardware)
- [ ] Implement `ScaleService.android.kt` (Bluetooth)
- [ ] Implement `PaymentTerminal.android.kt`

### Touch-Optimized UI
- [ ] Adapt buttons for touch (larger hit areas)
- [ ] Implement swipe gestures where appropriate
- [ ] Optimize keyboard handling
- [ ] Test on various screen sizes

### Device Testing
- [ ] Test on Sunmi V2 Pro / V2s
- [ ] Test on PAX A920 / A930
- [ ] Test on generic Android tablet
- [ ] Test printer functionality
- [ ] Test scanner functionality
- [ ] Test payment processing

### Kiosk Mode
- [ ] Implement lock task mode
- [ ] Disable system navigation
- [ ] Set up boot receiver for auto-start
- [ ] Test recovery from crashes

---

## Phase 5: Polish & Deployment (Week 19-21)

### Testing
- [ ] Complete unit test coverage (>80% business logic)
- [ ] UI tests for critical flows
- [ ] Integration tests for hardware
- [ ] End-to-end transaction tests
- [ ] Stress testing (many transactions)
- [ ] Offline mode testing

### Performance Optimization
- [ ] Profile startup time (<2s target)
- [ ] Profile memory usage (<400MB desktop, <200MB Android)
- [ ] Optimize recomposition
- [ ] Lazy load non-critical features
- [ ] Optimize database queries

### Packaging
- [ ] Configure JLink for desktop
- [ ] Create Windows MSI installer
- [ ] Create Linux DEB package
- [ ] Create Linux RPM package
- [ ] Create AppImage (optional)
- [ ] Sign Android APK/AAB
- [ ] Test silent installation

### Auto-Update System
- [ ] Set up Conveyor for desktop updates
- [ ] Configure update server
- [ ] Test update flow
- [ ] Implement rollback capability

### Documentation
- [ ] Update installation guides
- [ ] Update hardware setup guides
- [ ] Create troubleshooting guide
- [ ] Document configuration options
- [ ] Create admin manual

### Deployment
- [ ] Deploy to staging environment
- [ ] UAT with real users
- [ ] Create rollback plan
- [ ] Deploy to pilot location(s)
- [ ] Monitor for issues
- [ ] Full rollout

---

## Post-Launch 🎉

### Monitoring
- [ ] Set up crash reporting (Firebase/Sentry)
- [ ] Configure performance monitoring
- [ ] Set up alerting for critical errors

### Cleanup
- [ ] Finalize CI/CD pipeline
- [ ] Clean up development branches
- [ ] Archive development docs as historical reference

### Knowledge Transfer
- [ ] Train development team on Kotlin/Compose
- [ ] Document architecture decisions
- [ ] Create onboarding guide for new developers

---

## Sign-Off

| Phase | Completed | Date | Signed By |
|-------|-----------|------|-----------|
| Phase 1: Foundation | ☐ | | |
| Phase 2: Core Logic | ☐ | | |
| Phase 3: Desktop UI | ☐ | | |
| Phase 4: Android | ☐ | | |
| Phase 5: Polish | ☐ | | |
| **🚀 LAUNCH!** | ☐ | | |

---

## Notes

*Add any notes, blockers, or decisions here during the migration process.*

---

**Last Updated:** January 2026

