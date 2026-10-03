package com.remotecoding.workstation.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.remotecoding.workstation.data.ProjectItem

@Composable
fun ProjectsScreen(
    projects: List<ProjectItem>,
    onOpen: (String) -> Unit,
    onAdd: (String, String) -> Unit,
    onBack: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Projects", style = MaterialTheme.typography.headlineSmall)
        Text("Open using the PC LAN IP. Do not use localhost on this phone.")
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(projects) { project ->
                Card(Modifier.fillMaxWidth().clickable { onOpen(project.url) }) {
                    Column(Modifier.padding(12.dp)) {
                        Text(project.name, style = MaterialTheme.typography.titleMedium)
                        Text(project.url, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(url, { url = it }, label = { Text("http://PC_IP:port/path") }, modifier = Modifier.fillMaxWidth())
        Button(
            onClick = {
                if (name.isNotBlank() && url.isNotBlank()) {
                    onAdd(name.trim(), url.trim())
                    name = ""
                    url = ""
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("ADD PROJECT") }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("BACK") }
    }
}
