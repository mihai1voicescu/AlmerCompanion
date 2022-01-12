package io.almer.companionshared.server

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import com.juul.kable.DiscoveredCharacteristic
import com.juul.kable.Peripheral
import io.almer.companionshared.model.WiFi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.*

interface CommandCatalog<Type> {
    val ListWiFi: Type
}

object CommandsUUID : CommandCatalog<UUID> {
    override val ListWiFi: UUID = UUID.fromString("7fa37eab-d654-4b1f-b271-9532027cf660")
}

object Commands :CommandCatalog<Command<*>> {
    override val ListWiFi = Command.ListWiFi
}

abstract class Command<Response>(
) {
    object ListWiFi : Command<List<WiFi>>() {
        override fun deserialize(byteArray: ByteArray): List<WiFi> {
            val wifis = Json.decodeFromString<List<WiFi>>(byteArray.decodeToString())

            return wifis
        }
    }

    abstract fun deserialize(byteArray: ByteArray): Response
    open fun serialize(): ByteArray = ByteArray(0)
}

class ClientCommandCatalog(peripheral: Peripheral) : CommandCatalog<DiscoveredCharacteristic> {
    val service = peripheral.services!!.first {
        it.serviceUuid == SERVICE_UUID
    }

    private val charMap = service.characteristics.associateBy { it.characteristicUuid }

    override val ListWiFi = charMap[CommandsUUID.ListWiFi]!!
}

class CharacteristicCommandCatalog(val service: BluetoothGattService) :
    CommandCatalog<BluetoothGattCharacteristic> {

    private fun writeChar(uuid: UUID): BluetoothGattCharacteristic {
        val char = BluetoothGattCharacteristic(
            uuid,
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )

        service.addCharacteristic(char)

        return char
    }

    private fun readChar(uuid: UUID): BluetoothGattCharacteristic {
        val char = BluetoothGattCharacteristic(
            uuid,
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
        )

        service.addCharacteristic(char)

        return char
    }

    override val ListWiFi: BluetoothGattCharacteristic = readChar(CommandsUUID.ListWiFi)
}