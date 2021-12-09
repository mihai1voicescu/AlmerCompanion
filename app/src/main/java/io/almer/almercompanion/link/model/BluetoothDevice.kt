package io.almer.almercompanion.link.model

data class BluetoothDevice(
    val name: String,
    val isPaired: Boolean,
    val uuid: String,
)