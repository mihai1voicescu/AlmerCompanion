package io.almer.almercompanion.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.almer.almercompanion.LocalNavHostController
import io.almer.almercompanion.MainApp.Companion.mainApp
import io.almer.almercompanion.R
import io.almer.almercompanion.composable.loaders.SubmitView
import io.almer.almercompanion.composable.loaders.ViewLoader
import io.almer.almercompanion.composable.navigation.BackButton
import io.almer.almercompanion.composable.navigation.ReturnableScreen
import io.almer.almercompanion.composable.select.itemSelector
import io.almer.almercompanion.composable.text.BodyText
import io.almer.almercompanion.safePopBackStack
import io.almer.companionshared.model.BluetoothDevice
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

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

            val localJob = app.link.scanBluetooth()
                .onEach {
                    scanDevices.add(it)
                }
                .onCompletion {
                    scanningJob = null
                }
                .launchIn(scope)

            scanningJob = localJob

            scope.launch {
                delay(13_000)
                if (localJob.isActive) localJob.cancel()
            }
        }
    }

    fun stopScan() {
        scanningJob?.cancel()
        scanningJob = null
    }

    fun onSelect(device: BluetoothDevice, toggle: () -> Unit) {
        scope.launch {
            app.link.selectBluetooth(device.name)
            toggle()
            navController.safePopBackStack()
        }
    }

    fun onForgetSelect(device: BluetoothDevice) {
        scope.launch {
            app.link.forgetBluetooth(device.name)
        }
    }

    ReturnableScreen(
        title = stringResource(R.string.info_item_bluetooth),
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
        }
    ) {
        ViewLoader(
            stateLoader = {
                delay(300)

                app.link.pairedDevices()
            }
        ) { _list ->
            val list = remember {
                mutableStateListOf(*_list.toTypedArray())
            }

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
                    list.map { bluetoothDevice ->
                        itemSelector(element = bluetoothDevice, onSelect = {
                            onSelect(bluetoothDevice, toggle = toggle)
                        }) {
                            Row {
                                BodyText(it.name, modifier = Modifier.weight(1f))

                                IconButton(onClick = {
                                    list.remove(it)
                                    onForgetSelect(it)
                                }) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_baseline_remove_24),
                                        contentDescription = "Remove"
                                    )
                                }
                            }
                        }
                    }
                    item { Divider(Modifier.padding(top = 20.dp), thickness = 3.dp) }
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
