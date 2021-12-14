package io.almer.commander

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicReference
import android.util.Log

import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile.ServiceListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import java.lang.reflect.Method


typealias BluetoothDeviceModel = io.almer.companionshared.model.BluetoothDevice


private fun BluetoothDevice.toBluetoothDeviceModel(isPaired: Boolean = true) = BluetoothDeviceModel(
    name = name,
    isPaired = isPaired,
    uuid = null
)

const val TAG = "BluetoothCommander"

class BluetoothCommander(
    val context: Context
) : AutoCloseable {

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
                Log.d(TAG, "[Bluetooth] Headset disconnected")
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
        context.registerReceiver(mReceiver,
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

    suspend fun getBondedDevices(): Collection<BluetoothDeviceModel> {
        return bluetoothAdapter.bondedDevices.map(BluetoothDevice::toBluetoothDeviceModel)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun scanDevices(): Flow<BluetoothDeviceModel> {

        val toRemove: AtomicReference<BroadcastReceiver?> = AtomicReference()

        return callbackFlow {
            val receiver = object : BroadcastReceiver() {

                override fun onReceive(context: Context, intent: Intent) {
                    val action: String? = intent.action
                    when (action) {
                        BluetoothDevice.ACTION_FOUND -> {
                            // Discovery has found a device. Get the BluetoothDevice
                            // object and its info from the Intent.
                            val device: BluetoothDevice =
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)!!

                            this@callbackFlow.trySendBlocking(device.toBluetoothDeviceModel())
                        }
                    }
                }
            }

            toRemove.set(receiver)

            context.registerReceiver(
                receiver,
                IntentFilter(BluetoothDevice.ACTION_FOUND)
            )

            delay(12_000)
//            delay(1_000)
            close()
            awaitClose {
                context.unregisterReceiver(toRemove.get())
            }
        }.buffer(10)
    }


    fun selectDevice(name: String): Boolean {
        /*
         * Unfortunately this requires access to non-public API
         *
         * Method is here https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/bluetooth/BluetoothHeadset.java;l=480?q=BluetoothHeadse&ss=android%2Fplatform%2Fsuperproject
         */
        val headset = headset.value ?: return false
        val method: Method = headset.javaClass.getMethod("connect", BluetoothDevice::class.java)

        val device = bluetoothAdapter.bondedDevices.first {
            it.name == name
        } ?: return false

        method.invoke(headset, device)

        return true
    }

    override fun close() {
        context.unregisterReceiver(mReceiver)
    }
}