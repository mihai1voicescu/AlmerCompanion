package io.almer.almercompanion.screen

import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import io.almer.almercompanion.composable.loaders.ViewLoader
import io.almer.almercompanion.composable.select.ListSelector
import io.almer.almercompanion.composable.text.BodyText
import kotlinx.coroutines.delay


data class WiFi(
    val name: String,
    val ssid: String,
    val strength: Int,
    val isKnown: Boolean,
)

@Composable
@Preview
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
private fun SelectWiFi() {
    ViewLoader(
        stateLoader = {
            delay(100)
            mockData
        }
    ) {
        SelectWiFiListView(options = it, onSelect = {})
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

