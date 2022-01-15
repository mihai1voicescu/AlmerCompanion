package io.almer.almercompanion.link

import android.bluetooth.*
import android.content.Context
import com.juul.kable.*
import com.juul.kable.logs.Logging
import io.almer.companionshared.server.MESSAGE_UUID
import io.almer.companionshared.server.SERVICE_UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.*
import org.lighthousegames.logging.logging

class CommanderConnector(
    val context: Context,
    val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {

    companion object {
        val Log = logging()
    }

    // BluetoothAdapter should never be null if the app is installed from the Play store
    // since BLE is required per the <uses-feature> tag in the AndroidManifest.xml.
    // If the app is installed on an emulator without bluetooth then the app will crash
    // on launch since installing via Android Studio bypasses the <uses-feature> flags
    private val adapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()


    init {
        if (!adapter.isEnabled) {
            error("Bluetooth adapter is not enabled")
        }
    }

    // Properties for current chat device connection

    private var _currentPeripheral = MutableStateFlow<Peripheral?>(null)
    val currentPeripheral = _currentPeripheral.asStateFlow()

    private var messageCharacteristic: Characteristic? = null


//    /**
//     * The questions of how to obtain a device's own MAC address comes up a lot. The answer is
//     * you cannot; it would be a security breach. Only system apps can get that permission.
//     * Otherwise apps might use that address to fingerprint a device (e.g. for advertising, etc.)
//     * A user can find their own MAC address through Settings, but apps cannot find it.
//     * This method, which some might be tempted to use, returns a default value,
//     * usually 02:00:00:00:00:00
//     */
////    fun getYourDeviceAddress(): String = bluetoothManager.adapter.address

    suspend fun setCurrentChatConnection(device: BluetoothDevice) {
        Log.i { "Peripheral: creating" }
        val peripheral = scope.peripheral(device) {
            this.logging {
                level = Logging.Level.Events
            }
            this.onServicesDiscovered {
                Log.i { "Successfully discovered services" }
            }
        }

        peripheral.state.onEach {
            Log.i { "Peripheral: state: $it" }
        }.launchIn(scope)

        Log.i { "Peripheral: connecting" }
        peripheral.connect()
        Log.i { "Peripheral: connected" }

        Log.i { "Peripheral: discover services" }
        val service = peripheral.services!!.first {
            it.serviceUuid == SERVICE_UUID
        }

        Log.i { "Peripheral: discover char" }
        val char = service.characteristics.first {
            it.characteristicUuid == MESSAGE_UUID
        }

        Log.i { "Peripheral: setting vars" }
        messageCharacteristic = char

        _currentPeripheral.value = peripheral

        Log.i { "Successfully selected current peripheral: ${_currentPeripheral.value}" }
    }

    suspend fun sendMessage(message: String): Boolean {
        Log.d { "Send a message" }

        currentPeripheral.value?.let { periferal ->
            scope.launch {
                val messageBytes = message.toByteArray(Charsets.UTF_8)
                periferal.write(
                    messageCharacteristic!!,
                    messageBytes,
//                    writeType = WriteType.WithoutResponse
                )
//                _messages.value = _messages.value + message
            }.join()

            return true
        }

        return false
    }
}