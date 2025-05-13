package com.example.mirrorapp

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mirrorapp.repository.CalendarEvent
import com.example.mirrorapp.repository.CalendarRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.*
import java.time.format.DateTimeFormatter
import androidx.compose.ui.graphics.Color
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Timer
import kotlin.concurrent.schedule
import java.time.LocalDateTime

data class CalendarDay(
    val date: LocalDate,
    val events: List<CalendarEvent>

)



class StartViewModel : ViewModel() {

    private val _events = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val events: StateFlow<List<CalendarEvent>> = _events

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _weeklyGrid = MutableStateFlow<List<List<CalendarDay>>>(emptyList())
    val weeklyGrid: StateFlow<List<List<CalendarDay>>> = _weeklyGrid

    private var timer: Timer? = null
    private var lastUrlColorMap: Map<String, Color>? = null

    fun fetchEvents(urlColorMap: Map<String, Color>) {
        viewModelScope.launch {
            _isLoading.value = true
            Log.d("StartViewModel", "Fetching events started")

            try {
                val calendarRepository = CalendarRepository()
                val allEvents = calendarRepository.fetchEventsFromIcsUrls(urlColorMap)
                val sortedEvents = allEvents.sortedBy { LocalDate.parse(it.date) }

                _events.value = sortedEvents
                Log.d("StartViewModel", "Events sorted and updated")

                generateWeeklyGrid(sortedEvents)

                // Spara senaste kartan
                lastUrlColorMap = urlColorMap

                // Starta timern bara en gång
                if (timer == null) {
                    startTimer()
                }

            } catch (e: Exception) {
                Log.e("StartViewModel", "Error fetching events", e)
                _events.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun startTimer(intervalMillis: Long = 60_000L) {
        timer = Timer()
        timer?.schedule(delay = intervalMillis, period = intervalMillis) {
            Log.d("StartViewModel", "Timer triggered, fetching again")
            val now = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            Log.d("TimeCheck", "App thinks current date/time is: ${now.format(formatter)}")
            lastUrlColorMap?.let { map ->
                viewModelScope.launch {
                    fetchEvents(map)
                }
            } ?: Log.w("StartViewModel", "No urlColorMap saved; skipping fetch.")
        }
    }

    override fun onCleared() {
        super.onCleared()
        timer?.cancel()
        timer = null
    }



    private fun parseICalDate(line: String): LocalDate? {
        return try {
            val value = line.substringAfter(":")
            if (line.contains("VALUE=DATE")) {
                // Heldag, t.ex. 20250506
                LocalDate.parse(value, DateTimeFormatter.ofPattern("yyyyMMdd"))
            } else {
                // Tidssatt, t.ex. 20250506T143000Z
                val formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
                    .withZone(ZoneOffset.UTC)
                LocalDateTime.parse(value, formatter).toLocalDate()
            }
        } catch (e: Exception) {
            null
        }
    }

    fun generateWeeklyGrid(events: List<CalendarEvent>, weeksToShow: Int = 6) {
        val now = LocalDate.now()
        val startOfWeek = now.with(DayOfWeek.MONDAY)

        val eventMap = events.groupBy {
            LocalDate.parse(it.date)
        }

        val weeks = mutableListOf<List<CalendarDay>>()

        for (weekOffset in 0 until weeksToShow) {
            val weekStart = startOfWeek.plusWeeks(weekOffset.toLong())
            Log.d("StartViewModel", "Generating week: ${weekStart}")

            val week = (0..6).map { dayOffset ->
                val date = weekStart.plusDays(dayOffset.toLong())
                val eventsForDay = eventMap[date] ?: emptyList()
                Log.d("StartViewModel", "Generated day: $date with ${eventsForDay.size} events")
                CalendarDay(
                    date = date,
                    events = eventsForDay
                )
            }
            weeks.add(week)
        }

        // Här säkerställer vi att alla veckor innehåller exakt 7 dagar
        _weeklyGrid.value = weeks
        Log.d("StartViewModel", "Weekly grid generated with ${weeks.size} weeks")
    }



}
