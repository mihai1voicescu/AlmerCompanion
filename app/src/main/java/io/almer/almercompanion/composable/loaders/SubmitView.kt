package io.almer.almercompanion.composable.loaders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.min

val DefaultSubmittingView: @Composable BoxWithConstraintsScope.() -> Unit = {
    val shortest = min(constraints.maxHeight, constraints.maxWidth)
    val strokeWidth = (shortest / 30).dp
    CircularProgressIndicator(
        Modifier.fillMaxSize(),
        strokeWidth = strokeWidth
    )
}

@Composable
fun SubmitView(
    submittingView: (@Composable BoxWithConstraintsScope.() -> Unit)? = null,
    content: @Composable (
        markSubmitting: () -> Unit,
        markComplete: () -> Unit
    ) -> Unit
) {
    var isSubmitting by remember {
        mutableStateOf(false)
    }

    val markSubmitting = {
        isSubmitting = true
    }

    val markComplete = {
        isSubmitting = false
    }

    SubmitView(isSubmitting, submittingView) {
        content(markSubmitting, markComplete)
    }
}


@Composable
fun SubmitView(
    submittingView: (@Composable BoxWithConstraintsScope.() -> Unit)? = null,
    content: @Composable (
        toggle: () -> Unit,
    ) -> Unit
) {
    var isSubmitting by remember {
        mutableStateOf(false)
    }

    val toggle = {
        isSubmitting = !isSubmitting
    }

    SubmitView(isSubmitting, submittingView) {
        content(toggle)
    }
}

@Composable
fun SubmitView(
    isSubmitting: Boolean,
    submittingView: (@Composable BoxWithConstraintsScope.() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        content()
        if (isSubmitting) {
            Box(
                Modifier
                    .fillMaxSize()
                    .alpha(0.5f)
                    .background(Color.Gray)
                    .clickable { }
            )

            BoxWithConstraints(Modifier.fillMaxSize()) {
                submittingView?.let { submittingView() } ?: DefaultSubmittingView()
            }
        }
    }
}

@Composable
@Preview
private fun SubmitViewPreview() {
    SubmitView(true) {
        Button(onClick = { }) {
            Text("Toogle")
        }
    }
}

@Composable
@Preview
private fun SubmitViewPreviewSmall() {
    Box(Modifier.size(100.dp)) {
        SubmitView(true) {
            Button(onClick = { }) {
                Text("Toogle")
            }
        }
    }
}