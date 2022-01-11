package io.almer.companionshared.server

import android.Manifest
import android.bluetooth.*
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.ParcelUuid
import androidx.core.app.ActivityCompat
import com.juul.kable.Advertisement
import com.juul.kable.ObsoleteKableApi
import com.juul.kable.Peripheral
import com.juul.kable.Scanner
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import timber.log.Timber


private const val TAG = "DeviceScanViewModel"

// 30 second scan period
private const val SCAN_PERIOD = 30000L

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceScan(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    @OptIn(ObsoleteKableApi::class)
    private fun scanner(): Scanner {
        return Scanner {
            services = listOf(
                SERVICE_UUID
            )
            scanSettings = this@DeviceScan.buildScanSettings()
        }
    }

    fun scan(): Flow<Advertisement> {
        val scanner = scanner()
        return scanner.advertisements
    }

    /**
     * Return a List of [ScanFilter] objects to filter by Service UUID.
     */
    private fun buildScanFilters(): List<ScanFilter> {
        val builder = ScanFilter.Builder()
        // Comment out the below line to see all BLE devices around you
        builder.setServiceUuid(ParcelUuid(SERVICE_UUID))
        val filter = builder.build()
        return listOf(filter)
    }

    /**
     * Return a [ScanSettings] object set to use low power (to preserve battery life).
     */
    private fun buildScanSettings(): ScanSettings {
        return ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()
    }
}