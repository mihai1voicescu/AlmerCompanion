package io.almer.almercompanion.link

import android.bluetooth.BluetoothGattDescriptor
import android.content.Context
import com.juul.kable.*
import io.almer.companionshared.model.BluetoothDevice
import io.almer.companionshared.model.WiFi
import io.almer.companionshared.model.WifiConnectionInfo
import io.almer.companionshared.server.commands.ClientCommandCatalog
import io.almer.companionshared.server.MESSAGE_UUID
import io.almer.companionshared.server.SERVICE_UUID
import io.almer.companionshared.server.commands.CCCD
import io.almer.companionshared.server.commands.command.Commands
import io.almer.companionshared.server.commands.command.Listen
import io.almer.companionshared.server.commands.command.Write
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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

@OptIn(ExperimentalSerializationApi::class)
class Link private constructor(
    private val context: Context,
    private val peripheral: Peripheral,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
) {
    private val catalog = ClientCommandCatalog(peripheral)

    private val _wifi = MutableStateFlow<WiFi?>(UNKNOWN_WIFI)
    val wifi = _wifi.asStateFlow()

    private val _bluetooth = MutableStateFlow<BluetoothDevice?>(UNKNOWN_BLUETOOTH)
    val bluetooth: StateFlow<BluetoothDevice?> = _bluetooth
    val messageCharacteristic = characteristicOf(SERVICE_UUID.toString(), MESSAGE_UUID.toString())

    val state = peripheral.state.stateIn(scope, SharingStarted.Eagerly, State.Connecting.Bluetooth)

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
        //todo collect wifi
//        scope.launch {
//            scope.launch {
//                wifiCommander.wifi.collect {
//                    _wifi.emit(it?.toWiFI())
//                }
//            }
//            _wifi.emit(wifiCommander.wifi.value?.toWiFI())
//        }
    }

    suspend fun listWiFi(): List<WiFi> {
        val value = try {
            peripheral.read(catalog.ListWiFi)
        } catch (e: Exception) {
            throw RuntimeException("Unable to ListWifi", e)
        }

        val response = try {
            Commands.ListWiFi.deserializeResponse(value)
        } catch (e: Exception) {
            throw RuntimeException("Unable to decode string", e)
        }
        return response
    }


    private fun listen() {
        scope.launch {
            peripheral.observe(catalog.WiFi).map { Listen.WiFi.deserializeResponse(it) }.collect {
                _wifi.value = it
            }
        }

        scope.launch {
            peripheral.observe(catalog.Bluetooth).map { Listen.Bluetooth.deserializeResponse(it) }
                .collect {
                    _bluetooth.value = it
                }
        }
    }

    suspend fun selectWiFi(networkId: Int) {
        return peripheral.write(catalog.SelectWiFi, Write.SelectWiFi.serializeRequest(networkId))
    }

    suspend fun forgetWiFi(networkId: Int) {
        return peripheral.write(catalog.ForgetWiFi, Write.ForgetWiFi.serializeRequest(networkId))
    }

    suspend fun connectToWifi(connectionInfo: WifiConnectionInfo): String? {
        peripheral.write(
            catalog.ConnectToWifi,
            Write.ConnectToWifi.serializeRequest(connectionInfo)
        )
        return null
    }

    suspend fun pairedDevices(): List<BluetoothDevice> {
        val value = try {
            peripheral.read(catalog.PairedDevices)
        } catch (e: Exception) {
            throw RuntimeException("Unable to ListWifi", e)
        }

        val response = try {
            Commands.PairedDevices.deserializeResponse(value)
        } catch (e: Exception) {
            throw RuntimeException("Unable to decode string", e)
        }
        return response
    }


    suspend fun callLink(): String? {
        val value = try {
            peripheral.read(catalog.CallLink)
        } catch (e: Exception) {
            throw RuntimeException("Unable to CallLink", e)
        }

        val response = try {
            Commands.CallLink.deserializeResponse(value)
        } catch (e: Exception) {
            throw RuntimeException("Unable to decode string", e)
        }
        return response
    }

    fun scanBluetooth(): Flow<BluetoothDevice> {
        return peripheral.observe(catalog.ScanBluetooth)
            .map { Listen.ScanBluetooth.deserializeResponse(it) }
            .onCompletion {
                scope.launch {
                    peripheral.disableListen(catalog.ScanBluetooth)
                }
            }
    }

    suspend fun selectBluetooth(name: String) {
        peripheral.write(catalog.SelectBluetooth, Write.SelectBluetooth.serializeRequest(name))
    }

    suspend fun forgetBluetooth(name: String) {
        peripheral.write(catalog.ForgetBluetooth, Write.ForgetBluetooth.serializeRequest(name))
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

            val link = Link(context, peripheral, scope)

            link.sendMessage("Hello")

            link.listen()

            return link
        }
    }
}