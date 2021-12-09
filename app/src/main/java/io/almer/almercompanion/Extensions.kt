package io.almer.almercompanion

import androidx.navigation.NavHostController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun NavHostController.safePopBackStack() {
    withContext(Dispatchers.Main) {
        this@safePopBackStack.popBackStack()
    }
}