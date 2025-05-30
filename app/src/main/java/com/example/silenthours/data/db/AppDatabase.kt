package com.example.silenthours.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.silenthours.model.Contact
import com.example.silenthours.model.Group

@Database(entities = [Contact::class, Group::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class) // This ensures Converters are available for all DAOs/Entities in this DB
abstract class AppDatabase : RoomDatabase() {

    abstract fun contactDao(): ContactDao
    abstract fun groupDao(): GroupDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "silent_hours_database"
                )
                // Add migrations here if schema changes in the future
                .fallbackToDestructiveMigration() // For now, if schema changes, it will destroy and recreate
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
