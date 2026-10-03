package com.remotecoding.workstation.data

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class RemoteApi {
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    fun health(target: ConnectionTarget): Boolean {
        val request = Request.Builder().url("${target.httpBase()}/health").build()
        return try {
            client.newCall(request).execute().use { it.isSuccessful }
        } catch (_: IOException) {
            false
        }
    }

    fun pair(target: ConnectionTarget, code: String, deviceName: String): String {
        val body = JSONObject()
            .put("code", code)
            .put("deviceName", deviceName)
            .toString()
            .toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("${target.httpBase()}/api/pair")
            .post(body)
            .build()
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val error = runCatching { JSONObject(text).optString("error") }.getOrNull()
                throw IOException(error?.ifBlank { null } ?: "Pairing failed (${response.code})")
            }
            return JSONObject(text).getString("token")
        }
    }

    fun projects(target: ConnectionTarget, token: String): List<ProjectItem> {
        val request = Request.Builder()
            .url("${target.httpBase()}/api/projects")
            .header("Authorization", "Bearer $token")
            .build()
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) return emptyList()
            val array = JSONObject(text).optJSONArray("projects") ?: JSONArray()
            return buildList {
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    add(ProjectItem(item.optString("name"), item.optString("url")))
                }
            }
        }
    }

    fun connectHint(target: ConnectionTarget, hadTimeout: Boolean, hadRefused: Boolean): String {
        val endpoint = when (target.mode) {
            ConnectionMode.LAN -> "${target.host}:${target.port}"
            ConnectionMode.INTERNET -> "${target.relayUrl}/n/${target.nodeId}"
        }
        return buildString {
            append("Unable to connect to $endpoint.\n\nPossible causes:\n")
            append("- PC offline or server not running\n")
            when (target.mode) {
                ConnectionMode.LAN -> {
                    append("- Wrong IP address or port\n")
                    append("- Firewall blocking TCP ${target.port} on the Private network\n")
                    append("- Phone and PC are not on the same Wi-Fi\n")
                }
                ConnectionMode.INTERNET -> {
                    append("- Relay not running or wrong Relay URL\n")
                    append("- Node ID does not match the PC config\n")
                    append("- relay.enabled is false on the PC\n")
                    append("- PC has no internet or cannot reach the relay\n")
                }
            }
            if (hadTimeout) append("\nThe request timed out.")
            if (hadRefused) append("\nThe connection was refused.")
        }
    }
}
