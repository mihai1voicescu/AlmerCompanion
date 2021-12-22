package io.almer.almercompanion.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import io.almer.almercompanion.LocalNavHostController
import io.almer.almercompanion.MainApp
import io.almer.almercompanion.R
import io.almer.almercompanion.composable.loaders.SubmitView
import io.almer.almercompanion.composable.loaders.ViewLoader
import io.almer.almercompanion.composable.navigation.ReturnableScreen
import io.almer.almercompanion.composable.select.ListSelector
import io.almer.almercompanion.composable.select.itemSelector
import io.almer.almercompanion.composable.text.BodyText
import io.almer.companionshared.model.WiFi
import io.almer.almercompanion.safePopBackStack
import kotlinx.coroutines.*
import timber.log.Timber


@Composable
@Preview
fun WiFiScreen() {
    ReturnableScreen(title = stringResource(R.string.info_item_wifi)) {
        SelectWiFi()
    }
}

val NavController.pathToWifiScreen get() = "wifi"


@Composable
private fun SelectWiFi() {
    val app = MainApp.mainApp()
    val scope = rememberCoroutineScope()
    val navController = LocalNavHostController.current
    var openDialog by remember {
        mutableStateOf(false)
    }

    fun close() {
        openDialog = false
    }

    if (openDialog) {

    }

    ViewLoader(
        stateLoader = {
            app.link.listWiFi()
        }
    ) {
        SubmitView { toogle ->
            SelectWiFiListView(
                options = it,
                onKnownSelect = {
                    toogle()
                    scope.launch {
                        app.link.selectWiFi(it.networkId!!)
                        toogle()

                        navController.safePopBackStack()
                    }
                },
                onUnknownSelect = {
                    openDialog = true
                })
        }
    }
}

data class WifiConnectionInfo(val ssid: String, val password: String)

@Composable
fun WiFiPasswordDialog(
    onClose: () -> Unit,
    onSubmit: (WifiConnectionInfo) -> Unit,
) {
    var password by remember {
        mutableStateOf("")
    }

    AlertDialog(
        onDismissRequest = onClose,
        title = {
            Text("New connection")
        },
        text = {
            Text("Hi")
        },
        confirmButton = {
            TextButton(onClick = { }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = { onSubmit }) {
                Text("Dismiss")
            }
        }
    )

    TextField(
        value = password,
        onValueChange = { password = it },
        visualTransformation = PasswordVisualTransformation()
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SelectWiFiListView(
    options: Collection<WiFi>,
    onKnownSelect: (wifi: WiFi) -> Unit,
    onUnknownSelect: (wifi: WiFi) -> Unit,
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
                    BodyText(it.name)
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

