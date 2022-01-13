package io.almer.companionshared.server.commands.command

import io.almer.companionshared.model.WifiConnectionInfo
import io.almer.companionshared.server.commands.WriteUUID

sealed class Write<Request, Response>(
) : Command {

    object SelectWiFi : Write<Int, Unit>() {
        override fun serializeRequest(request: Int): ByteArray = serialize(request)
        override fun deserializeRequest(byteArray: ByteArray): Int = deserialize(byteArray)

//        override fun serializeResponse(response: Unit): ByteArray = empty
//        override fun deserializeResponse(byteArray: ByteArray) {}
        override val uuid = WriteUUID.SelectWiFi.uuid
    }

    object ForgetWiFi : Write<Int, Unit>() {
        override fun serializeRequest(request: Int): ByteArray = serialize(request)
        override fun deserializeRequest(byteArray: ByteArray): Int = deserialize(byteArray)

//        override fun serializeResponse(response: Unit): ByteArray = empty
//        override fun deserializeResponse(byteArray: ByteArray) {}
        override val uuid = WriteUUID.ForgetWiFi.uuid
    }

    object ConnectToWifi : Write<WifiConnectionInfo, String?>() {
        override fun serializeRequest(request: WifiConnectionInfo): ByteArray = serialize(request)
        override fun deserializeRequest(byteArray: ByteArray): WifiConnectionInfo =
            deserialize(byteArray)

//        override fun serializeResponse(response: String?): ByteArray = serialize(response)
//        override fun deserializeResponse(byteArray: ByteArray): String? = deserialize(byteArray)
        override val uuid = WriteUUID.ConnectToWifi.uuid
    }

    object SelectBluetooth : Write<String, Boolean>() {
        override fun serializeRequest(request: String): ByteArray = serialize(request)
        override fun deserializeRequest(byteArray: ByteArray): String = deserialize(byteArray)

//        override fun serializeResponse(response: Boolean): ByteArray = serialize(response)
//        override fun deserializeResponse(byteArray: ByteArray): Boolean = deserialize(byteArray)
        override val uuid = WriteUUID.SelectBluetooth.uuid
    }

//
//    object StartBluetoothScan : Write<Unit, Unit>() {
//        override fun serializeRequest(request: Unit): ByteArray = serialize(request)
//        override fun deserializeRequest(byteArray: ByteArray): Unit = deserialize(byteArray)
//
////        override fun serializeResponse(response: Unit): ByteArray = serialize(response)
////        override fun deserializeResponse(byteArray: ByteArray): Unit = deserialize(byteArray)
//        override val uuid = WriteUUID.StartBluetoothScan.uuid
//    }
//
//
//    object StopBluetoothScan : Write<Unit, Unit>() {
//        override fun serializeRequest(request: Unit): ByteArray = serialize(request)
//        override fun deserializeRequest(byteArray: ByteArray): Unit = deserialize(byteArray)
//
//        //        override fun serializeResponse(response: Unit): ByteArray = serialize(response)
////        override fun deserializeResponse(byteArray: ByteArray): Unit = deserialize(byteArray)
//        override val uuid = WriteUUID.StopBluetoothScan.uuid
//    }

//    abstract fun serializeResponse(response: Response): ByteArray
//    abstract fun deserializeResponse(byteArray: ByteArray): Response

    abstract fun serializeRequest(request: Request): ByteArray
    abstract fun deserializeRequest(byteArray: ByteArray): Request
}