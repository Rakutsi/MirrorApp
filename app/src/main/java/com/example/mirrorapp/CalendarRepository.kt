package com.example.mirrorapp.repository

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.Color
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.net.HttpURLConnection
import java.io.IOException

data class CalendarEvent(
    val title: String,
    val date: String,          // Format: yyyy-MM-dd
    val time: String?,         // Nytt fält för tid
    val color: Color,
    val isAllDay: Boolean,
    val sourceUrl: String
)

class CalendarRepository {

    suspend fun fetchEventsFromIcsUrls(urlColorMap: Map<String, Color>): List<CalendarEvent> {
        val allEvents = mutableListOf<CalendarEvent>()
        for ((url, color) in urlColorMap) {
            try {
                val icsData = downloadIcsFile(url)
                val events = parseIcsEvents(icsData, color, url)
                allEvents.addAll(events)
            } catch (e: Exception) {
                Log.e("CalendarRepository", "Error while fetching events from $url", e)
            }
        }
        return allEvents
    }

    private suspend fun downloadIcsFile(url: String): String = withContext(Dispatchers.IO) {
        try {
            // Skapa anslutningen
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", "MirrorApp/1.0")
                setRequestProperty("Accept", "text/calendar, text/plain, */*")
                instanceFollowRedirects = true // Följ omdirigeringar
            }

            // Kontrollera om anslutningen leder till omdirigering
            val statusCode = connection.responseCode
            Log.d("CalendarRepository", "Response Code: $statusCode")
            var redirectedUrl = url
            if (statusCode in 300..399) {
                redirectedUrl = connection.getHeaderField("Location") ?: url
                Log.d("CalendarRepository", "Redirected URL: $redirectedUrl")
                connection.disconnect()
                // Om vi får en omdirigering, försök att ladda ner filen från den nya URL:en
                return@withContext downloadIcsFile(redirectedUrl)
            }

            // Kontrollera om innehållet är en ICS-fil
            val contentType = connection.getHeaderField("Content-Type")
            Log.d("CalendarRepository", "Content-Type: $contentType")
            if (contentType?.contains("text/calendar") == true) {
                // Läs data från InputStream
                connection.inputStream.use { inputStream ->
                    return@withContext BufferedReader(InputStreamReader(inputStream)).readText()
                }
            } else if (contentType?.contains("text/html") == true) {
                // Om vi får en HTML-sida, så kan det vara att sidan gör en omdirigering till ICS-filen
                // Vi försöker att följa den omdirigeringen eller ladda ner filen
                throw IOException("HTML response received. Trying to handle the redirect.")
            } else {
                // Om innehållet inte är en ICS-fil, kasta ett fel
                throw IOException("The content is not an ICS file. Content-Type: $contentType")
            }

        } catch (e: Exception) {
            Log.e("CalendarRepository", "Error downloading ICS file from $url", e)
            throw e // Hantera fel
        }
    }

    private fun parseIcsEvents(icsData: String, color: Color, url: String): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()
        val lines = icsData.lines()

        var currentTitle = ""
        var currentDate: LocalDate? = null
        var currentTime: String? = null  // Ny variabel för tid
        var isAllDay = false
        var recurrenceRule: String? = null
        var insideEvent = false
        val eventLines = mutableListOf<String>()

        for (line in lines) {
            when {
                line.startsWith("BEGIN:VEVENT") -> {
                    insideEvent = true
                    eventLines.clear()
                }
                line.startsWith("END:VEVENT") -> {
                    insideEvent = false
                    currentTitle = ""
                    currentDate = null
                    currentTime = null
                    isAllDay = false
                    recurrenceRule = null

                    for (evLine in eventLines) {
                        when {
                            evLine.startsWith("SUMMARY:") -> currentTitle = evLine.removePrefix("SUMMARY:").trim()
                            evLine.startsWith("DTSTART") -> {
                                isAllDay = evLine.contains("VALUE=DATE")
                                currentDate = parseDateFromLine(evLine)?.let { LocalDate.parse(it) }
                                currentTime = parseTimeFromLine(evLine) // Extrahera tid om tillgängligt
                            }
                            evLine.startsWith("RRULE:") -> recurrenceRule = evLine.removePrefix("RRULE:").trim()
                        }
                    }

                    if (!currentTitle.isBlank() && currentDate != null) {
                        if (recurrenceRule != null) {
                            val recurrenceDates = generateRecurringDates(currentDate, recurrenceRule)
                            for (date in recurrenceDates) {
                                events.add(
                                    CalendarEvent(
                                        title = "$currentTime $currentTitle", // Sätt tid innan titel
                                        date = date.toString(),
                                        time = if (isAllDay) "" else currentTime,  // Om det är heldag, sätt tiden som en tom sträng
                                        color = color,
                                        isAllDay = isAllDay,
                                        sourceUrl = url
                                    )
                                )
                            }
                        } else {
                            events.add(
                                CalendarEvent(
                                    title = "$currentTime $currentTitle", // Sätt tid innan titel
                                    date = currentDate.toString(),
                                    time = if (isAllDay) "" else currentTime,  // Om det är heldag, sätt tiden som en tom sträng
                                    color = color,
                                    isAllDay = isAllDay,
                                    sourceUrl = url
                                )
                            )
                        }
                    }

                }
                insideEvent -> eventLines.add(line)
            }
        }

        return events
    }

    private fun parseDateFromLine(line: String): String? {
        return try {
            if (line.contains("TZID")) {
                val rawDateTime = line.substringAfter(":").trim()
                val timeZoneString = line.substringBefore(":").substringAfter("TZID=").replace(" ", "_")
                val dateTimeWithoutTZ = rawDateTime
                val formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
                val localDateTime = LocalDateTime.parse(dateTimeWithoutTZ, formatter)
                val zoneId = ZoneId.of(timeZoneString)
                ZonedDateTime.of(localDateTime, zoneId).toLocalDate().toString()
            } else if (line.contains("VALUE=DATE")) {
                val rawDate = line.substringAfter(":").trim()
                LocalDate.parse(rawDate, DateTimeFormatter.BASIC_ISO_DATE).toString()
            } else {
                val rawDateTime = line.substringAfter(":").trim()
                val formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
                ZonedDateTime.parse(rawDateTime, formatter.withZone(ZoneOffset.UTC)).toLocalDate().toString()
            }
        } catch (e: Exception) {
            Log.e("CalendarRepository", "Error parsing date: $line", e)
            null
        }
    }

    private fun parseTimeFromLine(line: String): String? {
        return try {
            if (line.contains("DTSTART")) {
                val timePart = line.substringAfter(":").trim()

                // Kollar om det är ett heldagsevent (utan tid, ex: "20230510")
                if (timePart.length == 8 && timePart.matches(Regex("\\d{8}"))) {
                    // Om det är ett heldagsevent (endast datum)
                    return ""  // Helt dagsevent, returnera en tom sträng
                }

                // Annars, hantera vanliga datum- och tidsformat (ex: "20230510T123000")
                val dateTimePart = timePart.takeIf { it.contains("T") }
                if (!dateTimePart.isNullOrEmpty()) {
                    val dateStr = dateTimePart.substringBefore("T")
                    val timeStr = dateTimePart.substringAfter("T")

                    // Om tiden inte innehåller 'Z' (UTC), behandla det som en lokal tid
                    if (!timeStr.endsWith("Z")) {
                        val time = LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HHmmss"))
                        val zonedDateTime = ZonedDateTime.of(LocalDateTime.of(LocalDate.parse(dateStr, DateTimeFormatter.BASIC_ISO_DATE), time), ZoneId.of("Europe/Stockholm"))
                        return zonedDateTime.toLocalTime().toString()
                    } else {
                        // Om tiden slutar med 'Z', behandla det som UTC och omvandla till Stockholm-tid
                        val localTime = LocalTime.parse(timeStr.removeSuffix("Z"), DateTimeFormatter.ofPattern("HHmmss"))
                        val zonedDateTime = ZonedDateTime.of(LocalDateTime.of(LocalDate.parse(dateStr, DateTimeFormatter.BASIC_ISO_DATE), localTime), ZoneOffset.UTC)
                            .withZoneSameInstant(ZoneId.of("Europe/Stockholm"))
                        return zonedDateTime.toLocalTime().toString()
                    }
                }
            }
            return null  // Om det inte finns något giltigt DTSTART, returnera null
        } catch (e: Exception) {
            Log.e("CalendarRepository", "Error parsing time: $line", e)
            return ""  // Om det finns ett fel, returnera en tom sträng istället för null
        }
    }












    private fun generateRecurringDates(startDate: LocalDate, rule: String): List<LocalDate> {
        val dates = mutableListOf<LocalDate>()
        val freq = Regex("FREQ=([A-Z]+)").find(rule)?.groupValues?.get(1) ?: return dates
        val interval = Regex("INTERVAL=(\\d+)").find(rule)?.groupValues?.get(1)?.toIntOrNull() ?: 1
        val count = Regex("COUNT=(\\d+)").find(rule)?.groupValues?.get(1)?.toIntOrNull()
        val untilRaw = Regex("UNTIL=([0-9TZ]+)").find(rule)?.groupValues?.get(1)

        val untilDate = try {
            untilRaw?.let {
                if (it.length == 8) {
                    LocalDate.parse(it, DateTimeFormatter.BASIC_ISO_DATE)
                } else {
                    ZonedDateTime.parse(it, DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssX")).toLocalDate()
                }
            }
        } catch (e: Exception) {
            Log.e("CalendarRepository", "Error parsing UNTIL date: $untilRaw", e)
            null
        }

        var current = startDate
        var generated = 0

        when (freq) {
            "DAILY" -> {
                while (true) {
                    if (untilDate != null && current.isAfter(untilDate)) break
                    if (count != null && generated >= count) break
                    dates.add(current)
                    current = current.plusDays(interval.toLong())
                    generated++
                    if (generated > 1000) break
                }
            }

            "WEEKLY" -> {
                var weekStart = startDate
                while (true) {
                    val nextDate = weekStart.with(TemporalAdjusters.nextOrSame(startDate.dayOfWeek))
                    if (untilDate != null && nextDate.isAfter(untilDate)) return dates
                    if (count != null && generated >= count) return dates
                    if (!dates.contains(nextDate)) {
                        dates.add(nextDate)
                        generated++
                    }
                    weekStart = weekStart.plusWeeks(interval.toLong())
                    if (generated > 1000) break
                }
            }

            "YEARLY" -> {
                var yearStart = startDate
                while (true) {
                    if (untilDate != null && yearStart.isAfter(untilDate)) break
                    if (count != null && generated >= count) break
                    dates.add(yearStart)
                    yearStart = yearStart.plusYears(interval.toLong())
                    generated++
                    if (generated > 1000) break
                }
            }

            // Hantera fler regler som MONTHLY om det behövs
        }

        return dates
    }

}
