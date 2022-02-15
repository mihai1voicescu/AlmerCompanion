package io.almer.commander

import android.content.SharedPreferences
import com.russhwolf.settings.*

private fun getNumberString(length: Int): String {
    val allowedChars = ('0'..'9')
    return (1..length)
        .map { allowedChars.random() }
        .joinToString("")
}

class AppSettings(preferences: SharedPreferences) {

    private val settings: AndroidSettings = AndroidSettings(preferences, true)

    init {
        if ("id" !in settings) {
            settings["id"] = getNumberString(3)
        }
    }

    val id by settings.string()

    var backendUrl: BackendUrl?
        get() = run {
            val settingStr = settings.getStringOrNull("backendUrl") ?: return null

            val tokens = settingStr.split("|")

            return BackendUrl(tokens[0], tokens[1].toBoolean())
        }
        private set(value: BackendUrl?) = settings.set(
            "backendUrl",
            "${value?.host}|${value?.secure}"
        )

    var isReady by settings.boolean()
}