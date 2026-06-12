package com.example.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventory_ledger")
data class InventoryLedgerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "product_id") val productId: String,
    @ColumnInfo(name = "movement_type") val movementType: String, // "IN", "OUT", "WASTE", "RETURN"
    val quantity: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val reason: String? = null,
    @ColumnInfo(name = "reference_id") val referenceId: String? = null // e.g. orderId
)
