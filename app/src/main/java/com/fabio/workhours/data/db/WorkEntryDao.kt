package com.fabio.workhours.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkEntryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: WorkEntry)

    @Update
    suspend fun update(entry: WorkEntry)

    @Delete
    suspend fun delete(entry: WorkEntry)

    @Query("SELECT * FROM work_entries WHERE date = :date ORDER BY startTime ASC")
    fun getEntriesByDate(date: String): Flow<List<WorkEntry>>

    @Query("SELECT * FROM work_entries WHERE date LIKE :monthPrefix ORDER BY date ASC, startTime ASC")
    fun getEntriesByMonth(monthPrefix: String): Flow<List<WorkEntry>>

    @Query("SELECT * FROM work_entries ORDER BY date DESC, startTime ASC")
    fun getAllEntries(): Flow<List<WorkEntry>>
}