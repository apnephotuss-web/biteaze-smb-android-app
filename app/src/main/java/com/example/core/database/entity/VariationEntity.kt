package com.example.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "variations")
data class VariationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val options: String = "", // comma-separated options, e.g. "Small,Medium,Large"
    val price: Double = 0.0 // kept for backward compatibility and virtual pricing
)
