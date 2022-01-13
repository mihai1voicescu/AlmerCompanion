package io.almer.companionshared.model

import kotlinx.serialization.Serializable

@Serializable
data class WifiConnectionInfo(val password: String, val wifi: WiFi)