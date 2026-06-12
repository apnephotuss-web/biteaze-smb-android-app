package com.example.core.database.dao

import androidx.room.*
import com.example.core.database.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

data class VoiceIndexedProduct(
    val id: String,
    val name: String,
    val price: Double,
    val stock: Double,
    val category: String,
    val unit: String
)

@Dao
interface ProductDao {
    @Query("SELECT id, name, price, stock, category, unit FROM products WHERE is_deleted = 0")
    suspend fun getActiveVoiceIndexingCatalog(): List<VoiceIndexedProduct>
    @Query("SELECT * FROM products WHERE is_deleted = 0 ORDER BY name ASC")
    fun getAllProductsFlow(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE is_deleted = 0 ORDER BY name ASC")
    fun getActiveProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE is_deleted = 0 ORDER BY is_favorite DESC, name ASC")
    fun getActiveProductsSortedByFavorite(): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("UPDATE products SET is_deleted = 1, last_updated = :timestamp WHERE id = :productId")
    suspend fun markDeleted(productId: String, timestamp: Long)

    @Query("UPDATE products SET stock = :newStock, last_updated = :timestamp WHERE id = :id")
    suspend fun quickUpdateStock(id: String, newStock: Double, timestamp: Long)

    @Query("SELECT DISTINCT category FROM products WHERE is_deleted = 0")
    fun getAllCategoriesFlow(): Flow<List<String>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: String): ProductEntity?
}
