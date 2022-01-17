package io.almer.comanderdebug

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import io.almer.commander.CommanderServer
import io.almer.companionshared.server.DeviceScan
import org.lighthousegames.logging.KmLogging
import org.lighthousegames.logging.LogLevelController
import org.lighthousegames.logging.PlatformLogger
import org.lighthousegames.logging.logging

@OptIn(ExperimentalPermissionsApi::class)
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        KmLogging.setLoggers(PlatformLogger(object : LogLevelController {
            override fun isLoggingDebug() = true

            override fun isLoggingError() = true

            override fun isLoggingInfo() = true

            override fun isLoggingVerbose() = false

            override fun isLoggingWarning() = true
        }))


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
                        val intent = Intent(this, CommanderService::class.java) // Build the intent for the service

                        applicationContext.startService(intent)
                        Main()
                    }
                }
            }
        }
    }

    companion object {
        val Log = logging()
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
//    val act = mainActivity()

    Text("Online")

//    val device by act.commanderServer.device.collectAsState()
//
//    device?.let { device ->
//        Text(getName(device) ?: device.address)
//    } ?: run {
//        Text("Not connected")
//    }
}
