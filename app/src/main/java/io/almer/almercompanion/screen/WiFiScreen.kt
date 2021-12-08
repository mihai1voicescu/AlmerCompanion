package io.almer.almercompanion.screen

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import io.almer.almercompanion.composable.loaders.ViewLoader
import io.almer.almercompanion.composable.select.ListSelector
import kotlinx.coroutines.delay


data class WiFi(
    val name: String,
    val ssid: String,
    val strength: Int,
    val isKnown: Boolean,
)

@Composable
fun WiFiScreen() {
    SelectWiFi()
}

val NavController.pathToWifiScreen get() = "wifi"


private val mockData = listOf(
    WiFi("Wifi1", "ssid1", 90, true),
    WiFi("Wifi2", "ssid2", 90, false),
    WiFi("Wifi3", "ssid3", 80, true),
    WiFi("Wifi4", "ssid4", 70, false),
    WiFi("Wifi5", "ssid5", 60, true),
    WiFi("Wifi6", "ssid6", 50, false),
)


@Composable
fun SelectWiFi() {
    ViewLoader(
        stateLoader = {
            delay(2000)
            mockData
        }
    ) {
        SelectWiFiListView(options = it, onSelect = {})
    }
}

@Composable
fun SelectWiFiListView(
    options: Collection<WiFi>,
    onSelect: (wifi: WiFi) -> Unit
) {
    ListSelector(items = options, onSelect = onSelect) {
        Text(it.name)
    }
}

