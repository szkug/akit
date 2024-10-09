package com.korilin.samples.compose.trace.acts

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.NinePatchDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bumptech.glide.Glide
import com.korilin.samples.compose.trace.R
import com.korilin.samples.compose.trace.Stores
import com.korilin.samples.compose.trace.draw9Patch
import com.korilin.samples.compose.trace.glide.DrawableTranscoder
import com.korilin.samples.compose.trace.glide.GlideExtension
import com.korilin.samples.compose.trace.glide.glideBackground
import com.korilin.samples.compose.trace.ninepatch.NinePatchChunk
import com.korilin.samples.compose.trace.sp

class NinePatchActivity : ComponentActivity() {

    private val url = Stores.ninePatchUrl
    val extension = GlideExtension(
        transcoder = NinePatchDrawableTranscoder(this)
    )

    inline fun Modifier.background(
        model: Any?,
        placeholder: Int? = null,
    ) = glideBackground(
        model,
        placeholder,
        extension = extension
    ) {
        Glide.with(it).asDrawable()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Column(
                modifier = Modifier.padding(10.dp)
            ) {


                Text(
                    text = "Kotlin",
                    color = Color.White,
                    fontSize = 8.dp.sp,
                    modifier = Modifier
                        .draw9Patch(R.drawable.nine_patch_1)
                        .padding(2.dp)
                )


                Text(
                    text = "Kotlin & Compose & Kotlin & Compose",
                    color = Color.White,
                    modifier = Modifier
                        .draw9Patch(R.drawable.nine_patch_1)
                        .padding(2.dp)
                )

                Text(
                    text = "Kotlin & Compose & Kotlin & Compose & Kotlin & Compose & Kotlin & Compose & Kotlin & Compose",
                    color = Color.White,
                    modifier = Modifier
                        .draw9Patch(R.drawable.nine_patch_1)
                        .padding(2.dp)
                )

                Text("===============")

                Text(
                    text = "Kotlin",
                    color = Color.White,
                    fontSize = 8.dp.sp,
                    modifier = Modifier
                        .background(
                            model = R.drawable.nine_patch_1,
                        )
                )


                Text(
                    text = "Kotlin & Compose & Kotlin & Compose",
                    color = Color.White,
                    modifier = Modifier.background(
                        model = R.drawable.nine_patch_1,
                    )
                )


                Text(
                    text = "Kotlin & Compose & Kotlin & Compose & Kotlin & Compose & Kotlin & Compose & Kotlin & Compose & Compose & Kotlin & Compose & Kotlin & Compose\"",
                    color = Color.White,
                    modifier = Modifier.background(
                        model = R.drawable.nine_patch_1,
                    )
                )


                Text("===============")


                Text(
                    text = "Kotlin & Compose & Kotlin & Compose",
                    color = Color.White,
                    fontSize = 8.dp.sp,
                    modifier = Modifier
                        .background(R.drawable.nine_patch_2)
                )

                Text(
                    text = "Kotlin & Compose & Kotlin & Compose",
                    color = Color.White,
                    fontSize = 8.dp.sp,
                    modifier = Modifier
                        .background(R.drawable.nine_patch_2)
                )

                Text(
                    text = "Kotlin & Compose & Kotlin & Compose & Kotlin & Compose & Kotlin & Compose & Kotlin & Compose",
                    color = Color.White,
                    modifier = Modifier
                        .background(R.drawable.nine_patch_2)
                )

                Text("===============")

                Text(
                    text = "Kotlin & Compose & Kotlin & Compose",
                    color = Color.White,
                    fontSize = 8.dp.sp,
                    modifier = Modifier
                        .background(
                            model = url,
                        )
                )


                Text(
                    text = "Kotlin & Compose & Kotlin & Compose",
                    color = Color.White,
                    fontSize = 8.dp.sp,
                    modifier = Modifier.background(
                        model = url,
                    )
                )


                Text(
                    text = "Kotlin & Compose & Kotlin & Compose & Kotlin & Compose & Kotlin & Compose & Kotlin & Compose & Compose & Kotlin & Compose & Kotlin & Compose\"",
                    color = Color.White,
                    modifier = Modifier.background(
                        model = url,
                    )
                )
            }
        }
    }
}


class NinePatchDrawableTranscoder(private val context: Context) : DrawableTranscoder {
    override fun transcode(
        drawable: Drawable
    ): Drawable {
        if (drawable !is BitmapDrawable) return drawable
        val bitmap = drawable.bitmap
        if (!NinePatchChunk.isRawNinePatchBitmap(bitmap)) return drawable
        return NinePatchChunk.create9PatchDrawable(context, bitmap, null)
    }
}