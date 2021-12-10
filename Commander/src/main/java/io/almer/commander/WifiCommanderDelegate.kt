import android.net.wifi.ScanResult
import android.net.wifi.WifiInfo

interface WifiCommanderDelegate {
    fun getCurrentWifiInfo(): WifiInfo?
    suspend fun scanWifi(): List<ScanResult>
}