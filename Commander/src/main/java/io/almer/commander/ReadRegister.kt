package io.almer.commander

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import java.util.*
import java.util.concurrent.TimeUnit

// todo see if we can add requestId
data class ResponseKey (val device: BluetoothDevice, val characteristicUUID: UUID)
data class ResponseValue (val value: ByteArray, val result: Int)

class ReadRegister {
    private val cache = CacheBuilder
        .newBuilder()
        .expireAfterWrite(3, TimeUnit.MINUTES)
        .concurrencyLevel(1)
        .maximumSize(20)
        .build<ResponseKey, ResponseValue>()

    fun add(device: BluetoothDevice, characteristic: BluetoothGattCharacteristic, result: Int, value: ByteArray) {
        cache.put(ResponseKey(device, characteristic.uuid), ResponseValue(value, result))
    }

    fun get(device: BluetoothDevice, characteristic: BluetoothGattCharacteristic): ResponseValue? {
        return cache.getIfPresent(ResponseKey(device, characteristic.uuid))
    }
}