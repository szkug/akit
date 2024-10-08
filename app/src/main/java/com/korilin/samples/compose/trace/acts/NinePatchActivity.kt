package com.korilin.samples.compose.trace.acts

import android.graphics.Rect
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.korilin.samples.compose.trace.R
import com.korilin.samples.compose.trace.glide.glideBackground

fun Modifier.draw9Patch(
    @DrawableRes ninePatchRes: Int,
) = composed {
    val context = LocalContext.current
    drawBehind {
        drawIntoCanvas {
            val ninePatch = ContextCompat.getDrawable(context, ninePatchRes)!!
            ninePatch.run {
                bounds = Rect(0, 0, size.width.toInt(), size.height.toInt())
                draw(it.nativeCanvas)
            }
        }
    }
}

class NinePatchActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Column(
                modifier = Modifier.padding(10.dp)
            ) {

                Text(
                    text = "Kotlin",
                    color = Color.White,
                    modifier = Modifier.draw9Patch(
                        ninePatchRes = R.drawable.chat_msg_bg,
                    )
                )


                Text(
                    text = "Kotlin & Compose & Kotlin & Compose",
                    color = Color.White,
                    modifier = Modifier.draw9Patch(
                        ninePatchRes = R.drawable.chat_msg_bg,
                    )
                )


                Text(
                    text = "Kotlin & Compose & Kotlin & Compose & Kotlin & Compose & Kotlin & Compose & Kotlin & Compose",
                    color = Color.White,
                    modifier = Modifier.draw9Patch(
                        ninePatchRes = R.drawable.chat_msg_bg,
                    )
                )


                Text(
                    text = "Kotlin",
                    color = Color.White,
                    modifier = Modifier.glideBackground(
                        model = R.drawable.chat_msg_bg,
                    )
                )


                Text(
                    text = "Kotlin & Compose & Kotlin & Compose",
                    color = Color.White,
                    modifier = Modifier.glideBackground(
                        model = R.drawable.chat_msg_bg,
                    )
                )


                Text(
                    text = "Kotlin & Compose & Kotlin & Compose & Kotlin & Compose & Kotlin & Compose & Kotlin & Compose",
                    color = Color.White,
                    modifier = Modifier.glideBackground(
                        model = R.drawable.chat_msg_bg,
                    )
                )
            }
        }
    }
}
