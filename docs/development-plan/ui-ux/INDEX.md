# GroPOS UI/UX Master Index

> Complete one-to-one mapping from JavaFX implementation to Kotlin Compose Multiplatform

---

## Quick Navigation

| Category | Document | Purpose |
|----------|----------|---------|
| 📐 Design System | [UI_DESIGN_SYSTEM.md](./UI_DESIGN_SYSTEM.md) | Colors, typography, spacing, CSS → Compose mapping |
| 📱 Screens | [SCREEN_LAYOUTS.md](./SCREEN_LAYOUTS.md) | All main application screens |
| 🧩 Components | [COMPONENTS.md](./COMPONENTS.md) | Reusable UI components |
| 📂 Dialogs | [DIALOGS.md](./DIALOGS.md) | All dialog and modal specifications |
| 👥 Customer Screen | [CUSTOMER_SCREEN.md](./CUSTOMER_SCREEN.md) | Secondary display (all orientations) |
| ⌨️ Keyboard | [KEYBOARD_SHORTCUTS.md](./KEYBOARD_SHORTCUTS.md) | Keyboard shortcuts and mappings |
| 🔧 Functions | [FUNCTIONS_MENU.md](./FUNCTIONS_MENU.md) | Functions panel operations |
| 🛠️ Implementation | [COMPOSE_IMPLEMENTATION.md](./COMPOSE_IMPLEMENTATION.md) | Technical patterns |

---

## Complete Screen Inventory

### Main Screens (page/)

| JavaFX FXML | Compose Screen | ViewModel | Status |
|-------------|----------------|-----------|--------|
| `login/login-view.fxml` | `LoginScreen.kt` | `LoginViewModel.kt` | ✅ Documented |
| `lock/lock-view.fxml` | `LockScreen.kt` | `LockViewModel.kt` | ✅ Documented |
| `home/home-view.fxml` | `HomeScreen.kt` | `HomeViewModel.kt` | ✅ Documented |
| `pay/pay-view.fxml` | `PayScreen.kt` | `PayViewModel.kt` | ✅ Documented |
| `returnitem/return-item-view.fxml` | `ReturnItemScreen.kt` | `ReturnItemViewModel.kt` | ✅ Documented |
| `cashpickup/cash-pickup-view.fxml` | `CashPickupScreen.kt` | `CashPickupViewModel.kt` | ✅ Documented |
| `vendorpayout/vendor-payout-view.fxml` | `VendorPayoutScreen.kt` | `VendorPayoutViewModel.kt` | ✅ Documented |
| `report/order/order-report-view.fxml` | `OrderReportScreen.kt` | `OrderReportViewModel.kt` | ✅ Documented |
| `report/orderProduct/order-product-report-view.fxml` | `OrderProductReportScreen.kt` | `OrderProductReportViewModel.kt` | ✅ Documented |

### Customer Display Screens

| JavaFX FXML | Compose Screen | Orientation | Status |
|-------------|----------------|-------------|--------|
| `customerscreen/customer-screen-view.fxml` | `CustomerScreen.kt` | Landscape (16:9) | ✅ Documented |
| `customerscreen/customer-screen-view-portrait.fxml` | `CustomerScreenPortrait.kt` | Portrait (9:16) | ✅ Documented |
| `customerscreen/customer-screen-view-unified.fxml` | `CustomerScreenUnified.kt` | Adaptive | ✅ Documented |

### Layout Wrappers

| JavaFX FXML | Compose Component | Purpose |
|-------------|-------------------|---------|
| `layout/main.fxml` | `MainLayout.kt` | Main app shell with header |
| `layout/header.fxml` | `HeaderBar.kt` | Top navigation header |
| `layout/index.fxml` | `IndexLayout.kt` | Entry point wrapper |
| `layout/customer-screen.fxml` | `CustomerScreenLayout.kt` | Customer display wrapper |

---

## Complete Dialog Inventory

### Transaction Dialogs

| JavaFX FXML | Compose Dialog | Purpose | Status |
|-------------|----------------|---------|--------|
| `holddialog/hold-dialog.fxml` | `HoldDialog.kt` | Hold transaction with name | ✅ |
| `recalldialog/recall-dialog.fxml` | `RecallDialog.kt` | Recall held transactions | ✅ |
| `transactiondiscount/transaction-discount-dialog.fxml` | `TransactionDiscountDialog.kt` | Invoice-level discount | ✅ |
| `transactioninfodialog/transaction-info-dialog.fxml` | `TransactionInfoDialog.kt` | Transaction details | ✅ |

### Product Dialogs

| JavaFX FXML | Compose Dialog | Purpose | Status |
|-------------|----------------|---------|--------|
| `lookup/fxml/lookup.fxml` | `LookupDialog.kt` | Product category lookup | ✅ |
| `moredialog/product-details-dialog.fxml` | `ProductDetailsDialog.kt` | Full item details | ✅ |
| `moredialog/product-details-dialog-customer.fxml` | `ProductDetailsDialogCustomer.kt` | Customer-facing details | ✅ |
| `pricecheck/price-check-dialog.fxml` | `PriceCheckDialog.kt` | Price inquiry | ✅ |
| `chooseitemdialog/choose-prompt-item-dialog.fxml` | `ChoosePromptItemDialog.kt` | PLU selection | ✅ |

### Price & Quantity Dialogs

| JavaFX FXML | Compose Dialog | Purpose | Status |
|-------------|----------------|---------|--------|
| `manualpriceandqtydialog/manual-price-and-qty-dialog.fxml` | `ManualPriceQtyDialog.kt` | Manual price/qty entry | ✅ |
| `discountprice/discount-price-suggestion-dialog.fxml` | `DiscountPriceSuggestionDialog.kt` | Discount suggestions | ✅ |
| `totalpricedialog/total-price-dialog.fxml` | `TotalPriceDialog.kt` | Total confirmation | ✅ |

### Payment Dialogs

| JavaFX FXML | Compose Dialog | Purpose | Status |
|-------------|----------------|---------|--------|
| `changedialog/change-dialog.fxml` | `ChangeDialog.kt` | Display change due | ✅ |
| `ebtdialog/ebt-balance-dialog.fxml` | `EbtBalanceDialog.kt` | EBT balance display | ✅ |

### Return Dialogs

| JavaFX FXML | Compose Dialog | Purpose | Status |
|-------------|----------------|---------|--------|
| `returniteminfodialog/return-item-info-dialog.fxml` | `ReturnItemInfoDialog.kt` | Return item details | ✅ |
| `returniteminfodialog/manual-quantity-dialog.fxml` | `ReturnQuantityDialog.kt` | Return quantity entry | ✅ |
| `returnproductreasondialod/return-product-reason-dialog.fxml` | `ReturnReasonDialog.kt` | Return reason selection | ✅ |
| `pullbackscandialog/pullback-scan-dialog.fxml` | `PullbackScanDialog.kt` | Pullback scanning | ✅ |
| `findtransaction/find-transaction-cell.fxml` | `FindTransactionDialog.kt` | Transaction lookup | ✅ |

### Authentication Dialogs

| JavaFX FXML | Compose Dialog | Purpose | Status |
|-------------|----------------|---------|--------|
| `managerapproval/skin/manager-approval-view.fxml` | `ManagerApprovalDialog.kt` | Manager PIN approval | ✅ |
| `approvedialog/approve-authorization-dialog.fxml` | `ApproveAuthorizationDialog.kt` | Authorization confirmation | ✅ |
| `ageverificationdialog/age-verification-dialog.fxml` | `AgeVerificationDialog.kt` | Age check for restricted items | ✅ |
| `logoutdialog/log-out-info-dialog.fxml` | `LogOutDialog.kt` | Logout confirmation | ✅ |

### Till & Cash Dialogs

| JavaFX FXML | Compose Dialog | Purpose | Status |
|-------------|----------------|---------|--------|
| `addcashdialog/add-cash-dialog.fxml` | `AddCashDialog.kt` | Add cash to drawer | ✅ |
| `accountlistdialog/account-list-dialog.fxml` | `AccountListDialog.kt` | Account selection | ✅ |
| `accountlistdialog/scan-till-dialog.fxml` | `ScanTillDialog.kt` | Till scanning | ✅ |
| `vendorpayoutdialog/vendor-payout-dialog.fxml` | `VendorPayoutDialog.kt` | Vendor payment entry | ✅ |

### Message Dialogs

| JavaFX FXML | Compose Dialog | Purpose | Status |
|-------------|----------------|---------|--------|
| `errormessagedialog/error-message-dialog.fxml` | `ErrorMessageDialog.kt` | Error with OK button | ✅ |
| `errormessagedialog/error-message-dialog-without-button.fxml` | `ErrorMessageDialogAuto.kt` | Auto-dismiss error | ✅ |
| `errormessagedialog/info-message-dialog.fxml` | `InfoMessageDialog.kt` | Informational message | ✅ |
| `errormessagedialog/item-not-found-dialog.fxml` | `ItemNotFoundDialog.kt` | Product not found | ✅ |
| `errormessagedialog/item-by-weight-dialog.fxml` | `ItemByWeightDialog.kt` | Weight prompt | ✅ |
| `errormessagedialog/amount-over-error-message-dialog.fxml` | `AmountOverDialog.kt` | Payment exceeds amount | ✅ |
| `errormessagedialog/printer-error-dialog.fxml` | `PrinterErrorDialog.kt` | Printer issue | ✅ |

### Function Dialogs

| JavaFX FXML | Compose Dialog | Purpose | Status |
|-------------|----------------|---------|--------|
| `functiondialog/function-dialog.fxml` | `FunctionDialog.kt` | Function selection | ✅ |
| `requestfunctiondialog/request-function-dialog.fxml` | `RequestFunctionDialog.kt` | Function request | ✅ |
| `promptreceipt/prompt-receipt-dialog.fxml` | `PromptReceiptDialog.kt` | Receipt options | ✅ |

### Environment/Admin Dialogs

| JavaFX FXML | Compose Dialog | Purpose | Status |
|-------------|----------------|---------|--------|
| `changeenvironmentdialog/change-environment-dialog.fxml` | `AdminSettingsDialog.kt` | Hidden administration settings | ✅ |

> **Hidden Settings Access:** This dialog is accessed by clicking/swiping on "©Unisight BIT" copyright text on the Login Screen. See [SCREEN_LAYOUTS.md - Hidden Settings Menu](./SCREEN_LAYOUTS.md#hidden-settings-menu-administration-settings) for complete details.

---

## Complete Component Inventory

### Ten-Key Variants

| JavaFX FXML | Compose Component | Mode | Purpose |
|-------------|-------------------|------|---------|
| `tenkey/skin/ten-key-digit.fxml` | `TenKeyDigit.kt` | DIGIT | Standard numeric entry |
| `tenkey/skin/ten-key-login.fxml` | `TenKeyLogin.kt` | LOGIN | PIN entry |
| `tenkey/skin/ten-key-qty.fxml` | `TenKeyQuantity.kt` | QTY | Quantity entry |
| `tenkey/skin/ten-key-refund.fxml` | `TenKeyRefund.kt` | REFUND | Refund amounts |
| `tenkey/skin/ten-key-bag.fxml` | `TenKeyBag.kt` | BAG | Bag count |

### List Cell Components

| JavaFX FXML | Compose Component | Purpose |
|-------------|-------------------|---------|
| `orderlistview/order-cell.fxml` | `OrderListItem.kt` | Transaction line item |
| `customerscreenorderlistview/order-cell.fxml` | `CustomerOrderListItem.kt` | Customer-facing item |
| `paymentlistview/payment-cell.fxml` | `PaymentListItem.kt` | Applied payment |
| `recalllistview/recall-cell.fxml` | `RecallListItem.kt` | Held transaction |
| `employeelistview/employee-list-cell.fxml` | `EmployeeListItem.kt` | Employee selection |
| `lookuplistview/lookup-list-cell.fxml` | `LookupCategoryItem.kt` | Category button |
| `vendorlistview/vendor-list-cell.fxml` | `VendorListItem.kt` | Vendor selection |
| `taxlistview/tax-list-cell.fxml` | `TaxListItem.kt` | Tax breakdown row |
| `transactioninfolistview/transaction-info-order-cell.fxml` | `TransactionInfoItem.kt` | Transaction detail row |
| `findtransaction/find-transaction-cell.fxml` | `FindTransactionItem.kt` | Search result |
| `environmentlistview/environment-list-cell.fxml` | `EnvironmentListItem.kt` | Environment option |
| `accounttillitem/account-till-item.fxml` | `AccountTillItem.kt` | Account/till row |

### Grid Item Components

| JavaFX FXML | Compose Component | Purpose |
|-------------|-------------------|---------|
| `lookupitem/lookup-grid-item.fxml` | `LookupGridItem.kt` | Product in lookup grid |
| `returnitem/return-grid-item.fxml` | `ReturnGridItem.kt` | Returnable item card |
| `returnpaymentitem/return-payment-grid-item.fxml` | `ReturnPaymentItem.kt` | Return payment option |

### Info Components

| JavaFX FXML | Compose Component | Purpose |
|-------------|-------------------|---------|
| `employeeinfo/employee-info.fxml` | `EmployeeInfo.kt` | Horizontal employee badge |
| `employeeinfo/employee-info-vertical.fxml` | `EmployeeInfoVertical.kt` | Vertical employee badge |

### Panel Components

| JavaFX FXML | Compose Component | Purpose |
|-------------|-------------------|---------|
| `functioncontroller/skin/function-view.fxml` | `FunctionsPanel.kt` | Functions sidebar |
| `bagcontroller/skin/bag-product-quantity-view.fxml` | `BagQuantityPanel.kt` | Bag count entry |

### Receipt Components

| JavaFX FXML | Compose Component | Purpose |
|-------------|-------------------|---------|
| `receipt/receipt-view.fxml` | `ReceiptView.kt` | Receipt preview |
| `receipt/product/product-receipt-item.fxml` | `ReceiptProductItem.kt` | Receipt line item |
| `receipt/total/total-receipt-item.fxml` | `ReceiptTotalItem.kt` | Receipt totals |

### Keyboard Components

| JavaFX FXML | Compose Component | Purpose |
|-------------|-------------------|---------|
| `core/control/keyboard/skin/keyboard_pane.fxml` | `KeyboardPane.kt` | Full QWERTY keyboard |
| `core/control/keyboard/skin/digit.fxml` | `DigitKey.kt` | Single digit key |
| `core/control/keyboard/skin/digit-login.fxml` | `DigitKeyLogin.kt` | Login digit key |
| `core/control/keyboard/skin/digit-decimal.fxml` | `DigitKeyDecimal.kt` | Decimal key |
| `core/control/keyboard/key/shiftable_key.fxml` | `ShiftableKey.kt` | Shift-capable key |
| `core/control/keyboard/key/non_shiftable_key.fxml` | `NonShiftableKey.kt` | Static key |
| `core/control/keyboard/key/modifier_key.fxml` | `ModifierKey.kt` | Shift/Ctrl key |

---

## CSS to Compose Theme Mapping

### Global Stylesheets

| JavaFX CSS | Compose File | Purpose |
|------------|--------------|---------|
| `layout/css/main.css` | `Theme.kt` | Base theme |
| `layout/css/responsive.css` | `WindowSizeClass.kt` | Responsive breakpoints |
| `layout/css/header.css` | `HeaderStyles.kt` | Header styling |
| `layout/css/header-1024.css` | `HeaderStyles.kt` | Header compact mode |
| `layout/css/max-width-1100.css` | `CompactLayout.kt` | Compact responsive |
| `layout/css/max-width-1800.css` | `MediumLayout.kt` | Medium responsive |

### Screen-Specific Styles

| JavaFX CSS | Screen | Compose Location |
|------------|--------|------------------|
| `page/home/css/home.css` | Home | `HomeScreen.kt` modifiers |
| `page/pay/css/pay.css` | Payment | `PayScreen.kt` modifiers |
| `page/login/css/login.css` | Login | `LoginScreen.kt` modifiers |
| `page/lock/css/lock.css` | Lock | `LockScreen.kt` modifiers |
| `page/customerscreen/css/customer-screen.css` | Customer | `CustomerScreen.kt` modifiers |
| `page/returnitem/css/return-item.css` | Returns | `ReturnItemScreen.kt` modifiers |
| `page/cashpickup/css/cash-pickup.css` | Cash Pickup | `CashPickupScreen.kt` modifiers |
| `page/vendorpayout/css/vendor-payout.css` | Vendor Payout | `VendorPayoutScreen.kt` modifiers |
| `page/report/css/report.css` | Reports | `ReportScreen.kt` modifiers |

---

## Color Mapping (CSS → Compose)

| CSS Variable/Class | Hex Value | Compose Color |
|--------------------|-----------|---------------|
| `.left-block` background | `#E1E3E3` | `BackgroundLight` |
| `.right-block` background | `#EFF1F1` | `BackgroundRight` |
| `.list-view` background | `#FFFFFF` | `Surface` |
| `.btn-success` background | `#04571B` | `SuccessGreen` |
| `.back-button` border | `#857370` | `BorderBrown` |
| `.delete-cancel` background | `#FA1B1B` | `DangerRed` |
| `.secondary` border | `#857370` | `BorderBrown` |
| `.hover-block` background | `#000000B2` | `OverlayBlack` (70% opacity) |
| `.whiteBox` background | `#FFFFFF` | `Surface` |
| `.greenBox` background | `#04571B` | `SuccessGreen` |
| Payment tab active | `#2073BE` | `PrimaryBlue` |
| Text primary | `#000000` | `OnSurface` |
| Text secondary | `#857370` | `TextSecondary` |
| Error text | `#FA1B1B` | `DangerRed` |

---

## Typography Mapping (CSS → Compose)

| CSS Class | Font Size | Compose TextStyle |
|-----------|-----------|-------------------|
| `.pay-value-field` | 24sp | `Typography.titleLarge` |
| `.b, h3` | 18sp bold | `Typography.titleMedium` |
| `.white-box-label` | 16sp | `Typography.bodyLarge` |
| `.product-name` | 16sp bold | `Typography.titleSmall` |
| `.info-details-text` | 14sp | `Typography.bodyMedium` |
| `.subtotal_text` | 16sp | `Typography.bodyLarge` |
| `.subtotal_value` | 18sp bold | `Typography.titleMedium` |
| `.station-text` | 20sp | `Typography.headlineSmall` |
| `.header-text` | 22sp | `Typography.headlineMedium` |
| `.dialog-header-text` | 18sp | `Typography.titleMedium` |
| `.saving_value` | 18sp bold | `Typography.titleMedium` |

---

## Spacing & Dimensions

| CSS Property | Value | Compose |
|--------------|-------|---------|
| `.left-block` padding | 38dp | `Modifier.padding(38.dp)` |
| `.right-block` padding | 0 48 8 48 | `Modifier.padding(start=48.dp, end=48.dp, bottom=8.dp)` |
| Button border-radius | 20dp | `RoundedCornerShape(20.dp)` |
| List border-radius | 16dp | `RoundedCornerShape(16.dp)` |
| Button padding | 18 24 | `Modifier.padding(18.dp, 24.dp)` |
| Grid spacing | 20dp | `Arrangement.spacedBy(20.dp)` |

---

## Layout Ratios

| Screen | Layout | Ratios |
|--------|--------|--------|
| Home | HBox → GridPane | 70% / 30% |
| Pay | HBox | 60% / 40% |
| Login | GridPane | 50% / 50% |
| Lock | GridPane | 50% / 50% |
| Customer (Landscape) | HBox | 60% / 40% |
| Customer (Portrait) | VBox | Stacked |
| Returns | HBox | 70% / 30% |
| Lookup Dialog | GridPane | Categories list / Product grid |

---

## Related Documentation

- **[UI_DESIGN_SYSTEM.md](./UI_DESIGN_SYSTEM.md)** - Complete design tokens
- **[SCREEN_LAYOUTS.md](./SCREEN_LAYOUTS.md)** - Screen wireframes
- **[COMPONENTS.md](./COMPONENTS.md)** - Component specifications
- **[DIALOGS.md](./DIALOGS.md)** - Dialog specifications
- **[CUSTOMER_SCREEN.md](./CUSTOMER_SCREEN.md)** - Customer display
- **[COMPOSE_IMPLEMENTATION.md](./COMPOSE_IMPLEMENTATION.md)** - Technical patterns

---

## Implementation Checklist

When implementing a screen:

1. ✅ Reference the corresponding FXML file for structure
2. ✅ Extract colors from CSS and map to Compose theme
3. ✅ Map JavaFX layout (HBox/VBox/GridPane) to Compose (Row/Column/Box)
4. ✅ Convert JavaFX bindings to StateFlow observations
5. ✅ Map event handlers to ViewModel functions
6. ✅ Apply responsive modifiers based on window size
7. ✅ Verify font sizes and spacing match CSS
8. ✅ Test on all target resolutions (1024x768, 1920x1080)

