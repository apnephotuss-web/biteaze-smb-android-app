package com.example.core.database

import androidx.room.TypeConverter
import com.example.core.database.entity.IndustryMetadata
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

class DatabaseConverters {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @TypeConverter
    fun fromMetadata(metadata: IndustryMetadata?): String? {
        if (metadata == null) return null
        return try {
            json.encodeToString<IndustryMetadata>(metadata)
        } catch (e: Exception) {
            null
        }
    }

    @TypeConverter
    fun toMetadata(value: String?): IndustryMetadata? {
        if (value == null) return null
        return try {
            json.decodeFromString<IndustryMetadata>(value)
        } catch (e: Exception) {
            IndustryMetadata.Generic()
        }
    }
}
