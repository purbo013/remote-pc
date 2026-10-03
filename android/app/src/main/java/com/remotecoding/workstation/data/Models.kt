package com.remotecoding.workstation.data

data class ProjectItem(
    val name: String,
    val url: String,
)

enum class ConnectionStatus {
    Offline,
    Connecting,
    PairingRequired,
    Connected,
    Reconnecting,
}

data class SessionState(
    val status: ConnectionStatus = ConnectionStatus.Offline,
    val message: String = "",
    val retry: Int = 0,
    val remoteControl: Boolean = true,
    val frameJpeg: ByteArray? = null,
    val frameWidth: Int = 0,
    val frameHeight: Int = 0,
)
