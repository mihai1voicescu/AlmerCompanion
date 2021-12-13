//import android.content.Context
//import android.net.ConnectivityManager
//import android.net.wifi.ScanResult
//import android.net.wifi.WifiInfo
//import android.net.wifi.WifiManager
//import android.os.Build
//import androidx.annotation.RequiresApi
//import kotlinx.coroutines.ExperimentalCoroutinesApi
//import kotlinx.coroutines.channels.awaitClose
//import kotlinx.coroutines.flow.buffer
//import kotlinx.coroutines.flow.callbackFlow
//import kotlinx.coroutines.flow.first
//
//@RequiresApi(Build.VERSION_CODES.R)
//class RWifiCommanderDelegate(val context: Context) : WifiCommanderDelegate {
//    val connectivityManager =
//        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//    val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
//
//    override fun getCurrentWifiInfo(): WifiInfo? {
//        val network = connectivityManager.activeNetwork ?: return null
//
//        val networkCapabilities =
//            connectivityManager.getNetworkCapabilities(network) ?: return null
//
//        return networkCapabilities.transportInfo as WifiInfo?
//    }
//
//    @OptIn(ExperimentalCoroutinesApi::class)
//    override suspend fun scanWifi() = callbackFlow<List<ScanResult>> {
//        wifiManager.registerScanResultsCallback(
//            {},
//            object : WifiManager.ScanResultsCallback() {
//                override fun onScanResultsAvailable() {
//                    trySend(wifiManager.scanResults).getOrThrow()
//                    channel.close()
//                }
//            })
//
//        awaitClose()
//
//    }.buffer(1).first()
//}