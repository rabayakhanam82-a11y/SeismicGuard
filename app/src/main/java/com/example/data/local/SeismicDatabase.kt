package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TremorEventEntity::class], version = 1, exportSchema = false)
abstract class SeismicDatabase : RoomDatabase() {
    abstract fun tremorEventDao(): TremorEventDao

    companion object {
        @Volatile
        private var INSTANCE: SeismicDatabase? = null

        fun getDatabase(context: Context): SeismicDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SeismicDatabase::class.java,
                    "seismicguard_database.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
