package com.example.mirrorapp

import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
// ... andra befintliga importer ...
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import com.google.gson.Gson
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

// ---- NYA/VIKTIGA IMPORTER FÖR STRÄNGRESURSER ----
import androidx.compose.ui.res.stringResource
import com.example.mirrorapp.R // Denna pekar på dina resurs-ID:n
// -------------------------------------------------

@Composable
fun StartScreenContent(
    navController: NavHostController,
    isDarkMode: Boolean,
    toggleTheme: () -> Unit
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("mirror_prefs", Context.MODE_PRIVATE)

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
        // ---- ANVÄND stringResource HÄR ----
        Text(
            text = stringResource(id = R.string.enter_ics_urls_title),
            style = MaterialTheme.typography.headlineSmall
        )
        // ------------------------------------
        Spacer(modifier = Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            for (index in urls.indices) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                            .border(1.dp, Color.Gray)
                    ) {
                        BasicTextField(
                            value = urls[index],
                            onValueChange = { newText ->
                                val updatedUrls = urls.toMutableList()
                                updatedUrls[index] = newText
                                urls = updatedUrls
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            textStyle = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }

                    IconButton(onClick = {
                        val currentColorIndex = colorOptions.indexOf(selectedColors.getOrElse(index) { Color.Gray })
                        val nextColorIndex = (currentColorIndex + 1) % colorOptions.size
                        selectedColors = selectedColors.toMutableList().apply {
                            set(index, colorOptions[nextColorIndex])
                        }
                    }) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(selectedColors.getOrElse(index) { Color.Gray })
                                .border(1.dp, MaterialTheme.colorScheme.outline)
                        )
                    }

                    if (urls.size > 1) {
                        IconButton(onClick = {
                            urls = urls.toMutableList().apply { removeAt(index) }
                            selectedColors = selectedColors.toMutableList().apply { removeAt(index) }
                        }) {
                            // ---- ANVÄND stringResource HÄR ----
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(id = R.string.remove_url_description)
                            )
                            // ------------------------------------
                        }
                    } else {
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val iconButtonSize = 56.dp
            val iconSize = 32.dp
            val buttonSpacing = 24.dp

            IconButton(
                onClick = toggleTheme,
                modifier = Modifier.size(iconButtonSize)
            ) {
                Icon(
                    imageVector = if (isDarkMode) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                    // ---- ANVÄND stringResource HÄR ----
                    contentDescription = stringResource(
                        if (isDarkMode) R.string.switch_to_light_mode_description
                        else R.string.switch_to_dark_mode_description
                    ),
                    // ------------------------------------
                    modifier = Modifier.size(iconSize)
                )
            }

            Spacer(modifier = Modifier.width(buttonSpacing))

            IconButton(
                onClick = {
                    urls = urls + TextFieldValue("")
                    selectedColors = selectedColors + Color.Gray
                },
                modifier = Modifier.size(iconButtonSize)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    // ---- ANVÄND stringResource HÄR ----
                    contentDescription = stringResource(id = R.string.add_field_description),
                    // ------------------------------------
                    modifier = Modifier.size(iconSize)
                )
            }

            Spacer(modifier = Modifier.width(buttonSpacing))

            IconButton(
                onClick = {
                    val nonEmptyPairs = urls.mapIndexedNotNull { index, tfv ->
                        if (tfv.text.isNotBlank()) index to tfv.text else null
                    }

                    if (nonEmptyPairs.isNotEmpty()) {
                        val nonEmptyUrls = nonEmptyPairs.map { it.second }
                        val nonEmptyColors = nonEmptyPairs.map { (i, _) -> selectedColors.getOrElse(i) { Color.Gray } }

                        val editor = prefs.edit()
                        editor.clear()
                        val urlColorMapAsInts = nonEmptyUrls.zip(nonEmptyColors).associate { (url, color) ->
                            url to color.toArgb()
                        }
                        val urlJson = Gson().toJson(urlColorMapAsInts)
                        editor.putString("saved_urls", urlJson)
                        editor.apply()

                        val urlColorMap = nonEmptyUrls.zip(nonEmptyColors).associate { (url, color) -> url to color }
                        viewModel.fetchEvents(urlColorMap)

                        val encodedUrls = nonEmptyUrls.map {
                            URLEncoder.encode(it, StandardCharsets.UTF_8.toString())
                        }
                        val encodedColors = nonEmptyColors.joinToString(",") { it.toArgb().toString() }

                        navController.navigate("home_screen/${encodedUrls.joinToString(",")}/${encodedColors}")
                    }
                },
                modifier = Modifier.size(iconButtonSize)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    // ---- ANVÄND stringResource HÄR ----
                    contentDescription = stringResource(id = R.string.continue_button_description),
                    // ------------------------------------
                    modifier = Modifier.size(iconSize)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        // CalendarGrid(viewModel = viewModel, isDarkMode = isDarkMode, showHeader = false)
    }
}

