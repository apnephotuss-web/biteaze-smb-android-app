package com.example.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.core.database.dao.AddonDao
import com.example.core.database.dao.CustomerDao
import com.example.core.database.dao.ExpenseDao
import com.example.core.database.dao.OrderDao
import com.example.core.database.dao.ProductDao
import com.example.core.database.dao.VariationDao
import com.example.core.database.dao.InventoryLedgerDao
import com.example.core.database.dao.StaffDao
import com.example.core.database.entity.StaffEntity
import com.example.core.database.entity.AddonEntity
import com.example.core.database.entity.CustomerEntity
import com.example.core.database.entity.ExpenseEntity
import com.example.core.database.entity.OrderEntity
import com.example.core.database.entity.OrderItemEntity
import com.example.core.database.entity.ProductEntity
import com.example.core.database.entity.VariationEntity
import com.example.core.database.entity.InventoryLedgerEntity

@Database(
    entities = [
        ProductEntity::class,
        OrderEntity::class,
        OrderItemEntity::class,
        ExpenseEntity::class,
        CustomerEntity::class,
        AddonEntity::class,
        VariationEntity::class,
        InventoryLedgerEntity::class,
        StaffEntity::class
    ],
    version = 12,
    exportSchema = false
)
@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun orderDao(): OrderDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun customerDao(): CustomerDao
    abstract fun addonDao(): AddonDao
    abstract fun variationDao(): VariationDao
    abstract fun inventoryLedgerDao(): InventoryLedgerDao
    abstract fun staffDao(): StaffDao
}
