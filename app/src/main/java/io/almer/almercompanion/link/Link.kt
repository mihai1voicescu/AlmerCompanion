package io.almer.almercompanion.link

import android.content.Context
import android.net.wifi.WifiInfo
import android.os.Build
import com.juul.kable.AndroidPeripheral
import com.juul.kable.Peripheral
import com.juul.kable.WriteType
import com.juul.kable.characteristicOf
import io.almer.companionshared.model.BluetoothDevice
import io.almer.companionshared.model.WiFi
import io.almer.companionshared.model.WifiConnectionInfo
import io.almer.companionshared.server.commands.ClientCommandCatalog
import io.almer.companionshared.server.MESSAGE_UUID
import io.almer.companionshared.server.SERVICE_UUID
import io.almer.companionshared.server.commands.ReadUUID
import io.almer.companionshared.server.commands.command.Commands
import io.almer.companionshared.server.commands.command.Listen
import io.almer.companionshared.server.commands.command.Write
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import timber.log.Timber
import kotlin.RuntimeException

val UNKNOWN_WIFI = WiFi(name = "UNKNOWN", ssid = "UNKNOWN", 0, null)
val UNKNOWN_BLUETOOTH = BluetoothDevice(name = "UNKNOWN", isPaired = true, "")

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

    fun scanBluetooth(): Flow<BluetoothDevice> {
        return peripheral.observe(catalog.ScanBluetooth)
            .map { Listen.ScanBluetooth.deserializeResponse(it) }
    }

    suspend fun selectBluetooth(name: String) {
        peripheral.write(catalog.SelectBluetooth, Write.SelectBluetooth.serializeRequest(name))
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

            link.listen()

            return link
        }
    }
}