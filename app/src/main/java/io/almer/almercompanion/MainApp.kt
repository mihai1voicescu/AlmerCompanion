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
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat.startIntentSenderForResult
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import java.util.*
import java.util.regex.Pattern

const val SELECT_DEVICE_REQUEST_CODE = 42

class MainApp : Application() {

    val settings by lazy {
        AppSettings(this)
    }

    val isWifiEnabled: Boolean
        get() {
            val cm = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
            val isConnected: Boolean = activeNetwork?.isConnectedOrConnecting == true

            return isConnected
        }

    val bluetoothAdapter by lazy {
        BluetoothAdapter.getDefaultAdapter() ?: error("Device does not support Bluetooth")
    }

    val isBluetoothEnabled: Boolean get() = bluetoothAdapter.isEnabled


    fun filterDevices(deviceFilter: DeviceFilter<*>) {
        val pairingRequest: AssociationRequest = AssociationRequest.Builder()
            // Find only devices that match this request filter.
            .addDeviceFilter(deviceFilter)
            // Stop scanning as soon as one device matching the filter is found.
            .setSingleDevice(true)
            .build()


        val deviceManager =
            this.getSystemService(Context.COMPANION_DEVICE_SERVICE) as CompanionDeviceManager

        deviceManager.associate(
            pairingRequest,
            object : CompanionDeviceManager.Callback() {
                // Called when a device is found. Launch the IntentSender so the user
                // can select the device they want to pair with.
                override fun onDeviceFound(chooserLauncher: IntentSender) {
                    startIntentSenderForResult(
                        MainActivity(),
                        chooserLauncher,
                        SELECT_DEVICE_REQUEST_CODE, null, 0, 0, 0,
                        null
                    )
                }


                override fun onFailure(error: CharSequence?) {
                    // Handle the failure.
                }
            }, null
        )
    }

    override fun onCreate() {
        super.onCreate()


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