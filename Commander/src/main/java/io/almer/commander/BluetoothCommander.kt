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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onCompletion
import java.util.concurrent.atomic.AtomicReference
import android.util.Log

import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile.ServiceListener
import io.almer.companionshared.model.oneshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.buffer


typealias BluetoothDeviceModel = io.almer.companionshared.model.BluetoothDevice


private fun BluetoothDevice.toBluetoothDeviceModel(isPaired: Boolean = true) = BluetoothDeviceModel(
    name = name,
    isPaired = isPaired,
    uuid = null
)

const val TAG = "BluetoothCommander"

class BluetoothCommander(
    val context: Context
) {

    val bluetoothAdapter =
        BluetoothAdapter.getDefaultAdapter() ?: error("Device does not support Bluetooth")

    val isBluetoothOn = bluetoothAdapter.isEnabled


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


    suspend fun connectedHeadset(): BluetoothDevice {
        val headset: BluetoothHeadset = oneshot {
            val mProfileListener = object : ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    if (profile == BluetoothProfile.HEADSET) {
                        val mBluetoothHeadset = proxy as BluetoothHeadset
                        send(mBluetoothHeadset)
                    }
                }

                override fun onServiceDisconnected(profile: Int) {
                    if (profile == BluetoothProfile.HEADSET) {
                        Log.d(TAG, "[Bluetooth] Headset disconnected")
                    }
                }
            }

            bluetoothAdapter.getProfileProxy(context, mProfileListener, BluetoothProfile.HEADSET)
        }

        return headset.connectedDevices.first()
    }

}