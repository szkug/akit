package com.korilin.samples.compose.trace.acts

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.korilin.samples.compose.trace.cmp.AkitImageDemoScreen
import com.korilin.samples.compose.trace.cmp.DemoUrls

class NinePatchActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AkitImageDemoScreen(url = DemoUrls.ninePatchUrl)
        }
    }
}
