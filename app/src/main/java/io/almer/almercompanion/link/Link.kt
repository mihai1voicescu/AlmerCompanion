package io.almer.almercompanion.link

import WiFiCommander
import android.content.Context
import android.net.wifi.WifiInfo
import android.os.Build
import io.almer.commander.BluetoothCommander
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
     val bluetoothCommander = BluetoothCommander(context)

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

    suspend fun listWiFi(): Collection<WiFi> {
        return wifiCommander.listWifi()
    }

    suspend fun selectWiFi(networkId: Int) {
        return wifiCommander.setWifi(networkId)
    }

    suspend fun pairedDevices(): Collection<BluetoothDevice> {
        return bluetoothCommander.getBondedDevices()
    }

    fun scanBluetooth(): Flow<BluetoothDevice> {
        return bluetoothCommander.scanDevices()
    }

    suspend fun selectBluetooth(name: String) {
        bluetoothCommander.selectDevice(name)
    }
}