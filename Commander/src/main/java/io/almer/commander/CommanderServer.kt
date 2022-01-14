package io.almer.commander

import WiFiCommander
import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.ParcelUuid
import io.almer.companionshared.model.WifiConnectionInfo
import io.almer.companionshared.model.toBluetoothDeviceModel
import io.almer.companionshared.model.toWiFI
import io.almer.companionshared.server.*
import io.almer.companionshared.server.commands.*
import io.almer.companionshared.server.commands.command.Listen
import io.almer.companionshared.server.commands.command.Read
import io.almer.companionshared.server.commands.command.Write
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import kotlin.math.min


@OptIn(ExperimentalSerializationApi::class)
inline fun toGattSuccess(byteArray: ByteArray) =
    Pair(BluetoothGatt.GATT_SUCCESS, byteArray)

private fun toGattSuccess() = Pair(BluetoothGatt.GATT_SUCCESS, null)

private data class NotificationEnableResponse(
    val status: Int = BluetoothGatt.GATT_SUCCESS,
    val shouldNotify: Boolean = false,
    val notificationValue: ByteArray? = null
)

class CommanderServer(
    val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : AutoCloseable {

    private val wifiCommander = WiFiCommander(context)
    private val bluetoothCommander = BluetoothCommander(context)
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
    private val _device = MutableStateFlow<BluetoothDevice?>(null)
    val device get() = _device.asStateFlow()

    private var gattServer: BluetoothGattServer? = null
    private val gattServerCallback: BluetoothGattServerCallback = GattServerCallback()

    private val characteristicCommandCatalog = setupCharacteristicCommandCatalog()

    private val gattWriteRequestChannel = Channel<GattWriteRequest>(200)
    private val gattReadRequestChannel = Channel<GattReadRequest>(200)

    private var mtu: Int = 23

    // todo use requestId as key to ensure atomics
    private val responseRegister = ResponseRegister()

//    private var _deviceScope: CoroutineScope = scope + Dispatchers.Default
//    private val deviceScope get() = _deviceScope

//    fun newScope() {
//        _deviceScope.cancel()
//        _deviceScope = scope + Dispatchers.Default
//    }

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
                _device.value = device
//                newScope()
            } else {
                _device.compareAndSet(device, null)
            }
        }

        override fun onMtuChanged(device: BluetoothDevice?, mtu: Int) {
            super.onMtuChanged(device, mtu)
            Timber.i("MTU Changed to $mtu")
            this@CommanderServer.mtu = mtu
        }


        override fun onDescriptorReadRequest(
            device: BluetoothDevice?,
            requestId: Int,
            offset: Int,
            descriptor: BluetoothGattDescriptor?
        ) {
            super.onDescriptorReadRequest(device, requestId, offset, descriptor)

            Timber.d("New descriptor read request")
        }

        override fun onDescriptorWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            descriptor: BluetoothGattDescriptor?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            super.onDescriptorWriteRequest(
                device,
                requestId,
                descriptor,
                preparedWrite,
                responseNeeded,
                offset,
                value
            )


            Timber.d("New descriptor write request")

            if (descriptor == null)
                return

            if (descriptor.uuid == CCCD) {
                val uuid = listenUUID(descriptor.characteristic.uuid) ?: kotlin.run {
                    Timber.w("Unknown listen UUID ${descriptor.characteristic.uuid}$}")
                    return
                }

                val response: NotificationEnableResponse = when {
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE.contentEquals(value) -> when (uuid) {
                        ListenUUID.WiFi -> enableWifiNotifications()
                        ListenUUID.Bluetooth -> enableBluetoothNotifications()
                        ListenUUID.ScanBluetooth -> enableScanBluetoothNotifications(device)
                    }

                    BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE.contentEquals(value) -> {
                        disableNotification(uuid)
                    }
                    else -> {
                        Timber.w("Unknown value on CCCD descriptor")
                        return
                    }
                }

                if (responseNeeded) {
                    Timber.d("Sending successful response")
                    gattServer?.sendResponse(device, requestId, response.status, 0, null)
                }

                if (response.shouldNotify) {
                    descriptor.characteristic.value = response.notificationValue
                    gattServer?.notifyCharacteristicChanged(
                        device,
                        descriptor.characteristic,
                        false
                    )
                }
            }
        }

        var listens = mutableMapOf<ListenUUID, Job>()


        fun enableWifiNotifications(): NotificationEnableResponse {
            listens.put(ListenUUID.WiFi,
//                deviceScope.launch {
                scope.launch {
                    wifiCommander.wifi.collect {
                        val char = characteristicCommandCatalog.WiFi

                        char.value =
                            Listen.WiFi.serializeResponse(it?.toWiFI())
                        this@CommanderServer.gattServer?.notifyCharacteristicChanged(
                            device.value,
                            char,
                            false
                        )
                    }
                })?.apply { cancel() }

            return NotificationEnableResponse(
                shouldNotify = true,
                notificationValue = Listen.WiFi.serializeResponse(wifiCommander.wifi.value?.toWiFI())
            )
        }

        fun enableBluetoothNotifications(): NotificationEnableResponse {
            listens.put(ListenUUID.Bluetooth,
//                deviceScope.launch {
                scope.launch {
                    bluetoothCommander.headset.collect {
                        val char = characteristicCommandCatalog.Bluetooth

                        char.value =
                            Listen.Bluetooth.serializeResponse(
                                it?.connectedDevices?.firstOrNull()?.toBluetoothDeviceModel(true)
                            )

                        this@CommanderServer.gattServer?.notifyCharacteristicChanged(
                            device.value,
                            char,
                            false
                        )
                    }
                })?.apply { cancel() }

            return NotificationEnableResponse(
                shouldNotify = true,
                notificationValue = Listen.Bluetooth.serializeResponse(
                    bluetoothCommander.headset.value?.connectedDevices?.firstOrNull()
                        ?.toBluetoothDeviceModel(true)
                )
            )

        }

        fun enableScanBluetoothNotifications(device: BluetoothDevice?): NotificationEnableResponse {
            val newJob = scope.launch(start = CoroutineStart.LAZY) {
                bluetoothCommander.scanDevices().collect {
                    Timber.d("New BluetoothDevice model found")
                    val char = characteristicCommandCatalog.ScanBluetooth

                    char.value = Listen.ScanBluetooth.serializeResponse(it)
                    this@CommanderServer.gattServer?.notifyCharacteristicChanged(
                        device,
                        char,
                        false
                    )
                }
            }
            listens.put(ListenUUID.ScanBluetooth, newJob)?.apply { cancel() }
            newJob.start()
            return NotificationEnableResponse()
        }

        fun disableNotification(uuid: ListenUUID): NotificationEnableResponse {
            Timber.d("Disabling notifications on $uuid")
            listens.get(uuid)?.apply {
                Timber.d("Found active job, disabling it")
                cancel()
            } ?: kotlin.run {
                Timber.d("Did not find active job")
            }
            return NotificationEnableResponse()
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

            if (characteristic == null) {
                Timber.w("No characteristic has been requested")
                return
            }
            if (offset > 0) {
                Timber.d("Request to continue ${device?.name} $characteristic with offset $offset")
                responseRegister.get(device, characteristic)?.let { response ->
                    val to = min(offset + mtu, response.value.size)

                    Timber.d("Request to continue ${device?.name} $characteristic with to $to because size ${response.value.size}")

                    gattServer?.sendResponse(
                        device,
                        requestId,
                        response.result,
                        offset,
                        response.value.sliceArray(offset until to)
                    )
                } ?: kotlin.run {
                    Timber.w("Request to continue ${device?.name} $characteristic with offset $offset was not found")
                }
            } else {
                Timber.d("Request to read ${device?.name} $characteristic with offset $offset")

                readUUID(characteristic.uuid) ?: listenUUID(characteristic.uuid) ?: kotlin.run {
                    Timber.w("Unrecognized UUID")
                    return
                }

                Timber.d("Sending new request to gattReadRequestChannel")
                gattReadRequestChannel.trySendBlocking(
                    GattReadRequest(
                        device, requestId, offset, characteristic
                    )
                )
                Timber.d("Request sent to gattReadRequestChannel")

            }
        }
    }

    init {
        gattWriteRequestChannel.receiveAsFlow().onEach { request ->
            if (request.value == null) {
                Timber.w("Empty value was sent on ${request.characteristic}")
                return@onEach
            }

            val (result, value) = when (writeUUID(request.characteristic.uuid)) {
                WriteUUID.SelectWiFi -> handleSelectWifi(Write.SelectWiFi.deserializeRequest(request.value))
                WriteUUID.ConnectToWifi -> handleConnectToWifi(
                    Write.ConnectToWifi.deserializeRequest(
                        request.value
                    )
                )
                WriteUUID.ForgetWiFi -> handleForgetWifi(Write.ForgetWiFi.deserializeRequest(request.value))
                WriteUUID.SelectBluetooth -> handleSelectBluetooth(
                    Write.SelectBluetooth.deserializeRequest(
                        request.value
                    )
                )
                WriteUUID.ForgetBluetooth -> handleForgetBluetooth(
                    Write.ForgetBluetooth.deserializeRequest(
                        request.value
                    )
                )
                null -> {
                    Timber.w("Could not identify request UUID")
                    return@onEach
                }
            }
            gattServer?.sendResponse(request.device, request.requestId, result, 0, value)
        }.launchIn(scope)

        gattReadRequestChannel.receiveAsFlow().onEach { request ->

            Timber.d("New gattReadRequestChannel message")

            val uuid = request.characteristic?.uuid!!

            val (result, value) = when (readUUID(uuid)) {
                ReadUUID.ListWiFi -> handleListWifi()
                ReadUUID.PairedDevices -> handlePairedDevices()
                null -> {
                    when (listenUUID(uuid)) {
                        ListenUUID.WiFi -> toGattSuccess(Listen.WiFi.serializeResponse(wifiCommander.wifi.value?.toWiFI()))
                        ListenUUID.Bluetooth -> toGattSuccess(
                            Listen.Bluetooth.serializeResponse(
                                bluetoothCommander.headset.value?.connectedDevices?.firstOrNull()
                                    ?.toBluetoothDeviceModel(true)
                            )
                        )
                        ListenUUID.ScanBluetooth -> TODO()
                        null -> error("Unrecognized UUID")
                    }
                }
            }

            Timber.d("Request handled")


            mtu.let { mtu ->
                if (value.size < mtu) {
                    Timber.d("Request sent fully for  ${request.device?.name} ${request.characteristic}")
                    gattServer?.sendResponse(request.device, request.requestId, result, 0, value)
                } else {
                    val toSend = value.sliceArray(0 until mtu)
                    Timber.d("Request sent partially for  ${request.device?.name} ${request.characteristic} ${toSend.toList()}")
                    responseRegister.add(request.device, request.characteristic, result, value)
                    gattServer?.sendResponse(
                        request.device,
                        request.requestId,
                        result,
                        0,
                        toSend
                    )
                }
            }

        }.launchIn(scope)
    }

    private fun handleScanBluetooth(): Pair<Int, ByteArray> {
        TODO("Not yet implemented")
    }

    private suspend fun handlePairedDevices(): Pair<Int, ByteArray> {
        val devices = bluetoothCommander.getBondedDevices()

        return toGattSuccess(Read.PairedDevices.serializeResponse(devices))
    }

    private fun handleSelectBluetooth(name: String): Pair<Int, ByteArray?> {
        val isConnected = bluetoothCommander.selectDevice(name)

//        return toGattSuccess(Write.SelectBluetooth.serializeResponse(isConnected))
        return toGattSuccess()
    }

    private fun handleForgetBluetooth(name: String): Pair<Int, ByteArray?> {
        bluetoothCommander.forgetDevice(name)
        return toGattSuccess()
    }

    private fun handleForgetWifi(networkId: Int): Pair<Int, ByteArray?> {
        wifiCommander.forgetWifi(networkId)

        return toGattSuccess()
    }

    private suspend fun handleConnectToWifi(wifiConnectionInfo: WifiConnectionInfo): Pair<Int, ByteArray?> {
        val networkId =
            wifiCommander.learnWifiWPA(wifiConnectionInfo.wifi.ssid, wifiConnectionInfo.password)

        wifiCommander.setWifi(networkId)

        return toGattSuccess()
    }

    private suspend fun handleListWifi(): Pair<Int, ByteArray> {
        Timber.d("handleListWifi()")
        val wifis = wifiCommander.listWifi()

        return toGattSuccess(Read.ListWiFi.serializeResponse(wifis))
    }

    private suspend fun handleSelectWifi(networkId: Int): Pair<Int, ByteArray?> {
        wifiCommander.setWifi(networkId)

        return toGattSuccess()
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