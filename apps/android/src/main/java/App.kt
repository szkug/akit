package munchkin.sample

import android.app.Application
import munchkin.resources.loader.DefaultPlatformMunchkinLogger
import munchkin.resources.loader.MunchkinLogger


class App : Application() {

    override fun onCreate() {
        super.onCreate()
        DefaultPlatformMunchkinLogger.setLevel(MunchkinLogger.Level.DEBUG)
    }
}
