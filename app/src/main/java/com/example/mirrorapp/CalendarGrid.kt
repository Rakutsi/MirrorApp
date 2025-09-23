package com.example.mirrorapp

import com.example.mirrorapp.repository.CalendarEvent
import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue // Importera om det behövs av annan kod
import androidx.compose.runtime.setValue  // Importera om det behövs av annan kod
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle as ComposeTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle as JavaTextStyle
import java.util.*
import java.time.format.DateTimeParseException
import androidx.compose.ui.res.stringResource
import com.example.mirrorapp.R

@Composable
fun MarqueeText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    softWrap: Boolean = false, // Ska vara false för marquee
    style: ComposeTextStyle = LocalTextStyle.current,
    marqueeDelayMillis: Long = 2500,
    marqueeGapMillis: Long = 2000,
    marqueeSpeedFactor: Float = 20f
) {
    var textLayoutResultState by remember { mutableStateOf<TextLayoutResult?>(null) }
    var containerWidthState by remember { mutableStateOf(0) }
    val animatedOffset = remember { Animatable(0f) }

    Log.d("MarqueeFull", "Recomposing MarqueeText for '$text'")
    Log.d("MarqueeFull", "Initial states: textLayoutResult is ${if (textLayoutResultState == null) "null" else "set"}, containerWidth is $containerWidthState")

    LaunchedEffect(text, textLayoutResultState, containerWidthState) {
        val currentTextLayout = textLayoutResultState
        val currentContainerWidth = containerWidthState

        Log.d("MarqueeFull-Effect", "Triggered. Text: '$text'")
        Log.d("MarqueeFull-Effect", "ContainerWidth: $currentContainerWidth, TextLayout Width: ${currentTextLayout?.size?.width}")

        if (currentTextLayout == null || currentContainerWidth == 0) {
            Log.d("MarqueeFull-Effect", "TextLayout or ContainerWidth not ready. Snapping to 0 and returning.")
            animatedOffset.snapTo(0f)
            return@LaunchedEffect
        }

        val textWidth = currentTextLayout.size.width
        val shouldMarquee = textWidth > currentContainerWidth

        Log.d("MarqueeFull-Effect", "Calculated: textWidth=$textWidth, containerWidth=$currentContainerWidth, shouldMarquee=$shouldMarquee")

        if (shouldMarquee) {
            val distanceToScroll = textWidth - currentContainerWidth
            if (distanceToScroll <= 0) {
                Log.w("MarqueeFull-Effect", "Distance to scroll is not positive ($distanceToScroll). Snapping.")
                animatedOffset.snapTo(0f)
                return@LaunchedEffect
            }

            Log.i("MarqueeFull-Effect", "STARTING MARQUEE for '$text'. Distance: $distanceToScroll")
            while (isActive) {
                Log.d("MarqueeFull-Effect", "Loop Start: Snapping to 0f.")
                animatedOffset.snapTo(0f)
                Log.d("MarqueeFull-Effect", "Loop: Delaying before start ($marqueeDelayMillis ms)")
                delay(marqueeDelayMillis)
                if (!isActive) { Log.d("MarqueeFull-Effect", "Loop: Inactive post-delay. Breaking."); break }

                Log.d("MarqueeFull-Effect", "Loop: Animating to ${-distanceToScroll.toFloat()}. Duration: ${(distanceToScroll * marqueeSpeedFactor).toInt().coerceAtLeast(100)}ms")
                animatedOffset.animateTo(
                    targetValue = -distanceToScroll.toFloat(),
                    animationSpec = tween(
                        durationMillis = (distanceToScroll * marqueeSpeedFactor).toInt().coerceAtLeast(100),
                        easing = LinearEasing
                    )
                )
                Log.d("MarqueeFull-Effect", "Loop: Animation finished. Offset: ${animatedOffset.value}")
                if (!isActive) { Log.d("MarqueeFull-Effect", "Loop: Inactive post-animation. Breaking."); break }

                Log.d("MarqueeFull-Effect", "Loop: Delaying for gap ($marqueeGapMillis ms)")
                delay(marqueeGapMillis)
                if (!isActive) { Log.d("MarqueeFull-Effect", "Loop: Inactive post-gap. Breaking."); break }
                Log.d("MarqueeFull-Effect", "Loop: Restarting.")
            }
            Log.d("MarqueeFull-Effect", "Marquee loop for '$text' ended or became inactive.")
        } else {
            Log.d("MarqueeFull-Effect", "NOT MARQUEEING for '$text'. Snapping offset to 0.")
            animatedOffset.snapTo(0f)
        }
    }

    SubcomposeLayout(modifier = modifier.clipToBounds()) { constraints ->
        if (containerWidthState != constraints.maxWidth) {
            Log.d("MarqueeFull-Subcompose", "ContainerWidth changing from $containerWidthState to ${constraints.maxWidth} for '$text'")
            containerWidthState = constraints.maxWidth
        }

        val textPlaceableForMeasure = subcompose("textToMeasure") {
            Text(
                text = text,
                color = color,
                fontSize = fontSize,
                fontWeight = fontWeight,
                textAlign = textAlign,
                style = style,
                overflow = TextOverflow.Visible,
                maxLines = 1,
                softWrap = false,
                onTextLayout = { result ->
                    if (textLayoutResultState?.size?.width != result.size.width || textLayoutResultState?.layoutInput?.text?.toString() != result.layoutInput.text.toString()) {
                        Log.d("MarqueeFull-onTextLayout", "TextLayout for '$text': MeasuredWidth=${result.size.width}. Updating state.")
                        textLayoutResultState = result
                    }
                }
            )
        }[0].measure(Constraints())
        Log.d("MarqueeFull-Subcompose", "Text '${text}' measured with full width (textToMeasure): ${textPlaceableForMeasure.width}")


        // ---- ÄNDRINGARNA FRÅN FÖREGÅENDE SVAR ÄR HÄR ----
        val visibleTextSubcompose = subcompose("visibleMarqueeText") {
            Text(
                text = text,
                color = color,
                fontSize = fontSize,
                fontWeight = fontWeight,
                textAlign = textAlign,
                style = style,
                overflow = TextOverflow.Visible, // <<< ÄNDRING: Använd Visible här för mätning
                maxLines = 1,
                softWrap = softWrap,
                modifier = Modifier.offset { IntOffset(animatedOffset.value.toInt(), 0) }
            )
        }

        // Mät visibleTextPlaceable MED OBEGRÄNSAD BREDD initialt
        val visibleTextPlaceable = visibleTextSubcompose[0].measure(Constraints(minWidth = 0, maxWidth = Int.MAX_VALUE)) // <<< ÄNDRING

        val layoutWidth = constraints.maxWidth // Bredden på vår faktiska komponent
        val layoutHeight = visibleTextPlaceable.height // Höjden kan tas från texten

        Log.d("MarqueeFull-Subcompose", "Visible text measured with unlimited constraints (visibleTextPlaceable): ${visibleTextPlaceable.width} x ${visibleTextPlaceable.height}")
        Log.d("MarqueeFull-Subcompose", "Layouting with: $layoutWidth x $layoutHeight")

        layout(layoutWidth, layoutHeight) {
            visibleTextPlaceable.placeRelative(0, 0)
        }
        // ---- SLUT PÅ ÄNDRINGARNA ----
    }
}


// --- Hela resten av din CalendarGrid.kt fil följer nedan, oförändrad ---
@Composable
fun CalendarGrid(viewModel: StartViewModel, isDarkMode: Boolean, showHeader: Boolean = true) {
    val weeks by viewModel.weeklyGrid.collectAsState()

    if (showHeader) {
        Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                DayOfWeek.values().forEach { dayOfWeek ->
                    Text(
                        text = dayOfWeek.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault()),
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            var displayedMonthForGrid: Month? = null

            weeks.forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    week.forEach { day ->
                        val isToday = day.date == LocalDate.now()
                        val shouldDisplayMonthName = (day.date.dayOfMonth == 1 ||
                                (week.firstOrNull()?.date?.month == day.date.month && week.indexOf(day) == 0)) &&
                                displayedMonthForGrid != day.date.month

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(115.dp)
                                .border(
                                    0.5.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                    RoundedCornerShape(2.dp)
                                ),
                            contentAlignment = Alignment.TopStart
                        ) {
                            Column(
                                modifier = Modifier.fillMaxHeight()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = if (isToday) {
                                                if (isDarkMode) Color.White.copy(alpha = 0.85f)
                                                else Color.Black.copy(alpha = 0.85f)
                                            } else {
                                                Color.Transparent
                                            },
                                            shape = RoundedCornerShape(
                                                topStart = 2.dp,
                                                topEnd = 2.dp
                                            )
                                        )
                                        .padding(vertical = 2.dp, horizontal = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isToday) {
                                        Text(
                                            text = "${day.date.dayOfMonth}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDarkMode) Color.Black else Color.White
                                        )
                                        if (shouldDisplayMonthName) {
                                            Text(
                                                text = day.date.month.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault()).uppercase(),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDarkMode) Color.DarkGray else Color.LightGray,
                                                modifier = Modifier.padding(start = 4.dp, end = 8.dp)
                                            )
                                            displayedMonthForGrid = day.date.month
                                        } else {
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Text(
                                            text = stringResource(id = R.string.today),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDarkMode) Color.Black else Color.White,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.weight(1f)
                                        )
                                        var tempLeftContentWidthInDp = 0.dp
                                        tempLeftContentWidthInDp += (day.date.dayOfMonth.toString().length * 6).dp
                                        if (shouldDisplayMonthName) {
                                            tempLeftContentWidthInDp += (day.date.month.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault()).length * 5).dp + 4.dp + 8.dp
                                        } else {
                                            tempLeftContentWidthInDp += 4.dp
                                        }
                                        Spacer(modifier = Modifier.width(tempLeftContentWidthInDp))
                                    } else {
                                        Text(
                                            text = "${day.date.dayOfMonth}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (shouldDisplayMonthName) {
                                            Text(
                                                text = day.date.month.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault()).uppercase(),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(start = 4.dp)
                                            )
                                            displayedMonthForGrid = day.date.month
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                if (day.events.isNotEmpty()) {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxHeight()
                                    ) {
                                        items(day.events) { event ->
                                            var eventStartDate: LocalDate? = null
                                            var eventEndDate: LocalDate? = null
                                            try {
                                                eventStartDate = LocalDate.parse(event.startDate)
                                                eventEndDate = LocalDate.parse(event.endDate ?: event.startDate)
                                            } catch (e: DateTimeParseException) {
                                                Log.e("CalendarGrid", "Error parsing event dates for padding: ${event.title}", e)
                                            }

                                            val isCurrentEventMultiDay = eventStartDate != null && eventEndDate != null && eventStartDate != eventEndDate
                                            val isFirstDayOfCurrentEvent = eventStartDate != null && day.date == eventStartDate
                                            val isLastDayOfCurrentEvent = eventEndDate != null && day.date == eventEndDate
                                            val isMiddleDayOfCurrentEvent = isCurrentEventMultiDay && !isFirstDayOfCurrentEvent && !isLastDayOfCurrentEvent &&
                                                    eventStartDate != null && eventEndDate != null &&
                                                    day.date.isAfter(eventStartDate) && day.date.isBefore(eventEndDate)

                                            val shouldGoEdgeToEdge = if (isCurrentEventMultiDay) {
                                                (isFirstDayOfCurrentEvent && !isLastDayOfCurrentEvent) ||
                                                        (isLastDayOfCurrentEvent && !isFirstDayOfCurrentEvent) ||
                                                        isMiddleDayOfCurrentEvent
                                            } else {
                                                false
                                            }

                                            val outerHorizontalPadding = if (shouldGoEdgeToEdge) 0.dp else 2.dp

                                            Box(modifier = Modifier.padding(horizontal = outerHorizontalPadding)) {
                                                EventView(event = event, currentDisplayDate = day.date)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(1.dp))
            }
        }
    }
}

@Composable
fun EventView(
    event: CalendarEvent,
    currentDisplayDate: LocalDate
) {
    var eventStartDate: LocalDate? = null
    var eventEndDate: LocalDate? = null

    try {
        eventStartDate = LocalDate.parse(event.startDate)
        eventEndDate = LocalDate.parse(event.endDate ?: event.startDate)
    } catch (e: DateTimeParseException) {
        Log.e("EventView", "Failed to parse event dates: ${event.startDate}, ${event.endDate}", e)
        Row(modifier = Modifier.padding(vertical = 1.dp)) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                "${event.title} (Error parsing dates)",
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color.Gray
            )
        }
        return
    }

    val luminance = calculateLuminance(event.color)
    val textColor = if (luminance > 0.5) Color.Black else Color.White

    val isMultiDayEvent = eventStartDate != eventEndDate
    val isFirstDayInSpan = currentDisplayDate == eventStartDate
    val isLastDayInSpan = currentDisplayDate == eventEndDate

    val backgroundShape = if (isMultiDayEvent) {
        when {
            isFirstDayInSpan && !isLastDayInSpan -> RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 0.dp, bottomEnd = 0.dp)
            !isFirstDayInSpan && !isLastDayInSpan -> RoundedCornerShape(0.dp)
            !isFirstDayInSpan && isLastDayInSpan -> RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 4.dp, bottomEnd = 4.dp)
            else -> RoundedCornerShape(4.dp)
        }
    } else {
        RoundedCornerShape(4.dp)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(event.color, shape = backgroundShape)
            .padding(horizontal = 3.dp, vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            var prefixTime: String? = null
            var suffixTime: String? = null
            val eventTitle = event.title

            if (!event.isAllDay) {
                if (isFirstDayInSpan && !event.startTime.isNullOrEmpty()) {
                    prefixTime = event.startTime
                }
                if (isMultiDayEvent && isLastDayInSpan && !event.endTime.isNullOrEmpty() && event.endTime != event.startTime) {
                    suffixTime = event.endTime
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                prefixTime?.let {
                    Text(
                        text = "$it -",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor,
                        modifier = Modifier.padding(end = 3.dp)
                    )
                }
                MarqueeText(
                    text = eventTitle,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor,
                    softWrap = false,
                    style = LocalTextStyle.current.copy( // Se till att style matchar
                        color = textColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp
                    ),
                    modifier = Modifier.weight(1f, fill = false),
                    marqueeDelayMillis = 2500,
                    marqueeGapMillis = 2000,
                    marqueeSpeedFactor = 18f // Lite långsammare som du hade i EventView
                )
                suffixTime?.let {
                    Text(
                        text = "- $it",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor,
                        modifier = Modifier.padding(start = 3.dp)
                    )
                }
            }
        }
    }
}

fun calculateLuminance(color: Color): Float {
    return (0.2126f * color.red + 0.7152f * color.green + 0.0722f * color.blue)
}

