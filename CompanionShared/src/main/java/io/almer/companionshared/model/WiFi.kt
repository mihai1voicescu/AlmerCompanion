package io.almer.companionshared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("wifi")
data class WiFi(
    val name: String,
    val ssid: String,
    val strength: Int,
    val networkId: Int? = null,
) {
    val isKnow get() = networkId != null
}