package io.almer.companionshared.server.commands.command

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

inline fun <reified T> deserialize(byteArray: ByteArray): T {
    val s = byteArray.decodeToString()
    return try {
        Json.decodeFromString<T>(s)
    } catch (e: Exception) {
        throw RuntimeException("Unable to decode string \"$s\"", e)
    }
}


inline fun <reified T> serialize(t: T) = Json.encodeToString(t).toByteArray()


val empty = ByteArray(0)