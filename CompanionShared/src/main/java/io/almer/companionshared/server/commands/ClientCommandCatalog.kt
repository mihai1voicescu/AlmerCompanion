package io.almer.companionshared.server.commands

import com.juul.kable.DiscoveredCharacteristic
import com.juul.kable.Peripheral
import io.almer.companionshared.server.SERVICE_UUID

class ClientCommandCatalog(peripheral: Peripheral) : CommandCatalog<DiscoveredCharacteristic> {
    val service = peripheral.services!!.first {
        it.serviceUuid == SERVICE_UUID
    }

    private val charMap = service.characteristics.associateBy { it.characteristicUuid }

    override val ListWiFi = charMap[CommandsUUID.ListWiFi]!!
    override val PairedDevices = charMap[CommandsUUID.PairedDevices]!!

    override val SelectWiFi = charMap[CommandsUUID.SelectWiFi]!!
    override val ForgetWiFi = charMap[CommandsUUID.ForgetWiFi]!!
    override val ConnectToWifi = charMap[CommandsUUID.ConnectToWifi]!!
    override val SelectBluetooth = charMap[CommandsUUID.SelectBluetooth]!!
    override val ForgetBluetooth = charMap[CommandsUUID.ForgetBluetooth]!!
//    override val StartBluetoothScan = charMap[CommandsUUID.StartBluetoothScan]!!
//    override val StopBluetoothScan = charMap[CommandsUUID.StopBluetoothScan]!!

    override val WiFi = charMap[CommandsUUID.WiFi]!!
    override val ScanBluetooth = charMap[CommandsUUID.ScanBluetooth]!!
    override val Bluetooth = charMap[CommandsUUID.Bluetooth]!!
}