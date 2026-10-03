package com.remotecoding.workstation

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.remotecoding.workstation.ui.AppViewModel
import com.remotecoding.workstation.ui.screens.BrowserScreen
import com.remotecoding.workstation.ui.screens.HomeScreen
import com.remotecoding.workstation.ui.screens.PairScreen
import com.remotecoding.workstation.ui.screens.ProjectsScreen
import com.remotecoding.workstation.ui.screens.RemoteScreen
import com.remotecoding.workstation.ui.screens.SettingsScreen
import com.remotecoding.workstation.ui.theme.RemoteTheme

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RemoteTheme {
                val prefs by viewModel.prefs.collectAsStateWithLifecycle()
                val session by viewModel.sessionState.collectAsStateWithLifecycle()
                val projects by viewModel.projects.collectAsStateWithLifecycle()
                val notice by viewModel.notice.collectAsStateWithLifecycle()
                var route by remember { mutableStateOf("home") }
                var browserUrl by remember { mutableStateOf("") }
                var askLandscape by remember { mutableStateOf(false) }

                fun goHome() {
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    route = "home"
                }

                Scaffold { padding ->
                    Surface(Modifier.fillMaxSize().padding(padding)) {
                        when (route) {
                            "home" -> HomeScreen(
                                prefs = prefs,
                                session = session,
                                onSave = viewModel::saveConnection,
                                onConnect = viewModel::connect,
                                onDisconnect = viewModel::disconnect,
                                onRemote = {
                                    if (prefs.landscapeRemote) askLandscape = true else route = "remote"
                                },
                                onLocalhost = {
                                    browserUrl = "http://${prefs.host}/"
                                    route = "browser"
                                },
                                onProjects = {
                                    viewModel.refreshProjects()
                                    route = "projects"
                                },
                                onSettings = { route = "settings" },
                                onPair = { route = "pair" },
                            )
                            "pair" -> PairScreen(
                                onPair = {
                                    viewModel.pair(it)
                                    route = "home"
                                },
                                onBack = { route = "home" },
                            )
                            "settings" -> SettingsScreen(
                                prefs = prefs,
                                onSave = viewModel::saveSettings,
                                onBack = { route = "home" },
                            )
                            "projects" -> ProjectsScreen(
                                projects = projects,
                                onOpen = {
                                    browserUrl = it
                                    route = "browser"
                                },
                                onAdd = viewModel::addProject,
                                onBack = { route = "home" },
                            )
                            "browser" -> BrowserScreen(startUrl = browserUrl, onBack = { route = "home" })
                            "remote" -> RemoteScreen(
                                session = session,
                                controller = viewModel.session,
                                onBack = { goHome() },
                            )
                        }
                    }
                }

                if (notice.isNotBlank()) {
                    AlertDialog(
                        onDismissRequest = viewModel::clearNotice,
                        title = { Text("Notice") },
                        text = { Text(notice) },
                        confirmButton = { TextButton(onClick = viewModel::clearNotice) { Text("OK") } },
                    )
                }

                if (askLandscape) {
                    AlertDialog(
                        onDismissRequest = { askLandscape = false },
                        title = { Text("Switch to landscape?") },
                        text = { Text("Remote coding is easier in landscape.") },
                        confirmButton = {
                            TextButton(onClick = {
                                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                askLandscape = false
                                route = "remote"
                            }) { Text("YES") }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                askLandscape = false
                                route = "remote"
                            }) { Text("STAY PORTRAIT") }
                        },
                    )
                }

                BackHandler(enabled = route != "home") { goHome() }
            }
        }
    }
}
