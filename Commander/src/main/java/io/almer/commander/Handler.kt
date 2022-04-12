package io.almer.commander

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.ParcelUuid
import io.almer.companionshared.server.CCCD
import io.almer.companionshared.server.SERVICE_UUID
import io.almer.companionshared.server.commands.command.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import org.lighthousegames.logging.logging
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min
import kotlin.reflect.KProperty

interface HandlerAction<T : Command<*>> : Action<T> {
    val characteristic: BluetoothGattCharacteristic
}

sealed interface WriteHandlerAction<Request, Cmd : WriteCommand<Request>> : HandlerAction<Cmd> {
    suspend fun write(request: ByteArray)
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

private class WriteHandlerActionImpl<Request, Cmd : WriteCommand<Request>> constructor(
    command: Cmd,
    private val handler: suspend (Request) -> Unit,
) : WriteHandlerAction<Request, Cmd>, ActionImpl<Cmd>(command) {
    override suspend fun write(request: ByteArray) {
        val requestObj = command.decode(request)
        handler(requestObj)
    }

    override val characteristic = BluetoothGattCharacteristic(
        uuid,
        BluetoothGattCharacteristic.PROPERTY_WRITE,
        BluetoothGattCharacteristic.PERMISSION_WRITE
    )
}

sealed interface ReadHandlerAction<Response, Cmd : ReadCommand<Response>> : HandlerAction<Cmd> {
    suspend fun read(): ByteArray
}

private class ReadHandlerActionImpl<Response, Cmd : ReadCommand<Response>>(
    command: Cmd,
    private val handler: suspend () -> Response,
) : ReadHandlerAction<Response, Cmd>, ActionImpl<Cmd>(command) {
    override suspend fun read(): ByteArray {
        val responseObj = handler()
        return command.encode(responseObj)
    }

    override val characteristic = BluetoothGattCharacteristic(
        uuid,
        BluetoothGattCharacteristic.PROPERTY_READ,
        BluetoothGattCharacteristic.PERMISSION_READ
    )
}

sealed interface ListenHandlerAction<Response, Cmd : ListenCommand<Response>> :
    HandlerAction<Cmd> {
    suspend fun listen(): Flow<ByteArray>
}

private class ListenHandlerActionImpl<Response, Cmd : ListenCommand<Response>>(
    command: Cmd,
    private val handler: suspend () -> Flow<Response>,
) : ListenHandlerAction<Response, Cmd>, ActionImpl<Cmd>(command) {
    override suspend fun listen(): Flow<ByteArray> {
        return handler().map { command.encode(it) }
    }

    override val characteristic = BluetoothGattCharacteristic(
        uuid,
        BluetoothGattCharacteristic.PROPERTY_NOTIFY,
        BluetoothGattCharacteristic.PERMISSION_READ
    ).apply {
        addDescriptor(
            BluetoothGattDescriptor(
                UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"),
                BluetoothGattDescriptor.PERMISSION_WRITE
            )
        )
    }
}

sealed class HandlerState {
    object Disconnected : HandlerState()
    object Starting : HandlerState()
    object Advertising : HandlerState()
    class Connected(val device: BluetoothDevice) : HandlerState()

    class Erred(val throwable: Throwable) : HandlerState()
}

// fixme deal with this nicely
@SuppressLint("MissingPermission")
abstract class Handler(
    scope: CoroutineScope,
    val context: Context,
) : ActionBase<HandlerAction<*>>() {
    companion object {
        val Log = logging()
    }

    private val scope = scope + scope.coroutineContext

    private val advertiseSettings: AdvertiseSettings by lazy { buildAdvertiseSettings() }
    private val advertiseData: AdvertiseData by lazy { buildAdvertiseData() }

    private val bluetoothManager: BluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    // BluetoothAdapter should never be null if the app is installed from the Play store
    // since BLE is required per the <uses-feature> tag in the AndroidManifest.xml.
    // If the app is installed on an emulator without bluetooth then the app will crash
    // on launch since installing via Android Studio bypasses the <uses-feature> flags
    private val adapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private val advertiser: BluetoothLeAdvertiser = adapter.bluetoothLeAdvertiser

    private val _writeActions = mutableMapOf<UUID, WriteHandlerAction<*, *>>()
    private val _readActions = mutableMapOf<UUID, ReadHandlerAction<*, *>>()
    private val _listenActions = mutableMapOf<UUID, ListenHandlerAction<*, *>>()

    // todo handle MTU state
    private val _state = MutableStateFlow<HandlerState>(HandlerState.Disconnected)
    val state = _state.asStateFlow()

    private val processingQueue = Mutex()

    private var mtu by AtomicInteger(23)

    private inline fun mtuGuard(
        device: BluetoothDevice?,
        requestId: Int,
        block: (device: BluetoothDevice) -> Unit
    ) {
        if (device == null) {
            Log.w { "Anonymous requests are not supported" }
            return
        }
        if (mtu != 512) {
            gattServer.sendResponse(
                device,
                requestId,
                BluetoothGatt.GATT_FAILURE,
                0,
                "MTU needs to be 512".toByteArray()
            )
            return
        }

        block(device)
    }

    private val gattServerCallback: BluetoothGattServerCallback =
        object : BluetoothGattServerCallback() {
            override fun onConnectionStateChange(
                device: BluetoothDevice,
                status: Int,
                newState: Int
            ) {
                super.onConnectionStateChange(device, status, newState)
                val isSuccess = status == BluetoothGatt.GATT_SUCCESS
                val isConnected = newState == BluetoothProfile.STATE_CONNECTED
                val isDisconnected = newState == BluetoothProfile.STATE_DISCONNECTED
                Log.d {
                    "onConnectionStateChange: Server $device ${device.name} success: $isSuccess disconnect: $isDisconnected connected: $isConnected"
                }
                if (isSuccess && isConnected) {
                    when (newState) {
                        BluetoothProfile.STATE_DISCONNECTED -> {
                            mtu = 23
                            startAdvertisement()
                        }
                        /** The profile is in connecting state  */
                        BluetoothProfile.STATE_CONNECTING -> {
                            stopAdvertising()
                        }
                        /** The profile is in connected state  */
                        BluetoothProfile.STATE_CONNECTED -> {
                            stopAdvertising()
                        }
                        /** The profile is in disconnecting state  */
                        BluetoothProfile.STATE_DISCONNECTING -> {}
                    }
                    _state.value = HandlerState.Connected(device)
                    stopAdvertising()
                } else {
                    Log.e { "Error on connecting state change $device ${device.name} status:$status newState:$newState" }
                }
            }


            override fun onMtuChanged(device: BluetoothDevice?, mtu: Int) {
                super.onMtuChanged(device, mtu)
                Log.i { "MTU Changed to $mtu" }
                this@Handler.mtu = mtu
            }

            override fun onDescriptorReadRequest(
                device: BluetoothDevice?,
                requestId: Int,
                offset: Int,
                descriptor: BluetoothGattDescriptor?
            ) {
                super.onDescriptorReadRequest(device, requestId, offset, descriptor)

                Log.d { "New descriptor read request" }
            }


            override fun onDescriptorWriteRequest(
                device: BluetoothDevice?,
                requestId: Int,
                descriptor: BluetoothGattDescriptor?,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                value: ByteArray?
            ) = mtuGuard(device, requestId) { device ->
                super.onDescriptorWriteRequest(
                    device,
                    requestId,
                    descriptor,
                    preparedWrite,
                    responseNeeded,
                    offset,
                    value
                )


                Log.d { "New descriptor write request" }

                if (descriptor == null) {
                    Log.w { "Write request w/o descriptors are not supported" }
                    return
                }
                if (responseNeeded) {
                    Log.w { "Response requested but not supported" }
                    return
                }
                if (value == null) {
                    Log.w { "Write without value are not supported" }
                    return
                }

                if (descriptor.uuid == CCCD) {
                    if (BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE.contentEquals(value)) {
                        handleEnableListen(device, descriptor)
                    } else if (BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE.contentEquals(
                            value
                        )
                    ) {
                        handleDisableListen(descriptor.characteristic.uuid)
                    } else {
                        Log.w { "CCCD requests with un unknown behaviour submitted" }
                    }
                } else {
                    Log.w { "No other descriptor than CCCD is supported" }
                }
            }

            private val writeRegister = WriteRegister()

            override fun onCharacteristicWriteRequest(
                device: BluetoothDevice,
                requestId: Int,
                characteristic: BluetoothGattCharacteristic,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                value: ByteArray?
            ) = mtuGuard(device, requestId) { device ->
                super.onCharacteristicWriteRequest(
                    device,
                    requestId,
                    characteristic,
                    preparedWrite,
                    responseNeeded,
                    offset,
                    value
                )

                if (value == null) {
                    Log.w { "Characteristic writes without value are not supported" }
                    return
                }

                if (responseNeeded) {
                    Log.w { "Characteristic with responseNeeded are not supported" }
                    return
                }

                if (preparedWrite) {
                    Log.w { "Characteristic with preparedWrite are not supported" }
                }

                val potentiallyHasMore = value.size == mtu

                val toSendByteArray = if (offset == 0) {
                    if (potentiallyHasMore) {
                        // they potentially have more to send
                        // store it
                        writeRegister.add(device, characteristic, value)
                        return
                    } else {
                        value
                    }
                } else {
                    val store = if (potentiallyHasMore) {
                        writeRegister.get(device, characteristic)
                    } else {
                        writeRegister.remove(device, characteristic)
                    } ?: kotlin.run {
                        return Log.e { "Received a requestId with offset, but no initial request" }
                    }

                    val currentSize = store.sumOf { it.size }

                    if (currentSize != offset) {
                        return Log.e { "The given offset does not match the stored on" }
                    }

                    if (currentSize >= 32768) {
                        Log.e { "Maximum size is 32768" }
                        return disconnect()
                    }
                    if (potentiallyHasMore) {
                        store + value

                        return
                    } else {
                        store.reduce { a, b -> a + b }
                    }
                }


                scope.launch {
                    handleWrite(characteristic.uuid, toSendByteArray)
                }
            }

            val responseRegister = ReadRegister()

            // fixme add GATT error handler

            override fun onCharacteristicReadRequest(
                device: BluetoothDevice?,
                requestId: Int,
                offset: Int,
                characteristic: BluetoothGattCharacteristic?
            ) = mtuGuard(device, requestId) { device ->
                super.onCharacteristicReadRequest(device, requestId, offset, characteristic)

                if (characteristic == null) {
                    Log.w { "No characteristic reads are not supported" }
                    return
                }

                if (offset > 0) {
                    Log.d { "Request to continue ${device.name} $characteristic with offset $offset" }
                    responseRegister.get(device, characteristic)?.let { response ->
                        val to = min(offset + mtu, response.value.size)

                        Log.d { "Request to continue ${device.name} $characteristic with to $to because size ${response.value.size}" }

                        gattServer.sendResponse(
                            device,
                            requestId,
                            response.result,
                            offset,
                            response.value.sliceArray(offset until to)
                        )
                    } ?: kotlin.run {
                        Log.w { "Request to continue ${device.name} $characteristic with offset $offset was not found" }
                    }
                } else {
                    Log.d { "New request to read ${device.name} $characteristic" }

                    scope.launch {
                        val response = handleRead(characteristic.uuid)

                        if (response.size < mtu) {
                            gattServer.sendResponse(
                                device,
                                requestId,
                                BluetoothGatt.GATT_SUCCESS,
                                0,
                                response
                            )
                        } else {
                            responseRegister.add(
                                device,
                                characteristic,
                                BluetoothGatt.GATT_SUCCESS,
                                response
                            )
                        }
                    }
                }
            }
        }

    private val service =
        BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY).apply {
            addCharacteristics(this)
        }


    val gattServer: BluetoothGattServer = bluetoothManager.openGattServer(
        context,
        gattServerCallback
    ).apply {
        addService(service)
    }

    private fun <T : HandlerAction<*>> registerPerAction(
        mutableMap: MutableMap<UUID, T>,
        handlerAction: T
    ) {
        mutableMap[handlerAction.uuid] = handlerAction
        register(handlerAction)
    }

    protected fun <Request, Cmd : WriteCommand<Request>> write(
        command: Cmd,
        handler: suspend (Request) -> Unit
    ): WriteHandlerAction<Request, Cmd> = WriteHandlerActionImpl(command, handler).apply {
        registerPerAction(_writeActions, this)
    }

    protected fun <Response, Cmd : ReadCommand<Response>> read(
        command: Cmd,
        handler: suspend () -> Response
    ): ReadHandlerAction<Response, Cmd> = ReadHandlerActionImpl(command, handler).apply {
        registerPerAction(_readActions, this)
    }

    protected fun <Response, Cmd : ListenCommand<Response>> listen(
        command: Cmd,
        handler: suspend () -> Flow<Response>
    ): ListenHandlerAction<Response, Cmd> = ListenHandlerActionImpl(command, handler).apply {
        registerPerAction(_listenActions, this)
    }


    suspend fun handleWrite(uuid: UUID, value: ByteArray) {
        val handler = _writeActions[uuid] ?: return Log.e { "Handler $uuid not found" }

        handler.write(value)
    }

    suspend fun handleRead(uuid: UUID): ByteArray {
        val handler = _readActions[uuid] ?: return ByteArray(0).also {
            Log.e { "Handler $uuid not found" }
        }

        return handler.read()
    }

    private val listens = mutableMapOf<UUID, Job>()


    fun handleEnableListen(device: BluetoothDevice, descriptor: BluetoothGattDescriptor) {
        val handler =
            _listenActions[descriptor.uuid]
                ?: return Log.e { "Handler ${descriptor.uuid} not found" }

        val job = scope.launch {
            handler.listen().collect {
                descriptor.characteristic.value = it
                gattServer.notifyCharacteristicChanged(
                    device,
                    descriptor.characteristic,
                    false
                )
            }
        }
        listens[handler.uuid] = job
    }

    fun handleDisableListen(uuid: UUID) {
        listens[uuid]?.cancel()
    }

    fun disableAllListen() {
        listens.forEach {
            it.value.cancel()
        }
        listens.clear()
    }

    fun addCharacteristics(service: BluetoothGattService) {
        listOf(
            _writeActions,
            _readActions,
            _listenActions
        ).forEach { it.forEach { service.addCharacteristic(it.value.characteristic) } }
    }

    /**
     * Start advertising this device so other BLE devices can see it and connect
     */
    private fun startAdvertisement() {
        Log.d { "startAdvertisement: with advertiser $advertiser" }

        advertiser.startAdvertising(advertiseSettings, advertiseData, advertiseCallback)
    }


    /**
     * Stops BLE Advertising.
     */
    private fun stopAdvertising() {
        Log.d { "Stopping Advertising with advertiser $advertiser" }
        advertiser.stopAdvertising(advertiseCallback)
    }

    private fun disconnect() {
        TODO()
    }

    fun close() {
        scope.cancel()
    }

    /**
     * Custom callback after Advertising succeeds or fails to start. Broadcasts the error code
     * in an Intent to be picked up by AdvertiserFragment and stops this Service.
     */
    private val advertiseCallback by lazy {
        object : AdvertiseCallback() {
            override fun onStartFailure(errorCode: Int) {
                super.onStartFailure(errorCode)
                // Send error state to display
                val errorMessage = "Advertise failed with error: $errorCode"
                Log.e { errorMessage }

                _state.value = HandlerState.Erred(Throwable(errorMessage))
            }

            override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                super.onStartSuccess(settingsInEffect)
                Log.d { "Advertising successfully started" }
                _state.value = HandlerState.Advertising
            }
        }
    }
}

private operator fun AtomicInteger.setValue(handler: Handler, property: KProperty<*>, value: Int) =
    set(value)

private operator fun AtomicInteger.getValue(handler: Handler, property: KProperty<*>): Int = get()
