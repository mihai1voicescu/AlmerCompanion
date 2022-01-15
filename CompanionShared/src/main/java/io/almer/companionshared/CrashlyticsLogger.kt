package io.almer.companionshared

import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.lighthousegames.logging.Logger

class CrashlyticsLogger : Logger {
    private val firebaseCrashlytics = FirebaseCrashlytics.getInstance()
    override fun verbose(tag: String, msg: String) {}

    override fun debug(tag: String, msg: String) {
        firebaseCrashlytics.log(msg)
    }

    override fun info(tag: String, msg: String) {
        firebaseCrashlytics.log(msg)
    }

    override fun warn(tag: String, msg: String, t: Throwable?) {
        firebaseCrashlytics.log(msg)
    }

    override fun error(tag: String, msg: String, t: Throwable?) {
        firebaseCrashlytics.log(msg)
    }

    override fun isLoggingVerbose(): Boolean = false

    override fun isLoggingDebug(): Boolean = true

    override fun isLoggingInfo(): Boolean = true

    override fun isLoggingWarning(): Boolean = true

    override fun isLoggingError(): Boolean = true
}