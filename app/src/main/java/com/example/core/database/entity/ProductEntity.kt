package com.example.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sku: String,
    val price: Double,
    @ColumnInfo(name = "tax_rate") val taxRate: Double,
    val category: String,
    val stock: Double,
    @ColumnInfo(name = "min_stock") val minStock: Double = 5.0,
    val unit: String = "pcs",
    @ColumnInfo(name = "is_favorite") val isFavorite: Boolean = false,
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false,
    @ColumnInfo(name = "last_updated") val lastUpdated: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "addon_ids") val addonIds: String? = null,
    @ColumnInfo(name = "variation_ids") val variationIds: String? = null,
    @ColumnInfo(name = "track_stock") val trackStock: Boolean = false,
    val metadata: IndustryMetadata = IndustryMetadata.Generic()
)
