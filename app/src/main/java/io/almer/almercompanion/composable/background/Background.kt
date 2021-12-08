package io.almer.almercompanion.composable.background

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import io.almer.almercompanion.R

@Composable
fun Background(
    painter: Painter,
//    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painter, "Background",
            modifier = Modifier
                .align(Alignment.Center)
                .alpha(.5f)
        )
        content()
    }
}

@Composable
fun AlmerLogoBackground(
    content: @Composable BoxScope.() -> Unit
) {
    Background(painterResource(id = R.drawable.logo), content)
}

@Composable
@Preview
private fun BackgroundPreview() {
    Background(painter = painterResource(id = R.drawable.logo)) {
        Text("Almer")
    }
}