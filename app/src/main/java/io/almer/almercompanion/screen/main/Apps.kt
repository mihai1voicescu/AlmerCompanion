package io.almer.almercompanion.screen.main

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import io.almer.almercompanion.R

object Apps : MainScreenType(R.drawable.ic_baseline_apps_24, R.string.navigation_item_apps) {
    @Composable
    override fun Screen() {
        Text("Remote")
    }
}
