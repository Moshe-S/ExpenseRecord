package com.example.expenserecord.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY lastUsed DESC LIMIT 10")
    fun getRecentCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(category: CategoryEntity)

    @Query("SELECT * FROM categories WHERE name LIKE :query || '%' ORDER BY lastUsed DESC")
    suspend fun searchCategories(query: String): List<CategoryEntity>
}