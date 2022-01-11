package io.almer.commander

import WiFiCommander
import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.ParcelUuid
import io.almer.companionshared.server.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import timber.log.Timber


@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T> toGattSuccess(t: T) =
    Pair(BluetoothGatt.GATT_SUCCESS, ProtoBuf.encodeToByteArray(t))

class CommanderServer(
    val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : AutoCloseable {

    private val wifiCommander = WiFiCommander(context)
    private val bluetoothManager: BluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    // BluetoothAdapter should never be null if the app is installed from the Play store
    // since BLE is required per the <uses-feature> tag in the AndroidManifest.xml.
    // If the app is installed on an emulator without bluetooth then the app will crash
    // on launch since installing via Android Studio bypasses the <uses-feature> flags
    private val adapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    // This property will be null if bluetooth is not enabled or if advertising is not
    // possible on the device
    private var advertiser: BluetoothLeAdvertiser? = null
    private var advertiseCallback: AdvertiseCallback? = null
    private var advertiseSettings: AdvertiseSettings = buildAdvertiseSettings()
    private var advertiseData: AdvertiseData = buildAdvertiseData()

    // LiveData for reporting the messages sent to the device
    private val _messages = MutableStateFlow(listOf<String>())
    val messages get() = _messages.asStateFlow()

    // LiveData for reporting connection requests
    private val _connectionRequest = Channel<BluetoothDevice>(100)
    val connectionRequest get() = _connectionRequest.receiveAsFlow()

    private var gattServer: BluetoothGattServer? = null
    private val gattServerCallback: BluetoothGattServerCallback = GattServerCallback()

    private val characteristicCommandCatalog = setupCharacteristicCommandCatalog()

    private val gattWriteRequestChannel = Channel<GattWriteRequest>(200)
    private val gattReadRequestChannel = Channel<GattReadRequest>(200)

    init {
        if (!adapter.isEnabled) {
            error("Bluetooth adapter is not enabled")
        }

        gattServer = bluetoothManager.openGattServer(
            context,
            gattServerCallback
        ).apply {
            addService(characteristicCommandCatalog.service)
        }
        startAdvertisement()
    }

    // Properties for current chat device connection
    private val _deviceConnection = Channel<DeviceConnectionState?>(100)

    fun stopServer() {
        stopAdvertising()
    }

    /**
     * Function to create the GATT Server with the required characteristics and descriptors
     */
    private fun setupCharacteristicCommandCatalog(): CharacteristicCommandCatalog {
        // Setup gatt service
        val service = BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        // need to ensure that the property is writable and has the write permission
        val messageCharacteristic = BluetoothGattCharacteristic(
            MESSAGE_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        service.addCharacteristic(messageCharacteristic)
        val confirmCharacteristic = BluetoothGattCharacteristic(
            CONFIRM_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        service.addCharacteristic(confirmCharacteristic)

        return CharacteristicCommandCatalog(service)
    }

    /**
     * Start advertising this device so other BLE devices can see it and connect
     */
    private fun startAdvertisement() {
        advertiser = adapter.bluetoothLeAdvertiser
        Timber.d("startAdvertisement: with advertiser $advertiser")

        if (advertiseCallback == null) {
            advertiseCallback = DeviceAdvertiseCallback()

            advertiser?.startAdvertising(advertiseSettings, advertiseData, advertiseCallback)
        }
    }

    /**
     * Stops BLE Advertising.
     */
    private fun stopAdvertising() {
        Timber.d("Stopping Advertising with advertiser $advertiser")
        advertiser?.stopAdvertising(advertiseCallback)
        advertiseCallback = null
    }

    /**
     * Returns an AdvertiseData object which includes the Service UUID and Device Name.
     */
    private fun buildAdvertiseData(): AdvertiseData {
        /**
         * Note: There is a strict limit of 31 Bytes on packets sent over BLE Advertisements.
         * This limit is outlined in section 2.3.1.1 of this document:
         * https://inst.eecs.berkeley.edu/~ee290c/sp18/note/BLE_Vol6.pdf
         *
         * This limit includes everything put into AdvertiseData including UUIDs, device info, &
         * arbitrary service or manufacturer data.
         * Attempting to send packets over this limit will result in a failure with error code
         * AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE. Catch this error in the
         * onStartFailure() method of an AdvertiseCallback implementation.
         */
        val dataBuilder = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(SERVICE_UUID))
            .setIncludeDeviceName(true)

        /* For example - this will cause advertising to fail (exceeds size limit) */
        //String failureData = "asdghkajsghalkxcjhfa;sghtalksjcfhalskfjhasldkjfhdskf";
        //dataBuilder.addServiceData(Constants.Service_UUID, failureData.getBytes());
        return dataBuilder.build()
    }

    /**
     * Returns an AdvertiseSettings object set to use low power (to help preserve battery life)
     * and disable the built-in timeout since this code uses its own timeout runnable.
     */
    private fun buildAdvertiseSettings(): AdvertiseSettings {
        return AdvertiseSettings.Builder()
//            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTimeout(0)
            .build()
    }

    data class GattWriteRequest(
        val device: BluetoothDevice,
        val requestId: Int,
        val characteristic: BluetoothGattCharacteristic,
        val preparedWrite: Boolean,
        val responseNeeded: Boolean,
        val offset: Int,
        val value: ByteArray?
    )

    data class GattReadRequest(
        val device: BluetoothDevice?,
        val requestId: Int,
        val offset: Int,
        val characteristic: BluetoothGattCharacteristic?
    )

    /**
     * Custom callback for the Gatt Server this device implements
     */
    private inner class GattServerCallback : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            super.onConnectionStateChange(device, status, newState)
            val isSuccess = status == BluetoothGatt.GATT_SUCCESS
            val isConnected = newState == BluetoothProfile.STATE_CONNECTED
            Timber.d(
                "onConnectionStateChange: Server $device ${device.name} success: $isSuccess connected: $isConnected"
            )
            if (isSuccess && isConnected) {
                _connectionRequest.trySendBlocking(device)
            } else {
                _deviceConnection.trySendBlocking(DeviceConnectionState.Disconnected)
            }
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            super.onCharacteristicWriteRequest(
                device,
                requestId,
                characteristic,
                preparedWrite,
                responseNeeded,
                offset,
                value
            )

            when (characteristic.uuid) {
                MESSAGE_UUID -> {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
                    val message = value?.toString(Charsets.UTF_8)
                    Timber.d("onCharacteristicWriteRequest: Have message: \"$message\"")
                    message?.let {
                        _messages.value = _messages.value + message
                    }
                }

                else -> {
                    gattWriteRequestChannel.trySendBlocking(
                        GattWriteRequest(
                            device,
                            requestId,
                            characteristic,
                            preparedWrite,
                            responseNeeded,
                            offset,
                            value
                        )
                    )
                }
            }
        }


        override fun onCharacteristicReadRequest(
            device: BluetoothDevice?,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic?
        ) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic)
        }
    }

    init {
        gattWriteRequestChannel.receiveAsFlow().onEach { request ->
            val (result, value) = when (request.characteristic.uuid) {
                CommandsUUID.ListWiFi -> {
                    handleListWifi()
                }
                else -> error("Could not identify request UUID")
            }
            gattServer?.sendResponse(request.device, request.requestId, result, 0, value)
        }.launchIn(scope)

        gattReadRequestChannel.receiveAsFlow().onEach { request ->
            val (result, value) = when (request.characteristic?.uuid) {
                CommandsUUID.ListWiFi -> {
                    handleListWifi()
                }
                else -> error("Could not identify request UUID")
            }
            gattServer?.sendResponse(request.device, request.requestId, result, 0, value)
        }.launchIn(scope)
    }

    private suspend fun handleListWifi(): Pair<Int, ByteArray> {
        val wifis = wifiCommander.listWifi()

        return toGattSuccess(wifis)
    }

    /**
     * Custom callback after Advertising succeeds or fails to start. Broadcasts the error code
     * in an Intent to be picked up by AdvertiserFragment and stops this Service.
     */
    private class DeviceAdvertiseCallback : AdvertiseCallback() {
        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            // Send error state to display
            val errorMessage = "Advertise failed with error: $errorCode"
            Timber.d("Advertising failed")
            //_viewState.value = DeviceScanViewState.Error(errorMessage)
        }

        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            super.onStartSuccess(settingsInEffect)
            Timber.d("Advertising successfully started")
        }
    }

    override fun close() {
        gattServer?.close()
    }
}