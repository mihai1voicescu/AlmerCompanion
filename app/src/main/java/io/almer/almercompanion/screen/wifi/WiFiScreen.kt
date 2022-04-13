package io.almer.almercompanion.screen

import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.navigation.NavController
import io.almer.almercompanion.LocalLink
import io.almer.almercompanion.MainApp.Companion.mainApp
import io.almer.almercompanion.R
import io.almer.almercompanion.composable.loaders.SubmitButtonContent
import io.almer.almercompanion.composable.navigation.ReturnableScreen
import io.almer.almercompanion.link.Link
import io.almer.companionshared.model.WiFi
import io.almer.almercompanion.screen.wifi.SelectWiFi
import io.almer.companionshared.model.WifiConnectionInfo
import kotlinx.coroutines.*
import org.lighthousegames.logging.logging

private val Log = logging("WiFiScreen")

@Composable
fun WiFiScreen(
    link: Link = LocalLink.current
) {
    val wifi by link.wifi.collectAsState()

    WiFiScreenView(wifi)
}

@Composable
fun WiFiScreenView(wifi: WiFi?) {
    ReturnableScreen(title = stringResource(R.string.info_item_wifi), actions = {
        Text(wifi?.ssid ?: "Not connected")
    }) {
        SelectWiFi()
    }
}

val NavController.pathToWifiScreen get() = "wifi"

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
    link: Link = LocalLink.current,
) {
    val scope = rememberCoroutineScope()


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
                    link.forgetWiFi(wifi.networkId!!)
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


