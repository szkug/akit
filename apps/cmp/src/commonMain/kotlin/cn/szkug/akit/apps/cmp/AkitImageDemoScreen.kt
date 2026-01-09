package cn.szkug.akit.apps.cmp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import cn.szkug.akit.image.AkitAsyncImage
import cn.szkug.akit.image.AsyncImageLogger
import cn.szkug.akit.image.DefaultPlatformAsyncImageLogger
import cn.szkug.akit.image.PainterModel
import cn.szkug.akit.image.akitAsyncBackground
import cn.szkug.akit.image.rememberAsyncImageContext

@Composable
fun AkitImageDemoScreen(
    url: String,
    modifier: Modifier = Modifier,
) {
    DefaultPlatformAsyncImageLogger.setLevel(AsyncImageLogger.Level.DEBUG)
    val placeholder = PainterModel(Res.drawable.nine_patch_2())

    Column(
        modifier = modifier.padding(10.dp).padding(top = 44.dp)
    ) {
        Text(text = Res.strings.demo_title())

        Text(text = Res.strings.section_res())

        Row {
            AkitAsyncImage(
                model = DemoUrls.urls.first(),
                modifier = Modifier
                    .height(50.dp)
                    .wrapContentWidth()
                    .background(Color.Red),
                contentScale = ContentScale.FillHeight,
                alignment = Alignment.CenterStart,
                contentDescription = null
            )
        }

        DemoTextCard(
            text = Res.strings.sample_short(),
            modifier = Modifier
                .akitAsyncBackground(Res.drawable.nine_patch_2(), contentScale = ContentScale.FillBounds)
                .padding(8.dp),
        )

        DemoTextCard(
            text = Res.strings.sample_medium(),
            modifier = Modifier
                .akitAsyncBackground(Res.drawable.nine_patch_2(), contentScale = ContentScale.FillBounds)
                .padding(8.dp),
        )

        DemoTextCard(
            text = Res.strings.sample_long(),
            modifier = Modifier
                .akitAsyncBackground(Res.drawable.nine_patch_2(), contentScale = ContentScale.FillBounds)
                .padding(8.dp),
        )

        Text(text = Res.strings.section_url())

        DemoTextCard(
            text = Res.strings.sample_short(),
            modifier = Modifier.akitAsyncBackground(
                model = url,
                placeholder = placeholder,
            ),
        )

        DemoTextCard(
            text = Res.strings.sample_medium(),
            modifier = Modifier.akitAsyncBackground(
                model = url,
                placeholder = placeholder,
            ),
        )

        DemoTextCard(
            text = Res.strings.sample_long(),
            modifier = Modifier.akitAsyncBackground(
                model = url,
                placeholder = placeholder,
            ),
        )
    }
}

@Composable
private fun DemoTextCard(
    text: String,
    modifier: Modifier,
) {
    Text(
        text = text,
        color = Color.White,
        modifier = modifier,
    )
}
