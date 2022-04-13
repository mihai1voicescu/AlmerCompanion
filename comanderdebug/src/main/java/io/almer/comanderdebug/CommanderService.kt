package io.almer.comanderdebug

import android.app.Service
import android.content.Intent
import android.os.IBinder
import io.almer.commander.CommanderServer
import org.lighthousegames.logging.logging

class CommanderService : Service() {
    companion object {
        val Log = logging()
    }

    lateinit var commanderServer: CommanderServer


    override fun onCreate() {
        Log.i { "Commander service started" }
        super.onCreate()
        commanderServer = CommanderServer(this)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}