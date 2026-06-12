package com.example.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "staff")
data class StaffEntity(
    @PrimaryKey val id: String,
    val name: String,
    val role: String,
    val commissionRate: Double = 10.0, // standard percentage e.g. 10%
    val isActive: Boolean = true
)
