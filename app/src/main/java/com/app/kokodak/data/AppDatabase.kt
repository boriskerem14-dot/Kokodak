package com.app.kokodak

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PageEntity::class,
        CrawlQueueEntity::class,
        LinkEntity::class // <--- ДОБАВЕТЕ LinkEntity ТУК!
    ],
    version = 3, // <--- УВЕЛИЧЕТЕ ВЕРСИЯТА НА 3
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun pageDao(): PageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kokodak_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}