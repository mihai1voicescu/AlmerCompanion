package io.almer.almercompanion.composable.select

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun <T> ListSelector(
    items: Collection<T>,
    onSelect: (item: T) -> Unit,
    inflater: @Composable (item: T) -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        content = {
            items.forEach {
                itemSelector(
                    it,
                    onSelect,
                    inflater
                )
            }
        })
}

@OptIn(ExperimentalMaterialApi::class)
fun <T> LazyListScope.itemSelector(
    element: T,
    onSelect: (item: T) -> Unit,
    inflater: @Composable (item: T) -> Unit
) {
    item {

        Card(
            onClick = {
                onSelect(element)
            },
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 2.dp)
                .fillMaxWidth(),
            role = Role.Button,

            border = BorderStroke(2.dp, Color.Gray),
            elevation = 8.dp,
            shape = MaterialTheme.shapes.medium,

            ) {
            Box(
                Modifier
                    .padding(10.dp, 2.dp)
                    .defaultMinSize(minHeight = 40.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                inflater(element)
            }
        }
    }
}

@Composable
@Preview
private fun ListSelectorPreview() {
    ListSelector(items = listOf("Ana", "are", "mere"), onSelect = {}) {
        Card {
            Text(it, style = MaterialTheme.typography.body1)
        }
    }
}