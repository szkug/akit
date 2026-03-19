package munchkin.apps.cmp

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import munchkin.image.PainterModel
import munchkin.resources.loader.DefaultPlatformMunchkinLogger
import munchkin.resources.loader.MunchkinLogger
import munchkin.image.munchkinAsyncBackground
import munchkin.resources.runtime.painterResource
import munchkin.resources.runtime.stringResource

@Composable
fun MunchkinImageDemoScreen(
    url: String,
    modifier: Modifier = Modifier,
) {
    DefaultPlatformMunchkinLogger.setLevel(MunchkinLogger.Level.DEBUG)
    val engine = rememberDemoAsyncEngine()
    val placeholder = PainterModel(painterResource(SharedDrawable.nine_patch_2))

    Column(
        modifier = modifier.padding(16.dp)
    ) {
        Text(text = stringResource(Res.strings.ninepatch_demo_title))

        Text(text = stringResource(Res.strings.section_res))

        DemoTextCard(
            text = stringResource(Res.strings.sample_short),
            modifier = Modifier
                .munchkinAsyncBackground(
                    painterResource(SharedDrawable.nine_patch_2),
                    contentScale = ContentScale.FillBounds,
                    engine = engine,
                )
                .padding(8.dp),
        )

        DemoTextCard(
            text = stringResource(Res.strings.sample_medium),
            modifier = Modifier
                .munchkinAsyncBackground(
                    painterResource(SharedDrawable.nine_patch_2),
                    contentScale = ContentScale.FillBounds,
                    engine = engine,
                )
                .padding(8.dp),
        )

        DemoTextCard(
            text = stringResource(Res.strings.sample_long),
            modifier = Modifier
                .munchkinAsyncBackground(
                    painterResource(SharedDrawable.nine_patch_2),
                    contentScale = ContentScale.FillBounds,
                    engine = engine,
                )
                .padding(8.dp),
        )

        Text(text = stringResource(Res.strings.section_url))

        DemoTextCard(
            text = stringResource(Res.strings.sample_short),
            modifier = Modifier.munchkinAsyncBackground(
                model = url,
                placeholder = placeholder,
                engine = engine,
            ),
        )

        DemoTextCard(
            text = stringResource(Res.strings.sample_medium),
            modifier = Modifier.munchkinAsyncBackground(
                model = url,
                placeholder = placeholder,
                engine = engine,
            ),
        )

        DemoTextCard(
            text = stringResource(Res.strings.sample_long),
            modifier = Modifier.munchkinAsyncBackground(
                model = url,
                placeholder = placeholder,
                engine = engine,
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
