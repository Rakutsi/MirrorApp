package com.example.mirrorapp

import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun StartScreenContent(navController: NavHostController) {
    var urls by remember { mutableStateOf(listOf(TextFieldValue(""))) }
    var selectedColors by remember { mutableStateOf(listOf(Color.Gray)) }

    val colorOptions = listOf(Color.Red, Color.Green, Color.Blue, Color.Yellow, Color.Cyan, Color.Magenta)
    val viewModel: StartViewModel = viewModel()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Ange en eller flera ICS-URL:er", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(20.dp))

        // Skapa ett fält per URL
        for (index in urls.indices) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(8.dp)
                        .border(1.dp, Color.Gray)
                ) {
                    BasicTextField(
                        value = urls[index],
                        onValueChange = { newText ->
                            val updatedUrls = urls.toMutableList()
                            updatedUrls[index] = newText
                            urls = updatedUrls
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.Black)
                    )
                }

                // Färgvalsknapp
                IconButton(onClick = {
                    // Hämta index för nuvarande färg
                    val currentColorIndex = colorOptions.indexOf(selectedColors[index])

                    // Beräkna nästa index (cykling genom listan)
                    val nextColorIndex = (currentColorIndex + 1) % colorOptions.size

                    // Uppdatera färgen till nästa i listan
                    selectedColors = selectedColors.toMutableList().apply {
                        set(index, colorOptions[nextColorIndex])
                    }
                }) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(selectedColors[index])
                    )
                }


                // Ta bort fält
                if (urls.size > 1) {
                    IconButton(onClick = {
                        urls = urls.toMutableList().apply { removeAt(index) }
                        selectedColors = selectedColors.toMutableList().apply { removeAt(index) }
                    }) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Remove URL")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Lägg till nytt fält
        Button(onClick = {
            urls = urls + TextFieldValue("")
            selectedColors = selectedColors + Color.Gray
        }) {
            Text("Lägg till fält")
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                val nonEmptyPairs = urls.mapIndexedNotNull { index, tfv ->
                    if (tfv.text.isNotBlank()) index to tfv.text else null
                }

                if (nonEmptyPairs.isNotEmpty()) {
                    val nonEmptyUrls = nonEmptyPairs.map { it.second }
                    val nonEmptyColors = nonEmptyPairs.map { (i, _) -> selectedColors.getOrElse(i) { Color.Gray } }

                    // Bygg en Map istället för två separata listor
                    val urlColorMap = nonEmptyUrls.zip(nonEmptyColors).toMap()

                    // Kodning av URL:er
                    val encodedUrls = nonEmptyUrls.map {
                        URLEncoder.encode(it, StandardCharsets.UTF_8.toString())
                    }
                    val encodedColors = nonEmptyColors.joinToString(",") { it.toArgb().toString() }

                    viewModel.fetchEvents(urlColorMap) // Skicka Map istället för List

                    // Skicka URL och färger som parametrar i navController
                    navController.navigate("home_screen/${encodedUrls.joinToString(",")}/${encodedColors}")
                }
            }
        ) {
            Text("Fortsätt")
        }

    }
}


