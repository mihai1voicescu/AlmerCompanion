package io.almer.almercompanion.screen.main

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import io.almer.almercompanion.R

object Home : MainScreenType(R.drawable.ic_round_home_24, R.string.navigation_item_home) {
    @Composable
    override fun Screen() {
        Text("Home")
    }
}
