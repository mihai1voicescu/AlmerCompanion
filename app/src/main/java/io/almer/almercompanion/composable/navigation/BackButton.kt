package io.almer.almercompanion.composable.navigation

import androidx.compose.material.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import io.almer.almercompanion.LocalNavHostController
import io.almer.almercompanion.R
import io.almer.almercompanion.safePopBackStack

@Composable
@Preview
fun BackButton() {
    val nav = LocalNavHostController.current
    IconButton(onClick = {
        nav.popBackStack()
    }) {
        Icon(painter = painterResource(id = R.drawable.ic_baseline_arrow_back_ios_24), contentDescription = "Back")
    }
}