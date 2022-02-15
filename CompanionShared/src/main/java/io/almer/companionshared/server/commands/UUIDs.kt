package io.almer.companionshared.server.commands

import java.util.*

/*
5f961fef-8385-46cc-9e9f-653ef5d2ac4b
b0ea0c81-cf09-4b9e-bed8-69b5287d6180
817a799a-561c-4452-b077-99e8ea84a128
f5980ca5-8fac-46d1-8483-581dbd2bd7dd
f2cd7407-d430-4b95-a525-e4c08a6a94e1
 */


enum class ReadUUID(val uuid: UUID) {
    ListWiFi(UUID.fromString("7fa37eab-d654-4b1f-b271-9532027cf660")),
    PairedDevices(UUID.fromString("737b58f2-8e11-43da-bbc2-65c9ce0d07aa")),
    CallLink(UUID.fromString("9d73ed7a-c884-4082-aa92-a292f061d6d0"))
}

fun readUUID(uuid: UUID): ReadUUID? {
    return when (uuid) {
        ReadUUID.ListWiFi.uuid -> ReadUUID.ListWiFi
        ReadUUID.PairedDevices.uuid -> ReadUUID.PairedDevices
        ReadUUID.CallLink.uuid -> ReadUUID.CallLink
        else -> null
    }
}

enum class WriteUUID(val uuid: UUID) {
    SelectWiFi(UUID.fromString("17759776-a670-4cf0-9e74-770bf89852b2")),
    ForgetWiFi(UUID.fromString("0cb77972-89cb-4ba7-9a2e-a0438eafdee3")),
    ConnectToWifi(UUID.fromString("228d48b8-6fc1-4a1e-8ba8-f456cc6f1535")),
    SelectBluetooth(UUID.fromString("3d044f39-ac57-4c6e-808a-0c845a2b0376")),
    ForgetBluetooth(UUID.fromString("99c8f16c-f4b5-4d35-a29a-66bdde2e2ac4")),
//    StartBluetoothScan(UUID.fromString("765a7fab-8b60-4684-8593-9f44cc058f6a")),
//    StopBluetoothScan(UUID.fromString("3d74b4a5-7673-4d9c-a158-cd3b0b330c6a")),
}

fun writeUUID(uuid: UUID): WriteUUID? {
    return when (uuid) {
        WriteUUID.SelectWiFi.uuid -> WriteUUID.SelectWiFi
        WriteUUID.ForgetWiFi.uuid -> WriteUUID.ForgetWiFi
        WriteUUID.ConnectToWifi.uuid -> WriteUUID.ConnectToWifi
        WriteUUID.SelectBluetooth.uuid -> WriteUUID.SelectBluetooth
        WriteUUID.ForgetBluetooth.uuid -> WriteUUID.ForgetBluetooth
//        WriteUUID.StartBluetoothScan.uuid -> WriteUUID.StartBluetoothScan
//        WriteUUID.StopBluetoothScan.uuid -> WriteUUID.StopBluetoothScan
        else -> null
    }
}

enum class ListenUUID(val uuid: UUID) {
    WiFi(UUID.fromString("6b46363d-4498-4b13-8767-3fa6ac766001")),
    Bluetooth(UUID.fromString("90a2ece1-2e7e-46ab-956c-7d069a0ed236")),
    ScanBluetooth(UUID.fromString("5d8d8cee-a7a2-4fbb-b9d9-25d59f00413d")),
}

fun listenUUID(uuid: UUID): ListenUUID? {
    return when (uuid) {
        ListenUUID.WiFi.uuid -> ListenUUID.WiFi
        ListenUUID.Bluetooth.uuid -> ListenUUID.Bluetooth
        ListenUUID.ScanBluetooth.uuid -> ListenUUID.ScanBluetooth
        else -> null
    }
}

object CommandsUUID : CommandCatalog<UUID> {
    override val ListWiFi: UUID = ReadUUID.ListWiFi.uuid
    override val PairedDevices: UUID = ReadUUID.PairedDevices.uuid
    override val CallLink: UUID = ReadUUID.CallLink.uuid

    override val SelectWiFi: UUID = WriteUUID.SelectWiFi.uuid
    override val ForgetWiFi: UUID = WriteUUID.ForgetWiFi.uuid
    override val ConnectToWifi: UUID = WriteUUID.ConnectToWifi.uuid
    override val SelectBluetooth: UUID = WriteUUID.SelectBluetooth.uuid
    override val ForgetBluetooth: UUID = WriteUUID.ForgetBluetooth.uuid
//    override val StopBluetoothScan: UUID = WriteUUID.StopBluetoothScan.uuid    override val StartBluetoothScan: UUID = WriteUUID.StartBluetoothScan.uuid
//    override val StopBluetoothScan: UUID = WriteUUID.StopBluetoothScan.uuid

    override val WiFi: UUID = ListenUUID.WiFi.uuid
    override val Bluetooth: UUID = ListenUUID.Bluetooth.uuid
    override val ScanBluetooth: UUID = ListenUUID.ScanBluetooth.uuid
}

val CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")