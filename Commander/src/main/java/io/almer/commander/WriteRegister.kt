package io.almer.commander

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import java.util.*
import java.util.concurrent.TimeUnit


class WriteRegister {
    private val cache = CacheBuilder
        .newBuilder()
        .expireAfterWrite(3, TimeUnit.MINUTES)
        .concurrencyLevel(1)
        .maximumSize(20)
        .build<ResponseKey, List<ByteArray>>()

    fun add(
        device: BluetoothDevice,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ) {
        cache.put(ResponseKey(device, characteristic.uuid), listOf(value))
    }

    fun get(
        device: BluetoothDevice,
        characteristic: BluetoothGattCharacteristic
    ): List<ByteArray>? {
        return cache.getIfPresent(ResponseKey(device, characteristic.uuid))
    }

    fun remove(
        device: BluetoothDevice,
        characteristic: BluetoothGattCharacteristic
    ): List<ByteArray>? {
        val key = ResponseKey(device, characteristic.uuid)
        return cache.getIfPresent(key)?.also {
            cache.invalidate(key)
        }
    }
}