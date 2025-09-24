package com.example.expenserecord.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "txns")
data class TxnEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val occurredAtEpochMillis: Long,
    val category: String,
    val amount: Double,
    val title: String?,
    val manuallySetDateTime: Boolean
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val name: String,
    val lastUsed: Long,
    val usageCount: Int
)