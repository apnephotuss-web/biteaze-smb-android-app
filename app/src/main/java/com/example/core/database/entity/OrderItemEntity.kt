package com.example.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "order_items",
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["order_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["order_id"]),
        Index(value = ["product_id"])
    ]
)
data class OrderItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "order_id") val orderId: String,
    @ColumnInfo(name = "product_id") val productId: String,
    val quantity: Double,
    val price: Double,
    @ColumnInfo(name = "selected_variation_id") val selectedVariationId: String? = null,
    @ColumnInfo(name = "selected_addon_ids") val selectedAddonIds: String? = null,
    @ColumnInfo(name = "discount_value") val discountValue: Double = 0.0,
    @ColumnInfo(name = "discount_type") val discountType: String? = null, // "PERCENT", "FIXED", null
    @ColumnInfo(name = "tax_amount") val taxAmount: Double = 0.0,
    @ColumnInfo(name = "staff_id") val staffId: String? = null,
    val metadata: IndustryMetadata = IndustryMetadata.Generic()
)
