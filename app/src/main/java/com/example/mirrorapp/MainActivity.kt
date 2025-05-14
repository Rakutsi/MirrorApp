package com.example.mirrorapp

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mirrorapp.ui.theme.MirrorAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Håll skärmen påslagen medan appen körs
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Fullscreen-läge
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = window?.insetsController
            if (controller != null) {
                window?.decorView?.let {
                    WindowCompat.setDecorFitsSystemWindows(window, false)
                    controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                    controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        }

        enableEdgeToEdge()

        setContent {
            var isDarkMode by rememberSaveable { mutableStateOf(false) }
            val toggleTheme = { isDarkMode = !isDarkMode }

            MirrorAppTheme(darkTheme = isDarkMode, dynamicColor = false) {
                val navController = rememberNavController()

                // Välj bakgrundsfärg baserat på tema
                val backgroundColor = if (isDarkMode) Color.Black else MaterialTheme.colorScheme.background

                Surface(
                    modifier = Modifier.fillMaxSize().background(backgroundColor),
                    color = Color.Transparent // Gör Surface genomskinlig så bakgrunden gäller
                ) {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = "start_screen",
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable("start_screen") {
                                StartScreenContent(
                                    navController = navController,
                                    isDarkMode = isDarkMode,
                                    toggleTheme = toggleTheme
                                )
                            }
                            composable("home_screen/{urls}/{colors}") { backStackEntry ->
                                val urls = backStackEntry.arguments?.getString("urls") ?: ""
                                val colors = backStackEntry.arguments?.getString("colors") ?: ""

                                HomeScreen(
                                    navBackStackEntry = backStackEntry,
                                    allUrls = urls,
                                    allColors = colors,
                                    isDarkMode = isDarkMode
                                )
                            }
                        }
                    }
                }
            }
        }


    }
}
