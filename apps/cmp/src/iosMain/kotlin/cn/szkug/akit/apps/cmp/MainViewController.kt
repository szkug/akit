package cn.szkug.akit.apps.cmp

import androidx.compose.ui.window.ComposeUIViewController

fun MainViewController() = ComposeUIViewController {
    AkitImageDemoScreen(url = DemoUrls.ninePatchUrl)
}
