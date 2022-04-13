package io.almer.almercompanion

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavHostController
import io.almer.almercompanion.link.Link

val LocalNavHostController =
    staticCompositionLocalOf<NavHostController> { error("No navhost provided") }

val LocalLink =
    staticCompositionLocalOf<Link> { error("No Link provided") }
