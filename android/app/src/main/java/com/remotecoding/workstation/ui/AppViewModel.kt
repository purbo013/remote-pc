package com.remotecoding.workstation.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.remotecoding.workstation.data.AppPrefs
import com.remotecoding.workstation.data.ConnectionMode
import com.remotecoding.workstation.data.ConnectionStatus
import com.remotecoding.workstation.data.ConnectionTarget
import com.remotecoding.workstation.data.PreferencesRepository
import com.remotecoding.workstation.data.ProjectItem
import com.remotecoding.workstation.data.RemoteApi
import com.remotecoding.workstation.data.RemoteSession
import com.remotecoding.workstation.data.SessionState
import java.net.ConnectException
import java.net.SocketTimeoutException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val prefsRepo = PreferencesRepository(application)
    private val api = RemoteApi()
    val session = RemoteSession(viewModelScope)

    val prefs: StateFlow<AppPrefs> = prefsRepo.prefs.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        AppPrefs(),
    )

    val sessionState: StateFlow<SessionState> = session.state
    val projects = MutableStateFlow<List<ProjectItem>>(emptyList())
    val notice = MutableStateFlow("")

    fun saveConnection(
        mode: ConnectionMode,
        host: String,
        port: Int,
        relayUrl: String,
        nodeId: String,
        deviceName: String,
    ) {
        viewModelScope.launch {
            prefsRepo.update(
                connectionMode = mode,
                host = host.trim(),
                port = port,
                relayUrl = relayUrl.trim(),
                nodeId = nodeId.trim(),
                deviceName = deviceName.trim(),
            )
        }
    }

    fun saveSettings(
        mode: ConnectionMode,
        host: String,
        port: Int,
        relayUrl: String,
        nodeId: String,
        deviceName: String,
        autoReconnect: Boolean,
        landscapeRemote: Boolean,
        screenQuality: Int,
    ) {
        viewModelScope.launch {
            prefsRepo.update(
                connectionMode = mode,
                host = host.trim(),
                port = port,
                relayUrl = relayUrl.trim(),
                nodeId = nodeId.trim(),
                deviceName = deviceName.trim(),
                autoReconnect = autoReconnect,
                landscapeRemote = landscapeRemote,
                screenQuality = screenQuality,
            )
            val target = ConnectionTarget(mode, host.trim(), port, relayUrl.trim(), nodeId.trim())
            session.configure(target, prefs.value.token, autoReconnect)
        }
    }

    fun connect() {
        viewModelScope.launch {
            val p = prefs.value
            val target = ConnectionTarget.from(p)
            if (!target.isConfigured()) {
                notice.value = when (target.mode) {
                    ConnectionMode.LAN -> "Enter the PC IP address first."
                    ConnectionMode.INTERNET -> "Enter Relay URL and Node ID from the PC console."
                }
                return@launch
            }
            session.configure(target, p.token, p.autoReconnect)
            val online = withContext(Dispatchers.IO) {
                runCatching { api.health(target) }.getOrElse { false }
            }
            if (!online) {
                notice.value = api.connectHint(target, hadTimeout = true, hadRefused = false)
                return@launch
            }
            if (p.token.isBlank()) {
                notice.value = "Pairing is required. Enter the 6-digit code from the PC console."
                return@launch
            }
            session.connect()
            refreshProjects()
        }
    }

    fun disconnect() {
        session.disconnect()
    }

    fun pair(code: String) {
        viewModelScope.launch {
            val p = prefs.value
            val target = ConnectionTarget.from(p)
            try {
                val token = withContext(Dispatchers.IO) {
                    api.pair(target, code, p.deviceName)
                }
                prefsRepo.update(token = token)
                session.configure(target, token, p.autoReconnect)
                session.connect()
                notice.value = "Paired and connected."
                refreshProjects()
            } catch (error: Exception) {
                val hint = when (error) {
                    is SocketTimeoutException -> api.connectHint(target, hadTimeout = true, hadRefused = false)
                    is ConnectException -> api.connectHint(target, hadTimeout = false, hadRefused = true)
                    else -> error.message ?: "Pairing failed"
                }
                notice.value = hint
            }
        }
    }

    fun refreshProjects() {
        viewModelScope.launch {
            val p = prefs.value
            if (p.token.isBlank()) return@launch
            val target = ConnectionTarget.from(p)
            val remote = withContext(Dispatchers.IO) {
                runCatching { api.projects(target, p.token) }.getOrElse { emptyList() }
            }
            val extras = p.extraProjects.lines()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .map { line ->
                    val parts = line.split("|", limit = 2)
                    if (parts.size == 2) ProjectItem(parts[0], parts[1]) else ProjectItem(line, line)
                }
            projects.value = remote + extras
        }
    }

    fun addProject(name: String, url: String) {
        viewModelScope.launch {
            val next = (prefs.value.extraProjects.lines() + "$name|$url")
                .filter { it.isNotBlank() }
                .distinct()
                .joinToString("\n")
            prefsRepo.update(extraProjects = next)
            refreshProjects()
        }
    }

    fun clearNotice() {
        notice.value = ""
    }

    fun showNotice(message: String) {
        notice.value = message
    }

    fun isConnected(): Boolean = sessionState.value.status == ConnectionStatus.Connected

    fun localhostUrl(): String? = ConnectionTarget.from(prefs.value).lanBrowserRoot()
}
