package com.remotecoding.workstation.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("remote_prefs")

data class AppPrefs(
    val host: String = "",
    val port: Int = 8765,
    val deviceName: String = "Android Phone",
    val token: String = "",
    val autoReconnect: Boolean = true,
    val landscapeRemote: Boolean = true,
    val screenQuality: Int = 70,
    val extraProjects: String = "",
    val connectionMode: ConnectionMode = ConnectionMode.LAN,
    val relayUrl: String = "",
    val nodeId: String = "",
)

class PreferencesRepository(private val context: Context) {
    private val hostKey = stringPreferencesKey("host")
    private val portKey = intPreferencesKey("port")
    private val nameKey = stringPreferencesKey("device_name")
    private val tokenKey = stringPreferencesKey("token")
    private val reconnectKey = booleanPreferencesKey("auto_reconnect")
    private val landscapeKey = booleanPreferencesKey("landscape_remote")
    private val qualityKey = intPreferencesKey("screen_quality")
    private val extraKey = stringPreferencesKey("extra_projects")
    private val modeKey = stringPreferencesKey("connection_mode")
    private val relayUrlKey = stringPreferencesKey("relay_url")
    private val nodeIdKey = stringPreferencesKey("node_id")

    val prefs: Flow<AppPrefs> = context.dataStore.data.map { p ->
        val modeRaw = p[modeKey] ?: ConnectionMode.LAN.name
        val mode = runCatching { ConnectionMode.valueOf(modeRaw) }.getOrDefault(ConnectionMode.LAN)
        AppPrefs(
            host = p[hostKey].orEmpty(),
            port = p[portKey] ?: 8765,
            deviceName = p[nameKey] ?: "Android Phone",
            token = p[tokenKey].orEmpty(),
            autoReconnect = p[reconnectKey] ?: true,
            landscapeRemote = p[landscapeKey] ?: true,
            screenQuality = p[qualityKey] ?: 70,
            extraProjects = p[extraKey].orEmpty(),
            connectionMode = mode,
            relayUrl = p[relayUrlKey].orEmpty(),
            nodeId = p[nodeIdKey].orEmpty(),
        )
    }

    suspend fun update(
        host: String? = null,
        port: Int? = null,
        deviceName: String? = null,
        token: String? = null,
        autoReconnect: Boolean? = null,
        landscapeRemote: Boolean? = null,
        screenQuality: Int? = null,
        extraProjects: String? = null,
        connectionMode: ConnectionMode? = null,
        relayUrl: String? = null,
        nodeId: String? = null,
    ) {
        context.dataStore.edit { p ->
            host?.let { p[hostKey] = it }
            port?.let { p[portKey] = it }
            deviceName?.let { p[nameKey] = it }
            token?.let { p[tokenKey] = it }
            autoReconnect?.let { p[reconnectKey] = it }
            landscapeRemote?.let { p[landscapeKey] = it }
            screenQuality?.let { p[qualityKey] = it }
            extraProjects?.let { p[extraKey] = it }
            connectionMode?.let { p[modeKey] = it.name }
            relayUrl?.let { p[relayUrlKey] = it }
            nodeId?.let { p[nodeIdKey] = it }
        }
    }
}
