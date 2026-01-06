package com.korilin.samples.compose.trace.cmp

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

fun MainViewController() = ComposeUIViewController {
    AkitImageDemoScreen(url = DemoUrls.ninePatchUrl)
}

class AkitCmpEntry {
    fun mainViewController(): UIViewController {
        return ComposeUIViewController {
            AkitImageDemoScreen(url = DemoUrls.ninePatchUrl)
        }
    }
}
