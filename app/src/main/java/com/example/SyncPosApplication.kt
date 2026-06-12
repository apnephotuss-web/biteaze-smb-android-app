package com.example

import android.app.Application
import androidx.room.Room
import com.example.core.database.AppDatabase
import com.example.core.repository.AppRepository
import com.example.core.repository.AppRepositoryImpl

class SyncPosApplication : Application() {
    lateinit var repository: AppRepository
    lateinit var database: AppDatabase

    override fun onCreate() {
        super.onCreate()
        val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE orders ADD COLUMN tableNumber TEXT")
            }
        }

        database = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "syncpos_database"
        ).addMigrations(MIGRATION_8_9)
         .fallbackToDestructiveMigration().build()
        repository = AppRepositoryImpl(database)
    }
}
