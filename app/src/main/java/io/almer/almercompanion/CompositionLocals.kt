package io.almer.almercompanion

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavHostController

val LocalNavHostController =
    staticCompositionLocalOf<NavHostController> { error("No navhost provided") }
