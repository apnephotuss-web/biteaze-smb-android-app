package com.example.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "total_amount") val totalAmount: Double,
    val subtotal: Double,
    @ColumnInfo(name = "tax_amount") val taxAmount: Double,
    @ColumnInfo(name = "payment_method") val paymentMethod: String,
    val status: String, // "COMPLETED", "VOIDED", "HELD"
    val shiftId: String = "shift-1",
    val customerPhone: String? = null,
    val customerName: String? = null,
    val cartDiscountValue: Double = 0.0,
    val cartDiscountType: String? = null, // "PERCENT", "FIXED", null
    val syncedToCloud: Boolean = false,
    val displayId: String = "",
    val paymentMode: String = "CASH",
    @ColumnInfo(name = "scheduled_start_time") val scheduledStartTime: Long? = null,
    @ColumnInfo(name = "estimated_duration") val estimatedDuration: Int? = null, // in minutes
    val tableNumber: String? = null,
    val assignedStaffId: String? = null,
    val assignedStaffName: String? = null
)
