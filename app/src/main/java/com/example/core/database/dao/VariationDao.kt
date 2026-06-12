package com.example.core.database.dao

import androidx.room.*
import com.example.core.database.entity.VariationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VariationDao {
    @Query("SELECT * FROM variations ORDER BY name ASC")
    fun getAllVariations(): Flow<List<VariationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVariation(variation: VariationEntity)

    @Query("DELETE FROM variations WHERE id = :id")
    suspend fun deleteVariationById(id: String)
}
