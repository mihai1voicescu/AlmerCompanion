package io.almer.commander

data class BackendUrl(
    val host: String,
    val secure: Boolean
) {
    fun rSocketUrl(): String {
        val protocol = if (secure) "wss" else "ws"
        return "$protocol://$host/rSocket"
    }

    fun callUrl(id: String): String {
        val protocol = if (secure) "https" else "http"

        return "$protocol://$host/?callId=$id"
    }
}