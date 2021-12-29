package io.almer.companionshared.server

import io.almer.companionshared.model.WiFi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import java.util.*

sealed class Command<Response>(
    val uuid: UUID
) {
    object ListWiFi : Command<List<WiFi>>(UUID.fromString("7fa37eab-d654-4b1f-b271-9532027cf660")) {
        override fun deserialize(byteArray: ByteArray): List<WiFi> {
            val wifis = ProtoBuf.decodeFromByteArray<List<WiFi>>(byteArray)

            return wifis
        }
    }

    abstract fun deserialize(byteArray: ByteArray): Response
    open fun serialize(): ByteArray = ByteArray(0)
}