package com.fabio.workhours.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [WorkEntry::class],
    version = 1,
    exportSchema = false
)
abstract class WorkHoursDatabase : RoomDatabase() {

    abstract fun workEntryDao(): WorkEntryDao

    companion object {
        @Volatile
        private var INSTANCE: WorkHoursDatabase? = null

        fun getInstance(context: Context): WorkHoursDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WorkHoursDatabase::class.java,
                    "workhours_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}