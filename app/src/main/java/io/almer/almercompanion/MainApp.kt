package io.almer.almercompanion

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.*
import android.net.ConnectivityManager
import android.net.NetworkInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.juul.kable.Advertisement
import com.juul.kable.AndroidPeripheral
import com.juul.kable.peripheral
import io.almer.almercompanion.link.Link
import io.almer.companionshared.CrashlyticsLogger
import io.almer.companionshared.server.DeviceScan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.lighthousegames.logging.KmLogging
import org.lighthousegames.logging.LogLevelController
import org.lighthousegames.logging.PlatformLogger

const val SELECT_DEVICE_REQUEST_CODE = 42

enum class BluetoothState {
    Unknown,
    NotSupported,
    Off,
    On
}

class MainApp : Application() {

    private lateinit var analytics: FirebaseAnalytics

    val settings by lazy {
        AppSettings(this)
    }

    private val scope = CoroutineScope(Dispatchers.Main)

    val deviceScan = DeviceScan(this)

    private val _link = MutableStateFlow<Link?>(null)

    val linkState = _link.asStateFlow()
    val link get() = _link.value ?: error("No link is selected")

    suspend fun selectDevice(advertisement: Advertisement) {
        val peripheral: AndroidPeripheral = scope.peripheral(advertisement) as AndroidPeripheral


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

        KmLogging.addLogger(CrashlyticsLogger())
        analytics = Firebase.analytics
        KmLogging.setLoggers(PlatformLogger(object : LogLevelController {
            override fun isLoggingDebug() = true

            override fun isLoggingError() = true

            override fun isLoggingInfo() = true

            override fun isLoggingVerbose() = false

            override fun isLoggingWarning() = true
        }))

        listen()
    }

    companion object {

        @Composable
        fun mainApp(): MainApp {
            return LocalContext.current.applicationContext!! as MainApp
        }
    }


    val _bluetoothState = MutableStateFlow(BluetoothState.Unknown)
    val bluetoothState = _bluetoothState.asStateFlow()

    fun listen() {
        val receiver = object : BroadcastReceiver() {
            var hasClosed = false
            override fun onReceive(context: Context, intent: Intent) {
                if (hasClosed)
                    return
                val action: String? = intent.action
                when (action) {

                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        _bluetoothState.value = getCurrentBluetoothState()
                    }
                }
            }
        }
        this.registerReceiver(
            receiver,
            IntentFilter(BluetoothDevice.ACTION_FOUND).apply {
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            }
        )

        _bluetoothState.value = getCurrentBluetoothState()
    }

    fun getCurrentBluetoothState(): BluetoothState {
        val mBluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (mBluetoothAdapter == null) {
            return BluetoothState.NotSupported
        } else if (!mBluetoothAdapter.isEnabled()) {
            // Bluetooth is not enabled :)
            return BluetoothState.Off
        } else {
            // Bluetooth is enabled
            return BluetoothState.On
        }
    }
}