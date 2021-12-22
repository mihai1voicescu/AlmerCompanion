import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import androidx.core.app.ActivityCompat
import io.almer.companionshared.model.WiFi

import android.net.*
import android.net.ConnectivityManager.NetworkCallback
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import android.R.attr.name

fun Boolean.successString(): String = if (this) "successful" else "unsuccessful"

class WiFiCommander private constructor(
    val context: Context,
    val wifiCommanderDelegate: WifiCommanderDelegate
) : WifiCommanderDelegate by wifiCommanderDelegate, AutoCloseable {

    // todo Maybe use scrambled SSIDs so the data
    //  transmitted via BT can not be used to track the user


    override suspend fun scanWifi(): List<ScanResult> {
        return wifiCommanderDelegate.scanWifi().distinctBy { it.SSID }
            .filter { it.SSID != null && it.SSID != "" }
    }


    private fun configuredNetworks(): MutableList<WifiConfiguration> {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            error("No permission")
        }
        return wifiManager.configuredNetworks!!
    }

    suspend fun listWifi(): List<WiFi> {
        val list = scanWifi()

        val known = configuredNetworks().associateBy { it.SSID.trim('"') }

        return list.map {
            WiFi(
                it.SSID,
                it.SSID,
                it.level,
                known[it.SSID]?.networkId
            )
        }
    }


    companion object {
        operator fun invoke(context: Context): WiFiCommander {
            val wifiCommanderDelegate =
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                    RWifiCommanderDelegate(context)
//                } else {
                PreRWifiCommander(context)
//                }

            return WiFiCommander(context, wifiCommanderDelegate)
        }
    }

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private val _wifi = MutableStateFlow(wifiCommanderDelegate.getCurrentWifiInfo())
    val wifi = _wifi.asStateFlow()

    private val networkCallback = object :
        NetworkCallback() {
        override fun onAvailable(network: Network) {
            _wifi.value = wifiCommanderDelegate.getCurrentWifiInfo()
        }

        override fun onLost(network: Network) {
            _wifi.value = wifiCommanderDelegate.getCurrentWifiInfo()
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            _wifi.value = wifiCommanderDelegate.getCurrentWifiInfo()
        }
    }

    init {
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
    }

    // todo Probably does not handle lifecycle accordingly
    override fun close() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    fun learnWifiWPA(ssid: String, key: String): Int {
        val conf = WifiConfiguration()
        conf.SSID = java.lang.String.format("\"%s\"", ssid)
        conf.preSharedKey = String.format("\"%s\"", key)


        return wifiManager.addNetwork(conf)
    }

    fun learnWifiWEP(ssid: String, networkPass: String): Int {
        val conf = wifiConfig(ssid)
        conf.wepKeys[0] = "\"" + networkPass + "\"";
        conf.wepTxKeyIndex = 0;
        conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);

        return wifiManager.addNetwork(conf)
    }

    fun learnWifiOpen(ssid: String): Int {
        val conf = wifiConfig(ssid)
        conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        return wifiManager.addNetwork(conf)
    }

    private fun wifiConfig(ssid: String): WifiConfiguration {
        val conf = WifiConfiguration()
        conf.SSID = java.lang.String.format("\"%s\"", ssid)

        return conf
    }

    fun forgetWifi(networkId: Int) {
        wifiManager.removeNetwork(networkId)
    }

    private fun forgetAll() {
        val list = configuredNetworks()
        for (i in list) {
            val isRemoved = wifiManager.removeNetwork(i.networkId)
            Timber.d("Wifi %s removal was %s", i.SSID, isRemoved.successString())
            wifiManager.saveConfiguration()
        }
    }

    fun disableWifi(networkId: Int) {
        val isDisabled = wifiManager.disableNetwork(networkId)
        Timber.d("Wifi %s disabling was %s", networkId, isDisabled.successString())
    }

    suspend fun setWifi(networkId: Int) {
//        forgetAll()

        coroutineScope {
            wifi.value?.let { disableWifi(it.networkId) }

            val hasReset = async {
                wifi.takeWhile { it == null }.collect()
            }
            wifiManager.disconnect()
            hasReset.await()

            wifiManager.enableNetwork(networkId, true)
            wifiManager.reconnect()
        }
    }
}