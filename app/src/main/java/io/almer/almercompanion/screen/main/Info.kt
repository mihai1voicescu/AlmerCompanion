package io.almer.almercompanion.screen.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.almer.almercompanion.LocalNavHostController
import io.almer.almercompanion.MainApp.Companion.mainApp
import io.almer.almercompanion.R
import io.almer.almercompanion.screen.pathToBluetoothScreen
import io.almer.almercompanion.screen.pathToWifiScreen

object Info : MainScreenType(R.drawable.ic_round_info_24, R.string.navigation_item_info) {
    @Composable
    override fun Screen() {
        LazyColumn(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            item {
                Battery()
                WiFi()
                Bluetooth()
            }
        }
    }
}

@Composable
@Preview
private fun Battery() {
    InfoItem(
        name = stringResource(R.string.info_item_battery),
        icon = painterResource(id = R.drawable.ic_round_battery_std_24),
        value = "98%"
    )
}

@Composable
@Preview
private fun WiFi() {
    val app = mainApp()

    val wifi by app.link.wifi.collectAsState()
    val nav = LocalNavHostController.current
    InfoItem(
        name = stringResource(R.string.info_item_wifi),
        icon = painterResource(id = R.drawable.ic_baseline_wifi_24),
        value = wifi?.name ?: "Not connected",
        onClick = {
            nav.navigate(nav.pathToWifiScreen)
        }
    )
}


@Composable
@Preview
private fun Bluetooth() {

    BluetoothView("Sony Headphones")
}

@Composable
private fun BluetoothView(
    connectedTo: String?
) {
    val nav = LocalNavHostController.current
    InfoItem(
        name = stringResource(R.string.info_item_bluetooth),
        icon = painterResource(
            id = if (connectedTo == null)
                R.drawable.ic_baseline_bluetooth_disabled_24 else
                R.drawable.ic_round_bluetooth_connected_24
        ),
        value = connectedTo ?: "Not connected",
        onClick = {
            nav.navigate(nav.pathToBluetoothScreen)
        }
    )
}


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun InfoItem(
    name: String,
    icon: Painter,
    value: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(10.dp, 2.dp),
        shape = MaterialTheme.shapes.large,
        onClick = onClick
    ) {
        InfoItemContent(name, icon, value)
    }
}

@Composable
fun InfoItem(
    name: String,
    icon: Painter,
    value: String
) {
    Surface(
        modifier = Modifier
            .padding(10.dp, 2.dp),
        shape = MaterialTheme.shapes.large,
    ) {
        InfoItemContent(name, icon, value)
    }
}

@Composable
private fun InfoItemContent(
    name: String,
    icon: Painter,
    value: String,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = icon,
            contentDescription = name,
            modifier = Modifier
                .size(40.dp)
                .padding(start = 10.dp)
        )
        Text(
            text = name,
            style = MaterialTheme.typography.button,
            modifier = Modifier
                .padding(16.dp)
                .weight(1f)
        )

        Text(
            text = value,
            textAlign = TextAlign.Right,
            style = MaterialTheme.typography.button,
            modifier = Modifier
                .padding(16.dp)
        )
    }
}

@Composable
@Preview
private fun InfoItemPreview() {
    InfoItem(
        name = "Test",
        icon = painterResource(id = R.drawable.ic_round_battery_std_24),
        value = "Value"
    )
}

@Composable
@Preview
private fun ScreenPreview() {
    Info.Screen()
}