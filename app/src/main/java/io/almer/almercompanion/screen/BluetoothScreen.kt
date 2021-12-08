package io.almer.almercompanion.screen

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import io.almer.almercompanion.composable.loaders.ViewLoader
import io.almer.almercompanion.composable.select.ListSelector
import kotlinx.coroutines.delay

data class BluetoothDevice(
    val name: String,
    val isPaired: Boolean
)

private val mockData = listOf(
    BluetoothDevice("Sony Headphones", true),
    BluetoothDevice("Bose Headphones", false),
)

@Composable
fun BluetoothScreen() {
    ViewLoader(
        stateLoader = {
            delay(2000)
            mockData
        }
    ) {
        SelectBluethootListView(options = it, onSelect = {})
    }
}

@Composable
private fun SelectBluethootListView(
    options: Collection<BluetoothDevice>,
    onSelect: (device: BluetoothDevice) -> Unit
) {
    ListSelector(items = options, onSelect = onSelect) {
        Text(it.name)
    }
}

val NavController.pathToBluetoothScreen get() = "bluetooth"
