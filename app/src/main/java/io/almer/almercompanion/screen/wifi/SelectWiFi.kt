package io.almer.almercompanion.screen.wifi

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.juul.kable.State
import io.almer.almercompanion.LocalLink
import io.almer.almercompanion.LocalNavHostController
import io.almer.almercompanion.MainApp
import io.almer.almercompanion.composable.loaders.SubmitView
import io.almer.almercompanion.composable.loaders.ViewLoader
import io.almer.almercompanion.link.FakeLink
import io.almer.almercompanion.link.Link
import io.almer.almercompanion.safePopBackStack
import io.almer.almercompanion.screen.WiFiForgetDialog
import io.almer.almercompanion.screen.WiFiPasswordDialog
import io.almer.companionshared.model.BluetoothDevice
import io.almer.companionshared.model.WiFi
import io.almer.companionshared.model.WifiConnectionInfo
import io.almer.companionshared.server.commands.command.PairedDevices
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@Composable
fun SelectWiFi(
    link: Link = LocalLink.current,
    navController: NavHostController = LocalNavHostController.current
) {
    val scope = rememberCoroutineScope()
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
            link.listWiFi()
        }
    ) { wifiList ->

        val collection = if (wifis.isEmpty()) wifiList else wifis

        connectWifi?.let { wifi ->
            WiFiPasswordDialog(
                ssid = wifi.ssid,
                wifi = wifi,
                onClose = {
                    connectWifi = null
//                    navController.popBackStack()
                },
                onSubmit = { wifiConnectionInfo ->
                    link.connectToWifi(wifiConnectionInfo)
                    navController.popBackStack()

                    null
                })
        }

        forgetWifi?.let { wifi ->
            WiFiForgetDialog(
                wifi,
                onClose = {
                    forgetWifi = null
                },
                onForget = {
                    if (wifis.isEmpty()) {
                        wifiList.filter { wifi.ssid != it.ssid }.toCollection(wifis)
                    } else {
                        wifis.removeIf {
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
                        link.selectWiFi(it.networkId!!)
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
