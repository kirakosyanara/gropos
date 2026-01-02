# GroPOS Localization & i18n Reference

> Complete internationalization (i18n) guide and string catalog for multi-language support

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Complete String Catalog](#complete-string-catalog)
- [Language Files](#language-files)
- [Kotlin Compose Implementation](#kotlin-compose-implementation)
- [Adding New Languages](#adding-new-languages)
- [Best Practices](#best-practices)

---

## Overview

GroPOS supports multiple languages using Java ResourceBundle for desktop and Android string resources for mobile. All user-facing text is externalized to properties files.

### Supported Languages (12 Total)

| Language | Locale | File | Status |
|----------|--------|------|--------|
| English (US) | `en_US` | `AppStrings.properties` | ✅ Complete (Default) |
| Arabic | `ar_US` | `AppStrings_ar_US.properties` | ✅ Complete |
| Spanish (US) | `es_US` | `AppStrings_es_US.properties` | ✅ Complete |
| Farsi/Persian | `fa_US` | `AppStrings_fa_US.properties` | ✅ Complete |
| French | `fr_US` | `AppStrings_fr_US.properties` | ✅ Complete |
| Hindi | `hi_US` | `AppStrings_hi_US.properties` | ✅ Complete |
| Armenian | `hy_US` | `AppStrings_hy_US.properties` | ✅ Complete |
| Korean | `ko_US` | `AppStrings_ko_US.properties` | ✅ Complete |
| Russian | `ru_US` | `AppStrings_ru_US.properties` | ✅ Complete |
| Tagalog | `tl_US` | `AppStrings_tl_US.properties` | ✅ Complete |
| Vietnamese | `vi_US` | `AppStrings_vi_US.properties` | ✅ Complete |
| Chinese (Simplified) | `zh_US` | `AppStrings_zh_US.properties` | ✅ Complete |

### File Location

```
Desktop (JVM):
app/src/main/resources/i18n/
├── AppStrings.properties           # English (default)
├── AppStrings_ar_US.properties     # Arabic
├── AppStrings_es_US.properties     # Spanish
├── AppStrings_fa_US.properties     # Farsi/Persian
├── AppStrings_fr_US.properties     # French
├── AppStrings_hi_US.properties     # Hindi
├── AppStrings_hy_US.properties     # Armenian
├── AppStrings_ko_US.properties     # Korean
├── AppStrings_ru_US.properties     # Russian
├── AppStrings_tl_US.properties     # Tagalog
├── AppStrings_vi_US.properties     # Vietnamese
└── AppStrings_zh_US.properties     # Chinese

Android:
app/src/main/res/
├── values/strings.xml              # English (default)
├── values-ar/strings.xml           # Arabic
├── values-es/strings.xml           # Spanish
├── values-fa/strings.xml           # Farsi/Persian
├── values-fr/strings.xml           # French
├── values-hi/strings.xml           # Hindi
├── values-hy/strings.xml           # Armenian
├── values-ko/strings.xml           # Korean
├── values-ru/strings.xml           # Russian
├── values-tl/strings.xml           # Tagalog
├── values-vi/strings.xml           # Vietnamese
└── values-zh/strings.xml           # Chinese
```

---

## Architecture

### Current Java Implementation

```java
// I18n.java
package com.unisight.gropos.utils;

import java.util.Locale;
import java.util.ResourceBundle;

public class I18n {
    private static ResourceBundle bundle = ResourceBundle.getBundle("i18n.AppStrings", Locale.getDefault());
    
    public static String tr(String key) {
        try {
            return bundle.getString(key);
        } catch (Exception e) {
            return key; // Fallback to key if not found
        }
    }
    
    public static String tr(String key, Object... args) {
        String template = tr(key);
        return String.format(template, args);
    }
    
    public static void setLocale(Locale locale) {
        bundle = ResourceBundle.getBundle("i18n.AppStrings", locale);
    }
}
```

### Kotlin Compose Multiplatform Implementation

```kotlin
// commonMain/kotlin/com/unisight/gropos/i18n/Strings.kt

expect object Strings {
    fun get(key: String): String
    fun format(key: String, vararg args: Any): String
    fun setLocale(locale: String)
    fun getCurrentLocale(): String
    fun getAvailableLocales(): List<String>
}

// StringKeys.kt - Type-safe key constants
object StringKeys {
    // Buttons
    const val ADD_CASH_BUTTON = "add.cash.button"
    const val BACK_BUTTON = "back.button"
    const val CANCEL_BUTTON = "cancel.button"
    const val CANCEL_PAYMENT = "cancel.payment"
    const val CASH_PICKUP_BUTTON = "cash.pickup.button"
    const val EBT_BALANCE_BUTTON = "ebt.balance.button"
    const val FORCE_SALE = "force.sale"
    const val LOGIN_KEYPAD_BUTTON = "login.keypad.button"
    const val LOTTO_PAY_BUTTON = "lotto.pay.button"
    const val NO_BUTTON = "no.button"
    const val OK_BUTTON = "ok.button"
    const val OPEN_DRAWER_BUTTON = "open.drawer.button"
    const val PRICE_CHECK_BUTTON = "price.check.button"
    const val PRINT_LAST_RECEIPT = "print.last.receipt"
    const val PROCEED_TO_PAYMENT = "proceed.to.payment"
    const val PULLBACK_BUTTON = "pullback.button"
    const val RECALL_RETURN_BUTTON = "recall.return.button"
    const val RUN_TEST = "run.test"
    const val SIGN_OUT_BUTTON = "sign.out.button"
    const val TRY_AGAIN_BUTTON = "try.again.button"
    const val VENDOR_PAYOUT_BUTTON = "vendor.payout.button"
    const val VOID_TRANSACTION_BUTTON = "void.transaction.button"
    const val YES_BUTTON = "yes.button"
    
    // Labels
    const val BAG_FEE = "bag.fee"
    const val CASH = "cash"
    const val CHANGE_DUE = "change.due"
    const val CHARGE = "charge"
    const val CUSTOMER_VIEW_SAVINGS = "customer.view.savings"
    const val DISCOUNT = "discount"
    const val EBT = "ebt"
    const val SNAP_ELIGIBLE = "snap.eligible"
    const val FOOD_STAMP_ELIGIBLE = "food.stamp.eligible"
    const val GRAND_TOTAL = "grand.total"
    const val INVOICE_NUMBER = "invoice.number"
    const val ITEM_SUBTOTAL = "item.subtotal"
    const val ITEMS_TEXT = "items.text"
    const val OTHER = "other"
    const val PAY_AMOUNT = "pay.amount"
    const val PAYMENT_AMOUNT = "payment.amount"
    const val REMAINING = "remaining"
    const val SALES_TAX = "sales.tax"
    const val SOLD_BY_WEIGHT = "sold.by.weight"
    const val SUBTOTAL = "subtotal"
    const val TOTAL = "total"
    const val VENDOR_NAME = "vendor.name"
    
    // Headers
    const val ADD_CASH_DIALOG_HEADER = "add.cash.dialog.header"
    const val INFO_HEADER = "info.header"
    const val PAYMENTS_HEADER = "payments.header"
    const val RECALL_HEADER = "recall.header"
    const val TILL_HEADER = "till.header"
    
    // Input Prompts
    const val ENTER_AMOUNT = "enter.amount"
    const val ENTER_INVOICE_NUMBER = "enter.invoice.number"
    const val ENTER_PRODUCT_WEIGHT = "enter.product.weight"
    const val ENTER_QUANTITY = "enter.quantity"
    const val PRICE_REQUIRED = "price.required"
    const val QUANTITY_REQUIRED = "quantity.required"
    
    // Informational Messages
    const val CHOOSE_VENDOR_FROM_RECENT = "choose.a.vendor.from.recent"
    const val VENDOR_SHIPMENT_INFO = "if.the.vendor.or.their.shipment"
    const val PINPAD_REQUEST_SEND = "pinpad.request.send"
    const val PLACE_PRODUCT_ON_SCALE = "place.product.on.scale"
    const val PLEASE_ENTER_PRICE = "please.enter.the.price"
    const val PLEASE_SELECT_VENDOR = "please.select.vendor"
    const val PRINT_RECEIPT = "print.receipt"
    const val RECEIVED_BY_INFO = "received.by.info"
    const val TRANSACTION_APPROVED = "transaction.approved"
    const val WAITING_ON_CUSTOMER = "waiting.on.customer"
    
    // Error Messages
    const val AMOUNT_IS_BIGGER = "amount.is.bigger"
    const val AMOUNT_IS_BIGGER_THAN_MAX = "amount.is.bigger.than.max"
    const val CLOSE_DRAWER_TO_CONTINUE = "close.drawer.to.continue"
    const val COMPLETE_TRANSACTION_MESSAGE = "complete.transaction.message"
    const val DRAWER_OPEN = "drawer.open"
    const val ERROR_QUANTITY_CHANGE = "error.quantity.change.not.allowed"
    const val ITEM_IS_NOT_FOR_SALE = "item.is.not.for.sale"
    const val ITEM_NOT_FOUND = "item.not.found"
    const val MINIMUM_WEIGHT_MESSAGE = "minimum.weight.message"
    const val PINPAD_COMMUNICATION_ERROR = "pinpad.communication.error"
    const val PULLBACK_NOT_ELIGIBLE = "pullback.not.eligible"
    const val SCALE_INFO = "scale.info"
    const val SCALE_NOT_READY = "scale.not.ready"
    const val SIGN_OUT_MESSAGE = "sign.out.message"
    const val WEIGHT_OVERWEIGHT = "weight.overweight"
    const val WEIGHT_UNDER_ZERO = "weight.under.zero"
}
```

---

## Complete String Catalog

### English (AppStrings_en.properties)

The complete set of strings currently in the application:

```properties
# ═══════════════════════════════════════════════════════════════════════════════
# BUTTONS
# ═══════════════════════════════════════════════════════════════════════════════

add.cash.button = Add Cash
back.button = Back
cancel.button = Cancel
cancel.payment = Cancel Payment
cash.pickup.button = Cash Pickup
ebt.balance.button = SNAP Balance
force.sale = Force Sale
login.keypad.button = Use Keypad
lotto.pay.button = Lotto Pay
no.button = No
ok.button = OK
open.drawer.button = Open Drawer
price.check.button = Price Check
print.last.receipt = Print Last Receipt
proceed.to.payment = Proceed to Payment
pullback.button = Pullback
recall.return.button = Recall / Return
run.test = Run Test
sign.out.button = Sign Out
try.again.button = Try Again
vendor.payout.button = Vendor Payout
void.transaction.button = VOID Transaction
yes.button = Yes

# ═══════════════════════════════════════════════════════════════════════════════
# DIALOG HEADERS
# ═══════════════════════════════════════════════════════════════════════════════

add.cash.dialog.header = Add Cash
info.header = Info
payments.header = Payments
recall.header = Recall
till.header = Till

# ═══════════════════════════════════════════════════════════════════════════════
# LABELS AND DISPLAY TEXT
# ═══════════════════════════════════════════════════════════════════════════════

bag.fee = Bag Fee
cash = Cash
change.due = Change Due
charge = Charge
customer.view.savings = Your Savings
discount = Discount
ebt = SNAP
food.stamp.eligible = SNAP Eligible:
grand.total = Grand Total:
invoice.number = Invoice Number
item.subtotal = Item Subtotal
items.text = Items:
other = Other
pay.amount = Pay Amount
payment.amount = Payout Amount
remaining = Remaining
sales.tax = Sales Tax(s)
snap.eligible = SNAP Eligible
sold.by.weight = Sold By Weight
subtotal = Subtotal
total = Total:
vendor.name = Vendor Name

# ═══════════════════════════════════════════════════════════════════════════════
# INPUT PROMPTS
# ═══════════════════════════════════════════════════════════════════════════════

enter.amount = Enter Amount
enter.invoice.number = Enter Invoice Number
enter.product.weight = Please enter the weight for this product. \n Weight should be greater than 10 lbs.
enter.quantity = Enter Quantity
price.required = Price Required
quantity.required = Quantity Required

# ═══════════════════════════════════════════════════════════════════════════════
# INFORMATIONAL MESSAGES
# ═══════════════════════════════════════════════════════════════════════════════

choose.a.vendor.from.recent = Choose a Vendor from recent shipments or alphabetically.
if.the.vendor.or.their.shipment = If the vendor or their shipment does not appear, the vendor must see a manager.
pinpad.request.send = Pinpad Request Sent
place.product.on.scale = Place the product on a scale.
please.enter.the.price = Please enter the price for this product.
please.select.vendor = Please select vendor
print.receipt = Print Receipt ?
received.by.info = Received By info
transaction.approved = Transaction Approved
waiting.on.customer = Waiting on Customer

# ═══════════════════════════════════════════════════════════════════════════════
# ERROR MESSAGES
# ═══════════════════════════════════════════════════════════════════════════════

amount.is.bigger = Amount is bigger than your available balance
amount.is.bigger.than.max = Amount is bigger than the max available cash payment for this vendor.
close.drawer.to.continue = Close drawer to continue
complete.transaction.message = Please complete the transaction
drawer.open = Drawer open
error.quantity.change.not.allowed = Quantity change isn't permitted for weighed items.
item.is.not.for.sale = Item Is Not For Sale
item.not.found = Item Not Found
minimum.weight.message = The minimum weight should be 10 lbs.
pinpad.communication.error = PinPad communication error please restart the device and try again.
pullback.not.eligible = Pullback is not eligible for this transaction.
scale.info = Scale info
scale.not.ready = Scale is not ready
sign.out.message = Please void or complete the transaction before Sign Out
weight.overweight = Weight Overweight
weight.under.zero = Weight Under Zero
```

---

## Language Files

> **Note:** All 12 language translation files are complete and available in the codebase at:
> `app/src/main/resources/i18n/` (build output: `app/build/resources/main/i18n/`)
> 
> Below are sample translations for reference. For complete translations, refer to the source files.

### All Available Languages

| Language | File | Native Name | Script Direction |
|----------|------|-------------|------------------|
| English | `AppStrings.properties` | English | LTR |
| Arabic | `AppStrings_ar_US.properties` | العربية | RTL |
| Spanish | `AppStrings_es_US.properties` | Español | LTR |
| Farsi/Persian | `AppStrings_fa_US.properties` | فارسی | RTL |
| French | `AppStrings_fr_US.properties` | Français | LTR |
| Hindi | `AppStrings_hi_US.properties` | हिन्दी | LTR |
| Armenian | `AppStrings_hy_US.properties` | Հայdelays | LTR |
| Korean | `AppStrings_ko_US.properties` | 한국어 | LTR |
| Russian | `AppStrings_ru_US.properties` | Русский | LTR |
| Tagalog | `AppStrings_tl_US.properties` | Tagalog | LTR |
| Vietnamese | `AppStrings_vi_US.properties` | Tiếng Việt | LTR |
| Chinese | `AppStrings_zh_US.properties` | 中文 | LTR |

### RTL Language Support

Arabic and Farsi/Persian use right-to-left (RTL) text direction. The application handles RTL layout automatically when these locales are selected.

```kotlin
fun isRtlLocale(locale: String): Boolean {
    return locale.startsWith("ar") || locale.startsWith("fa")
}
```

---

### Spanish (AppStrings_es_US.properties)

```properties
# ═══════════════════════════════════════════════════════════════════════════════
# BOTONES
# ═══════════════════════════════════════════════════════════════════════════════

add.cash.button = Agregar Efectivo
back.button = Atrás
cancel.button = Cancelar
cancel.payment = Cancelar Pago
cash.pickup.button = Retiro de Efectivo
ebt.balance.button = Saldo SNAP
force.sale = Forzar Venta
login.keypad.button = Usar Teclado
lotto.pay.button = Pago Lotería
no.button = No
ok.button = Aceptar
open.drawer.button = Abrir Cajón
price.check.button = Verificar Precio
print.last.receipt = Imprimir Último Recibo
proceed.to.payment = Proceder al Pago
pullback.button = Devolución
recall.return.button = Recuperar / Devolver
run.test = Ejecutar Prueba
sign.out.button = Cerrar Sesión
try.again.button = Intentar de Nuevo
vendor.payout.button = Pago a Proveedor
void.transaction.button = ANULAR Transacción
yes.button = Sí

# ═══════════════════════════════════════════════════════════════════════════════
# ENCABEZADOS DE DIÁLOGO
# ═══════════════════════════════════════════════════════════════════════════════

add.cash.dialog.header = Agregar Efectivo
info.header = Información
payments.header = Pagos
recall.header = Recuperar
till.header = Caja

# ═══════════════════════════════════════════════════════════════════════════════
# ETIQUETAS Y TEXTO DE PANTALLA
# ═══════════════════════════════════════════════════════════════════════════════

bag.fee = Cargo por Bolsa
cash = Efectivo
change.due = Cambio
charge = Cargo
customer.view.savings = Sus Ahorros
discount = Descuento
ebt = SNAP
food.stamp.eligible = Elegible para SNAP:
grand.total = Total General:
invoice.number = Número de Factura
item.subtotal = Subtotal del Artículo
items.text = Artículos:
other = Otro
pay.amount = Monto a Pagar
payment.amount = Monto del Pago
remaining = Restante
sales.tax = Impuesto(s) de Venta
snap.eligible = Elegible para SNAP
sold.by.weight = Vendido por Peso
subtotal = Subtotal
total = Total:
vendor.name = Nombre del Proveedor

# ═══════════════════════════════════════════════════════════════════════════════
# SOLICITUDES DE ENTRADA
# ═══════════════════════════════════════════════════════════════════════════════

enter.amount = Ingrese Monto
enter.invoice.number = Ingrese Número de Factura
enter.product.weight = Por favor ingrese el peso de este producto. \n El peso debe ser mayor a 10 lbs.
enter.quantity = Ingrese Cantidad
price.required = Precio Requerido
quantity.required = Cantidad Requerida

# ═══════════════════════════════════════════════════════════════════════════════
# MENSAJES INFORMATIVOS
# ═══════════════════════════════════════════════════════════════════════════════

choose.a.vendor.from.recent = Elija un Proveedor de envíos recientes o alfabéticamente.
if.the.vendor.or.their.shipment = Si el proveedor o su envío no aparece, el proveedor debe ver a un gerente.
pinpad.request.send = Solicitud de Pinpad Enviada
place.product.on.scale = Coloque el producto en la balanza.
please.enter.the.price = Por favor ingrese el precio de este producto.
please.select.vendor = Por favor seleccione proveedor
print.receipt = ¿Imprimir Recibo?
received.by.info = Información de Recibido Por
transaction.approved = Transacción Aprobada
waiting.on.customer = Esperando al Cliente

# ═══════════════════════════════════════════════════════════════════════════════
# MENSAJES DE ERROR
# ═══════════════════════════════════════════════════════════════════════════════

amount.is.bigger = El monto es mayor que su saldo disponible
amount.is.bigger.than.max = El monto es mayor que el pago máximo en efectivo disponible para este proveedor.
close.drawer.to.continue = Cierre el cajón para continuar
complete.transaction.message = Por favor complete la transacción
drawer.open = Cajón abierto
error.quantity.change.not.allowed = No se permite cambio de cantidad para artículos pesados.
item.is.not.for.sale = El Artículo No Está a la Venta
item.not.found = Artículo No Encontrado
minimum.weight.message = El peso mínimo debe ser 10 lbs.
pinpad.communication.error = Error de comunicación con PinPad, por favor reinicie el dispositivo e intente de nuevo.
pullback.not.eligible = La devolución no es elegible para esta transacción.
scale.info = Información de balanza
scale.not.ready = La balanza no está lista
sign.out.message = Por favor anule o complete la transacción antes de Cerrar Sesión
weight.overweight = Peso Excedido
weight.under.zero = Peso Bajo Cero
```

### Korean (AppStrings_ko.properties)

```properties
# ═══════════════════════════════════════════════════════════════════════════════
# 버튼
# ═══════════════════════════════════════════════════════════════════════════════

add.cash.button = 현금 추가
back.button = 뒤로
cancel.button = 취소
cancel.payment = 결제 취소
cash.pickup.button = 현금 수거
ebt.balance.button = SNAP 잔액
force.sale = 강제 판매
login.keypad.button = 키패드 사용
lotto.pay.button = 복권 결제
no.button = 아니오
ok.button = 확인
open.drawer.button = 서랍 열기
price.check.button = 가격 확인
print.last.receipt = 마지막 영수증 인쇄
proceed.to.payment = 결제 진행
pullback.button = 반품
recall.return.button = 보류 / 반품
run.test = 테스트 실행
sign.out.button = 로그아웃
try.again.button = 다시 시도
vendor.payout.button = 공급업체 지불
void.transaction.button = 거래 취소
yes.button = 예

# ═══════════════════════════════════════════════════════════════════════════════
# 대화 상자 헤더
# ═══════════════════════════════════════════════════════════════════════════════

add.cash.dialog.header = 현금 추가
info.header = 정보
payments.header = 결제
recall.header = 보류
till.header = 금고

# ═══════════════════════════════════════════════════════════════════════════════
# 레이블 및 표시 텍스트
# ═══════════════════════════════════════════════════════════════════════════════

bag.fee = 봉투 요금
cash = 현금
change.due = 거스름돈
charge = 청구
customer.view.savings = 절약 금액
discount = 할인
ebt = SNAP
food.stamp.eligible = SNAP 적격:
grand.total = 총 합계:
invoice.number = 송장 번호
item.subtotal = 품목 소계
items.text = 품목:
other = 기타
pay.amount = 결제 금액
payment.amount = 지불 금액
remaining = 잔액
sales.tax = 판매세
snap.eligible = SNAP 적격
sold.by.weight = 무게로 판매
subtotal = 소계
total = 합계:
vendor.name = 공급업체 이름

# ═══════════════════════════════════════════════════════════════════════════════
# 입력 프롬프트
# ═══════════════════════════════════════════════════════════════════════════════

enter.amount = 금액 입력
enter.invoice.number = 송장 번호 입력
enter.product.weight = 이 제품의 무게를 입력하세요. \n 무게는 10파운드 이상이어야 합니다.
enter.quantity = 수량 입력
price.required = 가격 필요
quantity.required = 수량 필요

# ═══════════════════════════════════════════════════════════════════════════════
# 정보 메시지
# ═══════════════════════════════════════════════════════════════════════════════

choose.a.vendor.from.recent = 최근 배송 또는 알파벳순으로 공급업체를 선택하세요.
if.the.vendor.or.their.shipment = 공급업체 또는 배송이 표시되지 않으면 관리자에게 문의해야 합니다.
pinpad.request.send = 핀패드 요청 전송됨
place.product.on.scale = 제품을 저울에 올려주세요.
please.enter.the.price = 이 제품의 가격을 입력하세요.
please.select.vendor = 공급업체를 선택하세요
print.receipt = 영수증을 인쇄하시겠습니까?
received.by.info = 수령자 정보
transaction.approved = 거래 승인됨
waiting.on.customer = 고객 대기 중

# ═══════════════════════════════════════════════════════════════════════════════
# 오류 메시지
# ═══════════════════════════════════════════════════════════════════════════════

amount.is.bigger = 금액이 사용 가능한 잔액보다 큽니다
amount.is.bigger.than.max = 이 공급업체에 대한 최대 현금 결제 금액을 초과했습니다.
close.drawer.to.continue = 계속하려면 서랍을 닫으세요
complete.transaction.message = 거래를 완료하세요
drawer.open = 서랍 열림
error.quantity.change.not.allowed = 무게 측정 품목의 수량 변경은 허용되지 않습니다.
item.is.not.for.sale = 판매 불가 품목
item.not.found = 품목을 찾을 수 없음
minimum.weight.message = 최소 무게는 10파운드여야 합니다.
pinpad.communication.error = 핀패드 통신 오류입니다. 장치를 재시작하고 다시 시도하세요.
pullback.not.eligible = 이 거래에는 반품이 적용되지 않습니다.
scale.info = 저울 정보
scale.not.ready = 저울이 준비되지 않았습니다
sign.out.message = 로그아웃하기 전에 거래를 취소하거나 완료하세요
weight.overweight = 무게 초과
weight.under.zero = 무게가 0 미만
```

### Vietnamese (AppStrings_vi.properties)

```properties
# ═══════════════════════════════════════════════════════════════════════════════
# NÚT BẤM
# ═══════════════════════════════════════════════════════════════════════════════

add.cash.button = Thêm Tiền Mặt
back.button = Quay Lại
cancel.button = Hủy
cancel.payment = Hủy Thanh Toán
cash.pickup.button = Thu Tiền Mặt
ebt.balance.button = Số Dư SNAP
force.sale = Bán Bắt Buộc
login.keypad.button = Sử Dụng Bàn Phím
lotto.pay.button = Thanh Toán Xổ Số
no.button = Không
ok.button = Đồng Ý
open.drawer.button = Mở Ngăn Kéo
price.check.button = Kiểm Tra Giá
print.last.receipt = In Hóa Đơn Cuối
proceed.to.payment = Tiến Hành Thanh Toán
pullback.button = Trả Lại
recall.return.button = Gọi Lại / Trả Lại
run.test = Chạy Thử
sign.out.button = Đăng Xuất
try.again.button = Thử Lại
vendor.payout.button = Chi Trả Nhà Cung Cấp
void.transaction.button = HỦY Giao Dịch
yes.button = Có

# ═══════════════════════════════════════════════════════════════════════════════
# TIÊU ĐỀ HỘP THOẠI
# ═══════════════════════════════════════════════════════════════════════════════

add.cash.dialog.header = Thêm Tiền Mặt
info.header = Thông Tin
payments.header = Thanh Toán
recall.header = Gọi Lại
till.header = Ngăn Kéo

# ═══════════════════════════════════════════════════════════════════════════════
# NHÃN VÀ VĂN BẢN HIỂN THỊ
# ═══════════════════════════════════════════════════════════════════════════════

bag.fee = Phí Túi
cash = Tiền Mặt
change.due = Tiền Thối
charge = Tính Phí
customer.view.savings = Tiết Kiệm Của Bạn
discount = Giảm Giá
ebt = SNAP
food.stamp.eligible = Đủ Điều Kiện SNAP:
grand.total = Tổng Cộng:
invoice.number = Số Hóa Đơn
item.subtotal = Tổng Phụ Mặt Hàng
items.text = Mặt Hàng:
other = Khác
pay.amount = Số Tiền Thanh Toán
payment.amount = Số Tiền Chi Trả
remaining = Còn Lại
sales.tax = Thuế Bán Hàng
snap.eligible = Đủ Điều Kiện SNAP
sold.by.weight = Bán Theo Cân
subtotal = Tổng Phụ
total = Tổng:
vendor.name = Tên Nhà Cung Cấp

# ═══════════════════════════════════════════════════════════════════════════════
# LỜI NHẮC NHẬP LIỆU
# ═══════════════════════════════════════════════════════════════════════════════

enter.amount = Nhập Số Tiền
enter.invoice.number = Nhập Số Hóa Đơn
enter.product.weight = Vui lòng nhập trọng lượng sản phẩm này. \n Trọng lượng phải lớn hơn 10 lbs.
enter.quantity = Nhập Số Lượng
price.required = Yêu Cầu Giá
quantity.required = Yêu Cầu Số Lượng

# ═══════════════════════════════════════════════════════════════════════════════
# THÔNG BÁO THÔNG TIN
# ═══════════════════════════════════════════════════════════════════════════════

choose.a.vendor.from.recent = Chọn Nhà Cung Cấp từ lô hàng gần đây hoặc theo thứ tự bảng chữ cái.
if.the.vendor.or.their.shipment = Nếu nhà cung cấp hoặc lô hàng của họ không xuất hiện, nhà cung cấp phải gặp quản lý.
pinpad.request.send = Yêu Cầu Bàn Phím PIN Đã Gửi
place.product.on.scale = Đặt sản phẩm lên cân.
please.enter.the.price = Vui lòng nhập giá cho sản phẩm này.
please.select.vendor = Vui lòng chọn nhà cung cấp
print.receipt = In Hóa Đơn?
received.by.info = Thông tin Người Nhận
transaction.approved = Giao Dịch Được Chấp Thuận
waiting.on.customer = Đang Chờ Khách Hàng

# ═══════════════════════════════════════════════════════════════════════════════
# THÔNG BÁO LỖI
# ═══════════════════════════════════════════════════════════════════════════════

amount.is.bigger = Số tiền lớn hơn số dư khả dụng của bạn
amount.is.bigger.than.max = Số tiền lớn hơn số tiền mặt tối đa có thể thanh toán cho nhà cung cấp này.
close.drawer.to.continue = Đóng ngăn kéo để tiếp tục
complete.transaction.message = Vui lòng hoàn thành giao dịch
drawer.open = Ngăn kéo đang mở
error.quantity.change.not.allowed = Không được phép thay đổi số lượng cho các mặt hàng cân.
item.is.not.for.sale = Mặt Hàng Không Được Bán
item.not.found = Không Tìm Thấy Mặt Hàng
minimum.weight.message = Trọng lượng tối thiểu phải là 10 lbs.
pinpad.communication.error = Lỗi giao tiếp bàn phím PIN, vui lòng khởi động lại thiết bị và thử lại.
pullback.not.eligible = Giao dịch này không đủ điều kiện trả lại.
scale.info = Thông tin cân
scale.not.ready = Cân chưa sẵn sàng
sign.out.message = Vui lòng hủy hoặc hoàn thành giao dịch trước khi Đăng Xuất
weight.overweight = Quá Cân
weight.under.zero = Cân Dưới Không
```

### Chinese Simplified (AppStrings_zh.properties)

```properties
# ═══════════════════════════════════════════════════════════════════════════════
# 按钮
# ═══════════════════════════════════════════════════════════════════════════════

add.cash.button = 添加现金
back.button = 返回
cancel.button = 取消
cancel.payment = 取消付款
cash.pickup.button = 收取现金
ebt.balance.button = SNAP余额
force.sale = 强制销售
login.keypad.button = 使用键盘
lotto.pay.button = 彩票支付
no.button = 否
ok.button = 确定
open.drawer.button = 打开钱箱
price.check.button = 价格查询
print.last.receipt = 打印上次收据
proceed.to.payment = 继续付款
pullback.button = 退货
recall.return.button = 调取 / 退货
run.test = 运行测试
sign.out.button = 登出
try.again.button = 重试
vendor.payout.button = 供应商付款
void.transaction.button = 作废交易
yes.button = 是

# ═══════════════════════════════════════════════════════════════════════════════
# 对话框标题
# ═══════════════════════════════════════════════════════════════════════════════

add.cash.dialog.header = 添加现金
info.header = 信息
payments.header = 付款
recall.header = 调取
till.header = 收银台

# ═══════════════════════════════════════════════════════════════════════════════
# 标签和显示文本
# ═══════════════════════════════════════════════════════════════════════════════

bag.fee = 袋子费用
cash = 现金
change.due = 找零
charge = 收费
customer.view.savings = 您的节省
discount = 折扣
ebt = SNAP
food.stamp.eligible = 符合SNAP条件:
grand.total = 总计:
invoice.number = 发票号码
item.subtotal = 商品小计
items.text = 商品:
other = 其他
pay.amount = 支付金额
payment.amount = 付款金额
remaining = 剩余
sales.tax = 销售税
snap.eligible = 符合SNAP条件
sold.by.weight = 按重量销售
subtotal = 小计
total = 合计:
vendor.name = 供应商名称

# ═══════════════════════════════════════════════════════════════════════════════
# 输入提示
# ═══════════════════════════════════════════════════════════════════════════════

enter.amount = 输入金额
enter.invoice.number = 输入发票号码
enter.product.weight = 请输入此产品的重量。\n 重量应大于10磅。
enter.quantity = 输入数量
price.required = 需要价格
quantity.required = 需要数量

# ═══════════════════════════════════════════════════════════════════════════════
# 信息消息
# ═══════════════════════════════════════════════════════════════════════════════

choose.a.vendor.from.recent = 从最近的发货或按字母顺序选择供应商。
if.the.vendor.or.their.shipment = 如果供应商或其发货未显示，供应商必须联系经理。
pinpad.request.send = PIN键盘请求已发送
place.product.on.scale = 请将产品放在秤上。
please.enter.the.price = 请输入此产品的价格。
please.select.vendor = 请选择供应商
print.receipt = 打印收据？
received.by.info = 收货人信息
transaction.approved = 交易已批准
waiting.on.customer = 等待客户

# ═══════════════════════════════════════════════════════════════════════════════
# 错误消息
# ═══════════════════════════════════════════════════════════════════════════════

amount.is.bigger = 金额超过您的可用余额
amount.is.bigger.than.max = 金额超过此供应商的最大现金支付限额。
close.drawer.to.continue = 关闭钱箱以继续
complete.transaction.message = 请完成交易
drawer.open = 钱箱已打开
error.quantity.change.not.allowed = 不允许更改称重商品的数量。
item.is.not.for.sale = 商品不可销售
item.not.found = 未找到商品
minimum.weight.message = 最小重量应为10磅。
pinpad.communication.error = PIN键盘通信错误，请重启设备并重试。
pullback.not.eligible = 此交易不符合退货条件。
scale.info = 秤信息
scale.not.ready = 秤未准备好
sign.out.message = 登出前请作废或完成交易
weight.overweight = 超重
weight.under.zero = 重量低于零
```

---

## Kotlin Compose Implementation

### Platform-Specific Implementation

```kotlin
// jvmMain/kotlin/com/unisight/gropos/i18n/Strings.jvm.kt

import java.text.MessageFormat
import java.util.*

actual object Strings {
    private var currentLocale: Locale = Locale.getDefault()
    private var bundle: ResourceBundle = loadBundle(currentLocale)
    
    private fun loadBundle(locale: Locale): ResourceBundle {
        return try {
            ResourceBundle.getBundle("i18n.AppStrings", locale)
        } catch (e: MissingResourceException) {
            // Fall back to English
            ResourceBundle.getBundle("i18n.AppStrings", Locale.ENGLISH)
        }
    }
    
    actual fun get(key: String): String = try {
        bundle.getString(key)
    } catch (e: MissingResourceException) {
        key // Return key as fallback
    }
    
    actual fun format(key: String, vararg args: Any): String {
        val template = get(key)
        return MessageFormat.format(template, *args)
    }
    
    actual fun setLocale(locale: String) {
        currentLocale = Locale.forLanguageTag(locale)
        bundle = loadBundle(currentLocale)
    }
    
    actual fun getCurrentLocale(): String = currentLocale.toLanguageTag()
    
    actual fun getAvailableLocales(): List<String> = listOf(
        "en", "es", "ko", "vi", "zh"
    )
}
```

### Usage in Compose UI

```kotlin
@Composable
fun PaymentScreen(viewModel: PaymentViewModel) {
    val state by viewModel.state.collectAsState()
    
    Column {
        // Using StringKeys for type safety
        Text(
            text = Strings.get(StringKeys.GRAND_TOTAL),
            style = MaterialTheme.typography.titleLarge
        )
        
        Text(
            text = state.grandTotal.formatCurrency(),
            style = MaterialTheme.typography.headlineMedium
        )
        
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = { viewModel.cancel() }) {
                Text(Strings.get(StringKeys.CANCEL_BUTTON))
            }
            
            Button(onClick = { viewModel.proceed() }) {
                Text(Strings.get(StringKeys.PROCEED_TO_PAYMENT))
            }
        }
        
        // Formatted string with parameters
        if (state.itemCount > 0) {
            Text(
                text = Strings.format("home.item.count", state.itemCount)
            )
        }
    }
}
```

### Language Switcher Component

```kotlin
@Composable
fun LanguageSwitcher(
    currentLocale: String,
    onLocaleChange: (String) -> Unit
) {
    val availableLocales = mapOf(
        "en" to "English",
        "es" to "Español",
        "ko" to "한국어",
        "vi" to "Tiếng Việt",
        "zh" to "中文"
    )
    
    var expanded by remember { mutableStateOf(false) }
    
    Box {
        TextButton(onClick = { expanded = true }) {
            Text(availableLocales[currentLocale] ?: currentLocale)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            availableLocales.forEach { (code, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onLocaleChange(code)
                        expanded = false
                    },
                    leadingIcon = if (code == currentLocale) {
                        { Icon(Icons.Default.Check, null) }
                    } else null
                )
            }
        }
    }
}
```

---

## Adding New Languages

### Step-by-Step Process

1. **Create properties file**
   ```
   app/src/main/resources/i18n/AppStrings_XX.properties
   ```
   (Replace XX with language code, e.g., `fr` for French)

2. **Copy all keys from English file**
   ```bash
   cp AppStrings_en.properties AppStrings_fr.properties
   ```

3. **Translate all values**
   - Keep the keys exactly the same
   - Only translate the values after the `=`

4. **Update available locales**
   ```kotlin
   actual fun getAvailableLocales(): List<String> = listOf(
       "en", "es", "ko", "vi", "zh", "fr"  // Add new locale
   )
   ```

5. **Test the translation**
   - Switch locale in the app
   - Verify all strings display correctly
   - Check for text truncation or overflow

---

## Best Practices

### Key Naming Convention

| Pattern | Example | Use For |
|---------|---------|---------|
| `screen.element` | `login.title` | Screen-specific labels |
| `button.action` | `cancel.button` | Button text |
| `error.context` | `error.printer.paper.out` | Error messages |
| `dialog.type.message` | `dialog.confirm.void` | Dialog content |
| `label.field` | `invoice.number` | Form labels |

### Guidelines

1. **Never hardcode strings** - Always use i18n keys
2. **Use parameters** - Use `{0}`, `{1}` for dynamic values
3. **Keep keys descriptive** - Make keys self-documenting
4. **Group related strings** - Use consistent prefixes
5. **Test with long translations** - German/Korean often longer than English
6. **Handle missing keys** - Return key as fallback, log warning
7. **Use context-appropriate translations** - "Cancel" may translate differently in different contexts

### String Formatting

```kotlin
// With parameters
enter.product.weight = Please enter the weight for this product. Weight should be greater than {0} lbs.

// Usage
Strings.format("enter.product.weight", minWeight)  // "...greater than 10 lbs."
```

---

*Last Updated: January 2026*
