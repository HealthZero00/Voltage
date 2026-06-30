/*
© Жиляков Д.Э., 2026. Все права защищены.
*/

package com.example.sborkapc.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "selected_components")
data class SavedComponent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val category: String,
    val remoteId: Long,
    val name: String,
    val socketData: String,
    val imageUrl: String = "",
    val price: String = "0",
    val priceRegard: String = "---",
    val priceDns: String = "---",
    val productUrl: String = "",
    val productUrlRegard: String = "",
    val productUrlDns: String = "",
    val selectedStore: String = "citilink"
)

@Entity(tableName = "saved_builds")
data class SavedBuild(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val totalPrice: Int,
    val componentsJson: String
)

@Dao
interface ComponentDao {
    @Query("SELECT * FROM selected_components")
    fun getAllSelected(): Flow<List<SavedComponent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveComponent(component: SavedComponent)

    @Query("DELETE FROM selected_components WHERE id = :id")
    suspend fun deleteComponentById(id: Int)

    @Query("DELETE FROM selected_components WHERE category = :category")
    suspend fun deleteComponentsByCategory(category: String)

    @Query("DELETE FROM selected_components")
    suspend fun clearBuild()
}

@Dao
interface BuildDao {
    @Query("SELECT * FROM saved_builds ORDER BY timestamp DESC")
    fun getAllBuilds(): Flow<List<SavedBuild>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveBuild(build: SavedBuild)

    @Query("DELETE FROM saved_builds WHERE id = :id")
    suspend fun deleteBuild(id: Int)
}

@Database(entities = [SavedComponent::class, SavedBuild::class], version = 12, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun componentDao(): ComponentDao
    abstract fun buildDao(): BuildDao
}
