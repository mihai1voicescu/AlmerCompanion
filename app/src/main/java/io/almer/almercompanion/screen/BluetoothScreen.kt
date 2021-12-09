package io.almer.almercompanion.screen

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import io.almer.almercompanion.composable.loaders.ViewLoader
import io.almer.almercompanion.composable.select.ListSelector
import io.almer.almercompanion.composable.text.BodyText
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
            delay(200)
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
        BodyText(it.name)
    }
}

val NavController.pathToBluetoothScreen get() = "bluetooth"
