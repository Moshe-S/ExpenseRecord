package com.example.expenserecord

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        DbProvider.init(this)
    }
}
