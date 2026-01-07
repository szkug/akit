package com.korilin.samples.compose.trace.acts

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import cn.szkug.akit.apps.cmp.AkitImageDemoScreen
import cn.szkug.akit.apps.cmp.DemoUrls

class NinePatchActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AkitImageDemoScreen(url = DemoUrls.ninePatchUrl)
        }
    }
}
