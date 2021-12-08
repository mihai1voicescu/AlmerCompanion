package io.almer.almercompanion.composable.select

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun <T> ListSelector(
    items: Collection<T>,
    onSelect: (item: T) -> Unit,
    inflater: @Composable (item: T) -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize(),
//        verticalArrangement = Arrangement.SpaceEvenly,
        content = {
            items.forEach {
                item {
                    Surface(
                        onClick = {
                            onSelect(it)
                        },
                        modifier = Modifier
                            .padding(horizontal = 10.dp, vertical = 2.dp)
                            .fillMaxWidth(),
                        role = Role.Button,

                        ) {
                        inflater(it)
                    }
                }
            }
        })
}

@Composable
@Preview
fun ListSelectorPreview() {
    ListSelector(items = listOf("Ana", "are", "mere"), onSelect = {}) {
        Text("Item")
        Text(it)
    }
}