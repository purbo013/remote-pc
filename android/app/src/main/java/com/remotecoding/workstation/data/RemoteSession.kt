package com.remotecoding.workstation.data

import android.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class RemoteSession(
    private val scope: CoroutineScope,
) {
    private val client = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .build()

    private val _state = MutableStateFlow(SessionState())
    val state: StateFlow<SessionState> = _state

    private var socket: WebSocket? = null
    private var reconnectJob: Job? = null
    private val manualStop = AtomicBoolean(false)
    private var host = ""
    private var port = 8765
    private var token = ""
    private var autoReconnect = true

    fun configure(host: String, port: Int, token: String, autoReconnect: Boolean) {
        this.host = host
        this.port = port
        this.token = token
        this.autoReconnect = autoReconnect
    }

    fun connect() {
        if (host.isBlank() || token.isBlank()) {
            _state.update {
                it.copy(status = ConnectionStatus.PairingRequired, message = "Pairing is required before connecting.")
            }
            return
        }
        manualStop.set(false)
        reconnectJob?.cancel()
        openSocket()
    }

    fun disconnect() {
        manualStop.set(true)
        reconnectJob?.cancel()
        socket?.close(1000, "bye")
        socket = null
        _state.update { SessionState(status = ConnectionStatus.Offline, message = "Disconnected") }
    }

    fun sendJson(builder: JSONObject.() -> Unit) {
        val obj = JSONObject().apply(builder)
        socket?.send(obj.toString())
    }

    fun mouseMove(x: Float, y: Float) {
        sendJson {
            put("type", "mouse_move")
            put("x", x.toDouble())
            put("y", y.toDouble())
        }
    }

    fun mouseClick(button: String = "left", double: Boolean = false) {
        sendJson {
            put("type", "mouse_click")
            put("button", button)
            put("double", double)
        }
    }

    fun mouseDown(button: String = "left") {
        sendJson {
            put("type", "mouse_down")
            put("button", button)
        }
    }

    fun mouseUp(button: String = "left") {
        sendJson {
            put("type", "mouse_up")
            put("button", button)
        }
    }

    fun scroll(deltaY: Float, deltaX: Float = 0f) {
        if (deltaY == 0f && deltaX == 0f) return
        sendJson {
            put("type", "scroll")
            if (deltaY != 0f) put("delta", deltaY.toDouble())
            if (deltaX != 0f) put("deltaX", deltaX.toDouble())
        }
    }

    fun key(name: String) {
        sendJson {
            put("type", "key")
            put("key", name)
        }
    }

    fun keyCombo(keys: List<String>) {
        sendJson {
            put("type", "key_combo")
            put("keys", JSONArray(keys))
        }
    }

    fun textInput(text: String) {
        sendJson {
            put("type", "text_input")
            put("text", text)
        }
    }

    private fun openSocket() {
        socket?.cancel()
        _state.update {
            it.copy(
                status = if (it.retry > 0) ConnectionStatus.Reconnecting else ConnectionStatus.Connecting,
                message = if (it.retry > 0) "Connection lost\nRetry: ${it.retry}" else "Connecting…",
            )
        }
        val request = Request.Builder().url("ws://$host:$port/ws").build()
        socket = client.newWebSocket(request, listener)
    }

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            webSocket.send(JSONObject().put("type", "auth").put("token", token).toString())
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            val json = runCatching { JSONObject(text) }.getOrNull() ?: return
            when (json.optString("type")) {
                "hello" -> {
                    _state.update {
                        it.copy(
                            status = ConnectionStatus.Connected,
                            message = "Connected",
                            retry = 0,
                            remoteControl = json.optBoolean("remoteControl", true),
                        )
                    }
                }
                "screen_frame" -> {
                    val data = json.optString("data")
                    val bytes = runCatching { Base64.decode(data, Base64.DEFAULT) }.getOrNull()
                    _state.update {
                        it.copy(
                            frameJpeg = bytes,
                            frameWidth = json.optInt("width"),
                            frameHeight = json.optInt("height"),
                        )
                    }
                }
                "error" -> {
                    val msg = json.optString("message")
                    if (msg.contains("Unauthorized", ignoreCase = true)) {
                        _state.update { it.copy(status = ConnectionStatus.PairingRequired, message = msg) }
                    }
                }
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            scheduleReconnect(t.message)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            if (code == 4003) {
                _state.update {
                    it.copy(status = ConnectionStatus.PairingRequired, message = "Invalid token. Pair this phone again.")
                }
                return
            }
            if (!manualStop.get()) scheduleReconnect(reason)
        }
    }

    private fun scheduleReconnect(reason: String?) {
        if (manualStop.get() || !autoReconnect) {
            _state.update {
                it.copy(
                    status = ConnectionStatus.Offline,
                    message = reason ?: "Disconnected",
                )
            }
            return
        }
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            val next = _state.value.retry + 1
            _state.update {
                it.copy(
                    status = ConnectionStatus.Reconnecting,
                    retry = next,
                    message = "Connection lost\nRetry: $next",
                )
            }
            delay((1000L * next).coerceAtMost(8000L))
            if (isActive && !manualStop.get()) openSocket()
        }
    }
}
