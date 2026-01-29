package cn.szkug.akit.apps.cmp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import cn.szkug.akit.image.AkitAsyncImage

private enum class DemoPage {
    Home,
    ImageDemo,
    AnimatedImageList,
    LocalizedDemo,
    ShadowEffects,
    BlurDemo,
    LottieDemo,
}

@Composable
fun AkitCmpApp() {
    Box(
        modifier = Modifier.padding(vertical = 44.dp)
    ) {
        var page by remember { mutableStateOf(DemoPage.Home) }
        when (page) {
            DemoPage.Home -> HomeScreen(onNavigate = { page = it })
            DemoPage.ImageDemo -> ImageNinePatchPage(onBack = { page = DemoPage.Home })
            DemoPage.AnimatedImageList -> AnimatedImageListPage(onBack = { page = DemoPage.Home })
            DemoPage.LocalizedDemo -> LanguageDemoPage(onBack = { page = DemoPage.Home })
            DemoPage.ShadowEffects -> ShadowEffectDemoPage(onBack = { page = DemoPage.Home })
            DemoPage.BlurDemo -> BlurDemoPage(onBack = { page = DemoPage.Home })
            DemoPage.LottieDemo -> LottieDemoPage(onBack = { page = DemoPage.Home })
        }
    }
}

@Composable
private fun HomeScreen(onNavigate: (DemoPage) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Akit CMP Demo")
        Text(text = "Select a page:")
        Button(onClick = { onNavigate(DemoPage.ImageDemo) }) {
            Text(text = "NinePatch Demo")
        }
        Button(onClick = { onNavigate(DemoPage.AnimatedImageList) }) {
            Text(text = "Animated Image List")
        }
        Button(onClick = { onNavigate(DemoPage.LocalizedDemo) }) {
            Text(text = "Language Demo")
        }
        Button(onClick = { onNavigate(DemoPage.ShadowEffects) }) {
            Text(text = "Shadow Effects")
        }
        Button(onClick = { onNavigate(DemoPage.BlurDemo) }) {
            Text(text = "Gaussian Blur")
        }
        Button(onClick = { onNavigate(DemoPage.LottieDemo) }) {
            Text(text = "Lottie Demo")
        }
    }
}

@Composable
private fun ImageNinePatchPage(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        PageHeader(title = "NinePatch", onBack = onBack)
        AkitImageDemoScreen(
            url = DemoUrls.ninePatchUrl,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun AnimatedImageListPage(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        PageHeader(title = "Animated Image List", onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(DemoUrls.gifUrls) { index, url ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(text = "GIF #${index + 1}")
                    AkitAsyncImage(
                        model = url,
                        contentDescription = "gif-$index",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun PageHeader(
    title: String,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(onClick = onBack) {
            Text(text = "Back")
        }
        Text(text = title)
    }
}
