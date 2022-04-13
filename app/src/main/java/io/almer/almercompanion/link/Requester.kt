package io.almer.almercompanion.link

import com.juul.kable.Peripheral
import com.juul.kable.characteristicOf
import io.almer.almercompanion.screen.wifi.Log
import io.almer.companionshared.server.SERVICE_UUID
import io.almer.companionshared.server.commands.command.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch

interface RequesterAction<T : Command<*>> : Action<T>

abstract class RequesterActionImpl<Cmd : Command<*>>(
    command: Cmd,
    protected val peripheral: Peripheral
) : ActionImpl<Cmd>(command) {
    val characteristic = characteristicOf(SERVICE_UUID.toString(), command.uuid.toString())
}


sealed interface WriteRequesterAction<Request, Cmd : WriteCommand<Request>> : RequesterAction<Cmd> {
    suspend fun write(request: Request)
}

suspend inline operator fun <Request, Cmd : WriteCommand<Request>> WriteRequesterAction<Request, Cmd>.invoke(
    request: Request
) = write(request)

private class WriteRequesterActionImpl<Request, Cmd : WriteCommand<Request>>(
    command: Cmd,
    peripheral: Peripheral,
) : WriteRequesterAction<Request, Cmd>, RequesterActionImpl<Cmd>(command, peripheral) {

    override suspend fun write(request: Request) {
        val data = command.encode(request)
        peripheral.write(characteristic, data)
    }
}

sealed interface ReadRequesterAction<Response, Cmd : ReadCommand<Response>> : RequesterAction<Cmd> {
    suspend fun read(): Response
}

suspend inline operator fun <Response, Cmd : ReadCommand<Response>> ReadRequesterAction<Response, Cmd>.invoke() =
    read()

private class ReadRequesterActionImpl<Response, Cmd : ReadCommand<Response>>(
    command: Cmd,
    peripheral: Peripheral,

    ) : ReadRequesterAction<Response, Cmd>, RequesterActionImpl<Cmd>(command, peripheral) {
    override suspend fun read(): Response {
        val data = peripheral.read(characteristic)

        return command.decode(data)
    }
}

sealed interface ListenRequesterAction<Response, Cmd : ListenCommand<Response>> :
    RequesterAction<Cmd> {
    fun listen(): Flow<Response>
}

inline operator fun <Response, Cmd : ListenCommand<Response>> ListenRequesterAction<Response, Cmd>.invoke() =
    listen()

private class ListenRequesterActionImpl<Response, Cmd : ListenCommand<Response>>(
    command: Cmd,
    peripheral: Peripheral,
) : ListenRequesterAction<Response, Cmd>, RequesterActionImpl<Cmd>(command, peripheral) {


    override fun listen(): Flow<Response> {
        return peripheral.observe(characteristic)
            .map { command.decode(it) }
            .onCompletion {
                GlobalScope.launch {
                    // todo check
                    val discoveredCharacteristic =
                        try {
                            peripheral.services!!
                                .first { it.serviceUuid == characteristic.serviceUuid }
                                .characteristics
                                .first { it.characteristicUuid == characteristic.characteristicUuid }
                        } catch (e: Throwable) {
                            Log.e(e) {
                                "Unable to find the ${characteristic.serviceUuid} in ${
                                    peripheral.services!!
                                        .first { it.serviceUuid == characteristic.serviceUuid }
                                        .characteristics
                                }"
                            }

                            throw e
                        }

                    peripheral.disableListen(discoveredCharacteristic)
                }
            }
    }
}

abstract class Requester(
    private val peripheral: Peripheral
) : ActionBase<RequesterAction<*>>() {
    protected fun <Request, Cmd : WriteCommand<Request>> write(
        command: Cmd
    ): WriteRequesterAction<Request, Cmd> = register(WriteRequesterActionImpl(command, peripheral))

    protected fun <Response, Cmd : ReadCommand<Response>> read(
        command: Cmd
    ): ReadRequesterAction<Response, Cmd> = register(ReadRequesterActionImpl(command, peripheral))

    protected fun <Response, Cmd : ListenCommand<Response>> listen(
        command: Cmd
    ): ListenRequesterAction<Response, Cmd> =
        register(ListenRequesterActionImpl(command, peripheral))
}