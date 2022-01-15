package io.almer.commander

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothProfile.ServiceListener
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import io.almer.companionshared.model.toBluetoothDeviceModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.*
import org.lighthousegames.logging.logging
import java.util.concurrent.atomic.AtomicReference


typealias BluetoothDeviceModel = io.almer.companionshared.model.BluetoothDevice

const val TAG = "BluetoothCommander"

class BluetoothCommander(
    val context: Context
) : AutoCloseable {

    companion object {
        val Log = logging()
    }

    val bluetoothAdapter =
        BluetoothAdapter.getDefaultAdapter() ?: error("Device does not support Bluetooth")

    val isBluetoothOn = bluetoothAdapter.isEnabled

    private val _headset = MutableStateFlow<BluetoothHeadset?>(null)
    val headset = _headset.asStateFlow()

    private val mProfileListener = object : ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile == BluetoothProfile.HEADSET) {
                val mBluetoothHeadset = proxy as BluetoothHeadset
                _headset.value = mBluetoothHeadset
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HEADSET) {
                Log.d { "Headset disconnected" }
            }
        }
    }

    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action: String = intent?.action ?: return

            if (BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED == action) {
                scanHeadset()
            }
        }
    }


    init {
        scanHeadset()
        context.registerReceiver(
            mReceiver,
            IntentFilter(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
//            Not needed
//                .apply {
//                    addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
//                }
        )
    }

    private fun scanHeadset() {
        bluetoothAdapter.getProfileProxy(context, mProfileListener, BluetoothProfile.HEADSET)
    }

    fun forgetDevice(name: String) {
        bluetoothAdapter.bondedDevices.firstOrNull { it.name == name }?.removeBond()
    }

    suspend fun getBondedDevices(): List<BluetoothDeviceModel> {
        return bluetoothAdapter.bondedDevices.map(BluetoothDevice::toBluetoothDeviceModel)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun scanDevices(): Flow<BluetoothDeviceModel> {

        val toRemove: AtomicReference<BroadcastReceiver?> = AtomicReference()

        return callbackFlow {
            val receiver = object : BroadcastReceiver() {

                var hasClosed = false
                override fun onReceive(context: Context, intent: Intent) {
                    if (hasClosed)
                        return
                    val action: String? = intent.action
                    when (action) {
                        BluetoothDevice.ACTION_FOUND -> {
                            // Discovery has found a device. Get the BluetoothDevice
                            // object and its info from the Intent.
                            val device: BluetoothDevice =
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)!!

                            Log.d { "Found new bluetooth device ${device.name}" }

                            this@callbackFlow.trySendBlocking(device.toBluetoothDeviceModel())
                        }
                        BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                            hasClosed = true
                            Log.d { "Discovery has stopped" }
                            this@callbackFlow.close()
                        }
                    }
                }
            }

            toRemove.set(receiver)

            context.registerReceiver(
                receiver,
                IntentFilter(BluetoothDevice.ACTION_FOUND).apply {
                    addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                }
            )

//            delay(12_000)
//            delay(1_000)
//            close()

            Log.d { "Start scanning for bluetooth devices" }
            bluetoothAdapter.startDiscovery()
            awaitClose {
                Log.d { "Scan for bluetooth devices finished" }
                bluetoothAdapter.cancelDiscovery()
                context.unregisterReceiver(toRemove.get())
                Log.d { "Scan for bluetooth devices cleaned" }
            }
        }.buffer(10)
    }


    fun selectDevice(name: String): Boolean {

        val headset = headset.value ?: return false

        val device = bluetoothAdapter.bondedDevices.first {
            it.name == name
        } ?: return false

        headset.connect(device)

        return true
    }

    override fun close() {
        context.unregisterReceiver(mReceiver)
    }
}