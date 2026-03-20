package munchkin.sample

import android.app.Application
import munchkin.resources.runtime.DefaultPlatformMunchkinLogger
import munchkin.resources.runtime.MunchkinLogger


class App : Application() {

    override fun onCreate() {
        super.onCreate()
        DefaultPlatformMunchkinLogger.setLevel(MunchkinLogger.Level.DEBUG)
    }
}
