package io.almer.almercompanion.link

import android.bluetooth.BluetoothGattDescriptor
import android.content.Context
import com.juul.kable.*
import io.almer.companionshared.model.BluetoothDevice
import io.almer.companionshared.model.WiFi
import io.almer.companionshared.model.WifiConnectionInfo
import io.almer.companionshared.server.CCCD
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.serialization.ExperimentalSerializationApi
import org.lighthousegames.logging.logging
import kotlin.RuntimeException

val UNKNOWN_WIFI = WiFi(name = "UNKNOWN", ssid = "UNKNOWN", 0, null)
val UNKNOWN_BLUETOOTH = BluetoothDevice(name = "UNKNOWN", isPaired = true, "")

fun DiscoveredCharacteristic.cccd() = descriptors.firstOrNull { it.descriptorUuid == CCCD }

suspend fun Peripheral.disableListen(characteristic: DiscoveredCharacteristic) {
    characteristic.cccd()?.let {
        write(it, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)
    }
}


interface Link {
    val wifi: StateFlow<WiFi?>
    val bluetooth: StateFlow<BluetoothDevice?>
    suspend fun listWiFi(): List<WiFi>

    suspend fun selectWiFi(networkId: Int)
    suspend fun forgetWiFi(networkId: Int)
    suspend fun connectToWifi(connectionInfo: WifiConnectionInfo): String?
    suspend fun pairedDevices(): List<BluetoothDevice>
    suspend fun callLink(): String?
    fun scanBluetooth(): Flow<BluetoothDevice>
    suspend fun selectBluetooth(name: String)
    suspend fun forgetBluetooth(name: String)

    companion object {
        val Log = logging()

        suspend operator fun invoke(
            context: Context,
            peripheral: AndroidPeripheral,
            scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
        ): Link {
            return LinkImpl(context, peripheral, scope)
        }
    }

    val state: StateFlow<State>
}

class LinkImpl private constructor(
    private val context: Context,
    private val peripheral: Peripheral,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
) : Link {
    private val _wifi = MutableStateFlow<WiFi?>(UNKNOWN_WIFI)
    override val wifi = _wifi.asStateFlow()

    private val _bluetooth = MutableStateFlow<BluetoothDevice?>(UNKNOWN_BLUETOOTH)
    override val bluetooth: StateFlow<BluetoothDevice?> = _bluetooth

    override val state: StateFlow<State> =
        peripheral.state.stateIn(scope, SharingStarted.Eagerly, State.Connecting.Bluetooth)

    private val companionRequester = CompanionRequester(peripheral)

    override suspend fun listWiFi(): List<WiFi> {
        return try {
            companionRequester.listWifi()
        } catch (e: Exception) {
            Log.e(e) { "Unable to ListWifi" }
            emptyList()
        }
    }


    private fun listen() {
        companionRequester.listenWifi().onEach {
            _wifi.value = it
        }.launchIn(scope)

        companionRequester.listenBluetooth().onEach {
            _bluetooth.value = it
        }.launchIn(scope)
    }

    override suspend fun selectWiFi(networkId: Int) {
        return companionRequester.selectWifi(networkId)
    }

    override suspend fun forgetWiFi(networkId: Int) {
        return companionRequester.forgetWiFi(networkId)
    }

    override suspend fun connectToWifi(connectionInfo: WifiConnectionInfo): String? {
        companionRequester.connectToWifi(connectionInfo)
        return null
    }

    override suspend fun pairedDevices(): List<BluetoothDevice> {
        return try {
            companionRequester.pairedDevices()
        } catch (e: Exception) {
            throw RuntimeException("Unable to ListWifi", e)
        }
    }


    override suspend fun callLink(): String? {
        return try {
            companionRequester.callLink()
        } catch (e: Exception) {
            throw RuntimeException("Unable to CallLink", e)
        }
    }

    override fun scanBluetooth(): Flow<BluetoothDevice> {
        return companionRequester.scanBluetooth()
    }

    override suspend fun selectBluetooth(name: String) {
        companionRequester.selectBluetooth(name)
    }

    override suspend fun forgetBluetooth(name: String) {
        companionRequester.forgetBluetooth(name)
    }

    companion object {
        val Log = logging()

        suspend operator fun invoke(
            context: Context,
            peripheral: AndroidPeripheral,
            scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
        ): Link {
            Log.i { "Peripheral: creating" }

            peripheral.state.onEach {
                Log.i { "Peripheral: state: $it" }
            }.launchIn(scope)

            Log.i { "Peripheral: connecting" }
            peripheral.connect()
            Log.i { "Peripheral: connected" }

            Log.i { "Successfully selected current peripheral: $peripheral" }


            Log.i { "Setting the new MTU to 512" }
            val newMtu = peripheral.requestMtu(512)
            Log.i { "MTU set to $newMtu" }

            val link = LinkImpl(context, peripheral, scope)

            link.listen()

            return link
        }
    }
}