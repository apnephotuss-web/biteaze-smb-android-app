package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.core.database.entity.InventoryLedgerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryLedgerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedgerEntry(entry: InventoryLedgerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedgerEntries(entries: List<InventoryLedgerEntity>)

    @Query("SELECT * FROM inventory_ledger ORDER BY timestamp DESC")
    fun getAllLedgerEntries(): Flow<List<InventoryLedgerEntity>>

    @Query("SELECT * FROM inventory_ledger WHERE product_id = :productId ORDER BY timestamp DESC")
    fun getLedgerForProduct(productId: String): Flow<List<InventoryLedgerEntity>>

    @Query("SELECT * FROM inventory_ledger WHERE movement_type = :type ORDER BY timestamp DESC")
    fun getLedgerByMovementType(type: String): Flow<List<InventoryLedgerEntity>>

    @Query("SELECT SUM(CASE WHEN movement_type = 'IN' OR movement_type = 'RETURN' THEN quantity ELSE -quantity END) FROM inventory_ledger WHERE product_id = :productId")
    suspend fun getComputedStockForProduct(productId: String): Double?
}
