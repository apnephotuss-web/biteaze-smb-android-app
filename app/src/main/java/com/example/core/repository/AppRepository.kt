package com.example.core.repository

import com.example.core.database.dao.ExpenseDao
import com.example.core.database.dao.OrderDao
import com.example.core.database.dao.ProductDao
import com.example.core.database.entity.ExpenseEntity
import com.example.core.database.entity.OrderEntity
import com.example.core.database.entity.OrderItemEntity
import com.example.core.database.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

interface AppRepository {
    // Products
    fun getAllProducts(): Flow<List<ProductEntity>>
    fun getActiveProducts(): Flow<List<ProductEntity>>
    fun getActiveProductsSortedByFavorite(): Flow<List<ProductEntity>>
    suspend fun insertOrUpdateProduct(product: ProductEntity)
    suspend fun insertProduct(product: ProductEntity)
    suspend fun updateProduct(product: ProductEntity)
    suspend fun markDeleted(productId: String)
    suspend fun quickUpdateStock(id: String, newStock: Double)
    fun getAllCategories(): Flow<List<String>>
    suspend fun getProductById(id: String): ProductEntity?

    // Customers
    fun getAllCustomers(): Flow<List<com.example.core.database.entity.CustomerEntity>>
    suspend fun insertCustomer(customer: com.example.core.database.entity.CustomerEntity)
    suspend fun getCustomerByPhone(phone: String): com.example.core.database.entity.CustomerEntity?
    suspend fun getCustomerById(id: String): com.example.core.database.entity.CustomerEntity?
    suspend fun updateCustomer(customer: com.example.core.database.entity.CustomerEntity)

    // Orders
    fun getAllOrders(): Flow<List<OrderEntity>>
    fun getAllOrderItems(): Flow<List<OrderItemEntity>>
    suspend fun getOrderWithItems(orderId: String): com.example.core.database.dao.OrderWithItems?
    suspend fun saveOrderWithItems(
        order: OrderEntity,
        items: List<OrderItemEntity>,
        updateStock: Boolean = true
    )

    // Expenses
    fun getAllExpenses(): Flow<List<ExpenseEntity>>
    suspend fun insertExpense(expense: ExpenseEntity)
    suspend fun deleteExpense(id: String)

    // Addons
    fun getAllAddons(): Flow<List<com.example.core.database.entity.AddonEntity>>
    suspend fun insertAddon(addon: com.example.core.database.entity.AddonEntity)
    suspend fun deleteAddon(id: String)

    // Variations
    fun getAllVariations(): Flow<List<com.example.core.database.entity.VariationEntity>>
    suspend fun insertVariation(variation: com.example.core.database.entity.VariationEntity)
    suspend fun deleteVariation(id: String)

    // Inventory Ledger / Transactions
    fun getAllLedgerEntries(): Flow<List<com.example.core.database.entity.InventoryLedgerEntity>>
    fun getLedgerForProduct(productId: String): Flow<List<com.example.core.database.entity.InventoryLedgerEntity>>
    suspend fun insertLedgerEntry(entry: com.example.core.database.entity.InventoryLedgerEntity)

    // Staff
    fun getAllStaff(): Flow<List<com.example.core.database.entity.StaffEntity>>
    fun getAllActiveStaff(): Flow<List<com.example.core.database.entity.StaffEntity>>
    suspend fun insertStaff(staff: com.example.core.database.entity.StaffEntity)
    suspend fun deleteStaff(id: String)
}
