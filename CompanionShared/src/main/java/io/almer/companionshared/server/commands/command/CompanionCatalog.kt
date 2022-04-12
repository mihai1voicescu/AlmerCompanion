package io.almer.companionshared.server.commands.command

import io.almer.companionshared.model.BluetoothDevice
import io.almer.companionshared.model.WiFi
import io.almer.companionshared.model.WifiConnectionInfo
import kotlinx.serialization.serializer
import java.util.*

/*
5f961fef-8385-46cc-9e9f-653ef5d2ac4b
b0ea0c81-cf09-4b9e-bed8-69b5287d6180
817a799a-561c-4452-b077-99e8ea84a128
f5980ca5-8fac-46d1-8483-581dbd2bd7dd
f2cd7407-d430-4b95-a525-e4c08a6a94e1
 */


class ListWifi : ReadCommand<List<WiFi>>() {
    override val uuid: UUID = UUID.fromString("7fa37eab-d654-4b1f-b271-9532027cf660")
    override val kSerializer = serializer<List<WiFi>>()
}

class ListenWifi : ListenCommand<WiFi?>() {
    override val uuid: UUID = UUID.fromString("6b46363d-4498-4b13-8767-3fa6ac766001")
    override val kSerializer = serializer<WiFi?>()

}


class SelectWifi : WriteCommand<Int>() {
    override val uuid: UUID = UUID.fromString("17759776-a670-4cf0-9e74-770bf89852b2")
    override val kSerializer = serializer<Int>()

}

class ForgetWiFi : WriteCommand<Int>() {
    override val uuid: UUID = UUID.fromString("0cb77972-89cb-4ba7-9a2e-a0438eafdee3")
    override val kSerializer = serializer<Int>()

}

class ConnectToWifi : WriteCommand<WifiConnectionInfo>() {
    override val uuid: UUID = UUID.fromString("228d48b8-6fc1-4a1e-8ba8-f456cc6f1535")
    override val kSerializer = serializer<WifiConnectionInfo>()

}

class PairedDevices : ReadCommand<List<BluetoothDevice>>() {
    override val uuid: UUID = UUID.fromString("737b58f2-8e11-43da-bbc2-65c9ce0d07aa")
    override val kSerializer = serializer<List<BluetoothDevice>>()

}


class ListenBluetooth : ListenCommand<BluetoothDevice?>() {
    override val uuid: UUID = UUID.fromString("90a2ece1-2e7e-46ab-956c-7d069a0ed236")
    override val kSerializer = serializer<BluetoothDevice?>()

}


class ScanBluetooth : ListenCommand<BluetoothDevice>() {
    override val uuid: UUID = UUID.fromString("5d8d8cee-a7a2-4fbb-b9d9-25d59f00413d")
    override val kSerializer = serializer<BluetoothDevice>()

}


class SelectBluetooth : WriteCommand<String>() {
    override val uuid: UUID = UUID.fromString("3d044f39-ac57-4c6e-808a-0c845a2b0376")
    override val kSerializer = serializer<String>()

}

class ForgetBluetooth : WriteCommand<String>() {
    override val uuid: UUID = UUID.fromString("99c8f16c-f4b5-4d35-a29a-66bdde2e2ac4")
    override val kSerializer = serializer<String>()

}

//class StartBluetoothScan : WriteCommand<WiFi>() {
//    override val uuid: UUID = UUID.fromString("765a7fab-8b60-4684-8593-9f44cc058f6a")
//}
//
//class StopBluetoothScan : WriteCommand<WiFi>() {
//    override val uuid: UUID = UUID.fromString("3d74b4a5-7673-4d9c-a158-cd3b0b330c6a")
//}


class CallLink : ReadCommand<String?>() {
    override val uuid: UUID = UUID.fromString("9d73ed7a-c884-4082-aa92-a292f061d6d0")
    override val kSerializer = serializer<String?>()

}


interface CompanionCatalog : CommandCatalog {
    val listWifi: Action<ListWifi>
    val listenWifi: Action<ListenWifi>
    val selectWifi: Action<SelectWifi>
    val forgetWiFi: Action<ForgetWiFi>
    val connectToWifi: Action<ConnectToWifi>
    val pairedDevices: Action<PairedDevices>
    val listenBluetooth: Action<ListenBluetooth>
    val scanBluetooth: Action<ScanBluetooth>
    val selectBluetooth: Action<SelectBluetooth>
    val forgetBluetooth: Action<ForgetBluetooth>
    val callLink: Action<CallLink>
}