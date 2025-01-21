package com.korilin.samples.compose.trace.acts

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.korilin.samples.compose.trace.R
import com.korilin.samples.compose.trace.Stores
import com.korilin.akit.compose.image.publics.AsyncImageContext
import com.korilin.akit.compose.image.publics.DrawableTranscoder
import com.korilin.akit.compose.image.publics.glideBackground
import com.korilin.akit.compose.image.publics.rememberAsyncImageContext
import com.korilin.samples.compose.trace.ninepatch.NinePatchChunk
import com.korilin.samples.compose.trace.sp

class NinePatchActivity : ComponentActivity() {

    private val url = Stores.ninePatchUrl
    private val extension = AsyncImageContext(
        this,
        enableLog = true,
        drawableTransformations = listOf(NinePatchDrawableTranscoder),
        requestBuilder = {
            Glide.with(it).asDrawable().skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
        }
    )

    private fun Modifier.background2(
        model: Any?,
        placeholder: Int? = null,
    ) = glideBackground(
        model,
        placeholder,
        context = extension
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Column(
                modifier = Modifier.padding(10.dp)
            ) {

                Text(
                    text = "Kotlin & Compose & Kotlin & Compose",
                    color = Color.White,
                    fontSize = 8.dp.sp,
                    modifier = Modifier
                        .background2(model = R.drawable.nine_patch_2)
                )

                Text(
                    text = "Kotlin & Compose & Kotlin & Compose",
                    color = Color.White,
                    fontSize = 8.dp.sp,
                    modifier = Modifier
                        .background2(model = R.drawable.nine_patch_2)
                )

                Text(
                    text = "Kotlin & Compose & Kotlin & Compose & Kotlin & Compose & Kotlin & Compose & Kotlin & Compose",
                    color = Color.White,
                    modifier = Modifier
                        .background2(model = R.drawable.nine_patch_2)
                )

                Text("===============")

                Text(
                    text = "Kotlin & Compose & Kotlin & Compose",
                    color = Color.White,
                    fontSize = 8.dp.sp,
                    modifier = Modifier
                        .background2(
                            model = url,
                            placeholder = R.drawable.nine_patch_2
                        )
                )

                Text(
                    text = "Kotlin & Compose & Kotlin & Compose & Kotlin & Compose & Kotlin & Compose",
                    color = Color.White,
                    modifier = Modifier.background2(
                        model = url,
                        placeholder = R.drawable.nine_patch_2
                    )
                )

                Text(
                    text = "Kotlin & Compose & Kotlin & Compose & Kotlin & Compose & Kotlin & Compose & Kotlin & Compose & Compose & Kotlin & Compose & Kotlin & Compose & Kotlin & Compose",
                    color = Color.White,
                    modifier = Modifier.background2(
                        model = url,
                        placeholder = R.drawable.nine_patch_2
                    )
                )
            }
        }
    }
}

@Composable
@Preview
fun Preview() {
    Text(
        text = "Kotlin Kotlin Kotlin Kotlin",
        color = Color.White,
        fontSize = 8.dp.sp,
        modifier = Modifier
            .glideBackground(
                model = R.drawable.nine_patch_2,
                context = rememberAsyncImageContext(
                    drawableTransformation = listOf(NinePatchDrawableTranscoder),
                )
            )
    )
}

object NinePatchDrawableTranscoder : DrawableTranscoder() {

    override fun key(): String {
        return "NinePatchDrawableTranscoder"
    }

    override fun transcode(
        context: Context,
        resource: Drawable,
        width: Int,
        height: Int
    ): Drawable {
        if (resource !is BitmapDrawable) return resource
        val bitmap = resource.bitmap
        if (!NinePatchChunk.isRawNinePatchBitmap(bitmap)) return resource
        Log.d("NinePatchDrawableTranscoder", "transcode")
        return NinePatchChunk.create9PatchDrawable(context, bitmap, null)
    }
}