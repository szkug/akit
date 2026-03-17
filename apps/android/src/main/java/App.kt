package akit.sample

import android.app.Application
import akit.image.AsyncImageLogger
import akit.image.DefaultPlatformAsyncImageLogger


class App : Application() {

    override fun onCreate() {
        super.onCreate()
        DefaultPlatformAsyncImageLogger.setLevel(AsyncImageLogger.Level.DEBUG)
    }
}
