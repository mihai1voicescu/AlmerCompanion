package io.almer.companionshared.model

data class WiFi(
    val name: String,
    val ssid: String,
    val strength: Int,
    val networkId: Int? = null,
) {
    val isKnow get() = networkId != null
}