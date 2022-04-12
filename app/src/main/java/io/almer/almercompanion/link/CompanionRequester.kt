package io.almer.almercompanion.link

import com.juul.kable.Peripheral
import io.almer.companionshared.server.commands.command.*

class CompanionRequester(peripheral: Peripheral) : CompanionCatalog, Requester(peripheral) {
    override val listWifi = read(ListWifi())
    override val listenWifi = listen(ListenWifi())
    override val selectWifi = write(SelectWifi())
    override val forgetWiFi = write(ForgetWiFi())
    override val connectToWifi = write(ConnectToWifi())
    override val pairedDevices = read(PairedDevices())
    override val listenBluetooth = listen(ListenBluetooth())
    override val scanBluetooth = listen(ScanBluetooth())
    override val selectBluetooth = write(SelectBluetooth())
    override val forgetBluetooth = write(ForgetBluetooth())
    override val callLink = read(CallLink())
}

