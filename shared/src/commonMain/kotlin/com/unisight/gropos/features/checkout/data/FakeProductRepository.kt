package com.unisight.gropos.features.checkout.data

import com.unisight.gropos.features.checkout.domain.model.ItemNumber
import com.unisight.gropos.features.checkout.domain.model.Product
import com.unisight.gropos.features.checkout.domain.model.ProductTax
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
