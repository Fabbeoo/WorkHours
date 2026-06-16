package com.fabio.workhours.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [WorkEntry::class],
    version = 2,  // ← versione da 1 a 2
    exportSchema = false
)
abstract class WorkHoursDatabase : RoomDatabase() {

    abstract fun workEntryDao(): WorkEntryDao

    companion object {
        @Volatile
        private var INSTANCE: WorkHoursDatabase? = null

        // Migrazione da versione 1 a 2
        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE work_entries ADD COLUMN isHoliday INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        fun getInstance(context: Context): WorkHoursDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WorkHoursDatabase::class.java,
                    "workhours_database"
                )
                    .addMigrations(MIGRATION_1_2)  // ← AGGIUNGI
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}