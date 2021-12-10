package io.almer.almercompanion.link

import WiFiCommander
import android.content.Context
import android.net.wifi.WifiInfo
import android.os.Build
import io.almer.companionshared.model.BluetoothDevice
import io.almer.companionshared.model.WiFi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private fun WifiInfo.toWiFI(): WiFi {
    val name: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        this.passpointProviderFriendlyName ?: this.ssid
    } else {
        // todo Get a name somehow
        this.ssid
    }

    return WiFi(
        name = name,
        ssid = this.ssid,
        strength = this.linkSpeed, // todo not OK
    )
}

class Link(
    context: Context,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val wifiCommander = WiFiCommander(context)

    private val _wifi = MutableStateFlow(wifiCommander.wifi.value?.toWiFI())
    val wifi = _wifi.asStateFlow()

    private val _bluetooth = MutableStateFlow<BluetoothDevice?>(null)
    val bluetooth: StateFlow<BluetoothDevice?> = _bluetooth


    init {
        scope.launch {
            scope.launch {
                wifiCommander.wifi.collect {
                    _wifi.emit(it?.toWiFI())
                }
            }
            _wifi.emit(wifiCommander.wifi.value?.toWiFI())
        }
    }

    private val mockBluetooth = listOf(
        BluetoothDevice("Sony Headphones", true, "1"),
        BluetoothDevice("Bose Headphones", false, "2"),
    ).map { it.uuid to it }.toMap()


    suspend fun listWiFi(): Collection<WiFi> {
        return wifiCommander.listWifi()
    }

    suspend fun selectWiFi(ssid: String) {
        delay(500)
//        _wifi.emit(mockWifi[ssid])
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