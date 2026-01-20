package cn.szkug.samples.compose.trace

import android.app.Application
import cn.szkug.akit.image.AsyncImageLogger
import cn.szkug.akit.image.DefaultPlatformAsyncImageLogger


class App : Application() {

    override fun onCreate() {
        super.onCreate()
        DefaultPlatformAsyncImageLogger.setLevel(AsyncImageLogger.Level.DEBUG)
    }
}
