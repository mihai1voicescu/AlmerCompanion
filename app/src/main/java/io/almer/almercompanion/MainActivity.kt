package io.almer.almercompanion

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.*
import com.juul.kable.Advertisement
import com.juul.kable.peripheral
import io.almer.almercompanion.MainApp.Companion.mainApp
import io.almer.almercompanion.composable.select.ListSelector
import io.almer.almercompanion.link.Link
import io.almer.almercompanion.screen.*
import io.almer.almercompanion.ui.theme.AlmerCompanionTheme
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AlmerCompanionTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    DebugGuard()
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun DebugGuard() {


    val permissions = rememberMultiplePermissionsState(
        listOf(
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.CHANGE_WIFI_STATE,
            android.Manifest.permission.ACCESS_NETWORK_STATE,
            android.Manifest.permission.ACCESS_WIFI_STATE,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                android.Manifest.permission.BLUETOOTH_CONNECT
            } else {
                android.Manifest.permission.BLUETOOTH
            }
        )
    )

    PermissionsRequired(
        multiplePermissionsState = permissions,
        permissionsNotGrantedContent = {
            LaunchedEffect(true) {
                permissions.launchMultiplePermissionRequest()
            }
            Text("No permissions no App")
        },
        permissionsNotAvailableContent = {
            Text("Device does not have the required permissions")
        }) {
        LinkEnsure()
    }
}


@OptIn(InternalCoroutinesApi::class)
@Composable
fun LinkEnsure() {
    val app = mainApp()

    val scope = rememberCoroutineScope()
    val link by app.linkState.collectAsState()

    val advertisers = remember {
        mutableStateMapOf<String, Advertisement>()
    }

    if (link == null) {
        LaunchedEffect(true) {
            advertisers.clear()
            scope.launch {
                app.deviceScan.scan().collect {
                    advertisers[it.address] = it
                }
            }
        }

        ListSelector(items = advertisers.values, onSelect = {
            scope.launch {
                app.selectDevice(it)
            }
        }) {
            Text(it.name ?: it.address)
        }
    } else {
        NavigationBootstrap()
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