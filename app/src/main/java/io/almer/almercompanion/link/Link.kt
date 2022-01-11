package io.almer.almercompanion.link

import WiFiCommander
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.net.wifi.WifiInfo
import android.os.Build
import com.juul.kable.Characteristic
import com.juul.kable.Peripheral
import com.juul.kable.logs.Logging
import com.juul.kable.peripheral
import io.almer.almercompanion.screen.WifiConnectionInfo
import io.almer.commander.BluetoothCommander
import io.almer.companionshared.model.BluetoothDevice
import io.almer.companionshared.model.WiFi
import io.almer.companionshared.server.ClientCommandCatalog
import io.almer.companionshared.server.MESSAGE_UUID
import io.almer.companionshared.server.SERVICE_UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import timber.log.Timber

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

@OptIn(ExperimentalSerializationApi::class)
class Link private constructor(
    private val context: Context,
    private val peripheral: Peripheral,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
) {
    private val catalog = ClientCommandCatalog(peripheral)

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
//        return wifiCommander.listWifi()
        val value = peripheral.read(catalog.ListWiFi) ?: error("No connected device")

        return ProtoBuf.decodeFromByteArray(value)
    }

    suspend fun selectWiFi(networkId: Int) {
        return wifiCommander.setWifi(networkId)
    }

    suspend fun forgetWiFi(networkId: Int) {
        return wifiCommander.forgetWifi(networkId)
    }

    suspend fun connectToWifi(wifiInfo: WiFi, connectionInfo: WifiConnectionInfo): String? {
        val wifi = wifiCommander.learnWifiWPA(wifiInfo.ssid, connectionInfo.password)

        wifiCommander.setWifi(wifi)

        return null
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

    companion object {
        suspend operator fun invoke(
            context: Context,
            peripheral: Peripheral,
            scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
        ): Link {
            Timber.i("Peripheral: creating")

            peripheral.state.onEach {
                Timber.i("Peripheral: state: %s", it)
            }.launchIn(scope)

            Timber.i("Peripheral: connecting")
            peripheral.connect()
            Timber.i("Peripheral: connected")

            Timber.i("Successfully selected current peripheral: %s", peripheral)

            return Link(context, peripheral, scope)
        }
    }
}