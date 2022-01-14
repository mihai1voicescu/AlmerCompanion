package io.almer.commander

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset

fun BluetoothDevice.removeBond() {
    javaClass.getMethod("removeBond").invoke(this)
}

fun BluetoothHeadset.connect(device: BluetoothDevice) {
    /*
     * Unfortunately this requires access to non-public API
     *
     * Method is here https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/bluetooth/BluetoothHeadset.java;l=480?q=BluetoothHeadse&ss=android%2Fplatform%2Fsuperproject
     */
    val method = javaClass.getMethod("connect", BluetoothDevice::class.java)

    method.invoke(this, device)
}