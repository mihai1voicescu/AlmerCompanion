package io.almer.companionshared.server

import android.bluetooth.BluetoothDevice

sealed class DeviceConnectionState {
    class Connected(val device: BluetoothDevice) : DeviceConnectionState()
    object Disconnected : DeviceConnectionState()
}