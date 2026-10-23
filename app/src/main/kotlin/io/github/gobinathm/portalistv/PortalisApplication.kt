package io.github.gobinathm.portalistv

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import io.github.gobinathm.portalistv.core.common.log.Log
import io.github.gobinathm.portalistv.core.common.log.LogLevel

@HiltAndroidApp
class PortalisApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.install(AndroidLogSink, if (BuildConfig.DEBUG) LogLevel.VERBOSE else LogLevel.WARN)
    }
}
