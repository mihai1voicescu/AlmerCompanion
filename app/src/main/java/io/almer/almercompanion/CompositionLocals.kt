package io.almer.almercompanion

import androidx.compose.runtime.compositionLocalOf
import androidx.navigation.NavHostController

val LocalNavHostController = compositionLocalOf<NavHostController> { error("No navhost provided") }