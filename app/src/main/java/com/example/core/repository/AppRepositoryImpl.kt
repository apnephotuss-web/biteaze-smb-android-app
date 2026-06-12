package com.example.core.repository

import androidx.room.withTransaction
import com.example.core.database.AppDatabase
import com.example.core.database.entity.ExpenseEntity
import com.example.core.database.entity.OrderEntity
import com.example.core.database.entity.OrderItemEntity
import com.example.core.database.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

class AppRepositoryImpl(private val database: AppDatabase) : AppRepository {
    private val productDao = database.productDao()
    private val orderDao = database.orderDao()
    private val expenseDao = database.expenseDao()
    private val customerDao = database.customerDao()
    private val addonDao = database.addonDao()
    private val variationDao = database.variationDao()
    private val staffDao = database.staffDao()

    override fun getAllCustomers() = customerDao.getAllCustomers()
    override suspend fun insertCustomer(customer: com.example.core.database.entity.CustomerEntity) = customerDao.insertCustomer(customer)
    override suspend fun getCustomerByPhone(phone: String) = customerDao.getCustomerByPhone(phone)
    override suspend fun getCustomerById(id: String) = customerDao.getCustomerById(id)
    override suspend fun updateCustomer(customer: com.example.core.database.entity.CustomerEntity) = customerDao.updateCustomer(customer)

    override fun getAllProducts(): Flow<List<ProductEntity>> = productDao.getAllProductsFlow()

    override fun getActiveProducts(): Flow<List<ProductEntity>> = productDao.getActiveProducts()

    override fun getActiveProductsSortedByFavorite(): Flow<List<ProductEntity>> = productDao.getActiveProductsSortedByFavorite()

    override suspend fun insertOrUpdateProduct(product: ProductEntity) =
        productDao.insertOrUpdateProduct(product)

    override suspend fun insertProduct(product: ProductEntity) =
        productDao.insertProduct(product)

    override suspend fun updateProduct(product: ProductEntity) =
        productDao.updateProduct(product)

    override suspend fun markDeleted(productId: String) =
        productDao.markDeleted(productId, System.currentTimeMillis())

    override suspend fun quickUpdateStock(id: String, newStock: Double) =
        productDao.quickUpdateStock(id, newStock, System.currentTimeMillis())

    override fun getAllCategories(): Flow<List<String>> = productDao.getAllCategoriesFlow()
    
    override suspend fun getProductById(id: String): ProductEntity? = productDao.getProductById(id)

    override fun getAllOrders(): Flow<List<OrderEntity>> = orderDao.getAllOrdersFlow()
    override fun getAllOrderItems(): Flow<List<OrderItemEntity>> = orderDao.getAllOrderItemsFlow()

    override suspend fun getOrderWithItems(orderId: String): com.example.core.database.dao.OrderWithItems? =
        orderDao.getOrderWithItems(orderId)

    override suspend fun saveOrderWithItems(
        order: OrderEntity,
        items: List<OrderItemEntity>,
        updateStock: Boolean
    ) {
        database.withTransaction {
            orderDao.insertOrder(order)
            orderDao.insertOrderItems(items)
            
            if (updateStock) {
                for (item in items) {
                    val isVar = item.productId.contains("__var__")
                    val parentId = if (isVar) item.productId.substringBefore("__var__") else item.productId
                    val varId = if (isVar) item.productId.substringAfter("__var__") else null
                    
                    val product = productDao.getProductById(parentId)
                    if (product != null && product.trackStock) {
                        val newStock = product.stock - item.quantity
                        var updatedProduct = product
                        
                        val meta = product.metadata as? com.example.core.database.entity.IndustryMetadata.Grocery
                        if (meta != null) {
                            if (isVar && varId != null) {
                                val oldVariations = meta.variations
                                val updatedVariations = oldVariations.map { v ->
                                    if (v.id == varId) {
                                        val newVarStock = v.stock - item.quantity
                                        var remainingToDeduct = item.quantity
                                        val sortedBatches = v.batches.sortedBy { it.expiryDate }.toMutableList()
                                        val newBatches = mutableListOf<com.example.core.database.entity.IndustryMetadata.BatchInfo>()
                                        
                                        for (batch in sortedBatches) {
                                            if (remainingToDeduct <= 0) {
                                                newBatches.add(batch)
                                            } else {
                                                if (batch.quantity <= remainingToDeduct) {
                                                    remainingToDeduct -= batch.quantity
                                                } else {
                                                    newBatches.add(batch.copy(quantity = batch.quantity - remainingToDeduct))
                                                    remainingToDeduct = 0.0
                                                }
                                            }
                                        }
                                        v.copy(stock = newVarStock, batches = newBatches)
                                    } else {
                                        v
                                    }
                                }
                                updatedProduct = updatedProduct.copy(
                                    metadata = meta.copy(variations = updatedVariations),
                                    stock = newStock,
                                    lastUpdated = System.currentTimeMillis()
                                )
                                productDao.insertOrUpdateProduct(updatedProduct)
                            } else {
                                var remainingToDeduct = item.quantity
                                val sortedBatches = meta.batches.sortedBy { it.expiryDate }.toMutableList()
                                val newBatches = mutableListOf<com.example.core.database.entity.IndustryMetadata.BatchInfo>()
                                
                                for (batch in sortedBatches) {
                                    if (remainingToDeduct <= 0) {
                                        newBatches.add(batch)
                                    } else {
                                        if (batch.quantity <= remainingToDeduct) {
                                            remainingToDeduct -= batch.quantity
                                        } else {
                                            newBatches.add(batch.copy(quantity = batch.quantity - remainingToDeduct))
                                            remainingToDeduct = 0.0
                                        }
                                    }
                                }
                                updatedProduct = updatedProduct.copy(
                                    metadata = meta.copy(batches = newBatches),
                                    stock = newStock,
                                    lastUpdated = System.currentTimeMillis()
                                )
                                productDao.insertOrUpdateProduct(updatedProduct)
                            }
                        } else {
                            productDao.quickUpdateStock(product.id, newStock, System.currentTimeMillis())
                        }
                        
                        // Create immutable ledger entry for OUT movement
                        val ledgerEntry = com.example.core.database.entity.InventoryLedgerEntity(
                            productId = parentId,
                            movementType = "OUT",
                            quantity = item.quantity,
                            timestamp = System.currentTimeMillis(),
                            reason = "Sales Order #${order.displayId.ifBlank { order.id.takeLast(4) }}",
                            referenceId = order.id
                        )
                        database.inventoryLedgerDao().insertLedgerEntry(ledgerEntry)
                    }
                }
            }
        }
    }

    override fun getAllExpenses(): Flow<List<ExpenseEntity>> = expenseDao.getAllExpensesFlow()

    override suspend fun insertExpense(expense: ExpenseEntity) = expenseDao.insertExpense(expense)

    override suspend fun deleteExpense(id: String) = expenseDao.deleteExpenseById(id)

    override fun getAllAddons(): Flow<List<com.example.core.database.entity.AddonEntity>> = addonDao.getAllAddons()

    override suspend fun insertAddon(addon: com.example.core.database.entity.AddonEntity) = addonDao.insertAddon(addon)

    override suspend fun deleteAddon(id: String) = addonDao.deleteAddonById(id)

    override fun getAllVariations(): Flow<List<com.example.core.database.entity.VariationEntity>> = variationDao.getAllVariations()

    override suspend fun insertVariation(variation: com.example.core.database.entity.VariationEntity) = variationDao.insertVariation(variation)

    override suspend fun deleteVariation(id: String) = variationDao.deleteVariationById(id)

    override fun getAllLedgerEntries(): Flow<List<com.example.core.database.entity.InventoryLedgerEntity>> =
        database.inventoryLedgerDao().getAllLedgerEntries()

    override fun getLedgerForProduct(productId: String): Flow<List<com.example.core.database.entity.InventoryLedgerEntity>> =
        database.inventoryLedgerDao().getLedgerForProduct(productId)

    override suspend fun insertLedgerEntry(entry: com.example.core.database.entity.InventoryLedgerEntity) {
        database.withTransaction {
            database.inventoryLedgerDao().insertLedgerEntry(entry)
            val product = productDao.getProductById(entry.productId)
            if (product != null) {
                val delta = when (entry.movementType.uppercase()) {
                    "IN", "RETURN" -> entry.quantity
                    "OUT", "WASTE" -> -entry.quantity
                    else -> 0.0
                }
                val newStock = product.stock + delta
                productDao.quickUpdateStock(product.id, newStock, System.currentTimeMillis())
            }
        }
    }

    override fun getAllStaff() = staffDao.getAllStaff()
    override fun getAllActiveStaff() = staffDao.getAllActiveStaff()
    override suspend fun insertStaff(staff: com.example.core.database.entity.StaffEntity) = staffDao.insertStaff(staff)
    override suspend fun deleteStaff(id: String) = staffDao.deleteStaff(id)
}
