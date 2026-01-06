package com.korilin.samples.compose.trace.ios

import androidx.compose.ui.window.ComposeUIViewController
import com.korilin.samples.compose.trace.cmp.AkitImageDemoScreen
import com.korilin.samples.compose.trace.cmp.DemoUrls

fun MainViewController() = ComposeUIViewController {
    AkitImageDemoScreen(url = DemoUrls.ninePatchUrl)
}
