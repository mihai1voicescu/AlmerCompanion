package io.almer.commander

import WiFiCommander
import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import io.almer.companionshared.model.BluetoothDevice
import io.almer.companionshared.model.WifiConnectionInfo
import io.almer.companionshared.model.toBluetoothDeviceModel
import io.almer.companionshared.model.toWiFI
import io.almer.companionshared.server.*
import io.almer.companionshared.server.commands.*
import io.almer.companionshared.server.commands.command.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.ExperimentalSerializationApi
import org.lighthousegames.logging.logging


@OptIn(ExperimentalSerializationApi::class)
inline fun toGattSuccess(byteArray: ByteArray) =
    Pair(BluetoothGatt.GATT_SUCCESS, byteArray)

private fun toGattSuccess() = Pair(BluetoothGatt.GATT_SUCCESS, null)

private data class NotificationEnableResponse(
    val status: Int = BluetoothGatt.GATT_SUCCESS,
    val shouldNotify: Boolean = false,
    val notificationValue: ByteArray? = null
)

class CommanderServer(
    val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : AutoCloseable {

    val appSettings by lazy {
        AppSettings(
            context.getSharedPreferences(
                "app",
                Context.MODE_PRIVATE
            )
        )
    }

    companion object {
        val Log = logging()
    }

    private val wifiCommander = WiFiCommander(context)
    private val bluetoothCommander = BluetoothCommander(context)
    private val bluetoothManager: BluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    private inner class CompanionHandler : CompanionCatalog, Handler(scope, context) {
        override val listWifi = read(ListWifi()) {
            wifiCommander.listWifi()
        }
        override val listenWifi = listen(ListenWifi()) {
            wifiCommander.wifi.map { it?.toWiFI() }
        }
        override val selectWifi = write(SelectWifi()) {
            wifiCommander.setWifi(it)
        }
        override val forgetWiFi = write(ForgetWiFi()) {
            wifiCommander.forgetWifi(it)
        }
        override val connectToWifi = write(ConnectToWifi()) {
            val networkId =
                wifiCommander.learnWifiWPA(it.wifi.ssid, it.password)

            wifiCommander.setWifi(networkId)
        }
        override val pairedDevices = read(PairedDevices()) {
            bluetoothCommander.getBondedDevices()
        }
        override val listenBluetooth = listen(ListenBluetooth()) {
            bluetoothCommander.headset.map {
                it?.connectedDevices?.firstOrNull()?.toBluetoothDeviceModel(true)
            }
        }
        override val scanBluetooth = listen(ScanBluetooth()) {
            bluetoothCommander.scanDevices()
        }
        override val selectBluetooth = write(SelectBluetooth()) {
            bluetoothCommander.selectDevice(it)
        }
        override val forgetBluetooth = write(ForgetBluetooth()) {
            bluetoothCommander.forgetDevice(it)
        }
        override val callLink = read(CallLink()) {
            TODO()
        }
    }

    private val handler = CompanionHandler()

    override fun close() {
        handler.close()
    }
}