package io.almer.almercompanion.composable.loaders

import androidx.compose.runtime.*

@Composable
fun <State> ViewLoader(
    stateLoader: suspend () -> State,
    content: @Composable (state: State) -> Unit
) {
    val state by produceState<State?>(initialValue = null, producer = {
        value = stateLoader()
    })

    state?.let {
        content(it)
    } ?: LoadingView()
}

