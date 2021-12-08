package io.almer.almercompanion.screen.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource

sealed class MainScreenType(
    val iconId: Int,
    val nameId: Int
) {
    @Composable
    abstract fun Screen(): Unit

    companion object {
        val StartScreen = Info as MainScreenType

        val screens = arrayOf(
            Home, Info
        )
    }
}

@Composable
inline fun MainScreenType.icon() = painterResource(iconId)

@Composable
inline fun MainScreenType.name() = stringResource(nameId)