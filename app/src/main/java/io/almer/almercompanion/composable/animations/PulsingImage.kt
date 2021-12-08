package io.almer.almercompanion.composable.animations

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
fun PulsingImage(
    painter: Painter,
    modifier: Modifier = Modifier,
    durationMillis: Int = 2000
) {

    val infiniteTransition = rememberInfiniteTransition()
//    val angle by infiniteTransition.animateFloat(
//        initialValue = 0F,
//        targetValue = 360F,
//        animationSpec = infiniteRepeatable(
//            animation = tween(durationMillis, easing = LinearEasing)
//        )
//    )

    val deviation by infiniteTransition.animateFloat(
        initialValue = -.5f,
        targetValue = .5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing)
        )
    )

    val size = .5f + abs(deviation)

    Image(
        painter = painter,
        contentDescription = null,
        modifier = modifier
            .padding(2.dp)
            .graphicsLayer(
//                rotationZ = angle,
                scaleX = size,
                scaleY = size,
            )
    )
}