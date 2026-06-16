package com.fabio.workhours.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "work_entries")
data class WorkEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: String,
    val startTime: String,
    val endTime: String,
    val note: String = "",
    val isHoliday: Boolean = false  // ← AGGIUNGI
)