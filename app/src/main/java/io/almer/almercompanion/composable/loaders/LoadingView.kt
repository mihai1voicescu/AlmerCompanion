package io.almer.almercompanion.composable.loaders

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import io.almer.almercompanion.R
import io.almer.almercompanion.composable.animations.PulsingImage

@Composable
@Preview
fun LoadingView(
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        PulsingImage(
            painter = painterResource(id = R.drawable.symbol),
            durationMillis = 2000,
            modifier = modifier.fillMaxSize()
        )
    }
}