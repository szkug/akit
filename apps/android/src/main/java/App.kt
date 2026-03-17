package munchkin.sample

import android.app.Application
import munchkin.image.AsyncImageLogger
import munchkin.image.DefaultPlatformAsyncImageLogger


class App : Application() {

    override fun onCreate() {
        super.onCreate()
        DefaultPlatformAsyncImageLogger.setLevel(AsyncImageLogger.Level.DEBUG)
    }
}
