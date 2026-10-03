package com.remotecoding.workstation.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.remotecoding.workstation.data.AppPrefs
import com.remotecoding.workstation.data.ConnectionMode
import com.remotecoding.workstation.data.ConnectionStatus
import com.remotecoding.workstation.data.SessionState

@Composable
fun HomeScreen(
    prefs: AppPrefs,
    session: SessionState,
    onSave: (ConnectionMode, String, Int, String, String, String) -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onRemote: () -> Unit,
    onLocalhost: () -> Unit,
    onProjects: () -> Unit,
    onSettings: () -> Unit,
    onPair: () -> Unit,
) {
    var mode by remember { mutableStateOf(prefs.connectionMode) }
    var host by remember { mutableStateOf(prefs.host) }
    var port by remember { mutableStateOf(prefs.port.toString()) }
    var relayUrl by remember { mutableStateOf(prefs.relayUrl) }
    var nodeId by remember { mutableStateOf(prefs.nodeId) }
    var name by remember { mutableStateOf(prefs.deviceName) }

    LaunchedEffect(prefs) {
        mode = prefs.connectionMode
        host = prefs.host
        port = prefs.port.toString()
        relayUrl = prefs.relayUrl
        nodeId = prefs.nodeId
        name = prefs.deviceName
    }

    val online = session.status == ConnectionStatus.Connected
    val localhostOk = mode == ConnectionMode.LAN && host.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Remote Coding", style = MaterialTheme.typography.headlineMedium)
        Text(
            if (online) "PC  •  ONLINE" else "PC  •  OFFLINE",
            color = if (online) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
        )
        if (session.message.isNotBlank()) {
            Text(session.message, style = MaterialTheme.typography.bodyMedium)
        }
        Text("Connection", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = mode == ConnectionMode.LAN,
                onClick = { mode = ConnectionMode.LAN },
                label = { Text("Wi‑Fi (LAN)") },
            )
            FilterChip(
                selected = mode == ConnectionMode.INTERNET,
                onClick = { mode = ConnectionMode.INTERNET },
                label = { Text("Internet") },
            )
        }
        when (mode) {
            ConnectionMode.LAN -> {
                OutlinedTextField(host, { host = it }, label = { Text("PC IP") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(port, { port = it }, label = { Text("Port") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
            ConnectionMode.INTERNET -> {
                OutlinedTextField(
                    relayUrl,
                    { relayUrl = it },
                    label = { Text("Relay URL") },
                    placeholder = { Text("https://relay.example.com") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    nodeId,
                    { nodeId = it },
                    label = { Text("Node ID") },
                    placeholder = { Text("my-pc") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Text(
                    "Use Relay URL + Node ID from the PC console (config relay.enabled).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        OutlinedTextField(name, { name = it }, label = { Text("Device name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Button(
            onClick = {
                onSave(mode, host, port.toIntOrNull() ?: 8765, relayUrl, nodeId, name)
                if (online) onDisconnect() else onConnect()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (online) "DISCONNECT" else "CONNECT")
        }
        OutlinedButton(onClick = onPair, modifier = Modifier.fillMaxWidth()) { Text("PAIR") }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onRemote, modifier = Modifier.fillMaxWidth(), enabled = online) { Text("REMOTE") }
        Button(onClick = onLocalhost, modifier = Modifier.fillMaxWidth(), enabled = localhostOk) { Text("LOCALHOST") }
        if (mode == ConnectionMode.INTERNET) {
            Text(
                "LOCALHOST preview needs LAN or a public URL in Projects.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Button(onClick = onProjects, modifier = Modifier.fillMaxWidth()) { Text("PROJECTS") }
        OutlinedButton(onClick = onSettings, modifier = Modifier.fillMaxWidth()) { Text("SETTINGS") }
    }
}
