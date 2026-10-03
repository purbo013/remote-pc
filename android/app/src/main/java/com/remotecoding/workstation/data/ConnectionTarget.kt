package com.remotecoding.workstation.data

enum class ConnectionMode {
    LAN,
    INTERNET,
}

data class ConnectionTarget(
    val mode: ConnectionMode,
    val host: String,
    val port: Int,
    val relayUrl: String,
    val nodeId: String,
) {
    fun isConfigured(): Boolean = when (mode) {
        ConnectionMode.LAN -> host.isNotBlank()
        ConnectionMode.INTERNET -> relayUrl.isNotBlank() && nodeId.isNotBlank()
    }

    fun httpBase(): String = when (mode) {
        ConnectionMode.LAN -> "http://${host.trim()}:$port"
        ConnectionMode.INTERNET -> {
            val base = relayUrl.trim().removeSuffix("/")
            val id = nodeId.trim()
            "$base/n/$id"
        }
    }

    fun wsUrl(): String = when (mode) {
        ConnectionMode.LAN -> "ws://${host.trim()}:$port/ws"
        ConnectionMode.INTERNET -> {
            val base = relayUrl.trim().removeSuffix("/")
            val ws = when {
                base.startsWith("https://", ignoreCase = true) ->
                    "wss://${base.substringAfter("://")}"
                base.startsWith("http://", ignoreCase = true) ->
                    "ws://${base.substringAfter("://")}"
                else -> "wss://$base"
            }
            "$ws/n/${nodeId.trim()}/ws"
        }
    }

    fun lanBrowserRoot(): String? = when (mode) {
        ConnectionMode.LAN -> "http://${host.trim()}/"
        ConnectionMode.INTERNET -> null
    }

    companion object {
        fun from(prefs: AppPrefs): ConnectionTarget = ConnectionTarget(
            mode = prefs.connectionMode,
            host = prefs.host,
            port = prefs.port,
            relayUrl = prefs.relayUrl,
            nodeId = prefs.nodeId,
        )
    }
}
