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
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    fun health(host: String, port: Int): Boolean {
        val request = Request.Builder().url("http://$host:$port/health").build()
        return try {
            client.newCall(request).execute().use { it.isSuccessful }
        } catch (_: IOException) {
            false
        }
    }

    fun pair(host: String, port: Int, code: String, deviceName: String): String {
        val body = JSONObject()
            .put("code", code)
            .put("deviceName", deviceName)
            .toString()
            .toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("http://$host:$port/api/pair")
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

    fun projects(host: String, port: Int, token: String): List<ProjectItem> {
        val request = Request.Builder()
            .url("http://$host:$port/api/projects")
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

    fun connectHint(host: String, hadTimeout: Boolean, hadRefused: Boolean): String {
        return buildString {
            append("Unable to connect to $host.\n\nPossible causes:\n")
            append("- PC offline\n")
            append("- Wrong IP address or port\n")
            append("- Firewall blocking TCP 8765 on the Private network\n")
            append("- Remote server is not running\n")
            append("- Phone and PC are not on the same Wi-Fi\n")
            if (hadTimeout) append("\nThe request timed out.")
            if (hadRefused) append("\nThe connection was refused.")
        }
    }
}
