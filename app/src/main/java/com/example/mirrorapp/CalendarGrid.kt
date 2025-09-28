package com.example.mirrorapp

// Importera DailyLayout och EventLayoutInfo om de ligger i en annan fil
// import com.example.mirrorapp.DailyLayout
// import com.example.mirrorapp.EventLayoutInfo
import com.example.mirrorapp.repository.CalendarEvent // CalendarEvent behövs för EventViewSimplified
import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.size
// import androidx.compose.foundation.lazy.LazyColumn // Tas bort då den inte används per dag längre
// import androidx.compose.foundation.lazy.items // Likaså
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isEmpty
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle as ComposeTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
// import androidx.compose.ui.unit.Dp // Dp används implicit, explicit import ej nödvändig
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
// import java.time.format.DateTimeParseException // Används inte direkt här längre
import androidx.compose.ui.res.stringResource
import androidx.core.text.color
import com.example.mirrorapp.R
import kotlin.math.roundToInt
import kotlin.text.forEach
import kotlin.text.forEachIndexed
import kotlin.text.indexOf
import kotlin.text.isNotEmpty
import kotlin.text.isNullOrEmpty
import kotlin.text.uppercase

// DEFINIERA KONSTANTER FÖR HÖJD OCH MELLANRUM HÄR
private val EVENT_ROW_MIN_HEIGHT = 18.dp
private val EVENT_SPACING = 2.dp

// MarqueeText (oförändrad)
@Composable
fun MarqueeText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    softWrap: Boolean = false,
    style: ComposeTextStyle = LocalTextStyle.current,
    marqueeDelayMillis: Long = 2500,
    marqueeGapMillis: Long = 2000,
    marqueeSpeedFactor: Float = 20f
) {
    var textLayoutResultState by remember { mutableStateOf<TextLayoutResult?>(null) }
    var containerWidthState by remember { mutableStateOf(0) }
    val animatedOffset = remember { Animatable(0f) }

    LaunchedEffect(text, textLayoutResultState, containerWidthState) {
        val currentTextLayout = textLayoutResultState
        val currentContainerWidth = containerWidthState
        if (text.isEmpty() || currentTextLayout == null || currentContainerWidth == 0) {
            animatedOffset.snapTo(0f)
            return@LaunchedEffect
        }
        val textWidth = currentTextLayout.size.width
        val shouldMarquee = textWidth > currentContainerWidth
        if (shouldMarquee) {
            val distanceToScroll = textWidth - currentContainerWidth
            if (distanceToScroll <= 0) {
                animatedOffset.snapTo(0f)
                return@LaunchedEffect
            }
            while (isActive) {
                animatedOffset.snapTo(0f)
                delay(marqueeDelayMillis)
                if (!isActive) break
                animatedOffset.animateTo(
                    targetValue = -distanceToScroll.toFloat(),
                    animationSpec = tween(
                        durationMillis = (distanceToScroll * marqueeSpeedFactor).toInt().coerceAtLeast(100),
                        easing = LinearEasing
                    )
                )
                if (!isActive) break
                delay(marqueeGapMillis)
                if (!isActive) break
            }
        } else {
            animatedOffset.snapTo(0f)
        }
    }

    SubcomposeLayout(modifier = modifier.clipToBounds()) { constraints ->
        if (containerWidthState != constraints.maxWidth) {
            containerWidthState = constraints.maxWidth
        }
        val textPlaceableForMeasure = subcompose("textToMeasure") {
            Text(text = text, color = color, fontSize = fontSize, fontWeight = fontWeight, textAlign = textAlign, style = style, overflow = TextOverflow.Visible, maxLines = 1, softWrap = false,
                onTextLayout = { result ->
                    if (textLayoutResultState?.size?.width != result.size.width || textLayoutResultState?.layoutInput?.text?.toString() != result.layoutInput.text.toString()) {
                        textLayoutResultState = result
                    }
                }
            )
        }[0].measure(Constraints())
        val visibleTextPlaceable = subcompose("visibleMarqueeText") {
            Text(text = text, color = color, fontSize = fontSize, fontWeight = fontWeight, textAlign = textAlign, style = style, overflow = TextOverflow.Visible, maxLines = 1, softWrap = softWrap, modifier = Modifier.offset { IntOffset(animatedOffset.value.toInt(), 0) })
        }[0].measure(Constraints(minWidth = 0, maxWidth = Int.MAX_VALUE))
        val minHeightForText = textPlaceableForMeasure.height
        layout(constraints.maxWidth, visibleTextPlaceable.height.coerceAtLeast(minHeightForText)) {
            visibleTextPlaceable.placeRelative(0, 0)
        }
    }
}


@Composable
fun CalendarGrid(viewModel: StartViewModel, isDarkMode: Boolean, showHeader: Boolean = true) {
    val weeksLayout by viewModel.weeklyCalendarLayout.collectAsState()
    val numDaysInWeek = DayOfWeek.values().size

    val dayCellHeight = 115.dp
    val weekSpacerHeight = 1.dp

    val gridLineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    val gridStrokeWidth = 0.5.dp

    if (weeksLayout.isEmpty() && showHeader) {
        // Kan visa laddningsindikator etc.
    }

    if (showHeader) {
        Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                DayOfWeek.values().forEach { dayOfWeek ->
                    Text(
                        text = dayOfWeek.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault()),
                        modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))

            var displayedMonthForGrid: Month? = null

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        if (weeksLayout.isEmpty()) return@drawBehind
                        val cellWidth = size.width / numDaysInWeek
                        val strokeWidthPx = gridStrokeWidth.toPx()
                        val halfStroke = strokeWidthPx / 2f
                        val dayCellHeightPx = dayCellHeight.toPx()

                        for (i in 0..numDaysInWeek) {
                            var xIdeal = cellWidth * i
                            if (i == 0) xIdeal = halfStroke
                            else if (i == numDaysInWeek) xIdeal = size.width - halfStroke
                            val xActual = xIdeal.roundToInt().toFloat()
                            drawLine(
                                color = gridLineColor,
                                start = Offset(xActual.coerceIn(halfStroke, size.width - halfStroke), 0f),
                                end = Offset(xActual.coerceIn(halfStroke, size.width - halfStroke), size.height),
                                strokeWidth = strokeWidthPx
                            )
                        }

                        var currentYIdeal = halfStroke
                        drawLine(
                            color = gridLineColor,
                            start = Offset(0f, currentYIdeal.roundToInt().toFloat()),
                            end = Offset(size.width, currentYIdeal.roundToInt().toFloat()),
                            strokeWidth = strokeWidthPx
                        )
                        weeksLayout.forEachIndexed { index, _ ->
                            currentYIdeal += dayCellHeightPx
                            if (index < weeksLayout.size - 1) {
                                currentYIdeal += weekSpacerHeight.toPx()
                            }
                            val yToDrawIdeal = if (index == weeksLayout.size - 1) size.height - halfStroke else currentYIdeal
                            val yActual = yToDrawIdeal.roundToInt().toFloat()
                            drawLine(
                                color = gridLineColor,
                                start = Offset(0f, yActual.coerceIn(halfStroke, size.height - halfStroke)),
                                end = Offset(size.width, yActual.coerceIn(halfStroke, size.height - halfStroke)),
                                strokeWidth = strokeWidthPx
                            )
                        }
                    }
            ) {
                weeksLayout.forEachIndexed { weekIndex, weekDays ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(dayCellHeight),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        weekDays.forEach { dailyLayout ->
                            Box( // DAG-CELL BOX
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(), // VIKTIGT: Ingen .padding(1.dp) här längre
                                contentAlignment = Alignment.TopStart
                            ) {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Row( // Dag-header
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                color = if (dailyLayout.date == LocalDate.now()) {
                                                    if (isDarkMode) Color.White.copy(alpha = 0.85f)
                                                    else Color.Black.copy(alpha = 0.85f)
                                                } else {
                                                    Color.Transparent
                                                },
                                                shape = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)
                                            )
                                            .padding(vertical = 2.dp, horizontal = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val isToday = dailyLayout.date == LocalDate.now()
                                        val currentMonth = dailyLayout.date.month
                                        val shouldDisplayMonthName = (dailyLayout.date.dayOfMonth == 1 || (weekDays.indexOf(dailyLayout) == 0 && displayedMonthForGrid != currentMonth)) && displayedMonthForGrid != currentMonth

                                        if (isToday) {
                                            Text(text = "${dailyLayout.date.dayOfMonth}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.Black else Color.White)
                                            if (shouldDisplayMonthName) {
                                                Text(text = currentMonth.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault()).uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.DarkGray else Color.LightGray, modifier = Modifier.padding(start = 4.dp, end = 8.dp))
                                                displayedMonthForGrid = currentMonth
                                            } else { Spacer(modifier = Modifier.width(4.dp)) }
                                            Text(text = stringResource(id = R.string.today), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.Black else Color.White, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                                            var tempLeftContentWidthInDp = 0.dp; tempLeftContentWidthInDp += (dailyLayout.date.dayOfMonth.toString().length * 6).dp; if (shouldDisplayMonthName) { tempLeftContentWidthInDp += (currentMonth.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault()).length * 5).dp + 4.dp + 8.dp } else { tempLeftContentWidthInDp += 4.dp }; Spacer(modifier = Modifier.width(tempLeftContentWidthInDp))
                                        } else {
                                            Text(text = "${dailyLayout.date.dayOfMonth}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                            if (shouldDisplayMonthName) {
                                                Text(text = currentMonth.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault()).uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
                                                displayedMonthForGrid = currentMonth
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))

                                    if (dailyLayout.eventsInTracks.isNotEmpty()) {
                                        Column(modifier = Modifier.fillMaxHeight().padding(bottom = 1.dp)) { // Lade till padding i botten för att inte sista eventet ska klippas av rutnätet
                                            dailyLayout.eventsInTracks.forEachIndexed { trackIndex, eventLayoutInfo ->
                                                if (eventLayoutInfo != null) {
                                                    val outerHorizontalPadding = if (eventLayoutInfo.isVisualSegmentStart && eventLayoutInfo.isVisualSegmentEnd) 2.dp else 0.dp
                                                    Box(modifier = Modifier.padding(horizontal = outerHorizontalPadding)) {
                                                        EventViewSimplified(
                                                            eventInfo = eventLayoutInfo,
                                                            currentDisplayDate = dailyLayout.date
                                                        )
                                                    }
                                                } else {
                                                    Spacer(modifier = Modifier.fillMaxWidth().height(EVENT_ROW_MIN_HEIGHT))
                                                }
                                                if (trackIndex < dailyLayout.eventsInTracks.size - 1) {
                                                    Spacer(modifier = Modifier.height(EVENT_SPACING))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (weekIndex < weeksLayout.size - 1) {
                        Spacer(modifier = Modifier.height(weekSpacerHeight))
                    }
                }
            }
        }
    }
}

@Composable
fun EventViewSimplified(
    eventInfo: EventLayoutInfo,
    currentDisplayDate: LocalDate
) {
    val event = eventInfo.event
    val totalEventStartDate = eventInfo.totalEventStartDate
    val totalEventEndDate = eventInfo.totalEventEndDate

    val luminance = calculateLuminance(event.color)
    val textColor = if (luminance > 0.5) Color.Black else Color.White

    val isMultiDayEvent = totalEventStartDate != totalEventEndDate

    val backgroundShape: Shape = if (isMultiDayEvent) {
        when {
            eventInfo.isVisualSegmentStart && eventInfo.isVisualSegmentEnd ->
                RoundedCornerShape(4.dp)
            eventInfo.isVisualSegmentStart && !eventInfo.isVisualSegmentEnd ->
                RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 0.dp, bottomEnd = 0.dp)
            !eventInfo.isVisualSegmentStart && eventInfo.isVisualSegmentEnd ->
                RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 4.dp, bottomEnd = 4.dp)
            !eventInfo.isVisualSegmentStart && !eventInfo.isVisualSegmentEnd ->
                RoundedCornerShape(0.dp)
            else ->
                RoundedCornerShape(4.dp)
        }
    } else {
        RoundedCornerShape(4.dp)
    }

    val isActualStartDayForEvent = currentDisplayDate == totalEventStartDate
    val isActualEndDayForEvent = currentDisplayDate == totalEventEndDate
    val isEventActiveOnCurrentDate = !currentDisplayDate.isBefore(totalEventStartDate) && !currentDisplayDate.isAfter(totalEventEndDate)
    val firstDayOfWeekDefinition = DayOfWeek.MONDAY
    val isCurrentDateFirstDayOfWeek = currentDisplayDate.dayOfWeek == firstDayOfWeekDefinition

    val displayTitle: String
    if (isMultiDayEvent) {
        val shouldShowTitleForMultiDay = isActualStartDayForEvent || (isCurrentDateFirstDayOfWeek && isEventActiveOnCurrentDate)
        displayTitle = if (shouldShowTitleForMultiDay) event.title else ""
    } else {
        displayTitle = event.title
    }

    var prefixText: String? = null
    var suffixText: String? = null

    if (!event.isAllDay) {
        if (!isMultiDayEvent) {
            if (!event.startTime.isNullOrEmpty()) {
                prefixText = event.startTime
            }
        } else {
            if (isActualStartDayForEvent && !event.startTime.isNullOrEmpty()) {
                prefixText = "${event.startTime} -"
            }
            if (isActualEndDayForEvent && !isActualStartDayForEvent && !event.endTime.isNullOrEmpty() && event.endTime != event.startTime) {
                suffixText = "- ${event.endTime}"
            } else if (isActualEndDayForEvent && isActualStartDayForEvent && !event.endTime.isNullOrEmpty() && event.endTime != event.startTime && prefixText != null && prefixText.endsWith(" -")) {
                prefixText = "${event.startTime} - ${event.endTime}"
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(EVENT_ROW_MIN_HEIGHT)
            .background(event.color, shape = backgroundShape)
            .padding(horizontal = 3.dp), // Alltid 3.dp här för textens skull
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            prefixText?.let {
                Text(
                    text = it,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor,
                    modifier = Modifier.padding(end = if (displayTitle.isNotEmpty()) 3.dp else 0.dp)
                )
            }

            MarqueeText(
                text = displayTitle,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                softWrap = false,
                style = LocalTextStyle.current.copy(
                    color = textColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp
                ),
                modifier = Modifier.weight(1f, fill = displayTitle.isNotEmpty()),
                marqueeDelayMillis = 2500,
                marqueeGapMillis = 2000,
                marqueeSpeedFactor = 18f
            )

            suffixText?.let {
                Text(
                    text = it,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor,
                    modifier = Modifier.padding(start = if (displayTitle.isNotEmpty() || prefixText != null) 3.dp else 0.dp)
                )
            }
        }
    }
}

fun calculateLuminance(color: Color): Float {
    return (0.2126f * color.red + 0.7152f * color.green + 0.0722f * color.blue)
}

