package io.almer.almercompanion.screen.wifi

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.almer.almercompanion.R
import io.almer.almercompanion.composable.loaders.SubmitView
import io.almer.almercompanion.composable.select.itemSelector
import io.almer.almercompanion.composable.text.BodyText
import io.almer.companionshared.model.WiFi
import org.lighthousegames.logging.logging

val Log = logging("SelectWiFiListView")


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SelectWiFiListView(
    options: Collection<WiFi>,
    onKnownSelect: (wifi: WiFi) -> Unit,
    onUnknownSelect: (wifi: WiFi) -> Unit,
    onForgetSelect: (wifi: WiFi) -> Unit,
) {
    if (options.isEmpty()) {
        BodyText(text = "No available WiFis")
        return
    }

    Log.d { "Available Wifi: $options" }
    val known = options.filter { it.isKnow }
    val unknown = options.filter { !it.isKnow }
    Log.d { "Known Wifi: $known" }
    Log.d { "Unknown Wifi: $unknown" }

    SubmitView { toggle ->
        LazyColumn {
            stickyHeader {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(
                        "Known wifi",
                        Modifier
                            .padding(bottom = 6.dp, top = 2.dp)
                    )
                }
            }
//                    item { Divider() }
            known.map { wifi ->
                itemSelector(element = wifi, onSelect = {
                    onKnownSelect(wifi)
                }) {
                    Row {
                        BodyText(it.name, modifier = Modifier.weight(1f))
                        IconButton(onClick = { onForgetSelect(it) }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_baseline_remove_24),
                                contentDescription = "Remove"
                            )
                        }
                    }
                }
            }
            item { Divider(Modifier.padding(top = 20.dp), thickness = 3.dp) }
            stickyHeader {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(
                        "Other wifi",
                        Modifier
                            .padding(bottom = 6.dp, top = 2.dp)
                    )
                }
            }
            unknown.map { wifi ->
                itemSelector(element = wifi, onSelect = {
                    onUnknownSelect(wifi)
                }) {
                    BodyText(it.name)
                }
            }
        }
    }
}