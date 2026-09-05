package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.NavigationSessionDao
import com.example.data.local.dao.SearchAnalyticsDao
import com.example.data.local.entity.NavigationEventEntity
import com.example.data.local.entity.NavigationSessionEntity
import com.example.data.local.entity.SearchAnalyticsEntity

@Database(
    entities = [
        NavigationSessionEntity::class,
        NavigationEventEntity::class,
        SearchAnalyticsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun navigationSessionDao(): NavigationSessionDao
    abstract fun searchAnalyticsDao(): SearchAnalyticsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pathfinder_analytics.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
