package io.almer.companionshared.model

import android.net.wifi.WifiInfo
import android.os.Build
import kotlinx.serialization.Serializable

@Serializable
data class WiFi(
    val name: String,
    val ssid: String,
    val strength: Int,
    val networkId: Int? = null,
) {
    val isKnow get() = networkId != null
}

fun WifiInfo.toWiFI(): WiFi {
    val name: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        this.passpointProviderFriendlyName ?: this.ssid
    } else {
        // todo Get a name somehow
        this.ssid
    }

    return WiFi(
        name = name,
        ssid = this.ssid,
        strength = this.linkSpeed, // todo not OK
    )
}