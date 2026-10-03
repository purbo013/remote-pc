package com.remotecoding.workstation.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.remotecoding.workstation.data.AppPrefs

@Composable
fun SettingsScreen(
    prefs: AppPrefs,
    onSave: (String, Int, String, Boolean, Boolean, Int) -> Unit,
    onBack: () -> Unit,
) {
    var host by remember { mutableStateOf(prefs.host) }
    var port by remember { mutableStateOf(prefs.port.toString()) }
    var name by remember { mutableStateOf(prefs.deviceName) }
    var reconnect by remember { mutableStateOf(prefs.autoReconnect) }
    var landscape by remember { mutableStateOf(prefs.landscapeRemote) }
    var quality by remember { mutableStateOf(prefs.screenQuality.toString()) }

    LaunchedEffect(prefs) {
        host = prefs.host
        port = prefs.port.toString()
        name = prefs.deviceName
        reconnect = prefs.autoReconnect
        landscape = prefs.landscapeRemote
        quality = prefs.screenQuality.toString()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(host, { host = it }, label = { Text("PC IP") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(port, { port = it }, label = { Text("PC Port") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(name, { name = it }, label = { Text("Device name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(quality, { quality = it.filter(Char::isDigit).take(3) }, label = { Text("Screen quality (1-100)") }, modifier = Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Auto reconnect", modifier = Modifier.weight(1f))
            Switch(checked = reconnect, onCheckedChange = { reconnect = it })
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Landscape for Remote", modifier = Modifier.weight(1f))
            Switch(checked = landscape, onCheckedChange = { landscape = it })
        }
        Button(
            onClick = {
                onSave(
                    host,
                    port.toIntOrNull() ?: 8765,
                    name,
                    reconnect,
                    landscape,
                    (quality.toIntOrNull() ?: 70).coerceIn(20, 95),
                )
                onBack()
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("SAVE") }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("BACK") }
    }
}
