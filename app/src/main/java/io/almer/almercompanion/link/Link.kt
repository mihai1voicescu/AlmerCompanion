package io.almer.almercompanion.link

import WiFiCommander
import android.content.Context
import android.net.wifi.WifiInfo
import android.os.Build
import com.juul.kable.AndroidPeripheral
import com.juul.kable.Peripheral
import com.juul.kable.characteristicOf
import io.almer.almercompanion.screen.WifiConnectionInfo
import io.almer.commander.BluetoothCommander
import io.almer.companionshared.model.BluetoothDevice
import io.almer.companionshared.model.WiFi
import io.almer.companionshared.server.ClientCommandCatalog
import io.almer.companionshared.server.MESSAGE_UUID
import io.almer.companionshared.server.SERVICE_UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import timber.log.Timber
import kotlin.RuntimeException

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
    val messageCharacteristic = characteristicOf(SERVICE_UUID.toString(), MESSAGE_UUID.toString())

    suspend fun sendMessage(message: String) {
        scope.launch {
            val messageBytes = message.toByteArray(Charsets.UTF_8)
            peripheral.write(
                messageCharacteristic,
                messageBytes,
//                    writeType = WriteType.WithoutResponse
            )
//                _messages.value = _messages.value + message
        }.join()
    }


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

    suspend fun listWiFi(): List<WiFi> {
//        return wifiCommander.listWifi()

        val char = catalog.ListWiFi;
        val value = try {
            peripheral.read(char)
        } catch (e: Exception) {
//            throw e
            throw RuntimeException("Unable to read", e)
        }

        Timber.d("Got response ${value.toList()}")

        val string = value.decodeToString()
        Timber.d("Got response $string")

        val response = try {
            Json.decodeFromString<List<WiFi>>(string)
        } catch (e: Exception) {
            throw RuntimeException("Unable to decode string: \"$string\"", e)
        }
        return response
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
            peripheral: AndroidPeripheral,
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


            Timber.i("Setting the new MTU to 512")
            val newMtu = peripheral.requestMtu(512)
            Timber.i("MTU set to $newMtu")

            val link = Link(context, peripheral, scope)

            link.sendMessage("Hello")

            return link
        }
    }
}