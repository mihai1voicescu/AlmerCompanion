package io.almer.companionshared.server.commands.command

import io.almer.companionshared.model.BluetoothDevice
import io.almer.companionshared.model.WiFi
import io.almer.companionshared.server.commands.ReadUUID

sealed class Read<Response>(
) : Command {
    object ListWiFi : Read<List<WiFi>>() {
        override fun deserializeResponse(byteArray: ByteArray): List<WiFi> = deserialize(byteArray)
        override fun serializeResponse(response: List<WiFi>): ByteArray = serialize(response)
        override val uuid = ReadUUID.ListWiFi.uuid
    }

    object PairedDevices : Read<List<BluetoothDevice>>() {
        override fun deserializeResponse(byteArray: ByteArray): List<BluetoothDevice> =
            deserialize(byteArray)

        override fun serializeResponse(response: List<BluetoothDevice>): ByteArray =
            serialize(response)

        override val uuid = ReadUUID.PairedDevices.uuid
    }

    abstract fun serializeResponse(response: Response): ByteArray
    abstract fun deserializeResponse(byteArray: ByteArray): Response
}