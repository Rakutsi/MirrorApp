package com.example.mirrorapp

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import com.example.mirrorapp.CalendarGrid
import java.net.URLDecoder
import androidx.compose.ui.platform.LocalContext


@Composable
fun HomeScreen(
    navBackStackEntry: NavBackStackEntry,
    allUrls: String,
    allColors: String,
    isDarkMode: Boolean
)
 {
    // Få kontext och ViewModel inom composable
    val context = LocalContext.current
    val viewModel: StartViewModel = viewModel()

    // Dekoda URL:er och färger från den passerade strängen
    val urls = allUrls.split(",").map { encodedUrl ->
        try {
            URLDecoder.decode(encodedUrl, "UTF-8")
        } catch (e: Exception) {
            ""
        }
    }

    val colors = allColors.split(",").mapNotNull { colorString ->
        try {
            Color(colorString.toInt())
        } catch (e: Exception) {
            null
        }
    }

    val urlColorMap = urls.zip(colors).toMap()  // Skapa Map från urls och colors

    val events by viewModel.events.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Anropa ViewModel för att hämta händelser
    LaunchedEffect(Unit) {
        Log.d("HomeScreen", "Fetching events with urlColorMap: $urlColorMap")
        viewModel.fetchEvents(urlColorMap)  // Skicka urlColorMap till ViewModel
    }

    // Layout för att visa URL:er och färger
    Column(modifier = Modifier.padding(16.dp)) {

        // Logga URL:erna och färgerna
        Log.d("HomeScreen", "Loaded URLs and Colors:")
        urlColorMap.forEach { (url, color) ->
            Log.d("HomeScreen", "URL: $url, Color: $color")
        }

        // Visa en spinner (laddningsindikator) om vi fortfarande väntar på händelser
        if (isLoading) {
            Log.d("HomeScreen", "Loading events...")
            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        } else {
            Log.d("HomeScreen", "Events loaded: ${events.size}")
            if (events.isEmpty()) {
                Log.d("HomeScreen", "No events found")
                Text("Inga händelser hittades.", style = MaterialTheme.typography.bodyMedium)
            } else {
                Log.d("HomeScreen", "Rendering calendar grid with ${events.size} events")
                CalendarGrid(viewModel = viewModel, isDarkMode = isDarkMode, showHeader = true)

            }
        }
    }
}
