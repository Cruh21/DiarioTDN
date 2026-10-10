package com.example.diariotdn.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryOptionDao {

    @Query("SELECT * FROM category_options WHERE category = :category ORDER BY id ASC")
    fun getOptionsByCategory(category: String): Flow<List<CategoryOption>>

    @Query("SELECT * FROM category_options ORDER BY category ASC, value ASC")
    fun getAllCategoryOptions(): Flow<List<CategoryOption>>

    @Query("SELECT * FROM category_options ORDER BY category ASC, value ASC")
    suspend fun getAllCategoryOptionsDirect(): List<CategoryOption>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOption(option: CategoryOption)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOptions(options: List<CategoryOption>)

    @Update
    suspend fun updateOption(option: CategoryOption)

    @Delete
    suspend fun deleteOption(option: CategoryOption)

    @Query("DELETE FROM category_options WHERE id = :id")
    suspend fun deleteOptionById(id: Int)

    @Query("SELECT COUNT(*) FROM category_options")
    suspend fun getCount(): Int
}
