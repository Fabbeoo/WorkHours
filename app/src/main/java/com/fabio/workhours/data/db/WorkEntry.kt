package com.fabio.workhours.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "work_entries")
data class WorkEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: String,        // formato "2025-06-04"
    val startTime: String,   // formato "09:00"
    val endTime: String,     // formato "18:00"
    val note: String = ""
)