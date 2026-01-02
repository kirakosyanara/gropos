package com.unisight.gropos.core.database.seeder

import com.unisight.gropos.features.checkout.domain.model.ItemNumber
import com.unisight.gropos.features.checkout.domain.model.Product
import com.unisight.gropos.features.checkout.domain.model.ProductTax
import java.math.BigDecimal

/**
 * Initial product data for seeding the database.
 * 
 * This contains the exact same products that were in FakeProductRepository,
 * ensuring consistent behavior after migrating to CouchbaseLite.
 * 
 * Per DATABASE_SCHEMA.md: Product structure with itemNumbers, taxes, crvRatePerUnit
 * Per TAX_CALCULATIONS.md: Tax rates and CRV for calculation testing
 */
object InitialProducts {
    
    /**
     * All initial products to seed into the database.
     */
    val products: List<Product> = listOf(
        // Per DATABASE_SCHEMA.md example: Organic Whole Milk
        Product(
            branchProductId = 12345,
            productId = 100,
            productName = "Organic Whole Milk 1 Gallon",
            description = "Fresh organic whole milk",
            category = 5,
            categoryName = "Dairy",
            departmentId = 2,
            departmentName = "Refrigerated",
            retailPrice = BigDecimal("5.99"),
            floorPrice = BigDecimal("4.00"),
            cost = BigDecimal("3.50"),
            soldById = "Quantity",
            soldByName = "Each",
            isSnapEligible = true,
            isActive = true,
            isForSale = true,
            ageRestriction = "NO",
            order = 10,
            itemNumbers = listOf(
                ItemNumber("070000000121", isPrimary = true),
                ItemNumber("070000000122", isPrimary = false)
            ),
            taxes = listOf(
                ProductTax(taxId = 1, tax = "Sales Tax", percent = BigDecimal("8.5"))
            ),
            crvRatePerUnit = BigDecimal("0.05"),
            crvId = 2
        ),
        
        // Apple
        Product(
            branchProductId = 12346,
            productId = 101,
            productName = "Apple",
            category = 3,
            categoryName = "Produce",
            departmentId = 1,
            departmentName = "Fresh",
            retailPrice = BigDecimal("1.00"),
            isSnapEligible = true,
            itemNumbers = listOf(
                ItemNumber("111", isPrimary = true)
            ),
            taxes = emptyList()
        ),
        
        // Banana (Weighted)
        Product(
            branchProductId = 12347,
            productId = 102,
            productName = "Banana",
            category = 3,
            categoryName = "Produce",
            departmentId = 1,
            departmentName = "Fresh",
            retailPrice = BigDecimal("0.50"),
            soldById = "Weight",
            soldByName = "Per Pound",
            isSnapEligible = true,
            itemNumbers = listOf(
                ItemNumber("222", isPrimary = true)
            ),
            taxes = emptyList()
        ),
        
        // Orange
        Product(
            branchProductId = 12348,
            productId = 103,
            productName = "Orange",
            category = 3,
            categoryName = "Produce",
            retailPrice = BigDecimal("0.75"),
            isSnapEligible = true,
            itemNumbers = listOf(
                ItemNumber("333", isPrimary = true)
            ),
            taxes = emptyList()
        ),
        
        // Bread
        Product(
            branchProductId = 12349,
            productId = 104,
            productName = "Bread",
            category = 4,
            categoryName = "Bakery",
            retailPrice = BigDecimal("2.49"),
            isSnapEligible = true,
            itemNumbers = listOf(
                ItemNumber("444", isPrimary = true)
            ),
            taxes = emptyList()
        ),
        
        // =====================================================================
        // TAXABLE ITEMS WITH CRV (for testing calculation engine)
        // =====================================================================
        
        // Cola 2-Liter: Taxable beverage with CRV
        // Per TAX_CALCULATIONS.md:
        //   Price: $2.99, CRV: $0.10, Taxable: $3.09
        //   Tax Rate: 9.5%, Tax: $0.29, Total: $3.38
        Product(
            branchProductId = 12350,
            productId = 105,
            productName = "Cola 2-Liter",
            description = "Classic cola soda 2-liter bottle",
            category = 6,
            categoryName = "Beverages",
            departmentId = 3,
            departmentName = "Grocery",
            retailPrice = BigDecimal("2.99"),
            floorPrice = BigDecimal("1.50"),
            cost = BigDecimal("1.25"),
            soldById = "Quantity",
            soldByName = "Each",
            isSnapEligible = false,  // Soda is NOT SNAP eligible
            isActive = true,
            isForSale = true,
            ageRestriction = "NO",
            order = 20,
            itemNumbers = listOf(
                ItemNumber("555", isPrimary = true),
                ItemNumber("5551234567890", isPrimary = false)
            ),
            taxes = listOf(
                ProductTax(taxId = 1, tax = "CA State Tax", percent = BigDecimal("7.25")),
                ProductTax(taxId = 2, tax = "County Tax", percent = BigDecimal("1.00")),
                ProductTax(taxId = 3, tax = "City Tax", percent = BigDecimal("1.25"))
            ),
            crvRatePerUnit = BigDecimal("0.10"),  // 24oz+ rate
            crvId = 1
        ),
        
        // Cola Can (12oz): Smaller CRV rate
        Product(
            branchProductId = 12351,
            productId = 106,
            productName = "Cola Can 12oz",
            description = "Classic cola soda can",
            category = 6,
            categoryName = "Beverages",
            departmentId = 3,
            departmentName = "Grocery",
            retailPrice = BigDecimal("1.29"),
            floorPrice = BigDecimal("0.75"),
            cost = BigDecimal("0.50"),
            soldById = "Quantity",
            soldByName = "Each",
            isSnapEligible = false,
            isActive = true,
            isForSale = true,
            ageRestriction = "NO",
            order = 21,
            itemNumbers = listOf(
                ItemNumber("556", isPrimary = true)
            ),
            taxes = listOf(
                ProductTax(taxId = 1, tax = "CA State Tax", percent = BigDecimal("7.25")),
                ProductTax(taxId = 2, tax = "County Tax", percent = BigDecimal("1.00")),
                ProductTax(taxId = 3, tax = "City Tax", percent = BigDecimal("1.25"))
            ),
            crvRatePerUnit = BigDecimal("0.05"),  // <24oz rate
            crvId = 2
        ),
        
        // Bottled Water (SNAP eligible but still has CRV)
        Product(
            branchProductId = 12352,
            productId = 107,
            productName = "Bottled Water 16oz",
            description = "Spring water bottle",
            category = 6,
            categoryName = "Beverages",
            departmentId = 3,
            departmentName = "Grocery",
            retailPrice = BigDecimal("1.49"),
            soldById = "Quantity",
            soldByName = "Each",
            isSnapEligible = true,  // Water IS SNAP eligible (no tax)
            isActive = true,
            isForSale = true,
            ageRestriction = "NO",
            order = 22,
            itemNumbers = listOf(
                ItemNumber("557", isPrimary = true)
            ),
            taxes = listOf(
                // These taxes exist but WON'T apply since SNAP-eligible
                ProductTax(taxId = 1, tax = "CA State Tax", percent = BigDecimal("7.25")),
                ProductTax(taxId = 2, tax = "County Tax", percent = BigDecimal("1.00")),
                ProductTax(taxId = 3, tax = "City Tax", percent = BigDecimal("1.25"))
            ),
            crvRatePerUnit = BigDecimal("0.05"),  // <24oz rate
            crvId = 2
        ),
        
        // Potato Chips (SNAP eligible, no CRV)
        Product(
            branchProductId = 12353,
            productId = 108,
            productName = "Potato Chips",
            description = "Classic salted potato chips",
            category = 7,
            categoryName = "Snacks",
            departmentId = 3,
            departmentName = "Grocery",
            retailPrice = BigDecimal("4.99"),
            floorPrice = BigDecimal("3.00"),
            soldById = "Quantity",
            soldByName = "Each",
            isSnapEligible = true,  // Chips ARE SNAP eligible (no tax)
            isActive = true,
            isForSale = true,
            ageRestriction = "NO",
            order = 30,
            itemNumbers = listOf(
                ItemNumber("558", isPrimary = true)
            ),
            taxes = listOf(
                // These taxes exist but WON'T apply since SNAP-eligible
                ProductTax(taxId = 1, tax = "CA State Tax", percent = BigDecimal("7.25")),
                ProductTax(taxId = 2, tax = "County Tax", percent = BigDecimal("1.00")),
                ProductTax(taxId = 3, tax = "City Tax", percent = BigDecimal("1.25"))
            ),
            crvRatePerUnit = BigDecimal.ZERO,  // No CRV (not a beverage)
            crvId = null
        ),
        
        // Hot Dog (Taxable, NOT SNAP eligible)
        Product(
            branchProductId = 12354,
            productId = 109,
            productName = "Hot Dog",
            description = "Prepared hot dog from deli",
            category = 8,
            categoryName = "Deli",
            departmentId = 4,
            departmentName = "Prepared Foods",
            retailPrice = BigDecimal("3.49"),
            soldById = "Quantity",
            soldByName = "Each",
            isSnapEligible = false,  // Hot prepared food is NOT SNAP eligible
            isActive = true,
            isForSale = true,
            ageRestriction = "NO",
            order = 40,
            itemNumbers = listOf(
                ItemNumber("559", isPrimary = true)
            ),
            taxes = listOf(
                ProductTax(taxId = 1, tax = "CA State Tax", percent = BigDecimal("7.25")),
                ProductTax(taxId = 2, tax = "County Tax", percent = BigDecimal("1.00")),
                ProductTax(taxId = 3, tax = "City Tax", percent = BigDecimal("1.25"))
            ),
            crvRatePerUnit = BigDecimal.ZERO,
            crvId = null
        )
    )
}

