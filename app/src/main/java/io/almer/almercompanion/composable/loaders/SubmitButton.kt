package io.almer.almercompanion.composable.loaders

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun SubmitButtonContent(
    isLoading: Boolean,
    content: @Composable () -> Unit
) {
    Box(contentAlignment = Alignment.Center) {
        content()
        if (isLoading) {
            CircularProgressIndicator(
                color = Color.Gray,
                modifier = Modifier
                    .matchParentSize()
                    .scale(0.5f)
            )
//            CircularProgressIndicator(
//                color = Color.Red,
//                modifier = Modifier
//                    .matchParentSize()
//            )
        }
    }
}

@Composable
fun SubmitButtonContent(
    progress: Float?,
    content: @Composable () -> Unit
) {
    Box(contentAlignment = Alignment.Center) {
        content()
        if (progress != null) {
            CircularProgressIndicator(
                progress,
                color = Color.Gray,
                modifier = Modifier
                    .matchParentSize()
                    .scale(0.5f)
            )
//            CircularProgressIndicator(
//                color = Color.Red,
//                modifier = Modifier
//                    .matchParentSize()
//            )
        }
    }

}

@Composable
@Preview
private fun SubmitButtonContentPreview() {
    Button(onClick = { /*TODO*/ }) {
        SubmitButtonContent(progress = 0.5f) {
            Text("Mdea")
        }
    }
}


@Composable
@Preview
private fun SubmitButtonContentDynamicPreview() {
    Button(onClick = { /*TODO*/ }) {
        SubmitButtonContent(true) {
            Text("Mdea")
        }
    }
}
