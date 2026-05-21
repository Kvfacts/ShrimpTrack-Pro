package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        FeedLog::class,
        MedicineLog::class,
        CountRecord::class,
        TrayCheckAlarm::class,
        UserProfile::class,
        Pond::class,
        ApMarketRate::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AquacultureDatabase : RoomDatabase() {

    abstract fun aquacultureDao(): AquacultureDao

    companion object {
        @Volatile
        private var INSTANCE: AquacultureDatabase? = null

        fun getDatabase(context: Context): AquacultureDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AquacultureDatabase::class.java,
                    "aquaculture_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
