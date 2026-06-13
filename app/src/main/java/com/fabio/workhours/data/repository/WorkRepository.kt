package com.fabio.workhours.data.repository

import com.fabio.workhours.data.db.WorkEntry
import com.fabio.workhours.data.db.WorkEntryDao
import kotlinx.coroutines.flow.Flow

class WorkRepository(private val dao: WorkEntryDao) {

    fun getEntriesByDate(date: String): Flow<List<WorkEntry>> =
        dao.getEntriesByDate(date)

    fun getEntriesByMonth(monthPrefix: String): Flow<List<WorkEntry>> =
        dao.getEntriesByMonth(monthPrefix)

    fun getAllEntries(): Flow<List<WorkEntry>> =
        dao.getAllEntries()

    suspend fun insert(entry: WorkEntry) = dao.insert(entry)

    suspend fun update(entry: WorkEntry) = dao.update(entry)

    suspend fun delete(entry: WorkEntry) = dao.delete(entry)
}