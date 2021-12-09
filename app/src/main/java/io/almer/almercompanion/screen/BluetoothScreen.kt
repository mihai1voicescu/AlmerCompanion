package io.almer.almercompanion.screen

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import io.almer.almercompanion.LocalNavHostController
import io.almer.almercompanion.MainApp.Companion.mainApp
import io.almer.almercompanion.composable.loaders.SubmitView
import io.almer.almercompanion.composable.loaders.ViewLoader
import io.almer.almercompanion.composable.select.ListSelector
import io.almer.almercompanion.composable.text.BodyText
import io.almer.companionshared.model.BluetoothDevice
import io.almer.almercompanion.safePopBackStack
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun BluetoothScreen() {
    val app = mainApp()
    val navController = LocalNavHostController.current

    ViewLoader(
        stateLoader = {
            delay(200)
            app.link.listBluetooth()
        }
    ) {
        SubmitView { toggle ->
            SelectBluethootListView(options = it, onSelect = {
                toggle()
                GlobalScope.launch {
                    app.link.selectWiFi(it.uuid)
                    toggle()
                    navController.safePopBackStack()
                }
            })
        }
    }
}

@Composable
private fun SelectBluethootListView(
    options: Collection<io.almer.companionshared.model.BluetoothDevice>,
    onSelect: (device: io.almer.companionshared.model.BluetoothDevice) -> Unit
) {
    ListSelector(items = options, onSelect = onSelect) {
        BodyText(it.name)
    }
}

val NavController.pathToBluetoothScreen get() = "bluetooth"
