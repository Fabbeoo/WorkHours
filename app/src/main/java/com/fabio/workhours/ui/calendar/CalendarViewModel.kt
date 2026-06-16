package com.fabio.workhours.ui.calendar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fabio.workhours.data.db.WorkEntry
import com.fabio.workhours.data.db.WorkHoursDatabase
import com.fabio.workhours.data.repository.WorkRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class CalendarViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WorkRepository

    // Data selezionata nel calendario
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    // Mese visualizzato
    private val _currentMonth = MutableStateFlow(LocalDate.now())
    val currentMonth: StateFlow<LocalDate> = _currentMonth.asStateFlow()

    // Entry del giorno selezionato
    private val _entriesForSelectedDate = MutableStateFlow<List<WorkEntry>>(emptyList())
    val entriesForSelectedDate: StateFlow<List<WorkEntry>> = _entriesForSelectedDate.asStateFlow()

    // Entry del mese corrente (per mostrare i pallini nel calendario)
    private val _entriesForMonth = MutableStateFlow<List<WorkEntry>>(emptyList())
    val entriesForMonth: StateFlow<List<WorkEntry>> = _entriesForMonth.asStateFlow()

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    fun toggleDarkTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    init {
        val dao = WorkHoursDatabase.getInstance(application).workEntryDao()
        repository = WorkRepository(dao)
        loadEntriesForSelectedDate()
        loadEntriesForMonth()
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        loadEntriesForSelectedDate()
    }

    fun changeMonth(date: LocalDate) {
        _currentMonth.value = date
        loadEntriesForMonth()
    }

    fun addEntry(startTime: String, endTime: String, note: String = "", isHoliday: Boolean = false) {
        viewModelScope.launch {
            val entry = WorkEntry(
                date = _selectedDate.value.format(dateFormatter),
                startTime = startTime,
                endTime = endTime,
                note = note,
                isHoliday = isHoliday  // ← AGGIUNGI
            )
            repository.insert(entry)
            loadEntriesForSelectedDate()
            loadEntriesForMonth()
        }
    }

    fun updateEntry(entry: WorkEntry) {
        viewModelScope.launch {
            repository.update(entry)
            loadEntriesForSelectedDate()
            loadEntriesForMonth()
        }
    }

    fun deleteEntry(entry: WorkEntry) {
        viewModelScope.launch {
            repository.delete(entry)
            loadEntriesForSelectedDate()
            loadEntriesForMonth()
        }
    }

    private fun loadEntriesForSelectedDate() {
        viewModelScope.launch {
            repository.getEntriesByDate(
                _selectedDate.value.format(dateFormatter)
            ).collect { entries ->
                _entriesForSelectedDate.value = entries
            }
        }
    }

    private fun loadEntriesForMonth() {
        viewModelScope.launch {
            repository.getEntriesByMonth(
                _currentMonth.value.format(monthFormatter) + "%"
            ).collect { entries ->
                _entriesForMonth.value = entries
            }
        }
    }

    fun calculateHours(startTime: String, endTime: String): String {
        val (startH, startM) = startTime.split(":").map { it.toInt() }
        val (endH, endM) = endTime.split(":").map { it.toInt() }
        val startMinutes = startH * 60 + startM
        val endMinutes = endH * 60 + endM
        val totalMinutes = endMinutes - startMinutes
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return "${hours}h ${String.format("%02d", minutes)}m"
    }
}