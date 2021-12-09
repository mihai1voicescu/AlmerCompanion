package io.almer.almercompanion.link.model

data class WiFi(
    val name: String,
    val ssid: String,
    val strength: Int,
    val isKnown: Boolean,
)