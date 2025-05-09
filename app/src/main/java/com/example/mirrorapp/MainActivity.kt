package com.example.mirrorapp

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mirrorapp.ui.theme.MirrorAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Sätt appen i helskärmsläge
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        actionBar?.hide() // Ta bort ActionBar om den finns

        enableEdgeToEdge()

        setContent {
            MirrorAppTheme {
                val navController = rememberNavController() // Skapa NavController
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController, // Lägg till NavHost
                        startDestination = "start_screen", // Startskärm
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        // Definiera navigeringsdestinationer
                        composable("start_screen") {
                            StartScreenContent(navController) // Skärm 1: StartScreen
                        }
                        composable("home_screen/{urls}/{colors}") { backStackEntry ->
                            // Hämta URL:erna och färgerna från backStackEntry
                            val urls = backStackEntry.arguments?.getString("urls") ?: ""
                            val colors = backStackEntry.arguments?.getString("colors") ?: ""

                            // Navigera till HomeScreen och skicka värdena
                            HomeScreen(navBackStackEntry = backStackEntry, allUrls = urls, allColors = colors)
                        }
                    }
                }
            }
        }
    }
}
