package io.almer.almercompanion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.almer.almercompanion.MainApp.Companion.mainApp
import io.almer.almercompanion.screen.*
import io.almer.almercompanion.ui.theme.AlmerCompanionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AlmerCompanionTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    NavigationBootstrap()
                }
            }
        }
    }
}

@Composable
fun NavigationBootstrap() {
    val navController = rememberNavController()

    CompositionLocalProvider(LocalNavHostController provides navController) {
        NavHost(navController = navController, startDestination = navController.pathToMainScreen) {
            composable(navController.pathToMainScreen) { MainScreen() }
            composable(navController.pathToWifiScreen) { WiFiScreen() }
            composable(navController.pathToBluetoothScreen) { BluetoothScreen() }
        }
    }
}