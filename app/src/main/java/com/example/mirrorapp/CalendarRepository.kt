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
import java.time.temporal.ChronoUnit
import java.net.HttpURLConnection
import java.io.IOException

data class CalendarEvent(
    val title: String, // Ska nu ALLTID vara den rena titeln från SUMMARY
    val startDate: String,
    val startTime: String?, // "HH:mm", "" (för heldag), eller null
    val endDate: String?,
    val endTime: String?,   // "HH:mm", "" (för heldag), eller null
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
                Log.e("CalendarRepository", "Error fetching events from $url", e)
            }
        }
        return allEvents
    }

    private suspend fun downloadIcsFile(url: String): String = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", "MirrorApp/1.0")
                setRequestProperty("Accept", "text/calendar, text/plain, */*")
                instanceFollowRedirects = true
            }

            val statusCode = connection.responseCode
            // Log.d("CalendarRepository", "Response Code: $statusCode for $url")

            if (statusCode in 300..399) {
                val redirectedUrl = connection.getHeaderField("Location") ?: url
                Log.d("CalendarRepository", "Redirected from $url to $redirectedUrl")
                connection.disconnect()
                return@withContext downloadIcsFile(redirectedUrl)
            }

            if (statusCode != HttpURLConnection.HTTP_OK) {
                val errorBody = connection.errorStream?.let {
                    BufferedReader(InputStreamReader(it)).use(BufferedReader::readText)
                } ?: "No error body"
                throw IOException("HTTP error $statusCode for $url. $errorBody")
            }

            val contentType = connection.getHeaderField("Content-Type")
            // Log.d("CalendarRepository", "Content-Type: $contentType for $url")

            if (contentType?.contains("text/calendar", ignoreCase = true) == true) {
                return@withContext connection.inputStream.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).readText()
                }
            } else {
                val preview = connection.inputStream.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).readText().take(500)
                }
                throw IOException("Expected 'text/calendar' but got '$contentType' for $url. Preview: $preview")
            }
        } catch (e: Exception) {
            Log.e("CalendarRepository", "Error downloading ICS file from $url", e)
            throw e
        }
    }

    private fun parseIcsEvents(icsData: String, color: Color, url: String): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()
        val lines = icsData.lines()

        // Behåll dina befintliga variabler
        var dtStartLineContent: String? = null
        var dtEndLineContent: String? = null
        var recurrenceRule: String? = null
        var isAllDayEvent = false

        var insideEvent = false
        val eventLines = mutableListOf<String>()

        for (line in lines) {
            when {
                line.startsWith("BEGIN:VEVENT") -> {
                    insideEvent = true
                    eventLines.clear()
                    // Återställ dessa här för varje nytt event
                    dtStartLineContent = null
                    dtEndLineContent = null
                    recurrenceRule = null
                    isAllDayEvent = false
                }
                line.startsWith("END:VEVENT") -> {
                    insideEvent = false

                    var rawTitle = "" // Byt namn för tydlighet, detta är den råa titeln
                    var tempDtStartLine: String? = null
                    var tempDtEndLine: String? = null
                    var tempRRule: String? = null

                    for (evLine in eventLines) {
                        when {
                            // ******** ÄNDRINGEN BÖRJAR HÄR ********
                            evLine.startsWith("SUMMARY:") -> {
                                val summaryValue = evLine.removePrefix("SUMMARY:").trim()
                                // Rensa bort escapade tecken från summaryn
                                rawTitle = summaryValue
                                    .replace("\\\\", "\\") // Först \\ -> \  (för att hantera ett escapat \)
                                    .replace("\\,", ",")   // Sedan \, -> ,
                                    .replace("\\;", ";")   // Och \; -> ;
                                    // Lägg till fler vid behov, t.ex. för nyradstecken
                                    .replace("\\n", "\n")  // \n -> nyrad
                                    .replace("\\N", "\n")  // \N -> nyrad (enligt vissa ICS-implementationer)
                                Log.d("ICS_Parse", "Raw Summary Value: '$summaryValue', Cleaned Title: '$rawTitle'")
                            }
                            // ******** ÄNDRINGEN SLUTAR HÄR ********
                            evLine.startsWith("DTSTART") -> tempDtStartLine = evLine
                            evLine.startsWith("DTEND") -> tempDtEndLine = evLine
                            evLine.startsWith("RRULE:") -> tempRRule = evLine.removePrefix("RRULE:").trim()
                        }
                    }
                    // Denna variabel ska nu heta rawTitle eller liknande
                    // currentTitleInternal = rawTitle // Du kommer använda 'rawTitle' direkt nedan

                    dtStartLineContent = tempDtStartLine
                    dtEndLineContent = tempDtEndLine
                    recurrenceRule = tempRRule

                    if (dtStartLineContent != null) {
                        isAllDayEvent = dtStartLineContent!!.contains("VALUE=DATE")
                        val parsedStartDate = parseDateFromLine(dtStartLineContent!!)?.let { LocalDate.parse(it) }
                        val parsedStartTime = parseTimeFromLine(dtStartLineContent!!)

                        var parsedEndDate: LocalDate? = null
                        var parsedEndTime: String? = null

                        if (dtEndLineContent != null) {
                            val isDtEndValueDate = dtEndLineContent!!.contains("VALUE=DATE")
                            parsedEndDate = parseDateFromLine(dtEndLineContent!!)?.let { LocalDate.parse(it) }
                            parsedEndTime = if (isAllDayEvent || isDtEndValueDate) {
                                if (isAllDayEvent) "" else null // Tidigare var det "" för heldag, ändrat till null för konsekvens
                            } else {
                                parseTimeFromLine(dtEndLineContent!!)
                            }

                            if (isAllDayEvent && parsedEndDate != null && parsedStartDate != null && parsedEndDate.isAfter(parsedStartDate)) {
                                val dtEndTimeFromDtEndLine = parseTimeFromLine(dtEndLineContent!!)
                                if (dtEndTimeFromDtEndLine.isNullOrEmpty() || dtEndTimeFromDtEndLine == "00:00") {
                                    parsedEndDate = parsedEndDate.minusDays(1)
                                }
                            }
                        } else { // Om DTEND saknas
                            parsedEndDate = parsedStartDate
                            parsedEndTime = if(isAllDayEvent) null else parsedStartTime
                        }

                        // Använd den RENSADE titeln här (rawTitle från loopen ovan)
                        if (!rawTitle.isBlank() && parsedStartDate != null) {
                            val eventStartDateStr = parsedStartDate.toString()
                            val eventEndDateStr = (parsedEndDate ?: parsedStartDate).toString()

                            // titleForEvent blir nu direkt rawTitle som redan är rensad
                            val titleForEvent = rawTitle

                            if (recurrenceRule != null) {
                                // Din logik för återkommande event (se till att den får den rensade titeln)
                                val originalDuration = if (parsedStartDate != null && parsedEndDate != null && !isAllDayEvent) {
                                    val startDateTime = parsedStartDate.atTime(LocalTime.parse(parsedStartTime ?: "00:00"))
                                    val endDateTime = parsedEndDate.atTime(LocalTime.parse(parsedEndTime ?: parsedStartTime ?: "00:00"))
                                    Duration.between(startDateTime, endDateTime)
                                } else if (parsedStartDate != null && parsedEndDate != null && isAllDayEvent) {
                                    Duration.between(parsedStartDate.atStartOfDay(), parsedEndDate.plusDays(1).atStartOfDay()) // För heldagsevent
                                }
                                else {
                                    Duration.ZERO
                                }


                                val recurrenceDates = generateRecurringDates(parsedStartDate, recurrenceRule)
                                for (recDate in recurrenceDates) {
                                    val currentRecEndDate: LocalDate
                                    val currentRecEndTime: String?

                                    if (isAllDayEvent) {
                                        currentRecEndDate = if (originalDuration != Duration.ZERO && !originalDuration.isNegative) {
                                            recDate.plusDays(originalDuration.toDays() -1) // -1 eftersom DTEND är exklusiv för heldag
                                        } else {
                                            recDate
                                        }
                                        currentRecEndTime = null
                                    } else {
                                        val recStartDateTime = recDate.atTime(LocalTime.parse(parsedStartTime ?: "00:00"))
                                        val recEndDateTime = recStartDateTime.plus(originalDuration)
                                        currentRecEndDate = recEndDateTime.toLocalDate()
                                        currentRecEndTime = recEndDateTime.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
                                    }

                                    events.add(
                                        CalendarEvent(
                                            title = titleForEvent, // REN titel
                                            startDate = recDate.toString(),
                                            startTime = parsedStartTime, // Behåll original starttid för varje instans
                                            endDate = currentRecEndDate.toString(),
                                            endTime = if (isAllDayEvent) null else currentRecEndTime,
                                            color = color,
                                            isAllDay = isAllDayEvent,
                                            sourceUrl = url
                                        )
                                    )
                                }
                            } else { // För icke-återkommande event
                                events.add(
                                    CalendarEvent(
                                        title = titleForEvent, // REN titel
                                        startDate = eventStartDateStr,
                                        startTime = if(isAllDayEvent) null else parsedStartTime,
                                        endDate = eventEndDateStr,
                                        endTime = if(isAllDayEvent) null else parsedEndTime,
                                        color = color,
                                        isAllDay = isAllDayEvent,
                                        sourceUrl = url
                                    )
                                )
                            }
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
            val valuePart = line.substringAfter(":") // Hämta värdet efter första :
            val actualDateStr = valuePart.take(8) // Ta de första 8 tecknen (YYYYMMDD)

            if (line.contains("TZID")) {
                // Om TZID finns, försök parsa som LocalDateTime och konvertera till UTC, sedan till LocalDate
                // Detta är en förenkling; korrekt TZID-hantering är komplex.
                val rawDateTime = line.substringAfter(":").trim()
                // Extrahera tidzonen mer robust
                val tzidPattern = Regex("TZID=([^:;]+)")
                val timeZoneString = tzidPattern.find(line)?.groupValues?.get(1)?.replace(" ", "_") ?: ZoneId.systemDefault().id

                val formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
                val localDateTime = LocalDateTime.parse(rawDateTime, formatter) // Antag att tiden är specificerad
                val zoneId = try { ZoneId.of(timeZoneString) } catch (e: Exception) { ZoneId.systemDefault() }
                ZonedDateTime.of(localDateTime, zoneId).withZoneSameInstant(ZoneOffset.UTC).toLocalDate().toString()

            } else if (line.contains("VALUE=DATE") || (valuePart.length == 8 && valuePart.all { it.isDigit() })) {
                // Om det är VALUE=DATE eller bara YYYYMMDD
                LocalDate.parse(actualDateStr, DateTimeFormatter.BASIC_ISO_DATE).toString()
            } else if (valuePart.endsWith("Z")) { // UTC tid
                val dateTimeStr = valuePart.removeSuffix("Z")
                val formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
                LocalDateTime.parse(dateTimeStr, formatter).atZone(ZoneOffset.UTC).toLocalDate().toString()
            }
            else { // Lokal tid utan tidszonsinfo (tolkas som systemets default)
                val dateTimeStr = valuePart
                val formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
                LocalDateTime.parse(dateTimeStr, formatter).atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC).toLocalDate().toString()
            }
        } catch (e: Exception) {
            Log.e("CalendarRepository", "Error parsing date from line: \"$line\"", e)
            null
        }
    }

    private fun parseTimeFromLine(line: String): String? { // Kan returnera "" för heldag, null om ingen tid/fel
        try {
            if (!(line.startsWith("DTSTART") || line.startsWith("DTEND"))) return null

            val valuePart = line.substringAfter(":")
            val targetZoneId = ZoneId.of("Europe/Stockholm") // Eller din önskade tidszon

            if (line.contains("VALUE=DATE") || (valuePart.length == 8 && valuePart.all { it.isDigit() }) ) {
                // Explicit heldag eller bara datum angivet
                return null // Ändrat från "" till null för att vara tydligare med "ingen tid"
            }
            if (!valuePart.contains("T")) {
                Log.w("CalendarRepo_TimeParse", "No 'T' separator in datetime string, assuming no time: $valuePart from $line")
                return null // Ingen tidinformation
            }

            val dateTimeString = valuePart
            val zonedDateTime: ZonedDateTime

            if (line.contains("TZID=")) {
                val tzidPattern = Regex("TZID=([^:;]+)")
                val tzidMatch = tzidPattern.find(line)
                val specifiedTzStr = tzidMatch?.groupValues?.get(1)?.replace(" ", "_")
                val sourceZoneId = try {
                    specifiedTzStr?.let { ZoneId.of(it) }
                } catch (e: Exception) {
                    Log.w("CalendarRepo_TimeParse", "Invalid TZID '$specifiedTzStr' in line: $line, falling back.")
                    null
                }

                val formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
                val localDateTime = LocalDateTime.parse(dateTimeString, formatter)
                zonedDateTime = ZonedDateTime.of(localDateTime, sourceZoneId ?: targetZoneId) // Fallback till target om TZID är ogiltig/saknas

            } else if (dateTimeString.endsWith("Z")) {
                val formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
                zonedDateTime = ZonedDateTime.parse(dateTimeString, formatter.withZone(ZoneOffset.UTC))
            } else {
                // Lokal tid utan explicit tidszonsinformation - antar att den är i targetZoneId redan
                // Detta är en vanlig fallgrop; ICS specificerar ofta UTC eller med TZID.
                // Om ingen zoninfo, bör den tolkas som "floating time" eller enligt kontext.
                // För säkerhets skull, anta att den är UTC om inget annat sägs, eller din target.
                // Låt oss för detta exempel anta att om ingen zon finns, är den i targetZoneId.
                val formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
                val localDateTime = LocalDateTime.parse(dateTimeString, formatter)
                zonedDateTime = localDateTime.atZone(targetZoneId) // Antagande
            }

            return zonedDateTime.withZoneSameInstant(targetZoneId)
                .toLocalTime()
                .format(DateTimeFormatter.ofPattern("HH:mm"))

        } catch (e: Exception) {
            Log.e("CalendarRepository", "Error parsing time from line: \"$line\". Error: ${e.message}", e)
            return null // Returnera null vid fel
        }
    }


    private fun generateRecurringDates(startDate: LocalDate, rule: String): List<LocalDate> {
        val dates = mutableListOf<LocalDate>()
        val freq = Regex("FREQ=([A-Z]+)").find(rule)?.groupValues?.get(1) ?: return dates
        val interval = Regex("INTERVAL=(\\d+)").find(rule)?.groupValues?.get(1)?.toIntOrNull() ?: 1
        val count = Regex("COUNT=(\\d+)").find(rule)?.groupValues?.get(1)?.toIntOrNull()
        val untilRaw = Regex("UNTIL=([0-9A-Za-zTZ:]+)").find(rule)?.groupValues?.get(1)

        val untilDate = try {
            untilRaw?.let {
                val cleanUntil = it.removeSuffix("Z").replace("T","").take(8) // Ta YYYYMMDD delen
                if (cleanUntil.length == 8 && cleanUntil.all { char -> char.isDigit() }) {
                    LocalDate.parse(cleanUntil, DateTimeFormatter.BASIC_ISO_DATE)
                } else {
                    Log.w("CalendarRepository", "Unparseable UNTIL date format: $it")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("CalendarRepository", "Error parsing UNTIL date: $untilRaw", e)
            null
        }

        var current = startDate
        var generated = 0
        val maxRecurrences = 365 * 2 // Begränsa till ca 2 års framtida händelser för prestanda

        loop@ while (generated < maxRecurrences) {
            if (count != null && generated >= count) break@loop // Om COUNT finns och är nådd
            if (untilDate != null && current.isAfter(untilDate)) break@loop // Om UNTIL finns och är passerad

            dates.add(current)
            generated++

            if (count != null && generated >= count) break@loop // Kolla igen efter add för att inte generera en för mycket
            if (untilDate != null && current.isAfter(untilDate)) break@loop // Kolla igen

            current = when (freq) {
                "DAILY" -> current.plusDays(interval.toLong())
                "WEEKLY" -> current.plusWeeks(interval.toLong())
                "MONTHLY" -> current.plusMonths(interval.toLong())
                "YEARLY" -> current.plusYears(interval.toLong())
                else -> {
                    Log.w("CalendarRepository", "Unsupported FREQ in RRULE: $freq")
                    break@loop
                }
            }
        }
        if (generated >= maxRecurrences && (count == null && untilDate == null)) { // Logga bara om inte count/until begränsade
            Log.w("CalendarRepository", "Max recurrences ($maxRecurrences) reached for RRULE: '$rule' starting at $startDate. Truncating.")
        }
        return dates
    }
}
