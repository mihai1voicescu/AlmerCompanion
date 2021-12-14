package io.almer.almercompanion.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.almer.almercompanion.LocalNavHostController
import io.almer.almercompanion.MainApp.Companion.mainApp
import io.almer.almercompanion.composable.loaders.SubmitView
import io.almer.almercompanion.composable.loaders.ViewLoader
import io.almer.almercompanion.composable.navigation.BackButton
import io.almer.almercompanion.composable.select.itemSelector
import io.almer.almercompanion.composable.text.BodyText
import io.almer.almercompanion.safePopBackStack
import io.almer.companionshared.model.BluetoothDevice
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.toCollection

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BluetoothScreen() {
    val app = mainApp()
    val navController = LocalNavHostController.current
    val scope = rememberCoroutineScope()

    var scanningJob by remember {
        mutableStateOf<Job?>(null)
    }

    val scanDevices = remember { mutableStateListOf<BluetoothDevice>() }

    fun startScan() {
        if (scanningJob == null) {
            scanDevices.clear()
            scanningJob = scope.launch {
                app.link.scanBluetooth().toCollection(scanDevices)
                scanningJob = null
            }
        }
    }

    fun stopScan() {
        scanningJob?.cancel()
        scanningJob = null
    }

    fun onSelect(device: BluetoothDevice, toggle: () -> Unit) {
        GlobalScope.launch {
            app.link.selectBluetooth(device.name)
            toggle()
            navController.safePopBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Bluetooth") },
                navigationIcon = { BackButton() },
                actions = {
                    Button(onClick = {
                        if (scanningJob != null) {
                            stopScan()
                        } else {
                            startScan()
                        }
                    }) {
                        if (scanningJob != null) {
                            Text(text = "Stop")
                        } else {
                            Text(text = "Scan")
                        }
                    }
                },
            )
        },
    ) {
        ViewLoader(
            stateLoader = {
                delay(300)

                app.link.pairedDevices()
            }
        ) {

            SubmitView { toggle ->
                LazyColumn {
                    stickyHeader {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Text(
                                "Paired devices",
                                Modifier
                                    .padding(bottom = 6.dp, top = 2.dp)
                            )
                        }
                    }
//                    item { Divider() }
                    it.map { bluetoothDevice ->
                        itemSelector(element = bluetoothDevice, onSelect = {
                            onSelect(bluetoothDevice, toggle = toggle)
                        }) {
                            BodyText(it.name)
                        }
                    }
                    item { Divider(Modifier.padding(vertical = 10.dp), thickness = 3.dp) }
                    stickyHeader {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Text(
                                "Available devices",
                                Modifier
                                    .padding(bottom = 6.dp, top = 2.dp)
                            )
                        }
                    }
                    scanDevices.map { bluetoothDevice ->
                        itemSelector(element = bluetoothDevice, onSelect = {
                            onSelect(bluetoothDevice, toggle = toggle)
                        }) {
                            BodyText(it.name)
                        }
                    }
                }
            }
        }
    }
}

val NavController.pathToBluetoothScreen get() = "bluetooth"
