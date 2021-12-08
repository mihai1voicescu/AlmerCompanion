package io.almer.almercompanion.screen.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.almer.almercompanion.R

object Info : MainScreenType(R.drawable.ic_round_info_24, R.string.navigation_item_info) {
    @Composable
    override fun Screen() {
        LazyColumn {
            item {
                InfoItem(
                    name = "Battery",
                    icon = painterResource(id = R.drawable.ic_round_battery_std_24),
                    value = "98%"
                )
            }
        }
    }
}

@Composable
fun InfoItem(
    name: String,
    icon: Painter,
    value: String
) {
    Surface(Modifier.padding(10.dp, 2.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = icon,
                contentDescription = name,
                modifier = Modifier
                    .size(40.dp)
            )
            Text(
                text = name,
                style = MaterialTheme.typography.button,
                modifier = Modifier
                    .padding(16.dp)
                    .weight(1f)
            )

            Text(
                text = value,
                textAlign = TextAlign.Right,
                style = MaterialTheme.typography.button,
                modifier = Modifier
                    .padding(16.dp)
            )
        }
    }
}

@Composable
@Preview
private fun InfoItemPreview() {
    InfoItem(
        name = "Battery",
        icon = painterResource(id = R.drawable.ic_round_battery_std_24),
        value = "98%"
    )
}

@Composable
@Preview
private fun ScreenPreview() {
    Info.Screen()
}