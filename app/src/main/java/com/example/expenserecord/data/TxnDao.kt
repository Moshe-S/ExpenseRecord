package com.example.expenserecord.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import androidx.room.Update

// New: distinct month row
data class MonthRow(val year: Int, val month: Int)

@Dao
interface TxnDao {
    @Insert
    suspend fun insert(e: TxnEntity)

    @Query("DELETE FROM txns WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM txns ORDER BY occurredAtEpochMillis DESC")
    fun all(): Flow<List<TxnEntity>>

    @Update
    suspend fun update(entity: TxnEntity)

    @Query("""
        SELECT * FROM txns
        WHERE occurredAtEpochMillis >= :startMillis
          AND occurredAtEpochMillis <= :endMillis
        ORDER BY occurredAtEpochMillis DESC
    """)
    fun getByDateRange(startMillis: Long, endMillis: Long): Flow<List<TxnEntity>>

    // New: all months that have any data (distinct, ascending)
    @Query("""
        SELECT 
          CAST(strftime('%Y', datetime(occurredAtEpochMillis/1000, 'unixepoch', 'localtime')) AS INTEGER) AS year,
          CAST(strftime('%m', datetime(occurredAtEpochMillis/1000, 'unixepoch', 'localtime')) AS INTEGER) AS month
        FROM txns
        GROUP BY year, month
        ORDER BY year ASC, month ASC
    """)
    fun getMonthsWithData(): Flow<List<MonthRow>>
}
