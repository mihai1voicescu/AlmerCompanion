package io.almer.companionshared.server

import android.bluetooth.*
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
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
class DeviceScan(context: Context) {


    private val _scanResults = MutableStateFlow<Map<String, BluetoothDevice>>(mapOf())
    val scanResults = _scanResults.asStateFlow()

    // LiveData for sending the view state to the DeviceScanFragment
    private val _viewState: MutableStateFlow<DeviceScanViewState> = MutableStateFlow(
        DeviceScanViewState.Done
    )
    val viewState: StateFlow<DeviceScanViewState> = _viewState

    // BluetoothAdapter should never be null since BLE is required per
    // the <uses-feature> tag in the AndroidManifest.xml
    private val adapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    private val scanFilters = buildScanFilters()
    private val scanSettings = buildScanSettings()


    fun addDevice(device: BluetoothDevice) {
        val name = device.name ?: kotlin.run {
            Timber.w("Device %s has no name", device)
            return
        }
        _scanResults.value = _scanResults.value + (name to device)
    }

    suspend fun scan(scanMillis: Long) {
        val scanner = adapter.bluetoothLeScanner!!

        val cb = object : ScanCallback() {
            override fun onBatchScanResults(results: List<ScanResult>) {
                super.onBatchScanResults(results)
                Timber.i("Finish scan")
                for (item in results) {
                    addDevice(item.device)
                }
                _viewState.value = DeviceScanViewState.Done
            }

            override fun onScanResult(
                callbackType: Int,
                result: ScanResult
            ) {
                super.onScanResult(callbackType, result)
                Timber.i("Found a result")
                addDevice(result.device)
            }


            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)
                // Send error state to the fragment to display
                val errorMessage = "Scan failed with error: $errorCode"
                error(errorMessage)
            }
        }

        Timber.d("Spawning coroutine scope")

        coroutineScope {
            launch {
                Timber.d("Kill coroutine: launched")
                delay(scanMillis)
                Timber.d("Kill coroutine: about to kill")
                scanner.stopScan(cb)
                Timber.d("Kill coroutine: kill sent")
            }

            Timber.d("Activating scan")
            scanner.startScan(scanFilters, scanSettings, cb)
            Timber.d("Awaiting close job")
        }

        Timber.d("Closed")
    }
//    fun startScan() {
//        // If advertisement is not supported on this device then other devices will not be able to
//        // discover and connect to it.
//        if (!adapter.isMultipleAdvertisementSupported) {
//            error("Advertising not supported")
//        }
//
//        if (scanCallback == null) {
//            scanner = adapter.bluetoothLeScanner
//            Log.d(TAG, "Start Scanning")
//            // Update the UI to indicate an active scan is starting
//            _viewState.value = DeviceScanViewState.ActiveScan
//
//            // Stop scanning after the scan period
//            Handler().postDelayed({ stopScanning() }, SCAN_PERIOD)
//
//            // Kick off a new scan
//            scanCallback = DeviceScanCallback()
//            scanner?.startScan(scanFilters, scanSettings, scanCallback)
//        } else {
//            Log.d(TAG, "Already scanning")
//        }
//    }
//
//    private fun stopScanning() {
//        Log.d(TAG, "Stopping Scanning")
//        scanner?.stopScan(scanCallback)
//        scanCallback = null
//        // return the current results
//        _viewState.value = DeviceScanViewState.Done
//    }

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

//    /**
//     * Custom ScanCallback object - adds found devices to list on success, displays error on failure.
//     */
//    private inner class DeviceScanCallback : ScanCallback() {
//        override fun onBatchScanResults(results: List<ScanResult>) {
//            super.onBatchScanResults(results)
//            for (item in results) {
//                item.device?.let { device ->
//                    addDevice(device)
//                }
//            }
//            _viewState.value = DeviceScanViewState.Done
//        }
//
//        override fun onScanResult(
//            callbackType: Int,
//            result: ScanResult
//        ) {
//            super.onScanResult(callbackType, result)
//            result.device?.let { device ->
//                addDevice(device)
//            }
//            _viewState.value = DeviceScanViewState.Done
//        }
//
//        override fun onScanFailed(errorCode: Int) {
//            super.onScanFailed(errorCode)
//            // Send error state to the fragment to display
//            val errorMessage = "Scan failed with error: $errorCode"
//            _viewState.value = DeviceScanViewState.Error(errorMessage)
//        }
//    }


}