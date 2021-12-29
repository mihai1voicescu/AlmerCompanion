package io.almer.comanderdebug

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionsRequired
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import io.almer.comanderdebug.MainActivity.Companion.mainActivity
import io.almer.comanderdebug.ui.theme.AlmerCompanionTheme
import io.almer.companionshared.server.CommanderConnector
import io.almer.companionshared.server.CommanderServer
import io.almer.companionshared.server.DeviceScan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

@OptIn(ExperimentalPermissionsApi::class)
class MainActivity : ComponentActivity() {

    val deviceScan = DeviceScan(this)
    lateinit var commanderServer: CommanderServer
    lateinit var commanderConnector: CommanderConnector

    override fun onDestroy() {
        super.onDestroy()
        commanderServer.close()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Timber.plant(Timber.DebugTree())
        commanderServer = CommanderServer(this)
        commanderConnector = CommanderConnector(this)

        setContent {
            AlmerCompanionTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {

                    val permissions = rememberMultiplePermissionsState(
                        listOf(
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_ADMIN,
                            Manifest.permission.CHANGE_WIFI_STATE,
                            Manifest.permission.ACCESS_NETWORK_STATE,
                            Manifest.permission.ACCESS_WIFI_STATE,
                        )
                    )

                    PermissionsRequired(
                        multiplePermissionsState = permissions,
                        permissionsNotGrantedContent = {
                            LaunchedEffect(true) {
                                permissions.launchMultiplePermissionRequest()
                            }
                            Text("No permissions no App")
                        },
                        permissionsNotAvailableContent = {
                            Text("Device does not have the required permissions")
                        }) {
                        Main()
                    }
                }
            }
        }
    }

    companion object {
        @Composable
        fun mainActivity(): MainActivity {
            return LocalContext.current as MainActivity
        }
    }
}

@Composable
fun Main() {
    val act = mainActivity()
    val state by act.deviceScan.viewState.collectAsState()

    val scope = rememberCoroutineScope()

    val results by act.deviceScan.scanResults.collectAsState()

    val messages by act.commanderServer.messages.collectAsState()

    Column {
        Button(onClick = {
            scope.launch {
//                act.chatServer.scanner()
                act.deviceScan.scan(4_000)
            }
        }) {
            Text("Scan")
        }

        Text("Entries")

        LazyColumn(content = {
            results.map {
                item {
                    Button(onClick = {
                        scope.launch {
                            act.commanderConnector.setCurrentChatConnection(it.value)

                            withContext(Dispatchers.Main) {
                                while (true) {
                                    delay(2000)
                                    act.commanderConnector.sendMessage("Hello")
                                }
                            }

                        }
                    }) {
                        Text(it.value.name)
                    }
                }
            }

            messages.map {
                item {
                    Text(it)
                }
            }
        })
    }
}
