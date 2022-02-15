package io.almer.companionshared.server.commands.command

import io.almer.companionshared.server.commands.CommandCatalog
import io.almer.companionshared.server.commands.WriteUUID

object Commands : CommandCatalog<Command> {
    override val ListWiFi = Read.ListWiFi
    override val PairedDevices = Read.PairedDevices
    override val CallLink = Read.CallLink

    override val SelectWiFi = Write.SelectWiFi
    override val ForgetWiFi = Write.ForgetWiFi
    override val ConnectToWifi = Write.ConnectToWifi
    override val SelectBluetooth = Write.SelectBluetooth
    override val ForgetBluetooth = Write.ForgetBluetooth
//    override val StartBluetoothScan = Write.StartBluetoothScan
//    override val StopBluetoothScan = Write.StopBluetoothScan

    override val WiFi = Listen.WiFi
    override val Bluetooth = Listen.Bluetooth
    override val ScanBluetooth = Listen.ScanBluetooth
}


