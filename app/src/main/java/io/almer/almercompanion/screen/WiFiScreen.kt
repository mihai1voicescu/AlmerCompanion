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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.almer.almercompanion.LocalNavHostController
import io.almer.almercompanion.MainApp
import io.almer.almercompanion.MainApp.Companion.mainApp
import io.almer.almercompanion.R
import io.almer.almercompanion.composable.loaders.SubmitButtonContent
import io.almer.almercompanion.composable.loaders.SubmitView
import io.almer.almercompanion.composable.loaders.ViewLoader
import io.almer.almercompanion.composable.navigation.ReturnableScreen
import io.almer.almercompanion.composable.select.itemSelector
import io.almer.almercompanion.composable.text.BodyText
import io.almer.companionshared.model.WiFi
import io.almer.almercompanion.safePopBackStack
import io.almer.companionshared.model.WifiConnectionInfo
import kotlinx.coroutines.*
import timber.log.Timber


@Composable
@Preview
fun WiFiScreen() {
    val app = mainApp()

    val wifi by app.link.wifi.collectAsState()

    ReturnableScreen(title = stringResource(R.string.info_item_wifi), actions = {
        Text(wifi?.ssid ?: "Not connected")
    }) {
        SelectWiFi()
    }
}

val NavController.pathToWifiScreen get() = "wifi"


@Composable
private fun SelectWiFi() {
    val app = MainApp.mainApp()
    val scope = rememberCoroutineScope()
    val navController = LocalNavHostController.current
    var connectWifi by remember {
        mutableStateOf<WiFi?>(null)
    }

    var forgetWifi by remember {
        mutableStateOf<WiFi?>(null)
    }

    val wifis = remember {
        mutableStateListOf<WiFi>()
    }
    ViewLoader(
        stateLoader = {
            app.link.listWiFi()
        }
    ) {

        val collection = if (wifis.isEmpty()) it else wifis

        connectWifi?.let { wifi ->
            WiFiPasswordDialog(
                ssid = wifi.ssid,
                wifi= wifi,
                onClose = {
                    navController.popBackStack()
                },
                onSubmit = { wifiConnectionInfo ->
                    app.link.connectToWifi(wifiConnectionInfo)

                    null
                })
        } ?: forgetWifi?.let { wifi ->
            WiFiForgetDialog(
                wifi,
                onClose = {
                    forgetWifi = null
                },
                onForget = {
                    if (wifis.isEmpty()) {
                        it.filter { wifi.ssid != it.ssid }.toCollection(wifis)
                    } else {
                        wifis.removeIf {
                            // todo bug
                            wifi.ssid != it.ssid
                        }
                    }

                    wifis.add(wifi.copy(networkId = null))

                    forgetWifi = null
                })
        }


        SubmitView { toogle ->
            SelectWiFiListView(
                options = collection,
                onKnownSelect = {
                    toogle()
                    scope.launch {
                        app.link.selectWiFi(it.networkId!!)
                        toogle()

                        navController.safePopBackStack()
                    }
                },
                onUnknownSelect = {
                    connectWifi = it
                },
                onForgetSelect = {
                    forgetWifi = it
                }
            )
        }
    }
}

@Composable
fun WiFiPasswordDialog(
    ssid: String,
    onClose: () -> Unit,
    wifi: WiFi,
    onSubmit: suspend (WifiConnectionInfo) -> String?,
) {
    val scope = rememberCoroutineScope()
    var password by remember {
        mutableStateOf("")
    }

    var isSubmitting by remember {
        mutableStateOf(false)
    }

    AlertDialog(
        onDismissRequest = onClose,
        title = {
            Text(ssid)
        },
        text = {
            TextField(
                label = {
                    Text("Password")
                },
                value = password,
                onValueChange = { password = it },
                visualTransformation = PasswordVisualTransformation()
            )
        },
        confirmButton = {
            Button(onClick = {
                isSubmitting = true
                scope.launch {
                    val err = onSubmit(WifiConnectionInfo(password, wifi))

                    if (err != null) {
                        // todo handle show error
                        isSubmitting = false
                    } else {
                        onClose()
                    }
                }
            }) {
                SubmitButtonContent(isLoading = isSubmitting) {
                    Text("Confirm")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onClose) {
                Text("Dismiss")
            }
        }
    )
}

@Composable
fun WiFiForgetDialog(
    wifi: WiFi,
    onClose: () -> Unit,
    onForget: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    val app = mainApp()

    var isSubmitting by remember {
        mutableStateOf(false)
    }

    AlertDialog(
        onDismissRequest = onClose,
        title = {
            Text(wifi.ssid)
        },
        confirmButton = {
            Button(onClick = {
                isSubmitting = true
                scope.launch {
                    app.link.forgetWiFi(wifi.networkId!!)
                    onForget()
                }
            }) {
                SubmitButtonContent(isLoading = isSubmitting) {
                    Text("Delete")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onClose) {
                Text("Dismiss")
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SelectWiFiListView(
    options: Collection<WiFi>,
    onKnownSelect: (wifi: WiFi) -> Unit,
    onUnknownSelect: (wifi: WiFi) -> Unit,
    onForgetSelect: (wifi: WiFi) -> Unit,
) {
    if (options.isEmpty()) {
        BodyText(text = "No available WiFis")
        return
    }

    Timber.d("Available Wifi: %s", options)
    val known = options.filter { it.isKnow }
    val unknown = options.filter { !it.isKnow }
    Timber.d("Known Wifi: %s", known)
    Timber.d("Unknown Wifi: %s", unknown)

    SubmitView { toggle ->
        LazyColumn {
            stickyHeader {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(
                        "Known wifi",
                        Modifier
                            .padding(bottom = 6.dp, top = 2.dp)
                    )
                }
            }
//                    item { Divider() }
            known.map { wifi ->
                itemSelector(element = wifi, onSelect = {
                    onKnownSelect(wifi)
                }) {
                    Row {
                        BodyText(it.name, modifier = Modifier.weight(1f))
                        IconButton(onClick = { onForgetSelect(it) }) {
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
                        "Other wifi",
                        Modifier
                            .padding(bottom = 6.dp, top = 2.dp)
                    )
                }
            }
            unknown.map { wifi ->
                itemSelector(element = wifi, onSelect = {
                    onUnknownSelect(wifi)
                }) {
                    BodyText(it.name)
                }
            }
        }
    }
}

