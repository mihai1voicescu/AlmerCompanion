import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first

class PreRWifiCommander(val context: Context) : WifiCommanderDelegate {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager

    override fun getCurrentWifiInfo(): WifiInfo? {
        return wifiManager.connectionInfo
    }

    override suspend fun scanWifi(): List<ScanResult> {
        return callbackFlow<List<ScanResult>> {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    context!!.unregisterReceiver(this)
                    trySend(wifiManager.scanResults)
                    channel.close()
                }
            }

            context.registerReceiver(
                receiver,
                IntentFilter().apply { addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) })
            wifiManager.startScan()
            awaitClose()
        }.buffer(1).first()
    }
}