package com.example.expenserecord

import android.content.Context
import androidx.room.Room
import com.example.expenserecord.data.AppDb
import com.example.expenserecord.secure.Passphrase
import net.sqlcipher.database.SupportFactory

object DbProvider {
    lateinit var db: AppDb
        private set

    fun init(context: Context) {
        val passphrase = Passphrase.get(context)
        val factory = SupportFactory(passphrase)
        db = Room.databaseBuilder(context, AppDb::class.java, "expenserecord.db")
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration()
            .build()
    }
}
