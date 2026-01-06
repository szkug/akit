package com.korilin.samples.compose.trace.cmp

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import cn.szkug.akit.publics.PainterModel
import cn.szkug.akit.publics.akitAsyncBackground
import cn.szkug.akit.publics.rememberAsyncImageContext

@Composable
fun AkitImageDemoScreen(
    url: String,
    modifier: Modifier = Modifier,
) {
    val placeholder = PainterModel(Res.drawable.nine_patch_2())
    val asyncContext = rememberAsyncImageContext()

    Column(
        modifier = modifier.padding(10.dp)
    ) {
        Text(text = Res.strings.demo_title())

        Text(text = Res.strings.section_res())

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
                context = asyncContext,
            ),
        )

        DemoTextCard(
            text = Res.strings.sample_medium(),
            modifier = Modifier.akitAsyncBackground(
                model = url,
                placeholder = placeholder,
                context = asyncContext,
            ),
        )

        DemoTextCard(
            text = Res.strings.sample_long(),
            modifier = Modifier.akitAsyncBackground(
                model = url,
                placeholder = placeholder,
                context = asyncContext,
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
