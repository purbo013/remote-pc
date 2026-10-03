package com.remotecoding.workstation.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import com.remotecoding.workstation.data.ConnectionStatus
import com.remotecoding.workstation.data.SessionState

@Composable
fun HomeScreen(
    prefs: AppPrefs,
    session: SessionState,
    onSave: (String, Int, String) -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onRemote: () -> Unit,
    onLocalhost: () -> Unit,
    onProjects: () -> Unit,
    onSettings: () -> Unit,
    onPair: () -> Unit,
) {
    var host by remember { mutableStateOf(prefs.host) }
    var port by remember { mutableStateOf(prefs.port.toString()) }
    var name by remember { mutableStateOf(prefs.deviceName) }

    LaunchedEffect(prefs.host, prefs.port, prefs.deviceName) {
        host = prefs.host
        port = prefs.port.toString()
        name = prefs.deviceName
    }

    val online = session.status == ConnectionStatus.Connected
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
        OutlinedTextField(host, { host = it }, label = { Text("PC IP") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(port, { port = it }, label = { Text("Port") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(name, { name = it }, label = { Text("Device name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Button(
            onClick = {
                onSave(host, port.toIntOrNull() ?: 8765, name)
                if (online) onDisconnect() else onConnect()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (online) "DISCONNECT" else "CONNECT")
        }
        OutlinedButton(onClick = onPair, modifier = Modifier.fillMaxWidth()) { Text("PAIR") }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onRemote, modifier = Modifier.fillMaxWidth(), enabled = online) { Text("REMOTE") }
        Button(onClick = onLocalhost, modifier = Modifier.fillMaxWidth(), enabled = prefs.host.isNotBlank()) { Text("LOCALHOST") }
        Button(onClick = onProjects, modifier = Modifier.fillMaxWidth()) { Text("PROJECTS") }
        OutlinedButton(onClick = onSettings, modifier = Modifier.fillMaxWidth()) { Text("SETTINGS") }
    }
}
