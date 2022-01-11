package io.almer.almercompanion

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanFilter
import android.companion.*
import android.content.Context
import android.content.IntentSender
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.ParcelUuid
import androidx.annotation.MainThread
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat.startIntentSenderForResult
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.juul.kable.Advertisement
import com.juul.kable.Peripheral
import com.juul.kable.peripheral
import io.almer.almercompanion.link.Link
import io.almer.companionshared.server.DeviceScan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.plus
import timber.log.Timber
import java.util.*
import java.util.regex.Pattern

const val SELECT_DEVICE_REQUEST_CODE = 42

class MainApp : Application() {

    val settings by lazy {
        AppSettings(this)
    }

    private val scope = CoroutineScope(Dispatchers.Main)

    val deviceScan = DeviceScan(this)

    private val _link = MutableStateFlow<Link?>(null)

    val linkState = _link.asStateFlow()
    val link get() = _link.value ?: error("No link is selected")

    suspend fun selectDevice(advertisement: Advertisement) {
        val peripheral = scope.peripheral(advertisement)
        _link.value = Link(this, peripheral, scope)
    }

    fun disconnectPeripheral() {
        _link.value = null
    }

    val isWifiEnabled: Boolean
        get() {
            val cm = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
            val isConnected: Boolean = activeNetwork?.isConnectedOrConnecting == true

            return isConnected
        }

    override fun onCreate() {
        super.onCreate()

        Timber.plant(Timber.DebugTree())

//        _link = Link(this)

        val deviceFilter: BluetoothDeviceFilter = BluetoothDeviceFilter.Builder()
            // Match only Bluetooth devices whose name matches the pattern.
            .setNamePattern(Pattern.compile("My device"))
            // Match only Bluetooth devices whose service UUID matches this pattern.
            .addServiceUuid(ParcelUuid(UUID(0x123abcL, -1L)), null)
            .build()
    }

    companion object {

        @Composable
        fun mainApp(): MainApp {
            return LocalContext.current.applicationContext!! as MainApp
        }
    }
}