package com.example.expenserecord

import android.content.Context
import androidx.room.Room
import com.example.expenserecord.data.AppDb
import com.example.expenserecord.secure.Passphrase
import net.sqlcipher.database.SupportFactory
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DbProvider {
    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // No-op placeholder; implement when bumping schema to 3
        }
    }
    lateinit var db: AppDb
        private set

    fun init(context: Context) {
        val passphrase = Passphrase.get(context)
        val factory = SupportFactory(passphrase)
        db = Room.databaseBuilder(context, AppDb::class.java, "expenserecord.db")
            .openHelperFactory(factory)
            .addMigrations(MIGRATION_2_3)
            .build()
    }
}
