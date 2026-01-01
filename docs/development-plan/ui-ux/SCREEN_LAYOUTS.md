# GroPOS Screen Layouts

> Detailed documentation of all application screens and their layouts

## Table of Contents

- [Login Screen](#login-screen)
- [Home Screen (Transaction)](#home-screen-transaction)
- [Payment Screen](#payment-screen)
- [Customer Screen](#customer-screen)
- [Lock Screen](#lock-screen)
- [Functions Panel](#functions-panel)
- [Dialogs and Modals](#dialogs-and-modals)
- [Reports](#reports)

---

## Login Screen

**Composable:** `LoginScreen.kt`  
**ViewModel:** `LoginViewModel.kt`  
**Layout:** 50/50 horizontal split

### Structure

```
┌─────────────────────────────────────────────────────────────────┐
│                         LOGIN SCREEN                             │
├────────────────────────────┬────────────────────────────────────┤
│                            │                                     │
│      LEFT SECTION          │        RIGHT SECTION               │
│      (Branding)            │        (Authentication)            │
│                            │                                     │
│  ┌─ Station Name ─────┐    │    ┌── QR Registration ──┐         │
│  │ Station 4          │    │    │ Welcome to GroPOS   │         │
│  └────────────────────┘    │    │ [QR Code Image]     │         │
│                            │    │ Activation Code     │         │
│  ┌─ Logo Area ────────┐    │    └────────────────────┘         │
│  │                    │    │                                     │
│  │   [Company Logo]   │    │    ┌── Employee List ────┐         │
│  │                    │    │    │ On Site Cashiers    │         │
│  └────────────────────┘    │    │ [Employee 1]        │         │
│                            │    │ [Employee 2]        │         │
│  ┌─ Time ─────────────┐    │    │ [Employee 3]        │         │
│  │ 12:30 PM           │    │    └────────────────────┘         │
│  └────────────────────┘    │                                     │
│                            │    ┌── PIN Entry ────────┐         │
│  ┌─ Footer ───────────┐    │    │ [NFC Icon]          │         │
│  │ ©Unisight BIT 2024 │    │    │ [Ten-Key Pad]       │         │
│  └────────────────────┘    │    │ [Sign In Button]    │         │
│                            │    └────────────────────┘         │
├────────────────────────────┴────────────────────────────────────┤
│ [Keypad Button]              [NFC Switch]       [Back Button]   │
└─────────────────────────────────────────────────────────────────┘
```

### States

1. **Splash State** - Initial loading with "Welcome to GroPOS"
2. **Registration State** - QR code for device activation
3. **Employee Selection** - List of cashiers to select
4. **PIN Entry** - Ten-key pad for PIN authentication
5. **NFC Entry** - Hardware token authentication

### Key Elements

| Element | State Property | Description |
|---------|----------------|-------------|
| Station Name | `stationName` | Display station identifier |
| Time | `currentTime` | Current time display |
| QR Image | `qrCodeBitmap` | Device registration QR code |
| Activation Code | `activationCode` | Manual activation code |
| Employee List | `employees` | Scrollable cashier list |
| Ten-Key | `TenKeyComponent` | PIN entry keypad |
| Auth Field | `pinInput` | Password input (hidden) |

---

## Home Screen (Transaction)

**Composable:** `HomeScreen.kt`  
**ViewModel:** `HomeViewModel.kt`  
**Layout:** 70/30 horizontal split

### Structure

```
┌─────────────────────────────────────────────────────────────────┐
│                         HOME SCREEN                              │
├──────────────────────────────────────────┬──────────────────────┤
│           LEFT SECTION (70%)              │   RIGHT SECTION (30%)│
│                                          │                      │
│  ┌── Info Bar ──────────────────────┐    │  ┌── Totals ───────┐ │
│  │ [Customer] [Weight] [Quantity]   │    │  │ Items: 5        │ │
│  └──────────────────────────────────┘    │  │ Grand Total: $X │ │
│                                          │  │                 │ │
│  ┌── Order List ────────────────────┐    │  │ [Pay Button]    │ │
│  │                                   │    │  └─────────────────┘ │
│  │  [Product 1]    Qty x Price      │    │                      │
│  │  [Product 2]    Qty x Price      │    │  ┌── Ten-Key ──────┐ │
│  │  [Product 3]    Qty x Price      │    │  │ [7] [8] [9]     │ │
│  │  [Product 4]    Qty x Price      │    │  │ [4] [5] [6]     │ │
│  │  ...                              │    │  │ [1] [2] [3]     │ │
│  │                                   │    │  │ [Qty] [0] [OK]  │ │
│  │                                   │    │  └─────────────────┘ │
│  │                                   │    │                      │
│  └───────────────────────────────────┘    │  ┌── Actions ──────┐ │
│                                          │  │[Lookup][Recall]  │ │
│                                          │  │   [Functions]    │ │
│                                          │  └─────────────────┘ │
├──────────────────────────────────────────┴──────────────────────┤
│                     (Overlay Panels - Hidden by Default)         │
│  [Price Prompt] [Manager Approval] [Functions] [Bag Quantity]   │
└─────────────────────────────────────────────────────────────────┘
```

### Info Bar Components

```
┌─────────────────────────────────────────────────────────────────┐
│ ┌─ Customer ─────┐  ┌─ Weight ───────┐  ┌─ Quantity ─────────┐ │
│ │ [Avatar]       │  │ Weight: 0.0 lb │  │ Quantity: 1        │ │
│ │ Valuable       │  │                │  │                    │ │
│ │ Customer       │  │                │  │                    │ │
│ │ [Search]       │  │                │  │                    │ │
│ └────────────────┘  └────────────────┘  └────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

### Order Cell Layout

```
┌─────────────────────────────────────────────────────────────────┐
│ [Qty] [Img]    Product Name                    [%]    $Price    │
│  2           Description Line                         $XX.XX    │
│  [Sale]      $Unit @ Qty  Tax+CRV              [Disc]           │
│              "You saved $X.XX"                                  │
└─────────────────────────────────────────────────────────────────┘
Columns: 10% | 10% | 60% | 5% | 15%
```

### Modification Mode

When item is selected, right panel switches:

```
┌── Modification Mode ──────┐
│  [Back]      [Quantity]   │
│             [Discount]    │
│             [Price Change]│
├───────────────────────────┤
│     [Ten-Key Pad]         │
│    (Mode-specific)        │
├───────────────────────────┤
│ [Remove Item]             │
│ [More Information]        │
└───────────────────────────┘
```

### Key Elements

| Element | State Property | Purpose |
|---------|----------------|---------|
| Order List | `orderItems` | Transaction line items |
| Ten-Key | `TenKeyComponent` | Numeric entry |
| Quantity | `currentQuantity` | Current quantity multiplier |
| Weight | `currentWeight` | Scale weight display |
| Pay Button | `onPayClick` | Navigate to payment |
| Grand Total | `grandTotal` | Running transaction total |
| Item Count | `itemCount` | Number of items |
| Functions | `onFunctionsClick` | Open functions panel |
| Lookup | `onLookupClick` | Product lookup dialog |
| Recall | `onRecallClick` | Hold/Recall transactions |

---

## Payment Screen

**Composable:** `PayScreen.kt`  
**ViewModel:** `PayViewModel.kt`  
**Layout:** Horizontal split with payment types

### Structure

```
┌─────────────────────────────────────────────────────────────────┐
│ [Return to Items]                        [Hold Transaction]     │
├──────────────────────────────────────────┬──────────────────────┤
│         LEFT SECTION                      │    RIGHT SECTION     │
│                                          │                      │
│  ┌── Summary ─────────────────────┐      │ [CHARGE][EBT][CASH]  │
│  │                                │      │ [OTHER]              │
│  │  Item Subtotal        $XX.XX   │      │                      │
│  │  Discount             -$X.XX   │      │ ┌── Credit ────────┐ │
│  │  ─────────────────────────     │      │ │[Credit] [Debit]  │ │
│  │  Subtotal             $XX.XX   │      │ └─────────────────┘ │
│  │  Bottle Fee            $X.XX   │      │                      │
│  │  Bag Fee               $X.XX   │      │ ┌── EBT ──────────┐ │
│  │  ─────────────────────────     │      │ │[Food Stamp]     │ │
│  │  Tax 8.5%              $X.XX   │      │ │[EBT Cash]       │ │
│  │  ─────────────────────────     │      │ │[Balance Check]  │ │
│  │  Items: 5       Total $XX.XX   │      │ └─────────────────┘ │
│  └────────────────────────────────┘      │                      │
│                                          │ ┌── Cash ─────────┐ │
│  ┌── SNAP Eligible ───────────────┐      │ │[$1][$5][$10][$20│ │
│  │  Food Stamp Eligible   $XX.XX  │      │ │[$50][$100][Amt] │ │
│  └────────────────────────────────┘      │ └─────────────────┘ │
│                                          │                      │
│  ┌── Payments Applied ────────────┐      │ ┌── Other ────────┐ │
│  │  [Payment 1]           $XX.XX  │      │ │[Check]          │ │
│  │  [Payment 2]           $XX.XX  │      │ │[On Account]     │ │
│  │  ──────────────────────────    │      │ └─────────────────┘ │
│  │  Remaining             $XX.XX  │      │                      │
│  └────────────────────────────────┘      │ ┌── Ten-Key ──────┐ │
│                                          │ │ [7] [8] [9]     │ │
│                                          │ │ [4] [5] [6]     │ │
│                                          │ │ [1] [2] [3]     │ │
│                                          │ │ [.] [0] [OK]    │ │
│                                          │ └─────────────────┘ │
└──────────────────────────────────────────┴──────────────────────┘
```

### Payment Methods

1. **Charge Tab** - Credit/Debit card payments
2. **EBT Tab** - Food Stamp, EBT Cash, Balance Check
3. **Cash Tab** - Quick cash buttons ($1, $5, $10, $20, $50, $100)
4. **Other Tab** - Check, On Account

### Transaction Progress Overlay

```
┌── Transaction Details ──────────┐
│ [✓] Pinpad Request Sent         │
│ [✓] Waiting on Customer         │
│ [○] Transaction Approved        │
│                                 │
│      [Cancel Payment]           │
└─────────────────────────────────┘
```

### Key Elements

| Element | State Property | Purpose |
|---------|----------------|---------|
| Subtotal | `subtotal` | Pre-tax total |
| Grand Total | `grandTotal` | Final amount due |
| Remaining | `remainingAmount` | Amount still owed |
| Tax List | `taxBreakdown` | Breakdown by tax type |
| Payment List | `appliedPayments` | Applied payments |
| Food Stamp Eligible | `snapEligibleAmount` | SNAP eligible amount |
| Credit Button | `onCreditClick` | Credit card payment |
| Debit Button | `onDebitClick` | Debit card payment |

---

## Customer Screen

**Composable:** `CustomerScreen.kt`  
**ViewModel:** `CustomerScreenViewModel.kt`  
**Purpose:** Secondary display for customer viewing

### Structure

```
┌─────────────────────────────────────────────────────────────────┐
│ [Store Name]     [Savings: $X.XX]     [Weight: 0.0 lb]         │
├────────────────────────────────────────┬────────────────────────┤
│                                        │                        │
│         ORDER ITEMS                    │      TOTALS            │
│                                        │                        │
│  ┌─────────────────────────────────┐   │  Subtotal     $XX.XX   │
│  │  [Product 1]         $XX.XX     │   │  ─────────────────     │
│  │  [Product 2]         $XX.XX     │   │  Sales Tax     $X.XX   │
│  │  [Product 3]         $XX.XX     │   │  ─────────────────     │
│  │  [Product 4]         $XX.XX     │   │  Bag Fee       $X.XX   │
│  │  ...                            │   │  ─────────────────     │
│  │                                 │   │  Items: X  Total $X.XX │
│  │                                 │   │                        │
│  │                                 │   │  ┌── SNAP ───────────┐ │
│  │                                 │   │  │ SNAP Eligible $X  │ │
│  │                                 │   │  │ Remaining   $X.XX │ │
│  │                                 │   │  │ Change Due  $X.XX │ │
│  │                                 │   │  └───────────────────┘ │
│  │                                 │   │                        │
│  │                                 │   │  ┌── Advertisement ──┐ │
│  └─────────────────────────────────┘   │  │                   │ │
│                                        │  │   [Ad Image]      │ │
│                                        │  │                   │ │
│                                        │  └───────────────────┘ │
└────────────────────────────────────────┴────────────────────────┘
```

### Advertisement Overlay

Full-screen ad display when transaction is idle:
- Image advertisements
- Animated discount labels
- Brand promotion scrolling text

---

## Lock Screen

**Composable:** `LockScreen.kt`  
**ViewModel:** `LockViewModel.kt`  
**Purpose:** Screen lock when inactive

### Features

- Station ID display
- Time display
- PIN/NFC unlock
- Manager override option

---

## Functions Panel

**Composable:** `FunctionsPanel.kt`  
**ViewModel:** `FunctionsViewModel.kt`  
**Purpose:** Access to non-transaction operations

### Structure

```
┌── Functions Panel ──────────────────────┐
│                                         │
│  [RECALL]  [PAYMENTS]  [TILL]           │
│                                         │
│  ┌── Recall Section ──────────────────┐ │
│  │  [Return/Invoice]                   │ │
│  │  [Pullback]                         │ │
│  │  [Void Transaction]                 │ │
│  │  [Print Last Receipt]               │ │
│  │  [Run Test]                         │ │
│  └─────────────────────────────────────┘ │
│                                         │
│  ┌── Payments Section ────────────────┐ │
│  │  [Vendor Payout]                    │ │
│  │  [Cash Pickup]                      │ │
│  │  [Lotto Pay]                        │ │
│  │  [Discount]                         │ │
│  └─────────────────────────────────────┘ │
│                                         │
│  ┌── Till Section ────────────────────┐ │
│  │  [Open Drawer]                      │ │
│  │  [Price Check]                      │ │
│  │  [Add Cash]                         │ │
│  │  [EBT Balance]                      │ │
│  └─────────────────────────────────────┘ │
│                                         │
│            [Sign Out]                   │
│            [Back]                       │
└─────────────────────────────────────────┘
```

### Available Functions

| Tab | Function | Description |
|-----|----------|-------------|
| Recall | Return/Invoice | Access return transactions |
| Recall | Pullback | Process item returns |
| Recall | Void Transaction | Cancel current transaction |
| Recall | Print Last Receipt | Reprint previous receipt |
| Payments | Vendor Payout | Pay vendors from till |
| Payments | Cash Pickup | Remove cash for deposit |
| Payments | Lotto Pay | Lottery winnings payout |
| Payments | Discount | Apply transaction discount |
| Till | Open Drawer | Manual drawer open |
| Till | Price Check | Scan item for price |
| Till | Add Cash | Add cash to drawer |
| Till | EBT Balance | Check EBT card balance |

---

## Dialogs and Modals

### Product Lookup Dialog

```
┌── Product Lookup ───────────────────────────────────────────────┐
│                                                                  │
│  ┌── Categories ──┐  ┌── Products Grid ────────────────────────┐│
│  │  [Category 1]  │  │  [Prod]  [Prod]  [Prod]  [Prod]         ││
│  │  [Category 2]  │  │  [Prod]  [Prod]  [Prod]  [Prod]         ││
│  │  [Category 3]  │  │  [Prod]  [Prod]  [Prod]  [Prod]         ││
│  │  [Category 4]  │  │  ...                                    ││
│  │  ...           │  │                                          ││
│  └────────────────┘  └──────────────────────────────────────────┘│
│                                                                  │
│                    [Close]                                       │
└──────────────────────────────────────────────────────────────────┘
```

### Manager Approval Dialog

```
┌── Manager Approval ─────────────────────┐
│                                         │
│  Request Type: LINE_DISCOUNT            │
│                                         │
│  [Employee List]                        │
│  ┌──────────────────────────────────┐  │
│  │ [Employee 1 - Manager]           │  │
│  │ [Employee 2 - Supervisor]        │  │
│  └──────────────────────────────────┘  │
│                                         │
│  [Ten-Key for PIN]                      │
│                                         │
│  [Back]              [Approve]          │
└─────────────────────────────────────────┘
```

### Error Dialog

```
┌── Error ────────────────────────────────┐
│                                         │
│           [Error Icon]                  │
│                                         │
│       Error Message Text                │
│       (Large, centered)                 │
│                                         │
│              [OK]                       │
└─────────────────────────────────────────┘
```

### Age Verification Dialog

For age-restricted products (tobacco, alcohol):
- Displays required age
- Calculates from birthdate
- Accept/Reject options

### Hold Dialog

```
┌── Hold Transaction ─────────────────────┐
│                                         │
│  Recall Name: [________________]        │
│                                         │
│  [Hold]   [Hold & Print]   [Cancel]     │
└─────────────────────────────────────────┘
```

---

## Reports

### Order Report Screen

**Composable:** `OrderReportScreen.kt`

Transaction history and reporting interface with:
- Date range selection
- Transaction filtering
- Export options

### Order Product Report Screen

**Composable:** `OrderProductReportScreen.kt`

Detailed product-level reporting with:
- Sales by product
- Category breakdown
- Time-based analysis
