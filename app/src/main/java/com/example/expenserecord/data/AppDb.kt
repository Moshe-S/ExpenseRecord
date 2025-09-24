package com.example.expenserecord.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [TxnEntity::class, CategoryEntity::class], version = 2, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun txnDao(): TxnDao
    abstract fun categoryDao(): CategoryDao
}
