package com.example.expenserecord.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TxnDao {
    @Insert
    suspend fun insert(e: TxnEntity)
    @Query("DELETE FROM txns WHERE id = :id")
    suspend fun deleteById(id: Long)


    @Query("SELECT * FROM txns ORDER BY occurredAtEpochMillis DESC")
    fun all(): Flow<List<TxnEntity>>
}
