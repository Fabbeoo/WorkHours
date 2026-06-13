package com.fabio.workhours

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.fabio.workhours.ui.calendar.CalendarScreen
import com.fabio.workhours.ui.theme.WorkHoursTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fabio.workhours.ui.calendar.CalendarViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: CalendarViewModel = viewModel()
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()

            WorkHoursTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CalendarScreen(viewModel = viewModel)
                }
            }
        }
    }
}

