package io.almer.almercompanion.screen

import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import io.almer.almercompanion.LocalNavHostController
import io.almer.almercompanion.MainApp
import io.almer.almercompanion.composable.loaders.SubmitView
import io.almer.almercompanion.composable.loaders.ViewLoader
import io.almer.almercompanion.composable.select.ListSelector
import io.almer.almercompanion.composable.text.BodyText
import io.almer.almercompanion.link.model.WiFi
import io.almer.almercompanion.safePopBackStack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@Composable
@Preview
fun WiFiScreen() {
    SelectWiFi()
}

val NavController.pathToWifiScreen get() = "wifi"


@Composable
private fun SelectWiFi() {
    val app = MainApp.mainApp()
    val navController = LocalNavHostController.current

    ViewLoader(
        stateLoader = {
            app.link.listWiFi()
        }
    ) {
        SubmitView { toogle ->
            SelectWiFiListView(options = it, onSelect = {
                toogle()
                GlobalScope.launch {
                    app.link.selectWiFi(it.ssid)
                    toogle()

                    navController.safePopBackStack()
                }
            })
        }
    }
}

@Composable
private fun SelectWiFiListView(
    options: Collection<WiFi>,
    onSelect: (wifi: WiFi) -> Unit
) {
    ListSelector(items = options, onSelect = onSelect) {
        BodyText(
            text = it.name,
        )
    }
}

