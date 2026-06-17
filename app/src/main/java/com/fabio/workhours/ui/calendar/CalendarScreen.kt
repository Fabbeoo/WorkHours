package com.fabio.workhours.ui.calendar

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Share
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fabio.workhours.data.db.WorkEntry
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import com.fabio.workhours.sheets.SheetsExporter
import java.time.format.DateTimeFormatter as DTF
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = viewModel()
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val currentMonth by viewModel.currentMonth.collectAsState()
    val entriesForDate by viewModel.entriesForSelectedDate.collectAsState()
    val entriesForMonth by viewModel.entriesForMonth.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var entryToEdit by remember { mutableStateOf<WorkEntry?>(null) }

    val context = LocalContext.current

    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/spreadsheets"))
            .requestScopes(com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/drive.file"))
            .build()
    }

    val googleSignInClient = remember {
        GoogleSignIn.getClient(context, gso)
    }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            task.getResult(com.google.android.gms.common.api.ApiException::class.java)
        } catch (e: Exception) {
            // login fallito o annullato
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isExporting by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WorkHours") },
                actions = {
                    IconButton(onClick = { viewModel.toggleDarkTheme() }) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Cambia tema"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FloatingActionButton(
                    onClick = {
                        val account = GoogleSignIn.getLastSignedInAccount(context)
                        if (account == null) {
                            signInLauncher.launch(googleSignInClient.signInIntent)
                        } else {
                            if (entriesForMonth.isEmpty()) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Nessun turno da esportare questo mese")
                                }
                            } else {
                                isExporting = true
                                coroutineScope.launch {
                                    try {
                                        val exporter = SheetsExporter(context, account.email!!)
                                        val monthName = currentMonth.format(DTF.ofPattern("MMMM yyyy"))
                                        exporter.exportEntries(entriesForMonth, "WorkHours - $monthName")
                                        snackbarHostState.showSnackbar("Esportato! Apri da Google Drive")
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("Errore: ${e.message}")
                                    } finally {
                                        isExporting = false
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Esporta")
                }
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Aggiungi turno")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                MonthHeader(
                    currentMonth = currentMonth,
                    onPreviousMonth = { viewModel.changeMonth(currentMonth.minusMonths(1)) },
                    onNextMonth = { viewModel.changeMonth(currentMonth.plusMonths(1)) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                MonthlyHoursSummary(
                    entriesForMonth = entriesForMonth,
                    calculateHours = viewModel::calculateHours
                )
                Spacer(modifier = Modifier.height(8.dp))
                CalendarGrid(
                    currentMonth = currentMonth,
                    selectedDate = selectedDate,
                    entriesForMonth = entriesForMonth,
                    onDateSelected = { viewModel.selectDate(it) }
                )
                Spacer(modifier = Modifier.height(16.dp))
                DaySummary(
                    date = selectedDate,
                    entries = entriesForDate,
                    calculateHours = viewModel::calculateHours,
                    onEdit = { entryToEdit = it },
                    onDelete = { viewModel.deleteEntry(it) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showAddDialog) {
        EntryDialog(
            title = "Aggiungi turno",
            onConfirm = { start, end, note, isHoliday ->
                viewModel.addEntry(start, end, note, isHoliday)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    entryToEdit?.let { entry ->
        EntryDialog(
            title = "Modifica turno",
            initialStart = entry.startTime,
            initialEnd = entry.endTime,
            initialNote = entry.note,
            initialIsHoliday = entry.isHoliday,
            onConfirm = { start, end, note, isHoliday ->
                viewModel.updateEntry(entry.copy(startTime = start, endTime = end, note = note, isHoliday = isHoliday))
                entryToEdit = null
            },
            onDismiss = { entryToEdit = null }
        )
    }
}

@Composable
fun MonthHeader(
    currentMonth: LocalDate,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onPreviousMonth) { Text("←") }
        Text(
            text = currentMonth.month.getDisplayName(TextStyle.FULL, Locale.ITALIAN)
                .replaceFirstChar { it.uppercase() } + " ${currentMonth.year}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        TextButton(onClick = onNextMonth) { Text("→") }
    }
}

@Composable
fun MonthlyHoursSummary(
    entriesForMonth: List<WorkEntry>,
    calculateHours: (String, String) -> String
) {
    fun totalMinutes(entries: List<WorkEntry>): Int {
        return entries.sumOf { entry ->
            val (startH, startM) = entry.startTime.split(":").map { it.toInt() }
            val (endH, endM) = entry.endTime.split(":").map { it.toInt() }
            (endH * 60 + endM) - (startH * 60 + startM)
        }
    }

    fun minutesToString(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return "${h}h ${String.format("%02d", m)}m"
    }

    val totalMins = totalMinutes(entriesForMonth)
    val holidayMins = totalMinutes(entriesForMonth.filter { it.isHoliday })
    val weekdayMins = totalMins - holidayMins
    val daysWorked = entriesForMonth.map { it.date }.distinct().size

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Totale ore",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = minutesToString(totalMins),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Giorni lavorati",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "$daysWorked",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Feriali",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = minutesToString(weekdayMins),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Festivi",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = minutesToString(holidayMins),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}
@Composable
fun CalendarGrid(
    currentMonth: LocalDate,
    selectedDate: LocalDate,
    entriesForMonth: List<WorkEntry>,
    onDateSelected: (LocalDate) -> Unit
) {
    val yearMonth = YearMonth.of(currentMonth.year, currentMonth.month)
    val firstDayOfMonth = yearMonth.atDay(1)
    val firstDayOfWeek = (firstDayOfMonth.dayOfWeek.value - 1)
    val daysInMonth = yearMonth.lengthOfMonth()
    val datesWithEntries = entriesForMonth.map { it.date }.toSet()
    val dayLabels = listOf("Lun", "Mar", "Mer", "Gio", "Ven", "Sab", "Dom", "Tot")

    // Calcola minuti totali per data
    val minutesByDate = entriesForMonth.groupBy { it.date }.mapValues { (_, entries) ->
        entries.sumOf { entry ->
            val (startH, startM) = entry.startTime.split(":").map { it.toInt() }
            val (endH, endM) = entry.endTime.split(":").map { it.toInt() }
            (endH * 60 + endM) - (startH * 60 + startM)
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Intestazioni giorni + colonna Tot
            Row(modifier = Modifier.fillMaxWidth()) {
                dayLabels.forEach { label ->
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (label == "Tot")
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))

            val totalCells = firstDayOfWeek + daysInMonth
            val rows = (totalCells + 6) / 7

            for (row in 0 until rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var weekMinutes = 0

                    // 7 celle dei giorni
                    for (col in 0 until 7) {
                        val cellIndex = row * 7 + col
                        val day = cellIndex - firstDayOfWeek + 1

                        if (day < 1 || day > daysInMonth) {
                            Box(modifier = Modifier.weight(1f))
                        } else {
                            val date = yearMonth.atDay(day)
                            val dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                            val isSelected = date == selectedDate
                            val isToday = date == LocalDate.now()
                            val hasEntries = datesWithEntries.contains(dateStr)

                            // Accumula minuti della settimana
                            weekMinutes += minutesByDate[dateStr] ?: 0

                            DayCell(
                                day = day,
                                isSelected = isSelected,
                                isToday = isToday,
                                hasEntries = hasEntries,
                                onClick = { onDateSelected(date) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Colonna totale settimana
                    val weekHours = weekMinutes / 60
                    val weekMins = weekMinutes % 60
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (weekMinutes > 0) {
                            Text(
                                text = "${weekHours}h",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "${String.format("%02d", weekMins)}m",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DayCell(
    day: Int,
    isSelected: Boolean,
    isToday: Boolean,
    hasEntries: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primaryContainer
        else -> Color.Transparent
    }
    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = day.toString(),
            color = textColor,
            fontSize = 13.sp,
            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
        )
        if (hasEntries) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.primary
                    )
            )
        }
    }
}

@Composable
fun DaySummary(
    date: LocalDate,
    entries: List<WorkEntry>,
    calculateHours: (String, String) -> String,
    onEdit: (WorkEntry) -> Unit,
    onDelete: (WorkEntry) -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.ITALIAN)
    val totalMinutes = entries.sumOf { entry ->
        val (startH, startM) = entry.startTime.split(":").map { it.toInt() }
        val (endH, endM) = entry.endTime.split(":").map { it.toInt() }
        (endH * 60 + endM) - (startH * 60 + startM)
    }
    val totalStr = "${totalMinutes / 60}h ${String.format("%02d", totalMinutes % 60)}m"

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = date.format(formatter).replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (totalMinutes > 0) {
                Text(
                    text = "Totale: $totalStr",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (entries.isEmpty()) {
            Text(
                text = "Nessun turno registrato",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            entries.forEach { entry ->
                EntryCard(
                    entry = entry,
                    hours = calculateHours(entry.startTime, entry.endTime),
                    onEdit = { onEdit(entry) },
                    onDelete = { onDelete(entry) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
@Composable
fun EntryCard(
    entry: WorkEntry,
    hours: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${entry.startTime} – ${entry.endTime}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (entry.isHoliday) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "festivo",
                            fontSize = 14.sp
                        )
                    }
                }
                Text(
                    text = hours,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (entry.note.isNotBlank()) {
                    Text(
                        text = entry.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Modifica")
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Elimina",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun EntryDialog(
    title: String,
    initialStart: String = "08:00",
    initialEnd: String = "13:00",
    initialNote: String = "",
    initialIsHoliday: Boolean = false,
    onConfirm: (String, String, String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var startTime by remember { mutableStateOf(initialStart) }
    var endTime by remember { mutableStateOf(initialEnd) }
    var note by remember { mutableStateOf(initialNote) }
    var isHoliday by remember { mutableStateOf(initialIsHoliday) }
    var errorMessage by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = startTime,
                    onValueChange = { startTime = it },
                    label = { Text("Inizio (HH:MM)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = endTime,
                    onValueChange = { endTime = it },
                    label = { Text("Fine (HH:MM)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Nota (opzionale)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = isHoliday,
                        onCheckedChange = { isHoliday = it }
                    )
                    Text(
                        text = "Giorno festivo",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
                if (errorMessage.isNotBlank()) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (!startTime.matches(Regex("\\d{2}:\\d{2}")) ||
                    !endTime.matches(Regex("\\d{2}:\\d{2}"))) {
                    errorMessage = "Formato orario non valido (usa HH:MM)"
                    return@TextButton
                }
                onConfirm(startTime, endTime, note, isHoliday)
            }) {
                Text("Salva")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        }
    )
}