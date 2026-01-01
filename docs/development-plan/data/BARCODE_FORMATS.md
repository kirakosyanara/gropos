# GroPOS Barcode Format Reference

**Version:** 2.0 (Kotlin)  
**Status:** Specification Document

Supported barcode formats and embedded data extraction for GroPOS.

---

## Supported Formats

| Format | Length | Check Digit | Usage |
|--------|--------|-------------|-------|
| UPC-A | 12 | Yes | Most US products |
| UPC-E | 8 | Yes | Small packages |
| EAN-13 | 13 | Yes | International |
| EAN-8 | 8 | Yes | Small packages |
| PLU | 4-5 | No | Produce, deli |
| Internal | Variable | No | Store-specific |

---

## Standard Barcodes

### UPC-A Format

```
Position:  0  1  2  3  4  5  6  7  8  9  10 11
Example:   0  1  2  3  4  5  6  7  8  9  0  5
           ─────────────────────────────────
           │  Manufacturer  │  Product │ Check
```

### PLU Codes

Price Look-Up codes for produce:

| Range | Category |
|-------|----------|
| 3000-4999 | Standard produce |
| 83000-84999 | Organic produce |
| 93000-94999 | Organic (extended) |

---

## Embedded Price Barcodes

Used for random-weight items (deli, meat, produce by weight).

### Format 2 (Price in barcode)

```
Position:  0  1  2  3  4  5  6  7  8  9  10 11
Structure: 2  X  X  X  X  X  $  $  $  $  $  C
           ─  ──────────── ──────────── ─
           │  Item Number   Price       │ Check
           Prefix (2)                   Digit
           
Example:   2  1  2  3  4  5  0  0  4  9  9  8
           → Item: 12345
           → Price: $4.99
```

### Extraction Logic

```kotlin
fun parseEmbeddedPriceBarcode(barcode: String): EmbeddedBarcodeResult? {
    // Prefix "2" indicates embedded price
    if (!barcode.startsWith("2") || barcode.length != 12) {
        return null
    }
    
    val itemNumber = barcode.substring(1, 6)
    val priceStr = barcode.substring(6, 11)
    val price = BigDecimal(priceStr).divide(
        BigDecimal(100), 
        2, 
        RoundingMode.HALF_UP
    )
    
    return EmbeddedBarcodeResult(
        itemNumber = itemNumber,
        embeddedPrice = price,
        embeddedWeight = null
    )
}
```

---

## Embedded Quantity Barcodes

Used for weighted items with quantity in barcode.

### Format 28 (Quantity in barcode)

```
Position:  0  1  2  3  4  5  6  7  8  9  10 11
Structure: 2  8  X  X  X  X  Q  Q  Q  Q  Q  C
           ─── ──────────── ──────────── ─
           │   Item Number   Weight/Qty  │ Check
           Prefix (28)                   Digit
           
Example:   2  8  1  2  3  4  0  1  5  0  0  9
           → Item: 1234
           → Weight: 1.500 lbs
```

### Weight Calculation

```kotlin
fun parseEmbeddedWeightBarcode(barcode: String): EmbeddedBarcodeResult? {
    // Prefix "28" indicates embedded weight
    if (!barcode.startsWith("28") || barcode.length != 12) {
        return null
    }
    
    val itemNumber = barcode.substring(2, 6)
    val weightStr = barcode.substring(6, 11)
    val weight = BigDecimal(weightStr).divide(
        BigDecimal(1000), 
        3, 
        RoundingMode.HALF_UP
    )
    // Result: 1.500 lbs
    
    return EmbeddedBarcodeResult(
        itemNumber = itemNumber,
        embeddedPrice = null,
        embeddedWeight = weight
    )
}
```

---

## Internal Barcodes

Store-specific barcodes for internal products.

### Coupon Barcodes

```
Prefix: 5
Format: 5XXXXXXXXXXC
        │└ Coupon Code (10 digits)
        └ Coupon prefix
```

### Gift Card Barcodes

```
Prefix: 6
Format: 6XXXXXXXXXXC
        │└ Card Number (10 digits)
        └ Gift card prefix
```

### Loyalty Card Barcodes

```
Prefix: 9
Format: 9XXXXXXXXXXC
        │└ Customer ID (10 digits)
        └ Loyalty prefix
```

---

## Receipt Barcodes

Printed on receipts for returns/lookups.

### Transaction Barcode

```
Format: Last 20 characters of transaction GUID
Example: 550e8400-e29b-41d4-a716-446655440000
Barcode: d4a716-446655440000 (printed)
```

### Code 128

Receipts use Code 128 symbology:
- Full ASCII support
- High density
- Built-in checksum

---

## Barcode Parser

```kotlin
object BarcodeParser {
    
    fun parse(barcode: String): BarcodeResult {
        // Check for embedded price
        if (barcode.startsWith("2") && barcode.length == 12) {
            val embedded = parseEmbeddedPriceBarcode(barcode)
            if (embedded != null) {
                return BarcodeResult.EmbeddedPrice(embedded)
            }
        }
        
        // Check for embedded weight
        if (barcode.startsWith("28") && barcode.length == 12) {
            val embedded = parseEmbeddedWeightBarcode(barcode)
            if (embedded != null) {
                return BarcodeResult.EmbeddedWeight(embedded)
            }
        }
        
        // Check for internal barcodes
        when {
            barcode.startsWith("5") -> {
                return BarcodeResult.Coupon(barcode.substring(1, 11))
            }
            barcode.startsWith("6") -> {
                return BarcodeResult.GiftCard(barcode.substring(1, 11))
            }
            barcode.startsWith("9") && barcode.length == 12 -> {
                return BarcodeResult.LoyaltyCard(barcode.substring(1, 11))
            }
        }
        
        // Standard barcode
        return BarcodeResult.Standard(barcode)
    }
}

sealed class BarcodeResult {
    data class Standard(val barcode: String) : BarcodeResult()
    data class EmbeddedPrice(val data: EmbeddedBarcodeResult) : BarcodeResult()
    data class EmbeddedWeight(val data: EmbeddedBarcodeResult) : BarcodeResult()
    data class Coupon(val couponCode: String) : BarcodeResult()
    data class GiftCard(val cardNumber: String) : BarcodeResult()
    data class LoyaltyCard(val customerId: String) : BarcodeResult()
}

data class EmbeddedBarcodeResult(
    val itemNumber: String,
    val embeddedPrice: BigDecimal?,
    val embeddedWeight: BigDecimal?
)
```

---

## Scanner Configuration

### Datalogic Settings

```
Enable:
- UPC-A
- UPC-E
- EAN-13
- EAN-8
- Code 128
- Code 39

Prefix/Suffix:
- None (raw barcode data)

Reading Mode:
- Continuous
- Single read (manual trigger)
```

### Serial Scanner Settings

```
Baud: 9600
Data bits: 8
Stop bits: 1
Parity: None
Terminator: CR (0x0D) or LF (0x0A)
```

---

## Barcode Validation

### Check Digit Calculation (UPC-A)

```kotlin
fun validateUPCA(barcode: String): Boolean {
    if (barcode.length != 12) return false
    
    var sum = 0
    for (i in 0 until 11) {
        val digit = barcode[i].digitToInt()
        sum += if (i % 2 == 0) digit * 3 else digit
    }
    
    val checkDigit = (10 - (sum % 10)) % 10
    return checkDigit == barcode[11].digitToInt()
}
```

### Product Lookup Flow

```
1. Receive barcode from scanner
2. Check for embedded price prefix (2)
3. Check for embedded quantity prefix (28)
4. If standard barcode:
   a. Search product_item_number table
   b. If not found, search PLU codes
   c. If not found, show "Item Not Found"
5. If embedded:
   a. Extract item number
   b. Extract price or quantity
   c. Build transaction item with embedded data
```

---

## Force Sale Barcodes

When item not found, force sale creates temporary item:

```
Format: FORCE-XXXXXXXXX
Where X = timestamp or unique ID

Product created:
- Name: "Force Sale Item"
- Sold By: PromptForPrice or PromptForQty
- Category: Miscellaneous
- Tax: Default store tax
```

---

## Common Issues

### Barcode Won't Scan

1. Check scanner connection
2. Verify barcode format is enabled
3. Ensure barcode is clean/undamaged
4. Check minimum/maximum length settings

### Embedded Price Wrong

1. Verify product is configured as `EmbeddedBarcode`
2. Check store scale configuration matches barcode format
3. Verify price multiplier (÷100 for cents)

### PLU Not Found

1. PLU must be in product_item_number table
2. Leading zeros matter: 4011 ≠ 04011
3. Check if PLU is for different location

---

## Related Documentation

- [Data Models](./DATA_MODELS.md)
- [Hardware Integration](../hardware/DESKTOP_HARDWARE.md)
- [Transaction Flow](../features/TRANSACTION_FLOW.md)

---

*Last Updated: January 2026*

