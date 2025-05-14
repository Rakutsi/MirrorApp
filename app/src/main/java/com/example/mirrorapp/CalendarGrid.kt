package com.example.mirrorapp

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mirrorapp.CalendarDay
import com.example.mirrorapp.StartViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.util.*



@Composable
fun CalendarGrid(viewModel: StartViewModel, isDarkMode: Boolean, showHeader: Boolean = true) {
    val weeks by viewModel.weeklyGrid.collectAsState()
    val isDarkTheme = isDarkMode


    Log.d("CalendarGrid", "isDarkTheme = $isDarkTheme")

    Column(modifier = Modifier.padding(8.dp)) {
        // Header: Mån - Tis - Ons ...
        if (showHeader) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DayOfWeek.values().forEach { day ->
                    Text(
                        text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        var currentMonth: Month? = null
        var monthDisplayed: Boolean = false

        // Veckor
        weeks.forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->

                    if (currentMonth != day.date.month) {
                        currentMonth = day.date.month
                        monthDisplayed = false
                    }

                    val isToday = day.date == LocalDate.now()

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp)
                            .padding(2.dp)
                            .border(1.dp, Color.Gray, RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.TopStart
                    ) {
                        Column(modifier = Modifier.padding(4.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        when {
                                            isToday && isDarkTheme -> Color.White
                                            isToday && !isDarkTheme -> Color.Black
                                            else -> Color.Transparent
                                        }
                                    ),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Spacer(modifier = Modifier.width(6.dp))

                                // Datum
                                Text(
                                    text = "${day.date.dayOfMonth}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        isToday && isDarkTheme -> Color.Black
                                        isToday && !isDarkTheme -> Color.White
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )

                                // Månadsnamn
                                if (!monthDisplayed) {
                                    Text(
                                        text = currentMonth!!.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            isToday && isDarkTheme -> Color.Black
                                            isToday && !isDarkTheme -> Color.White
                                            else -> Color.Gray
                                        },
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                    monthDisplayed = true
                                }

                                // "Idag"-etikett
                                if (isToday) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Idag",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDarkTheme) Color.Black else Color.White,
                                            modifier = Modifier
                                                .background(
                                                    if (isDarkTheme) Color.White else Color.Black,
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                .align(Alignment.Center)
                                        )
                                    }
                                }
                            }

                            // Eventlistan
                            day.events.forEach { event ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 0.dp, bottom = 0.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(event.color)
                                    )

                                    Spacer(modifier = Modifier.width(4.dp))

                                    Text(
                                        text = event.title,
                                        fontSize = 10.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}
