package cn.szkug.akit.apps.cmp

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import cn.szkug.akit.image.AsyncImageLogger
import cn.szkug.akit.image.DefaultPlatformAsyncImageLogger
import cn.szkug.akit.image.PainterModel
import cn.szkug.akit.image.akitAsyncBackground
import cn.szkug.akit.resources.runtime.painterResource
import cn.szkug.akit.resources.runtime.stringResource

@Composable
fun AkitImageDemoScreen(
    url: String,
    modifier: Modifier = Modifier,
) {
    DefaultPlatformAsyncImageLogger.setLevel(AsyncImageLogger.Level.DEBUG)
    val placeholder = PainterModel(painterResource(SharedDrawable.nine_patch_2))

    Column(
        modifier = modifier.padding(16.dp)
    ) {
        Text(text = stringResource(Res.strings.ninepatch_demo_title))

        Text(text = stringResource(Res.strings.section_res))

        DemoTextCard(
            text = stringResource(Res.strings.sample_short),
            modifier = Modifier
                .akitAsyncBackground(
                    painterResource(SharedDrawable.nine_patch_2),
                    contentScale = ContentScale.FillBounds,
                )
                .padding(8.dp),
        )

        DemoTextCard(
            text = stringResource(Res.strings.sample_medium),
            modifier = Modifier
                .akitAsyncBackground(
                    painterResource(SharedDrawable.nine_patch_2),
                    contentScale = ContentScale.FillBounds,
                )
                .padding(8.dp),
        )

        DemoTextCard(
            text = stringResource(Res.strings.sample_long),
            modifier = Modifier
                .akitAsyncBackground(
                    painterResource(SharedDrawable.nine_patch_2),
                    contentScale = ContentScale.FillBounds,
                )
                .padding(8.dp),
        )

        Text(text = stringResource(Res.strings.section_url))

        DemoTextCard(
            text = stringResource(Res.strings.sample_short),
            modifier = Modifier.akitAsyncBackground(
                model = url,
                placeholder = placeholder,
            ),
        )

        DemoTextCard(
            text = stringResource(Res.strings.sample_medium),
            modifier = Modifier.akitAsyncBackground(
                model = url,
                placeholder = placeholder,
            ),
        )

        DemoTextCard(
            text = stringResource(Res.strings.sample_long),
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
