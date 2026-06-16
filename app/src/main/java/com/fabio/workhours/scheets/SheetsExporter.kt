package com.fabio.workhours.sheets

import android.content.Context
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.Spreadsheet
import com.google.api.services.sheets.v4.model.SpreadsheetProperties
import com.google.api.services.sheets.v4.model.ValueRange
import com.fabio.workhours.data.db.WorkEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Collections

class SheetsExporter(
    private val context: Context,
    private val accountEmail: String
) {

    private val sheetsService: Sheets by lazy {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            Collections.singleton("https://www.googleapis.com/auth/spreadsheets")
        )
        credential.selectedAccountName = accountEmail

        Sheets.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("WorkHours")
            .build()
    }

    /**
     * Crea un nuovo foglio Google e scrive i dati delle ore lavorate.
     * Restituisce l'URL del foglio creato.
     */
    suspend fun exportEntries(entries: List<WorkEntry>, sheetTitle: String): String {
        return withContext(Dispatchers.IO) {
            // 1. Crea un nuovo spreadsheet
            val spreadsheet = Spreadsheet().apply {
                properties = SpreadsheetProperties().apply {
                    title = sheetTitle
                }
            }
            val created = sheetsService.spreadsheets().create(spreadsheet).execute()
            val spreadsheetId = created.spreadsheetId

            // 2. Prepara i dati: intestazione + righe
            val header = listOf("Data", "Inizio", "Fine", "Ore", "Tipo", "Nota")
            val rows = entries.map { entry ->
                val hours = calculateHours(entry.startTime, entry.endTime)
                val tipo = if (entry.isHoliday) "Festivo" else "Feriale"
                listOf(entry.date, entry.startTime, entry.endTime, String.format("%.2f", hours), tipo, entry.note)
            }
            val values = listOf(header) + rows

            // 3. Scrive i dati a partire dalla cella A1
            val body = ValueRange().setValues(values)
            sheetsService.spreadsheets().values()
                .update(spreadsheetId, "A1", body)
                .setValueInputOption("RAW")
                .execute()

            // 4. Restituisce il link al foglio
            "https://docs.google.com/spreadsheets/d/$spreadsheetId"
        }
    }

    private fun calculateHours(startTime: String, endTime: String): Float {
        val (startH, startM) = startTime.split(":").map { it.toInt() }
        val (endH, endM) = endTime.split(":").map { it.toInt() }
        return ((endH * 60 + endM) - (startH * 60 + startM)) / 60f
    }
}