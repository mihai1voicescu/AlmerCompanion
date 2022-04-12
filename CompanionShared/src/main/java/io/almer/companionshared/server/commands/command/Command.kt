package io.almer.companionshared.server.commands.command

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.util.*

interface CommandField

sealed class Command<T>(
) : CommandField {
    abstract val uuid: UUID
    abstract val kSerializer: KSerializer<T>

    fun decode(byteArray: ByteArray): T = Json.decodeFromString(kSerializer, byteArray.decodeToString())
    fun encode(t: T): ByteArray = Json.encodeToString(kSerializer, t).toByteArray()
}

abstract class ReadCommand<Response> : Command<Response>()

abstract class WriteCommand<Request> : Command<Request>()

abstract class ListenCommand<Request> : Command<Request>()


interface Action<T : Command<*>> : CommandField {
    val command: T
    val uuid: UUID get() = command.uuid
}

abstract class ActionImpl<T : Command<*>>(override val command: T) : Action<T>


interface CommandCatalog


abstract class ActionBase<T : Action<*>> {
    protected val uuids = mutableMapOf<UUID, T>()
    protected fun <U : T> register(action: U): U {
        if (uuids.containsKey(action.uuid)) {
            error("UUID ${action.uuid} was already registered for ${uuids[action.uuid]}")
        }

        uuids[action.uuid] = action

        return action
    }

    protected fun  <U : T> register(compute: () -> U): U = register(compute())
}
