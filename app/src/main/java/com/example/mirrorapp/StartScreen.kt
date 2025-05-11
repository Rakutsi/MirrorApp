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
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import com.google.gson.Gson
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp


@Composable
fun StartScreenContent(navController: NavHostController) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("mirror_prefs", Context.MODE_PRIVATE)

    // Hämta tidigare sparade URL:er och färger från SharedPreferences
    var urls by remember {
        mutableStateOf(
            run {
                val savedJson = prefs.getString("saved_urls", null)
                if (savedJson != null) {
                    val type = object : com.google.gson.reflect.TypeToken<Map<String, Int>>() {}.type
                    val savedMap: Map<String, Int> = Gson().fromJson(savedJson, type)
                    savedMap.keys.map { TextFieldValue(it) }.ifEmpty { listOf(TextFieldValue("")) }
                } else {
                    listOf(TextFieldValue(""))
                }
            }
        )
    }

    var selectedColors by remember {
        mutableStateOf(
            run {
                val savedJson = prefs.getString("saved_urls", null)
                if (savedJson != null) {
                    val type = object : com.google.gson.reflect.TypeToken<Map<String, Int>>() {}.type
                    val savedMap: Map<String, Int> = Gson().fromJson(savedJson, type)
                    savedMap.values.map { Color(it) }.ifEmpty { listOf(Color.Gray) }
                } else {
                    listOf(Color.Gray)
                }
            }
        )
    }

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
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.Black
                        )

                    )
                }

                IconButton(onClick = {
                    val currentColorIndex = colorOptions.indexOf(selectedColors[index])
                    val nextColorIndex = (currentColorIndex + 1) % colorOptions.size
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

                    // Rensa SharedPreferences
                    val editor = prefs.edit()
                    editor.clear()

                    // Spara nya värden
                    val urlColorMapAsInts = nonEmptyUrls.zip(nonEmptyColors).associate { (url, color) ->
                        url to color.toArgb()
                    }
                    val urlJson = Gson().toJson(urlColorMapAsInts)
                    editor.putString("saved_urls", urlJson)
                    editor.apply()

                    // Anropa ViewModel
                    val urlColorMap = nonEmptyUrls.zip(nonEmptyColors).associate { (url, color) -> url to color }
                    viewModel.fetchEvents(urlColorMap)

                    // Navigera
                    val encodedUrls = nonEmptyUrls.map {
                        URLEncoder.encode(it, StandardCharsets.UTF_8.toString())
                    }
                    val encodedColors = nonEmptyColors.joinToString(",") { it.toArgb().toString() }

                    navController.navigate("home_screen/${encodedUrls.joinToString(",")}/${encodedColors}")
                }
            }
        ) {
            Text("Fortsätt")
        }
    }
}



