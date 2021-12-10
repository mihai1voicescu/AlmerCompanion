package io.almer.almercompanion.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import io.almer.almercompanion.LocalNavHostController
import io.almer.almercompanion.MainApp
import io.almer.almercompanion.composable.loaders.SubmitView
import io.almer.almercompanion.composable.loaders.ViewLoader
import io.almer.almercompanion.composable.select.ListSelector
import io.almer.almercompanion.composable.text.BodyText
import io.almer.companionshared.model.WiFi
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
    if (options.isEmpty()) {
        BodyText(text = "No available WiFis")
        return
    }
    val known = options.filter { it.isKnow }
    val unknown = options.filter { !it.isKnow }

    Column {
        if (known.isNotEmpty()) {
            BodyText(text = "Known")
            ListSelector(items = known, onSelect = onSelect) {
                BodyText(
                    text = it.name,
                )
            }
        }

        if (unknown.isNotEmpty()) {
            BodyText(text = "Unknown")
            ListSelector(items = unknown, onSelect = onSelect) {
                BodyText(
                    text = it.name,
                )
            }
        }
    }
}

