package io.almer.companionshared.server.commands

interface ReadCatalog<Type> {
    val ListWiFi: Type
    val PairedDevices: Type
}

interface WriteCatalog<Type> {
    val SelectWiFi: Type
    val ForgetWiFi: Type
    val ConnectToWifi: Type
    val SelectBluetooth: Type
//    val StartBluetoothScan: Type
//    val StopBluetoothScan: Type
}

interface ListenCatalog<Type> {
    val WiFi: Type
    val Bluetooth: Type
    val ScanBluetooth: Type
}

interface CommandCatalog<Type> : ReadCatalog<Type>, WriteCatalog<Type>, ListenCatalog<Type>


