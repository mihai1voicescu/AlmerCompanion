package io.almer.almercompanion

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.*
import com.juul.kable.Advertisement
import com.juul.kable.State
import io.almer.almercompanion.MainApp.Companion.mainApp
import io.almer.almercompanion.composable.select.ListSelector
import io.almer.almercompanion.screen.*
import io.almer.almercompanion.ui.theme.AlmerCompanionTheme
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.lighthousegames.logging.logging

private val Log = logging("MainActivity")

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
        mutableListOf(
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
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(android.Manifest.permission.BLUETOOTH_SCAN)
            }
        }
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
        BluetoothGuard()
    }
}


@Composable
fun BluetoothGuard() {

    val bluetoothState by mainApp().bluetoothState.collectAsState()

    when (bluetoothState) {
        BluetoothState.Unknown -> Text("Bluetooth state is currently unknown, please wait")
        BluetoothState.NotSupported -> Text("Device does not support Bluetooth")
        BluetoothState.Off -> Text("Bluetooth is not enabled, please enable it")
        BluetoothState.On -> LinkEnsure()
    }
}

class ResetState {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        return false
    }
}

@OptIn(InternalCoroutinesApi::class)
@Composable
fun LinkEnsure() {
    val app = mainApp()

    val scope = rememberCoroutineScope()
    val link by app.linkState.collectAsState()

    var resetState by remember {
        mutableStateOf(ResetState())
    }

    val advertisers = remember {
        mutableStateMapOf<String, Advertisement>()
    }

    var scanJob by remember {
        mutableStateOf<Job?>(null)
    }

    val currentLink = link
    if (currentLink == null) {
        LaunchedEffect(resetState) {
            advertisers.clear()

            // ensure we start fresh
            assert(advertisers.size == 0)

            Log.i { "Start scanning" }
            scanJob = app.deviceScan.scan().onEach {
                Log.d { "Found new device ${it.name ?: it.address} with ${it.txPower}" }
                advertisers[it.name ?: it.address] = it
            }.launchIn(scope)

        }

        Scaffold(topBar = {
            TopAppBar {
                Text("Scanning for Almer devices")
            }
        }) {
            ListSelector(items = advertisers.values, onSelect = {
                scope.launch {
                    try {
                        app.selectDevice(it)
                    } catch (e: Exception) {
                        Log.e(e) { "Unable to connect to device" }
                    }
                }
            }) {
                Text(it.name ?: it.address)
            }
        }
    } else {

        val linkState by currentLink.state.collectAsState()

        scanJob?.apply {
            if (isActive) {
                Log.i { "Stop scanning" }
                cancel()
            }
        }

        when (linkState) {
            State.Connecting.Bluetooth -> Text("Link Connecting")
            State.Connecting.Services -> Text("Link Connecting")
            State.Connecting.Observes -> Text("Link Connecting")
            State.Connected -> NavigationBootstrap()
            State.Disconnecting -> Text("Link Disconnecting")
            is State.Disconnected -> {
                advertisers.clear()
                LaunchedEffect(scanJob) {
                    Log.i { "Resting the state" }
                    resetState = ResetState()
                    app.disconnectPeripheral()
                }
                Text("Link Disconnected")
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