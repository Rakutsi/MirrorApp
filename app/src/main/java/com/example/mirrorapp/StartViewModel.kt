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
import java.time.format.DateTimeFormatter // Behövs för LocalTime.parse med specifikt mönster
import androidx.compose.ui.graphics.Color
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Timer
import kotlin.concurrent.schedule

// --- DATAKLASSER för Layout ---
data class EventLayoutInfo(
    val event: CalendarEvent,
    val trackIndex: Int,
    val isVisualSegmentStart: Boolean,
    val isVisualSegmentEnd: Boolean,
    val totalEventStartDate: LocalDate,
    val totalEventEndDate: LocalDate
)

data class DailyLayout(
    val date: LocalDate,
    val eventsInTracks: List<EventLayoutInfo?>
)
// --- SLUT PÅ DATAKLASSER ---

class StartViewModel : ViewModel() {

    private val _events = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val events: StateFlow<List<CalendarEvent>> = _events

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _weeklyCalendarLayout = MutableStateFlow<List<List<DailyLayout>>>(emptyList())
    val weeklyCalendarLayout: StateFlow<List<List<DailyLayout>>> = _weeklyCalendarLayout

    private var timer: Timer? = null
    private var lastUrlColorMap: Map<String, Color>? = null

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

                // Denna initiala sortering av _events är ok om den används någon annanstans,
                // men generateWeeklyGridLayout kommer att göra sin egen mer detaljerade sortering.
                val sortedRawEvents = allEvents.sortedWith(compareBy(
                    { safeLocalDateParse(it.startDate) },
                    { it.startTime ?: "23:59:59" }
                ))
                _events.value = sortedRawEvents
                Log.d("StartViewModel", "Raw events sorted and updated, count: ${sortedRawEvents.size}")

                generateWeeklyGridLayout(sortedRawEvents)

                lastUrlColorMap = urlColorMap
                if (timer == null) {
                    startTimer()
                }
            } catch (e: Exception) {
                Log.e("StartViewModel", "Error fetching events", e)
                _events.value = emptyList()
                _weeklyCalendarLayout.value = emptyList()
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

    private data class EventPlacement(
        val event: CalendarEvent,
        var trackIndex: Int,
        val actualStartDate: LocalDate,
        val actualEndDate: LocalDate
    )

    fun generateWeeklyGridLayout(eventsToDisplay: List<CalendarEvent>, weeksToShow: Int = 6) {
        Log.d("StartViewModel", "Generating weekly grid layout with ${eventsToDisplay.size} total events for $weeksToShow weeks.")
        val now = LocalDate.now()
        val firstDayOfView = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val lastDayOfView = firstDayOfView.plusWeeks(weeksToShow.toLong() - 1).with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

        val relevantPreparedEvents = eventsToDisplay.mapNotNull { event ->
            val eStart = safeLocalDateParse(event.startDate)
            val eEnd = safeLocalDateParse(event.endDate ?: event.startDate)
            if (eStart != null && eEnd != null && !eStart.isAfter(lastDayOfView) && !eEnd.isBefore(firstDayOfView)) {
                EventPlacement(event, -1, eStart, eEnd)
            } else {
                null
            }
        }

        // --- HÄR ÄR DEN UPPDATERADE SORTERINGSLOGIKEN ---
        val sortedPlacements = relevantPreparedEvents.sortedWith(
            compareBy<EventPlacement> { it.actualStartDate } // 1. Startdatum (tidigast först)
                .thenBy { // 2. Starttid (tidigast först, heldagsevent behandlas som tidigast)
                    if (it.event.isAllDay) {
                        LocalTime.MIN
                    } else if (!it.event.startTime.isNullOrEmpty()) {
                        try {
                            val timeString = it.event.startTime!!
                            // Kontrollera om formatet är HH:mm:ss eller HH:mm
                            if (timeString.count { char -> char == ':' } == 2) { // Kollar antal kolon
                                LocalTime.parse(timeString, DateTimeFormatter.ISO_LOCAL_TIME) // Försöker parsa HH:mm:ss
                            } else {
                                LocalTime.parse(timeString, DateTimeFormatter.ofPattern("HH:mm")) // Försöker parsa HH:mm
                            }
                        } catch (e: DateTimeParseException) {
                            Log.w("StartViewModel", "Could not parse time: ${it.event.startTime} for event ${it.event.title}. Error: ${e.message}")
                            LocalTime.MAX // Om tiden inte kan parsas, lägg sist
                        }
                    } else {
                        // Om ingen starttid och inte heldag, lägg efter tidsspecificerade event.
                        LocalTime.MAX
                    }
                }
                .thenBy { it.event.title } // 3. Titel (alfabetiskt, som tie-breaker)
        )
        // --- SLUT PÅ UPPDATERAD SORTERINGSLOGIK ---

        val finalEventPlacements = mutableListOf<EventPlacement>()
        val trackAvailability = mutableMapOf<Int, LocalDate>()
        var maxTrackForAllWeeks = -1

        for (currentPlacement in sortedPlacements) {
            var assignedTrack = -1
            for (trackIndex in 0..(maxTrackForAllWeeks + 1)) {
                val trackEndsOn = trackAvailability[trackIndex]
                if (trackEndsOn == null || currentPlacement.actualStartDate.isAfter(trackEndsOn)) {
                    assignedTrack = trackIndex
                    break
                }
            }
            if (assignedTrack == -1) {
                assignedTrack = ++maxTrackForAllWeeks
            }

            trackAvailability[assignedTrack] = currentPlacement.actualEndDate
            finalEventPlacements.add(currentPlacement.copy(trackIndex = assignedTrack))
            if (assignedTrack > maxTrackForAllWeeks) {
                maxTrackForAllWeeks = assignedTrack
            }
        }
        Log.d("StartViewModel", "Max tracks allocated: $maxTrackForAllWeeks. Total Placements: ${finalEventPlacements.size}")

        val newWeeksLayout = mutableListOf<List<DailyLayout>>()
        for (weekOffset in 0 until weeksToShow) {
            val weekDaysLayout = mutableListOf<DailyLayout>()
            for (dayOfWeekIndex in 0..6) {
                val dayDate = firstDayOfView.plusWeeks(weekOffset.toLong()).plusDays(dayOfWeekIndex.toLong())
                if (dayDate.isAfter(lastDayOfView)) break

                val eventsForThisDayInTracks = MutableList<EventLayoutInfo?>(maxTrackForAllWeeks + 1) { null }

                for (placement in finalEventPlacements) {
                    if (!dayDate.isBefore(placement.actualStartDate) && !dayDate.isAfter(placement.actualEndDate)) {
                        if (placement.trackIndex >= 0 && placement.trackIndex < eventsForThisDayInTracks.size) {
                            if (eventsForThisDayInTracks[placement.trackIndex] == null) {
                                val isVisualStart = dayDate == placement.actualStartDate ||
                                        (dayDate.dayOfWeek == DayOfWeek.MONDAY && placement.actualStartDate.isBefore(dayDate))
                                val isVisualEnd = dayDate == placement.actualEndDate ||
                                        (dayDate.dayOfWeek == DayOfWeek.SUNDAY && placement.actualEndDate.isAfter(dayDate))

                                eventsForThisDayInTracks[placement.trackIndex] = EventLayoutInfo(
                                    event = placement.event,
                                    trackIndex = placement.trackIndex,
                                    isVisualSegmentStart = isVisualStart,
                                    isVisualSegmentEnd = isVisualEnd,
                                    totalEventStartDate = placement.actualStartDate,
                                    totalEventEndDate = placement.actualEndDate
                                )
                            }
                        }
                    }
                }
                weekDaysLayout.add(DailyLayout(date = dayDate, eventsInTracks = eventsForThisDayInTracks))
            }
            if (weekDaysLayout.isNotEmpty()) {
                newWeeksLayout.add(weekDaysLayout)
            }
        }
        _weeklyCalendarLayout.value = newWeeksLayout
        Log.d("StartViewModel", "Weekly grid layout generated. Weeks: ${newWeeksLayout.size}, Max Tracks: $maxTrackForAllWeeks")
    }
}

