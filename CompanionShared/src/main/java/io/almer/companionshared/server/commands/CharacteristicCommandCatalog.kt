package io.almer.companionshared.server.commands

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import java.util.*


private class CharacteristicReadCatalog(val service: BluetoothGattService) :
    ReadCatalog<BluetoothGattCharacteristic> {
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

    override val PairedDevices: BluetoothGattCharacteristic = readChar(CommandsUUID.PairedDevices)
}

private class CharacteristicWriteCatalog(val service: BluetoothGattService) :
    WriteCatalog<BluetoothGattCharacteristic> {

    private fun writeChar(uuid: UUID): BluetoothGattCharacteristic {
        val char = BluetoothGattCharacteristic(
            uuid,
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )

        service.addCharacteristic(char)

        return char
    }


    override val SelectWiFi: BluetoothGattCharacteristic = writeChar(CommandsUUID.SelectWiFi)

    override val ForgetWiFi: BluetoothGattCharacteristic = writeChar(CommandsUUID.ForgetWiFi)

    override val ConnectToWifi: BluetoothGattCharacteristic = writeChar(CommandsUUID.ConnectToWifi)

    override val SelectBluetooth: BluetoothGattCharacteristic =
        writeChar(CommandsUUID.SelectBluetooth)

//    override val StartBluetoothScan: BluetoothGattCharacteristic =
//        writeChar(CommandsUUID.StartBluetoothScan)
//
//    override val StopBluetoothScan: BluetoothGattCharacteristic =
//        writeChar(CommandsUUID.StopBluetoothScan)

}


private class CharacteristicListenCatalog(val service: BluetoothGattService) :
    ListenCatalog<BluetoothGattCharacteristic> {

    private fun notificationChar(uuid: UUID): BluetoothGattCharacteristic {
        val char = BluetoothGattCharacteristic(
            uuid,
            BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ
        )

        char.addDescriptor(
            BluetoothGattDescriptor(
                UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"),
                BluetoothGattDescriptor.PERMISSION_WRITE
            )
        )

        service.addCharacteristic(char)

        return char
    }


    override val WiFi: BluetoothGattCharacteristic = notificationChar(CommandsUUID.WiFi)
    override val Bluetooth: BluetoothGattCharacteristic = notificationChar(CommandsUUID.Bluetooth)
    override val ScanBluetooth: BluetoothGattCharacteristic =
        notificationChar(CommandsUUID.ScanBluetooth)
}


class CharacteristicCommandCatalog private constructor(
    val service: BluetoothGattService,
    writeCatalog: CharacteristicWriteCatalog,
    readCatalog: CharacteristicReadCatalog,
    listenCatalog: CharacteristicListenCatalog,
) :
    ReadCatalog<BluetoothGattCharacteristic> by readCatalog,
    WriteCatalog<BluetoothGattCharacteristic> by writeCatalog,
    ListenCatalog<BluetoothGattCharacteristic> by listenCatalog,
    CommandCatalog<BluetoothGattCharacteristic> {

    companion object {
        operator fun invoke(service: BluetoothGattService): CharacteristicCommandCatalog {
            val write = CharacteristicWriteCatalog(service)
            val read = CharacteristicReadCatalog(service)
            val listen = CharacteristicListenCatalog(service)

            return CharacteristicCommandCatalog(service, write, read, listen)
        }
    }
}