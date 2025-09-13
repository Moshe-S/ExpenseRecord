package com.example.expenserecord.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [TxnEntity::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun txnDao(): TxnDao
}
