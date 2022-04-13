package io.almer.almercompanion.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.almer.almercompanion.App
import io.almer.almercompanion.link.FakeLink
import io.almer.companionshared.model.BluetoothDevice
import io.almer.companionshared.model.WiFi

@Composable
@Preview
fun AppPreview() {
    App(
        link = FakeLink(
            wifis = mutableListOf(
                WiFi("Wifi1", "Wifi1", 3, 0),
                WiFi("Wifi2", "Wifi2", 4, 1),
                WiFi("Wifi5", "Wifi5", 6, null),
                WiFi("Wifi3", "Wifi3", 5, 2),
                WiFi("Wifi6", "Wifi6", 6, null),
                WiFi("Wifi4", "Wifi4", 6, 3),
                WiFi("Wifi7", "Wifi7", 6, null),
            ),
            pairedDevices = mutableListOf(
                BluetoothDevice("bt1", true, "1231"),
                BluetoothDevice("bt2", true, "1232"),
                BluetoothDevice("bt3", true, "1233"),
                BluetoothDevice("bt4", true, "1234"),
            ),
            scanDevices = mutableListOf(
                BluetoothDevice("bt1", true, "1235"),
                BluetoothDevice("ubt2", false, "1237"),
                BluetoothDevice("bt2", true, "1236"),
                BluetoothDevice("ubt2", false, "1238"),
            ),
        )
    )
}