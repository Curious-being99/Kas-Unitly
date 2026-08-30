package com.example

import android.app.Application
import androidx.room.Room
import com.example.data.AppDatabase

class BaseApplication : Application() {
    lateinit var db: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        db = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "kaspa-history-db"
        ).build()
    }
}
