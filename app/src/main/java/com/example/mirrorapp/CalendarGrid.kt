package com.example.mirrorapp.ui

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mirrorapp.CalendarDay
import com.example.mirrorapp.StartViewModel
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.*
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import java.time.LocalDate
import java.time.Month


@Composable
fun CalendarGrid(viewModel: StartViewModel) {
    val weeks by viewModel.weeklyGrid.collectAsState()

    Column(modifier = Modifier.padding(8.dp)) {
        // Header: Mån - Tis - Ons ...
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
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

        Spacer(modifier = Modifier.height(8.dp))

        var currentMonth: Month? = null
        var monthDisplayed: Boolean = false

        // Veckor
        weeks.forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->

                    // Kontrollera om månaden har ändrats
                    if (currentMonth != day.date.month) {
                        currentMonth = day.date.month
                        monthDisplayed = false  // Sätt tillbaka till false när vi går in i en ny månad
                    }

                    // Om det är dagens datum, gör hela raden röd
                    val isToday = day.date == LocalDate.now()

                    Box(
                        modifier = Modifier
                            .weight(1f) // Säkerställer att alla rutor är lika stora
                            .height(100.dp) // Sätt en exakt höjd på varje ruta
                            .padding(2.dp)
                            .border(1.dp, Color.Gray, RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.TopStart
                    ) {
                        Column(modifier = Modifier.padding(4.dp)) {
                            // Lägg till både månadsnamnet och datumet på samma rad
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isToday) Color.Red else Color.Transparent), // Gör bara raden röd om det är idag
                                horizontalArrangement = Arrangement.Start, // Datum och månad till vänster
                                verticalAlignment = Alignment.CenterVertically // Centrerar vertikalt på raden
                            ) {
                                // Lägg till lite extra avstånd mellan datumet och "Idag"-texten
                                Spacer(modifier = Modifier.width(6.dp)) // Skjuter datumet åt höger

                                // Visa datumet
                                Text(
                                    text = "${day.date.dayOfMonth}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isToday) Color.White else Color.Black // Ändra textfärg om det är idag
                                )

                                // Visa månadsnamnet endast om det inte har visats än för den månaden
                                if (!monthDisplayed) {
                                    Text(
                                        text = currentMonth!!.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isToday) Color.White else Color.Gray, // Ändra textfärg om det är idag
                                        modifier = Modifier.padding(start = 4.dp) // Liten avstånd mellan datum och månad
                                    )
                                    monthDisplayed = true // Markera att månadsnamnet har visats
                                }

                                // För att hantera "Idag"-texten, som ska vara centrerad på raden:
                                if (isToday) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth() // Se till att boxen täcker hela raden
                                    ) {
                                        Text(
                                            text = "Idag",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier
                                                .background(Color.Red, RoundedCornerShape(4.dp)) // Bakgrund täcker hela bredden
                                                .padding(horizontal = 6.dp, vertical = 2.dp) // Padding för att justera utrymme runt texten
                                                .align(Alignment.Center) // Här använder vi align korrekt på Boxen
                                        )
                                    }
                                }
                            }

                            // Event loop - här minskar vi avståndet mellan varje event
                            day.events.forEach { event ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 0.dp, bottom = 0.dp) // Ta bort all padding här för att få eventen att ligga närmare varandra
                                ) {
                                    // Lägg till en liten färgruta för eventets färg
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(event.color)
                                    )

                                    Spacer(modifier = Modifier.width(4.dp))

                                    // Visa eventets titel
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
            Spacer(modifier = Modifier.height(2.dp)) // Reducera avstånd mellan veckorna
        }
    }
}


















