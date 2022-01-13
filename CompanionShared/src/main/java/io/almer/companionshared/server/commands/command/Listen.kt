package io.almer.companionshared.server.commands.command

import io.almer.companionshared.model.BluetoothDevice
import io.almer.companionshared.server.commands.ListenUUID

sealed class Listen<Response>(
) : Command {

    // todo these should automatically handle the streams in a nice way
    //    a generic solution is possible which includes the BluetoothCharacteristic and a StateFlow
    //    to read from

    object WiFi : Listen<io.almer.companionshared.model.WiFi?>() {
        override fun deserializeResponse(byteArray: ByteArray): io.almer.companionshared.model.WiFi? =
            deserialize(byteArray)
        override fun serializeResponse(response: io.almer.companionshared.model.WiFi?): ByteArray =
            serialize(response)
        override val uuid = ListenUUID.WiFi.uuid
    }


    object Bluetooth : Listen<BluetoothDevice?>() {
        override fun deserializeResponse(byteArray: ByteArray): BluetoothDevice? =
            deserialize(byteArray)
        override fun serializeResponse(response: BluetoothDevice?): ByteArray =
            serialize(response)
        override val uuid = ListenUUID.Bluetooth.uuid
    }

    object ScanBluetooth : Listen<BluetoothDevice>() {
        override fun deserializeResponse(byteArray: ByteArray): BluetoothDevice =
            deserialize(byteArray)

        override fun serializeResponse(response: BluetoothDevice): ByteArray =
            serialize(response)

        override val uuid = ListenUUID.ScanBluetooth.uuid
    }

    abstract fun serializeResponse(response: Response): ByteArray
    abstract fun deserializeResponse(byteArray: ByteArray): Response
}