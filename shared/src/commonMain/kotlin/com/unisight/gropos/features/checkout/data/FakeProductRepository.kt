package com.unisight.gropos.features.checkout.data

import com.unisight.gropos.features.checkout.domain.model.ItemNumber
import com.unisight.gropos.features.checkout.domain.model.Product
import com.unisight.gropos.features.checkout.domain.model.ProductTax
import com.unisight.gropos.features.checkout.domain.repository.LookupCategory
import com.unisight.gropos.features.checkout.domain.repository.ProductRepository
import java.math.BigDecimal

/**
 * Fake implementation of ProductRepository for development and testing.
 * 
 * Pre-populated with sample products matching DATABASE_SCHEMA.md examples.
 * 
 * Per testing-strategy.mdc: "Use Fakes for State" - this fake
 * maintains a product catalog that tests can inspect.
 * 
 * Per DATABASE_SCHEMA.md: Products have branchProductId, itemNumbers array,
 * and getByBarcode searches the itemNumbers[].itemNumber field.
 * 
 * TODO: Replace with CouchbaseLiteProductRepository when database layer is ready
 */
class FakeProductRepository : ProductRepository {
    
    /**
     * In-memory product catalog.
     * Key is branchProductId for fast lookup.
     */
    private val products: MutableMap<Int, Product> = mutableMapOf(
        // Per DATABASE_SCHEMA.md example: Organic Whole Milk
        12345 to Product(
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
        
        // Additional test products
        12346 to Product(
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
            taxes = emptyList() // Food items often tax exempt
        ),
        
        12347 to Product(
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
        
        12348 to Product(
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
        
        12349 to Product(
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
        // Per TAX_CALCULATIONS.md: CRV is ALWAYS taxable in California
        // Per DEPOSITS_FEES.md: CRV rates are $0.05 (<24oz) or $0.10 (>=24oz)
        // =====================================================================
        
        // Soda 2-Liter: Taxable beverage with CRV
        // Per TAX_CALCULATIONS.md example:
        //   Price: $2.99
        //   CRV (24oz+): $0.10
        //   Taxable Amount: $3.09
        //   Tax Rate: 9.5%
        //   Tax: $3.09 × 9.5% = $0.29
        //   Total: $3.38
        12350 to Product(
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
        
        // Soda Can (12oz): Smaller CRV rate
        12351 to Product(
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
        
        // Water bottle (SNAP eligible but still has CRV)
        12352 to Product(
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
        
        // Non-CRV taxable item (Chips)
        12353 to Product(
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
        
        // Hot Prepared Food (Taxable, NOT SNAP eligible)
        12354 to Product(
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
        ),
        
        // =====================================================================
        // AGE-RESTRICTED ITEMS (for testing Age Verification Dialog)
        // Per DIALOGS.md: Age Verification Dialog required for alcohol/tobacco
        // =====================================================================
        
        // Beer 6-Pack: Age 21+ required
        12355 to Product(
            branchProductId = 12355,
            productId = 110,
            productName = "Craft Beer 6-Pack",
            description = "Premium craft beer, 6 x 12oz bottles",
            category = 9,
            categoryName = "Alcohol",
            departmentId = 5,
            departmentName = "Beer & Wine",
            retailPrice = BigDecimal("12.99"),
            floorPrice = BigDecimal("10.00"),
            cost = BigDecimal("7.50"),
            soldById = "Quantity",
            soldByName = "Each",
            isSnapEligible = false,  // Alcohol is NOT SNAP eligible
            isActive = true,
            isForSale = true,
            ageRestriction = 21,  // Must be 21+ to purchase
            order = 50,
            itemNumbers = listOf(
                ItemNumber("BEER21", isPrimary = true),
                ItemNumber("600", isPrimary = false)
            ),
            taxes = listOf(
                ProductTax(taxId = 1, tax = "CA State Tax", percent = BigDecimal("7.25")),
                ProductTax(taxId = 2, tax = "County Tax", percent = BigDecimal("1.00")),
                ProductTax(taxId = 3, tax = "City Tax", percent = BigDecimal("1.25"))
            ),
            crvRatePerUnit = BigDecimal("0.30"),  // 6 x $0.05 per can
            crvId = 2
        ),
        
        // Cigarettes: Age 18+ required
        12356 to Product(
            branchProductId = 12356,
            productId = 111,
            productName = "Cigarettes Pack",
            description = "Premium tobacco cigarettes, 20 count",
            category = 10,
            categoryName = "Tobacco",
            departmentId = 6,
            departmentName = "Tobacco Products",
            retailPrice = BigDecimal("9.99"),
            floorPrice = BigDecimal("8.00"),
            cost = BigDecimal("5.00"),
            soldById = "Quantity",
            soldByName = "Each",
            isSnapEligible = false,  // Tobacco is NOT SNAP eligible
            isActive = true,
            isForSale = true,
            ageRestriction = 18,  // Must be 18+ to purchase
            order = 51,
            itemNumbers = listOf(
                ItemNumber("TOBACCO18", isPrimary = true),
                ItemNumber("601", isPrimary = false)
            ),
            taxes = listOf(
                ProductTax(taxId = 1, tax = "CA State Tax", percent = BigDecimal("7.25")),
                ProductTax(taxId = 2, tax = "County Tax", percent = BigDecimal("1.00")),
                ProductTax(taxId = 3, tax = "City Tax", percent = BigDecimal("1.25")),
                ProductTax(taxId = 4, tax = "Tobacco Tax", percent = BigDecimal("5.00"))
            ),
            crvRatePerUnit = BigDecimal.ZERO,
            crvId = null
        )
    )
    
    /**
     * Finds a product by barcode.
     * 
     * Per DATABASE_SCHEMA.md: Searches itemNumbers[].itemNumber field.
     * Equivalent to: ArrayExpression.any("x").in(itemNumbers).satisfies(x.itemNumber == barcode)
     */
    override suspend fun getByBarcode(barcode: String): Product? {
        return products.values.find { product ->
            product.itemNumbers.any { it.itemNumber == barcode }
        }
    }
    
    /**
     * Finds products by category.
     * 
     * Per DATABASE_SCHEMA.md: Query by category, ordered by order field.
     */
    override suspend fun getByCategory(categoryId: Int): List<Product> {
        return products.values
            .filter { it.category == categoryId && it.isActive && it.isForSale }
            .sortedBy { it.order }
    }
    
    /**
     * Finds a product by branchProductId.
     * 
     * Per DATABASE_SCHEMA.md: Document ID is branchProductId.
     */
    override suspend fun getById(branchProductId: Int): Product? {
        return products[branchProductId]
    }
    
    /**
     * Searches products by name.
     * 
     * Per DATABASE_SCHEMA.md: Uses full-text index on productName.
     */
    override suspend fun searchByName(query: String): List<Product> {
        val lowercaseQuery = query.lowercase()
        return products.values.filter { 
            it.productName.lowercase().contains(lowercaseQuery) 
        }
    }
    
    /**
     * Searches products by name OR barcode.
     * 
     * Per SCREEN_LAYOUTS.md: Product Lookup Dialog supports search by name and barcode.
     * Searches both productName (LIKE) and itemNumbers (exact match).
     */
    override suspend fun searchProducts(query: String): List<Product> {
        if (query.isBlank()) {
            return products.values
                .filter { it.isActive && it.isForSale }
                .sortedBy { it.productName }
        }
        
        val lowercaseQuery = query.lowercase().trim()
        return products.values.filter { product ->
            // Match by product name (partial)
            product.productName.lowercase().contains(lowercaseQuery) ||
            // Match by barcode (exact or partial)
            product.itemNumbers.any { 
                it.itemNumber.contains(lowercaseQuery) 
            }
        }.filter { it.isActive && it.isForSale }
    }
    
    /**
     * Gets all available lookup categories.
     * 
     * Per DATA_MODELS.md: Builds categories from product catalog.
     */
    override suspend fun getCategories(): List<LookupCategory> {
        return products.values
            .filter { it.isActive && it.isForSale && it.category != null }
            .groupBy { it.category!! to (it.categoryName ?: "Unknown") }
            .map { (categoryPair, _) ->
                LookupCategory(
                    id = categoryPair.first,
                    name = categoryPair.second,
                    displayOrder = categoryPair.first
                )
            }
            .sortedBy { it.displayOrder }
    }
    
    /**
     * Inserts or updates a product in the repository.
     * 
     * Per SYNC_MECHANISM.md: Used for data sync operations.
     */
    override suspend fun insertProduct(product: Product): Boolean {
        products[product.branchProductId] = product
        return true
    }
    
    /**
     * Gets the count of products in the repository.
     */
    override suspend fun getProductCount(): Long {
        return products.size.toLong()
    }
    
    /**
     * Adds a product to the fake catalog.
     * For testing purposes only.
     */
    fun addProduct(product: Product) {
        products[product.branchProductId] = product
    }
    
    /**
     * Clears all products from the fake catalog.
     * For testing purposes only.
     */
    fun clear() {
        products.clear()
    }
    
    /**
     * Gets all products (for debugging/testing).
     */
    fun getAllProducts(): List<Product> = products.values.toList()
}
