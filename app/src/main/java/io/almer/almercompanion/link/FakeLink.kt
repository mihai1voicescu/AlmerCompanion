package io.almer.almercompanion.link

import com.juul.kable.State
import io.almer.companionshared.model.BluetoothDevice
import io.almer.companionshared.model.WiFi
import io.almer.companionshared.model.WifiConnectionInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlin.random.Random
import kotlin.random.nextInt

class FakeLink(
    val wifis: MutableList<WiFi> = mutableListOf(),
    val pairedDevices: MutableList<BluetoothDevice> = mutableListOf(),
    val scanDevices: MutableList<BluetoothDevice> = mutableListOf(),
    initialState: State = State.Connected
) : Link {

    var networkId = wifis.maxOf { it.networkId ?: -1 } + 100
    override val wifi = MutableStateFlow<WiFi?>(null)
    override val bluetooth = MutableStateFlow<BluetoothDevice?>(null)


    suspend fun <T> simulateDelay(block: () -> T): T {
        delay(Random.nextLong(1000, 2500))
        return block()
    }

    override suspend fun listWiFi(): List<WiFi> = wifis

    private fun getWifi(networkId: Int) = wifis.first { it.networkId == networkId }
    private fun getBluetooth(name: String): BluetoothDevice? = pairedDevices
        .firstOrNull { it.name == name }

    override suspend fun selectWiFi(networkId: Int) = simulateDelay {
        wifi.value = getWifi(networkId)
    }

    override suspend fun forgetWiFi(networkId: Int) = simulateDelay {
        val index = wifis.indexOfFirst { it.networkId == networkId }
        if (wifi.value?.networkId == networkId) {
            wifi.value = null
        }
        wifis[index] = wifis[index].copy(networkId = null)
    }

    override suspend fun connectToWifi(connectionInfo: WifiConnectionInfo): String? =
        simulateDelay {
            val connectWifi = wifis.first { it.name == connectionInfo.wifi.name }

            if (connectWifi.networkId == null) {
                wifis.replace(connectWifi, connectWifi.copy(networkId = networkId++))
            }

            wifi.value = connectWifi

            null
        }

    override suspend fun pairedDevices(): List<BluetoothDevice> = simulateDelay {
        pairedDevices
    }

    override suspend fun callLink(): String? = simulateDelay {
        null
    }

    override fun scanBluetooth(): Flow<BluetoothDevice> {
        return scanDevices.asFlow().map {
            simulateDelay { it }
        }
    }

    override suspend fun selectBluetooth(name: String) = simulateDelay {
        val bt = getBluetooth(name) ?: scanDevices.first { it.name == name }

        bluetooth.value = bt
    }

    override suspend fun forgetBluetooth(name: String) = simulateDelay {
        val index = pairedDevices.indexOfFirst { it.name == name }

        pairedDevices.removeAt(index)
        if (bluetooth.value?.name == name) {
            bluetooth.value = null
        }
    }

    override val state: StateFlow<State> = MutableStateFlow(initialState)
}

private fun <T> MutableList<T>.replace(t: T, with: T) {
    val index = indexOf(t)
    if (index == -1) {
        error("Element was not found")
    }
    this[index] = with
}