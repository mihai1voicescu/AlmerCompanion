package io.almer.almercompanion.composable.layouts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun Aligned(
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable() () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = contentAlignment) {
        content()
    }
}

@Composable
@Preview
private fun CenteredPreview() {
    Aligned {
        Text("Almer")
    }
}