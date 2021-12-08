package io.almer.almercompanion

import android.content.Context
import android.content.Context.MODE_PRIVATE
import com.russhwolf.settings.AndroidSettings
import com.russhwolf.settings.boolean

class AppSettings(val context: Context) {
    private val delegate = context.getSharedPreferences("settings", MODE_PRIVATE)!!
    val settings = AndroidSettings(delegate)

    val isBoardingDone by settings.boolean(defaultValue = false)

//    val someClass: SomeClass by settings.serializedValue(SomeClass.serializer(), "someClass", defaultValue)
}