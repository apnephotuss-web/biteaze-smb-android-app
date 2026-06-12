package com.example.core.database.dao

import androidx.room.*
import com.example.core.database.entity.AddonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AddonDao {
    @Query("SELECT * FROM addons ORDER BY name ASC")
    fun getAllAddons(): Flow<List<AddonEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAddon(addon: AddonEntity)

    @Query("DELETE FROM addons WHERE id = :id")
    suspend fun deleteAddonById(id: String)
}
