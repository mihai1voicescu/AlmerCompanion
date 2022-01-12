package io.almer.comanderdebug

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresPermission
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionsRequired
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import io.almer.comanderdebug.MainActivity.Companion.mainActivity
import io.almer.comanderdebug.ui.theme.AlmerCompanionTheme
import io.almer.commander.CommanderServer
import io.almer.companionshared.server.DeviceScan
import timber.log.Timber

@OptIn(ExperimentalPermissionsApi::class)
class MainActivity : ComponentActivity() {

    val deviceScan = DeviceScan(this)
    lateinit var commanderServer: CommanderServer

    override fun onDestroy() {
        super.onDestroy()
        commanderServer.close()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Timber.plant(Timber.DebugTree())
        commanderServer = CommanderServer(this)

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

@SuppressLint("MissingPermission")
fun getName(device: BluetoothDevice): String? {
    return device.name
}

@Composable
fun Main() {
    val act = mainActivity()

    val device by act.commanderServer.device.collectAsState()

    device?.let { device ->

        val name =
            Text(getName(device) ?: device.address)

    } ?: run {
        Text("Not connected")
    }
}
