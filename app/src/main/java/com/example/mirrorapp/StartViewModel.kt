package com.example.mirrorapp

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mirrorapp.repository.CalendarEvent // Importera den uppdaterade CalendarEvent
import com.example.mirrorapp.repository.CalendarRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.*
import java.time.format.DateTimeFormatter
import androidx.compose.ui.graphics.Color
import java.time.format.DateTimeParseException
import java.util.Timer
import kotlin.concurrent.schedule
import java.time.temporal.TemporalAdjusters // Behövs för startOfWeek

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

    // Hjälpfunktion för säker parsning (kan flyttas om den används på fler ställen)
    private fun safeLocalDateParse(dateString: String?): LocalDate? {
        return if (dateString.isNullOrBlank()) null else try {
            LocalDate.parse(dateString)
        } catch (e: DateTimeParseException) {
            Log.e("StartViewModel", "Failed to parse date string: '$dateString'", e)
            null
        }
    }

    fun fetchEvents(urlColorMap: Map<String, Color>) {
        viewModelScope.launch {
            _isLoading.value = true
            Log.d("StartViewModel", "Fetching events started")
            try {
                val calendarRepository = CalendarRepository()
                val allEvents = calendarRepository.fetchEventsFromIcsUrls(urlColorMap)

                // Sortera på startDate, sedan på startTime (hantera null för startTime)
                val sortedEvents = allEvents.sortedWith(compareBy(
                    { safeLocalDateParse(it.startDate) },
                    { it.startTime ?: "23:59:59" } // Tom sträng (heldag) eller null tid sist
                ))

                _events.value = sortedEvents
                Log.d("StartViewModel", "Events sorted and updated, count: ${sortedEvents.size}")
                generateWeeklyGrid(sortedEvents)
                lastUrlColorMap = urlColorMap
                if (timer == null) {
                    startTimer()
                }
            } catch (e: Exception) {
                Log.e("StartViewModel", "Error fetching events", e)
                _events.value = emptyList()
                _weeklyGrid.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun startTimer(intervalMillis: Long = 60_000L) {
        timer?.cancel()
        timer = Timer()
        timer?.schedule(delay = intervalMillis, period = intervalMillis) {
            Log.d("StartViewModel", "Timer triggered, fetching again")
            lastUrlColorMap?.let { map ->
                fetchEvents(map)
            } ?: Log.w("StartViewModel", "No urlColorMap saved; skipping fetch from timer.")
        }
    }

    override fun onCleared() {
        super.onCleared()
        timer?.cancel()
        timer = null
    }

    // parseICalDate är nu ersatt av logiken i CalendarRepository
    // private fun parseICalDate(line: String): LocalDate? { ... }


    fun generateWeeklyGrid(eventsToDisplay: List<CalendarEvent>, weeksToShow: Int = 6) {
        Log.d("StartViewModel", "Generating weekly grid with ${eventsToDisplay.size} total events.")
        val now = LocalDate.now()
        val startOfCurrentWeek = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val newWeeksGrid = mutableListOf<List<CalendarDay>>()

        for (weekOffset in 0 until weeksToShow) {
            val currentIterationWeekStartDate = startOfCurrentWeek.plusWeeks(weekOffset.toLong())
            val weekDays = (0..6).map { dayOffset ->
                val currentDateInGridCell = currentIterationWeekStartDate.plusDays(dayOffset.toLong())

                val eventsForThisDay = eventsToDisplay.filter { event ->
                    val localEventStartDate = safeLocalDateParse(event.startDate)
                    // Om endDate är null eller tom, använd startDate som fallback (händelsen är bara en dag)
                    val localEventEndDate = safeLocalDateParse(event.endDate) ?: localEventStartDate

                    if (localEventStartDate != null && localEventEndDate !=null) {
                        // Händelsen är aktiv på dagen om:
                        // Dagens datum >= Händelsens startdatum OCH Dagens datum <= Händelsens slutdatum
                        !currentDateInGridCell.isBefore(localEventStartDate) && !currentDateInGridCell.isAfter(localEventEndDate)
                    } else {
                        false // Ogiltigt start/slutdatum för händelsen
                    }
                }.sortedBy { it.startTime ?: "23:59:59" } // Sortera igen för säkerhets skull

                CalendarDay(
                    date = currentDateInGridCell,
                    events = eventsForThisDay
                )
            }
            newWeeksGrid.add(weekDays)
        }
        _weeklyGrid.value = newWeeksGrid
        Log.d("StartViewModel", "Weekly grid generated. Weeks: ${newWeeksGrid.size}, Events in grid: ${newWeeksGrid.sumOf { w -> w.sumOf { d -> d.events.size } }}")
    }
}
