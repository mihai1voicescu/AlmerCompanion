package io.almer.companionshared.model

data class BluetoothDevice(
    val name: String,
    val isPaired: Boolean,
    val uuid: String?,
)