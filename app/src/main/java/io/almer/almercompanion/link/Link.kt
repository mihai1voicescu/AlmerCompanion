package io.almer.almercompanion.link

import io.almer.companionshared.model.BluetoothDevice
import io.almer.companionshared.model.WiFi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class Link {
    private val _wifi = MutableStateFlow<WiFi?>(null)
    val wifi: StateFlow<WiFi?> = _wifi

    private val _bluetooth = MutableStateFlow<BluetoothDevice?>(null)
    val bluetooth: StateFlow<BluetoothDevice?> = _bluetooth


    private val mockWifi = listOf(
        WiFi("Wifi1", "ssid1", 90, true),
        WiFi("Wifi2", "ssid2", 90, false),
        WiFi("Wifi3", "ssid3", 80, true),
        WiFi("Wifi4", "ssid4", 70, false),
        WiFi("Wifi5", "ssid5", 60, true),
        WiFi("Wifi6", "ssid6", 50, false),
    ).map { it.ssid to it }.toMap()

    private val mockBluetooth = listOf(
        BluetoothDevice("Sony Headphones", true, "1"),
        BluetoothDevice("Bose Headphones", false, "2"),
    ).map { it.uuid to it }.toMap()


    suspend fun listWiFi(): Collection<WiFi> {
        delay(200)
        return mockWifi.values
    }

    suspend fun selectWiFi(ssid: String) {
        delay(500)
        _wifi.emit(mockWifi[ssid])
    }

    suspend fun listBluetooth(): Collection<BluetoothDevice> {
        delay(200)
        return mockBluetooth.values
    }

    suspend fun selectBluetooth(uuid: String) {
        delay(500)
        _bluetooth.emit(mockBluetooth[uuid])
    }
}