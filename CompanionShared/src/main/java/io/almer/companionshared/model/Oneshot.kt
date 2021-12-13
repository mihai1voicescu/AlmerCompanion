package io.almer.companionshared.model

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlin.experimental.ExperimentalTypeInference

@OptIn(ExperimentalCoroutinesApi::class)
class OneshotScope<T> constructor(val producerScope: ProducerScope<T>) {
    fun send(t: T) {
        producerScope.trySend(t)
        producerScope.close()
    }
}

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTypeInference::class)
suspend fun <T> oneshot(
    @BuilderInference block: suspend OneshotScope<T>.() -> Unit
): T {
    return callbackFlow<T> {
        OneshotScope(this).apply {
            block()
        }

        awaitClose()
    }.buffer(1).first()
}