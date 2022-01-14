package io.almer.companionshared.model

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import kotlinx.serialization.Serializable

@Serializable
data class BluetoothDevice(
    val name: String,
    val isPaired: Boolean,
    val uuid: String?,
)

@SuppressLint("MissingPermission")
fun BluetoothDevice.toBluetoothDeviceModel(isPaired: Boolean = true) = BluetoothDevice(
    name = name ?: address ?: "Unknown",
    isPaired = isPaired,
    uuid = null
)